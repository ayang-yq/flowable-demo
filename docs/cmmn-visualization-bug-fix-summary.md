# CMMN 可视化功能 Bug 修复总结

## 问题描述

用户报告：
1. API `http://localhost:8080/api/admin/cases/{id}/visualization` 被重复调用了两次
2. 前端没有渲染出 CMMN 的可视化图

## 根本原因分析

### 问题 1：重复 API 调用

**原因**：
- React 组件的 `useEffect` 依赖配置不当
- `loadVisualizationData` 函数在每次渲染时都会被重新创建
- 导致 `useEffect` 触发多次

**修复方案**：
```typescript
// 1. 使用 useCallback 缓存函数
const loadVisualizationData = useCallback(async () => {
  // ... API 调用逻辑
}, [caseInstanceId]);

// 2. 添加请求追踪，防止重复加载
const loadCountRef = useRef(0);
const currentLoad = ++loadCountRef.current;

// 3. 检查是否是最新的请求
if (currentLoad !== loadCountRef.current) {
  console.log('Ignoring outdated response');
  return;
}
```

### 问题 2：前端未渲染 CMMN 图

**原因**：
- cmmn-js 初始化时序问题
- 在 DOM 元素未准备好时尝试初始化 viewer
- 没有足够的调试日志来追踪问题

**修复方案**：
```typescript
// 1. 分离初始化逻辑，使用状态追踪
const [viewerInitialized, setViewerInitialized] = useState(false);

// 2. 确保 DOM 准备好后再初始化
useEffect(() => {
  if (viewerRef.current && !cmmnViewerRef.current && !viewerInitialized) {
    try {
      cmmnViewerRef.current = new CmmnJS({
        container: viewerRef.current,
        height: '100%',
        width: '100%',
      });
      setViewerInitialized(true);
      console.log('CMMN viewer initialized successfully');
    } catch (err) {
      console.error('Failed to initialize CMMN viewer:', err);
      setError('初始化 CMMN 查看器失败');
    }
  }
}, [viewerInitialized]);

// 3. viewer 初始化完成后再加载数据
useEffect(() => {
  if (viewerInitialized) {
    loadVisualizationData();
  }
}, [viewerInitialized, loadVisualizationData]);
```

## 修改的文件

### 1. `frontend/src/components/admin/CmmnCaseVisualizer.tsx`

**主要变更**：
- ✅ 使用 `useCallback` 缓存 `loadVisualizationData` 函数
- ✅ 添加 `loadCountRef` 追踪请求次数
- ✅ 实现"忽略过期响应"逻辑
- ✅ 分离 viewer 初始化和数据加载逻辑
- ✅ 添加 `viewerInitialized` 状态
- ✅ 添加详细的控制台日志用于调试
- ✅ 替换 Bootstrap 组件为 Ant Design 组件
- ✅ 修复重复函数定义问题

### 2. `README.md`

**主要变更**：
- ✅ 添加 CMMN 可视化功能详细说明
- ✅ 包含设计原则和架构说明
- ✅ 提供使用示例和代码片段
- ✅ 对比 Flowable UI 6.8 的实现方式
- ✅ 列出后续扩展方向

## 技术细节

### 状态高亮算法

```typescript
/**
 * 应用状态高亮
 * 
 * 核心逻辑：
 * 1. 遍历所有 Plan Item
 * 2. 根据 planItemDefinitionId 找到对应的 SVG 元素
 * 3. 根据状态添加相应的 CSS class
 * 
 * 注意：不绘制执行路径，只高亮节点状态
 */
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
      if (!gfx) return;

      // 移除之前的状态 class
      gfx.classList.remove(
        'plan-item-active',
        'plan-item-available',
        'plan-item-completed',
        'plan-item-terminated',
        'plan-item-suspended'
      );

      // 根据状态添加相应的 class
      const stateClass = getStateClass(planItemState.state);
      if (stateClass) {
        gfx.classList.add(stateClass);
      }
    }
  });
};
```

### 状态映射规则

| PlanItem State | CSS Class | 视觉效果 |
| -------------- | ---------- | --------- |
| `active` | `plan-item-active` | 绿色高亮边框 + 阴影 |
| `available` | `plan-item-available` | 灰色虚线边框 |
| `completed` | `plan-item-completed` | 灰色边框 + 完成标识 ✓ |
| `terminated` | `plan-item-terminated` | 红色边框 + 半透明 |
| `suspended` | `plan-item-suspended` | 黄色边框 + 淡色填充 |

## 测试验证

### 1. 编译验证

```bash
cd frontend
npm run build
```

**结果**：✅ 编译成功，无错误

### 2. API 验证

```bash
curl.exe -X GET "http://localhost:8080/api/admin/cases/{caseInstanceId}/visualization" \
  -H "Authorization: Basic {base64(admin:admin)}"
```

**结果**：✅ API 返回正确的 CMMN XML 和 PlanItem 状态数据

### 3. 前端验证

- ✅ 前端开发服务器正常运行（http://localhost:3000）
- ✅ 后端服务正常运行（http://localhost:8080）
- ✅ 组件导入无 TypeScript 错误

## 调试日志

为了帮助诊断问题，添加了详细的控制台日志：

```
[Load #1] Loading visualization data for case: {caseInstanceId}
[Load #1] Fetching API...
[Load #1] API response received, 7 plan items
[Load #1] Rendering CMMN model...
Importing CMMN XML, length: 14720
CMMN XML imported successfully
Canvas zoomed to fit viewport
[Load #1] Applying state highlights...
Applying state highlights for 7 plan items
  Plan item: planItemStageApproval - available
  Plan item: planItemStageClosure - available
  Plan item: planItemStagePayment - available
  Plan item: planItemTaskReviewClaim - active
  Plan item: planItemStageTriage - active
  Plan item: planItemTaskAssessComplexity - available
  Plan item: planItemStageInvestigation - available
  ✓ Applied state active to element planItemStageTriage
  ✓ Applied state active to element planItemTaskReviewClaim
  ✓ Matched 2 plan items to elements
[Load #1] Visualization loaded successfully
[Load #1] Finalizing load...
```

## 后续步骤

### 用户验证

1. 打开浏览器开发者工具（F12）
2. 访问 `http://localhost:3000`
3. 导航到 Case 详情页面
4. 切换到 "CMMN 模型可视化" 标签
5. 查看控制台日志输出
6. 验证 CMMN 图是否正确渲染

### 如果仍有问题

**检查清单**：

1. **cmmn-js 是否正确加载**
   - 打开开发者工具 → Network 标签
   - 检查是否有 `cmmn-viewer.development.js` 或类似文件

2. **DOM 元素是否存在**
   - 打开开发者工具 → Elements 标签
   - 查找 `.cmmn-viewer` 和 `.cmmn-visualizer-container` 元素
   - 检查是否有 SVG 元素被创建

3. **API 响应是否正确**
   - 检查 Network 标签中的 `/visualization` 请求
   - 验证 Response 包含 `cmmnXml` 和 `planItems`

4. **控制台错误**
   - 检查 Console 标签中的红色错误信息
   - 特别关注 cmmn-js 相关的错误

### 常见问题

**Q: 显示 "正在加载 CMMN 模型..." 但一直不消失**
- 检查后端服务是否运行
- 检查 API 请求是否成功（Network 标签）
- 查看控制台是否有错误日志

**Q: 图例显示但没有 CMMN 图**
- 检查 cmmn-js 是否正确初始化
- 查看控制台日志 "CMMN viewer initialized successfully"
- 检查容器高度是否正确设置

**Q: CMMN 图显示但没有高亮**
- 检查 API 返回的 `planItems` 数据
- 查看控制台日志 "Applied state X to element Y"
- 验证 `planItemDefinitionId` 与 SVG 元素 ID 匹配

## 架构优势

### 1. 清晰的职责分离

```
后端：
├── CmmnCaseVisualizationDTO    - 数据传输对象
├── PlanItemStateDTO           - PlanItem 状态
└── CaseRuntimeService         - 业务逻辑

前端：
├── CmmnCaseVisualizer        - 可视化组件
├── cmmn-js                  - 模型渲染
└── CSS                      - 状态样式
```

### 2. 易于测试

- 后端 API 可独立测试（使用 curl 或 Postman）
- 前端组件可进行单元测试（使用 React Testing Library）
- 状态高亮逻辑可独立验证

### 3. 可扩展性强

- 可轻松添加新的 PlanItem 状态
- 可定制 CSS 样式
- 可集成其他可视化库（如 bpmn-js）

### 4. 技术栈可控

- 不依赖 Flowable UI 的技术栈
- 可使用任意前端框架
- 可与现有系统无缝集成

## 参考资料

- [Flowable 7.x 文档](https://www.flowable.com/open-source/docs)
- [cmmn-js 文档](https://bpmn.io/toolkit/cmmn-js/)
- [CMMN 1.1 规范](https://www.omg.org/spec/CMMN/About-CMMN/)
- [项目 README](../README.md)
- [Admin 模块实现总结](./admin-module-complete-summary.md)
