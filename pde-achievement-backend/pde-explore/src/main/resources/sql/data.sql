-- =============================================
-- 清空旧数据
-- =============================================
TRUNCATE TABLE stage_faq;
TRUNCATE TABLE stage_step;
TRUNCATE TABLE stage;

-- =============================================
-- 关卡配置
-- =============================================
INSERT IGNORE INTO stage (id, name, title, description, verify_type, verify_hint, sort_order, is_active) VALUES
(1, '第1关', '安装 CatPaw',       'CatPaw 是美团内部的 AI 编程助手，后续所有关卡都在它里面完成。',          'clipboard', '',                                                                 1, 1),
(2, '第2关', '安装开发环境',       '装好 Homebrew、Git、Node.js 等基础工具，装一次后面一直用。',              'clipboard', '请粘贴 node -v 和 git --version 的输出，如：v18.17.0 / git version 2.39.0', 2, 1),
(3, '第3关', '安装Claude Code',   '安装美团 CLI 和 Claude Code，让 AI 帮你写代码。',                         'clipboard', '请粘贴 mc --version 或 claude --version 的输出',                       3, 1),
(4, '第4关', '本地代码跑起来',     '用 Claude Code 创建 React+Vite 项目，在本地浏览器里看到页面。',            'clipboard', '请粘贴 npm run dev 的启动日志，包含 Local: http://localhost:5173 即可',   4, 1),
(5, '第5关', '纯前端页面发布',     '用 Claude Code 完成一个需求，提交代码并部署到泳道看到效果。',               'manual',    '请填写你的泳道访问地址，如 https://xxx.test.sankuai.com',              5, 1),
(6, '第6关', '前后端联调并发布',   '调用已有后端接口，完成前后端联动功能，部署到泳道验证效果。',                'manual',    '请填写你的泳道访问地址，如 https://xxx.test.sankuai.com',              6, 1),
(7, '第7关', '前后端全栈开发',     '基于已有仓库，从零完成一个前后端完整的功能模块并部署到泳道。',              'manual',    '请描述你完成的功能模块，并填写泳道访问地址',                           7, 1);

-- =============================================
-- 关卡步骤
-- =============================================

-- 第1关：安装 CatPaw
INSERT IGNORE INTO stage_step (stage_id, sort_order, title, description, commands, tips) VALUES
(1, 1, '下载并安装 CatPaw', '打开 [CatPaw 下载页](https://catpaw.sankuai.com)，用 SSO 登录下载 Desktop 版，安装完成后打开。', '[]', ''),
(1, 2, '登录美团账号',      '用美团账号登录 CatPaw，看到主界面说明成功。', '[]', '');

-- 第2关：安装开发环境
INSERT IGNORE INTO stage_step (stage_id, sort_order, title, description, commands, tips) VALUES
(2, 1, '安装 Homebrew（macOS）', 'Homebrew 是 macOS 上最常用的包管理器，后续安装 Git、Node.js 都会用到它。Windows 用户忽略此步骤。', '["/bin/bash -c \"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""]', '安装完成后执行 brew --version 验证'),
(2, 2, '安装 Git',               'macOS 一般已自带 Git，执行以下命令确认可用。', '["brew install git", "git --version"]', '看到版本号说明已有'),
(2, 3, '安装 Node.js',           'Claude Code 和后续项目都依赖 Node.js。Windows 用户前往 [nodejs.org](https://nodejs.org) 下载 LTS 版本安装。', '["brew install node@18", "node -v", "npm -v"]', '应该显示 v18.x.x 或更高');

-- 第3关：安装 Claude Code
INSERT IGNORE INTO stage_step (stage_id, sort_order, title, description, commands, tips) VALUES
(3, 1, '安装美团 CLI', '美团 CLI（mc）是 CatPaw 的命令行版本，mc --code 会启动 Claude Code 对话界面。', '["bash -c \"$(curl -sL https://s3plus.sankuai.com/mcopilot-cli/install.sh)\""]', '遇到 Permission denied 在命令前加 sudo 再试，详见 [CatPaw CLI 用户文档](https://km.sankuai.com/collabpage/1699559267)'),
(3, 2, '安装 Claude Code',       '通过 npm 安装 Claude Code 命令行工具。', '["npm install -g @anthropic-ai/claude-code"]', ''),
(3, 3, '启动 Claude Code',       '执行以下命令，看到对话界面说明成功。', '["mc --code"]', '');

-- 第4关：本地代码跑起来
INSERT IGNORE INTO stage_step (stage_id, sort_order, title, description, commands, tips) VALUES
(4, 1, '启动 Claude Code', '在终端启动 Claude Code。', '["mc --code"]', ''),
(4, 2, '创建 React+Vite 项目', '告诉 Claude Code 创建项目，例如：「帮我用 Vite 创建一个 React 项目，项目名叫 my-first-app」', '["npm create vite@latest my-first-app -- --template react", "cd my-first-app", "npm install", "npm run dev"]', 'Claude Code 会自动执行这些命令，你只需要确认'),
(4, 3, '浏览器访问页面', '打开浏览器访问 http://localhost:5173，看到 Vite + React 页面说明成功。', '[]', '遇到端口被占用？让 Claude Code 帮你换一个端口');

-- 第5关：纯前端页面发布
INSERT IGNORE INTO stage_step (stage_id, sort_order, title, description, commands, tips) VALUES
(5, 1, '配置 Git SSH Key', '首次使用需要配置，否则 push 代码会失败。详见 [配置 Git SSH Key](https://km.sankuai.com/collabpage/1465033066)。', '[]', ''),
(5, 2, 'Clone 前端仓库并启动 Claude Code', '前端仓库：[runmap-frontend](https://dev.sankuai.com/code/repo-detail/ulive-pde-explore/runmap-frontend/file/list)，clone 到本地后在项目目录启动 Claude Code。', '["mc --code"]', ''),
(5, 3, '完成需求并提交代码', '告诉 Claude Code 你要做什么，确认没问题后提交代码。', '["git checkout -b feat/你的名字-改动描述", "git add .", "git commit -m \"feat: 你的改动描述\"", "git push origin feat/你的名字-改动描述"]', ''),
(5, 4, '发布到测试环境', '在 [Talos 发布平台](https://talos-better.sankuai.com/) 进行测试环境发布，发布后在泳道环境查看部署效果。', '[]', ''),
(5, 5, '提 PR', '在自己的泳道环境测试没问题后，打开仓库页面提 PR，等小祥 review merge。', '[]', '');

-- 第6关：前后端联调并发布
INSERT IGNORE INTO stage_step (stage_id, sort_order, title, description, commands, tips) VALUES
(6, 1, '查阅后端 API 文档', '查看后端接口说明，了解需要调用哪些接口。后端 API 文档：（待补充）', '[]', ''),
(6, 2, '完成前端改动并联调', '用 Claude Code 完成前端改动，调用指定后端接口实现页面与后端数据的交互。', '["mc --code"]', '前端仓库：[runmap-frontend](https://dev.sankuai.com/code/repo-detail/ulive-pde-explore/runmap-frontend/file/list)'),
(6, 3, '发布并验证效果', '提交代码、发布到测试环境、在泳道验证接口调用与页面展示正常（可参考第5关第4-5步）。', '[]', '');

-- 第7关：前后端全栈开发
INSERT IGNORE INTO stage_step (stage_id, sort_order, title, description, commands, tips) VALUES
(7, 1, '安装后端开发环境', '安装 JDK 17 和 Maven。前端仓库：[runmap-frontend](https://dev.sankuai.com/code/repo-detail/ulive-pde-explore/runmap-frontend/file/list)，后端仓库：[runmap-backend](https://dev.sankuai.com/code/repo-detail/ulive-pde-explore/runmap-backend/file/list)', '["brew install openjdk@17", "java -version", "brew install maven", "mvn -version"]', 'Windows 用户前往 [Adoptium](https://adoptium.net/temurin/releases/?version=17) 下载 JDK 17，前往 [maven.apache.org](https://maven.apache.org/download.cgi) 下载 Maven'),
(7, 2, '确定功能模块需求', '从以下场景任选一个，或自己设计：用户账号管理（列表+搜索+冻结/解冻）、商户入驻系统（表单+状态流转）、或你自己想的任何业务场景。', '[]', ''),
(7, 3, '完成前后端开发并发布', '用 Claude Code 完成前后端代码开发，部署到测试环境，泳道可访问，接口调用与页面展示正常。', '["mc --code"]', '');

-- =============================================
-- 常见问题（暂留占位，后续补充）
-- =============================================
INSERT IGNORE INTO stage_faq (stage_id, question, answer, sort_order) VALUES
(1, 'CatPaw 下载失败怎么办？', '请检查美团内网连接是否正常，或联系小祥。', 1),
(2, 'Homebrew 安装很慢怎么办？', '可以使用国内镜像源加速，或者等待网络稳定后重试。', 1),
(2, 'node -v 显示版本不对怎么办？', '可以使用 nvm 管理多个 Node.js 版本，执行 nvm use 18 切换到正确版本。', 2),
(3, '执行 mc --code 报 Permission denied 怎么办？', '在命令前加 sudo 再试：sudo mc --code。', 1),
(4, '端口 5173 被占用怎么办？', '让 Claude Code 帮你换一个端口，告诉它「帮我把端口改成 5174」即可。', 1),
(5, 'git push 报错没有权限怎么办？', '请先配置 Git SSH Key，参考文档：https://km.sankuai.com/collabpage/1465033066', 1),
(6, '调接口报跨域错误怎么办？', '在泳道环境下前后端同域不会有跨域问题，本地开发可以让 Claude Code 帮你配置代理。', 1),
(7, 'Maven 构建失败怎么办？', '检查 JAVA_HOME 环境变量是否正确指向 JDK 17，执行 echo $JAVA_HOME 查看。', 1);
