import os
import pickle
import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List

app = FastAPI(title="SoulBites AI Analytics Service", version="1.0")

# Paths to models
MODEL_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "models")
INGREDIENT_MODEL_PATH = os.path.join(MODEL_DIR, "ingredient_model.pkl")
DEMAND_MODEL_PATH = os.path.join(MODEL_DIR, "demand_model.pkl")
DISH_ENCODER_PATH = os.path.join(MODEL_DIR, "dish_encoder.pkl")

# Global variables for models
ingredient_model = None
demand_model = None
dish_encoder = None

def load_models():
    global ingredient_model, demand_model, dish_encoder
    
    # Load Ingredient model
    if os.path.exists(INGREDIENT_MODEL_PATH):
        with open(INGREDIENT_MODEL_PATH, "rb") as f:
            ingredient_model = pickle.load(f)
        print("Loaded Ingredient Estimation Model.")
    else:
        print(f"Warning: Ingredient model not found at {INGREDIENT_MODEL_PATH}")

    # Load Demand model
    if os.path.exists(DEMAND_MODEL_PATH):
        with open(DEMAND_MODEL_PATH, "rb") as f:
            demand_model = pickle.load(f)
        print("Loaded Demand Forecasting Model.")
    else:
        print(f"Warning: Demand model not found at {DEMAND_MODEL_PATH}")

    # Load Dish Encoder
    if os.path.exists(DISH_ENCODER_PATH):
        with open(DISH_ENCODER_PATH, "rb") as f:
            dish_encoder = pickle.load(f)
        print("Loaded Dish Encoder.")
    else:
        print(f"Warning: Dish Encoder not found at {DISH_ENCODER_PATH}")

# Call load on startup
@app.on_event("startup")
def startup_event():
    load_models()

# Pydantic schemas
class SubscriptionItem(BaseModel):
    meal_type: str  # Veg Meal, Protein Meal, Family Meal
    duration_days: int
    num_subscribers: int = 1

class SubscriptionRequest(BaseModel):
    subscriptions: List[SubscriptionItem]

class DishDemandItem(BaseModel):
    dish: str
    day_of_week: int
    month: int
    lag_1: float
    lag_7: float
    rolling_mean_7: float

class DemandRequest(BaseModel):
    dishes: List[DishDemandItem]

@app.post("/predict/ingredients")
def predict_ingredients(payload: SubscriptionRequest):
    if ingredient_model is None:
        raise HTTPException(status_code=503, detail="Ingredient estimation model not loaded.")
    
    # Initialize totals
    totals = np.zeros(5) # Rice, Tomato, Onion, Chicken, Paneer
    
    # Process each active subscription
    for item in payload.subscriptions:
        # Columns mapping: ['num_subscribers', 'duration_days', 'meal_type_Family Meal', 'meal_type_Protein Meal', 'meal_type_Veg Meal']
        veg = 1 if item.meal_type.lower() == "veg meal" else 0
        protein = 1 if item.meal_type.lower() == "protein meal" else 0
        family = 1 if item.meal_type.lower() == "family meal" else 0
        
        # Build features dataframe with exactly matching column names
        features = pd.DataFrame(
            [[item.num_subscribers, item.duration_days, family, protein, veg]],
            columns=['num_subscribers', 'duration_days', 'meal_type_Family Meal', 'meal_type_Protein Meal', 'meal_type_Veg Meal']
        )
        
        # Predict: returns array of shape (1, 5)
        pred = ingredient_model.predict(features)[0]
        totals += pred
        
    # Return formatted response
    return {
        "Rice": f"{totals[0]:.1f} kg",
        "Tomato": f"{totals[1]:.1f} kg",
        "Onion": f"{totals[2]:.1f} kg",
        "Chicken": f"{totals[3]:.1f} kg",
        "Paneer": f"{totals[4]:.1f} kg"
    }

@app.post("/predict/demand")
def predict_demand(payload: DemandRequest):
    if demand_model is None or dish_encoder is None:
        raise HTTPException(status_code=503, detail="Demand forecasting model or encoder not loaded.")
        
    results = []
    
    for item in payload.dishes:
        try:
            # Encode dish name
            dish_encoded = dish_encoder.transform([item.dish])[0]
        except ValueError:
            # Handle unseen dish by using a default or fallback encoding (0)
            dish_encoded = 0
            
        # Feature columns mapping: ['dish_encoded', 'day_of_week', 'month', 'lag_1', 'lag_7', 'rolling_mean_7']
        features = pd.DataFrame(
            [[dish_encoded, item.day_of_week, item.month, item.lag_1, item.lag_7, item.rolling_mean_7]],
            columns=['dish_encoded', 'day_of_week', 'month', 'lag_1', 'lag_7', 'rolling_mean_7']
        )
        
        # Predict orders count
        pred = demand_model.predict(features)[0]
        expected_orders = max(1, int(round(pred)))
        
        results.append({
            "dish": item.dish,
            "expectedOrders": expected_orders
        })
        
    return results

@app.get("/health")
def health():
    return {
        "status": "healthy",
        "models_loaded": {
            "ingredient_model": ingredient_model is not None,
            "demand_model": demand_model is not None,
            "dish_encoder": dish_encoder is not None
        }
    }
