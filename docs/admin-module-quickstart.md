# Flowable Admin 模块 - 快速启动指南

## 🚀 5分钟快速体验

### 前提条件
- ✅ Java 17+
- ✅ Maven 3.6+
- ✅ PostgreSQL 14+
- ✅ Node.js 16+ (如需前端)

### 步骤 1: 启动数据库
```bash
# 使用 Docker 快速启动 PostgreSQL
docker run -d \
  --name flowable-postgres \
  -e POSTGRES_DB=flowable \
  -e POSTGRES_USER=flowable \
  -e POSTGRES_PASSWORD=flowable \
  -p 5432:5432 \
  postgres:14
```

### 步骤 2: 启动后端
```bash
cd backend

# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动

### 步骤 3: 测试 Admin API

#### 获取系统统计
```bash
curl -X GET "http://localhost:8080/api/admin/statistics" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ=" \
  | jq
```

#### 查询模型列表
```bash
curl -X GET "http://localhost:8080/api/admin/models?type=CMMN" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ=" \
  | jq
```

#### 查询 Case 实例
```bash
curl -X GET "http://localhost:8080/api/admin/cases?state=ACTIVE" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ=" \
  | jq
```

### 步骤 4: 启动前端 (可选)
```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm start
```

前端将在 `http://localhost:3000` 启动

---

## 📊 核心功能演示

### 1. 模型管理

#### 查询所有模型
```bash
GET /api/admin/models
```

响应示例:
```json
{
  "content": [
    {
      "id": "ClaimCase:1:xxx",
      "key": "ClaimCase",
      "name": "理赔案件",
      "type": "CMMN",
      "version": 1,
      "deployed": true,
      "latestDeploymentId": "xxx"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

#### 获取模型详情
```bash
GET /api/admin/models/ClaimCase?modelType=CMMN
```

#### 部署新模型
```bash
curl -X POST "http://localhost:8080/api/admin/models/deploy" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ=" \
  -F "file=@ClaimCase.cmmn" \
  -F "modelType=CMMN"
```

### 2. Case 管理

#### 查询 Case 实例
```bash
GET /api/admin/cases?caseDefinitionKey=ClaimCase&state=ACTIVE
```

响应示例:
```json
{
  "content": [
    {
      "id": "case-001",
      "caseDefinitionKey": "ClaimCase",
      "caseDefinitionName": "理赔案件",
      "businessKey": "CLAIM-2025-001",
      "state": "ACTIVE",
      "startTime": "2025-12-21T10:00:00",
      "activePlanItems": 3,
      "completedPlanItems": 2
    }
  ]
}
```

#### 获取 Case 详情(包含 Plan Item Tree)
```bash
GET /api/admin/cases/case-001
```

响应包含:
- Case 基本信息
- 变量列表
- **Plan Item Tree**(树形结构,展示所有 Plan Items 及其状态)

#### 终止 Case
```bash
POST /api/admin/cases/case-001/terminate
Content-Type: application/json

{
  "reason": "管理员手动终止"
}
```

#### 触发 Plan Item
```bash
POST /api/admin/cases/case-001/plan-items/planItem-001/trigger
```

### 3. Process 管理

#### 查询 Process 实例
```bash
GET /api/admin/processes?processDefinitionKey=ClaimPayment
```

#### 获取 Process 详情
```bash
GET /api/admin/processes/process-001
```

#### 获取流程图高亮数据
```bash
GET /api/admin/processes/process-001/diagram
```

响应示例:
```json
{
  "processDefinitionId": "ClaimPayment:1:xxx",
  "diagramXml": "<?xml version=\"1.0\"...",
  "highlightedActivities": ["审核任务", "支付任务"],
  "completedActivities": ["开始事件", "提交申请"],
  "highlightedFlows": ["flow1", "flow2"]
}
```

### 4. 统计分析

#### 获取系统统计
```bash
GET /api/admin/statistics
```

响应示例:
```json
{
  "models": {
    "total": 5,
    "cmmn": 2,
    "bpmn": 2,
    "dmn": 1
  },
  "deployments": {
    "total": 10,
    "lastDeploymentTime": "2025-12-21T15:30:00"
  },
  "cases": {
    "ACTIVE": 15,
    "COMPLETED": 50,
    "TERMINATED": 2,
    "SUSPENDED": 0
  },
  "processes": {
    "ACTIVE": 8,
    "COMPLETED": 30,
    "SUSPENDED": 1
  }
}
```

---

## 🎯 常见使用场景

### 场景 1: 监控活动的 Case
```bash
# 查询所有活动的 Case
curl -X GET "http://localhost:8080/api/admin/cases?state=ACTIVE" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ="

# 查看特定 Case 的详情
curl -X GET "http://localhost:8080/api/admin/cases/{caseId}" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ="

# 如果需要,手动触发某个 Plan Item
curl -X POST "http://localhost:8080/api/admin/cases/{caseId}/plan-items/{planItemId}/trigger" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ="
```

### 场景 2: 部署新版本模型
```bash
# 1. 准备模型文件 (ClaimCase-v2.cmmn)

# 2. 部署新版本
curl -X POST "http://localhost:8080/api/admin/models/deploy" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ=" \
  -F "file=@ClaimCase-v2.cmmn" \
  -F "modelType=CMMN" \
  -F "deploymentName=ClaimCase-v2"

# 3. 验证部署
curl -X GET "http://localhost:8080/api/admin/models/ClaimCase?modelType=CMMN" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ="
```

### 场景 3: 故障排查
```bash
# 1. 查看系统统计,发现异常
curl -X GET "http://localhost:8080/api/admin/statistics" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ="

# 2. 查询问题 Case
curl -X GET "http://localhost:8080/api/admin/cases?state=ACTIVE" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ="

# 3. 查看 Case 详情和 Plan Item Tree
curl -X GET "http://localhost:8080/api/admin/cases/{problemCaseId}" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ="

# 4. 如果无法恢复,终止 Case
curl -X POST "http://localhost:8080/api/admin/cases/{problemCaseId}/terminate" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ=" \
  -H "Content-Type: application/json" \
  -d '{"reason": "系统故障,需要重新处理"}'
```

---

## 🔐 认证说明

所有 Admin API 都需要认证。默认账户:

- **用户名**: `admin`
- **密码**: `password`

使用 Basic Auth:
```bash
# Base64 编码: admin:password = YWRtaW46cGFzc3dvcmQ=
Authorization: Basic YWRtaW46cGFzc3dvcmQ=
```

或使用 curl 的 `-u` 选项:
```bash
curl -u admin:password http://localhost:8080/api/admin/statistics
```

---

## 📱 前端使用

### Dashboard
访问: `http://localhost:3000/admin/dashboard`

显示:
- 模型统计卡片
- 部署统计卡片
- Case 实例统计卡片
- Process 实例统计卡片

### Case 管理
访问: `http://localhost:3000/admin/cases`

功能:
- 筛选 Case (按 Key、Business Key、状态)
- 查看 Case 列表
- 查看 Case 详情
- 终止 Case
- 触发 Plan Item

### Process 管理
访问: `http://localhost:3000/admin/processes`

功能:
- 筛选 Process
- 查看 Process 列表
- 查看 Process 详情
- **BPMN 流程图可视化**(高亮当前节点)
- 终止/挂起/恢复 Process

---

## 🐛 故障排除

### 问题 1: 编译失败
```bash
# 清理并重新编译
cd backend
mvn clean compile
```

### 问题 2: 数据库连接失败
检查 `application.yml` 中的数据库配置:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flowable
    username: flowable
    password: flowable
```

### 问题 3: API 返回 401 Unauthorized
确保使用正确的认证信息:
```bash
curl -u admin:password http://localhost:8080/api/admin/statistics
```

### 问题 4: Case 挂起/恢复失败
这是已知问题,Flowable 7.2.0 CMMN API 需要确认正确的方法名。
当前这两个功能暂时禁用,会抛出 `UnsupportedOperationException`。

---

## 📚 更多资源

- **完整文档**: `docs/admin-module-complete-summary.md`
- **设计文档**: `docs/admin-module-design.md`
- **进度报告**: `docs/admin-module-progress.md`
- **API 文档**: 启动后访问 `http://localhost:8080/swagger-ui.html`

---

## 💡 提示

1. **使用 jq 格式化 JSON 输出**:
   ```bash
   curl ... | jq
   ```

2. **保存响应到文件**:
   ```bash
   curl ... > response.json
   ```

3. **查看详细请求信息**:
   ```bash
   curl -v ...
   ```

4. **批量操作**:
   ```bash
   # 查询所有活动 Case 并终止
   curl -X GET "http://localhost:8080/api/admin/cases?state=ACTIVE" | \
     jq -r '.content[].id' | \
     xargs -I {} curl -X POST "http://localhost:8080/api/admin/cases/{}/terminate"
   ```

---

**祝使用愉快!** 🎉

如有问题,请查看详细文档或提交 Issue。
