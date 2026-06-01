-- 修复 bounty_vote 唯一约束
-- 原约束 (task_id, user_mis) 只允许每人每任务投一票，与业务要求（每人3票）冲突
-- 改为 (task_id, submission_id, user_mis)：同一任务同一方案每人只能投一次，允许投多个方案

ALTER TABLE bounty_vote
    DROP INDEX uniq_task_user,
    ADD UNIQUE KEY uniq_task_submission_user (task_id, submission_id, user_mis),
    ADD KEY idx_task_user (task_id, user_mis);
