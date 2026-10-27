<template>
  <van-popup :show="show" position="bottom" round :close-on-click-overlay="false" @update:show="onShowChange">
    <div class="sheet">
      <h3 class="sheet-title">{{ visit ? '修改拜访记录' : '登记拜访' }}</h3>
      <p class="sheet-sub">
        <template v-if="selectedPlan">
          {{ selectedPlan.groupName }} · {{ selectedPlan.groupCode }} · 计划 {{ selectedPlan.planMonth }}
        </template>
        <template v-else>请选择要登记的集团</template>
      </p>

      <SelectField
        v-if="!plan"
        v-model="form.planId"
        label="选择集团"
        placeholder="请选择要登记的集团"
        :options="planOptions"
      />

      <div class="field-block">
        <div class="form-label">拜访日期 <em>*</em></div>
        <input v-model="form.visitDate" class="native-input" type="date" :max="maxDate" />
        <div class="form-tip">默认今天，如补录可改为实际拜访日期</div>
      </div>

      <SelectField v-model="form.method" label="拜访方式" :options="methodOptions" />

      <div class="mt12"></div>
      <SelectField v-model="form.result" label="拜访结果" :options="resultOptions" />

      <div class="field-block mt12">
        <div class="form-label">拜访记录</div>
        <textarea
          v-model="form.remark"
          class="native-textarea"
          maxlength="1000"
          placeholder="沟通内容、客户诉求、后续动作…"
        ></textarea>
        <div class="form-tip" style="text-align: right">{{ form.remark.length }}/1000</div>
      </div>

      <div class="form-actions">
        <van-button plain block @click="close">取消</van-button>
        <van-button type="primary" block :loading="saving" @click="submit">
          {{ visit ? '保存修改' : '提交拜访记录' }}
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { showToast } from 'vant'
import SelectField from './SelectField.vue'
import { visitApi } from '../api'

const props = defineProps({
  show: { type: Boolean, default: false },
  plan: { type: Object, default: null },
  plans: { type: Array, default: () => [] },
  visit: { type: Object, default: null }
})
const emit = defineEmits(['update:show', 'saved'])

const fallbackMethods = ['上门拜访', '电话沟通', '视频会议', '客户到访']
const fallbackResults = ['拜访顺利', '初步意向', '需再次跟进', '暂无意向', '未见到负责人']

const methods = ref(fallbackMethods)
const results = ref(fallbackResults)
const saving = ref(false)

const form = reactive({
  planId: null,
  visitDate: today(),
  method: fallbackMethods[0],
  result: fallbackResults[0],
  remark: ''
})

function today() {
  const date = new Date()
  const offset = date.getTimezoneOffset()
  return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 10)
}

const maxDate = today()

const methodOptions = computed(() => methods.value.map((item) => ({ label: item, value: item })))
const resultOptions = computed(() => results.value.map((item) => ({ label: item, value: item })))
const planOptions = computed(() =>
  props.plans.map((item) => ({
    label: `${item.groupName}（${item.groupCode}） · ${item.planMonth}`,
    value: item.id
  }))
)

const selectedPlan = computed(() => {
  if (props.plan) return props.plan
  return props.plans.find((item) => item.id === form.planId) || null
})

watch(
  () => props.show,
  async (visible) => {
    if (!visible) return
    form.planId = props.plan?.id || (props.plans.length === 1 ? props.plans[0].id : null)
    form.visitDate = props.visit?.visitDate || today()
    form.method = props.visit?.method || methods.value[0]
    form.result = props.visit?.result || results.value[0]
    form.remark = props.visit?.remark || ''
    try {
      const options = await visitApi.options()
      if (options?.methods?.length) methods.value = options.methods
      if (options?.results?.length) results.value = options.results
    } catch (e) {
      // 使用本地默认选项
    }
  }
)

function onShowChange(value) {
  emit('update:show', value)
}

function close() {
  emit('update:show', false)
}

async function submit() {
  if (!form.planId) {
    showToast('请选择要登记的集团')
    return
  }
  if (!form.visitDate) {
    showToast('请选择拜访日期')
    return
  }
  saving.value = true
  try {
    const payload = {
      planId: form.planId,
      visitDate: form.visitDate,
      method: form.method,
      result: form.result,
      remark: form.remark
    }
    if (props.visit) {
      await visitApi.update(props.visit.id, payload)
    } else {
      await visitApi.create(payload)
    }
    showToast(props.visit ? '拜访记录已更新' : '拜访记录已保存')
    emit('saved')
    close()
  } catch (e) {
    // 错误提示已在请求层统一处理
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.field-block {
  margin-top: 12px;
}
.form-label em {
  color: var(--danger);
  font-style: normal;
}
.field-block .native-input {
  margin-top: 0;
}
</style>
