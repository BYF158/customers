<template>
  <div>
    <h1 class="page-title">拜访计划</h1>
    <p class="page-sub">按集团查看计划完成情况，登记或撤销拜访记录</p>

    <div class="filter-bar">
      <van-search
        v-model="keyword"
        placeholder="搜索集团名称 / 编码"
        shape="round"
        background="transparent"
        @search="reload"
        @clear="reload"
      />
      <div class="filter-row">
        <div class="chip-scroll grow">
          <button
            v-for="item in statusOptions"
            :key="item.value"
            class="chip"
            :class="{ on: status === item.value }"
            @click="changeStatus(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
      <div class="filter-row">
        <MonthField v-model="month" allow-all />
        <div v-if="auth.isAdmin" class="grow">
          <SelectField
            v-model="managerId"
            placeholder="全部客户经理"
            :options="managerOptions"
            @change="reload"
          />
        </div>
        <span class="mini" style="margin-left: auto">共 {{ total }} 条</span>
      </div>
    </div>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <div class="list-card">
        <PlanCard
          v-for="plan in list"
          :key="plan.id"
          :plan="plan"
          :show-manager="auth.isAdmin"
          @click="openDetail(plan)"
        />
        <div v-if="!list.length && !loading" class="empty-box">
          <van-icon name="notes-o" size="34" />
          <div class="mt8">当前筛选条件下没有拜访计划</div>
          <div v-if="auth.isAdmin" class="mini mt8">可以去「我的 → 拜访清单」新增，或用 Excel 批量导入</div>
        </div>
        <van-list
          v-if="list.length"
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="loadPage"
        />
      </div>
    </van-pull-refresh>

    <button v-if="todoCount" class="fab" @click="openQuick">
      <van-icon name="plus" />
      登记拜访
    </button>

    <PlanDetailSheet
      v-model:show="detailVisible"
      :plan="activePlan"
      @register="onRegister"
      @edit="onEdit"
      @revoke="onRevoke"
    />

    <VisitSheet
      v-model:show="visitVisible"
      :plan="visitPlan"
      :plans="quickPlans"
      :visit="editingVisit"
      @saved="onSaved"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import MonthField from '../components/MonthField.vue'
import SelectField from '../components/SelectField.vue'
import PlanCard from '../components/PlanCard.vue'
import PlanDetailSheet from '../components/PlanDetailSheet.vue'
import VisitSheet from '../components/VisitSheet.vue'
import { planApi, userApi, visitApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { currentMonth } from '../utils/format'

const auth = useAuthStore()

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '未拜访', value: 'TODO' },
  { label: '已拜访', value: 'DONE' }
]

const keyword = ref('')
const status = ref('ALL')
const month = ref(currentMonth())
const managerId = ref(null)
const managers = ref([])

const list = ref([])
const page = ref(1)
const size = 15
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const todoCount = ref(0)
const quickPlans = ref([])
const quickTodoCount = ref(0)

const detailVisible = ref(false)
const activePlan = ref(null)
const visitVisible = ref(false)
const visitPlan = ref(null)
const editingVisit = ref(null)

const managerOptions = computed(() => [
  { label: '全部客户经理', value: null },
  ...managers.value.map((item) => ({ label: item.realName, value: item.id }))
])

let searchTimer = null

watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => reload(), 400)
})

async function loadPage() {
  if (finished.value && page.value > 1) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const res = await planApi.search({
      managerId: managerId.value || undefined,
      month: month.value || undefined,
      status: status.value === 'ALL' ? undefined : status.value,
      keyword: keyword.value || undefined,
      page: page.value,
      size
    })
    list.value = page.value === 1 ? res.list : list.value.concat(res.list)
    total.value = res.total
    finished.value = list.value.length >= res.total || !res.list.length
    page.value += 1
  } catch (e) {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function reload() {
  page.value = 1
  finished.value = false
  list.value = []
  loadPage()
  refreshTodoCount()
}

async function refreshTodoCount() {
  try {
    const res = await planApi.search({
      managerId: managerId.value || undefined,
      month: month.value || undefined,
      status: 'TODO',
      page: 1,
      size: 1
    })
    todoCount.value = res.total
  } catch (e) {
    todoCount.value = 0
  }
}

function changeStatus(value) {
  status.value = value
  reload()
}

function onRefresh() {
  refreshing.value = true
  reload()
}

function openDetail(plan) {
  activePlan.value = plan
  detailVisible.value = true
}

function onRegister(plan) {
  detailVisible.value = false
  visitPlan.value = plan
  editingVisit.value = null
  visitVisible.value = true
}

function onEdit(plan) {
  detailVisible.value = false
  visitPlan.value = plan
  editingVisit.value = {
    id: plan.visitId,
    visitDate: plan.visitDate,
    method: plan.method,
    result: plan.result,
    remark: plan.visitRemark
  }
  visitVisible.value = true
}

async function onRevoke(plan) {
  try {
    await showConfirmDialog({
      title: '撤销拜访记录',
      message: `确定撤销「${plan.groupName}」的拜访记录吗？撤销后可重新登记。`
    })
  } catch (e) {
    return
  }
  try {
    await visitApi.remove(plan.visitId)
    showToast('已撤销')
    detailVisible.value = false
    reload()
  } catch (e) {
    // 已提示
  }
}

function onSaved() {
  detailVisible.value = false
  reload()
}

async function openQuick() {
  quickPlans.value = []
  try {
    const res = await planApi.search({
      managerId: managerId.value || undefined,
      month: month.value || undefined,
      status: 'TODO',
      page: 1,
      size: 200
    })
    quickPlans.value = res.list
    quickTodoCount.value = res.total
  } catch (e) {
    return
  }
  if (!quickPlans.value.length) {
    showToast('当前没有待拜访计划')
    return
  }
  visitPlan.value = null
  editingVisit.value = null
  visitVisible.value = true
}

async function loadManagers() {
  if (!auth.isAdmin) return
  try {
    managers.value = await userApi.managers()
  } catch (e) {
    managers.value = []
  }
}

watch([month, managerId], reload)

onMounted(() => {
  loadManagers()
  reload()
})
</script>
