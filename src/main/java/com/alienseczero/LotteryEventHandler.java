package com.alienseczero;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.AreaEffectCloud;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.*;

public class LotteryEventHandler {
    private final LuckyVault mod;
    // Remove our local tick counter and instead use LuckyVault's nextDrawTime.
    private static int advertisementTickCounter = 0;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Integer> playerTicketCounts = new HashMap<>();
    private static final Map<UUID, Long> activeEffects = new HashMap<>();
    private static final List<AreaEffectCloud> activeClouds = new ArrayList<>();

    public LotteryEventHandler(LuckyVault mod) {
        this.mod = mod;
        LOGGER.info("LotteryEventHandler created.");
        playerTicketCounts.clear();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server starting: resetting ticket counts.");
        mod.getLotteryPot();
        playerTicketCounts.clear();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID playerId = player.getUUID();
        int winnings = mod.getUnclaimedWinnings(playerId);
        if (winnings > 0) {
            player.getInventory().add(new ItemStack(Items.DIAMOND, winnings));
            player.sendSystemMessage(Component.literal("🎉 Welcome back! You have been automatically paid out " + winnings + " diamonds from the lottery!"));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            addWinnerEffect(playerId);
            new FireworksDisplay(player, 120);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        advertisementTickCounter++;

        long currentTime = System.currentTimeMillis();
        // Auto-draw the lottery when the current time exceeds the persistent nextDrawTime.
        if (currentTime >= LuckyVault.getInstance().getNextDrawTime()) {
            if (LuckyVault.getInstance().getTicketCount() > 0) {
                // Tickets were sold, proceed with the draw
                LuckyVault.getInstance().drawLottery(server);
                LOGGER.info("Lottery draw triggered automatically.");
            } else {
                // No tickets sold, log once and reset draw time
                LOGGER.info("Lottery Mod: No tickets sold. Skipping draw.");

                // Reset the next draw time to avoid continuous triggering
                LuckyVault.getInstance().setNextDrawTime(currentTime + LuckyVault.DRAW_INTERVAL);
            }
        }

        // Advertisement message broadcast.
        if (advertisementTickCounter >= LotteryConfig.ADVERTISEMENT_INTERVAL_TICKS) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(LotteryConfig.ADVERTISEMENT_MESSAGE), false);
            advertisementTickCounter = 0;
            LOGGER.info("Advertisement broadcast.");
        }

        // Process active effects (particle spawning for winners).
        for (UUID playerId : new ArrayList<>(activeEffects.keySet())) {
            if (currentTime >= activeEffects.get(playerId)) {
                activeEffects.remove(playerId);
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        LotteryConfig.getPlayerParticle(),
                        player.getX(), player.getY() + 1, player.getZ(),
                        5, 0.5, 0.5, 0.5, 0.1
                );
            }
        }
    }

    // Computes the remaining time (in seconds) until the next lottery draw using the persistent nextDrawTime.
    public static int getRemainingTimeSeconds() {
        long remainingMs = LuckyVault.getInstance().getNextDrawTime() - System.currentTimeMillis();
        return (int) Math.max(remainingMs / 1000, 0);
    }

    public static void addWinnerEffect(UUID playerId) {
        activeEffects.put(playerId, System.currentTimeMillis() + LotteryConfig.WINNER_EFFECT_DURATION_MS);
    }

    public static int getPlayerTicketCount(UUID playerId) {
        return playerTicketCounts.getOrDefault(playerId, 0);
    }

    public static void incrementPlayerTicketCount(UUID playerId) {
        int current = playerTicketCounts.getOrDefault(playerId, 0);
        playerTicketCounts.put(playerId, current + 1);
    }

    public static void registerCloud(AreaEffectCloud cloud) {
        activeClouds.add(cloud);
    }

    public static void clearActiveEffects(MinecraftServer server) {
        for (AreaEffectCloud cloud : new ArrayList<>(activeClouds)) {
            if (cloud != null && !cloud.isRemoved()) {
                cloud.discard();
            }
        }
        activeClouds.clear();
        activeEffects.clear();
        LOGGER.info("Cleared all mod particle effects.");
    }
}