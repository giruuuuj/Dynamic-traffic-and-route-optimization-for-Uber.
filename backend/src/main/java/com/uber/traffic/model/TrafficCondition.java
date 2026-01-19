package com.uber.traffic.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Node("TrafficCondition")
public class TrafficCondition {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Property("segmentId")
    private String segmentId;
    
    @Property("currentSpeed")
    private Double currentSpeed; // km/h
    
    @Property("congestionLevel")
    private CongestionLevel congestionLevel;
    
    @Property("trafficDensity")
    private Double trafficDensity; // vehicles per km
    
    @Property("flowRate")
    private Double flowRate; // vehicles per hour
    
    @Property("incidentType")
    private IncidentType incidentType;
    
    @Property("incidentDescription")
    private String incidentDescription;
    
    @Property("weatherImpact")
    private Double weatherImpact; // 0.0 to 1.0
    
    @Property("visibility")
    private Double visibility; // in meters
    
    @Property("precipitation")
    private Double precipitation; // mm/hour
    
    @Property("temperature")
    private LocalDateTime timestamp;
    private double reliability; // 0.0 to 1.0 (confidence in data)
    
    /**
     * Get the congestion level as a percentage
     */
    public double getCongestionPercentage() {
        return congestionLevel * 100.0;
    }
    
    /**
     * Get the speed reduction percentage
     */
    public double getSpeedReductionPercentage() {
        // Assuming free flow speed of 60 km/h
        double freeFlowSpeed = 60.0;
        return Math.max(0, (freeFlowSpeed - currentSpeed) / freeFlowSpeed * 100.0);
    }
    
    /**
     * Check if traffic is heavy
     */
    public boolean isHeavyTraffic() {
        return congestionLevel > 0.6;
    }
    
    /**
     * Check if traffic is light
     */
    public boolean isLightTraffic() {
        return congestionLevel < 0.3;
    }
    
    /**
     * Get traffic level description
     */
    public String getTrafficLevel() {
        if (congestionLevel < 0.2) {
            return "Very Light";
        } else if (congestionLevel < 0.4) {
            return "Light";
        } else if (congestionLevel < 0.6) {
            return "Moderate";
        } else if (congestionLevel < 0.8) {
            return "Heavy";
        } else {
            return "Very Heavy";
        }
    }
    
    /**
     * Get the travel time multiplier for routing calculations
     */
    public double getTravelTimeMultiplier() {
        return 1.0 + (congestionLevel * 0.5);
    }
    
    /**
     * Check if the data is recent (within last 5 minutes)
     */
    public boolean isRecent() {
        return timestamp.isAfter(LocalDateTime.now().minusMinutes(5));
    }
    
    /**
     * Get the estimated delay in minutes for a 10km segment
     */
    public double getEstimatedDelayMinutes() {
        double freeFlowSpeed = 60.0; // km/h
        double freeFlowTime = (10.0 / freeFlowSpeed) * 60; // minutes
        double currentTime = (10.0 / currentSpeed) * 60; // minutes
        return currentTime - freeFlowTime;
    }
}
