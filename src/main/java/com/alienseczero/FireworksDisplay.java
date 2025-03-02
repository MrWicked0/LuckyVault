package com.alienseczero;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public class FireworksDisplay {

    private final ServerPlayer player;
    private final ServerLevel level;
    private int ticksElapsed;
    private final int durationTicks;

    // Jingle sequence: a fixed array of pitch values to form a melody.
    private static final float[] JINGLE_PITCHES = new float[] { 1.0f, 1.3f, 1.6f, 2.0f, 1.8f, 1.0f};
    private int jingleIndex = 0;

    public FireworksDisplay(ServerPlayer player, int durationTicks) {
        this.player = player;
        // Use player.level() cast to ServerLevel.
        this.level = (ServerLevel) player.level();
        this.ticksElapsed = 0;
        this.durationTicks = durationTicks;
        // Register for tick events using ServerTickEvent.Post.
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (ticksElapsed >= durationTicks) {
            NeoForge.EVENT_BUS.unregister(this);
            return;
        }
        ticksElapsed++;

        // Spawn a fast burst every 10 ticks.
        if (ticksElapsed % 10 == 0) {
            // Spawn multiple fireworks for a burst effect.
            for (int i = 0; i < 3; i++) {
                spawnFireworkAndEffects();
            }
        }
    }

    private void spawnFireworkAndEffects() {
        // Calculate a random offset (±2 blocks in X and Z)
        double offsetX = (Math.random() - 0.5) * 4;
        double offsetZ = (Math.random() - 0.5) * 4;
        double x = player.getX() + offsetX;
        double y = player.getY();
        double z = player.getZ() + offsetZ;

        // Spawn a firework rocket with a random effect.
        ItemStack fireworkStack = getRandomFireworkItem();
        FireworkRocketEntity firework = new FireworkRocketEntity(level, x, y, z, fireworkStack);
        level.addFreshEntity(firework);

        // Spawn psychedelic particle effects.
        level.sendParticles(ParticleTypes.ENCHANT, x, y + 1, z, 30, 1, 1, 1, 0.1);

        // Play a note using the jingle sequence.
        float jinglePitch = JINGLE_PITCHES[jingleIndex % JINGLE_PITCHES.length];
        jingleIndex++;
        level.playSound(null, x, y, z, SoundEvents.NOTE_BLOCK_BELL, SoundSource.MASTER, 1.0f, jinglePitch);

        // Spawn floating text using an invisible ArmorStand.
        ArmorStand textStand = new ArmorStand(level, player.getX(), player.getY() + 2, player.getZ());
        textStand.setInvisible(true);
        textStand.setCustomName(Component.literal("* Winner *"));
        textStand.setCustomNameVisible(true);
        level.addFreshEntity(textStand);

        // Schedule removal of the floating text after 40 ticks (2 seconds).
        new DelayedRemovalTask(textStand, 40).start();
    }

    // Returns a basic firework rocket ItemStack (customize NBT as needed).
    private ItemStack getRandomFireworkItem() {
        return new ItemStack(Items.FIREWORK_ROCKET);
    }
}

class DelayedRemovalTask {
    private final ArmorStand entity;
    private int ticksRemaining;

    public DelayedRemovalTask(ArmorStand entity, int delayTicks) {
        this.entity = entity;
        this.ticksRemaining = delayTicks;
    }

    public void start() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        ticksRemaining--;
        if (ticksRemaining <= 0) {
            entity.discard();
            NeoForge.EVENT_BUS.unregister(this);
        }
    }
}