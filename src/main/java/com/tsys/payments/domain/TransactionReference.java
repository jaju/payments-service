package com.tsys.payments.domain;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class TransactionReference {
    public static final TransactionReference EMPTY = new TransactionReference(null, null, "");

    public final UUID id;
    public final Date date;
    public final String status;

    public TransactionReference(UUID id, Date date, String status) {
        this.id = id;
        this.date = date;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionReference that = (TransactionReference) o;
        return id.equals(that.id) &&
                date.equals(that.date) &&
                status.equals(that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, status);
    }

    @Override
    public String toString() {
        return "TransactionReference{" +
                "date=" + date +
                ", id='" + id + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
