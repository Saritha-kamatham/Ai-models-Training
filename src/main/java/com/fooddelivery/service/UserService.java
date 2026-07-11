package com.fooddelivery.service;

import com.fooddelivery.dto.AuthResponse;
import com.fooddelivery.dto.CustomerDto;
import com.fooddelivery.dto.LoginRequest;
import com.fooddelivery.dto.RegisterRequest;
import com.fooddelivery.entity.User;

import java.util.List;

public interface UserService {
    CustomerDto saveCustomer(RegisterRequest request);
    List<CustomerDto> getCustomers();
    CustomerDto getCustomerById(Long id);
    CustomerDto updateCustomer(Long id, CustomerDto customerDto);
    void deleteCustomer(Long id);
    AuthResponse login(LoginRequest request);
    
    // Helper methods for internal service communications
    User findUserByEmail(String email);
    User findUserById(Long id);
}
