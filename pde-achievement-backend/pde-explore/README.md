# PDE 闯关系统 — 后端服务

美团内部 PDE 闯关系统后端，帮助新员工/产品开发工程师完成开发环境配置、代码部署等学习任务。用户通过完成 7 个关卡（青铜→白银→黄金→铂金→钻石→星耀→最强王者）逐步掌握开发技能。

---

## 技术栈

| 组件 | 版本/说明 |
|------|------|
| Java | 8 |
| Spring Boot | 2.0.7（由 mdp-parent 管理） |
| Maven Parent | com.meituan.mdp:mdp-parent:1.8.5.10 |
| 数据库 | MySQL，通过 Zebra 框架接入 |
| ORM | MyBatis |
| 构建工具 | Maven |

---

## 项目结构

```
pde-explore/
├── src/main/java/com/meituan/pde/
│   ├── controller/          # HTTP 接口层
│   │   ├── UserController.java        # /api/user/*
│   │   ├── StageController.java       # /api/stages/*
│   │   ├── ProgressController.java    # /api/progress/*
│   │   └── LeaderboardController.java # /api/leaderboard/*
│   ├── service/             # 业务逻辑层
│   ├── dao/                 # MyBatis 数据访问层
│   ├── entity/              # 数据实体
│   ├── common/              # 统一响应封装、全局异常处理
│   ├── util/                # 工具类（AuthUtils 等）
│   └── config/              # 数据库配置、CORS 配置
├── src/main/resources/
│   ├── application.properties   # 应用配置
│   ├── schema.sql               # 建表 DDL
│   ├── data.sql                 # 关卡初始化数据
│   └── mapper/                  # MyBatis SQL 映射文件
└── pom.xml
```

---

## 数据库接入（Zebra）

数据库通过美团 Zebra 框架接入，连接配置由 Lion 配置中心管理，不在代码里写连接地址。

### 部署前需要在数据库平台完成

1. **Zebra配置 tab** — 确认 JDBCRef `ulivepde_ulivepdeweb_test` 存在，在详情里将 appkey `com.sankuai.ulivepde.aicoding.explore` 绑定为授权服务（会自动同步 shardds 配置到 Lion）
2. **服务鉴权 tab** — 将 appkey 加入 KMS 白名单

两步都完成后，Zebra 才能从 Lion 拉到 `shardds.ulivepde_ulivepdeweb_test.shard` 配置，服务才能正常连接数据库。

### jdbcRef 说明

`application.properties` 中的 `mdp.zebra[0].jdbcRef` 必须和数据库平台上注册的 JDBCRef 名字完全一致：

```properties
mdp.zebra[0].jdbcRef=ulivepde_ulivepdeweb_test
```

---

## 本地启动

> 需要在美团内网，Zebra 启动时会连接 Lion 配置中心。

### 前置条件

- Java 8+
- Maven 3.6+
- 美团内网环境

### 启动服务

```bash
mvn spring-boot:run
```

看到以下日志说明启动成功：

```
Tomcat started on port(s): 8081
Started PdeApplication in X.XXX seconds
```

---

## 接口文档

### 认证方式

- **开发调试**：请求加 `?mock_mis=zhang_san` 模拟登录
- **生产环境**：从 Cookie `SSO_TOKEN` 或 Header `X-SSO-Token` 读取美团 WebSSO 凭证

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/me` | 获取当前用户信息 |
| GET | `/api/user/progress` | 获取当前用户进度统计 |
| GET | `/api/stages` | 获取所有关卡列表（含完成状态） |
| GET | `/api/stages/:id` | 获取关卡详情（步骤 + FAQ + 视频） |
| POST | `/api/progress/step` | 标记步骤完成/未完成 |
| POST | `/api/progress/stage/:id/complete` | 标记整个关卡完成 |
| GET | `/api/leaderboard/group` | 小组排行榜 |
| GET | `/api/leaderboard/meituan` | 美团全员排行榜 |
| GET | `/api/leaderboard/me` | 我的排名 |

### 快速验证

```bash
# 关卡列表
curl "http://localhost:8081/ulivepde/api/stages?mock_mis=zhang_san"

# 关卡详情
curl "http://localhost:8081/ulivepde/api/stages/1?mock_mis=zhang_san"

# 标记步骤完成
curl -X POST "http://localhost:8081/ulivepde/api/progress/step?mock_mis=zhang_san" \
  -H "Content-Type: application/json" \
  -d '{"stageId": 1, "stepId": 1, "completed": true}'

# 全员排行榜
curl "http://localhost:8081/ulivepde/api/leaderboard/meituan?mock_mis=zhang_san"
```

### 响应格式

所有接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

---

## 数据模型

| 表名 | 说明 |
|------|------|
| `users` | 用户表，mis 工号为唯一标识 |
| `stages` | 关卡配置表，id 1~7 固定 |
| `stage_steps` | 关卡步骤表，commands 字段为 JSON 数组 |
| `stage_faqs` | 关卡常见问题表 |
| `user_progress` | 用户进度表，`step_id=0` 表示关卡级记录 |

---

## CORS

默认允许所有来源跨域（`allowedOrigins("*")`），配置在 `src/main/java/com/meituan/pde/config/CorsConfig.java`。
