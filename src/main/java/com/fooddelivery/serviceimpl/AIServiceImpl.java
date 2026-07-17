package com.fooddelivery.serviceimpl;

import com.fooddelivery.dto.ChefRecommendationDto;
import com.fooddelivery.dto.DemandForecastDto;
import com.fooddelivery.dto.TopDishDto;
import com.fooddelivery.entity.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private static final String PY_SERVICE_URL = "http://127.0.0.1:5000";

    @Override
    public List<TopDishDto> getTopDishes() {
        // Query database to aggregate order items by food name
        List<Order> orders = orderRepository.findAll();
        Map<String, Long> dishOrderCounts = new HashMap<>();

        for (Order order : orders) {
            if ("COMPLETED".equalsIgnoreCase(order.getPaymentStatus())) {
                for (OrderItem item : order.getOrderItems()) {
                    String dishName = item.getFood().getName();
                    dishOrderCounts.put(dishName, dishOrderCounts.getOrDefault(dishName, 0L) + item.getQuantity());
                }
            }
        }

        // Add dummy counts for menu items with no orders to ensure all seeded dishes are shown
        List<Food> foods = foodRepository.findAll();
        for (Food food : foods) {
            dishOrderCounts.putIfAbsent(food.getName(), 0L);
        }

        // Map and sort descending
        return dishOrderCounts.entrySet().stream()
                .map(entry -> new TopDishDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(TopDishDto::getOrders).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<DemandForecastDto> getDemandForecast() {
        List<Food> foods = foodRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        int dayOfWeek = now.getDayOfWeek().getValue() % 7; // 0=Monday to 6=Sunday (or similar mapping)
        int month = now.getMonthValue();

        // Prepare request body for python service
        StringBuilder jsonBuilder = new StringBuilder("{\"dishes\":[");
        for (int i = 0; i < foods.size(); i++) {
            Food food = foods.get(i);
            double[] stats = getHistoricalStatsForFood(food);
            jsonBuilder.append(String.format(
                "{\"dish\":\"%s\",\"day_of_week\":%d,\"month\":%d,\"lag_1\":%.1f,\"lag_7\":%.1f,\"rolling_mean_7\":%.1f}",
                food.getName(), dayOfWeek, month, stats[0], stats[1], stats[2]
            ));
            if (i < foods.size() - 1) {
                jsonBuilder.append(",");
            }
        }
        jsonBuilder.append("]}");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PY_SERVICE_URL + "/predict/demand"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBuilder.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // Parse simple JSON array response e.g. [{"dish":"Margherita Pizza", "expectedOrders":24}, ...]
                return parseDemandForecastJson(response.body());
            }
        } catch (Exception e) {
            System.out.println("FastAPI AI Service offline. Using Java-based fallback estimator for demand forecasting.");
        }

        // Fallback Logic: Heuristic Prediction based on historical average and weekend effects
        List<DemandForecastDto> forecastList = new ArrayList<>();
        Random rand = new Random(42); // Seeded random for consistency
        for (Food food : foods) {
            double[] stats = getHistoricalStatsForFood(food);
            double base = stats[2] > 0 ? stats[2] : getSeededBaseDemand(food.getName());
            
            // Weekend effect multiplier
            double dayMultiplier = (dayOfWeek >= 4) ? 1.25 : 0.95;
            int predicted = (int) Math.round(base * dayMultiplier * (0.95 + rand.nextDouble() * 0.1));
            forecastList.add(new DemandForecastDto(food.getName(), Math.max(1, predicted)));
        }
        return forecastList;
    }

    @Override
    public List<ChefRecommendationDto> getChefRecommendations() {
        List<DemandForecastDto> forecasts = getDemandForecast();
        List<ChefRecommendationDto> recommendations = new ArrayList<>();

        for (DemandForecastDto forecast : forecasts) {
            Food food = foodRepository.findAll().stream()
                    .filter(f -> f.getName().equalsIgnoreCase(forecast.getDish()))
                    .findFirst().orElse(null);

            double[] stats = getHistoricalStatsForFood(food);
            double recentAvg = stats[2] > 0 ? stats[2] : getSeededBaseDemand(forecast.getDish());

            String priority;
            String action;

            if (forecast.getExpectedOrders() > recentAvg * 1.15) {
                priority = "HIGH";
                action = "✔ Prepare more " + forecast.getDish() + " (Demand expected to rise)";
            } else if (forecast.getExpectedOrders() < recentAvg * 0.85) {
                priority = "LOW";
                action = "✘ Reduce preparation for " + forecast.getDish() + " (Low expected demand)";
            } else {
                priority = "MEDIUM";
                action = "● Maintain normal stock for " + forecast.getDish();
            }

            recommendations.add(new ChefRecommendationDto(forecast.getDish(), priority, action));
        }

        // Sort recommendations so High priority comes first
        recommendations.sort((r1, r2) -> {
            int p1 = r1.getPriority().equals("HIGH") ? 3 : r1.getPriority().equals("MEDIUM") ? 2 : 1;
            int p2 = r2.getPriority().equals("HIGH") ? 3 : r2.getPriority().equals("MEDIUM") ? 2 : 1;
            return Integer.compare(p2, p1);
        });

        return recommendations;
    }

    @Override
    public Map<String, String> getIngredientEstimation() {
        List<Subscription> activeSubs = subscriptionRepository.findByStatus("ACTIVE");

        // Prepare request body for python service
        StringBuilder jsonBuilder = new StringBuilder("{\"subscriptions\":[");
        for (int i = 0; i < activeSubs.size(); i++) {
            Subscription sub = activeSubs.get(i);
            jsonBuilder.append(String.format(
                "{\"meal_type\":\"%s\",\"duration_days\":%d,\"num_subscribers\":1}",
                sub.getMealPlan().getType(), sub.getDurationDays()
            ));
            if (i < activeSubs.size() - 1) {
                jsonBuilder.append(",");
            }
        }
        jsonBuilder.append("]}");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PY_SERVICE_URL + "/predict/ingredients"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBuilder.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseIngredientEstimationJson(response.body());
            }
        } catch (Exception e) {
            System.out.println("FastAPI AI Service offline. Using Java-based fallback estimator for ingredient estimation.");
        }

        // Fallback Logic: Calculate exact ingredient recipe scaling + 5% standard kitchen wastage allowance
        double rice = 0, tomato = 0, onion = 0, chicken = 0, paneer = 0;

        for (Subscription sub : activeSubs) {
            String type = sub.getMealPlan().getType();
            int days = sub.getDurationDays();

            if ("Veg Meal".equalsIgnoreCase(type)) {
                rice += 0.15 * days;
                tomato += 0.10 * days;
                onion += 0.08 * days;
                paneer += 0.12 * days;
            } else if ("Protein Meal".equalsIgnoreCase(type)) {
                rice += 0.10 * days;
                tomato += 0.08 * days;
                onion += 0.06 * days;
                chicken += 0.20 * days;
            } else if ("Family Meal".equalsIgnoreCase(type)) {
                rice += 0.50 * days;
                tomato += 0.30 * days;
                onion += 0.25 * days;
                chicken += 0.40 * days;
                paneer += 0.20 * days;
            }
        }

        // Apply a 5% buffer for wastage
        rice *= 1.05;
        tomato *= 1.05;
        onion *= 1.05;
        chicken *= 1.05;
        paneer *= 1.05;

        Map<String, String> results = new LinkedHashMap<>();
        results.put("Rice", String.format("%.1f kg", rice));
        results.put("Tomato", String.format("%.1f kg", tomato));
        results.put("Onion", String.format("%.1f kg", onion));
        results.put("Chicken", String.format("%.1f kg", chicken));
        results.put("Paneer", String.format("%.1f kg", paneer));

        return results;
    }

    // Helper to fetch historical details from DB for a food item
    private double[] getHistoricalStatsForFood(Food food) {
        if (food == null) {
            return new double[]{0.0, 0.0, 0.0};
        }

        List<Order> orders = orderRepository.findAll();
        Map<Long, Double> dailyCounts = new TreeMap<>(); // Sorted by day difference
        LocalDateTime now = LocalDateTime.now();

        for (Order order : orders) {
            if ("COMPLETED".equalsIgnoreCase(order.getPaymentStatus())) {
                long daysAgo = ChronoUnit.DAYS.between(order.getOrderedAt(), now);
                if (daysAgo >= 0 && daysAgo <= 14) {
                    for (OrderItem item : order.getOrderItems()) {
                        if (item.getFood().getId().equals(food.getId())) {
                            dailyCounts.put(daysAgo, dailyCounts.getOrDefault(daysAgo, 0.0) + item.getQuantity());
                        }
                    }
                }
            }
        }

        double lag_1 = dailyCounts.getOrDefault(1L, 0.0);
        double lag_7 = dailyCounts.getOrDefault(7L, 0.0);

        double rollingSum = 0;
        int rollingCount = 0;
        for (long i = 1; i <= 7; i++) {
            if (dailyCounts.containsKey(i)) {
                rollingSum += dailyCounts.get(i);
            }
            rollingCount++;
        }
        double rollingMean = rollingCount > 0 ? (rollingSum / rollingCount) : 0.0;

        return new double[]{lag_1, lag_7, rollingMean};
    }

    private double getSeededBaseDemand(String dishName) {
        switch (dishName) {
            case "Margherita Pizza": return 25.0;
            case "Double Pepperoni Pizza": return 15.0;
            case "Classic Cheese Burger": return 30.0;
            case "Truffle French Fries": return 20.0;
            case "Hyderabadi Chicken Biryani": return 40.0;
            case "Spicy Chicken Tikka": return 18.0;
            case "Chocolate Fudge Waffle": return 12.0;
            case "Veg Hakka Noodles": return 15.0;
            default: return 10.0;
        }
    }

    // Manual JSON Parsing to keep it free from third-party JSON library incompatibilities
    private List<DemandForecastDto> parseDemandForecastJson(String body) {
        List<DemandForecastDto> forecasts = new ArrayList<>();
        try {
            // FastAPI returns list of objects: [{"dish":"Margherita Pizza", "expectedOrders":24}, ...]
            body = body.trim();
            if (body.startsWith("[")) body = body.substring(1);
            if (body.endsWith("]")) body = body.substring(0, body.length() - 1);

            String[] items = body.split("\\},\\{");
            for (String item : items) {
                item = item.replace("{", "").replace("}", "");
                String[] pairs = item.split(",");
                String dish = "";
                int expectedOrders = 0;
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    String key = kv[0].replace("\"", "").trim();
                    String val = kv[1].replace("\"", "").trim();
                    if (key.equalsIgnoreCase("dish")) {
                        dish = val;
                    } else if (key.equalsIgnoreCase("expectedOrders") || key.equalsIgnoreCase("expected_orders")) {
                        expectedOrders = (int) Double.parseDouble(val);
                    }
                }
                if (!dish.isEmpty()) {
                    forecasts.add(new DemandForecastDto(dish, expectedOrders));
                }
            }
        } catch (Exception e) {
            System.err.println("JSON Parsing failed: " + e.getMessage());
        }
        return forecasts;
    }

    private Map<String, String> parseIngredientEstimationJson(String body) {
        Map<String, String> ingredients = new LinkedHashMap<>();
        try {
            // FastAPI returns: {"Rice": "85.2 kg", "Tomato": "40.1 kg", ...}
            body = body.trim().replace("{", "").replace("}", "");
            String[] pairs = body.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();
                ingredients.put(key, val);
            }
        } catch (Exception e) {
            System.err.println("JSON Parsing failed: " + e.getMessage());
        }
        return ingredients;
    }
}
