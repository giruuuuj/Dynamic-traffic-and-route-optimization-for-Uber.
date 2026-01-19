package com.uber.traffic.service;

import com.uber.traffic.model.TrafficCondition;
import com.uber.traffic.model.RoadSegment;
import com.uber.traffic.model.Incident;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Real-time Traffic Data Integration Service
 * 
 * This service handles:
 * - Live traffic data collection from multiple sources
 * - Traffic condition updates and processing
 * - Incident detection and reporting
 * - Data aggregation and caching
 * - Real-time weight calculation for routing
 */
@Slf4j
@Service
public class TrafficDataIntegrationService {
    
    // Cache for traffic conditions
    private final Map<String, TrafficCondition> trafficCache = new ConcurrentHashMap<>();
    
    // Cache for incidents
    private final Map<String, Incident> incidentCache = new ConcurrentHashMap<>();
    
    // Road segments database
    private final Map<String, RoadSegment> roadSegments = new HashMap<>();
    
    // Traffic data sources
    private final List<TrafficDataSource> dataSources = new ArrayList<>();
    
    // Data freshness thresholds
    private static final long TRAFFIC_DATA_TTL = 300000; // 5 minutes
    private static final long INCIDENT_DATA_TTL = 600000; // 10 minutes
    
    public TrafficDataIntegrationService() {
        initializeDataSources();
        initializeRoadSegments();
        startDataCollection();
    }
    
    /**
     * Get current traffic conditions for a road segment
     */
    public TrafficCondition getTrafficCondition(String segmentId) {
        TrafficCondition condition = trafficCache.get(segmentId);
        
        if (condition == null || isDataExpired(condition.getTimestamp(), TRAFFIC_DATA_TTL)) {
            // Refresh data for this segment
            condition = refreshTrafficData(segmentId);
            trafficCache.put(segmentId, condition);
        }
        
        return condition;
    }
    
    /**
     * Get all active incidents in an area
     */
    public List<Incident> getActiveIncidents(double lat, double lng, double radius) {
        List<Incident> activeIncidents = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (Incident incident : incidentCache.values()) {
            if (isIncidentActive(incident, now) && 
                isWithinRadius(incident, lat, lng, radius)) {
                activeIncidents.add(incident);
            }
        }
        
        return activeIncidents;
    }
    
    /**
     * Calculate dynamic weight for a road segment
     */
    public double calculateDynamicWeight(String segmentId) {
        RoadSegment segment = roadSegments.get(segmentId);
        if (segment == null) {
            return Double.MAX_VALUE;
        }
        
        TrafficCondition traffic = getTrafficCondition(segmentId);
        double baseWeight = segment.getDistance() / 1000.0 / segment.getBaseSpeedLimit() * 3600.0; // Convert to seconds
        
        // Apply traffic multiplier
        double trafficMultiplier = 1.0 + (traffic.getCongestionLevel() * 0.5);
        
        // Check for incidents affecting this segment
        List<Incident> affectingIncidents = getIncidentsAffectingSegment(segmentId);
        double incidentMultiplier = calculateIncidentMultiplier(affectingIncidents);
        
        // Time-based multiplier
        double timeMultiplier = getTimeMultiplier();
        
        // Weather multiplier
        double weatherMultiplier = getWeatherMultiplier();
        
        double finalWeight = baseWeight * trafficMultiplier * incidentMultiplier * 
                          timeMultiplier * weatherMultiplier;
        
        log.debug("Calculated weight for segment {}: {} (base: {}, traffic: {}, incident: {})",
                segmentId, finalWeight, baseWeight, trafficMultiplier, incidentMultiplier);
        
        return finalWeight;
    }
    
    /**
     * Refresh traffic data for a specific segment
     */
    private TrafficCondition refreshTrafficData(String segmentId) {
        // Aggregate data from all sources
        List<TrafficCondition> sourceData = new ArrayList<>();
        
        for (TrafficDataSource source : dataSources) {
            try {
                TrafficCondition data = source.getTrafficData(segmentId);
                if (data != null) {
                    sourceData.add(data);
                }
            } catch (Exception e) {
                log.warn("Failed to get traffic data from source {} for segment {}", 
                        source.getName(), segmentId, e);
            }
        }
        
        // Aggregate the data
        return aggregateTrafficData(sourceData, segmentId);
    }
    
    /**
     * Aggregate traffic data from multiple sources
     */
    private TrafficCondition aggregateTrafficData(List<TrafficCondition> sourceData, String segmentId) {
        if (sourceData.isEmpty()) {
            // Return default condition if no data available
            return TrafficCondition.builder()
                    .segmentId(segmentId)
                    .congestionLevel(0.0)
                    .currentSpeed(50.0)
                    .timestamp(LocalDateTime.now())
                    .reliability(0.5)
                    .build();
        }
        
        double avgCongestion = sourceData.stream()
                .mapToDouble(TrafficCondition::getCongestionLevel)
                .average()
                .orElse(0.0);
        
        double avgSpeed = sourceData.stream()
                .mapToDouble(TrafficCondition::getCurrentSpeed)
                .average()
                .orElse(50.0);
        
        // Weight by source reliability
        double totalWeight = sourceData.stream()
                .mapToDouble(data -> data.getReliability())
                .sum();
        
        double weightedCongestion = 0.0;
        double weightedSpeed = 0.0;
        
        for (TrafficCondition data : sourceData) {
            double weight = data.getReliability() / totalWeight;
            weightedCongestion += data.getCongestionLevel() * weight;
            weightedSpeed += data.getCurrentSpeed() * weight;
        }
        
        return TrafficCondition.builder()
                .segmentId(segmentId)
                .congestionLevel(weightedCongestion)
                .currentSpeed(weightedSpeed)
                .timestamp(LocalDateTime.now())
                .reliability(0.8)
                .build();
    }
    
    /**
     * Get incidents affecting a road segment
     */
    private List<Incident> getIncidentsAffectingSegment(String segmentId) {
        List<Incident> affectingIncidents = new ArrayList<>();
        RoadSegment segment = roadSegments.get(segmentId);
        
        if (segment == null) {
            return affectingIncidents;
        }
        
        for (Incident incident : incidentCache.values()) {
            if (isIncidentActive(incident, LocalDateTime.now()) &&
                doesIncidentAffectSegment(incident, segment)) {
                affectingIncidents.add(incident);
            }
        }
        
        return affectingIncidents;
    }
    
    /**
     * Calculate incident multiplier based on incident severity and type
     */
    private double calculateIncidentMultiplier(List<Incident> incidents) {
        if (incidents.isEmpty()) {
            return 1.0;
        }
        
        double multiplier = 1.0;
        
        for (Incident incident : incidents) {
            switch (incident.getSeverity()) {
                case HIGH:
                    multiplier *= 2.5;
                    break;
                case MEDIUM:
                    multiplier *= 1.8;
                    break;
                case LOW:
                    multiplier *= 1.3;
                    break;
                case CRITICAL:
                    multiplier *= 3.0;
                    break;
            }
            
            // Additional multiplier based on incident type
            switch (incident.getType()) {
                case ACCIDENT:
                    multiplier *= 1.5;
                    break;
                case CONSTRUCTION:
                    multiplier *= 1.3;
                    break;
                case WEATHER:
                    multiplier *= 1.2;
                    break;
                case EVENT:
                    multiplier *= 1.4;
                    break;
                case ROAD_CLOSURE:
                    multiplier *= 5.0;
                    break;
                case VEHICLE_BREAKDOWN:
                    multiplier *= 1.1;
                    break;
            }
        }
        
        return multiplier;
    }
    
    /**
     * Check if an incident is currently active
     */
    private boolean isIncidentActive(Incident incident, LocalDateTime now) {
        return !incident.getEndTime().isBefore(now) && 
               !incident.getStartTime().isAfter(now);
    }
    
    /**
     * Check if an incident affects a road segment
     */
    private boolean doesIncidentAffectSegment(Incident incident, RoadSegment segment) {
        // Simple distance-based check
        double distance = calculateDistance(
                incident.getLatitude(), incident.getLongitude(),
                40.7128, -74.0060 // Default NYC coordinates for demo
        );
        
        return distance <= incident.getRadius();
    }
    
    /**
     * Check if a location is within radius
     */
    private boolean isWithinRadius(Incident incident, double lat, double lng, double radius) {
        double distance = calculateDistance(
                incident.getLatitude(), incident.getLongitude(),
                lat, lng
        );
        
        return distance <= radius;
    }
    
    /**
     * Calculate distance between two points
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
     * Get time-based multiplier
     */
    private double getTimeMultiplier() {
        int hour = LocalDateTime.now().getHour();
        int dayOfWeek = LocalDateTime.now().getDayOfWeek().getValue();
        
        // Peak hours: 7-9 AM and 5-7 PM on weekdays
        boolean isPeakHour = (hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19);
        boolean isWeekend = dayOfWeek == 6 || dayOfWeek == 7; // Saturday or Sunday
        
        if (isPeakHour && !isWeekend) {
            return 1.3;
        } else if (isWeekend) {
            return 0.8;
        }
        
        return 1.0;
    }
    
    /**
     * Get weather-based multiplier
     */
    private double getWeatherMultiplier() {
        // In a real implementation, this would check current weather
        // For demo purposes, we'll simulate weather conditions
        double random = ThreadLocalRandom.current().nextDouble();
        
        if (random < 0.1) {
            return 1.2; // Rain
        } else if (random < 0.05) {
            return 1.5; // Snow
        }
        
        return 1.0; // Clear
    }
    
    /**
     * Check if data is expired
     */
    private boolean isDataExpired(LocalDateTime timestamp, long ttl) {
        return timestamp.plusNanos(ttl * 1000000).isBefore(LocalDateTime.now());
    }
    
    /**
     * Initialize traffic data sources
     */
    private void initializeDataSources() {
        // Add mock data sources for demonstration
        dataSources.add(new MockTrafficDataSource("GPS Data", 0.9));
        dataSources.add(new MockTrafficDataSource("City Sensors", 0.8));
        dataSources.add(new MockTrafficDataSource("Traffic APIs", 0.7));
    }
    
    /**
     * Initialize road segments
     */
    private void initializeRoadSegments() {
        // Create sample road segments
        roadSegments.put("A-B", RoadSegment.builder()
                .id("A-B")
                .segmentId("A-B")
                .name("Broadway")
                .distance(2500.0) // 2.5km in meters
                .baseSpeedLimit(50)
                .lanes(2)
                .roadType(RoadSegment.RoadType.ARTERIAL)
                .tollRoad(false)
                .build());
                
        roadSegments.put("B-C", RoadSegment.builder()
                .id("B-C")
                .segmentId("B-C")
                .name("Highway 1")
                .distance(3200.0) // 3.2km in meters
                .baseSpeedLimit(60)
                .lanes(3)
                .roadType(RoadSegment.RoadType.HIGHWAY)
                .tollRoad(true)
                .build());
                
        roadSegments.put("C-D", RoadSegment.builder()
                .id("C-D")
                .segmentId("C-D")
                .name("Main Street")
                .distance(1800.0) // 1.8km in meters
                .baseSpeedLimit(45)
                .lanes(2)
                .roadType(RoadSegment.RoadType.COLLECTOR)
                .tollRoad(false)
                .build());
                
        roadSegments.put("D-E", RoadSegment.builder()
                .id("D-E")
                .segmentId("D-E")
                .name("Expressway")
                .distance(2100.0) // 2.1km in meters
                .baseSpeedLimit(55)
                .lanes(4)
                .roadType(RoadSegment.RoadType.HIGHWAY)
                .tollRoad(true)
                .build());
    }
    
    /**
     * Start data collection process
     */
    private void startDataCollection() {
        log.info("Starting traffic data collection from {} sources", dataSources.size());
        
        // Collect initial data
        collectInitialData();
        
        // Schedule periodic updates
        // In a real implementation, this would use proper scheduling
        log.info("Traffic data collection started");
    }
    
    /**
     * Collect initial traffic data
     */
    private void collectInitialData() {
        for (String segmentId : roadSegments.keySet()) {
            TrafficCondition condition = refreshTrafficData(segmentId);
            trafficCache.put(segmentId, condition);
        }
        
        // Generate some sample incidents
        generateSampleIncidents();
        
        log.info("Initial traffic data collected for {} segments", trafficCache.size());
    }
    
    /**
     * Generate sample incidents for demonstration
     */
    private void generateSampleIncidents() {
        LocalDateTime now = LocalDateTime.now();
        
        // Sample incident 1
        Incident incident1 = Incident.builder()
                .id("INC001")
                .type(Incident.IncidentType.ACCIDENT)
                .severity(Incident.Severity.HIGH)
                .latitude(40.7128)
                .longitude(-74.0060)
                .radius(1000.0) // 1km radius
                .description("Multi-vehicle collision on highway")
                .startTime(now.minusMinutes(30))
                .endTime(now.plusHours(2))
                .build();
        incidentCache.put(incident1.getId(), incident1);
        
        // Sample incident 2
        Incident incident2 = Incident.builder()
                .id("INC002")
                .type(Incident.IncidentType.CONSTRUCTION)
                .severity(Incident.Severity.MEDIUM)
                .latitude(40.7260)
                .longitude(-73.9897)
                .radius(500.0) // 500m radius
                .description("Road construction work")
                .startTime(now.minusHours(1))
                .endTime(now.plusHours(6))
                .build();
        incidentCache.put(incident2.getId(), incident2);
        
        log.info("Generated {} sample incidents", incidentCache.size());
    }
    
    /**
     * Scheduled task to refresh traffic data
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void refreshTrafficData() {
        log.debug("Refreshing traffic data...");
        
        // Refresh a subset of segments to avoid overwhelming the system
        List<String> segmentIds = new ArrayList<>(roadSegments.keySet());
        Collections.shuffle(segmentIds);
        
        int refreshCount = Math.min(10, segmentIds.size());
        for (int i = 0; i < refreshCount; i++) {
            String segmentId = segmentIds.get(i);
            TrafficCondition condition = refreshTrafficData(segmentId);
            trafficCache.put(segmentId, condition);
        }
        
        log.debug("Refreshed traffic data for {} segments", refreshCount);
    }
    
    /**
     * Scheduled task to update incidents
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void updateIncidents() {
        log.debug("Updating incidents...");
        
        // Remove expired incidents
        LocalDateTime now = LocalDateTime.now();
        incidentCache.entrySet().removeIf(entry -> 
                !isIncidentActive(entry.getValue(), now));
        
        // Occasionally add new incidents
        if (ThreadLocalRandom.current().nextDouble() < 0.1) {
            generateRandomIncident();
        }
        
        log.debug("Updated incidents. Active count: {}", incidentCache.size());
    }
    
    /**
     * Generate a random incident for demonstration
     */
    private void generateRandomIncident() {
        Incident.IncidentType[] types = Incident.IncidentType.values();
        Incident.Severity[] severities = Incident.Severity.values();
        
        Incident.IncidentType type = types[ThreadLocalRandom.current().nextInt(types.length)];
        Incident.Severity severity = severities[ThreadLocalRandom.current().nextInt(severities.length)];
        
        // Random location within NYC area
        double lat = 40.7 + ThreadLocalRandom.current().nextDouble() * 0.1;
        double lng = -74.0 + ThreadLocalRandom.current().nextDouble() * 0.1;
        
        Incident incident = Incident.builder()
                .id("INC" + System.currentTimeMillis())
                .type(type)
                .severity(severity)
                .latitude(lat)
                .longitude(lng)
                .radius(500.0 + ThreadLocalRandom.current().nextDouble() * 1000.0)
                .description("Random incident for demonstration")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1 + ThreadLocalRandom.current().nextInt(4)))
                .build();
        
        incidentCache.put(incident.getId(), incident);
        log.info("Generated new incident: {} at ({}, {})", type, lat, lng);
    }
    
    /**
     * Mock traffic data source for demonstration
     */
    private static class MockTrafficDataSource implements TrafficDataSource {
        private final String name;
        private final double reliability;
        
        public MockTrafficDataSource(String name, double reliability) {
            this.name = name;
            this.reliability = reliability;
        }
        
        @Override
        public TrafficCondition getTrafficData(String segmentId) {
            // Generate mock traffic data
            double congestion = ThreadLocalRandom.current().nextDouble();
            double speed = 50.0 - (congestion * 20.0); // Speed decreases with congestion
            
            return TrafficCondition.builder()
                    .segmentId(segmentId)
                    .congestionLevel(congestion)
                    .currentSpeed(speed)
                    .timestamp(LocalDateTime.now())
                    .reliability(reliability)
                    .build();
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public double getReliability() {
            return reliability;
        }
    }
    
    /**
     * Interface for traffic data sources
     */
    private interface TrafficDataSource {
        TrafficCondition getTrafficData(String segmentId);
        String getName();
        double getReliability();
    }
}
