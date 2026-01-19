from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import redis
import json
import numpy as np
from datetime import datetime, timedelta
import logging
import os
import asyncio
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import StandardScaler
import joblib

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Redis connection
redis_client = redis.Redis(
    host=os.getenv('REDIS_URL', 'localhost'),
    port=int(os.getenv('REDIS_PORT', 6379)),
    decode_responses=False
)

# ML Model (will be loaded from file or trained on startup)
traffic_model = None
scaler = None

class TrafficData(BaseModel):
    segment_id: str
    current_speed: float
    congestion_factor: float
    traffic_density: float
    timestamp: datetime

class TrafficPrediction(BaseModel):
    segment_id: str
    predicted_speed: float
    predicted_congestion: float
    prediction_time: datetime
    valid_until: datetime
    confidence: float

class RouteRequest(BaseModel):
    origin: str
    destination: str
    optimization_objective: str = "FASTEST"
    max_distance: Optional[float] = None
    max_travel_time: Optional[float] = None
    max_cost: Optional[float] = None
    avoid_tolls: bool = False
    avoid_highways: bool = False
    prefer_scenic: bool = False
    eco_weight: float = 0.0
    departure_hour: Optional[int] = None
    arrival_hour: Optional[int] = None
    consider_rush_hour: bool = True

class RouteResponse(BaseModel):
    route_id: str
    origin: str
    destination: str
    total_distance: float
    total_travel_time: float
    total_cost: float
    confidence_score: float
    route_type: str
    calculated_at: datetime
    valid_until: datetime
    traffic_lights: int
    toll_roads: int
    average_speed: float
    congestion_level: float
    weather_impact: float

class TrafficAlert(BaseModel):
    alert_id: str
    alert_type: str
    segment_id: str
    latitude: float
    longitude: float
    description: str
    severity: int
    timestamp: datetime

# Initialize ML model
def initialize_model():
    global traffic_model, scaler
    
    try:
        # Try to load existing model
        traffic_model = joblib.load('/app/models/traffic_prediction_model.pkl')
        scaler = joblib.load('/app/models/scaler.pkl')
        logger.info("Loaded existing ML model")
    except FileNotFoundError:
        logger.info("No existing model found, training new model...")
        train_new_model()

def train_new_model():
    """Train a new traffic prediction model with sample data"""
    # Generate sample training data
    np.random.seed(42)
    n_samples = 1000
    
    # Features: hour_of_day, day_of_week, weather_impact, base_speed_limit, historical_congestion
    hour_of_day = np.random.randint(0, 24, n_samples)
    day_of_week = np.random.randint(0, 7, n_samples)
    weather_impact = np.random.uniform(0, 1, n_samples)  # 0 = clear, 1 = severe weather
    base_speed_limit = np.random.uniform(30, 80, n_samples)  # km/h
    historical_congestion = np.random.uniform(0, 1, n_samples)
    
    # Target: current_speed
    # Simulate traffic patterns
    rush_hour_factor = np.where((hour_of_day >= 7) & (hour_of_day <= 9) | (hour_of_day >= 16) & (hour_of_day <= 19), 0.7, 1.0)
    weekend_factor = np.where(day_of_week >= 5, 1.2, 1.0)  # Higher traffic on weekends
    
    current_speed = base_speed_limit * (1 - historical_congestion * 0.5) * rush_hour_factor * weekend_factor
    current_speed *= (1 - weather_impact * 0.3)  # Weather reduces speed
    
    X = np.column_stack([hour_of_day, day_of_week, weather_impact, base_speed_limit, historical_congestion])
    y = current_speed
    
    # Split and scale data
    from sklearn.model_selection import train_test_split
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    
    # Train model
    model = RandomForestRegressor(n_estimators=100, random_state=42)
    model.fit(X_train_scaled, y_train)
    
    # Evaluate model
    train_score = model.score(X_train_scaled, y_train)
    test_score = model.score(X_test_scaled, y_test)
    
    logger.info(f"Model trained - Train score: {train_score:.3f}, Test score: {test_score:.3f}")
    
    # Save model and scaler
    os.makedirs('/app/models', exist_ok=True)
    joblib.dump(model, '/app/models/traffic_prediction_model.pkl')
    joblib.dump(scaler, '/app/models/scaler.pkl')
    
    return model, scaler

# Prediction function
def predict_traffic(segment_id: str, current_data: TrafficData) -> TrafficPrediction:
    """Predict traffic conditions for a road segment"""
    global traffic_model, scaler
    
    if traffic_model is None or scaler is None:
        raise HTTPException(status_code=500, detail="ML model not loaded")
    
    try:
        # Extract features for prediction
        current_time = datetime.now()
        hour_of_day = current_time.hour
        day_of_week = current_time.weekday()
        
        # Get historical data from Redis
        historical_key = f"historical_traffic:{segment_id}"
        historical_data = redis_client.lrange(historical_key, 0, -1)
        
        # Calculate average historical congestion
        avg_historical_congestion = 0.3  # Default if no data
        if historical_data:
            congestion_values = []
            for data in historical_data:
                try:
                    traffic_json = json.loads(data)
                    congestion_values.append(traffic_json.get('congestion_factor', 0.3))
                except:
                    continue
            if congestion_values:
                avg_historical_congestion = np.mean(congestion_values)
        
        # Prepare features
        features = np.array([[
            hour_of_day,
            day_of_week,
            current_data.weather_impact,
            50.0,  # Assume base speed limit of 50 km/h
            avg_historical_congestion
        ]]).reshape(1, -1)
        
        # Scale features
        features_scaled = scaler.transform(features)
        
        # Make prediction
        predicted_speed = traffic_model.predict(features_scaled)[0]
        
        # Calculate predicted congestion
        base_speed = 50.0  # Base speed limit
        predicted_congestion = max(0, min(1, (base_speed - predicted_speed) / base_speed))
        
        # Create prediction
        prediction = TrafficPrediction(
            segment_id=segment_id,
            predicted_speed=float(predicted_speed),
            predicted_congestion=float(predicted_congestion),
            prediction_time=current_time,
            valid_until=current_time + timedelta(minutes=30),
            confidence=min(0.95, max(0.5, 1 - abs(predicted_congestion - 0.5)))  # Higher confidence for moderate congestion
        )
        
        # Cache prediction
        prediction_key = f"prediction:{segment_id}"
        redis_client.setex(
            prediction_key,
            prediction.json(),
            ex=1800  # 30 minutes
        )
        
        logger.info(f"Generated traffic prediction for segment {segment_id}: speed={predicted_speed:.1f}, congestion={predicted_congestion:.2f}")
        
        return prediction
        
    except Exception as e:
        logger.error(f"Error predicting traffic for segment {segment_id}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")

# FastAPI app
app = FastAPI(title="Uber Traffic Prediction Service", version="1.0.0")

@app.on_event("startup")
async def startup_event():
    logger.info("Starting Traffic Prediction Service...")
    initialize_model()

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "traffic-prediction"}

@app.post("/predict/traffic")
async def predict_traffic_endpoint(traffic_data: TrafficData):
    """Predict traffic conditions for a road segment"""
    try:
        prediction = predict_traffic(traffic_data.segment_id, traffic_data)
        return prediction
    except HTTPException as e:
        raise e
    except Exception as e:
        logger.error(f"Unexpected error in traffic prediction: {str(e)}")
        raise HTTPException(status_code=500, detail="Internal server error")

@app.post("/predict/batch")
async def predict_batch_traffic(traffic_data_list: List[TrafficData]):
    """Predict traffic conditions for multiple road segments"""
    try:
        predictions = []
        for traffic_data in traffic_data_list:
            prediction = predict_traffic(traffic_data.segment_id, traffic_data)
            predictions.append(prediction)
        
        return {"predictions": predictions, "count": len(predictions)}
    except Exception as e:
        logger.error(f"Error in batch traffic prediction: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Batch prediction failed: {str(e)}")

@app.post("/train/model")
async def train_model_endpoint():
    """Retrain the traffic prediction model"""
    try:
        # Collect training data from Redis
        training_data = []
        for key in redis_client.scan_iter(match="historical_traffic:*"):
            data_list = redis_client.lrange(key, 0, -1)
            for data in data_list:
                try:
                    traffic_json = json.loads(data)
                    training_data.append({
                        'hour_of_day': datetime.now().hour,
                        'day_of_week': datetime.now().weekday(),
                        'weather_impact': traffic_json.get('weather_impact', 0),
                        'base_speed_limit': 50.0,
                        'historical_congestion': traffic_json.get('congestion_factor', 0.3),
                        'current_speed': traffic_json.get('current_speed', 25.0)
                    })
                except:
                    continue
        
        if len(training_data) < 100:
            raise HTTPException(status_code=400, detail="Insufficient training data (need at least 100 samples)")
        
        # Train new model
        global traffic_model, scaler
        traffic_model, scaler = train_new_model()
        
        return {"message": "Model retrained successfully", "samples_used": len(training_data)}
    except Exception as e:
        logger.error(f"Error training model: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Model training failed: {str(e)}")

@app.get("/model/info")
async def get_model_info():
    """Get information about the current ML model"""
    global traffic_model, scaler
    
    if traffic_model is None:
        raise HTTPException(status_code=404, detail="No model loaded")
    
    return {
        "model_type": "RandomForestRegressor",
        "features": ["hour_of_day", "day_of_week", "weather_impact", "base_speed_limit", "historical_congestion"],
        "target": "current_speed",
        "model_loaded": traffic_model is not None,
        "scaler_loaded": scaler is not None,
        "last_training": datetime.now().isoformat()
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
