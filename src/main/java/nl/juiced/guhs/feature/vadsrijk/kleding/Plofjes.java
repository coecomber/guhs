package nl.juiced.guhs.feature.vadsrijk.kleding;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * T-KLEDING: the cheer-up (kill) effects (§4.3) and the kill-based sources. Kern's pink plof stays; the player who gave the LAST
 * hit adds their chosen effect on top (default confetti). The server only says "effect X at spot Y" to the clients within
 * {@link #BEREIK} blocks; each client draws it with its own effects slider (KledingClient). Also: the account-wide cheer-up counter
 * (every helper), and the Vahoeg rewards: the first Vahoeg boss ever ("vahoegkill") and the rare Schemervleugels drop.
 */
public final class Plofjes {
    public static final double BEREIK = 48;
    /** The rare Vahoeg boss drop (Schemervleugels): per helper, a normal boss / the dungeon's last boss. */
    public static final double VAHOEG_KANS = 0.03, VAHOEG_KANS_EIND = 0.08;
    private static final Map<UUID, UUID> LAATSTE = new ConcurrentHashMap<>();

    private Plofjes() {
    }

    /** Schade.NA_KLAP: remember who hit a mob last. */
    static void klap(Schade.Uitkomst u) {
        if (u.klap().bron() instanceof ServerPlayer p && u.klap().doel() instanceof VadsrijkMob m) {
            if (LAATSTE.size() > 4096) {
                LAATSTE.clear();   // (never grows without bound: mobs that walked off)
            }
            LAATSTE.put(m.getUUID(), p.getUUID());
        }
    }

    /** Haken.VERSLAGEN. */
    static void verslagen(Haken.Verslagen v) {
        VadsrijkMob m = v.mob();
        UUID laatste = LAATSTE.remove(m.getUUID());
        ServerPlayer wie = null;
        for (ServerPlayer p : v.helpers()) {
            Kledingkast.telKill(p);
            if (p.getUUID().equals(laatste) || (laatste == null && wie == null)) {
                wie = p;
            }
        }
        if (wie != null && m.level() instanceof ServerLevel sl) {
            String effect = Kledingkast.keuze(wie, "plof");
            if (effect.isEmpty() || !Kledingkast.heeft(wie, effect)) {
                effect = Cosmetica.PLOF_CONFETTI.id();
            }
            toon(sl, wie, effect, m.getX(), m.getY() + m.getBbHeight() * 0.6, m.getZ());
        }
        vahoeg(v);
    }

    /** Sends the effect to every client near the spot. */
    public static void toon(ServerLevel sl, ServerPlayer wie, String effect, double x, double y, double z) {
        KledingPayloads.Plof pl = new KledingPayloads.Plof(wie.getUUID(), effect, x, y, z);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(x, y, z) < BEREIK * BEREIK) {
                KledingPayloads.stuur(p, pl);
            }
        }
    }

    private static void vahoeg(Haken.Verslagen v) {
        if (!(v.mob() instanceof nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas b) || !b.vahoeg()) {
            return;
        }
        double kans = b.eindbaas() ? VAHOEG_KANS_EIND : VAHOEG_KANS;
        for (ServerPlayer p : v.helpers()) {
            Kledingkast.geefBron(p, "vahoegkill");
            if (!Kledingkast.heeft(p, Cosmetica.SCHEMER.id()) && p.getRandom().nextDouble() < kans) {
                Kledingkast.geefBron(p, "vahoeg");
                var server = p.level().getServer();
                if (server != null) {
                    server.getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.kleding.schemer.melding",
                            p.getDisplayName()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.BOLD), false);
                }
            }
        }
    }
}
