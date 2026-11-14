package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;

/**
 * 1.10.1: escorts with a personal copy. Before, the ONE quest NPC guh followed one player (and stayed away for minutes after the
 * escort, or forever when that player logged out), so a second player could not do the quest. Now:
 * <ul>
 * <li>the NPC itself never leaves its post. Starting the escort spawns a <b>copy</b> of it ({@link RijkGuh#kopie}) next to it,
 * which follows the player who started it (the leader). The copy is never saved and never counted as the NPC
 * ({@link Npcs#levend} only knows the real one);</li>
 * <li>party members on the same quest step share the copy: talking to the NPC hands them the party's copy, and on arrival
 * everyone of the party on that step near the destination gets the escort;</li>
 * <li>the copy vanishes at once (and the step can be started again at the NPC) when the leader logs out, dies, goes to another
 * dimension, is more than {@link #KWIJT} blocks away, abandons the quest, or the step got done another way;</li>
 * <li>after the arrival the copy rests {@link #RUST} ticks at the destination (to hand in there) and then vanishes.</li>
 * </ul>
 */
public final class Escortes {
    /** After the arrival: the copy stays this long at the destination (hand-in), then vanishes. */
    public static final int RUST = 20 * 45;
    /** Copy and leader further apart than this (the copy teleports along beyond 24): the escort fails. */
    public static final double KWIJT = 64;
    /** Party members within this of the destination get the arrival too. */
    public static final double PARTY_STRAAL = 48;

    /** One running escort. */
    public static final class Escorte {
        public final String quest;
        public final int doel;
        public final String npc;
        public final UUID leider;
        public final UUID kopie;
        /** Where the copy must go. */
        public BlockPos naar;
        /** Game time the resting copy vanishes (-1: still walking). */
        long weg = -1;
        /** The leader as an object: a FakePlayer (quest robot) is not in the player list. */
        final ServerPlayer leiderObj;

        Escorte(String quest, int doel, String npc, ServerPlayer leider, UUID kopie, BlockPos naar) {
            this.quest = quest;
            this.doel = doel;
            this.npc = npc;
            this.leider = leider.getUUID();
            this.leiderObj = leider;
            this.kopie = kopie;
            this.naar = naar;
        }

        public boolean aangekomen() {
            return weg >= 0;
        }
    }

    private static final List<Escorte> LIJST = new CopyOnWriteArrayList<>();

    private Escortes() {
    }

    static void init() {
        // U-DUBBEL's heuristic scan would see two guhs with the same NPC id: the copies have their own life, leave them alone
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.NIET_HEURISTISCH.add(e -> e instanceof RijkGuh g && g.kopie);
    }

    @Nullable
    static BlockPos bestemming(Quest.Doel d) {
        String poi = d.bron().startsWith("poi:") ? d.bron().substring(4) : d.bron();
        BlockPos b = NpcPlekken.poi(poi);
        if (b == null) {
            b = QuestPlek.pos(null, "poi:" + poi);
        }
        return b;
    }

    /**
     * The player starts (or rejoins) the escort of objective {@code i} of quest {@code q} at {@code bij} (the NPC, or a copy).
     * Returns the copy that follows now (the party's copy when a party member on the same step already has one), or null.
     */
    @Nullable
    public static RijkGuh start(ServerPlayer p, RijkGuh bij, Quest q, int i) {
        Quest.Doel d = q.doelen().get(i);
        Escorte e = van(p, q.id(), i);
        ServerLevel sl = (ServerLevel) p.level();
        if (e != null && kopie(sl, e) instanceof RijkGuh g) {
            if (!e.aangekomen()) {
                if (g.distanceTo(p) > 8) {
                    g.snapTo(p.getX() + 1, p.getY(), p.getZ() + 1);   // (a stuck copy comes to you)
                    g.getNavigation().stop();
                }
                return g;
            }
            stop(sl, e, false);   // (an arrived copy of a step you still have open: start a fresh one)
        }
        NpcDef def = Npcs.van(d.doel());
        BlockPos naar = bestemming(d);
        if (def == null || naar == null) {
            return null;
        }
        RijkGuh g = NpcRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
        if (g == null) {
            return null;
        }
        g.stelIn(def);
        g.kopie = true;
        g.thuis = null;
        g.snapTo(bij.getX(), bij.getY(), bij.getZ(), bij.getYRot(), 0);
        g.volg(p);
        sl.addFreshEntity(g);
        LIJST.add(new Escorte(q.id(), i, d.doel(), p, g.getUUID(), naar));
        return g;
    }

    /** The escort of this step the player leads or shares (party member on the same step), or null. */
    @Nullable
    public static Escorte van(ServerPlayer p, String quest, int doel) {
        Escorte eigen = null, party = null;
        for (Escorte e : LIJST) {
            if (!e.quest.equals(quest) || e.doel != doel) {
                continue;
            }
            if (e.leider.equals(p.getUUID())) {
                eigen = e;
            } else if (party == null && zelfdeParty(p, e)) {
                party = e;
            }
        }
        return eigen != null ? eigen : party;
    }

    /** The walking copy of NPC {@code npc} that belongs to this player (own or the party's, same open step), or null. */
    @Nullable
    public static RijkGuh kopieVan(ServerPlayer p, String npc) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return null;
        }
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (d.type() == Quest.Doel.Type.ESCORTE && d.doel().equals(npc) && !Questlog.doelKlaar(p, q, i)) {
                    Escorte e = van(p, q.id(), i);
                    if (e != null && !e.aangekomen() && kopie(sl, e) instanceof RijkGuh g) {
                        return g;
                    }
                }
            }
        }
        return null;
    }

    /** The escort a copy belongs to, or null (not a copy / already gone). */
    @Nullable
    public static Escorte vanKopie(RijkGuh g) {
        for (Escorte e : LIJST) {
            if (e.kopie.equals(g.getUUID())) {
                return e;
            }
        }
        return null;
    }

    /** May this player use this escort (the leader, or a party member with the same step open)? */
    public static boolean hoort(ServerPlayer p, Escorte e) {
        if (e.leider.equals(p.getUUID())) {
            return true;
        }
        Quest q = Quests.van(e.quest);
        return q != null && zelfdeParty(p, e) && Questlog.actief(p, q.id()) && !Questlog.doelKlaar(p, q, e.doel);
    }

    private static boolean zelfdeParty(ServerPlayer p, Escorte e) {
        Partijen.Party a = Partijen.party(p), b = Partijen.partyVan(e.leider);
        return a != null && a == b;
    }

    public static List<Escorte> alle() {
        return List.copyOf(LIJST);
    }

    @Nullable
    static RijkGuh kopie(ServerLevel sl, Escorte e) {
        return sl.getEntity(e.kopie) instanceof RijkGuh g && g.isAlive() ? g : null;
    }

    // ---- the ticker ---------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 4 || LIJST.isEmpty()) {
            return;
        }
        kijk(server);
    }

    /** One check of every escort (the ticker twice a second; tests and the quest robot call it directly). */
    public static void kijk(MinecraftServer server) {
        for (Escorte e : LIJST) {
            ServerPlayer l = leider(server, e);
            RijkGuh g = null;
            ServerLevel sl = null;
            for (ServerLevel lv : server.getAllLevels()) {
                if ((g = kopie(lv, e)) != null) {
                    sl = lv;
                    break;
                }
            }
            if (g == null) {
                LIJST.remove(e);
                if (!e.aangekomen() && l != null) {
                    terugMelding(l, e);
                }
                continue;
            }
            if (e.aangekomen()) {
                if (sl.getGameTime() >= e.weg) {
                    stop(sl, e, false);
                }
                continue;
            }
            Quest q = Quests.van(e.quest);
            if (l == null || l.isDeadOrDying() || l.level() != sl || q == null || !Questlog.actief(l, q.id()) || Questlog.doelKlaar(l, q, e.doel)
                    || l.distanceTo(g) > KWIJT) {
                stop(sl, e, l != null);
                continue;
            }
            if (NpcHaken.bij(g, e.naar, NpcPlekken.POI_STRAAL + 2, NpcPlekken.POI_HOOGTE)) {
                aankomst(sl, e, g, q, l);
            }
        }
    }

    /** The leader when still here (logged in with the same player object, so a relog or a respawn ends the escort). */
    @Nullable
    private static ServerPlayer leider(MinecraftServer server, Escorte e) {
        ServerPlayer l = e.leiderObj;
        if (l.hasDisconnected() || l.isRemoved()) {
            return null;
        }
        ServerPlayer lijst = server.getPlayerList().getPlayer(e.leider);
        return lijst == null || lijst == l ? l : null;
    }

    private static void aankomst(ServerLevel sl, Escorte e, RijkGuh g, Quest q, ServerPlayer leider) {
        Quest.Doel d = q.doelen().get(e.doel);
        List<ServerPlayer> wie = new ArrayList<>();
        wie.add(leider);
        for (ServerPlayer m : Partijen.leden(leider)) {
            if (m != leider && m.level() == sl && !m.isDeadOrDying() && Questlog.actief(m, q.id()) && !Questlog.doelKlaar(m, q, e.doel)
                    && NpcHaken.bij(m, e.naar, PARTY_STRAAL, PARTY_STRAAL)) {
                wie.add(m);
            }
        }
        g.volg(null);
        g.thuis = e.naar;
        g.rust(e.naar, RUST);
        e.weg = sl.getGameTime() + RUST;
        g.emotes.start(Emote.VAHOEG, false, GuhEmotes.Source.SELF);
        for (ServerPlayer m : wie) {
            Questlog.tel(m, Quest.Doel.Type.ESCORTE, x -> x == d, 1);
        }
        for (ServerPlayer m : wie) {
            if (q.inleveren().equals(d.doel()) && Questlog.allesKlaar(m, q)) {
                QuestMotor.klik(m, g);
            }
        }
    }

    /** Ends an escort: the copy vanishes; {@code melden}: tell the leader the step can be started again at the NPC. */
    static void stop(ServerLevel sl, Escorte e, boolean melden) {
        LIJST.remove(e);
        RijkGuh g = kopie(sl, e);
        if (g != null) {
            g.discard();
        }
        if (melden) {
            ServerPlayer l = sl.getServer().getPlayerList().getPlayer(e.leider);
            if (l != null) {
                terugMelding(l, e);
            }
        }
    }

    private static void terugMelding(ServerPlayer l, Escorte e) {
        NpcDef d = Npcs.van(e.npc);
        Component naam = d == null ? Component.literal("?") : Component.translatable(d.naamKey());
        l.sendSystemMessage(Component.translatable("vadsrijk.npc.escorte.terug", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** The leader left the game: end his escorts at once (the ticker would also see it within half a second). */
    static void uitgelogd(MinecraftServer server, UUID id) {
        for (Escorte e : LIJST) {
            if (e.leider.equals(id)) {
                for (ServerLevel lv : server.getAllLevels()) {
                    if (kopie(lv, e) != null) {
                        stop(lv, e, false);
                    }
                }
                LIJST.remove(e);
            }
        }
    }

    /** Tests / dev: every escort away. */
    public static void wisAlles(MinecraftServer server) {
        for (Escorte e : LIJST) {
            for (ServerLevel lv : server.getAllLevels()) {
                RijkGuh g = kopie(lv, e);
                if (g != null) {
                    g.discard();
                }
            }
        }
        LIJST.clear();
    }
}
