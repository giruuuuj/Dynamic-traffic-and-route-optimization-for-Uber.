import React, { useEffect, useRef, useState } from 'react';
import mapboxgl from 'mapbox-gl';
import MapboxGeocoder from '@mapbox/mapbox-gl-geocoder';
import { Card, Button, Select, Space, Typography, Tag, Alert } from 'antd';
import { useRoute } from '../contexts/RouteContext';
import './MapView.css';

const { Text } = Typography;

// Set Mapbox access token (fallback for demo)
const MAPBOX_TOKEN = process.env.REACT_APP_MAPBOX_TOKEN || 'pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpejY4NXVycTA2emYycXBndHRqcmZ3N3gifQ.rJcFIG214AriISLbB6B5aw';

if (MAPBOX_TOKEN) {
    mapboxgl.accessToken = MAPBOX_TOKEN;
}

const MapView = () => {
  const mapContainer = useRef(null);
  const map = useRef(null);
  const [viewport, setViewport] = useState({
    longitude: -74.0060,
    latitude: 40.7128,
    zoom: 12
  });
  const [trafficLayer, setTrafficLayer] = useState(true);
  const [selectedRoute, setSelectedRoute] = useState(null);
  const [mapError, setMapError] = useState(false);
  
  const { 
    calculateRoute, 
    isCalculating, 
    origin, 
    destination, 
    setOrigin, 
    setDestination 
  } = useRoute();

  useEffect(() => {
    if (!mapContainer.current || mapError) return;

    // Check if Mapbox token is available
    if (!MAPBOX_TOKEN) {
      setMapError(true);
      return;
    }

    try {
      const mapInstance = new mapboxgl.Map({
        container: mapContainer.current,
        style: 'mapbox://styles/mapbox/streets-v12',
        center: [viewport.longitude, viewport.latitude],
        zoom: viewport.zoom,
        pitch: 45,
        bearing: -17.6,
        antialias: true
      });

      // Add geocoder control
      const geocoder = new MapboxGeocoder({
        accessToken: MAPBOX_TOKEN,
        mapboxgl: mapboxgl,
        marker: false,
        placeholder: 'Search for a location'
      });

      mapInstance.addControl(geocoder);

      // Handle geocoder results
      geocoder.on('result', (e) => {
        const result = e.result;
        const [lng, lat] = result.center;
        
        if (!origin) {
          setOrigin({ 
            nodeId: result.id, 
            latitude: lat, 
            longitude: lng 
          });
        } else if (!destination) {
          setDestination({ 
            nodeId: result.id, 
            latitude: lat, 
            longitude: lng 
          });
        }
      });

      // Add navigation control
      mapInstance.addControl(new mapboxgl.NavigationControl());

      // Add scale control
      mapInstance.addControl(new mapboxgl.ScaleControl({
        maxWidth: 80,
        unit: 'metric'
      }));

      // Handle map clicks to set origin/destination
      mapInstance.on('click', (e) => {
        const coordinates = e.lngLat;
        
        if (!origin) {
          setOrigin({ 
            nodeId: `click_${Date.now()}`, 
            latitude: coordinates.lat, 
            longitude: coordinates.lng 
          });
        } else if (!destination) {
          setDestination({ 
            nodeId: `click_${Date.now()}`, 
            latitude: coordinates.lat, 
            longitude: coordinates.lng 
          });
        }
      });

      // Handle viewport changes
      mapInstance.on('move', () => {
        const newViewport = {
          longitude: mapInstance.getCenter().lng,
          latitude: mapInstance.getCenter().lat,
          zoom: mapInstance.getZoom()
        };
        setViewport(newViewport);
      });

      map.current = mapInstance;

      return () => {
        mapInstance.remove();
      };
    } catch (error) {
      console.error('Error initializing Mapbox:', error);
      setMapError(true);
    }
  }, [mapError]);

  // Draw route on map when selected
  useEffect(() => {
    if (map.current && selectedRoute && selectedRoute.geometry && !mapError) {
      // Wait for map to be fully loaded before adding layers
      const addRouteLayer = () => {
        // Remove existing route layer
        if (map.current.getLayer('route')) {
          map.current.removeLayer('route');
        }
        if (map.current.getSource('route')) {
          map.current.removeSource('route');
        }

        // Add route layer
        try {
          map.current.addLayer({
            id: 'route',
            type: 'line',
            source: {
              type: 'geojson',
              data: {
                type: 'Feature',
                properties: {},
                geometry: selectedRoute.geometry
              }
            },
            layout: {
              'line-join': 'round',
              'line-cap': 'round'
            },
            paint: {
              'line-color': '#1890ff',
              'line-width': 6,
              'line-opacity': 0.8
            }
          });

          // Fit map to route bounds
          const bounds = new mapboxgl.LngLatBounds();
          selectedRoute.geometry.coordinates.forEach(coord => {
            bounds.extend(coord);
          });
          map.current.fitBounds(bounds, { padding: 50 });
        } catch (error) {
          console.error('Error adding route layer:', error);
        }
      };

      // Check if map style is loaded
      if (map.current.isStyleLoaded()) {
        addRouteLayer();
      } else {
        // Wait for style to load
        map.current.once('style.load', addRouteLayer);
      }
    }
  }, [selectedRoute, map.current, mapError]);

  // Add traffic layer
  useEffect(() => {
    // Remove the problematic traffic layer that's causing style loading errors
    // This will be implemented later when the map is stable
  }, [trafficLayer, map.current, mapError]);

  const handleCalculateRoute = () => {
    if (origin && destination) {
      calculateRoute(origin.nodeId, destination.nodeId, {
        optimizationObjective: 'FASTEST'
      }).then(route => {
        setSelectedRoute(route);
      }).catch(error => {
        console.error('Route calculation failed:', error);
      });
    }
  };

  const handleClearRoute = () => {
    setSelectedRoute(null);
    if (map.current) {
      if (map.current.getLayer('route')) {
        map.current.removeLayer('route');
      }
      if (map.current.getSource('route')) {
        map.current.removeSource('route');
      }
    }
  };

  return (
    <Card 
      title="Interactive Traffic Map" 
      variant="outlined"
      style={{ height: '600px', position: 'relative' }}
    >
      {mapError ? (
        <div style={{ 
          height: '100%', 
          display: 'flex', 
          flexDirection: 'column', 
          justifyContent: 'center', 
          alignItems: 'center',
          padding: '20px'
        }}>
          <Alert
            message="Mapbox Token Required"
            description="To use the interactive map, please set your REACT_APP_MAPBOX_TOKEN environment variable."
            type="warning"
            showIcon
            style={{ marginBottom: '20px', maxWidth: '500px' }}
          />
          <div style={{ textAlign: 'center' }}>
            <Text strong>Map Features Unavailable</Text>
            <br />
            <Text type="secondary">
              You can still use the route calculation controls below.
            </Text>
          </div>
        </div>
      ) : (
        <div ref={mapContainer} className="map-container" style={{ height: '100%', width: '100%' }} />
      )}
      
      <div className="map-controls" style={{ 
        position: 'absolute', 
        top: '10px', 
        left: '10px', 
        zIndex: 1000,
        background: 'white',
        padding: '10px',
        borderRadius: '6px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.15)'
      }}>
        <Space direction="vertical" size="small">
          <Text strong>Route Controls</Text>
          <Button 
            type="primary" 
            onClick={handleCalculateRoute}
            loading={isCalculating}
            disabled={!origin || !destination}
          >
            Calculate Route
          </Button>
          <Button onClick={handleClearRoute}>
            Clear Route
          </Button>
          <div>
            <Text type="secondary">Origin:</Text>
            {origin ? (
              <Tag color="blue">
                {origin.latitude.toFixed(4)}, {origin.longitude.toFixed(4)}
              </Tag>
            ) : (
              <Tag color="default">Click on map</Tag>
            )}
          </div>
          <div>
            <Text type="secondary">Destination:</Text>
            {destination ? (
              <Tag color="green">
                {destination.latitude.toFixed(4)}, {destination.longitude.toFixed(4)}
              </Tag>
            ) : (
              <Tag color="default">Click on map</Tag>
            )}
          </div>
        </Space>
      </div>
      
      {!mapError && (
        <div className="map-legend" style={{ 
          position: 'absolute', 
          bottom: '10px', 
          right: '10px', 
          zIndex: 1000,
          background: 'white',
          padding: '10px',
          borderRadius: '6px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.15)'
        }}>
          <Text strong>Traffic Legend</Text>
          <Space direction="vertical" size="small">
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <div style={{ 
                width: '12px', 
                height: '12px', 
                backgroundColor: '#52c41a', 
                borderRadius: '50%',
                marginRight: '8px'
              }} />
              <Text>Light Traffic</Text>
            </div>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <div style={{ 
                width: '12px', 
                height: '12px', 
                backgroundColor: '#faad14', 
                borderRadius: '50%',
                marginRight: '8px'
              }} />
              <Text>Moderate Traffic</Text>
            </div>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <div style={{ 
                width: '12px', 
                height: '12px', 
                backgroundColor: '#ff4d4f', 
                borderRadius: '50%',
                marginRight: '8px'
              }} />
              <Text>Heavy Traffic</Text>
            </div>
          </Space>
        </div>
      )}
    </Card>
  );
};

export default MapView;
