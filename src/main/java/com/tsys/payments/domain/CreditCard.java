package com.tsys.payments.domain;

import java.util.Date;

public class CreditCard {
    public final String number;
    public final String holderName;
    public final String issuingBank;
    public final Date validUntil;
    public final Integer cvv;

    public CreditCard(String number, String holderName, String issuingBank, Date validUntil, Integer cvv) {
        this.number = number;
        this.holderName = holderName;
        this.issuingBank = issuingBank;
        this.validUntil = validUntil;
        this.cvv = cvv;
    }

    @Override
    public String toString() {
        return "CreditCard{" +
                "number='" + number + '\'' +
                ", holderName='" + holderName + '\'' +
                ", issuingBank='" + issuingBank + '\'' +
                ", validUntil=" + validUntil +
                ", cvv=" + cvv +
                '}';
    }
}
