package nl.juiced.guhs.feature.vadsrijk.dungeon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData.Deur;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * W-GROT: the generic dungeon finder behind every dungeon door ({@link Dungeon}, {@link DungeonType}: caves now; outdoor, building
 * and flying-mount dungeons later use the same code): who is inside which dungeon, the queue, the ready check, the cooldown.
 * <ul>
 *   <li><b>Instance per group</b>: a dungeon belongs to ONE group at a time ({@link Instantie}): only its members may be inside
 *       (anybody else found in it is put outside the door: {@link #uitzetten}); the run scales with the group. Another group that
 *       wants the same dungeon queues and goes in when it is free. (The caves are part of the map, with U-GROTTEN's puzzle,
 *       treasure and craft spots at fixed places, so a cave is not copied: the one cave is the group's private instance while
 *       they are in. The same model as the raid's tower lock.)</li>
 *   <li><b>Starting</b> (the door's buttons): <i>Solo starten</i>; <i>Starten met groep</i> = your party ({@link Partijen}, the
 *       leader starts, every member gets the ready check); <i>In de wachtrij</i> = matchmaking with the others queued for the SAME
 *       dungeon: at {@link #GROEP_MAX} (or when a queued player presses "Nu starten") everyone of that batch gets the ready check
 *       ("Klaar?" yes/no, {@link #CHECK_TICKS}); all yes = all are teleported in together.</li>
 *   <li><b>The queue</b> survives walking away; it drops you on logout or a dimension change. Queued players see a small status in
 *       the action bar ("Wachtrij dungeon De Zoutgrot 2/4"). One queue at a time.</li>
 *   <li><b>Rules</b> (server side): not while in another dungeon or in the raid; not while your cooldown for that dungeon runs
 *       ({@link #WACHTTIJD_MS} after a clear, per player per dungeon, survives restarts); the door never opens for non-members.</li>
 *   <li><b>Out</b>: the door from the inside ("Dungeon verlaten"), or the window after the clear: you stand outside on the door's
 *       stand spot, which is OUTSIDE the dungeon and never teleports anybody in (the raid's old exit loop cannot happen here: going
 *       in is only ever a button). A member who fainted outside can go back in with "Terug naar binnen".</li>
 *   <li><b>End</b>: when no member is inside for {@link #LEEG_TICKS} (a cleared dungeon: {@link #LEEG_KLAAR_TICKS}), or when the
 *       group left by the door; after a clear with another group waiting, it closes {@link #SLUIT_TICKS} later (members are put
 *       out). Then {@link Dungeon#reset} and the next group can go.</li>
 * </ul>
 * Not saved (a restart empties every dungeon and queue, like parties and the raid). Dev: {@code /guhs vadsrijk dungeon
 * deur|nep|rij|wachttijd|verlaat|start} ({@code grot} is an alias).
 */
public final class DungeonRij {
    /** The group size the queue fills up to (the pack scaling tops out at 4: 3 + 3 extra in room 1). */
    public static final int GROEP_MAX = 4;
    /** A party may go in whole (Partijen.MAX = 5). */
    public static final int PARTY_MAX = Partijen.MAX;
    public static long WACHTTIJD_MS = 10L * 60 * 1000;
    public static int CHECK_TICKS = 20 * 30, LEEG_TICKS = 20 * 60, LEEG_KLAAR_TICKS = 20 * 10, SLUIT_TICKS = 20 * 120, POPUP_TICKS = 60;
    /** How far from the door's inside spot a member still counts as "in the dungeon" (the stair between the door and the dungeon). */
    public static final double BIJ_DEUR = 24;

    /** The group that owns a dungeon now. */
    public static final class Instantie {
        public final String dungeon;
        public final LinkedHashSet<UUID> leden = new LinkedHashSet<>();
        public final Map<UUID, String> namen = new LinkedHashMap<>();
        public long start, leegSinds = -1, sluitOp = -1, popupOp = -1, klaarOp = -1;
        /** The run was started for this group (once; a reset of an unfinished run clears it). */
        public boolean gestart;
        public boolean klaar;
        /** Non-player companions that went in with the group (w-party): they count for the scaling, never for loot or clears. */
        public final Map<UUID, Metgezel> metgezellen = new LinkedHashMap<>();

        Instantie(String dungeon) {
            this.dungeon = dungeon;
        }
    }

    /** A ready check: a batch from the queue ({@code uitRij}) or a party start. */
    public static final class Check {
        public final String dungeon;
        public final List<UUID> leden;
        public final Map<UUID, Boolean> antwoord = new HashMap<>();
        public final long start, tot;
        public final boolean uitRij;
        public final UUID starter;

        Check(String dungeon, List<UUID> leden, long nu, boolean uitRij, UUID starter) {
            this.dungeon = dungeon;
            this.leden = leden;
            this.start = nu;
            this.tot = nu + CHECK_TICKS;
            this.uitRij = uitRij;
            this.starter = starter;
        }
    }

    public static final Map<String, Instantie> BEZET = new LinkedHashMap<>();
    public static final Map<String, LinkedHashSet<UUID>> RIJ = new LinkedHashMap<>();
    public static final Map<String, Check> CHECKS = new LinkedHashMap<>();
    /** Dev: made-up queue members (id -> name) and how they answer the ready check (true yes, false no, absent: never). */
    public static final Map<UUID, String> NEP = new LinkedHashMap<>();
    public static final Map<UUID, Boolean> NEP_ANTWOORD = new HashMap<>();
    /** Players with the door window open (id -> dungeon): they get a fresh state every second. */
    public static final Map<UUID, String> KIJKERS = new HashMap<>();
    /** Just went out: door clicks ignored for a moment (no double click straight back in). */
    public static final Map<UUID, Long> NET_UIT = new HashMap<>();
    /** Gametests: no dimension checks, the player's own level counts as Het Vadsrijk. */
    public static boolean TEST;
    @Nullable
    public static ServerLevel TEST_LEVEL;
    public static int nepTeller;

    private DungeonRij() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> wisAlles());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> uitRijWeg(e.getEntity().getUUID(), null));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && !Vadsrijk.in(p)) {
                uitRijWeg(p.getUUID(), "vadsrijk.dungeon.msg.rij.weg.dim");
            }
        });
        NeoForge.EVENT_BUS.addListener(DungeonRij::commando);
    }

    public static void wisAlles() {
        BEZET.clear();
        RIJ.clear();
        METGEZELLEN.clear();
        BLOK.clear();
        RIJ_SINDS.clear();
        CHECKS.clear();
        NEP.clear();
        NEP_ANTWOORD.clear();
        KIJKERS.clear();
        NET_UIT.clear();
        DungeonDeur.nieuweSessie();
    }

    // ---- queries --------------------------------------------------------------------------------------------------------------

    @Nullable
    public static Instantie instantie(String dungeon) {
        return BEZET.get(dungeon);
    }

    /** The dungeon this player is a member of (null: none). */
    @Nullable
    public static String dungeonVan(UUID id) {
        for (Instantie i : BEZET.values()) {
            if (i.leden.contains(id)) {
                return i.dungeon;
            }
        }
        return null;
    }

    public static boolean lid(ServerPlayer p, String dungeon) {
        Instantie i = BEZET.get(dungeon);
        return i != null && i.leden.contains(p.getUUID());
    }

    /** The dungeon whose queue this player is in (null: none). */
    @Nullable
    public static String rijVan(UUID id) {
        for (var e : RIJ.entrySet()) {
            if (e.getValue().contains(id)) {
                return e.getKey();
            }
        }
        return null;
    }

    public static int rijGrootte(String dungeon) {
        LinkedHashSet<UUID> r = RIJ.get(dungeon);
        return r == null ? 0 : r.size();
    }

    /** Remaining cooldown for this dungeon in ms (0: none). */
    public static long wachttijd(ServerPlayer p, String dungeon) {
        long tot = Spelers.data(p).extra(DATA).getLongOr(dungeon + ".wachttijdTot", 0L);
        return Math.max(0, tot - System.currentTimeMillis());
    }

    public static void zetWachttijd(ServerPlayer p, String dungeon, long ms) {
        if (ms <= 0) {
            Spelers.data(p).extra(DATA).remove(dungeon + ".wachttijdTot");
        } else {
            Spelers.data(p).extra(DATA).putLong(dungeon + ".wachttijdTot", System.currentTimeMillis() + ms);
        }
    }

    public static String tijd(long ms) {
        long s = (ms + 999) / 1000;
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    /** The data tag of the dungeon system per player (cooldowns: "&lt;id&gt;.wachttijdTot"). */
    public static final String DATA = "dungeon";

    public static Component dungeonNaam(String id) {
        Dungeon d = Dungeons.van(id);
        return d == null ? Component.literal(id) : d.naam();
    }

    public static boolean inVadsrijk(ServerPlayer p) {
        return TEST || Vadsrijk.in(p);
    }

    @Nullable
    public static ServerLevel wereld(ServerPlayer p) {
        return TEST ? (ServerLevel) p.level() : Vadsrijk.level(p.level().getServer());
    }

    @Nullable
    public static ServerPlayer speler(MinecraftServer s, UUID id) {
        return s.getPlayerList().getPlayer(id);
    }

    public static String naam(MinecraftServer s, UUID id) {
        Metgezel m = METGEZELLEN.get(id);
        if (m != null) {
            return m.naam();
        }
        String n = NEP.get(id);
        if (n != null) {
            return n;
        }
        ServerPlayer p = speler(s, id);
        return p == null ? "?" : p.getGameProfile().name();
    }

    /** Why this player may not go into this dungeon now (null: may). Checks: Het Vadsrijk, another dungeon, the raid, the cooldown. */
    @Nullable
    public static Component weiger(ServerPlayer p, String dungeon, boolean zelf) {
        if (!inVadsrijk(p)) {
            return Component.translatable("vadsrijk.dungeon.msg.niet.vadsrijk");
        }
        Component poort = nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.dungeonDicht(p, dungeon);   // X-ZONE1: its zone is closed
        if (poort != null) {
            return poort;
        }
        String al = dungeonVan(p.getUUID());
        if (al != null) {
            return zelf ? Component.translatable("vadsrijk.dungeon.msg.al.kerker", dungeonNaam(al))
                    : Component.translatable("vadsrijk.dungeon.msg.bezet.ander", p.getName());
        }
        if (inRaid(p)) {
            return zelf ? Component.translatable("vadsrijk.dungeon.msg.al.raid") : Component.translatable("vadsrijk.dungeon.msg.bezet.ander", p.getName());
        }
        long w = wachttijd(p, dungeon);
        if (w > 0) {
            return zelf ? Component.translatable("vadsrijk.dungeon.msg.cooldown", tijd(w), dungeonNaam(dungeon))
                    : Component.translatable("vadsrijk.dungeon.msg.cooldown.ander", p.getName(), tijd(w));
        }
        return null;
    }

    public static boolean inRaid(ServerPlayer p) {
        try {
            return nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6Raid.inGroep(p);
        } catch (LinkageError e) {
            return false;
        }
    }

    public static void zeg(ServerPlayer p, Component c, ChatFormatting kleur) {
        p.sendSystemMessage(c.copy().withStyle(kleur));
    }

    // ---- the buttons ----------------------------------------------------------------------------------------------------------

    /** A button of the window (from the client; checked here). */
    public static void actie(ServerPlayer p, String wat, String dungeon) {
        if (wat.equals("dicht")) {
            KIJKERS.remove(p.getUUID());
            return;
        }
        if (wat.equals("klaar_ja") || wat.equals("klaar_nee")) {
            antwoord(p, wat.equals("klaar_ja"));
            return;
        }
        if (Dungeons.van(dungeon) == null) {
            return;
        }
        if (!Vadsrijk.in(p)) {
            return;   // Y-SCHEIDING: the window's buttons only work inside ("verlaat"/"terug" from home teleported to any dungeon door)
        }
        switch (wat) {
            case "solo" -> solo(p, dungeon, false);
            case "groep" -> groep(p, dungeon);
            case "rij" -> inRij(p, dungeon);
            case "rij_uit" -> uitRij(p);
            case "nu" -> nuStarten(p);
            case "terug" -> terug(p, dungeon);
            case "verlaat" -> verlaat(p, dungeon);
            case "blijf" -> KIJKERS.remove(p.getUUID());
            default -> {
            }
        }
        if (KIJKERS.containsKey(p.getUUID())) {
            stuurDeur(p, dungeon, false);
        }
    }

    /** The door was right-clicked. */
    public static void klikDeur(ServerLevel sl, BlockPos pos, ServerPlayer p) {
        Deur d = DungeonDeur.op(pos);
        if (d == null) {
            return;
        }
        long nu = sl.getServer().getTickCount();
        Long uit = NET_UIT.get(p.getUUID());
        if (uit != null && nu - uit < 20) {
            return;
        }
        if (d.zegel()) {   // X-DUNGEON: a sealed second entrance only says where the door is
            p.sendSystemMessage(Component.translatable("vadsrijk.dungeon.zegel.msg." + d.dungeon()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        openDeur(p, d.dungeon());
    }

    /** Opens the door window for this dungeon (also the dev command). */
    public static void openDeur(ServerPlayer p, String dungeon) {
        KIJKERS.put(p.getUUID(), dungeon);
        stuurDeur(p, dungeon, true);
    }

    /** Solo start ({@code dev}: rules and cooldown skipped). True = inside. */
    public static boolean solo(ServerPlayer p, String dungeon, boolean dev) {
        if (!dev) {
            Component w = weiger(p, dungeon, true);
            if (w != null) {
                zeg(p, w, ChatFormatting.RED);
                return false;
            }
        } else {
            String al = dungeonVan(p.getUUID());
            if (al != null && !al.equals(dungeon)) {
                verlaat(p, al);
            }
        }
        Instantie i = BEZET.get(dungeon);
        if (i != null) {
            if (dev && i.leden.contains(p.getUUID())) {
                terug(p, dungeon);
                return true;
            }
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.bezet"), ChatFormatting.GOLD);
            return false;
        }
        if (CHECKS.containsKey(dungeon)) {
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.check.bezig"), ChatFormatting.GOLD);
            return false;
        }
        uitRijWeg(p.getUUID(), null);
        return binnen(dungeon, List.of(p)) != null;
    }

    /** Start with the party: the leader starts, every online member gets the ready check (the leader is "yes" at once). */
    public static void groep(ServerPlayer p, String dungeon) {
        Partijen.Party party = Partijen.party(p);
        if (party == null) {
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.geen.groep"), ChatFormatting.GOLD);
            return;
        }
        if (!party.leider.equals(p.getUUID())) {
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.groep.leider"), ChatFormatting.GOLD);
            return;
        }
        List<ServerPlayer> wie = Partijen.leden(p);
        if (wie.size() > PARTY_MAX) {
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.groep.te.groot", PARTY_MAX), ChatFormatting.RED);
            return;
        }
        for (ServerPlayer l : wie) {
            Component w = weiger(l, dungeon, l == p);
            if (w != null) {
                zeg(p, w, ChatFormatting.RED);
                return;
            }
        }
        if (BEZET.containsKey(dungeon)) {
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.bezet"), ChatFormatting.GOLD);
            return;
        }
        if (CHECKS.containsKey(dungeon)) {
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.check.bezig"), ChatFormatting.GOLD);
            return;
        }
        List<UUID> ids = new ArrayList<>();
        for (ServerPlayer l : wie) {
            ids.add(l.getUUID());
        }
        startCheck(p.level().getServer(), dungeon, ids, false, p.getUUID());
    }

    /** Into the queue of this dungeon (out of any other queue). */
    public static boolean inRij(ServerPlayer p, String dungeon) {
        Component w = weiger(p, dungeon, true);
        if (w != null) {
            zeg(p, w, ChatFormatting.RED);
            return false;
        }
        String al = rijVan(p.getUUID());
        if (dungeon.equals(al)) {
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.al.rij", dungeonNaam(dungeon)), ChatFormatting.GOLD);
            return true;
        }
        uitRijWeg(p.getUUID(), null);
        RIJ.computeIfAbsent(dungeon, g -> new LinkedHashSet<>()).add(p.getUUID());
        zeg(p, Component.translatable("vadsrijk.dungeon.msg.rij.erin", dungeonNaam(dungeon), rijGrootte(dungeon), GROEP_MAX), ChatFormatting.LIGHT_PURPLE);
        matchmaak(p.level().getServer(), dungeon);
        return true;
    }

    public static void uitRij(ServerPlayer p) {
        String g = rijVan(p.getUUID());
        uitRijWeg(p.getUUID(), null);
        if (g != null) {
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.rij.uit", dungeonNaam(g)), ChatFormatting.GRAY);
        }
    }

    /** Out of every queue + any running check (logout, dimension change, a solo start). {@code melding}: told to the player. */
    public static void uitRijWeg(UUID id, @Nullable String melding) {
        String g = null;
        for (var e : RIJ.entrySet()) {
            if (e.getValue().remove(id)) {
                g = e.getKey();
            }
        }
        for (Check c : CHECKS.values()) {
            if (c.leden.contains(id) && !c.antwoord.containsKey(id)) {
                c.antwoord.put(id, false);
            }
        }
        KIJKERS.remove(id);
        if (g != null && melding != null) {
            for (ServerPlayer p : alleSpelers()) {
                if (p.getUUID().equals(id)) {
                    zeg(p, Component.translatable(melding, dungeonNaam(g)), ChatFormatting.GRAY);
                }
            }
        }
    }

    private static MinecraftServer server;

    public static List<ServerPlayer> alleSpelers() {
        return server == null ? List.of() : server.getPlayerList().getPlayers();
    }

    /** A queued player: start now with whoever is in the queue (fewer than 4 is fine). */
    public static void nuStarten(ServerPlayer p) {
        String g = rijVan(p.getUUID());
        if (g == null) {
            return;
        }
        if (BEZET.containsKey(g)) {
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.bezet"), ChatFormatting.GOLD);
            return;
        }
        if (CHECKS.containsKey(g)) {
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.check.bezig"), ChatFormatting.GOLD);
            return;
        }
        startCheck(p.level().getServer(), g, batch(g, GROEP_MAX), true, p.getUUID());
    }

    /** A member back in (after fainting outside, say). */
    public static void terug(ServerPlayer p, String dungeon) {
        Instantie i = BEZET.get(dungeon);
        Deur d = DungeonDeur.hoofd(dungeon);
        if (i == null || d == null || !i.leden.contains(p.getUUID())) {
            return;
        }
        naar(p, d.binnen(), d.yawBinnen());
        KIJKERS.remove(p.getUUID());
        DungeonNet.stuur(p, DungeonNet.sluit("deur"));
    }

    /** Out through the door: stand outside on the main door's stand spot; the member leaves the group. */
    public static void verlaat(ServerPlayer p, String dungeon) {
        Instantie i = BEZET.get(dungeon);
        Deur d = DungeonDeur.hoofd(dungeon);
        if (d == null) {
            return;
        }
        if (i != null) {
            i.leden.remove(p.getUUID());
        }
        naarBuiten(p, dungeon, d);
        zeg(p, Component.translatable("vadsrijk.dungeon.msg.uit", dungeonNaam(dungeon)), ChatFormatting.LIGHT_PURPLE);
        if (i != null && i.leden.isEmpty()) {
            sluit(p.level().getServer(), dungeon, null);
        }
    }

    public static void naarBuiten(ServerPlayer p, String dungeon, Deur d) {
        naar(p, d.voor(), d.yawVoor());
        NET_UIT.put(p.getUUID(), (long) p.level().getServer().getTickCount());
        KIJKERS.remove(p.getUUID());
        DungeonNet.stuur(p, DungeonNet.sluit("deur"));
        ServerLevel sl = wereld(p);
        if (sl != null) {
            DungeonDeur.geluid(sl, d, false);
        }
    }

    public static void naar(ServerPlayer p, BlockPos b, float yaw) {
        ServerLevel sl = wereld(p);
        if (sl == null) {
            return;
        }
        if (!TEST && !Vadsrijk.in(p)) {
            VadsrijkCommando.naarBinnen(p, false);
        }
        sl.getChunk(b);
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(b), Vec3.ZERO, yaw, 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
    }

    // ---- the ready check ------------------------------------------------------------------------------------------------------

    public static void startCheck(MinecraftServer s, String dungeon, List<UUID> leden, boolean uitRij, UUID starter) {
        server = s;
        long nu = s.getTickCount();
        Check c = new Check(dungeon, leden, nu, uitRij, starter);
        if (!uitRij) {
            c.antwoord.put(starter, true);
        }
        CHECKS.put(dungeon, c);
        for (UUID id : leden) {
            ServerPlayer p = speler(s, id);
            if (p != null) {
                zeg(p, Component.translatable("vadsrijk.dungeon.msg.check.start", dungeonNaam(dungeon), CHECK_TICKS / 20), ChatFormatting.GOLD);
                p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1f, 1.2f);
                KIJKERS.remove(id);
                stuurCheck(p, c, true);
            }
        }
    }

    public static void antwoord(ServerPlayer p, boolean ja) {
        for (Check c : CHECKS.values()) {
            if (c.leden.contains(p.getUUID()) && !c.antwoord.containsKey(p.getUUID())) {
                c.antwoord.put(p.getUUID(), ja);
                if (ja) {
                    for (UUID id : c.leden) {
                        ServerPlayer l = speler(p.level().getServer(), id);
                        if (l != null && l != p) {
                            l.sendSystemMessage(Component.translatable("vadsrijk.dungeon.msg.klaar.ja", p.getName()).withStyle(ChatFormatting.GRAY));
                        }
                    }
                }
                verwerk(p.level().getServer(), c);
                return;
            }
        }
    }

    /** All answered (or time up): in together, or the decliners / silent ones out. */
    public static void verwerk(MinecraftServer s, Check c) {
        verwerk(s, c, s.getTickCount());
    }

    public static void verwerk(MinecraftServer s, Check c, long nu) {
        boolean nee = c.antwoord.containsValue(false);
        boolean alle = c.leden.stream().allMatch(id -> Boolean.TRUE.equals(c.antwoord.get(id)));
        boolean tijd = nu >= c.tot;
        if (!nee && !alle && !tijd) {
            for (UUID id : c.leden) {
                ServerPlayer p = speler(s, id);
                if (p != null) {
                    stuurCheck(p, c, false);
                }
            }
            return;
        }
        CHECKS.remove(c.dungeon);
        for (UUID id : c.leden) {
            ServerPlayer p = speler(s, id);
            if (p != null) {
                DungeonNet.stuur(p, DungeonNet.sluit("klaar"));
            }
        }
        if (alle) {
            List<ServerPlayer> wie = new ArrayList<>();
            List<String> nep = new ArrayList<>();
            List<Metgezel> mee = new ArrayList<>();
            for (UUID id : c.leden) {
                ServerPlayer p = speler(s, id);
                if (METGEZELLEN.containsKey(id)) {
                    mee.add(METGEZELLEN.remove(id));
                } else if (NEP.containsKey(id)) {
                    nep.add(NEP.get(id));
                } else if (p != null && weiger(p, c.dungeon, true) == null) {
                    wie.add(p);
                }
            }
            for (UUID id : c.leden) {
                LinkedHashSet<UUID> r = RIJ.get(c.dungeon);
                if (r != null) {
                    r.remove(id);
                }
                NEP.remove(id);
                NEP_ANTWOORD.remove(id);
            }
            for (UUID id : c.leden) {
                BLOK.remove(id);
            }
            gebeurtenis(c, true);
            if (!wie.isEmpty() && !BEZET.containsKey(c.dungeon)) {
                Instantie in = binnen(c.dungeon, wie);
                if (in != null) {
                    for (Metgezel m : mee) {
                        in.metgezellen.put(m.id(), m);
                    }
                    for (var h : BIJ_START) {
                        h.start(in, wie, List.copyOf(mee));
                    }
                }
                for (ServerPlayer p : wie) {
                    for (String n : nep) {
                        p.sendSystemMessage(Component.translatable("vadsrijk.dungeon.msg.nep.buiten", n).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
            return;
        }
        gebeurtenis(c, false);
        // somebody said no / the time ran out: from the queue, the "no" and silent ones leave it; the rest stays queued
        List<String> weg = new ArrayList<>();
        for (UUID id : c.leden) {
            Boolean a = c.antwoord.get(id);
            if (!Boolean.TRUE.equals(a)) {
                weg.add(naam(s, id));
                if (c.uitRij) {
                    LinkedHashSet<UUID> r = RIJ.get(c.dungeon);
                    if (r != null) {
                        r.remove(id);
                    }
                    NEP.remove(id);
                    NEP_ANTWOORD.remove(id);
                    METGEZELLEN.remove(id);
                    BLOK.remove(id);
                }
            }
        }
        String namen = String.join(", ", weg);
        for (UUID id : c.leden) {
            ServerPlayer p = speler(s, id);
            if (p == null) {
                continue;
            }
            if (nee) {
                zeg(p, Component.translatable(c.uitRij ? "vadsrijk.dungeon.msg.check.nee" : "vadsrijk.dungeon.msg.check.nee.groep", namen), ChatFormatting.GOLD);
            } else {
                zeg(p, Component.translatable(c.uitRij ? "vadsrijk.dungeon.msg.check.tijd" : "vadsrijk.dungeon.msg.check.tijd.groep"), ChatFormatting.GOLD);
            }
        }
    }

    /** The queue of this dungeon is full (or ready to go) and the dungeon is free: a ready check for the first batch. */
    public static void matchmaak(MinecraftServer s, String dungeon) {
        server = s;
        LinkedHashSet<UUID> r = RIJ.get(dungeon);
        if (r == null || r.size() < GROEP_MAX || BEZET.containsKey(dungeon) || CHECKS.containsKey(dungeon)) {
            return;
        }
        List<UUID> batch = batch(dungeon, GROEP_MAX);
        if (batch.size() >= GROEP_MAX) {
            startCheck(s, dungeon, batch, true, batch.get(0));
        }
    }

    /** The first batch of the queue, at most {@code max}: whole blocks (a party queued together) in queue order, skipping a block that does not fit. */
    public static List<UUID> batch(String dungeon, int max) {
        List<UUID> uit = new ArrayList<>();
        LinkedHashSet<UUID> r = RIJ.get(dungeon);
        if (r == null) {
            return uit;
        }
        Set<UUID> gezien = new java.util.HashSet<>();
        for (UUID id : r) {
            if (gezien.contains(id)) {
                continue;
            }
            UUID blok = BLOK.get(id);
            List<UUID> deel = new ArrayList<>();
            if (blok == null) {
                deel.add(id);
            } else {
                for (UUID o : r) {
                    if (blok.equals(BLOK.get(o))) {
                        deel.add(o);
                    }
                }
            }
            gezien.addAll(deel);
            if (uit.size() + deel.size() <= max) {
                uit.addAll(deel);
            }
            if (uit.size() >= max) {
                break;
            }
        }
        return uit;
    }

    // ---- in / out of the instance ---------------------------------------------------------------------------------------------

    /** The group goes in: the dungeon is theirs, everyone stands just inside the door; the run starts when the first one is in the dungeon. */
    @Nullable
    public static Instantie binnen(String dungeon, List<ServerPlayer> wie) {
        Dungeon k = Dungeons.van(dungeon);
        Deur d = DungeonDeur.hoofd(dungeon);
        if (k == null || d == null || wie.isEmpty()) {
            return null;
        }
        ServerPlayer eerste = wie.get(0);
        server = eerste.level().getServer();
        ServerLevel sl = wereld(eerste);
        if (sl == null) {
            return null;
        }
        if (k.bezig()) {
            k.reset(sl);   // (a stale run of nobody: fresh for the new group)
        }
        Instantie i = new Instantie(dungeon);
        i.start = server.getTickCount();
        for (ServerPlayer p : wie) {
            i.leden.add(p.getUUID());
            i.namen.put(p.getUUID(), p.getGameProfile().name());
            uitRijWeg(p.getUUID(), null);
        }
        BEZET.put(dungeon, i);
        DungeonDeur.geluid(sl, d, true);
        int n = wie.size();
        for (ServerPlayer p : wie) {
            naar(p, d.binnen(), d.yawBinnen());
            DungeonNet.stuur(p, DungeonNet.sluit("deur"));
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.binnen", dungeonNaam(dungeon), n,
                    Component.translatable(n == 1 ? "vadsrijk.dungeon.msg.binnen.speler" : "vadsrijk.dungeon.msg.binnen.spelers")), ChatFormatting.GOLD);
            RijkScenes.titel(p, dungeonNaam(dungeon).copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    k.onderTitel(), 60, 0xFF8CC8);
        }
        return i;
    }

    /** The dungeon closes for its group: the run resets, the instance goes, the next group may come. */
    public static void sluit(MinecraftServer s, String dungeon, @Nullable String melding) {
        Instantie i = BEZET.remove(dungeon);
        Dungeon k = Dungeons.van(dungeon);
        if (i != null) {
            for (var h : BIJ_EINDE) {
                h.accept(i);
            }
        }
        ServerLevel sl = TEST ? TEST_LEVEL : Vadsrijk.level(s);
        if (i != null && melding != null) {
            for (UUID id : i.leden) {
                ServerPlayer p = speler(s, id);
                if (p != null) {
                    zeg(p, Component.translatable(melding), ChatFormatting.LIGHT_PURPLE);
                }
            }
        }
        if (k != null && sl != null) {
            k.reset(sl);
        }
        matchmaak(s, dungeon);
    }

    /** (the dungeon's run) The boss is beaten: the group's dungeon is cleared; the cooldown for everyone who counted; the window soon. */
    public static void klaar(ServerLevel sl, String dungeon, Set<ServerPlayer> wie) {
        Instantie i = BEZET.get(dungeon);
        long nu = sl.getServer().getTickCount();
        if (i != null) {
            i.klaar = true;
            i.klaarOp = nu;
            i.popupOp = nu + POPUP_TICKS;
        }
        for (ServerPlayer p : wie) {
            zetWachttijd(p, dungeon, WACHTTIJD_MS);
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.wachttijd.gezet", dungeonNaam(dungeon), tijd(WACHTTIJD_MS)), ChatFormatting.GRAY);
            zeg(p, Component.translatable("vadsrijk.dungeon.msg.klaar.uit"), ChatFormatting.LIGHT_PURPLE);
        }
    }

    /** (the dungeon's reset) An unfinished run reset (everyone left the dungeon for a while): it may start again for the group. */
    public static void runGereset(String dungeon) {
        Instantie i = BEZET.get(dungeon);
        if (i != null && !i.klaar) {
            i.gestart = false;
        }
    }

    /** May the dungeon start its run now? Only for its group, once per instance. */
    /** The group size a dungeon run scales with: the members + their companions (min 1). */
    public static int groepsGrootte(String dungeon) {
        Instantie i = BEZET.get(dungeon);
        return i == null ? 1 : Math.max(1, i.leden.size() + i.metgezellen.size());
    }

    public static boolean magStarten(String dungeon) {
        Instantie i = BEZET.get(dungeon);
        return i != null && !i.gestart && !i.klaar;
    }

    public static void gestart(String dungeon) {
        Instantie i = BEZET.get(dungeon);
        if (i != null) {
            i.gestart = true;
        }
    }

    /** Dev (dungeon tp / baas / run): this player becomes a member of the dungeon's group (a new group of one when the dungeon is free), no teleport. */
    public static void devToegang(ServerPlayer p, String dungeon) {
        if (Dungeons.van(dungeon) == null || lid(p, dungeon)) {
            return;
        }
        String al = dungeonVan(p.getUUID());
        if (al != null) {
            Instantie a = BEZET.get(al);
            a.leden.remove(p.getUUID());
            if (a.leden.isEmpty()) {
                sluit(p.level().getServer(), al, null);
            }
        }
        uitRijWeg(p.getUUID(), null);
        Instantie i = BEZET.computeIfAbsent(dungeon, g -> {
            Instantie n = new Instantie(g);
            n.start = p.level().getServer().getTickCount();
            return n;
        });
        i.leden.add(p.getUUID());
        i.namen.put(p.getUUID(), p.getGameProfile().name());
    }

    /** Is this player still "in" the group's dungeon (online, in the dungeon or on the way between the door and the box)? */
    public static boolean aanwezig(MinecraftServer s, UUID id, Dungeon k, Deur d) {
        ServerPlayer p = speler(s, id);
        if (p == null || !inVadsrijk(p) || p.isRemoved()) {
            return false;
        }
        if (k.bevat(p.getX(), p.getY(), p.getZ())) {
            return true;
        }
        return p.distanceToSqr(d.binnen().getX() + 0.5, d.binnen().getY(), d.binnen().getZ() + 0.5) <= BIJ_DEUR * BIJ_DEUR && DungeonDeur.binnenKant(p, d);
    }

    /** Anyone in the dungeon who is not a member of its group (and not creative/spectator) is put outside the main door. */
    public static void uitzetten(ServerLevel sl, Dungeon k) {
        Instantie i = BEZET.get(k.id());
        Deur d = DungeonDeur.hoofd(k.id());
        if (d == null) {
            return;
        }
        for (ServerPlayer p : new ArrayList<>(sl.players())) {
            if (p.isSpectator() || p.isCreative() || !k.bevat(p.getX(), p.getY(), p.getZ())) {
                continue;
            }
            if (i != null && i.leden.contains(p.getUUID())) {
                continue;
            }
            naarBuiten(p, k.id(), d);
            p.sendSystemMessage(Component.translatable(i == null ? "vadsrijk.dungeon.msg.niet.gestart" : "vadsrijk.dungeon.msg.niet.van.jou")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // ---- the tick ---------------------------------------------------------------------------------------------------------------

    public static void tick(MinecraftServer s) {
        server = s;
        long nu = s.getTickCount();
        if (nu % 10 != 0) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(s);
        if (sl != null) {
            DungeonDeur.tick(sl);
            for (Dungeon k : Dungeons.alle()) {
                if (!Dungeons.TEST.containsKey(k.id())) {
                    uitzetten(sl, k);
                }
            }
        }
        tickRij(s, nu);
    }

    /** The queue, checks, instances and windows (also called by the gametests). */
    public static void tickRij(MinecraftServer s, long nu) {
        server = s;
        // made-up members answer after a second
        for (Check c : new ArrayList<>(CHECKS.values())) {
            if (nu - c.start >= 20) {
                for (UUID id : c.leden) {
                    if (NEP.containsKey(id) && NEP_ANTWOORD.containsKey(id) && !c.antwoord.containsKey(id)) {
                        c.antwoord.put(id, NEP_ANTWOORD.get(id));
                    }
                    if (METGEZELLEN.containsKey(id) && !c.antwoord.containsKey(id)) {
                        c.antwoord.put(id, true);
                    }
                }
            }
            verwerk(s, c, nu);
        }
        // the queue: gone players out, the action bar status, matchmaking
        for (var e : new ArrayList<>(RIJ.entrySet())) {
            String g = e.getKey();
            for (Iterator<UUID> it = e.getValue().iterator(); it.hasNext();) {
                UUID id = it.next();
                if (NEP.containsKey(id) || METGEZELLEN.containsKey(id)) {
                    continue;
                }
                ServerPlayer p = speler(s, id);
                if (p == null) {
                    it.remove();
                } else if (!inVadsrijk(p)) {
                    it.remove();
                    zeg(p, Component.translatable("vadsrijk.dungeon.msg.rij.weg.dim", dungeonNaam(g)), ChatFormatting.GRAY);
                }
            }
            if (nu % 40 == 0) {
                boolean bezet = BEZET.containsKey(g);
                for (UUID id : e.getValue()) {
                    ServerPlayer p = speler(s, id);
                    if (p != null && !CHECKS.containsKey(g)) {
                        p.sendOverlayMessage(Component.translatable(bezet ? "vadsrijk.dungeon.msg.rij.status.bezet" : "vadsrijk.dungeon.msg.rij.status",
                                dungeonNaam(g), e.getValue().size(), GROEP_MAX).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
            }
            if (e.getValue().isEmpty()) {
                RIJ.remove(g);
                RIJ_SINDS.remove(g);
            } else {
                RIJ_SINDS.putIfAbsent(g, nu);
                matchmaak(s, g);
            }
        }
        // the instances: who is still in, the end, the closing when another group waits, the window after the clear
        for (Instantie i : new ArrayList<>(BEZET.values())) {
            Dungeon k = Dungeons.van(i.dungeon);
            Deur d = DungeonDeur.hoofd(i.dungeon);
            if (k == null || d == null) {
                BEZET.remove(i.dungeon);
                continue;
            }
            boolean iemand = false;
            for (UUID id : i.leden) {
                if (aanwezig(s, id, k, d)) {
                    iemand = true;
                    break;
                }
            }
            if (iemand) {
                i.leegSinds = -1;
            } else if (i.leegSinds < 0) {
                i.leegSinds = nu;
            }
            if (!iemand && nu - i.leegSinds >= (i.klaar ? LEEG_KLAAR_TICKS : LEEG_TICKS)) {
                sluit(s, i.dungeon, "vadsrijk.dungeon.msg.leeg");
                continue;
            }
            boolean wacht = rijGrootte(i.dungeon) > 0 || CHECKS.containsKey(i.dungeon);
            if (i.klaar && wacht && i.sluitOp < 0) {
                i.sluitOp = nu + SLUIT_TICKS;
                for (UUID id : i.leden) {
                    ServerPlayer p = speler(s, id);
                    if (p != null) {
                        zeg(p, Component.translatable("vadsrijk.dungeon.msg.sluit.straks", tijd(SLUIT_TICKS * 50L)), ChatFormatting.GOLD);
                    }
                }
            }
            if (i.sluitOp >= 0 && nu >= i.sluitOp) {
                for (UUID id : new ArrayList<>(i.leden)) {
                    ServerPlayer p = speler(s, id);
                    if (p != null && aanwezig(s, id, k, d)) {
                        naarBuiten(p, i.dungeon, d);
                    }
                }
                sluit(s, i.dungeon, "vadsrijk.dungeon.msg.gesloten");
                continue;
            }
            if (i.popupOp >= 0 && nu >= i.popupOp) {
                i.popupOp = -1;
                for (UUID id : i.leden) {
                    ServerPlayer p = speler(s, id);
                    if (p != null && aanwezig(s, id, k, d)) {
                        openDeur(p, i.dungeon);
                    }
                }
            }
        }
        // the open windows: fresh state every second
        if (nu % 20 == 0) {
            for (var e : new ArrayList<>(KIJKERS.entrySet())) {
                ServerPlayer p = speler(s, e.getKey());
                if (p == null) {
                    KIJKERS.remove(e.getKey());
                } else {
                    stuurDeur(p, e.getValue(), false);
                }
            }
            for (Check c : CHECKS.values()) {
                for (UUID id : c.leden) {
                    ServerPlayer p = speler(s, id);
                    if (p != null) {
                        stuurCheck(p, c, false);
                    }
                }
            }
        }
        NET_UIT.values().removeIf(t -> nu - t > 200);
    }

    // ---- the windows ------------------------------------------------------------------------------------------------------------

    /** The door window: the dungeon, the advice, the boss, the loot (+ the personal rare rolls with pity), your clears/cooldown, buttons. */
    public static void stuurDeur(ServerPlayer p, String dungeon, boolean open) {
        Dungeon k = Dungeons.van(dungeon);
        if (k == null) {
            return;
        }
        DungeonNet.Bouw b = new DungeonNet.Bouw("deur", dungeon, open, Component.translatable("vadsrijk.dungeon.titel", k.naam()), Component.empty());
        b.lijn(Component.translatable("vadsrijk.dungeon.type", k.type().label(), k.zone()), 0xFF7A4AB0, true, false);
        b.lijn(Component.translatable("vadsrijk.dungeon.aanbevolen", k.minLv(), k.maxLv(), Component.translatable(k.aanbevolenSpelers())), 0xFFB07A10);
        k.venster(p, b);   // the dungeon type's own lines (boss, loot, rare rolls)
        // you
        b.kop(Component.translatable("vadsrijk.dungeon.jij", k.clears(p)));
        long w = wachttijd(p, dungeon);
        b.lijn(w > 0 ? Component.translatable("vadsrijk.dungeon.wachttijd", tijd(w)) : Component.translatable("vadsrijk.dungeon.wachttijd.geen"),
                w > 0 ? 0xFFC23A3A : 0xFF2F8A5A);
        // the status + buttons
        Instantie i = BEZET.get(dungeon);
        boolean lid = i != null && i.leden.contains(p.getUUID());
        String rij = rijVan(p.getUUID());
        boolean inRij = dungeon.equals(rij);
        long nu = p.level().getServer().getTickCount();
        if (lid) {
            b.lijn(Component.translatable("vadsrijk.dungeon.status.binnen", tijd((nu - i.start) * 50L)), 0xFF2F8A5A, true, false);
        } else if (i != null) {
            b.lijn(Component.translatable("vadsrijk.dungeon.status.bezet", i.leden.size(), tijd((nu - i.start) * 50L)), 0xFFC23A3A, true, false);
        } else {
            b.lijn(Component.translatable("vadsrijk.dungeon.status.vrij"), 0xFF2F8A5A, true, false);
        }
        if (inRij) {
            b.lijn(Component.translatable(i != null ? "vadsrijk.dungeon.status.rij.bezet" : "vadsrijk.dungeon.status.rij", rijGrootte(dungeon), GROEP_MAX),
                    0xFF7A4AB0, true, false);
        }
        if (CHECKS.containsKey(dungeon)) {
            b.lijn(Component.translatable("vadsrijk.dungeon.status.check"), 0xFFB07A10);
        }
        Deur d = DungeonDeur.hoofd(dungeon);
        if (lid) {
            boolean binnenKant = d != null && (k.bevat(p.getX(), p.getY(), p.getZ()) || DungeonDeur.binnenKant(p, d));
            if (binnenKant) {
                b.knop("verlaat", Component.translatable("vadsrijk.dungeon.knop.verlaat"), true, null);
                b.knop("blijf", Component.translatable("vadsrijk.dungeon.knop.blijf"), true, null);
            } else {
                b.knop("terug", Component.translatable("vadsrijk.dungeon.knop.terug"), !(i.klaar), null);
                b.knop("verlaat", Component.translatable("vadsrijk.dungeon.knop.verlaat"), true, null);
            }
        } else {
            boolean vrij = i == null && !CHECKS.containsKey(dungeon);
            boolean mag = weiger(p, dungeon, true) == null;
            Partijen.Party party = Partijen.party(p);
            int n = party == null ? 0 : Partijen.leden(p).size();
            b.knop("solo", Component.translatable("vadsrijk.dungeon.knop.solo"), vrij && mag, Component.translatable("vadsrijk.dungeon.tip.solo"));
            b.knop("groep", party == null ? Component.translatable("vadsrijk.dungeon.knop.groep.geen") : Component.translatable("vadsrijk.dungeon.knop.groep", n),
                    vrij && mag && party != null && party.leider.equals(p.getUUID()),
                    Component.translatable(party == null ? "vadsrijk.dungeon.tip.groep.geen" : "vadsrijk.dungeon.tip.groep"));
            if (inRij) {
                b.knop("rij_uit", Component.translatable("vadsrijk.dungeon.knop.rij.uit"), true, null);
                b.knop("nu", Component.translatable("vadsrijk.dungeon.knop.nu", rijGrootte(dungeon), GROEP_MAX), vrij, Component.translatable("vadsrijk.dungeon.tip.nu"));
            } else {
                b.knop("rij", Component.translatable("vadsrijk.dungeon.knop.rij"), mag, Component.translatable("vadsrijk.dungeon.tip.rij"));
            }
        }
        DungeonNet.stuur(p, b.klaar());
    }

    /** The ready check window: who is in, who said yes, the time bar, Klaar!/Nee. */
    public static void stuurCheck(ServerPlayer p, Check c, boolean open) {
        MinecraftServer s = p.level().getServer();
        DungeonNet.Bouw b = new DungeonNet.Bouw("klaar", c.dungeon, open, Component.translatable("vadsrijk.dungeon.klaar.titel", dungeonNaam(c.dungeon)),
                Component.translatable("vadsrijk.dungeon.klaar.uitleg"));
        for (UUID id : c.leden) {
            Boolean a = c.antwoord.get(id);
            String teken = a == null ? "…" : a ? "✔" : "✖";
            b.lijn(Component.literal(teken + "  " + naam(s, id)), a == null ? 0xFF9A8094 : a ? 0xFF2F8A5A : 0xFFC23A3A, a != null && a, false);
        }
        long over = Math.max(0, c.tot - s.getTickCount());
        b.meta.putInt("tijd", (int) over);
        b.meta.putInt("duur", CHECK_TICKS);
        if (c.antwoord.containsKey(p.getUUID())) {
            b.lijn(Component.translatable("vadsrijk.dungeon.klaar.wacht"), 0xFFB07A10);
        } else {
            b.knop("klaar_ja", Component.translatable("vadsrijk.dungeon.klaar.ja"), true, null);
            b.knop("klaar_nee", Component.translatable("vadsrijk.dungeon.klaar.nee"), true, null);
        }
        DungeonNet.stuur(p, b.klaar());
    }


    // ---- API for other slices (w-party: party tab, saved parties, NPC companions) -----------------------------------------------

    /** A non-player companion in a queue / group (an entity the caller spawns, or just a role): never loot, never a clear. */
    public record Metgezel(UUID id, String naam, String rol) {
    }

    /** Companions waiting in a queue (id -> companion); they say "yes" to the ready check at once. */
    public static final Map<UUID, Metgezel> METGEZELLEN = new LinkedHashMap<>();
    /** Queue blocks: members queued together (a party) share a block key (the leader's id) and are matched as one block. */
    public static final Map<UUID, UUID> BLOK = new HashMap<>();
    /** Since when (server tick) a dungeon's queue is not empty. */
    public static final Map<String, Long> RIJ_SINDS = new HashMap<>();

    /** Listener: a ready check ended ({@code allemaal} = everyone said yes). */
    public interface CheckHaak {
        void klaar(String dungeon, List<UUID> leden, Map<UUID, Boolean> antwoord, boolean allemaal, boolean uitRij);
    }

    /** Listener: a group went in (players teleported in; companions given, for the caller to spawn next to them). */
    public interface StartHaak {
        void start(Instantie i, List<ServerPlayer> spelers, List<Metgezel> metgezellen);
    }

    /** Add-only listener lists. */
    public static final List<CheckHaak> BIJ_CHECK = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final List<StartHaak> BIJ_START = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final List<java.util.function.Consumer<Instantie>> BIJ_EINDE = new java.util.concurrent.CopyOnWriteArrayList<>();

    static void gebeurtenis(Check c, boolean allemaal) {
        for (CheckHaak h : BIJ_CHECK) {
            h.klaar(c.dungeon, List.copyOf(c.leden), Map.copyOf(c.antwoord), allemaal, c.uitRij);
        }
    }

    /**
     * Queues a whole party at once (the leader's party; the leader only). Every online member must pass the rules (else a message
     * and false). The members are one block: matchmaking keeps them together. A party of {@link #GROEP_MAX} or more starts its
     * ready check at once.
     */
    public static boolean zetGroepInRij(ServerPlayer leider, String dungeon) {
        Partijen.Party party = Partijen.party(leider);
        if (party == null) {
            return inRij(leider, dungeon);
        }
        if (!party.leider.equals(leider.getUUID())) {
            zeg(leider, Component.translatable("vadsrijk.dungeon.msg.groep.leider"), ChatFormatting.GOLD);
            return false;
        }
        List<ServerPlayer> wie = Partijen.leden(leider);
        if (wie.size() > GROEP_MAX) {
            zeg(leider, Component.translatable("vadsrijk.dungeon.msg.groep.te.groot", GROEP_MAX), ChatFormatting.RED);
            return false;
        }
        for (ServerPlayer l : wie) {
            Component w = weiger(l, dungeon, l == leider);
            if (w != null) {
                zeg(leider, w, ChatFormatting.RED);
                return false;
            }
        }
        for (ServerPlayer l : wie) {
            uitRijWeg(l.getUUID(), null);
            RIJ.computeIfAbsent(dungeon, g -> new LinkedHashSet<>()).add(l.getUUID());
            BLOK.put(l.getUUID(), leider.getUUID());
            zeg(l, Component.translatable("vadsrijk.dungeon.msg.rij.groep", leider.getName(), dungeonNaam(dungeon)), ChatFormatting.LIGHT_PURPLE);
        }
        matchmaak(leider.level().getServer(), dungeon);
        return true;
    }

    /** Empty spots in the next batch of this dungeon's queue ({@link #GROEP_MAX} minus who is queued; 0 while a check runs). */
    public static int vrijePlekken(String dungeon) {
        return CHECKS.containsKey(dungeon) ? 0 : Math.max(0, GROEP_MAX - rijGrootte(dungeon));
    }

    /** The real (non-companion, non-made-up) players queued for this dungeon. */
    public static List<UUID> echteSpelersInRij(String dungeon) {
        List<UUID> l = new ArrayList<>();
        LinkedHashSet<UUID> r = RIJ.get(dungeon);
        if (r != null) {
            for (UUID id : r) {
                if (!METGEZELLEN.containsKey(id) && !NEP.containsKey(id)) {
                    l.add(id);
                }
            }
        }
        return l;
    }

    /**
     * Adds a non-player companion to this dungeon's queue (fills an empty spot; it says yes to the ready check; at the start it is
     * handed to {@link #BIJ_START} and kept in {@link Instantie#metgezellen}; no loot, no clear, no cooldown). False when the queue is
     * full or a check runs. Check {@link #geenNpcVulling} of the queued players first.
     */
    public static boolean voegMetgezelToe(String dungeon, Metgezel m) {
        if (vrijePlekken(dungeon) <= 0 || Dungeons.van(dungeon) == null) {
            return false;
        }
        METGEZELLEN.put(m.id(), m);
        RIJ.computeIfAbsent(dungeon, g -> new LinkedHashSet<>()).add(m.id());
        if (server != null) {
            matchmaak(server, dungeon);
        }
        return true;
    }

    /** Takes a companion out of every queue. */
    public static void haalMetgezelWeg(UUID id) {
        METGEZELLEN.remove(id);
        for (LinkedHashSet<UUID> r : RIJ.values()) {
            r.remove(id);
        }
    }

    /** The per-player "no NPC fill" flag (saved; w-party owns the switch). */
    public static boolean geenNpcVulling(ServerPlayer p) {
        return Spelers.data(p).extra(DATA).getBooleanOr("geenNpc", false);
    }

    public static void zetGeenNpcVulling(ServerPlayer p, boolean geen) {
        Spelers.data(p).extra(DATA).putBoolean("geenNpc", geen);
    }

    /** Ticks this dungeon's queue has not been empty (-1: empty). */
    public static long wachtTicks(String dungeon) {
        Long t = RIJ_SINDS.get(dungeon);
        return t == null || server == null ? -1 : server.getTickCount() - t;
    }

    // ---- dev commands ------------------------------------------------------------------------------------------------------------

    private static void commando(RegisterCommandsEvent event) {
        for (String wortel : List.of("dungeon", "grot")) {   // ("grot" = the alias of round U's cave commands)
            event.getDispatcher().register(VadsrijkCommando.wortel(commandos(wortel)));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandos(String wortel) {
        LiteralArgumentBuilder<CommandSourceStack> g = Commands.literal(wortel).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        var ids = (com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>) (c, b) -> SharedSuggestionProvider.suggest(
                Dungeons.alle().stream().map(Dungeon::id), b);
        // the window of a dungeon (as if you clicked its door), or to its door
        g.then(Commands.literal("deur").executes(c -> {
            StringBuilder sb = new StringBuilder("Dungeondeuren (staanplek voor de deur / binnen):");
            for (Deur d : DungeonDeur.deuren()) {
                sb.append("\n - ").append(d.dungeon()).append(d.hoofd() ? "" : " (2e deur)").append(": ").append(d.cellen().size()).append(" blokken, voor ")
                        .append(d.voor().toShortString()).append(", binnen ").append(d.binnen().toShortString());
            }
            String t = sb.toString();
            c.getSource().sendSuccess(() -> Component.literal(t), false);
            return 1;
        }).then(Commands.argument("id", StringArgumentType.word()).suggests(ids).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String id = StringArgumentType.getString(c, "id");
            if (Dungeons.van(id) == null) {
                c.getSource().sendFailure(Component.literal("Onbekende dungeon: " + id));
                return 0;
            }
            openDeur(p, id);
            return 1;
        }).then(Commands.literal("tp").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Deur d = DungeonDeur.hoofd(StringArgumentType.getString(c, "id"));
            if (d == null) {
                c.getSource().sendFailure(Component.literal("Geen deur voor die dungeon"));
                return 0;
            }
            naar(p, d.voor(), d.yawVoor());
            ServerLevel sl = wereld(p);
            if (sl != null) {
                DungeonDeur.plaats(sl, d);
            }
            return 1;
        }))));
        // made-up queue members to test the ready check alone
        g.then(Commands.literal("nep").then(Commands.argument("id", StringArgumentType.word()).suggests(ids)
                .then(Commands.argument("n", IntegerArgumentType.integer(1, GROEP_MAX - 1)).executes(c -> nep(c.getSource(), StringArgumentType.getString(c, "id"),
                        IntegerArgumentType.getInteger(c, "n"), "ja"))
                        .then(Commands.argument("antwoord", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("ja", "nee", "niets"), b))
                                .executes(c -> nep(c.getSource(), StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "n"),
                                        StringArgumentType.getString(c, "antwoord")))))));
        // the overview: every instance, queue and check
        g.then(Commands.literal("rij").executes(c -> {
            MinecraftServer s = c.getSource().getServer();
            StringBuilder sb = new StringBuilder("Dungeon-wachtrijen:");
            for (Dungeon k : Dungeons.alle()) {
                Instantie i = BEZET.get(k.id());
                LinkedHashSet<UUID> r = RIJ.get(k.id());
                Check ch = CHECKS.get(k.id());
                sb.append("\n - ").append(k.id()).append(": ").append(i == null ? "vrij" : "bezet door " + i.namen.values() + (i.klaar ? " (uitgespeeld)" : ""));
                if (r != null && !r.isEmpty()) {
                    sb.append(", rij ").append(r.stream().map(id -> naam(s, id)).toList());
                }
                if (ch != null) {
                    sb.append(", klaar-check ").append(ch.antwoord.size()).append("/").append(ch.leden.size());
                }
            }
            String t = sb.toString();
            c.getSource().sendSuccess(() -> Component.literal(t), false);
            return 1;
        }));
        // the cooldown: away (one dungeon or all), or set (seconds) to see it in the window
        g.then(Commands.literal("wachttijd").then(Commands.literal("weg").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Dungeon k : Dungeons.alle()) {
                zetWachttijd(p, k.id(), 0);
            }
            c.getSource().sendSuccess(() -> Component.literal("Wachttijd van alle grotten weg"), false);
            return 1;
        }).then(Commands.argument("id", StringArgumentType.word()).suggests(ids).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            zetWachttijd(p, StringArgumentType.getString(c, "id"), 0);
            c.getSource().sendSuccess(() -> Component.literal("Wachttijd weg"), false);
            return 1;
        }))).then(Commands.argument("id", StringArgumentType.word()).suggests(ids).then(Commands.argument("seconden", IntegerArgumentType.integer(0, 36000))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    zetWachttijd(p, StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "seconden") * 1000L);
                    c.getSource().sendSuccess(() -> Component.literal("Wachttijd gezet"), false);
                    return 1;
                }))));
        g.then(Commands.literal("verlaat").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String al = dungeonVan(p.getUUID());
            if (al == null) {
                c.getSource().sendFailure(Component.literal("Je zit in geen dungeon"));
                return 0;
            }
            verlaat(p, al);
            return 1;
        }));
        // dev: solo in without cooldown/rules
        g.then(Commands.literal("start").then(Commands.argument("id", StringArgumentType.word()).suggests(ids).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return solo(p, StringArgumentType.getString(c, "id"), true) ? 1 : 0;
        })));
        return g;
    }

    /** Dev: n made-up members in this dungeon's queue (+ you, when you are not in it yet); they answer the ready check as asked. */
    public static int nep(CommandSourceStack src, String dungeon, int n, String antwoord) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (Dungeons.van(dungeon) == null) {
            src.sendFailure(Component.literal("Onbekende dungeon: " + dungeon));
            return 0;
        }
        voegNepToe(dungeon, n, antwoord);
        if (!dungeon.equals(rijVan(p.getUUID()))) {
            inRij(p, dungeon);
        } else {
            matchmaak(src.getServer(), dungeon);
        }
        src.sendSuccess(() -> Component.literal(n + " nep-speler(s) in de wachtrij van " + dungeon + " (antwoord: " + antwoord + "). Rij: " + rijGrootte(dungeon) + "/" + GROEP_MAX
                + ". Vol = klaar-check; anders 'Nu starten' in het deurvenster (/guhs vadsrijk dungeon deur " + dungeon + ")."), false);
        return 1;
    }

    public static List<UUID> voegNepToe(String dungeon, int n, String antwoord) {
        List<UUID> l = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            UUID id = UUID.nameUUIDFromBytes(("guhs-dungeon-nep-" + (++nepTeller)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            NEP.put(id, "Testguh " + nepTeller);
            if (antwoord.equals("ja")) {
                NEP_ANTWOORD.put(id, true);
            } else if (antwoord.equals("nee")) {
                NEP_ANTWOORD.put(id, false);
            }
            RIJ.computeIfAbsent(dungeon, g -> new LinkedHashSet<>()).add(id);
            l.add(id);
        }
        return l;
    }
}
