<template>
  <div class="stat" :class="{ accent }">
    <div class="k">
      <span v-if="dot" class="dot" :style="{ background: dot }"></span>
      {{ label }}
    </div>
    <div class="v">{{ value }}</div>
    <div class="d">{{ desc }}</div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  value: { type: [String, Number], default: 0 },
  desc: { type: String, default: '' },
  accent: { type: Boolean, default: false },
  dot: { type: String, default: '' }
})
</script>
