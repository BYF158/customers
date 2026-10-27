<template>
  <button type="button" class="plan-card" :class="{ done: plan.visited }" @click="$emit('click', plan)">
    <div class="vi-date">
      <b>{{ monthNumber }}</b>
      <span>月</span>
    </div>
    <div class="row-main">
      <div class="row-title">
        <span>{{ plan.groupName }}</span>
        <span v-if="showManager" class="pill info">{{ plan.managerName }}</span>
      </div>
      <div class="row-sub">
        <span class="code">{{ plan.groupCode }}</span>
        <span>{{ plan.planMonth }}</span>
      </div>
      <div v-if="plan.visited" class="row-desc">
        已拜访 {{ plan.visitDate }} · {{ plan.result }}
        <template v-if="plan.visitRemark"> · {{ shortRemark }}</template>
      </div>
      <div v-else class="row-desc" style="color: var(--text-3)">尚未登记拜访，点击登记</div>
    </div>
    <span class="pill" :class="plan.visited ? 'ok' : 'wait'">{{ plan.visited ? '已拜访' : '未拜访' }}</span>
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  plan: { type: Object, required: true },
  showManager: { type: Boolean, default: false }
})
defineEmits(['click'])

const monthNumber = computed(() => {
  const parts = String(props.plan.planMonth || '').split('-')
  return parts.length > 1 ? Number(parts[1]) : '—'
})

const shortRemark = computed(() => {
  const text = props.plan.visitRemark || ''
  return text.length > 18 ? `${text.slice(0, 18)}…` : text
})
</script>

<style scoped>
.plan-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  text-align: left;
  padding: 14px 16px;
  border: 0;
  border-bottom: 1px solid var(--line);
  background: #fff;
  cursor: pointer;
}
.plan-card:last-child {
  border-bottom: 0;
}
.plan-card:active {
  background: #f8fafd;
}
.vi-date {
  width: 42px;
  height: 46px;
  border-radius: 12px;
  background: var(--warn-light);
  color: #b45309;
  display: grid;
  place-content: center;
  text-align: center;
  flex: 0 0 auto;
}
.vi-date b {
  font-size: 17px;
  line-height: 1;
}
.vi-date span {
  font-size: 10.5px;
  opacity: 0.8;
}
.done .vi-date {
  background: var(--ok-light);
  color: var(--ok);
}
</style>
