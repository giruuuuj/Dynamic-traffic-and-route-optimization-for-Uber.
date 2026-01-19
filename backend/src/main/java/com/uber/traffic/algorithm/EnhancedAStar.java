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
                return distance / 1000.0; // Use distance as proxy for emissions
            case SCENIC:
                // Prioritize scenic value (inverse of distance on major roads)
                return (distance / 1000.0) * 0.8; // Slightly lower penalty for distance
            case SAFEST:
                // Prioritize safety (use distance as base, highways get bonus)
                return distance / 1000.0 * 0.9; // Slightly lower penalty
            case COMFORTABLE:
                // Prioritize comfort (use distance as proxy)
                return distance / 1000.0;
            default:
                return distance / 1000.0;
        }
    }
    
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }
    
    private Route buildRoute(RouteNode endNode, Map<String, GraphNode> graph, 
                           RouteOptimizationCriteria criteria) {
        Route route = new Route(
            UUID.randomUUID().toString(),
            reconstructNodePath(endNode),
            endNode.getPath()
        );
        
        // Calculate route metrics
        calculateRouteMetrics(route, graph, criteria);
        
        return route;
    }
    
    private List<String> reconstructNodePath(RouteNode endNode) {
        List<String> path = new ArrayList<>();
        path.add(endNode.getNodeId());
        
        // This would need backtracking through parent references
        // For now, return a simple path
        return path;
    }
    
    private void calculateRouteMetrics(Route route, Map<String, GraphNode> graph,
                                     RouteOptimizationCriteria criteria) {
        double totalDistance = 0.0;
        double totalTravelTime = 0.0;
        double totalCost = 0.0;
        int trafficLights = 0;
        int tollRoads = 0;
        
        for (String edgeId : route.getEdgeIds()) {
            // Find edge in graph (this would need a more efficient lookup)
            for (GraphNode node : graph.values()) {
                for (GraphEdge edge : node.getAllOutgoingEdges()) {
                    if (edge.getId().equals(edgeId)) {
                        totalDistance += edge.getDistance();
                        totalTravelTime += edge.getDynamicWeight();
                        
                        if (edge.getTollRoad()) {
                            tollRoads++;
                            totalCost += 5.0; // Average toll cost
                        }
                        
                        // Estimate traffic lights (simplified)
                        if (edge.getRoadType() == GraphEdge.RoadType.ARTERIAL ||
                            edge.getRoadType() == GraphEdge.RoadType.COLLECTOR) {
                            trafficLights++;
                        }
                        
                        break;
                    }
                }
            }
        }
        
        route.setTotalDistance(totalDistance);
        route.setTotalTravelTime(totalTravelTime);
        route.setTotalCost(totalCost);
        route.setTrafficLights(trafficLights);
        route.setTollRoads(tollRoads);
        route.setAverageSpeed(totalDistance / totalTravelTime * 3.6); // km/h
        route.setConfidenceScore(0.85); // Default confidence
        route.setRouteType(Route.RouteType.valueOf(criteria.getPrimaryObjective().name()));
    }
    
    private static class RouteNode {
        private final String nodeId;
        private final String parentId;
        private final double gCost; // Cost from start
        private final double hCost; // Heuristic cost to goal
        private final List<String> path; // Edge IDs
        
        public RouteNode(String nodeId, String parentId, double gCost, double hCost, List<String> path) {
            this.nodeId = nodeId;
            this.parentId = parentId;
            this.gCost = gCost;
            this.hCost = hCost;
            this.path = path;
        }
        
        public double getFCost() {
            return gCost + hCost;
        }
        
        public String getNodeId() { return nodeId; }
        public String getParentId() { return parentId; }
        public double getGCost() { return gCost; }
        public double getHCost() { return hCost; }
        public List<String> getPath() { return path; }
    }
}
