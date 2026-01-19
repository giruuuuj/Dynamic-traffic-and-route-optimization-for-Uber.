package com.uber.traffic.repository;

import com.uber.traffic.model.Intersection;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntersectionRepository extends Neo4jRepository<Intersection, Long> {
    
    Optional<Intersection> findByNodeId(String nodeId);
    
    @Query("MATCH (i:Intersection) WHERE i.nodeId = $nodeId RETURN i")
    Optional<Intersection> findByNodeIdWithQuery(String nodeId);
    
    @Query("MATCH (i:Intersection) " +
           "WHERE point.distance(i.location, point({latitude: $lat, longitude: $lon})) < $radius " +
           "RETURN i")
    List<Intersection> findIntersectionsWithinRadius(Double lat, Double lon, Double radius);
    
    @Query("MATCH (i1:Intersection {nodeId: $fromNodeId})-[r:CONNECTS]->(i2:Intersection {nodeId: $toNodeId}) " +
           "RETURN r, i2")
    List<Intersection> findConnectedIntersections(String fromNodeId, String toNodeId);
    
    @Query("MATCH (i:Intersection) " +
           "WHERE i.latitude >= $minLat AND i.latitude <= $maxLat " +
           "AND i.longitude >= $minLon AND i.longitude <= $maxLon " +
           "RETURN i")
    List<Intersection> findIntersectionsInBoundingBox(Double minLat, Double maxLat, 
                                                   Double minLon, Double maxLon);
    
    @Query("MATCH (i:Intersection) " +
           "WHERE i.hasTrafficLight = true " +
           "RETURN i")
    List<Intersection> findIntersectionsWithTrafficLights();
    
    @Query("MATCH (i:Intersection) " +
           "WHERE i.nodeId STARTS WITH $prefix " +
           "RETURN i")
    List<Intersection> findIntersectionsByPrefix(String prefix);
}
