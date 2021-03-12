package com.tsys.payments.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Tag("UnitTest")
public class OrderSpecs {
    private final Order order = new Order("TEST-ORDER-ID",
            List.of(
                    new Item(1L, "Dant Kanti Toothpaste", new Money(Currency.getInstance("INR"), 123.545), 10),
                    new Item(2L, "Toothbrush", new Money(Currency.getInstance("INR"), 100.00), 2)
            )
    );

    @Test
    public void calculatesTotalAmount() {
        // When-Then
        assertThat(order.amount, is(new Money(Currency.getInstance("INR"), 1435.45)));
    }

    @Test
    public void calculatesTotalItems() {
        // When-Then
        assertThat(order.totalItems(), is(2));
    }

    @Test
    public void emptyOrderHasZeroAmount() {
        // Given
        Order empty = new Order("EMPTY-ORDER-ID", List.of());
        // When-Then
        assertThat(empty.amount, is(Money.ZERO));
    }

    @Test
    public void emptyOrderHasNoItems() {
        // Given
        Order empty = new Order("EMPTY-ORDER-ID", List.of());
        // When-Then
        assertThat(empty.totalItems(), is(0));
    }
}
