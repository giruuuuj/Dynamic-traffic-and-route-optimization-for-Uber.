import React, { useState, useEffect } from 'react';
import { Card, Progress, Space, Typography, Tag, Divider, Badge, Alert } from 'antd';
import { useRoute } from '../contexts/RouteContext';

const { Title, Text } = Typography;

const TrafficPanel = () => {
  const { selectedRoute } = useRoute();
  const [trafficData, setTrafficData] = useState({
    overallCongestion: 0.35,
    averageSpeed: 42.5,
    incidents: 3,
    weatherImpact: 0.15,
    timestamp: new Date().toISOString(),
    incidentsList: [
      {
        id: 1,
        type: 'ACCIDENT',
        location: 'Highway 1',
        severity: 'HIGH',
        latitude: 40.7128,
        longitude: -74.0060,
        description: 'Multi-vehicle collision',
        timestamp: new Date().toISOString()
      },
      {
        id: 2,
        type: 'CONSTRUCTION',
        location: 'Main St',
        severity: 'MEDIUM',
        latitude: 40.7589,
        longitude: -73.9851,
        description: 'Road construction work',
        timestamp: new Date().toISOString()
      },
      {
        id: 3,
        type: 'WEATHER',
        location: 'Broadway',
        severity: 'LOW',
        latitude: 40.7614,
        longitude: -73.9776,
        description: 'Light rain affecting visibility',
        timestamp: new Date().toISOString()
      }
    ]
  });

  // Simulate real-time updates
  useEffect(() => {
    const interval = setInterval(() => {
      setTrafficData(prev => ({
        ...prev,
        overallCongestion: Math.max(0, Math.min(1, prev.overallCongestion + (Math.random() - 0.5) * 0.1)),
        averageSpeed: Math.max(20, Math.min(80, prev.averageSpeed + (Math.random() - 0.5) * 5)),
        timestamp: new Date().toISOString()
      }));
    }, 5000);

    return () => clearInterval(interval);
  }, []);

  const getCongestionColor = (congestion) => {
    if (congestion < 0.3) return '#00d084';
    if (congestion < 0.6) return '#ffaa00';
    return '#ff6b6b';
  };

  const getCongestionLevel = (congestion) => {
    if (congestion < 0.3) return 'Light';
    if (congestion < 0.6) return 'Moderate';
    return 'Heavy';
  };

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'HIGH': return '#ff6b6b';
      case 'MEDIUM': return '#ffaa00';
      case 'LOW': return '#00d084';
      default: return '#e8e8e8';
    }
  };

  const getSeverityBadge = (severity) => {
    switch (severity) {
      case 'HIGH': return <Badge status="error" text={severity} />;
      case 'MEDIUM': return <Badge status="warning" text={severity} />;
      case 'LOW': return <Badge status="success" text={severity} />;
      default: return <Badge status="default" text={severity} />;
    }
  };

  const getIncidentIcon = (type) => {
    switch (type) {
      case 'ACCIDENT': return '🚗';
      case 'CONSTRUCTION': return '🚧';
      case 'WEATHER': return '🌧️';
      default: return '⚠️';
    }
  };

  return (
    <Card 
      title={
        <div className="uber-panel-title">
          🚦 Traffic Conditions
        </div>
      }
      variant="outlined"
      className="uber-card"
    >
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <div>
          <Text strong className="uber-body">Overall Congestion:</Text>
          <div style={{ marginTop: 12 }}>
            <Progress 
              percent={trafficData.overallCongestion * 100}
              strokeColor={getCongestionColor(trafficData.overallCongestion)}
              showInfo={false}
              size="small"
            />
            <div style={{ 
              display: 'flex', 
              justifyContent: 'space-between', 
              marginTop: '8px',
              alignItems: 'center'
            }}>
              <Text className="uber-body">{getCongestionLevel(trafficData.overallCongestion)}</Text>
              <Tag 
                color={trafficData.overallCongestion < 0.3 ? 'green' : trafficData.overallCongestion < 0.6 ? 'orange' : 'red'}
                className="uber-caption"
              >
                {(trafficData.overallCongestion * 100).toFixed(1)}%
              </Tag>
            </div>
          </div>
        </div>

        <Divider />

        <div>
          <Text strong className="uber-body">Traffic Metrics:</Text>
          <div style={{ marginTop: 12 }}>
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <Text className="uber-body">Average Speed:</Text>
                <Text strong className="uber-body">{trafficData.averageSpeed.toFixed(1)} km/h</Text>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <Text className="uber-body">Active Incidents:</Text>
                <Text strong className="uber-body">{trafficData.incidents}</Text>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <Text className="uber-body">Weather Impact:</Text>
                <Text strong className="uber-body">{(trafficData.weatherImpact * 100).toFixed(1)}%</Text>
              </div>
            </Space>
          </div>
        </div>

        <Divider />

        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <Text strong className="uber-body">Recent Incidents</Text>
            <Tag color="blue" className="uber-caption">
              Live Updates
            </Tag>
          </div>
          <Space direction="vertical" size="small" style={{ width: '100%' }}>
            {trafficData.incidentsList.map(incident => (
              <div 
                key={incident.id}
                className="uber-panel"
                style={{ 
                  padding: '12px', 
                  border: '1px solid #e8e8e8', 
                  borderRadius: '8px',
                  transition: 'all 0.2s ease'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                      <span style={{ fontSize: '16px' }}>{getIncidentIcon(incident.type)}</span>
                      <Text strong className="uber-body">{incident.location}</Text>
                    </div>
                    <Text className="uber-caption" style={{ display: 'block', marginBottom: '4px' }}>
                      {incident.description}
                    </Text>
                    <Text className="uber-caption" style={{ color: '#6c757d' }}>
                      {new Date(incident.timestamp).toLocaleTimeString()}
                    </Text>
                  </div>
                  <div style={{ marginLeft: '12px' }}>
                    {getSeverityBadge(incident.severity)}
                  </div>
                </div>
              </div>
            ))}
          </Space>
        </div>

        <Divider />

        <div>
          <Text strong className="uber-body">Traffic Flow Indicators:</Text>
          <div style={{ marginTop: 12 }}>
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <div className="uber-traffic-light uber-traffic-light-green"></div>
                <Text className="uber-body">Light Traffic - Free Flow</Text>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <div className="uber-traffic-light uber-traffic-light-yellow"></div>
                <Text className="uber-body">Moderate Traffic - Some Delays</Text>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <div className="uber-traffic-light uber-traffic-light-red"></div>
                <Text className="uber-body">Heavy Traffic - Major Delays</Text>
              </div>
            </Space>
          </div>
        </div>

        <Alert
          message="Real-time Updates"
          description="Traffic data updates every 5 seconds based on current conditions."
          type="info"
          showIcon
          style={{ marginTop: '16px' }}
        />
      </Space>
    </Card>
  );
};

export default TrafficPanel;
