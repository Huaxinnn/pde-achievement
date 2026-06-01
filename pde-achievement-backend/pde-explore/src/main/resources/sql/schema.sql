-- 关卡配置表
CREATE TABLE IF NOT EXISTS stage (
    id INTEGER PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    title VARCHAR(64) NOT NULL,
    description VARCHAR(512) NOT NULL DEFAULT '',
    verify_type VARCHAR(20) NOT NULL DEFAULT 'self',
    verify_hint VARCHAR(512) NOT NULL DEFAULT '',
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active INTEGER NOT NULL DEFAULT 1,
    deleted_flag INTEGER NOT NULL DEFAULT 0,
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 关卡步骤表
CREATE TABLE IF NOT EXISTS stage_step (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    stage_id INTEGER NOT NULL,
    sort_order INTEGER NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(512) NOT NULL DEFAULT '',
    commands TEXT,
    tips VARCHAR(512) NOT NULL DEFAULT '',
    deleted_flag INTEGER NOT NULL DEFAULT 0,
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (stage_id) REFERENCES stage(id)
);

-- 关卡常见问题表
CREATE TABLE IF NOT EXISTS stage_faq (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    stage_id INTEGER NOT NULL,
    question TEXT,
    answer TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    deleted_flag INTEGER NOT NULL DEFAULT 0,
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (stage_id) REFERENCES stage(id)
);

-- 用户关卡通关记录表（只记录已提交/已通关的关卡）
CREATE TABLE IF NOT EXISTS user_stage_progress (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_mis VARCHAR(64) NOT NULL,
    user_org VARCHAR(512) NOT NULL DEFAULT '',
    user_org_id VARCHAR(64) NOT NULL DEFAULT '',
    stage_id INTEGER NOT NULL,
    verify_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    submitted_value VARCHAR(2000) NOT NULL DEFAULT '',
    passed_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
    lane VARCHAR(100) NOT NULL DEFAULT '',
    url VARCHAR(500) NOT NULL DEFAULT '',
    reviewed_at DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
    reviewed_by VARCHAR(64) NOT NULL DEFAULT '',
    reject_reason VARCHAR(500) NOT NULL DEFAULT '',
    deleted_flag INTEGER NOT NULL DEFAULT 0,
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_mis, stage_id)
);

-- 用户步骤完成记录表（只记录已勾选的步骤）
CREATE TABLE IF NOT EXISTS user_step_progress (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_mis VARCHAR(64) NOT NULL,
    user_org VARCHAR(512) NOT NULL DEFAULT '',
    user_org_id VARCHAR(64) NOT NULL DEFAULT '',
    step_id INTEGER NOT NULL,
    completed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted_flag INTEGER NOT NULL DEFAULT 0,
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_mis, step_id)
);

-- 讨论区帖子表（帖子 + 回复，parent_id = 0 表示顶层帖子）
CREATE TABLE IF NOT EXISTS stage_discussion_post (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    stage_id INTEGER NOT NULL,
    parent_id INTEGER NOT NULL DEFAULT 0,
    user_mis VARCHAR(64) NOT NULL,
    content TEXT,
    like_count INTEGER NOT NULL DEFAULT 0,
    deleted_flag INTEGER NOT NULL DEFAULT 0,
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 讨论区点赞表（帖子/回复点赞去重）
CREATE TABLE IF NOT EXISTS stage_discussion_like (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER NOT NULL,
    user_mis VARCHAR(64) NOT NULL,
    deleted_flag INTEGER NOT NULL DEFAULT 0,
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_mis)
);

CREATE INDEX IF NOT EXISTS idx_stage_step_stage ON stage_step(stage_id);
CREATE INDEX IF NOT EXISTS idx_stage_faq_stage ON stage_faq(stage_id);
CREATE INDEX IF NOT EXISTS idx_user_stage_progress_mis ON user_stage_progress(user_mis);
CREATE INDEX IF NOT EXISTS idx_user_step_progress_mis ON user_step_progress(user_mis);

-- 终端命令校验上报记录表（第1-3关 curl 类型校验使用）
CREATE TABLE IF NOT EXISTS verify_checkin_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_mis VARCHAR(64) NOT NULL,
    stage_id INTEGER NOT NULL,
    version_info VARCHAR(512) NOT NULL DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_verify_checkin_mis_stage ON verify_checkin_log(user_mis, stage_id, created_at);

-- 组织信息缓存表（每周定时刷新，供排行榜统计使用）
CREATE TABLE IF NOT EXISTS org_info (
    org_id VARCHAR(64) NOT NULL PRIMARY KEY,
    org_name VARCHAR(128) NOT NULL DEFAULT '',
    emp_count INTEGER NOT NULL DEFAULT 0,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
