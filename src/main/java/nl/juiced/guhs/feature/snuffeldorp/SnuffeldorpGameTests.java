package nl.juiced.guhs.feature.snuffeldorp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.guhpixel.Stempel;
import nl.juiced.guhs.feature.snuffel.BewonerEntity;
import nl.juiced.guhs.feature.snuffel.Bewoners;
import nl.juiced.guhs.feature.snuffel.Boom;
import nl.juiced.guhs.feature.snuffel.BoompjeEntity;
import nl.juiced.guhs.feature.snuffel.Daden;
import nl.juiced.guhs.feature.snuffel.Eiland;
import nl.juiced.guhs.feature.snuffel.Examen;
import nl.juiced.guhs.feature.snuffel.Geurbronnen;
import nl.juiced.guhs.feature.snuffel.Geuren;
import nl.juiced.guhs.feature.snuffel.Hondvorm;
import nl.juiced.guhs.feature.snuffel.MaatjeEntity;
import nl.juiced.guhs.feature.snuffel.Maatjes;
import nl.juiced.guhs.feature.snuffel.Rang;
import nl.juiced.guhs.feature.snuffel.Reis;
import nl.juiced.guhs.feature.snuffel.Snuffel;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.snuffel.SnuffelKluis;
import nl.juiced.guhs.feature.snuffel.Snuffelen;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.taal.NlTekst;

/**
 * Game tests of Snuffeldorp (batches {@code snuffeldorp} and {@code snuffeldorp_verhaal}; run with
 * {@code -Pgt=SnuffeldorpGameTests}). The game test server has no datapack dimensions, so the story is played on a small
 * test island marked on the floor {@code snuffeldorp_test_vloer} ({@link Proef}): it has EVERY resident and EVERY scent
 * source of the real island's data (the same keys and ids, on a grid), and its own named spots.
 * <ul>
 *   <li>{@code snuffeldorpHeleVerhaalTweeSpelers}: a mock player walks through the whole first series, step by step, the
 *   way a player does it (talking to the residents, sniffing and digging in real ticks, the scenes): washed ashore,
 *   Jutje, the doctor, the three lessons, the companion, good deeds and the tree, the exam, the diploma, the twig, the
 *   scarf, the end with the Guhstation, a fifth deed after the story, the roadblock, the captain's trip home. Then a
 *   SECOND player does all of it after the first, and the first one's story is still as it was.</li>
 *   <li>{@code snuffeldorpTussendoor}: home in the middle of the story and back (nothing replays, nothing is lost, the
 *   Guhdex says how to get back), a dog that runs past Jutje, what the residents say out of turn.</li>
 *   <li>{@code snuffeldorpWegversperring}: the line behind the roadblock for a dog, a builder and a spectator.</li>
 *   <li>{@code snuffeldorpAlleenInHetDorp}: on an island that is not Snuffeldorp (the kern's bare test islands) nothing
 *   of the village happens.</li>
 *   <li>{@code snuffeldorpEilandKlopt}: the REAL island's data: ten residents with a role each, every scent source the
 *   story names (buried or not as the story plays it), the spots, the line, the tiles, the scenes and their lengths,
 *   every conversation and screen line in Dutch.</li>
 * </ul>
 */
public class SnuffeldorpGameTests {
    private static final String BATCH = "snuffeldorp", VERHAAL = "snuffeldorp_verhaal", VLOER = "snuffeldorp_test_vloer";
    private static final Verhaallijn LIJN = SnuffelFeature.LIJN;
    private static final int GRENS = 3;

    /** One test's island (every resident and source of the real data, on a grid) and its players. */
    private static final class Proef {
        final GameTestHelper helper;
        final Eiland.Plaats plaats;
        final List<ServerPlayer> spelers = new ArrayList<>();
        final Map<String, BlockPos> bron = new HashMap<>();
        final Map<String, Boolean> graven = new HashMap<>();

        Proef(GameTestHelper helper, boolean dorp) {
            this.helper = helper;
            // (a kern test may have given a resident another role in this JVM: the village's own roles again)
            DorpRollen.init();
            Eiland.Opzet echt = Eiland.opzet();
            List<Eiland.BewonerPlek> bewoners = new ArrayList<>();
            int i = 0;
            for (Eiland.BewonerPlek b : echt.bewoners()) {
                bewoners.add(new Eiland.BewonerPlek(b.sleutel(), b.bewoner(), b.ras(), b.kleur(), b.pup(), new Vec3(2.5 + 3 * i++, 1, 5.5), 0f, ""));
            }
            List<Eiland.BronPlek> bronnen = new ArrayList<>();
            i = 0;
            for (Eiland.BronPlek b : echt.bronnen()) {
                BlockPos plek = b.id().equals(Dorp.SJAAL_BRON) ? new BlockPos(24, 1, 15) : new BlockPos(2 + 4 * (i % 4), 1, 12 + 4 * (i / 4));
                if (!b.id().equals(Dorp.SJAAL_BRON)) {
                    i++;
                }
                bronnen.add(new Eiland.BronPlek(b.id(), b.geur(), plek, b.graven(), 3));
                bron.put(b.id(), plek);
                graven.put(b.id(), b.graven());
            }
            Eiland.Opzet opzet = new Eiland.Opzet(1, BlockPos.ZERO, new Vec3i(31, 5, 31), List.of(), new Eiland.Punt(new Vec3(15.5, 1, 29.5), 180f),
                    new Eiland.Punt(new Vec3(28.5, 1, 10.5), 90f), new BlockPos(24, 1, 17), 0, 1, dorp ? bewoners : List.of(), dorp ? bronnen : List.of(), List.of());
            this.plaats = Eiland.test(helper.getLevel(), helper.absolutePos(new BlockPos(5, 1, 5)), opzet);
            if (dorp) {
                Plekken.test(plaats, new Plekken(Map.of(Plekken.STRAND, new BlockPos(15, 1, 29), Plekken.STRANDPOORT, new BlockPos(3, 1, 7),
                        Plekken.EMMER, new BlockPos(27, 2, 27), Plekken.PLEIN, new BlockPos(27, 1, 24), Plekken.WEIPOORT, new BlockPos(9, 1, 8),
                        Plekken.WEI, new BlockPos(9, 1, 7), Plekken.BOOM, new BlockPos(24, 1, 17), Plekken.HAVEN, new BlockPos(28, 1, 10),
                        Plekken.VERSPERRING, new BlockPos(15, 1, 7), Plekken.DOKTER, new BlockPos(5, 1, 7)), GRENS));
            }
        }

        /** "Home": a spot on the test floor outside the island. */
        Vec3 thuis(int welke) {
            BlockPos b = helper.absolutePos(welke == 0 ? new BlockPos(1, 2, 1) : new BlockPos(39, 2, 39));
            return new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        }

        /** A spot on the island (island coordinates, feet on the floor). */
        Vec3 op(double x, double z) {
            return plaats.wereld(new Vec3(x, 1, z));
        }

        ServerPlayer speler(int welke) {
            ServerPlayer p = GuhMockPlayer.of(helper);
            p.setGameMode(GameType.SURVIVAL);
            Vec3 t = thuis(welke);
            p.snapTo(t.x, t.y, t.z, 33f, -12f);
            p.setOnGround(true);
            spelers.add(p);
            return p;
        }

        void zet(ServerPlayer p, Vec3 waar) {
            p.teleportTo(helper.getLevel(), waar.x, waar.y, waar.z, Set.of(), p.getYRot(), p.getXRot(), true);
            p.setOnGround(true);
        }

        /** On the spot of a scent source. */
        void naarBron(ServerPlayer p, String id) {
            BlockPos b = bron.get(id);
            helper.assertTrue(b != null, "the island's data has the scent source " + id);
            zet(p, op(b.getX() + 0.5, b.getZ() + 0.5));
        }

        /** What a right-click on this resident does. */
        void praat(ServerPlayer p, String sleutel) {
            BewonerEntity npc = Eiland.bewoner(plaats, sleutel);
            Bewoners.Rol rol = Bewoners.rol(sleutel);
            helper.assertTrue(npc != null && rol != null, "resident " + sleutel + " stands on the island and has a role");
            rol.praat(npc, p);
        }

        boolean bewoond() {
            for (Eiland.BewonerPlek b : plaats.opzet().bewoners()) {
                if (Eiland.bewoner(plaats, b.sleutel()) == null) {
                    return false;
                }
            }
            return true;
        }

        /** What the server does for every real player every tick (the dog form, sniffing, the scenes' lock, the village's story). */
        void tick() {
            for (ServerPlayer p : spelers) {
                if (!p.isRemoved()) {
                    NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
                }
            }
        }

        void klaar() {
            for (ServerPlayer p : spelers) {
                if (Hondvorm.actief(p)) {
                    Reis.naarHuis(p);
                }
                Praat.vergeet(p);
                Wegversperring.vergeet(p.getUUID());
                Minigames.forget(p);
                if (!p.isRemoved()) {
                    helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
                }
            }
            for (Entity e : helper.getLevel().getEntities((Entity) null, helper.getBounds().inflate(6),
                    x -> x instanceof BoompjeEntity || x instanceof BewonerEntity || x instanceof MaatjeEntity)) {
                e.discard();
            }
            Plekken.testWeg(plaats);
            Eiland.testWeg(plaats);
        }
    }

    /** A test as a list of things to do, each followed by something to wait for (in real game ticks). */
    private static final class Draaiboek {
        private record Stap(String naam, Runnable doe, Runnable elkeTick, BooleanSupplier klaar) {
        }

        private final List<Stap> stappen = new ArrayList<>();
        private int nu = -1;
        private boolean af;

        /** Do this, then wait until {@code klaar}; {@code elkeTick} runs every tick while waiting (may be null). */
        void dan(String naam, Runnable doe, Runnable elkeTick, BooleanSupplier klaar) {
            stappen.add(new Stap(naam, doe, elkeTick, klaar));
        }

        void dan(String naam, Runnable doe) {
            dan(naam, doe, null, () -> true);
        }

        /** Lets the game run for this many ticks. */
        void wacht(GameTestHelper helper, int ticks) {
            long[] tot = {0};
            dan("wait " + ticks + " ticks", () -> tot[0] = helper.getLevel().getGameTime() + ticks, null, () -> helper.getLevel().getGameTime() >= tot[0]);
        }

        void speel(GameTestHelper helper, Proef t) {
            helper.onEachTick(() -> {
                if (af) {
                    return;
                }
                t.tick();
                while (nu < 0 || stappen.get(nu).klaar().getAsBoolean()) {
                    nu++;
                    if (nu >= stappen.size()) {
                        af = true;
                        t.klaar();
                        return;
                    }
                    stappen.get(nu).doe().run();
                }
                if (stappen.get(nu).elkeTick() != null) {
                    stappen.get(nu).elkeTick().run();
                }
            });
            helper.succeedWhen(() -> helper.assertTrue(af, "still waiting at step " + (nu + 1) + " of " + stappen.size() + ": "
                    + (nu >= 0 && nu < stappen.size() ? stappen.get(nu).naam() : "?")));
        }
    }

    private static boolean dicht(Vec3 a, Vec3 b) {
        return a.distanceToSqr(b) < 1.0e-6;
    }

    private static int tel(ServerPlayer p, net.minecraft.world.item.Item item) {
        return p.getInventory().countItem(item);
    }

    private static boolean leeg(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Sniffs up one scent source the way a dog does: on the spot, digging (a buried one) or nose down (one that hangs there). */
    private static void vind(Draaiboek d, Proef t, ServerPlayer p, String wie, String id) {
        boolean graven = t.graven.getOrDefault(id, true);
        d.dan(wie + " finds " + id, () -> {
            t.naarBron(p, id);
            Geurbronnen.Neus neus = Geurbronnen.ruik(p);
            t.helper.assertTrue(neus != null && neus.bron().id().equals(id) && neus.opDePlek(), wie + " smells " + id + " on the spot: " + neus);
            if (!graven) {
                Hondvorm.zetHouding(p, Hondvorm.SNUFFELT);
            }
        }, () -> {
            if (graven && !Snuffelen.graaft(p)) {
                Snuffelen.graaf(p);
            }
        }, () -> Geurbronnen.gevonden(p, id));
        d.dan(wie + " found " + id, () -> Hondvorm.zetHouding(p, 0));
    }

    /** Not in the air for this dog here and now. */
    private static void ruiktNiet(Proef t, ServerPlayer p, String id, String waarom) {
        t.naarBron(p, id);
        t.helper.assertTrue(Geurbronnen.ruik(p) == null && Geurbronnen.graafbaarBij(p) == null, waarom + ": " + Geurbronnen.ruik(p));
    }

    // =====================================================================================================================
    // the whole first series, two players one after the other
    // =====================================================================================================================

    /** Adds the whole story for one player to the script. */
    private static void verhaal(Draaiboek d, Proef t, ServerPlayer p, int thuis, String wie) {
        GameTestHelper helper = t.helper;
        Vec3 huis = t.thuis(thuis);
        int[] boom = {0};
        // --- washed ashore ---
        d.dan(wie + " washes ashore", () -> {
            helper.assertTrue(LIJN.stap(p) == 0 && !LIJN.begonnen(p) && Boom.stap(p) == 0 && Geuren.aantal(p) == 0 && Daden.aantal(p) == 0 && !Maatjes.heeft(p),
                    wie + " begins with nothing (whatever others did)");
            p.getInventory().setItem(3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKIE, 5));
            helper.assertTrue(Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND) && Hondvorm.actief(p), wie + " is a dog on the beach");
        }, null, () -> LIJN.vlag(p, Dorp.WAKKER) && !Cutscenes.bezig(p));
        d.dan(wie + " wakes up, the residents are there", () -> {
            helper.assertTrue(Cutscenes.gezien(p, DorpScenes.WAKKER.id()), "the waking scene played to its end");
            helper.assertTrue(LIJN.begonnen(p) && LIJN.stap(p) == SnuffelFeature.STAP_STRAND && "2".equals(LIJN.sleutel(p)), "step Aangespoeld: " + LIJN.sleutel(p));
            Doel doel = LIJN.doel(p);
            helper.assertTrue(doel != null && t.plaats.wereld(new BlockPos(3, 1, 7)).equals(doel.plek()), "Mijn verhaal points to the strandpoort");
        }, null, t::bewoond);
        // --- Jutje, the doctor ---
        d.dan(wie + " meets Jutje and the doctor", () -> {
            t.praat(p, "trainer");
            t.praat(p, "bakker");
            helper.assertTrue(LIJN.stap(p) == SnuffelFeature.STAP_STRAND && !LIJN.vlag(p, "bakker_gevraagd") && LIJN.teller(p, Dorp.LES) == 0,
                    "the trainer and the baker have nothing for a dog that just washed ashore");
            t.praat(p, "redder");
            helper.assertTrue(LIJN.stap(p) == SnuffelFeature.STAP_DOKTER && Gesprek.SLEUTEL.equals(Praat.lopend(p)), "Jutje sends you to the doctor (her screen is open)");
            t.praat(p, "redder");
            helper.assertTrue(LIJN.stap(p) == SnuffelFeature.STAP_DOKTER, "talking to her again changes nothing");
            t.praat(p, "dokter");
            helper.assertTrue(LIJN.stap(p) == SnuffelFeature.STAP_LES && "4".equals(LIJN.sleutel(p)), "the doctor sends you to the meadow");
            // --- the lessons: one source at a time ---
            ruiktNiet(t, p, Dorp.LESSEN.get(0), "no lesson yet: the bone is not in the air");
            t.praat(p, "trainer");
            helper.assertTrue(LIJN.teller(p, Dorp.LES) == 1 && "4_bot".equals(LIJN.sleutel(p)), "lesson 1 runs: " + LIJN.sleutel(p));
            ruiktNiet(t, p, Dorp.LESSEN.get(1), "the whistle is lesson 2's");
        });
        vind(d, t, p, wie, Dorp.LESSEN.get(0));
        d.dan(wie + " gets lesson 2", () -> {
            helper.assertTrue(LIJN.vlag(p, Dorp.LES_GEVONDEN) && "4_terug".equals(LIJN.sleutel(p)) && Geuren.kent(p, "kluifje"), "the bone is found: back to the trainer");
            ruiktNiet(t, p, Dorp.LESSEN.get(1), "the next lesson only begins at the trainer");
            t.praat(p, "trainer");
            helper.assertTrue(LIJN.teller(p, Dorp.LES) == 2 && !LIJN.vlag(p, Dorp.LES_GEVONDEN) && "4_fluit".equals(LIJN.sleutel(p)), "lesson 2 runs");
            t.praat(p, "trainer");
            helper.assertTrue(LIJN.teller(p, Dorp.LES) == 2, "asking again repeats the hint, the lesson stays");
        });
        vind(d, t, p, wie, Dorp.LESSEN.get(1));
        d.dan(wie + " gets lesson 3", () -> {
            t.praat(p, "trainer");
            helper.assertTrue(LIJN.teller(p, Dorp.LES) == 3 && "4_bij".equals(LIJN.sleutel(p)) && !t.graven.get(Dorp.LESSEN.get(2)), "lesson 3: a scent that is not buried");
        });
        vind(d, t, p, wie, Dorp.LESSEN.get(2));
        // --- the companion ---
        d.dan(wie + " hears the noise on the plein", () -> {
            helper.assertTrue(Geuren.kent(p, "fluitje") && Geuren.kent(p, "bijen") && Geuren.aantal(p) == 3, "three scents, three kinds");
            t.praat(p, "trainer");
            helper.assertTrue(LIJN.stap(p) == SnuffelFeature.STAP_MAATJE && "5".equals(LIJN.sleutel(p)) && !Maatjes.heeft(p), "the lessons are done: something rattles in the village");
            t.praat(p, "trainer");
            t.praat(p, "pup");
            helper.assertTrue(LIJN.stap(p) == SnuffelFeature.STAP_MAATJE && !LIJN.vlag(p, "pup_gevraagd"), "nobody has a chore before the companion is there");
            t.zet(p, t.op(26.5, 23.5));
        }, null, () -> LIJN.stap(p) == SnuffelFeature.STAP_DADEN && !Cutscenes.bezig(p));
        d.dan(wie + " has a companion", () -> {
            helper.assertTrue(Cutscenes.gezien(p, DorpScenes.MAATJE.id()) && Maatjes.heeft(p) && Geuren.kent(p, Dorp.GEEST), "the bucket scene played: the companion and its strange scent");
            helper.assertTrue("6".equals(LIJN.sleutel(p)) && Boom.stap(p) == 0, "the good deeds begin, the tree is a bare spot");
            t.zet(p, t.op(15.5, 9.5));
        });
        // --- four good deeds: ask, sniff up, bring back; the tree grows a step each time ---
        for (int i = 0; i < SnuffelFeature.DADEN_NODIG; i++) {
            Dorp.Klus k = Dorp.KLUSSEN.get(i);
            boolean laatste = i == SnuffelFeature.DADEN_NODIG - 1;
            d.dan(wie + " asks " + k.bewoner(), () -> {
                ruiktNiet(t, p, k.bron(), "nobody asked for it yet");
                t.praat(p, k.bewoner());
                helper.assertTrue(LIJN.vlag(p, k.gevraagd()) && "6_zoek".equals(LIJN.sleutel(p)), k.bewoner() + " asked: " + LIJN.sleutel(p));
            });
            vind(d, t, p, wie, k.bron());
            d.dan(wie + " brings it back to " + k.bewoner(), () -> {
                helper.assertTrue(LIJN.vlag(p, k.gevonden()) && "6_breng".equals(LIJN.sleutel(p)) && !Daden.heeft(p, k.daad()), "found, not brought back yet");
                boom[0] = Boom.stap(p);
                t.praat(p, k.bewoner());
                helper.assertTrue(Daden.heeft(p, k.daad()) && Boom.stap(p) == boom[0] + 1, "a good deed: the tree grows to stage " + Boom.stap(p));
                helper.assertTrue(Cutscenes.bezig(p), "the growth scene plays");
                helper.assertTrue(LIJN.stap(p) == (laatste ? SnuffelFeature.STAP_EXAMEN : SnuffelFeature.STAP_DADEN), "the step after deed " + Daden.aantal(p));
                VerhaalStand stand = LIJN.stand(p);
                helper.assertTrue(laatste || stand.nodig().size() == 1 && stand.nodig().get(0).heb() == Daden.aantal(p), "the Guhdex counts the deeds");
            }, null, () -> !Cutscenes.bezig(p));
        }
        // --- the exam ---
        d.dan(wie + " begins the exam", () -> {
            helper.assertTrue(Boom.stap(p) == Boom.MAX && "7".equals(LIJN.sleutel(p)), "a jong boompje: time for the exam");
            ruiktNiet(t, p, Dorp.EXAMEN_BRONNEN.get(0), "the exam's scents are not in the air before the exam");
            t.praat(p, "trainer");
            Examen.Loop loop = Examen.bezig(p);
            helper.assertTrue(loop != null && loop.examen().id().equals(Dorp.EXAMEN) && loop.gevonden() == 0 && loop.examen().bronnen().size() == 4
                    && "7_bezig".equals(LIJN.sleutel(p)), "the exam runs: 0 / 4");
        });
        for (String id : Dorp.EXAMEN_BRONNEN) {
            vind(d, t, p, wie, id);
        }
        d.dan(wie + " gets the diploma", () -> {
            helper.assertTrue(LIJN.vlag(p, Dorp.GESLAAGD) && Examen.gehaald(p, Dorp.EXAMEN) && Examen.bezig(p) == null && "7_diploma".equals(LIJN.sleutel(p)),
                    "passed: fetch the diploma");
            helper.assertTrue(!Snuffel.heeftDiploma(p) && LIJN.stap(p) == SnuffelFeature.STAP_EXAMEN, "the diploma is the trainer's to give");
            t.praat(p, "trainer");
            helper.assertTrue(Snuffel.heeftDiploma(p) && LIJN.stap(p) == SnuffelFeature.STAP_SPOOR && "8".equals(LIJN.sleutel(p)), "the diploma: a Snuffelpup");
            helper.assertTrue(Rang.van(p) == Rang.SNUFFELPUP && Rang.van(p).nummer() == 1, "rank 1 of 5");
            // --- the tree's gift, then father's scarf ---
            helper.assertTrue(!LIJN.vlag(p, Dorp.BLOESEM) && !Boom.cadeauGehad(p), "no twig before the dog comes to the tree");
            t.naarBron(p, Dorp.SJAAL_BRON);
            helper.assertTrue(Geurbronnen.graafbaarBij(p) == null, "the scarf is not in the air before the tree gave its twig");
        }, null, () -> LIJN.vlag(p, Dorp.BLOESEM));
        d.dan(wie + " digs up the scarf", () -> {
            helper.assertTrue(Boom.cadeauGehad(p) && SnuffelKluis.postAantal(p) == 1 && "8_snuffel".equals(LIJN.sleutel(p)), "the blossom twig travels home; now something strange is in the air");
            helper.assertTrue(!LIJN.klaar(p) && !Snuffel.heeftGuhstationGehad(p), "not finished yet");
        }, () -> {
            if (!Snuffelen.graaft(p) && !LIJN.vlag(p, Dorp.SJAAL)) {
                Snuffelen.graaf(p);
            }
        }, () -> LIJN.klaar(p) && !Cutscenes.bezig(p));
        // --- the end, and what stays possible after it ---
        Dorp.Klus extra = Dorp.KLUSSEN.get(SnuffelFeature.DADEN_NODIG);
        d.dan(wie + " finished the first series", () -> {
            helper.assertTrue(LIJN.vlag(p, Dorp.SJAAL) && Geuren.kent(p, "papa_sjaal") && Cutscenes.gezien(p, DorpScenes.SPOOR.id()), "the scarf, and the last scene");
            helper.assertTrue(Snuffel.klaar(p) && Snuffel.heeftGuhstationGehad(p) && SnuffelKluis.postAantal(p) == 2 && LIJN.stand(p).klaar(),
                    "the story is finished (what the Guhpad asks) and the Guhstation travels home with the twig");
            helper.assertTrue(Geuren.aantal(p) == 3 + 1 + SnuffelFeature.DADEN_NODIG + 4 + 1, "thirteen scents learned: " + Geuren.geleerd(p));
            helper.assertTrue(Hondvorm.actief(p) && tel(p, SnuffelFeature.GUHSTATION_ITEM.get()) == 0, "still a dog with a dog's pockets");
            t.praat(p, "trainer");
            t.praat(p, "dokter");
            t.praat(p, "redder");
            helper.assertTrue(LIJN.klaar(p) && Daden.aantal(p) == SnuffelFeature.DADEN_NODIG, "they only talk now");
            // a fifth good deed stays to be done
            t.praat(p, extra.bewoner());
            helper.assertTrue(LIJN.vlag(p, extra.gevraagd()), "the fifth resident still asks");
        });
        vind(d, t, p, wie, extra.bron());
        d.dan(wie + " does a fifth deed and meets the roadblock", () -> {
            t.praat(p, extra.bewoner());
            helper.assertTrue(Daden.aantal(p) == SnuffelFeature.DADEN_NODIG + 1 && Boom.stap(p) == Boom.MAX && !Cutscenes.bezig(p), "it counts; the tree was full-grown");
            // behind the roadblock's line: put back, unharmed
            helper.assertTrue(!Wegversperring.mag(p), "a Snuffelpup does not pass the roadblock");
            t.zet(p, t.op(15.5, 9.5));
        });
        d.wacht(helper, 3);
        d.dan(wie + " tries the closed part", () -> t.zet(p, t.op(15.5, 1.5)), null,
                () -> p.getZ() - t.plaats.oorsprong().getZ() >= GRENS);
        // --- Kapitein Zoutsnoet: a new Guhstation, and home ---
        d.dan(wie + " sails home", () -> {
            helper.assertTrue(dicht(p.position(), t.op(15.5, 9.5)) && p.getHealth() == p.getMaxHealth(), "put back where the dog last stood south of the line: " + p.position());
            t.praat(p, DorpRollen.KAPITEIN);
            helper.assertTrue("snuffeldorp_kapitein".equals(Praat.lopend(p)), "the captain asks");
            Praat.antwoord(p, Eiland.bewoner(t.plaats, DorpRollen.KAPITEIN), 2);
            helper.assertTrue(SnuffelKluis.postAantal(p) == 3, "a lost Guhstation is replaced for whoever finished");
            t.praat(p, DorpRollen.KAPITEIN);
            Praat.antwoord(p, Eiland.bewoner(t.plaats, DorpRollen.KAPITEIN), 1);
            helper.assertTrue(Cutscenes.bezig(p) && Hondvorm.actief(p), "the boat scene plays first");
        }, null, () -> !Hondvorm.actief(p) && !Cutscenes.bezig(p));
        d.dan(wie + " is home", () -> {
            helper.assertTrue(dicht(p.position(), huis) && p.level() == helper.getLevel(), "exactly home: " + p.position());
            helper.assertTrue(tel(p, SnuffelFeature.GUHSTATION_ITEM.get()) == 2 && tel(p, SnuffelFeature.SNUFFEL_BLOESEMTAKJE.get()) == 1
                    && p.getInventory().getItem(3).is(net.minecraft.world.item.Items.COOKIE) && p.getInventory().getItem(3).getCount() == 5,
                    "the own things back in their slots, the island's gifts added");
            helper.assertTrue(LIJN.klaar(p) && "klaar".equals(LIJN.sleutel(p)) && LIJN.doel(p) == null, "finished, at home");
        });
    }

    @GuhTest(template = VLOER, batch = VERHAAL, timeoutTicks = 6000)
    public static void snuffeldorpHeleVerhaalTweeSpelers(GameTestHelper helper) {
        Proef t = new Proef(helper, true);
        ServerPlayer p = t.speler(0), q = t.speler(1);
        Draaiboek d = new Draaiboek();
        verhaal(d, t, p, 0, "the first player");
        d.dan("the second player has nothing of the first", () -> helper.assertTrue(!LIJN.begonnen(q) && !Reis.bezocht(q) && leeg(q) && Daden.aantal(q) == 0,
                "everything is per player"));
        verhaal(d, t, q, 1, "the second player");
        d.dan("the first player's story is as it was", () -> {
            helper.assertTrue(LIJN.klaar(p) && Daden.aantal(p) == SnuffelFeature.DADEN_NODIG + 1 && Boom.stap(p) == Boom.MAX && Snuffel.heeftDiploma(p)
                    && tel(p, SnuffelFeature.GUHSTATION_ITEM.get()) == 2, "the first player lost nothing");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(BewonerEntity.class, helper.getBounds().inflate(4)).size() == t.plaats.opzet().bewoners().size(),
                    "the island still has exactly one of every resident");
        });
        d.speel(helper, t);
    }

    // =====================================================================================================================
    // out of turn
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 1200)
    public static void snuffeldorpTussendoor(GameTestHelper helper) {
        Proef t = new Proef(helper, true);
        ServerPlayer p = t.speler(0), q = t.speler(1);
        Draaiboek d = new Draaiboek();
        d.dan("two dogs wash ashore", () -> {
            helper.assertTrue(LIJN.sleutel(p).equals("0") && LIJN.doel(p) == null, "before the island: not this slice's step");
            Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
            Reis.naarEiland(q, t.plaats, Reis.Aankomst.STRAND);
        }, null, () -> LIJN.vlag(p, Dorp.WAKKER) && LIJN.vlag(q, Dorp.WAKKER) && !Cutscenes.bezig(p) && !Cutscenes.bezig(q) && t.bewoond());
        d.dan("home in the middle of the story", () -> {
            t.praat(p, "redder");
            helper.assertTrue(LIJN.stap(p) == SnuffelFeature.STAP_DOKTER && "3".equals(LIJN.sleutel(p)), "on the way to the doctor");
            Doel hier = LIJN.doel(p);
            helper.assertTrue(hier != null && t.plaats.wereld(new BlockPos(5, 1, 7)).equals(hier.plek()), "Mijn verhaal: the doctor's practice");
            helper.assertTrue(Reis.naarHuis(p) && !Hondvorm.actief(p), "the memory card: home");
            helper.assertTrue("thuis".equals(LIJN.sleutel(p)) && LIJN.stap(p) == SnuffelFeature.STAP_DOKTER, "the Guhdex says how to get back: " + LIJN.sleutel(p));
            Doel thuis = LIJN.doel(p);
            helper.assertTrue(thuis != null && "steigerhuisje".equals(thuis.structuur()), "Mijn verhaal at home: a steigerhuisje");
            // (forget that the waking scene was seen: were it to play again, it would be marked seen again)
            Cutscenes.vergeet(p, DorpScenes.WAKKER.id());
            helper.assertTrue(Reis.naarEiland(p, t.plaats, Reis.Aankomst.LAATSTE) && Hondvorm.actief(p), "and back");
        });
        d.wacht(helper, 12);
        d.dan("a dog that runs past Jutje", () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && !Cutscenes.gezien(p, DorpScenes.WAKKER.id()) && LIJN.stap(p) == SnuffelFeature.STAP_DOKTER
                    && "3".equals(LIJN.sleutel(p)), "the story goes on where it was, without the waking scene");
            helper.assertTrue(LIJN.stap(q) == SnuffelFeature.STAP_STRAND, "q never spoke to Jutje");
            t.praat(q, "trainer");
            helper.assertTrue(LIJN.stap(q) == SnuffelFeature.STAP_STRAND && LIJN.teller(q, Dorp.LES) == 0, "the trainer wants the doctor first");
            t.praat(q, "dokter");
            helper.assertTrue(LIJN.stap(q) == SnuffelFeature.STAP_LES, "the doctor hears the story anyway");
            t.praat(q, "redder");
            helper.assertTrue(LIJN.stap(q) == SnuffelFeature.STAP_LES, "Jutje just greets now");
            // the captain sails a dog home at any point of the story, without a Guhstation to give
            t.praat(q, DorpRollen.KAPITEIN);
            Praat.antwoord(q, Eiland.bewoner(t.plaats, DorpRollen.KAPITEIN), 2);
            helper.assertTrue(SnuffelKluis.postAantal(q) == 0 && Hondvorm.actief(q), "no Guhstation for a dog that did not finish");
            Praat.antwoord(q, Eiland.bewoner(t.plaats, DorpRollen.KAPITEIN), 0);
            helper.assertTrue(Hondvorm.actief(q) && !Cutscenes.bezig(q), "\"Ik blijf nog even\": nothing happens");
            // the exam cannot be started out of turn, and its scents are nobody's
            t.naarBron(q, Dorp.EXAMEN_BRONNEN.get(1));
            helper.assertTrue(Geurbronnen.ruik(q) == null && Examen.bezig(q) == null, "no exam scents in the air");
        });
        d.speel(helper, t);
    }

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 600)
    public static void snuffeldorpWegversperring(GameTestHelper helper) {
        Proef t = new Proef(helper, true);
        ServerPlayer hond = t.speler(0), bouwer = t.speler(1);
        Draaiboek d = new Draaiboek();
        Vec3 goed = t.op(10.5, 8.5), dichtbij = t.op(10.5, 4.5), achter = t.op(10.5, 1.5);
        d.dan("two dogs on the island", () -> {
            Reis.naarEiland(hond, t.plaats, Reis.Aankomst.STRAND);
            Reis.naarEiland(bouwer, t.plaats, Reis.Aankomst.STRAND);
            bouwer.setGameMode(GameType.CREATIVE);
            helper.assertTrue(!Wegversperring.mag(hond) && Wegversperring.mag(bouwer), "a builder passes, a Snuffelpup does not");
            Plekken pl = Plekken.van(t.plaats);
            helper.assertTrue(pl != null && pl.dicht(t.plaats, achter) && !pl.dicht(t.plaats, dichtbij) && !pl.dicht(t.plaats, goed), "the line");
        }, null, () -> LIJN.vlag(hond, Dorp.WAKKER) && !Cutscenes.bezig(hond) && !Cutscenes.bezig(bouwer));
        d.dan("a dog stands south of the line", () -> t.zet(hond, goed));
        d.wacht(helper, 3);
        d.dan("right in front of the roadblock is fine", () -> t.zet(hond, dichtbij));
        d.wacht(helper, 3);
        d.dan("behind it is not", () -> {
            helper.assertTrue(dicht(hond.position(), dichtbij), "nobody is moved in front of the roadblock");
            t.zet(hond, achter);
            t.zet(bouwer, achter);
        }, null, () -> !Plekken.van(t.plaats).dicht(t.plaats, hond.position()));
        d.dan("put back, unharmed; the builder stays", () -> {
            helper.assertTrue(dicht(hond.position(), goed), "back where the dog last stood well south of the line: " + hond.position());
            helper.assertTrue(hond.getHealth() == hond.getMaxHealth() && Hondvorm.actief(hond), "nothing hurts");
            helper.assertTrue(dicht(bouwer.position(), achter), "a builder in creative mode may look around there");
            bouwer.setGameMode(GameType.SPECTATOR);
            helper.assertTrue(Wegversperring.mag(bouwer), "a spectator passes too");
            // a dog that was never south of the line (a login behind it): the spot in front of the roadblock
            Wegversperring.vergeet(hond.getUUID());
            t.zet(hond, achter);
        }, null, () -> !Plekken.van(t.plaats).dicht(t.plaats, hond.position()));
        d.dan("without a known spot: in front of the roadblock", () -> helper.assertTrue(dicht(hond.position(), Vec3.atBottomCenterOf(t.plaats.wereld(new BlockPos(15, 1, 7)))),
                "at the roadblock's own spot: " + hond.position()));
        d.speel(helper, t);
    }

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 400)
    public static void snuffeldorpAlleenInHetDorp(GameTestHelper helper) {
        Proef t = new Proef(helper, false);
        ServerPlayer p = t.speler(0);
        Draaiboek d = new Draaiboek();
        long[] begin = {0};
        d.dan("a dog on an island that is not Snuffeldorp", () -> {
            helper.assertTrue(Plekken.van(t.plaats) == null, "a bare test island has no village spots");
            Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
            t.zet(p, t.op(15.5, 1.5));
            begin[0] = helper.getLevel().getGameTime();
        }, null, () -> helper.getLevel().getGameTime() >= begin[0] + 30);
        d.dan("nothing of the village happened", () -> {
            helper.assertTrue(dicht(p.position(), t.op(15.5, 1.5)), "no roadblock moved the dog");
            helper.assertTrue(!LIJN.vlag(p, Dorp.WAKKER) && !LIJN.begonnen(p) && LIJN.stap(p) == 0 && !Cutscenes.bezig(p), "no story began");
            helper.assertTrue(LIJN.doel(p) == null && "0".equals(LIJN.sleutel(p)), "the questline's own texts");
        });
        d.speel(helper, t);
    }

    // =====================================================================================================================
    // the real island's data
    // =====================================================================================================================

    @GuhTest(template = "empty", batch = BATCH)
    public static void snuffeldorpEilandKlopt(GameTestHelper helper) {
        List<String> mis = new ArrayList<>();
        Eiland.Opzet opzet = Eiland.opzet();
        Plekken pl = Plekken.echt();
        helper.assertTrue(opzet.versie() >= 2 && opzet.maat().getX() >= 120 && opzet.maat().getZ() >= 120, "the real island (not the kern's test island): " + opzet.maat());
        // ten residents, each with a role of the village; father is not on the island in this series
        Set<String> sleutels = new HashSet<>();
        for (Eiland.BewonerPlek b : opzet.bewoners()) {
            sleutels.add(b.sleutel());
            if (Bewoners.rol(b.sleutel()) == null) {
                mis.add("role of " + b.sleutel());
            }
            if ("vader".equals(b.bewoner())) {
                mis.add("father stands on the island");
            }
        }
        helper.assertTrue(opzet.bewoners().size() == 10 && sleutels.containsAll(List.of("redder", "dokter", "trainer", "bakker", "visser", "juf", "oma", "tuinder",
                "pup", DorpRollen.KAPITEIN)), "the ten residents: " + sleutels);
        // every source the story names, buried or hanging as the story plays it, all in the open part of the island
        Map<String, Eiland.BronPlek> bronnen = new HashMap<>();
        for (Eiland.BronPlek b : opzet.bronnen()) {
            bronnen.put(b.id(), b);
            if (b.plek().getZ() < pl.grensZ() + 2) {
                mis.add("source " + b.id() + " lies behind the roadblock");
            }
            if (Geuren.van(b.geur()) == null || Geuren.van(b.geur()).rang() != 1) {
                mis.add("source " + b.id() + ": a scent a Snuffelpup cannot smell");
            }
        }
        List<String> nodig = new ArrayList<>(Dorp.LESSEN);
        nodig.addAll(Dorp.EXAMEN_BRONNEN);
        nodig.add(Dorp.SJAAL_BRON);
        for (Dorp.Klus k : Dorp.KLUSSEN) {
            nodig.add(k.bron());
            if (!sleutels.contains(k.bewoner())) {
                mis.add("resident of " + k.daad());
            }
            if (!NlTekst.has("gui.guhs.snuffel.daad." + k.daad())) {
                mis.add("name of the deed " + k.daad());
            }
            for (String wat : List.of("hallo", "vraag", "hint", "terug", "dank")) {
                if (Plekken.paginas(k.bewoner() + "." + wat) < 1) {
                    mis.add("conversation " + k.bewoner() + "." + wat);
                }
            }
        }
        for (String id : nodig) {
            if (!bronnen.containsKey(id)) {
                mis.add("source " + id);
            }
        }
        helper.assertTrue(bronnen.size() == nodig.size(), "exactly the story's sources: " + bronnen.keySet());
        helper.assertTrue(Dorp.KLUSSEN.size() == 6 && Dorp.KLUSSEN.size() > SnuffelFeature.DADEN_NODIG, "six lost things, four needed");
        helper.assertTrue(mis.isEmpty() && bronnen.get(Dorp.LESSEN.get(0)).graven() && bronnen.get(Dorp.LESSEN.get(1)).graven() && !bronnen.get(Dorp.LESSEN.get(2)).graven(),
                "the lessons: dig, dig further away, sniff what is not buried " + mis);
        Set<Object> soorten = new HashSet<>();
        int hangend = 0;
        for (String id : Dorp.EXAMEN_BRONNEN) {
            soorten.add(Geuren.van(bronnen.get(id).geur()).soort());
            hangend += bronnen.get(id).graven() ? 0 : 1;
        }
        helper.assertTrue(soorten.size() == 4 && hangend == 2, "the exam: one of every kind of scent, two buried and two hanging");
        helper.assertTrue(bronnen.get(Dorp.SJAAL_BRON).plek().closerThan(opzet.boom(), 3) && Geuren.van(Dorp.GEEST) != null, "the scarf lies at the tree");
        // the named spots, inside the island's box, south of the line
        for (String naam : List.of(Plekken.STRAND, Plekken.STRANDPOORT, Plekken.EMMER, Plekken.PLEIN, Plekken.WEIPOORT, Plekken.WEI, Plekken.BOOM, Plekken.HAVEN,
                Plekken.VERSPERRING, Plekken.DOKTER)) {
            BlockPos p = pl.plekken().get(naam);
            if (p == null || p.getX() < 0 || p.getZ() <= pl.grensZ() || p.getX() >= opzet.maat().getX() || p.getZ() >= opzet.maat().getZ()) {
                mis.add("spot " + naam + " " + p);
            }
        }
        helper.assertTrue(pl.grensZ() > 8 && pl.grensZ() < opzet.strand().plek().z && pl.plekken().get(Plekken.BOOM).equals(opzet.boom())
                && BlockPos.containing(opzet.strand().plek()).equals(pl.plekken().get(Plekken.STRAND))
                && BlockPos.containing(opzet.haven().plek()).equals(pl.plekken().get(Plekken.HAVEN)), "the line, the tree, the beach and the harbour agree with the kern's data");
        // the tiles cover the box and exist
        helper.assertTrue(opzet.stukken().size() >= 4, "the island is saved as tiles");
        for (Eiland.Stuk s : opzet.stukken()) {
            Vec3i maat = Stempel.maat(helper.getLevel(), s.template());
            if (maat == null || s.plek().getX() + maat.getX() > opzet.maat().getX() || s.plek().getZ() + maat.getZ() > opzet.maat().getZ()
                    || maat.getY() > opzet.maat().getY()) {
                mis.add("tile " + s.template() + " " + maat);
            }
        }
        // the scenes: registered, their lengths, on the questline's page, every line in Dutch
        Map<Cutscene, Integer> scenes = Map.of(DorpScenes.WAKKER, 300, DorpScenes.MAATJE, 300, DorpScenes.SPOOR, 200, DorpScenes.AFVAART, 100);
        scenes.forEach((s, duur) -> {
            if (Cutscene.van(s.id()) != s || s.duur() != duur || !NlTekst.has(s.titelKey())) {
                mis.add("scene " + s.id());
            }
        });
        helper.assertTrue(Boom.GROEI.duur() == 120 && opzet.boomDraai() == 2, "the growth scene: six seconds, looking back over the village");
        // every conversation has all its pages in Dutch; the screen lines too
        Plekken.gesprekken().forEach((id, n) -> {
            for (int i = 0; i < n; i++) {
                if (!NlTekst.has(Gesprek.key(id, i))) {
                    mis.add("text " + Gesprek.key(id, i));
                }
            }
        });
        for (String id : List.of("redder.welkom", "redder.dag", "redder.klaar", "dokter.verhaal", "dokter.dag", "dokter.klaar", "trainer.eerst_dokter", "trainer.les1",
                "trainer.les1_hint", "trainer.les2", "trainer.les2_hint", "trainer.les3", "trainer.les3_hint", "trainer.lessen_klaar", "trainer.kabaal",
                "trainer.daden", "trainer.examen", "trainer.examen_hint", "trainer.diploma", "trainer.boom", "trainer.klaar", "kapitein.vraag",
                "kapitein.vraag_klaar", "kapitein.station")) {
            if (Plekken.paginas(id) < 1) {
                mis.add("conversation " + id);
            }
        }
        for (String key : List.of(Gesprek.OKE, "gui.guhs.snuffeldorp.optie.station", "gui.guhs.snuffeldorp.les_gevonden", "gui.guhs.snuffeldorp.breng_terug",
                "gui.guhs.snuffeldorp.examen_klaar", "gui.guhs.snuffeldorp.daden_klaar", "gui.guhs.snuffeldorp.versperring", "gui.guhs.snuffeldorp.wakker.1",
                "gui.guhs.snuffeldorp.wakker.2", "gui.guhs.snuffeldorp.maatje.1", "gui.guhs.snuffeldorp.maatje.2", "gui.guhs.snuffeldorp.bloesem.1",
                "gui.guhs.snuffeldorp.bloesem.2", "gui.guhs.snuffeldorp.einde.1", "gui.guhs.snuffeldorp.einde.2", "gui.guhs.snuffeldorp.einde.3",
                "gui.guhs.snuffeldorp.doel.strandpoort", "gui.guhs.snuffeldorp.doel.dokter", "gui.guhs.snuffeldorp.doel.wei", "gui.guhs.snuffeldorp.doel.plein",
                "gui.guhs.snuffeldorp.doel.boom", "gui.guhs.snuffeldorp.doel.terug", "gui.guhs.snuffel.examen." + Dorp.EXAMEN)) {
            if (!NlTekst.has(key)) {
                mis.add("text " + key);
            }
        }
        for (String s : List.of("thuis", "4_bot", "4_fluit", "4_bij", "4_terug", "6_zoek", "6_breng", "7_bezig", "7_diploma", "8_snuffel")) {
            if (!LIJN.extraSleutels().contains(s) || !NlTekst.has("gui.guhs.verhalen.snuffeleiland.nu." + s) || !NlTekst.has("gui.guhs.verhalen.snuffeleiland.kort." + s)) {
                mis.add("text variant " + s);
            }
        }
        helper.assertTrue(mis.isEmpty(), "missing or wrong: " + mis);
        helper.succeed();
    }
}
