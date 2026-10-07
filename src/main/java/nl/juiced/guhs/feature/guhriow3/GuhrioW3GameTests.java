package nl.juiced.guhs.feature.guhriow3;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioLevel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of bbq2 guhrio-w3 (batch "guhriow3", run with {@code -Pgt=GuhrioW3GameTests}): the server side of world 3.
 * <ul>
 *     <li>The duel is fought in the room guhriow3_test_duel: the very arena tools/features/guhrio_w3_banen.py builds for the
 *     castle (the lane runs east along z = 1; cell s of the lane is x = 1 + s, row r is helper y = r + 3). A mock player has
 *     no tick of its own, so the tests post the player tick themselves (the level and the story engine both hang on it) and
 *     put the player where a real one would walk.</li>
 *     <li>The pieces (Guhshi's hitching post, the Vuurpeper bush) and the coal are tried on a little lane in the engine's
 *     empty room guhrio_test_baan.</li>
 * </ul>
 * Also here: a player who still reads the narrator card keeps nobody waiting and is left alone, the end scene that is owed
 * to whoever could not watch it (a reader, somebody who logged out), and the bridge a stopped server left half broken.
 * What only a client shows (the box models, the end scene's picture) is not covered here: tools/autocheck/bbq2_guhrio-w3.txt.
 */
public class GuhrioW3GameTests {
    private static final String BATCH = "guhriow3", DUEL = "guhriow3_test_duel", KAMER = "guhrio_test_baan", LEVEL = "guhriow3_test";
    /** The start block of the duel room (cell 2, row 4) and the row you walk on. */
    private static final BlockPos START = new BlockPos(3, 7, 1);
    private static final int LOOP_Y = 7, Z = 3;

    static {
        GuhrioLevel.zet(new GuhrioLevel(LEVEL, "W3", List.of(new GuhrioLevel.BaanDef("test", List.of(new BlockPos(-1, 0, 0), new BlockPos(19, 0, 0)),
                true, 13, 6, -3, 8)), new BlockPos(18, 0, 0), new BlockPos(-1, 0, 0), null));
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** Puts a player on cell {@code s} of the duel lane, standing on the ledges / the bridge. */
    private static void opCel(GameTestHelper helper, ServerPlayer p, double s) {
        Vec3 v = helper.absoluteVec(new Vec3(1 + s + 0.5, LOOP_Y, 1.5));
        p.snapTo(v.x, v.y, v.z);
        p.setOnGround(true);
    }

    private static BlockPos cel(GameTestHelper helper, int s, int rij) {
        return helper.absolutePos(new BlockPos(1 + s, rij + 3, 1));
    }

    private static void tik(ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            if (!p.isRemoved()) {
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
            }
        }
    }

    private static GroteNetherMikaEntity baas(GameTestHelper helper, BlockPos startAbs) {
        GuhrioSpel.Actief actief = GuhrioSpel.actief(helper.getLevel(), startAbs);
        if (actief == null) {
            return null;
        }
        for (Entity e : GuhrioSpel.wezens(actief)) {
            if (e instanceof GroteNetherMikaEntity m) {
                return m;
            }
        }
        return null;
    }

    private static boolean brugHeel(GameTestHelper helper, DuelPlan plan, int van, int tot) {
        for (int x = van; x <= tot; x++) {
            if (!helper.getLevel().getBlockState(cel(helper, x + 2, 3)).is(GuhrioW3Feature.BRUG.get())) {
                return false;
            }
        }
        return true;
    }

    private static boolean brugWeg(GameTestHelper helper, int van, int tot) {
        for (int x = van; x <= tot; x++) {
            if (!helper.getLevel().getBlockState(cel(helper, x + 2, 3)).isAir()) {
                return false;
            }
        }
        return true;
    }

    // =====================================================================================================================
    // what the generators wrote
    // =====================================================================================================================

    /** The level files of world 3, the duel's plan, the end scene, the narrator card and the advancements are there. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void guhriow3BestandenKloppen(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (String id : new String[]{GuhrioW3Feature.LEVEL_3_1, GuhrioW3Feature.LEVEL_3_2}) {
            GuhrioLevel level = GuhrioLevel.bestand(server, id);
            helper.assertTrue(level != null && level.banen().size() == 2, id + ": the main lane and its bonus room");
            helper.assertTrue(level.banen().get(0).boven() - level.banen().get(0).onder() == 12, id + ": the main lane is rows 0..12");
            helper.assertTrue(level.na() != null, id + ": comes after another level");
        }
        GuhrioLevel duel = GuhrioLevel.bestand(server, GuhrioKasteel.DUEL);
        helper.assertTrue(duel != null && duel.banen().size() == 1 && duel.banen().get(0).hoogte() == 7 && GuhrioW3Feature.LEVEL_3_2.equals(duel.na()),
                "the duel: one lane, a wider picture, after 3-2");
        helper.assertTrue(DuelPlan.van(server).equals(DuelPlan.STANDAARD), "the duel's plan is read, and is the arena the boss falls back on: " + DuelPlan.van(server));
        DuelPlan plan = DuelPlan.STANDAARD;
        helper.assertTrue(plan.rolMin() < 0 && plan.rolMax() < plan.breuk() && plan.loopMin() > plan.brugVan() && plan.loopMax() < plan.brugTot()
                && plan.mokPlek() > plan.brugTot() + 1 && plan.mokPlek() + 1 < plan.hendel().getX(), "the plan's sums fit the arena");
        Cutscene einde = Cutscene.van("guhriow3_einde");
        helper.assertTrue(einde == GuhrioW3Feature.EINDE && einde.heeftSpeler() && "guhrio".equals(einde.lijn()) && GuhrioW3Feature.KAART_DUEL.equals(einde.kaart())
                && einde.acteur("mika") != null && einde.acteur("perzik") != null && einde.acteur("taart") != null, "the end scene and its cast");
        helper.assertTrue(Verteller.van(GuhrioW3Feature.KAART_DUEL) != null && Verteller.van(GuhrioW3Feature.KAART_DUEL).regels() == 4, "the narrator card of the duel");
        for (String naam : new String[]{"quest/guhrio_w3_hendel", "quest/guhrio_w3_schild", "quest/guhrio_w3_taart", "quest/guhrio_w3_vadsmunten",
                "guhrio/guhrio_w3_taart"}) {
            helper.assertTrue(server.getAdvancements().get(Guhs.id(naam)) != null, "advancement " + naam);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // the duel
    // =====================================================================================================================

    /**
     * The whole duel, as a player goes through it: the narrator card, his roar, the lever (the far half of the bridge drops),
     * the pipe back, three rolls against the wall, the ?-block that fills up, three knabbels that bounce the shell back, the
     * splash, the sulk, the bridge that comes back, the end scene, and the player has won and stands outside. Nobody is hurt.
     */
    @GuhTest(template = DUEL, batch = BATCH, timeoutTicks = 3000)
    public static void guhriow3DuelVanBeginTotEind(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos startAbs = helper.absolutePos(START);
        helper.assertTrue(level.getBlockState(startAbs).getBlock() instanceof GuhrioBlocks.StartBlok, "the arena's start block: " + level.getBlockState(startAbs));
        ServerPlayer p = speler(helper);
        float leven = p.getHealth();
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "the arena lets the player in");
        GroteNetherMikaEntity mika = baas(helper, startAbs);
        helper.assertTrue(mika != null, "the Grote Nether-Mika is on his spot");
        DuelPlan plan = DuelPlan.van(level.getServer());
        BlockPos vraag = cel(helper, plan.vraag().getX() + 2, plan.vraag().getY() + 4);
        BlockPos hendel = cel(helper, plan.hendel().getX() + 2, 4);
        helper.assertTrue(level.getBlockState(vraag).getBlock() instanceof GuhrioBlocks.VraagBlok && level.getBlockState(hendel).getBlock() instanceof GuhrioW3Blocks.HendelBlok,
                "the ?-block and the lever are where the plan says");
        int[] fase = {0}, teller = {0};
        helper.onEachTick(() -> {
            tik(p);
            GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
            teller[0]++;
            switch (fase[0]) {
                case 0 -> {
                    // the card (a mock reads it in two ticks), then the roar; the ?-block is empty until round 3
                    if (mika.stand() == GroteNetherMikaEntity.LOOPT) {
                        helper.assertTrue(Verteller.gezien(p, GuhrioW3Feature.KAART_DUEL), "the narrator card was shown first");
                        helper.assertTrue(mika.ronde() == 1 && brugHeel(helper, plan, plan.brugVan(), plan.brugTot()), "round 1 on a whole bridge");
                        helper.assertTrue(s.staat(vraag) == 1, "the ?-block is empty for now");
                        helper.assertTrue(mika.gevaarlijk() && !mika.stampbaar(), "he shoves and nobody lands on him");
                        opCel(helper, p, plan.hendel().getX() + 2);
                        fase[0] = 1;
                    }
                }
                case 1 -> {
                    // standing in the lever: the far half of the bridge drops, he hops to what is left
                    if (mika.ronde() == 2 && brugWeg(helper, plan.breuk(), plan.brugTot()) && mika.stand() == GroteNetherMikaEntity.WACHT) {
                        helper.assertTrue(level.getBlockState(hendel).getValue(GuhrioW3Blocks.HendelBlok.GETROKKEN), "the lever is pulled");
                        helper.assertTrue(brugHeel(helper, plan, plan.brugVan(), plan.breuk() - 1), "the near half stands");
                        helper.assertTrue(mika.plek() <= plan.rolMax() + 0.01 && mika.hoogte() == 0, "he stands on the half that is left: " + mika.plek());
                        teller[0] = 0;
                        fase[0] = 2;
                    }
                }
                case 2 -> {
                    // he waits as long as everybody is on the far ledge; back on his half (the pipe), he rolls
                    if (teller[0] == 60) {
                        helper.assertTrue(mika.stand() == GroteNetherMikaEntity.WACHT, "he waits for somebody to come back");
                        opCel(helper, p, 4);
                    }
                    if (mika.ronde() == 3) {
                        helper.assertTrue(mika.bonken() >= GroteNetherMikaEntity.BONKEN, "three bonks against the wall first");
                        fase[0] = 3;
                    }
                }
                case 3 -> {
                    // round 3: the ?-block is full again for whoever has no Vuurpeper; knabbels bounce the rolling shell back
                    if (s.kracht != GuhrioSpel.Kracht.VUUR) {
                        if (s.staat(vraag) == 0) {
                            GuhrioSpel.actie(p, nl.juiced.guhs.feature.guhrio.GuhrioPayloads.Actie.BOTS, vraag, 0);
                        }
                    } else if (mika.stand() == GroteNetherMikaEntity.ROLT && mika.kijk() < 0 && mika.plek() < 9 && mika.plek() > 5) {
                        GuhrioSpel.gooi(p, s, 1);
                    }
                    if (mika.stand() >= GroteNetherMikaEntity.VALT) {
                        helper.assertTrue(mika.treffers() == GroteNetherMikaEntity.TREFFERS, "three hits: " + mika.treffers());
                        fase[0] = 4;
                    }
                }
                case 4 -> {
                    // the splash, the climb, the sulk, the scene (two ticks for a mock): won, and out of the level
                    if (s == null && GuhrioKasteel.duelGewonnen(p)) {
                        helper.assertTrue(GuhrioKasteel.LIJN.klaar(p), "the questline is done");
                        helper.assertTrue(p.getHealth() == leven, "nobody was hurt");
                        teller[0] = 0;
                        fase[0] = 5;
                    }
                }
                default -> {
                    // he mends his bridge before he goes
                    if (teller[0] > 30 && brugHeel(helper, plan, plan.brugVan(), plan.brugTot())
                            && !level.getBlockState(hendel).getValue(GuhrioW3Blocks.HendelBlok.GETROKKEN)) {
                        fase[0] = 6;
                    }
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(fase[0] == 6, "the duel is at step " + fase[0] + " (stand " + mika.stand() + ", round " + mika.ronde() + ", at " + mika.plek() + ")");
            weg(helper, p);
        });
    }

    /**
     * Everybody wins who is in the arena, and the next visitor finds a new fight: a second player who walks in while the
     * loser still sulks gets round 1 on a whole bridge, and wins too.
     */
    @GuhTest(template = DUEL, batch = BATCH, timeoutTicks = 2000)
    public static void guhriow3IedereenWintEnHijKomtTerug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos startAbs = helper.absolutePos(START);
        ServerPlayer a = speler(helper), b = speler(helper), c = speler(helper);
        helper.assertTrue(GuhrioSpel.start(a, startAbs) && GuhrioSpel.start(b, startAbs), "two players in the arena");
        GroteNetherMikaEntity mika = baas(helper, startAbs);
        DuelPlan plan = DuelPlan.van(level.getServer());
        int[] fase = {0}, teller = {0};
        helper.onEachTick(() -> {
            tik(a, b, c);
            teller[0]++;
            switch (fase[0]) {
                case 0 -> {
                    if (mika.stand() == GroteNetherMikaEntity.LOOPT) {
                        mika.devRonde(level, 4);                // (straight to the last knabbel)
                        fase[0] = 1;
                    }
                }
                case 1 -> {
                    if (GuhrioKasteel.duelGewonnen(a) && GuhrioKasteel.duelGewonnen(b) && GuhrioSpel.sessie(a) == null && GuhrioSpel.sessie(b) == null) {
                        helper.assertTrue(mika.isAlive() && mika.stand() == GroteNetherMikaEntity.MOKT, "he sits and sulks");
                        helper.assertFalse(mika.gevaarlijk(), "a sulking Mika shoves nobody");
                        helper.assertFalse(GuhrioKasteel.duelGewonnen(c), "the third player was not there");
                        helper.assertTrue(GuhrioSpel.start(c, startAbs), "the next visitor walks in");
                        teller[0] = 0;
                        fase[0] = 2;
                    }
                }
                case 2 -> {
                    if (mika.ronde() == 1 && mika.stand() == GroteNetherMikaEntity.LOOPT) {
                        helper.assertTrue(brugHeel(helper, plan, plan.brugVan(), plan.brugTot()), "a new fight on a whole bridge");
                        mika.devRonde(level, 4);
                        fase[0] = 3;
                    }
                }
                default -> {
                    if (GuhrioKasteel.duelGewonnen(c) && GuhrioSpel.sessie(c) == null) {
                        fase[0] = 4;
                    }
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(fase[0] == 4, "step " + fase[0] + " (stand " + mika.stand() + ", round " + mika.ronde() + ")");
            weg(helper, a, b, c);
        });
    }

    /** His landing shoves whoever stands near him on the ground (a push, never damage); somebody in the air is left alone. */
    @GuhTest(template = DUEL, batch = BATCH, timeoutTicks = 2000)
    public static void guhriow3LandingDuwt(GameTestHelper helper) {
        BlockPos startAbs = helper.absolutePos(START);
        ServerPlayer grond = speler(helper), lucht = speler(helper);
        helper.assertTrue(GuhrioSpel.start(grond, startAbs) && GuhrioSpel.start(lucht, startAbs), "two players");
        GroteNetherMikaEntity mika = baas(helper, startAbs);
        float leven = grond.getHealth();
        boolean[] geland = {false};
        helper.onEachTick(() -> {
            tik(grond, lucht);
            if (geland[0]) {
                return;
            }
            // both three blocks to his left; one of them "in the air"
            if (mika.stand() == GroteNetherMikaEntity.SPRINGT || mika.stand() == GroteNetherMikaEntity.HURKT) {
                double s = mika.plek() + 2 - 3.2;
                opCel(helper, grond, s);
                opCel(helper, lucht, s);
                lucht.setOnGround(false);
                grond.setDeltaMovement(Vec3.ZERO);
                lucht.setDeltaMovement(Vec3.ZERO);
            } else if (mika.stand() == GroteNetherMikaEntity.LANDT) {
                geland[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(geland[0], "he has not landed yet (stand " + mika.stand() + ")");
            helper.assertTrue(grond.getDeltaMovement().x < -0.5 && grond.getDeltaMovement().y > 0, "shoved away from him, with a little hop: " + grond.getDeltaMovement());
            helper.assertTrue(lucht.getDeltaMovement().lengthSqr() < 1e-6, "whoever jumped is left alone: " + lucht.getDeltaMovement());
            helper.assertTrue(grond.getHealth() == leven && GuhrioSpel.sessie(grond) != null, "a shove costs nothing");
            weg(helper, grond, lucht);
        });
    }

    /**
     * Two players walk into the arena for the first time; one closes the narrator card, the other keeps reading. The fight
     * does not wait for the reader: it starts for the one who is ready. The reader is nobody's target and nothing shoves or
     * touches him (the landing, a coal, the Mika himself); when the Mika falls, both have won - the reader gets the end
     * scene the moment he closes the card, and only then counts as the winner.
     */
    @GuhTest(template = DUEL, batch = BATCH, timeoutTicks = 3000)
    public static void guhriow3LezerHoudtNiemandOp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos startAbs = helper.absolutePos(START);
        ServerPlayer klaar = speler(helper), lezer = speler(helper);
        helper.assertTrue(GuhrioSpel.start(klaar, startAbs) && GuhrioSpel.start(lezer, startAbs), "two players in the arena");
        GroteNetherMikaEntity mika = baas(helper, startAbs);
        DuelPlan plan = DuelPlan.van(level.getServer());
        int[] fase = {0}, teller = {0};
        Vec3[] stond = {null};
        helper.onEachTick(() -> {
            // (only the ready player's game ticks: the reader's card stays open until fase 5)
            tik(klaar);
            if (fase[0] >= 5) {
                tik(lezer);
            }
            teller[0]++;
            GuhrioSpel.Sessie sl = GuhrioSpel.sessie(lezer);
            switch (fase[0]) {
                case 0 -> {
                    if (mika.stand() == GroteNetherMikaEntity.LOOPT) {
                        helper.assertTrue(Verteller.gezien(klaar, GuhrioW3Feature.KAART_DUEL) && !Verteller.gezien(lezer, GuhrioW3Feature.KAART_DUEL)
                                && Cutscenes.bezig(lezer) && !Cutscenes.bezig(klaar), "one has read the card, the other is still reading");
                        helper.assertTrue(teller[0] < 400, "the fight began without waiting for the reader (tick " + teller[0] + ")");
                        // the ready one on the far ledge, the reader left of the Mika: he walks to the ready one
                        opCel(helper, klaar, plan.hendel().getX() + 2 - 2);
                        opCel(helper, lezer, 12);
                        stond[0] = lezer.position();
                        teller[0] = 0;
                        fase[0] = 1;
                    }
                }
                case 1 -> {
                    // he keeps facing (and walking to) the player who is in the fight, never the reader on his other side
                    if (mika.stand() == GroteNetherMikaEntity.LOOPT || mika.stand() == GroteNetherMikaEntity.GOOIT) {
                        helper.assertTrue(mika.kijk() > 0, "he looks at the player who plays, not at the reader");
                    }
                    if (mika.stand() == GroteNetherMikaEntity.HURKT || mika.stand() == GroteNetherMikaEntity.SPRINGT) {
                        // the reader right beside his landing, on the ground; the ready one out of reach
                        opCel(helper, lezer, mika.plek() + 2 - 2.5);
                        lezer.setDeltaMovement(Vec3.ZERO);
                        stond[0] = lezer.position();
                    } else if (mika.stand() == GroteNetherMikaEntity.LANDT) {
                        helper.assertTrue(lezer.getDeltaMovement().lengthSqr() < 1e-9 && lezer.position().distanceTo(stond[0]) < 1e-6,
                                "his landing does not shove whoever reads: " + lezer.getDeltaMovement());
                        // a coal and the Mika himself "touch" the reader (what the reader's game would report): nothing
                        KooltjeEntity kool = kool(helper, sl, 0, 1);
                        kool.snapTo(lezer.getX(), lezer.getY() + 0.3, lezer.getZ(), 0f, 0f);
                        GuhrioSpel.actie(lezer, nl.juiced.guhs.feature.guhrio.GuhrioPayloads.Actie.GERAAKT, lezer.blockPosition(), kool.getId());
                        GuhrioSpel.actie(lezer, nl.juiced.guhs.feature.guhrio.GuhrioPayloads.Actie.GERAAKT, lezer.blockPosition(), mika.getId());
                        GuhrioSpel.geraakt(lezer, sl);
                        helper.assertTrue(lezer.position().distanceTo(stond[0]) < 1e-6 && GuhrioSpel.sessie(lezer) == sl,
                                "a coal and the Mika do not send a reader back to his flag");
                        // (the same coal does send back somebody who plays: the check above is no empty one)
                        GuhrioSpel.Sessie sk = GuhrioSpel.sessie(klaar);
                        Vec3 was = klaar.position();
                        kool.snapTo(was.x, was.y + 0.3, was.z, 0f, 0f);
                        GuhrioSpel.actie(klaar, nl.juiced.guhs.feature.guhrio.GuhrioPayloads.Actie.GERAAKT, klaar.blockPosition(), kool.getId());
                        helper.assertTrue(klaar.position().distanceTo(was) > 3 && GuhrioSpel.sessie(klaar) == sk, "whoever plays is put back at the flag by a coal");
                        kool.discard();
                        mika.devRonde(level, 4);              // (straight to his fall)
                        fase[0] = 2;
                    }
                }
                case 2 -> {
                    // he fell: the ready one watches the end scene and has won; the reader is owed it
                    if (GuhrioKasteel.duelGewonnen(klaar) && GuhrioSpel.sessie(klaar) == null) {
                        helper.assertTrue(GuhrioW3Feature.tegoed(klaar) == null, "nothing is owed to who watched the scene");
                        helper.assertTrue(!GuhrioKasteel.duelGewonnen(lezer) && GuhrioW3Feature.tegoed(lezer) != null && GuhrioSpel.sessie(lezer) == sl
                                && Cutscenes.bezig(lezer) && !Cutscenes.gezien(lezer, "guhriow3_einde"), "the reader has not won yet: the end scene is owed to him");
                        helper.assertTrue(mika.stand() == GroteNetherMikaEntity.MOKT, "the Mika sulks");
                        teller[0] = 0;
                        fase[0] = 3;
                    }
                }
                case 3 -> {
                    // as long as he reads nothing changes: no new fight starts for him, he is not thrown out
                    if (teller[0] > 150) {
                        helper.assertTrue(mika.stand() == GroteNetherMikaEntity.MOKT && GuhrioSpel.sessie(lezer) == sl && !GuhrioKasteel.duelGewonnen(lezer),
                                "the Mika waits for the reader");
                        fase[0] = 5;                            // the reader closes the card (his game ticks from now on)
                    }
                }
                default -> {
                    if (GuhrioKasteel.duelGewonnen(lezer) && GuhrioSpel.sessie(lezer) == null) {
                        helper.assertTrue(Verteller.gezien(lezer, GuhrioW3Feature.KAART_DUEL) && Cutscenes.gezien(lezer, "guhriow3_einde")
                                && GuhrioW3Feature.tegoed(lezer) == null, "the card was read, the end scene watched, nothing is owed any more");
                        fase[0] = 6;
                    }
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(fase[0] == 6, "step " + fase[0] + " (stand " + mika.stand() + ", round " + mika.ronde() + ")");
            weg(helper, klaar, lezer);
        });
    }

    /**
     * A player logs out during the end scene (the story engine drops a scene's ending then). The win is still owed: at the
     * next login the scene plays again from the level hall, and at its end the duel is won and the player stands in the
     * tower room - nobody fights the Grote Nether-Mika twice for one win.
     */
    @GuhTest(template = DUEL, batch = BATCH, timeoutTicks = 3000)
    public static void guhriow3UitgelogdTijdensHetEinde(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos startAbs = helper.absolutePos(START);
        ServerPlayer p = speler(helper);
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "in the arena");
        GroteNetherMikaEntity mika = baas(helper, startAbs);
        BlockPos uit = GuhrioSpel.sessie(p).level().uitgang();
        int[] fase = {0}, teller = {0};
        helper.onEachTick(() -> {
            teller[0]++;
            switch (fase[0]) {
                case 0 -> {
                    tik(p);
                    if (mika.stand() == GroteNetherMikaEntity.LOOPT) {
                        mika.devRonde(level, 4);
                        fase[0] = 1;
                    }
                }
                case 1 -> {
                    // (no tick of the player's game once the scene plays: a mock would have watched it in two)
                    if (GuhrioW3Feature.tegoed(p) != null && Cutscenes.bezig(p)) {
                        helper.assertTrue(!GuhrioKasteel.duelGewonnen(p) && GuhrioSpel.sessie(p) != null, "the scene plays: not won yet");
                        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
                        helper.assertTrue(GuhrioSpel.sessie(p) == null && !Cutscenes.bezig(p) && !GuhrioKasteel.duelGewonnen(p),
                                "logged out during the scene: out of the level, the duel not won");
                        helper.assertTrue(GuhrioW3Feature.tegoed(p) != null, "but the ending is still owed (saved with the player)");
                        helper.assertTrue(!Cutscenes.gezien(p, "guhriow3_einde"), "the scene does not count as watched");
                        teller[0] = 0;
                        fase[0] = 2;
                    } else {
                        tik(p);
                    }
                }
                case 2 -> {
                    // "the next login": the player stands at the level's entrance (where a logout puts you); twice a second
                    // the owed scene is offered, and now it can play
                    if (Cutscenes.bezig(p)) {
                        helper.assertTrue(GuhrioSpel.sessie(p) == null, "the scene plays again, outside the level");
                        fase[0] = 3;
                    }
                    helper.assertTrue(teller[0] < 200, "the owed scene was not offered again");
                }
                default -> {
                    tik(p);
                    if (GuhrioKasteel.duelGewonnen(p)) {
                        helper.assertTrue(GuhrioW3Feature.tegoed(p) == null && Cutscenes.gezien(p, "guhriow3_einde") && GuhrioKasteel.LIJN.klaar(p),
                                "won at the end of the scene: nothing owed, the questline done");
                        helper.assertTrue(uit != null && p.blockPosition().equals(uit), "and put where the duel lets you out: " + p.blockPosition() + " / " + uit);
                        fase[0] = 4;
                    }
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(fase[0] == 4, "step " + fase[0] + " (stand " + mika.stand() + ")");
            weg(helper, p);
        });
    }

    /**
     * The bridge columns and the lever are real blocks for the length of a fight. A server that stopped in the middle of one
     * leaves half a bridge and a pulled lever in the world: the next Grote Nether-Mika mends both before his first roar.
     */
    @GuhTest(template = DUEL, batch = BATCH, timeoutTicks = 2000)
    public static void guhriow3BrugHeelNaEenStop(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos startAbs = helper.absolutePos(START);
        DuelPlan plan = DuelPlan.van(level.getServer());
        BlockPos hendel = cel(helper, plan.hendel().getX() + 2, 4);
        // what a fight that never ended leaves behind
        for (int x = plan.breuk(); x <= plan.brugTot(); x++) {
            level.setBlock(cel(helper, x + 2, 3), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(hendel, level.getBlockState(hendel).setValue(GuhrioW3Blocks.HendelBlok.GETROKKEN, true), 3);
        helper.assertTrue(brugWeg(helper, plan.breuk(), plan.brugTot()) && !brugHeel(helper, plan, plan.brugVan(), plan.brugTot()), "half a bridge");
        ServerPlayer p = speler(helper);
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "the next visitor walks in");
        GroteNetherMikaEntity mika = baas(helper, startAbs);
        helper.assertTrue(mika != null, "a new Grote Nether-Mika");
        helper.onEachTick(() -> tik(p));
        helper.succeedWhen(() -> {
            helper.assertTrue(mika.stand() == GroteNetherMikaEntity.LOOPT, "he has not begun yet (stand " + mika.stand() + ")");
            helper.assertTrue(brugHeel(helper, plan, plan.brugVan(), plan.brugTot()), "the whole bridge is back");
            helper.assertFalse(level.getBlockState(hendel).getValue(GuhrioW3Blocks.HendelBlok.GETROKKEN), "the lever stands up again");
            helper.assertTrue(mika.ronde() == 1, "round 1");
            weg(helper, p);
        });
    }

    // =====================================================================================================================
    // the pieces and the coal
    // =====================================================================================================================

    private static BlockPos baan(GameTestHelper helper) {
        for (int x = 1; x <= 21; x++) {
            helper.setBlock(new BlockPos(x, 1, Z), GuhrioFeature.GROND.get());
        }
        BlockPos start = new BlockPos(2, 2, Z);
        helper.setBlock(start, GuhrioFeature.STARTBLOK.get().defaultBlockState().setValue(GuhrioBlocks.StartBlok.FACING, Direction.EAST));
        BlockPos abs = helper.absolutePos(start);
        ((GuhrioBlocks.StartBlockEntity) helper.getLevel().getBlockEntity(abs)).zetLevel(LEVEL);
        return abs;
    }

    private static void zet(GameTestHelper helper, ServerPlayer p, double x) {
        Vec3 v = helper.absoluteVec(new Vec3(x, 2, Z + 0.5));
        p.snapTo(v.x, v.y, v.z);
    }

    /** Guhshi stays at his hitching post (you go on on foot); the Vuurpeper bush gives the Vuurpeper as often as you like. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriow3PaalEnStruik(GameTestHelper helper) {
        BlockPos startAbs = baan(helper);
        helper.setBlock(new BlockPos(6, 2, Z), GuhrioW3Feature.PARKEERPAAL.get());
        helper.setBlock(new BlockPos(9, 2, Z), GuhrioW3Feature.PEPERSTRUIK.get());
        ServerPlayer p = speler(helper), q = speler(helper);
        helper.assertTrue(GuhrioSpel.start(p, startAbs) && GuhrioSpel.start(q, startAbs), "in the level");
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        helper.assertTrue(s.actief.stukken.stream().anyMatch(x -> x.state().getBlock() instanceof GuhrioW3Blocks.ParkeerPaal)
                && s.actief.stukken.stream().anyMatch(x -> x.state().getBlock() instanceof GuhrioW3Blocks.Peperstruik), "the engine found both pieces in the lane");
        GuhrioKasteel.geefEi(p);
        GuhrioSpel.zetGuhshi(p, s, true);
        helper.assertTrue(s.guhshi, "on Guhshi");
        // the bush gives the Vuurpeper also on Guhshi's back; the post takes Guhshi
        zet(helper, p, 9.5);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.kracht == GuhrioSpel.Kracht.VUUR, "the Vuurpeper from the bush");
        zet(helper, p, 6.5);
        GuhrioSpel.tick(p);
        helper.assertFalse(s.guhshi, "Guhshi waits at the post");
        helper.assertTrue(s.kracht == GuhrioSpel.Kracht.VUUR && GuhrioSpel.gooi(p, s, 1), "on foot you throw knabbels");
        // losing the Vuurpeper is no dead end: the bush gives it again
        GuhrioSpel.zetKracht(p, s, GuhrioSpel.Kracht.GEEN);
        zet(helper, p, 9.5);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.kracht == GuhrioSpel.Kracht.VUUR, "and again");
        helper.assertTrue(GuhrioSpel.sessie(q).kracht == GuhrioSpel.Kracht.GEEN, "everybody their own");
        weg(helper, p, q);
        helper.succeed();
    }

    /** A coal floats slowly along the lane; a touch sends you back to your flag (unhurt); a knabbel puts it out; a wall too. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 2000)
    public static void guhriow3Kooltje(GameTestHelper helper) {
        BlockPos startAbs = baan(helper);
        helper.setBlock(new BlockPos(16, 2, Z), GuhrioFeature.BLOK.get());
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        float leven = p.getHealth();
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "in the level");
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        KooltjeEntity een = kool(helper, s, 8.5, 1), twee = kool(helper, s, 8.5, 1);
        helper.assertTrue(een.gevaarlijk() && !een.stampbaar(), "a coal shoves; nobody lands on it");
        // it touches the player (the player's own game reports that): back to the flag, nothing lost
        zet(helper, p, 8.5);
        GuhrioSpel.actie(p, nl.juiced.guhs.feature.guhrio.GuhrioPayloads.Actie.GERAAKT, p.blockPosition(), een.getId());
        helper.assertTrue(Math.abs(p.getX() - (startAbs.getX() + 0.5)) < 0.01 && p.getHealth() == leven && GuhrioSpel.sessie(p) == s, "back at the start, unhurt");
        // a knabbel puts one out
        helper.assertTrue(een.knabbel(p, s) && een.isRemoved(), "out");
        double beginX = twee.getX();
        helper.succeedWhen(() -> {
            // the other one floated on (slowly: about two blocks a second) until the block at 16 stopped it
            helper.assertTrue(twee.isRemoved(), "the coal still floats: " + (twee.getX() - beginX) + " blocks in " + twee.tickCount + " ticks");
            helper.assertTrue(twee.tickCount > 40, "it is slow: " + twee.tickCount + " ticks for 7 blocks");
            helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(16, 2, Z))).is(GuhrioFeature.BLOK.get()), "the wall is still there");
            weg(helper, p);
        });
    }

    private static KooltjeEntity kool(GameTestHelper helper, GuhrioSpel.Sessie s, double x, int teken) {
        ServerLevel level = helper.getLevel();
        KooltjeEntity kool = GuhrioW3Feature.KOOLTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        Vec3 v = helper.absoluteVec(new Vec3(x, 2.9, Z + 0.5));
        kool.snapTo(v.x, v.y, v.z, 0f, 0f);
        kool.zetBaan(s.actief, s.lane(), null);
        kool.gooi(teken, helper.absoluteVec(new Vec3(0, 2 + KooltjeEntity.VLIEGHOOGTE, 0)).y);
        level.addFreshEntity(kool);
        s.actief.los.add(kool);
        return kool;
    }

    /**
     * The six big vadsmunten of the burcht are counted at a flagpole of world 3 (the hidden advancement of its FTB quest):
     * not with five, and with all six only once a flagpole of world 3 is reached. And Pad-guh's pointer to the duel's gate
     * is on its way after the flagpole of 3-2 (not of 3-1), as long as the duel is not won.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriow3VadsmuntenVanDeBurcht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (String id : new String[]{GuhrioW3Feature.LEVEL_3_1, GuhrioW3Feature.LEVEL_3_2}) {
            GuhrioLevel.zet(new GuhrioLevel(id, id, List.of(new GuhrioLevel.BaanDef("test", List.of(new BlockPos(-1, 0, 0), new BlockPos(15, 0, 0)),
                    true, 13, 6, -3, 8)), new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), null));
        }
        for (int x = 1; x <= 21; x++) {
            helper.setBlock(new BlockPos(x, 1, Z), GuhrioFeature.GROND.get());
        }
        BlockPos[] starts = new BlockPos[2];
        for (int i = 0; i < 2; i++) {
            BlockPos start = new BlockPos(2 + i * 3, 2, Z);
            helper.setBlock(start, GuhrioFeature.STARTBLOK.get().defaultBlockState().setValue(GuhrioBlocks.StartBlok.FACING, Direction.EAST));
            starts[i] = helper.absolutePos(start);
            ((GuhrioBlocks.StartBlockEntity) level.getBlockEntity(starts[i])).zetLevel(i == 0 ? GuhrioW3Feature.LEVEL_3_1 : GuhrioW3Feature.LEVEL_3_2);
        }
        BlockPos mast = new BlockPos(12, 2, Z);
        helper.setBlock(mast, GuhrioFeature.MAST.get());
        var adv = level.getServer().getAdvancements().get(Guhs.id("quest/guhrio_w3_vadsmunten"));
        ServerPlayer p = speler(helper);
        // 3-1: all three, and its flagpole
        helper.assertTrue(GuhrioSpel.start(p, starts[0]), "into 3-1");
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        for (int n = 0; n < 3; n++) {
            GuhrioSpel.vadsmunt(p, s, starts[0].offset(3 + n, 3, 0), n);
        }
        zet(helper, p, 12.5);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.klaar() && GuhrioKasteel.vadsmunten(p, GuhrioW3Feature.LEVEL_3_1) == 7, "3-1 done with its three vadsmunten");
        helper.assertFalse(p.getAdvancements().getOrStartProgress(adv).isDone(), "three of the six is not all of them");
        helper.assertFalse(GuhrioW3Feature.padguhStraks(p), "Pad-guh says nothing after 3-1");
        GuhrioSpel.stop(p, GuhrioSpel.Einde.KLAAR);
        // 3-2: two of them and the flagpole: not yet; the third and the flagpole again: now
        helper.assertTrue(GuhrioSpel.start(p, starts[1]), "into 3-2");
        s = GuhrioSpel.sessie(p);
        GuhrioSpel.vadsmunt(p, s, starts[1].offset(3, 3, 0), 0);
        GuhrioSpel.vadsmunt(p, s, starts[1].offset(4, 3, 0), 2);
        zet(helper, p, 12.5);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.klaar() && GuhrioKasteel.gehaald(p, GuhrioW3Feature.LEVEL_3_2), "3-2 done");
        helper.assertFalse(p.getAdvancements().getOrStartProgress(adv).isDone(), "five of the six is not all of them");
        helper.assertTrue(GuhrioW3Feature.padguhStraks(p), "after 3-2 Pad-guh will point at the duel's gate");
        GuhrioSpel.stop(p, GuhrioSpel.Einde.KLAAR);
        helper.assertTrue(GuhrioSpel.start(p, starts[1]), "3-2 again");
        s = GuhrioSpel.sessie(p);
        GuhrioSpel.vadsmunt(p, s, starts[1].offset(5, 3, 0), 1);
        zet(helper, p, 12.5);
        GuhrioSpel.tick(p);
        helper.assertTrue(p.getAdvancements().getOrStartProgress(adv).isDone(), "all six, at a flagpole of the burcht");
        // whoever has won the duel needs no pointer any more
        ServerPlayer q = speler(helper);
        GuhrioKasteel.winDuel(q);
        helper.assertTrue(GuhrioSpel.start(q, starts[1]), "the winner in 3-2");
        zet(helper, q, 12.5);
        GuhrioSpel.tick(q);
        helper.assertTrue(GuhrioSpel.sessie(q).klaar() && !GuhrioW3Feature.padguhStraks(q), "no pointer for who has won the duel");
        weg(helper, p, q);
        GuhrioLevel.vergeet(GuhrioW3Feature.LEVEL_3_1);
        GuhrioLevel.vergeet(GuhrioW3Feature.LEVEL_3_2);
        helper.succeed();
    }
}
