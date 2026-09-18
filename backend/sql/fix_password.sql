-- 修复演示账号的 BCrypt 密码为 123456
UPDATE `user` SET password = '$2b$10$aXKY2uk.rmhxeNTvlygNzerTk7RbQPv7h.SQBauehgoVmtTxW3B22' WHERE username IN ('admin','teacher01','student01');
