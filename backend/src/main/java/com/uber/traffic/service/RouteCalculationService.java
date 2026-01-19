package com.uber.traffic.service;

import com.uber.traffic.algorithm.EnhancedAStar;
import com.uber.traffic.algorithm.GraphNode;
import com.uber.traffic.algorithm.GraphEdge;
import com.uber.traffic.algorithm.Route;
import com.uber.traffic.algorithm.RouteOptimizationCriteria;
import com.uber.traffic.model.Intersection;
import com.uber.traffic.model.RoadSegment;
import com.uber.traffic.repository.IntersectionRepository;
import com.uber.traffic.repository.RoadSegmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RouteCalculationService {
    
    private final EnhancedAStar enhancedAStar;
    private final IntersectionRepository intersectionRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    
    @Autowired
    public RouteCalculationService(EnhancedAStar enhancedAStar,
                               IntersectionRepository intersectionRepository,
                               RoadSegmentRepository roadSegmentRepository) {
        this.enhancedAStar = enhancedAStar;
        this.intersectionRepository = intersectionRepository;
        this.roadSegmentRepository = roadSegmentRepository;
    }
    
    public Route calculateOptimalRoute(String startNodeId, String endNodeId, 
                                   RouteOptimizationCriteria criteria) {
        log.info("Calculating optimal route from {} to {} with criteria: {}", 
                startNodeId, endNodeId, criteria.getPrimaryObjective());
        
        // Build graph from database
        Map<String, GraphNode> graph = buildGraphFromDatabase(startNodeId, endNodeId);
        
        // Calculate route using A* algorithm
        Route route = enhancedAStar.findOptimalRoute(graph, startNodeId, endNodeId, criteria);
        
        if (route != null) {
            log.info("Route calculated successfully. Distance: {}m, Time: {}s, Cost: ${}", 
                    route.getTotalDistance(), route.getTotalTravelTime(), route.getTotalCost());
        } else {
            log.warn("No route found between {} and {}", startNodeId, endNodeId);
        }
        
        return route;
    }
    
    public List<Route> calculateAlternativeRoutes(String startNodeId, String endNodeId,
                                             RouteOptimizationCriteria criteria,
                                             int maxAlternatives) {
        log.info("Calculating {} alternative routes from {} to {}", 
                maxAlternatives, startNodeId, endNodeId);
        
        List<Route> alternatives = new ArrayList<>();
        Map<String, GraphNode> graph = buildGraphFromDatabase(startNodeId, endNodeId);
        
        // Calculate primary route
        Route primaryRoute = enhancedAStar.findOptimalRoute(graph, startNodeId, endNodeId, criteria);
        if (primaryRoute != null) {
            alternatives.add(primaryRoute);
        }
        
        // Calculate alternative routes with different criteria
        RouteOptimizationCriteria.OptimizationObjective[] objectives = {
            RouteOptimizationCriteria.OptimizationObjective.FASTEST,
            RouteOptimizationCriteria.OptimizationObjective.SHORTEST,
            RouteOptimizationCriteria.OptimizationObjective.ECONOMICAL,
            RouteOptimizationCriteria.OptimizationObjective.ECO_FRIENDLY
        };
        
        for (RouteOptimizationCriteria.OptimizationObjective objective : objectives) {
            if (alternatives.size() >= maxAlternatives) break;
            if (objective == criteria.getPrimaryObjective()) continue; // Skip primary objective
            
            RouteOptimizationCriteria altCriteria = new RouteOptimizationCriteria(objective);
            Route altRoute = enhancedAStar.findOptimalRoute(graph, startNodeId, endNodeId, altCriteria);
            
            if (altRoute != null && isRouteUnique(altRoute, alternatives)) {
                alternatives.add(altRoute);
            }
        }
        
        log.info("Found {} alternative routes", alternatives.size());
        return alternatives;
    }
    
    private Map<String, GraphNode> buildGraphFromDatabase(String startNodeId, String endNodeId) {
        log.debug("Building graph from database for route calculation");
        
        Map<String, GraphNode> graph = new HashMap<>();
        
        // Find bounding box to limit the graph size
        Optional<Intersection> startOpt = intersectionRepository.findByNodeId(startNodeId);
        Optional<Intersection> endOpt = intersectionRepository.findByNodeId(endNodeId);
        
        if (startOpt.isEmpty() || endOpt.isEmpty()) {
            throw new IllegalArgumentException("Start or end intersection not found");
        }
        
        Intersection start = startOpt.get();
        Intersection end = endOpt.get();
        
        // Calculate bounding box with some padding
        double minLat = Math.min(start.getLatitude(), end.getLatitude()) - 0.1;
        double maxLat = Math.max(start.getLatitude(), end.getLatitude()) + 0.1;
        double minLon = Math.min(start.getLongitude(), end.getLongitude()) - 0.1;
        double maxLon = Math.max(start.getLongitude(), end.getLongitude()) + 0.1;
        
        // Load intersections within bounding box
        List<Intersection> intersections = intersectionRepository
            .findIntersectionsInBoundingBox(minLat, maxLat, minLon, maxLon);
        
        // Convert to graph nodes
        for (Intersection intersection : intersections) {
            GraphNode node = convertToGraphNode(intersection);
            graph.put(intersection.getNodeId(), node);
        }
        
        // Load road segments and connect nodes
        List<RoadSegment> roadSegments = new ArrayList<>();
        for (Intersection intersection : intersections) {
            roadSegments.addAll(roadSegmentRepository.findOutgoingRoadSegments(intersection.getNodeId()));
        }
        
        // Convert to graph edges and connect nodes
        for (RoadSegment segment : roadSegments) {
            GraphEdge edge = convertToGraphEdge(segment);
            GraphNode fromNode = graph.get(segment.getFromIntersection().getNodeId());
            GraphNode toNode = graph.get(segment.getToIntersection().getNodeId());
            
            if (fromNode != null && toNode != null) {
                fromNode.addOutgoingEdge(edge);
                toNode.addIncomingEdge(edge);
            }
        }
        
        log.debug("Built graph with {} nodes and {} edges", 
                graph.size(), roadSegments.size());
        
        return graph;
    }
    
    private GraphNode convertToGraphNode(Intersection intersection) {
        return GraphNode.builder()
                .id(intersection.getNodeId())
                .latitude(intersection.getLatitude())
                .longitude(intersection.getLongitude())
                .elevation(intersection.getElevation())
                .hasTrafficLight(intersection.getHasTrafficLight())
                .trafficLightDuration(intersection.getTrafficLightDuration())
                .build();
    }
    
    private GraphEdge convertToGraphEdge(RoadSegment segment) {
        return GraphEdge.builder()
                .id(segment.getSegmentId())
                .fromNodeId(segment.getFromIntersection().getNodeId())
                .toNodeId(segment.getToIntersection().getNodeId())
                .name(segment.getName())
                .roadType(GraphEdge.RoadType.valueOf(segment.getRoadType().name()))
                .distance(segment.getDistance())
                .baseSpeedLimit(segment.getBaseSpeedLimit())
                .lanes(segment.getLanes())
                .oneWay(segment.getOneWay())
                .tollRoad(segment.getTollRoad())
                .grade(segment.getGrade())
                .surfaceType(segment.getSurfaceType())
                .currentSpeed(segment.getBaseSpeedLimit().doubleValue())
                .congestionFactor(0.0)
                .weatherImpact(0.0)
                .trafficDensity(0.0)
                .build();
    }
    
    private boolean isRouteUnique(Route newRoute, List<Route> existingRoutes) {
        // Simple uniqueness check based on distance and time similarity
        for (Route existing : existingRoutes) {
            double distanceDiff = Math.abs(existing.getTotalDistance() - newRoute.getTotalDistance());
            double timeDiff = Math.abs(existing.getTotalTravelTime() - newRoute.getTotalTravelTime());
            
            // If routes are very similar in distance and time, consider them duplicates
            if (distanceDiff < 100 && timeDiff < 30) { // 100m, 30s tolerance
                return false;
            }
        }
        return true;
    }
    
    public Route recalculateRoute(String routeId, String currentLocation, 
                              String destination, RouteOptimizationCriteria criteria) {
        log.info("Recalculating route {} from current location {} to {}", 
                routeId, currentLocation, destination);
        
        // Build fresh graph with current traffic conditions
        Map<String, GraphNode> graph = buildGraphFromDatabase(currentLocation, destination);
        
        // Update edge weights with current traffic data
        updateGraphWithRealTimeData(graph);
        
        // Calculate new route
        return enhancedAStar.findOptimalRoute(graph, currentLocation, destination, criteria);
    }
    
    private void updateGraphWithRealTimeData(Map<String, GraphNode> graph) {
        // This would integrate with real-time traffic data from Redis cache
        // For now, we'll simulate some traffic conditions
        Random random = new Random();
        
        for (GraphNode node : graph.values()) {
            for (GraphEdge edge : node.getAllOutgoingEdges()) {
                // Simulate random congestion
                double congestionFactor = random.nextDouble() * 0.5; // 0-50% congestion
                double weatherImpact = random.nextDouble() * 0.2; // 0-20% weather impact
                
                edge.updateTrafficConditions(
                    edge.getBaseSpeedLimit().doubleValue() * (1 - congestionFactor),
                    congestionFactor,
                    weatherImpact,
                    random.nextDouble() * 50 // 0-50 vehicles per km
                );
            }
        }
    }
}
