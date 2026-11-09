package nl.juiced.guhs.feature.vadsrijk.zonepoort;

import java.util.Map;
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
    /** The level each zone hands you over at (the next zone's entry level): the soft cap while the zone after it is closed. */
    static final int[] PLAFOND = {0, 20, 30, 50, 56, 60, 0};
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

    /** Is zone {@code zone} closed for this player? (zone 0 = Knuffelkamp, never) */
    public static boolean dicht(ServerPlayer p, int zone) {
        return zone > open() && !vrij(p);
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
        return dicht(p, plekZone(plek)) ? K + "reis." + plek : null;
    }

    /** For an airship line: the lang key when its destination zone is closed, or null. */
    @Nullable
    public static String lijnSlot(ServerPlayer p, String naarZone) {
        int z = zoneId(naarZone);
        return dicht(p, z) ? K + "lijn." + z : null;
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
        return new QuestStap(q.id(), true, Component.translatable(K + "volg.titel"), Component.translatable(K + "volg.wat", zoneNaam(z)),
                Component.translatable(K + "volg.waar"), null, null);
    }

    /**
     * The giver of the next main quest (the first main quest not done) when that quest lies in a closed zone and its earlier quests
     * are done: the captain's "the ship sails soon" line. Null otherwise.
     */
    @Nullable
    public static Praat.Regel binnenkortRegel(ServerPlayer p, Entity npc, String npcId) {
        Quest q = volgendeHoofd(p);
        if (q == null || !questDicht(p, q) || !q.gever().equals(npcId) || Questlog.status(p, q.id()) != 0) {
            return null;
        }
        for (String na : q.na()) {
            if (Quests.van(na) != null && !Questlog.klaar(p, na)) {
                return null;
            }
        }
        return new Praat.Regel(npc, "", K + "schip", zoneNaam(ZoneBoeken.zoneVan(q.id())));
    }

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
        return Component.translatable(K + "dungeon", d.naam(), zoneNaam(d.zone()));
    }

    // ---- the soft cap -------------------------------------------------------------------------------------------------------------

    /** The soft cap level now for this player (0 = none: every zone open, or bypass). */
    public static int plafond(ServerPlayer p) {
        int o = open();
        if (o >= 6 || vrij(p)) {
            return 0;
        }
        return PLAFOND[o];
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
                p.sendSystemMessage(Component.translatable(K + "plafond", cap, zoneNaam(open() + 1), BANK_LEVELS).withStyle(ChatFormatting.GOLD));
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
        }
    }

    /** One player: remember an open spot, or send him back from a closed zone. True = he was in a closed zone. */
    public static boolean controleer(ServerLevel sl, ServerPlayer p, long t) {
        if (vrij(p) || RijkScenes.bezig(p) || p.isDeadOrDying()) {
            return false;
        }
        int z = zoneOp(p.getX(), p.getY(), p.getZ());
        if (!dicht(p, z)) {
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
        MutableComponent m = Component.translatable(K + "grens", zoneNaam(zone));
        p.sendOverlayMessage(m.copy().withStyle(ChatFormatting.GOLD));
        Long vorig = MELDING.get(p.getUUID());
        if (vorig == null || t - vorig > 20 * 30 || t < vorig) {
            MELDING.put(p.getUUID(), t);
            p.sendSystemMessage(m.withStyle(ChatFormatting.GOLD));
        }
    }

    static void vergeet(UUID id) {
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
