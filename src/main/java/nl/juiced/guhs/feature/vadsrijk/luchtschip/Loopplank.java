package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * W-SCHEPEN: boarding on foot, forgiving. Every harbour POI ({@link Lijnen.Haven#plek()}) is the last stand cell at the open tip
 * of its pier and the docked ship lies flush beyond it ({@link Lijnen.Haven#UIT}), so the ship's stern click box starts on the
 * tip. On top of clicking the ship itself:
 * <ul>
 *   <li><b>walk on:</b> step off the tip towards a docked ship and you are on board ({@link Luchtschepen#instappen}); when the
 *       line is still closed for you, you get the explanation and stay on the pier;</li>
 *   <li><b>click the tip:</b> a right-click on a block of the pier within {@link #KLIK_BEREIK} of the tip boards too;</li>
 *   <li><b>safety net:</b> nobody falls off an open pier tip: whoever drops off it (no ship there, or the line is closed) is put
 *       back on the tip, with the time of the next departure.</li>
 * </ul>
 * Real lines only act in Het Vadsrijk; the gametests' own lines in any level.
 */
public final class Loopplank {
    /** Horizontal distance (blocks) from the tip within which a click on the pier boards the docked ship. */
    public static final double KLIK_BEREIK = 4;
    /** How far beyond the tip's edge (along the pier axis) and to the side the "step on board" zone reaches. */
    static final double VOOR = 4.0, OPZIJ = 3.5;
    private static final Map<UUID, Long> LAATST = new HashMap<>();

    private Loopplank() {
    }

    /** Every tick: players stepping or dropping off a pier tip. */
    static void tik(MinecraftServer server) {
        for (ServerLevel sl : server.getAllLevels()) {
            if (sl.players().isEmpty()) {
                continue;
            }
            boolean vadsrijk = Vadsrijk.in(sl);
            for (ServerPlayer p : sl.players()) {
                if (p.isPassenger() || p.isSpectator() || p.getAbilities().flying || !p.isAlive() || RijkScenes.bezig(p)) {
                    continue;
                }
                for (Lijnen.Lijn l : Lijnen.ALLE) {
                    if (!(l.test() || vadsrijk && l.actief())) {
                        continue;
                    }
                    if (stap(sl, p, l, l.a()) || stap(sl, p, l, l.b())) {
                        break;
                    }
                }
            }
        }
    }

    /** True when this player was handled at this harbour's tip (boarded or put back). */
    static boolean stap(ServerLevel sl, ServerPlayer p, Lijnen.Lijn l, Lijnen.Haven h) {
        BlockPos b = h.plek();
        Vec3 u = h.uit();
        double dx = p.getX() - (b.getX() + 0.5), dz = p.getZ() - (b.getZ() + 0.5);
        double voor = dx * u.x + dz * u.z, opzij = -dx * u.z + dz * u.x;
        double dy = p.getY() - b.getY();
        if (voor <= 0.55 || voor > 0.5 + VOOR || Math.abs(opzij) > OPZIJ || dy > 1.5 || dy < -4) {
            return false;
        }
        // only over the open air beyond the pier (a pier that runs on is just walked on)
        BlockPos voet = BlockPos.containing(p.getX(), b.getY() - 1, p.getZ());
        if (!sl.getBlockState(voet).getCollisionShape(sl, voet).isEmpty()) {
            return false;
        }
        LuchtschipEntity e = aangemeerd(sl, l, h);
        long nu = sl.getGameTime();
        if (e != null && dy > -1.0) {
            Long t = LAATST.get(p.getUUID());
            if (t == null || nu - t >= 10 || nu < t) {
                LAATST.put(p.getUUID(), nu);
                if (Luchtschepen.instappen(p, e)) {
                    return true;
                }
            }
            if (dy > -0.5) {
                return true;   // (refused and still level with the tip: Poorten said why; the fall below catches a drop)
            }
        }
        if (dy < -0.5) {
            terug(sl, p, h, e == null ? l : null);
            return true;
        }
        return false;
    }

    /** The ship of this line lying at this harbour now (spawned), or null. */
    @Nullable
    static LuchtschipEntity aangemeerd(ServerLevel sl, Lijnen.Lijn l, Lijnen.Haven h) {
        int k = Dienstregeling.schipAan(l, h, Tijd.nu(sl, l));
        if (k < 0) {
            return null;
        }
        LuchtschipEntity e = Luchtschepen.schip(sl, l.id() + ":" + k);
        return e != null && !e.vliegt() ? e : null;
    }

    /** Back onto the tip (no fall damage); {@code wacht}: say when the next ship leaves from here. */
    static void terug(ServerLevel sl, ServerPlayer p, Lijnen.Haven h, @Nullable Lijnen.Lijn wacht) {
        BlockPos b = h.plek();
        Vec3 v = new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5).subtract(h.uit().scale(0.4));
        p.teleport(new TeleportTransition(sl, v, Vec3.ZERO, p.getYRot(), p.getXRot(), TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        p.setDeltaMovement(Vec3.ZERO);
        if (wacht != null) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.luchtschip.loopplank.nog_niet",
                    Luchtschepen.klok(Dienstregeling.totVertrek(wacht, (long) Tijd.nu(sl, wacht)))).withStyle(ChatFormatting.GOLD));
        }
    }

    /**
     * A right-click on a block near a harbour tip: board the docked ship when the line is open for you (true = handled, the
     * click is cancelled). A closed line is left to {@link Luchtschepen#pierKlik}'s explanation.
     */
    static boolean klik(ServerPlayer p, BlockPos k) {
        if (!(p.level() instanceof ServerLevel sl) || p.isPassenger() || p.isSpectator()) {
            return false;
        }
        boolean vadsrijk = Vadsrijk.in(sl);
        for (Lijnen.Lijn l : Lijnen.ALLE) {
            if (!(l.test() || vadsrijk && l.actief())) {
                continue;
            }
            for (Lijnen.Haven h : new Lijnen.Haven[] {l.a(), l.b()}) {
                BlockPos b = h.plek();
                double dx = k.getX() - b.getX(), dz = k.getZ() - b.getZ();
                if (Math.abs(k.getY() - b.getY()) > 3 || dx * dx + dz * dz > KLIK_BEREIK * KLIK_BEREIK) {
                    continue;
                }
                LuchtschipEntity e = aangemeerd(sl, l, h);
                if (e == null || Poorten.dicht(p, l, h == l.a()) != null) {
                    return false;
                }
                return Luchtschepen.instappen(p, e);
            }
        }
        return false;
    }

    static void vergeet(ServerPlayer p) {
        LAATST.remove(p.getUUID());
    }
}
