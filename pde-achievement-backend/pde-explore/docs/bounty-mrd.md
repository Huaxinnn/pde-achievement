# 悬赏任务系统 MRD

> 版本：v2.0 | 日期：2026-05-02 | 作者：yanli06

---

## 一、背景与目标

PDE 闯关完成 7 关后，高阶用户没有后续路径。悬赏任务系统为这批用户提供进阶实战入口：

- **官方任务**：官方发布限时功能任务，分设计和开发两个阶段，公开竞争，官方选出最优版本合并主系统
- **Idea 板块**：用户自由发布想法，官方「翻牌」认可后进入展示区

核心价值：提下限靠闯关，拉上限靠悬赏任务的公开竞争。

---

## 二、第一期范围（MVP）

### 包含
- 官方任务两阶段完整流程：设计阶段（提交 + 投票）→ 开发阶段（提交 + 官方审核）
- Idea 板块简版：发布 → 管理员翻牌 → 展示区

### 不包含（后续迭代）
- 投票状态自动流转（先手动，管理员操作）
- Idea 独占开发期、开放认领
- 大象通知集成
- 实物奖励发放流程

---

## 三、任务生命周期

每个任务独立走完整流程，多个任务可同时并行进行。

```
draft
  ↓
design_open     设计阶段开放：所有登录用户可提交需求说明 + S3 链接
  ↓
design_voting   设计投票中：有门槛的用户对设计方案投票
  ↓
design_closed   设计阶段结束：官方选定方案，线下确定开发者名单
  ↓
dev_open        开发阶段开放：符合门槛的用户提交上线 URL + 代码仓库
  ↓
closed          开发结束：官方线下审核，标记 winner，公示
```

**状态流转由管理员手动操作，系统不自动流转。**

---

## 四、数据模型

### bounty_tasks（任务表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | |
| title | varchar(200) | 任务标题 |
| description | text | 任务详情（MRD 内容，支持 Markdown） |
| type | enum | official（官方任务）/ idea（用户提案） |
| status | enum | draft / design_open / design_voting / design_closed / dev_open / closed |
| created_by | varchar(50) | 发布人 mis |
| design_deadline | datetime | 设计阶段截止时间 |
| voting_end | datetime | 投票截止时间 |
| dev_deadline | datetime | 开发阶段截止时间 |
| dev_min_stage | int | 参与开发的最低关卡门槛（5=纯前端，7=前后端） |
| reward_desc | varchar(500) | 奖励描述 |
| is_featured | tinyint | 是否被翻牌（仅 idea 类型） |
| featured_by | varchar(50) | 翻牌人 mis |
| featured_at | datetime | 翻牌时间 |
| created_at | datetime | |
| updated_at | datetime | |

### bounty_submissions（提交表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | |
| task_id | bigint FK | 关联任务 |
| phase | enum | design（设计阶段）/ dev（开发阶段） |
| user_mis | varchar(50) | 提交人 |
| title | varchar(200) | 提交标题 |
| url | varchar(500) | 上线地址（开发阶段必填） / S3 链接（设计阶段必填） |
| repo_url | varchar(500) | 代码仓库链接（开发阶段填写） |
| description | text | 需求说明（设计阶段）/ 技术说明（开发阶段） |
| status | enum | submitted / approved / rejected |
| reject_reason | varchar(500) | 拒绝原因 |
| is_winner | tinyint | 是否获选 winner |
| vote_count | int | 票数（冗余字段，方便排序，仅设计阶段有效） |
| created_at | datetime | |
| updated_at | datetime | |

### bounty_votes（投票表，仅设计阶段）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | |
| task_id | bigint FK | 关联任务 |
| submission_id | bigint FK | 投给哪个提交 |
| user_mis | varchar(50) | 投票人 |
| created_at | datetime | |
| UNIQUE KEY | (task_id, user_mis) | 同一任务每人只能投一票 |

---

## 五、API 设计

### 5.1 任务相关

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/bounty/tasks` | 登录 | 任务列表，支持 type/status 过滤 |
| GET | `/api/bounty/tasks/:id` | 登录 | 任务详情 |
| POST | `/api/bounty/tasks` | 管理员 | 发布官方任务 |
| PUT | `/api/bounty/tasks/:id/status` | 管理员 | 推进任务状态 |
| POST | `/api/bounty/ideas` | 登录 | 用户发布 Idea |
| POST | `/api/bounty/tasks/:id/feature` | 管理员 | 翻牌 Idea |

### 5.2 提交相关

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/bounty/tasks/:id/submissions` | 登录 | 某任务某阶段的所有提交 |
| POST | `/api/bounty/tasks/:id/submissions` | 登录 | 提交（phase 由当前任务状态决定） |
| POST | `/api/bounty/tasks/:id/vote` | 登录 | 投票（设计阶段，每人一票） |
| POST | `/api/bounty/submissions/:id/review` | 管理员 | 审核提交（approve/reject/winner） |

---

## 六、页面设计

### 6.1 悬赏任务首页 `/bounty`

**核心视觉：**

进入页面第一眼：

```
够胆你就来
```

大字，暗色背景，冲击感。副标题：「官方任务 · 真实需求 · 最优作品合并主系统」

**下方内容区（不用 Tab，同一页面视觉分层）：**

上方：进行中的官方任务（权重高，卡片大）
- 任务标题
- 当前阶段（设计中 / 投票中 / 开发中）
- 倒计时
- 已参与人数头像墙（最多显示 5 个）
- 开发门槛标签（如「第5关以上可参与开发」）

下方：Idea 广场（权重低，卡片小）
- 被翻牌的 Idea 有金色「翻牌」标记
- 右上角「发布 Idea」按钮

已结束的任务折叠在「往期任务」区域，默认收起。

---

### 6.2 任务详情 `/bounty/:id`

**顶部：任务信息**
- 标题、当前阶段、截止时间、奖励描述
- MRD 内容（Markdown 渲染）
- 参与按钮（根据阶段和用户资格动态显示）：
  - design_open + 未提交 → 「提交设计方案」
  - design_open + 已提交 → 「修改我的方案」
  - design_voting → 「去投票」
  - dev_open + 符合门槛 + 未提交 → 「提交开发作品」
  - dev_open + 不符合门槛 → 「需第X关以上才能参与开发」（置灰）
  - closed → 「查看结果」

**下方：提交列表**

设计阶段（design_voting / design_closed）：
- 按票数倒序
- 每个卡片：提交人、标题、需求说明摘要、S3 链接预览、票数
- design_voting 状态：显示投票按钮（每人一票，投过则高亮已投）
- winner 有金色标记

开发阶段（dev_open / closed）：
- 按提交时间排序
- 每个卡片：提交人、标题、上线链接、代码仓库链接、说明摘要
- winner 有金色标记

---

### 6.3 提交设计方案 `/bounty/:id/design-submit`

**表单字段：**
- 方案标题（必填）
- 需求说明（必填，支持 Markdown，说清楚「做什么、为什么、长什么样」）
- S3 链接（必填，截图 / 原型 / 录屏均可，上传后粘贴链接）

**提交后：** 跳回任务详情，提示「方案已提交，等待投票阶段开启」

---

### 6.4 提交开发作品 `/bounty/:id/dev-submit`

**表单字段：**
- 作品标题（必填）
- 上线 URL（必填）
- 代码仓库链接（必填）
- 技术说明（选填，支持 Markdown）

**提交后：** 跳回任务详情，提示「作品已提交，等待官方审核」

---

### 6.5 发布 Idea `/bounty/idea/new`

**表单字段：**
- Idea 标题（必填）
- 详细描述（必填，你想解决什么问题，预期效果是什么）

**提交后：** 进入 Idea 广场展示

---

### 6.6 管理员后台 `/admin/bounty`

**Tab 1：任务管理**
- 发布新官方任务（标题、MRD、设计截止时间、开发截止时间、开发门槛、奖励描述）
- 任务列表，每条可推进状态

**Tab 2：设计审核**
- 列出 design_closed 状态任务的所有设计提交
- 可标记 winner（最佳设计方案）

**Tab 3：开发审核**
- 列出 closed 状态任务的所有开发提交
- 可操作：approve / reject（填原因）/ 设为 winner

**Tab 4：Idea 翻牌**
- 列出所有用户 Idea
- 可操作：翻牌（featured）

---

## 七、业务规则

1. **设计阶段**：所有登录用户均可提交，同一任务每人只能提交一次设计方案
2. **投票规则**：同一任务每人只能投一票，不能投自己的方案，设计阶段结束后投票入口关闭
3. **开发门槛**：`dev_min_stage` 控制，用户通关数 >= 该值才能提交开发作品
4. **开发提交**：符合门槛的用户均可提交，同一任务每人只能提交一次
5. **截止限制**：对应阶段截止后，提交按钮消失
6. **winner 唯一**：同一任务同一阶段只能有一个 winner
7. **状态流转**：手动，管理员操作，不自动流转
8. **Idea 可见性**：所有登录用户可见，翻牌后有特殊标记

---

## 八、验收标准

**官方任务流程：**
- [ ] 管理员可发布官方任务，设置两阶段截止时间和开发门槛
- [ ] 管理员可手动推进任务状态
- [ ] design_open 阶段，登录用户可提交设计方案（需求说明 + S3 链接）
- [ ] design_voting 阶段，登录用户可对设计方案投票（每人一票，不能投自己）
- [ ] 投票后票数实时更新，提交列表按票数倒序
- [ ] design_closed 后，管理员可标记最佳设计方案
- [ ] dev_open 阶段，符合门槛用户可提交开发作品（URL + 代码仓库）
- [ ] 不符合门槛用户看到置灰提示
- [ ] 管理员可审核开发提交，设置 winner
- [ ] winner 提交有金色视觉标记

**Idea 板块：**
- [ ] 登录用户可发布 Idea
- [ ] Idea 在广场公开展示
- [ ] 管理员可翻牌，翻牌后有金色标记

**首页：**
- [ ] 「够胆你就来」大字展示
- [ ] 进行中任务显示倒计时和参与人数
- [ ] 已结束任务收起在「往期任务」

---

## 九、后续迭代方向

- 状态自动流转（cron job）
- Idea 独占开发期 + 开放认领
- 大象通知（截止提醒、被翻牌通知、winner 公示）
- 提交票数实时更新（SSE）
- 悬赏任务与段位/积分体系打通
- 设计阶段投票门槛（需过几关才能投票）
