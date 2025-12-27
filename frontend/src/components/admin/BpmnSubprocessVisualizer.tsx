import React, { useEffect, useRef, useState } from 'react';
import { Modal, Spin, Alert, Tag, Descriptions, Empty, Switch } from 'antd';
import { caseApi } from '../../services/adminApi';

// Import bpmn-js CSS styles
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';
import './BpmnSubprocessVisualizer.css';

// @ts-ignore - bpmn-js types are not available
import Viewer from 'bpmn-js/lib/NavigatedViewer';

console.log('Bpmn-js Viewer module loaded:', typeof Viewer);

interface ActivityState {
  activityId: string;
  activityName: string;
  activityType: string;
  state: 'active' | 'completed' | 'available';
  processInstanceId: string;
  startTime?: string;
  endTime?: string;
}

interface SubprocessVisualization {
  processInstanceId: string;
  processDefinitionId: string;
  processDefinitionKey: string;
  processDefinitionName: string;
  bpmnXml: string;
  activityStates: ActivityState[];
  processInstanceState: string;
  startTime?: string;
  endTime?: string;
}

interface BpmnSubprocessVisualizerProps {
  planItemInstanceId: string;
  onClose: () => void;
}

export const BpmnSubprocessVisualizer: React.FC<BpmnSubprocessVisualizerProps> = ({
  planItemInstanceId,
  onClose
}) => {
  const [visualization, setVisualization] = useState<SubprocessVisualization | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  console.log('BpmnSubprocessVisualizer mounted with planItemInstanceId:', planItemInstanceId);
  const [renderMode, setRenderMode] = useState<'bpmnjs' | 'flowable'>('bpmnjs');
  const [flowableDiagramContent, setFlowableDiagramContent] = useState<{ type: 'svg' | 'png'; content: string } | null>(null);
  const [showBpmnJsViewer, setShowBpmnJsViewer] = useState(true);
  const containerRef = useRef<HTMLDivElement>(null);
  const viewerRef = useRef<any>(null);

  useEffect(() => {
    console.log('BpmnSubprocessVisualizer useEffect triggered, loading:', loading);
    loadSubprocessVisualization();
    return () => {
      console.log('BpmnSubprocessVisualizer cleanup');
      cleanupViewer();
    };
  }, [planItemInstanceId]);

  // Load diagram when render mode changes
  useEffect(() => {
    if (!visualization) return;

    if (renderMode === 'flowable') {
      // Clean up bpmn-js viewer before switching to Flowable mode
      setShowBpmnJsViewer(false);
      cleanupViewer();
      loadFlowableDiagram();
    } else if (renderMode === 'bpmnjs') {
      setFlowableDiagramContent(null);
      // Show bpmn-js viewer after a tick to allow React to update DOM
      setShowBpmnJsViewer(false);
      setTimeout(() => {
        setShowBpmnJsViewer(true);
        if (visualization.bpmnXml) {
          renderBpmnDiagram(visualization.bpmnXml, visualization.activityStates);
        }
      }, 0);
    }
  }, [renderMode, visualization]);

  // Add resize observer to adjust viewer when container resizes
  useEffect(() => {
    if (!containerRef.current || !viewerRef.current || renderMode !== 'bpmnjs') return;

    const resizeObserver = new ResizeObserver(entries => {
      for (const entry of entries) {
        if (!viewerRef.current || !containerRef.current) return;
        
        try {
          // @ts-ignore
          const canvas = viewerRef.current?.get('canvas');
          if (canvas && containerRef.current) {
            const containerRect = containerRef.current.getBoundingClientRect();
            // Only resize if container has dimensions
            if (containerRect.width > 0 && containerRect.height > 0) {
              // @ts-ignore
              canvas.resized();
              
              // Reapply zoom to ensure proper fit
              setTimeout(() => {
                if (viewerRef.current) {
                  try {
                    // @ts-ignore
                    const currentZoom = canvas.zoom();
                    if (currentZoom < 1.0) {
                      // @ts-ignore
                      canvas.zoom(1.0);
                    }
                  } catch (e) {
                    // Ignore errors during deferred zoom
                  }
                }
              }, 50);
            }
          }
        } catch (e) {
          console.warn('Error in resize observer:', e);
        }
      }
    });

    resizeObserver.observe(containerRef.current);

    return () => {
      resizeObserver.disconnect();
    };
  }, [viewerRef, renderMode]);

  const cleanupViewer = () => {
    if (viewerRef.current) {
      try {
        viewerRef.current.destroy();
      } catch (e) {
        console.warn('Error destroying viewer:', e);
      }
      viewerRef.current = null;
    }
  };

  const loadSubprocessVisualization = async () => {
    console.log('Loading subprocess visualization for plan item:', planItemInstanceId);
    try {
      setLoading(true);
      setError(null);
      console.log('About to call API...');
      const response = await caseApi.getSubprocessVisualization(planItemInstanceId);
      console.log('Subprocess visualization response:', response.data);
      console.log('Response data keys:', Object.keys(response.data));
      setVisualization(response.data);
      console.log('Visualization state set, loading:', false);
      
      // Initial render will be triggered by useEffect when renderMode is checked
      if (renderMode === 'bpmnjs' && response.data.bpmnXml) {
        console.log('Will render BPMN diagram in bpmnjs mode');
        setTimeout(() => {
          renderBpmnDiagram(response.data.bpmnXml, response.data.activityStates);
        }, 0);
      } else if (renderMode === 'flowable') {
        console.log('Will load Flowable diagram');
        await loadFlowableDiagram();
      } else {
        console.log('No diagram to render - renderMode:', renderMode, ', hasBpmnXml:', !!response.data.bpmnXml);
      }
    } catch (err: any) {
      console.error('Failed to load subprocess visualization:', err);
      console.error('Error response:', err.response);
      setError(err.response?.data?.message || err.message || 'Failed to load subprocess visualization');
    } finally {
      console.log('Finally block, setting loading to false');
      setLoading(false);
    }
  };

  const loadFlowableDiagram = async () => {
    console.log('Loading Flowable generated diagram for plan item:', planItemInstanceId);
    try {
      const response = await caseApi.getSubprocessDiagram(planItemInstanceId);
      console.log('Flowable diagram loaded, length:', response.data.length);
      
      // Check if it's a base64 PNG data URL
      if (response.data.startsWith('data:image/png;base64,')) {
        console.log('Flowable diagram is PNG format');
        setFlowableDiagramContent({ type: 'png', content: response.data });
      } else {
        console.log('Flowable diagram is SVG format');
        setFlowableDiagramContent({ type: 'svg', content: response.data });
      }
    } catch (err: any) {
      console.error('Failed to load Flowable diagram:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load Flowable diagram');
    }
  };

  const handleRenderModeChange = (checked: boolean) => {
    const newMode = checked ? 'flowable' : 'bpmnjs';
    console.log('Render mode changed to:', newMode);
    setRenderMode(newMode);
  };

  const renderBpmnDiagram = async (bpmnXml: string, activityStates: ActivityState[]) => {
    console.log('Rendering BPMN diagram with', activityStates.length, 'activities');
    
    // Wait for container to be available
    await new Promise(resolve => setTimeout(resolve, 100));

    if (!containerRef.current) {
      console.error('Container ref is not available');
      return;
    }

    // Destroy existing viewer
    cleanupViewer();

    try {
      // Initialize BPMN viewer
      const viewer = new Viewer({
        container: containerRef.current,
      });
      viewerRef.current = viewer;
      console.log('BPMN viewer initialized');

      // Import BPMN XML
      await viewer.importXML(bpmnXml);
      console.log('BPMN XML imported successfully');

      // Apply state highlights
      applyStateHighlights(viewer, activityStates);

      // Force container height update
      if (containerRef.current) {
        const bjsContainer = containerRef.current.querySelector('.bjs-container');
        if (bjsContainer) {
          (bjsContainer as HTMLElement).style.height = '100%';
          (bjsContainer as HTMLElement).style.minHeight = '100%';
          
          const djsContainer = bjsContainer.querySelector('.djs-container');
          if (djsContainer) {
            (djsContainer as HTMLElement).style.height = '100%';
            (djsContainer as HTMLElement).style.minHeight = '100%';
            
            const viewport = djsContainer.querySelector('.viewport');
            if (viewport) {
              (viewport as HTMLElement).style.height = '100%';
              (viewport as HTMLElement).style.minHeight = '100%';
            }
          }
        }
      }

      // Wait for DOM to be fully rendered
      await new Promise(resolve => setTimeout(resolve, 100));

      // Get current zoom level
      // @ts-ignore
      const canvas = viewer.get('canvas');
      
      // First fit to viewport
      if (canvas && containerRef.current) {
        // Ensure container has dimensions
        const containerRect = containerRef.current.getBoundingClientRect();
        if (containerRect.width > 0 && containerRect.height > 0) {
          try {
            // @ts-ignore
            canvas.zoom('fit-viewport', true);
            
            // Get the current zoom level after fit-viewport
            // @ts-ignore
            const currentZoom = canvas.zoom();
            console.log('Current zoom level after fit-viewport:', currentZoom);
            
            // If zoom is less than 1.0, increase it to make diagram larger
            if (currentZoom < 1.0) {
              // @ts-ignore
              canvas.zoom(1.0);
              console.log('Zoom level adjusted to 1.0');
            }
            
            console.log('Canvas zoomed to fit viewport');
          } catch (e) {
            console.warn('Error during zoom operation:', e);
          }
        } else {
          console.warn('Container has no dimensions yet, deferring zoom');
        }
      }

      // Trigger a resize after a short delay to ensure full render
      setTimeout(() => {
        // @ts-ignore
        const canvas = viewer.get('canvas');
        if (canvas && containerRef.current) {
          const containerRect = containerRef.current.getBoundingClientRect();
          if (containerRect.width > 0 && containerRect.height > 0) {
            try {
              // @ts-ignore
              canvas.resized();
              
              // Reapply zoom after resize
              // @ts-ignore
              const currentZoom = canvas.zoom();
              if (currentZoom < 1.0) {
                // @ts-ignore
                canvas.zoom(1.0);
              }
            } catch (e) {
              console.warn('Error during resize:', e);
            }
          }
        }
      }, 100);

      // Another resize trigger with longer delay
      setTimeout(() => {
        if (containerRef.current) {
          const bjsContainer = containerRef.current.querySelector('.bjs-container');
          if (bjsContainer) {
            const svg = bjsContainer.querySelector('svg');
            if (svg) {
              // Get container dimensions
              const containerRect = containerRef.current.getBoundingClientRect();
              const containerHeight = containerRect.height;
              
              // Get SVG dimensions
              const svgHeight = svg.clientHeight || 0;
              console.log('Container height:', containerHeight, 'SVG height:', svgHeight);
              
              // If SVG is much smaller than container, increase zoom
              if (svgHeight < containerHeight * 0.7 && svgHeight > 0) {
                // @ts-ignore
                const canvas = viewer.get('canvas');
                if (canvas) {
                  // Calculate zoom needed to fill about 80% of container
                  const targetZoom = (containerHeight * 0.8) / svgHeight;
                  try {
                    // @ts-ignore
                    canvas.zoom(Math.max(1.0, Math.min(targetZoom, 2.0)));
                    console.log('Adjusted zoom to fill container:', Math.max(1.0, Math.min(targetZoom, 2.0)));
                  } catch (e) {
                    console.warn('Error during final zoom adjustment:', e);
                  }
                }
              }
            }
          }
        }
      }, 200);

      console.log('BPMN diagram rendered successfully');
    } catch (err: any) {
      console.error('Failed to render BPMN diagram:', err);
      throw err;
    }
  };

  const applyStateHighlights = (viewer: any, activityStates: ActivityState[]) => {
    const elementRegistry = viewer.get('elementRegistry');
    const overlays = viewer.get('overlays');

    console.log('Applying state highlights for', activityStates.length, 'activities');

    // Create activity ID to state mapping
    const activityStateMap = new Map<string, ActivityState>();
    activityStates.forEach(activity => {
      activityStateMap.set(activity.activityId, activity);
    });

    // Get all elements
    const allElements = elementRegistry.getAll();
    console.log('Total elements in diagram:', allElements.length);

    let matchedCount = 0;
    allElements.forEach((element: any) => {
      if (!element.businessObject) return;

      const elementId = element.businessObject.id;
      const activityState = activityStateMap.get(elementId);

      if (activityState) {
        matchedCount++;
        
        // Get the graphics element
        const gfx = elementRegistry.getGraphics(element);
        if (!gfx) {
          console.error(`No graphics found for element ${elementId}`);
          return;
        }

        const svgElement = gfx;

        // Remove previous state classes
        svgElement.classList.remove(
          'bpmn-activity-active',
          'bpmn-activity-completed',
          'bpmn-activity-available'
        );

        // Add appropriate class based on state
        const stateClass = `bpmn-activity-${activityState.state}`;
        svgElement.classList.add(stateClass);

        // Add overlay for better visibility
        const color = activityState.state === 'active' 
          ? '#52c41a' 
          : activityState.state === 'completed' 
          ? '#1890ff' 
          : '#d9d9d9';

        overlays.add(elementId, {
          position: {
            bottom: 0,
            left: 0
          },
          html: `<div style="
            background: ${color};
            color: white;
            padding: 1px 4px;
            font-size: 8px;
            border-radius: 2px;
            font-weight: bold;
            line-height: 1;
          ">${activityState.state.toUpperCase()}</div>`
        });
      }
    });

    console.log(`Matched ${matchedCount} of ${activityStates.length} activities to diagram elements`);
  };

  return (
    <Modal
      title={visualization?.processDefinitionName || 'BPMN 子流程可视化'}
      open={true}
      onCancel={onClose}
      footer={null}
      width={1200}
      style={{ top: 20 }}
      destroyOnClose
      bodyStyle={{ padding: 0, height: '80vh' }}
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px' }}>
          <Spin size="large" />
          <p style={{ marginTop: 16 }}>加载子流程可视化...</p>
        </div>
      ) : error ? (
        <Alert
          message="加载失败"
          description={error}
          type="error"
          showIcon
          style={{ margin: 16 }}
        />
      ) : !visualization ? (
        <Empty description="没有子流程数据" style={{ padding: '60px' }} />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          {/* 流程信息 */}
          <div style={{ padding: '16px', borderBottom: '1px solid #f0f0f0' }}>
            <Descriptions
              bordered
              size="small"
              column={2}
            >
              <Descriptions.Item label="流程实例 ID" span={2}>
                <code>{visualization.processInstanceId}</code>
              </Descriptions.Item>
              <Descriptions.Item label="流程定义 Key">
                <code>{visualization.processDefinitionKey}</code>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag
                  color={
                    visualization.processInstanceState === 'active'
                      ? 'green'
                      : 'default'
                  }
                >
                  {visualization.processInstanceState?.toUpperCase()}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="开始时间">
                {visualization.startTime
                  ? new Date(visualization.startTime).toLocaleString()
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="结束时间">
                {visualization.endTime
                  ? new Date(visualization.endTime).toLocaleString()
                  : '-'}
              </Descriptions.Item>
            </Descriptions>
          </div>

          {/* 图例 */}
          <div
            style={{
              padding: '8px 16px',
              display: 'flex',
              gap: 16,
              alignItems: 'center',
              justifyContent: 'space-between',
              background: '#fafafa',
              borderBottom: '1px solid #f0f0f0'
            }}
          >
            <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
              <span style={{ fontWeight: 'bold' }}>图例:</span>
              <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <span
                  style={{
                    width: 16,
                    height: 16,
                    borderRadius: '2px',
                    backgroundColor: '#52c41a',
                    border: '2px solid #389e0d'
                  }}
                />
                <span>Active (活动)</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <span
                  style={{
                    width: 16,
                    height: 16,
                    borderRadius: '2px',
                    backgroundColor: '#1890ff',
                    border: '2px solid #096dd9'
                  }}
                />
                <span>Completed (已完成)</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <span
                  style={{
                    width: 16,
                    height: 16,
                    borderRadius: '2px',
                    backgroundColor: '#d9d9d9',
                    border: '2px solid #bfbfbf'
                  }}
                />
                <span>Available (可用)</span>
              </div>
            </div>
            
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span>渲染模式:</span>
              <Switch
                checked={renderMode === 'flowable'}
                onChange={handleRenderModeChange}
                checkedChildren="Flowable"
                unCheckedChildren="bpmn-js"
              />
            </div>
          </div>

          {/* BPMN 图形 */}
          <div
            style={{
              flex: 1,
              minHeight: 0,
              overflow: 'auto',
              background: '#ffffff',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center'
            }}
          >
            {renderMode === 'bpmnjs' ? (
              showBpmnJsViewer ? (
                <div ref={containerRef} style={{ width: '100%', height: '100%' }} />
              ) : (
                <div style={{ width: '100%', height: '100%' }} />
              )
            ) : flowableDiagramContent ? (
              flowableDiagramContent.type === 'png' ? (
                <img
                  src={flowableDiagramContent.content}
                  alt="BPMN Process Diagram"
                  style={{
                    maxWidth: '100%',
                    maxHeight: '100%',
                    objectFit: 'contain'
                  }}
                />
              ) : (
                <div
                  dangerouslySetInnerHTML={{ __html: flowableDiagramContent.content }}
                  style={{
                    width: '100%',
                    height: '100%',
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center'
                  }}
                />
              )
            ) : null}
          </div>

          {/* 活动列表 */}
          <div style={{ padding: '16px', borderTop: '1px solid #f0f0f0', maxHeight: '25%', overflow: 'auto', minHeight: 0 }}>
            <h4 style={{ marginBottom: 12 }}>活动状态列表 ({visualization.activityStates.length})</h4>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 8 }}>
              {visualization.activityStates.map((activity) => (
                <div
                  key={activity.activityId}
                  style={{
                    padding: '12px',
                    borderRadius: '4px',
                    border: '1px solid',
                    backgroundColor:
                      activity.state === 'active'
                        ? '#f6ffed'
                        : activity.state === 'completed'
                        ? '#e6f7ff'
                        : '#fafafa',
                    borderColor:
                      activity.state === 'active'
                        ? '#b7eb8f'
                        : activity.state === 'completed'
                        ? '#91d5ff'
                        : '#d9d9d9'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
                    <span style={{ fontWeight: 500 }}>
                      {activity.activityName || activity.activityId}
                    </span>
                    <Tag
                      color={
                        activity.state === 'active'
                          ? 'green'
                          : activity.state === 'completed'
                          ? 'blue'
                          : 'default'
                      }
                    >
                      {activity.state}
                    </Tag>
                  </div>
                  <div style={{ fontSize: '12px', color: '#666' }}>
                    {activity.activityType}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </Modal>
  );
};
