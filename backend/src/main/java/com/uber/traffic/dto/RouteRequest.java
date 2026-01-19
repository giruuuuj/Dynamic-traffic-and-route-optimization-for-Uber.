package com.uber.traffic.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteRequest {
    
    @NotBlank(message = "Origin node ID is required")
    private String origin;
    
    @NotBlank(message = "Destination node ID is required")
    private String destination;
    
    private String currentLocation; // For route recalculation
    
    @NotNull(message = "Optimization objective is required")
    private String optimizationObjective; // FASTEST, SHORTEST, ECONOMICAL, ECO_FRIENDLY
    
    // Constraints
    @Min(value = 0, message = "Max distance must be positive")
    private Double maxDistance; // in km
    
    @Min(value = 0, message = "Max travel time must be positive")
    private Double maxTravelTime; // in minutes
    
    @Min(value = 0, message = "Max cost must be positive")
    private Double maxCost; // in currency units
    
    // Preferences
    private Boolean avoidTolls = false;
    private Boolean avoidHighways = false;
    private Boolean preferScenic = false;
    
    @Min(value = 0, message = "Eco weight must be between 0 and 1")
    @Max(value = 1, message = "Eco weight must be between 0 and 1")
    private Double ecoWeight = 0.0;
    
    // Time preferences
    @Min(value = 0, message = "Departure hour must be between 0 and 23")
    @Max(value = 23, message = "Departure hour must be between 0 and 23")
    private Integer departureHour;
    
    @Min(value = 0, message = "Arrival hour must be between 0 and 23")
    @Max(value = 23, message = "Arrival hour must be between 0 and 23")
    private Integer arrivalHour;
    
    private Boolean considerRushHour = true;
    
    // Vehicle preferences
    private String vehicleType; // CAR, MOTORCYCLE, TRUCK, BICYCLE, PEDESTRIAN, ELECTRIC_VEHICLE, HYBRID
    
    @Min(value = 0, message = "Vehicle range must be positive")
    private Double vehicleRange; // for EVs in km
    
    private Boolean requireChargingStations = false;
    
    // Multi-criteria weights (should sum to 1.0)
    @Min(value = 0, message = "Time weight must be between 0 and 1")
    @Max(value = 1, message = "Time weight must be between 0 and 1")
    private Double timeWeight = 0.4;
    
    @Min(value = 0, message = "Distance weight must be between 0 and 1")
    @Max(value = 1, message = "Distance weight must be between 0 and 1")
    private Double distanceWeight = 0.2;
    
    @Min(value = 0, message = "Cost weight must be between 0 and 1")
    @Max(value = 1, message = "Cost weight must be between 0 and 1")
    private Double costWeight = 0.2;
    
    @Min(value = 0, message = "Comfort weight must be between 0 and 1")
    @Max(value = 1, message = "Comfort weight must be between 0 and 1")
    private Double comfortWeight = 0.1;
    
    @Min(value = 0, message = "Safety weight must be between 0 and 1")
    @Max(value = 1, message = "Safety weight must be between 0 and 1")
    private Double safetyWeight = 0.1;
}
