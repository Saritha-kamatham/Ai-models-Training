import os
import json
import numpy as np

try:
    import tensorflow as tf
except ImportError:
    print("Error: TensorFlow is not installed. Run: pip install tensorflow")
    import sys
    sys.exit(1)

# Paths to the model and label mapping
MODEL_PATH = "models/demand_model.tflite"
LABELS_PATH = "models/dish_labels.json"

if not os.path.exists(MODEL_PATH) or not os.path.exists(LABELS_PATH):
    print("Error: Model or label file not found. Please run 'python train.py' first.")
    import sys
    sys.exit(1)

def run_demand_test(day_of_week, month):
    # Load TFLite Model
    interpreter = tf.lite.Interpreter(model_path=MODEL_PATH)
    interpreter.allocate_tensors()
    
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    # Load dish label mappings
    with open(LABELS_PATH, "r") as f:
        dish_labels = json.load(f)
        
    # Simulated historical sales average for each dish
    recent_sales_averages = {
        "Hyderabadi Chicken Biryani": 50,
        "Paneer Butter Masala": 35,
        "Butter Chicken": 40,
        "Masala Dosa": 45,
        "Dal Makhani": 25,
        "Chole Bhature": 30,
        "Tandoori Chicken": 30,
        "Gulab Jamun": 20
    }
    
    predictions = []
    
    # Run prediction for each dish
    for dish, avg_sales in recent_sales_averages.items():
        dish_encoded = dish_labels.get(dish, 0)
        
        # Features: [dish_encoded, day_of_week, month, lag_1, lag_7, rolling_mean_7]
        # Simulating active trends based on typical averages
        lag_1 = float(avg_sales * 1.1)
        lag_7 = float(avg_sales * 1.2)
        rolling_mean_7 = float(avg_sales * 1.05)
        
        input_data = np.array([[
            float(dish_encoded), 
            float(day_of_week), 
            float(month), 
            lag_1, 
            lag_7, 
            rolling_mean_7
        ]], dtype=np.float32)
        
        # Run inference
        interpreter.set_tensor(input_details[0]['index'], input_data)
        interpreter.invoke()
        pred_val = interpreter.get_tensor(output_details[0]['index'])[0][0]
        
        expected_orders = max(1, int(round(pred_val)))
        predictions.append({
            "dish": dish,
            "expected_orders": expected_orders
        })
        
    # Sort dishes by highest expected orders (most ordered at the top)
    predictions.sort(key=lambda x: x['expected_orders'], reverse=True)
    
    print("=" * 60)
    print("        ESTIMATED DISH DEMAND RANKING REPORT (TFLITE)")
    print("=" * 60)
    print(f" Target Date Context: Weekday ID {day_of_week} (0=Mon, 4=Fri, 5=Sat), Month {month}")
    print("-" * 60)
    print(f"{'Rank':5s} | {'Dish Name':30s} | {'Expected Orders':15s}")
    print("-" * 60)
    for idx, item in enumerate(predictions):
        print(f"#{idx+1:<4d} | {item['dish']:30s} | {item['expected_orders']:<15d}")
    print("=" * 60)

if __name__ == "__main__":
    # Test for Friday (4) in July (7)
    test_day = 4 
    test_month = 7
    run_demand_test(test_day, test_month)
