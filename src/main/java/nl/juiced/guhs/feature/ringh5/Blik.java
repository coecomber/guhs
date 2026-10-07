package nl.juiced.guhs.feature.ringh5;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Rustpunten;

/**
 * bbq2 (ring-h5): the gaze of the Eye of Sausron: ONE spot of light on the valley floor that you can read and avoid
 * (DESIGN_130 4: "sneak past the Eye"). The Eye ({@link OogEntity}) owns one Blik and lets it tick on the server.
 * <ul>
 *   <li><b>Where it looks.</b> While somebody is in het Asveld (zone A) or on de Kale Vlakte (zone B), the light runs up and
 *       down the line of that zone ({@link Plekken#BLIK_A}, {@link Plekken#BLIK_B}), at {@link #SNELHEID} blocks a tick: a
 *       little faster than a sprint, so you can't just run behind it. Players in both zones: it does one run in each, in
 *       turn. Nobody in a zone: a slow round over the valley ({@link Plekken#BLIK_RUST}).</li>
 *   <li><b>Who it sees.</b> Whoever is within {@link #STRAAL} blocks of the spot, with a free line from the Eye to their head
 *       or their middle, and who is no rock under the Elfenmanteltje ({@link Ring#oogZiet}). After {@link #GENADE} ticks in
 *       the light (a warning first) they are put back on their last rest point ({@link Ring#terugNaarRustpunt}): never
 *       damage, nothing lost.</li>
 *   <li><b>The ring.</b> A worn ring in the valley draws the Eye whatever stands in between: the light leaves its line and
 *       homes in on the bearer; after exactly {@link #FOCUS} ticks it has them, wherever the light was (taking the ring off
 *       lets the count run down twice as fast). That is long enough to slip past the Mika guards of het Wachthek.</li>
 *   <li>Within {@link #RUST_VEILIG} blocks of your own rest point you are safe ({@link #bijRustpunt}).</li>
 *   <li>Players whose story is done are left alone (for them the Eye had its piece of ring and naps); when nobody else
 *       is around the Eye sleeps.</li>
 * </ul>
 * Nothing is saved: after a restart the light starts its round again.
 */
public final class Blik {
    /** The radius of the spot of light (blocks). */
    public static final double STRAAL = 4.5;
    /** How fast the light runs along a zone's line, travels to another line, and strolls its idle round (blocks a tick). */
    public static final double SNELHEID = 0.3, REIS = 0.8, RUST = 0.16;
    /** Ticks in the light before you are caught; ticks of a worn ring before the Eye has you; when it warns you. */
    public static final int GENADE = 10, FOCUS = 80, FOCUS_WAARSCHUWING = 20;
    /** After a catch, or after a ring went off, the light lingers this long before it goes back to its line. */
    public static final int TREUZEL = 40;
    /** Within this many blocks of your own rest point nothing sees you. */
    public static final double RUST_VEILIG = 5.0;
    /** Players further than this from the Eye don't count. */
    public static final double BEREIK = 120;

    /** What the Eye is doing (synced by the entity: the animation). */
    public static final int WAAKT = 0, ZOEKT = 1, SLAAPT = 2;

    private enum Zone { RUST, A, B }

    /** A line of points with a position along it. */
    private record Lijn(List<Vec3> punten, double[] tot, double lengte) {
        static Lijn van(List<Vec3> punten, boolean rond) {
            List<Vec3> p = new ArrayList<>(punten);
            if (rond) {
                p.add(punten.get(0));
            }
            double[] tot = new double[p.size()];
            for (int i = 1; i < p.size(); i++) {
                tot[i] = tot[i - 1] + p.get(i).distanceTo(p.get(i - 1));
            }
            return new Lijn(p, tot, tot[tot.length - 1]);
        }

        Vec3 op(double s) {
            s = Math.max(0, Math.min(lengte, s));
            for (int i = 1; i < punten.size(); i++) {
                if (s <= tot[i]) {
                    double stuk = tot[i] - tot[i - 1];
                    return stuk < 1e-6 ? punten.get(i) : punten.get(i - 1).lerp(punten.get(i), (s - tot[i - 1]) / stuk);
                }
            }
            return punten.get(punten.size() - 1);
        }
    }

    @Nullable
    private Vec3 plek;
    private Zone zone = Zone.RUST;
    private double s;
    private int richting = 1;
    private boolean onderweg = true;
    private int treuzel;
    private final Map<UUID, Integer> gezien = new HashMap<>(), focus = new HashMap<>();
    @Nullable
    private Terrein lijnenVan;
    private Lijn lijnA, lijnB, lijnRust;
    /** How many players were caught since the last time the entity asked (for its happy squint). */
    private int betrapt;
    /** Until this game time the light sweeps het Asveld whoever is there (the scene on the ridge shows it). */
    private long toonVeldTot;

    /** The spot the Eye looks at right now (world; null: it sleeps or has not looked yet). */
    @Nullable
    public Vec3 plek() {
        return plek;
    }

    /** How long this player has been in the light (ticks, 0: not). */
    public int gezien(ServerPlayer p) {
        return gezien.getOrDefault(p.getUUID(), 0);
    }

    /** How far the Eye is in finding this ring bearer (ticks of {@link #FOCUS}). */
    public int focus(ServerPlayer p) {
        return focus.getOrDefault(p.getUUID(), 0);
    }

    /** The zone the light is working on: "rust", "a" or "b" (for the dev command and the tests). */
    public String zone() {
        return zone.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** (the entity) how many players were caught since the last call. */
    int neemBetrapt() {
        int n = betrapt;
        betrapt = 0;
        return n;
    }

    /** Who keeps the Eye awake: every player in this copy, within reach, whose story is not done yet. */
    public static List<ServerPlayer> aanwezig(ServerLevel level, Terrein t, Vec3 oog) {
        List<ServerPlayer> uit = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator() && !p.isCreative() && !Ring.klaar(p) && p.distanceToSqr(oog) <= BEREIK * BEREIK && t.bevat(p.position())) {
                uit.add(p);
            }
        }
        return uit;
    }

    /** Whom the Eye can catch right now: who is present and not in a scene, reading a card or talking. */
    public static List<ServerPlayer> doelwitten(ServerLevel level, Terrein t, Vec3 oog) {
        List<ServerPlayer> uit = aanwezig(level, t, oog);
        uit.removeIf(p -> !Duwtje.mag(p));
        return uit;
    }

    /**
     * One server tick. {@code oog}: where the Eye's sight starts (world), {@code bron}: the Eye itself (for its sounds).
     * Returns {@link #WAAKT}, {@link #ZOEKT} or {@link #SLAAPT}.
     */
    public int tick(ServerLevel level, Terrein t, Vec3 oog, Entity bron) {
        if (lijnenVan != t) {
            lijnenVan = t;
            lijnA = Lijn.van(t.route(Plekken.BLIK_A), false);
            lijnB = Lijn.van(t.route(Plekken.BLIK_B), false);
            lijnRust = Lijn.van(t.route(Plekken.BLIK_RUST), true);
        }
        List<ServerPlayer> spelers = aanwezig(level, t, oog);
        if (spelers.isEmpty() && toonVeldTot <= level.getGameTime() && treuzel <= 0) {
            gezien.clear();
            focus.clear();
            plek = null;
            onderweg = true;
            return SLAAPT;
        }
        spelers.removeIf(p -> !Duwtje.mag(p));
        if (plek == null) {
            plek = lijnRust.op(s);
        }
        // --- the ring: a worn ring in the valley draws the Eye ---------------------------------------------------------------
        ServerPlayer drager = null;
        int meeste = 0;
        for (ServerPlayer p : spelers) {
            boolean om = Ring.om(p) && t.in(Plekken.DOMEIN, p.position()) && !bijRustpunt(p);
            int f = focus.getOrDefault(p.getUUID(), 0);
            f = om ? f + 1 : Math.max(0, f - 2);
            if (f <= 0) {
                focus.remove(p.getUUID());
            } else {
                focus.put(p.getUUID(), f);
            }
            if (om && f > meeste) {
                meeste = f;
                drager = p;
            }
            if (om && f == FOCUS_WAARSCHUWING) {
                p.sendOverlayMessage(Component.translatable("quest.guhs.ringh5.oog.ring").withStyle(ChatFormatting.RED));
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0f, 1.3f);
            }
        }
        focus.keySet().removeIf(id -> spelers.stream().noneMatch(p -> p.getUUID().equals(id)));
        int staat = WAAKT;
        if (drager != null) {
            // the light homes in: it is on the bearer exactly when the count is full
            int rest = Math.max(1, FOCUS - meeste);
            Vec3 naar = new Vec3(drager.getX(), plek.y, drager.getZ());
            plek = plek.lerp(naar, 1.0 / rest);
            onderweg = true;
            treuzel = TREUZEL;
            staat = ZOEKT;
            if (meeste >= FOCUS && betrap(drager, oog, bron)) {
                focus.remove(drager.getUUID());
            }
        } else if (treuzel > 0) {
            treuzel--;
            staat = ZOEKT;
        } else {
            loop(t, spelers, toonVeldTot > level.getGameTime());
        }
        plek = opDeGrond(level, t, plek);
        // --- who stands in the light ---------------------------------------------------------------------------------------
        for (ServerPlayer p : spelers) {
            double dx = p.getX() - plek.x, dz = p.getZ() - plek.z;
            boolean inLicht = dx * dx + dz * dz <= STRAAL * STRAAL && Math.abs(p.getY() - plek.y) <= 8;
            // (a ring bearer is the business of the count above: it gives them exactly FOCUS ticks, wherever the light was)
            boolean ziet = inLicht && !Ring.om(p) && Ring.oogZiet(p) && !bijRustpunt(p) && vrijZicht(level, oog, p, bron);
            int g = gezien.getOrDefault(p.getUUID(), 0);
            g = ziet ? g + 1 : Math.max(0, g - 1);
            if (g <= 0) {
                gezien.remove(p.getUUID());
                continue;
            }
            gezien.put(p.getUUID(), g);
            if (ziet && g == 1) {
                p.sendOverlayMessage(Component.translatable("quest.guhs.ringh5.oog.gezien").withStyle(ChatFormatting.RED));
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0f, 1.6f);
            }
            if (g >= GENADE && betrap(p, oog, bron)) {
                gezien.remove(p.getUUID());
                treuzel = TREUZEL;
            }
        }
        gezien.keySet().removeIf(id -> spelers.stream().noneMatch(p -> p.getUUID().equals(id)));
        return staat;
    }

    /** The light on its line: the zone somebody is in, one run each when both are, else the idle round. */
    private void loop(Terrein t, List<ServerPlayer> spelers, boolean toonVeld) {
        boolean inA = toonVeld, inB = false;
        for (ServerPlayer p : spelers) {
            inA |= t.in(Plekken.ZONE_A, p.position());
            inB |= t.in(Plekken.ZONE_B, p.position());
        }
        Zone wil = zone == Zone.A && inA ? Zone.A : zone == Zone.B && inB ? Zone.B : inA ? Zone.A : inB ? Zone.B : Zone.RUST;
        if (wil != zone) {
            naar(wil);
        }
        Lijn lijn = lijn(zone);
        if (onderweg) {
            Vec3 doel = lijn.op(s);
            Vec3 d = new Vec3(doel.x - plek.x, 0, doel.z - plek.z);
            double afstand = d.length();
            if (afstand <= REIS) {
                plek = doel;
                onderweg = false;
            } else {
                plek = plek.add(d.scale(REIS / afstand));
            }
            return;
        }
        if (zone == Zone.RUST) {
            s = (s + RUST) % lijn.lengte();
            plek = lijn.op(s);
            return;
        }
        s += richting * SNELHEID;
        boolean einde = false;
        if (s >= lijn.lengte()) {
            s = lijn.lengte();
            richting = -1;
            einde = true;
        } else if (s <= 0) {
            s = 0;
            richting = 1;
            einde = true;
        }
        plek = lijn.op(s);
        if (einde && inA && inB) {
            naar(zone == Zone.A ? Zone.B : Zone.A);   // (somebody in both: one run each, in turn)
        }
    }

    /** The light goes to another line: to the end of it that is nearest. */
    private void naar(Zone nieuw) {
        zone = nieuw;
        onderweg = true;
        Lijn lijn = lijn(nieuw);
        if (nieuw == Zone.RUST) {
            s = 0;
            double beste = Double.MAX_VALUE;
            for (int i = 0; i < lijn.punten().size() - 1; i++) {
                double d = plek == null ? 0 : lijn.punten().get(i).distanceToSqr(plek);
                if (d < beste) {
                    beste = d;
                    s = lijn.tot()[i];
                }
            }
            return;
        }
        boolean begin = plek == null || lijn.op(0).distanceToSqr(plek) <= lijn.op(lijn.lengte()).distanceToSqr(plek);
        s = begin ? 0 : lijn.lengte();
        richting = begin ? 1 : -1;
    }

    private Lijn lijn(Zone z) {
        return z == Zone.A ? lijnA : z == Zone.B ? lijnB : lijnRust;
    }

    /** The light lies on what is under it: the first floor (or roof) from above. */
    private static Vec3 opDeGrond(ServerLevel level, Terrein t, Vec3 plek) {
        int boven = t.nul().getY() + Plekken.VUUR_KAMP.getY() + 7;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = (int) Math.floor(plek.x), z = (int) Math.floor(plek.z);
        for (int y = boven; y >= boven - 14; y--) {
            if (level.getBlockState(pos.set(x, y, z)).blocksMotion()) {
                return new Vec3(plek.x, y + 1, plek.z);
            }
        }
        return plek;
    }

    /**
     * At your own rest point you are safe: within {@link #RUST_VEILIG} blocks of it nothing in the valley sees you (so
     * whoever was just put back there can catch their breath, wherever the light happens to be).
     */
    public static boolean bijRustpunt(ServerPlayer p) {
        Rustpunten.Punt punt = Ring.rustpunt(p);
        return punt != null && punt.dim() == p.level().dimension() && punt.plek().distanceToSqr(p.position()) <= RUST_VEILIG * RUST_VEILIG;
    }

    /** Is there a free line from the Eye to this player's head or to their middle? */
    public static boolean vrijZicht(ServerLevel level, Vec3 oog, ServerPlayer p, Entity bron) {
        return vrij(level, oog, p.getEyePosition(), bron) || vrij(level, oog, p.position().add(0, p.getBbHeight() * 0.5, 0), bron);
    }

    private static boolean vrij(ServerLevel level, Vec3 van, Vec3 naar, Entity bron) {
        return level.clip(new ClipContext(van, naar, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bron)).getType() == HitResult.Type.MISS;
    }

    /** Seen: back to the last rest point. False when the player may not be moved right now (a scene, a talk). */
    private boolean betrap(ServerPlayer p, Vec3 oog, Entity bron) {
        if (!Ring.terugNaarRustpunt(p, Ring.OOG, oog)) {
            return false;
        }
        betrapt++;
        Ring.behaald(p, "ring_h5_betrapt");
        bron.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.5f, 1.6f);
        return true;
    }

    /** The light sweeps het Asveld until this game time, also when nobody is in it (a scene shows how it goes). */
    public void toonVeld(long totGameTime) {
        toonVeldTot = totGameTime;
    }

    /** (a scene, tests, the dev command) the light jumps here and stays for a while. */
    public void zet(Vec3 hier, int ticks) {
        plek = hier;
        treuzel = ticks;
        onderweg = true;
    }
}
