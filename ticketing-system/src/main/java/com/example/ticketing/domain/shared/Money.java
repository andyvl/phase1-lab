package com.example.ticketing.domain.shared;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {
    public Money {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");
        if (currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be blank");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative");
        }
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "other is required");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
