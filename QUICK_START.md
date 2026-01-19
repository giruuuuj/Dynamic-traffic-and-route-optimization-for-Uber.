# 🚀 Uber Traffic Optimization System - Quick Start Guide

## Prerequisites
- Docker & Docker Compose
- Java 17+ (for backend development)
- Node.js 18+ (for frontend development)
- Git

## Quick Start Commands

### 1. Start All Services
```bash
# Navigate to project directory
cd "d:/project 2/Uber Dynamic traffic and route optimization"

# Start all services with Docker Compose
docker-compose up -d

# Check service status
docker-compose ps
```

### 2. Access Services

#### Backend API
- **URL**: http://localhost:8080/api/v1
- **Health Check**: http://localhost:8080/api/v1/routes/health
- **API Docs**: http://localhost:8080/swagger-ui.html (when running)

#### Frontend Application
- **URL**: http://localhost:3000
- **Interactive Map**: Available immediately

#### ML Service
- **URL**: http://localhost:8000
- **API Docs**: http://localhost:8000/docs
- **Health Check**: http://localhost:8000/health

#### Database & Infrastructure
- **Neo4j Browser**: http://localhost:7474
- **Redis CLI**: `docker exec -it uber-redis redis-cli`
- **Kafka Topics**: `docker exec -it uber-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list`

### 3. Development Setup

#### Backend Development
```bash
# Navigate to backend
cd backend

# Run with Maven
./mvnw spring-boot:run

# Or with your IDE
# Import project as Maven project in IntelliJ/Eclipse
```

#### Frontend Development
```bash
# Navigate to frontend
cd frontend

# Install dependencies
npm install

# Start development server
npm start

# Or with your IDE
# Open folder in VS Code/WebStorm
```

#### ML Service Development
```bash
# Navigate to ML service
cd ml-service

# Install dependencies
pip install -r requirements.txt

# Start development server
uvicorn main:app --host 0.0.0.0 --port 8000 --reload

# Or run with Docker
docker exec -it uber-ml-service uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### 4. Test the System

#### Test API Endpoints
```bash
# Test route calculation
curl -X POST http://localhost:8080/api/v1/routes/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "node_1",
    "destination": "node_2",
    "optimizationObjective": "FASTEST"
  }'

# Test ML prediction
curl -X POST http://localhost:8000/predict/traffic \
  -H "Content-Type: application/json" \
  -d '{
    "segment_id": "test_segment",
    "current_speed": 45.0,
    "congestion_factor": 0.3,
    "traffic_density": 25.0
  }'
```

#### Load Sample Data
```bash
# Use the provided scripts to load sample data
# (These would be in a scripts/ directory)
```

### 5. Monitor Services

#### Check Logs
```bash
# View all service logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f traffic-backend
docker-compose logs -f traffic-frontend
docker-compose logs -f ml-service
```

#### Monitor Resource Usage
```bash
# Check resource usage
docker stats

# Check disk usage
df -h

# Check memory usage
free -h
```

### 6. Common Development Tasks

#### Add New Road Segments
```bash
# Access Neo4j browser
# Open http://localhost:7474

# Create nodes and relationships
# Example Cypher query:
CREATE (n1:Intersection {nodeId: 'new_node_1', latitude: 40.7128, longitude: -74.0060})
CREATE (n2:Intersection {nodeId: 'new_node_2', latitude: 40.7589, longitude: -73.9851})
CREATE (n1)-[:CONNECTS {distance: 1000, roadType: 'ARTERIAL'}]->(n2)
```

#### Test Traffic Updates
```bash
# Send GPS data to Kafka
docker exec -it uber-kafka kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic gps-data \
  --message '{"vehicleId": "test_vehicle", "latitude": 40.7128, "longitude": -74.0060, "speed": 45.0}'
```

### 7. Production Deployment

#### Environment Setup
```bash
# Copy environment template
cp .env.example .env

# Edit with your values
# NEO4J_URI, REDIS_HOST, KAFKA_BOOTSTRAP_SERVERS, etc.
```

#### Build and Deploy
```bash
# Build production images
docker-compose -f docker-compose.yml build

# Deploy to production
docker-compose -f docker-compose.prod.yml up -d
```

### 8. Troubleshooting

#### Common Issues
1. **Port conflicts**: Ensure ports 8080, 3000, 8000, 7474, 6379, 9092 are available
2. **Memory issues**: Increase Docker memory limits in docker-compose.yml
3. **Network issues**: Check firewall settings and Docker network configuration
4. **Service not starting**: Check logs with `docker-compose logs [service-name]`

#### Health Checks
```bash
# Backend health
curl http://localhost:8080/api/v1/routes/health

# Frontend health
curl http://localhost:3000

# ML service health
curl http://localhost:8000/health

# Database connection
docker exec -it uber-neo4j cypher-shell -u neo4j -p password "RETURN 1"
```

### 9. API Documentation

#### Main Endpoints
- `POST /api/v1/routes/calculate` - Calculate optimal route
- `POST /api/v1/routes/alternatives` - Get alternative routes
- `POST /api/v1/routes/recalculate/{routeId}` - Recalculate route
- `GET /api/v1/routes/health` - Health check

#### ML Service Endpoints
- `POST /predict/traffic` - Predict traffic conditions
- `POST /predict/batch` - Batch traffic prediction
- `POST /train/model` - Retrain ML model
- `GET /model/info` - Get model information
- `GET /health` - Health check

### 10. Development Workflow
1. Start infrastructure services
2. Run backend development server
3. Run frontend development server  
4. Make changes to code
5. Test APIs and functionality
6. Commit changes
7. Repeat

---

## 🎯 Next Steps
- Load sample traffic data into Neo4j
- Test route calculations with real scenarios
- Configure external API keys (TomTom, Weather)
- Set up monitoring dashboards
- Deploy to staging environment

## 📞 Support
For issues or questions:
1. Check service logs first
2. Review this README
3. Check API documentation at /docs endpoints
4. Refer to troubleshooting section above

Happy coding! 🚀
