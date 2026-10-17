package nl.juiced.guhs.feature.einde2.spverhaal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 3.2), the skelet: the STAND-IN of every step that no slice has taken over yet
 * ({@link SpStappen#echt}). It keeps the whole chain playable from the first day: when such a step starts the player gets one
 * grey line ("[stand-in] Stap 4: Knabbelburgers bakken. Ga naar ..."), a thin column of light stands on the step's place
 * ({@link SpStappen#STANDIN}) for that player, and the step is done when the player stands within {@value #DICHTBIJ} blocks of
 * it (a step without a place: {@value #VANZELF_TICKS} ticks after it started, anywhere in the Guheinde). Never while a
 * cutscene plays. Moves on only with {@code verder(p, stap)}: a step is never skipped twice, a slice's step never.
 */
public final class SpStandIn {
    /** How close (blocks) to the place of a stand-in step counts as "there". */
    public static final double DICHTBIJ = 4.5;
    /** A stand-in step without a place is done this many ticks after it started. */
    public static final int VANZELF_TICKS = 60;
    /** The column of light is drawn up to this far away. */
    public static final double BAKEN = 160;

    /** Player -> when (server tick) a placeless stand-in step was first seen. */
    private static final Map<UUID, Long> SINDS = new ConcurrentHashMap<>();

    private SpStandIn() {
    }

    /** Every 10 ticks per player (SpVerhaal.opTick). */
    static void tick(ServerPlayer p) {
        if (!GuheindeFeature.isGuheinde(p.level()) || !SpVerhaal.open(p) || Cutscenes.bezig(p)) {
            return;
        }
        for (String lijn : SpVerhaal.LIJNEN) {
            if (!SpVerhaal.actief(p, lijn)) {
                continue;
            }
            Verhaallijn l = SpVerhaal.lijn(lijn);
            int stap = l.stap(p);
            if (SpStappen.isEcht(lijn, stap)) {
                SINDS.remove(p.getUUID());
                return;
            }
            stap(p, l, stap);
            return;                                     // (one line at a time: part 2 waits for part 1)
        }
    }

    private static void stap(ServerPlayer p, Verhaallijn l, int stap) {
        ServerLevel level = p.level();
        SpStappen.Plek plek = SpStappen.standIn(l.id(), stap);
        long nu = level.getServer().getTickCount();
        if (plek == null) {
            long sinds = SINDS.computeIfAbsent(p.getUUID(), u -> nu);
            if (nu - sinds >= VANZELF_TICKS) {
                SINDS.remove(p.getUUID());
                voltooi(p, l.id(), stap);
            }
            return;
        }
        SINDS.remove(p.getUUID());
        Vec3 doel = SpStappen.wereld(level, plek);
        if (doel == null) {
            return;
        }
        double dx = p.getX() - doel.x, dz = p.getZ() - doel.z, dy = p.getY() - doel.y;
        if (dx * dx + dz * dz <= DICHTBIJ * DICHTBIJ && Math.abs(dy) <= 4) {
            voltooi(p, l.id(), stap);
        } else if (p.position().distanceToSqr(doel) <= BAKEN * BAKEN && nu % 20 < 10) {
            for (int i = 0; i < 12; i++) {
                level.sendParticles(p, ParticleTypes.END_ROD, true, true, doel.x, doel.y + 0.5 + i * 1.5, doel.z, 1, 0.05, 0.2, 0.05, 0);
            }
        }
    }

    /**
     * Completes a stand-in step for this player (the stand-in itself, the dev command {@code klaar}, tests): only from exactly
     * that step and only when no slice owns it. True = the step moved on.
     */
    public static boolean voltooi(ServerPlayer p, String lijn, int stap) {
        if (SpStappen.isEcht(lijn, stap)) {
            return false;
        }
        Verhaallijn l = SpVerhaal.lijn(lijn);
        if (!l.begonnen(p)) {
            l.begin(p);
        }
        boolean verder = l.verder(p, stap);
        if (verder) {
            p.sendSystemMessage(Component.translatable(SpVerhaal.P + "standin.klaar", Component.translatable("gui.guhs.verhalen." + lijn + ".stap." + stap))
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        return verder;
    }

    /** A step of a Sponsguh line started (SpStappen.registreer): the stand-in says what to do, when no slice owns that step. */
    static void nieuweStap(ServerPlayer p, String lijn, int stap) {
        Verhaallijn l = SpVerhaal.lijn(lijn);
        if (stap >= l.stappen()) {
            p.sendSystemMessage(Component.translatable(SpVerhaal.P + "standin.lijn_klaar", Component.translatable("gui.guhs.verhalen." + lijn + ".naam"))
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        if (SpStappen.isEcht(lijn, stap)) {
            return;
        }
        String s = String.valueOf(stap);
        p.sendSystemMessage(Component.translatable(SpVerhaal.P + "standin.stap", stap, Component.translatable("gui.guhs.verhalen." + lijn + ".stap." + s),
                Component.translatable("gui.guhs.verhalen." + lijn + ".waar." + s)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    static void vergeet(UUID u) {
        SINDS.remove(u);
    }
}
