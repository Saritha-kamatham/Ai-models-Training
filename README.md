# Food Delivery Application

A complete, production-grade, enterprise-ready Full Stack Food Delivery Web Application (similar to Swiggy/Zomato) built with **Java 21, Spring Boot 3.x, Hibernate, Spring Security, JWT, MySQL 8.x**, and a **Modern responsive vanilla HTML5/CSS3/ES6 JavaScript frontend** featuring glassmorphism elements, CSS variables, and dynamic micro-animations.


## AI Analytics & TensorFlow Lite Integration

This project includes a complete AI-powered Chef Analytics system using pre-trained **TensorFlow Lite (`.tflite`)** neural networks.

### AI File Directory Map
All AI-related files are located in the project root folder:
* **`models/`**: The folder containing compiled model files:
  * `models/ingredient_model.tflite`: Model for predicting ingredient quantities (Rice, Tomato, Onion, Chicken, Paneer) from active subscriptions.
  * `models/demand_model.tflite`: Model for predicting expected daily orders per dish.
  * `models/dish_labels.json`: The label translation mapping for Indian dishes.
* `train.py`: Python script that generates datasets, compiles/trains Keras neural networks, and exports them to TFLite format.
* `predict.py`: The main backend runner that runs TFLite inference using the `tf.lite.Interpreter` API.
* `test_ingredients.py`: A lightweight helper script to test ingredient purchase estimations.
* `test_demand.py`: A lightweight helper script to estimate and rank daily dish orders.
* `ml_training.ipynb`: The interactive Jupyter Notebook version of the model building and training process.
* `synthetic_subscriptions.csv` & `synthetic_orders.csv`: The training datasets generated during training.

### How to Run and Test the AI Code
1. Install the required libraries in your terminal:
   ```bash
   pip install tensorflow numpy pandas
2. Compile and serialize the models:
   ```bash
   python train.py
<img width="476" height="208" alt="image" src="https://github.com/user-attachments/assets/e95c6fb0-109d-473b-b6f3-0a7e8cc09335" />

