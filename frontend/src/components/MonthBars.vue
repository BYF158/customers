<template>
  <div>
    <div v-for="item in list" :key="item.month" class="bar-row">
      <div class="bar-label">{{ label(item.month) }}</div>
      <div class="bar-track">
        <div
          class="bar-fill"
          :style="{ width: item.rate + '%', background: gradient }"
        ></div>
      </div>
      <div class="bar-num">{{ item.done }}/{{ item.total }}</div>
    </div>
    <div v-if="!list.length" class="empty-box">暂无数据</div>
  </div>
</template>

<script setup>
const props = defineProps({
  list: { type: Array, default: () => [] },
  gradient: {
    type: String,
    default: 'linear-gradient(90deg, #60a5fa, #2563eb)'
  }
})

function label(month) {
  if (!month) return '—'
  const parts = String(month).split('-')
  return `${Number(parts[1])}月`
}
</script>
