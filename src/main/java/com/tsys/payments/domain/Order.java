package com.tsys.payments.domain;

import java.util.List;

public class Order {
    public final String id;
    public final List<Item> items;
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
