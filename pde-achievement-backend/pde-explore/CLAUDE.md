# PDE 闯关系统 — 后端服务

## 项目概述

美团内部 PDE 闯关系统后端，帮助新员工/产品开发工程师完成开发环境配置、代码部署等学习任务。7 个关卡：青铜→白银→黄金→铂金→钻石→星耀→最强王者。

## 技术栈

- **框架**：Spring Boot 2.0.7（由 mdp-parent 管理），Java 8
- **数据库**：MySQL，通过美团 Zebra 框架接入
- **ORM**：MyBatis
- **端口**：8081
- **构建**：Maven（parent: com.meituan.mdp:mdp-parent:1.8.5.10）

## 项目结构

```
src/main/java/com/meituan/pde/
├── controller/     # HTTP 接口层
│   ├── UserController.java        # /api/user/*
│   ├── StageController.java       # /api/stages/*
│   ├── ProgressController.java    # /api/progress/*
│   └── LeaderboardController.java # /api/leaderboard/*
├── service/        # 业务逻辑层
│   ├── UserService.java
│   ├── ProgressService.java
│   └── LeaderboardService.java
├── dao/            # MyBatis 数据访问层
└── entity/         # 数据实体

src/main/resources/
├── application.properties  # 配置（注意数据库路径）
├── schema.sql              # 建表 DDL
├── data.sql                # 初始化数据（7 个关卡）
└── mapper/*.xml            # MyBatis SQL 映射
```

## 数据模型

- `users` — 用户（mis 工号为唯一标识）
- `stages` — 关卡配置（静态，id 1~7）
- `stage_steps` — 关卡步骤（含 commands JSON 数组）
- `stage_faqs` — 关卡常见问题
- `user_progress` — 用户进度（步骤级 + 关卡级，step_id=0 表示关卡级记录）

## 认证方式

- 生产：美团 WebSSO，从 Cookie `SSO_TOKEN` 或 Header `X-SSO-Token` 获取 mis
- 开发调试：`?mock_mis=zhang_san` query param
- 默认回退用户：`zhang_san`

## 数据库接入（Zebra）

数据库通过美团 Zebra 框架接入，连接配置由 Lion 配置中心管理，**不在代码里写连接地址**。

### 关键配置

`application.properties` 中：
```properties
mdp.zebra[0].jdbcRef=ulivepde_ulivepdeweb_test
```

`jdbcRef` 必须和数据库平台（DBPlatform）上注册的 JDBCRef 名字完全一致，Zebra 启动时会去 Lion 拉 `shardds.{jdbcRef}.shard` 这个 key 获取数据库地址。

### 部署前需要在数据库平台完成的操作

1. **Zebra配置 tab**：确认 `ulivepde_ulivepdeweb_test` 这个 JDBCRef 存在，并在详情里将 appkey `com.sankuai.ulivepde.aicoding.explore` 绑定为授权服务（这一步会自动把 shardds 配置同步到 Lion）
2. **服务鉴权 tab**：将 appkey 加入 KMS 白名单（控制服务能否拿到数据库密码）

两步都完成后，Lion 里才会有 `shardds.ulivepde_ulivepdeweb_test.shard` 配置，服务才能正常启动。

### META-INF/app.properties

`mdp-parent` 的 `MdpJarLauncher` 要求 classpath 下必须有 `META-INF/app.properties`，内容：
```properties
app.name=com.sankuai.ulivepde.aicoding.explore
```
已放在 `src/main/resources/META-INF/app.properties`，不要删除。

## 本地启动

> 本地启动需要在美团内网，Zebra 需要连接 Lion 配置中心拉取数据库配置。

### 前置条件

- Java 8+
- Maven 3.6+
- 美团内网环境

### 启动服务

```bash
cd pde-explore
mvn spring-boot:run
```

看到如下日志说明启动成功：
```
Started PdeApplication in X.XXX seconds
Tomcat started on port(s): 8081
```

### 验证接口

```bash
# 关卡列表
curl "http://localhost:8081/ulivepde/api/stages?mock_mis=zhang_san"

# 关卡详情
curl "http://localhost:8081/ulivepde/api/stages/1?mock_mis=zhang_san"

# 用户信息
curl "http://localhost:8081/ulivepde/api/user/me?mock_mis=zhang_san"

# 排行榜
curl "http://localhost:8081/ulivepde/api/leaderboard/meituan?mock_mis=zhang_san"
```

> `mock_mis` 参数用于开发调试时模拟登录用户，生产环境通过 WebSSO 自动识别。

## 已知问题 / 待完善

1. 排行榜逻辑在内存中逐个查询用户进度，大数据量下性能较差
2. WebSSO 解析逻辑为 stub，生产环境需对接实际 SSO 系统

## 构建问题修复记录

### Maven 插件版本与构建环境不兼容（2026-04-28）

**现象**：构建报错 `The plugin org.apache.maven.plugins:maven-dependency-plugin:3.8.0 requires Maven version 3.6.3`，构建失败。

**根因**：`mdp-parent 1.8.9` 会自动引入 `maven-clean-plugin 3.4.0` 和 `maven-dependency-plugin 3.8.0`，这两个插件要求 Maven >= 3.6.3，但 `manifest.yaml` 中构建环境配置的是 `maven_offline: 3.6.0`。

**修复方案**：
1. `manifest.yaml` 中将 `maven_offline` 从 `3.6.0` 升级为 `3.9.5`：
   ```yaml
   build:
     tools:
       maven_offline: 3.9.5
   ```
2. `pom.xml` 中显式锁定两个插件到兼容版本（双保险）：
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-clean-plugin</artifactId>
       <version>3.1.0</version>
   </plugin>
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-dependency-plugin</artifactId>
       <version>3.1.2</version>
   </plugin>
   ```

**说明**：AutoDeploy（普通类型）项目的 Maven 版本通过 `manifest.yaml` 的 `build.tools.maven_offline` 字段指定，默认为 `3.6.0`。MDP 类型项目则通过 `plusboot.yaml` 的 `MavenVersion` 字段指定。
