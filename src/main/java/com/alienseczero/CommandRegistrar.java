package com.alienseczero;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.*;

public class CommandRegistrar {
    private static final Logger LOGGER = LogUtils.getLogger();

    public CommandRegistrar() {
        LOGGER.info("[LM] CommandRegistrar instance created.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("[LM] Registering lottery commands from CommandRegistrar...");
        event.getDispatcher().register(
                Commands.literal("lottery")
                        // Base /lottery command: displays general lottery info with remaining time in minutes.
                        .executes(context -> {
                            int currentPot = (LotteryMod.getInstance().getLotteryPot() + LotteryConfig.BONUS_DIAMONDS);
                            String lastWinner = LotteryMod.getInstance().getLastWinner();
                            int lastWinningAmount = LotteryMod.getInstance().getLastWinningAmount();
                            long remainingMs = LotteryMod.getInstance().getNextDrawTime() - System.currentTimeMillis();
                            int nextDrawMinutes = (int)(remainingMs / 60000);
                            int nextDrawSeconds = (int)((remainingMs / 1000) % 60);
                            Component infoMessage = Component.literal("")
                                    .append(Component.literal("Lottery Info:").withStyle(ChatFormatting.GOLD))
                                    .append(Component.literal("\nCurrent Pot: ").withStyle(ChatFormatting.WHITE))
                                    .append(Component.literal(currentPot + " diamonds").withStyle(ChatFormatting.AQUA))
                                    .append(Component.literal("\nLast Winner: ").withStyle(ChatFormatting.WHITE))
                                    .append(Component.literal(lastWinner).withStyle(ChatFormatting.AQUA))
                                    .append(Component.literal(" (won ").withStyle(ChatFormatting.WHITE))
                                    .append(Component.literal(lastWinningAmount + " diamonds").withStyle(ChatFormatting.AQUA))
                                    .append(Component.literal(")").withStyle(ChatFormatting.WHITE))
                                    .append(Component.literal("\nNext draw in: ").withStyle(ChatFormatting.WHITE))
                                    .append(Component.literal(nextDrawMinutes + " minutes ").withStyle(ChatFormatting.AQUA))
                                    .append(Component.literal(nextDrawSeconds + " seconds").withStyle(ChatFormatting.AQUA));

                            context.getSource().sendSuccess(() -> infoMessage, false);
                            return 1;
                        })
                        // /lottery about command.
                        .then(Commands.literal("about")
                                .executes(context -> {
                                    Component aboutMessage = Component.literal("")
                                            .append(Component.literal("LotteryMod v1.0").withStyle(ChatFormatting.GOLD))
                                            .append(Component.literal("\nDeveloped by ").withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal("AlienSecZero").withStyle(ChatFormatting.LIGHT_PURPLE))
                                            .append(Component.literal("\nThe only server-side lottery mod for ").withStyle(ChatFormatting.YELLOW))
                                            .append(Component.literal("NeoForge Minecraft").withStyle(ChatFormatting.GREEN))
                                            .append(Component.literal("\nUse ").withStyle(ChatFormatting.BLUE))
                                            .append(Component.literal("/lottery").withStyle(ChatFormatting.GOLD))
                                            .append(Component.literal(" for info, ").withStyle(ChatFormatting.BLUE))
                                            .append(Component.literal("/lottery buy/claim").withStyle(ChatFormatting.GOLD))
                                            .append(Component.literal(" to participate,\n").withStyle(ChatFormatting.BLUE))
                                            .append(Component.literal("and ").withStyle(ChatFormatting.GOLD))
                                            .append(Component.literal("/lottery leaderboard").withStyle(ChatFormatting.GOLD))
                                            .append(Component.literal(" to see top winners.").withStyle(ChatFormatting.BLUE));

                                    context.getSource().sendSuccess(() -> aboutMessage, false);
                                    return 1;
                                })
                        )
                        // /lottery leaderboard command.
                        .then(Commands.literal("leaderboard")
                                .executes(context -> {
                                    Map<UUID, Integer> board = LotteryMod.getInstance().getLeaderboard();
                                    if (board.isEmpty()) {
                                        context.getSource().sendSuccess(() ->
                                                Component.literal(ChatFormatting.YELLOW + "No winners yet!"), false);
                                        return 1;
                                    }
                                    List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(board.entrySet());
                                    sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
                                    StringBuilder sb = new StringBuilder();
                                    sb.append(ChatFormatting.GOLD + "Lottery Leaderboard:\n");
                                    int displayCount = Math.min(10, sorted.size());
                                    for (int i = 0; i < displayCount; i++) {
                                        Map.Entry<UUID, Integer> entry = sorted.get(i);
                                        ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(entry.getKey());
                                        String name = (player != null) ? player.getName().getString() : entry.getKey().toString();
                                        sb.append(ChatFormatting.LIGHT_PURPLE).append(name)
                                                .append(" - ").append(ChatFormatting.WHITE).append(entry.getValue()).append(" wins\n");
                                    }
                                    context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                                    return 1;
                                })
                        )
                        // /lottery draw command.
                        .then(Commands.literal("draw")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    try {
                                        MinecraftServer server = context.getSource().getServer();
                                        LotteryMod.getInstance().drawLottery(server);
                                        context.getSource().sendSuccess(() -> Component.literal(ChatFormatting.GREEN + "Lottery draw completed!"), true);
                                        return 1;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        context.getSource().sendFailure(Component.literal(ChatFormatting.RED + "An error occurred during the lottery draw."));
                                        return 0;
                                    }
                                })
                        )
                        // /lottery claim command.
                        .then(Commands.literal("claim")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player == null) return 0;
                                    int winnings = LotteryMod.getInstance().getUnclaimedWinnings(player.getUUID());
                                    if (winnings <= 0) {
                                        player.sendSystemMessage(Component.literal(ChatFormatting.RED + "❌ You have no unclaimed lottery winnings."));
                                        return 0;
                                    }
                                    LotteryMod.getInstance().claimWinnings(player);
                                    return 1;
                                })
                        )
                        // /lottery buy command.
                        .then(Commands.literal("buy")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player == null) return 0;
                                    UUID playerId = player.getUUID();
                                    // Check persistent ticket count from LotteryMod's ticketEntries.
                                    int currentTickets = LotteryMod.getInstance().getTicketEntries().getOrDefault(playerId, 0);
                                    if (currentTickets >= LotteryConfig.MAX_TICKETS_PER_PLAYER) {
                                        context.getSource().sendFailure(Component.literal(ChatFormatting.RED +
                                                "You have reached the max ticket limit (" + LotteryConfig.MAX_TICKETS_PER_PLAYER + ") for this draw!"));
                                        return 0;
                                    }
                                    boolean hasDiamond = player.getInventory().items.stream()
                                            .anyMatch(stack -> stack.getItem() == Items.DIAMOND && stack.getCount() > 0);
                                    if (!hasDiamond) {
                                        context.getSource().sendFailure(Component.literal(ChatFormatting.RED + "You need 1 diamond to buy a ticket!"));
                                        return 0;
                                    }
                                    for (int i = 0; i < player.getInventory().items.size(); i++) {
                                        ItemStack stack = player.getInventory().items.get(i);
                                        if (stack.getItem() == Items.DIAMOND && stack.getCount() > 0) {
                                            stack.shrink(1);
                                            break;
                                        }
                                    }
                                    // Add ticket to persistent map.
                                    LotteryMod.getInstance().addLotteryTicket(player.getUUID());
                                    // Retrieve updated count.
                                    int updatedTickets = LotteryMod.getInstance().getTicketEntries().getOrDefault(playerId, 0);
                                    context.getSource().sendSuccess(() ->
                                                    Component.literal("")
                                                            .append(Component.literal("Lottery Ticket Purchased!").withStyle(ChatFormatting.GOLD))
                                                            .append(Component.literal("\nYou now have ").withStyle(ChatFormatting.WHITE))
                                                            .append(Component.literal(updatedTickets + (updatedTickets == 1 ? " ticket" : " tickets"))
                                                                    .withStyle(ChatFormatting.AQUA))
                                                            .append(Component.literal(" (Max: " + LotteryConfig.MAX_TICKETS_PER_PLAYER + ").")
                                                                    .withStyle(ChatFormatting.GRAY))
                                            , true);
                                    player.level().playSound(null, player.blockPosition(), SoundEvents.BUBBLE_POP, SoundSource.MASTER, 1.0F, 1.0F);
                                    return 1;
                                })
                        )
                        // /lottery cleareffects command.
                        .then(Commands.literal("cleareffects")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    LotteryEventHandler.clearActiveEffects(server);
                                    context.getSource().sendSuccess(() -> Component.literal(ChatFormatting.GREEN + "All mod particle effects have been cleared."), true);
                                    return 1;
                                })
                        )
                        // /lottery debug command.
                        .then(Commands.literal("clear")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    for (int i = 0; i < 100; i++) {
                                        server.getPlayerList().getPlayers().forEach(player ->
                                                player.sendSystemMessage(Component.literal(""))
                                        );
                                    }
                                    return 1;
                                })
                        )
        );
    }
}