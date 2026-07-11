package com.fooddelivery.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable()) // Disable Spring CORS, custom WebConfig handles CORS if needed
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow public static resources (frontend files)
                .requestMatchers("/", "/index.html", "/login.html", "/register.html", 
                               "/restaurants.html", "/restaurant-details.html", "/menu.html", 
                               "/cart.html", "/checkout.html", "/orders.html", 
                               "/dashboard.html", "/404.html").permitAll()
                .requestMatchers("/css/**", "/js/**", "/uploads/**", "/favicon.ico").permitAll()
                
                // Allow public Auth endpoints
                .requestMatchers("/auth/**").permitAll()
                
                // Public GET endpoints for browsing menus and restaurants
                .requestMatchers(HttpMethod.GET, "/restaurants/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/foods/**").permitAll()
                
                // Public registration API for customers
                .requestMatchers(HttpMethod.POST, "/customers/add").permitAll()
                
                // Require authentication for all other requests
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
