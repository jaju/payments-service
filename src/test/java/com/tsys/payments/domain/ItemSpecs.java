package com.tsys.payments.domain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Currency;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Tag("UnitTest")
class ItemSpecs {

    private final Currency inr = Currency.getInstance("INR");
    private final double itemPrice = 20.50;
    private final Money inr20_50 = new Money(inr, itemPrice);

    @Test
    public void calculatesPrice() {
        // Given
        final var quantity = 10;
        final var testItem = new Item(1L, "testItem", inr20_50, quantity);
        // When-Then
        final Money expected = new Money(inr, itemPrice * quantity);
        assertThat(testItem.totalPrice(), is(expected));
    }

    @Test
    public void equalityUsesIDForComparison() {
        // Given
        final var inr30_50 = new Money(inr, 30.50);
        final var testItem_1a = new Item(1L, "testItem", inr20_50, 10);
        final var testItem_1b = new Item(1L, "testItem", inr30_50, 20);
        final var testItem2 = new Item(2L, "testItem", inr20_50, 30);
        // When-Then
        assertThat(testItem_1a.equals(testItem_1b), is(true));
        assertThat(testItem_1a.equals(testItem2), is(false));
        assertThat(testItem_1b.equals(testItem2), is(false));
    }
}