package com.uber.traffic.algorithm;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {
    
    private String routeId;
    private List<String> nodeIds;
    private List<String> edgeIds;
    private Double totalDistance; // in meters
    private Double totalTravelTime; // in seconds
    private Double totalCost; // monetary cost
    private Double confidenceScore; // 0.0 to 1.0
    private RouteType routeType;
    private LocalDateTime calculatedAt;
    private LocalDateTime validUntil;
    
    // Route characteristics
    private Integer trafficLights;
    private Integer tollRoads;
    private Double averageSpeed; // km/h
    private Double congestionLevel; // 0.0 to 1.0
    private Double weatherImpact; // 0.0 to 1.0
    
    // Alternative routes
    private List<Route> alternativeRoutes;
    
    public Route(String routeId, List<String> nodeIds, List<String> edgeIds) {
        this.routeId = routeId;
        this.nodeIds = new ArrayList<>(nodeIds);
        this.edgeIds = new ArrayList<>(edgeIds);
        this.calculatedAt = LocalDateTime.now();
        this.validUntil = LocalDateTime.now().plusMinutes(15);
        this.alternativeRoutes = new ArrayList<>();
    }
    
    public void addNode(String nodeId) {
        this.nodeIds.add(nodeId);
    }
    
    public void addEdge(String edgeId) {
        this.edgeIds.add(edgeId);
    }
    
    public String getStartNodeId() {
        return nodeIds.isEmpty() ? null : nodeIds.get(0);
    }
    
    public String getEndNodeId() {
        return nodeIds.isEmpty() ? null : nodeIds.get(nodeIds.size() - 1);
    }
    
    public int getNodeCount() {
        return nodeIds.size();
    }
    
    public int getEdgeCount() {
        return edgeIds.size();
    }
    
    public boolean isValid() {
        return LocalDateTime.now().isBefore(validUntil);
    }
    
    public void addAlternativeRoute(Route alternative) {
        this.alternativeRoutes.add(alternative);
    }
    
    public enum RouteType {
        FASTEST, SHORTEST, ECONOMICAL, SCENIC, ECO_FRIENDLY, AVOID_TOLLS
    }
}
