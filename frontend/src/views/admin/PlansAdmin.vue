<template>
  <div>
    <h1 class="page-title">拜访清单</h1>
    <p class="page-sub">给客户经理分配「集团 + 月份」的拜访计划；一条计划最多登记一条拜访记录</p>

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
        <MonthField v-model="month" allow-all />
        <div class="grow">
          <SelectField v-model="managerId" placeholder="全部客户经理" :options="managerOptions" @change="reload" />
        </div>
      </div>
      <div class="flex-between">
        <span class="mini">共 {{ total }} 条计划</span>
        <van-button size="small" type="primary" icon="plus" @click="openCreate">新增计划</van-button>
      </div>
    </div>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <div class="list-card">
        <PlanCard
          v-for="plan in list"
          :key="plan.id"
          :plan="plan"
          show-manager
          @click="openEdit(plan)"
        />
        <div v-if="!list.length && !loading" class="empty-box">当前筛选下没有计划</div>
        <van-list
          v-if="list.length"
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="loadPage"
        />
      </div>
    </van-pull-refresh>

    <van-popup v-model:show="formVisible" position="bottom" round>
      <div class="sheet">
        <h3 class="sheet-title">{{ form.id ? '编辑计划' : '新增计划' }}</h3>
        <p class="sheet-sub">{{ form.id ? '修改会同步影响该计划的拜访记录归属' : '客户经理 + 集团 + 月份 唯一' }}</p>

        <SelectField v-model="form.managerId" label="客户经理" :options="managerSelectOptions" placeholder="请选择客户经理" />
        <div class="mt12"></div>
        <SelectField v-model="form.groupId" label="集团" :options="groupOptions" placeholder="请选择集团" />
        <div class="mt12"></div>
        <div class="form-label">拜访月份</div>
        <MonthField v-model="form.planMonth" />
        <van-field v-model="form.remark" label="备注" placeholder="选填，如重点跟进事项" class="mt12" />

        <div class="form-actions">
          <van-button v-if="form.id" plain type="danger" @click="removePlan">删除</van-button>
          <van-button plain @click="formVisible = false">取消</van-button>
          <van-button type="primary" :loading="saving" @click="submit">保存</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import MonthField from '../../components/MonthField.vue'
import SelectField from '../../components/SelectField.vue'
import PlanCard from '../../components/PlanCard.vue'
import { groupApi, planApi, userApi } from '../../api'
import { currentMonth } from '../../utils/format'

const keyword = ref('')
const month = ref('')
const managerId = ref(null)
const managers = ref([])
const groups = ref([])

const list = ref([])
const page = ref(1)
const size = 15
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const formVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, managerId: null, groupId: null, planMonth: currentMonth(), remark: '' })

const managerOptions = computed(() => [
  { label: '全部客户经理', value: null },
  ...managers.value.map((item) => ({ label: item.realName, value: item.id }))
])
const managerSelectOptions = computed(() =>
  managers.value.map((item) => ({ label: `${item.realName}（${item.username}）`, value: item.id }))
)
const groupOptions = computed(() =>
  groups.value.map((item) => ({ label: `${item.name}（${item.code}）`, value: item.id }))
)

let timer = null
watch(keyword, () => {
  clearTimeout(timer)
  timer = setTimeout(reload, 400)
})
watch([month, managerId], reload)

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
}

function onRefresh() {
  refreshing.value = true
  reload()
}

async function loadMeta() {
  try {
    const [managerData, groupData] = await Promise.all([userApi.managers(), groupApi.list()])
    managers.value = managerData
    groups.value = groupData
  } catch (e) {
    // 已提示
  }
}

function openCreate() {
  Object.assign(form, {
    id: null,
    managerId: managers.value[0]?.id || null,
    groupId: null,
    planMonth: month.value || currentMonth(),
    remark: ''
  })
  formVisible.value = true
}

function openEdit(plan) {
  Object.assign(form, {
    id: plan.id,
    managerId: plan.managerId,
    groupId: plan.groupId,
    planMonth: plan.planMonth,
    remark: plan.remark || ''
  })
  formVisible.value = true
}

async function submit() {
  if (!form.managerId || !form.groupId || !form.planMonth) {
    showToast('请选择客户经理、集团和月份')
    return
  }
  saving.value = true
  try {
    const payload = {
      managerId: form.managerId,
      groupId: form.groupId,
      planMonth: form.planMonth,
      remark: form.remark || null
    }
    if (form.id) {
      await planApi.update(form.id, payload)
      showToast('已保存')
    } else {
      await planApi.create(payload)
      showToast('已新增')
    }
    formVisible.value = false
    reload()
  } catch (e) {
    // 已提示
  } finally {
    saving.value = false
  }
}

async function removePlan() {
  try {
    await showConfirmDialog({
      title: '删除计划',
      message: '删除后该计划的拜访记录也会一并删除，确定继续？',
      confirmButtonText: '删除',
      confirmButtonColor: '#dc2626'
    })
  } catch (e) {
    return
  }
  try {
    await planApi.remove(form.id)
    showToast('已删除')
    formVisible.value = false
    reload()
  } catch (e) {
    // 已提示
  }
}

onMounted(async () => {
  await loadMeta()
  reload()
})
</script>
