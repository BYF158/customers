<template>
  <div>
    <h1 class="page-title">客户经理</h1>
    <p class="page-sub">账号、状态与拜访数据概览（删除会连带删除其计划与拜访记录）</p>

    <div class="flex-between" style="margin-bottom: 12px">
      <span class="mini">共 {{ list.length }} 位客户经理</span>
      <van-button size="small" type="primary" icon="plus" @click="openCreate">新增</van-button>
    </div>

    <div class="list-card">
      <div v-for="item in list" :key="item.id" class="row-item" style="cursor: default; align-items: flex-start">
        <div class="row-main">
          <div class="row-title">
            {{ item.realName }}
            <span class="pill" :class="item.enabled ? 'ok' : 'danger'">{{ item.enabled ? '启用' : '停用' }}</span>
          </div>
          <div class="row-sub">
            <span class="code">{{ item.username }}</span>
            <span>计划 {{ item.planCount }}</span>
            <span>拜访 {{ item.visitCount }}</span>
          </div>
        </div>
        <div class="flex" style="gap: 6px; flex-wrap: wrap; justify-content: flex-end">
          <van-button size="mini" plain @click="openEdit(item)">编辑</van-button>
          <van-button size="mini" plain @click="openPassword(item)">密码</van-button>
          <van-button size="mini" plain type="danger" @click="remove(item)">删除</van-button>
        </div>
      </div>
      <div v-if="!list.length && !loading" class="empty-box">还没有客户经理，点击右上角新增</div>
    </div>

    <van-popup v-model:show="formVisible" position="bottom" round>
      <div class="sheet">
        <h3 class="sheet-title">{{ isEdit ? '编辑客户经理' : '新增客户经理' }}</h3>
        <p class="sheet-sub">{{ isEdit ? '账号不可修改' : '新账号可用于登录手机端' }}</p>
        <van-field v-model="form.username" label="账号" placeholder="如 xiaowang" :disabled="isEdit" />
        <van-field v-model="form.realName" label="姓名" placeholder="如 小王" />
        <van-field v-if="!isEdit" v-model="form.password" type="password" label="初始密码" placeholder="至少 6 位" />
        <van-cell v-if="isEdit" center title="启用账号">
          <template #right-icon>
            <van-switch v-model="form.enabled" size="20" />
          </template>
        </van-cell>
        <div class="form-actions">
          <van-button plain block @click="formVisible = false">取消</van-button>
          <van-button type="primary" block :loading="saving" @click="submit">保存</van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="passwordVisible" position="bottom" round>
      <div class="sheet">
        <h3 class="sheet-title">重置密码</h3>
        <p class="sheet-sub">{{ passwordTarget?.realName }} · {{ passwordTarget?.username }}</p>
        <van-field v-model="newPassword" type="password" label="新密码" placeholder="至少 6 位" />
        <div class="form-actions">
          <van-button plain block @click="passwordVisible = false">取消</van-button>
          <van-button type="primary" block :loading="saving" @click="submitPassword">确定</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { userApi } from '../../api'

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const passwordVisible = ref(false)
const passwordTarget = ref(null)
const newPassword = ref('')

const form = reactive({ id: null, username: '', realName: '', password: '123456', enabled: true })

const isEdit = computed(() => !!form.id)

async function load() {
  loading.value = true
  try {
    list.value = await userApi.managers()
  } catch (e) {
    // 已提示
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, username: '', realName: '', password: '123456', enabled: true })
  formVisible.value = true
}

function openEdit(item) {
  Object.assign(form, {
    id: item.id,
    username: item.username,
    realName: item.realName,
    password: '',
    enabled: !!item.enabled
  })
  formVisible.value = true
}

async function submit() {
  if (!form.realName.trim()) {
    showToast('请输入姓名')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await userApi.update(form.id, { realName: form.realName.trim(), enabled: form.enabled })
      showToast('已保存')
    } else {
      if (!/^[a-zA-Z0-9_]{3,50}$/.test(form.username.trim())) {
        showToast('账号需为 3-50 位字母、数字或下划线')
        return
      }
      if ((form.password || '').length < 6) {
        showToast('初始密码至少 6 位')
        return
      }
      await userApi.create({
        username: form.username.trim(),
        realName: form.realName.trim(),
        password: form.password,
        role: 'MANAGER'
      })
      showToast('已新增')
    }
    formVisible.value = false
    load()
  } catch (e) {
    // 已提示
  } finally {
    saving.value = false
  }
}

function openPassword(item) {
  passwordTarget.value = item
  newPassword.value = ''
  passwordVisible.value = true
}

async function submitPassword() {
  if ((newPassword.value || '').length < 6) {
    showToast('新密码至少 6 位')
    return
  }
  saving.value = true
  try {
    await userApi.resetPassword(passwordTarget.value.id, { password: newPassword.value })
    showToast('密码已重置')
    passwordVisible.value = false
  } catch (e) {
    // 已提示
  } finally {
    saving.value = false
  }
}

async function remove(item) {
  try {
    await showConfirmDialog({
      title: '删除客户经理',
      message: `确定删除「${item.realName}」吗？\n将同时删除其 ${item.planCount} 条计划、${item.visitCount} 条拜访记录，且不可恢复。`,
      confirmButtonText: '删除',
      confirmButtonColor: '#dc2626'
    })
  } catch (e) {
    return
  }
  try {
    await userApi.remove(item.id)
    showToast('已删除')
    load()
  } catch (e) {
    // 已提示
  }
}

onMounted(load)
</script>
