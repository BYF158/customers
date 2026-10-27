<template>
  <div>
    <h1 class="page-title">集团管理</h1>
    <p class="page-sub">集团编码唯一，所有客户经理的拜访计划都引用同一份集团主数据</p>

    <div class="flex" style="margin-bottom: 12px; gap: 10px">
      <van-search
        v-model="keyword"
        placeholder="搜索编码 / 名称"
        shape="round"
        background="transparent"
        style="flex: 1; padding-left: 0"
        @search="load"
        @clear="load"
      />
      <van-button size="small" type="primary" icon="plus" @click="openCreate">新增</van-button>
    </div>

    <div class="list-card">
      <div v-for="item in list" :key="item.id" class="row-item" style="cursor: default">
        <div class="row-main">
          <div class="row-title">{{ item.name }}</div>
          <div class="row-sub">
            <span class="code">{{ item.code }}</span>
            <span>被 {{ item.planCount }} 条计划引用</span>
          </div>
        </div>
        <div class="flex" style="gap: 6px">
          <van-button size="mini" plain @click="openEdit(item)">编辑</van-button>
          <van-button size="mini" plain type="danger" @click="remove(item)">删除</van-button>
        </div>
      </div>
      <div v-if="!list.length" class="empty-box">没有找到集团</div>
    </div>

    <van-popup v-model:show="formVisible" position="bottom" round>
      <div class="sheet">
        <h3 class="sheet-title">{{ form.id ? '编辑集团' : '新增集团' }}</h3>
        <p class="sheet-sub">修改编码会影响已有计划的展示，但不会丢失记录</p>
        <van-field v-model="form.code" label="集团编码" placeholder="如 G011" />
        <van-field v-model="form.name" label="集团名称" placeholder="如 某某集团有限公司" />
        <div class="form-actions">
          <van-button plain block @click="formVisible = false">取消</van-button>
          <van-button type="primary" block :loading="saving" @click="submit">保存</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { groupApi } from '../../api'

const keyword = ref('')
const list = ref([])
const formVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, code: '', name: '' })
let timer = null

watch(keyword, () => {
  clearTimeout(timer)
  timer = setTimeout(load, 400)
})

async function load() {
  try {
    list.value = await groupApi.list(keyword.value || undefined)
  } catch (e) {
    // 已提示
  }
}

function openCreate() {
  Object.assign(form, { id: null, code: '', name: '' })
  formVisible.value = true
}

function openEdit(item) {
  Object.assign(form, { id: item.id, code: item.code, name: item.name })
  formVisible.value = true
}

async function submit() {
  if (!form.code.trim() || !form.name.trim()) {
    showToast('请填写编码和名称')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await groupApi.update(form.id, { code: form.code.trim(), name: form.name.trim() })
    } else {
      await groupApi.create({ code: form.code.trim(), name: form.name.trim() })
    }
    showToast('已保存')
    formVisible.value = false
    load()
  } catch (e) {
    // 已提示
  } finally {
    saving.value = false
  }
}

async function remove(item) {
  try {
    await showConfirmDialog({
      title: '删除集团',
      message: `确定删除「${item.name}」吗？\n其关联的 ${item.planCount} 条拜访计划及记录会一并删除。`,
      confirmButtonText: '删除',
      confirmButtonColor: '#dc2626'
    })
  } catch (e) {
    return
  }
  try {
    await groupApi.remove(item.id)
    showToast('已删除')
    load()
  } catch (e) {
    // 已提示
  }
}

onMounted(load)
</script>
