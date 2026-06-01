# PDE 闯关系统 — 生产上线清单

> 最后更新：2026-04-28
> 负责人：待填写
> 目标环境：生产（appkey: `com.sankuai.ulivepde.aicoding.explore`）

---

## 一、平台权限申请（阻塞项，需提前申请）

### 1.1 数据库平台（DBPlatform）

- [x] 确认 JDBCRef `ulivepde_ulivepdeweb_product` 已在 DBPlatform 注册
- [x] 在 **Zebra 配置 tab** → 详情 → 授权服务，将 appkey `com.sankuai.ulivepde.aicoding.explore` 绑定为授权服务
- [x] 在 **服务鉴权 tab** → KMS 白名单，将 appkey 加入
- [x] 验证：Lion 上 `shardds.ulivepde_ulivepdeweb_product.shard` key 存在且有值

### 1.2 Org 开放平台

- [x] 为 appkey `com.sankuai.ulivepde.aicoding.explore` 申请调用权限
- [x] 生产环境 Org 应用编码已配置：`b1dd539c7c`（写入 `profiles/prod/application.properties`，test 环境保持 `582cd5719c`）
- [x] 验证 `OrgConfig` 中 SDK 可正常拉取员工信息

### 1.3 SSO（WebSSO / OIDC）

- [x] **sso-java-sdk 版本锁定**：构建容器 Maven 3.6.0 不兼容 mdp-parent `1.8.9-RC2` 引入的插件，回退至 `1.8.5.12`，在 `dependencyManagement` 中单独锁定 `sso-java-sdk=2.7.0.3` 满足 SSO 要求
- [x] 生产环境 client-id：`543d948530`（已写入 `profiles/prod/application.properties`）
- [x] SSO 平台已配置回调域名：`pdecamp.sankuai.com`
- [x] **KMS 密钥**：`sso.secret` 已在 KMS 平台创建并授权

### 1.4 Lion 配置中心

- [ ] 创建 Lion key：`pde.admin.mis.list`，值为逗号分隔的管理员 mis 列表
  - 示例：`zhang_san,li_si`
  - 该 key 为空时所有人均无管理员权限，审核功能不可用
- [ ] 确认 `shardds.ulivepde_ulivepdeweb_product.shard` 已由 DBPlatform 自动同步（见 1.1）

---

## 二、代码 / 配置修改（上线前必须完成）

### 2.1 生产配置文件

- [x] `profiles/prod/application.properties` 所有配置项已填写完毕：
  - `mdp.sso.client-id=543d948530`
  - `mdp.sso.front-end-hosts=pdecamp.sankuai.com`
  - `mdp.zebra[0].jdbcRef=ulivepde_ulivepdeweb_product`
  - `org.app-key=b1dd539c7c`
  - `cors.allowed-origins=https://pdecamp.sankuai.com`

### 2.2 CORS 配置

- [x] CORS 域名按环境区分：`CorsConfig` 改为读 `cors.allowed-origins` 配置项，prod 收窄为 `https://pdecamp.sankuai.com`，test 默认 `*`

### 2.3 日志级别

- [x] 生产日志级别已调整为 INFO（写入 `profiles/prod/application.properties`，test 保持 DEBUG）

---

## 三、数据库初始化

### 3.1 建表

- [ ] 在生产数据库执行 `src/main/resources/sql/schema.sql`
  - 包含表：`stage`、`stage_step`、`stage_faq`、`user_stage_progress`、`user_step_progress`、`activity_event`
- [ ] 确认所有表的字符集为 `utf8mb4`（schema.sql 中已声明，确认 DB 实例支持）

### 3.2 初始化关卡数据

- [ ] 执行 `src/main/resources/sql/data.sql`，写入 7 个关卡及其步骤、FAQ
- [ ] 执行后验证：`SELECT COUNT(*) FROM stage;` 应返回 7
- [ ] 验证每个关卡的 `verify_type` 正确：
  - 关卡 1~4：`clipboard`
  - 关卡 5~7：`manual`

### 3.3 数据库连接池

- [ ] 评估生产并发量，按需调整连接池参数（当前：min=5, max=20）：
  ```properties
  mdp.zebra[0].minPoolSize=5
  mdp.zebra[0].maxPoolSize=20
  ```

---

## 四、功能完善（上线前需确认范围）

### 4.1 排行榜（当前为存根实现）

- [ ] **小组排行榜**（`LeaderboardService.getGroupLeaderboard`）
  - 当前：直接返回全公司数据（TODO 注释）
  - 需要：接入 SSO/Org，获取当前用户所在小组的成员 mis 列表，再过滤
  - 上线决策：**若首期不需要小组排行榜，可隐藏前端入口，暂不修复**

- [ ] **全公司排行榜**（`LeaderboardService.getMeituanLeaderboard`）
  - 当前：仅统计当前登录用户的进度（TODO 注释）
  - 需要：接入 Org 获取全量员工 mis 列表，遍历统计通关数
  - 注意：全量遍历性能较差，建议加定时缓存（如每 5 分钟刷新一次）
  - 上线决策：**若首期排行榜为空/不准确可接受，可带 TODO 上线**

### 4.2 手动审核流程验证

- [ ] 端到端测试：提交第 5/6/7 关 → 管理员登录 → 审核通过/拒绝 → 用户状态更新
- [ ] 确认 Lion key `pde.admin.mis.list` 已配置，管理员可正常访问 `/ulivepde/api/admin/reviews`

### 4.3 URL 验证（第 5/6/7 关 url 类型）

- [ ] 确认生产环境服务器可访问用户提交的 URL（网络策略，10 秒超时 HEAD 请求）
- [ ] 若生产网络受限，URL 可达性检查会失败并降级为 `pending`（手动审核），确认该行为可接受

---

## 五、SSE 实时动态

### 5.1 网关 / 负载均衡配置

- [ ] 确认 Oceanus 网关对 `/ulivepde/api/activity/stream` 接口**不做响应缓冲**（SSE 需要流式传输）
- [ ] 确认网关超时时间 > 30 分钟（当前 Emitter 超时设为 30 分钟），否则连接会被提前断开
  - 当前心跳间隔 10 秒，Oceanus 默认 15 秒断连，心跳可保活
- [ ] 若使用多实例部署，SSE 广播只在单实例内有效（`CopyOnWriteArrayList` 存内存），需确认：
  - 方案 A：单实例部署（简单，首期可接受）
  - 方案 B：引入消息队列（MQ）广播到所有实例（多实例必须）

### 5.2 activity_event 表

- [ ] 确认 `activity_event` 表已在 schema.sql 中建立（含 index）
- [ ] 确认 `ActivityEventPersistService` 的数据库写入逻辑正常（异步持久化）
- [ ] 定时清理任务：每天 3:00 清理 90 天前的记录（`@Scheduled`，需确认 `@EnableScheduling` 生效）

---

## 六、部署配置

### 6.1 Maven 打包

- [ ] 使用 prod profile 打包：
  ```bash
  mvn clean package -P prod -DskipTests
  ```
- [ ] 确认打包产物中 `META-INF/app.properties` 存在且内容为：
  ```properties
  app.name=com.sankuai.ulivepde.aicoding.explore
  ```

### 6.2 JVM 参数

- [ ] 根据机器规格设置合理的堆内存（建议 `-Xms512m -Xmx1g` 起步）
- [ ] 确认 Java 8 运行时（`java -version`）

### 6.3 端口 / 路由

- [ ] 服务端口 8081，确认与 Oceanus 路由规则一致
- [ ] 所有 API 路径前缀为 `/ulivepde`（context-path 配置）
- [ ] 健康检查接口：`GET /monitor/alive` → 返回 `ok`，配置到部署平台的存活探针

### 6.4 多实例注意事项

- [ ] 若多实例部署，SSE 广播需引入 MQ（见 5.1）
- [ ] 确认 Zebra 连接池在多实例下总连接数不超过 DB 上限

---

## 七、上线验证（冒烟测试）

部署完成后，按顺序执行以下验证：

### 7.1 基础健康

```bash
curl https://<prod_host>/monitor/alive
# 期望: ok

curl https://<prod_host>/ulivepde/api/stages
# 期望: 返回 7 个关卡（需带有效 SSO Cookie）
```

### 7.2 用户接口

- [ ] 使用真实 SSO Cookie 访问 `/ulivepde/api/user/me`，确认返回正确的 mis/name/org
- [ ] 访问 `/ulivepde/api/stages`，确认 7 个关卡均返回且 `is_active=true`

### 7.3 进度流程

- [ ] 标记第 1 关第 1 步完成：`POST /ulivepde/api/progress/step`
- [ ] 提交第 1 关验证（clipboard 类型）：`POST /ulivepde/api/progress/verify`
- [ ] 确认第 1 关状态变为 `passed`

### 7.4 SSE 推流

- [ ] 浏览器打开 SSE 连接，确认可收到心跳和历史活动记录
- [ ] 完成一个关卡后，确认 SSE 推送了对应的 `complete` 事件

### 7.5 管理员审核

- [ ] 用管理员 mis 账号访问 `/ulivepde/api/admin/me`，确认 `isAdmin: true`
- [ ] 提交一个 manual 类型关卡（第 5/6/7 关），确认出现在待审核列表
- [ ] 执行审核通过，确认用户状态更新为 `passed`

### 7.6 排行榜

- [ ] 访问 `/ulivepde/api/leaderboard/meituan`，确认接口正常返回（即使数据不完整）

---

## 八、回滚预案

- [ ] 保留上一版本 JAR 包，确认可快速切换
- [ ] 数据库 schema 变更均向前兼容（无破坏性 DDL），无需回滚 SQL
- [ ] Lion 配置变更（admin mis list 等）可实时回滚

---

## 九、遗留问题跟踪

| 编号 | 问题 | 影响 | 计划解决 |
|------|------|------|---------|
| P1 | 小组排行榜返回全公司数据 | 功能不正确 | 待定 |
| P1 | 全公司排行榜仅含当前用户 | 功能不正确 | 待定 |
| P2 | CORS 允许所有来源 | 安全风险 | 上线前修复 |
| P3 | 多实例 SSE 广播不同步 | 多实例部署时失效 | 单实例首期规避 |
| P3 | 排行榜无缓存，全量遍历性能差 | 大数据量时慢 | 接入全量用户时修复 |

---

## 检查项汇总

| 类别 | 总数 | 完成 |
|------|------|------|
| 平台权限申请 | 8 | — |
| 代码/配置修改 | 6 | — |
| 数据库初始化 | 5 | — |
| 功能完善确认 | 4 | — |
| SSE 配置 | 5 | — |
| 部署配置 | 6 | — |
| 上线冒烟测试 | 10 | — |
| 回滚预案 | 3 | — |
