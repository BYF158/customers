<template>
  <div class="ring" :style="{ width: size + 'px', height: size + 'px' }">
    <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`">
      <circle :cx="c" :cy="c" :r="r" fill="none" stroke="#eef2f8" :stroke-width="stroke" />
      <circle
        :cx="c"
        :cy="c"
        :r="r"
        fill="none"
        :stroke="color"
        :stroke-width="stroke"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="offset"
        :style="{ transition: 'stroke-dashoffset .8s cubic-bezier(.2,.8,.2,1)' }"
      />
    </svg>
    <div class="ring-text">
      <b>{{ rate }}%</b>
      <span>{{ caption }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  rate: { type: Number, default: 0 },
  size: { type: Number, default: 96 },
  stroke: { type: Number, default: 9 },
  caption: { type: String, default: '完成率' },
  color: { type: String, default: '#2563eb' }
})

const c = computed(() => props.size / 2)
const r = computed(() => props.size / 2 - props.stroke - 1)
const circumference = computed(() => 2 * Math.PI * r.value)
const offset = computed(() => circumference.value * (1 - Math.min(props.rate, 100) / 100))
</script>

<style scoped>
.ring {
  position: relative;
  flex: 0 0 auto;
}
.ring svg {
  transform: rotate(-90deg);
}
.ring-text {
  position: absolute;
  inset: 0;
  display: grid;
  place-content: center;
  text-align: center;
}
</style>
