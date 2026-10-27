<template>
  <button type="button" class="month-btn" @click="open = true">
    <van-icon name="calendar-o" />
    <span>{{ displayText }}</span>
    <van-icon name="arrow-down" size="10" />
  </button>

  <van-popup v-model:show="open" position="bottom" round>
    <div class="picker-head">
      <div class="picker-title">选择月份</div>
      <button v-if="allowAll" class="picker-all" @click="chooseAll">全部月份</button>
    </div>
    <van-date-picker
      v-model="pickerValue"
      :title="''"
      :columns-type="['year', 'month']"
      :min-date="minDate"
      :max-date="maxDate"
      @confirm="confirm"
      @cancel="open = false"
    />
  </van-popup>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  allowAll: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const open = ref(false)
const now = new Date()
const minDate = new Date(now.getFullYear() - 3, 0, 1)
const maxDate = new Date(now.getFullYear() + 2, 11, 31)

const pickerValue = ref(buildValue(props.modelValue))

function buildValue(value) {
  const nowMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  const target = value || nowMonth
  const [year, month] = target.split('-')
  return [String(year), String(Number(month))]
}

watch(
  () => props.modelValue,
  (value) => {
    if (value) pickerValue.value = buildValue(value)
  }
)

const displayText = computed(() => {
  if (!props.modelValue) return props.allowAll ? '全部月份' : '选择月份'
  const [year, month] = props.modelValue.split('-')
  return `${year} 年 ${Number(month)} 月`
})

function confirm({ selectedValues }) {
  const [year, month] = selectedValues
  emit('update:modelValue', `${year}-${String(month).padStart(2, '0')}`)
  open.value = false
}

function chooseAll() {
  emit('update:modelValue', '')
  open.value = false
}
</script>

<style scoped>
.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 4px;
}
.picker-title {
  font-size: 15px;
  font-weight: 650;
}
.picker-all {
  border: 0;
  background: var(--brand-light);
  color: var(--brand-dark);
  font-size: 12.5px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 999px;
}
</style>
