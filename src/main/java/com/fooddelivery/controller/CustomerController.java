package com.fooddelivery.controller;

import com.fooddelivery.dto.CustomerDto;
import com.fooddelivery.dto.RegisterRequest;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@CrossOrigin
public class CustomerController {

    private final UserService userService;

    private User getLoggedInUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new UnauthorizedException("User not authenticated");
    }

    @PostMapping("/add")
    public ResponseEntity<CustomerDto> addCustomer(@Valid @RequestBody RegisterRequest request) {
        CustomerDto customer = userService.saveCustomer(request);
        return new ResponseEntity<>(customer, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        // Admin authorization check
        User user = getLoggedInUser();
        if (!user.getRole().equals("ADMIN")) {
            throw new UnauthorizedException("Only Admin can access this resource!");
        }
        return ResponseEntity.ok(userService.getCustomers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long id) {
        User user = getLoggedInUser();
        // Allow request if user is Admin or the customer themselves
        if (!user.getRole().equals("ADMIN") && !user.getId().equals(id)) {
            throw new UnauthorizedException("You are not authorized to view this customer's profile!");
        }
        return ResponseEntity.ok(userService.getCustomerById(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<CustomerDto> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerDto customerDto) {
        User user = getLoggedInUser();
        // Allow request if user is Admin or the customer themselves
        if (!user.getRole().equals("ADMIN") && !user.getId().equals(id)) {
            throw new UnauthorizedException("You are not authorized to update this customer's profile!");
        }
        return ResponseEntity.ok(userService.updateCustomer(id, customerDto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteCustomer(@PathVariable Long id) {
        User user = getLoggedInUser();
        if (!user.getRole().equals("ADMIN")) {
            throw new UnauthorizedException("Only Admin can delete customer profiles!");
        }
        userService.deleteCustomer(id);
        return ResponseEntity.ok("Customer profile deleted successfully.");
    }
}
