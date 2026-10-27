<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="logo">访</div>
        <div>
          <h1>客户拜访管理</h1>
          <p>计划 · 登记 · 统计 一体化</p>
        </div>
      </div>

      <van-field
        v-model="form.username"
        label="账号"
        placeholder="请输入账号"
        clearable
        autocomplete="username"
        @keyup.enter="submit"
      />
      <van-field
        v-model="form.password"
        :type="showPassword ? 'text' : 'password'"
        label="密码"
        placeholder="请输入密码"
        autocomplete="current-password"
        @keyup.enter="submit"
      >
        <template #button>
          <van-icon :name="showPassword ? 'eye-o' : 'closed-eye'" @click="showPassword = !showPassword" />
        </template>
      </van-field>

      <button class="login-btn" :disabled="loading" @click="submit">
        {{ loading ? '登录中…' : '登 录' }}
      </button>

      <div class="login-hint">
        首次部署演示账号：管理者 <b>admin / admin123</b>；客户经理 <b>xiaoli / 123456</b>。
        正式使用前请在「我的 → 修改密码」中修改。
      </div>
    </div>
    <div class="login-foot">Vue 3 + Spring Boot + MySQL</div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const showPassword = ref(false)

async function submit() {
  if (!form.username.trim() || !form.password) {
    showToast('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await auth.login({ username: form.username.trim(), password: form.password })
    showToast('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (e) {
    // 请求层已提示
  } finally {
    loading.value = false
  }
}
</script>
