package nl.juiced.guhs.feature.vadsrijk.zonepoort;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeon;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeons;
import nl.juiced.guhs.feature.vadsrijk.indeling.Barrieres;
import nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * X-ZONE1: the zone gate. The server opens zones 1..{@link #open()} ({@link ZonePoortConfig}, {@code /guhs vadsrijk zones open
 * <n>}); everything beyond is "binnenkort" for players:
 * <ul>
 *   <li>every way in says no in words: the airship lines ({@code Poorten.dicht}), the travel NPCs / bells / travel map / bond
 *       teleports ({@code QuestMotor.reisSlot}, {@code Reiskaart.plek}), the quests of a closed zone are never offered
 *       ({@code Questlog.beschikbaar}), the dungeons of a closed zone refuse ({@code DungeonRij.weiger});</li>
 *   <li>the quest tracker shows "Wordt vervolgd" instead of the next zone's first main quest ({@link #vervolgd}), and its giver
 *       (the captain) says the ship sails soon ({@link #binnenkortRegel});</li>
 *   <li>the safety net ({@link #tik}): whoever still ends up in a closed zone (walking, swimming, elytra, ender pearl, a story
 *       teleport, a login) is pushed back, or put back on his last open spot (De Haven when there is none);</li>
 *   <li>the soft cap ({@link #plafond}): while the next zone is closed the level stops at that zone's entry level; extra xp is
 *       banked (at most {@link #BANK_LEVELS} levels' worth) and paid out as a +100 % bonus on later xp once the gate is raised.</li>
 * </ul>
 * Ops (permission 2), creative and spectator bypass everything (testing). Raising the gate needs nothing else: quest states never
 * change while a zone is closed, so players simply continue.
 */
public final class ZonePoort {
    private ZonePoort() {
    }

    public static final String K = "vadsrijk.zonepoort.";
    /** The bank holds at most this many levels' worth of xp above the soft cap. */
    public static final int BANK_LEVELS = 3;
    /** The level each zone hands you over at (the next zone's entry level). */
    static final int[] PLAFOND = {0, 20, 30, 50, 56, 60, 0};
    /**
     * 1.10.2: the top level of each zone (index = zone; 0 = the camp counts as zone 1). The per-player level cap is the top of the
     * highest zone you ENTERED + {@link #CAP_EXTRA}: zone 1 -> 25, 2 -> 35, 3 -> 55, 4 -> 61, 5 -> 65, 6 -> 70 (= Levels.MAX: no cap).
     */
    static final int[] TOP = {20, 20, 30, 50, 56, 60, 70};
    public static final int CAP_EXTRA = 5;
    /** 1.10.2 (tests / the quest robot): players for whom the Verkenningsboek % of the per-player gate counts as met. */
    public static final Set<UUID> BOEK_VRIJ = ConcurrentHashMap.newKeySet();
    /** Tests: never bypass (the gametest mock player is creative). */
    public static boolean TEST_STRENG;

    // ---- the gate ---------------------------------------------------------------------------------------------------------------

    /** The highest open zone (1..6). */
    public static int open() {
        return ZonePoortConfig.open();
    }

    public static boolean alleOpen() {
        return open() >= 6;
    }

    /** Ops, creative and spectator walk through the gate (testing). */
    public static boolean vrij(ServerPlayer p) {
        if (TEST_STRENG) {
            return false;
        }
        return p.isCreative() || p.isSpectator() || p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /**
     * Is zone {@code zone} closed for this player? (zone 0 = Knuffelkamp, never.) 1.10.2: closed by the server gate
     * ({@link #serverDicht}) OR by the per-player gate ({@link #toegang}: zone N's main story + {@link ZonePoortConfig#boek()} % of its
     * Verkenningsboek).
     */
    public static boolean dicht(ServerPlayer p, int zone) {
        return !vrij(p) && (zone > open() || !toegang(p, zone));
    }

    /** Closed by the server gate only ("binnenkort"). */
    public static boolean serverDicht(ServerPlayer p, int zone) {
        return zone > open() && !vrij(p);
    }

    // ---- 1.10.2: the per-player gate ------------------------------------------------------------------------------------------------

    /**
     * May this player enter zone {@code zone} (server gate aside)? Zones 0/1 always. Granted for good (stored "toegang") when he was
     * already in that zone or started one of its quests (grandfathering), or once zone {@code zone - 1}'s main story is done AND its
     * Verkenningsboek is at least {@link ZonePoortConfig#boek()} %.
     */
    public static boolean toegang(ServerPlayer p, int zone) {
        if (zone <= 1) {
            return true;
        }
        CompoundTag b = bank(Spelers.data(p));
        if (b.getIntOr("toegang", 0) >= zone) {
            return true;
        }
        String k = p.getUUID() + "/" + zone;
        long t = p.level().getGameTime();
        Long nee = NEE.get(k);
        if (nee != null && t < nee && t >= nee - 20) {
            return false;   // (a "no" is remembered for a second: quest lists ask this for every quest)
        }
        if (hoogste(p) >= zone || begonnen(p, zone) || (verhaalKlaar(p, zone - 1) && boekOk(p, zone - 1))) {
            b.putInt("toegang", Math.max(zone, b.getIntOr("toegang", 0)));
            NEE.remove(k);
            return true;
        }
        NEE.put(k, t + 20);
        return false;
    }

    private static final Map<String, Long> NEE = new ConcurrentHashMap<>();

    /** Is zone n's main story done up to the trip to zone n + 1 (the airship line's own story check)? */
    public static boolean verhaalKlaar(ServerPlayer p, int n) {
        String lijn = switch (n) {
            case 1 -> "z1_z2";
            case 2 -> "z2_z3";
            case 3 -> "z3_z4";
            case 4 -> "z4_z5";
            default -> null;
        };
        if (lijn != null) {
            var g = nl.juiced.guhs.feature.vadsrijk.luchtschip.Poorten.POORTEN.get(lijn);
            if (g != null) {
                return g.verhaalKlaar().test(p);
            }
        }
        if (n == 5) {
            return nl.juiced.guhs.feature.vadsrijk.zone6.verhaal.Z6Verhaal.magZien(p);
        }
        for (Quest q : Quests.alle().values()) {   // (fallback: every main quest of zone n done)
            if (q.hoofd() && ZoneBoeken.zoneVan(q.id()) == n && !Questlog.klaar(p, q.id())) {
                return false;
            }
        }
        return true;
    }

    private record Boek(long tik, int procent) {
    }

    private static final Map<String, Boek> BOEK_CACHE = new ConcurrentHashMap<>();

    /** Zone n's Verkenningsboek % for this player (the number the book shows; cached 5 s). */
    public static int boekProcent(ServerPlayer p, int n) {
        long t = p.level().getGameTime();
        String k = p.getUUID() + "/" + n;
        Boek c = BOEK_CACHE.get(k);
        if (c != null && t - c.tik() < 100 && t >= c.tik()) {
            return c.procent();
        }
        int pct = 100;
        try {
            var zb = nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.zone("zone" + n);
            if (zb != null) {
                pct = nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.tel(p, zb).procent();
            }
        } catch (RuntimeException e) {
            pct = 100;   // (never lock someone out on a broken book)
        }
        BOEK_CACHE.put(k, new Boek(t, pct));
        return pct;
    }

    public static boolean boekOk(ServerPlayer p, int n) {
        return BOEK_VRIJ.contains(p.getUUID()) || boekProcent(p, n) >= ZonePoortConfig.boek();
    }

    /** Did this player start (or finish) any quest of that zone? (grandfathering) */
    static boolean begonnen(ServerPlayer p, int zone) {
        for (Quest q : Quests.alle().values()) {
            if (ZoneBoeken.zoneVan(q.id()) == zone && Questlog.status(p, q.id()) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * The highest zone this player has ENTERED (1..6; the camp counts as 1): stored when he stands in an open zone, plus credit for
     * every zone of which he finished a quest (players from before 1.10.2).
     */
    public static int hoogste(ServerPlayer p) {
        CompoundTag b = bank(Spelers.data(p));
        int h = Math.max(1, b.getIntOr("hoogste", 0));
        if (h < 6 && !b.getBooleanOr("hoogste_quests", false)) {
            for (Quest q : Quests.alle().values()) {
                int z = ZoneBoeken.zoneVan(q.id());
                if (z > h && Questlog.klaar(p, q.id())) {
                    h = z;
                }
            }
            b.putInt("hoogste", h);
            b.putBoolean("hoogste_quests", true);   // (the quest credit once; later only real visits raise it)
        }
        return Math.min(6, h);
    }

    /** A finished quest of zone z counts as having been there (only raises; Questlog.voltooi). */
    public static void questKlaar(ServerPlayer p, String quest) {
        hoogste(p);   // (the one-time credit for older saves first)
        betreden(p, ZoneBoeken.zoneVan(quest));
    }

    /** Remembers that this player stands in zone z now (only raises). */
    static void betreden(ServerPlayer p, int z) {
        CompoundTag b = bank(Spelers.data(p));
        if (z > b.getIntOr("hoogste", 0)) {
            b.putInt("hoogste", Math.min(6, z));
        }
    }

    /** (tests / dev) sets the highest entered zone (and forgets the access record above it when lower). */
    public static void zetHoogste(ServerPlayer p, int z) {
        CompoundTag b = bank(Spelers.data(p));
        b.putInt("hoogste", Math.max(0, Math.min(6, z)));
        b.putBoolean("hoogste_quests", true);
        if (b.getIntOr("toegang", 0) > z) {
            b.putInt("toegang", Math.max(1, z));
        }
        vergeetCache(p.getUUID());
    }

    /** (tests / a quest step) forget the cached book % and "no" answers of this player. */
    public static void vergeetCache(UUID id) {
        BOEK_CACHE.keySet().removeIf(k -> k.startsWith(id + "/"));
        NEE.keySet().removeIf(k -> k.startsWith(id + "/"));
    }

    /** "✔" / "✘" in green / red. */
    static Component vink(boolean ok) {
        return Component.literal(ok ? "✔" : "✘").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    /** The arguments of the "eis" line: zone name, ✔/✘ story, previous zone, ✔/✘ book, book %, needed %. */
    public static Object[] eisArgs(ServerPlayer p, int zone) {
        int n = Math.max(1, zone - 1);
        boolean boek = boekOk(p, n);
        return new Object[] {zoneNaam(zone), vink(verhaalKlaar(p, n)), zoneNaam(n), vink(boek), boekProcent(p, n), ZonePoortConfig.boek()};
    }

    /** "Naar De Splinterpieken: ✔ hoofdverhaal De Knuffelzwammen · ✘ Verkenningsboek 62% / 75%". */
    public static MutableComponent eisTekst(ServerPlayer p, int zone) {
        return Component.translatable(K + "eis", eisArgs(p, zone));
    }

    /** Is this gate key the per-player gate ("vadsrijk.zonepoort.eis.<zone>")? */
    public static boolean isEis(@Nullable String key) {
        return key != null && key.startsWith(K + "eis.");
    }

    static int eisZone(String key) {
        try {
            return Integer.parseInt(key.substring((K + "eis.").length()));
        } catch (RuntimeException e) {
            return 2;
        }
    }

    /** The text of a gate key: the per-player line with its progress, else the plain "binnenkort" line. */
    public static MutableComponent tekst(ServerPlayer p, String key) {
        return isEis(key) ? eisTekst(p, eisZone(key)) : Component.translatable(key);
    }

    /** The arguments for a gate key in a talk line (the level for the old keys). */
    public static Object[] args(ServerPlayer p, String key) {
        return isEis(key) ? eisArgs(p, eisZone(key)) : new Object[] {Spelers.level(p)};
    }

    /** The lang key to show for a gate key (the "eis" keys share one text). */
    public static String sleutel(String key) {
        return isEis(key) ? K + "eis" : key;
    }

    public static Component zoneNaam(int zone) {
        return Component.translatable(K + "zone." + Math.max(1, Math.min(6, zone)));
    }

    /** The zone a travel destination lies in (QuestMotor.REIZEN / Reiskaart.PLEKKEN ids); 0 = unknown / the camp. */
    public static int plekZone(String plek) {
        return switch (plek) {
            case "zwamzoet" -> 1;
            case "kandijkom" -> 2;
            case "schemerwortel" -> 3;
            case "lampionkust" -> 4;
            case "hoogvads" -> 5;
            case "zijdeland" -> 6;
            default -> 0;
        };
    }

    /** "kamp" = 0, "z1".."z6" = 1..6 (the airship harbours' zone ids). */
    public static int zoneId(String id) {
        if (id != null && id.length() >= 2 && id.charAt(0) == 'z' && Character.isDigit(id.charAt(1))) {
            return id.charAt(1) - '0';
        }
        return 0;
    }

    /** The zone at a world spot (tile boxes first, then the barrier free areas); 0 = the camp, the open sea or outside. */
    public static int zoneOp(double x, double y, double z) {
        VadsrijkZones.Zone zo = VadsrijkZones.zoneVan(x, y, z);
        if (zo != null) {
            return zo == VadsrijkZones.Zone.KAMP ? 0 : zoneId(zo.id);
        }
        for (Barrieres.Vrij v : Barrieres.VRIJ) {
            if (v.bevat(x, y, z)) {
                return zoneId(v.zone());
            }
        }
        return 0;
    }

    // ---- travel, quests, dungeons -------------------------------------------------------------------------------------------------

    /** For QuestMotor.reisSlot: the lang key why this destination is closed (the captain's line), or null. */
    @Nullable
    public static String reisSlot(ServerPlayer p, String plek) {
        int z = plekZone(plek);
        if (serverDicht(p, z)) {
            return K + "reis." + plek;
        }
        return dicht(p, z) ? K + "eis." + z : null;   // 1.10.2: the per-player gate
    }

    /** For an airship line: the lang key when its destination zone is closed, or null. */
    @Nullable
    public static String lijnSlot(ServerPlayer p, String naarZone) {
        int z = zoneId(naarZone);
        if (serverDicht(p, z)) {
            return K + "lijn." + z;
        }
        return dicht(p, z) ? K + "eis." + z : null;   // 1.10.2: the per-player gate
    }

    public static boolean isPoortSlot(@Nullable String key) {
        return key != null && key.startsWith(K);
    }

    /** A quest of a closed zone: never offered, never shown as the next step. */
    public static boolean questDicht(ServerPlayer p, Quest q) {
        return dicht(p, ZoneBoeken.zoneVan(q.id()));
    }

    /** For the quest tracker: "Wordt vervolgd" instead of the next main quest when that one lies in a closed zone (not started). */
    @Nullable
    public static QuestStap vervolgd(ServerPlayer p, Quest q) {
        if (!questDicht(p, q) || Questlog.status(p, q.id()) != 0) {
            return null;
        }
        int z = ZoneBoeken.zoneVan(q.id());
        if (!serverDicht(p, z)) {   // 1.10.2: the server has it open, the player not yet: explore zone z-1 to the book %
            int n = Math.max(1, z - 1);
            if (!verhaalKlaar(p, n)) {
                return null;   // (the story itself is the next step)
            }
            return new QuestStap(q.id(), true, Component.translatable(K + "verken.titel", zoneNaam(n), ZonePoortConfig.boek()),
                    Component.translatable(K + "verken.wat", boekProcent(p, n), ZonePoortConfig.boek()),
                    Component.translatable(K + "verken.waar", zoneNaam(z)), null, null);
        }
        return new QuestStap(q.id(), true, Component.translatable(K + "volg.titel"), Component.translatable(K + "volg.wat", zoneNaam(z)),
                Component.translatable(K + "volg.waar"), null, null);
    }

    /**
     * The giver of the next main quest (the first main quest not done), or the captain whose ship leaves for that zone
     * ({@link #VERTREK}), when that quest lies in a closed zone and its earlier quests are done: the "the ship sails soon" line.
     * Null otherwise.
     */
    @Nullable
    public static Praat.Regel binnenkortRegel(ServerPlayer p, Entity npc, String npcId) {
        Quest q = volgendeHoofd(p);
        if (q == null || !questDicht(p, q) || Questlog.status(p, q.id()) != 0) {
            return null;
        }
        int zone = ZoneBoeken.zoneVan(q.id());
        if (!q.gever().equals(npcId) && !npcId.equals(VERTREK.get(zone))) {
            return null;
        }
        for (String na : q.na()) {
            if (Quests.van(na) != null && !Questlog.klaar(p, na)) {
                return null;
            }
        }
        if (!serverDicht(p, zone)) {   // 1.10.2: open on the server, not yet for this player: both conditions with his progress
            return new Praat.Regel(npc, "", K + "eis", eisArgs(p, zone));
        }
        return new Praat.Regel(npc, "", K + "schip", zoneNaam(zone));
    }

    /**
     * 1.10.2: the captain whose ship leaves for zone N, who also says "the ship sails soon" at the end of zone N-1 when he is not the
     * giver of zone N's first main quest (zone 4's is given by Ordemeester Wasja, but the ship leaves from Kapitein's Oosttaksteiger).
     */
    static final Map<Integer, String> VERTREK = Map.of(2, "luchtschipper", 3, "z2_kapitein", 4, "z3_kapitein");

    @Nullable
    static Quest volgendeHoofd(ServerPlayer p) {
        for (Quest q : Quests.alle().values()) {
            if (q.hoofd() && !Questlog.klaar(p, q.id())) {
                return q;
            }
        }
        return null;
    }

    /** For DungeonRij.weiger: why a dungeon of a closed zone may not be entered/queued, or null. */
    @Nullable
    public static Component dungeonDicht(ServerPlayer p, String dungeon) {
        Dungeon d = Dungeons.van(dungeon);
        if (d == null || !dicht(p, d.zone())) {
            return null;
        }
        if (!serverDicht(p, d.zone())) {
            return Component.translatable(K + "dungeon.eis", d.naam(), eisTekst(p, d.zone()));
        }
        return Component.translatable(K + "dungeon", d.naam(), zoneNaam(d.zone()));
    }

    // ---- the soft cap -------------------------------------------------------------------------------------------------------------

    /**
     * The level cap now for this player (0 = none: bypass, or level 70 is allowed). 1.10.2: per player, the top level of the highest
     * zone he has ENTERED + {@link #CAP_EXTRA} (25 for someone who never set foot in zone 2), whatever the server gate.
     */
    public static int plafond(ServerPlayer p) {
        if (vrij(p)) {
            return 0;
        }
        int cap = Math.min(Levels.MAX, TOP[hoogste(p)] + CAP_EXTRA);
        return cap >= Levels.MAX ? 0 : cap;
    }

    static CompoundTag bank(SpelerData d) {
        return d.extra.computeIfAbsent("zonepoort", k -> new CompoundTag());
    }

    /** Banked xp: "g" = generic, "k_<class>" = that class. */
    public static long gespaard(SpelerData d, @Nullable String klasse) {
        return bank(d).getLongOr(klasse == null ? "g" : "k_" + klasse, 0L);
    }

    /**
     * Called from Spelers.geefXp for one gain of {@code n} xp (after the bonuses) to the generic level ({@code klasse} null) or to a
     * class: returns how much really goes on. Under the soft cap the rest is banked; without a cap the bank pays out up to
     * {@code n} extra (a +100 % bonus until it is empty).
     */
    public static long knip(ServerPlayer p, SpelerData d, long n, @Nullable String klasse) {
        if (n <= 0) {
            return n;
        }
        String sleutel = klasse == null ? "g" : "k_" + klasse;
        CompoundTag b = bank(d);
        long pot = b.getLongOr(sleutel, 0L);
        int cap = plafond(p);
        if (cap <= 0) {
            if (pot <= 0) {
                return n;
            }
            long extra = Math.min(pot, n);
            b.putLong(sleutel, pot - extra);
            return n + extra;
        }
        long capXp = klasse == null ? Levels.totaalVoorGeneriek(cap) : Levels.totaalVoor(cap);
        long nu = klasse == null ? d.generiekXp : d.xp(klasse);
        long ruimte = Math.max(0, capXp - nu);
        long erbij = Math.min(n, ruimte);
        long over = n - erbij;
        if (over > 0) {
            long max = (klasse == null ? Levels.totaalVoorGeneriek(cap + BANK_LEVELS) : Levels.totaalVoor(cap + BANK_LEVELS)) - capXp;
            b.putLong(sleutel, Math.min(max, pot + over));
            if (klasse == null && b.getIntOr("gemeld", 0) != cap) {
                b.putInt("gemeld", cap);
                p.sendSystemMessage(Component.translatable(K + "plafond", cap, zoneNaam(Math.min(6, hoogste(p) + 1)), BANK_LEVELS).withStyle(ChatFormatting.GOLD));
            }
        }
        return erbij;
    }

    /** At the soft cap now (the HUD shows a full bar, the menu "hoogste level voor nu")? */
    public static boolean opPlafond(ServerPlayer p, SpelerData d) {
        int cap = plafond(p);
        return cap > 0 && d.level() >= cap;
    }

    // ---- the safety net -------------------------------------------------------------------------------------------------------------

    private static final Map<UUID, Vec3> LAATST_OPEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> DUWT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> MELDING = new ConcurrentHashMap<>();
    /** Within this distance of the last open spot a player is pushed back; further (or after 3 s of pushing) he is put back. */
    static final double DUW_AFSTAND = 12;

    /** Every 2 ticks from the server tick. */
    static void tik(MinecraftServer server) {
        long t = server.getTickCount();
        if (t % 2 != 0 || alleOpen()) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            controleer(sl, p, t);
            if (t % 100 == 0) {
                meldToegang(p);
            }
        }
    }

    /** 1.10.2: "De weg naar <zone> is open!" once per zone, when the per-player gate opens (not for zones he already visited). */
    static void meldToegang(ServerPlayer p) {
        if (vrij(p)) {
            return;
        }
        CompoundTag b = bank(Spelers.data(p));
        int gemeld = b.getIntOr("toegang_gemeld", 1);
        for (int z = gemeld + 1; z <= Math.min(6, open()); z++) {
            if (hoogste(p) >= z) {
                b.putInt("toegang_gemeld", z);
                continue;
            }
            if (!toegang(p, z)) {
                return;
            }
            b.putInt("toegang_gemeld", z);
            p.sendSystemMessage(Component.translatable(K + "weg_open", zoneNaam(z)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            p.sendOverlayMessage(Component.translatable(K + "weg_open", zoneNaam(z)).withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    /** One player: remember an open spot, or send him back from a closed zone. True = he was in a closed zone. */
    public static boolean controleer(ServerLevel sl, ServerPlayer p, long t) {
        if (vrij(p) || RijkScenes.bezig(p) || p.isDeadOrDying()) {
            return false;
        }
        int z = zoneOp(p.getX(), p.getY(), p.getZ());
        if (!dicht(p, z)) {
            if (z > 0) {
                betreden(p, z);   // 1.10.2: the highest zone entered (the level cap)
            }
            DUWT.remove(p.getUUID());
            if (t % 10 == 0 && (p.onGround() || p.isInWater()) && !p.isPassenger()) {
                LAATST_OPEN.put(p.getUUID(), p.position());
            }
            return false;
        }
        Vec3 terug = LAATST_OPEN.get(p.getUUID());
        int keer = DUWT.merge(p.getUUID(), 1, Integer::sum);
        if (terug != null && keer < 30 && terug.distanceTo(p.position()) < DUW_AFSTAND && !p.isFallFlying() && !p.isPassenger()) {
            Vec3 naar = new Vec3(terug.x - p.getX(), 0, terug.z - p.getZ());
            if (naar.lengthSqr() > 1e-4) {
                Vec3 d = naar.normalize().scale(0.45);
                p.setDeltaMovement(d.x, Math.max(0.1, p.getDeltaMovement().y), d.z);
                p.hurtMarked = true;
            }
        } else {
            zetTerug(sl, p, terug);
        }
        meld(p, z, t);
        return true;
    }

    /** Puts the player back on his last open spot (De Haven when there is none), off any mount, no fall damage. */
    static void zetTerug(ServerLevel sl, ServerPlayer p, @Nullable Vec3 terug) {
        if (p.isPassenger()) {
            p.stopRiding();
        }
        if (p.isFallFlying()) {
            p.stopFallFlying();
        }
        Vec3 waar = terug != null ? terug : veiligeHaven(sl);
        p.teleport(new TeleportTransition(sl, waar, Vec3.ZERO, p.getYRot(), p.getXRot(), TeleportTransition.DO_NOTHING));
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        DUWT.remove(p.getUUID());
        sl.playSound(null, BlockPos.containing(waar), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    static Vec3 veiligeHaven(ServerLevel sl) {
        try {
            return nl.juiced.guhs.feature.vadsrijk.wereld.WereldReizen.veiligeLanding(sl, nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken.HAVEN_LANDING);
        } catch (RuntimeException | LinkageError e) {
            return Vec3.atBottomCenterOf(nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken.HAVEN_LANDING);
        }
    }

    private static void meld(ServerPlayer p, int zone, long t) {
        MutableComponent m = serverDicht(p, zone) ? Component.translatable(K + "grens", zoneNaam(zone)) : eisTekst(p, zone);
        p.sendOverlayMessage(m.copy().withStyle(ChatFormatting.GOLD));
        Long vorig = MELDING.get(p.getUUID());
        if (vorig == null || t - vorig > 20 * 30 || t < vorig) {
            MELDING.put(p.getUUID(), t);
            p.sendSystemMessage(m.withStyle(ChatFormatting.GOLD));
        }
    }

    static void vergeet(UUID id) {
        vergeetCache(id);
        LAATST_OPEN.remove(id);
        DUWT.remove(id);
        MELDING.remove(id);
    }

    /** (tests) the remembered open spot. */
    public static void zetLaatstOpen(ServerPlayer p, @Nullable Vec3 v) {
        if (v == null) {
            LAATST_OPEN.remove(p.getUUID());
        } else {
            LAATST_OPEN.put(p.getUUID(), v);
        }
    }
}
