# PDE 闯关·各关卡详细步骤

## 段位总览

| 段位 | 名称 | 一句话任务 | 通关方式 |
|------|------|-----------|---------|
| 🥉 青铜 | 安装 CatPaw | 安装 CatPaw Desktop，登录成功 | 输入回复自申报 |
| 🥈 白银 | 安装开发环境 | 装好 Homebrew、Git、Node.js 等基础环境，版本检查通过 | 输入回复自申报 |
| 🥇 黄金 | 安装Claude Code | 安装美团 CLI 和 Claude Code，通过mc --code打开 Claude Code | 输入回复自申报 |
| 💎 铂金 | 本地代码跑起来 | 用 Claude Code 创建 React+Vite 项目，本地能访问启动并访问页面 | 输入回复自申报 |
| 💠 钻石 | 纯前端页面发布 | 用 Claude Code 完成指定需求并在测试环境发布，泳道查看效果 | 测试环境泳道发布 |
| ⭐ 星耀 | 前后端联调并发布 | 与已有后端接口联调，泳道环境发布成功并看到效果 | 测试环境泳道发布，接口调用与页面展示正常 |
| 🏆 最强王者 | 前后端全栈开发 | 从零做出一个完整前后端全栈开发的功能模块 | 测试环境泳道发布，接口调用与页面展示正常 |

---

## 🥉 青铜·安装 CatPaw

**你要做的一件事：装好 CatPaw**

CatPaw 是美团内部的 AI 编程助手，后续所有关卡都在它里面完成。

**步骤：**

1. 打开 [CatPaw 下载页](https://catpaw.sankuai.com)，用 SSO 登录下载 Desktop 版
2. 安装完成后打开，用美团账号登录
3. 看到主界面，说明成功了

**通关：** 在下方输入「我装好了」，点「完成」即可

---

## 🥈 白银·安装开发环境

**你要做的一件事：装好本地开发环境**

后续写代码、跑项目都需要这些基础工具。装一次，后面一直用。

**第一步：装 Homebrew（macOS 专属，Windows 忽略）**

Homebrew 是 macOS 上最常用的包管理器，后续安装 Git、Node.js 都会用到它。打开「终端」（Terminal），执行：

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 验证安装成功
brew --version
# 看到版本号说明安装成功
```

**第二步：确认 Git 可用（Mac 一般已有）**

```bash
brew install git       # macOS（通过 Homebrew）
git --version          # 看到版本号说明已有
```

**第三步：安装 Node.js**

Claude Code 和后续项目都依赖 Node.js，这里一并装好。

```bash
# macOS（通过 Homebrew）
brew install node@18

# 验证安装
node -v    # 应该显示 v18.x.x 或更高
npm -v     # 应该显示 9.x.x 或更高
```

> Windows 用户：前往 [nodejs.org](https://nodejs.org) 下载 LTS 版本安装包，安装完成后在命令提示符验证 `node -v`。

**通关：** 在下方输入 `brew --version`、`git --version`、`node -v` 的版本号，点「完成」即可

---

## 🥇 黄金·安装Claude Code

**你要做的一件事：装好美团 CLI 和 Claude Code，让 AI 帮你写代码**

美团 CLI（`mc`）是 CatPaw 的命令行版本，`mc --code` 会启动 Claude Code 对话界面，后续所有关卡的编码任务都在这里完成。

**步骤：**

1. 打开终端

2. 安装美团 CLI：

```bash
bash -c "$(curl -sL https://s3plus.sankuai.com/mcopilot-cli/install.sh)"
```

3. 安装 Claude Code：

```bash
npm install -g @anthropic-ai/claude-code
```

4. 启动 Claude Code，看到对话界面说明成功：

```bash
mc --code
```

> 遇到 Permission denied？在命令前加 `sudo` 再试。CLI 安装详情可参考：[CatPaw CLI 用户文档](https://km.sankuai.com/collabpage/1699559267)

**通关：** 在下方输入「跑起来了」，点「完成」即可

---

## 💎 铂金·本地代码跑起来

**你要做的一件事：用 Claude Code 创建一个 React+Vite 项目并在本地跑起来**

不需要自己写代码，告诉 Claude Code 你想要什么，让它帮你创建项目、安装依赖、启动服务器。

**步骤：**

1. 在终端启动 Claude Code：

```bash
mc --code
```

2. 告诉 Claude Code 创建项目，例如：

```
帮我用 Vite 创建一个 React 项目，项目名叫 my-first-app
```

3. Claude Code 会自动执行以下操作（你只需要确认）：

```bash
npm create vite@latest my-first-app -- --template react
cd my-first-app
npm install
npm run dev
```

4. 打开浏览器访问 `http://localhost:5173`，看到 Vite + React 页面说明成功。

> 遇到端口被占用？让 Claude Code 帮你换一个端口。

**通关：** 在下方输入「本地跑起来了」，点「完成」即可

---

## 💠 钻石·纯前端页面发布

**你要做的一件事：用 Claude Code 完成一个需求，提交代码至代码仓库，部署到泳道看到效果**

这是第一次真正意义上的「写代码 → 上线」全流程。仓库里有一个 good-first-issue 列表，每人认领一个。**到这一关需要先申请仓库写权限，小祥会在一个工作日内处理。**

前端仓库：[runmap-frontend](https://dev.sankuai.com/code/repo-detail/ulive-pde-explore/runmap-frontend/file/list)

**步骤：**

1. 确认已配置 Git SSH Key（首次使用需要配置，否则 push 代码会失败）：[配置 Git SSH Key](https://km.sankuai.com/collabpage/1465033066)

2. 权限审批通过后，clone 代码至 CatPaw，在项目目录启动 Claude Code：

```bash
mc --code
```

3. 告诉 Claude Code 你要做什么，它会自动修改代码，确认没问题后提交：

```bash
git checkout -b feat/你的名字-改动描述
git add .
git commit -m "feat: 你的改动描述"
git push origin feat/你的名字-改动描述
```

4. 提交代码后到代码仓库确认提交记录

5. 在前端代码发布平台进行测试环境发布：[https://talos-better.sankuai.com/](https://talos-better.sankuai.com/)，发布后在泳道环境查看部署效果

6. 在自己的泳道环境测试没问题后，打开仓库页面提 PR，等小祥 review merge

**通关：** 功能成功发布到测试环境，泳道可访问，点「完成」

参考文档：[https://km.sankuai.com/collabpage/1465033066](https://km.sankuai.com/collabpage/1465033066)

---

## ⭐ 星耀·前后端联调并发布

**你要做的一件事：与后端接口联调，在泳道环境发布成功并看到效果**

这一关不再是纯前端改动，你需要调用已有的后端接口，完成一个前后端联动的功能，并在泳道环境验证效果。

前端仓库：[runmap-frontend](https://dev.sankuai.com/code/repo-detail/ulive-pde-explore/runmap-frontend/file/list)
后端 API 文档：（待补充）

**步骤：**

1. 用 Claude Code 完成前端改动，调用指定后端接口实现页面与后端数据的交互（接口说明见上方 API 文档）

2. 提交代码并发布、验证效果（可参考钻石关卡的第 4-6 步）

> **配套视频教程**（小祥录制）：完整演示从提 PR 到泳道发布的全流程，约 10 分钟。视频链接待补充。

**通关：** 在测试环境可通过控制台查看到页面上有接口调用，无逻辑错误，点「完成」

---

## 🏆 最强王者·前后端全栈开发

**你要做的一件事：从零 Coding 出一个完整功能模块**

> 这一关涉及后端开发，需要先装好 Java 开发环境。

**前置：安装后端开发环境**

**第一步：安装 JDK 17**

```bash
# macOS（通过 Homebrew）
brew install openjdk@17

# 验证安装
java -version
# 应该显示 openjdk version "17.x.x"
```

> Windows 用户：前往 [Adoptium 下载页](https://adoptium.net/temurin/releases/?version=17) 下载 JDK 17 安装包，安装完成后在命令提示符验证 `java -version`。

**第二步：安装 Maven**

```bash
# macOS（通过 Homebrew）
brew install maven

# 验证安装
mvn -version
# 应该显示 Apache Maven 3.x.x
```

> Windows 用户：前往 [maven.apache.org](https://maven.apache.org/download.cgi) 下载安装包，配置好 `JAVA_HOME` 和 `M2_HOME` 环境变量。

这一关基于已有仓库，你需要用任何工具（Claude Code / Nocode / Cursor），从一句话需求出发，完成一个前后端完整的功能模块。

前端仓库：[runmap-frontend](https://dev.sankuai.com/code/repo-detail/ulive-pde-explore/runmap-frontend/file/list)
后端仓库：[runmap-backend](https://dev.sankuai.com/code/repo-detail/ulive-pde-explore/runmap-backend/file/list)

**参考场景（任选其一）：**

- 用户账号管理：列表 + 搜索 + 冻结/解冻 + 异常记录
- 商户入驻系统：表单 + 状态流转（待提交 → 待审核 → 已认证）
- 或者你自己想的任何业务场景

**完成标准：**

- 有一个可以访问的链接（内网或公网均可）
- 核心功能跑通，不是 demo 截图
- 分享会上演示并讲思路

**通关：** 功能成功发布到测试环境，泳道可访问，接口调用与页面展示正常，点「完成」

---

## 成果校验

| 段位 | 校验方式 |
|------|---------|
| 🥉 青铜 ～ 💎 铂金 | 输入一句话自申报，相信你 |
| 💠 钻石 | 测试环境泳道发布，可访问即通关 |
| ⭐ 星耀 | 测试环境泳道发布，接口调用与页面展示正常 |
| 🏆 最强王者 | 测试环境泳道发布，接口调用与页面展示正常 |

