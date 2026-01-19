package com.uber.traffic.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteResponse {
    
    private String routeId;
    private String origin;
    private String destination;
    private List<String> nodeIds;
    private List<String> edgeIds;
    private Double totalDistance; // in meters
    private Double totalTravelTime; // in seconds
    private Double totalCost; // in currency units
    private Double confidenceScore; // 0.0 to 1.0
    private String routeType; // FASTEST, SHORTEST, ECONOMICAL, etc.
    private LocalDateTime calculatedAt;
    private LocalDateTime validUntil;
    
    // Route characteristics
    private Integer trafficLights;
    private Integer tollRoads;
    private Double averageSpeed; // km/h
    private Double congestionLevel; // 0.0 to 1.0
    private Double weatherImpact; // 0.0 to 1.0
    
    // Alternative routes
    private List<RouteResponse> alternativeRoutes;
    
    // Additional metadata
    private String status; // SUCCESS, NO_ROUTE_FOUND, ERROR
    private String message;
    private Long processingTimeMs; // Time taken to calculate route
    
    // Route geometry for mapping
    private List<Coordinate> geometry;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Coordinate {
        private Double latitude;
        private Double longitude;
        private Double elevation;
    }
}
