-- 关卡配置表
CREATE TABLE IF NOT EXISTS stage (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关卡ID',
    name VARCHAR(32) NOT NULL COMMENT '关卡短名，如第1关',
    title VARCHAR(64) NOT NULL COMMENT '关卡标题',
    description VARCHAR(512) NOT NULL DEFAULT '' COMMENT '关卡描述',
    verify_type VARCHAR(20) NOT NULL DEFAULT 'self' COMMENT '通关验证方式：self/url/paste/branch/manual',
    verify_hint VARCHAR(512) NOT NULL DEFAULT '' COMMENT '验证输入框提示文字',
    sort_order BIGINT NOT NULL DEFAULT 0 COMMENT '排序权重',
    is_active TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否上线：1-上线 0-下线',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关卡配置表';

-- 关卡步骤表
CREATE TABLE IF NOT EXISTS stage_step (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stage_id BIGINT NOT NULL COMMENT '关联关卡ID',
    sort_order BIGINT NOT NULL COMMENT '步骤顺序',
    title VARCHAR(128) NOT NULL COMMENT '步骤标题',
    description VARCHAR(512) NOT NULL DEFAULT '' COMMENT '步骤说明',
    commands TEXT COMMENT '命令列表，JSON数组格式',
    tips VARCHAR(512) NOT NULL DEFAULT '' COMMENT '提示文字',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_stage_id (stage_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关卡步骤表';

-- 关卡常见问题表
CREATE TABLE IF NOT EXISTS stage_faq (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stage_id BIGINT NOT NULL COMMENT '关联关卡ID',
    question TEXT COMMENT '问题',
    answer TEXT COMMENT '答案',
    sort_order BIGINT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_stage_id (stage_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关卡常见问题表';

-- 用户关卡通关记录表（只记录已提交/已通关的关卡）
CREATE TABLE IF NOT EXISTS user_stage_progress (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_mis VARCHAR(64) NOT NULL COMMENT '用户MIS，来自SSO',
    user_org VARCHAR(512) NOT NULL DEFAULT '' COMMENT '末级组织名称，写入时从Org SDK获取',
    user_org_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '末级组织ID，来自emp.getOrgId()',
    stage_id BIGINT NOT NULL COMMENT '关联关卡ID',
    verify_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '验证状态：pending/passed/failed',
    submitted_value VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '用户提交的验证内容（URL/粘贴内容等）',
    passed_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '通关时间，默认值表示尚未通关',
    lane VARCHAR(100) NOT NULL DEFAULT '' COMMENT '用户填写的泳道名',
    url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '用户填写的页面访问地址',
    reviewed_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '审核时间',
    reviewed_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '审核人 mis',
    reject_reason VARCHAR(500) NOT NULL DEFAULT '' COMMENT '拒绝原因',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_user_mis_stage_id (user_mis, stage_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关卡通关记录表';

-- 用户步骤完成记录表（只记录已勾选的步骤）
CREATE TABLE IF NOT EXISTS user_step_progress (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_mis VARCHAR(64) NOT NULL COMMENT '用户MIS，来自SSO',
    user_org VARCHAR(512) NOT NULL DEFAULT '' COMMENT '末级组织名称，写入时从Org SDK获取',
    user_org_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '末级组织ID，来自emp.getOrgId()',
    step_id BIGINT NOT NULL COMMENT '关联步骤ID',
    completed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_user_mis_step_id (user_mis, step_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户步骤完成记录表';

-- 用户活动事件表（用于实时动态历史，多实例共享）
CREATE TABLE IF NOT EXISTS activity_event (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_mis VARCHAR(64) NOT NULL COMMENT '用户MIS',
    user_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户姓名（冗余，避免每次查Org）',
    event_type VARCHAR(20) NOT NULL COMMENT 'start / complete',
    stage_id BIGINT NOT NULL COMMENT '关卡ID',
    stage_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '关卡名称',
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件发生时间',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_occurred_at (occurred_at),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户活动事件表';

-- 讨论区帖子表（帖子 + 回复，parent_id 为 NULL 表示顶层帖子）
CREATE TABLE IF NOT EXISTS stage_discussion_post (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stage_id BIGINT NOT NULL COMMENT '关联关卡ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父帖ID，0表示顶层帖子',
    user_mis VARCHAR(64) NOT NULL COMMENT '发帖人MIS',
    content TEXT COMMENT '帖子内容',
    like_count BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数（冗余，避免每次COUNT）',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_stage_id (stage_id),
    KEY idx_parent_id (parent_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='讨论区帖子表';

-- 讨论区点赞表（帖子/回复点赞去重）
CREATE TABLE IF NOT EXISTS stage_discussion_like (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_mis VARCHAR(64) NOT NULL COMMENT '点赞人MIS',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_post_user (post_id, user_mis),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='讨论区点赞表';

-- 组织信息缓存表（每周定时刷新，供排行榜统计使用）
CREATE TABLE IF NOT EXISTS org_info (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    org_id VARCHAR(64) NOT NULL COMMENT '末级组织ID',
    org_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '末级组织名称',
    emp_count BIGINT NOT NULL DEFAULT 0 COMMENT '在职员工人数（定时刷新）',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_org_id (org_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织信息缓存表';

-- =============================================
-- 初始化数据
-- =============================================

INSERT IGNORE INTO stage (id, name, title, description, verify_type, verify_hint, sort_order, is_active) VALUES
(1, '第1关', '武器初始化', '安装蓝星自研开发平台 Catpaw', 'curl', '', 1, 1),
(2, '第2关', '战斗系统核心激活', '配置 Node.js、Git 等基础开发环境', 'curl', '', 2, 1),
(3, '第3关', 'AI战伴绑定', '安装 Claude Code，绑定你最强的 AI 战伴', 'curl', '', 3, 1),
(4, '第4关', '本地代码跑起来', '运行第一个示例项目', 'self', '', 4, 1),
(5, '第5关', '攻占前线阵地', '把前端代码发布上线，让用户看到它', 'url', '请填写你部署后的访问地址，如 https://your-app.example.com', 5, 1),
(6, '第6关', '前后端联合作战', '完成前后端联调并完整部署', 'url', '请填写你的前后端联调项目访问地址', 6, 1),
(7, '第7关', '终极挑战', '提交真实项目，接受人工审核', 'manual', '请描述你完成的项目，包括功能介绍和访问地址', 7, 1);

INSERT IGNORE INTO stage_step (stage_id, sort_order, title, description, commands, tips) VALUES
(1, 1, '下载CatPaw', '从官方仓库下载CatPaw工具', '["curl -o catpaw https://nocode.meituan.com/catpaw/latest"]', '确保网络连接正常'),
(1, 2, '安装CatPaw', '安装并配置CatPaw到系统路径', '["chmod +x catpaw", "sudo mv catpaw /usr/local/bin/"]', '需要管理员权限'),
(1, 3, '验证安装', '检查CatPaw是否正确安装', '["catpaw --version"]', '应该显示版本号信息'),
(2, 1, '安装Claude Code', '通过npm安装Claude Code', '["npm install -g @anthropic-ai/claude-code"]', '确保已安装Node.js和npm'),
(2, 2, '配置API密钥', '设置Anthropic API密钥', '["export ANTHROPIC_API_KEY=your_api_key_here"]', '请替换为你的实际API密钥'),
(2, 3, '验证安装', '检查Claude Code是否正确安装', '["claude --version"]', '应该显示版本号信息'),
(3, 1, '检查Node.js版本', '验证Node.js环境', '["node --version"]', '需要Node.js 14+版本'),
(3, 2, '检查npm安装', '验证npm包管理器', '["npm --version"]', '确保npm可用'),
(3, 3, '创建测试项目', '初始化一个测试项目', '["mkdir test-project && cd test-project && npm init -y"]', '用于验证环境配置'),
(4, 1, '克隆示例项目', '获取示例代码', '["git clone https://github.com/example/sample-project.git"]', '需要Git环境'),
(4, 2, '安装依赖', '安装项目依赖包', '["cd sample-project && npm install"]', '可能需要一些时间'),
(4, 3, '运行项目', '启动开发服务器', '["npm run dev"]', '访问 http://localhost:3000'),
(5, 1, '构建生产版本', '生成优化后的代码', '["npm run build"]', '会生成dist目录'),
(5, 2, '部署到静态服务器', '上传到CDN或静态服务器', '["scp -r dist/* user@server:/var/www/html"]', '需要服务器访问权限'),
(5, 3, '验证部署', '访问线上地址', '["curl -I https://your-domain.com"]', '检查HTTP状态码'),
(6, 1, '构建前端代码', '准备前端生产版本', '["npm run build"]', '确保环境变量正确'),
(6, 2, '部署后端服务', '启动后端API服务', '["java -jar backend.jar --server.port=8080"]', '需要Java环境'),
(6, 3, '配置反向代理', '设置Nginx代理', '["配置Nginx转发API请求"]', '确保前后端通信正常'),
(7, 1, '设计组件架构', '规划页面组件结构', '["使用Figma或Sketch设计"]', '考虑组件复用性'),
(7, 2, '实现核心功能', '开发主要业务逻辑', '["使用React/Vue实现"]', '关注性能优化'),
(7, 3, '集成测试', '验证功能完整性', '["编写单元测试和E2E测试"]', '确保代码质量');

INSERT IGNORE INTO stage_faq (stage_id, question, answer, sort_order) VALUES
(1, '下载失败怎么办？', '请检查网络连接，或者尝试使用镜像源下载', 1),
(1, '权限不足如何解决？', '请使用sudo命令或者联系系统管理员', 2),
(2, 'npm安装失败怎么办？', '请检查网络设置，尝试使用淘宝镜像源：npm config set registry https://registry.npmmirror.com', 1),
(2, 'API密钥在哪里获取？', '访问Anthropic官网注册账号获取API密钥', 2),
(3, 'Node.js版本过低怎么办？', '请升级Node.js到14+版本，可以使用nvm管理多个版本', 1),
(4, 'Git克隆失败怎么办？', '请检查网络连接和Git配置，确保有访问权限', 1),
(5, '部署后页面空白怎么办？', '请检查文件路径是否正确，以及index.html的base路径配置', 1),
(6, '跨域问题如何解决？', '在后端配置CORS，或者使用Nginx反向代理解决跨域问题', 1),
(7, '性能优化有哪些方法？', '可以使用代码分割、懒加载、缓存策略等方法优化性能', 1);

-- 悬赏任务表
CREATE TABLE IF NOT EXISTS bounty_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    description TEXT COMMENT '任务详情，支持Markdown',
    type VARCHAR(20) NOT NULL DEFAULT 'official' COMMENT '任务类型：official-官方任务 idea-用户提案',
    status VARCHAR(30) NOT NULL DEFAULT 'draft' COMMENT '任务状态：draft/design_open/design_voting/design_closed/dev_open/closed',
    created_by VARCHAR(64) NOT NULL COMMENT '发布人mis',
    design_deadline DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '设计阶段截止时间',
    voting_end DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '投票截止时间',
    dev_deadline DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '开发阶段截止时间',
    dev_min_stage BIGINT NOT NULL DEFAULT 5 COMMENT '参与开发的最低通关关卡数，5=纯前端，7=前后端',
    reward_desc VARCHAR(500) NOT NULL DEFAULT '' COMMENT '奖励描述',
    cover_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '封面图URL（创意用）',
    ref_link VARCHAR(500) NOT NULL DEFAULT '' COMMENT '参考链接（创意用）',
    like_count BIGINT NOT NULL DEFAULT 0 COMMENT '点赞数（冗余）',
    is_featured TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否被翻牌（仅idea类型）：0-否 1-是',
    featured_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '翻牌人mis',
    featured_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '翻牌时间',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_type_status (type, status),
    KEY idx_created_by (created_by),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='悬赏任务表';

-- 悬赏任务提交表
CREATE TABLE IF NOT EXISTS bounty_submission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '关联任务ID',
    phase VARCHAR(20) NOT NULL COMMENT '提交阶段：design-设计阶段 dev-开发阶段',
    user_mis VARCHAR(64) NOT NULL COMMENT '提交人mis',
    title VARCHAR(200) NOT NULL COMMENT '提交标题',
    url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '上线地址（开发阶段）或S3链接（设计阶段）',
    repo_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '代码仓库链接（开发阶段）',
    description TEXT COMMENT '说明内容，支持Markdown',
    status VARCHAR(20) NOT NULL DEFAULT 'submitted' COMMENT '审核状态：submitted/approved/rejected',
    reject_reason VARCHAR(500) NOT NULL DEFAULT '' COMMENT '拒绝原因',
    is_winner TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否获选winner：0-否 1-是',
    vote_count BIGINT NOT NULL DEFAULT 0 COMMENT '得票数（冗余字段，仅设计阶段有效）',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_task_phase_mis (task_id, phase, user_mis),
    KEY idx_user_mis (user_mis),
    KEY idx_vote_count (vote_count),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='悬赏任务提交表';

-- 悬赏任务投票表（仅设计阶段）
CREATE TABLE IF NOT EXISTS bounty_vote (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '关联任务ID',
    submission_id BIGINT NOT NULL COMMENT '投票的提交ID',
    user_mis VARCHAR(64) NOT NULL COMMENT '投票人mis',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_task_sub_mis (task_id, submission_id, user_mis),
    KEY idx_task_user (task_id, user_mis),
    KEY idx_submission_id (submission_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='悬赏任务投票表';

-- 悬赏共建成员表
CREATE TABLE IF NOT EXISTS bounty_member (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_mis VARCHAR(64) NOT NULL COMMENT '用户MIS',
    user_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户姓名（冗余）',
    dept VARCHAR(200) NOT NULL DEFAULT '' COMMENT '部门',
    role VARCHAR(30) NOT NULL COMMENT '角色：pm/design/fe/be/qa/other',
    custom_role VARCHAR(100) NOT NULL DEFAULT '' COMMENT '自定义角色（选other时填写）',
    reason VARCHAR(500) NOT NULL DEFAULT '' COMMENT '加入理由',
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    add_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_user_mis (user_mis),
    KEY idx_role (role),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='悬赏共建成员表';
