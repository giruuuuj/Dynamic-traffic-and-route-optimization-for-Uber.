import React, { useState, useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { Layout, Menu, Button, Card, Row, Col, Select, Input, Space, Typography } from 'antd';
import MapView from './components/MapView';
import RoutePanel from './components/RoutePanel';
import TrafficPanel from './components/TrafficPanel';
import { RouteProvider } from './contexts/RouteContext';
import './App.css';

const { Header, Content, Sider } = Layout;
const { Title } = Typography;

function App() {
  const [collapsed, setCollapsed] = useState(false);
  const [selectedRoute, setSelectedRoute] = useState(null);

  return (
    <RouteProvider>
      <Layout style={{ minHeight: '100vh', backgroundColor: '#f8f9fa' }}>
        <Sider 
          collapsible 
          collapsed={collapsed} 
          onCollapse={setCollapsed}
          className="uber-nav"
          width={280}
          trigger={null}
        >
          <div style={{ 
            padding: '24px 16px', 
            borderBottom: '1px solid #e8e8e8',
            marginBottom: '16px'
          }}>
            <div className="uber-logo" style={{ marginBottom: '4px' }}>
              🚗 Uber
            </div>
            <div className="uber-subtitle">
              Traffic Optimization
            </div>
          </div>
          <Menu 
            mode="inline" 
            defaultSelectedKeys={['map']} 
            className="uber-nav"
            items={[
              {
                key: 'map',
                icon: '🗺️',
                label: 'Map View',
              },
              {
                key: 'routes',
                icon: '🛣️',
                label: 'Routes',
              },
              {
                key: 'traffic',
                icon: '🚦',
                label: 'Traffic',
              },
              {
                key: 'analytics',
                icon: '📊',
                label: 'Analytics',
              }
            ]}
          />
        </Sider>
        <Layout>
          <Header className="uber-header" style={{ 
            padding: '0 32px', 
            display: 'flex', 
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <div>
              <Title level={2} className="uber-heading-1" style={{ margin: 0, color: '#ffffff' }}>
                Dynamic Traffic Optimization
              </Title>
              <div className="uber-subtitle" style={{ marginTop: '4px' }}>
                Real-time route calculation and traffic monitoring
              </div>
            </div>
            <Space>
              <Select
                defaultValue="FASTEST"
                className="uber-btn-secondary"
                style={{ width: 140 }}
                options={[
                  { value: 'FASTEST', label: 'Fastest' },
                  { value: 'SHORTEST', label: 'Shortest' },
                  { value: 'ECONOMICAL', label: 'Economical' },
                  { value: 'ECO_FRIENDLY', label: 'Eco-Friendly' },
                ]}
              />
              <Button type="primary" className="uber-btn-primary">
                Calculate Route
              </Button>
            </Space>
          </Header>
          <Content style={{ 
            margin: '24px 32px', 
            padding: 0, 
            backgroundColor: '#f8f9fa'
          }}>
            <Routes>
              <Route path="/" element={
                <Row gutter={[24, 24]}>
                  <Col span={16}>
                    <Card 
                      className="uber-card uber-fade-in"
                      title={
                        <div className="uber-panel-title">
                          🗺️ Interactive Traffic Map
                        </div>
                      }
                      variant="outlined"
                    >
                      <MapView />
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Space direction="vertical" size="large" style={{ width: '100%' }}>
                      <div className="uber-panel uber-fade-in">
                        <RoutePanel />
                      </div>
                      <div className="uber-panel uber-fade-in">
                        <TrafficPanel />
                      </div>
                    </Space>
                  </Col>
                </Row>
              } />
              <Route path="/routes" element={
                <div className="uber-panel">
                  <h2 className="uber-heading-2">🛣️ Routes Management</h2>
                  <p className="uber-body">Manage and monitor your saved routes</p>
                </div>
              } />
              <Route path="/traffic" element={
                <div className="uber-panel">
                  <h2 className="uber-heading-2">🚦 Traffic Monitoring</h2>
                  <p className="uber-body">Real-time traffic conditions and incidents</p>
                </div>
              } />
              <Route path="/analytics" element={
                <div className="uber-panel">
                  <h2 className="uber-heading-2">📊 Analytics Dashboard</h2>
                  <p className="uber-body">Performance metrics and insights</p>
                </div>
              } />
            </Routes>
          </Content>
        </Layout>
      </Layout>
    </RouteProvider>
  );
}

export default App;
