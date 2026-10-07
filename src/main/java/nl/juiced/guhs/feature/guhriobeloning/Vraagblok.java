package nl.juiced.guhs.feature.guhriobeloning;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhTime;

/**
 * What the ?-block to build with gives: ONE kaasknabbel per day per player, whichever ?-block they bump (so a wall of
 * them is decoration, not a knabbel farm, and every player of a server gets theirs from the same block). The day is the
 * guh day (the overworld clock); the day a player last got one is kept in their own data.
 * A bump is a jump against the block's underside ({@link #kijk}, every tick for a player in the air) or a click.
 */
public final class Vraagblok {
    /** Player data (GuhQuests.saved): the day number of the last knabbel (+1, so 0 = never). */
    public static final String DAG = "guhs_guhriobeloning_vraag_dag";
    /** Ticks before the same player's next bump counts (so one jump is one bump). */
    public static final int RUST = 12;

    private static final Map<UUID, Long> LAATST = new ConcurrentHashMap<>();
    /** Per player: how high their head was a tick ago (to see that they came up against the block). */
    private static final Map<UUID, Double> HOOFD = new ConcurrentHashMap<>();

    private Vraagblok() {
    }

    /** The guh day of this world. */
    public static long dag(ServerLevel level) {
        return Math.floorDiv(GuhTime.dayTime(level.getServer().overworld()), 24000L);
    }

    /** Did this player already get today's knabbel? */
    public static boolean gehad(ServerPlayer p) {
        return GuhQuests.saved(p).getLongOr(DAG, 0L) == dag(p.level()) + 1;
    }

    /** (tests, dev command) today's knabbel is there again for this player. */
    public static void vergeet(ServerPlayer p) {
        GuhQuests.saved(p).remove(DAG);
        LAATST.remove(p.getUUID());
    }

    /**
     * This player bumps the ?-block at pos (head or hand). The first time of the day a kaasknabbel pops out of the top
     * (theirs alone to pick up); after that the block only goes "tok" until tomorrow. True when a knabbel came out.
     */
    public static boolean bots(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        long nu = level.getGameTime();
        Long laatst = LAATST.get(p.getUUID());
        if (laatst != null && nu - laatst < RUST && nu >= laatst) {
            return false;
        }
        LAATST.put(p.getUUID(), nu);
        if (gehad(p)) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.BLOCKS, 0.8f, 0.7f);
            p.sendOverlayMessage(Component.translatable("gui.guhs.guhriobeloning.vraagblok.morgen").withStyle(ChatFormatting.GRAY));
            return false;
        }
        GuhQuests.saved(p).putLong(DAG, dag(level) + 1);
        ItemStack knabbel = new ItemStack(ModItems.KAAS_KNABBELS.get());
        BlockPos boven = pos.above();
        if (level.getBlockState(boven).getCollisionShape(level, boven).isEmpty()) {
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, knabbel, 0, 0.28, 0);
            item.setPickUpDelay(10);
            item.setTarget(p.getUUID());                          // (only whoever bumped it out picks it up)
            level.addFreshEntity(item);
        } else {
            Minigames.give(p, knabbel);
        }
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.9f, 1.7f);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.7f, 1.6f);
        level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.0);
        p.sendOverlayMessage(Component.translatable("gui.guhs.guhriobeloning.vraagblok.knabbel").withStyle(ChatFormatting.GOLD));
        GuhAdvancements.grant(p, "guhrio_beloning_vraagblok");
        return true;
    }

    /**
     * (every tick of a player) a player in the air whose head came up against the underside of a ?-block bumps it. The
     * server only knows where a player is, so "came up" = the head is higher than a tick ago and now touches the block.
     */
    static void kijk(ServerPlayer p) {
        double hoofd = p.getY() + p.getBbHeight();
        Double vorig = HOOFD.put(p.getUUID(), hoofd);
        if (p.onGround() || vorig == null || hoofd <= vorig + 1e-4 || p.isSpectator()) {
            return;
        }
        BlockPos boven = BlockPos.containing(p.getX(), hoofd + 0.08, p.getZ());
        if (boven.getY() - hoofd <= 0.08 && p.level().getBlockState(boven).getBlock() instanceof VraagblokBlock) {
            bots(p, boven);
        }
    }

    /** (logout, server stop) */
    static void wis(UUID speler) {
        LAATST.remove(speler);
        HOOFD.remove(speler);
    }

    static void wisAlles() {
        LAATST.clear();
        HOOFD.clear();
    }
}
