package com.uber.traffic.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Node("RoadSegment")
public class RoadSegment {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Property("segmentId")
    private String segmentId;
    
    @Property("name")
    private String name;
    
    @Property("roadType")
    private RoadType roadType;
    
    @Property("distance")
    private Double distance; // in meters
    
    @Property("baseSpeedLimit")
    private Integer baseSpeedLimit; // in km/h
    
    @Property("lanes")
    private Integer lanes;
    
    @Property("oneWay")
    private Boolean oneWay;
    
    @Property("tollRoad")
    private Boolean tollRoad;
    
    @Property("grade")
    private Double grade; // road grade percentage
    
    @Property("surfaceType")
    private String surfaceType;
    
    @Property("createdAt")
    private LocalDateTime createdAt;
    
    @Property("updatedAt")
    private LocalDateTime updatedAt;
    
    @Relationship(type = "CONNECTS", direction = Relationship.Direction.OUTGOING)
    private Intersection fromIntersection;
    
    @Relationship(type = "CONNECTS", direction = Relationship.Direction.INCOMING)
    private Intersection toIntersection;
    
    public RoadSegment(String segmentId, String name, RoadType roadType, 
                       Double distance, Integer baseSpeedLimit, Integer lanes) {
        this.segmentId = segmentId;
        this.name = name;
        this.roadType = roadType;
        this.distance = distance;
        this.baseSpeedLimit = baseSpeedLimit;
        this.lanes = lanes;
        this.oneWay = false;
        this.tollRoad = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum RoadType {
        HIGHWAY, ARTERIAL, COLLECTOR, LOCAL, RESIDENTIAL, BRIDGE, TUNNEL
    }
}
