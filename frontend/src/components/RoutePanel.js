import React from 'react';
import { Card, Button, Select, Space, Typography, Tag, Divider, Badge } from 'antd';
import { useRoute } from '../contexts/RouteContext';

const { Title, Text } = Typography;

const RoutePanel = () => {
  const { 
    selectedRoute, 
    alternativeRoutes, 
    origin, 
    destination, 
    isCalculating,
    calculateRoute,
    clearRoute 
  } = useRoute();

  const handleCalculateRoute = () => {
    if (origin && destination) {
      calculateRoute(origin.nodeId, destination.nodeId, {
        optimizationObjective: 'FASTEST'
      });
    }
  };

  const getStatusBadge = () => {
    if (isCalculating) {
      return <Badge status="processing" text="Calculating..." />;
    } else if (selectedRoute) {
      return <Badge status="success" text="Route Found" />;
    } else {
      return <Badge status="default" text="No Route" />;
    }
  };

  const formatDistance = (meters) => {
    return (meters / 1000).toFixed(2) + ' km';
  };

  const formatTime = (seconds) => {
    const minutes = Math.round(seconds / 60);
    return minutes + ' min';
  };

  const formatSpeed = (kmh) => {
    return kmh.toFixed(1) + ' km/h';
  };

  return (
    <Card 
      title={
        <div className="uber-panel-title">
          🛣️ Route Information
        </div>
      }
      variant="outlined"
      className="uber-card"
    >
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <div>
          <Text strong className="uber-body">Route Status:</Text>
          <div style={{ marginTop: 8 }}>
            {getStatusBadge()}
          </div>
        </div>

        <Divider />

        <div>
          <Text strong className="uber-body">Origin:</Text>
          <div style={{ marginTop: 8 }}>
            {origin ? (
              <Tag color="blue" className="uber-status-active">
                📍 {origin.latitude.toFixed(4)}, {origin.longitude.toFixed(4)}
              </Tag>
            ) : (
              <Tag color="default" className="uber-status-default">
                📍 Click on map to set origin
              </Tag>
            )}
          </div>
        </div>

        <div>
          <Text strong className="uber-body">Destination:</Text>
          <div style={{ marginTop: 8 }}>
            {destination ? (
              <Tag color="green" className="uber-status-active">
                🎯 {destination.latitude.toFixed(4)}, {destination.longitude.toFixed(4)}
              </Tag>
            ) : (
              <Tag color="default" className="uber-status-default">
                🎯 Click on map to set destination
              </Tag>
            )}
          </div>
        </div>

        <Divider />

        <Space>
          <Button 
            type="primary" 
            className="uber-btn-primary"
            onClick={handleCalculateRoute}
            loading={isCalculating}
            disabled={!origin || !destination}
            size="large"
            block
          >
            {isCalculating ? 'Calculating Route...' : 'Calculate Route'}
          </Button>
          <Button 
            className="uber-btn-secondary"
            onClick={clearRoute}
            size="large"
          >
            Clear Route
          </Button>
        </Space>

        {selectedRoute && (
          <>
            <Divider />
            <div className="uber-fade-in">
              <div className="uber-panel-title" style={{ marginBottom: '16px' }}>
                📊 Route Details
              </div>
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Text className="uber-body">Distance:</Text>
                  <Text strong className="uber-body">{formatDistance(selectedRoute.totalDistance)}</Text>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Text className="uber-body">Travel Time:</Text>
                  <Text strong className="uber-body">{formatTime(selectedRoute.totalTravelTime)}</Text>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Text className="uber-body">Average Speed:</Text>
                  <Text strong className="uber-body">{formatSpeed(selectedRoute.averageSpeed)}</Text>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Text className="uber-body">Traffic Lights:</Text>
                  <Text strong className="uber-body">{selectedRoute.trafficLights}</Text>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Text className="uber-body">Toll Roads:</Text>
                  <Text strong className="uber-body">{selectedRoute.tollRoads}</Text>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Text className="uber-body">Confidence:</Text>
                  <Text strong className="uber-body">{(selectedRoute.confidenceScore * 100).toFixed(1)}%</Text>
                </div>
              </Space>
            </div>
          </>
        )}

        {alternativeRoutes && alternativeRoutes.length > 1 && (
          <>
            <Divider />
            <div className="uber-fade-in">
              <div className="uber-panel-title" style={{ marginBottom: '16px' }}>
                🔄 Alternative Routes
              </div>
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                {alternativeRoutes.slice(1, 4).map((route, index) => (
                  <div 
                    key={route.routeId} 
                    className="uber-panel"
                    style={{ 
                      padding: '12px', 
                      border: '1px solid #e8e8e8', 
                      borderRadius: '8px',
                      cursor: 'pointer',
                      transition: 'all 0.2s ease'
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <Text strong className="uber-body">Option {index + 1}</Text>
                        <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                          <Tag color="blue" className="uber-caption">
                            {formatDistance(route.totalDistance)}
                          </Tag>
                          <Tag color="green" className="uber-caption">
                            {formatTime(route.totalTravelTime)}
                          </Tag>
                        </div>
                      </div>
                      <Tag 
                        color={route.routeType === 'FASTEST' ? 'blue' : route.routeType === 'SHORTEST' ? 'green' : 'default'}
                        className="uber-caption"
                      >
                        {route.routeType}
                      </Tag>
                    </div>
                  </div>
                ))}
              </Space>
            </div>
          </>
        )}
      </Space>
    </Card>
  );
};

export default RoutePanel;
