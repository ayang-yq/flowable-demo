import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Spin, Alert, Button } from 'antd';
import { PlanItemState } from '../../types';
import { caseApi } from '../../services/adminApi';
import { BpmnSubprocessVisualizer } from './BpmnSubprocessVisualizer';
import './CmmnCaseVisualizer.css';

// Import cmmn-js CSS styles
import 'cmmn-js/dist/assets/diagram-js.css';
import 'cmmn-js/dist/assets/cmmn-font/css/cmmn-embedded.css';

// 使用 ES6 导入 cmmn-js
// @ts-ignore - cmmn-js types are not available
import Viewer from 'cmmn-js/lib/NavigatedViewer';

console.log('Cmmn-js Viewer module loaded:', typeof Viewer);

interface CmmnCaseVisualizerProps {
  caseInstanceId: string;
  height?: string;
  onPlanItemClick?: (planItem: PlanItemState) => void;
}

/**
 * CMMN Case 可视化组件
 * 
 * 参考 Flowable UI 6.8 设计：
 * - 使用 cmmn-js 渲染 CMMN 模型
 * - 基于 PlanItem 状态高亮 SVG 节点
 * - 前端负责所有状态渲染逻辑
 * 
 * 状态映射规则：
 * - active: 绿色高亮边框
 * - available: 灰色边框
 * - completed: 灰色 + 完成标识
 * - terminated: 红色
 * - suspended: 黄色
 */
export const CmmnCaseVisualizer: React.FC<CmmnCaseVisualizerProps> = ({
  caseInstanceId,
  height = '800px',
  onPlanItemClick,
}) => {
  const viewerRef = useRef<HTMLDivElement>(null);
  const cmmnViewerRef = useRef<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [subprocessPlanItemId, setSubprocessPlanItemId] = useState<string | null>(null);

  /**
   * 根据状态获取对应的 CSS class
   */
  const getStateClass = (state: string): string | null => {
    switch (state) {
      case 'active':
        return 'plan-item-active';
      case 'available':
        return 'plan-item-available';
      case 'completed':
        return 'plan-item-completed';
      case 'terminated':
        return 'plan-item-terminated';
      case 'suspended':
        return 'plan-item-suspended';
      default:
        return null;
    }
  };

  const applyStateHighlights = useCallback((planItems: any[]) => {
    if (!cmmnViewerRef.current) {
      console.error('CMMN viewer is not initialized when applying highlights');
      return;
    }

    const elementRegistry = cmmnViewerRef.current.get('elementRegistry');
    const eventBus = cmmnViewerRef.current.get('eventBus');
    console.log('=== Applying state highlights ===');
    console.log('Plan items received:', planItems.length);
    
    // 创建 PlanItem 定义 ID 到状态的映射
    const planItemStateMap = new Map<string, any>();
    planItems.forEach(item => {
      planItemStateMap.set(item.planItemDefinitionId, item);
    });

    // Remove previous click listeners if any
    if (eventBus) {
      eventBus.off('element.click');
    }

    // 遍历所有图形元素
    const allElements = elementRegistry.getAll();
    console.log('All elements in diagram:', allElements.length);
    
    let matchedCount = 0;
    allElements.forEach((element: any) => {
      if (!element.businessObject) return;

      const elementId = element.businessObject.id;
      const planItemState = planItemStateMap.get(elementId);

      if (planItemState) {
        matchedCount++;
        
        const gfx = elementRegistry.getGraphics(element);
        
        if (!gfx) {
          console.error(`No graphics found for element ${elementId}`);
          return;
        }

        const svgElement = gfx;
        
        // 移除之前的状态 class
        svgElement.classList.remove(
          'plan-item-active',
          'plan-item-available',
          'plan-item-completed',
          'plan-item-terminated',
          'plan-item-suspended'
        );

        // 根据状态添加相应的 class
        const stateClass = getStateClass(planItemState.state);
        if (stateClass) {
          svgElement.classList.add(stateClass);
        }

        // 设置光标样式
        svgElement.style.cursor = 'pointer';
      }
    });

    // 使用 cmmn-js 事件总线处理点击事件
    if (eventBus) {
      eventBus.on('element.click', (event: any) => {
        const element = event.element;
        if (!element.businessObject) return;

        const elementId = element.businessObject.id;
        const planItemState = planItemStateMap.get(elementId);

        if (planItemState) {
          event.stopPropagation();
          
          console.log('PlanItem clicked (via eventBus):', planItemState.type, planItemState.id, planItemState.state);
          
          // 如果是 processtask 类型，打开子流程可视化器
          if (planItemState.type === 'processtask') {
            console.log('Opening subprocess visualization for plan item:', planItemState.id);
            setSubprocessPlanItemId(planItemState.id);
          }
          
          // 同时触发父组件的回调
          if (onPlanItemClick) {
            onPlanItemClick({
              id: planItemState.id,
              planItemDefinitionId: planItemState.planItemDefinitionId,
              name: planItemState.name,
              type: planItemState.type,
              state: planItemState.state,
              stageInstanceId: planItemState.stageInstanceId,
              createTime: planItemState.createTime,
              completedTime: planItemState.completedTime,
              terminatedTime: planItemState.terminatedTime,
            });
          }
        }
      });
    }

    console.log(`=== Highlight summary: Matched ${matchedCount} of ${planItems.length} plan items to elements ===`);
  }, [onPlanItemClick]);

  const loadVisualizationData = useCallback(async () => {
    if (!caseInstanceId) {
      console.log('Skipping load: caseInstanceId not available');
      return;
    }

    console.log('Starting loadVisualizationData...');
    setLoading(true);
    setError(null);

    let viewer = cmmnViewerRef.current;

    try {
      if (!viewer) {
        // Wait for viewerRef to be available
        let attempts = 0;
        const maxAttempts = 10;
        while (!viewerRef.current && attempts < maxAttempts) {
          console.log(`Waiting for viewerRef... attempt ${attempts + 1}`);
          await new Promise(resolve => setTimeout(resolve, 100));
          attempts++;
        }

        if (!viewerRef.current) {
          throw new Error('Viewer container not available after multiple attempts');
        }

        console.log('Initializing CMMN viewer...');
        try {
          viewer = new Viewer({
            container: viewerRef.current,
          });
          cmmnViewerRef.current = viewer;
          console.log('CMMN viewer initialized successfully');
        } catch (viewerError) {
          console.error('Failed to initialize CMMN viewer:', viewerError);
          throw new Error('Failed to initialize viewer: ' + viewerError);
        }
      }

      console.log(`Loading visualization data for case: ${caseInstanceId}`);
      const response = await caseApi.getCaseVisualization(caseInstanceId);
      const data = response.data;
      console.log(`API response received, ${data.planItems?.length || 0} plan items`);

      if (!data.cmmnXml) {
        throw new Error('CMMN XML is empty');
      }

      console.log('Importing CMMN XML...');
      const importResult = viewer.importXML(data.cmmnXml);
      await importResult;
      console.log('CMMN XML imported successfully');
      
      // Add a small delay to ensure elements are registered
      await new Promise(resolve => setTimeout(resolve, 100));
      
      const canvas = viewer.get('canvas');
      if (canvas) {
        canvas.zoom('fit-viewport');
        console.log('Canvas zoomed to fit viewport');
      }
      
      console.log('Applying state highlights...');
      applyStateHighlights(data.planItems);
      
      console.log('Visualization loaded successfully');
    } catch (err: any) {
      console.error('Failed to load CMMN visualization:', err);
      setError(err.response?.data?.message || err.message || '加载可视化数据失败');
    } finally {
      console.log('Setting loading to false');
      setLoading(false);
    }
  }, [caseInstanceId, applyStateHighlights]);

  // 关闭子流程可视化器
  const closeSubprocessVisualization = () => {
    setSubprocessPlanItemId(null);
  };
  
  useEffect(() => {
    // Small delay to ensure DOM is ready
    const timer = setTimeout(() => {
      loadVisualizationData();
    }, 100);

    return () => {
      clearTimeout(timer);
      if (cmmnViewerRef.current) {
        cmmnViewerRef.current.destroy();
        cmmnViewerRef.current = null;
      }
    };
  }, [loadVisualizationData]);

  return (
    <div className="cmmn-visualizer-container" style={{ height }}>
      {/* 图例 */}
      <div className="cmmn-state-legend">
        <div className="legend-title">状态图例：</div>
        <div className="legend-items">
          <div className="legend-item">
            <div className="legend-color legend-active"></div>
            <span>Active（活动）</span>
          </div>
          <div className="legend-item">
            <div className="legend-color legend-available"></div>
            <span>Available（可用）</span>
          </div>
          <div className="legend-item">
            <div className="legend-color legend-completed"></div>
            <span>Completed（已完成）</span>
          </div>
          <div className="legend-item">
            <div className="legend-color legend-terminated"></div>
            <span>Terminated（已终止）</span>
          </div>
          <div className="legend-item">
            <div className="legend-color legend-suspended"></div>
            <span>Suspended（已挂起）</span>
          </div>
        </div>
      </div>

      {/* CMMN Viewer 容器 */}
      <div ref={viewerRef} className="cmmn-viewer"></div>

      {/* Loading overlay */}
      {loading && (
        <div className="cmmn-visualizer-loading">
          <Spin size="large" />
          <p>正在加载 CMMN 模型...</p>
        </div>
      )}

      {/* Error overlay */}
      {error && (
        <div className="cmmn-visualizer-error">
          <Alert
            message="加载失败"
            description={error}
            type="error"
            showIcon
            action={
              <Button type="primary" size="small" onClick={loadVisualizationData}>
                重试
              </Button>
            }
          />
        </div>
      )}

      {/* 刷新按钮 */}
      <div className="cmmn-visualizer-toolbar">
        <Button
          type="default"
          size="small"
          onClick={loadVisualizationData}
          icon={<span>🔄</span>}
          title="刷新状态"
        >
          刷新
        </Button>
      </div>

      {/* BPMN 子流程可视化器 */}
      {subprocessPlanItemId && (
        <>
          {console.log('Rendering BpmnSubprocessVisualizer with planItemInstanceId:', subprocessPlanItemId)}
          <BpmnSubprocessVisualizer
            planItemInstanceId={subprocessPlanItemId}
            onClose={closeSubprocessVisualization}
          />
        </>
      )}
    </div>
  );
};

export default CmmnCaseVisualizer;
