-- 创建数据库
CREATE DATABASE IF NOT EXISTS manage_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE manage_system;

-- 删除已存在的表（注意顺序：先删除有外键约束的表）
DROP TABLE IF EXISTS `repayment_record`;
DROP TABLE IF EXISTS `repayment_plan`;
DROP TABLE IF EXISTS `loan`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `file`;

-- 创建文件表
CREATE TABLE `file` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `file_name` VARCHAR(255) NOT NULL,
    `file_path` VARCHAR(255) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `file_type` VARCHAR(100),
    `upload_time` DATETIME NOT NULL,
    `uploader` VARCHAR(100) NOT NULL
);

-- 插入初期数据
INSERT INTO `file` (`file_name`, `file_path`, `file_size`, `file_type`, `upload_time`, `uploader`) VALUES
('示例文件1.txt', 'uploads/1620000000000_示例文件1.txt', 1024, 'text/plain', NOW(), 'admin'),
('示例文件2.pdf', 'uploads/1620000000001_示例文件2.pdf', 2048, 'application/pdf', NOW(), 'admin'),
('示例文件3.jpg', 'uploads/1620000000002_示例文件3.jpg', 3072, 'image/jpeg', NOW(), 'user1');

-- 创建用户表（预留）
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(100) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `role` VARCHAR(50) NOT NULL,
    `credit_score` INT DEFAULT 800 -- 用户信誉度
);

-- 插入初期用户数据
INSERT INTO `user` (`username`, `password`, `name`, `role`, `credit_score`) VALUES
('admin', 'admin123', '管理员', 'ADMIN', 900),
('user1', 'user123', '用户1', 'USER', 850),
('user2', 'user123', '用户2', 'USER', 780);

-- 创建贷款表
CREATE TABLE `loan` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `loan_amount` DECIMAL(12,2) NOT NULL,
    `interest_rate` DECIMAL(5,2) NOT NULL,
    `loan_term` INT NOT NULL, -- 贷款期限（月）
    `loan_date` DATE NOT NULL,
    `status` VARCHAR(50) NOT NULL, -- 状态：申请中、已批准、已拒绝、已还款
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- 创建还款计划表
CREATE TABLE `repayment_plan` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `loan_id` BIGINT NOT NULL,
    `installment_number` INT NOT NULL, -- 期数
    `due_date` DATE NOT NULL, -- 到期日期
    `principal` DECIMAL(12,2) NOT NULL, -- 本金
    `interest` DECIMAL(12,2) NOT NULL, -- 利息
    `total_amount` DECIMAL(12,2) NOT NULL, -- 总金额
    `status` VARCHAR(50) NOT NULL, -- 状态：未还款、已还款、逾期
    FOREIGN KEY (`loan_id`) REFERENCES `loan`(`id`)
);

-- 创建还款记录表
CREATE TABLE `repayment_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `loan_id` BIGINT NOT NULL,
    `plan_id` BIGINT NOT NULL,
    `payment_date` DATE NOT NULL, -- 还款日期
    `amount` DECIMAL(12,2) NOT NULL, -- 还款金额
    `payment_method` VARCHAR(100), -- 还款方式
    FOREIGN KEY (`loan_id`) REFERENCES `loan`(`id`),
    FOREIGN KEY (`plan_id`) REFERENCES `repayment_plan`(`id`)
);

-- 插入初期贷款数据
INSERT INTO `loan` (`user_id`, `loan_amount`, `interest_rate`, `loan_term`, `loan_date`, `status`) VALUES
(2, 10000.00, 4.5, 12, NOW(), '已批准'),
(3, 50000.00, 4.2, 24, NOW(), '已批准');

-- 插入初期还款计划数据
INSERT INTO `repayment_plan` (`loan_id`, `installment_number`, `due_date`, `principal`, `interest`, `total_amount`, `status`) VALUES
(1, 1, DATE_ADD(NOW(), INTERVAL 1 MONTH), 833.33, 37.50, 870.83, '未还款'),
(1, 2, DATE_ADD(NOW(), INTERVAL 2 MONTH), 833.33, 34.38, 867.71, '未还款'),
(1, 3, DATE_ADD(NOW(), INTERVAL 3 MONTH), 833.33, 31.25, 864.58, '未还款'),
(2, 1, DATE_ADD(NOW(), INTERVAL 1 MONTH), 2083.33, 175.00, 2258.33, '未还款'),
(2, 2, DATE_ADD(NOW(), INTERVAL 2 MONTH), 2083.33, 170.83, 2254.16, '未还款');