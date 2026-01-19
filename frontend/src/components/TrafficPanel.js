import React, { useState, useEffect } from 'react';
import { Card, Typography, Progress, Tag, Space, Button } from 'antd';

const { Title, Text } = Typography;

const TrafficPanel = () => {
  const [trafficData, setTrafficData] = useState({
    overallCongestion: 0.3,
    averageSpeed: 45.2,
    incidents: 2,
    weatherImpact: 0.1
  });

  const [incidents, setIncidents] = useState([
    { id: 1, type: 'ACCIDENT', location: 'Highway 1', severity: 'HIGH' },
    { id: 2, type: 'CONSTRUCTION', location: 'Main St', severity: 'MEDIUM' }
  ]);

  useEffect(() => {
    // Simulate real-time traffic updates
    const interval = setInterval(() => {
      setTrafficData(prev => ({
        ...prev,
        overallCongestion: Math.random() * 0.8,
        averageSpeed: 30 + Math.random() * 40,
        weatherImpact: Math.random() * 0.3
      }));
    }, 5000);

    return () => clearInterval(interval);
  }, []);

  const getCongestionColor = (level) => {
    if (level < 0.3) return '#52c41a';
    if (level < 0.6) return '#faad14';
    return '#ff4d4f';
  };

  const getCongestionText = (level) => {
    if (level < 0.3) return 'Light';
    if (level < 0.6) return 'Moderate';
    return 'Heavy';
  };

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'HIGH': return 'red';
      case 'MEDIUM': return 'orange';
      case 'LOW': return 'green';
      default: return 'default';
    }
  };

  return (
    <Card title="Traffic Conditions" variant="outlined">
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <div>
          <Text strong>Overall Congestion:</Text>
          <div style={{ marginTop: 8 }}>
            <Progress 
              percent={trafficData.overallCongestion * 100}
              strokeColor={getCongestionColor(trafficData.overallCongestion)}
              showInfo={false}
              size="small"
            />
            <Tag 
              color={getCongestionColor(trafficData.overallCongestion)}
              style={{ marginLeft: 8 }}
            >
              {getCongestionText(trafficData.overallCongestion)}
            </Tag>
          </div>
        </div>

        <div>
          <Text strong>Average Speed:</Text>
          <Text style={{ marginLeft: 8 }}>
            {trafficData.averageSpeed.toFixed(1)} km/h
          </Text>
        </div>

        <div>
          <Text strong>Weather Impact:</Text>
          <Progress 
            percent={trafficData.weatherImpact * 100}
            strokeColor="#1890ff"
            showInfo={false}
            size="small"
            style={{ marginTop: 4 }}
          />
          <Text type="secondary" style={{ marginLeft: 8, fontSize: '12px' }}>
            {(trafficData.weatherImpact * 100).toFixed(0)}%
          </Text>
        </div>

        <div>
          <Text strong>Active Incidents:</Text>
          <div style={{ marginTop: 8 }}>
            {incidents.map(incident => (
              <div 
                key={incident.id} 
                style={{ 
                  padding: '4px 8px', 
                  marginBottom: '4px',
                  border: '1px solid #d9d9d9',
                  borderRadius: '4px',
                  fontSize: '12px'
                }}
              >
                <Tag color={getSeverityColor(incident.severity)} size="small">
                  {incident.type}
                </Tag>
                <Text style={{ marginLeft: 8 }}>{incident.location}</Text>
              </div>
            ))}
          </div>
        </div>

        <Button type="link" size="small">
          View Detailed Traffic Map
        </Button>
      </Space>
    </Card>
  );
};

export default TrafficPanel;
