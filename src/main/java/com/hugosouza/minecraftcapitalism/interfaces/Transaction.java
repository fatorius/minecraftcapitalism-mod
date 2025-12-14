package com.hugosouza.minecraftcapitalism.interfaces;

public record Transaction(
        String from,
        String to,
        int amount,
        String type,
        long timestamp
) {}
