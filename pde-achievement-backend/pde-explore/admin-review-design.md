# 关卡审核功能技术方案

生成时间：2026-04-26
分支：master

---

## 一、功能概述

在页面右上角（姓名左侧）新增「审核」入口按钮，仅对有权限的管理员可见。点击后跳转至 `#/admin/review` 页面，展示所有待审核的关卡提交记录，管理员可操作「通过」或「不通过」。

权限来源：后端通过 Lion 配置管理员 mis 名单，前端通过独立的 `/admin/me` 接口查询当前用户是否有管理员权限，与 `/user/me` 解耦。

---

## 二、涉及页面 & 组件

| 改动点 | 文件 | 说明 |
|--------|------|------|
| 导航栏审核按钮 | `src/pages/StageDetail.jsx` | 右上角姓名左侧，isAdmin 时显示 |
| 首页导航栏审核按钮 | `src/pages/Index.jsx` | 同上，保持一致 |
| 审核页面（新建） | `src/pages/AdminReview.jsx` | 审核列表 + 操作 |
| 路由注册 | `src/nav-items.jsx` + `src/App.jsx` | 新增 `/admin/review` 路由 |

---

## 三、后端接口要求

### 3.1 用户信息接口（已有，不变）

**GET** `/ulivepde/api/user/me`

不做任何改动，不新增 `isAdmin` 字段，保持与业务逻辑解耦。

---

### 3.2 管理员权限查询接口（新增，独立）

**GET** `/ulivepde/api/admin/me`

前端页面初始化时调用，判断当前登录用户是否有管理员权限。与 `/user/me` 完全独立，后端通过 Lion 配置的 mis 名单判断。

**无需请求体**（依赖 Cookie/Session 中的登录态）

**期望返回（有权限）：**
```json
{
  "isAdmin": true
}
```

**期望返回（无权限）：**
```json
{
  "isAdmin": false
}
```

> 注意：此接口**不返回 403**，任何登录用户都可以调用，只是返回值不同。403 只用于实际的管理操作接口（3.4、3.5）。前端根据 `isAdmin` 决定是否显示审核入口按钮。

---

### 3.3 提交审核接口（已有，需确认字段）

**POST** `/ulivepde/api/progress/verify`

用户在 manual 类型关卡填写泳道名和页面地址后提交。前端当前将两个字段拼接为一个字符串发送，**建议后端改为结构化字段**，便于审核页面独立展示。

**当前请求体：**
```json
{
  "stageId": 5,
  "value": "泳道名：yanligame\n页面访问地址：https://xxx.test.sankuai.com"
}
```

**期望改为（前端配合同步修改）：**
```json
{
  "stageId": 5,
  "value": "泳道名：yanligame\n页面访问地址：https://xxx.test.sankuai.com",
  "lane": "yanligame",
  "url": "https://xxx.test.sankuai.com"
}
```

> 保留 `value` 字段向后兼容，新增 `lane` 和 `url` 供后端存储和审核页面使用。

**返回（已有）：**
```json
{
  "verifyStatus": "pending",
  "message": "已提交，等待审核"
}
```

---

### 3.4 获取待审核列表（新增）

**GET** `/ulivepde/api/admin/reviews`

获取所有待审核（及历史已审核）的提交记录，仅管理员可访问。

**Query 参数：**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `status` | string | `pending` | `pending` / `passed` / `failed` / `all` |
| `page` | number | `1` | 分页页码 |
| `pageSize` | number | `20` | 每页条数 |

**期望返回：**
```json
{
  "total": 42,
  "page": 1,
  "pageSize": 20,
  "list": [
    {
      "id": 101,
      "mis": "zhangsan",
      "name": "张三",
      "org": "平台产品部",
      "stageId": 5,
      "stageName": "第5关",
      "stageTitle": "前端上线",
      "lane": "yanligame",
      "url": "https://yanligame.test.sankuai.com",
      "submittedAt": "2026-04-25T14:30:00Z",
      "status": "pending",
      "reviewedAt": null,
      "reviewedBy": null,
      "rejectReason": null
    }
  ]
}
```

**权限：** 非管理员请求返回 `403`。

---

### 3.5 执行审核操作（新增）

**POST** `/ulivepde/api/admin/reviews/:id/review`

管理员对某条提交执行通过或不通过操作。

**路径参数：**
- `id`：提交记录 ID（来自 3.3 接口的 `list[].id`）

**请求体：**
```json
{
  "action": "pass",
  "rejectReason": ""
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `pass`（通过）或 `reject`（不通过）|
| `rejectReason` | string | 否 | `action=reject` 时建议填写，反馈给用户 |

**期望返回：**
```json
{
  "success": true,
  "id": 101,
  "action": "pass"
}
```

**副作用（后端处理）：**
- `action=pass`：将该用户该关卡的 `verifyStatus` 更新为 `passed`，触发解锁下一关逻辑
- `action=reject`：将 `verifyStatus` 更新为 `failed`，`rejectReason` 写入记录，用户重新进入该关卡时可看到原因

**权限：** 非管理员请求返回 `403`。

---

## 四、前端页面设计

### 4.1 审核按钮（导航栏）

- 位置：右上角，姓名文字左侧
- 显示条件：`isAdmin === true`（来自独立的 `/admin/me` 接口，与用户信息分开存储）
- 样式：与现有导航栏风格一致（小胶囊按钮）
- 点击行为：`navigate('/admin/review')`（新 Hash 路由，当前页跳转）

### 4.2 审核列表页 `/admin/review`

**布局：**
- 顶部：返回按钮 + 页面标题「关卡审核」+ 状态筛选 Tab（待审核 / 已通过 / 已拒绝）
- 主体：卡片列表，每条记录一张卡片
- 底部：分页（如记录较多）

**每条记录展示：**

```
┌─────────────────────────────────────────────────┐
│  zhangsan · 张三 · 平台产品部          2026-04-25 │
│  第5关 · 前端上线                                 │
│                                                   │
│  泳道名：yanligame                                │
│  访问地址：https://yanligame.test.sankuai.com  ↗  │
│                                                   │
│  [通过]  [不通过]                                 │
└─────────────────────────────────────────────────┘
```

- 页面地址可点击，在新标签页打开
- 点「不通过」弹出输入框填写原因，确认后提交
- 操作成功后该条记录从「待审核」列表消失，切换到对应 Tab 可查看历史

### 4.3 权限守卫

- 进入 `/admin/review` 时检查 `isAdmin`（来自 `/admin/me`），非管理员重定向回首页 `/`
- 审核按钮本身也靠 `isAdmin` 控制，双重保障（即使直接访问 URL，后端接口也会返回 403）

---

## 五、数据库变更建议（供后端参考）

现有 `user_progress` 表需确认或新增以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `lane` | VARCHAR(100) | 用户填写的泳道名 |
| `url` | VARCHAR(500) | 用户填写的页面访问地址 |
| `submitted_at` | DATETIME | 提交时间 |
| `reviewed_at` | DATETIME | 审核时间 |
| `reviewed_by` | VARCHAR(50) | 审核人 mis |
| `reject_reason` | TEXT | 拒绝原因 |

---

## 六、开发顺序建议

1. **后端先行**：新增 `/admin/me`、3.4 和 3.5 接口，`/user/me` 不改动
2. **前端同步**：
   - `AdminReview.jsx` 页面骨架（可先用 mock 数据联调）
   - 注册路由
   - 导航栏加按钮（依赖 `isAdmin` 字段）
   - 提交接口同步新增 `lane`、`url` 字段
3. **联调验证**：管理员账号走完完整审核流程

---

## 七、未决问题

- [ ] 审核不通过后，用户在关卡页看到的提示文案是什么？（`rejectReason` 怎么展示给用户）
- [ ] 是否需要审核通知？（站内消息 / 飞书机器人推送）
- [ ] 分页还是无限滚动？（待审核记录量预估）
