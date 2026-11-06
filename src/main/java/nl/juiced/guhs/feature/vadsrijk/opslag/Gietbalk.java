package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * T-OPSLAG: cast bars (VOORSTEL_ROADMAP §6.1) for everything that takes a moment: a player's channel (Losknuffelen, later
 * professions/hearth/rez...) above the hotbar, and a boss's wind-up under the boss bar (name of the attack, interrupt colour).
 * <pre>
 * // a player channel, pushed:            Gietbalk.speler(p, "vadsrijk.mijn.giet.vissen", 60, 0x8CC4FF);   ... Gietbalk.stopSpeler(p, true);
 * // or polled every 5 ticks (no calls at all while it runs; start/stop/interrupt are found by comparing):
 * Gietbalk.BRONNEN.add(p -&gt; iksBezig(p) ? new Gietbalk.Kanaal("vadsrijk.mijn.giet.x", beginGameTime, duurTicks, 0x8CC4FF) : null);
 * // a boss attack wind-up (players within straal see it):
 * Gietbalk.baas(this, "vadsrijk.mijnbaas.aanval.stamp", 40, 0xFF6A6A, true, 48);   ... Gietbalk.stopBaas(this, true, 48);
 * </pre>
 */
public final class Gietbalk {
    /** A polled channel: lang key of its name, the game time it began, its length in ticks, the bar colour (RGB). */
    public record Kanaal(String naamKey, long begin, int duur, int kleur) {
    }

    public interface Bron {
        @Nullable
        Kanaal kanaal(ServerPlayer p);
    }

    public static final List<Bron> BRONNEN = new CopyOnWriteArrayList<>();
    private static final Map<UUID, Kanaal> GEZIEN = new ConcurrentHashMap<>();

    private Gietbalk() {
    }

    public static void speler(ServerPlayer p, String naamKey, int ticks, int kleur) {
        PacketDistributor.sendToPlayer(p, new OpslagPayloads.Giet(p.getId(), naamKey, Math.max(1, ticks), kleur, 0));
    }

    public static void stopSpeler(ServerPlayer p, boolean onderbroken) {
        PacketDistributor.sendToPlayer(p, new OpslagPayloads.Giet(p.getId(), "", 0, 0, onderbroken ? OpslagPayloads.Giet.ONDERBROKEN : 0));
    }

    public static void baas(LivingEntity baas, String naamKey, int ticks, int kleur, boolean onderbreekbaar, double straal) {
        if (baas instanceof nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas vb) {   // T-INT: ONE boss cast bar on screen: t-vfx's
            nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.cast(vb, naamKey, ticks, onderbreekbaar);
            return;
        }
        stuurRond(baas, new OpslagPayloads.Giet(baas.getId(), naamKey, Math.max(1, ticks), kleur,
                OpslagPayloads.Giet.BAAS | (onderbreekbaar ? OpslagPayloads.Giet.ONDERBREEKBAAR : 0)), straal);
    }

    public static void stopBaas(LivingEntity baas, boolean onderbroken, double straal) {
        if (baas instanceof nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas vb) {   // T-INT: t-vfx's bar
            nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.castStop(vb, onderbroken);
            return;
        }
        stuurRond(baas, new OpslagPayloads.Giet(baas.getId(), "", 0, 0, OpslagPayloads.Giet.BAAS | (onderbroken ? OpslagPayloads.Giet.ONDERBROKEN : 0)), straal);
    }

    private static void stuurRond(LivingEntity e, OpslagPayloads.Giet g, double straal) {
        if (e.level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(e) <= straal * straal) {
                    PacketDistributor.sendToPlayer(p, g);
                }
            }
        }
    }

    /** Every 5 ticks per player in the dimension (from {@link HudBron}): start/stop polled channels. */
    static void peil(ServerPlayer p, long nu) {
        Kanaal k = null;
        for (Bron b : BRONNEN) {
            k = b.kanaal(p);
            if (k != null) {
                break;
            }
        }
        Kanaal oud = GEZIEN.get(p.getUUID());
        if (k != null && (oud == null || oud.begin() != k.begin() || !oud.naamKey().equals(k.naamKey()))) {
            GEZIEN.put(p.getUUID(), k);
            int rest = (int) Math.max(1, k.duur() - (nu - k.begin()));
            PacketDistributor.sendToPlayer(p, new OpslagPayloads.Giet(p.getId(), k.naamKey(), rest, k.kleur(), 0));
        } else if (k == null && oud != null) {
            GEZIEN.remove(p.getUUID());
            boolean af = nu - oud.begin() >= oud.duur() - 2;
            stopSpeler(p, !af);
        }
    }

    static void vergeet(UUID id) {
        GEZIEN.remove(id);
    }
}
