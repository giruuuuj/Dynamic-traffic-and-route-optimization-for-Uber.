package com.uber.traffic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.traffic.dto.RouteRequest;
import com.uber.traffic.model.TrafficCondition;
import com.uber.traffic.service.TrafficCacheService.TrafficData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TrafficDataIngestionService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TrafficCacheService trafficCacheService;
    private final ObjectMapper objectMapper;
    
    // In-memory storage for recent GPS data (for processing)
    private final Map<String, List<GPSData>> recentGPSData = new ConcurrentHashMap<>();
    
    @Autowired
    public TrafficDataIngestionService(KafkaTemplate<String, String> kafkaTemplate,
                                    TrafficCacheService trafficCacheService,
                                    ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.trafficCacheService = trafficCacheService;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "${app.kafka.topic.gps-data}", groupId = "traffic-ingestion-group")
    public void ingestGPSData(String gpsDataJson) {
        try {
            GPSData gpsData = objectMapper.readValue(gpsDataJson, GPSData.class);
            
            log.debug("Received GPS data from vehicle {}: lat={}, lon={}, speed={}", 
                    gpsData.getVehicleId(), gpsData.getLatitude(), gpsData.getLongitude(), gpsData.getSpeed());
            
            // Store recent GPS data for processing
            recentGPSData.computeIfAbsent(gpsData.getVehicleId(), k -> new java.util.ArrayList<>()).add(gpsData);
            
            // Process GPS data to extract traffic information
            processGPSDataForTraffic(gpsData);
            
            // Clean old data (keep only last 10 minutes per vehicle)
            cleanOldGPSData(gpsData.getVehicleId());
            
        } catch (Exception e) {
            log.error("Error processing GPS data: {}", gpsDataJson, e);
        }
    }
    
    @KafkaListener(topics = "${app.kafka.topic.traffic-data}", groupId = "traffic-ingestion-group")
    public void ingestTrafficData(String trafficDataJson) {
        try {
            TrafficDataMessage trafficData = objectMapper.readValue(trafficDataJson, TrafficDataMessage.class);
            
            log.debug("Received traffic data for segment {}: speed={}, congestion={}", 
                    trafficData.getSegmentId(), trafficData.getCurrentSpeed(), trafficData.getCongestionFactor());
            
            // Cache traffic data
            TrafficData cacheData = new TrafficData();
            cacheData.setSegmentId(trafficData.getSegmentId());
            cacheData.setCurrentSpeed(trafficData.getCurrentSpeed());
            cacheData.setCongestionFactor(trafficData.getCongestionFactor());
            cacheData.setTrafficDensity(trafficData.getTrafficDensity());
            cacheData.setLastUpdated(LocalDateTime.now());
            
            trafficCacheService.cacheTrafficData(trafficData.getSegmentId(), cacheData);
            
            // Publish route updates if significant changes
            if (isSignificantTrafficChange(trafficData)) {
                publishRouteUpdate(trafficData);
            }
            
        } catch (Exception e) {
            log.error("Error processing traffic data: {}", trafficDataJson, e);
        }
    }
    
    private void processGPSDataForTraffic(GPSData gpsData) {
        // Find nearby road segments (this would integrate with a spatial index)
        List<String> nearbySegments = findNearbyRoadSegments(gpsData);
        
        for (String segmentId : nearbySegments) {
            // Calculate traffic metrics from GPS data
            double avgSpeed = calculateAverageSpeed(segmentId, gpsData);
            double congestion = calculateCongestion(segmentId, avgSpeed);
            double density = calculateTrafficDensity(segmentId);
            
            // Update traffic cache
            TrafficData trafficData = new TrafficData();
            trafficData.setSegmentId(segmentId);
            trafficData.setCurrentSpeed(avgSpeed);
            trafficData.setCongestionFactor(congestion);
            trafficData.setTrafficDensity(density);
            trafficData.setLastUpdated(LocalDateTime.now());
            
            trafficCacheService.cacheTrafficData(segmentId, trafficData);
        }
    }
    
    private List<String> findNearbyRoadSegments(GPSData gpsData) {
        // Simplified implementation - in production, this would use spatial indexing
        // For now, return mock segment IDs based on location
        return List.of(
            "segment_" + (int)(gpsData.getLatitude() * 100) + "_" + (int)(gpsData.getLongitude() * 100),
            "segment_" + (int)(gpsData.getLatitude() * 100 + 1) + "_" + (int)(gpsData.getLongitude() * 100),
            "segment_" + (int)(gpsData.getLatitude() * 100) + "_" + (int)(gpsData.getLongitude() * 100 + 1)
        );
    }
    
    private double calculateAverageSpeed(String segmentId, GPSData currentGPS) {
        List<GPSData> vehicleData = recentGPSData.get(currentGPS.getVehicleId());
        if (vehicleData == null || vehicleData.isEmpty()) {
            return currentGPS.getSpeed();
        }
        
        // Calculate average speed from recent data points
        return vehicleData.stream()
                .mapToDouble(GPSData::getSpeed)
                .average()
                .orElse(currentGPS.getSpeed());
    }
    
    private double calculateCongestion(String segmentId, double currentSpeed) {
        // Get base speed limit for this segment (would come from database)
        double baseSpeedLimit = 50.0; // km/h default
        
        // Congestion factor: 0 = free flow, 1 = completely congested
        double congestionFactor = Math.max(0.0, Math.min(1.0, 1.0 - (currentSpeed / baseSpeedLimit)));
        
        return congestionFactor;
    }
    
    private double calculateTrafficDensity(String segmentId) {
        // Count unique vehicles in this segment recently
        int vehicleCount = 0;
        for (List<GPSData> vehicleData : recentGPSData.values()) {
            vehicleCount += vehicleData.size();
        }
        
        // Simplified density calculation (vehicles per km)
        double segmentLength = 1.0; // km, would come from database
        return vehicleCount / segmentLength;
    }
    
    private void cleanOldGPSData(String vehicleId) {
        List<GPSData> vehicleData = recentGPSData.get(vehicleId);
        if (vehicleData != null) {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
            vehicleData.removeIf(gps -> gps.getTimestamp().isBefore(cutoff));
            
            if (vehicleData.isEmpty()) {
                recentGPSData.remove(vehicleId);
            }
        }
    }
    
    private boolean isSignificantTrafficChange(TrafficDataMessage trafficData) {
        // Check if traffic change is significant enough to trigger route updates
        TrafficData cached = trafficCacheService.getTrafficData(trafficData.getSegmentId());
        
        if (cached == null) {
            return true; // New data is always significant
        }
        
        double speedChange = Math.abs(cached.getCurrentSpeed() - trafficData.getCurrentSpeed());
        double congestionChange = Math.abs(cached.getCongestionFactor() - trafficData.getCongestionFactor());
        
        // Trigger update if speed changed by more than 10 km/h or congestion by more than 0.2
        return speedChange > 10.0 || congestionChange > 0.2;
    }
    
    private void publishRouteUpdate(TrafficDataMessage trafficData) {
        try {
            RouteUpdateMessage update = new RouteUpdateMessage();
            update.setSegmentId(trafficData.getSegmentId());
            update.setNewSpeed(trafficData.getCurrentSpeed());
            update.setNewCongestion(trafficData.getCongestionFactor());
            update.setTimestamp(LocalDateTime.now());
            update.setReason("TRAFFIC_CHANGE");
            
            String updateJson = objectMapper.writeValueAsString(update);
            kafkaTemplate.send("${app.kafka.topic.route-updates}", updateJson);
            
            log.info("Published route update for segment: {}", trafficData.getSegmentId());
            
        } catch (Exception e) {
            log.error("Error publishing route update", e);
        }
    }
    
    public void publishTrafficAlert(TrafficAlert alert) {
        try {
            String alertJson = objectMapper.writeValueAsString(alert);
            kafkaTemplate.send("${app.kafka.topic.traffic-alerts}", alertJson);
            
            log.info("Published traffic alert: {}", alert.getAlertType());
            
        } catch (Exception e) {
            log.error("Error publishing traffic alert", e);
        }
    }
    
    // Data classes for Kafka messages
    public static class GPSData {
        private String vehicleId;
        private Double latitude;
        private Double longitude;
        private Double speed; // km/h
        private Double heading; // degrees
        private LocalDateTime timestamp;
        
        // Getters and setters
        public String getVehicleId() { return vehicleId; }
        public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
        
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        
        public Double getSpeed() { return speed; }
        public void setSpeed(Double speed) { this.speed = speed; }
        
        public Double getHeading() { return heading; }
        public void setHeading(Double heading) { this.heading = heading; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    public static class TrafficDataMessage {
        private String segmentId;
        private Double currentSpeed;
        private Double congestionFactor;
        private Double trafficDensity;
        private String source; // GPS, SENSORS, EXTERNAL_API
        private LocalDateTime timestamp;
        
        // Getters and setters
        public String getSegmentId() { return segmentId; }
        public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
        
        public Double getCurrentSpeed() { return currentSpeed; }
        public void setCurrentSpeed(Double currentSpeed) { this.currentSpeed = currentSpeed; }
        
        public Double getCongestionFactor() { return congestionFactor; }
        public void setCongestionFactor(Double congestionFactor) { this.congestionFactor = congestionFactor; }
        
        public Double getTrafficDensity() { return trafficDensity; }
        public void setTrafficDensity(Double trafficDensity) { this.trafficDensity = trafficDensity; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    public static class RouteUpdateMessage {
        private String segmentId;
        private Double newSpeed;
        private Double newCongestion;
        private LocalDateTime timestamp;
        private String reason;
        
        // Getters and setters
        public String getSegmentId() { return segmentId; }
        public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
        
        public Double getNewSpeed() { return newSpeed; }
        public void setNewSpeed(Double newSpeed) { this.newSpeed = newSpeed; }
        
        public Double getNewCongestion() { return newCongestion; }
        public void setNewCongestion(Double newCongestion) { this.newCongestion = newCongestion; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    public static class TrafficAlert {
        private String alertId;
        private String alertType; // ACCIDENT, CONGESTION, WEATHER, ROAD_CLOSURE
        private String segmentId;
        private Double latitude;
        private Double longitude;
        private String description;
        private Integer severity; // 1-5
        private LocalDateTime timestamp;
        
        // Getters and setters
        public String getAlertId() { return alertId; }
        public void setAlertId(String alertId) { this.alertId = alertId; }
        
        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        
        public String getSegmentId() { return segmentId; }
        public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
        
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Integer getSeverity() { return severity; }
        public void setSeverity(Integer severity) { this.severity = severity; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
