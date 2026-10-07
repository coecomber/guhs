package nl.juiced.guhs.feature.guhriow1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guhrio.GuhmbaEntity;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioLevel;
import nl.juiced.guhs.feature.guhrio.GuhrioPayloads;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhrio.GuhrioStukken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of Super Guhrio's world 1 (batch "guhriow1"; run with {@code -Pgt=GuhrioW1GameTests}). The server side only
 * (mock players, whose game the tests play by calling {@link GuhrioSpel#tick} and {@link GuhrioSpel#actie} themselves):
 * <ul>
 *     <li>the castle's tiles hold this world's two levels (three lanes each, the tips, the secret rooms, the pipes and
 *     doors, all inside their lanes);</li>
 *     <li>level 1-1 as the generator wrote it, put in the world and walked: the tip, the secret pipe to the mole's den, the
 *     way back, the big pipe up, the flag, the three big vadsmunten, the flagpole;</li>
 *     <li>level 1-2 the same way: the hidden block, the door on the arch to the greenhouse and back, the tower door, the
 *     flagpole, and Pad-guh's thanks (every time, per player; the advancements the first time);</li>
 *     <li>the two invisible pieces in a little lane of their own: a tip once per run and never after the level was
 *     finished, a secret per player.</li>
 * </ul>
 * The two level templates (guhriow1_test_1_1 / _1_2, 98 long) are put in the air above the test with their chunks forced
 * and taken away again; a cell (s, row) of a level is at corner + (1 + s, 2 + row, 1).
 */
public class GuhrioW1GameTests {
    private static final String BATCH = "guhriow1", KAMER = "guhrio_test_baan", LEVEL = "guhriow1_gametest";

    static {
        GuhrioLevel.zet(new GuhrioLevel(LEVEL, "W-1", List.of(new GuhrioLevel.BaanDef("test", List.of(new BlockPos(-1, 0, 0), new BlockPos(19, 0, 0)),
                true, 11, 5, -3, 8)), new BlockPos(18, 0, 0), new BlockPos(-1, 0, 0), null));
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            GuhrioSpel.stop(p, GuhrioSpel.Einde.WEG);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean heeft(ServerPlayer p, String advancement) {
        return GidsFeature.heeft(p, advancement);
    }

    // --- a whole level in the air above the test ---------------------------------------------------------------------------

    private static BlockPos plaats(GameTestHelper helper, String naam, int hoogte) {
        ServerLevel level = helper.getLevel();
        StructureTemplate template = level.getStructureManager().get(Guhs.id(naam)).orElse(null);
        helper.assertTrue(template != null, "the template guhs:" + naam);
        BlockPos hoek = helper.absolutePos(new BlockPos(0, hoogte, 0));
        forceer(level, hoek, template, true);
        template.placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), 2);
        return hoek;
    }

    private static void forceer(ServerLevel level, BlockPos hoek, StructureTemplate template, boolean aan) {
        for (int cx = hoek.getX() >> 4; cx <= (hoek.getX() + template.getSize().getX()) >> 4; cx++) {
            for (int cz = hoek.getZ() >> 4; cz <= (hoek.getZ() + template.getSize().getZ()) >> 4; cz++) {
                level.setChunkForced(cx, cz, aan);
            }
        }
    }

    private static void ruim(GameTestHelper helper, String naam, BlockPos hoek) {
        ServerLevel level = helper.getLevel();
        StructureTemplate template = level.getStructureManager().get(Guhs.id(naam)).orElseThrow();
        BlockPos eind = hoek.offset(template.getSize().getX() - 1, template.getSize().getY() - 1, template.getSize().getZ() - 1);
        for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(hoek.getX() - 2, hoek.getY() - 2, hoek.getZ() - 2, eind.getX() + 3, eind.getY() + 3, eind.getZ() + 3),
                e -> !(e instanceof Player))) {
            e.discard();
        }
        for (BlockPos pos : BlockPos.betweenClosed(hoek, eind)) {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
        forceer(level, hoek, template, false);
    }

    /** The block of cell (s, row) of a level whose template stands at {@code hoek}. */
    private static BlockPos cel(BlockPos hoek, int s, int rij) {
        return hoek.offset(1 + s, 2 + rij, 1);
    }

    /** Puts the player in the middle of cell (s, row), feet on the bottom of the row. */
    private static void zet(ServerPlayer p, BlockPos hoek, int s, int rij) {
        BlockPos c = cel(hoek, s, rij);
        p.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
    }

    private static void door(ServerPlayer p, int ticks) {
        for (int i = 0; i < ticks; i++) {
            GuhrioSpel.tick(p);
        }
    }

    private static int baan(GuhrioSpel.Sessie s, String naam) {
        for (int i = 0; i < s.level().banen().size(); i++) {
            if (s.level().banen().get(i).id.equals(naam)) {
                return i;
            }
        }
        return -1;
    }

    private static long tel(GuhrioSpel.Sessie s, java.util.function.Predicate<BlockState> wat) {
        return s.actief.stukken.stream().filter(x -> wat.test(x.state())).count();
    }

    // =====================================================================================================================
    // what the generators wrote
    // =====================================================================================================================

    /**
     * The castle's tiles hold the two levels of this world, not the engine's practice lanes: three lanes each (the garden,
     * the floor above, the secret room), tips in the garden, the secret's mark only in the secret room, and every pipe
     * mouth and door inside a lane.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void guhriow1LevelsInHetKasteel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Map<String, BlockPos> starts = new HashMap<>();
        Map<BlockPos, BlockState> blokken = new HashMap<>();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        net.minecraft.world.level.block.Block[] soorten = {GuhrioW1Feature.TIP.get(), GuhrioW1Feature.GEHEIM.get(), GuhrioFeature.PIJP.get(),
                GuhrioFeature.DEUR.get(), GuhrioFeature.STARTBLOK.get()};
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                StructureTemplate tegel = level.getStructureManager().get(Guhs.id(GuhrioKasteel.STRUCTUUR + "/stuk_" + i + "_" + j)).orElse(null);
                helper.assertTrue(tegel != null, "the castle's tile " + i + " " + j);
                BlockPos hoek = new BlockPos(i * 32, 0, j * 32);
                for (net.minecraft.world.level.block.Block soort : soorten) {
                    for (StructureTemplate.StructureBlockInfo info : tegel.filterBlocks(hoek, settings, soort)) {
                        blokken.put(info.pos(), info.state());
                        if (soort == GuhrioFeature.STARTBLOK.get()) {
                            starts.put(info.nbt().getStringOr("Level", ""), info.pos());
                        }
                    }
                }
            }
        }
        String[][] verwacht = {{Binnentuin.LEVEL_1, "hol", "boven"}, {Binnentuin.LEVEL_2, "muur", "kas"}};
        for (String[] v : verwacht) {
            String id = v[0];
            GuhrioLevel def = GuhrioLevel.bestand(level.getServer(), id);
            helper.assertTrue(def != null && def.banen().size() == 3, id + ": a level file with three lanes");
            helper.assertTrue(def.banen().get(1).id().equals(v[1]) && def.banen().get(2).id().equals(v[2]), id + ": its lanes " + def.banen());
            BlockPos start = starts.get(id);
            helper.assertTrue(start != null, id + ": its start block in the castle");
            GuhrioLevel.Geplaatst lvl = def.plaats(level.dimension(), start, blokken.get(start).getValue(GuhrioBlocks.StartBlok.FACING));
            int geheimBaan = Binnentuin.LEVEL_1.equals(id) ? 1 : 2;
            int tips = 0, geheimen = 0, monden = 0, deuren = 0;
            for (var e : blokken.entrySet()) {
                int b = lvl.baanVan(e.getKey());
                if (b < 0) {
                    continue;
                }
                BlockState state = e.getValue();
                if (state.getBlock() instanceof GuhrioW1Blocks.TipBlok) {
                    tips++;
                    helper.assertTrue(state.getValue(GuhrioW1Blocks.TipBlok.TIP) < Binnentuin.TIPS, id + ": a tip with a text: " + state);
                } else if (state.getBlock() instanceof GuhrioW1Blocks.GeheimBlok) {
                    geheimen++;
                    helper.assertTrue(b == geheimBaan, id + ": the secret's mark is in the secret room, not in lane " + b);
                } else if (state.getBlock() instanceof GuhrioBlocks.PijpBlok && !state.getValue(GuhrioBlocks.PijpBlok.BOVEN)) {
                    monden++;
                } else if (state.getBlock() instanceof GuhrioBlocks.DeurBlok
                        && state.getValue(GuhrioBlocks.DeurBlok.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                    deuren++;
                }
            }
            helper.assertTrue(tips > 0 && geheimen > 0, id + ": tips and a secret: " + tips + " / " + geheimen);
            if (Binnentuin.LEVEL_1.equals(id)) {
                helper.assertTrue(monden == 6 && deuren == 0, id + ": three pairs of pipe mouths: " + monden + " / " + deuren);
            } else {
                helper.assertTrue(monden == 0 && deuren == 4, id + ": two pairs of doors: " + monden + " / " + deuren);
            }
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // the levels, walked
    // =====================================================================================================================

    /**
     * Level 1-1 as built: the first tip, the pipe with the coin over it leads to the mole's den (the secret, a big
     * vadsmunt), the den's sideways pipe leads back to the lawn further on, the big pipe leads up to the clouds, a flag
     * there is yours, and with all three big vadsmunten the flagpole grants the level's own advancement. No thanks here:
     * Pad-guh stands at the end of 1-2.
     */
    // (the timeout is in ticks, and the test server runs hundreds of ticks a second: the template's entities and the level's
    //  creatures come when their chunks are loaded, which takes real time - 400 ticks were over in one second at the merge,
    //  and 4000 in less than the chunk took in a run of fifteen classes together)
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 40000)
    public static void guhriow1BinnentuinGelopen(GameTestHelper helper) {
        String naam = "guhriow1_test_1_1";
        BlockPos hoek = plaats(helper, naam, 40);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        helper.assertTrue(GuhrioSpel.start(p, cel(hoek, 2, 3)), "the start block of 1-1 lets the player in");
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        helper.assertTrue(Binnentuin.LEVEL_1.equals(s.level().level().id()) && s.level().banen().size() == 3, "level 1-1 with its three lanes");
        int hol = baan(s, "hol"), boven = baan(s, "boven");
        helper.assertTrue(hol == 1 && boven == 2, "the den and the clouds: " + hol + " / " + boven);
        helper.assertTrue(tel(s, st -> st.getBlock() instanceof GuhrioStukken.VadsmuntBlok) == 3, "three big vadsmunten");
        helper.assertTrue(tel(s, st -> st.getBlock() instanceof GuhrioBlocks.GuhmbaPlek) == 8, "eight Guhmba's");
        // the first tip: two cells after the start
        zet(p, hoek, 3, 3);
        GuhrioSpel.tick(p);
        helper.assertTrue((Binnentuin.getoond(s) & 1) != 0, "the first tip was walked through");
        // the first big vadsmunt, on the bricks
        zet(p, hoek, 27, 7);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.vads == 1, "the vadsmunt on the bricks: " + s.vads);
        // the secret: down the pipe at s = 48 (its mouth is row 4, you stand on row 5)
        zet(p, hoek, 48, 5);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, cel(hoek, 48, 4), 0);
        helper.assertTrue(s.inPijp(), "into the pipe with the coin over it");
        door(p, GuhrioSpel.PIJP_TICKS * 2 + 12);
        helper.assertTrue(s.baan == hol && !s.inPijp() && p.blockPosition().getX() == cel(hoek, 3, 0).getX(), "out of the den's ceiling: lane " + s.baan);
        helper.assertFalse(Binnentuin.geheimGevonden(p, Binnentuin.LEVEL_1), "not found before you stand in it");
        zet(p, hoek, 3, 12);
        GuhrioSpel.tick(p);
        helper.assertTrue(Binnentuin.geheimGevonden(p, Binnentuin.LEVEL_1) && heeft(p, "quest/guhrio_w1_geheim_1_1"), "the mole's den is found");
        helper.assertFalse(Binnentuin.geheimGevonden(p, Binnentuin.LEVEL_2), "the other level's secret is not");
        zet(p, hoek, 10, 15);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.vads == 3, "the vadsmunt of the den: " + s.vads);
        // out through the sideways pipe: back on the lawn, on the pipe at s = 58
        zet(p, hoek, 16, 12);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, cel(hoek, 17, 12), 0);
        helper.assertTrue(s.inPijp(), "into the den's sideways pipe");
        door(p, GuhrioSpel.PIJP_TICKS * 2 + 12);
        helper.assertTrue(s.baan == 0 && p.blockPosition().getX() == cel(hoek, 58, 0).getX(), "back on the lawn at s = 58: " + p.blockPosition());
        // the big pipe at s = 88 (mouth row 5): up to the clouds
        zet(p, hoek, 88, 6);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, cel(hoek, 88, 5), 0);
        helper.assertTrue(s.inPijp(), "into the big pipe");
        door(p, GuhrioSpel.PIJP_TICKS * 2 + 12);
        helper.assertTrue(s.baan == boven && p.blockPosition().getX() == cel(hoek, 27, 0).getX(), "out of the pipe on the clouds: " + p.blockPosition());
        zet(p, hoek, 30, 12);
        GuhrioSpel.tick(p);
        helper.assertTrue(cel(hoek, 30, 12).equals(s.vlagPos) && s.vlagBaan == boven, "the flag on the clouds is yours");
        // the hidden block under the little cloud, and the vadsmunt on it
        BlockPos verborgen = cel(hoek, 59, 15);
        helper.assertTrue(level.getBlockState(verborgen).getBlock() instanceof GuhrioStukken.OnzichtbaarBlok, "the hidden block: " + level.getBlockState(verborgen));
        zet(p, hoek, 59, 12);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.BOTS, verborgen, 0);
        helper.assertTrue(s.staat(verborgen) == 1, "bumped: it is there now");
        zet(p, hoek, 62, 17);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.vads == 7 && GuhrioKasteel.vadsmunten(p, Binnentuin.LEVEL_1) == 7, "all three: " + s.vads);
        helper.assertFalse(heeft(p, "quest/guhrio_w1_vads_1_1"), "its advancement comes at the flagpole");
        // the flagpole
        zet(p, hoek, 88, 12);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.klaar() && GuhrioKasteel.gehaald(p, Binnentuin.LEVEL_1), "the flagpole of 1-1");
        helper.assertTrue(heeft(p, "quest/guhrio_w1_vads_1_1") && heeft(p, "quest/guhrio_stap_2"), "three vadsmunten and the level: granted");
        helper.assertFalse(Binnentuin.wacht(p) || heeft(p, "quest/guhrio_w1_prinses"), "Pad-guh thanks nobody at the end of 1-1");
        helper.assertFalse(Binnentuin.allesGevonden(p), "half the garden's secrets is not all of them");
        // (the level's creatures come once their chunks tick: the far end of the lane a second or two after the near end)
        helper.succeedWhen(() -> {
            long guhmbas = s.actief.wezens.values().stream().filter(e -> e instanceof GuhmbaEntity && !e.isRemoved()).count();
            helper.assertTrue(guhmbas == 8, "all eight Guhmba's are in the level: " + guhmbas);
            weg(helper, p);
            ruim(helper, naam, hoek);
        });
    }

    /**
     * Level 1-2 as built: the hidden block beside the arch, the door on the arch to the coin greenhouse (the secret) and
     * back, the tower door to the top of the wall, the flagpole - and then Pad-guh, who stands there, thanks you a moment
     * later: every time, each player their own count, the advancements the first time.
     */
    // (the timeout is in ticks, and the test server runs hundreds of ticks a second: the template's entities and the level's
    //  creatures come when their chunks are loaded, which takes real time - 400 ticks were over in one second at the merge,
    //  and 4000 in less than the chunk took in a run of fifteen classes together: "Pad-guh at the end of 1-2: 0 on tick 4002")
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 40000)
    public static void guhriow1HeggentuinEnPrinses(GameTestHelper helper) {
        String naam = "guhriow1_test_1_2";
        BlockPos hoek = plaats(helper, naam, 70);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper), q = speler(helper);
        BlockPos start = cel(hoek, 2, 3);
        helper.assertFalse(GuhrioSpel.start(p, start), "1-2 stays shut until 1-1 is done");
        GuhrioKasteel.ontgrendel(p, Binnentuin.LEVEL_2);
        helper.assertTrue(GuhrioSpel.start(p, start), "the start block of 1-2 lets the player in");
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        int muur = baan(s, "muur"), kas = baan(s, "kas");
        helper.assertTrue(Binnentuin.LEVEL_2.equals(s.level().level().id()) && muur == 1 && kas == 2, "level 1-2 with the wall and the greenhouse");
        helper.assertTrue(tel(s, st -> st.getBlock() instanceof GuhrioStukken.SchildMikaPlek) == 1
                && tel(s, st -> st.getBlock() instanceof GuhrioBlocks.GuhmbaPlek) == 9, "a Schild-Mika and nine Guhmba's");
        // the hidden block beside the little hedge, then the door on the arch
        BlockPos verborgen = cel(hoek, 64, 5);
        zet(p, hoek, 64, 3);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.BOTS, verborgen, 0);
        helper.assertTrue(s.staat(verborgen) == 1, "the hidden block is found");
        zet(p, hoek, 68, 7);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DEUR, cel(hoek, 68, 7), 0);
        helper.assertTrue(s.baan == kas && p.blockPosition().equals(cel(hoek, 78, 12)), "through the door on the arch into the greenhouse: " + p.blockPosition());
        helper.assertFalse(Binnentuin.geheimGevonden(p, Binnentuin.LEVEL_2), "not found in the doorway");
        zet(p, hoek, 79, 12);
        GuhrioSpel.tick(p);
        helper.assertTrue(Binnentuin.geheimGevonden(p, Binnentuin.LEVEL_2) && heeft(p, "quest/guhrio_w1_geheim_1_2"), "the coin greenhouse is found");
        helper.assertFalse(Binnentuin.geheimGevonden(q, Binnentuin.LEVEL_2) || heeft(q, "quest/guhrio_w1_geheim_1_2"), "somebody else has not found it");
        zet(p, hoek, 78, 12);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DEUR, cel(hoek, 78, 12), 0);
        helper.assertTrue(s.baan == 0 && p.blockPosition().equals(cel(hoek, 68, 7)), "and back onto the arch: " + p.blockPosition());
        // the tower door at the end of the garden: onto the wall
        zet(p, hoek, 90, 3);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DEUR, cel(hoek, 90, 3), 0);
        helper.assertTrue(s.baan == muur && p.blockPosition().equals(cel(hoek, 3, 12)), "through the tower door onto the wall: " + p.blockPosition());
        // the flagpole
        zet(p, hoek, 64, 12);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.klaar() && GuhrioKasteel.gehaald(p, Binnentuin.LEVEL_2), "the flagpole of 1-2");
        helper.assertTrue(Binnentuin.wacht(p) && Binnentuin.bedankt(p) == 0, "Pad-guh takes a breath first");
        helper.assertFalse(heeft(p, "quest/guhrio_w1_prinses") || heeft(p, "quest/guhrio_w1_vads_1_2"), "nothing granted yet (and not all vadsmunten)");
        int[] ronde = {0};
        helper.onEachTick(() -> {
            if (ronde[0] == 0 && Binnentuin.bedankt(p) == 1) {
                ronde[0] = 1;
                helper.assertTrue(heeft(p, "quest/guhrio_w1_prinses") && heeft(p, "guhrio/guhrio_w1_binnentuin"), "the first thanks grants both advancements");
                helper.assertTrue(Binnentuin.bedankt(q) == 0 && !heeft(q, "quest/guhrio_w1_prinses"), "only for the player who got there");
                // once more: he says it every time
                GuhrioSpel.stop(p, GuhrioSpel.Einde.WEG);
                helper.assertTrue(GuhrioSpel.start(p, start), "a second run");
                zet(p, hoek, 64, 12);
                GuhrioSpel.tick(p);
                helper.assertTrue(Binnentuin.wacht(p), "the flagpole again");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(Binnentuin.bedankt(p) == 2, "thanked twice: " + Binnentuin.bedankt(p));
            helper.assertFalse(Binnentuin.wacht(p), "and nothing left to say");
            // Pad-guh himself stands behind the flagpole, in front of the wrong part of the castle (the template's
            // entities come once their chunk has loaded them: not on the tick the level was put down)
            List<GuhNpcEntity> padguh = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(cel(hoek, 67, 12)).inflate(2),
                    e -> e.getKind() == GuhNpcEntity.Kind.PADGUH);
            helper.assertTrue(padguh.size() == 1, "Pad-guh at the end of 1-2: " + padguh.size());
            weg(helper, p, q);
            ruim(helper, naam, hoek);
        });
    }

    // =====================================================================================================================
    // the two invisible pieces
    // =====================================================================================================================

    /**
     * A tip is shown once per run, to somebody who never finished the level; a secret's mark counts per player, once, in
     * any level (a level that is not of this world just has no advancement for it).
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriow1TipEnGeheim(GameTestHelper helper) {
        int z = 3;
        for (int x = 1; x <= 21; x++) {
            helper.setBlock(new BlockPos(x, 1, z), GuhrioW1Feature.GRAS.get());
        }
        BlockPos startPos = new BlockPos(2, 2, z);
        helper.setBlock(startPos, GuhrioFeature.STARTBLOK.get().defaultBlockState().setValue(GuhrioBlocks.StartBlok.FACING, Direction.EAST));
        BlockPos start = helper.absolutePos(startPos);
        ((GuhrioBlocks.StartBlockEntity) helper.getLevel().getBlockEntity(start)).zetLevel(LEVEL);
        helper.setBlock(new BlockPos(5, 2, z), GuhrioW1Feature.TIP.get().defaultBlockState().setValue(GuhrioW1Blocks.TipBlok.TIP, 1));
        helper.setBlock(new BlockPos(6, 2, z), GuhrioW1Feature.TIP.get().defaultBlockState().setValue(GuhrioW1Blocks.TipBlok.TIP, 1));
        helper.setBlock(new BlockPos(8, 2, z), GuhrioW1Feature.GEHEIM.get());
        helper.setBlock(new BlockPos(10, 2, z), GuhrioW1Feature.BLOEM.get().defaultBlockState().setValue(GuhrioW1Blocks.Plantje.SOORT, 2));
        ServerPlayer p = speler(helper), q = speler(helper);
        helper.assertTrue(GuhrioSpel.start(p, start) && GuhrioSpel.start(q, start), "both in the little level");
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        helper.assertTrue(tel(s, st -> st.getBlock() instanceof GuhrioW1Blocks.TipBlok) == 2 && tel(s, st -> st.getBlock() instanceof GuhrioW1Blocks.GeheimBlok) == 1,
                "the engine found the pieces (and the plant is no piece): " + s.actief.stukken.size());
        // walking through the two blocks of one tip shows it once
        var v = helper.absoluteVec(new net.minecraft.world.phys.Vec3(5.5, 2, z + 0.5));
        p.snapTo(v.x, v.y, v.z);
        GuhrioSpel.tick(p);
        helper.assertTrue(Binnentuin.getoond(s) == 2, "tip 1 was shown: " + Binnentuin.getoond(s));
        helper.assertTrue(Binnentuin.leest(p) && !Binnentuin.leest(q), "and it will be said once more to whoever stays to read it");
        helper.assertFalse(Binnentuin.tip(p, s, 1), "not twice in one run");
        helper.assertTrue(Binnentuin.tip(p, s, 0), "another tip is another line");
        helper.assertFalse(Binnentuin.tip(p, s, Binnentuin.TIPS) || Binnentuin.tip(p, s, -1), "a tip without a text is nothing");
        helper.assertTrue(Binnentuin.getoond(GuhrioSpel.sessie(q)) == 0, "the other player has seen nothing yet");
        // the plant stops nobody
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(10, 2, z)))
                .getCollisionShape(helper.getLevel(), helper.absolutePos(new BlockPos(10, 2, z))).isEmpty(), "you walk through the plant");
        // the secret: per player
        v = helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.5, 2, z + 0.5));
        p.snapTo(v.x, v.y, v.z);
        GuhrioSpel.tick(p);
        GuhrioSpel.tick(p);
        helper.assertTrue(Binnentuin.geheimGevonden(p, LEVEL) && !Binnentuin.geheimGevonden(q, LEVEL), "found by the one who stood there");
        helper.assertFalse(Binnentuin.leest(p), "the secret's line ends the tip");
        helper.assertFalse(heeft(p, "quest/guhrio_w1_geheim_1_1") || heeft(p, "quest/guhrio_w1_geheim_1_2"), "no advancement of the garden for another level");
        // a new run shows the tip again...
        GuhrioSpel.stop(p, GuhrioSpel.Einde.WEG);
        helper.assertTrue(GuhrioSpel.start(p, start), "a second run");
        helper.assertTrue(Binnentuin.tip(p, GuhrioSpel.sessie(p), 1), "a new run, the tip again");
        helper.assertTrue(Binnentuin.geheimGevonden(p, LEVEL), "the secret stays found");
        // ... until the level was finished once
        GuhrioSpel.stop(p, GuhrioSpel.Einde.WEG);
        net.minecraft.nbt.CompoundTag gehaald = new net.minecraft.nbt.CompoundTag();
        gehaald.putBoolean(LEVEL, true);
        GuhrioSpel.spaar(p).put("Gehaald", gehaald);
        helper.assertTrue(GuhrioSpel.start(p, start) && GuhrioKasteel.gehaald(p, LEVEL), "a third run, after finishing it");
        helper.assertFalse(Binnentuin.tip(p, GuhrioSpel.sessie(p), 1), "whoever finished the level gets no tips");
        // Pad-guh's lines for somebody who comes back never run out
        List<Integer> keren = new ArrayList<>();
        for (int i = 0; i < Binnentuin.ALWEER + 2; i++) {
            Binnentuin.bedank(q);
            keren.add(Binnentuin.bedankt(q));
        }
        helper.assertTrue(keren.get(keren.size() - 1) == Binnentuin.ALWEER + 2 && heeft(q, "quest/guhrio_w1_prinses"), "thanked " + keren);
        // the famous line and one more, every time - but the first time not the famous line once more when the Pad-guh
        // of the forecourt (guhrio-beloning) has just called it after this player
        helper.assertTrue(Binnentuin.woorden(p, 0).size() == 2 && Binnentuin.woorden(p, 1).size() == 2, "two lines in the chat");
        nl.juiced.guhs.quest.GuhQuests.saved(p).putBoolean(Binnentuin.BELONING_GAG, true);
        helper.assertTrue(Binnentuin.woorden(p, 0).size() == 1 && Binnentuin.woorden(p, 1).size() == 2, "said by the other Pad-guh: not twice");
        weg(helper, p, q);
        helper.succeed();
    }
}
