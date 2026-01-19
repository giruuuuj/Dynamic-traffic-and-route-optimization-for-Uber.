package com.uber.traffic.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficCondition {
    
    private String segmentId;
    private double congestionLevel; // 0.0 to 1.0 (0% to 100% congestion)
    private double currentSpeed; // Current average speed in km/h
    private LocalDateTime timestamp;
    private double reliability; // 0.0 to 1.0 (confidence in data)
    private double trafficDensity; // vehicles per km
    private double flowRate; // vehicles per hour
    private String incidentType;
    private String incidentDescription;
    private double weatherImpact; // 0.0 to 1.0
    private double visibility; // in meters
    private double precipitation; // mm/hour
    private double temperature; // in celsius
    
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
