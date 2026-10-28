package nl.juiced.guhs.feature.herinnering;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * The herinneringsbelletjes of the Herinneringseiland: three big hollow zeepbelglas balls, one next to each little memory
 * of Snoes. Touch one (walk up to it) or click it and its retro film plays ({@link HerinneringScenes}); a film you saw before
 * only plays again on a click. The last bubble (the stars) opens once the other two were seen; watching it gives Snoes' piece
 * "de herinnering" ({@link DroomVoortgang.Stukje#HERINNERING}) while the story still runs.
 * <p>
 * The positions are those of tools/features/herinnering.py BELLEN, relative to the island's centre spot (template
 * (38, 22, 38)); keep both in sync.
 */
public final class HerinneringBellen {
    /** A bubble: its centre block and the scene's anchor block (both relative to the island's centre spot). */
    public record Bel(String id, BlockPos rel, double straal, Cutscene scene, BlockPos anker) {
        /** The middle of the bubble in the world. */
        public Vec3 midden() {
            return DroomEilanden.wereld(DroomEilanden.HERINNERING, Vec3.atCenterOf(rel));
        }

        public BlockPos ankerInWereld() {
            return BlockPos.containing(DroomEilanden.wereld(DroomEilanden.HERINNERING, Vec3.atLowerCornerOf(anker)));
        }
    }

    /** In the order they are told; the last one (the stars) opens after the others. */
    public static final List<Bel> BELLEN = List.of(
            new Bel("knabbel", new BlockPos(-16, 4, -2), 2.4, HerinneringScenes.KNABBEL, new BlockPos(-11, 1, 2)),
            new Bel("blub", new BlockPos(9, 4, 10), 2.4, HerinneringScenes.BLUB, new BlockPos(2, 1, 14)),
            new Bel("sterren", new BlockPos(-12, 4, -5), 2.4, HerinneringScenes.STERREN, new BlockPos(-7, 4, -11)));
    /** The last bubble (the stars): it opens after the others and gives the piece. */
    public static final Bel LAATSTE = BELLEN.get(BELLEN.size() - 1);
    /** How close (beyond the bubble's radius) a player's middle must come to touch it, and a clicked block to belong to it. */
    public static final double AANRAAK = 1.3, KLIK = 0.9;

    /** The bubble each player touched last (so standing against one doesn't start it over and over). */
    private static final Map<UUID, String> LAATST = new ConcurrentHashMap<>();

    private HerinneringBellen() {
    }

    /** The bubble at this spot (world), within its radius plus {@code extra}; null: none. */
    @Nullable
    public static Bel bij(Vec3 pos, double extra) {
        for (Bel b : BELLEN) {
            if (b.midden().distanceTo(pos) <= b.straal() + extra) {
                return b;
            }
        }
        return null;
    }

    public static boolean gezien(ServerPlayer p, Bel b) {
        return Cutscenes.gezien(p, b.scene().id());
    }

    /** How many of the films before the last one this player has seen. */
    public static int gezienVoorLaatste(ServerPlayer p) {
        int n = 0;
        for (Bel b : BELLEN) {
            if (b != LAATSTE && gezien(p, b)) {
                n++;
            }
        }
        return n;
    }

    /** Is this bubble open for this player? (The last one opens after the others.) */
    public static boolean open(ServerPlayer p, Bel b) {
        return b != LAATSTE || gezienVoorLaatste(p) >= BELLEN.size() - 1;
    }

    /** Server, every half second for a dreaming player near the island: walking up to a bubble. */
    static void aanraken(ServerPlayer p) {
        if (Cutscenes.bezig(p)) {
            return;
        }
        Bel b = bij(p.position().add(0, p.getBbHeight() / 2, 0), AANRAAK);
        String vorige = LAATST.get(p.getUUID());
        if (b == null) {
            LAATST.remove(p.getUUID());
            return;
        }
        if (!b.id().equals(vorige)) {
            LAATST.put(p.getUUID(), b.id());
            probeer(p, b, false);
        }
    }

    /** Server: a click on a block of a bubble (true: it was one). */
    static boolean klik(ServerPlayer p, BlockPos pos) {
        Bel b = bij(Vec3.atCenterOf(pos), KLIK);
        if (b == null || Cutscenes.bezig(p)) {
            return b != null;
        }
        LAATST.put(p.getUUID(), b.id());
        probeer(p, b, true);
        return true;
    }

    /** Touch (klik false) or click a bubble: its film, or why not. */
    public static void probeer(ServerPlayer p, Bel b, boolean klik) {
        if (!open(p, b)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.herinnering.bel_dicht").withStyle(ChatFormatting.LIGHT_PURPLE));
            bubbels(p, b, 6);
            return;
        }
        if (gezien(p, b) && !klik) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.herinnering.bel_gezien").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        bubbels(p, b, 24);
        p.sendOverlayMessage(Component.translatable("gui.guhs.herinnering.bel").withStyle(ChatFormatting.LIGHT_PURPLE));
        Cutscenes.speel(p, b.scene(), b.ankerInWereld(), Rotation.NONE, speler -> na(speler, b));
    }

    /** After a film: how far you are, and Snoes' piece after the last one. */
    static void na(ServerPlayer p, Bel b) {
        int gezien = gezienVoorLaatste(p) + (gezien(p, LAATSTE) ? 1 : 0);
        p.sendOverlayMessage(Component.translatable("gui.guhs.herinnering.voortgang", gezien).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (b == LAATSTE) {
            stukje(p);
        }
    }

    /** Gives Snoes' piece "de herinnering" (only while the story runs: not after the finale); true when it was new. */
    public static boolean stukje(ServerPlayer p) {
        if (DroomVoortgang.finaleKlaar(p) || !DroomVoortgang.geef(p, DroomVoortgang.Stukje.HERINNERING)) {
            return false;
        }
        p.sendSystemMessage(Component.translatable("gui.guhs.herinnering.stukje").withStyle(ChatFormatting.LIGHT_PURPLE));
        ServerLevel level = (ServerLevel) p.level();
        level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.5, p.getZ(), 10, 0.6, 0.5, 0.6, 0.02);
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
        return true;
    }

    private static void bubbels(ServerPlayer p, Bel b, int n) {
        Vec3 m = b.midden();
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.BUBBLE_POP, m.x, m.y, m.z, n, b.straal() * 0.6, b.straal() * 0.6,
                b.straal() * 0.6, 0.02);
    }

    /** (tests) Forget who touched what. */
    static void vergeet(ServerPlayer p) {
        LAATST.remove(p.getUUID());
    }
}
