package com.uber.traffic.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a traffic incident that affects routing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {
    
    private String id;
    private IncidentType type;
    private Severity severity;
    private double latitude;
    private double longitude;
    private double radius; // Affected radius in meters
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    public enum IncidentType {
        ACCIDENT,
        CONSTRUCTION,
        WEATHER,
        EVENT,
        ROAD_CLOSURE,
        VEHICLE_BREAKDOWN
    }
    
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * Check if the incident is currently active
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return !startTime.isAfter(now) && !endTime.isBefore(now);
    }
    
    /**
     * Get the duration of the incident in minutes
     */
    public long getDurationMinutes() {
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
    
    /**
     * Check if the incident affects a given location
     */
    public boolean affectsLocation(double lat, double lng) {
        double distance = calculateDistance(latitude, longitude, lat, lng);
        return distance <= radius;
    }
    
    /**
     * Calculate distance between two points in meters
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double latDiff = Math.toRadians(lat2 - lat1);
        double lngDiff = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(latDiff/2) * Math.sin(latDiff/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(lngDiff/2) * Math.sin(lngDiff/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return 6371000 * c; // Earth's radius in meters
    }
    
    /**
     * Get the impact multiplier for routing calculations
     */
    public double getImpactMultiplier() {
        switch (severity) {
            case LOW:
                return 1.2;
            case MEDIUM:
                return 1.5;
            case HIGH:
                return 2.0;
            case CRITICAL:
                return 3.0;
            default:
                return 1.0;
        }
    }
}
