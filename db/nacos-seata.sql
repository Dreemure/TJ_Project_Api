-- ============================================================================
-- Seata 2.5.0 + Nacos 3.1.1 初始化脚本
--
-- 该脚本由 docker-compose 挂载到 MySQL 的 docker-entrypoint-initdb.d，
-- 仅在数据卷为空（首次初始化）时执行。
-- 若数据卷已存在而需要补表，请手动执行对应 DDL（见 README 说明）。
--
-- ⚠️ Nacos 版本必须与 spring-cloud-alibaba 匹配：
--    spring-cloud-alibaba 2025.1.0.0 -> nacos-client 3.1.1 -> 服务端 Nacos 3.1.1
--    下面使用的是 Nacos 3.1.1 的官方 mysql-schema.sql 结构。
-- ============================================================================

CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4;
USE seata;

CREATE TABLE IF NOT EXISTS global_table (
    xid VARCHAR(128) NOT NULL,
    transaction_id BIGINT,
    status TINYINT NOT NULL,
    application_id VARCHAR(32),
    transaction_service_group VARCHAR(32),
    transaction_name VARCHAR(128),
    timeout INT,
    begin_time BIGINT,
    application_data VARCHAR(2000),
    gmt_create DATETIME,
    gmt_modified DATETIME,
    PRIMARY KEY (xid),
    KEY idx_status_gmt_modified (status, gmt_modified),
    KEY idx_transaction_id (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS branch_table (
    branch_id BIGINT NOT NULL,
    xid VARCHAR(128) NOT NULL,
    transaction_id BIGINT,
    resource_group_id VARCHAR(32),
    resource_id VARCHAR(256),
    branch_type VARCHAR(8),
    status TINYINT,
    client_id VARCHAR(64),
    application_data VARCHAR(2000),
    gmt_create DATETIME(6),
    gmt_modified DATETIME(6),
    PRIMARY KEY (branch_id),
    KEY idx_xid (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lock_table (
    row_key VARCHAR(128) NOT NULL,
    xid VARCHAR(128),
    transaction_id BIGINT,
    branch_id BIGINT NOT NULL,
    resource_id VARCHAR(256),
    table_name VARCHAR(32),
    pk VARCHAR(36),
    status TINYINT NOT NULL DEFAULT 0,
    gmt_create DATETIME,
    gmt_modified DATETIME,
    PRIMARY KEY (row_key),
    KEY idx_branch_id (branch_id),
    KEY idx_xid (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS distributed_lock (
    lock_key CHAR(20) NOT NULL,
    lock_value VARCHAR(20) NOT NULL,
    expire BIGINT,
    PRIMARY KEY (lock_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO distributed_lock (lock_key, lock_value, expire) VALUES
    ('AsyncCommitting', ' ', 0),
    ('RetryCommitting', ' ', 0),
    ('RetryRollbacking', ' ', 0),
    ('TxTimeoutCheck', ' ', 0);

-- ============================================================================
-- Nacos 3.1.1 schema
-- ============================================================================

CREATE DATABASE IF NOT EXISTS nacos DEFAULT CHARACTER SET utf8mb4;
USE nacos;

CREATE TABLE IF NOT EXISTS config_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    data_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(128) DEFAULT NULL,
    content LONGTEXT NOT NULL,
    md5 VARCHAR(32) DEFAULT NULL,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    src_user TEXT,
    src_ip VARCHAR(50) DEFAULT NULL,
    app_name VARCHAR(128) DEFAULT NULL,
    tenant_id VARCHAR(128) DEFAULT '',
    c_desc VARCHAR(256) DEFAULT NULL,
    c_use VARCHAR(64) DEFAULT NULL,
    effect VARCHAR(64) DEFAULT NULL,
    type VARCHAR(64) DEFAULT NULL,
    c_schema TEXT,
    encrypted_data_key VARCHAR(1024) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    UNIQUE KEY uk_configinfo_datagrouptenant (data_id, group_id, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Nacos 2.5.0 起引入的灰度发布配置表
CREATE TABLE IF NOT EXISTS config_info_gray (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    data_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(128) NOT NULL,
    content LONGTEXT NOT NULL,
    md5 VARCHAR(32) DEFAULT NULL,
    src_user TEXT,
    src_ip VARCHAR(100) DEFAULT NULL,
    gmt_create DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    gmt_modified DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    app_name VARCHAR(128) DEFAULT NULL,
    tenant_id VARCHAR(128) DEFAULT '',
    gray_name VARCHAR(128) NOT NULL,
    gray_rule TEXT NOT NULL,
    encrypted_data_key VARCHAR(256) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    UNIQUE KEY uk_configinfogray_datagrouptenantgray (data_id, group_id, tenant_id, gray_name),
    KEY idx_dataid_gmt_modified (data_id, gmt_modified),
    KEY idx_gmt_modified (gmt_modified)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS config_tags_relation (
    id BIGINT NOT NULL,
    tag_name VARCHAR(128) NOT NULL,
    tag_type VARCHAR(64) DEFAULT NULL,
    data_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(128) DEFAULT '',
    nid BIGINT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (nid),
    UNIQUE KEY uk_configtagrelation_configidtag (id, tag_name, tag_type),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS group_capacity (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    group_id VARCHAR(128) NOT NULL DEFAULT '',
    quota INT UNSIGNED NOT NULL DEFAULT 0,
    `usage` INT UNSIGNED NOT NULL DEFAULT 0,
    max_size INT UNSIGNED NOT NULL DEFAULT 0,
    max_aggr_count INT UNSIGNED NOT NULL DEFAULT 0,
    max_aggr_size INT UNSIGNED NOT NULL DEFAULT 0,
    max_history_count INT UNSIGNED NOT NULL DEFAULT 0,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS his_config_info (
    id BIGINT UNSIGNED NOT NULL,
    nid BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    data_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(128) NOT NULL,
    app_name VARCHAR(128) DEFAULT NULL,
    content LONGTEXT NOT NULL,
    md5 VARCHAR(32) DEFAULT NULL,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    src_user TEXT,
    src_ip VARCHAR(50) DEFAULT NULL,
    op_type CHAR(10) DEFAULT NULL,
    tenant_id VARCHAR(128) DEFAULT '',
    encrypted_data_key VARCHAR(1024) NOT NULL DEFAULT '',
    publish_type VARCHAR(50) DEFAULT 'formal',
    gray_name VARCHAR(50) DEFAULT NULL,
    ext_info LONGTEXT,
    PRIMARY KEY (nid),
    KEY idx_gmt_create (gmt_create),
    KEY idx_gmt_modified (gmt_modified),
    KEY idx_did (data_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tenant_capacity (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(128) NOT NULL DEFAULT '',
    quota INT UNSIGNED NOT NULL DEFAULT 0,
    `usage` INT UNSIGNED NOT NULL DEFAULT 0,
    max_size INT UNSIGNED NOT NULL DEFAULT 0,
    max_aggr_count INT UNSIGNED NOT NULL DEFAULT 0,
    max_aggr_size INT UNSIGNED NOT NULL DEFAULT 0,
    max_history_count INT UNSIGNED NOT NULL DEFAULT 0,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tenant_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    kp VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(128) DEFAULT '',
    tenant_name VARCHAR(128) DEFAULT '',
    tenant_desc VARCHAR(256) DEFAULT NULL,
    create_source VARCHAR(32) DEFAULT NULL,
    gmt_create BIGINT NOT NULL,
    gmt_modified BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_info_kptenantid (kp, tenant_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Nacos 3.x 默认管理员账号（bcrypt: nacos）
CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50) NOT NULL,
    password VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL,
    PRIMARY KEY (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS roles (
    username VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    UNIQUE KEY idx_user_role (username, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS permissions (
    role VARCHAR(50) NOT NULL,
    resource VARCHAR(128) NOT NULL,
    action VARCHAR(8) NOT NULL,
    UNIQUE KEY uk_role_permission (role, resource, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO users (username, password, enabled) VALUES
    ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', TRUE);
INSERT IGNORE INTO roles (username, role) VALUES ('nacos', 'ROLE_ADMIN');
