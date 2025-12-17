package com.hugosouza.minecraftcapitalism.interfaces;

import java.util.UUID;

public record MarketListing(
        int id,
        UUID owner,
        String itemId,
        int quantity,
        int unitPrice,
        long createdAt) {
}
