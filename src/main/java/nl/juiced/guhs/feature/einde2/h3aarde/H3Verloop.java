package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.reizen.E2Reizen;
import nl.juiced.guhs.feature.einde2.reizen.E2Team;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * The chapter as it happens: every 10 ticks, for every player in or near Ba Sing Guh, what their own step asks (a zone you
 * walk into, a scene that plays, a lesson that runs). Talking is {@link H3Rollen}; the minigames have classes of their own.
 * The moments that need you in a spot (a scene, the narrator card) wait until you stand on your own feet: not on Appa, not
 * in the monorail car, not in another scene or a talking screen.
 */
public final class H3Verloop {
    /** How near the city (blocks, round its box) a player counts as "here". */
    public static final double BEREIK = 64;

    /** (tests) may this player be in the city at all (the veil): the structure unlocked for them, or for their host. */
    static Predicate<ServerPlayer> open = p -> E2Verhaal.open(p, E2H3Aarde.STRUCTUUR);

    /** The players who are inside the city's box now (for the team: put there on entering, gone on leaving). */
    private static final Set<UUID> BINNEN = ConcurrentHashMap.newKeySet();
    /** A player whose card/scene of a step is on its way (the step itself only moves when it is over): since which server tick. */
    private static final Map<UUID, Integer> BEZIG = new ConcurrentHashMap<>();

    private H3Verloop() {
    }

    static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || (p.tickCount + 3) % 10 != 0 || p.level().dimension() != GuheindeFeature.GUHEINDE) {
            return;
        }
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(p.level());
        if (stad == null) {
            return;
        }
        if (!stad.doos(BEREIK).contains(p.position())) {
            if (BINNEN.remove(p.getUUID())) {
                verlaat(p);
            }
            return;
        }
        tik(p, stad);
    }

    /** Leaves the city: the team stays here, the player's actors of the chapter go. */
    private static void verlaat(ServerPlayer p) {
        E2Team.weg(p, E2H3Aarde.STRUCTUUR);
        H3Acteurs.wegAlle(p, "");
        H3Blinddoek.stop(p);
        H3Monorail.uit(p);
    }

    /** Is this player free to start a moment of the story: on their own feet, not busy with anything else? */
    static boolean vrij(ServerPlayer p) {
        Integer sinds = BEZIG.get(p.getUUID());
        if (sinds != null && !Cutscenes.bezig(p) && p.level().getServer() != null && p.level().getServer().getTickCount() - sinds > 100) {
            BEZIG.remove(p.getUUID());   // (a scene that was broken off: logged out, died, another dimension; the step did not move)
        }
        return p.isAlive() && !p.isSpectator() && p.getVehicle() == null && !p.isSleeping() && !Cutscenes.bezig(p) && !Praat.bezig(p)
                && !BEZIG.containsKey(p.getUUID()) && !p.getAbilities().flying && Duwtje.mag(p);
    }

    /** What this player's step asks right now (also called by the game tests, with a city of their own). */
    static void tik(ServerPlayer p, E2H3Aarde.Stad stad) {
        if (!open.test(p)) {
            return;
        }
        boolean inDoos = stad.doos(2).contains(p.position());
        if (inDoos && BINNEN.add(p.getUUID())) {
            E2Team.zetInStructuur(p, E2H3Aarde.STRUCTUUR);
        } else if (!inDoos && BINNEN.remove(p.getUUID())) {
            E2Team.weg(p, E2H3Aarde.STRUCTUUR);
        }
        H3Schijn.traliesWeg(stad);   // (fix3-h3: the gate of Appa's chamber is open in the world; the bars are shown per player)
        int stap = E2H3Aarde.stap(p);
        H3Kool.tik(p, stad, stap);
        H3Knipogen.tik(p, stad, stap);
        H3Personen.tik(p, stad, stap);
        if (stap == E2H3Aarde.BLINDDOEK) {
            H3Blinddoek.tik(p, stad);
        } else {
            H3Blinddoek.stop(p);
        }
        if (stap == E2H3Aarde.MONORAIL || stap >= E2H3Aarde.STAPPEN) {
            H3Monorail.tik(p, stad, stap);
        }
        if (stap == E2H3Aarde.LAOGAI) {
            H3Sluipen.tik(p, stad);
        } else {
            H3Sluipen.stop(p);
        }
        if (!vrij(p)) {
            return;
        }
        switch (stap) {
            case E2H3Aarde.AANKOMST -> {
                if (inDoos && p.onGround() && !E2Reizen.landtNog(p)) {   // (merge_fix1: after Appa landed)
                    aankomst(p, stad);
                }
            }
            case E2H3Aarde.AARDROMMEL -> {
                if (stad.in(p, "aardrommel", 0)) {
                    scene(p, stad, H3Scenes.AARDROMMEL, stap, null);
                }
            }
            case E2H3Aarde.THEEHUIS -> {
                if (stad.in(p, "theehuis", 0)) {
                    scene(p, stad, H3Scenes.JASMIJN_DRAAK, stap, null);
                }
            }
            case E2H3Aarde.KAMP -> {
                if (stad.in(p, "kamp", 1) && p.onGround()) {
                    appaWeg(p, stad);
                }
            }
            case E2H3Aarde.LU_TEN -> {
                if (stad.in(p, "lu_ten_heuvel", 0) && p.onGround()) {
                    luTen(p, stad);
                }
            }
            case E2H3Aarde.VAL -> {
                if (stad.in(p, "kristalkamer", 0) || stad.bij(p, "kristalcel", 7)) {
                    val(p, stad);
                }
            }
            case E2H3Aarde.TOPH -> finale(p, stad);   // (fix3-h3: only when the chain after the fall was broken off)
            default -> {
            }
        }
    }

    // =====================================================================================================================
    // the moments
    // =====================================================================================================================

    /** Plays a scene of a step; when it is over, the step moves on (only from that step). */
    static void scene(ServerPlayer p, E2H3Aarde.Stad stad, H3Scenes.Scene s, int stap, Runnable daarna) {
        BEZIG.put(p.getUUID(), nu(p));
        if (!H3Scenes.speel(p, stad, s, speler -> klaar(speler, stap, daarna))) {
            BEZIG.remove(p.getUUID());
        }
    }

    private static void klaar(ServerPlayer p, int stap, Runnable daarna) {
        BEZIG.remove(p.getUUID());
        if (E2H3Aarde.LIJN.verder(p, stap) && daarna != null) {
            daarna.run();
        }
    }

    /** Step 0: the narrator card, then the scene over the outer wall. */
    static void aankomst(ServerPlayer p, E2H3Aarde.Stad stad) {
        BEZIG.put(p.getUUID(), nu(p));
        E2H3Aarde.LIJN.begin(p);
        if (!Verteller.toon(p, E2H3Aarde.KAART, speler -> {
            BEZIG.remove(speler.getUUID());
            scene(speler, stad, H3Scenes.AANKOMST, E2H3Aarde.AANKOMST, null);
        })) {
            BEZIG.remove(p.getUUID());
        }
    }

    /** Step 9: Appa-guh is taken away from the camp ("Appa's verloren dagen"). */
    static void appaWeg(ServerPlayer p, E2H3Aarde.Stad stad) {
        Appa.houdWeg(p, true);
        H3Acteurs.wegAlle(p, "volger");
        BEZIG.put(p.getUUID(), nu(p));
        if (!H3Scenes.speelMetMuziek(p, stad, H3Scenes.APPA_ONTVOERD, "appa_verloren", speler -> klaar(speler, E2H3Aarde.KAMP, null))) {
            BEZIG.remove(p.getUUID());
        }
    }

    /** Step 11 ends at Appa's bars: Long Feng unmasked, the bars give, the reunion. */
    static void tralies(ServerPlayer p, E2H3Aarde.Stad stad) {
        BEZIG.put(p.getUUID(), nu(p));
        H3Sluipen.stop(p);
        if (!H3Scenes.speel(p, stad, H3Scenes.LONG_FENG, speler -> {
            E2H3Aarde.LIJN.vlag(speler, H3Schijn.V_TRALIES, true);
            H3Schijn.ververs(speler);
            if (!H3Scenes.speel(speler, stad, H3Scenes.WEERZIEN, s2 -> {
                Appa.houdWeg(s2, false);
                klaar(s2, E2H3Aarde.LAOGAI, null);
            })) {
                BEZIG.remove(speler.getUUID());
            }
        })) {
            BEZIG.remove(p.getUUID());
        }
    }

    /** Step 12: Miroh's song for Lu Ten under the tree. */
    static void luTen(ServerPlayer p, E2H3Aarde.Stad stad) {
        BEZIG.put(p.getUUID(), nu(p));
        if (!H3Scenes.speelMetMuziek(p, stad, H3Scenes.LU_TEN, "lu_ten", speler -> klaar(speler, E2H3Aarde.LU_TEN, null))) {
            BEZIG.remove(p.getUUID());
        }
    }

    /**
     * Step 13: at the crystal cell first the scene of Katara and Zuka in the cell (fix3-h3, playtest 2 item 8: why Zuka
     * stands with you), then the fall. After it (step 14) the finale runs on by itself ({@link #finale}).
     */
    static void val(ServerPlayer p, E2H3Aarde.Stad stad) {
        BEZIG.put(p.getUUID(), nu(p));
        if (Cutscenes.gezien(p, H3Scenes.KRISTALCEL.id())) {
            valZelf(p, stad);
        } else if (!H3Scenes.speel(p, stad, H3Scenes.KRISTALCEL, speler -> valZelf(speler, stad))) {
            BEZIG.remove(p.getUUID());
        }
    }

    private static void valZelf(ServerPlayer p, E2H3Aarde.Stad stad) {
        BEZIG.put(p.getUUID(), nu(p));
        if (!H3Scenes.speelMetMuziek(p, stad, H3Scenes.VAL, "val", speler -> {
            klaar(speler, E2H3Aarde.VAL, null);
            finale(speler, stad);
        })) {
            BEZIG.remove(p.getUUID());
        }
    }

    /**
     * Step 14 (fix3-h3, playtest 2 items 9 and 10), it runs by itself: Miroh is caught in the crystal chamber (only when you
     * are still down there: right after the fall), then the escape on Appa over the camp, where Toph-guh jumps on, and the
     * talk in flight about where to go. Then Toph is in the team and Appa flies on to the Fire Nation ({@link H3Rollen#klaar}).
     * There is nothing to talk to: a player who left in the middle gets the rest when back in the city.
     */
    static void finale(ServerPlayer p, E2H3Aarde.Stad stad) {
        if (E2H3Aarde.stap(p) != E2H3Aarde.TOPH) {
            BEZIG.remove(p.getUUID());
            return;
        }
        BEZIG.put(p.getUUID(), nu(p));
        if (!Cutscenes.gezien(p, H3Scenes.MIROH_GEVANGEN.id()) && stad.bij(p, "miroh_gevangen", 80)
                && H3Scenes.speel(p, stad, H3Scenes.MIROH_GEVANGEN, speler -> ontsnap(speler, stad))) {
            return;
        }
        ontsnap(p, stad);
    }

    private static void ontsnap(ServerPlayer p, E2H3Aarde.Stad stad) {
        BEZIG.put(p.getUUID(), nu(p));
        Vec3 kamp = stad.voet("aankomst_speler");
        if (kamp != null && !stad.bij(p, "aankomst_speler", 40)) {
            Duwtje.terug(p, stad.level().dimension(), kamp, -90f);   // (out of the catacombs: the escape plays over the camp)
        }
        if (!H3Scenes.speel(p, stad, H3Scenes.ONTSNAPPING, speler -> {
            BEZIG.put(speler.getUUID(), nu(speler));
            if (!H3Scenes.speel(speler, stad, H3Scenes.KOERS, s2 -> {
                BEZIG.remove(s2.getUUID());
                H3Rollen.klaar(s2);
            })) {
                BEZIG.remove(speler.getUUID());
            }
        })) {
            BEZIG.remove(p.getUUID());
        }
    }

    /**
     * fix3-h3 (playtest 2 item 1): the story scene that opens a step (H3Hints: why we go there, who the people are), then
     * the step's hint. False: it could not start now (the caller shows the hint).
     */
    static boolean brug(ServerPlayer p, E2H3Aarde.Stad stad, H3Scenes.Scene s, int stap, java.util.function.Consumer<ServerPlayer> daarna) {
        BEZIG.put(p.getUUID(), nu(p));
        if (H3Scenes.speel(p, stad, s, speler -> {
            BEZIG.remove(speler.getUUID());
            if (E2H3Aarde.stap(speler) == stap) {
                daarna.accept(speler);
            }
        })) {
            return true;
        }
        BEZIG.remove(p.getUUID());
        return false;
    }

    private static int nu(ServerPlayer p) {
        return p.level().getServer() == null ? 0 : p.level().getServer().getTickCount();
    }

    /** (dev command) forgets that a moment is on its way for this player. */
    static void vergeet(ServerPlayer p) {
        BEZIG.remove(p.getUUID());
        BINNEN.remove(p.getUUID());
    }

    /** The world spot of a marker of the copy this player is in (null: none). */
    static BlockPos pos(ServerPlayer p, String plek) {
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(p.level());
        return stad == null ? null : stad.pos(plek);
    }

    /** (used by the roles) is this npc one of the chapter's characters on the marker plek? */
    static boolean op(GuhNpcEntity npc, String plek) {
        return (E2H3Aarde.STRUCTUUR + "/" + plek).equals(npc.roleData.getStringOr(nl.juiced.guhs.feature.verhaal.NpcRollen.PLEK, ""));
    }
}
