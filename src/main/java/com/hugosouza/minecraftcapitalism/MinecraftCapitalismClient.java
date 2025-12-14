package com.hugosouza.minecraftcapitalism;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = MinecraftCapitalism.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = MinecraftCapitalism.MODID, value = Dist.CLIENT)
public class MinecraftCapitalismClient {
    public MinecraftCapitalismClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
    }
}
