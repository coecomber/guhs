package nl.juiced.guhs.feature.einde2.reizen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B (CONTRACT_E2_B 3, 2.4, 7.10), owner: slice reizen. The story flights on Appa and the guest rule of co-op.
 * <p>
 * <b>A flight</b> ({@link #vlieg}) is a REAL flight on the player's own Appa ({@link Appa#vlieg}: he points the way, the
 * player may steer, and when they let go he flies himself), about two minutes between two chapters. Whoever travels with
 * the player as a guest gets a seat in the saddle basket; the team of that moment sits on the five team seats (seen by
 * everybody: it is the player's own Appa) and talks in the chat ({@link Reis}); between the first and second and the second
 * and third chapter Zuka's ship turns up and fires shoving fireballs ({@link ZukaSchipEntity}): a short dodge part where
 * Appa flies himself and the player swerves with the movement keys. On landing {@code daarna} runs once, also when the
 * player steered there. A flight that was broken off (the player landed elsewhere and got off) waits: the whistle list
 * shows its goal with a star ({@link Fluitlijst}) and picking it flies on (with the same {@code daarna} while the server
 * runs; the goal itself is kept in the player's data, {@value #REIS_KEY}).
 * <p>
 * <b>The guest rule</b>: a player is the guest of a host while riding in the host's Appa's basket, and after getting off
 * until they are more than {@value #GAST_AFSTAND} blocks from the host, or the host leaves the story structure (its veil
 * box) they landed in together, or the host leaves the game or the dimension. {@link E2Verhaal#open} lets a guest in where
 * the host may go; {@link E2Team} shows a guest the host's team.
 */
public final class E2Reizen {
    /** The kind of flight: the start (from the kaasbol), the three between the chapters, a free one (the whistle). */
    public enum Vlucht { START, H1_H2, H2_H3, H3_H4, VRIJ }

    /** A guest stops being one this far (blocks) from the host. */
    public static final int GAST_AFSTAND = 64;
    /** Guests within this many blocks of the host get on with them when a flight starts. */
    public static final int MEE_AFSTAND = 24;
    /** The player data of a story flight that has not landed yet: {Structuur, Soort}. */
    public static final String REIS_KEY = "guhs_einde2_reizen_reis";

    /** A guest: whose, and (after getting off) the story structure they landed in together (null: none, or still riding). */
    record Gast(UUID host, @Nullable String structuur, boolean rijdt) {
    }

    private static final Map<UUID, Gast> GASTEN = new ConcurrentHashMap<>();
    /** The flight each player is on (or that waits to be flown on). */
    static final Map<UUID, Reis> REIZEN = new ConcurrentHashMap<>();

    private E2Reizen() {
    }

    static void register() {
        Appa.AANGEKOMEN.add(E2Reizen::geland);
        Appa.OP_ZIJN_KOP = Fluitlijst::opZijnKop;
        Appa.KLIK = Fluitlijst::klik;
        Appa.FLUIT = AppaPlatform::fluit;
    }

    // =====================================================================================================================
    // the flights
    // =====================================================================================================================

    /**
     * Seats p and their guests on p's Appa and flies to {@code Plekken.aankomst(structuur)} (team chat, Zuka's chase on
     * H1_H2 / H2_H3); {@code daarna} once on landing, also when the player steered there. False = could not start, nothing
     * happened (no Appa or he is kept away, not in the Guheinde, the structure's spot is not known yet).
     */
    public static boolean vlieg(ServerPlayer p, String structuur, Vlucht soort, @Nullable Consumer<ServerPlayer> daarna) {
        ServerLevel guheinde = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (guheinde == null || p.level() != guheinde || !Plekken.bekend(guheinde, structuur)) {
            return false;
        }
        Appa.Landing landing = Landingen.van(guheinde, structuur, soort != Vlucht.VRIJ);
        if (landing == null || !Appa.heeft(p) || Appa.isWeg(p)) {
            return false;
        }
        Reis oud = REIZEN.remove(p.getUUID());
        if (oud != null) {
            oud.ruimOp(p.level().getServer());
        }
        if (!Appa.vlieg(p, landing, true)) {
            return false;
        }
        AppaGuhEntity appa = Appa.geladen(p);
        if (appa == null) {
            return false;
        }
        E2Team.weg(p, null);                                       // (the campfire team climbs on Appa instead)
        for (ServerPlayer g : gasten(p)) {
            if (g.getVehicle() != appa && g.level() == p.level() && g.distanceTo(p) <= MEE_AFSTAND && appa.gasten().size() < AppaGuhEntity.GASTEN) {
                g.stopRiding();
                g.startRiding(appa, true, false);
            }
        }
        Reis r = new Reis(p, structuur, soort, landing, daarna);
        REIZEN.put(p.getUUID(), r);
        if (soort != Vlucht.VRIJ) {
            CompoundTag t = new CompoundTag();
            t.putString("Structuur", structuur);
            t.putString("Soort", soort.name());
            GuhQuests.saved(p).put(REIS_KEY, t);
        }
        r.begin(appa);
        return true;
    }

    /** Is this player on a flight of this slice right now (or one that waits to be flown on)? */
    @Nullable
    public static Vlucht reis(ServerPlayer p) {
        Reis r = REIZEN.get(p.getUUID());
        return r == null ? null : r.soort;
    }

    /**
     * einde2 merge_fix1 (playtest 1 items 4 + 12): is p still on Appa's back on a flight of this slice that has not landed?
     * The chapters wait with their arrival card and scene while this is true (they start them on entering the structure's
     * box, which a flight crosses up to ~170 blocks before the landing place): Appa lands on his spot first and sets his
     * rider down on {@code aankomst_speler}, where the scene stands them, and the scene plays after that. Someone who got
     * off on the way (a waiting flight, on foot) is not held up.
     */
    public static boolean landtNog(ServerPlayer p) {
        return REIZEN.containsKey(p.getUUID()) && p.getVehicle() instanceof AppaGuhEntity;
    }

    /** The story flight that has not landed yet (also one of before a restart): {structuur, soort}, or null. */
    @Nullable
    static String[] wachtend(ServerPlayer p) {
        Reis r = REIZEN.get(p.getUUID());
        if (r != null && r.soort != Vlucht.VRIJ) {
            return new String[]{r.structuur, r.soort.name()};
        }
        CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty(REIS_KEY);
        String s = t.getStringOr("Structuur", "");
        return s.isEmpty() ? null : new String[]{s, t.getStringOr("Soort", Vlucht.VRIJ.name())};
    }

    /** Flies on with the story flight that waits (from the whistle list). False: none, or it could not start. */
    static boolean hervat(ServerPlayer p) {
        String[] w = wachtend(p);
        if (w == null) {
            return false;
        }
        Reis r = REIZEN.get(p.getUUID());
        Vlucht soort;
        try {
            soort = Vlucht.valueOf(w[1]);
        } catch (IllegalArgumentException ex) {
            soort = Vlucht.VRIJ;
        }
        return vlieg(p, w[0], soort, r == null ? null : r.daarna);
    }

    /**
     * Appa landed after an auto flight (Appa.AANGEKOMEN). einde2 fix-reizen: it is Appa who must be there, not the player: a
     * cutscene of the place may hold the player up in the air while he lands on his spot (they are set down after it).
     */
    private static void geland(ServerPlayer p) {
        Reis r = REIZEN.get(p.getUUID());
        AppaGuhEntity appa = Appa.geladen(p);
        if (r != null && (r.dichtbij(p, 16) || appa != null && Reis.horizontaal(appa.position(), r.doel) <= 16)) {
            r.geland = true;
        }
    }

    /**
     * einde2 fix-reizen: the player reached this place another way (Appa took them through the void from another dimension,
     * {@link AppaReis}): a story flight that waited for it is done now ({@code daarna} runs, once); one of before a restart
     * is forgotten.
     */
    static void aangekomenZonderVlucht(ServerPlayer p, String structuur) {
        Reis r = REIZEN.get(p.getUUID());
        if (r != null && r.structuur.equals(structuur)) {
            klaar(p, r);
            return;
        }
        CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty(REIS_KEY);
        if (structuur.equals(t.getStringOr("Structuur", ""))) {
            GuhQuests.saved(p).remove(REIS_KEY);
        }
    }

    /**
     * einde2 fix-reizen: the place the story wants this player at now (the main building of the chapter they are in), or
     * null (the story has not begun, it is done, or the chapter's place is not unlocked). A story flight that has not
     * landed yet always counts first. merge_skelet (CONTRACT_E2_C 4.4): once the Guhvatar story is done (the finale), the
     * place where the Sponsguh story goes on ({@link nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal#reisDoel}: Bikinibodem,
     * or Schelpenstad for the steps that play there), so Appa's gold row also flies to that story's landing spot.
     */
    @Nullable
    public static String verhaalDoel(ServerPlayer p) {
        String[] w = wachtend(p);
        if (w != null) {
            return w[0];
        }
        for (String lijn : E2Verhaal.HOOFDSTUKKEN) {
            if (E2Verhaal.klaar(p, lijn)) {
                continue;
            }
            String s = E2Verhaal.structuurVan(lijn);
            return E2Verhaal.ontgrendeld(p, s) ? s : null;
        }
        return nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal.reisDoel(p);
    }

    /** Lands it now: the team climbs off, the music fades, daarna runs (once), guests keep being guests in that structure. */
    static void klaar(ServerPlayer p, Reis r) {
        REIZEN.remove(p.getUUID(), r);
        r.ruimOp(p.level().getServer());
        if (r.soort != Vlucht.VRIJ) {
            GuhQuests.saved(p).remove(REIS_KEY);
        }
        String in = structuurBij(p);
        for (ServerPlayer g : r.kijkers(p)) {
            E2Audio.stopMoment(g, 40);
            if (g != p) {
                GASTEN.put(g.getUUID(), new Gast(p.getUUID(), in, g.getVehicle() instanceof AppaGuhEntity));
                nl.juiced.guhs.quest.GuhAdvancements.grant(g, "einde2_reizen_gast");
            }
        }
        if (r.daarna != null) {
            Consumer<ServerPlayer> d = r.daarna;
            r.daarna = null;
            d.accept(p);
        }
    }

    /** Every server tick: the flights and Appa's trips through the void; every 10 ticks: the guests. */
    static void tick(MinecraftServer server) {
        AppaReis.tick(server);
        for (Map.Entry<UUID, Reis> e : REIZEN.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Reis r = e.getValue();
            if (p == null) {
                REIZEN.remove(e.getKey(), r);
                r.ruimOp(server);
                continue;
            }
            r.tick(p);
        }
        if (server.getTickCount() % 10 == 3) {
            gastenTick(server);
            Fluitlijst.vertrekTick(server);
        }
        if (server.getTickCount() % 40 == 17) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                AppaPlatform.geschenkTick(p);                   // (fix4-appa: the platform at the story's end, once)
            }
        }
    }

    /** A player left the game: their flight waits in their data, their guests are nobody's any more. */
    static void weg(ServerPlayer p) {
        AppaReis.vergeet(p.getUUID());
        Fluitlijst.VERTREK.remove(p.getUUID());
        Reis r = REIZEN.remove(p.getUUID());
        if (r != null) {
            r.ruimOp(p.level().getServer());
        }
        GASTEN.remove(p.getUUID());
        GASTEN.values().removeIf(g -> g.host().equals(p.getUUID()));
    }

    static void leeg() {
        REIZEN.clear();
        GASTEN.clear();
        Fluitlijst.VERTREK.clear();
        AppaReis.leeg();
    }

    // =====================================================================================================================
    // guests
    // =====================================================================================================================

    /** The host this player travels with as a guest (null: none). */
    @Nullable
    public static ServerPlayer gastheer(ServerPlayer p) {
        if (p.getVehicle() instanceof AppaGuhEntity appa && !appa.isEigenaar(p) && appa.baas() instanceof ServerPlayer host && host != p) {
            return host;                                          // (riding in someone's basket: always their guest)
        }
        Gast g = GASTEN.get(p.getUUID());
        if (g == null || p.level().getServer() == null) {
            return null;
        }
        ServerPlayer host = p.level().getServer().getPlayerList().getPlayer(g.host());
        if (host == null || host == p || host.level() != p.level() || host.distanceTo(p) > GAST_AFSTAND) {
            return null;
        }
        return host;
    }

    /** The guests of this host (riding in their basket, or still near after landing). */
    public static List<ServerPlayer> gasten(ServerPlayer host) {
        List<ServerPlayer> uit = new ArrayList<>();
        if (host.level().getServer() == null) {
            return uit;
        }
        for (ServerPlayer p : host.level().getServer().getPlayerList().getPlayers()) {
            if (p != host && gastheer(p) == host) {
                uit.add(p);
            }
        }
        return uit;
    }

    static void gastenTick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getVehicle() instanceof AppaGuhEntity appa && !appa.isEigenaar(p) && appa.baas() instanceof ServerPlayer host && host != p) {
                GASTEN.put(p.getUUID(), new Gast(host.getUUID(), null, true));
            }
        }
        for (Map.Entry<UUID, Gast> e : GASTEN.entrySet()) {
            ServerPlayer g = server.getPlayerList().getPlayer(e.getKey());
            Gast gast = e.getValue();
            ServerPlayer host = server.getPlayerList().getPlayer(gast.host());
            if (g == null || host == null || host.level() != g.level()) {
                GASTEN.remove(e.getKey(), gast);
                continue;
            }
            boolean rijdt = g.getVehicle() instanceof AppaGuhEntity appa && appa.isEigenaar(host);
            if (rijdt) {
                continue;
            }
            if (gast.rijdt()) {                                   // just got off: where did they land together?
                Gast nu = new Gast(gast.host(), structuurBij(host), false);
                GASTEN.replace(e.getKey(), gast, nu);
                gast = nu;
            }
            if (g.distanceTo(host) > GAST_AFSTAND) {
                GASTEN.remove(e.getKey(), gast);
                continue;
            }
            if (gast.structuur() != null) {
                BoundingBox doos = doos(host.level(), gast.structuur());
                if (doos == null || !doos.isInside(host.blockPosition())) {
                    GASTEN.remove(e.getKey(), gast);
                }
            }
        }
    }

    /** (tests, dev) makes p the guest of host as if they had just flown along and got off here. */
    static void maakGast(ServerPlayer p, ServerPlayer host, @Nullable String structuur) {
        GASTEN.put(p.getUUID(), new Gast(host.getUUID(), structuur, false));
    }

    // =====================================================================================================================
    // where is who
    // =====================================================================================================================

    /** The box of a story structure that counts as "in it" (the veil's box), or null. */
    @Nullable
    static BoundingBox doos(ServerLevel level, String structuur) {
        if (!GuheindeFeature.isGuheinde(level) || !Plekken.bekend(level, structuur)) {
            return null;
        }
        return E2Verhaal.sluierDoos(level, structuur);
    }

    /** The story structure this player stands in (its veil box), or null. */
    @Nullable
    static String structuurBij(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level) || !GuheindeFeature.isGuheinde(level)) {
            return null;
        }
        for (String s : Einde2Ids.VERHAAL_STRUCTUREN) {
            BoundingBox d = doos(level, s);
            if (d != null && d.isInside(p.blockPosition())) {
                return s;
            }
        }
        return null;
    }

    // =====================================================================================================================
    // the chat on Appa's back
    // =====================================================================================================================

    /** "<Aang-guh> text" in the chat (spreker "" = the narrator: grey and in italics). */
    static void zeg(ServerPlayer p, String spreker, String key) {
        if (spreker.isEmpty()) {
            p.sendSystemMessage(Component.literal("* ").append(Component.translatable(key)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        p.sendSystemMessage(Component.literal("<").append(naam(spreker)).append("> ").withStyle(ChatFormatting.AQUA)
                .append(Component.translatable(key).withStyle(ChatFormatting.WHITE)));
    }

    /** The name of a speaker of the chat lines. */
    static Component naam(String spreker) {
        E2Team.Lid lid = E2Team.Lid.van(spreker);
        if (lid != null) {
            return Component.translatable("entity.guhs.guh_npc." + lid.basis().id());
        }
        return Component.translatable("entity.guhs.guh_npc." + spreker);
    }

    /** A line above the hotbar for this player. */
    static void boven(ServerPlayer p, Component tekst) {
        p.sendOverlayMessage(tekst);
    }

    /** (dev) ends the flight of this player now, as if Appa had landed. */
    static boolean landNu(ServerPlayer p) {
        Reis r = REIZEN.get(p.getUUID());
        if (r == null) {
            return false;
        }
        klaar(p, r);
        return true;
    }

    /** The entity ids of the temporary actors (team on Appa, Zuka on his ship) that must not outlive their flight. */
    static boolean isTijdelijk(Entity e) {
        return e.entityTags().contains(Reis.TIJDELIJK);
    }
}
