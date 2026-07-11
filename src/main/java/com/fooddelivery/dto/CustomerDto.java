package com.fooddelivery.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String role;
}
