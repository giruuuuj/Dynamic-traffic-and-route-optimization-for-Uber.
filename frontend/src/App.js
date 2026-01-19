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
      <Layout style={{ minHeight: '100vh' }}>
        <Sider 
          collapsible 
          collapsed={collapsed} 
          onCollapse={setCollapsed}
          theme="dark"
          width={250}
        >
          <div className="logo" style={{ height: '32px', margin: '16px', color: 'white' }}>
            🚀 Uber Traffic
          </div>
          <Menu theme="dark" mode="inline" defaultSelectedKeys={['map']} items={[
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
          ]} />
        </Sider>
        <Layout>
          <Header style={{ 
            background: '#fff', 
            padding: '0 24px', 
            display: 'flex', 
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <Title level={3} style={{ margin: 0, color: '#1890ff' }}>
              Dynamic Traffic Optimization
            </Title>
            <Space>
              <Select
                defaultValue="FASTEST"
                style={{ width: 120 }}
                options={[
                  { value: 'FASTEST', label: 'Fastest' },
                  { value: 'SHORTEST', label: 'Shortest' },
                  { value: 'ECONOMICAL', label: 'Economical' },
                  { value: 'ECO_FRIENDLY', label: 'Eco-Friendly' },
                ]}
              />
              <Button type="primary">Calculate Route</Button>
            </Space>
          </Header>
          <Content style={{ margin: '24px 16px', padding: 0, overflow: 'auto' }}>
            <Routes>
              <Route path="/" element={
                <Row gutter={[16, 16]}>
                  <Col span={16}>
                    <Card title="Interactive Map" variant="outlined">
                      <MapView />
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Space direction="vertical" size="large" style={{ width: '100%' }}>
                      <RoutePanel />
                      <TrafficPanel />
                    </Space>
                  </Col>
                </Row>
              } />
              <Route path="/routes" element={
                <div>Routes Management</div>
              } />
              <Route path="/traffic" element={
                <div>Traffic Monitoring</div>
              } />
              <Route path="/analytics" element={
                <div>Analytics Dashboard</div>
              } />
            </Routes>
          </Content>
        </Layout>
      </Layout>
    </RouteProvider>
  );
}

export default App;
