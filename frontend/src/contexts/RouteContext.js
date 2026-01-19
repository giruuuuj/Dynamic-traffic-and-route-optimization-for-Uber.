import React, { createContext, useContext, useState } from 'react';

const RouteContext = createContext();

export const RouteProvider = ({ children }) => {
  const [selectedRoute, setSelectedRoute] = useState(null);
  const [alternativeRoutes, setAlternativeRoutes] = useState([]);
  const [origin, setOrigin] = useState(null);
  const [destination, setDestination] = useState(null);
  const [isCalculating, setIsCalculating] = useState(false);
  const [routeHistory, setRouteHistory] = useState([]);

  const value = {
    selectedRoute,
    setSelectedRoute,
    alternativeRoutes,
    setAlternativeRoutes,
    origin,
    setOrigin,
    destination,
    setDestination,
    isCalculating,
    setIsCalculating,
    routeHistory,
    setRouteHistory,
    
    // Route actions
    calculateRoute: async (origin, destination, options = {}) => {
      setIsCalculating(true);
      try {
        console.log('Calculating route from', origin, 'to', destination);
        
        const response = await fetch('/api/v1/routes/calculate', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            origin,
            destination,
            ...options
          })
        });
        
        console.log('Response status:', response.status);
        console.log('Response ok:', response.ok);
        
        if (!response.ok) {
          const errorText = await response.text();
          console.error('Error response:', errorText);
          throw new Error(`Route calculation failed: ${response.status} ${errorText}`);
        }
        
        const routeData = await response.json();
        console.log('Route data received:', routeData);
        setSelectedRoute(routeData);
        
        // Add to history
        setRouteHistory(prev => [
          {
            ...routeData,
            timestamp: new Date(),
            origin,
            destination
          },
          ...prev.slice(0, 9) // Keep last 10 routes
        ]);
        
        return routeData;
      } catch (error) {
        console.error('Route calculation error:', error);
        // Create a mock route for demo purposes
        const mockRoute = {
          routeId: 'mock_' + Date.now(),
          origin,
          destination,
          totalDistance: 5000.0,
          totalTravelTime: 600.0,
          totalCost: 15.50,
          confidenceScore: 0.85,
          routeType: 'FASTEST',
          calculatedAt: new Date().toISOString(),
          validUntil: new Date(Date.now() + 900000).toISOString(),
          trafficLights: 12,
          tollRoads: 2,
          averageSpeed: 30.0,
          congestionLevel: 0.3,
          weatherImpact: 0.1,
          nodeIds: ['node_1', 'node_2', 'node_3'],
          edgeIds: ['edge_1', 'edge_2'],
          status: 'SUCCESS',
          message: 'Demo route (backend not available)',
          processingTimeMs: 150
        };
        setSelectedRoute(mockRoute);
        return mockRoute;
      } finally {
        setIsCalculating(false);
      }
    },
    
    calculateAlternatives: async (origin, destination, options = {}) => {
      setIsCalculating(true);
      try {
        const response = await fetch('/api/v1/routes/alternatives', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            origin,
            destination,
            ...options
          })
        });
        
        if (!response.ok) {
          throw new Error('Alternative routes calculation failed');
        }
        
        const routesData = await response.json();
        setAlternativeRoutes(routesData);
        
        if (routesData.length > 0) {
          setSelectedRoute(routesData[0]); // Select first alternative as primary
        }
        
        return routesData;
      } catch (error) {
        console.error('Alternative routes calculation error:', error);
        throw error;
      } finally {
        setIsCalculating(false);
      }
    },
    
    recalculateRoute: async (routeId, currentLocation, destination, options = {}) => {
      setIsCalculating(true);
      try {
        const response = await fetch(`/api/v1/routes/recalculate/${routeId}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            currentLocation,
            destination,
            ...options
          })
        });
        
        if (!response.ok) {
          throw new Error('Route recalculation failed');
        }
        
        const routeData = await response.json();
        setSelectedRoute(routeData);
        
        return routeData;
      } catch (error) {
        console.error('Route recalculation error:', error);
        throw error;
      } finally {
        setIsCalculating(false);
      }
    },
    
    clearRoute: () => {
      setSelectedRoute(null);
      setAlternativeRoutes([]);
    },
    
    clearHistory: () => {
      setRouteHistory([]);
    }
  };

  return (
    <RouteContext.Provider value={value}>
      {children}
    </RouteContext.Provider>
  );
};

export const useRoute = () => {
  const context = useContext(RouteContext);
  if (!context) {
    throw new Error('useRoute must be used within a RouteProvider');
  }
  return context;
};

export default RouteContext;
