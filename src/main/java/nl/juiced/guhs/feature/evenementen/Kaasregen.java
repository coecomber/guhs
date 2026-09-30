package nl.juiced.guhs.feature.evenementen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The kaasregen: for a minute kaasknabbels rain down around the players (now and then a golden one). You catch them by
 * walking into them; the ones nobody catches lie on the ground for a bit, and wild guhs come running to eat them: a guh
 * that ate one is happy for a while and much easier to tame ({@link Evenementen#boosted}).
 */
public class Kaasregen extends Evenement {
    public static final int DURATION = 20 * 60;
    /** A knabbel every this many ticks (a bit more often with more players). */
    public static final int EVERY = 6;
    /** How long a knabbel lies on the ground before it's gone. */
    public static final int LIE_TICKS = 20 * 15;
    /** 1 in this many knabbels is a golden one. */
    public static final int GOLDEN_CHANCE = 40;
    /** How long a guh that ate a knabbel from the sky stays happy (and easy to tame). */
    public static final int BLIJ_TICKS = 20 * 60 * 5;
    /** Knabbels to catch in one kaasregen for the "kaasregen" quest. */
    public static final int GOAL = 20;
    /** Wild guhs this close to the middle notice the knabbels; they sniff them out from this far. */
    public static final double GUH_RANGE = 24, SNIFF = 10;

    /** How far from a player the knabbels fall (tests make this small). */
    public double radius = 10;
    /** How high above the ground they appear. */
    public double height = 16;
    /** Wild guhs this close to the middle come for the knabbels (tests make this small). */
    public double guhRange = GUH_RANGE;
    final List<VallendeKnabbelEntity> snacks = new ArrayList<>();
    final Map<UUID, Integer> caught = new HashMap<>();

    public Kaasregen(ServerLevel level, Vec3 center) {
        super(EvenementType.KAASREGEN, level, center, DURATION);
    }

    public int caught(Player player) {
        return caught.getOrDefault(player.getUUID(), 0);
    }

    public List<VallendeKnabbelEntity> snacks() {
        snacks.removeIf(VallendeKnabbelEntity::isRemoved);
        return snacks;
    }

    @Override
    protected void tickEvent() {
        if (age % 10 == 0) {
            followParticipants();
        }
        int every = Math.max(2, EVERY - (participants.size() - 1));
        if (spontaneous && age % every == 0 && age < duration - 60) {
            spawnSnack();
        }
        for (VallendeKnabbelEntity snack : List.copyOf(snacks())) {
            snack.serverStep();
            if (snack.landedTicks > LIE_TICKS) {
                level.sendParticles(ParticleTypes.POOF, snack.getX(), snack.getY() + 0.2, snack.getZ(), 3, 0.1, 0.1, 0.1, 0.01);
                snack.discard();
                continue;
            }
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, snack.getBoundingBox().inflate(0.45),
                    p -> p.isAlive() && !p.isSpectator())) {
                catchSnack(player, snack);
                break;
            }
        }
        if (age % 10 == 5) {
            guhsComeRunning();
        }
    }

    /** One knabbel starts falling near someone (outdoors, not on a building); null when there was no good spot. */
    @Nullable
    public VallendeKnabbelEntity spawnSnack() {
        Vec3 around = aroundSomeone();
        double angle = level.random.nextDouble() * Math.PI * 2, dist = Math.sqrt(level.random.nextDouble()) * radius;
        BlockPos ground = Evenementen.ground(level, Mth.floor(around.x + Math.cos(angle) * dist), Mth.floor(around.z + Math.sin(angle) * dist),
                around.y, false);
        if (ground == null) {
            return null;
        }
        return dropSnack(ground, level.random.nextInt(GOLDEN_CHANCE) == 0);
    }

    /** Drops a knabbel onto this spot. */
    public VallendeKnabbelEntity dropSnack(BlockPos ground, boolean golden) {
        VallendeKnabbelEntity snack = EvenementenFeature.VALLENDE_KNABBEL.get().create(level);
        double landY = ground.getY() + 0.05;
        snack.setUp(golden, landY);
        snack.moveTo(ground.getX() + 0.2 + level.random.nextDouble() * 0.6, landY + height + level.random.nextDouble() * 4,
                ground.getZ() + 0.2 + level.random.nextDouble() * 0.6, 0, 0);
        entities.add(snack.getUUID());
        level.addFreshEntity(snack);
        snacks.add(snack);
        return snack;
    }

    /** Caught! The knabbel goes into your pockets (or at your feet, when they're full). */
    public void catchSnack(ServerPlayer player, VallendeKnabbelEntity snack) {
        if (snack.isRemoved()) {
            return;
        }
        boolean golden = snack.isGolden();
        snack.discard();
        player.getInventory().placeItemBackInInventory(golden ? new ItemStack(EvenementenFeature.GOUDEN_KAASKNABBEL.get())
                : new ItemStack(ModItems.KAAS_KNABBELS.get()));
        int count = caught.merge(player.getUUID(), 1, Integer::sum);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f,
                1.2f + level.random.nextFloat() * 0.6f);
        if (golden) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.3f);
            level.sendParticles(ParticleTypes.WAX_OFF, player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.5, 0.4, 0.1);
            player.displayClientMessage(Component.translatable("gui.guhs.evenement.kaasregen.golden").withStyle(ChatFormatting.GOLD), true);
            GuhAdvancements.grant(player, "evenement_gouden_knabbel");
        } else {
            player.displayClientMessage(Component.translatable("gui.guhs.evenement.kaasregen.caught", count).withStyle(ChatFormatting.YELLOW), true);
        }
        if (count >= GOAL) {
            GuhAdvancements.grant(player, "evenement_kaasregen");
        }
    }

    /** Wild guhs nearby run to the knabbels on the ground, and eat them. */
    private void guhsComeRunning() {
        List<VallendeKnabbelEntity> onGround = new ArrayList<>();
        for (VallendeKnabbelEntity snack : snacks()) {
            if (snack.landed()) {
                onGround.add(snack);
            }
        }
        if (onGround.isEmpty()) {
            return;
        }
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, new AABB(center, center).inflate(guhRange + radius), Evenementen::wild)) {
            if (guh.isOrderedToSit() || guh.isPassenger() || guh.isVehicle()) {
                continue;
            }
            VallendeKnabbelEntity nearest = null;
            double best = SNIFF * SNIFF;
            for (VallendeKnabbelEntity snack : onGround) {
                double d = guh.distanceToSqr(snack);
                if (!snack.isRemoved() && d < best) {
                    best = d;
                    nearest = snack;
                }
            }
            if (nearest == null) {
                continue;
            }
            if (Math.sqrt(best) < 1.1 + guh.getBbWidth() / 2) {
                eat(guh, nearest);
            } else {
                guh.getNavigation().moveTo(nearest.getX(), nearest.getY(), nearest.getZ(), 1.3);
            }
        }
    }

    /** A wild guh eats a knabbel from the sky: yum, and happy (easy to tame) for a while. */
    public void eat(GuhEntity guh, VallendeKnabbelEntity snack) {
        snack.discard();
        guh.getNavigation().stop();
        guh.heal(5f);
        guh.playSound(ModSounds.GUH_EAT.get(), 1f, guh.getVoicePitch());
        guh.triggerAnim("action", "happy");
        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 2, 0.3, 0.1, 0.3, 0);
        Evenementen.makeHappy(guh, BLIJ_TICKS * (snack.isGolden() ? 2 : 1));
    }

    @Override
    protected void finish(boolean completed) {
        for (ServerPlayer player : players()) {
            int count = caught(player);
            player.sendSystemMessage(announce(Component.translatable(count > 0 ? "gui.guhs.evenement.kaasregen.end"
                    : "gui.guhs.evenement.kaasregen.end_none", count)));
        }
    }

    @Override
    protected void cleanUp(net.minecraft.world.entity.Entity entity) {
        level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 0.2, entity.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
        entity.discard();
    }
}
