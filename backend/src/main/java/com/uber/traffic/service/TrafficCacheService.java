package com.uber.traffic.service;

import com.uber.traffic.model.TrafficCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TrafficCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String TRAFFIC_CONDITION_PREFIX = "traffic:condition:";
    private static final String TRAFFIC_SEGMENT_PREFIX = "traffic:segment:";
    private static final String CONGESTION_PREFIX = "congestion:";
    private static final String WEATHER_PREFIX = "weather:";
    
    private static final long DEFAULT_CACHE_TTL = 300; // 5 minutes
    private static final long PREDICTION_CACHE_TTL = 1800; // 30 minutes
    
    @Autowired
    public TrafficCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public void cacheTrafficCondition(String segmentId, TrafficCondition condition) {
        String key = TRAFFIC_CONDITION_PREFIX + segmentId;
        
        try {
            redisTemplate.opsForValue().set(key, condition, DEFAULT_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("Cached traffic condition for segment: {}", segmentId);
        } catch (Exception e) {
            log.error("Error caching traffic condition for segment: {}", segmentId, e);
        }
    }
    
    public TrafficCondition getTrafficCondition(String segmentId) {
        String key = TRAFFIC_CONDITION_PREFIX + segmentId;
        
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof TrafficCondition) {
                log.debug("Retrieved cached traffic condition for segment: {}", segmentId);
                return (TrafficCondition) cached;
            }
        } catch (Exception e) {
            log.error("Error retrieving cached traffic condition for segment: {}", segmentId, e);
        }
        
        return null;
    }
    
    public void cacheTrafficData(String segmentId, TrafficData trafficData) {
        String key = TRAFFIC_SEGMENT_PREFIX + segmentId;
        
        try {
            redisTemplate.opsForHash().put(key, "currentSpeed", trafficData.getCurrentSpeed());
            redisTemplate.opsForHash().put(key, "congestionFactor", trafficData.getCongestionFactor());
            redisTemplate.opsForHash().put(key, "trafficDensity", trafficData.getTrafficDensity());
            redisTemplate.opsForHash().put(key, "lastUpdated", trafficData.getLastUpdated().toString());
            
            redisTemplate.expire(key, DEFAULT_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("Cached traffic data for segment: {}", segmentId);
        } catch (Exception e) {
            log.error("Error caching traffic data for segment: {}", segmentId, e);
        }
    }
    
    public TrafficData getTrafficData(String segmentId) {
        String key = TRAFFIC_SEGMENT_PREFIX + segmentId;
        
        try {
            Object speed = redisTemplate.opsForHash().get(key, "currentSpeed");
            Object congestion = redisTemplate.opsForHash().get(key, "congestionFactor");
            Object density = redisTemplate.opsForHash().get(key, "trafficDensity");
            Object lastUpdated = redisTemplate.opsForHash().get(key, "lastUpdated");
            
            if (speed != null && congestion != null && density != null) {
                TrafficData trafficData = new TrafficData();
                trafficData.setSegmentId(segmentId);
                trafficData.setCurrentSpeed(((Number) speed).doubleValue());
                trafficData.setCongestionFactor(((Number) congestion).doubleValue());
                trafficData.setTrafficDensity(((Number) density).doubleValue());
                trafficData.setLastUpdated(LocalDateTime.parse(lastUpdated.toString()));
                
                log.debug("Retrieved cached traffic data for segment: {}", segmentId);
                return trafficData;
            }
        } catch (Exception e) {
            log.error("Error retrieving cached traffic data for segment: {}", segmentId, e);
        }
        
        return null;
    }
    
    public void cacheCongestionLevel(String areaId, double congestionLevel) {
        String key = CONGESTION_PREFIX + areaId;
        
        try {
            redisTemplate.opsForValue().set(key, congestionLevel, DEFAULT_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("Cached congestion level for area: {}", areaId);
        } catch (Exception e) {
            log.error("Error caching congestion level for area: {}", areaId, e);
        }
    }
    
    public Double getCongestionLevel(String areaId) {
        String key = CONGESTION_PREFIX + areaId;
        
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Number) {
                log.debug("Retrieved cached congestion level for area: {}", areaId);
                return ((Number) cached).doubleValue();
            }
        } catch (Exception e) {
            log.error("Error retrieving cached congestion level for area: {}", areaId, e);
        }
        
        return null;
    }
    
    public void cacheWeatherImpact(String locationId, double weatherImpact) {
        String key = WEATHER_PREFIX + locationId;
        
        try {
            redisTemplate.opsForValue().set(key, weatherImpact, PREDICTION_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("Cached weather impact for location: {}", locationId);
        } catch (Exception e) {
            log.error("Error caching weather impact for location: {}", locationId, e);
        }
    }
    
    public Double getWeatherImpact(String locationId) {
        String key = WEATHER_PREFIX + locationId;
        
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Number) {
                log.debug("Retrieved cached weather impact for location: {}", locationId);
                return ((Number) cached).doubleValue();
            }
        } catch (Exception e) {
            log.error("Error retrieving cached weather impact for location: {}", locationId, e);
        }
        
        return null;
    }
    
    public void cacheTrafficPrediction(String segmentId, TrafficPrediction prediction) {
        String key = TRAFFIC_CONDITION_PREFIX + "prediction:" + segmentId;
        
        try {
            redisTemplate.opsForValue().set(key, prediction, PREDICTION_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("Cached traffic prediction for segment: {}", segmentId);
        } catch (Exception e) {
            log.error("Error caching traffic prediction for segment: {}", segmentId, e);
        }
    }
    
    public TrafficPrediction getTrafficPrediction(String segmentId) {
        String key = TRAFFIC_CONDITION_PREFIX + "prediction:" + segmentId;
        
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof TrafficPrediction) {
                log.debug("Retrieved cached traffic prediction for segment: {}", segmentId);
                return (TrafficPrediction) cached;
            }
        } catch (Exception e) {
            log.error("Error retrieving cached traffic prediction for segment: {}", segmentId, e);
        }
        
        return null;
    }
    
    public void invalidateTrafficCache(String segmentId) {
        try {
            redisTemplate.delete(TRAFFIC_CONDITION_PREFIX + segmentId);
            redisTemplate.delete(TRAFFIC_SEGMENT_PREFIX + segmentId);
            redisTemplate.delete(TRAFFIC_CONDITION_PREFIX + "prediction:" + segmentId);
            log.debug("Invalidated traffic cache for segment: {}", segmentId);
        } catch (Exception e) {
            log.error("Error invalidating traffic cache for segment: {}", segmentId, e);
        }
    }
    
    public void batchCacheTrafficData(List<TrafficData> trafficDataList) {
        trafficDataList.forEach(data -> cacheTrafficData(data.getSegmentId(), data));
        log.info("Batch cached {} traffic data entries", trafficDataList.size());
    }
    
    public void clearExpiredCache() {
        // This would typically be handled by Redis TTL, but can be used for manual cleanup
        log.info("Manual cache cleanup triggered");
    }
    
    public static class TrafficData {
        private String segmentId;
        private Double currentSpeed;
        private Double congestionFactor;
        private Double trafficDensity;
        private LocalDateTime lastUpdated;
        
        // Getters and setters
        public String getSegmentId() { return segmentId; }
        public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
        
        public Double getCurrentSpeed() { return currentSpeed; }
        public void setCurrentSpeed(Double currentSpeed) { this.currentSpeed = currentSpeed; }
        
        public Double getCongestionFactor() { return congestionFactor; }
        public void setCongestionFactor(Double congestionFactor) { this.congestionFactor = congestionFactor; }
        
        public Double getTrafficDensity() { return trafficDensity; }
        public void setTrafficDensity(Double trafficDensity) { this.trafficDensity = trafficDensity; }
        
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }
    
    public static class TrafficPrediction {
        private String segmentId;
        private Double predictedSpeed;
        private Double predictedCongestion;
        private LocalDateTime predictionTime;
        private LocalDateTime validUntil;
        private Double confidence;
        
        // Getters and setters
        public String getSegmentId() { return segmentId; }
        public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
        
        public Double getPredictedSpeed() { return predictedSpeed; }
        public void setPredictedSpeed(Double predictedSpeed) { this.predictedSpeed = predictedSpeed; }
        
        public Double getPredictedCongestion() { return predictedCongestion; }
        public void setPredictedCongestion(Double predictedCongestion) { this.predictedCongestion = predictedCongestion; }
        
        public LocalDateTime getPredictionTime() { return predictionTime; }
        public void setPredictionTime(LocalDateTime predictionTime) { this.predictionTime = predictionTime; }
        
        public LocalDateTime getValidUntil() { return validUntil; }
        public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
        
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
    }
}
