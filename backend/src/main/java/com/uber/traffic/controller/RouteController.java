package com.uber.traffic.controller;

import com.uber.traffic.algorithm.Route;
import com.uber.traffic.algorithm.RouteOptimizationCriteria;
import com.uber.traffic.dto.RouteRequest;
import com.uber.traffic.dto.RouteResponse;
import com.uber.traffic.service.RouteCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = "*")
public class RouteController {
    
    private final RouteCalculationService routeCalculationService;
    
    @Autowired
    public RouteController(RouteCalculationService routeCalculationService) {
        this.routeCalculationService = routeCalculationService;
    }
    
    @PostMapping("/calculate")
    public Mono<ResponseEntity<RouteResponse>> calculateOptimalRoute(
            @Valid @RequestBody RouteRequest request) {
        
        log.info("Received route calculation request: {} -> {}", 
                request.getOrigin(), request.getDestination());
        
        return Mono.fromCallable(() -> {
            try {
                RouteOptimizationCriteria criteria = convertToCriteria(request);
                Route route = routeCalculationService.calculateOptimalRoute(
                    request.getOrigin(), 
                    request.getDestination(), 
                    criteria
                );
                
                if (route != null) {
                    RouteResponse response = convertToResponse(route, request);
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.notFound().build();
                }
            } catch (Exception e) {
                log.error("Error calculating route", e);
                return ResponseEntity.internalServerError().build();
            }
        });
    }
    
    @PostMapping("/alternatives")
    public Mono<ResponseEntity<List<RouteResponse>>> calculateAlternativeRoutes(
            @Valid @RequestBody RouteRequest request,
            @RequestParam(defaultValue = "3") int maxAlternatives) {
        
        log.info("Received alternative routes request: {} -> {}", 
                request.getOrigin(), request.getDestination());
        
        return Mono.fromCallable(() -> {
            try {
                RouteOptimizationCriteria criteria = convertToCriteria(request);
                List<Route> routes = routeCalculationService.calculateAlternativeRoutes(
                    request.getOrigin(),
                    request.getDestination(),
                    criteria,
                    maxAlternatives
                );
                
                List<RouteResponse> responses = routes.stream()
                    .map(route -> convertToResponse(route, request))
                    .toList();
                
                return ResponseEntity.ok(responses);
            } catch (Exception e) {
                log.error("Error calculating alternative routes", e);
                return ResponseEntity.internalServerError().build();
            }
        });
    }
    
    @PostMapping("/recalculate/{routeId}")
    public Mono<ResponseEntity<RouteResponse>> recalculateRoute(
            @PathVariable String routeId,
            @Valid @RequestBody RouteRequest request) {
        
        log.info("Received route recalculation request for route: {}", routeId);
        
        return Mono.fromCallable(() -> {
            try {
                RouteOptimizationCriteria criteria = convertToCriteria(request);
                Route route = routeCalculationService.recalculateRoute(
                    routeId,
                    request.getCurrentLocation(),
                    request.getDestination(),
                    criteria
                );
                
                if (route != null) {
                    RouteResponse response = convertToResponse(route, request);
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.notFound().build();
                }
            } catch (Exception e) {
                log.error("Error recalculating route", e);
                return ResponseEntity.internalServerError().build();
            }
        });
    }
    
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthCheck() {
        return Mono.just(ResponseEntity.ok("Route calculation service is healthy"));
    }
    
    private RouteOptimizationCriteria convertToCriteria(RouteRequest request) {
        RouteOptimizationCriteria criteria = new RouteOptimizationCriteria(
            RouteOptimizationCriteria.OptimizationObjective.valueOf(
                request.getOptimizationObjective().toUpperCase()
            )
        );
        
        criteria.setMaxDistance(request.getMaxDistance());
        criteria.setMaxTravelTime(request.getMaxTravelTime());
        criteria.setMaxCost(request.getMaxCost());
        criteria.setAvoidTolls(request.getAvoidTolls());
        criteria.setAvoidHighways(request.getAvoidHighways());
        criteria.setPreferScenic(request.getPreferScenic());
        criteria.setEcoWeight(request.getEcoWeight());
        criteria.setDepartureHour(request.getDepartureHour());
        criteria.setArrivalHour(request.getArrivalHour());
        criteria.setConsiderRushHour(request.getConsiderRushHour());
        
        if (request.getVehicleType() != null) {
            criteria.setVehicleType(RouteOptimizationCriteria.VehicleType.valueOf(
                request.getVehicleType().toUpperCase()
            ));
        }
        
        return criteria;
    }
    
    private RouteResponse convertToResponse(Route route, RouteRequest request) {
        RouteResponse response = new RouteResponse();
        response.setRouteId(route.getRouteId());
        response.setOrigin(request.getOrigin());
        response.setDestination(request.getDestination());
        response.setNodeIds(route.getNodeIds());
        response.setEdgeIds(route.getEdgeIds());
        response.setTotalDistance(route.getTotalDistance());
        response.setTotalTravelTime(route.getTotalTravelTime());
        response.setTotalCost(route.getTotalCost());
        response.setConfidenceScore(route.getConfidenceScore());
        response.setRouteType(route.getRouteType().name());
        response.setCalculatedAt(route.getCalculatedAt());
        response.setValidUntil(route.getValidUntil());
        response.setTrafficLights(route.getTrafficLights());
        response.setTollRoads(route.getTollRoads());
        response.setAverageSpeed(route.getAverageSpeed());
        response.setCongestionLevel(route.getCongestionLevel());
        response.setWeatherImpact(route.getWeatherImpact());
        
        // Convert alternative routes
        if (route.getAlternativeRoutes() != null) {
            List<RouteResponse> alternatives = route.getAlternativeRoutes().stream()
                .map(alt -> convertToResponse(alt, request))
                .toList();
            response.setAlternativeRoutes(alternatives);
        }
        
        return response;
    }
}
