# 保险理赔系统 Demo

基于 **Flowable 7.x（CMMN + BPMN + DMN）** 的保险理赔系统 Demo，使用 **Spring Boot 3.5.x + Java 17 + PostgreSQL** 作为后端，**React JS + TypeScript + Ant Design** 作为前端管理控制台。

## 🎯 项目概述

这是一个完整的保险理赔管理系统，展示了 Flowable 7.x 的所有核心能力：

- **CMMN Case Management**：处理复杂的理赔案件生命周期
- **BPMN Process Engine**：执行支付等子流程
- **DMN Decision Engine**：基于规则的赔付决策
- **Job Executor**：异步任务处理
- **History Audit**：完整的审计跟踪

## 🏗️ 项目结构

```
flowable-demo/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/flowable/demo/
│   │   ├── domain/             # 领域层 (DDD)
│   │   │   ├── model/         # 实体模型
│   │   │   └── repository/    # 仓储接口
│   │   ├── service/            # 业务服务层
│   │   ├── web/                # REST API 层
│   │   │   └── rest/          # 业务 API
│   │   ├── admin/              # **Admin 管理模块** (NEW)
│   │   │   ├── model/         # Admin 领域模型
│   │   │   ├── adapter/       # Flowable 适配器
│   │   │   ├── service/       # Admin 业务服务
│   │   │   └── web/           # Admin REST API
│   │   └── config/            # 配置类
│   └── src/main/resources/
│       ├── cases/              # CMMN Case 定义
│       │   └── ClaimCase.cmmn
│       ├── dmn/                # DMN 决策表
│       │   └── ClaimDecisionTable.dmn
│       ├── processes/          # BPMN 流程定义
│       │   └── ClaimPaymentProcess.bpmn
│       └── application.yml    # 配置文件
├── frontend/                  # React 前端
│   ├── src/
│   │   ├── components/       # React 组件
│   │   ├── services/         # API 服务
│   │   ├── contexts/         # React Context
│   │   └── types/           # TypeScript 类型定义
│   ├── public/              # 静态资源
│   └── package.json         # 依赖配置
├── docs/                     # 文档
│   ├── admin-module-design.md              # Admin 模块设计文档
│   └── admin-module-implementation-summary.md  # Admin 实现总结
├── resources/                 # 资源文件
│   └── init-db.sql          # 数据库初始化脚本
└── README.md
```


## 🚀 快速开始

### 1. 环境要求

- **Java 17+**
- **Maven 3.8+**
- **Node.js 18+** (前端)
- **PostgreSQL** (本地安装)

### 2. 设置数据库

```bash
# 连接到本地 PostgreSQL
psql -U postgres

# 创建所需的数据库
CREATE DATABASE flowable_cline;
CREATE DATABASE flowable_demo;

# 验证数据库已创建
\l
```

**注意**: 确保本地 PostgreSQL 服务正在运行，默认连接配置为：
- 主机: localhost:5432
- 用户名: flowable_cline
- 密码: flowable_cline

### 3. 启动后端

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端将在 `http://localhost:8080/api` 启动

### 4. 启动前端

```bash
cd frontend
npm install
npm start
```

前端将在 `http://localhost:3000` 启动

### 5. 访问应用

- **前端应用**: `http://localhost:3000`
- **API 文档**: `http://localhost:8080/api/swagger-ui.html`

## 📋 业务流程

### 理赔案件处理流程 (CMMN Case)

1. **案件创建** - 创建新的理赔案件
2. **材料收集** - 收集理赔相关文档
3. **损失评估** - 评估实际损失金额
4. **规则判定** - DMN 决策表自动判定
5. **外部调查** - 可选的外部调查阶段
6. **人工审核** - 基于规则的审核流程
7. **支付执行** - BPMN 支付流程
8. **案件关闭** - 最终结案

### 支付流程 (BPMN Process)

1. **支付校验** - 校验支付信息
2. **执行支付** - 调用支付服务
3. **支付确认** - 确认支付结果
4. **更新状态** - 更新 Case 状态
5. **发送通知** - 发送完成通知

### 决策规则 (DMN Table)

基于以下条件进行智能决策：
- 保单类型（车险、财产险、人身险）
- 理赔金额
- 保额限制
- 报案类别（事故、盗窃、自然灾害等）
- 严重性等级

输出决策：
- 赔付方式（快速赔付、标准赔付、分级赔付）
- 是否需要调查
- 是否需要人工审核
- 审批级别（自动、主管、经理、总监）
- 优先级（普通、重要、紧急）

## 🔧 技术栈

### 后端技术
- **Spring Boot 3.2.x** - 应用框架
- **Flowable 7.0.1** - 流程引擎
- **Spring Security** - 安全框架
- **Spring Data JPA** - 数据访问
- **PostgreSQL** - 关系数据库
- **Redis** - 缓存和会话
- **Lombok** - 代码简化
- **MapStruct** - 对象映射
- **SpringDoc OpenAPI** - API 文档

### 前端技术
- **React 18** - UI 框架
- **TypeScript** - 类型安全
- **Ant Design** - UI 组件库
- **React Context** - 状态管理
- **Axios** - HTTP 客户端

## 📊 数据模型

### 核心实体

1. **User** - 系统用户
2. **Role** - 用户角色
3. **InsurancePolicy** - 保险保单
4. **ClaimCase** - 理赔案件
5. **ClaimDocument** - 理赔文档
6. **ClaimHistory** - 理赔历史

### 角色定义

- **ADMIN** - 系统管理员
- **CLAIM_HANDLER** - 理赔处理员
- **CLAIM_AUDITOR** - 理赔审核员
- **CLAIM_MANAGER** - 理赔经理

## 🔐 默认账户

系统预置了以下测试账户（密码：`password`）：

| 用户名 | 角色 | 说明 |
|--------|------|------|
| admin | ADMIN | 系统管理员 |
| handler1 | CLAIM_HANDLER | 理赔处理员 |
| auditor1 | CLAIM_AUDITOR | 理赔审核员 |
| manager1 | CLAIM_MANAGER | 理赔经理 |

## 📝 API 端点

### Case 管理
- `POST /api/cases` - 创建理赔 Case
- `GET /api/cases` - 查询 Case 列表
- `GET /api/cases/{id}` - 获取 Case 详情
- `PUT /api/cases/{id}` - 更新 Case
- `POST /api/cases/{id}/start` - 启动 Case 流程

### 任务管理
- `GET /api/tasks/my` - 我的任务
- `GET /api/tasks/all` - 所有任务
- `POST /api/tasks/{id}/complete` - 完成任务
- `POST /api/tasks/{id}/assign` - 分配任务

### 流程管理
- `GET /api/processes` - 查询流程定义
- `POST /api/processes/{key}/start` - 启动流程
- `GET /api/process-instances` - 查询流程实例

### 决策管理
- `POST /api/decisions/evaluate` - 执行 DMN 决策
- `GET /api/decisions/tables` - 查询决策表

### 用户管理
- `GET /api/users` - 用户列表
- `POST /api/users` - 创建用户
- `PUT /api/users/{id}` - 更新用户
- `POST /api/users/{id}/roles` - 分配角色

### **Admin 管理** (NEW)

#### 模型管理
- `GET /api/admin/models` - 查询模型列表(支持类型筛选)
- `GET /api/admin/models/{modelKey}` - 获取模型详情(包含所有版本和 XML)
- `POST /api/admin/models/deploy` - 部署模型(文件上传)

#### Case 运行态管理
- `GET /api/admin/cases` - 查询 Case 实例列表(支持多条件筛选)
- `GET /api/admin/cases/{caseInstanceId}` - 获取 Case 实例详情(包含 Plan Item Tree)
- `POST /api/admin/cases/{caseInstanceId}/terminate` - 终止 Case
- `POST /api/admin/cases/{caseInstanceId}/suspend` - 挂起 Case
- `POST /api/admin/cases/{caseInstanceId}/resume` - 恢复 Case
- `POST /api/admin/cases/{caseInstanceId}/plan-items/{planItemInstanceId}/trigger` - 手动触发 Plan Item

#### Process 运行态管理
- `GET /api/admin/processes` - 查询 Process 实例列表
- `GET /api/admin/processes/{processInstanceId}` - 获取 Process 实例详情
- `GET /api/admin/processes/{processInstanceId}/diagram` - 获取流程图高亮数据
- `POST /api/admin/processes/{processInstanceId}/terminate` - 终止 Process
- `POST /api/admin/processes/{processInstanceId}/suspend` - 挂起 Process
- `POST /api/admin/processes/{processInstanceId}/resume` - 恢复 Process

#### 统计分析
- `GET /api/admin/statistics` - 获取系统统计信息(模型、部署、Case、Process)

## 🎯 演示数据

系统自动创建以下演示数据：

1. **5 条保单记录**
   - 车险保单：POL2024001, POL2024004
   - 财产险保单：POL2024002, POL2024005
   - 人身险保单：POL2024003

2. **4 个用户账户**
   - 不同角色的测试用户

3. **流程部署**
   - 自动部署 CMMN、BPMN、DMN 定义

## 🔍 监控和管理

### **Admin 管理模块** (NEW)
自定义的技术管理员控制台,提供完整的 Flowable 模型和运行态管理:

#### 功能特性
- **模型管理**: 查询、部署 CMMN/BPMN/DMN 模型
- **Case 管理**: 查询、监控、操作 Case 实例
- **Process 管理**: 查询、监控、操作 Process 实例
- **可视化**: Plan Item Tree、BPMN 流程图高亮
- **统计分析**: 系统运行状态统计

#### API 端点
```
# 模型管理
GET    /api/admin/models                    - 查询模型列表
GET    /api/admin/models/{modelKey}         - 获取模型详情
POST   /api/admin/models/deploy             - 部署模型

# Case 管理
GET    /api/admin/cases                     - 查询 Case 列表
GET    /api/admin/cases/{id}                - 获取 Case 详情
POST   /api/admin/cases/{id}/terminate      - 终止 Case

# Process 管理
GET    /api/admin/processes                 - 查询 Process 列表
GET    /api/admin/processes/{id}            - 获取 Process 详情
GET    /api/admin/processes/{id}/diagram    - 获取流程图高亮数据

# 统计
GET    /api/admin/statistics                - 获取系统统计
```

详细文档: `docs/admin-module-complete-summary.md`

### Flowable 内置管理界面
访问 Flowable 内置管理界面:
- `http://localhost:8080/api/flowable-ui/cmmn` - Case 管理
- `http://localhost:8080/api/flowable-ui/modeler` - 流程设计器
- `http://localhost:8080/api/flowable-ui/admin` - 系统管理

### 健康检查
- `http://localhost:8080/api/actuator/health` - 应用健康状态
- `http://localhost:8080/api/actuator/metrics` - 应用指标

## 🧪 测试

### 单元测试
```bash
cd backend
mvn test
```

### 集成测试
```bash
mvn verify -P integration-test
```

## 📈 扩展功能

### 已实现
- ✅ 完整的 DDD 领域模型
- ✅ Flowable 7.x 三引擎集成
- ✅ 复杂的 CMMN Case 流程
- ✅ BPMN 支付子流程
- ✅ DMN 决策表规则
- ✅ PostgreSQL 数据持久化
- ✅ Spring Security 安全控制
- ✅ OpenAPI 文档
- ✅ React 前端界面

### 待实现
- 🔄 实时通知系统
- 🔄 文件上传功能
- 🔄 邮件集成
- 🔄 报表统计
- 🔄 移动端适配

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 支持

如有问题或建议，请：

1. 查看 [Issues](../../issues) 页面
2. 创建新的 Issue 描述问题
3. 提供详细的复现步骤

---

**注意**：这是一个演示项目，用于展示 Flowable 7.x 的能力和最佳实践。在生产环境中使用前，请确保进行充分的测试和安全评估。
