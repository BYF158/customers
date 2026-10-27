<template>
  <div>
    <h1 class="page-title">我的</h1>
    <p class="page-sub">账号、数据管理与导出</p>

    <div class="card">
      <div class="flex" style="gap: 14px">
        <div
          style="
            width: 54px;
            height: 54px;
            border-radius: 17px;
            background: linear-gradient(135deg, #2563eb, #1e40af);
            color: #fff;
            display: grid;
            place-items: center;
            font-size: 21px;
            font-weight: 700;
            flex: 0 0 auto;
          "
        >
          {{ auth.displayName.slice(0, 1) }}
        </div>
        <div class="grow">
          <div style="font-size: 17px; font-weight: 700">{{ auth.displayName }}</div>
          <div class="mini">{{ auth.user?.username }} · {{ auth.roleText }}</div>
        </div>
        <van-button size="small" plain @click="logout">退出</van-button>
      </div>
    </div>

    <div class="section-title">数据概览</div>
    <div class="card">
      <div class="detail-list">
        <div class="detail-row">
          <dt>计划总数</dt>
          <dd>{{ overview.total }}</dd>
        </div>
        <div class="detail-row">
          <dt>已拜访 / 未拜访</dt>
          <dd>{{ overview.done }} / {{ overview.todo }}</dd>
        </div>
        <div class="detail-row">
          <dt>拜访记录</dt>
          <dd>{{ overview.visitCount }} 条</dd>
        </div>
        <div class="detail-row">
          <dt>集团数量</dt>
          <dd>{{ groupCount }} 家</dd>
        </div>
        <div v-if="auth.isAdmin" class="detail-row">
          <dt>客户经理</dt>
          <dd>{{ managerCount }} 人</dd>
        </div>
      </div>
    </div>

    <template v-if="auth.isAdmin">
      <div class="section-title">数据管理 <span class="hint">仅管理者</span></div>
      <div class="tile-grid">
        <button class="tile" @click="$router.push('/admin/managers')">
          <span class="ti"><van-icon name="friends-o" /></span>
          <span class="tt">客户经理</span>
          <span class="ts">新增账号、重置密码、停用</span>
        </button>
        <button class="tile" @click="$router.push('/admin/groups')">
          <span class="ti"><van-icon name="shop-o" /></span>
          <span class="tt">集团管理</span>
          <span class="ts">维护集团编码与名称</span>
        </button>
        <button class="tile" @click="$router.push('/admin/plans')">
          <span class="ti"><van-icon name="orders-o" /></span>
          <span class="tt">拜访清单</span>
          <span class="ts">分配经理、集团与月份</span>
        </button>
        <button class="tile" @click="$router.push('/admin/import')">
          <span class="ti"><van-icon name="upgrade" /></span>
          <span class="tt">数据导入</span>
          <span class="ts">Excel 批量导入计划</span>
        </button>
      </div>
    </template>

    <div class="section-title">数据导出</div>
    <div class="tile-grid">
      <button class="tile" @click="exportVisits">
        <span class="ti"><van-icon name="down" /></span>
        <span class="tt">拜访记录</span>
        <span class="ts">{{ auth.isAdmin ? '全部/按经理导出' : '仅导出本人记录' }}</span>
      </button>
      <button class="tile" @click="exportFull">
        <span class="ti"><van-icon name="notes-o" /></span>
        <span class="tt">完整清单</span>
        <span class="ts">含未拜访集团，可再导入</span>
      </button>
    </div>

    <div class="section-title">账号安全</div>
    <div class="list-card">
      <button class="row-item" @click="passwordVisible = true">
        <div class="row-main">
          <div class="row-title">修改密码</div>
          <div class="row-sub">建议定期更换，至少 6 位</div>
        </div>
        <van-icon class="row-action" name="arrow" />
      </button>
      <button class="row-item" @click="logout">
        <div class="row-main">
          <div class="row-title" style="color: var(--danger)">退出登录</div>
          <div class="row-sub">退出后需要重新输入账号密码</div>
        </div>
        <van-icon class="row-action" name="arrow" />
      </button>
    </div>

    <div class="mini center mt20" style="padding-bottom: 12px">
      客户拜访管理 v1.0 · Vue 3 + Spring Boot + MySQL
    </div>

    <van-popup v-model:show="passwordVisible" position="bottom" round>
      <div class="sheet">
        <h3 class="sheet-title">修改密码</h3>
        <p class="sheet-sub">修改成功后当前登录仍然有效</p>
        <van-field v-model="password.oldPassword" type="password" label="当前密码" placeholder="请输入当前密码" />
        <van-field v-model="password.newPassword" type="password" label="新密码" placeholder="至少 6 位" />
        <van-field v-model="password.confirm" type="password" label="确认新密码" placeholder="再次输入新密码" />
        <div class="form-actions">
          <van-button plain block @click="passwordVisible = false">取消</van-button>
          <van-button type="primary" block :loading="saving" @click="submitPassword">保存</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { authApi, excelApi, groupApi, statApi, userApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const overview = ref({ total: 0, done: 0, todo: 0, visitCount: 0 })
const groupCount = ref(0)
const managerCount = ref(0)

const passwordVisible = ref(false)
const saving = ref(false)
const password = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function load() {
  try {
    const [overviewData, groups, managers] = await Promise.all([
      statApi.overview({}),
      groupApi.list(),
      auth.isAdmin ? userApi.managers() : Promise.resolve([])
    ])
    overview.value = overviewData
    groupCount.value = groups.length
    managerCount.value = managers.length
  } catch (e) {
    // 已提示
  }
}

async function exportVisits() {
  try {
    await excelApi.exportVisits({})
    showToast('已开始下载')
  } catch (e) {
    showToast(e.message || '导出失败')
  }
}

async function exportFull() {
  try {
    await excelApi.exportFull({})
    showToast('已开始下载')
  } catch (e) {
    showToast(e.message || '导出失败')
  }
}

async function submitPassword() {
  if (!password.oldPassword || !password.newPassword) {
    showToast('请填写完整')
    return
  }
  if (password.newPassword.length < 6) {
    showToast('新密码至少 6 位')
    return
  }
  if (password.newPassword !== password.confirm) {
    showToast('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await authApi.changePassword({
      oldPassword: password.oldPassword,
      newPassword: password.newPassword
    })
    showToast('密码已修改')
    passwordVisible.value = false
    password.oldPassword = ''
    password.newPassword = ''
    password.confirm = ''
  } catch (e) {
    // 已提示
  } finally {
    saving.value = false
  }
}

async function logout() {
  try {
    await showConfirmDialog({ title: '退出登录', message: '确定退出当前账号吗？' })
  } catch (e) {
    return
  }
  auth.logout()
  router.replace('/login')
}

onMounted(load)
</script>
