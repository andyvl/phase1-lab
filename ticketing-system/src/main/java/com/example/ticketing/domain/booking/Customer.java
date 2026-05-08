package com.example.ticketing.domain.booking;

import java.util.Objects;

public record Customer(String name, String email) {
    public Customer {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(email, "email is required");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Customer name cannot be blank");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }
}
