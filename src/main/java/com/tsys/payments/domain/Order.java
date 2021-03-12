package com.tsys.payments.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Order {
    public final String id;
    private final List<Item> items;
    public final Money amount;

    public Order(String id, List<Item> items) {
        this.id = id;
        this.items = items;
        amount = items.stream()
                .map(Item::totalPrice)
                .reduce(Money::add)
                .orElse(Money.ZERO);
    }

    public Integer totalItems() {
        return items.size();
    }


    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", items=" + items +
                '}';
    }
}
