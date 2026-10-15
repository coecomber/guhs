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
 *   <li>(1.4.1) {@code snuffeldorpZee}: on the little island {@code snuffeldorp_test_zee} with its own land map: three
 *   seconds in the sea and a dog stands where it last stood on land; the pond and the wet edge are no sea; a hop out of
 *   the water does not start the count again, touching land does; a deck is land and the water under it sea; a builder swims on; without a spot of its own the
 *   kern's last spot or the beach; and closed ground of the map counts as behind the roadblock (a dog that is already
 *   stuck there is put in front of it).</li>
 *   <li>(1.4.1) {@code snuffeldorpKaartKlopt}: the REAL island's land map: everything the story needs stands on walkable
 *   land, the pond and the well are no sea, the harbour basin is, the pocket behind the roadblock is closed ground.</li>
 * </ul>
 */
public class SnuffeldorpGameTests {
    private static final String BATCH = "snuffeldorp", VERHAAL = "snuffeldorp_verhaal", VLOER = "snuffeldorp_test_vloer", ZEE = "snuffeldorp_test_zee";
    private static final Verhaallijn LIJN = SnuffelFeature.LIJN;
    private static final int GRENS = 3;

    /** One test's island (every resident and source of the real data, on a grid) and its players. */
    private static final class Proef {
        final GameTestHelper helper;
        final Eiland.Plaats plaats;
        final List<ServerPlayer> spelers = new ArrayList<>();
        final Map<String, BlockPos> bron = new HashMap<>();
        final Map<String, Boolean> graven = new HashMap<>();
        final boolean zee;

        /**
         * (1.4.1) The sea rule's island: the whole template {@code snuffeldorp_test_zee} (tools/features/snuffel_dorp_bouw.py
         * test_zee: land with a pond, a sea one block deep, a deck over it) with the land map that was written with it.
         * No residents, no line: the map's closed ground (the land's two northern rows) is all that is closed.
         */
        Proef(GameTestHelper helper, Landkaart kaart) {
            this.helper = helper;
            this.zee = true;
            DorpRollen.init();
            Eiland.Opzet opzet = new Eiland.Opzet(1, BlockPos.ZERO, new Vec3i(kaart.breed(), 4, kaart.diep()), List.of(), new Eiland.Punt(new Vec3(5.5, 2, 9.5), 180f),
                    new Eiland.Punt(new Vec3(11.5, 3, 6.5), 90f), new BlockPos(8, 2, 11), 0, 1, List.of(), List.of(), List.of());
            this.plaats = Eiland.test(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 0)), opzet);
            Plekken.test(plaats, new Plekken(Map.of(Plekken.VERSPERRING, new BlockPos(5, 2, 3)), Plekken.GEEN_GRENS));
            Landkaart.test(plaats, kaart);
        }

        Proef(GameTestHelper helper, boolean dorp) {
            this.helper = helper;
            this.zee = false;
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
            BlockPos b = helper.absolutePos(zee ? new BlockPos(26, 2, welke == 0 ? 4 : 8) : welke == 0 ? new BlockPos(1, 2, 1) : new BlockPos(39, 2, 39));
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
                Zee.vergeet(p.getUUID());
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
            Landkaart.testWeg(plaats);
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
            helper.assertTrue(Snuffel.klaar(p) && Snuffel.heeftGuhstationGehad(p) && Snuffel.heeftPlaatGehad(p) && SnuffelKluis.postAantal(p) == 3
                    && LIJN.stand(p).klaar(), "the story is finished (what the Guhpad asks); the Guhstation and the music disc travel home with the twig");
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
            helper.assertTrue(SnuffelKluis.postAantal(p) == 4, "a lost Guhstation is replaced for whoever finished (and no second music disc)");
            t.praat(p, DorpRollen.KAPITEIN);
            Praat.antwoord(p, Eiland.bewoner(t.plaats, DorpRollen.KAPITEIN), 1);
            helper.assertTrue(Cutscenes.bezig(p) && Hondvorm.actief(p), "the boat scene plays first");
        }, null, () -> !Hondvorm.actief(p) && !Cutscenes.bezig(p));
        d.dan(wie + " is home", () -> {
            helper.assertTrue(dicht(p.position(), huis) && p.level() == helper.getLevel(), "exactly home: " + p.position());
            helper.assertTrue(tel(p, SnuffelFeature.GUHSTATION_ITEM.get()) == 2 && tel(p, SnuffelFeature.SNUFFEL_BLOESEMTAKJE.get()) == 1
                    && tel(p, SnuffelFeature.MUZIEKPLAAT.get()) == 1 && p.getInventory().getItem(3).is(net.minecraft.world.item.Items.COOKIE) && p.getInventory().getItem(3).getCount() == 5,
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
            // (merge verhalenpad: the steps 0-1 are the dock's, and its goal is a steigerhuisje; nothing of the village yet)
            helper.assertTrue(LIJN.sleutel(p).equals("0") && steigerDoel(p), "before the island: not this slice's step, the dock's goal");
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
            helper.assertTrue(SnuffelKluis.postAantal(q) == 0 && !Snuffel.heeftPlaatGehad(q) && Hondvorm.actief(q),
                    "no Guhstation and no music disc for a dog that did not finish");
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

    /**
     * (merge verhalenpad) The hand-over from the dock, which the two slices never tried together: the crossing leaves a
     * player on the beach at step 2 already ({@code SteigerVerhaal.aanLand}: {@code Snuffel.spoelAan}, then
     * {@code LIJN.verder(STAP_UITVAREN)}), with the sickbed's flag and a chosen dog. The village takes over there: the
     * waking scene plays, the step is not moved a second time, the text and "Mijn verhaal" are the village's from then on.
     */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 600)
    public static void snuffeldorpNaDeOvertocht(GameTestHelper helper) {
        Proef t = new Proef(helper, true);
        ServerPlayer p = t.speler(0);
        Draaiboek d = new Draaiboek();
        d.dan("the dock's crossing ends on the beach", () -> {
            // what the dock did for this player before the island: the feast, the sickbed, the choice
            LIJN.begin(p);
            helper.assertTrue(LIJN.verder(p, SnuffelFeature.STAP_STEIGER), "the feast at the dock: step 0 done");
            LIJN.vlag(p, nl.juiced.guhs.feature.snuffelsteiger.SteigerVerhaal.ZIEKBED, true);
            helper.assertTrue(nl.juiced.guhs.feature.snuffel.Keuze.zet(p, new nl.juiced.guhs.feature.snuffel.Keuze("jackrussell", "driekleur", "Stuiter", "b")),
                    "the choice at the captain");
            helper.assertTrue("1".equals(LIJN.sleutel(p)) && steigerDoel(p), "at the dock: step 1 with the dock's text and goal: " + LIJN.sleutel(p));
            // SteigerVerhaal.aanLand(p, true), with this test's island named: washed ashore, then the dock's step is done
            helper.assertTrue(Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND) && LIJN.verder(p, SnuffelFeature.STAP_UITVAREN), "washed ashore: step 1 done");
            helper.assertTrue(LIJN.stap(p) == SnuffelFeature.STAP_STRAND && !LIJN.vlag(p, Dorp.WAKKER) && Hondvorm.actief(p),
                    "a dog at step 2; the village has not seen it yet");
        }, null, () -> LIJN.vlag(p, Dorp.WAKKER) && !Cutscenes.bezig(p) && t.bewoond());
        d.dan("the village takes over", () -> {
            helper.assertTrue(Cutscenes.gezien(p, DorpScenes.WAKKER.id()), "the waking scene played after the crossing");
            helper.assertTrue(LIJN.stap(p) == SnuffelFeature.STAP_STRAND && "2".equals(LIJN.sleutel(p)),
                    "still step 2 (not moved a second time), with the village's text: " + LIJN.stap(p) + " / " + LIJN.sleutel(p));
            Doel doel = LIJN.doel(p);
            helper.assertTrue(doel != null && t.plaats.wereld(new BlockPos(3, 1, 7)).equals(doel.plek()), "Mijn verhaal: the strandpoort now, not a steigerhuisje");
            helper.assertTrue(Reis.thuis(p) != null && Reis.thuis(p).plek().distanceTo(t.thuis(0)) < 0.01, "home is where the crossing began");
            t.praat(p, "redder");
            helper.assertTrue(LIJN.stap(p) == SnuffelFeature.STAP_DOKTER, "Jutje sends the dog on to the doctor");
        });
        d.speel(helper, t);
    }

    /** (merge verhalenpad) the goal of the steps 0-1, which the dock slice registers: the nearest steigerhuisje in the Guhmensie. */
    private static boolean steigerDoel(ServerPlayer p) {
        nl.juiced.guhs.feature.verhaal.Doel d = LIJN.doel(p);
        return d != null && d.dim() == nl.juiced.guhs.world.ModDimensions.GUHMENSION && "steigerhuisje".equals(d.structuur());
    }

    /**
     * The island's music disc: once per player. With the Guhstation at the end of the story; from Kapitein Zoutsnoet for
     * whoever had finished before the disc existed; never for a dog that did not finish; never twice.
     */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 600)
    public static void snuffeldorpMuziekplaat(GameTestHelper helper) {
        Proef t = new Proef(helper, true);
        ServerPlayer p = t.speler(0), q = t.speler(1);
        net.minecraft.world.item.Item plaat = SnuffelFeature.MUZIEKPLAAT.get();
        Draaiboek d = new Draaiboek();
        d.dan("an old save: the story finished before the disc existed", () -> {
            helper.assertTrue(Snuffel.rondAf(p) && tel(p, plaat) == 1 && tel(p, SnuffelFeature.GUHSTATION_ITEM.get()) == 1 && Snuffel.heeftPlaatGehad(p),
                    "a player who finishes at home gets the Guhstation and the disc at once");
            p.getInventory().clearContent();
            nl.juiced.guhs.feature.snuffel.SnuffelData.van(p).remove("Muziekplaat");
            helper.assertTrue(Snuffel.klaar(p) && Snuffel.heeftGuhstationGehad(p) && !Snuffel.heeftPlaatGehad(p), "finished, a Guhstation, no disc yet");
            helper.assertTrue(!LIJN.stand(p).beloningen().get(1).binnen() && LIJN.stand(p).beloningen().get(0).binnen(), "the Guhdex: the disc is still to come");
            // (both saw the waking scene long ago)
            LIJN.vlag(p, Dorp.WAKKER, true);
            LIJN.begin(q);
            LIJN.zet(q, SnuffelFeature.STAP_DOKTER);
            LIJN.vlag(q, Dorp.WAKKER, true);
            Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
            Reis.naarEiland(q, t.plaats, Reis.Aankomst.STRAND);
        }, null, () -> t.bewoond() && Hondvorm.actief(p) && Hondvorm.actief(q) && !Cutscenes.bezig(p) && !Cutscenes.bezig(q));
        d.wacht(helper, 5);
        d.dan("the captain hands it over, once", () -> {
            BewonerEntity kapitein = Eiland.bewoner(t.plaats, DorpRollen.KAPITEIN);
            helper.assertTrue(!Snuffel.rondAf(p) && !Snuffel.heeftPlaatGehad(p) && SnuffelKluis.postAantal(p) == 0, "finishing again does not give it: the captain does");
            t.praat(p, DorpRollen.KAPITEIN);
            helper.assertTrue(Snuffel.heeftPlaatGehad(p) && SnuffelKluis.postAantal(p) == 1 && "snuffeldorp_kapitein".equals(Praat.lopend(p)),
                    "the next talk with the captain: the disc goes in the post, and he asks his question as always");
            helper.assertTrue(LIJN.stand(p).beloningen().get(1).binnen(), "the Guhdex ticks it");
            Praat.antwoord(p, kapitein, 0);
            t.praat(p, DorpRollen.KAPITEIN);
            Praat.antwoord(p, kapitein, 0);
            t.praat(p, DorpRollen.KAPITEIN);
            helper.assertTrue(SnuffelKluis.postAantal(p) == 1 && !Snuffel.geefPlaat(p), "never a second one");
            // a new Guhstation does not bring a new disc
            Praat.antwoord(p, kapitein, 2);
            helper.assertTrue(SnuffelKluis.postAantal(p) == 2, "a replaced Guhstation, no disc with it");
            // a dog in the middle of the story gets none
            t.praat(q, DorpRollen.KAPITEIN);
            Praat.antwoord(q, kapitein, 0);
            helper.assertTrue(!Snuffel.heeftPlaatGehad(q) && SnuffelKluis.postAantal(q) == 0, "per player: nothing for a dog that did not finish");
            // ...until it finishes: then with the Guhstation, and the captain has nothing more to give
            helper.assertTrue(Snuffel.rondAf(q) && Snuffel.heeftPlaatGehad(q) && SnuffelKluis.postAantal(q) == 3, "at the end: the twig, the Guhstation and the disc");
            t.praat(q, DorpRollen.KAPITEIN);
            Praat.antwoord(q, kapitein, 0);
            helper.assertTrue(SnuffelKluis.postAantal(q) == 3, "not again from the captain");
            helper.assertTrue(Reis.naarHuis(p) && Reis.naarHuis(q), "home");
            helper.assertTrue(tel(p, plaat) == 1 && tel(p, SnuffelFeature.GUHSTATION_ITEM.get()) == 1, "the old save: exactly one disc at home (and the new Guhstation)");
            helper.assertTrue(tel(q, plaat) == 1 && tel(q, SnuffelFeature.GUHSTATION_ITEM.get()) == 1 && tel(q, SnuffelFeature.SNUFFEL_BLOESEMTAKJE.get()) == 1,
                    "the fresh finish: exactly one disc at home");
        });
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
            // (merge verhalenpad: step 0 has the dock's goal, a steigerhuisje; the village adds none on an island that is not its own)
            helper.assertTrue(steigerDoel(p) && "0".equals(LIJN.sleutel(p)), "the questline's own texts, the dock's goal");
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

    // =====================================================================================================================
    // 1.4.1: the sea, and the closed ground of the land map
    // =====================================================================================================================

    /** Puts a mock player somewhere on the test island (island coordinates): standing on the ground, or afloat. */
    private static void zetOp(Proef t, ServerPlayer p, Vec3 rel, boolean opGrond) {
        t.zet(p, t.plaats.wereld(rel));
        p.setOnGround(opGrond);
    }

    @GuhTest(template = ZEE, batch = BATCH, timeoutTicks = 1600)
    public static void snuffeldorpZee(GameTestHelper helper) {
        Landkaart kaart = Landkaart.lees(Landkaart.TEST_PAD);
        helper.assertTrue(kaart != null && kaart.breed() == 21 && kaart.diep() == 13, "the test island's land map is in the jar");
        Proef t = new Proef(helper, kaart);
        ServerPlayer hond = t.speler(0), bouwer = t.speler(1);
        Draaiboek d = new Draaiboek();
        // island coordinates: the land's floor is y 2, the sea's water is the block y 1, the deck's floor is y 3
        Vec3 landA = new Vec3(5.5, 2, 5.5), landB = new Vec3(7.5, 2, 8.5), zee = new Vec3(15.5, 1, 3.5), rand = new Vec3(10.5, 1, 3.5),
                vijver = new Vec3(4.5, 1, 6.5), dek = new Vec3(12.5, 3, 6.5), onderDek = new Vec3(12.5, 1, 6.5), dichtGrond = new Vec3(5.5, 2, 0.5),
                versperring = new Vec3(5.5, 2, 3.5), strand = new Vec3(5.5, 2, 9.5);
        long[] sinds = {0};
        Runnable inZee = () -> {
            zetOp(t, hond, zee, false);
            sinds[0] = helper.getLevel().getGameTime();
        };
        BooleanSupplier eruit = () -> !dicht(hond.position(), t.plaats.wereld(zee));
        d.dan("two dogs on the island", () -> {
            Eiland.Plaats pl = t.plaats;
            helper.assertTrue(kaart.zee(pl, pl.wereld(zee)) && !kaart.zee(pl, pl.wereld(rand)) && !kaart.zee(pl, pl.wereld(vijver)) && kaart.zee(pl, pl.wereld(onderDek))
                    && kaart.zee(pl, pl.wereld(new Vec3(40.5, 1, 3.5))), "the map: the sea, the wet edge, the pond, under the deck, outside the map");
            helper.assertTrue(kaart.veilig(pl, pl.wereld(landA)) && kaart.veilig(pl, pl.wereld(dek)) && !kaart.veilig(pl, pl.wereld(vijver))
                    && !kaart.veilig(pl, pl.wereld(rand)) && !kaart.veilig(pl, pl.wereld(dichtGrond)), "the map: where a dog is put back");
            helper.assertTrue(helper.getLevel().getFluidState(BlockPos.containing(pl.wereld(zee))).is(net.minecraft.tags.FluidTags.WATER)
                    && helper.getLevel().getFluidState(BlockPos.containing(pl.wereld(vijver))).is(net.minecraft.tags.FluidTags.WATER)
                    && helper.getLevel().getFluidState(BlockPos.containing(pl.wereld(onderDek))).is(net.minecraft.tags.FluidTags.WATER)
                    && helper.getLevel().getFluidState(BlockPos.containing(pl.wereld(landA))).isEmpty(), "the template: water where the map says water");
            Reis.naarEiland(hond, t.plaats, Reis.Aankomst.STRAND);
            Reis.naarEiland(bouwer, t.plaats, Reis.Aankomst.STRAND);
            bouwer.setGameMode(GameType.CREATIVE);
        }, null, () -> LIJN.vlag(hond, Dorp.WAKKER) && !Cutscenes.bezig(hond) && !Cutscenes.bezig(bouwer));
        d.dan("the arrival on the beach moves nobody", () -> {
            helper.assertTrue(dicht(hond.position(), t.plaats.wereld(strand)) && Hondvorm.actief(hond), "the dog stands on the beach: " + hond.position());
            zetOp(t, hond, landA, true);
        });
        d.wacht(helper, 3);
        d.dan("the pond is no sea", () -> {
            helper.assertTrue(Zee.veilig(hond) != null && dicht(Zee.veilig(hond), t.plaats.wereld(landA)), "the spot on land is remembered: " + Zee.veilig(hond));
            zetOp(t, hond, vijver, false);
        });
        d.wacht(helper, Zee.TICKS + 30);
        d.dan("the wet edge of the beach is no sea", () -> {
            helper.assertTrue(dicht(hond.position(), t.plaats.wereld(vijver)), "a dog may splash in the pond as long as it likes: " + hond.position());
            zetOp(t, hond, rand, false);
        });
        d.wacht(helper, Zee.TICKS + 30);
        d.dan("the sea: nothing happens for two seconds", () -> {
            helper.assertTrue(dicht(hond.position(), t.plaats.wereld(rand)), "a dog may paddle in the wet edge as long as it likes: " + hond.position());
            helper.assertTrue(dicht(Zee.veilig(hond), t.plaats.wereld(landA)), "water is never a spot to go back to");
            inZee.run();
            zetOp(t, bouwer, zee, false);
        });
        d.wacht(helper, 25);
        // (a swimmer bobs and hops out of the water all the time: that does not start the count again)
        d.dan("a hop out of the water", () -> zetOp(t, hond, zee.add(0, 1.5, 0), false));
        d.wacht(helper, 8);
        d.dan("and in again", () -> zetOp(t, hond, zee, false));
        d.wacht(helper, 7);
        d.dan("and after about three seconds the dog is put back", () -> helper.assertTrue(!eruit.getAsBoolean(), "two seconds in the sea is fine: " + hond.position()),
                null, eruit);
        d.dan("where it last stood on land, unharmed; a builder swims on", () -> {
            long duur = helper.getLevel().getGameTime() - sinds[0];
            helper.assertTrue(duur >= Zee.TICKS - 2 && duur <= Zee.TICKS + 5, "after about three seconds: " + duur + " ticks");
            helper.assertTrue(dicht(hond.position(), t.plaats.wereld(landA)), "back on the last spot on land: " + hond.position());
            helper.assertTrue(hond.getHealth() == hond.getMaxHealth() && Hondvorm.actief(hond), "nothing hurts, still a dog");
            helper.assertTrue(dicht(bouwer.position(), t.plaats.wereld(zee)), "a builder in creative mode is left alone");
            bouwer.setGameMode(GameType.SPECTATOR);
            // touching land starts the count again
            inZee.run();
        });
        d.wacht(helper, 40);
        d.dan("out of the sea for a moment", () -> zetOp(t, hond, landB, true));
        d.wacht(helper, 3);
        d.dan("and in again", inZee);
        d.wacht(helper, 40);
        d.dan("the three seconds count from the last time in", () -> helper.assertTrue(!eruit.getAsBoolean(), "40 + 40 ticks with land in between is fine"), null, eruit);
        d.dan("back on the NEW spot; the deck is land", () -> {
            long duur = helper.getLevel().getGameTime() - sinds[0];
            helper.assertTrue(duur >= Zee.TICKS - 2 && dicht(hond.position(), t.plaats.wereld(landB)), "the last spot on land, after " + duur + " ticks: " + hond.position());
            helper.assertTrue(dicht(bouwer.position(), t.plaats.wereld(zee)), "a spectator is left alone");
            zetOp(t, hond, dek, true);
        });
        d.wacht(helper, 3);
        d.dan("the water under the deck is sea", () -> {
            helper.assertTrue(dicht(Zee.veilig(hond), t.plaats.wereld(dek)), "the deck is a spot to go back to");
            zetOp(t, hond, onderDek, false);
        }, null, () -> !dicht(hond.position(), t.plaats.wereld(onderDek)));
        d.dan("back on the deck; home from a spot on land", () -> {
            helper.assertTrue(dicht(hond.position(), t.plaats.wereld(dek)), "fallen off the jetty: back on the jetty: " + hond.position());
            // no spot of its own (a login in the water, a server that started again): the kern's last spot when that is land
            zetOp(t, hond, landB, true);
            helper.assertTrue(Reis.naarHuis(hond), "home with the memory card");
            Reis.naarEiland(hond, t.plaats, Reis.Aankomst.STRAND);
            Zee.vergeet(hond.getUUID());
            inZee.run();
        }, null, eruit);
        d.dan("no spot of its own: the kern's last spot", () -> {
            helper.assertTrue(dicht(hond.position(), t.plaats.wereld(landB)), "the last spot the kern remembers: " + hond.position());
            // and when that is no land either (it lies in the pond): the beach
            zetOp(t, hond, vijver, true);
            helper.assertTrue(Reis.naarHuis(hond), "home again");
            Reis.naarEiland(hond, t.plaats, Reis.Aankomst.STRAND);
            Zee.vergeet(hond.getUUID());
            inZee.run();
        }, null, eruit);
        d.dan("no good spot at all: the beach; a dog that is stuck on closed ground", () -> {
            helper.assertTrue(dicht(hond.position(), t.plaats.wereld(strand)), "the beach: " + hond.position());
            // the map's closed ground counts as behind the roadblock, also for a dog that already stands there (no spot known)
            Wegversperring.vergeet(hond.getUUID());
            Zee.vergeet(hond.getUUID());
            bouwer.setGameMode(GameType.CREATIVE);
            zetOp(t, hond, dichtGrond, true);
            zetOp(t, bouwer, dichtGrond, true);
        }, null, () -> !dicht(hond.position(), t.plaats.wereld(dichtGrond)));
        d.dan("is put in front of the roadblock", () -> {
            helper.assertTrue(dicht(hond.position(), t.plaats.wereld(versperring)), "at the roadblock's own spot: " + hond.position());
            helper.assertTrue(dicht(bouwer.position(), t.plaats.wereld(dichtGrond)), "a builder may stand on closed ground");
            helper.assertTrue(hond.getHealth() == hond.getMaxHealth() && Hondvorm.actief(hond), "nothing hurts");
            zetOp(t, hond, landA, true);
        });
        d.wacht(helper, 3);
        d.dan("with a spot on land: back there", () -> zetOp(t, hond, dichtGrond, true), null, () -> !dicht(hond.position(), t.plaats.wereld(dichtGrond)));
        d.dan("the dog's southern edge decides", () -> {
            helper.assertTrue(dicht(hond.position(), t.plaats.wereld(landA)), "back where it last stood on open land: " + hond.position());
            // leaning against the closed ground from the south: the middle of the dog is over the closed row, its southern edge is not
            zetOp(t, hond, new Vec3(5.5, 2, 1.8), true);
        });
        d.wacht(helper, 5);
        d.dan("a step further north it is closed", () -> {
            helper.assertTrue(dicht(hond.position(), t.plaats.wereld(new Vec3(5.5, 2, 1.8))), "leaning against it is not behind it: " + hond.position());
            helper.assertTrue(dicht(Zee.veilig(hond), t.plaats.wereld(landA)), "closed ground is never a spot to go back to");
            zetOp(t, hond, new Vec3(5.5, 2, 1.6), true);
        }, null, () -> dicht(hond.position(), t.plaats.wereld(landA)));
        d.speel(helper, t);
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void snuffeldorpKaartKlopt(GameTestHelper helper) {
        Landkaart kaart = Landkaart.echt();
        Eiland.Opzet opzet = Eiland.opzet();
        Plekken pl = Plekken.echt();
        helper.assertTrue(kaart != null && kaart.breed() == opzet.maat().getX() && kaart.diep() == opzet.maat().getZ(), "the land map covers the island's box");
        // (an island with its corner on 0, 0, 0: world = the island's own coordinates)
        Eiland.Plaats hier = new Eiland.Plaats(helper.getLevel(), BlockPos.ZERO, opzet, true);
        List<String> mis = new ArrayList<>();
        if (kaart.vak(hier, opzet.strand().plek()) != Landkaart.LAND || kaart.vak(hier, opzet.haven().plek()) != Landkaart.DEK) {
            mis.add("the beach is land, the harbour's arrival a deck");
        }
        for (Eiland.BewonerPlek b : opzet.bewoners()) {
            if (!naast(kaart, b.plek().x, b.plek().z)) {
                mis.add("resident " + b.sleutel() + " stands on walkable land");
            }
        }
        for (Eiland.BronPlek b : opzet.bronnen()) {
            if (!naast(kaart, b.plek().getX(), b.plek().getZ())) {
                mis.add("source " + b.id() + " lies at walkable land");
            }
        }
        for (Map.Entry<String, BlockPos> e : pl.plekken().entrySet()) {
            Vec3 v = Vec3.atBottomCenterOf(e.getValue());
            if (!e.getKey().equals(Plekken.EMMER) && (!kaart.veilig(hier, v) || Wegversperring.dicht(hier, pl, kaart, v))) {
                mis.add("spot " + e.getKey() + " is walkable land in front of the roadblock");
            }
        }
        // what is sea and what is not
        BlockPos plein = pl.plekken().get(Plekken.PLEIN), haven = pl.plekken().get(Plekken.HAVEN), weg = pl.plekken().get(Plekken.VERSPERRING);
        Vec3 put = new Vec3(plein.getX() + 0.5, 20, plein.getZ() + 5.5), vijver = new Vec3(64.5, 20, 62.5), bassin = new Vec3(haven.getX() + 0.5, 19, haven.getZ() + 8.5);
        helper.assertTrue(kaart.vak(hier, put) == Landkaart.EILAND && kaart.vak(hier, vijver) == Landkaart.EILAND && !kaart.zee(hier, put) && !kaart.zee(hier, vijver),
                "the well and the pond are the island's own water");
        helper.assertTrue(kaart.vak(hier, bassin) == Landkaart.ZEE && kaart.zee(hier, Vec3.atBottomCenterOf(haven)) && kaart.veilig(hier, Vec3.atBottomCenterOf(haven)),
                "the harbour basin is sea; the jetty is land to stand on and sea to swim under");
        helper.assertTrue(kaart.zee(hier, new Vec3(1.5, 19, 1.5)) && kaart.zee(hier, new Vec3(-30, 19, 400)), "the open sea, also outside the map");
        // the closed ground: the roadblock, the pocket behind it up to the line, nothing walkable near or north of the line
        int wz = weg.getZ() - 1;
        for (int z = pl.grensZ(); z <= wz; z++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (kaart.vak(weg.getX() + dx, z) != Landkaart.DICHT) {
                    mis.add("the pocket behind the roadblock at " + (weg.getX() + dx) + ", " + z + " is closed ground");
                }
            }
        }
        // (the roadblock's fence stands in the middle of its block: a dog is 0.6 wide)
        Vec3 noordkant = new Vec3(weg.getX() + 0.5, 20, wz + 0.07), zuidkant = new Vec3(weg.getX() + 0.5, 20, wz + 0.93);
        helper.assertTrue(kaart.dicht(hier, noordkant) && Wegversperring.dicht(hier, pl, kaart, noordkant) && !kaart.dicht(hier, zuidkant)
                && !Wegversperring.dicht(hier, pl, kaart, zuidkant) && kaart.veilig(hier, Vec3.atBottomCenterOf(weg)),
                "a dog that leans against the back of the roadblock is behind it, one that leans against its front is not");
        int[] tel = new int[128];
        for (int z = 0; z < kaart.diep(); z++) {
            for (int x = 0; x < kaart.breed(); x++) {
                char c = kaart.vak(x, z);
                tel[c & 127]++;
                if (z < pl.grensZ() + 2 && c != Landkaart.DICHT && c != Landkaart.ZEE) {
                    mis.add("north of the line everything is closed ground or sea, not '" + c + "' at " + x + ", " + z);
                }
            }
        }
        helper.assertTrue(tel[Landkaart.LAND] > 8000 && tel[Landkaart.DEK] > 50 && tel[Landkaart.DICHT] > 1000 && tel[Landkaart.EILAND] > 500
                && tel[Landkaart.ZEE] > 8000 && tel[Landkaart.LAND] + tel[Landkaart.DEK] + tel[Landkaart.DICHT] + tel[Landkaart.EILAND] + tel[Landkaart.ZEE]
                == kaart.breed() * kaart.diep(), "the five kinds of column, and no other");
        helper.assertTrue(NlTekst.has("gui.guhs.snuffeldorp.zee"), "the friendly line of the sea");
        helper.assertTrue(mis.isEmpty(), "the land map: " + mis);
        helper.succeed();
    }

    /** Walkable land on this column or on one of its eight neighbours (island coordinates). */
    private static boolean naast(Landkaart kaart, double x, double z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                char c = kaart.vak((int) Math.floor(x) + dx, (int) Math.floor(z) + dz);
                if (c == Landkaart.LAND || c == Landkaart.DEK) {
                    return true;
                }
            }
        }
        return false;
    }
}
