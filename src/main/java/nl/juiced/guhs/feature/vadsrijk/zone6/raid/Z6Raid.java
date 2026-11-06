package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vfx;

/**
 * U-Z6RAID: the raid "De Sterrendovende Toren" itself: the group, the lock + queue, the floors, wipes, checkpoints, the chest of
 * every clear.
 * <p><b>Why a lock (not shared like the rest of the game):</b> the bosses scale with the group that STARTS (2-10), mechanics count
 * per group (plates, mirrors, beacons) and a wipe resets a floor; two groups in one arena would break all three. So the tower
 * is ONE group at a time; the others wait in De Wachtzaal (the pocket's ground floor) in a queue. It is still one shared
 * world: everyone can walk into the Wachtzaal, see the board, and watch the group start.
 * <ul>
 *   <li><b>Start</b>: 2-10 players stand on the start circle in the Wachtzaal for 3 s (party members standing in the Wachtzaal
 *       come along). Solo: "neem minstens één vriendje mee". When the tower is busy, the circle puts you in the queue as a group.
 *       The next group is called when the tower is free; whoever of it is in the Wachtzaal goes up (at least 2, else skipped).</li>
 *   <li><b>Floors</b>: a floor's boss only spawns when the group is on that floor ({@link #magSpawnen}); cheering it up opens the
 *       sterrenvlies gate of the stair up; the landing of the current floor is the checkpoint.</li>
 *   <li><b>Fainting</b> (mild): you wake at the landing of the current floor (Duizelig as everywhere) and may run back in. Eénvads
 *       knights do NOT retire inside the raid (counts as "zacht"), they only lose the "Sterrenaansteker" run.</li>
 *   <li><b>Wipe</b>: when nobody of the group stands alive in the arena of a started fight for 3 s, the floor resets (boss full,
 *       adds gone), wipe counter +1. No wipe limit.</li>
 *   <li><b>Clear</b>: Ringeling cheered up = everyone of the group in the tower gets a chest (EVERY clear, no week limit), the stars
 *       light, Z6Verhaal.ringelingOpgevrolijkt(p) per player (soft dependency), titles. The tower frees 3 min later or when the
 *       group left.</li>
 * </ul>
 * Not saved (a restart frees the tower, like parties).
 */
public final class Z6Raid {
    public static final String[] BAZEN = {"", "zandwachter", "spiegelvizier", "sterrenbeelden", "ringeling"};
    public static final int START_TICKS = 60, LEEG_TICKS = 20 * 120, KLAAR_TICKS = 20 * 180, ROEP_TICKS = 20 * 60;

    /** The group in the tower. */
    public static final class Groep {
        public final List<UUID> leden = new ArrayList<>();
        public final Map<UUID, String> namen = new HashMap<>();
        public int n;
        /** The floor the group is on now (1-4; 5 = cleared). */
        public int verdieping = 1;
        public int wipes;
        public long start, gevechtStart = -1, klaarOp = -1, leegSinds = -1;
        public int leegInArena;
        public final Set<UUID> flauw = new HashSet<>();
        /** Dev "start with N players": made-up members (no player behind them; they only count for the scaling + the frames). */
        public final Set<UUID> nep = new HashSet<>();

        public boolean klaar() {
            return verdieping > 4;
        }
    }

    @Nullable
    static Groep groep;
    /** The queue: each entry a group of players (UUIDs). */
    static final List<List<UUID>> RIJ = new ArrayList<>();
    private static final Map<UUID, Long> OP_CIRKEL = new HashMap<>();
    private static long geroepenOp = -1;
    /** Gametests: the clears that were rewarded (count) and the last chests. */
    public static int clearsBeloond;

    private Z6Raid() {
    }

    // ---- queries ------------------------------------------------------------------------------------------------------------

    @Nullable
    public static Groep groep() {
        return groep;
    }

    /** The group size the bosses scale with (2 when nobody is in the tower: dev spawns). */
    public static int n() {
        return groep == null ? 2 : groep.n;
    }

    public static boolean inGroep(ServerPlayer p) {
        return groep != null && groep.leden.contains(p.getUUID());
    }

    public static boolean inToren(ServerPlayer p) {
        return Vadsrijk.in(p) && Z6RaidPlekken.inToren(p.getX(), p.getY(), p.getZ());
    }

    /** May this player fight the boss of floor k (in the group, in that arena)? Creative players without a group too (dev). */
    public static boolean doetMee(ServerPlayer p, int k) {
        if (!Z6RaidPlekken.inArena(k, p.getX(), p.getY(), p.getZ())) {
            return false;
        }
        return inGroep(p) || (p.isCreative() && (groep == null || DEV_MEE.contains(p.getUUID())));
    }

    /** Dev: creative players that fight along although not in the group. */
    public static final Set<UUID> DEV_MEE = new HashSet<>();

    /** Arenas asks: may the boss of floor k exist now? */
    public static boolean magSpawnen(int k) {
        return (groep != null && groep.verdieping == k) || DEV_SPAWN[k];
    }

    /** Dev: spawn this floor's boss without a group ({@code /guhs vadsrijk z6raid baas}). */
    static final boolean[] DEV_SPAWN = new boolean[5];

    public static String arenaId(int k) {
        return "z6raid:" + BAZEN[k];
    }

    // ---- the tick ----------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || sl.getGameTime() % 10 != 0) {
            return;
        }
        Z6RaidBouw.tick(sl);
        long nu = sl.getGameTime();
        poorten(sl);
        cirkel(sl, nu);
        if (groep != null) {
            groepTick(sl, nu);
        } else if (!RIJ.isEmpty()) {
            roep(sl, nu);
        }
        if (nu % 40 == 0) {
            wachtzaalMelding(sl);
        }
    }

    /** The tower door outside -> the Wachtzaal; the exit in the Wachtzaal -> outside; the light on the roof after a clear. */
    private static void poorten(ServerLevel sl) {
        BlockPos poort = Z6RaidPlekken.poort(), uit = Z6RaidPlekken.uitgang();
        for (ServerPlayer p : new ArrayList<>(sl.players())) {
            if (p.isSpectator() || RijkScenes.bezig(p)) {
                continue;
            }
            if (Math.abs(p.getX() - (poort.getX() + 0.5)) < 2 && Math.abs(p.getZ() - (poort.getZ() + 0.5)) < 2 && p.getY() >= poort.getY() - 1
                    && p.getY() < poort.getY() + 3) {
                naar(p, Vec3.atBottomCenterOf(Z6RaidPlekken.midden(0).offset(8, 0, 0)), -90);
                p.sendSystemMessage(Component.translatable("vadsrijk.z6raid.wachtzaal.welkom").withStyle(ChatFormatting.LIGHT_PURPLE));
                continue;
            }
            if (p.blockPosition().closerThan(uit, 1.6) || (groep != null && groep.klaar() && p.blockPosition().closerThan(Z6RaidPlekken.midden(4), 1.5))) {
                naar(p, Vec3.atBottomCenterOf(Z6RaidPlekken.buiten()), 90);
            }
        }
    }

    public static void naar(ServerPlayer p, Vec3 v, float yaw) {
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        if (sl == null) {
            return;
        }
        sl.getChunk(BlockPos.containing(v));
        p.teleport(new TeleportTransition(sl, v, Vec3.ZERO, yaw, 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        sl.playSound(null, BlockPos.containing(v), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.4f);
    }

    /** The start circle: 2+ players for 3 s = start (or into the queue when busy). */
    private static void cirkel(ServerLevel sl, long nu) {
        Vec3 c = Z6RaidPlekken.start();
        List<ServerPlayer> op = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.isAlive() && Math.abs(p.getY() - c.y) < 2 && p.position().multiply(1, 0, 1).distanceTo(c.multiply(1, 0, 1)) <= Z6RaidPlekken.START_R
                    && !inGroep(p)) {
                op.add(p);
            }
        }
        OP_CIRKEL.keySet().retainAll(op.stream().map(ServerPlayer::getUUID).toList());
        for (ServerPlayer p : op) {
            OP_CIRKEL.putIfAbsent(p.getUUID(), nu);
        }
        if (op.isEmpty() || op.stream().anyMatch(p -> nu - OP_CIRKEL.get(p.getUUID()) < START_TICKS)) {
            if (!op.isEmpty() && nu % 20 == 0) {
                for (ServerPlayer p : op) {
                    Z6RaidBaas.meld(p, op.size() < 2 ? "vadsrijk.z6raid.start.alleen" : "vadsrijk.z6raid.start.tel", ChatFormatting.GOLD, op.size());
                }
            }
            return;
        }
        if (op.size() < 2) {
            return;
        }
        List<UUID> wie = metParty(sl, op);
        OP_CIRKEL.clear();
        if (groep == null && RIJ.isEmpty()) {
            start(sl, wie);
        } else if (RIJ.stream().noneMatch(r -> r.contains(wie.get(0)))) {
            RIJ.add(wie);
            for (UUID id : wie) {
                if (sl.getEntity(id) instanceof ServerPlayer p) {
                    p.sendSystemMessage(Component.translatable("vadsrijk.z6raid.rij.erin", RIJ.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }
    }

    /** The players on the circle + their party members in the Wachtzaal, max 10, in that order. */
    private static List<UUID> metParty(ServerLevel sl, List<ServerPlayer> op) {
        LinkedHashSet<UUID> wie = new LinkedHashSet<>();
        for (ServerPlayer p : op) {
            wie.add(p.getUUID());
        }
        for (ServerPlayer p : op) {
            for (ServerPlayer l : Partijen.leden(p)) {
                if (l.level() == sl && Z6RaidPlekken.verdieping(l.getX(), l.getY(), l.getZ()) == 0) {
                    wie.add(l.getUUID());
                }
            }
        }
        return new ArrayList<>(wie).subList(0, Math.min(Z6Schaal.MAX, wie.size()));
    }

    /** The tower is free and someone waits: call the first group; whoever is in the Wachtzaal goes up after 10 s. */
    private static void roep(ServerLevel sl, long nu) {
        List<UUID> eerste = RIJ.get(0);
        List<UUID> hier = new ArrayList<>();
        for (UUID id : eerste) {
            if (sl.getEntity(id) instanceof ServerPlayer p && Z6RaidPlekken.verdieping(p.getX(), p.getY(), p.getZ()) == 0) {
                hier.add(id);
            }
        }
        if (geroepenOp < 0) {
            geroepenOp = nu;
            for (UUID id : eerste) {
                if (sl.getEntity(id) instanceof ServerPlayer p) {
                    p.sendSystemMessage(Component.translatable("vadsrijk.z6raid.rij.beurt").withStyle(ChatFormatting.GOLD));
                }
            }
        }
        if (nu - geroepenOp >= 200 && hier.size() >= 2) {
            RIJ.remove(0);
            geroepenOp = -1;
            start(sl, hier);
        } else if (nu - geroepenOp >= ROEP_TICKS) {
            RIJ.remove(0);   // not enough of them came: next please
            geroepenOp = -1;
        }
    }

    // ---- start / floors / wipe / clear -------------------------------------------------------------------------------------

    /** Starts the raid for these players (2..10): all floors reset, gates closed, everyone to the landing of floor 1. */
    public static Groep start(ServerLevel sl, List<UUID> wie) {
        Groep g = new Groep();
        for (UUID id : wie) {
            if (g.leden.size() < Z6Schaal.MAX && sl.getEntity(id) instanceof ServerPlayer p) {
                g.leden.add(id);
                g.namen.put(id, p.getGameProfile().name());
            }
        }
        g.n = Z6Schaal.n(g.leden.size());
        g.start = sl.getGameTime();
        groep = g;
        resetVerdiepingen(sl);
        for (UUID id : g.leden) {
            if (sl.getEntity(id) instanceof ServerPlayer p) {
                naar(p, Vec3.atBottomCenterOf(Z6RaidPlekken.landing(1)), -90);
                RijkScenes.titel(p, Component.translatable("vadsrijk.z6raid.titel").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                        Component.translatable("vadsrijk.z6raid.start.onder", g.n), 70, 0xC080FF);
            }
        }
        return g;
    }

    /** Every floor back to the start: bosses away (the current floor's comes back), gates closed, stars out (unless lit for good). */
    static void resetVerdiepingen(ServerLevel sl) {
        for (int k = 1; k <= 4; k++) {
            Arenas.reset(sl.getServer(), arenaId(k));
        }
        for (int k = 0; k <= 3; k++) {
            Z6RaidBouw.poort(sl, k, true);
        }
    }

    /** A real boss was cheered up. */
    static void baasVerslagen(ServerLevel sl, Z6RaidBaas b) {
        int k = b.verdieping();
        DEV_SPAWN[k] = false;
        Groep g = groep;
        if (g == null || g.verdieping != k) {
            return;
        }
        g.gevechtStart = -1;
        if (k < 4) {
            Z6RaidBouw.poort(sl, k, false);
            g.verdieping = k + 1;
            for (ServerPlayer p : leden(sl)) {
                RijkScenes.titel(p, Component.translatable("vadsrijk.z6raid.verdieping.op").withStyle(ChatFormatting.GOLD),
                        Component.translatable("vadsrijk.z6raid.verdieping." + (k + 1)), 60, 0xFFD86A);
            }
            Arenas.reset(sl.getServer(), arenaId(k + 1));
        } else {
            klaar(sl, g);
        }
    }

    /** The clear: a chest for everyone of the group in the tower, stars, titles, the story hook. */
    static void klaar(ServerLevel sl, Groep g) {
        g.verdieping = 5;
        g.klaarOp = sl.getGameTime();
        Z6RaidData d = Z6RaidData.get(sl.getServer());
        d.zet(() -> {
            d.clears++;
            d.sterrenAan = true;
        });
        Z6RaidBouw.sterren(sl, true);
        BlockPos m = Z6RaidPlekken.midden(4);
        Vfx.zuil(sl, Vec3.atBottomCenterOf(m), 14, 0xFFF0B0, 200);
        int i = 0;
        for (ServerPlayer p : leden(sl)) {
            if (!inToren(p)) {
                continue;
            }
            LootTabel.Resultaat r = Z6RaidLoot.clearKist(p, g, p.getRandom());
            double a = i++ * Math.PI * 2 / Math.max(1, g.leden.size());
            BaasKist.plaats(sl, p, m.offset((int) Math.round(Math.cos(a) * 5), 0, (int) Math.round(Math.sin(a) * 5)), r);
            Z6RaidLoot.titels(p, g);
            Z6RaidHaken.opgevrolijkt(p);
            p.sendSystemMessage(Component.translatable("vadsrijk.z6raid.klaar.chat", tijd(sl.getGameTime() - g.start), g.wipes).withStyle(ChatFormatting.GOLD));
        }
        clearsBeloond++;
    }

    /** Nobody of the group alive in the arena of a started fight: reset the floor. */
    static void wipe(ServerLevel sl, Groep g) {
        g.wipes++;
        g.gevechtStart = -1;
        g.leegInArena = 0;
        int k = g.verdieping;
        Arenas.reset(sl.getServer(), arenaId(k));
        for (ServerPlayer p : leden(sl)) {
            RijkScenes.titel(p, Component.translatable("vadsrijk.z6raid.wipe").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.translatable("vadsrijk.z6raid.wipe.onder", g.wipes), 60, 0xFF6A6A);
            if (Z6RaidPlekken.inArena(k, p.getX(), p.getY(), p.getZ())) {
                naar(p, Vec3.atBottomCenterOf(Z6RaidPlekken.landing(k)), -90);
            }
        }
    }

    /** Ends the run: the tower is free (the floors reset on the next start). */
    public static void stop(ServerLevel sl) {
        groep = null;
        for (int k = 0; k <= 4; k++) {
            DEV_SPAWN[k] = false;
        }
        for (int k = 1; k <= 4; k++) {
            Arenas.reset(sl.getServer(), arenaId(k));
        }
    }

    private static void groepTick(ServerLevel sl, long nu) {
        Groep g = groep;
        List<ServerPlayer> leden = leden(sl);
        boolean iemand = leden.stream().anyMatch(Z6Raid::inToren);
        g.leegSinds = iemand ? -1 : (g.leegSinds < 0 ? nu : g.leegSinds);
        if ((g.leegSinds >= 0 && nu - g.leegSinds >= LEEG_TICKS) || (g.klaar() && nu - g.klaarOp >= KLAAR_TICKS)) {
            for (ServerPlayer p : leden) {
                p.sendSystemMessage(Component.translatable("vadsrijk.z6raid.vrij").withStyle(ChatFormatting.GRAY));
            }
            stop(sl);
            return;
        }
        if (!g.klaar()) {
            VadsrijkBaas b = Arenas.baas(sl, arenaId(g.verdieping));
            boolean bezig = b != null && !b.geplofd() && (b.vads() < b.maxVads() - 0.5 || !b.bijdragen().spelers().isEmpty());
            if (bezig && g.gevechtStart < 0) {
                g.gevechtStart = nu;
            }
            boolean levend = leden.stream().anyMatch(p -> p.isAlive() && !Flauwvallen.beschermd(p) && Z6RaidPlekken.inArena(g.verdieping, p.getX(), p.getY(), p.getZ()));
            g.leegInArena = bezig && !levend ? g.leegInArena + 10 : 0;
            if (g.leegInArena >= 60) {
                wipe(sl, g);
            }
        }
        // the raid HUD for every member
        CompoundTag t = hud(sl, g, nu);
        for (ServerPlayer p : leden) {
            Z6RaidPayloads.stuur(p, t);
        }
    }

    /** The raid frames + boss timer + wipe counter (Z6RaidHud). */
    static CompoundTag hud(ServerLevel sl, Groep g, long nu) {
        CompoundTag t = new CompoundTag();
        t.putInt("verdieping", g.verdieping);
        t.putInt("n", g.n);
        t.putInt("wipes", g.wipes);
        t.putInt("totaal", (int) ((nu - g.start) / 20));
        t.putInt("gevecht", g.gevechtStart < 0 ? -1 : (int) ((nu - g.gevechtStart) / 20));
        if (!g.klaar()) {
            t.putString("baas", "vadsrijk.z6raid." + BAZEN[g.verdieping]);
            if (Arenas.baas(sl, arenaId(g.verdieping)) instanceof Z6RaidBaas b) {
                t.putInt("enrage", b.enrageSeconden());
                t.putInt("fase", b.fase());
                ListTag extra = new ListTag();
                for (Object[] e : b.hudExtra()) {
                    CompoundTag x = new CompoundTag();
                    x.putString("k", (String) e[0]);
                    x.putFloat("f", ((Number) e[1]).floatValue());
                    x.putBoolean("u", (Boolean) e[2]);
                    extra.add(x);
                }
                t.put("extra", extra);
            }
        }
        ListTag leden = new ListTag();
        for (UUID id : g.leden) {
            CompoundTag l = new CompoundTag();
            l.putString("naam", g.namen.getOrDefault(id, "?"));
            if (g.nep.contains(id)) {
                l.putFloat("vads", 1);
                l.putFloat("max", 1);
                l.putString("klasse", "nep");
                l.putBoolean("binnen", true);
            } else if (sl.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer p) {
                l.putFloat("vads", (float) Spelers.data(p).vads);
                l.putFloat("max", (float) Spelers.maxVads(p));
                l.putString("klasse", Spelers.data(p).klasse);
                l.putInt("lv", Spelers.klasseLevel(p));
                l.putBoolean("flauw", Flauwvallen.beschermd(p) || !p.isAlive());
                l.putBoolean("binnen", inToren(p));
            } else {
                l.putBoolean("weg", true);
            }
            leden.add(l);
        }
        t.put("leden", leden);
        return t;
    }

    private static void wachtzaalMelding(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (Z6RaidPlekken.verdieping(p.getX(), p.getY(), p.getZ()) != 0 || inGroep(p)) {
                continue;
            }
            int plek = -1;
            for (int i = 0; i < RIJ.size(); i++) {
                if (RIJ.get(i).contains(p.getUUID())) {
                    plek = i + 1;
                }
            }
            if (groep == null) {
                Z6RaidBaas.meld(p, "vadsrijk.z6raid.wachtzaal.vrij", ChatFormatting.LIGHT_PURPLE);
            } else if (plek > 0) {
                Z6RaidBaas.meld(p, "vadsrijk.z6raid.wachtzaal.plek", ChatFormatting.LIGHT_PURPLE, plek, groep.verdieping > 4 ? 4 : groep.verdieping);
            } else {
                Z6RaidBaas.meld(p, "vadsrijk.z6raid.wachtzaal.bezet", ChatFormatting.GOLD, groep.n, groep.verdieping > 4 ? 4 : groep.verdieping, RIJ.size());
            }
        }
    }

    public static List<ServerPlayer> leden(ServerLevel sl) {
        List<ServerPlayer> l = new ArrayList<>();
        if (groep != null) {
            for (UUID id : groep.leden) {
                ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
                if (p != null) {
                    l.add(p);
                }
            }
        }
        return l;
    }

    public static String tijd(long ticks) {
        long s = ticks / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    // ---- kern hooks (registered in Z6RaidRegistry) -----------------------------------------------------------------------------

    /** Flauwvallen.CHECKPOINT_KIEZER: a group member in the tower wakes at the landing of the current floor. */
    @Nullable
    static BlockPos checkpoint(ServerPlayer p) {
        if (!inGroep(p) || !inToren(p)) {
            return null;
        }
        return Z6RaidPlekken.landing(Math.max(1, Math.min(4, groep.verdieping)));
    }

    /** Haken.FLAUW: remember who fainted this run (the "Sterrenaansteker" title is for a run without fainting). */
    static void flauw(ServerPlayer p) {
        if (inGroep(p) && inToren(p)) {
            groep.flauw.add(p.getUUID());
            Vfx.deeltjes((ServerLevel) p.level(), Deeltje.ROOK, p.position().add(0, 1, 0), 10, 0x9A90B0, 0.5, 0.03);
        }
    }

    /** Eénvads: fainting in the raid is "zacht" (no retirement). */
    static boolean zacht(ServerPlayer p) {
        return inToren(p) && (inGroep(p) || groep == null);
    }

    /** Dev/tests: empties everything. */
    public static void wisAlles() {
        groep = null;
        RIJ.clear();
        OP_CIRKEL.clear();
        geroepenOp = -1;
        DEV_MEE.clear();
        for (int k = 0; k <= 4; k++) {
            DEV_SPAWN[k] = false;
        }
    }

    /** Gametests: a group without a world (n players, made-up ids). */
    public static Groep testGroep(int n) {
        Groep g = new Groep();
        for (int i = 0; i < n; i++) {
            UUID id = new UUID(0x26, i);
            g.leden.add(id);
            g.namen.put(id, "test" + i);
            g.nep.add(id);
        }
        g.n = Z6Schaal.n(n);
        groep = g;
        return g;
    }

    /** Dev: starts the raid for this player + (n - 1) made-up members (the bosses scale for n). */
    public static Groep devStart(ServerLevel sl, ServerPlayer p, int n) {
        Groep g = start(sl, List.of(p.getUUID()));
        for (int i = 1; i < Z6Schaal.n(n); i++) {
            UUID id = new UUID(0x2600, i);
            g.leden.add(id);
            g.nep.add(id);
            g.namen.put(id, "Nepguh " + i);
        }
        g.n = Z6Schaal.n(n);
        return g;
    }
}
