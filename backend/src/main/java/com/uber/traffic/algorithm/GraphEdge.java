package com.uber.traffic.algorithm;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphEdge {
    
    private String id;
    private String fromNodeId;
    private String toNodeId;
    private String name;
    private RoadType roadType;
    private Double distance; // in meters
    private Integer baseSpeedLimit; // in km/h
    private Integer lanes;
    private Boolean oneWay;
    private Boolean tollRoad;
    private Double grade; // road grade percentage
    private String surfaceType;
    
    // Dynamic traffic properties
    private Double currentSpeed; // km/h
    private Double congestionFactor; // 0.0 to 1.0
    private Double weatherImpact; // 0.0 to 1.0
    private Double trafficDensity; // vehicles per km
    private LocalDateTime lastUpdated;
    
    // Calculated properties
    private Double dynamicWeight; // calculated based on current conditions
    
    public GraphEdge(String id, String fromNodeId, String toNodeId, Double distance, Integer baseSpeedLimit) {
        this.id = id;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.distance = distance;
        this.baseSpeedLimit = baseSpeedLimit;
        this.currentSpeed = baseSpeedLimit.doubleValue();
        this.congestionFactor = 0.0;
        this.weatherImpact = 0.0;
        this.trafficDensity = 0.0;
        this.lastUpdated = LocalDateTime.now();
        this.dynamicWeight = calculateDynamicWeight();
    }
    
    public Double calculateDynamicWeight() {
        // Base travel time = distance / speed
        double baseTime = distance / (currentSpeed * 1000.0 / 3600.0); // in seconds
        
        // Apply dynamic factors
        double congestionMultiplier = 1.0 + congestionFactor;
        double weatherMultiplier = 1.0 + weatherImpact;
        double gradeMultiplier = 1.0 + Math.abs(grade) * 0.1; // 10% impact per grade percent
        
        // Road type multiplier (highways are faster, local roads are slower)
        double roadTypeMultiplier = getRoadTypeMultiplier();
        
        // Traffic light penalty for intersections
        double trafficLightPenalty = hasTrafficLightAtEnd() ? 30.0 : 0.0; // 30 seconds average
        
        this.dynamicWeight = baseTime * congestionMultiplier * weatherMultiplier * 
                           gradeMultiplier * roadTypeMultiplier + trafficLightPenalty;
        
        return this.dynamicWeight;
    }
    
    private Double getRoadTypeMultiplier() {
        if (roadType == null) return 1.0;
        
        switch (roadType) {
            case HIGHWAY: return 0.8; // 20% faster
            case ARTERIAL: return 1.0; // baseline
            case COLLECTOR: return 1.2; // 20% slower
            case LOCAL: return 1.5; // 50% slower
            case RESIDENTIAL: return 1.6; // 60% slower
            case BRIDGE: return 1.1; // 10% slower
            case TUNNEL: return 1.2; // 20% slower
            default: return 1.0;
        }
    }
    
    private Boolean hasTrafficLightAtEnd() {
        // This would be determined by checking the destination node
        // For now, we'll assume major intersections have traffic lights
        return roadType == RoadType.ARTERIAL || roadType == RoadType.COLLECTOR;
    }
    
    public void updateTrafficConditions(Double currentSpeed, Double congestionFactor, 
                                      Double weatherImpact, Double trafficDensity) {
        this.currentSpeed = currentSpeed;
        this.congestionFactor = congestionFactor;
        this.weatherImpact = weatherImpact;
        this.trafficDensity = trafficDensity;
        this.lastUpdated = LocalDateTime.now();
        calculateDynamicWeight();
    }
    
    public enum RoadType {
        HIGHWAY, ARTERIAL, COLLECTOR, LOCAL, RESIDENTIAL, BRIDGE, TUNNEL
    }
}
