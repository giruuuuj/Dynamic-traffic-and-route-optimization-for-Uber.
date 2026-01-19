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
public class GraphNode {
    
    private String id;
    private Double latitude;
    private Double longitude;
    private Double elevation;
    private Boolean hasTrafficLight;
    private Integer trafficLightDuration;
    
    @Builder.Default
    private Map<String, GraphEdge> outgoingEdges = new HashMap<>();
    
    @Builder.Default
    private Map<String, GraphEdge> incomingEdges = new HashMap<>();
    
    public void addOutgoingEdge(GraphEdge edge) {
        outgoingEdges.put(edge.getToNodeId(), edge);
    }
    
    public void addIncomingEdge(GraphEdge edge) {
        incomingEdges.put(edge.getFromNodeId(), edge);
    }
    
    public List<GraphEdge> getAllOutgoingEdges() {
        return new ArrayList<>(outgoingEdges.values());
    }
    
    public List<GraphEdge> getAllIncomingEdges() {
        return new ArrayList<>(incomingEdges.values());
    }
}
