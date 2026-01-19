package com.uber.traffic.algorithm;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteOptimizationCriteria {
    
    private OptimizationObjective primaryObjective;
    private Double maxDistance; // in km
    private Double maxTravelTime; // in minutes
    private Double maxCost; // in currency units
    private Boolean avoidTolls;
    private Boolean avoidHighways;
    private Boolean preferScenic;
    private Double ecoWeight; // 0.0 to 1.0, how much to prioritize eco-friendly routes
    
    // Time-based preferences
    private Integer departureHour; // 0-23
    private Integer arrivalHour; // 0-23
    private Boolean considerRushHour;
    
    // Vehicle preferences
    private VehicleType vehicleType;
    private Double vehicleRange; // for EVs in km
    private Boolean requireChargingStations;
    
    // Multi-criteria weights (should sum to 1.0)
    private Double timeWeight;
    private Double distanceWeight;
    private Double costWeight;
    private Double comfortWeight;
    private Double safetyWeight;
    
    public RouteOptimizationCriteria(OptimizationObjective primaryObjective) {
        this.primaryObjective = primaryObjective;
        this.avoidTolls = false;
        this.avoidHighways = false;
        this.preferScenic = false;
        this.ecoWeight = 0.0;
        this.considerRushHour = true;
        this.vehicleType = VehicleType.CAR;
        this.timeWeight = 0.4;
        this.distanceWeight = 0.2;
        this.costWeight = 0.2;
        this.comfortWeight = 0.1;
        this.safetyWeight = 0.1;
    }
    
    public void validateWeights() {
        double total = timeWeight + distanceWeight + costWeight + comfortWeight + safetyWeight;
        if (Math.abs(total - 1.0) > 0.01) {
            throw new IllegalArgumentException("Weights must sum to 1.0, current sum: " + total);
        }
    }
    
    public enum OptimizationObjective {
        FASTEST,        // Minimize travel time
        SHORTEST,       // Minimize distance
        ECONOMICAL,     // Minimize cost
        ECO_FRIENDLY,   // Minimize environmental impact
        SCENIC,         // Maximize scenic value
        SAFEST,         // Maximize safety
        COMFORTABLE     // Maximize comfort
    }
    
    public enum VehicleType {
        CAR, MOTORCYCLE, TRUCK, BICYCLE, PEDESTRIAN, ELECTRIC_VEHICLE, HYBRID
    }
}
