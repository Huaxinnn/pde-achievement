# PDE 闯关系统 — API 文档

> **本地**：`http://localhost:8081`
>
> **测试环境**：`http://ulivepde.adp.test.sankuai.com`
>
> 所有业务接口均带 `/ulivepde` 前缀，如 `/ulivepde/api/user/me`

---

## 认证方式

所有接口通过美团 WebSSO 识别用户身份，从 Cookie 或 Header 中读取 mis（工号）。

| 方式 | 说明 |
|------|------|
| Cookie `SSO_TOKEN` | 生产环境自动携带 |
| Header `X-SSO-Token` | 服务端调用时使用 |
| Query `?mock_mis=xxx` | **仅开发调试使用**，模拟登录 |

---

## 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ulivepde/api/user/me` | 获取当前用户信息 |
| GET | `/ulivepde/api/admin/me` | 查询当前用户是否有管理员权限 |
| GET | `/ulivepde/api/stages` | 获取关卡列表（含进度） |
| GET | `/ulivepde/api/stages/:id` | 获取关卡详情（含步骤进度） |
| POST | `/ulivepde/api/progress/step` | 标记步骤完成 |
| POST | `/ulivepde/api/progress/verify` | 提交通关验证 |
| GET | `/ulivepde/api/admin/reviews` | 获取审核列表（仅管理员） |
| POST | `/ulivepde/api/admin/reviews/:id/review` | 执行审核操作（仅管理员） |
| POST | `/ulivepde/api/admin/backfill/user-org` | 回填历史记录的组织路径（仅管理员） |
| POST | `/ulivepde/api/admin/refresh/org-info` | 手动触发 org_info 员工人数刷新（仅管理员） |
| GET | `/ulivepde/api/leaderboard/group` | 小组排行榜 |
| GET | `/ulivepde/api/leaderboard/meituan` | 全公司排行榜 |
| GET | `/ulivepde/api/activity/stream` | 实时动态 SSE 推送 |
| GET | `/ulivepde/api/bounty/tasks` | 悬赏任务列表 |
| GET | `/ulivepde/api/bounty/tasks/:taskId` | 悬赏任务详情 |
| POST | `/ulivepde/api/bounty/tasks/:taskId/design-submit` | 提交设计方案 |
| POST | `/ulivepde/api/bounty/tasks/:taskId/dev-submit` | 提交开发作品 |
| POST | `/ulivepde/api/bounty/tasks/:taskId/vote` | 投票 |
| DELETE | `/ulivepde/api/bounty/tasks/:taskId/vote/:submissionId` | 取消投票 |
| POST | `/ulivepde/api/bounty/tasks/:id/like` | 点赞/取消点赞 |
| POST | `/ulivepde/api/bounty/ideas` | 发布 Idea |
| PUT | `/ulivepde/api/bounty/ideas/:id` | 编辑自己的 Idea |
| DELETE | `/ulivepde/api/bounty/ideas/:id` | 删除自己的 Idea |
| GET | `/ulivepde/api/bounty/ideas/similar` | 相似标题检测 |
| POST | `/ulivepde/api/bounty/join` | 加入悬赏共建 |
| GET | `/ulivepde/api/bounty/membership` | 查询是否已加入共建 |
| GET | `/ulivepde/api/bounty/members` | 查询所有共建成员列表 |
| POST | `/ulivepde/api/bounty/admin/tasks` | 发布官方任务（仅管理员） |
| PUT | `/ulivepde/api/bounty/admin/tasks/:taskId` | 编辑任务（仅管理员） |
| PUT | `/ulivepde/api/bounty/admin/tasks/:taskId/status` | 推进任务状态（仅管理员） |
| DELETE | `/ulivepde/api/bounty/admin/tasks/:taskId` | 删除任务（仅管理员） |
| POST | `/ulivepde/api/bounty/admin/tasks/:taskId/feature` | 翻牌 Idea（仅管理员） |
| POST | `/ulivepde/api/bounty/admin/submissions/:submissionId/review` | 审核提交（仅管理员） |
| POST | `/ulivepde/api/bounty/admin/submissions/:submissionId/winner` | 设置 winner（仅管理员） |
| GET | `/monitor/alive` | 健康检查（仅内部探活） |

---

## 用户相关

### GET /ulivepde/api/user/me

获取当前登录用户信息。

**响应示例**

```json
{
  "mis": "zhang_san",
  "name": "张三",
  "avatar": null,
  "org": "美团-到店-商家平台-技术部"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| mis | string | 工号，用户唯一标识 |
| name | string | 姓名 |
| avatar | string\|null | 头像 URL（当前实现返回 null） |
| org | string | 完整组织名称链，如"美团-到店-商家平台-技术部" |
| completedCount | int | 当前最高已通关关卡编号（无记录时为 0） |

---

## 管理员相关

### GET /ulivepde/api/admin/me

查询当前登录用户是否有管理员权限。任何登录用户均可调用，不会返回 403。

**响应示例**

```json
{ "isAdmin": true }
```

| 字段 | 类型 | 说明 |
|------|------|------|
| isAdmin | boolean | 是否为管理员（由 Lion 配置 `pde.admin.mis.list` 控制） |

---

### GET /ulivepde/api/admin/reviews

获取审核列表，**仅管理员可访问**，非管理员返回 403。

**Query 参数**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| status | string | `pending` | `pending` / `passed` / `failed` / `all` |
| page | int | `1` | 页码，最小为 1 |
| pageSize | int | `20` | 每页条数，范围 1~100 |

**响应示例**

```json
{
  "total": 42,
  "page": 1,
  "pageSize": 20,
  "list": [
    {
      "id": 101,
      "mis": "zhang_san",
      "name": "张三",
      "org": "美团-到店-商家平台-技术部",
      "stageId": 5,
      "stageName": "第5关",
      "stageTitle": "纯前端页面发布",
      "lane": "yanligame",
      "url": "https://yanligame.test.sankuai.com",
      "submittedValue": "泳道名：yanligame\n页面访问地址：https://yanligame.test.sankuai.com",
      "submittedAt": "2026-04-25T14:30:00.000+0000",
      "status": "pending",
      "reviewedAt": null,
      "reviewedBy": null,
      "rejectReason": null
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 记录 ID，审核操作时使用 |
| mis | string | 提交者工号 |
| name | string | 提交者姓名 |
| org | string | 提交者完整组织名称链，如"美团-到店-商家平台-技术部" |
| stageId | long | 关卡 ID |
| stageName | string | 关卡短名，如"第5关" |
| stageTitle | string | 关卡标题 |
| lane | string | 用户填写的泳道名（可为空） |
| url | string | 用户填写的页面访问地址（可为空） |
| submittedAt | datetime | 提交时间 |
| status | string | `pending` / `passed` / `failed` |
| reviewedAt | datetime\|null | 审核时间，未审核为 null |
| reviewedBy | string\|null | 审核人 mis，未审核为 null |
| rejectReason | string\|null | 拒绝原因，未拒绝为 null |

---

### POST /ulivepde/api/admin/reviews/:id/review

对某条提交执行审核操作，**仅管理员可访问**，非管理员返回 403。

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 提交记录 ID（来自审核列表的 `list[].id`） |

**请求 Body**

```json
{
  "action": "pass",
  "rejectReason": ""
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| action | string | 是 | `pass`（通过）或 `reject`（不通过） |
| rejectReason | string | 否 | `action=reject` 时填写，反馈给用户 |

**响应示例**

```json
{
  "success": true,
  "id": 101,
  "action": "pass"
}
```

**副作用**

| action | 效果 |
|--------|------|
| `pass` | `verify_status` → `passed`，`passed_at` → 当前时间，触发 SSE 广播 `type=complete` |
| `reject` | `verify_status` → `failed`，`passed_at` 重置为 `1970-01-01`，`reject_reason` 写入记录 |

---

### POST /ulivepde/api/admin/backfill/user-org

回填历史进度记录中缺失的组织路径字段，**仅管理员可访问**。每次处理一批，反复调用直到 `done: true`。

**Query 参数**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| batchSize | int | `100` | 每次处理的 mis 数量，范围 1~500 |

**响应示例**

```json
{
  "processedMis": 100,
  "updatedStageRows": 87,
  "updatedStepRows": 213,
  "remainingStage": 42,
  "remainingStep": 98,
  "done": false
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| processedMis | int | 本次处理的 mis 数量 |
| updatedStageRows | int | `user_stage_progress` 实际更新行数 |
| updatedStepRows | int | `user_step_progress` 实际更新行数 |
| remainingStage | int | `user_stage_progress` 中仍待回填的行数 |
| remainingStep | int | `user_step_progress` 中仍待回填的行数 |
| done | boolean | 两张表均无待回填记录时为 `true` |

**说明**

- 每次调用只处理 `batchSize` 个 mis，需反复调用直到响应 `done: true` 为止
- 在 Org SDK 中查不到的用户（已离职）写入 `"已离职"`
- SDK 异常时跳过该 mis，下次调用继续重试
- **性能**：每个 mis 发起一次 Org SDK RPC，batchSize 越大单次耗时越长；建议生产环境用默认值 100，避免超时
- **对 Org SDK 的影响**：只有读操作，不写入 Org 系统任何数据
- **写表范围**：仅更新 `user_org_id` 或 `user_org_name_path` 为空的历史记录，不覆盖已有值
- 需要在执行 DDL 并部署新代码后调用；正常运行后新产生的进度记录会实时写入，无需再调用此接口

---

### POST /ulivepde/api/admin/refresh/org-info

手动触发一次 `org_info` 表员工人数刷新，**仅管理员可访问**。同步执行，完成后返回。

**响应示例**

```json
{
  "done": true
}
```

**说明**

- 遍历 `user_stage_progress`/`user_step_progress` 中所有不重复的 `user_org_id`，调 Org SDK `EmpService.queryEmp(orgId, depth=0, EmpHierarchyCond.jobStatusIdET(15), paging)` 分页拉取在职员工并计数，upsert 进 `org_info`
- `depth=0` 表示查询该组织及其所有子组织的员工（全子树），不只是直接成员
- 在职过滤通过 SDK 服务端的 `EmpHierarchyCond.jobStatusIdET(15)` 实现（15=在职，16=离职），由 Org 系统负责过滤，本服务不做二次筛选
- 定时任务每周一凌晨 3 点自动执行，此接口用于手动补跑
- **性能**：每个 org 至少发起一次 SDK RPC，org 数量较多时响应较慢（几秒到几十秒），属正常现象，调用后等待返回即可；分页 pageSize=500，超过 500 人的组织会多次请求
- **对 Org SDK 的影响**：只有读操作，不写入 Org 系统任何数据，不影响 Org SDK 或其他服务
- **写 `org_info` 的时机**：除本接口外，用户每次完成步骤/通关时 `ProgressService` 也会顺带 upsert `org_name`（不更新 `emp_count`），`emp_count` 只由本接口/定时任务维护

---

## 关卡相关

### GET /ulivepde/api/stages

获取所有上线关卡列表，含当前用户完成状态。

**响应示例**

```json
{
  "currentStage": 3,
  "completedCount": 2,
  "totalCount": 7,
  "stages": [
    {
      "id": 1,
      "name": "第1关",
      "title": "安装 CatPaw",
      "description": "CatPaw 是美团内部的 AI 编程助手...",
      "completed": true
    },
    {
      "id": 3,
      "name": "第3关",
      "title": "安装Claude Code",
      "description": "安装美团 CLI 和 Claude Code...",
      "completed": false
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| currentStage | long | 当前激活关卡 ID（= 最大已通关 stage_id + 1，无记录时为 1） |
| completedCount | int | 已通关关卡数 |
| totalCount | int | 全部上线关卡数 |
| stages[].completed | boolean | 该关卡 `verify_status='passed'` |

---

### GET /ulivepde/api/stages/:id

获取单个关卡详情，包含步骤、FAQ 及用户进度。

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 关卡 ID |

**响应示例**

```json
{
  "id": 5,
  "name": "第5关",
  "title": "纯前端页面发布",
  "description": "用 Claude Code 完成一个需求，提交代码并部署到泳道看到效果。",
  "verifyType": "manual",
  "verifyHint": "请填写你的泳道访问地址，如 https://xxx.test.sankuai.com",
  "verifyStatus": "pending",
  "submittedValue": "泳道名：yanligame\n页面访问地址：https://yanligame.test.sankuai.com",
  "steps": [
    {
      "id": 12,
      "title": "配置 Git SSH Key",
      "description": "首次使用需要配置...",
      "commands": [],
      "tips": "",
      "completed": true
    }
  ],
  "faqs": [
    {
      "question": "git push 报错没有权限怎么办？",
      "answer": "请先配置 Git SSH Key..."
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| verifyType | string | 通关验证方式，见下方说明 |
| verifyHint | string | 验证输入框提示文字 |
| verifyStatus | string | `none`（未提交）/ `pending` / `passed` / `failed` |
| submittedValue | string | 用户上次提交的原始内容，未提交时为 `""` |
| lane | string | 用户上次填写的泳道名，未填时为 `""`，直接回填输入框 |
| url | string | 用户上次填写的页面地址，未填时为 `""`，直接回填输入框 |
| rejectReason | string | 审核不通过的原因，未被拒绝时为 `""`，前端在 `verifyStatus=failed` 时展示给用户 |
| steps[].completed | boolean | 该步骤是否已完成 |

**verifyType 说明**

| 值 | 说明 | 关卡 |
|----|------|------|
| `clipboard` | 自动验证，直接通关（前端已完成校验） | 1~4 关 |
| `manual` | 人工审核，提交后 `status=pending`，等待管理员审核 | 5~7 关 |

> `self` / `url` / `paste` / `branch` 为历史类型，当前关卡配置不再使用。

---

## 进度相关

### POST /ulivepde/api/progress/step

标记某步骤完成（幂等，重复提交不报错）。

**请求 Body**

```json
{
  "stageId": 4,
  "stepId": 2
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| stageId | long | 是 | 关卡 ID |
| stepId | long | 是 | 步骤 ID |

**响应示例**

```json
{
  "ok": true,
  "stageCompleted": false
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| ok | boolean | 是否成功 |
| stageCompleted | boolean | 仅 `verify_type=self` 时，所有步骤完成后返回 `true`，前端弹通关弹窗 |

---

### POST /ulivepde/api/progress/verify

提交通关验证，适用于 `clipboard` / `manual` 类型关卡。

**请求 Body**

```json
{
  "stageId": 5,
  "value": "泳道名：yanligame\n页面访问地址：https://yanligame.test.sankuai.com",
  "lane": "yanligame",
  "url": "https://yanligame.test.sankuai.com"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| stageId | long | 是 | 关卡 ID |
| value | string | 是 | 用户提交的完整内容（向后兼容，始终传） |
| lane | string | 否 | 泳道名（5~7 关建议传，供审核页展示） |
| url | string | 否 | 页面访问地址（5~7 关建议传，供审核页展示） |

**响应示例（clipboard 类型，直接通关）**

```json
{
  "verifyStatus": "passed",
  "message": "验证通过！",
  "stageCompleted": true
}
```

**响应示例（manual 类型，等待审核）**

```json
{
  "verifyStatus": "pending",
  "message": "已提交，运营团队将在 1-3 个工作日内完成审核",
  "stageCompleted": false
}
```

**各 verifyType 处理逻辑**

| verifyType | 逻辑 |
|-----------|------|
| `clipboard` | 直接写 `passed`，触发 SSE 广播，返回 `stageCompleted=true` |
| `manual` | 写 `pending`，等待管理员通过 `/admin/reviews/:id/review` 审核 |
| `url` | 后端请求目标 URL，可访问写 `passed`，否则写 `pending` |

---

## 排行榜相关

### GET /ulivepde/api/leaderboard/group

获取当前用户所在小组的排行榜。

**Query 参数**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| limit | int | 50 | 返回条数 |

**响应示例**

```json
{
  "myRank": 3,
  "globalRank": 15,
  "totalPassCount": 428,
  "rankPct": 96,
  "group": {
    "name": "美团-到店-商家平台-技术部",
    "myRank": 3,
    "totalMembers": 86,
    "passCount": 24
  },
  "list": [
    {
      "rank": 1,
      "mis": "li_si",
      "name": "李四",
      "avatar": null,
      "org": "美团-到店-商家平台-技术部",
      "stageName": "第5关",
      "completedCount": 5,
      "lastPassedAt": "2026-05-30T10:13:21.000+08:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| myRank | int | 当前用户在本小组中的排名 |
| globalRank | int | 当前用户在全公司排行榜中的排名 |
| totalPassCount | int | 全公司已通关用户总数 |
| rankPct | int | 当前用户超过的用户百分比（向下取整） |
| group.name | string | 小组名称 |
| group.myRank | int | 当前用户在小组内排名 |
| group.totalMembers | int | 小组总人数（优先取 `org_info`） |
| group.passCount | int | 小组内已通关人数 |
| list[].org | string | 完整组织名称链，如"美团-到店-商家平台-技术部" |
| list[].completedCount | int | 已通关关卡数 |
| list[].stageName | string | 已通关的最高关卡名 |
| list[].lastPassedAt | datetime | 最高关卡通关时间 |

**排名规则**：按 `completedCount` 降序，相同通关数按 `lastPassedAt` 升序（先通关排前面）。

---

### GET /ulivepde/api/leaderboard/meituan

获取全公司排行榜，不按小组过滤，取全量参与用户 top N。

该接口返回 `myRank`、`globalRank`、`totalPassCount`、`rankPct` 和 `list`，不返回 `group` 字段。

---

## 实时播报

### GET /ulivepde/api/activity/stream

SSE（Server-Sent Events）长连接，实时推送所有用户的开始/完成关卡动态。

**连接建立时**立即推送最近 10 条历史通关记录（`type=complete`），随后挂起等待新事件。

**前端接入示例**

```js
const es = new EventSource('/ulivepde/api/activity/stream', { withCredentials: true });
es.onmessage = e => {
  const event = JSON.parse(e.data);
  // event.type === 'start' 或 'complete'
};
```

**推送格式**

```
data: {"id":1,"name":"王小明","avatar":"王","type":"complete","stageId":3,"stageTitle":"安装Claude Code","rankName":"🥇 黄金","timestamp":1745123456789}

data: {"id":2,"name":"李思远","avatar":"李","type":"start","stageId":2,"stageTitle":"安装开发环境","rankName":"🥈 白银","timestamp":1745123400000}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 事件唯一 ID |
| name | string | 用户姓名 |
| avatar | string | 姓名首字，前端用于显示头像圆 |
| type | string | `complete`（通关）/ `start`（开始挑战） |
| stageId | int | 关卡编号 1~7 |
| stageTitle | string | 关卡标题 |
| rankName | string | 段位文字 |
| timestamp | long | 事件时间，Unix 毫秒 |

**段位映射**

| stageId | rankName |
|---------|----------|
| 1 | 🥉 青铜 |
| 2 | 🥈 白银 |
| 3 | 🥇 黄金 |
| 4 | 💎 铂金 |
| 5 | 💠 钻石 |
| 6 | ⭐ 星耀 |
| 7 | 🏆 最强王者 |

**事件触发时机**

| type | 触发条件 |
|------|---------|
| `start` | 用户完成某关卡的第一个步骤 |
| `complete` | 用户通关（`clipboard` 自动通关，或管理员审核通过） |

服务端每 30 秒发送 `: ping` 心跳防止代理超时，前端无需处理。

---

## 关卡解锁规则

1. 第 1 关对所有用户默认开放，无记录时 `currentStage=1`
2. 第 N 关通关后，第 N+1 关自动解锁（实时计算，不存储）
3. `verify_type=clipboard`：前端校验通过后调 `/progress/verify`，后端直接写 `passed`
4. `verify_type=manual`：用户提交后 `status=pending`，管理员在审核页操作后更新为 `passed` 或 `failed`

---

## 悬赏任务系统

悬赏任务分两种类型：
- `official`：管理员发布的官方任务，有完整的设计→投票→开发流程
- `idea`：用户提交的创意提案，管理员可翻牌转化为官方任务

任务状态流转：`draft` → `design_open` → `design_voting` → `design_closed` → `dev_open` → `closed`

---

### GET /ulivepde/api/bounty/tasks

获取悬赏任务列表。

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | `official` / `idea`，不传返回全部 |
| status | string | 否 | 任务状态，不传返回全部 |

**响应示例**

```json
{
  "ok": true,
  "tasks": [
    {
      "id": 1,
      "title": "AI 代码审查工具",
      "description": "...",
      "type": "official",
      "status": "design_open",
      "createdBy": "zhang_san",
      "designDeadline": "2026-05-20 23:59:59",
      "votingEnd": null,
      "devDeadline": null,
      "devMinStage": 5,
      "rewardDesc": "最佳方案奖励 XXX",
      "coverUrl": "",
      "refLink": "",
      "likeCount": 12,
      "liked": false,
      "featured": false,
      "featuredBy": "",
      "featuredReason": "",
      "createTime": "2026-05-01 10:00:00",
      "participantCount": 8,
      "myParticipationStatus": "design_submitted"
    }
  ]
}
```

**TaskVO 字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | `official`（官方）/ `idea`（用户创意） |
| status | string | 任务当前状态 |
| devMinStage | int | 参与开发所需最低通关关卡数，5=纯前端，7=前后端 |
| likeCount | int | 点赞数 |
| liked | boolean | 当前用户是否已点赞 |
| featured | boolean | 是否已被翻牌（仅 idea 类型有效） |
| featuredReason | string | 翻牌理由 |
| participantCount | int | 参与人数 |
| myParticipationStatus | string\|null | 当前用户参与状态：`voted` / `design_submitted` / `dev_submitted` / `null` |

---

### GET /ulivepde/api/bounty/tasks/:taskId

获取任务详情，含所有提交方案和当前用户的投票状态。

**响应示例**

```json
{
  "ok": true,
  "detail": {
    "task": { },
    "designSubmissions": [
      {
        "id": 10,
        "taskId": 1,
        "phase": "design",
        "userMis": "li_si",
        "userName": "李四",
        "title": "方案A",
        "url": "https://s3.xxx/design.png",
        "repoUrl": "",
        "description": "...",
        "status": "submitted",
        "rejectReason": "",
        "winner": false,
        "voteCount": 5,
        "mySubmission": false,
        "myVote": true,
        "createTime": "2026-05-02 14:00:00",
        "voters": null
      }
    ],
    "devSubmissions": [],
    "myDesignSubmission": null,
    "myDevSubmission": null,
    "myVotedSubmissionIds": [10],
    "myVoteCount": 1,
    "canJoinDev": true,
    "devMinStage": 5
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| myVotedSubmissionIds | long[] | 当前用户已投票的方案 ID 列表 |
| myVoteCount | int | 当前用户已用票数 |
| canJoinDev | boolean | 当前用户是否满足参与开发的关卡条件 |
| submissions[].mySubmission | boolean | 是否为当前用户提交 |
| submissions[].myVote | boolean | 当前用户是否投了此方案 |
| submissions[].voters | array\|null | 投了我的人列表，仅自己提交的方案返回 |

---

### POST /ulivepde/api/bounty/tasks/:taskId/design-submit

提交设计方案（任务状态须为 `design_open`，每人限 1 份）。

**请求 Body**

```json
{
  "title": "方案A",
  "url": "https://s3.xxx/design.png",
  "description": "设计思路说明"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 方案标题 |
| url | string | 是 | 设计稿链接（S3 等） |
| description | string | 否 | 方案说明，支持 Markdown |

---

### POST /ulivepde/api/bounty/tasks/:taskId/dev-submit

提交开发作品（任务状态须为 `dev_open`，每人限 1 份）。

**请求 Body**

```json
{
  "title": "我的实现",
  "url": "https://xxx.test.sankuai.com",
  "repoUrl": "https://git.sankuai.com/xxx",
  "description": "实现说明"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 作品标题 |
| url | string | 是 | 线上访问地址 |
| repoUrl | string | 是 | 代码仓库链接 |
| description | string | 否 | 说明，支持 Markdown |

---

### POST /ulivepde/api/bounty/tasks/:taskId/vote

对设计方案投票（任务状态须为 `design_voting`，每人限投 1 票，同一方案只能投 1 次）。

**请求 Body**

```json
{ "submissionId": 10 }
```

**响应示例**

```json
{
  "ok": true,
  "submissionId": 10,
  "voteCount": 6,
  "myVoteCount": 1
}
```

---

### DELETE /ulivepde/api/bounty/tasks/:taskId/vote/:submissionId

取消对某方案的投票。响应格式同投票接口。

---

### POST /ulivepde/api/bounty/tasks/:id/like

点赞或取消点赞（toggle）。

**响应示例**

```json
{ "ok": true, "liked": true }
```

---

### POST /ulivepde/api/bounty/ideas

用户发布创意 Idea（type=idea 的任务）。

**请求 Body**

```json
{
  "title": "智能代码补全插件",
  "description": "详细描述...",
  "coverUrl": "https://xxx/cover.png",
  "refLink": "https://xxx/reference"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 创意标题 |
| description | string | 是 | 详细描述，支持 Markdown |
| coverUrl | string | 否 | 封面图 URL |
| refLink | string | 否 | 参考链接 |

---

### PUT /ulivepde/api/bounty/ideas/:id

编辑自己发布的 Idea，字段同发布接口。非本人操作返回错误。

---

### DELETE /ulivepde/api/bounty/ideas/:id

软删除自己发布的 Idea。非本人操作返回错误。

---

### GET /ulivepde/api/bounty/ideas/similar

相似标题检测，用于发布前轻量提示（不阻断提交）。

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 待检测的标题 |
| excludeId | long | 否 | 编辑时排除自身 ID |

**响应示例**

```json
{
  "ok": true,
  "similars": [
    { "id": 5, "title": "AI 代码补全工具", "createdBy": "li_si" }
  ]
}
```

---

### POST /ulivepde/api/bounty/join

加入悬赏共建（已加入过则更新信息）。

**请求 Body**

```json
{
  "dept": "美团-到店-商家平台",
  "role": "fe",
  "customRole": "",
  "reason": "我想参与前端开发"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dept | string | 是 | 所在部门 |
| role | string | 是 | `pm` / `design` / `fe` / `be` / `qa` / `other` |
| customRole | string | 否 | `role=other` 时必填 |
| reason | string | 否 | 加入理由 |

---

### GET /ulivepde/api/bounty/membership

查询当前用户是否已加入共建。

**响应示例（已加入）**

```json
{ "joined": true, "rank": 3, "total": 28 }
```

**响应示例（未加入）**

```json
{ "joined": false, "total": 28 }
```

| 字段 | 类型 | 说明 |
|------|------|------|
| joined | boolean | 是否已加入 |
| rank | int | 第几个加入，仅 `joined=true` 时返回 |
| total | int | 共建成员总人数 |

---

### GET /ulivepde/api/bounty/members

查询所有已加入共建的成员列表，按加入时间升序排列（最早加入的在最前面）。所有登录用户均可调用，最多返回 500 条。

**响应示例**

```json
{
  "ok": true,
  "members": [
    {
      "mis": "zhang_san",
      "name": "张三",
      "dept": "到家事业群",
      "role": "fe",
      "customRole": "",
      "reason": "想为社区贡献前端经验",
      "joinedAt": "2026-01-15 08:30:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| mis | string | 用户工号 |
| name | string | 用户姓名 |
| dept | string | 部门 |
| role | string | 角色：pm / design / fe / be / qa / other |
| customRole | string | 自定义角色，仅 `role=other` 时有值 |
| reason | string | 加入理由 |
| joinedAt | string | 加入时间，格式 `yyyy-MM-dd HH:mm:ss` |

---

## 悬赏任务管理（仅管理员）

以下接口均需管理员身份，非管理员返回 `{ "ok": false, "error": "无管理员权限" }`。

---

### POST /ulivepde/api/bounty/admin/tasks

发布官方任务。

**请求 Body**

```json
{
  "title": "AI 代码审查工具",
  "description": "详细描述...",
  "status": "design_open",
  "devMinStage": 5,
  "rewardDesc": "最佳方案奖励 XXX",
  "designDeadline": "2026-05-20 23:59:59",
  "votingEnd": "2026-05-25 23:59:59",
  "devDeadline": "2026-06-10 23:59:59"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 任务标题 |
| status | string | 否 | 初始状态，默认 `draft` |
| devMinStage | int | 否 | 参与开发最低通关关卡数，默认 5 |
| designDeadline / votingEnd / devDeadline | string | 否 | 格式 `yyyy-MM-dd HH:mm:ss` |

---

### PUT /ulivepde/api/bounty/admin/tasks/:taskId

编辑任务，字段同发布接口（不含 status）。

---

### PUT /ulivepde/api/bounty/admin/tasks/:taskId/status

推进任务状态。

**请求 Body**

```json
{ "status": "design_voting" }
```

---

### DELETE /ulivepde/api/bounty/admin/tasks/:taskId

软删除任务。

---

### POST /ulivepde/api/bounty/admin/tasks/:taskId/feature

翻牌 Idea，将用户创意标记为已选中。

**请求 Body**（可选）

```json
{ "reason": "方向与本期规划契合" }
```

---

### POST /ulivepde/api/bounty/admin/submissions/:submissionId/review

审核用户提交的设计方案或开发作品。

**请求 Body**

```json
{
  "status": "approved",
  "rejectReason": ""
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 是 | `approved`（通过）/ `rejected`（不通过） |
| rejectReason | string | 否 | `rejected` 时填写 |

---

### POST /ulivepde/api/bounty/admin/submissions/:submissionId/winner

将某提交设置为 winner。

---

## 健康检查

### GET /monitor/alive

```
ok
```
