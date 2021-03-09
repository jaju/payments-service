package com.tsys.payments.domain;

import java.util.Objects;

public class Item {
  public final Long id;
  public final String name;
  public final Money price;
  public final Integer quantity;

  public Item(Long id, String name, Money price, Integer quantity) {
    this.id = id;
    this.name = name;
    this.price = price;
    this.quantity = quantity;
  }

  public Money totalPrice() {
    return price.multiply(quantity.doubleValue());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Item item = (Item) o;
    return Objects.equals(id, item.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Item{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", price=" + price +
            ", quantity=" + quantity +
            '}';
  }
}
