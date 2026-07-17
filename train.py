import os
import json
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

# Standard error message if tensorflow is missing
try:
    import tensorflow as tf
except ImportError:
    print("\n" + "="*70)
    print("ERROR: TensorFlow is required to run this script.")
    print("Please install it in your terminal by running: pip install tensorflow")
    print("="*70 + "\n")
    import sys
    sys.exit(1)

def train_ingredient_model():
    print("--- 1. Training Ingredient Estimation Neural Network & Exporting to TFLite ---")
    np.random.seed(42)
    tf.random.set_seed(42)
    num_samples = 1500
    
    # Features
    subscribers = np.random.randint(5, 60, size=num_samples)
    durations = np.random.choice([7, 15, 30], size=num_samples)
    meal_types = np.random.choice(['Veg Meal', 'Protein Meal', 'Family Meal'], size=num_samples)
    portion_multipliers = np.random.choice([0.8, 1.0, 1.2, 1.5], size=num_samples, p=[0.2, 0.5, 0.2, 0.1])
    
    # Ingredient bases (kg per subscriber serving)
    recipe_bases = {
        'Veg Meal':     [0.15, 0.10, 0.08, 0.00, 0.12],
        'Protein Meal': [0.10, 0.08, 0.06, 0.20, 0.00],
        'Family Meal':  [0.50, 0.30, 0.25, 0.40, 0.20]
    }
    
    targets = []
    for i in range(num_samples):
        m_type = meal_types[i]
        subs = subscribers[i]
        days = durations[i]
        mult = portion_multipliers[i]
        
        base_needs = np.array(recipe_bases[m_type]) * subs * days * mult
        noise = np.random.normal(1.0, 0.03, size=5) # small variance for clean neural net training
        actual_needs = np.maximum(0, base_needs * noise)
        targets.append(actual_needs)
        
    targets = np.array(targets)
    sub_df = pd.DataFrame({
        'num_subscribers': subscribers,
        'duration_days': durations,
        'portion_size_multiplier': portion_multipliers,
        'meal_type': meal_types,
        'Rice_qty': targets[:, 0],
        'Tomato_qty': targets[:, 1],
        'Onion_qty': targets[:, 2],
        'Chicken_qty': targets[:, 3],
        'Paneer_qty': targets[:, 4]
    })
    
    # Save dataset to CSV for user viewing
    sub_df.to_csv('synthetic_subscriptions.csv', index=False)
    print("  Saved ingredient dataset to synthetic_subscriptions.csv")
    
    # One-hot encode meal type
    X = pd.get_dummies(sub_df[['num_subscribers', 'duration_days', 'portion_size_multiplier', 'meal_type']], columns=['meal_type'])
    y = sub_df[['Rice_qty', 'Tomato_qty', 'Onion_qty', 'Chicken_qty', 'Paneer_qty']]
    
    # Convert data types to float32 (standard for TFLite)
    X = X.astype(np.float32)
    y = y.astype(np.float32)
    
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    # Build Keras Multi-Output Regression Model
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(6,)),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(5) # 5 ingredient outputs
    ])
    
    model.compile(optimizer='adam', loss='mae')
    
    print("  Training neural network...")
    model.fit(X_train, y_train, epochs=80, batch_size=32, verbose=0, validation_split=0.1)
    
    # Evaluate
    loss = model.evaluate(X_test, y_test, verbose=0)
    print(f"  Evaluation Complete. Mean Absolute Error on test set: {loss:.4f} kg")
    
    # Export to TFLite
    os.makedirs('models', exist_ok=True)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    
    with open('models/ingredient_model.tflite', 'wb') as f:
        f.write(tflite_model)
    print("  Saved TFLite Model to models/ingredient_model.tflite\n")

def train_demand_model():
    print("--- 2. Training Dish Demand Forecasting Neural Network & Exporting to TFLite ---")
    np.random.seed(42)
    tf.random.set_seed(42)
    dishes = [
        "Hyderabadi Chicken Biryani", "Paneer Butter Masala", "Butter Chicken", 
        "Masala Dosa", "Dal Makhani", "Chole Bhature", 
        "Tandoori Chicken", "Gulab Jamun"
    ]
    base_demands = {
        "Hyderabadi Chicken Biryani": 50, "Paneer Butter Masala": 35, "Butter Chicken": 40,
        "Masala Dosa": 45, "Dal Makhani": 25, "Chole Bhature": 30,
        "Tandoori Chicken": 30, "Gulab Jamun": 20
    }
    
    dates = pd.date_range(end='2026-07-15', periods=180)
    order_records = []
    
    for dish in dishes:
        base = base_demands[dish]
        for date in dates:
            day_effect = 1.0
            if date.dayofweek in [4, 5, 6]:
                day_effect = 1.35 if date.dayofweek == 5 else 1.2
            
            month_effect = 1.0
            if date.month in [12, 1, 6, 7]:
                month_effect = 1.1
                
            expected = base * day_effect * month_effect
            noise = np.random.normal(1.0, 0.05)
            qty = int(np.maximum(0, expected * noise))
            
            order_records.append({
                'date': date,
                'dish': dish,
                'orders_count': qty
            })
            
    orders_df = pd.DataFrame(order_records).sort_values(by=['dish', 'date'])
    orders_df['lag_1'] = orders_df.groupby('dish')['orders_count'].shift(1)
    orders_df['lag_7'] = orders_df.groupby('dish')['orders_count'].shift(7)
    orders_df['rolling_mean_7'] = orders_df.groupby('dish')['orders_count'].shift(1).rolling(7).mean()
    orders_df['day_of_week'] = orders_df['date'].dt.dayofweek
    orders_df['month'] = orders_df['date'].dt.month
    orders_df = orders_df.dropna()
    
    # Save dataset to CSV for user viewing
    orders_df.to_csv('synthetic_orders.csv', index=False)
    print("  Saved daily orders dataset to synthetic_orders.csv")
    
    # Label encode and save labels as a JSON mapping
    encoder = LabelEncoder()
    orders_df['dish_encoded'] = encoder.fit_transform(orders_df['dish'])
    
    # Save mapping to JSON
    mapping = {dish: int(idx) for idx, dish in enumerate(encoder.classes_)}
    with open('models/dish_labels.json', 'w') as f:
        json.dump(mapping, f, indent=4)
    print("  Saved label mapping to models/dish_labels.json")
    
    features = ['dish_encoded', 'day_of_week', 'month', 'lag_1', 'lag_7', 'rolling_mean_7']
    X = orders_df[features].astype(np.float32)
    y = orders_df['orders_count'].astype(np.float32)
    
    split_date = pd.to_datetime('2026-06-30')
    train_mask = orders_df['date'] <= split_date
    test_mask = orders_df['date'] > split_date
    
    X_train, X_test = X[train_mask], X[test_mask]
    y_train, y_test = y[train_mask], y[test_mask]
    
    # Build Keras Regression Model
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(6,)),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(1) # expected orders count
    ])
    
    model.compile(optimizer='adam', loss='mae')
    
    print("  Training neural network...")
    model.fit(X_train, y_train, epochs=80, batch_size=32, verbose=0, validation_split=0.1)
    
    loss = model.evaluate(X_test, y_test, verbose=0)
    print(f"  Evaluation Complete. Mean Absolute Error on test set: {loss:.4f} Orders")
    
    # Export to TFLite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    
    with open('models/demand_model.tflite', 'wb') as f:
        f.write(tflite_model)
    print("  Saved TFLite Model to models/demand_model.tflite\n")

if __name__ == '__main__':
    train_ingredient_model()
    train_demand_model()
    
    # Clean up old pickle files to keep directory clean
    for filename in ['ingredient_model.pkl', 'demand_model.pkl', 'dish_encoder.pkl']:
        filepath = os.path.join('models', filename)
        if os.path.exists(filepath):
            os.remove(filepath)
            print(f"Removed old pickle file: {filepath}")
    
    print("\nModel training and TFLite conversion complete!")
