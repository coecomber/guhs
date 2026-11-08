package nl.juiced.guhs.feature.vadsrijk.grotkerker;

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
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotDeurData.Deur;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.vint.RijdierBuit;

/**
 * W-GROT: who is inside which cave dungeon, the queue and the ready check (the "dungeon finder" behind the cave doors).
 * <ul>
 *   <li><b>Instance per group</b>: a cave dungeon belongs to ONE group at a time ({@link Instantie}): only its members may be inside
 *       (anybody else found in the cave box is put outside the door: {@link #uitzetten}); the run scales with the group. Another
 *       group that wants the same cave queues and goes in when it is free. (The caves are part of the map, with U-GROTTEN's puzzle,
 *       treasure and craft spots at fixed places, so a cave is not copied: the one cave is the group's private instance while
 *       they are in. Same model as the raid's tower lock.)</li>
 *   <li><b>Starting</b> (the door's buttons): <i>Solo starten</i>; <i>Starten met groep</i> = your party ({@link Partijen}, the
 *       leader starts, every member gets the ready check); <i>In de wachtrij</i> = matchmaking with the others queued for the SAME
 *       cave: at {@link #GROEP_MAX} (or when a queued player presses "Nu starten") everyone of that batch gets the ready check
 *       ("Klaar?" yes/no, {@link #CHECK_TICKS}); all yes = all are teleported in together.</li>
 *   <li><b>The queue</b> survives walking away; it drops you on logout or a dimension change. Queued players see a small status in
 *       the action bar ("Wachtrij De Zoutgrot: 2/4"). One queue at a time.</li>
 *   <li><b>Rules</b> (server side): not while in another cave dungeon or in the raid; not while your cooldown for that cave runs
 *       ({@link #WACHTTIJD_MS} after a clear, per player per cave, survives restarts); the door never opens for non-members.</li>
 *   <li><b>Out</b>: the door from the inside ("Grot verlaten"), or the window after the clear: you stand outside on the door's stand
 *       spot, which is OUTSIDE the cave box and never teleports anybody in (the raid's old exit loop cannot happen here: going in is
 *       only ever a button). A member who fainted outside can go back in with "Terug naar binnen".</li>
 *   <li><b>End</b>: when no member is inside for {@link #LEEG_TICKS} (a cleared cave: {@link #LEEG_KLAAR_TICKS}), or when the group
 *       left by the door; after a clear with another group waiting, the cave closes {@link #SLUIT_TICKS} later (members are put out).
 *       Then {@link GrotKerker#reset} and the next group can go.</li>
 * </ul>
 * Not saved (a restart empties every cave and queue, like parties and the raid). Dev: {@code /guhs vadsrijk grot deur|nep|rij|wachttijd}.
 */
public final class GrotRij {
    /** The group size the queue fills up to (the pack scaling tops out at 4: 3 + 3 extra in room 1). */
    public static final int GROEP_MAX = 4;
    /** A party may go in whole (Partijen.MAX = 5). */
    public static final int PARTY_MAX = Partijen.MAX;
    public static long WACHTTIJD_MS = 10L * 60 * 1000;
    public static int CHECK_TICKS = 20 * 30, LEEG_TICKS = 20 * 60, LEEG_KLAAR_TICKS = 20 * 10, SLUIT_TICKS = 20 * 120, POPUP_TICKS = 60;
    /** How far from the door's inside spot a member still counts as "in the cave" (the stair between the door and the cave box). */
    static final double BIJ_DEUR = 24;

    /** The group that owns a cave now. */
    public static final class Instantie {
        public final String grot;
        public final LinkedHashSet<UUID> leden = new LinkedHashSet<>();
        public final Map<UUID, String> namen = new LinkedHashMap<>();
        public long start, leegSinds = -1, sluitOp = -1, popupOp = -1, klaarOp = -1;
        /** The run was started for this group (once; a reset of an unfinished run clears it). */
        public boolean gestart;
        public boolean klaar;

        Instantie(String grot) {
            this.grot = grot;
        }
    }

    /** A ready check: a batch from the queue ({@code uitRij}) or a party start. */
    static final class Check {
        final String grot;
        final List<UUID> leden;
        final Map<UUID, Boolean> antwoord = new HashMap<>();
        final long start, tot;
        final boolean uitRij;
        final UUID starter;

        Check(String grot, List<UUID> leden, long nu, boolean uitRij, UUID starter) {
            this.grot = grot;
            this.leden = leden;
            this.start = nu;
            this.tot = nu + CHECK_TICKS;
            this.uitRij = uitRij;
            this.starter = starter;
        }
    }

    static final Map<String, Instantie> BEZET = new LinkedHashMap<>();
    static final Map<String, LinkedHashSet<UUID>> RIJ = new LinkedHashMap<>();
    static final Map<String, Check> CHECKS = new LinkedHashMap<>();
    /** Dev: made-up queue members (id -> name) and how they answer the ready check (true yes, false no, absent: never). */
    static final Map<UUID, String> NEP = new LinkedHashMap<>();
    static final Map<UUID, Boolean> NEP_ANTWOORD = new HashMap<>();
    /** Players with the door window open (id -> cave): they get a fresh state every second. */
    static final Map<UUID, String> KIJKERS = new HashMap<>();
    /** Just went out: door clicks ignored for a moment (no double click straight back in). */
    static final Map<UUID, Long> NET_UIT = new HashMap<>();
    /** Gametests: no dimension checks, the player's own level counts as Het Vadsrijk. */
    static boolean TEST;
    @Nullable
    static ServerLevel TEST_LEVEL;
    static int nepTeller;

    private GrotRij() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> wisAlles());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> uitRijWeg(e.getEntity().getUUID(), null));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && !Vadsrijk.in(p)) {
                uitRijWeg(p.getUUID(), "vadsrijk.grotdeur.msg.rij.weg.dim");
            }
        });
        NeoForge.EVENT_BUS.addListener(GrotRij::commando);
    }

    static void wisAlles() {
        BEZET.clear();
        RIJ.clear();
        CHECKS.clear();
        NEP.clear();
        NEP_ANTWOORD.clear();
        KIJKERS.clear();
        NET_UIT.clear();
        GrotDeur.nieuweSessie();
    }

    // ---- queries --------------------------------------------------------------------------------------------------------------

    @Nullable
    public static Instantie instantie(String grot) {
        return BEZET.get(grot);
    }

    /** The cave dungeon this player is a member of (null: none). */
    @Nullable
    public static String grotVan(UUID id) {
        for (Instantie i : BEZET.values()) {
            if (i.leden.contains(id)) {
                return i.grot;
            }
        }
        return null;
    }

    public static boolean lid(ServerPlayer p, String grot) {
        Instantie i = BEZET.get(grot);
        return i != null && i.leden.contains(p.getUUID());
    }

    /** The cave whose queue this player is in (null: none). */
    @Nullable
    public static String rijVan(UUID id) {
        for (var e : RIJ.entrySet()) {
            if (e.getValue().contains(id)) {
                return e.getKey();
            }
        }
        return null;
    }

    public static int rijGrootte(String grot) {
        LinkedHashSet<UUID> r = RIJ.get(grot);
        return r == null ? 0 : r.size();
    }

    /** Remaining cooldown for this cave in ms (0: none). */
    public static long wachttijd(ServerPlayer p, String grot) {
        long tot = Spelers.data(p).extra(GrotKerker.DATA).getLongOr(grot + ".wachttijdTot", 0L);
        return Math.max(0, tot - System.currentTimeMillis());
    }

    static void zetWachttijd(ServerPlayer p, String grot, long ms) {
        if (ms <= 0) {
            Spelers.data(p).extra(GrotKerker.DATA).remove(grot + ".wachttijdTot");
        } else {
            Spelers.data(p).extra(GrotKerker.DATA).putLong(grot + ".wachttijdTot", System.currentTimeMillis() + ms);
        }
    }

    static String tijd(long ms) {
        long s = (ms + 999) / 1000;
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    static Component grotNaam(String grot) {
        return Component.translatable("vadsrijk.grotkerker.grot." + grot);
    }

    static boolean inVadsrijk(ServerPlayer p) {
        return TEST || Vadsrijk.in(p);
    }

    @Nullable
    static ServerLevel wereld(ServerPlayer p) {
        return TEST ? (ServerLevel) p.level() : Vadsrijk.level(p.level().getServer());
    }

    @Nullable
    static ServerPlayer speler(MinecraftServer s, UUID id) {
        return s.getPlayerList().getPlayer(id);
    }

    static String naam(MinecraftServer s, UUID id) {
        String n = NEP.get(id);
        if (n != null) {
            return n;
        }
        ServerPlayer p = speler(s, id);
        return p == null ? "?" : p.getGameProfile().name();
    }

    /** Why this player may not go into this cave now (null: may). Checks: Het Vadsrijk, another dungeon, the raid, the cooldown. */
    @Nullable
    static Component weiger(ServerPlayer p, String grot, boolean zelf) {
        if (!inVadsrijk(p)) {
            return Component.translatable("vadsrijk.grotdeur.msg.niet.vadsrijk");
        }
        String al = grotVan(p.getUUID());
        if (al != null) {
            return zelf ? Component.translatable("vadsrijk.grotdeur.msg.al.kerker", grotNaam(al))
                    : Component.translatable("vadsrijk.grotdeur.msg.bezet.ander", p.getName());
        }
        if (inRaid(p)) {
            return zelf ? Component.translatable("vadsrijk.grotdeur.msg.al.raid") : Component.translatable("vadsrijk.grotdeur.msg.bezet.ander", p.getName());
        }
        long w = wachttijd(p, grot);
        if (w > 0) {
            return zelf ? Component.translatable("vadsrijk.grotdeur.msg.cooldown", tijd(w), grotNaam(grot))
                    : Component.translatable("vadsrijk.grotdeur.msg.cooldown.ander", p.getName(), tijd(w));
        }
        return null;
    }

    static boolean inRaid(ServerPlayer p) {
        try {
            return nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6Raid.inGroep(p);
        } catch (LinkageError e) {
            return false;
        }
    }

    static void zeg(ServerPlayer p, Component c, ChatFormatting kleur) {
        p.sendSystemMessage(c.copy().withStyle(kleur));
    }

    // ---- the buttons ----------------------------------------------------------------------------------------------------------

    /** A button of the window (from the client; checked here). */
    static void actie(ServerPlayer p, String wat, String grot) {
        if (wat.equals("dicht")) {
            KIJKERS.remove(p.getUUID());
            return;
        }
        if (wat.equals("klaar_ja") || wat.equals("klaar_nee")) {
            antwoord(p, wat.equals("klaar_ja"));
            return;
        }
        if (Kerkers.van(grot) == null) {
            return;
        }
        switch (wat) {
            case "solo" -> solo(p, grot, false);
            case "groep" -> groep(p, grot);
            case "rij" -> inRij(p, grot);
            case "rij_uit" -> uitRij(p);
            case "nu" -> nuStarten(p);
            case "terug" -> terug(p, grot);
            case "verlaat" -> verlaat(p, grot);
            case "blijf" -> KIJKERS.remove(p.getUUID());
            default -> {
            }
        }
        if (KIJKERS.containsKey(p.getUUID())) {
            stuurDeur(p, grot, false);
        }
    }

    /** The door was right-clicked. */
    static void klikDeur(ServerLevel sl, BlockPos pos, ServerPlayer p) {
        Deur d = GrotDeur.op(pos);
        if (d == null) {
            return;
        }
        long nu = sl.getServer().getTickCount();
        Long uit = NET_UIT.get(p.getUUID());
        if (uit != null && nu - uit < 20) {
            return;
        }
        openDeur(p, d.grot());
    }

    /** Opens the door window for this cave (also the dev command). */
    public static void openDeur(ServerPlayer p, String grot) {
        KIJKERS.put(p.getUUID(), grot);
        stuurDeur(p, grot, true);
    }

    /** Solo start ({@code dev}: rules and cooldown skipped). True = inside. */
    public static boolean solo(ServerPlayer p, String grot, boolean dev) {
        if (!dev) {
            Component w = weiger(p, grot, true);
            if (w != null) {
                zeg(p, w, ChatFormatting.RED);
                return false;
            }
        } else {
            String al = grotVan(p.getUUID());
            if (al != null && !al.equals(grot)) {
                verlaat(p, al);
            }
        }
        Instantie i = BEZET.get(grot);
        if (i != null) {
            if (dev && i.leden.contains(p.getUUID())) {
                terug(p, grot);
                return true;
            }
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.bezet"), ChatFormatting.GOLD);
            return false;
        }
        if (CHECKS.containsKey(grot)) {
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.check.bezig"), ChatFormatting.GOLD);
            return false;
        }
        uitRijWeg(p.getUUID(), null);
        return binnen(grot, List.of(p)) != null;
    }

    /** Start with the party: the leader starts, every online member gets the ready check (the leader is "yes" at once). */
    static void groep(ServerPlayer p, String grot) {
        Partijen.Party party = Partijen.party(p);
        if (party == null) {
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.geen.groep"), ChatFormatting.GOLD);
            return;
        }
        if (!party.leider.equals(p.getUUID())) {
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.groep.leider"), ChatFormatting.GOLD);
            return;
        }
        List<ServerPlayer> wie = Partijen.leden(p);
        if (wie.size() > PARTY_MAX) {
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.groep.te.groot", PARTY_MAX), ChatFormatting.RED);
            return;
        }
        for (ServerPlayer l : wie) {
            Component w = weiger(l, grot, l == p);
            if (w != null) {
                zeg(p, w, ChatFormatting.RED);
                return;
            }
        }
        if (BEZET.containsKey(grot)) {
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.bezet"), ChatFormatting.GOLD);
            return;
        }
        if (CHECKS.containsKey(grot)) {
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.check.bezig"), ChatFormatting.GOLD);
            return;
        }
        List<UUID> ids = new ArrayList<>();
        for (ServerPlayer l : wie) {
            ids.add(l.getUUID());
        }
        startCheck(p.level().getServer(), grot, ids, false, p.getUUID());
    }

    /** Into the queue of this cave (out of any other queue). */
    public static boolean inRij(ServerPlayer p, String grot) {
        Component w = weiger(p, grot, true);
        if (w != null) {
            zeg(p, w, ChatFormatting.RED);
            return false;
        }
        String al = rijVan(p.getUUID());
        if (grot.equals(al)) {
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.al.rij", grotNaam(grot)), ChatFormatting.GOLD);
            return true;
        }
        uitRijWeg(p.getUUID(), null);
        RIJ.computeIfAbsent(grot, g -> new LinkedHashSet<>()).add(p.getUUID());
        zeg(p, Component.translatable("vadsrijk.grotdeur.msg.rij.erin", grotNaam(grot), rijGrootte(grot), GROEP_MAX), ChatFormatting.LIGHT_PURPLE);
        matchmaak(p.level().getServer(), grot);
        return true;
    }

    public static void uitRij(ServerPlayer p) {
        String g = rijVan(p.getUUID());
        uitRijWeg(p.getUUID(), null);
        if (g != null) {
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.rij.uit", grotNaam(g)), ChatFormatting.GRAY);
        }
    }

    /** Out of every queue + any running check (logout, dimension change, a solo start). {@code melding}: told to the player. */
    static void uitRijWeg(UUID id, @Nullable String melding) {
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
                    zeg(p, Component.translatable(melding, grotNaam(g)), ChatFormatting.GRAY);
                }
            }
        }
    }

    private static MinecraftServer server;

    static List<ServerPlayer> alleSpelers() {
        return server == null ? List.of() : server.getPlayerList().getPlayers();
    }

    /** A queued player: start now with whoever is in the queue (fewer than 4 is fine). */
    public static void nuStarten(ServerPlayer p) {
        String g = rijVan(p.getUUID());
        if (g == null) {
            return;
        }
        if (BEZET.containsKey(g)) {
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.bezet"), ChatFormatting.GOLD);
            return;
        }
        if (CHECKS.containsKey(g)) {
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.check.bezig"), ChatFormatting.GOLD);
            return;
        }
        List<UUID> l = new ArrayList<>(RIJ.get(g));
        startCheck(p.level().getServer(), g, new ArrayList<>(l.subList(0, Math.min(GROEP_MAX, l.size()))), true, p.getUUID());
    }

    /** A member back in (after fainting outside, say). */
    static void terug(ServerPlayer p, String grot) {
        Instantie i = BEZET.get(grot);
        Deur d = GrotDeur.hoofd(grot);
        if (i == null || d == null || !i.leden.contains(p.getUUID())) {
            return;
        }
        naar(p, d.binnen(), d.yawBinnen());
        KIJKERS.remove(p.getUUID());
        GrotNet.stuur(p, GrotNet.sluit("deur"));
    }

    /** Out through the door: stand outside on the main door's stand spot; the member leaves the group. */
    public static void verlaat(ServerPlayer p, String grot) {
        Instantie i = BEZET.get(grot);
        Deur d = GrotDeur.hoofd(grot);
        if (d == null) {
            return;
        }
        if (i != null) {
            i.leden.remove(p.getUUID());
        }
        naarBuiten(p, grot, d);
        zeg(p, Component.translatable("vadsrijk.grotdeur.msg.uit", grotNaam(grot)), ChatFormatting.LIGHT_PURPLE);
        if (i != null && i.leden.isEmpty()) {
            sluit(p.level().getServer(), grot, null);
        }
    }

    static void naarBuiten(ServerPlayer p, String grot, Deur d) {
        naar(p, d.voor(), d.yawVoor());
        NET_UIT.put(p.getUUID(), (long) p.level().getServer().getTickCount());
        KIJKERS.remove(p.getUUID());
        GrotNet.stuur(p, GrotNet.sluit("deur"));
        ServerLevel sl = wereld(p);
        if (sl != null) {
            GrotDeur.geluid(sl, d, false);
        }
    }

    static void naar(ServerPlayer p, BlockPos b, float yaw) {
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

    static void startCheck(MinecraftServer s, String grot, List<UUID> leden, boolean uitRij, UUID starter) {
        server = s;
        long nu = s.getTickCount();
        Check c = new Check(grot, leden, nu, uitRij, starter);
        if (!uitRij) {
            c.antwoord.put(starter, true);
        }
        CHECKS.put(grot, c);
        for (UUID id : leden) {
            ServerPlayer p = speler(s, id);
            if (p != null) {
                zeg(p, Component.translatable("vadsrijk.grotdeur.msg.check.start", grotNaam(grot), CHECK_TICKS / 20), ChatFormatting.GOLD);
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
                            l.sendSystemMessage(Component.translatable("vadsrijk.grotdeur.msg.klaar.ja", p.getName()).withStyle(ChatFormatting.GRAY));
                        }
                    }
                }
                verwerk(p.level().getServer(), c);
                return;
            }
        }
    }

    /** All answered (or time up): in together, or the decliners / silent ones out. */
    static void verwerk(MinecraftServer s, Check c) {
        long nu = s.getTickCount();
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
        CHECKS.remove(c.grot);
        for (UUID id : c.leden) {
            ServerPlayer p = speler(s, id);
            if (p != null) {
                GrotNet.stuur(p, GrotNet.sluit("klaar"));
            }
        }
        if (alle) {
            List<ServerPlayer> wie = new ArrayList<>();
            List<String> nep = new ArrayList<>();
            for (UUID id : c.leden) {
                ServerPlayer p = speler(s, id);
                if (NEP.containsKey(id)) {
                    nep.add(NEP.get(id));
                } else if (p != null && weiger(p, c.grot, true) == null) {
                    wie.add(p);
                }
            }
            for (UUID id : c.leden) {
                LinkedHashSet<UUID> r = RIJ.get(c.grot);
                if (r != null) {
                    r.remove(id);
                }
                NEP.remove(id);
                NEP_ANTWOORD.remove(id);
            }
            if (!wie.isEmpty() && !BEZET.containsKey(c.grot)) {
                binnen(c.grot, wie);
                for (ServerPlayer p : wie) {
                    for (String n : nep) {
                        p.sendSystemMessage(Component.translatable("vadsrijk.grotdeur.msg.nep.buiten", n).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
            return;
        }
        // somebody said no / the time ran out: from the queue, the "no" and silent ones leave it; the rest stays queued
        List<String> weg = new ArrayList<>();
        for (UUID id : c.leden) {
            Boolean a = c.antwoord.get(id);
            if (!Boolean.TRUE.equals(a)) {
                weg.add(naam(s, id));
                if (c.uitRij) {
                    LinkedHashSet<UUID> r = RIJ.get(c.grot);
                    if (r != null) {
                        r.remove(id);
                    }
                    NEP.remove(id);
                    NEP_ANTWOORD.remove(id);
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
                zeg(p, Component.translatable(c.uitRij ? "vadsrijk.grotdeur.msg.check.nee" : "vadsrijk.grotdeur.msg.check.nee.groep", namen), ChatFormatting.GOLD);
            } else {
                zeg(p, Component.translatable(c.uitRij ? "vadsrijk.grotdeur.msg.check.tijd" : "vadsrijk.grotdeur.msg.check.tijd.groep"), ChatFormatting.GOLD);
            }
        }
    }

    /** The queue of this cave is full (or ready to go) and the cave is free: a ready check for the first batch. */
    static void matchmaak(MinecraftServer s, String grot) {
        server = s;
        LinkedHashSet<UUID> r = RIJ.get(grot);
        if (r == null || r.size() < GROEP_MAX || BEZET.containsKey(grot) || CHECKS.containsKey(grot)) {
            return;
        }
        List<UUID> l = new ArrayList<>(r);
        startCheck(s, grot, new ArrayList<>(l.subList(0, GROEP_MAX)), true, l.get(0));
    }

    // ---- in / out of the instance ---------------------------------------------------------------------------------------------

    /** The group goes in: the cave is theirs, everyone stands just inside the door; the run starts when the first one is in the cave. */
    @Nullable
    static Instantie binnen(String grot, List<ServerPlayer> wie) {
        Kerker k = Kerkers.van(grot);
        Deur d = GrotDeur.hoofd(grot);
        if (k == null || d == null || wie.isEmpty()) {
            return null;
        }
        ServerPlayer eerste = wie.get(0);
        server = eerste.level().getServer();
        ServerLevel sl = wereld(eerste);
        if (sl == null) {
            return null;
        }
        GrotKerker.Run r = GrotKerker.run(k.id());
        if (r.staat != GrotKerker.Staat.SLAAPT) {
            GrotKerker.reset(sl, k, r, null);   // (a stale run of nobody: fresh for the new group)
        }
        Instantie i = new Instantie(grot);
        i.start = server.getTickCount();
        for (ServerPlayer p : wie) {
            i.leden.add(p.getUUID());
            i.namen.put(p.getUUID(), p.getGameProfile().name());
            uitRijWeg(p.getUUID(), null);
        }
        BEZET.put(grot, i);
        GrotDeur.geluid(sl, d, true);
        int n = wie.size();
        for (ServerPlayer p : wie) {
            naar(p, d.binnen(), d.yawBinnen());
            GrotNet.stuur(p, GrotNet.sluit("deur"));
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.binnen", grotNaam(grot), n,
                    Component.translatable(n == 1 ? "vadsrijk.grotdeur.msg.binnen.speler" : "vadsrijk.grotdeur.msg.binnen.spelers")), ChatFormatting.GOLD);
            RijkScenes.titel(p, grotNaam(grot).copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    Component.translatable("vadsrijk.grotkerker.baas." + grot + ".onder"), 60, 0xFF8CC8);
        }
        return i;
    }

    /** The cave closes for its group: the run resets, the instance goes, the next group may come. */
    static void sluit(MinecraftServer s, String grot, @Nullable String melding) {
        Instantie i = BEZET.remove(grot);
        Kerker k = Kerkers.van(grot);
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
            GrotKerker.reset(sl, k, GrotKerker.run(k.id()), null);
        }
        matchmaak(s, grot);
    }

    /** (GrotKerker) The boss is beaten: the group's cave is cleared; the cooldown for everyone who counted; the window soon. */
    static void klaar(ServerLevel sl, String grot, Set<ServerPlayer> wie) {
        Instantie i = BEZET.get(grot);
        long nu = sl.getServer().getTickCount();
        if (i != null) {
            i.klaar = true;
            i.klaarOp = nu;
            i.popupOp = nu + POPUP_TICKS;
        }
        for (ServerPlayer p : wie) {
            zetWachttijd(p, grot, WACHTTIJD_MS);
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.wachttijd.gezet", grotNaam(grot), tijd(WACHTTIJD_MS)), ChatFormatting.GRAY);
            zeg(p, Component.translatable("vadsrijk.grotdeur.msg.klaar.uit"), ChatFormatting.LIGHT_PURPLE);
        }
    }

    /** (GrotKerker.reset) An unfinished run reset (everyone left the cave box for a while): it may start again for the group. */
    static void runGereset(String grot) {
        Instantie i = BEZET.get(grot);
        if (i != null && !i.klaar) {
            i.gestart = false;
        }
    }

    /** May GrotKerker start the run now? Only for the cave's group, once per instance. */
    static boolean magStarten(String grot) {
        Instantie i = BEZET.get(grot);
        return i != null && !i.gestart && !i.klaar;
    }

    static void gestart(String grot) {
        Instantie i = BEZET.get(grot);
        if (i != null) {
            i.gestart = true;
        }
    }

    /** Dev (grot tp / baas / run): this player becomes a member of the cave's group (a new group of one when the cave is free), no teleport. */
    public static void devToegang(ServerPlayer p, String grot) {
        if (Kerkers.van(grot) == null || lid(p, grot)) {
            return;
        }
        String al = grotVan(p.getUUID());
        if (al != null) {
            Instantie a = BEZET.get(al);
            a.leden.remove(p.getUUID());
            if (a.leden.isEmpty()) {
                sluit(p.level().getServer(), al, null);
            }
        }
        uitRijWeg(p.getUUID(), null);
        Instantie i = BEZET.computeIfAbsent(grot, g -> {
            Instantie n = new Instantie(g);
            n.start = p.level().getServer().getTickCount();
            return n;
        });
        i.leden.add(p.getUUID());
        i.namen.put(p.getUUID(), p.getGameProfile().name());
    }

    /** Is this player still "in" the group's cave (online, in the cave box or on the way between the door and the box)? */
    static boolean aanwezig(MinecraftServer s, UUID id, Kerker k, Deur d) {
        ServerPlayer p = speler(s, id);
        if (p == null || !inVadsrijk(p) || p.isRemoved()) {
            return false;
        }
        if (k.bevat(p.getX(), p.getY(), p.getZ())) {
            return true;
        }
        return p.distanceToSqr(d.binnen().getX() + 0.5, d.binnen().getY(), d.binnen().getZ() + 0.5) <= BIJ_DEUR * BIJ_DEUR && GrotDeur.binnenKant(p, d);
    }

    /** Anyone in the cave box who is not a member of its group (and not creative/spectator) is put outside the main door. */
    static void uitzetten(ServerLevel sl, Kerker k) {
        Instantie i = BEZET.get(k.id());
        Deur d = GrotDeur.hoofd(k.id());
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
            p.sendSystemMessage(Component.translatable(i == null ? "vadsrijk.grotdeur.msg.niet.gestart" : "vadsrijk.grotdeur.msg.niet.van.jou")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    // ---- the tick ---------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer s) {
        server = s;
        long nu = s.getTickCount();
        if (nu % 10 != 0) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(s);
        if (sl != null) {
            GrotDeur.tick(sl);
            for (Kerker k : Kerkers.alle()) {
                if (!Kerkers.TEST.containsKey(k.id())) {
                    uitzetten(sl, k);
                }
            }
        }
        tickRij(s, nu);
    }

    /** The queue, checks, instances and windows (also called by the gametests). */
    static void tickRij(MinecraftServer s, long nu) {
        server = s;
        // made-up members answer after a second
        for (Check c : new ArrayList<>(CHECKS.values())) {
            if (nu - c.start >= 20) {
                for (UUID id : c.leden) {
                    if (NEP.containsKey(id) && NEP_ANTWOORD.containsKey(id) && !c.antwoord.containsKey(id)) {
                        c.antwoord.put(id, NEP_ANTWOORD.get(id));
                    }
                }
            }
            verwerk(s, c);
        }
        // the queue: gone players out, the action bar status, matchmaking
        for (var e : new ArrayList<>(RIJ.entrySet())) {
            String g = e.getKey();
            for (Iterator<UUID> it = e.getValue().iterator(); it.hasNext();) {
                UUID id = it.next();
                if (NEP.containsKey(id)) {
                    continue;
                }
                ServerPlayer p = speler(s, id);
                if (p == null) {
                    it.remove();
                } else if (!inVadsrijk(p)) {
                    it.remove();
                    zeg(p, Component.translatable("vadsrijk.grotdeur.msg.rij.weg.dim", grotNaam(g)), ChatFormatting.GRAY);
                }
            }
            if (nu % 40 == 0) {
                boolean bezet = BEZET.containsKey(g);
                for (UUID id : e.getValue()) {
                    ServerPlayer p = speler(s, id);
                    if (p != null && !CHECKS.containsKey(g)) {
                        p.sendOverlayMessage(Component.translatable(bezet ? "vadsrijk.grotdeur.msg.rij.status.bezet" : "vadsrijk.grotdeur.msg.rij.status",
                                grotNaam(g), e.getValue().size(), GROEP_MAX).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
            }
            if (e.getValue().isEmpty()) {
                RIJ.remove(g);
            } else {
                matchmaak(s, g);
            }
        }
        // the instances: who is still in, the end, the closing when another group waits, the window after the clear
        for (Instantie i : new ArrayList<>(BEZET.values())) {
            Kerker k = Kerkers.van(i.grot);
            Deur d = GrotDeur.hoofd(i.grot);
            if (k == null || d == null) {
                BEZET.remove(i.grot);
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
                sluit(s, i.grot, "vadsrijk.grotdeur.msg.leeg");
                continue;
            }
            boolean wacht = rijGrootte(i.grot) > 0 || CHECKS.containsKey(i.grot);
            if (i.klaar && wacht && i.sluitOp < 0) {
                i.sluitOp = nu + SLUIT_TICKS;
                for (UUID id : i.leden) {
                    ServerPlayer p = speler(s, id);
                    if (p != null) {
                        zeg(p, Component.translatable("vadsrijk.grotdeur.msg.sluit.straks", tijd(SLUIT_TICKS * 50L)), ChatFormatting.GOLD);
                    }
                }
            }
            if (i.sluitOp >= 0 && nu >= i.sluitOp) {
                for (UUID id : new ArrayList<>(i.leden)) {
                    ServerPlayer p = speler(s, id);
                    if (p != null && aanwezig(s, id, k, d)) {
                        naarBuiten(p, i.grot, d);
                    }
                }
                sluit(s, i.grot, "vadsrijk.grotdeur.msg.gesloten");
                continue;
            }
            if (i.popupOp >= 0 && nu >= i.popupOp) {
                i.popupOp = -1;
                for (UUID id : i.leden) {
                    ServerPlayer p = speler(s, id);
                    if (p != null && aanwezig(s, id, k, d)) {
                        openDeur(p, i.grot);
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

    /** The door window: the cave, the advice, the boss, the loot (+ the personal rare rolls with pity), your clears/cooldown, buttons. */
    static void stuurDeur(ServerPlayer p, String grot, boolean open) {
        Kerker k = Kerkers.van(grot);
        if (k == null) {
            return;
        }
        GrotNet.Bouw b = new GrotNet.Bouw("deur", grot, open, grotNaam(grot), Component.translatable("vadsrijk.grotdeur.zone", k.zone()));
        int zone = k.zone();
        b.lijn(Component.translatable("vadsrijk.grotdeur.aanbevolen", k.minLv(), k.baasLv(),
                Component.translatable(zone <= 2 ? "vadsrijk.grotdeur.spelers.solo" : "vadsrijk.grotdeur.spelers.groep")), 0xFFB07A10);
        b.lijn(Component.translatable("vadsrijk.grotdeur.baas", Component.translatable("vadsrijk.grotkerker.baas." + grot)), 0xFF7A2A5A, true, false);
        b.lijn(Component.translatable("vadsrijk.grotkerker.baas." + grot + ".onder").withStyle(ChatFormatting.ITALIC), 0xFF9A8094);
        // loot (KerkerLoot: every clear)
        b.kop(Component.translatable("vadsrijk.grotdeur.buit"));
        b.lijn(Component.translatable("vadsrijk.grotdeur.buit.munten", 400L * zone * zone + 300), 0xFF5A3A56);
        boolean eerste = GrotKerker.clears(p, grot) == 0;
        b.lijn(eerste ? Component.translatable("vadsrijk.grotdeur.buit.stuk.eerste", k.baasLv())
                : Component.translatable("vadsrijk.grotdeur.buit.stuk", k.baasLv(), Math.round(KerkerLoot.EPISCH_KANS * 100)), 0xFF5A3A56);
        b.lijn(Component.translatable("vadsrijk.grotdeur.buit.erts"), 0xFF5A3A56);
        if (zone <= 5) {
            b.lijn(Component.translatable("vadsrijk.grotdeur.buit.kern", Math.round(KerkerLoot.KERN_KANS * 100)), 0xFF5A3A56);
        }
        // the rare personal rolls, the same numbers as /guhs vadsrijk loot kansen
        GrotBazen.Def def = GrotBazen.def(grot);
        if (def != null) {
            b.kop(Component.translatable("vadsrijk.grotdeur.zeldzaam"));
            Component pet = Component.translatable("vadsrijk.huisdier." + def.pet() + ".naam");
            if (Huisdieren.heeft(p, def.pet())) {
                b.lijn(Component.translatable("vadsrijk.grotdeur.pet.heb", pet), 0xFF2F8A5A);
            } else {
                int pech = Spelers.data(p).extra(GrotKerker.DATA).getIntOr(grot + ".petpech", 0);
                b.lijn(Component.translatable("vadsrijk.grotdeur.pet", pet, Math.round(KerkerLoot.PET_KANS * 100), pech, KerkerLoot.PET_PITY), 0xFF7A4AB0);
            }
            Component kleur = Component.translatable("vadsrijk.economie.pluimkleur." + def.pluim().name().toLowerCase(Locale.ROOT));
            RijdierBuit.Bron bron = RijdierBuit.BRONNEN.get(def.mountBron());
            if (bron != null && bron.heeft().test(p)) {
                b.lijn(Component.translatable("vadsrijk.grotdeur.pluim.heb", kleur), 0xFF2F8A5A);
            } else if (bron != null && !bron.mag().test(p)) {
                b.lijn(Component.translatable("vadsrijk.grotdeur.pluim.geen", kleur, Math.round(RijdierBuit.KANS * 100)), 0xFF9A8094);
            } else {
                b.lijn(Component.translatable("vadsrijk.grotdeur.pluim", kleur, Math.round(RijdierBuit.KANS * 100), RijdierBuit.pech(p, def.mountBron()),
                        RijdierBuit.PITY), 0xFF7A4AB0);
            }
            b.lijn(Component.translatable("vadsrijk.grotdeur.pech.uitleg"), 0xFF9A8094);
        }
        // you
        b.kop(Component.translatable("vadsrijk.grotdeur.jij", GrotKerker.clears(p, grot)));
        long w = wachttijd(p, grot);
        b.lijn(w > 0 ? Component.translatable("vadsrijk.grotdeur.wachttijd", tijd(w)) : Component.translatable("vadsrijk.grotdeur.wachttijd.geen"),
                w > 0 ? 0xFFC23A3A : 0xFF2F8A5A);
        // the status + buttons
        Instantie i = BEZET.get(grot);
        boolean lid = i != null && i.leden.contains(p.getUUID());
        String rij = rijVan(p.getUUID());
        boolean inRij = grot.equals(rij);
        long nu = p.level().getServer().getTickCount();
        if (lid) {
            b.lijn(Component.translatable("vadsrijk.grotdeur.status.binnen", tijd((nu - i.start) * 50L)), 0xFF2F8A5A, true, false);
        } else if (i != null) {
            b.lijn(Component.translatable("vadsrijk.grotdeur.status.bezet", i.leden.size(), tijd((nu - i.start) * 50L)), 0xFFC23A3A, true, false);
        } else {
            b.lijn(Component.translatable("vadsrijk.grotdeur.status.vrij"), 0xFF2F8A5A, true, false);
        }
        if (inRij) {
            b.lijn(Component.translatable(i != null ? "vadsrijk.grotdeur.status.rij.bezet" : "vadsrijk.grotdeur.status.rij", rijGrootte(grot), GROEP_MAX),
                    0xFF7A4AB0, true, false);
        }
        if (CHECKS.containsKey(grot)) {
            b.lijn(Component.translatable("vadsrijk.grotdeur.status.check"), 0xFFB07A10);
        }
        Deur d = GrotDeur.hoofd(grot);
        if (lid) {
            boolean binnenKant = d != null && (k.bevat(p.getX(), p.getY(), p.getZ()) || GrotDeur.binnenKant(p, d));
            if (binnenKant) {
                b.knop("verlaat", Component.translatable("vadsrijk.grotdeur.knop.verlaat"), true, null);
                b.knop("blijf", Component.translatable("vadsrijk.grotdeur.knop.blijf"), true, null);
            } else {
                b.knop("terug", Component.translatable("vadsrijk.grotdeur.knop.terug"), !(i.klaar), null);
                b.knop("verlaat", Component.translatable("vadsrijk.grotdeur.knop.verlaat"), true, null);
            }
        } else {
            boolean vrij = i == null && !CHECKS.containsKey(grot);
            boolean mag = weiger(p, grot, true) == null;
            Partijen.Party party = Partijen.party(p);
            int n = party == null ? 0 : Partijen.leden(p).size();
            b.knop("solo", Component.translatable("vadsrijk.grotdeur.knop.solo"), vrij && mag, Component.translatable("vadsrijk.grotdeur.tip.solo"));
            b.knop("groep", party == null ? Component.translatable("vadsrijk.grotdeur.knop.groep.geen") : Component.translatable("vadsrijk.grotdeur.knop.groep", n),
                    vrij && mag && party != null && party.leider.equals(p.getUUID()),
                    Component.translatable(party == null ? "vadsrijk.grotdeur.tip.groep.geen" : "vadsrijk.grotdeur.tip.groep"));
            if (inRij) {
                b.knop("rij_uit", Component.translatable("vadsrijk.grotdeur.knop.rij.uit"), true, null);
                b.knop("nu", Component.translatable("vadsrijk.grotdeur.knop.nu", rijGrootte(grot), GROEP_MAX), vrij, Component.translatable("vadsrijk.grotdeur.tip.nu"));
            } else {
                b.knop("rij", Component.translatable("vadsrijk.grotdeur.knop.rij"), mag, Component.translatable("vadsrijk.grotdeur.tip.rij"));
            }
        }
        GrotNet.stuur(p, b.klaar());
    }

    /** The ready check window: who is in, who said yes, the time bar, Klaar!/Nee. */
    static void stuurCheck(ServerPlayer p, Check c, boolean open) {
        MinecraftServer s = p.level().getServer();
        GrotNet.Bouw b = new GrotNet.Bouw("klaar", c.grot, open, Component.translatable("vadsrijk.grotdeur.klaar.titel", grotNaam(c.grot)),
                Component.translatable("vadsrijk.grotdeur.klaar.uitleg"));
        for (UUID id : c.leden) {
            Boolean a = c.antwoord.get(id);
            String teken = a == null ? "…" : a ? "✔" : "✖";
            b.lijn(Component.literal(teken + "  " + naam(s, id)), a == null ? 0xFF9A8094 : a ? 0xFF2F8A5A : 0xFFC23A3A, a != null && a, false);
        }
        long over = Math.max(0, c.tot - s.getTickCount());
        b.meta.putInt("tijd", (int) over);
        b.meta.putInt("duur", CHECK_TICKS);
        if (c.antwoord.containsKey(p.getUUID())) {
            b.lijn(Component.translatable("vadsrijk.grotdeur.klaar.wacht"), 0xFFB07A10);
        } else {
            b.knop("klaar_ja", Component.translatable("vadsrijk.grotdeur.klaar.ja"), true, null);
            b.knop("klaar_nee", Component.translatable("vadsrijk.grotdeur.klaar.nee"), true, null);
        }
        GrotNet.stuur(p, b.klaar());
    }

    // ---- dev commands ------------------------------------------------------------------------------------------------------------

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> g = Commands.literal("grot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        var ids = (com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>) (c, b) -> SharedSuggestionProvider.suggest(
                Kerkers.alle().stream().map(Kerker::id), b);
        // the window of a cave (as if you clicked its door), or to its door
        g.then(Commands.literal("deur").executes(c -> {
            StringBuilder sb = new StringBuilder("Grotdeuren (staanplek voor de deur / binnen):");
            for (Deur d : GrotDeur.deuren()) {
                sb.append("\n - ").append(d.grot()).append(d.hoofd() ? "" : " (2e deur)").append(": ").append(d.cellen().size()).append(" blokken, voor ")
                        .append(d.voor().toShortString()).append(", binnen ").append(d.binnen().toShortString());
            }
            String t = sb.toString();
            c.getSource().sendSuccess(() -> Component.literal(t), false);
            return 1;
        }).then(Commands.argument("id", StringArgumentType.word()).suggests(ids).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String id = StringArgumentType.getString(c, "id");
            if (Kerkers.van(id) == null) {
                c.getSource().sendFailure(Component.literal("Onbekende grot: " + id));
                return 0;
            }
            openDeur(p, id);
            return 1;
        }).then(Commands.literal("tp").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Deur d = GrotDeur.hoofd(StringArgumentType.getString(c, "id"));
            if (d == null) {
                c.getSource().sendFailure(Component.literal("Geen deur voor die grot"));
                return 0;
            }
            naar(p, d.voor(), d.yawVoor());
            ServerLevel sl = wereld(p);
            if (sl != null) {
                GrotDeur.plaats(sl, d);
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
            StringBuilder sb = new StringBuilder("Grotkerker-wachtrijen:");
            for (Kerker k : Kerkers.alle()) {
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
        // the cooldown: away (one cave or all), or set (seconds) to see it in the window
        g.then(Commands.literal("wachttijd").then(Commands.literal("weg").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Kerker k : Kerkers.alle()) {
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
            String al = grotVan(p.getUUID());
            if (al == null) {
                c.getSource().sendFailure(Component.literal("Je zit in geen grotkerker"));
                return 0;
            }
            verlaat(p, al);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(g));
    }

    /** Dev: n made-up members in this cave's queue (+ you, when you are not in it yet); they answer the ready check as asked. */
    static int nep(CommandSourceStack src, String grot, int n, String antwoord) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (Kerkers.van(grot) == null) {
            src.sendFailure(Component.literal("Onbekende grot: " + grot));
            return 0;
        }
        voegNepToe(grot, n, antwoord);
        if (!grot.equals(rijVan(p.getUUID()))) {
            inRij(p, grot);
        } else {
            matchmaak(src.getServer(), grot);
        }
        src.sendSuccess(() -> Component.literal(n + " nep-speler(s) in de wachtrij van " + grot + " (antwoord: " + antwoord + "). Rij: " + rijGrootte(grot) + "/" + GROEP_MAX
                + ". Vol = klaar-check; anders 'Nu starten' in het deurvenster (/guhs vadsrijk grot deur " + grot + ")."), false);
        return 1;
    }

    static List<UUID> voegNepToe(String grot, int n, String antwoord) {
        List<UUID> l = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            UUID id = UUID.nameUUIDFromBytes(("guhs-grot-nep-" + (++nepTeller)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            NEP.put(id, "Testguh " + nepTeller);
            if (antwoord.equals("ja")) {
                NEP_ANTWOORD.put(id, true);
            } else if (antwoord.equals("nee")) {
                NEP_ANTWOORD.put(id, false);
            }
            RIJ.computeIfAbsent(grot, g -> new LinkedHashSet<>()).add(id);
            l.add(id);
        }
        return l;
    }
}
