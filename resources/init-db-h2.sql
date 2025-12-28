-- 保险理赔系统数据库初始化脚本 (H2 Database 兼容版本)

-- 创建用户表
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    department VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建角色表
CREATE TABLE IF NOT EXISTS app_role (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户角色关联表
CREATE TABLE IF NOT EXISTS user_role (
    user_id UUID REFERENCES app_user(id) ON DELETE CASCADE,
    role_id UUID REFERENCES app_role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 创建保单表
CREATE TABLE IF NOT EXISTS insurance_policy (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    policy_number VARCHAR(50) UNIQUE NOT NULL,
    policy_holder_name VARCHAR(100) NOT NULL,
    policy_holder_phone VARCHAR(20),
    policy_holder_email VARCHAR(100),
    policy_type VARCHAR(50) NOT NULL, -- 车险、财产险、人身险等
    coverage_amount DECIMAL(12,2) NOT NULL,
    premium_amount DECIMAL(10,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建理赔案件表
CREATE TABLE IF NOT EXISTS claim_case (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    case_instance_id VARCHAR(64), -- Flowable Case 实例 ID
    policy_id UUID REFERENCES insurance_policy(id),
    claim_number VARCHAR(50) UNIQUE NOT NULL,
    claimant_name VARCHAR(100) NOT NULL,
    claimant_phone VARCHAR(20),
    claimant_email VARCHAR(100),
    incident_date DATE NOT NULL,
    incident_location VARCHAR(200),
    incident_description VARCHAR(2000),
    claimed_amount DECIMAL(12,2) NOT NULL,
    claim_type VARCHAR(50) NOT NULL, -- 事故、盗窃、自然灾害等
    severity VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, UNDER_REVIEW, APPROVED, REJECTED, PAID, CLOSED
    assigned_to UUID REFERENCES app_user(id),
    created_by UUID REFERENCES app_user(id), -- 允许为 NULL，由代码层控制
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建理赔文档表
CREATE TABLE IF NOT EXISTS claim_document (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    claim_id UUID REFERENCES claim_case(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL, -- 照片、报告、收据、身份证等
    document_name VARCHAR(200) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_by UUID REFERENCES app_user(id),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建理赔历史记录表
CREATE TABLE IF NOT EXISTS claim_history (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    claim_id UUID REFERENCES claim_case(id) ON DELETE CASCADE,
    action VARCHAR(100) NOT NULL, -- CREATED, UPDATED, ASSIGNED, APPROVED, REJECTED, PAID, CLOSED
    description VARCHAR(2000),
    performed_by UUID REFERENCES app_user(id),
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_app_user_username ON app_user(username);
CREATE INDEX IF NOT EXISTS idx_app_user_email ON app_user(email);
CREATE INDEX IF NOT EXISTS idx_insurance_policy_policy_number ON insurance_policy(policy_number);
CREATE INDEX IF NOT EXISTS idx_insurance_policy_policy_holder ON insurance_policy(policy_holder_name);
CREATE INDEX IF NOT EXISTS idx_claim_case_claim_number ON claim_case(claim_number);
CREATE INDEX IF NOT EXISTS idx_claim_case_policy_id ON claim_case(policy_id);
CREATE INDEX IF NOT EXISTS idx_claim_case_status ON claim_case(status);
CREATE INDEX IF NOT EXISTS idx_claim_case_created_at ON claim_case(created_at);
CREATE INDEX IF NOT EXISTS idx_claim_document_claim_id ON claim_document(claim_id);
CREATE INDEX IF NOT EXISTS idx_claim_history_claim_id ON claim_history(claim_id);

-- Truncate Flowable tables first (before app tables due to foreign keys)
-- Note: Only truncate tables that exist (Flowable 7.x may have changed schema)
-- H2 doesn't support IF EXISTS with TRUNCATE, so we execute directly and ignore errors

-- BPMN runtime tables (if they exist)
TRUNCATE TABLE act_ru_identitylink CASCADE;
TRUNCATE TABLE act_ru_variable CASCADE;
TRUNCATE TABLE act_ru_task CASCADE;
TRUNCATE TABLE act_ru_execution CASCADE;
TRUNCATE TABLE act_ru_job CASCADE;
TRUNCATE TABLE act_ru_timer_job CASCADE;
TRUNCATE TABLE act_ru_suspended_job CASCADE;
TRUNCATE TABLE act_ru_deadletter_job CASCADE;
TRUNCATE TABLE act_ru_event_subscr CASCADE;

-- BPMN history tables (if they exist)
TRUNCATE TABLE act_hi_identitylink CASCADE;
TRUNCATE TABLE act_hi_varinst CASCADE;
TRUNCATE TABLE act_hi_taskinst CASCADE;
TRUNCATE TABLE act_hi_actinst CASCADE;
TRUNCATE TABLE act_hi_procinst CASCADE;
TRUNCATE TABLE act_hi_detail CASCADE;
TRUNCATE TABLE act_hi_comment CASCADE;
TRUNCATE TABLE act_hi_attachment CASCADE;

-- CMMN tables (if they exist)
TRUNCATE TABLE act_hi_caseinst CASCADE;
TRUNCATE TABLE act_hi_mil_inst CASCADE;
TRUNCATE TABLE act_ru_case_sentry_part CASCADE;
TRUNCATE TABLE act_ru_mil_execution CASCADE;
TRUNCATE TABLE act_ru_plan_item_instance CASCADE;
TRUNCATE TABLE act_ru_case_execution CASCADE;

-- DMN tables (if they exist)
TRUNCATE TABLE act_dmn_hi_decision CASCADE;

-- Truncate application tables
TRUNCATE TABLE user_role, claim_history, claim_document, claim_case, insurance_policy, app_user, app_role CASCADE;

-- Insert default roles
INSERT INTO app_role (name, description) VALUES 
('ADMIN', 'System Administrator'),
('MANAGER', 'Manager'),
('CLAIM_HANDLER', 'Claim Handler'),
('APPROVER', 'Approver'),
('FINANCE', 'Finance'),
('USER', 'Regular User');

-- Insert default users (password: admin for all users, BCrypt encrypted)
INSERT INTO app_user (username, password, first_name, last_name, email, department) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'System', 'Admin', 'admin@flowable-demo.com', 'IT'),
('handler1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Handler', 'One', 'handler1@flowable-demo.com', 'Claims'),
('auditor1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Auditor', 'One', 'auditor1@flowable-demo.com', 'Audit'),
('manager1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Manager', 'One', 'manager1@flowable-demo.com', 'Management');

-- Assign roles
INSERT INTO user_role (user_id, role_id) 
SELECT u.id, r.id FROM app_user u, app_role r 
WHERE u.username = 'admin' AND r.name = 'ADMIN';

INSERT INTO user_role (user_id, role_id) 
SELECT u.id, r.id FROM app_user u, app_role r 
WHERE u.username = 'handler1' AND r.name = 'CLAIM_HANDLER';

INSERT INTO user_role (user_id, role_id) 
SELECT u.id, r.id FROM app_user u, app_role r 
WHERE u.username = 'auditor1' AND r.name = 'APPROVER';

INSERT INTO user_role (user_id, role_id) 
SELECT u.id, r.id FROM app_user u, app_role r 
WHERE u.username = 'manager1' AND r.name = 'MANAGER';

-- Insert insurance policy data from DataInitializer
-- Note: H2 uses DATE_ADD function instead of INTERVAL
INSERT INTO insurance_policy (policy_number, policy_holder_name, policy_holder_phone, policy_holder_email, policy_type, coverage_amount, premium_amount, start_date, end_date, status) VALUES 
-- 车险保单
('CAR2024001', '张三', '13800138001', 'zhangsan@example.com', '车险', 200000.00, 3000.00, DATEADD('MONTH', -3, CURRENT_DATE), DATEADD('MONTH', 9, CURRENT_DATE), 'ACTIVE'),
('CAR2024002', '李四', '13800138002', 'lisi@example.com', '车险', 150000.00, 2500.00, DATEADD('MONTH', -1, CURRENT_DATE), DATEADD('MONTH', 11, CURRENT_DATE), 'ACTIVE'),
-- 人寿保险保单
('LIFE2024001', '王五', '13800138003', 'wangwu@example.com', '人寿保险', 500000.00, 8000.00, DATEADD('MONTH', -6, CURRENT_DATE), DATEADD('MONTH', 54, CURRENT_DATE), 'ACTIVE'),
('LIFE2024002', '赵六', '13800138004', 'zhaoliu@example.com', '人寿保险', 1000000.00, 15000.00, DATEADD('MONTH', -2, CURRENT_DATE), DATEADD('MONTH', 46, CURRENT_DATE), 'ACTIVE'),
-- 健康保险保单
('HEALTH2024001', '钱七', '13800138005', 'qianqi@example.com', '健康保险', 300000.00, 5000.00, DATEADD('MONTH', -4, CURRENT_DATE), DATEADD('MONTH', 8, CURRENT_DATE), 'ACTIVE'),
('HEALTH2024002', '孙八', '13800138006', 'sunba@example.com', '健康保险', 200000.00, 3500.00, DATEADD('MONTH', -5, CURRENT_DATE), DATEADD('MONTH', 7, CURRENT_DATE), 'ACTIVE'),
-- 财产保险保单
('PROP2024001', '周九', '13800138007', 'zhoujiu@example.com', '财产保险', 800000.00, 6000.00, DATEADD('MONTH', -3, CURRENT_DATE), DATEADD('MONTH', 9, CURRENT_DATE), 'ACTIVE'),
-- 意外险保单
('ACC2024001', '吴十', '13800138008', 'wushi@example.com', '意外险', 100000.00, 1200.00, DATEADD('MONTH', -2, CURRENT_DATE), DATEADD('MONTH', 10, CURRENT_DATE), 'ACTIVE');

COMMIT;
