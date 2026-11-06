package nl.juiced.guhs.feature.vadsrijk.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;

/**
 * U-HUD: the server side of the party frames. Every {@link #ELKE} ticks each party member in the Vadsrijk gets the OTHER members:
 * name, class + spec, level, Vads, buffs (at most {@link #MAX_BUFFS}), where they are, fainted/offline. Only party members get it
 * (nothing is sent without a party; one empty message when your party ends).
 * <p>
 * Click-to-target: a click on a frame (chat open) makes that member your {@link #gekozen chosen} party target (for heal/buff
 * skills that want one; it also becomes T-OPSLAG's target for the server).
 */
public final class PartyFrames {
    public static final int ELKE = 10, MAX_BUFFS = 6;
    /** How long a member shows "fainted" after Flauwvallen. */
    public static final int FLAUW_TICKS = 100;
    /** The spec line under the name: first non-null lang key wins (u-specs can add its spec here); default = kern's class title. */
    public static final List<Function<ServerPlayer, String>> SPEC_BRON = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static final Map<UUID, String> NAMEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> FLAUW = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> HAD_PARTY = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> GEKOZEN = new ConcurrentHashMap<>();
    /** Dev: fake members for testing alone: player -> (count, until game time). */
    private static final Map<UUID, long[]> NEP = new ConcurrentHashMap<>();

    private PartyFrames() {
    }

    static void init() {
        nl.juiced.guhs.feature.vadsrijk.kern.Haken.FLAUW.add(p -> FLAUW.put(p.getUUID(), p.level().getGameTime()));
    }

    /** The party member this player clicked last (online, still in the party), or null. */
    @Nullable
    public static ServerPlayer gekozen(ServerPlayer p) {
        UUID id = GEKOZEN.get(p.getUUID());
        if (id == null) {
            return null;
        }
        ServerPlayer l = p.level().getServer().getPlayerList().getPlayer(id);
        return l != null && Partijen.zelfdeParty(p, l) ? l : null;
    }

    static void kies(ServerPlayer p, UUID lid) {
        ServerPlayer l = p.level().getServer().getPlayerList().getPlayer(lid);
        if (l == null || l == p || !Partijen.zelfdeParty(p, l)) {
            return;
        }
        GEKOZEN.put(p.getUUID(), lid);
        if (l.level() == p.level()) {
            try {
                nl.juiced.guhs.feature.vadsrijk.opslag.HudBron.zetDoel(p, l);   // (T-OPSLAG's server target)
            } catch (RuntimeException ignored) {
                // (not essential)
            }
        }
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % ELKE != 0) {
            return;
        }
        long nu = server.overworld().getGameTime();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            NAMEN.put(p.getUUID(), p.getGameProfile().name());
            long[] nep = NEP.get(p.getUUID());
            if (nep != null && nu > nep[1]) {
                NEP.remove(p.getUUID());
                nep = null;
            }
            boolean party = Partijen.party(p) != null;
            if (!party && nep == null) {
                if (HAD_PARTY.remove(p.getUUID()) != null) {
                    PacketDistributor.sendToPlayer(p, new UHudPayloads.Party(List.of(), false));
                }
                continue;
            }
            if (!Vadsrijk.in(p)) {
                continue;   // (the frames only show in the dimension)
            }
            HAD_PARTY.put(p.getUUID(), true);
            List<UHudPayloads.Frame> l = nep != null ? nepLeden(p, (int) nep[0]) : frames(p);
            PacketDistributor.sendToPlayer(p, new UHudPayloads.Party(l, nep != null));
        }
    }

    /** The frames p sees: every other member of p's party (offline ones too), in join order. */
    public static List<UHudPayloads.Frame> frames(ServerPlayer p) {
        List<UHudPayloads.Frame> uit = new ArrayList<>();
        Partijen.Party party = Partijen.party(p);
        if (party == null) {
            return uit;
        }
        for (UUID id : party.leden) {
            if (id.equals(p.getUUID())) {
                continue;
            }
            ServerPlayer m = p.level().getServer().getPlayerList().getPlayer(id);
            boolean leider = party.leider.equals(id);
            if (m == null) {
                uit.add(new UHudPayloads.Frame(id, NAMEN.getOrDefault(id, "?"), "", "", 0, 0, 1, List.of(), "", 0, 0, 0,
                        UHudPayloads.STAAT_OFFLINE, leider));
            } else {
                uit.add(frame(m, leider));
            }
        }
        return uit;
    }

    static UHudPayloads.Frame frame(ServerPlayer m, boolean leider) {
        List<Integer> buffs = new ArrayList<>();
        for (Map.Entry<Status, Statussen.Actief> en : Statussen.van(m).entrySet()) {
            if (buffs.size() < MAX_BUFFS) {
                buffs.add(en.getKey().ordinal());
            }
        }
        long flauw = FLAUW.getOrDefault(m.getUUID(), -100000L);
        boolean isFlauw = !m.isAlive() || m.level().getGameTime() - flauw < FLAUW_TICKS;
        return new UHudPayloads.Frame(m.getUUID(), m.getGameProfile().name(), Spelers.klasse(m).id(), spec(m), Spelers.level(m),
                (float) Spelers.vads(m), (float) Spelers.maxVads(m), buffs, m.level().dimension().identifier().toString(), (float) m.getX(),
                (float) m.getY(), (float) m.getZ(), isFlauw ? UHudPayloads.STAAT_FLAUW : UHudPayloads.STAAT_OK, leider);
    }

    static String spec(ServerPlayer m) {
        for (Function<ServerPlayer, String> f : SPEC_BRON) {
            String s = f.apply(m);
            if (s != null) {
                return s;
            }
        }
        return Spelers.titelKey(m);
    }

    // ---- dev: fake members -------------------------------------------------------------------------------------------------------

    static void nep(ServerPlayer p, int aantal, int seconden) {
        if (aantal <= 0) {
            NEP.remove(p.getUUID());
            return;
        }
        NEP.put(p.getUUID(), new long[] {Math.min(4, aantal), p.level().getServer().overworld().getGameTime() + seconden * 20L});
    }

    /** Fake members around p: one healthy nearby, one hurt + buffed far away, one fainted, one offline. */
    static List<UHudPayloads.Frame> nepLeden(ServerPlayer p, int n) {
        String dim = p.level().dimension().identifier().toString();
        float x = (float) p.getX(), y = (float) p.getY(), z = (float) p.getZ();
        double t = p.level().getGameTime() / 20.0;
        List<UHudPayloads.Frame> l = new ArrayList<>();
        l.add(new UHudPayloads.Frame(new UUID(0, 1), "Knuffelaar", "knuffelridder", "vadsrijk.klassen.klasse.knuffelridder", 42, 900, 1000,
                List.of(Status.VAHOEG.ordinal()), dim, x + 4, y, z + 3, UHudPayloads.STAAT_OK, true));
        l.add(new UHudPayloads.Frame(new UUID(0, 2), "Pluisje", "pluisjager", "vadsrijk.klassen.klasse.pluisjager", 38,
                (float) (300 + 250 * Math.sin(t)), 800, List.of(Status.SNELPOOTJES.ordinal(), Status.BRANDEND.ordinal()), dim,
                x + 60 * (float) Math.cos(t / 4), y, z + 60 * (float) Math.sin(t / 4), UHudPayloads.STAAT_OK, false));
        l.add(new UHudPayloads.Frame(new UUID(0, 3), "Vadsmagiertje", "vadsmagier", "vadsrijk.klassen.klasse.vadsmagier", 40, 0, 700,
                List.of(Status.DUIZELIG.ordinal()), dim, x - 20, y, z - 20, UHudPayloads.STAAT_FLAUW, false));
        l.add(new UHudPayloads.Frame(new UUID(0, 4), "Slaapkop", "", "", 0, 0, 1, List.of(), "", 0, 0, 0, UHudPayloads.STAAT_OFFLINE, false));
        return l.subList(0, Math.min(n, l.size()));
    }

    static void vergeet(UUID id) {
        HAD_PARTY.remove(id);
        GEKOZEN.remove(id);
        NEP.remove(id);
        FLAUW.remove(id);
    }
}
