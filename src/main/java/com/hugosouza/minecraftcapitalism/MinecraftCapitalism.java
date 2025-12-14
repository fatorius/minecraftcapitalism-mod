package com.hugosouza.minecraftcapitalism;

import com.hugosouza.minecraftcapitalism.database.DatabaseService;
import com.hugosouza.minecraftcapitalism.database.DbExecutor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import java.sql.SQLException;
import java.util.UUID;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(MinecraftCapitalism.MODID)
public class MinecraftCapitalism {
    public static final String MODID = "minecraftcapitalism";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MinecraftCapitalism(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) throws SQLException {
        LOGGER.info("Iniciando banco de dados do capitalismo");
        DatabaseService.init(event.getServer().getWorldPath(LevelResource.ROOT));
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();

        DbExecutor.runAsync(() -> {
            LOGGER.info("Iniciando conta bancária para player " + uuid);
            DatabaseService.ensureAccountExists(uuid);
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registrando comandos do capitalismo");
        CommandRegister.register(event.getDispatcher());
    }
}
