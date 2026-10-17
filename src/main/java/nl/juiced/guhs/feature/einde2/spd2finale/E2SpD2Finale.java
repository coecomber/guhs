package nl.juiced.guhs.feature.einde2.spd2finale;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.Gerrit;
import nl.juiced.guhs.feature.einde2.sponsguh.Hasselguh;
import nl.juiced.guhs.feature.einde2.sponsguh.Sponsguh;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.Richting;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 2.2, 7.8), slice sp_d2_finale: the story of part 2's last three steps.
 * <ul>
 *   <li>{@link SpVerhaal#K_HASSELGUH} (9): after the souvenir shop, walk with the crown (and Sponsguh and Patguh, {@link D2fStoet})
 *       to the head of Schelpenstad's pier; scene {@code hasselguh}; the ride on David Hasselguh's back ({@link D2fRit}); done
 *       on landing;</li>
 *   <li>{@link SpVerhaal#K_EMMERS} (10): scene {@code thuis}, then Plankmika's bucket helmets ({@link D2fEmmers}; sleutel
 *       "10_b" once all six are free); done on walking into the Bellenkom;</li>
 *   <li>{@link SpVerhaal#K_GUHFY} (11): climb onto the stage (Plankmika waits there); scenes {@code guhfy_guhber} (the song) and
 *       {@code bedankt}; the line is done; part 2's rewards ({@link #beloon}).</li>
 * </ul>
 * Everything is per player; what a player did lives in the line (step, teller); the entities are only there while he is
 * at that step and place ({@link D2fStoet}). Every half second (server ticks) {@link #regie} looks at what the step asks
 * where the player stands, so a step starts again by itself after a death, a logout or a teleport. Hints: {@link StapHint}
 * (what, where, a look scene, a column of light, a resume spot). Visibility of the residents this slice owns: {@link #bijMaken}.
 * Clicks: {@link D2fKlik}. Texts: tools/features/einde2_sp_d2_finale.py. Dev: {@link E2SpD2FinaleCommando}.
 */
public final class E2SpD2Finale {
    public static final String G = "gui.guhs.einde2_sp_d2_finale.";
    public static final String TITEL = "guhfy_guhber";
    /** The line's flag (CONTRACT_E2_C 2.4) that part 2's rewards were given. */
    static final String BELOOND = "sp_d2_finale_beloond";
    static final Verhaallijn KROON = SpVerhaal.KROON_LIJN;
    /** How near the pier head starts the scene / the ride. */
    static final double PIER = 6.0;
    /** How near the landing the scene thuis may play (else the game starts with a line: the client must have the chunks). */
    static final double THUIS_BEREIK = 64;

    private E2SpD2Finale() {
    }

    /** From E2SpD2FinaleFeature.register (common code, both sides). */
    static void register() {
        D2fScenes.registreer();
        for (int s = SpVerhaal.K_HASSELGUH; s <= SpVerhaal.K_GUHFY; s++) {
            SpStappen.echt(SpVerhaal.KROON, s);
        }
        SpStappen.sleutel(SpVerhaal.KROON, SpVerhaal.K_EMMERS, (p, s) -> D2fEmmers.alleVrij(p) ? "10_b" : "10");
        SpStappen.doel(SpVerhaal.KROON, SpVerhaal.K_EMMERS, E2SpD2Finale::doelEmmers);
        SpStappen.doel(SpVerhaal.KROON, SpVerhaal.K_GUHFY, (p, s) -> doelMarker(p, "d2f_podium_trap", "11"));
        SpStappen.ga(SpVerhaal.KROON, SpVerhaal.K_HASSELGUH, E2SpD2Finale::ga9);
        SpStappen.ga(SpVerhaal.KROON, SpVerhaal.K_EMMERS, E2SpD2Finale::ga10);
        SpStappen.ga(SpVerhaal.KROON, SpVerhaal.K_GUHFY, E2SpD2Finale::ga11);
        SpStappen.beloningen(SpVerhaal.KROON, E2SpD2Finale::beloningen);
        // the hints: what, where (direction + landmark), a look scene, a column of light, where "Verder" puts you
        StapHint.naar(SpVerhaal.SCHELPENSTAD, "hasselguh_opstap").tekst(G + "hint.pier").kijk(D2fScenes.KIJK_PIER).bijStart().baken()
                .zet(SpVerhaal.KROON, SpVerhaal.K_HASSELGUH);
        StapHint.naar(SpVerhaal.BIKINIBODEM, "d2f_emmer_1").tekst(G + "hint.emmers").kijk(D2fScenes.KIJK_EMMERS).bijStart()
                .terug(SpVerhaal.BIKINIBODEM, "hasselguh_landing").zet(SpVerhaal.KROON, SpVerhaal.K_EMMERS);
        StapHint.naar(SpVerhaal.BIKINIBODEM, "d2f_podium_trap").tekst(G + "hint.podium").kijk(D2fScenes.KIJK_PODIUM).bijStart().baken()
                .terug(SpVerhaal.BIKINIBODEM, "d2f_bellenkom_poort").zet(SpVerhaal.KROON, SpVerhaal.K_GUHFY);
        KROON.opStap((p, oud, nieuw) -> {
            if (nieuw < SpVerhaal.K_HASSELGUH || nieuw > SpVerhaal.K_GUHFY) {
                D2fStoet.ruimOp(p);
            } else if (nieuw == SpVerhaal.K_GUHFY) {
                D2fEmmers.allemaalVrij(p);                                   // (a dev jump to 11: the six count as free)
            }
        });
        Hasselguh.AANGEKOMEN.add(D2fRit::aangekomen);
        Bewoners.BIJ_MAKEN.add(E2SpD2Finale::bijMaken);
        SponsguhWezen.BIJ_KLIK.add(D2fKlik::klik);
        KledingBronnen.bron(GuhClothes.SPONSGUH_ANTENNES, "einde2_sp_d2_finale");
        Titels.registreer(new Titels.Titel(TITEL, "gui.guhs.titel." + TITEL, ChatFormatting.LIGHT_PURPLE, "guhs:knabbelburger", SpVerhaal::klaar));
        NeoForge.EVENT_BUS.addListener(E2SpD2Finale::opTick);
        NeoForge.EVENT_BUS.addListener(E2SpD2Finale::opUitloggen);
        NeoForge.EVENT_BUS.addListener(E2SpD2Finale::opRespawn);
        NeoForge.EVENT_BUS.addListener(E2SpD2Finale::opJoin);
        NeoForge.EVENT_BUS.addListener(E2SpD2FinaleCommando::register);
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    @Nullable
    static ServerLevel guheinde(ServerPlayer p) {
        return GuheindeFeature.isGuheinde(p.level()) ? p.level() : null;
    }

    /** Is the player in (or right around) this build? */
    static boolean in(ServerPlayer p, String structuur, int rand) {
        ServerLevel g = guheinde(p);
        if (g == null || !Plekken.bekend(g, structuur)) {
            return false;
        }
        BoundingBox doos = Plekken.doos(g, structuur);
        return doos != null && doos.inflatedBy(rand).isInside(p.blockPosition());
    }

    @Nullable
    static BlockPos blok(ServerPlayer p, String structuur, String plek) {
        ServerLevel g = guheinde(p);
        return g == null || !Plekken.bekend(g, structuur) ? null : Plekken.wereld(g, structuur, plek);
    }

    static boolean bij(ServerPlayer p, String structuur, String plek, double r) {
        BlockPos b = blok(p, structuur, plek);
        return b != null && Vec3.atBottomCenterOf(b).distanceToSqr(p.position()) <= r * r;
    }

    /** Inside a zone marker (a box; one block of slack up and down). */
    static boolean inZone(ServerPlayer p, String structuur, String zone) {
        ServerLevel g = guheinde(p);
        Plekken.Plek m = Plekken.plek(structuur, zone);
        if (g == null || m == null || !Plekken.bekend(g, structuur)) {
            return false;
        }
        BlockPos o = Plekken.oorsprong(g, structuur);
        BlockPos a = o.offset(m.pos()), b = o.offset(m.tot() == null ? m.pos() : m.tot());
        BlockPos q = p.blockPosition();
        return q.getX() >= Math.min(a.getX(), b.getX()) && q.getX() <= Math.max(a.getX(), b.getX())
                && q.getY() >= Math.min(a.getY(), b.getY()) - 1 && q.getY() <= Math.max(a.getY(), b.getY()) + 1
                && q.getZ() >= Math.min(a.getZ(), b.getZ()) && q.getZ() <= Math.max(a.getZ(), b.getZ());
    }

    /** Runs this for the player a few ticks later (server ticks; dropped when they left). */
    static void later(ServerPlayer p, int ticks, Consumer<ServerPlayer> wat) {
        StapHint.later(p, ticks, wat);
    }

    /** A big title (and a subtitle) for this player only. */
    static void titel(ServerPlayer p, Component titel, @Nullable Component onder, int blijf) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(4, blijf, 8));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(onder == null ? Component.empty() : onder));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(titel));
    }

    /** Plays a scene of this slice at a marker (Rotation.NONE: the two builds are never turned). False: it could not start. */
    static boolean speel(ServerPlayer p, Cutscene scene, String structuur, String anker, @Nullable Consumer<ServerPlayer> daarna) {
        BlockPos a = blok(p, structuur, anker);
        return a != null && Cutscenes.speel(p, scene, a, Rotation.NONE, daarna);
    }

    /** Is the player busy (a scene, a ride, dead, a spectator)? */
    static boolean bezig(ServerPlayer p) {
        return Cutscenes.bezig(p) || Hasselguh.draagt(p) || !p.isAlive() || p.isSpectator();
    }

    // =====================================================================================================================
    // the direction: every half second, what the step of this player asks where he stands
    // =====================================================================================================================

    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().isClientSide() || p.level().getServer() == null) {
            return;
        }
        long nu = p.level().getServer().getTickCount();
        if ((nu + p.getId()) % 10 == 0) {
            regie(p, nu);
        }
    }

    /** What the player's step asks for, where he stands now. Public for the game tests and the dev command. */
    public static void regie(ServerPlayer p, long nu) {
        ServerLevel g = guheinde(p);
        if (g == null || !SpVerhaal.receptKlaar(p)) {
            D2fStoet.ruimOp(p);
            return;
        }
        if (KROON.klaar(p)) {
            D2fStoet.ruimOp(p);
            if (!KROON.vlag(p, BELOOND)) {
                beloon(p);                                                   // (the line was finished another way: a dev jump, a lost scene)
            }
            return;
        }
        int stap = KROON.stap(p);
        if (stap < SpVerhaal.K_HASSELGUH) {
            return;
        }
        if (Hasselguh.draagt(p) || Cutscenes.bezig(p)) {
            return;
        }
        if (!p.isAlive() || p.isSpectator()) {
            D2fStoet.ruimOp(p);
            return;
        }
        switch (stap) {
            case SpVerhaal.K_HASSELGUH -> stap9(p);
            case SpVerhaal.K_EMMERS -> stap10(p, g, nu);
            case SpVerhaal.K_GUHFY -> stap11(p);
            default -> {
            }
        }
    }

    /** Step 9: with the crown to the pier; there the scene, then the ride. */
    private static void stap9(ServerPlayer p) {
        if (!in(p, SpVerhaal.SCHELPENSTAD, 24)) {
            D2fStoet.ruimOp(p);
            return;
        }
        D2fStoet.metJe(p, true);
        if (!bij(p, SpVerhaal.SCHELPENSTAD, "hasselguh_opstap", PIER)) {
            return;
        }
        if (!Cutscenes.gezien(p, D2fScenes.HASSELGUH.id())) {
            SpAudio.moment(p, "hasselguh");
            if (speel(p, D2fScenes.HASSELGUH, SpVerhaal.SCHELPENSTAD, D2fScenes.ANKER_PIER, q -> later(q, 4, E2SpD2Finale::opDeRug))) {
                return;
            }
            SpAudio.stopMoment(p, 0);
        }
        p.sendSystemMessage(Component.translatable(G + "rit.weer").withStyle(ChatFormatting.AQUA));
        opDeRug(p);
    }

    private static void opDeRug(ServerPlayer p) {
        if (KROON.stap(p) != SpVerhaal.K_HASSELGUH || Hasselguh.draagt(p)) {
            return;
        }
        if (D2fRit.vertrek(p)) {
            p.sendOverlayMessage(Component.translatable(G + "rit.op").withStyle(ChatFormatting.AQUA));
        } else {
            SpAudio.stopMoment(p, 20);
        }
    }

    /** After the landing (and on a return): the scene thuis, then the bucket game. */
    static void thuis(ServerPlayer p) {
        if (KROON.stap(p) != SpVerhaal.K_EMMERS || bezig(p)) {
            return;
        }
        D2fStoet.metJe(p, true);
        if (!speel(p, D2fScenes.THUIS, SpVerhaal.BIKINIBODEM, D2fScenes.ANKER_THUIS, D2fEmmers::start)) {
            D2fEmmers.start(p);
        }
    }

    /** Step 10: buckets off, then into the Bellenkom. */
    private static void stap10(ServerPlayer p, ServerLevel g, long nu) {
        if (!in(p, SpVerhaal.BIKINIBODEM, 16)) {
            D2fStoet.ruimOp(p);
            return;
        }
        if (!Cutscenes.gezien(p, D2fScenes.THUIS.id()) && bij(p, SpVerhaal.BIKINIBODEM, D2fScenes.ANKER_THUIS, THUIS_BEREIK)) {
            thuis(p);
            return;
        }
        D2fStoet.metJe(p, true);
        D2fEmmers.geefBlazer(p, true);
        D2fEmmers.tick(p, g, nu);
        if (D2fEmmers.alleVrij(p) && inZone(p, SpVerhaal.BIKINIBODEM, "bellenkom") && D2fEmmers.inBellenkom(p)) {
            D2fStoet.ruimEmmersOp(p);
        }
    }

    /** All six free: where the Bellenkom is (the step's text is "10_b" now). */
    static void naarBellenkomHint(ServerPlayer p) {
        BlockPos poort = blok(p, SpVerhaal.BIKINIBODEM, "d2f_bellenkom_poort");
        Component waar = poort == null ? Component.translatable(StapHint.P + "elders") : Richting.van(p.getEyePosition(), Vec3.atCenterOf(poort));
        p.sendSystemMessage(Component.literal("➜ ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(G + "hint.bellenkom", waar).withStyle(ChatFormatting.YELLOW)));
    }

    /** Step 11: Plankmika waits on the stage; climbing onto it starts the song. */
    private static void stap11(ServerPlayer p) {
        if (!in(p, SpVerhaal.BIKINIBODEM, 16)) {
            D2fStoet.ruimOp(p);
            return;
        }
        D2fStoet.metJe(p, true);
        D2fStoet.Sessie s = D2fStoet.van(p);
        if (!D2fStoet.levend(s.plankmika, p)) {
            BlockPos midden = blok(p, SpVerhaal.BIKINIBODEM, D2fScenes.ANKER_PODIUM);
            if (midden != null) {
                s.plankmika = D2fStoet.maak(p, nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature.PLANKMIKA.get(),
                        Vec3.atBottomCenterOf(midden).add(0, 0, -0.5), 0f);
                if (s.plankmika != null) {
                    s.plankmika.zetAnimatie("plan");
                }
            }
        }
        BlockPos midden = blok(p, SpVerhaal.BIKINIBODEM, D2fScenes.ANKER_PODIUM);
        if (midden != null && inZone(p, SpVerhaal.BIKINIBODEM, "podium") && p.getY() >= midden.getY() - 0.2) {
            finale(p);
        }
    }

    /** The song, then the thanks, then the line is done. */
    static void finale(ServerPlayer p) {
        if (KROON.stap(p) != SpVerhaal.K_GUHFY || bezig(p)) {
            return;
        }
        D2fStoet.ruimOp(p);                                                  // (the scene's actors take over: never two of anybody)
        SpAudio.moment(p, "guhfy_guhber");
        boolean ok = speel(p, D2fScenes.GUHFY_GUHBER, SpVerhaal.BIKINIBODEM, D2fScenes.ANKER_PODIUM, q -> {
            SpAudio.stopMoment(q, 40);
            if (!speel(q, D2fScenes.BEDANKT, SpVerhaal.BIKINIBODEM, D2fScenes.ANKER_PODIUM, E2SpD2Finale::klaar)) {
                klaar(q);
            }
        });
        if (!ok) {
            SpAudio.stopMoment(p, 0);
        }
    }

    /** The line is done (step 11 -> 12) and part 2's rewards are given. Public for the game tests and the dev command. */
    public static void klaar(ServerPlayer p) {
        KROON.verder(p, SpVerhaal.K_GUHFY);
        if (KROON.klaar(p)) {
            beloon(p);
        }
    }

    // =====================================================================================================================
    // the rewards (once per player): the buddy, the pet, the antennas, the title
    // =====================================================================================================================

    static void beloon(ServerPlayer p) {
        if (KROON.vlag(p, BELOOND)) {
            return;
        }
        KROON.vlag(p, BELOOND, true);
        KledingUnlocks.ontgrendel(p, GuhClothes.SPONSGUH_ANTENNES);
        boolean al = Sponsguh.heeft(p) && Gerrit.heeft(p);
        Sponsguh.geef(p);
        Gerrit.geef(p);
        p.sendSystemMessage(Component.translatable(G + (al ? "beloning.al" : "beloning")).withStyle(ChatFormatting.GOLD));
        Titels.kijk(p);
    }

    static List<VerhaalStand.Beloning> beloningen(ServerPlayer p) {
        boolean binnen = KROON.vlag(p, BELOOND);
        return List.of(
                Verhaallijn.beloning("guhs:sponsguh_spawn_egg", "entity.guhs.sponsguh", binnen),
                Verhaallijn.beloning("guhs:gerrit_spawn_egg", "entity.guhs.gerrit", binnen),
                Verhaallijn.beloning("guhs:sponsguh_antennes", binnen),
                Verhaallijn.beloning("guhs:knabbelburger", "gui.guhs.titel." + TITEL, binnen));
    }

    // =====================================================================================================================
    // compass targets and the dev starts
    // =====================================================================================================================

    @Nullable
    private static Doel doelMarker(ServerPlayer p, String marker, String sleutel) {
        BlockPos b = blok(p, SpVerhaal.BIKINIBODEM, marker);
        if (b == null) {
            return Doel.structuur(GuheindeFeature.GUHEINDE, SpVerhaal.BIKINIBODEM, Component.translatable("structure.guhs." + SpVerhaal.BIKINIBODEM));
        }
        return Doel.plek(GuheindeFeature.GUHEINDE, b, Component.translatable("gui.guhs.verhalen." + SpVerhaal.KROON + ".waar." + sleutel));
    }

    @Nullable
    private static Doel doelEmmers(ServerPlayer p, int stap) {
        ServerLevel g = guheinde(p);
        if (D2fEmmers.alleVrij(p) || g == null || !Plekken.bekend(g, SpVerhaal.BIKINIBODEM)) {
            return doelMarker(p, "d2f_bellenkom_poort", D2fEmmers.alleVrij(p) ? "10_b" : "10");
        }
        BlockPos b = D2fEmmers.dichtsteEmmer(p, g);
        return b == null ? doelMarker(p, "d2f_bellenkom_poort", "10_b")
                : Doel.plek(GuheindeFeature.GUHEINDE, b, Component.translatable("gui.guhs.verhalen." + SpVerhaal.KROON + ".waar.10"));
    }

    /** /guhs einde2 sponsguh ga sponsguh_kroon 9: at the pier head (the scene and the ride follow by themselves). */
    static void ga9(ServerPlayer p) {
        Cutscenes.vergeet(p, D2fScenes.HASSELGUH.id());
        D2fStoet.ruimOp(p);
    }

    /** ga 10: home at the landing, the scene thuis again, then the game. */
    static void ga10(ServerPlayer p) {
        D2fStoet.ruimOp(p);
        Cutscenes.vergeet(p, D2fScenes.THUIS.id());
        BlockPos b = blok(p, SpVerhaal.BIKINIBODEM, D2fScenes.ANKER_THUIS);
        if (b != null) {
            p.teleportTo(p.level(), b.getX() + 0.5, b.getY() + 0.05, b.getZ() + 0.5, java.util.Set.of(), 135f, 0f, true);
            p.resetFallDistance();
        }
    }

    /** ga 11: at the gate of the Bellenkom with the six free. */
    static void ga11(ServerPlayer p) {
        D2fStoet.ruimOp(p);
        D2fEmmers.allemaalVrij(p);
        BlockPos b = blok(p, SpVerhaal.BIKINIBODEM, "d2f_bellenkom_poort");
        if (b != null) {
            p.teleportTo(p.level(), b.getX() + 0.5, b.getY() + 0.05, b.getZ() + 0.5, java.util.Set.of(), 180f, 0f, true);
            p.resetFallDistance();
        }
    }

    // =====================================================================================================================
    // the residents this slice owns (CONTRACT_E2_C 4.3): who sees whom
    // =====================================================================================================================

    /**
     * {@code bikinibodem/sponsguh_werk}: Sponsguh at the grill until he leaves on the road trip ({@link SpVerhaal#K_RIT});
     * from then on he is with the player, and after the story he is the player's own buddy (and a manager). {@code
     * bikinibodem/gerrit}: at home until the story is done (then he lives with the player). {@code schelpenstad/hasselguh}:
     * David Hasselguh beside the pier only from the step after his ride on (the player's own David carries him; before that
     * nobody called him). The townsfolk ({@code bewoner_*}, {@code klant_*}, {@code ijsverkoper}, {@code parel}, {@code patguh})
     * stay for everybody: the bucket-helmet guhs of step 10 stand on markers of their own (Zicht has one step range per
     * entity: "hidden at 10 only" cannot be said; the Deviations line of this slice).
     */
    static void bijMaken(Entity e, String plek) {
        switch (plek) {
            case "bikinibodem/sponsguh_werk" -> Zicht.alleenBij(e, SpVerhaal.KROON, 0, SpVerhaal.K_KROON);
            case "bikinibodem/gerrit" -> Zicht.alleenBij(e, SpVerhaal.KROON, 0, SpVerhaal.K_GUHFY);
            case "schelpenstad/hasselguh" -> Zicht.alleenBij(e, SpVerhaal.KROON, SpVerhaal.K_EMMERS, 99);
            default -> {
            }
        }
    }

    /** The same rule for residents that stood before (saved in an older run), and the stale entities of the finale. */
    private static void opJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        D2fStoet.opJoin(event);
        Entity e = event.getEntity();
        if (!event.isCanceled() && e instanceof SponsguhWezen && Bewoners.isBewoner(e)) {
            bijMaken(e, e.getPersistentData().getStringOr(Bewoners.PLEK, ""));
        }
    }

    private static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        D2fStoet.ruimOp(event.getEntity().getUUID());
        D2fKlik.vergeet(event.getEntity().getUUID());
    }

    private static void opRespawn(PlayerEvent.PlayerRespawnEvent event) {
        D2fStoet.ruimOp(event.getEntity().getUUID());
    }
}
