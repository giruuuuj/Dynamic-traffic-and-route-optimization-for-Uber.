package com.uber.traffic.repository;

import com.uber.traffic.model.RoadSegment;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadSegmentRepository extends Neo4jRepository<RoadSegment, Long> {
    
    Optional<RoadSegment> findBySegmentId(String segmentId);
    
    @Query("MATCH (rs:RoadSegment) WHERE rs.segmentId = $segmentId RETURN rs")
    Optional<RoadSegment> findBySegmentIdWithQuery(String segmentId);
    
    @Query("MATCH (from:Intersection {nodeId: $fromNodeId})-[r:CONNECTS]->(to:Intersection {nodeId: $toNodeId}) " +
           "RETURN r")
    List<RoadSegment> findRoadSegmentsBetweenIntersections(String fromNodeId, String toNodeId);
    
    @Query("MATCH (i:Intersection {nodeId: $nodeId})-[r:CONNECTS]->(adjacent:Intersection) " +
           "RETURN r, adjacent")
    List<RoadSegment> findOutgoingRoadSegments(String nodeId);
    
    @Query("MATCH (adjacent:Intersection)-[r:CONNECTS]->(i:Intersection {nodeId: $nodeId}) " +
           "RETURN r, adjacent")
    List<RoadSegment> findIncomingRoadSegments(String nodeId);
    
    @Query("MATCH (rs:RoadSegment) " +
           "WHERE rs.roadType = $roadType " +
           "RETURN rs")
    List<RoadSegment> findByRoadType(RoadSegment.RoadType roadType);
    
    @Query("MATCH (rs:RoadSegment) " +
           "WHERE rs.tollRoad = true " +
           "RETURN rs")
    List<RoadSegment> findTollRoads();
    
    @Query("MATCH (rs:RoadSegment) " +
           "WHERE rs.oneWay = true " +
           "RETURN rs")
    List<RoadSegment> findOneWayRoads();
    
    @Query("MATCH (rs:RoadSegment) " +
           "WHERE rs.distance >= $minDistance AND rs.distance <= $maxDistance " +
           "RETURN rs")
    List<RoadSegment> findByDistanceRange(Double minDistance, Double maxDistance);
    
    @Query("MATCH (rs:RoadSegment) " +
           "WHERE rs.baseSpeedLimit >= $minSpeed AND rs.baseSpeedLimit <= $maxSpeed " +
           "RETURN rs")
    List<RoadSegment> findBySpeedLimitRange(Integer minSpeed, Integer maxSpeed);
    
    @Query("MATCH (i1:Intersection)-[r:CONNECTS]->(i2:Intersection) " +
           "WHERE point.distance(i1.location, point({latitude: $lat, longitude: $lon})) < $radius " +
           "RETURN r, i1, i2")
    List<RoadSegment> findRoadSegmentsNearLocation(Double lat, Double lon, Double radius);
    
    @Query("MATCH (rs:RoadSegment) " +
           "WHERE rs.name CONTAINS $name " +
           "RETURN rs")
    List<RoadSegment> findByNameContaining(String name);
}
