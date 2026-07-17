import os
import numpy as np

try:
    import tensorflow as tf
except ImportError:
    print("Error: TensorFlow is not installed. Run: pip install tensorflow")
    import sys
    sys.exit(1)

# Path to the TFLite model
MODEL_PATH = "models/ingredient_model.tflite"

if not os.path.exists(MODEL_PATH):
    print(f"Error: {MODEL_PATH} not found. Please run 'python train.py' to compile it first.")
    import sys
    sys.exit(1)

def run_test_prediction(subscribers, duration_days, portion_multiplier, meal_type):
    # Load the TFLite model
    interpreter = tf.lite.Interpreter(model_path=MODEL_PATH)
    interpreter.allocate_tensors()
    
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    # Map meal type to one-hot encoded flags
    # [Family Meal, Protein Meal, Veg Meal]
    family = 1.0 if meal_type == "Family Meal" else 0.0
    protein = 1.0 if meal_type == "Protein Meal" else 0.0
    veg = 1.0 if meal_type == "Veg Meal" else 0.0
    
    # Build the 2D input batch
    # Features: [num_subscribers, duration_days, portion_size_multiplier, Family, Protein, Veg]
    input_data = np.array([[
        float(subscribers), 
        float(duration_days), 
        float(portion_multiplier), 
        family, 
        protein, 
        veg
    ]], dtype=np.float32)
    
    # Run prediction
    interpreter.set_tensor(input_details[0]['index'], input_data)
    interpreter.invoke()
    predictions = interpreter.get_tensor(output_details[0]['index'])[0]
    
    # Format and print output
    ingredients = ['Rice', 'Tomato', 'Onion', 'Chicken', 'Paneer']
    
    print("=" * 60)
    print("               CUSTOM TEST INGREDIENT REPORT")
    print("=" * 60)
    print(f" INPUTS:")
    print(f"  • Subscribers      : {subscribers}")
    print(f"  • Duration (Days)   : {duration_days}")
    print(f"  • Appetite Size    : {portion_multiplier}x")
    print(f"  • Meal Plan Choice : {meal_type}")
    print("-" * 60)
    print(" PREDICTED PURCHASE LIST:")
    for name, qty in zip(ingredients, predictions):
        print(f"  ➜ {name:10s} : {max(0.0, qty):6.2f} kg")
    print("=" * 60)

if __name__ == "__main__":
    # Feel free to change these test values to see different outputs!
    test_subscribers = 25
    test_duration = 30
    test_appetite = 1.2  # 1.2x portion size (large eaters)
    test_meal_type = "Veg Meal"
    
    run_test_prediction(test_subscribers, test_duration, test_appetite, test_meal_type)
