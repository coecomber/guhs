package nl.juiced.guhs.feature.einde2.h2water;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Rustpunten;

/**
 * einde2 h2_water: the two sneak parts on Zhao's flagship, the VNS Schroeiplek (markers {@code sluip_route},
 * {@code sluip_wacht_1..5}, {@code sluip_start}, {@code maanvis_kooi}).
 * <ul>
 *   <li>{@link E2H2Water#ONTSNAP}: out of the brig (where the Blue Spirit let you out) along the route backwards, off the
 *       ship to the harbour ({@code sluip_start}). The guard in front of the cell sleeps: the Blue Spirit saw to that.</li>
 *   <li>{@link E2H2Water#VIS_HALEN}: from the harbour along the route to the cell, where the moon fish hangs in a bag.</li>
 * </ul>
 * Every guard (a resident of the build, the Kind {@code VUURNATIE_MIKA}) looks out from its marker along a cone that sweeps
 * slowly left and right ({@link #kijkt}); the cone shows as little flames to the sneaking player only. Seen = a short
 * "HÉ!" and the player is put back on their last rest point (Rustpunten, kind {@link E2H2Water#RUST}: the start, or the last
 * checkpoint passed). Sneaking (crouching) makes the guards see less far. Never damage, never lost items.
 */
public final class Sluipen {
    /** How far a guard sees you (blocks), walking and sneaking; half the width of its cone (degrees). */
    public static final double ZICHT = 6.5, ZICHT_SLUIPEND = 3.6, KEGEL = 36;
    /** How far the cone sweeps left and right of the marker's yaw (degrees) and how long a sweep takes (ticks). */
    public static final double ZWAAI = 42, ZWAAI_TICKS = 140;
    /** The route points that are checkpoints (indices into sluip_route: the ladder top and the stern deck). */
    static final int[] CHECKPOINTS = {3, 6};

    private Sluipen() {
    }

    /** Where a caught player wakes up: tied to a post at the west end of the brig's corridor (five west, two north of the cell door), or null. */
    @Nullable
    static Vec3 cel(E2H2Water.Oord o) {
        BlockPos deur = laatste(o);
        return deur == null ? null : Vec3.atBottomCenterOf(deur.offset(-5, 0, -2));
    }

    /** The last point of the route: the door of the cell. */
    @Nullable
    static BlockPos laatste(E2H2Water.Oord o) {
        List<BlockPos> route = o.punten("sluip_route");
        return route.isEmpty() ? null : route.get(route.size() - 1);
    }

    /** A guard of the ship as the sneak sees it: where its eyes are and the yaw it looks along at this tick. */
    record Wacht(int nr, Vec3 oog, float yaw) {
    }

    /** The yaw a guard looks along at game time t: its marker's yaw, swept left and right (each guard its own phase). */
    static float kijkt(float basis, int nr, long t) {
        double fase = (t / ZWAAI_TICKS + nr * 0.37) * Math.PI * 2;
        return (float) (basis + ZWAAI * Math.sin(fase));
    }

    /**
     * Does a guard at {@code oog} looking along {@code yaw} see a target at {@code doel}? Pure maths: within reach and inside
     * the cone (the line of sight is the caller's).
     */
    public static boolean inKegel(Vec3 oog, float yaw, Vec3 doel, double bereik) {
        Vec3 d = doel.subtract(oog);
        double vlak = Math.sqrt(d.x * d.x + d.z * d.z);
        if (vlak > bereik || Math.abs(d.y) > 3.5) {
            return false;
        }
        if (vlak < 0.8) {
            return true;
        }
        float naar = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        return Math.abs(Mth.wrapDegrees(naar - yaw)) <= KEGEL;
    }

    /** The guards that watch at this step (the one in front of the cell sleeps during the escape). */
    static List<Wacht> wachten(E2H2Water.Oord o, int stap, long t) {
        java.util.ArrayList<Wacht> uit = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            if (stap == E2H2Water.ONTSNAP && i == 5) {
                continue;
            }
            Plekken.Plek p = Plekken.plek(E2H2Water.STRUCTUUR, "sluip_wacht_" + i);
            if (p == null) {
                continue;
            }
            Vec3 oog = Vec3.atBottomCenterOf(o.oorsprong().offset(p.pos())).add(0, 1.3, 0);
            uit.add(new Wacht(i, oog, kijkt(p.yaw(), i, t)));
        }
        return uit;
    }

    /** Once a second at a sneak step: the rest point at the start, the checkpoints, the goal. */
    static void seconde(ServerPlayer p, E2H2Water.Oord o, int stap) {
        List<BlockPos> route = o.punten("sluip_route");
        if (route.size() < 2) {
            return;
        }
        var dim = o.level().dimension();
        if (stap == E2H2Water.VIS_HALEN) {
            // the start of the sneak: on the harbour, before the ship
            if (Rustpunten.van(p, E2H2Water.RUST) == null) {
                Vec3 start = o.midden("rustpunt_2");
                if (start != null) {
                    Rustpunten.zet(p, E2H2Water.RUST, dim, start, 180f);
                }
            }
            for (int i : CHECKPOINTS) {
                if (i < route.size() && Vec3.atBottomCenterOf(route.get(i)).distanceToSqr(p.position()) < 2.2 * 2.2) {
                    Rustpunten.zet(p, E2H2Water.RUST, dim, Vec3.atBottomCenterOf(route.get(i)), p.getYRot());
                }
            }
            if (Vec3.atBottomCenterOf(route.get(route.size() - 1)).distanceToSqr(p.position()) < 2.4 * 2.4
                    || (o.wereld("maanvis_kooi") != null && Vec3.atBottomCenterOf(o.wereld("maanvis_kooi")).distanceToSqr(p.position()) < 2.6 * 2.6)) {
                E2H2Water.visGepakt(p);
            } else {
                E2H2Water.wijs(p, o, "maanvis_kooi");
            }
        } else if (stap == E2H2Water.ONTSNAP) {
            if (Rustpunten.van(p, E2H2Water.RUST) == null) {
                Vec3 cel = cel(o);
                if (cel != null) {
                    Rustpunten.zet(p, E2H2Water.RUST, dim, cel, 0f);
                }
            }
            for (int i : CHECKPOINTS) {
                if (i < route.size() && Vec3.atBottomCenterOf(route.get(i)).distanceToSqr(p.position()) < 2.2 * 2.2) {
                    Rustpunten.zet(p, E2H2Water.RUST, dim, Vec3.atBottomCenterOf(route.get(i)), p.getYRot());
                }
            }
            if (Vec3.atBottomCenterOf(route.get(0)).distanceToSqr(p.position()) < 3.0 * 3.0 || (o.in(p.position(), "haven", 0)
                    && !o.in(p.position(), "zhao_schip", 1) && p.getY() < route.get(0).getY() + 2)) {
                E2H2Water.ontsnapt(p);
            } else {
                E2H2Water.wijs(p, o, "sluip_start");
            }
        }
    }

    /** Every other tick at a sneak step, on or by the ship: do the guards see the player? Shows their cones. */
    static void tick(ServerPlayer p, E2H2Water.Oord o, int stap) {
        if (!o.in(p.position(), "zhao_schip", 3) || !Duwtje.mag(p)) {
            return;
        }
        long t = p.level().getGameTime();
        Vec3 doel = p.getEyePosition();
        double bereik = p.isShiftKeyDown() ? ZICHT_SLUIPEND : ZICHT;
        for (Wacht w : wachten(o, stap, t)) {
            if (w.oog().distanceToSqr(doel) > 18 * 18) {
                continue;
            }
            if (t % 6 == 0) {
                toonKegel(p, w);
            }
            if (inKegel(w.oog(), w.yaw(), doel, bereik) && zichtVrij(p, w.oog(), doel)) {
                gezien(p, w);
                return;
            }
        }
    }

    /** Nothing solid between the guard's eyes and the player. */
    static boolean zichtVrij(ServerPlayer p, Vec3 oog, Vec3 doel) {
        var hit = p.level().clip(new ClipContext(oog, doel, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(doel) < 0.25;
    }

    /** The edges of a guard's cone as little flames, for this player only. */
    static void toonKegel(ServerPlayer p, Wacht w) {
        for (int kant = -1; kant <= 1; kant += 2) {
            double yaw = Math.toRadians(w.yaw() + kant * KEGEL);
            Vec3 r = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            for (int i = 2; i <= 5; i++) {
                Vec3 q = w.oog().add(r.scale(i * 1.2)).add(0, -1.0, 0);
                Schijn.deeltjes(p, ParticleTypes.SMALL_FLAME, q, 1, 0.02, 0.0);
            }
        }
    }

    /** Caught: "HÉ!", a little shove, and back to the last rest point. */
    static void gezien(ServerPlayer p, Wacht w) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.HOSTILE, 1f, 0.7f);
        p.sendSystemMessage(Component.literal("<").append(Component.translatable("entity.guhs.guh_npc." + GuhNpcEntity.Kind.VUURNATIE_MIKA.id()))
                .append("> ").withStyle(ChatFormatting.RED).append(Component.translatable("quest.guhs.einde2_h2_water.sluip.gezien." + (w.nr() % 3))
                        .withStyle(ChatFormatting.WHITE)));
        if (!Rustpunten.terug(p, E2H2Water.RUST)) {
            Duwtje.duw(p, p.position().subtract(w.oog()), 1.0);
        }
        p.sendOverlayMessage(Component.translatable("quest.guhs.einde2_h2_water.sluip.terug").withStyle(ChatFormatting.GOLD));
    }
}
