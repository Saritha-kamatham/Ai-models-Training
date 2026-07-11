package com.fooddelivery.serviceimpl;

import com.fooddelivery.dto.AuthResponse;
import com.fooddelivery.dto.CustomerDto;
import com.fooddelivery.dto.LoginRequest;
import com.fooddelivery.dto.RegisterRequest;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.UserService;
import com.fooddelivery.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public CustomerDto saveCustomer(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered!");
        }

        String role = request.getRole();
        if (role == null || role.isBlank()) {
            role = "CUSTOMER";
        } else {
            role = role.toUpperCase();
            if (!role.equals("CUSTOMER") && !role.equals("OWNER") && !role.equals("ADMIN")) {
                throw new BadRequestException("Invalid role! Role must be CUSTOMER, OWNER, or ADMIN");
            }
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        return mapToDto(savedUser);
    }

    @Override
    public List<CustomerDto> getCustomers() {
        // Return users with role CUSTOMER
        return userRepository.findByRole("CUSTOMER").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerDto getCustomerById(Long id) {
        User user = findUserById(id);
        return mapToDto(user);
    }

    @Override
    public CustomerDto updateCustomer(Long id, CustomerDto customerDto) {
        User user = findUserById(id);
        
        user.setFullName(customerDto.getFullName());
        user.setPhone(customerDto.getPhone());
        user.setAddress(customerDto.getAddress());
        user.setCity(customerDto.getCity());
        
        User updatedUser = userRepository.save(user);
        return mapToDto(updatedUser);
    }

    @Override
    public void deleteCustomer(Long id) {
        User user = findUserById(id);
        userRepository.delete(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password!");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(user.getFullName())
                .userId(user.getId())
                .build();
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private CustomerDto mapToDto(User user) {
        return CustomerDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .role(user.getRole())
                .build();
    }
}
