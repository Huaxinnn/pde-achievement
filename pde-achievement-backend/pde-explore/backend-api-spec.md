# PDE闯关系统 — 后端接口与数据库设计文档

## 概述

本文档描述 PDE闯关系统前端所需的全部后端接口和数据库表结构，供后端开发参考实现。

**技术约定**
- 所有接口路径前缀：`/api`
- 请求/响应格式：`application/json`
- 用户身份：通过 `mis` 字段标识（美团内部员工号）。开发阶段前端传 `?mock_mis=<mis>`，生产环境对接 SSO 后从 token 中取
- 错误响应统一格式：`{ "error": "错误描述" }`，HTTP 状态码 4xx/5xx

---

## 一、数据库表设计

### 1. `stages` — 关卡配置表

存储每个关卡的静态内容，由运营后台管理，不频繁变更。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, PK | 关卡编号，从 1 开始，无上限 |
| `name` | VARCHAR(32) | 关卡短名，如"第1关" |
| `title` | VARCHAR(64) | 关卡标题，如"武器初始化" |
| `description` | TEXT | 关卡描述 |
| `verify_type` | ENUM('self','url','paste','branch','manual') | 通关验证方式（见下方说明） |
| `verify_hint` | TEXT | 验证输入框的提示文字 |
| `sort_order` | INT | 排序权重 |
| `is_active` | TINYINT(1) | 是否上线 |

**verify_type 说明：**
- `self`：用户自行勾选所有步骤即通关，无需后端验证
- `url`：用户提交一个 URL，后端可选择性校验可访问性
- `paste`：用户粘贴命令输出内容，人工或自动审核
- `branch`：用户提交代码分支链接，人工审核
- `manual`：用户填写说明文字，人工审核

---

### 2. `stage_steps` — 关卡步骤表

每个关卡包含若干操作步骤，步骤内容由运营管理。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, PK, AUTO_INCREMENT | 主键 |
| `stage_id` | INT, FK → stages.id | 所属关卡 |
| `sort_order` | INT | 步骤顺序（前端按此排序展示） |
| `title` | VARCHAR(128) | 步骤标题 |
| `description` | TEXT | 步骤说明 |
| `commands` | JSON | 命令行列表，如 `["node --version", "npm --version"]` |
| `tips` | TEXT | 提示文字 |

---

### 3. `stage_faqs` — 关卡常见问题表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INT, PK, AUTO_INCREMENT | 主键 |
| `stage_id` | INT, FK → stages.id | 所属关卡 |
| `question` | TEXT | 问题 |
| `answer` | TEXT | 答案 |
| `sort_order` | INT | 排序 |

---

### 4. `user_stage_progress` — 用户关卡通关记录表

只记录**已通关**的关卡，未通关不写行。当前进度通过计算得出，不存冗余状态。

| 字段 | 类型 | 说明 |
|------|------|------|
| `user_mis` | VARCHAR(64), NOT NULL | 用户 MIS（来自 SSO） |
| `stage_id` | INT, FK → stages.id | 关卡 ID |
| `verify_status` | ENUM('pending','passed','failed') | 验证状态（`self` 类型通关后直接写 `passed`） |
| `submitted_value` | TEXT | 用户提交的验证内容（URL/粘贴内容等） |
| `passed_at` | DATETIME | 通关时间，NULL 表示尚未通关 |

**主键：** `(user_mis, stage_id)`

**关卡状态推算逻辑（不存状态，按需计算）：**
```
passed_stages = SELECT stage_id FROM user_stage_progress WHERE user_mis=? AND verify_status='passed'
currentStage  = MAX(passed_stages) + 1，若无记录则为 1
stage.id ∈ passed_stages  → completed=true
stage.id == currentStage  → 当前关卡（可操作）
stage.id >  currentStage  → 锁定（可预览，不可操作）
```

---

### 5. `user_step_progress` — 用户步骤完成记录表

只记录已勾选的步骤，未完成不写行。

| 字段 | 类型 | 说明 |
|------|------|------|
| `user_mis` | VARCHAR(64), NOT NULL | 用户 MIS |
| `step_id` | INT, FK → stage_steps.id | 步骤 ID |
| `completed_at` | DATETIME | 完成时间 |

**主键：** `(user_mis, step_id)`

> `stage_id` 不需要单独存，通过 `step_id → stage_steps.stage_id` 可以关联查到。

---

## 二、API 接口详细说明

### 1. 获取当前用户信息

```
GET /api/user/me?mock_mis={mis}
```

**用途：** 首页顶部导航栏展示用户姓名。

**响应：**
```json
{
  "mis": "zhang_san",
  "name": "张三",
  "avatar": "https://xxx/avatar.jpg",
  "org": "平台产品部"
}
```

**逻辑：** 直接调用美团 SSO 接口，根据 mis 获取用户信息，不需要本地用户表。建议结果缓存（Redis，TTL 10 分钟），避免每次请求都打 SSO。

---

### 2. 获取关卡列表（含当前用户进度）

```
GET /api/stages?mock_mis={mis}
```

**用途：** 首页地图渲染所有关卡点位及状态（已完成/当前/锁定）；关卡详情页判断是否锁定。

**响应：**
```json
{
  "currentStage": 3,
  "completedCount": 2,
  "totalCount": 7,
  "stages": [
    {
      "id": 1,
      "name": "第1关",
      "title": "武器初始化",
      "description": "安装蓝星自研开发平台 Catpaw",
      "completed": true
    },
    {
      "id": 2,
      "name": "第2关",
      "title": "战斗系统核心激活",
      "description": "配置 Node.js、Git 等基础开发环境",
      "completed": true
    },
    {
      "id": 3,
      "name": "第3关",
      "title": "AI战伴绑定",
      "description": "安装 Claude Code，绑定你最强的 AI 战伴",
      "completed": false
    }
    // ...
  ]
}
```

**字段说明：**
- `currentStage`：由后端计算（= 最大已通关 stage_id + 1），不从数据库直接读取
- `totalCount`：`SELECT COUNT(*) FROM stages WHERE is_active=1`，不硬编码
- `stages[].completed`：该关卡是否在 `user_stage_progress` 中有 `verify_status='passed'` 的记录

---

### 3. 获取关卡详情（含用户进度）

```
GET /api/stages/{stageId}?mock_mis={mis}
```

**用途：** 关卡详情页展示步骤列表、FAQ、验证区域，并恢复用户已完成的步骤状态。

**响应：**
```json
{
  "id": 5,
  "name": "第5关",
  "title": "攻占前线阵地",
  "description": "把前端代码发布上线，让用户看到它",
  "verifyType": "url",
  "verifyHint": "请填写你部署后的访问地址，如 https://your-app.example.com",
  "verifyStatus": "none",
  "submittedValue": "",
  "steps": [
    {
      "id": 1,
      "title": "注册部署平台账号",
      "description": "前往 xxx 平台注册账号",
      "commands": ["catpaw deploy init"],
      "tips": "需要美团内网",
      "completed": false
    }
  ],
  "faqs": [
    {
      "question": "部署失败怎么办？",
      "answer": "检查网络连接..."
    }
  ]
}
```

**字段说明：**
- `verifyStatus`：从 `user_stage_progress.verify_status` 读取，无记录时返回 `"none"`
- `submittedValue`：用户上次提交的内容，用于回填输入框，无记录时返回 `""`
- `steps[].completed`：步骤 id 是否在 `user_step_progress` 中有该用户的记录

---

### 4. 标记步骤完成

```
POST /api/progress/step?mock_mis={mis}
Content-Type: application/json
```

**用途：** 用户点击步骤右侧"搞定啦"按钮时调用。

**请求体：**
```json
{
  "stageId": 4,
  "stepId": 2
}
```

**响应：**
```json
{
  "ok": true,
  "stageCompleted": false
}
```

**字段说明：**
- `stageCompleted`：当 `verify_type=self` 且本次勾选后该关卡所有步骤均已完成时返回 `true`，前端弹出通关弹窗

**逻辑：**
1. INSERT IGNORE INTO `user_step_progress`（幂等，重复勾选不报错）
2. 查询该关卡所有步骤数 vs 该用户已完成步骤数
3. 若全部完成且 `verify_type=self`：INSERT OR UPDATE `user_stage_progress` 写入 `verify_status='passed'`、`passed_at=NOW()`
4. 返回 `stageCompleted`

---

### 5. 提交通关验证

```
POST /api/progress/verify?mock_mis={mis}
Content-Type: application/json
```

**用途：** `verifyType` 为 `url`/`paste`/`branch`/`manual` 的关卡，用户提交验证内容时调用。

**请求体：**
```json
{
  "stageId": 5,
  "value": "https://my-app.example.com"
}
```

**响应（自动验证，如 url 类型）：**
```json
{
  "verifyStatus": "passed",
  "message": "验证通过！",
  "stageCompleted": true
}
```

**响应（人工审核，如 manual 类型）：**
```json
{
  "verifyStatus": "pending",
  "message": "已提交，运营团队将在 1-3 个工作日内完成审核",
  "stageCompleted": false
}
```

**逻辑：**
- `url` 类型：后端尝试 HTTP 请求目标 URL，可访问则直接写 `passed`，否则写 `pending` 转人工
- `paste`/`branch`/`manual` 类型：写入 `submitted_value`，`verify_status='pending'`，等待运营审核
- 通过时写入 `passed_at=NOW()`

---

### 6. 获取小组排行榜

```
GET /api/leaderboard/group?mock_mis={mis}
```

**用途：** 首页右侧面板"个人榜 → 小组"Tab。

**响应：**
```json
{
  "myRank": 3,
  "globalRank": 15,
  "totalPassCount": 428,
  "rankPct": 96,
  "group": {
    "name": "平台产品部",
    "myRank": 3,
    "totalMembers": 86,
    "passCount": 24
  },
  "list": [
    {
      "rank": 1,
      "mis": "li_si",
      "name": "李四",
      "avatar": "https://xxx/avatar.jpg",
      "org": "平台产品部",
      "stageName": "第5关",
      "completedCount": 5,
      "lastPassedAt": "2026-05-30T10:13:21.000+08:00"
    }
  ]
}
```

**字段说明：**
- `myRank`：当前用户在本小组中的排名
- `globalRank`：当前用户在全公司排行榜中的排名
- `totalPassCount`：全公司已通关用户总数
- `rankPct`：当前用户超过的用户百分比（向下取整）
- `group.name`：小组名称
- `group.myRank`：当前用户在小组内排名
- `group.totalMembers`：小组总人数（优先取 `org_info.emp_count`）
- `group.passCount`：小组已通关人数
- `list`：小组内成员按 `completedCount` 降序，取前 50 条
- `stageName`：该用户已通关的最高关卡名
- `lastPassedAt`：该用户最高关卡的通关时间

**实现说明：**
1. 通过 SSO 获取当前用户所属小组，再批量获取同组成员 mis 列表
2. 查 `user_stage_progress` 统计各成员通关数
3. 姓名/头像/部门从 SSO 批量拉取，**建议 Redis 缓存（TTL 10 分钟）**，避免排行榜每次请求都打 SSO

---

### 7. 获取全公司排行榜

```
GET /api/leaderboard/meituan?mock_mis={mis}
```

**用途：** 首页右侧面板"个人榜 → 美团"Tab。

**响应：**
```json
{
  "myRank": 156,
  "globalRank": 156,
  "totalPassCount": 428,
  "rankPct": 63,
  "list": [
    // 全公司 top 50，字段同小组榜 list 项
  ]
}
```

**差异：** 不按小组过滤，取全量参与用户排名；该接口不返回 `group` 字段。用户信息同样走 SSO 缓存。

---

## 三、接口汇总

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/api/user/me` | 获取当前用户信息 |
| GET | `/api/stages` | 获取关卡列表（含进度） |
| GET | `/api/stages/:id` | 获取关卡详情（含步骤进度） |
| POST | `/api/progress/step` | 标记步骤完成 |
| POST | `/api/progress/verify` | 提交通关验证 |
| GET | `/api/leaderboard/group` | 小组排行榜 |
| GET | `/api/leaderboard/meituan` | 全公司排行榜 |

---

## 四、用户身份认证说明

**开发阶段：** 所有接口通过 `?mock_mis=zhang_san` query param 传递用户身份。

**生产阶段：** 对接美团 SSO，从请求 Header 中的 token 解析 mis。建议后端封装一个 `getCurrentMis(request)` 方法，开发时读 query param，生产时读 token，对业务逻辑透明。

---

## 五、关卡解锁规则

1. 第 1 关对所有用户默认开放，`user_stage_progress` 无记录时 `currentStage=1`
2. 第 N 关通关后，第 N+1 关自动解锁（无需写入，`currentStage` 实时计算）
3. 锁定/解锁状态完全由 `currentStage` 推算，不存储在数据库
4. `verify_type=self`：步骤全部勾选后由 `POST /api/progress/step` 自动写入通关记录
5. 其他 verify_type：运营后台审核通过后更新 `verify_status='passed'` 并写入 `passed_at`

---

## 六、初始数据说明

后端需预置全部关卡数据，所有关卡的步骤和 FAQ 均应入库，`GET /api/stages/:id` 对任意关卡都应能正常返回。关卡数量由 `stages` 表行数决定，代码中不硬编码总数。

| 关卡 | verify_type | 核心任务 |
|------|------------|---------|
| 第1关 | self | 安装 Catpaw |
| 第2关 | self | 配置 Node.js、Git 基础环境 |
| 第3关 | self | 安装 Claude Code |
| 第4关 | self | 本地跑起第一段代码 |
| 第5关 | url | 前端项目部署上线，提交访问 URL |
| 第6关 | url 或 manual | 前后端联调完整部署 |
| 第7关 | manual | 提交真实项目，人工审核 |

具体步骤内容由产品/运营填写后导入 `stages`、`stage_steps`、`stage_faqs` 表。

> **前端现状说明（不影响后端设计）：** 当前前端对第 1~3 关使用了临时硬编码 mock，暂时不会调用 `GET /api/stages/:id`。这是前端的历史遗留问题，后续会移除。后端按理想方案实现即可，无需为此做特殊处理。
