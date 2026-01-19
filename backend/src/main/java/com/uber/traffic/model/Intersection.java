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
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Node("Intersection")
public class Intersection {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Property("nodeId")
    private String nodeId;
    
    @Property("latitude")
    private Double latitude;
    
    @Property("longitude")
    private Double longitude;
    
    @Property("elevation")
    private Double elevation;
    
    @Property("trafficLight")
    private Boolean hasTrafficLight;
    
    @Property("trafficLightDuration")
    private Integer trafficLightDuration;
    
    @Property("createdAt")
    private LocalDateTime createdAt;
    
    @Property("updatedAt")
    private LocalDateTime updatedAt;
    
    public Intersection(String nodeId, Double latitude, Double longitude) {
        this.nodeId = nodeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
