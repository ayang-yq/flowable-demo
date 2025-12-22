-- 保险理赔系统数据库初始化脚本

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 创建用户表
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
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
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    case_instance_id VARCHAR(64), -- Flowable Case 实例 ID
    policy_id UUID REFERENCES insurance_policy(id),
    claim_number VARCHAR(50) UNIQUE NOT NULL,
    claimant_name VARCHAR(100) NOT NULL,
    claimant_phone VARCHAR(20),
    claimant_email VARCHAR(100),
    incident_date DATE NOT NULL,
    incident_location VARCHAR(200),
    incident_description TEXT,
    claimed_amount DECIMAL(12,2) NOT NULL,
    claim_type VARCHAR(50) NOT NULL, -- 事故、盗窃、自然灾害等
    severity VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, UNDER_REVIEW, APPROVED, REJECTED, PAID, CLOSED
    assigned_to UUID REFERENCES app_user(id),
    created_by UUID REFERENCES app_user(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建理赔文档表
CREATE TABLE IF NOT EXISTS claim_document (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    claim_id UUID REFERENCES claim_case(id) ON DELETE CASCADE,
    action VARCHAR(100) NOT NULL, -- CREATED, UPDATED, ASSIGNED, APPROVED, REJECTED, PAID, CLOSED
    description TEXT,
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

-- 插入默认角色
INSERT INTO app_role (name, description) VALUES 
('ADMIN', '系统管理员'),
('CLAIM_HANDLER', '理赔处理员'),
('CLAIM_AUDITOR', '理赔审核员'),
('CLAIM_MANAGER', '理赔经理')
ON CONFLICT (name) DO NOTHING;

-- 插入默认用户（密码为：password，使用 BCrypt 加密）
INSERT INTO app_user (username, password, first_name, last_name, email, department) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '系统', '管理员', 'admin@flowable-demo.com', 'IT'),
('handler1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '张', '三', 'handler1@flowable-demo.com', '理赔部'),
('auditor1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '李', '四', 'auditor1@flowable-demo.com', '审核部'),
('manager1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '王', '五', 'manager1@flowable-demo.com', '管理部')
ON CONFLICT (username) DO NOTHING;

-- 分配角色
INSERT INTO user_role (user_id, role_id) 
SELECT u.id, r.id FROM app_user u, app_role r 
WHERE u.username = 'admin' AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_role (user_id, role_id) 
SELECT u.id, r.id FROM app_user u, app_role r 
WHERE u.username = 'handler1' AND r.name = 'CLAIM_HANDLER'
ON CONFLICT DO NOTHING;

INSERT INTO user_role (user_id, role_id) 
SELECT u.id, r.id FROM app_user u, app_role r 
WHERE u.username = 'auditor1' AND r.name = 'CLAIM_AUDITOR'
ON CONFLICT DO NOTHING;

INSERT INTO user_role (user_id, role_id) 
SELECT u.id, r.id FROM app_user u, app_role r 
WHERE u.username = 'manager1' AND r.name = 'CLAIM_MANAGER'
ON CONFLICT DO NOTHING;

-- 插入示例保单数据
INSERT INTO insurance_policy (policy_number, policy_holder_name, policy_holder_phone, policy_holder_email, policy_type, coverage_amount, premium_amount, start_date, end_date) VALUES 
('POL2024001', '张三', '13800138001', 'zhangsan@email.com', '车险', 200000.00, 3000.00, '2024-01-01', '2024-12-31'),
('POL2024002', '李四', '13800138002', 'lisi@email.com', '财产险', 500000.00, 5000.00, '2024-01-01', '2024-12-31'),
('POL2024003', '王五', '13800138003', 'wangwu@email.com', '人身险', 1000000.00, 8000.00, '2024-01-01', '2024-12-31'),
('POL2024004', '赵六', '13800138004', 'zhaoliu@email.com', '车险', 150000.00, 2500.00, '2024-01-01', '2024-12-31'),
('POL2024005', '钱七', '13800138005', 'qianqi@email.com', '财产险', 800000.00, 7000.00, '2024-01-01', '2024-12-31')
ON CONFLICT (policy_number) DO NOTHING;

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为需要的表创建更新时间触发器
CREATE TRIGGER update_app_user_updated_at BEFORE UPDATE ON app_user FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_insurance_policy_updated_at BEFORE UPDATE ON insurance_policy FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_claim_case_updated_at BEFORE UPDATE ON claim_case FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMIT;
