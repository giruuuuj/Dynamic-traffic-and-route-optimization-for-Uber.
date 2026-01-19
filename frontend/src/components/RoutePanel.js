import React from 'react';
import { Card, Button, Select, Space, Typography, Tag, Divider } from 'antd';
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

  return (
    <Card title="Route Information" variant="outlined">
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <div>
          <Text strong>Route Status:</Text>
          {isCalculating ? (
            <Tag color="processing">Calculating...</Tag>
          ) : selectedRoute ? (
            <Tag color="success">Route Found</Tag>
          ) : (
            <Tag color="default">No Route</Tag>
          )}
        </div>

        <div>
          <Text strong>Origin:</Text>
          {origin ? (
            <Tag color="blue">
              {origin.latitude.toFixed(4)}, {origin.longitude.toFixed(4)}
            </Tag>
          ) : (
            <Text type="secondary">Not set</Text>
          )}
        </div>

        <div>
          <Text strong>Destination:</Text>
          {destination ? (
            <Tag color="green">
              {destination.latitude.toFixed(4)}, {destination.longitude.toFixed(4)}
            </Tag>
          ) : (
            <Text type="secondary">Not set</Text>
          )}
        </div>

        <Divider />

        <Space>
          <Button 
            type="primary" 
            onClick={handleCalculateRoute}
            loading={isCalculating}
            disabled={!origin || !destination}
          >
            Calculate Route
          </Button>
          <Button onClick={clearRoute}>
            Clear
          </Button>
        </Space>

        {selectedRoute && (
          <div>
            <Title level={5}>Route Details</Title>
            <Space direction="vertical" size="small">
              <div>
                <Text>Distance: </Text>
                <Text strong>{(selectedRoute.totalDistance / 1000).toFixed(2)} km</Text>
              </div>
              <div>
                <Text>Travel Time: </Text>
                <Text strong>{Math.round(selectedRoute.totalTravelTime / 60)} min</Text>
              </div>
              <div>
                <Text>Average Speed: </Text>
                <Text strong>{selectedRoute.averageSpeed.toFixed(1)} km/h</Text>
              </div>
              <div>
                <Text>Traffic Lights: </Text>
                <Text strong>{selectedRoute.trafficLights}</Text>
              </div>
              <div>
                <Text>Toll Roads: </Text>
                <Text strong>{selectedRoute.tollRoads}</Text>
              </div>
              <div>
                <Text>Confidence: </Text>
                <Text strong>{(selectedRoute.confidenceScore * 100).toFixed(1)}%</Text>
              </div>
            </Space>
          </div>
        )}

        {alternativeRoutes && alternativeRoutes.length > 1 && (
          <div>
            <Title level={5}>Alternative Routes</Title>
            <Space direction="vertical" size="small">
              {alternativeRoutes.slice(1, 4).map((route, index) => (
                <div key={route.routeId} style={{ 
                  padding: '8px', 
                  border: '1px solid #d9d9d9', 
                  borderRadius: '4px' 
                }}>
                  <Text strong>Option {index + 1}: </Text>
                  <Text>{(route.totalDistance / 1000).toFixed(2)} km, </Text>
                  <Text>{Math.round(route.totalTravelTime / 60)} min</Text>
                  <Tag color={route.routeType === 'FASTEST' ? 'blue' : 'default'}>
                    {route.routeType}
                  </Tag>
                </div>
              ))}
            </Space>
          </div>
        )}
      </Space>
    </Card>
  );
};

export default RoutePanel;
