# CMMN Case 运行状态可视化实现文档

基于 **Flowable UI 6.8 设计思路**，为系统增加了轻量级的 CMMN Case 运行状态可视化功能。

---

## 📋 概述

在不引入 Flowable 官方 UI 的前提下，实现了：
1. 后端 REST API - 提供 CMMN XML 和运行态数据
2. 前端 React 组件 - 使用 cmmn-js 渲染模型并应用状态高亮
3. 状态映射系统 - 根据 PlanItem 状态高亮 SVG 节点

---

## 🏗️ 架构设计

### 设计原则

1. **前后端分离**
   - 后端：数据提供者（CMMN XML + PlanItem 状态）
   - 前端：表现层逻辑（渲染 + 高亮）

2. **静态模型 + 动态状态**
   - 静态：CMMN XML（通过 cmmn-js 渲染）
   - 动态：PlanItemInstance（运行态 + 历史态）

3. **不绘制执行路径**
   - 只高亮节点状态
   - 前端通过 CSS class 实现

---

## 🔧 后端实现

### 1. DTO 结构

#### CmmnCaseVisualizationDTO
```java
@Data
@Builder
public class CmmnCaseVisualizationDTO {
    private String caseInstanceId;
    private String caseDefinitionId;
    private String cmmnXml;                    // CMMN XML 用于 cmmn-js
    private List<PlanItemStateDTO> planItems;   // 所有 PlanItem 状态
}
```

#### PlanItemStateDTO
```java
@Data
@Builder
public class PlanItemStateDTO {
    private String id;
    private String planItemDefinitionId;  // 对应 CMMN XML elementId
    private String name;
    private String type;                 // HUMAN_TASK, STAGE, MILESTONE, etc.
    private String state;                // active, available, completed, etc.
    private String stageInstanceId;
    private LocalDateTime createTime;
    private LocalDateTime completedTime;
    private LocalDateTime terminatedTime;
}
```

### 2. Service 实现

#### CaseRuntimeService.getCaseVisualizationData()
```java
public CmmnCaseVisualizationDTO getCaseVisualizationData(String caseInstanceId) {
    // 1. 获取 Case 实例
    CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .singleResult();

    // 2. 获取 CMMN XML
    String cmmnXml = repositoryAdapter.getCaseDefinitionResourceContent(
            caseDefinition.getDeploymentId(),
            caseDefinition.getResourceName()
    );

    // 3. 获取运行态 Plan Items
    List<PlanItemInstance> runtimePlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .list();

    // 4. 获取历史 Plan Items（用于已完成节点的展示）
    List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceCaseInstanceId(caseInstanceId)
            .list();

    // 5. 合并运行态和历史态数据
    List<PlanItemStateDTO> allPlanItems = mergePlanItems(runtimePlanItems, historicPlanItems);

    return CmmnCaseVisualizationDTO.builder()
            .caseInstanceId(caseInstanceId)
            .caseDefinitionId(caseInstance.getCaseDefinitionId())
            .cmmnXml(cmmnXml)
            .planItems(allPlanItems)
            .build();
}
```

### 3. REST API

```java
@GetMapping("/{caseInstanceId}/visualization")
public ResponseEntity<CmmnCaseVisualizationDTO> getCaseVisualization(
        @PathVariable String caseInstanceId) {
    log.info("Get case visualization: {}", caseInstanceId);
    CmmnCaseVisualizationDTO visualization = caseRuntimeService.getCaseVisualizationData(caseInstanceId);
    return ResponseEntity.ok(visualization);
}
```

---

## 🎨 前端实现

### 1. TypeScript 类型定义

```typescript
export interface PlanItemState {
  id: string;
  planItemDefinitionId: string;  // 对应 CMMN XML 中的 elementId
  name: string;
  type: string;  // HUMAN_TASK, STAGE, MILESTONE, etc.
  state: 'active' | 'available' | 'completed' | 'terminated' | 'suspended';
  stageInstanceId?: string;
  createTime: string;
  completedTime?: string;
  terminatedTime?: string;
}

export interface CmmnCaseVisualization {
  caseInstanceId: string;
  caseDefinitionId: string;
  cmmnXml: string;
  planItems: PlanItemState[];
}
```

### 2. React 组件

```tsx
interface CmmnCaseVisualizerProps {
  caseInstanceId: string;
  height?: string;
  onPlanItemClick?: (planItem: PlanItemState) => void;
}
```

**核心功能：**
- 使用 cmmn-js `NavigatedViewer` 渲染 CMMN 模型
- 根据 `planItemDefinitionId` 映射到 SVG 元素 `data-element-id`
- 根据状态应用对应的 CSS class
- 支持点击节点查看 PlanItem 详情

### 3. 状态高亮逻辑

```typescript
const applyStateHighlights = (planItems: any[]) => {
  const elementRegistry = cmmnViewerRef.current.get('elementRegistry');

  // 创建 PlanItem 定义 ID 到状态的映射
  const planItemStateMap = new Map<string, any>();
  planItems.forEach(item => {
    planItemStateMap.set(item.planItemDefinitionId, item);
  });

  // 遍历所有图形元素
  elementRegistry.getAll().forEach((element: any) => {
    if (!element.businessObject) return;

    const elementId = element.businessObject.id;
    const planItemState = planItemStateMap.get(elementId);

    if (planItemState) {
      const gfx = elementRegistry.getGraphics(element);
      const stateClass = getStateClass(planItemState.state);
      if (stateClass) {
        gfx.classList.add(stateClass);
      }
    }
  });
};
```

### 4. CSS 状态样式

```css
/* Active 状态 - 绿色高亮 */
.plan-item-active > .djs-visual > * {
  stroke: #28a745 !important;
  stroke-width: 3px !important;
  filter: drop-shadow(0 0 4px rgba(40, 167, 69, 0.4));
}

/* Available 状态 - 灰色虚线边框 */
.plan-item-available > .djs-visual > * {
  stroke: #6c757d !important;
  stroke-width: 2px !important;
  stroke-dasharray: 5, 5;
}

/* Completed 状态 - 灰色 + 完成标识 */
.plan-item-completed > .djs-visual > * {
  stroke: #6c757d !important;
  stroke-width: 2px !important;
  opacity: 0.7;
}

/* Terminated 状态 - 红色 */
.plan-item-terminated > .djs-visual > * {
  stroke: #dc3545 !important;
  stroke-width: 3px !important;
  opacity: 0.6;
}

/* Suspended 状态 - 黄色 */
.plan-item-suspended > .djs-visual > * {
  stroke: #ffc107 !important;
  stroke-width: 3px !important;
}
```

---

## 📊 状态映射规则

| PlanItem State | UI 表现 | CSS Class |
| -------------- | ------- | --------- |
| `active` | 绿色高亮边框 + 阴影 | `plan-item-active` |
| `available` | 灰色虚线边框 | `plan-item-available` |
| `completed` | 灰色边框 + 完成标识 ✓ | `plan-item-completed` |
| `terminated` | 红色边框 + 半透明 | `plan-item-terminated` |
| `suspended` | 黄色边框 + 淡色填充 | `plan-item-suspended` |

---

## 🚀 使用方式

### 在 Case 详情页中使用

```tsx
import { CmmnCaseVisualizer } from './CmmnCaseVisualizer';

<CmmnCaseVisualizer
  caseInstanceId={caseInstanceId}
  height="600px"
  onPlanItemClick={(planItem) => {
    Modal.info({
      title: `Plan Item: ${planItem.name}`,
      content: <PlanItemDetail planItem={planItem} />
    });
  }}
/>
```

---

## 🔄 与 Flowable UI 6.8 的对比

| 特性 | Flowable UI 6.8 | 本实现 |
|------|---------------|--------|
| 模型渲染 | 自定义 SVG 库 | cmmn-js（标准） |
| 状态数据 | 后端生成高亮结果 | 后端只提供原始数据 |
| 状态高亮 | 后端注入 SVG | 前端 CSS class |
| 扩展性 | 依赖官方 UI | 完全可定制 |
| 依赖重量 | 重（包含整套 UI） | 轻量（仅可视化） |

---

## 🎯 后续扩展方向

### 1. Case Timeline
- 展示 Case 执行时间线
- 显示 PlanItem 启动/完成时间

### 2. Sentry 解释
- 可视化显示 Sentry 触发条件
- 解释为什么某个 PlanItem 被激活

### 3. 实时更新
- WebSocket 推送状态变化
- 实时刷新模型视图

### 4. 交互操作
- 在模型上直接触发 PlanItem
- 拖拽调整 Case 流程

---

## 📁 文件清单

### 后端文件
```
backend/src/main/java/com/flowable/demo/admin/
├── web/dto/
│   ├── CmmnCaseVisualizationDTO.java      # 可视化数据 DTO
│   └── PlanItemStateDTO.java             # Plan Item 状态 DTO
├── service/
│   └── CaseRuntimeService.java           # 运行态服务（包含 getCaseVisualizationData）
└── web/
    └── AdminCaseResource.java            # REST API（包含 /visualization 端点）
```

### 前端文件
```
frontend/src/
├── components/admin/
│   ├── CmmnCaseVisualizer.tsx           # 可视化组件
│   └── CmmnCaseVisualizer.css           # 状态样式
├── services/
│   └── adminApi.ts                      # API 客户端
└── types/
    └── index.ts                         # TypeScript 类型定义
```

---

## ✅ 实现检查清单

- [x] 后端：创建 CmmnCaseVisualizationDTO
- [x] 后端：创建 PlanItemStateDTO
- [x] 后端：实现 getCaseVisualizationData 方法
- [x] 后端：实现运行态和历史态 Plan Items 合并
- [x] 后端：创建 REST API 端点 /visualization
- [x] 前端：安装 cmmn-js 依赖
- [x] 前端：创建 CmmnCaseVisualizer 组件
- [x] 前端：实现 cmmn-js 渲染逻辑
- [x] 前端：实现状态高亮逻辑
- [x] 前端：添加 CSS 状态样式
- [x] 前端：实现点击交互功能
- [x] 前端：添加状态图例
- [x] 前端：添加刷新功能
- [x] 文档：更新 README.md
- [x] 文档：创建本文档

---

## 🔍 API 测试

### 获取 Case 列表
```bash
curl -X GET "http://localhost:8080/api/admin/cases?page=0&size=5" \
  -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
```

### 获取可视化数据
```bash
curl -X GET "http://localhost:8080/api/admin/cases/{caseInstanceId}/visualization" \
  -H "Authorization: Basic $(echo -n 'admin:admin' | base64)"
```

**响应示例：**
```json
{
  "caseInstanceId": "a92e90e3-e21f-11f0-b472-005056c00001",
  "caseDefinitionId": "ac622a4f-e20a-11f0-b771-005056c00001",
  "cmmnXml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<cmmn:definitions...",
  "planItems": [
    {
      "id": "a92eb80b-e21f-11f0-b472-005056c00001",
      "planItemDefinitionId": "planItemTaskReviewClaim",
      "name": "Review Claim Application",
      "type": "humantask",
      "state": "active",
      "stageInstanceId": "a92eb806-e21f-11f0-b472-005056c00001",
      "createTime": "2025-12-26T13:56:45.876"
    }
  ]
}
```

---

## 🎓 架构优势

### 1. 清晰的职责分离
- 后端：专注于数据查询和转换
- 前端：专注于展示和交互

### 2. 易于测试
- 后端 API 可独立测试
- 前端组件可单元测试

### 3. 技术栈可控
- 不依赖 Flowable UI 的技术栈
- 可使用任意前端框架

### 4. 可移植性强
- 后端 API 可被任何客户端使用
- 前端可替换为其他可视化库

---

## 📚 参考资料

- [cmmn-js Documentation](https://bpmn.io/toolkit/cmmn-js/)
- [Flowable CMMN Engine](https://www.flowable.com/open-source/docs/bpmn2/ch08-CMMN)
- [CMMN 1.1 Specification](http://www.omg.org/spec/CMMN/1.1/)

---

## 📞 联系与支持

如有问题或建议，请：
1. 查看本文档
2. 查看 README.md
3. 提交 Issue

---

**最后更新**: 2025-12-26
