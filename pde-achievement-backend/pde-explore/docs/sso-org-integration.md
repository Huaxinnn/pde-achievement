# SSO + Org 接入

## 方案概述

- **SSO**：接入美团 OIDC，引入 `mdp-boot-starter-web-tob`，starter 自动注册 Filter 验证 Cookie 中的 token，通过后写入 ThreadLocal，业务层用 `UserUtils.getUser().getLogin()` 取 mis
- **Org**：引入 `org:open-sdk:5.0.35`，用 `EmpService.queryByMis()` 查员工姓名，`OrgService.query(orgId)` 取部门路径截前 3 级
- **多环境**：client-id 按环境写在 `profiles/test/` 和 `profiles/prod/` 里，secret 用 `$KMS{sso.secret}` 由 KMS 托管，打包时 `-P test/prod` 选环境

## 改动文件

| 文件 | 变更 |
|---|---|
| `pom.xml` | 替换 `spring-boot-starter-web` → `mdp-boot-starter-web-tob`；新增 `org:open-sdk:5.0.35`；新增 Maven Profile |
| `application.properties` | 新增 `mdp.sso.*` 公共配置（paths、enabled 等） |
| `profiles/test/application.properties` | 新增，test 环境 client-id（待填） |
| `profiles/prod/application.properties` | 新增，prod 环境 client-id（待填） |
| `util/AuthUtils.java` | 手动解析 Cookie → `UserUtils.getUser().getLogin()` |
| `config/OrgConfig.java` | 新增，创建 `EmpService` + `OrgService` Bean |
| `service/SsoService.java` | mock → 真实 Org SDK，新增 `batchGetUserInfo()` |
| `service/LeaderboardService.java` | `buildEntries()` 改为批量查询 |

不改动：`SsoUserInfo`、`UserController`、`LeaderboardController`、所有 entity/dao/mapper

## 数据流

```
HTTP 请求（带 SSO Cookie）
    ↓
SSO Filter（自动注册）— 验证 token，写入 ThreadLocal
    ↓
AuthUtils.getMisFromRequest() — UserUtils.getUser().getLogin()
    ↓
SsoService.getUserInfo(mis)
    ├─ EmpService.queryByMis(mis) → emp.getName()
    └─ OrgService.query(emp.getOrgId()) → orgPath 截前 3 级
```

## 验证步骤

1. `mvn compile` 无报错
2. 带有效 SSO Cookie 访问 `/ulivepde/api/user/me`，返回真实姓名和部门
3. 访问 `/ulivepde/api/leaderboard/meituan`，排行榜显示真实 name/org
4. 传不存在的 mis，接口不报 500，name 降级为 mis

## 待补充信息

> 代码中对应位置已留 TODO 注释。

| # | 信息 | 状态 |
|---|---|---|
| 1 | SSO test `client-id` → `profiles/test/application.properties` | ✅ `c3c790521e` |
| 2 | SSO prod `client-id` → `profiles/prod/application.properties` | ⏳ 待申请 |
| 3 | SSO `secret` → KMS key `sso.secret` | ✅ 已建好 |
| 4 | test 前端域名 → `profiles/test/application.properties` | ✅ `pdecamp.adp.test.sankuai.com` |
| 5 | Org SDK 调用权限 | ✅ 已完成 |
| 6 | Zebra + KMS 白名单 | ✅ 已完成 |
