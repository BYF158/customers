<template>
  <div>
    <h1 class="page-title">数据导入</h1>
    <p class="page-sub">Excel 按 Sheet 导入客户经理的拜访计划，支持预校验与合并/替换</p>

    <div class="section-title">1. 准备文件</div>
    <div class="card">
      <div class="info-box">
        每个 Sheet 名称 = 客户经理姓名；表头需含「集团编码」「集团名称」及月份列（2026-10 / 十月 / 10月）。
        若客户经理账号不存在，导入时会自动创建（初始密码见结果提示）。
      </div>
      <div class="mt12 flex" style="gap: 10px; flex-wrap: wrap">
        <van-button size="small" plain icon="down" @click="downloadTemplate">下载导入模板</van-button>
        <span class="mini">支持 .xlsx / .xls / .csv</span>
      </div>
    </div>

    <div class="section-title">2. 选择文件并预览</div>
    <div class="card">
      <div class="dropzone" @click="fileInput?.click()">
        <b>{{ file ? file.name : '点击选择 Excel 文件' }}</b>
        <span>{{ selecting ? '正在解析，请稍候…' : '上传后先预览，不会直接写入数据' }}</span>
      </div>
      <input ref="fileInput" type="file" accept=".xlsx,.xls,.csv" style="display: none" @change="onFileChange" />

      <div v-if="preview" class="mt16">
        <div class="flex-between">
          <span class="pill info">{{ preview.rows.length }} 条计划</span>
          <span class="mini">{{ preview.managers.length }} 位经理 · {{ preview.months.length }} 个月份</span>
        </div>

        <div v-if="preview.warnings.length" class="warn-box mt12">
          <div v-for="(item, index) in preview.warnings.slice(0, 20)" :key="index">· {{ item }}</div>
          <div v-if="preview.warnings.length > 20" class="mini mt8">
            还有 {{ preview.warnings.length - 20 }} 条提示未显示
          </div>
        </div>

        <div class="form-label mt12">数据预览（前 30 条）</div>
        <div class="list-card" style="max-height: 320px; overflow-y: auto">
          <div
            v-for="(row, index) in preview.rows.slice(0, 30)"
            :key="index"
            class="row-item"
            style="cursor: default; padding: 10px 12px"
          >
            <div class="row-main">
              <div class="row-title" style="font-size: 14px">{{ row.name }}</div>
              <div class="row-sub">
                <span class="code">{{ row.code }}</span>
                <span>{{ row.managerName }}</span>
              </div>
            </div>
            <span class="pill">{{ row.planMonth }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="section-title">3. 选择导入方式</div>
    <div class="card">
      <div class="chip-scroll">
        <button class="chip" :class="{ on: mode === 'MERGE' }" @click="mode = 'MERGE'">增量合并</button>
        <button class="chip" :class="{ on: mode === 'REPLACE' }" @click="mode = 'REPLACE'">按范围替换</button>
      </div>
      <div class="form-tip">
        <template v-if="mode === 'MERGE'">保留已有计划和拜访记录，只补充缺失的记录，已存在则跳过。</template>
        <template v-else>
          先删除「本次导入涉及的客户经理 + 月份」范围内的计划与拜访记录，再写入。范围外的数据不受影响。
        </template>
      </div>
      <van-button
        type="primary"
        block
        class="mt16"
        :disabled="!preview || applying"
        :loading="applying"
        @click="apply"
      >
        开始导入
      </van-button>
    </div>

    <template v-if="result">
      <div class="section-title">导入结果</div>
      <div class="card">
        <div class="info-box">{{ result.message }}</div>
        <div v-if="result.createdManagers?.length" class="mt12">
          <div class="form-label">自动创建的账号（请转告本人登录后修改密码）</div>
          <div class="list-card">
            <div v-for="item in result.createdManagers" :key="item.username" class="row-item" style="cursor: default">
              <div class="row-main">
                <div class="row-title">{{ item.name }}</div>
                <div class="row-sub">
                  账号 <span class="code">{{ item.username }}</span>
                  初始密码 <span class="code">{{ item.password }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="result.warnings?.length" class="warn-box mt12">
          <div v-for="(item, index) in result.warnings.slice(0, 20)" :key="index">· {{ item }}</div>
        </div>
        <van-button plain block class="mt12" @click="reset">继续导入</van-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { excelApi } from '../../api'

const fileInput = ref(null)
const file = ref(null)
const selecting = ref(false)
const preview = ref(null)
const mode = ref('MERGE')
const applying = ref(false)
const result = ref(null)

async function downloadTemplate() {
  try {
    await excelApi.template()
  } catch (e) {
    showToast(e.message || '模板下载失败')
  }
}

async function onFileChange(event) {
  const selected = event.target.files?.[0]
  if (!selected) return
  file.value = selected
  preview.value = null
  result.value = null
  selecting.value = true
  try {
    preview.value = await excelApi.preview(selected)
  } catch (e) {
    file.value = null
  } finally {
    selecting.value = false
    event.target.value = ''
  }
}

async function apply() {
  if (!preview.value) return
  if (mode.value === 'REPLACE') {
    try {
      await showConfirmDialog({
        title: '按范围替换',
        message: '将先删除本次涉及的客户经理 + 月份范围内的计划及其拜访记录，再导入。确定继续吗？',
        confirmButtonText: '继续',
        confirmButtonColor: '#dc2626'
      })
    } catch (e) {
      return
    }
  }
  applying.value = true
  try {
    result.value = await excelApi.apply({ mode: mode.value, rows: preview.value.rows })
    showToast('导入完成')
  } catch (e) {
    // 已提示
  } finally {
    applying.value = false
  }
}

function reset() {
  file.value = null
  preview.value = null
  result.value = null
  if (fileInput.value) fileInput.value.value = ''
}
</script>
