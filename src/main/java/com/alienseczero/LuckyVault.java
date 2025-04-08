package com.alienseczero;

import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import java.security.SecureRandom;
import java.util.*;

@Mod(LuckyVault.MODID)
public class LuckyVault {
    public static final String MODID = "luckyvault";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static LuckyVault INSTANCE;

    // Map of player UUIDs to ticket counts.
    private final Map<UUID, Integer> ticketEntries = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private int lotteryPot = 0;
    private Map<UUID, Integer> unclaimedWinnings = new HashMap<>();
    // Next draw time in milliseconds.
    private long nextDrawTime;

    // Leaderboard for wins.
    private final Map<UUID, Integer> leaderboard = new HashMap<>();

    // Last winner info.
    private String lastWinner = "N/A";
    private int lastWinningAmount = 0;

    // New: Constant for the draw interval (in milliseconds).
    public static final long DRAW_INTERVAL = LotteryConfig.announcementIntervalSeconds * 1000L;

    public LuckyVault(IEventBus modBus) {
        INSTANCE = this;
        LOGGER.info("Lottery Mod Initialized!");

        LotteryConfig.load();

        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(new LotteryEventHandler(this));
        NeoForge.EVENT_BUS.register(new CommandRegistrar());

        // Load lottery state from JSON file.
        LotteryStateManager.LotteryState state = LotteryStateManager.loadLotteryState();
        this.lotteryPot = state.lotteryPot;
        for (Map.Entry<String, Integer> entry : state.entries.entrySet()) {
            try {
                UUID uuid = UUID.fromString(entry.getKey());
                ticketEntries.put(uuid, entry.getValue());
            } catch (IllegalArgumentException e) {
                LOGGER.error("Invalid UUID in saved lottery state: " + entry.getKey(), e);
            }
        }
        this.nextDrawTime = state.nextDrawTime;
        LOGGER.info("Next draw time loaded: " + nextDrawTime);

        unclaimedWinnings = LotteryStateManager.loadUnclaimedWinnings();
        leaderboard.putAll(LeaderboardManager.loadLeaderboard());
    }

    public static LuckyVault getInstance() {
        return INSTANCE;
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Lottery Mod: Common setup complete.");
    }

    public void drawLottery(MinecraftServer server) {
        // If no tickets sold, log once and reset next draw time.
        if (ticketEntries.isEmpty()) {
            LOGGER.info("Lottery Mod: No tickets sold. Skipping draw.");
            nextDrawTime = System.currentTimeMillis() + DRAW_INTERVAL;
            saveLotteryState();
            return;
        }

        // Build a list of tickets (each ticket is the owner's UUID)
        List<UUID> tickets = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : ticketEntries.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                tickets.add(entry.getKey());
            }
        }

        // Pick a random ticket
        UUID winnerId = tickets.get(random.nextInt(tickets.size()));
        ServerPlayer winner = server.getPlayerList().getPlayer(winnerId);
        int winnings = lotteryPot + LotteryConfig.BONUS_POT;

        if (winner != null) {
            lastWinner = winner.getName().getString();
            lastWinningAmount = winnings;
            leaderboard.put(winner.getUUID(), leaderboard.getOrDefault(winner.getUUID(), 0) + 1);
            LeaderboardManager.saveLeaderboard(leaderboard);

            // Broadcast a congratulatory message
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("🎉 " + ChatFormatting.GOLD + "Congratulations to " + lastWinner +
                            ChatFormatting.RESET + " for winning the lottery! Total winnings: " +
                            ChatFormatting.AQUA + winnings + " diamonds" + ChatFormatting.RESET + "!"),
                    false
            );

            // Create title and subtitle components
            Component title = Component.literal("Congratulations!");
            Component subtitle = Component.literal("You have won the Lottery!");

            // Create title packets (adjust packet class names as needed)
            ClientboundSetTitleTextPacket titlePacket = new ClientboundSetTitleTextPacket(title);
            ClientboundSetSubtitleTextPacket subtitlePacket = new ClientboundSetSubtitleTextPacket(subtitle);
            ClientboundSetTitlesAnimationPacket timingPacket = new ClientboundSetTitlesAnimationPacket(10, 70, 20);

            // Send the title packets to the winner
            winner.connection.send(titlePacket);
            winner.connection.send(subtitlePacket);
            winner.connection.send(timingPacket);

            // Award the diamonds to the winner
           // winner.getInventory().add(new ItemStack(Items.DIAMOND, winnings));
            winner.getInventory().add(new ItemStack(LotteryConfig.getCurrencyItem(), winnings));

            LOGGER.info("Lottery Mod: {} won with {} diamonds!", lastWinner, winnings);

            // Create a ground effect cloud
            AreaEffectCloud effectCloud = new AreaEffectCloud(winner.level(), winner.getX(), winner.getY(), winner.getZ());
            effectCloud.setParticle(LotteryConfig.getGroundParticle());
            effectCloud.setDuration(40); // lasts 2 seconds
            effectCloud.setRadius(1.5F);
            effectCloud.setWaitTime(0);
            winner.level().addFreshEntity(effectCloud);
            LotteryEventHandler.registerCloud(effectCloud);

            // Play a level-up sound effect
            winner.level().playSound(null, winner.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            LotteryEventHandler.addWinnerEffect(winner.getUUID());

            // Trigger the grand fireworks display for about 10 seconds (200 ticks)
            new FireworksDisplay(winner, 120);
        } else {
            LOGGER.info("Lottery Mod: {} won but is offline. Storing winnings.", winnerId);
            unclaimedWinnings.put(winnerId, winnings);
            LotteryStateManager.saveUnclaimedWinnings(unclaimedWinnings);
        }

        ticketEntries.clear();
        lotteryPot = 0;
        nextDrawTime = System.currentTimeMillis() + LotteryConfig.announcementIntervalSeconds * 1000L;
        saveLotteryState();
    }

    public int getUnclaimedWinnings(UUID playerId) {
        return unclaimedWinnings.getOrDefault(playerId, 0);
    }

    public void claimWinnings(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (unclaimedWinnings.containsKey(playerId)) {
            int winnings = unclaimedWinnings.remove(playerId);
            player.getInventory().add(new ItemStack(Items.DIAMOND, winnings));
            player.sendSystemMessage(Component.literal("🎉 " + ChatFormatting.GREEN +
                    "You have claimed your " + winnings + " diamonds!" + ChatFormatting.RESET));
            LotteryStateManager.saveUnclaimedWinnings(unclaimedWinnings);
        }
    }

    public Map<UUID, Integer> getTicketEntries() {
        return ticketEntries;
    }

    public int getLotteryPot() {
        return lotteryPot;
    }

    public void addLotteryTicket(UUID playerUUID) {
        int count = ticketEntries.getOrDefault(playerUUID, 0);
        ticketEntries.put(playerUUID, count + 1);
        lotteryPot += 1;
        saveLotteryState();
    }

    public long getNextDrawTime() {
        return nextDrawTime;
    }

    // New: Returns the total count of tickets sold.
    public int getTicketCount() {
        return ticketEntries.values().stream().mapToInt(Integer::intValue).sum();
    }

    // New: Sets the next draw time and saves the lottery state.
    public void setNextDrawTime(long nextTime) {
        nextDrawTime = nextTime;
        saveLotteryState();
    }

    public String getLastWinner() {
        return lastWinner;
    }

    public int getLastWinningAmount() {
        return lastWinningAmount;
    }

    public Map<UUID, Integer> getLeaderboard() {
        return leaderboard;
    }

    private void saveLotteryState() {
        LotteryStateManager.saveLotteryState(lotteryPot, ticketEntries, nextDrawTime);
    }
}