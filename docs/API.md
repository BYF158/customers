# 接口文档

- Base URL：`/api`
- 鉴权：除 `POST /api/auth/login`、`GET /api/health` 外，均需请求头 `Authorization: Bearer <token>`
- 统一响应：

```json
{ "code": 0, "message": "ok", "data": { } }
```

| HTTP | code | 含义 |
|---|---|---|
| 200 | 0 | 成功 |
| 400 | 400 | 参数/业务校验失败（message 可直接提示用户） |
| 401 | 401 | 未登录或 token 失效 |
| 403 | 403 | 权限不足（如客户经理调用管理接口） |
| 404 | 404 | 资源不存在 |
| 409 | 409 | 冲突（重复账号/集团编码/同一计划重复登记等） |
| 500 | 500 | 服务器异常 |

分页响应结构：`{ "list": [...], "total": 123, "page": 1, "size": 20 }`

---

## 1. 认证

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/auth/login` | 公开 | 登录，返回 token + 用户信息 |
| GET | `/auth/me` | 登录 | 当前用户 |
| POST | `/auth/password` | 登录 | 修改自己的密码 |

**POST /auth/login**

```json
{ "username": "admin", "password": "admin123" }
```

```json
{ "code": 0, "message": "ok", "data": {
  "token": "eyJhbGciOi...",
  "user": { "id": 1, "username": "admin", "realName": "管理员", "role": "ADMIN", "enabled": true }
} }
```

**POST /auth/password**：`{ "oldPassword": "...", "newPassword": "至少 6 位" }`

---

## 2. 用户 / 客户经理（仅管理者）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/users/managers` | 客户经理列表（含计划数、拜访数） |
| GET | `/users/options` | 简版列表（下拉用） |
| POST | `/users` | 新增账号 `{username, realName, password, role}`，role 通常为 MANAGER |
| PUT | `/users/{id}` | 修改 `{realName, enabled}` |
| PUT | `/users/{id}/password` | 重置密码 `{password}` |
| DELETE | `/users/{id}` | 删除（级联删除其计划与记录；管理员账号不可删） |

---

## 3. 集团

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/groups?keyword=` | 登录 | 集团列表（含被计划引用数） |
| POST | `/groups` | 管理者 | 新增 `{code, name}` |
| PUT | `/groups/{id}` | 管理者 | 修改 `{code, name}` |
| DELETE | `/groups/{id}` | 管理者 | 删除（级联删除相关计划与记录） |

---

## 4. 拜访计划

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/plans` | 登录 | 分页查询 |
| GET | `/plans/months` | 登录 | 数据中出现过的月份（去重排序） |
| GET | `/plans/{id}` | 登录（本人/管理者） | 计划详情（含拜访信息） |
| POST | `/plans` | 管理者 | 新增计划 |
| PUT | `/plans/{id}` | 管理者 | 修改计划 |
| DELETE | `/plans/{id}` | 管理者 | 删除计划（级联删除记录） |

`GET /plans` 参数：

| 参数 | 说明 |
|---|---|
| `month` | `YYYY-MM`，可选 |
| `status` | `TODO` 未拜访 / `DONE` 已拜访，可选 |
| `keyword` | 集团名称或编码模糊匹配 |
| `managerId` | 仅管理者生效；客户经理恒为本人的数据 |
| `page` / `size` | 默认 1 / 20，size 最大 200 |

计划对象：

```json
{
  "id": 12, "managerId": 2, "managerName": "小李",
  "groupId": 1, "groupCode": "G001", "groupName": "李明科技有限公司",
  "planMonth": "2026-10", "remark": null,
  "visitId": 8, "visitDate": "2026-10-26", "method": "上门拜访",
  "result": "拜访顺利", "visitRemark": "确认年度供货计划",
  "visited": true
}
```

---

## 5. 拜访记录

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/visits` | 登录 | 分页查询（managerId/month/keyword） |
| GET | `/visits/options` | 登录 | 可选的拜访方式与结果 |
| POST | `/visits` | 登录（本人计划/管理者） | 登记，一条计划仅一条记录 |
| PUT | `/visits/{id}` | 本人/管理者 | 修改 |
| DELETE | `/visits/{id}` | 本人/管理者 | 撤销 |

**POST /visits**

```json
{
  "planId": 12,
  "visitDate": "2026-10-27",
  "method": "上门拜访",
  "result": "初步意向",
  "remark": "沟通内容、客户诉求、后续动作"
}
```

- `method` ∈ 上门拜访 / 电话沟通 / 视频会议 / 客户到访
- `result` ∈ 拜访顺利 / 初步意向 / 需再次跟进 / 暂无意向 / 未见到负责人
- 同一计划重复登记返回 409；拜访日期不能晚于明天

---

## 6. 统计

| 方法 | 路径 | 参数 | 返回 |
|---|---|---|---|
| GET | `/stats/home` | `month` 可选 | `{overview, months[12], todoPlans[≤5], managerCount}` |
| GET | `/stats/overview` | `month`, `managerId` | `{total, done, todo, rate, onTime, groupCount, visitCount}` |
| GET | `/stats/by-manager` | `month` | `[{managerId, managerName, total, done, rate}]` |
| GET | `/stats/by-month` | `year`, `managerId` | 12 条 `{month, total, done, rate}` |
| GET | `/stats/by-group` | `month`, `managerId` | `[{code, name, managers, total, done}]` |

`onTime`＝拜访日期不晚于计划月份最后一天的记录数；客户经理只能看到本人范围。

---

## 7. Excel

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/excel/template` | 管理者 | 下载导入模板（两个示例 Sheet + 说明） |
| POST | `/excel/import/preview` | 管理者 | `multipart/form-data`，字段名 `file`；返回预览 |
| POST | `/excel/import/apply` | 管理者 | `{mode: "MERGE"|"REPLACE", rows: [...]}` |
| GET | `/excel/export/visits` | 登录 | 拜访记录 xlsx（参数 managerId/month） |
| GET | `/excel/export/full` | 登录 | 完整清单 xlsx（参数 managerId/month） |

预览响应：

```json
{
  "rows": [{"managerName":"小李","code":"G001","name":"李明科技有限公司","planMonth":"2026-10"}],
  "managers": ["小李","小甘"],
  "months": ["2026-10","2026-11"],
  "warnings": ["集团 G001 已存在，名称保持为「李明科技有限公司」（表格中为「李明科技」）"],
  "ignoredSheets": 0
}
```

写入结果：

```json
{
  "added": 3, "skipped": 1,
  "createdManagers": [{"name":"小王","username":"小王","password":"123456"}],
  "warnings": [], "message": "导入完成（合并）：新增 3 条计划，跳过 1 条已存在记录"
}
```

导出接口返回二进制流，文件名在 `Content-Disposition`（同时给 `filename*` 以保证中文名）。

---

## 8. 健康检查

`GET /api/health` → `{"status":"UP","db":"UP"}`（供 Docker/负载均衡探活，无需鉴权）
