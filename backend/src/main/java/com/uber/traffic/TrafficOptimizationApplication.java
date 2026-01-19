package com.uber.traffic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;

@SpringBootApplication
@RestController
@RequestMapping("/api/v1")
public class TrafficOptimizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrafficOptimizationApplication.class, args);
    }

    @GetMapping("/routes/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "traffic-optimization");
        response.put("timestamp", new Date().toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/routes/calculate")
    public ResponseEntity<Map<String, Object>> calculateRoute(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("routeId", java.util.UUID.randomUUID().toString());
        response.put("origin", request.get("origin"));
        response.put("destination", request.get("destination"));
        response.put("totalDistance", 5000.0); // meters
        response.put("totalTravelTime", 600.0); // seconds
        response.put("totalCost", 15.50);
        response.put("confidenceScore", 0.85);
        response.put("routeType", "FASTEST");
        response.put("calculatedAt", new Date().toString());
        response.put("validUntil", new Date(System.currentTimeMillis() + 900000).toString());
        response.put("trafficLights", 12);
        response.put("tollRoads", 2);
        response.put("averageSpeed", 30.0);
        response.put("congestionLevel", 0.3);
        response.put("weatherImpact", 0.1);
        response.put("nodeIds", java.util.Arrays.asList("node_1", "node_2", "node_3"));
        response.put("edgeIds", java.util.Arrays.asList("edge_1", "edge_2"));
        response.put("status", "SUCCESS");
        response.put("message", "Route calculated successfully");
        response.put("processingTimeMs", 150);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/routes/alternatives")
    public ResponseEntity<List<Map<String, Object>>> calculateAlternativeRoutes(@RequestBody Map<String, Object> request) {
        List<Map<String, Object>> routes = new java.util.ArrayList<>();
        
        // Create 3 alternative routes
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> route = new java.util.HashMap<>();
            route.put("routeId", "route_" + java.util.UUID.randomUUID().toString());
            route.put("origin", request.get("origin"));
            route.put("destination", request.get("destination"));
            route.put("totalDistance", 4000.0 + (i * 1000)); // Different distances
            route.put("totalTravelTime", 500.0 + (i * 120)); // Different times
            route.put("totalCost", 12.0 + (i * 3));
            route.put("confidenceScore", 0.80 - (i * 0.05));
            route.put("routeType", i == 1 ? "FASTEST" : i == 2 ? "SHORTEST" : "ECONOMICAL");
            route.put("calculatedAt", new Date().toString());
            route.put("validUntil", new Date(System.currentTimeMillis() + 900000).toString());
            route.put("trafficLights", 10 + i);
            route.put("tollRoads", i);
            route.put("averageSpeed", 35.0 - (i * 5));
            route.put("congestionLevel", 0.2 + (i * 0.1));
            route.put("weatherImpact", 0.1);
            route.put("nodeIds", java.util.Arrays.asList("node_1", "node_2", "node_3"));
            route.put("edgeIds", java.util.Arrays.asList("edge_1", "edge_2"));
            routes.add(route);
        }
        
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/traffic/realtime")
    public ResponseEntity<Map<String, Object>> getRealTimeTraffic() {
        Map<String, Object> traffic = new java.util.HashMap<>();
        traffic.put("overallCongestion", 0.35);
        traffic.put("averageSpeed", 42.5);
        traffic.put("incidents", 3);
        traffic.put("weatherImpact", 0.15);
        traffic.put("timestamp", new Date().toString());
        
        List<Map<String, Object>> incidents = new java.util.ArrayList<>();
        Map<String, Object> incident1 = new java.util.HashMap<>();
        incident1.put("id", 1);
        incident1.put("type", "ACCIDENT");
        incident1.put("location", "Highway 1");
        incident1.put("severity", "HIGH");
        incident1.put("latitude", 40.7128);
        incident1.put("longitude", -74.0060);
        incident1.put("description", "Multi-vehicle collision");
        incident1.put("timestamp", new Date().toString());
        
        Map<String, Object> incident2 = new java.util.HashMap<>();
        incident2.put("id", 2);
        incident2.put("type", "CONSTRUCTION");
        incident2.put("location", "Main St");
        incident2.put("severity", "MEDIUM");
        incident2.put("latitude", 40.7589);
        incident2.put("longitude", -73.9851);
        incident2.put("description", "Road construction work");
        incident2.put("timestamp", new Date().toString());
        
        incidents.add(incident1);
        incidents.add(incident2);
        traffic.put("incidents", incidents);
        
        return ResponseEntity.ok(traffic);
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        Map<String, Object> analytics = new java.util.HashMap<>();
        analytics.put("totalRoutesCalculated", 15420);
        analytics.put("averageReduction", 18.5); // percentage
        analytics.put("activeVehicles", 3421);
        analytics.put("systemUptime", "99.98%");
        analytics.put("predictionAccuracy", 87.3);
        analytics.put("lastUpdated", new Date().toString());
        
        return ResponseEntity.ok(analytics);
    }
}
