-- =============================================================
-- 客户拜访管理系统 · MySQL 8.0 表结构
-- 说明：首次启动后端会自动建演示数据（可用 INIT_DEMO=false 关闭）
-- =============================================================
CREATE DATABASE IF NOT EXISTS `customer_visit`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `customer_visit`;

-- 用户：ADMIN 管理者 / MANAGER 客户经理
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`   VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `real_name`  VARCHAR(50)  NOT NULL COMMENT '姓名',
  `password`   VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
  `role`       VARCHAR(20)  NOT NULL COMMENT 'ADMIN / MANAGER',
  `enabled`    TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_role` (`role`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号表';

-- 集团主数据（编码唯一，名称唯一维护，避免同码异名）
CREATE TABLE IF NOT EXISTS `customer_group` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `code`       VARCHAR(30)  NOT NULL COMMENT '集团编码',
  `name`       VARCHAR(120) NOT NULL COMMENT '集团名称',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_code` (`code`),
  KEY `idx_group_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集团主数据';

-- 拜访计划：某客户经理在某个年月负责拜访某个集团
CREATE TABLE IF NOT EXISTS `visit_plan` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `manager_id` BIGINT       NOT NULL COMMENT '客户经理 sys_user.id',
  `group_id`   BIGINT       NOT NULL COMMENT '集团 customer_group.id',
  `plan_month` CHAR(7)      NOT NULL COMMENT '计划月份 YYYY-MM',
  `remark`     VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan` (`manager_id`, `group_id`, `plan_month`),
  KEY `idx_plan_month` (`plan_month`),
  KEY `idx_plan_group` (`group_id`),
  CONSTRAINT `fk_plan_manager` FOREIGN KEY (`manager_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_group`   FOREIGN KEY (`group_id`)   REFERENCES `customer_group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拜访计划（客户经理-集团-月份）';

-- 拜访记录：一条计划最多一条记录（plan_id 唯一）
CREATE TABLE IF NOT EXISTS `visit_record` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `plan_id`    BIGINT        NOT NULL,
  `visit_date` DATE          NOT NULL COMMENT '实际拜访日期',
  `method`     VARCHAR(20)   NOT NULL COMMENT '上门拜访/电话沟通/视频会议/客户到访',
  `result`     VARCHAR(30)   NOT NULL COMMENT '拜访顺利/初步意向/需再次跟进/暂无意向/未见到负责人',
  `remark`     VARCHAR(1000) DEFAULT NULL COMMENT '拜访记录',
  `created_by` BIGINT        DEFAULT NULL COMMENT '提交人 sys_user.id',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_plan` (`plan_id`),
  KEY `idx_record_date` (`visit_date`),
  CONSTRAINT `fk_record_plan` FOREIGN KEY (`plan_id`) REFERENCES `visit_plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拜访记录';
