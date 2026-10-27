<template>
  <div>
    <div v-if="label" class="form-label">{{ label }}</div>
    <button type="button" class="select-like" :class="{ placeholder: !displayText }" @click="open = true">
      <span>{{ displayText || placeholder }}</span>
      <van-icon name="arrow-down" size="12" />
    </button>

    <van-popup v-model:show="open" position="bottom" round>
      <van-picker
        :title="title || label"
        :columns="pickerColumns"
        @confirm="confirm"
        @cancel="open = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: { type: [String, Number, null], default: null },
  options: { type: Array, default: () => [] },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '请选择' },
  title: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'change'])

const open = ref(false)

const pickerColumns = computed(() =>
  props.options.map((item) => ({ text: item.label, value: item.value }))
)

const displayText = computed(() => {
  const found = props.options.find((item) => item.value === props.modelValue)
  return found ? found.label : ''
})

function confirm({ selectedOptions }) {
  const selected = selectedOptions?.[0]
  if (selected) {
    emit('update:modelValue', selected.value)
    emit('change', selected.value)
  }
  open.value = false
}
</script>

<style scoped>
.select-like {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 13px;
  border: 1px solid var(--line);
  background: #f8fafd;
  border-radius: 12px;
  font-size: 15px;
  color: var(--text);
  text-align: left;
}
.select-like.placeholder {
  color: var(--text-3);
}
</style>
