<template>
  <van-popup :show="show" position="bottom" round @update:show="onShowChange">
    <div v-if="plan" class="sheet">
      <div class="flex-between">
        <h3 class="sheet-title">{{ plan.groupName }}</h3>
        <span class="pill" :class="plan.visited ? 'ok' : 'wait'">{{ plan.visited ? '已拜访' : '未拜访' }}</span>
      </div>
      <p class="sheet-sub">{{ plan.managerName }} · 计划 {{ plan.planMonth }}</p>

      <dl class="detail-list">
        <div class="detail-row">
          <dt>集团编码</dt>
          <dd><span class="code">{{ plan.groupCode }}</span></dd>
        </div>
        <div class="detail-row">
          <dt>计划月份</dt>
          <dd>{{ plan.planMonth }}</dd>
        </div>
        <div class="detail-row">
          <dt>实际拜访</dt>
          <dd>{{ plan.visited ? plan.visitDate : '—' }}</dd>
        </div>
        <div class="detail-row">
          <dt>拜访方式</dt>
          <dd>{{ plan.visited ? plan.method : '—' }}</dd>
        </div>
        <div class="detail-row">
          <dt>拜访结果</dt>
          <dd>{{ plan.visited ? plan.result : '—' }}</dd>
        </div>
      </dl>

      <div v-if="plan.visited && plan.visitRemark" class="mt12">
        <div class="form-label">拜访记录</div>
        <div class="remark-box">{{ plan.visitRemark }}</div>
      </div>

      <div class="form-actions">
        <template v-if="plan.visited">
          <van-button plain block @click="$emit('revoke', plan)">撤销记录</van-button>
          <van-button type="primary" block @click="$emit('edit', plan)">修改记录</van-button>
        </template>
        <van-button v-else type="primary" block @click="$emit('register', plan)">登记拜访</van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup>
const props = defineProps({
  show: { type: Boolean, default: false },
  plan: { type: Object, default: null }
})
const emit = defineEmits(['update:show', 'register', 'edit', 'revoke'])

function onShowChange(value) {
  emit('update:show', value)
}
</script>
