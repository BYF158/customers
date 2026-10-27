<template>
  <div>
    <h1 class="page-title">数据统计</h1>
    <p class="page-sub">完成率、客户经理对比、月度趋势与集团覆盖</p>

    <div class="filter-row" style="margin-bottom: 14px">
      <MonthField v-model="month" allow-all />
      <div v-if="auth.isAdmin" class="grow">
        <SelectField v-model="managerId" placeholder="全部客户经理" :options="managerOptions" @change="load" />
      </div>
    </div>

    <template v-if="overview">
      <div class="stat-grid">
        <StatCard accent label="整体完成率" :value="`${overview.rate}%`" :desc="`${overview.done} / ${overview.total} 已拜访`" />
        <StatCard label="未拜访" :value="overview.todo" desc="尚未登记拜访" dot="#f59e0b" />
        <StatCard label="计划月内完成" :value="overview.onTime" desc="按期完成次数" dot="#16a34a" />
        <StatCard label="拜访记录" :value="overview.visitCount" desc="累计提交" dot="#8b5cf6" />
      </div>

      <div class="section-title">
        <span>客户经理对比</span>
        <span class="hint">按完成率排序</span>
      </div>
      <div class="card">
        <div v-for="item in managerRows" :key="item.managerId" class="bar-row">
          <div class="bar-label" style="width: 56px">{{ item.managerName }}</div>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: item.rate + '%' }"></div>
          </div>
          <div class="bar-num">{{ item.done }}/{{ item.total }}</div>
        </div>
        <div v-if="!managerRows.length" class="empty-box">暂无客户经理数据</div>
      </div>

      <div class="section-title">各月完成情况 <span class="hint">{{ year }} 年</span></div>
      <div class="card">
        <MonthBars :list="months" gradient="linear-gradient(90deg, #34d399, #10b981)" />
      </div>

      <div class="section-title">集团覆盖明细 <span class="hint">{{ groupRows.length }} 家</span></div>
      <div class="list-card">
        <div v-for="item in groupRows" :key="item.code" class="row-item" style="cursor: default">
          <div class="row-main">
            <div class="row-title">{{ item.name }}</div>
            <div class="row-sub">
              <span class="code">{{ item.code }}</span>
              <span v-if="item.managers">{{ item.managers }}</span>
            </div>
          </div>
          <span class="pill" :class="item.done >= item.total ? 'ok' : item.done ? 'info' : 'wait'">
            {{ item.done }}/{{ item.total }}
          </span>
        </div>
        <div v-if="!groupRows.length" class="empty-box">暂无集团数据</div>
      </div>

      <div class="section-title">导出</div>
      <div class="tile-grid">
        <button class="tile" @click="exportVisits">
          <span class="ti"><van-icon name="down" /></span>
          <span class="tt">拜访记录</span>
          <span class="ts">仅含已登记的拜访明细</span>
        </button>
        <button class="tile" @click="exportFull">
          <span class="ti"><van-icon name="orders-o" /></span>
          <span class="tt">完整清单</span>
          <span class="ts">含未拜访集团，可再导入</span>
        </button>
      </div>
    </template>

    <van-skeleton v-else title :row="6" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { showToast } from 'vant'
import MonthField from '../components/MonthField.vue'
import SelectField from '../components/SelectField.vue'
import StatCard from '../components/StatCard.vue'
import MonthBars from '../components/MonthBars.vue'
import { excelApi, statApi, userApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { currentMonth } from '../utils/format'

const auth = useAuthStore()
const month = ref(currentMonth())
const managerId = ref(null)
const managers = ref([])
const overview = ref(null)
const managerRows = ref([])
const months = ref([])
const groupRows = ref([])

const year = computed(() => (month.value ? month.value.slice(0, 4) : String(new Date().getFullYear())))

const managerOptions = computed(() => [
  { label: '全部客户经理', value: null },
  ...managers.value.map((item) => ({ label: item.realName, value: item.id }))
])

async function load() {
  try {
    const [overviewData, managerData, monthData, groupData] = await Promise.all([
      statApi.overview({ managerId: managerId.value || undefined, month: month.value || undefined }),
      statApi.byManager(month.value || undefined),
      statApi.byMonth({ managerId: managerId.value || undefined, year: year.value }),
      statApi.byGroup({ managerId: managerId.value || undefined, month: month.value || undefined })
    ])
    overview.value = overviewData
    managerRows.value = [...managerData].sort((a, b) => b.rate - a.rate || b.done - a.done)
    months.value = monthData
    groupRows.value = groupData
  } catch (e) {
    // 已提示
  }
}

async function loadManagers() {
  if (!auth.isAdmin) return
  try {
    managers.value = await userApi.managers()
  } catch (e) {
    managers.value = []
  }
}

async function exportVisits() {
  try {
    await excelApi.exportVisits({
      managerId: managerId.value || undefined,
      month: month.value || undefined
    })
    showToast('已开始下载')
  } catch (e) {
    showToast(e.message || '导出失败')
  }
}

async function exportFull() {
  try {
    await excelApi.exportFull({
      managerId: managerId.value || undefined,
      month: month.value || undefined
    })
    showToast('已开始下载')
  } catch (e) {
    showToast(e.message || '导出失败')
  }
}

watch(month, load)
onMounted(() => {
  loadManagers()
  load()
})
</script>
