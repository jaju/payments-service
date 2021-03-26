package com.tsys.payments.utils;

import java.util.UUID;

public class UUIDGenerator implements IdGenerator<UUID> {
    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
