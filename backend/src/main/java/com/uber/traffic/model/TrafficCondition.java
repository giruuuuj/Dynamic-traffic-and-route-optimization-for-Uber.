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
    private Double temperature; // celsius
    
    @Property("confidence")
    private Double confidence; // 0.0 to 1.0
    
    @Property("timestamp")
    private LocalDateTime timestamp;
    
    @Property("expiresAt")
    private LocalDateTime expiresAt;
    
    public TrafficCondition(String segmentId, Double currentSpeed, CongestionLevel congestionLevel) {
        this.segmentId = segmentId;
        this.currentSpeed = currentSpeed;
        this.congestionLevel = congestionLevel;
        this.timestamp = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
        this.confidence = 0.8;
    }
    
    public enum CongestionLevel {
        FREE_FLOW(0.0, 0.2),
        LIGHT(0.2, 0.4),
        MODERATE(0.4, 0.6),
        HEAVY(0.6, 0.8),
        SEVERE(0.8, 1.0);
        
        private final double minRange;
        private final double maxRange;
        
        CongestionLevel(double minRange, double maxRange) {
            this.minRange = minRange;
            this.maxRange = maxRange;
        }
        
        public boolean isInRange(double value) {
            return value >= minRange && value < maxRange;
        }
    }
    
    public enum IncidentType {
        NONE, ACCIDENT, CONSTRUCTION, WEATHER, EVENT, ROAD_CLOSURE, VEHICLE_BREAKDOWN
    }
}
