package nl.juiced.guhs.feature.guhrio;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of Super Guhrio's engine (batch "guhrio"; run with {@code -Pgt=guhrio}): the lane's sums, and the server
 * side of a level played by mock players: held on the lane, back to the flag after a fall, every piece per player
 * (?-block, coin, brick), the Guhmba (walks and turns, flat when landed on, pops back, sends you back from the side),
 * the pipe, the flagpole, no damage, and that leaving a level always puts the player back to normal.
 * <p>
 * The tests build a little lane in the empty room guhrio_test_baan: the ground along z = 3 from x = 1 to 21 (its top is
 * y = 1, you walk at y = 2), the start block at (2, 2, 3) facing east. A mock player's tick is not run by the server, so
 * the tests call {@link GuhrioSpel#tick} themselves; what the player's own game would report goes through
 * {@link GuhrioSpel#actie}, exactly like the real message.
 */
public class GuhrioGameTests {
    private static final String BATCH = "guhrio", KAMER = "guhrio_test_baan", LEVEL = "guhrio_gametest";
    private static final BlockPos START = new BlockPos(2, 2, 3);
    private static final int Z = 3;

    static {
        // the start block is (0,0,0): the lane runs from one block behind it to 19 in front of it
        GuhrioLevel.zet(new GuhrioLevel(LEVEL, "T-1", List.of(new GuhrioLevel.BaanDef("test", List.of(new BlockPos(-1, 0, 0), new BlockPos(19, 0, 0)),
                true, 13, 6, -3, 8)), new BlockPos(18, 0, 0)));
    }

    /** The lane of the tests: ground, the start block. Returns the start block's place in the world. */
    private static BlockPos bouw(GameTestHelper helper) {
        for (int x = 1; x <= 21; x++) {
            helper.setBlock(new BlockPos(x, 1, Z), GuhrioFeature.GROND.get());
        }
        helper.setBlock(START, GuhrioFeature.STARTBLOK.get().defaultBlockState().setValue(GuhrioBlocks.StartBlok.FACING, Direction.EAST));
        BlockPos abs = helper.absolutePos(START);
        ((GuhrioBlocks.StartBlockEntity) helper.getLevel().getBlockEntity(abs)).zetLevel(LEVEL);
        return abs;
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    /** A player in the test level. */
    private static ServerPlayer start(GameTestHelper helper, BlockPos startAbs) {
        ServerPlayer p = speler(helper);
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "the start block lets the player in");
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** Puts a mock player in the middle of a template column of the lane, feet at template height y. */
    private static void zet(GameTestHelper helper, ServerPlayer p, double x, double y) {
        Vec3 v = helper.absoluteVec(new Vec3(x, y, Z + 0.5));
        p.snapTo(v.x, v.y, v.z);
    }

    private static void near(GameTestHelper helper, double a, double b, String wat) {
        helper.assertTrue(Math.abs(a - b) < 1e-6, wat + ": " + a + " (expected " + b + ")");
    }

    // =====================================================================================================================
    // the lane's sums
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void baanSommen(GameTestHelper helper) {
        // a straight lane east, the camera on its right hand (south)
        Baan recht = new Baan("recht", List.of(new BlockPos(0, 64, 0), new BlockPos(20, 64, 0)), true, 13, 6, 60, 80);
        near(helper, recht.lengte(), 20, "length");
        Baan.Plek p = recht.plek(7.25, 3.5);
        near(helper, p.s(), 6.75, "s of a spot beside the lane");
        near(helper, p.naast(), 3.0, "how far beside the lane");
        Baan.Stap st = recht.stap(0, 7.25, 3.5);
        near(helper, st.x(), 7.25, "a step keeps where you are along the lane");
        near(helper, st.z(), 0.5, "a step puts you on the line");
        helper.assertTrue(recht.stap(0, 30, 0.5).eind() && Math.abs(recht.stap(0, 30, 0.5).x() - (20.5 + Baan.RAND)) < 1e-6, "the far end stops you");
        helper.assertTrue(recht.stap(0, -5, 0.5).eind() && Math.abs(recht.stap(0, -5, 0.5).x() - (0.5 - Baan.RAND)) < 1e-6, "the near end stops you");
        near(helper, recht.naarCamera(0).z, 1, "the camera stands south of a lane that runs east");
        near(helper, recht.cameraYaw(10), 180, "and looks north");
        helper.assertTrue(recht.schermRechts() == 1, "further along the lane is right on the screen");
        helper.assertTrue(recht.bevat(new BlockPos(5, 70, 0)) && !recht.bevat(new BlockPos(5, 70, 1)) && !recht.bevat(new BlockPos(5, 81, 0))
                && !recht.bevat(new BlockPos(21, 70, 0)), "the lane's own blocks");
        helper.assertTrue(recht.kolommen().size() == 21, "21 columns");
        Baan links = new Baan("links", recht.punten(), false, 13, 6, 60, 80);
        near(helper, links.naarCamera(0).z, -1, "camera links: north of the lane");
        helper.assertTrue(links.schermRechts() == -1, "then further along the lane is left on the screen");

        // a corner: 10 east, then 10 south
        Baan hoek = new Baan("hoek", List.of(new BlockPos(0, 64, 0), new BlockPos(10, 64, 0), new BlockPos(10, 64, 10)), true, 13, 6, 60, 80);
        near(helper, hoek.lengte(), 20, "length around the corner");
        helper.assertTrue(hoek.richting(0) == Direction.EAST && hoek.richting(1) == Direction.SOUTH, "the two pieces");
        Baan.Stap om = hoek.stap(0, 10.5 + 0.3, 0.5);                    // 0.3 past the corner, still going east
        helper.assertTrue(om.stuk() == 1 && !om.eind(), "past the corner you are on the next piece");
        near(helper, om.x(), 10.5, "on its line");
        near(helper, om.z(), 0.5 + 0.3, "as far along it as you overshot");
        near(helper, om.s(), 10.3, "s goes on");
        Baan.Stap terug = hoek.stap(1, 10.5, 0.5 - 0.4);                 // and back around it
        helper.assertTrue(terug.stuk() == 0, "back around the corner");
        near(helper, terug.x(), 10.5 - 0.4, "back on the first piece");
        helper.assertTrue(hoek.kies(0, 10, 1) == 1 && hoek.kies(0, 10, -1) == 0 && hoek.kies(1, 10, -1) == 0 && hoek.kies(0, 5, 1) == 0,
                "standing on the corner, the way you push picks the piece");
        near(helper, hoek.cameraYaw(2), 180, "far before the corner the camera looks north");
        near(helper, hoek.cameraYaw(18), -90, "far after it east (the camera stands west of a lane that runs south)");
        float midden = hoek.cameraYaw(10);
        helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(midden - 225)) < 1e-3, "on the corner it is halfway the swing: " + midden);
        helper.assertTrue(hoek.kolommen().size() == 21, "the corner column counts once");
        helper.assertTrue(hoek.plek(10.5, 4.5).stuk() == 1 && Math.abs(hoek.plek(10.5, 4.5).s() - 14) < 1e-6, "a spot on the second piece");

        // a level turned with its start block: +x is the way it faces, +z its right hand
        BlockPos anker = new BlockPos(100, 64, 200);
        helper.assertTrue(GuhrioLevel.wereld(anker, Direction.EAST, new BlockPos(5, 2, 1)).equals(new BlockPos(105, 66, 201)), "facing east: as built");
        helper.assertTrue(GuhrioLevel.wereld(anker, Direction.SOUTH, new BlockPos(5, 2, 1)).equals(new BlockPos(99, 66, 205)), "facing south");
        helper.assertTrue(GuhrioLevel.wereld(anker, Direction.WEST, new BlockPos(5, 2, 1)).equals(new BlockPos(95, 66, 199)), "facing west");
        helper.assertTrue(GuhrioLevel.wereld(anker, Direction.NORTH, new BlockPos(5, 2, 1)).equals(new BlockPos(101, 66, 195)), "facing north");
        helper.assertTrue("1:23.4".equals(GuhrioSpel.tijd(20 * 83 + 9)), "the clock: " + GuhrioSpel.tijd(20 * 83 + 9));
        helper.succeed();
    }

    /** The test levels' data and templates belong together: the level files load, every piece lies in one of the lanes. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void testlevelsKloppen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (String naam : new String[]{GuhrioEvents.TESTLEVEL, GuhrioEvents.TESTHOEK}) {
            GuhrioLevel def = GuhrioLevel.vind(level.getServer(), naam);
            helper.assertTrue(def != null, "the level file of " + naam + " loads");
            StructureTemplate template = level.getStructureManager().get(Guhs.id(naam)).orElse(null);
            helper.assertTrue(template != null, "the template of " + naam + " is there");
            StructurePlaceSettings settings = new StructurePlaceSettings();
            List<StructureTemplate.StructureBlockInfo> start = template.filterBlocks(BlockPos.ZERO, settings, GuhrioFeature.STARTBLOK.get());
            helper.assertTrue(start.size() == 1, naam + " has one start block");
            helper.assertTrue(naam.equals(start.get(0).nbt().getStringOr("Level", "")), naam + ": the start block names its level");
            GuhrioLevel.Geplaatst lvl = def.plaats(level.dimension(), start.get(0).pos(), Direction.EAST);
            helper.assertTrue(lvl.baanVan(start.get(0).pos()) == 0, naam + ": the start block is on the first lane");
            int stukken = 0;
            for (Block blok : new Block[]{GuhrioFeature.MUNT.get(), GuhrioFeature.VRAAGBLOK.get(), GuhrioFeature.STEEN.get(), GuhrioFeature.VLAG.get(),
                    GuhrioFeature.MAST.get(), GuhrioFeature.PIJP.get(), GuhrioFeature.DEUR.get(), GuhrioFeature.GUHMBA_PLEK.get()}) {
                for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, settings, blok)) {
                    helper.assertTrue(lvl.baanVan(info.pos()) >= 0, naam + ": " + blok + " at " + info.pos() + " lies in a lane");
                    stukken++;
                }
            }
            helper.assertTrue(stukken > 10, naam + " has pieces: " + stukken);
        }
        helper.assertTrue(GuhrioLevel.vind(level.getServer(), GuhrioEvents.TESTLEVEL).banen().size() == 2, "the test level has its bonus lane");
        helper.succeed();
    }

    // =====================================================================================================================
    // in a level
    // =====================================================================================================================

    /** In: on the lane, a higher jump. A little off the line: put back. Taken far away: the level ends and all is normal again. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void baanHoudtJeVast(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        helper.assertTrue(s != null && GuhrioSpel.speelt(p), "in the level");
        near(helper, p.getX(), startAbs.getX() + 0.5, "put on the start");
        near(helper, p.getZ(), startAbs.getZ() + 0.5, "on the lane's line");
        helper.assertTrue(p.getAttribute(Attributes.JUMP_STRENGTH).hasModifier(GuhrioSpel.SPRONG)
                && p.getAttribute(Attributes.GRAVITY).hasModifier(GuhrioSpel.ZWAARTE), "the level's jump and weight");
        helper.assertFalse(GuhrioSpel.start(p, startAbs), "no second level on top of the first");
        // walked along the lane: nothing happens
        zet(helper, p, 9.3, 2);
        GuhrioSpel.tick(p);
        near(helper, p.getX(), helper.absoluteVec(new Vec3(9.3, 2, 0)).x, "free along the lane");
        // pushed 0.8 off the line: back on it, at the same place along the lane
        p.snapTo(p.getX(), p.getY(), p.getZ() + 0.8);
        GuhrioSpel.tick(p);
        near(helper, p.getZ(), startAbs.getZ() + 0.5, "back on the line");
        near(helper, p.getX(), helper.absoluteVec(new Vec3(9.3, 2, 0)).x, "at the same place along it");
        helper.assertTrue(GuhrioSpel.sessie(p) == s, "still in the level");
        // past the end: back to the end
        zet(helper, p, 25.0, 2);
        GuhrioSpel.tick(p);
        near(helper, p.getX(), helper.absoluteVec(new Vec3(21.5 + Baan.RAND, 2, 0)).x, "the end of the lane holds you");
        // taken away (a teleport): out of the level, back to normal
        p.snapTo(p.getX(), p.getY(), p.getZ() + 9);
        GuhrioSpel.tick(p);
        helper.assertTrue(GuhrioSpel.sessie(p) == null && !GuhrioSpel.speelt(p), "taken away: the level is over");
        helper.assertFalse(p.getAttribute(Attributes.JUMP_STRENGTH).hasModifier(GuhrioSpel.SPRONG)
                || p.getAttribute(Attributes.GRAVITY).hasModifier(GuhrioSpel.ZWAARTE), "jump and weight are normal again");
        // the start block wants you to step out of it first
        helper.assertFalse(GuhrioSpel.magStarten(p), "not straight back in");
        weg(helper, p);
        helper.succeed();
    }

    /** Falling under the lane brings you back: to the start, and after a flag to that flag. Nothing is lost. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void valBrengtJeTerug(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos vlag = new BlockPos(8, 2, Z);
        helper.setBlock(vlag, GuhrioFeature.VLAG.get());
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        GuhrioSpel.munt(p, s, 3);
        float leven = p.getHealth();
        zet(helper, p, 5.5, -2.5);                               // under the lane's floor (start - 3)
        GuhrioSpel.tick(p);
        near(helper, p.getX(), startAbs.getX() + 0.5, "back at the start");
        near(helper, p.getY(), startAbs.getY(), "on its floor");
        helper.assertTrue(GuhrioSpel.sessie(p) == s && s.munten == 3 && p.getHealth() == leven, "still playing, nothing lost, not hurt");
        // the flag
        zet(helper, p, 8.5, 2);
        GuhrioSpel.tick(p);
        helper.assertTrue(helper.absolutePos(vlag).equals(s.vlagPos), "the flag is yours");
        zet(helper, p, 14.5, -4);
        GuhrioSpel.tick(p);
        near(helper, p.getX(), helper.absolutePos(vlag).getX() + 0.5, "back at the flag");
        near(helper, p.getY(), helper.absolutePos(vlag).getY(), "standing where it stands");
        // somebody else has their own flag
        ServerPlayer q = start(helper, startAbs);
        zet(helper, q, 14.5, -4);
        GuhrioSpel.tick(q);
        near(helper, q.getX(), startAbs.getX() + 0.5, "the other player goes back to the start");
        weg(helper, p, q);
        helper.succeed();
    }

    /** Nobody is hurt in a level; out of it you can be again. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void geenSchadeInEenLevel(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        ServerPlayer p = start(helper, startAbs);
        ServerLevel level = helper.getLevel();
        float leven = p.getHealth();
        p.hurtServer(level, level.damageSources().cactus(), 4f);
        p.hurtServer(level, level.damageSources().lava(), 4f);
        p.hurtServer(level, level.damageSources().fall(), 8f);
        helper.assertTrue(p.getHealth() == leven, "not hurt in a level: " + p.getHealth());
        // (a mock player shrugs off damage anyway, so ask the damage event itself: stopped in a level, not out of it)
        helper.assertTrue(geweigerd(p, level.damageSources().cactus()) && geweigerd(p, level.damageSources().lava())
                && geweigerd(p, level.damageSources().fall()), "damage is stopped in a level");
        helper.assertFalse(geweigerd(p, level.damageSources().genericKill()), "(but /kill still works)");
        GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
        helper.assertFalse(geweigerd(p, level.damageSources().cactus()), "out of the level damage is normal again");
        weg(helper, p);
        helper.succeed();
    }

    /** Does the game refuse this damage for this player (the incoming-damage event is cancelled)? */
    private static boolean geweigerd(ServerPlayer p, net.minecraft.world.damagesource.DamageSource bron) {
        return net.neoforged.neoforge.common.CommonHooks.onEntityIncomingDamage(p, new net.neoforged.neoforge.common.damagesource.DamageContainer(bron, 4f));
    }

    // =====================================================================================================================
    // the pieces, per player
    // =====================================================================================================================

    /** A ?-block gives its coin once to every player; the block in the world never changes. Only a bump from near counts. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void vraagblokPerSpeler(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos blok = new BlockPos(5, 5, Z), buiten = new BlockPos(5, 5, Z + 2), kracht = new BlockPos(7, 5, Z);
        BlockState vraag = GuhrioFeature.VRAAGBLOK.get().defaultBlockState();
        helper.setBlock(blok, vraag);
        helper.setBlock(buiten, vraag);
        helper.setBlock(kracht, vraag.setValue(GuhrioBlocks.INHOUD, GuhrioBlocks.Inhoud.SUPERKNABBEL));
        ServerPlayer a = start(helper, startAbs), b = start(helper, startAbs);
        GuhrioSpel.Sessie sa = GuhrioSpel.sessie(a), sb = GuhrioSpel.sessie(b);
        int spaar = GuhrioSpel.munten(a);
        BlockPos abs = helper.absolutePos(blok);
        zet(helper, a, 5.5, 2);
        GuhrioSpel.actie(a, GuhrioPayloads.Actie.BOTS, abs, 0);
        helper.assertTrue(sa.munten == 1 && sa.staat(abs) == 1, "a coin for the first player, the block is empty for them");
        helper.assertTrue(GuhrioSpel.munten(a) == spaar + 1, "and in the pocket that stays");
        GuhrioSpel.actie(a, GuhrioPayloads.Actie.BOTS, abs, 0);
        helper.assertTrue(sa.munten == 1, "only once");
        helper.assertTrue(sb.munten == 0 && sb.staat(abs) == 0, "the other player's block is still full");
        helper.assertTrue(helper.getBlockState(blok).equals(vraag), "the block in the world never changes");
        // from far away: no
        zet(helper, b, 18.5, 2);
        GuhrioSpel.actie(b, GuhrioPayloads.Actie.BOTS, abs, 0);
        helper.assertTrue(sb.munten == 0, "a bump from 13 blocks away does not count");
        zet(helper, b, 5.5, 2);
        // a block that is not in the lane: no
        GuhrioSpel.actie(b, GuhrioPayloads.Actie.BOTS, helper.absolutePos(buiten), 0);
        helper.assertTrue(sb.munten == 0, "a ?-block outside the lane is not the level's");
        GuhrioSpel.actie(b, GuhrioPayloads.Actie.BOTS, abs, 0);
        helper.assertTrue(sb.munten == 1 && sb.staat(abs) == 1, "the second player gets their own coin");
        // the Superknabbel
        GuhrioSpel.actie(a, GuhrioPayloads.Actie.BOTS, helper.absolutePos(kracht), 0);
        helper.assertTrue(sa.kracht == GuhrioSpel.Kracht.SUPER && p(a), "the Superknabbel makes you big");
        helper.assertTrue(sb.kracht == GuhrioSpel.Kracht.GEEN, "only you");
        GuhrioSpel.stop(a, GuhrioSpel.Einde.GESTOPT);
        helper.assertFalse(p(a), "normal size again after the level");
        // a new run: everything is back
        helper.assertTrue(GuhrioSpel.start(a, startAbs) && GuhrioSpel.sessie(a).staat(abs) == 0 && GuhrioSpel.sessie(a).munten == 0, "a new run starts fresh");
        helper.assertTrue(GuhrioSpel.munten(a) == spaar + 1, "but your pocket stays");
        weg(helper, a, b);
        helper.succeed();
    }

    private static boolean p(ServerPlayer player) {
        return player.getAttribute(Attributes.SCALE).hasModifier(GuhrioSpel.GROOT);
    }

    /** A coin in the lane is taken by walking into it, by every player once. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void muntPerSpeler(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos munt = new BlockPos(6, 3, Z);
        helper.setBlock(munt, GuhrioFeature.MUNT.get());
        BlockPos abs = helper.absolutePos(munt);
        ServerPlayer a = start(helper, startAbs), b = start(helper, startAbs);
        zet(helper, a, 6.5, 2);                                  // (the coin hangs at head height)
        GuhrioSpel.tick(a);
        GuhrioSpel.tick(a);
        helper.assertTrue(GuhrioSpel.sessie(a).munten == 1 && GuhrioSpel.sessie(a).staat(abs) == 1, "walked into the coin: it is yours, once");
        helper.assertTrue(GuhrioSpel.sessie(b).munten == 0, "not the other player's");
        // the player's own game saw it first and reports it: the same
        zet(helper, b, 5.6, 2);
        GuhrioSpel.actie(b, GuhrioPayloads.Actie.RAAK, abs, 0);
        GuhrioSpel.actie(b, GuhrioPayloads.Actie.RAAK, abs, 0);
        helper.assertTrue(GuhrioSpel.sessie(b).munten == 1, "reported by the player's game: once too");
        helper.assertBlockPresent(GuhrioFeature.MUNT.get(), munt);
        weg(helper, a, b);
        helper.succeed();
    }

    /** A brick only breaks for a player with the Superknabbel, and only for that player. Being touched costs the power-up first. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void superknabbelBreektSteen(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos steen = new BlockPos(9, 5, Z);
        helper.setBlock(steen, GuhrioFeature.STEEN.get());
        BlockPos abs = helper.absolutePos(steen);
        ServerLevel level = helper.getLevel();
        ServerPlayer a = start(helper, startAbs), b = start(helper, startAbs);
        GuhrioSpel.Sessie sa = GuhrioSpel.sessie(a);
        zet(helper, a, 9.5, 2);
        zet(helper, b, 9.5, 2);
        GuhrioSpel.actie(a, GuhrioPayloads.Actie.BOTS, abs, 0);
        helper.assertTrue(sa.staat(abs) == 0, "small: the brick only hops");
        GuhrioSpel.zetKracht(a, sa, GuhrioSpel.Kracht.SUPER);
        GuhrioSpel.actie(a, GuhrioPayloads.Actie.BOTS, abs, 0);
        helper.assertTrue(sa.staat(abs) == 1, "big: it breaks");
        BlockState state = level.getBlockState(abs);
        helper.assertTrue(state.getCollisionShape(level, abs, CollisionContext.of(a)).isEmpty(), "you walk through where it was");
        helper.assertFalse(state.getCollisionShape(level, abs, CollisionContext.of(b)).isEmpty(), "the other player still stands on it");
        helper.assertFalse(state.getCollisionShape(level, abs, CollisionContext.empty()).isEmpty(), "and so does everything else");
        helper.assertBlockPresent(GuhrioFeature.STEEN.get(), steen);
        // touched: the power-up goes, you stay; touched again right away: nothing (safe for a moment); later: back to the flag
        Vec3 hier = a.position();
        GuhrioSpel.geraakt(a, sa);
        helper.assertTrue(sa.kracht == GuhrioSpel.Kracht.GEEN && a.position().equals(hier) && !p(a), "the first touch costs the Superknabbel");
        GuhrioSpel.geraakt(a, sa);
        helper.assertTrue(a.position().equals(hier), "safe for a moment");
        sa.veilig = 0;
        GuhrioSpel.geraakt(a, sa);
        near(helper, a.getX(), startAbs.getX() + 0.5, "then a touch brings you back to your flag");
        weg(helper, a, b);
        helper.succeed();
    }

    // =====================================================================================================================
    // the Guhmba
    // =====================================================================================================================

    /** The Guhmba of a spot: there while somebody plays, walks, turns at a wall and at a ledge, gone when the level is empty. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 420)
    public static void guhmbaLooptHeenEnWeer(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        helper.setBlock(new BlockPos(7, 2, Z), GuhrioFeature.BLOK.get());         // a wall on one side
        helper.setBlock(new BlockPos(13, 1, Z), net.minecraft.world.level.block.Blocks.AIR);   // a hole in the ground on the other
        helper.setBlock(new BlockPos(14, 1, Z), net.minecraft.world.level.block.Blocks.AIR);
        BlockPos plek = new BlockPos(10, 2, Z);
        helper.setBlock(plek, GuhrioFeature.GUHMBA_PLEK.get());
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Actief actief = GuhrioSpel.actief(helper.getLevel(), startAbs);
        helper.assertTrue(actief != null && actief.wezens.get(helper.absolutePos(plek)) instanceof GuhmbaEntity, "the spot has its Guhmba");
        GuhmbaEntity guhmba = (GuhmbaEntity) actief.wezens.get(helper.absolutePos(plek));
        helper.assertFalse(guhmba.shouldBeSaved(), "never saved");
        double x0 = helper.absoluteVec(new Vec3(0, 0, 0)).x, lijn = startAbs.getZ() + 0.5, vloer = startAbs.getY();
        double[] uiterst = {Double.MAX_VALUE, -Double.MAX_VALUE};
        helper.onEachTick(() -> {
            if (guhmba.isRemoved() || helper.getTick() > 250) {
                return;
            }
            double x = guhmba.getX() - x0;
            uiterst[0] = Math.min(uiterst[0], x);
            uiterst[1] = Math.max(uiterst[1], x);
            helper.assertTrue(Math.abs(guhmba.getZ() - lijn) < 1e-6, "the Guhmba stays on the lane's line");
            helper.assertTrue(guhmba.getY() > vloer - 0.6, "it never walks off the ledge: " + guhmba.position());
        });
        helper.runAtTickTime(250, () -> {
            helper.assertTrue(uiterst[0] < 8.6 && uiterst[0] > 8.3, "it walked to the wall and turned: " + uiterst[0]);
            helper.assertTrue(uiterst[1] > 12.0 && uiterst[1] < 12.7, "and to the ledge and turned: " + uiterst[1]);
            // the level empties: its Guhmba goes
            GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
        });
        helper.runAtTickTime(250 + 110, () -> {
            helper.assertTrue(guhmba.isRemoved(), "the level is empty: the Guhmba is gone");
            helper.assertTrue(GuhrioSpel.actief(helper.getLevel(), startAbs) == null, "and the level is forgotten");
            weg(helper, p);
            helper.succeed();
        });
    }

    /** Landed on: flat, harmless, pops back. From the side: back to your flag. Never hurt. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void guhmbaPlatEnTerug(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        helper.setBlock(new BlockPos(9, 2, Z), GuhrioFeature.BLOK.get());
        helper.setBlock(new BlockPos(12, 2, Z), GuhrioFeature.BLOK.get());
        BlockPos plek = new BlockPos(10, 2, Z);
        helper.setBlock(plek, GuhrioFeature.GUHMBA_PLEK.get());
        ServerPlayer p = start(helper, startAbs), ver = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        GuhmbaEntity guhmba = (GuhmbaEntity) GuhrioSpel.actief(helper.getLevel(), startAbs).wezens.get(helper.absolutePos(plek));
        helper.assertTrue(guhmba != null && guhmba.stampbaar() && guhmba.gevaarlijk(), "a Guhmba, up and grumbling");
        float leven = p.getHealth();
        helper.runAtTickTime(5, () -> {
            // somebody far away can't stamp it
            zet(helper, ver, 20.5, 2);
            GuhrioSpel.actie(ver, GuhrioPayloads.Actie.STAMP, BlockPos.ZERO, guhmba.getId());
            helper.assertFalse(guhmba.plat(), "a stamp from 10 blocks away does not count");
            // landed on it
            zet(helper, p, 10.5, 3);
            GuhrioSpel.actie(p, GuhrioPayloads.Actie.STAMP, BlockPos.ZERO, guhmba.getId());
            helper.assertTrue(guhmba.plat() && !guhmba.gevaarlijk() && !guhmba.stampbaar(), "flat, njeg!");
            Vec3 hier = p.position();
            GuhrioSpel.actie(p, GuhrioPayloads.Actie.GERAAKT, BlockPos.ZERO, guhmba.getId());
            helper.assertTrue(p.position().equals(hier), "a flat Guhmba does nothing to you");
        });
        helper.runAtTickTime(5 + GuhmbaEntity.PLAT_TICKS + 5, () -> {
            helper.assertTrue(!guhmba.plat() && guhmba.isAlive(), "it pops back up");
            s.veilig = 0;
            zet(helper, p, 10.5, 2);
            GuhrioSpel.actie(p, GuhrioPayloads.Actie.GERAAKT, BlockPos.ZERO, guhmba.getId());
            near(helper, p.getX(), startAbs.getX() + 0.5, "touched from the side: back at your flag");
            helper.assertTrue(p.getHealth() == leven && GuhrioSpel.sessie(p) == s, "not hurt, still playing");
            weg(helper, p, ver);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the pipe and the flagpole
    // =====================================================================================================================

    /** Ducking on a pipe's mouth takes you to the other mouth with the same channel; in between the pipes don't stop you. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void pijpBrengtJeVerder(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockState mond = GuhrioFeature.PIJP.get().defaultBlockState().setValue(GuhrioBlocks.PijpBlok.KANAAL, 3);
        BlockPos van = new BlockPos(6, 3, Z), naar = new BlockPos(15, 3, Z), ander = new BlockPos(11, 3, Z);
        for (BlockPos m : new BlockPos[]{van, naar, ander}) {
            helper.setBlock(m.below(), GuhrioFeature.PIJP_LIJF.get());
        }
        helper.setBlock(van, mond);
        helper.setBlock(naar, mond);
        helper.setBlock(ander, mond.setValue(GuhrioBlocks.PijpBlok.KANAAL, 4));      // another pipe: not this one's
        ServerLevel level = helper.getLevel();
        ServerPlayer p = start(helper, startAbs), q = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        BlockPos vanAbs = helper.absolutePos(van), naarAbs = helper.absolutePos(naar);
        // not standing on it: no
        zet(helper, p, 4.5, 2);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, vanAbs, 0);
        helper.assertFalse(s.inPijp(), "you must stand on the mouth");
        // a pipe without a partner: no
        zet(helper, p, 11.5, 4);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, helper.absolutePos(ander), 0);
        helper.assertFalse(s.inPijp(), "a pipe with nobody on its channel goes nowhere");
        zet(helper, p, 6.5, 4);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, vanAbs, 0);
        helper.assertTrue(s.inPijp() && GuhrioSpel.inPijp(p), "into the pipe");
        BlockState state = level.getBlockState(vanAbs);
        helper.assertTrue(state.getCollisionShape(level, vanAbs, CollisionContext.of(p)).isEmpty()
                && level.getBlockState(vanAbs.below()).getCollisionShape(level, vanAbs.below(), CollisionContext.of(p)).isEmpty(), "the pipe lets you through");
        helper.assertFalse(state.getCollisionShape(level, vanAbs, CollisionContext.of(q)).isEmpty(), "but not somebody who is not in it");
        for (int i = 0; i < GuhrioSpel.PIJP_TICKS; i++) {
            GuhrioSpel.tick(p);
        }
        near(helper, p.getX(), naarAbs.getX() + 0.5, "out at the other mouth");
        near(helper, p.getY(), naarAbs.getY() + 1 - GuhrioSpel.PIJP_DIEP, "deep in it (your game lets you rise out)");
        helper.assertTrue(GuhrioSpel.sessie(p) == s && s.inPijp(), "still in the level, still in the pipe while you rise");
        zet(helper, p, 15.5, 4);                                 // (the player's game has risen out)
        for (int i = 0; i < GuhrioSpel.PIJP_TICKS + 12; i++) {
            GuhrioSpel.tick(p);
        }
        helper.assertTrue(!s.inPijp() && GuhrioSpel.sessie(p) == s, "and out of it: the pipes are solid again");
        helper.assertFalse(state.getCollisionShape(level, vanAbs, CollisionContext.of(p)).isEmpty(), "solid for you too");
        weg(helper, p, q);
        helper.succeed();
    }

    /** W in a door takes you to the other door with the same channel, from either half of it. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void deurBrengtJeVerder(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockState onder = GuhrioFeature.DEUR.get().defaultBlockState().setValue(GuhrioBlocks.PijpBlok.KANAAL, 7);
        BlockState boven = onder.setValue(GuhrioBlocks.DeurBlok.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
        BlockPos van = new BlockPos(6, 2, Z), naar = new BlockPos(17, 2, Z);
        for (BlockPos d : new BlockPos[]{van, naar}) {
            helper.setBlock(d, onder);
            helper.setBlock(d.above(), boven);
        }
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        BlockPos vanAbs = helper.absolutePos(van), naarAbs = helper.absolutePos(naar);
        zet(helper, p, 9.5, 2);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DEUR, vanAbs, 0);
        near(helper, p.getX(), helper.absoluteVec(new Vec3(9.5, 0, 0)).x, "you must stand in the door");
        zet(helper, p, 6.5, 2);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DEUR, vanAbs.above(), 0);     // (the upper half works too)
        near(helper, p.getX(), naarAbs.getX() + 0.5, "through the door");
        near(helper, p.getY(), naarAbs.getY(), "standing in the other one");
        GuhrioSpel.tick(p);
        helper.assertTrue(GuhrioSpel.sessie(p) == s && !s.inPijp(), "still in the level");
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DEUR, naarAbs, 0);
        near(helper, p.getX(), vanAbs.getX() + 0.5, "and back");
        weg(helper, p);
        helper.succeed();
    }

    /** The flagpole ends the level: your time is kept, a few seconds later you are let go at the exit, back to normal. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void mastMaaktHetLevelAf(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        for (int y = 2; y <= 5; y++) {
            helper.setBlock(new BlockPos(16, y, Z), GuhrioFeature.MAST.get().defaultBlockState().setValue(GuhrioBlocks.MastBlok.TOP, y == 5));
        }
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        GuhrioSpel.spaar(p).remove("Tijden");
        for (int i = 0; i < 37; i++) {
            GuhrioSpel.tick(p);
        }
        zet(helper, p, 16.5, 4);                                 // jumped against the pole
        GuhrioSpel.tick(p);
        helper.assertTrue(s.klaar(), "the pole: done");
        helper.assertTrue(GuhrioSpel.besteTijd(p, LEVEL) == 38, "your time is kept: " + GuhrioSpel.besteTijd(p, LEVEL));
        zet(helper, p, 16.5, 2);
        for (int i = 0; i < GuhrioSpel.KLAAR_TICKS - 1; i++) {
            GuhrioSpel.tick(p);
        }
        helper.assertTrue(GuhrioSpel.sessie(p) == s, "a few seconds at the pole");
        GuhrioSpel.tick(p);
        helper.assertTrue(GuhrioSpel.sessie(p) == null, "then you are let go");
        near(helper, p.getX(), startAbs.getX() + 18 + 0.5, "at the level's exit");
        helper.assertFalse(p.getAttribute(Attributes.JUMP_STRENGTH).hasModifier(GuhrioSpel.SPRONG), "back to normal");
        // a slower run does not replace the time
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "again");
        GuhrioSpel.Sessie s2 = GuhrioSpel.sessie(p);
        for (int i = 0; i < 60; i++) {
            GuhrioSpel.tick(p);
        }
        zet(helper, p, 16.5, 2);
        GuhrioSpel.tick(p);
        helper.assertTrue(s2.klaar() && GuhrioSpel.besteTijd(p, LEVEL) == 38, "the best time stays");
        weg(helper, p);
        helper.succeed();
    }
}
