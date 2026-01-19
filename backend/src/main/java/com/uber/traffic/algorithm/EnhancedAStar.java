package com.uber.traffic.algorithm;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@Component
public class EnhancedAStar {

    private static final double EARTH_RADIUS = 6371000; // in meters
    
    public Route findOptimalRoute(Map<String, GraphNode> graph, 
                                String startNodeId, 
                                String endNodeId,
                                RouteOptimizationCriteria criteria) {
        
        log.info("Finding optimal route from {} to {} using criteria: {}", 
                startNodeId, endNodeId, criteria);
        
        if (!graph.containsKey(startNodeId) || !graph.containsKey(endNodeId)) {
            throw new IllegalArgumentException("Start or end node not found in graph");
        }
        
        // Priority queue ordered by f(n) = g(n) + h(n)
        PriorityQueue<RouteNode> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(node -> node.getFCost())
        );
        
        // Track visited nodes and their best costs
        Map<String, RouteNode> visitedNodes = new HashMap<>();
        
        // Initialize start node
        GraphNode startNode = graph.get(startNodeId);
        GraphNode endNode = graph.get(endNodeId);
        
        RouteNode startRouteNode = new RouteNode(startNodeId, null, 0.0, 
            calculateHeuristic(startNode, endNode, criteria), new ArrayList<>());
        
        openSet.add(startRouteNode);
        visitedNodes.put(startNodeId, startRouteNode);
        
        while (!openSet.isEmpty()) {
            RouteNode current = openSet.poll();
            
            // Check if we reached the destination
            if (current.getNodeId().equals(endNodeId)) {
                log.info("Route found with total cost: {}", current.getGCost());
                return buildRoute(current, graph, criteria);
            }
            
            GraphNode currentNode = graph.get(current.getNodeId());
            
            // Explore neighbors
            for (GraphEdge edge : currentNode.getAllOutgoingEdges()) {
                String neighborId = edge.getToNodeId();
                
                // Calculate tentative g-cost
                double edgeWeight = calculateEdgeWeight(edge, criteria);
                double tentativeGCost = current.getGCost() + edgeWeight;
                
                // Check if we found a better path to this neighbor
                RouteNode neighborNode = visitedNodes.get(neighborId);
                if (neighborNode == null || tentativeGCost < neighborNode.getGCost()) {
                    
                    // Create new path
                    List<String> newPath = new ArrayList<>(current.getPath());
                    newPath.add(edge.getId());
                    
                    // Calculate heuristic
                    GraphNode neighborGraphNode = graph.get(neighborId);
                    double heuristic = calculateHeuristic(neighborGraphNode, endNode, criteria);
                    
                    // Create new route node
                    RouteNode newRouteNode = new RouteNode(neighborId, current.getNodeId(), 
                        tentativeGCost, heuristic, newPath);
                    
                    // Update visited nodes
                    visitedNodes.put(neighborId, newRouteNode);
                    
                    // Add to open set
                    openSet.add(newRouteNode);
                }
            }
        }
        
        log.warn("No route found from {} to {}", startNodeId, endNodeId);
        return null;
    }
    
    private double calculateEdgeWeight(GraphEdge edge, RouteOptimizationCriteria criteria) {
        double baseWeight = edge.getDynamicWeight();
        
        // Apply optimization criteria multipliers
        switch (criteria.getPrimaryObjective()) {
            case FASTEST:
                // Already optimized for time in dynamic weight
                break;
            case SHORTEST:
                // Prioritize distance over time
                baseWeight = edge.getDistance() / 1000.0; // Convert to km
                break;
            case ECONOMICAL:
                // Consider toll costs and fuel consumption
                double tollCost = edge.getTollRoad() ? 5.0 : 0.0;
                double fuelCost = (edge.getDistance() / 1000.0) * 0.15; // $0.15 per km
                baseWeight = baseWeight * 0.7 + (tollCost + fuelCost) * 0.3;
                break;
            case ECO_FRIENDLY:
                // Prioritize routes with lower emissions
                double gradePenalty = Math.abs(edge.getGrade()) * 2.0;
                double congestionPenalty = edge.getCongestionFactor() * 10.0;
                baseWeight = baseWeight + gradePenalty + congestionPenalty;
                break;
            case SCENIC:
                // Prioritize scenic routes (lower traffic, more interesting roads)
                double scenicBonus = edge.getRoadType() == GraphEdge.RoadType.LOCAL ? -0.3 : 0.0;
                baseWeight = baseWeight * (1.0 + scenicBonus);
                break;
            case SAFEST:
                // Prioritize safer roads (highways, well-maintained roads)
                double safetyBonus = edge.getRoadType() == GraphEdge.RoadType.HIGHWAY ? -0.2 : 0.1;
                baseWeight = baseWeight * (1.0 + safetyBonus);
                break;
            case COMFORTABLE:
                // Prioritize comfortable routes (smoother roads, less congestion)
                double comfortBonus = edge.getCongestionFactor() * 5.0;
                baseWeight = baseWeight + comfortBonus;
                break;
        }
        
        // Apply user preferences
        if (criteria.getAvoidTolls() != null && criteria.getAvoidTolls() && edge.getTollRoad()) {
            baseWeight *= 10.0; // Heavily penalize toll roads
        }
        
        if (criteria.getAvoidHighways() != null && criteria.getAvoidHighways() && edge.getRoadType() == GraphEdge.RoadType.HIGHWAY) {
            baseWeight *= 5.0; // Penalize highways
        }
        
        return baseWeight;
    }
    
    private double calculateHeuristic(GraphNode from, GraphNode to, RouteOptimizationCriteria criteria) {
        double distance = calculateHaversineDistance(
            from.getLatitude(), from.getLongitude(),
            to.getLatitude(), to.getLongitude()
        );
        
        // Adjust heuristic based on optimization criteria
        switch (criteria.getPrimaryObjective()) {
            case FASTEST:
                // Estimate time based on typical speeds
                double avgSpeed = 50.0; // km/h urban average
                return (distance / 1000.0) / avgSpeed * 3600.0; // seconds
            case SHORTEST:
                return distance / 1000.0; // km
            case ECONOMICAL:
                // Estimate cost based on distance
                return (distance / 1000.0) * 0.2; // $0.20 per km average
            case ECO_FRIENDLY:
                // Estimate emissions based on distance
        
        // User preference multipliers
        double preferenceMultiplier = getPreferenceMultiplier(segment, criteria);
        
        // Final calculation
        double finalTime = baseTime * trafficMultiplier * timeMultiplier * 
                          weatherMultiplier * eventMultiplier * preferenceMultiplier;
        
        return finalTime;
    }
    
    /**
     * Calculate heuristic (estimated cost from current to destination)
     */
    private double calculateHeuristic(Intersection current, Intersection destination) {
        // Straight-line distance divided by maximum possible speed
        double distance = calculateDistance(current, destination);
        double maxSpeed = 120.0; // km/h - maximum reasonable speed
        return distance / maxSpeed;
    }
    
    /**
     * Calculate distance between two intersections
     */
    private double calculateDistance(Intersection a, Intersection b) {
        double latDiff = Math.toRadians(b.getLatitude() - a.getLatitude());
        double lonDiff = Math.toRadians(b.getLongitude() - a.getLongitude());
        
        double lat1 = Math.toRadians(a.getLatitude());
        double lat2 = Math.toRadians(b.getLatitude());
        
        double a1 = Math.sin(latDiff/2) * Math.sin(latDiff/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(lonDiff/2) * Math.sin(lonDiff/2);
        double c = 2 * Math.atan2(Math.sqrt(a1), Math.sqrt(1-a1));
        
        return 6371 * c; // Earth's radius in kilometers
    }
    
    /**
     * Reconstruct path from end node to start
     */
    private Route reconstructPath(Map<String, RouteNode> allNodes, RouteNode endNode, 
                                RouteOptimizationCriteria criteria) {
        List<String> path = new ArrayList<>();
        List<RoadSegment> segments = new ArrayList<>();
        double totalTime = 0.0;
        double totalDistance = 0.0;
        double totalCost = 0.0;
        
        RouteNode current = endNode;
        
        while (current != null) {
            path.add(0, current.getNodeId());
            
            if (current.getParentId() != null) {
                RoadSegment segment = getRoadSegment(current.getParentId(), current.getNodeId());
                if (segment != null) {
                    segments.add(0, segment);
                    totalTime += calculateDynamicTravelTime(segment, criteria);
                    totalDistance += segment.getLength();
                    totalCost += calculateCost(segment, criteria);
                }
            }
            
            current = allNodes.get(current.getParentId());
        }
        
        return new Route(path, segments, totalTime, totalDistance, totalCost, criteria);
    }
    
    /**
     * Get time-based multiplier for current conditions
     */
    private double getTimeMultiplier() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        DayOfWeek day = now.getDayOfWeek();
        
        // Peak hours: 7-9 AM and 5-7 PM on weekdays
        boolean isPeakHour = (hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19);
        boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        
        if (isPeakHour && !isWeekend) {
            return PEAK_HOUR_MULTIPLIER;
        } else if (isWeekend) {
            return WEEKEND_MULTIPLIER;
        }
        
        return 1.0;
    }
    
    /**
     * Get weather-based multiplier
     */
    private double getWeatherMultiplier() {
        // In a real implementation, this would check current weather conditions
        // For demo purposes, we'll return normal conditions
        return 1.0;
    }
    
    /**
     * Get event-based multiplier for road segment
     */
    private double getEventMultiplier(RoadSegment segment) {
        // In a real implementation, this would check for events affecting this segment
        // For demo purposes, we'll return normal conditions
        return 1.0;
    }
    
    /**
     * Get preference-based multiplier
     */
    private double getPreferenceMultiplier(RoadSegment segment, RouteOptimizationCriteria criteria) {
        double multiplier = 1.0;
        
        // Avoid tolls if specified
        if (criteria.isAvoidTolls() && segment.isTollRoad()) {
            multiplier *= 2.0; // Heavy penalty for toll roads
        }
        
        // Avoid highways if specified
        if (criteria.isAvoidHighways() && segment.isHighway()) {
            multiplier *= 1.5;
        }
        
        // Avoid ferries if specified
        if (criteria.isAvoidFerries() && segment.isFerry()) {
            multiplier *= 3.0;
        }
        
        // Apply road type preferences
        for (String avoidType : criteria.getAvoidRoadTypes()) {
            if (segment.getRoadType().equals(avoidType)) {
                multiplier *= 1.8;
            }
        }
        
        return multiplier;
    }
    
    /**
     * Calculate cost based on segment and criteria
     */
    private double calculateCost(RoadSegment segment, RouteOptimizationCriteria criteria) {
        double baseCost = segment.getLength() * 0.1; // Base cost per km
        
        // Add toll cost if applicable
        if (segment.isTollRoad()) {
            baseCost += segment.getTollCost();
        }
        
        // Apply preference-based adjustments
        if (criteria.getOptimizationObjective() == RouteOptimizationCriteria.Objective.ECONOMICAL) {
            baseCost *= 1.2; // Higher cost factor for economical routes
        }
        
        return baseCost;
    }
    
    /**
     * Helper methods for road segment operations
     */
    private List<RoadSegment> getConnectedRoads(String intersectionId) {
        List<RoadSegment> connected = new ArrayList<>();
        
        for (RoadSegment segment : roadSegments.values()) {
            if (segment.getStartIntersectionId().equals(intersectionId) || 
                segment.getEndIntersectionId().equals(intersectionId)) {
                connected.add(segment);
            }
        }
        
        return connected;
    }
    
    private String getOtherEnd(RoadSegment segment, String currentId) {
        if (segment.getStartIntersectionId().equals(currentId)) {
            return segment.getEndIntersectionId();
        } else {
            return segment.getStartIntersectionId();
        }
    }
    
    private RoadSegment getRoadSegment(String startId, String endId) {
        for (RoadSegment segment : roadSegments.values()) {
            if ((segment.getStartIntersectionId().equals(startId) && segment.getEndIntersectionId().equals(endId)) ||
                (segment.getStartIntersectionId().equals(endId) && segment.getEndIntersectionId().equals(startId))) {
                return segment;
            }
        }
        return null;
    }
    
    /**
     * Cache management methods
     */
    private String generateCacheKey(String startId, String endId, RouteOptimizationCriteria criteria) {
        return startId + "-" + endId + "-" + criteria.hashCode();
    }
    
    private boolean isRouteExpired(Route route) {
        // Routes expire after 5 minutes
        return System.currentTimeMillis() - route.getCalculatedAt() > 300000;
    }
    
    /**
     * Initialize test data for demonstration
     */
    private void initializeTestData() {
        // Create test intersections
        intersections.put("A", new Intersection("A", 40.7128, -74.0060));
        intersections.put("B", new Intersection("B", 40.7260, -73.9897));
        intersections.put("C", new Intersection("C", 40.7489, -73.9680));
        intersections.put("D", new Intersection("D", 40.7614, -73.9776));
        intersections.put("E", new Intersection("E", 40.7831, -73.9712));
        
        // Create test road segments
        roadSegments.put("A-B", new RoadSegment("A-B", "A", "B", 2.5, 50.0, false, false, false, "street", 0.0));
        roadSegments.put("B-C", new RoadSegment("B-C", "B", "C", 3.2, 60.0, true, false, false, "highway", 2.50));
        roadSegments.put("C-D", new RoadSegment("C-D", "C", "D", 1.8, 45.0, false, false, false, "street", 0.0));
        roadSegments.put("D-E", new RoadSegment("D-E", "D", "E", 2.1, 55.0, true, false, false, "highway", 1.75));
        roadSegments.put("A-C", new RoadSegment("A-C", "A", "C", 4.5, 40.0, false, false, false, "street", 0.0));
        roadSegments.put("B-D", new RoadSegment("B-D", "B", "D", 3.8, 45.0, false, false, false, "street", 0.0));
        
        // Create test traffic conditions
        trafficConditions.put("A-B", new TrafficCondition("A-B", 0.2, 45.0, LocalDateTime.now()));
        trafficConditions.put("B-C", new TrafficCondition("B-C", 0.6, 35.0, LocalDateTime.now()));
        trafficConditions.put("C-D", new TrafficCondition("C-D", 0.3, 40.0, LocalDateTime.now()));
        trafficConditions.put("D-E", new TrafficCondition("D-E", 0.4, 42.0, LocalDateTime.now()));
    }
    
    /**
     * Inner class representing a node in the A* search
     */
    private static class RouteNode {
        private final String nodeId;
        private double gScore; // Cost from start
        private double hScore; // Heuristic cost to end
        private String parentId;
        
        public RouteNode(String nodeId, double gScore, double hScore, String parentId) {
            this.nodeId = nodeId;
            this.gScore = gScore;
            this.hScore = hScore;
            this.parentId = parentId;
        }
        
        public double getFScore() {
            return gScore + hScore;
        }
        
        // Getters and setters
        public String getNodeId() { return nodeId; }
        public double getGScore() { return gScore; }
        public void setGScore(double gScore) { this.gScore = gScore; }
        public double getHScore() { return hScore; }
        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
    }
}
