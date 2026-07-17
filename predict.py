import os
import json
import numpy as np
import pandas as pd

# Define paths
MODEL_DIR = "models"
INGREDIENT_MODEL_PATH = os.path.join(MODEL_DIR, "ingredient_model.tflite")
DEMAND_MODEL_PATH = os.path.join(MODEL_DIR, "demand_model.tflite")
DISH_LABELS_PATH = os.path.join(MODEL_DIR, "dish_labels.json")

# Standard check for tensorflow
try:
    import tensorflow as tf
except ImportError:
    print("\n" + "="*70)
    print("ERROR: TensorFlow is required to run predict.py.")
    print("Please install it in your terminal by running: pip install tensorflow")
    print("="*70 + "\n")
    import sys
    sys.exit(1)

def check_models():
    for path in [INGREDIENT_MODEL_PATH, DEMAND_MODEL_PATH, DISH_LABELS_PATH]:
        if not os.path.exists(path):
            print(f"Error: Model file {path} not found. Please run 'python train.py' first.")
            return False
    return True

def run_tflite_inference(interpreter, input_data):
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    # Format input data as 2D array of float32
    input_arr = np.array(input_data, dtype=np.float32)
    interpreter.set_tensor(input_details[0]['index'], input_arr)
    
    interpreter.invoke()
    return interpreter.get_tensor(output_details[0]['index'])

def run_predictions():
    if not check_models():
        return
        
    # Load TFLite models
    interpreter_ing = tf.lite.Interpreter(model_path=INGREDIENT_MODEL_PATH)
    interpreter_ing.allocate_tensors()
    
    interpreter_dem = tf.lite.Interpreter(model_path=DEMAND_MODEL_PATH)
    interpreter_dem.allocate_tensors()
    
    # Load labels
    with open(DISH_LABELS_PATH, "r") as f:
        dish_labels = json.load(f)
        
    print("=" * 60)
    print("       SOULBITES TENSORFLOW LITE INFERENCE REPORT")
    print("=" * 60)
    
    # ---------------------------------------------------------
    # FEATURE 1: Ingredient Estimation for Subscriptions
    # ---------------------------------------------------------
    print("\n[AI FEATURE 1: INGREDIENT PURCHASE ESTIMATIONS (TFLITE)]")
    print("-" * 50)
    
    sample_subs = [
        {"meal_type": "Veg Meal", "duration": 30, "subs": 20, "portion": 1.0},
        {"meal_type": "Protein Meal", "duration": 15, "subs": 15, "portion": 1.5},
        {"meal_type": "Family Meal", "duration": 7, "subs": 10, "portion": 0.8}
    ]
    
    total_ingredients = np.zeros(5)
    ingredient_names = ['Rice', 'Tomato', 'Onion', 'Chicken', 'Paneer']
    
    print("Active Subscriptions entered:")
    for sub in sample_subs:
        print(f"  • {sub['subs']:2d} users on {sub['meal_type']:13s} for {sub['duration']:2d} days (appetite multiplier: {sub['portion']:.1f}x)")
        
        # Build features dataframe for model:
        # Columns: ['num_subscribers', 'duration_days', 'portion_size_multiplier', 'meal_type_Family Meal', 'meal_type_Protein Meal', 'meal_type_Veg Meal']
        veg = 1.0 if sub['meal_type'] == "Veg Meal" else 0.0
        protein = 1.0 if sub['meal_type'] == "Protein Meal" else 0.0
        family = 1.0 if sub['meal_type'] == "Family Meal" else 0.0
        
        # Prepare 2D input batch
        input_data = [[float(sub['subs']), float(sub['duration']), float(sub['portion']), family, protein, veg]]
        
        # TFLite inference
        pred = run_tflite_inference(interpreter_ing, input_data)[0]
        total_ingredients += pred
        
    print("\nEstimated Ingredients to Purchase:")
    for name, qty in zip(ingredient_names, total_ingredients):
        print(f"  ➜ {name:10s} : {max(0.0, qty):6.2f} kg")
        
    # ---------------------------------------------------------
    # FEATURE 2: Chef Recommendations & Demand Forecasting
    # ---------------------------------------------------------
    print("\n[AI FEATURE 2: CHEF PREPARATION RECOMMENDATIONS (TFLITE)]")
    print("-" * 50)
    
    day_of_week = 4 # Friday
    month = 7       # July
    
    # Simulated recent daily average orders (for comparing rising/dropping demand)
    recent_averages = {
        "Hyderabadi Chicken Biryani": 50, "Paneer Butter Masala": 35, "Butter Chicken": 40,
        "Masala Dosa": 45, "Dal Makhani": 25, "Chole Bhature": 30,
        "Tandoori Chicken": 30, "Gulab Jamun": 20
    }
    
    recommendations = []
    
    for dish, recent_avg in recent_averages.items():
        # Get label encoding from json map
        dish_encoded = dish_labels.get(dish, 0)
        
        # Features: ['dish_encoded', 'day_of_week', 'month', 'lag_1', 'lag_7', 'rolling_mean_7']
        # Simulating slightly higher demand for Friday (lag_1 was high, rolling is near base)
        lag_1 = float(recent_avg * 1.1)
        lag_7 = float(recent_avg * 1.2)
        rolling_mean_7 = float(recent_avg * 1.05)
        
        input_data = [[float(dish_encoded), float(day_of_week), float(month), lag_1, lag_7, rolling_mean_7]]
        
        # TFLite inference
        pred = run_tflite_inference(interpreter_dem, input_data)[0][0]
        expected_orders = max(1, int(round(pred)))
        
        if expected_orders > recent_avg * 1.15:
            priority = "HIGH"
            suggestion = f"✔ Increase ingredients & preparation (Demand rising to {expected_orders} orders)"
        elif expected_orders < recent_avg * 0.85:
            priority = "LOW"
            suggestion = f"✘ Reduce preparation to avoid wastage (Demand dropping to {expected_orders} orders)"
        else:
            priority = "NORMAL"
            suggestion = f"● Maintain normal stock & prep (Demand steady around {expected_orders} orders)"
            
        recommendations.append({
            "dish": dish,
            "expected": expected_orders,
            "priority": priority,
            "suggestion": suggestion
        })

    priority_order = {"HIGH": 0, "NORMAL": 1, "LOW": 2}
    recommendations.sort(key=lambda x: priority_order[x['priority']])
    
    print(f"{'Dish Name':30s} | {'Priority':8s} | Recommendation / Suggestion")
    print("=" * 100)
    for rec in recommendations:
        print(f"{rec['dish']:30s} | {rec['priority']:8s} | {rec['suggestion']}")
        
    print("=" * 60)

if __name__ == '__main__':
    run_predictions()
