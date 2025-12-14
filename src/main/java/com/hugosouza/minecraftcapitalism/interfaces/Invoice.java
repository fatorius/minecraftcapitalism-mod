package com.hugosouza.minecraftcapitalism.interfaces;

import java.util.UUID;

public record Invoice(
        int id,
        UUID fromUuid,
        UUID toUuid,
        int amount,
        String status,
        long createdAt) {
}
