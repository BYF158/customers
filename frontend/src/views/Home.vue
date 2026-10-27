<template>
  <div>
    <h1 class="page-title">客户拜访</h1>
    <p class="page-sub">按月份查看拜访进度，待拜访清单可直接登记</p>

    <div class="flex-between" style="margin-bottom: 12px">
      <div>
        <div style="font-size: 13px; color: var(--text-2)">
          {{ auth.isAdmin ? '全部客户经理' : `${auth.displayName} 的拜访进度` }}
        </div>
        <div class="mini">{{ monthLabel }}</div>
      </div>
      <MonthField v-model="month" allow-all />
    </div>

    <van-skeleton v-if="loading && !data" title :row="6" />

    <template v-else-if="data">
      <div class="card">
        <div class="ring-wrap">
          <ProgressRing :rate="data.overview.rate" :size="100" />
          <div class="grow">
            <div class="flex" style="gap: 18px">
              <div>
                <div style="font-size: 22px; font-weight: 750">{{ data.overview.done }}</div>
                <div class="mini">已拜访</div>
              </div>
              <div>
                <div style="font-size: 22px; font-weight: 750">{{ data.overview.todo }}</div>
                <div class="mini">未拜访</div>
              </div>
              <div>
                <div style="font-size: 22px; font-weight: 750">{{ data.overview.groupCount }}</div>
                <div class="mini">集团数</div>
              </div>
            </div>
            <div class="mini mt8">
              共 {{ data.overview.total }} 条计划 · 计划月内完成 {{ data.overview.onTime }} 次
            </div>
          </div>
        </div>
      </div>

      <div class="stat-grid mt12">
        <StatCard label="计划总数" :value="data.overview.total" desc="当前筛选范围" dot="#2563eb" />
        <StatCard label="拜访记录" :value="data.overview.visitCount" desc="累计提交" dot="#16a34a" />
        <StatCard
          label="待拜访"
          :value="data.overview.todo"
          desc="需要尽快安排"
          dot="#f59e0b"
        />
        <StatCard
          :label="auth.isAdmin ? '客户经理' : '我的集团'"
          :value="auth.isAdmin ? data.managerCount : data.overview.groupCount"
          :desc="auth.isAdmin ? '在岗人数' : '负责集团数'"
          dot="#8b5cf6"
        />
      </div>

      <div class="section-title">
        <span>本月待拜访</span>
        <span class="hint">点击直接登记</span>
      </div>
      <div class="list-card">
        <PlanCard
          v-for="plan in data.todoPlans"
          :key="plan.id"
          :plan="plan"
          :show-manager="auth.isAdmin"
          @click="quickRegister(plan)"
        />
        <div v-if="!data.todoPlans.length" class="empty-box">
          当前范围没有待拜访计划，去「拜访」页看看已完成的记录吧
        </div>
      </div>

      <div class="section-title">各月完成情况</div>
      <div class="card">
        <MonthBars :list="data.months" />
      </div>

      <button class="fab" @click="$router.push('/plans')">
        <van-icon name="plus" />
        去登记拜访
      </button>
    </template>

    <VisitSheet v-model:show="visitVisible" :plan="activePlan" @saved="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { showToast } from 'vant'
import MonthField from '../components/MonthField.vue'
import ProgressRing from '../components/ProgressRing.vue'
import StatCard from '../components/StatCard.vue'
import PlanCard from '../components/PlanCard.vue'
import MonthBars from '../components/MonthBars.vue'
import VisitSheet from '../components/VisitSheet.vue'
import { statApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { currentMonth, formatMonth } from '../utils/format'

const auth = useAuthStore()
const month = ref(currentMonth())
const data = ref(null)
const loading = ref(false)
const visitVisible = ref(false)
const activePlan = ref(null)

const monthLabel = computed(() => (month.value ? formatMonth(month.value) : '全部月份'))

async function load() {
  loading.value = true
  try {
    data.value = await statApi.home(month.value)
  } catch (e) {
    // 已提示
  } finally {
    loading.value = false
  }
}

function quickRegister(plan) {
  activePlan.value = plan
  visitVisible.value = true
}

watch(month, load)
onMounted(load)
</script>
