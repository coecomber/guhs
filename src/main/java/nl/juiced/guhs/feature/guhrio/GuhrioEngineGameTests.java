package nl.juiced.guhs.feature.guhrio;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the full Super Guhrio engine (batch "guhrio"; run with
 * {@code -Pgt=GuhrioGameTests,GuhrioEngineGameTests}): what the engine slice added to the prototype. The server side only
 * (mock players): coins that only count once, the big vadsmunten, hidden blocks, switches, the Vuurpeper and its knabbels,
 * the Schild-Mika, the Plof-Mika, the Hapbloem, the grill spit, platforms and falling blocks, pipes in every direction,
 * Guhshi, the gate, the castle's order of levels / questline / whole-castle time, a cutscene that wins from a level, and
 * the files the generators wrote (the little level made with the lane builder, the castle's seven levels and its tiles).
 * <p>
 * Like {@link GuhrioGameTests} the tests build a little lane in the empty room guhrio_test_baan (ground along z = 3, you
 * walk at y = 2, the start block at (2, 2, 3) facing east) and call {@link GuhrioSpel#tick} / {@link GuhrioSpel#actie}
 * themselves.
 */
public class GuhrioEngineGameTests {
    private static final String BATCH = "guhrio", KAMER = "guhrio_test_baan", LEVEL = "guhrio_enginetest", SLOT = "guhrio_test_slot";
    private static final BlockPos START = new BlockPos(2, 2, 3);
    private static final int Z = 3;

    static {
        GuhrioLevel.zet(new GuhrioLevel(LEVEL, "E-1", List.of(new GuhrioLevel.BaanDef("test", List.of(new BlockPos(-1, 0, 0), new BlockPos(19, 0, 0)),
                true, 13, 6, -3, 8)), new BlockPos(18, 0, 0), new BlockPos(-1, 0, 0), null));
    }

    private static BlockPos bouw(GameTestHelper helper) {
        for (int x = 1; x <= 21; x++) {
            helper.setBlock(new BlockPos(x, 1, Z), GuhrioFeature.GROND.get());
        }
        return startblok(helper, START, LEVEL);
    }

    private static BlockPos startblok(GameTestHelper helper, BlockPos pos, String level) {
        helper.setBlock(pos, GuhrioFeature.STARTBLOK.get().defaultBlockState().setValue(GuhrioBlocks.StartBlok.FACING, Direction.EAST));
        BlockPos abs = helper.absolutePos(pos);
        ((GuhrioBlocks.StartBlockEntity) helper.getLevel().getBlockEntity(abs)).zetLevel(level);
        return abs;
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

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

    private static void zet(GameTestHelper helper, ServerPlayer p, double x, double y) {
        Vec3 v = helper.absoluteVec(new Vec3(x, y, Z + 0.5));
        p.snapTo(v.x, v.y, v.z);
    }

    private static void near(GameTestHelper helper, double a, double b, String wat) {
        helper.assertTrue(Math.abs(a - b) < 1e-6, wat + ": " + a + " (expected " + b + ")");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> T wezen(GameTestHelper helper, BlockPos startAbs, BlockPos plek) {
        return (T) GuhrioSpel.actief(helper.getLevel(), startAbs).wezens.get(helper.absolutePos(plek));
    }

    // =====================================================================================================================
    // coins, vadsmunten, hidden blocks, switches
    // =====================================================================================================================

    /** A level's coin goes into your pocket the first time only; a later run shows it again but it is worth nothing more. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void engineMuntenZonderBoeren(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos munt = new BlockPos(6, 3, Z), vraag = new BlockPos(9, 5, Z);
        helper.setBlock(munt, GuhrioFeature.MUNT.get());
        helper.setBlock(vraag, GuhrioFeature.VRAAGBLOK.get());
        ServerPlayer p = start(helper, startAbs), q = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        zet(helper, p, 6.5, 2);
        GuhrioSpel.tick(p);
        zet(helper, p, 9.5, 2);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.BOTS, helper.absolutePos(vraag), 0);
        helper.assertTrue(s.munten == 2 && GuhrioSpel.munten(p) == 2 && GuhrioKasteel.muntenOoit(p) == 2, "two coins: on the panel and in the pocket");
        GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
        // again: the coins are back in the level, the pocket does not grow
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "a second run");
        s = GuhrioSpel.sessie(p);
        helper.assertTrue(s.staat(helper.absolutePos(munt)) == 0, "the coin is there again");
        zet(helper, p, 6.5, 2);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.munten == 1 && GuhrioSpel.munten(p) == 2, "taken again: on the panel, not in the pocket");
        // somebody else still gets theirs; and the shop takes coins out of the pocket
        zet(helper, q, 6.5, 2);
        GuhrioSpel.tick(q);
        helper.assertTrue(GuhrioSpel.munten(q) == 1, "every player has their own first time");
        helper.assertFalse(GuhrioKasteel.betaal(p, 3), "you can't pay more than you have");
        helper.assertTrue(GuhrioKasteel.betaal(p, 2) && GuhrioSpel.munten(p) == 0 && GuhrioKasteel.muntenOoit(p) == 2, "paid");
        // bonus coins of a level's own are for the panel only
        GuhrioSpel.munt(p, s, 5);
        helper.assertTrue(s.munten == 6 && GuhrioSpel.munten(p) == 0, "munt(): the panel only");
        weg(helper, p, q);
        helper.succeed();
    }

    /** A big vadsmunt is yours for good: the next run shows only its shadow; the castle counts them per level. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void engineVadsmuntBlijft(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockState vads = GuhrioFeature.VADSMUNT.get().defaultBlockState();
        BlockPos een = new BlockPos(6, 2, Z), twee = new BlockPos(9, 2, Z);
        helper.setBlock(een, vads.setValue(GuhrioStukken.VadsmuntBlok.NUMMER, 0));
        helper.setBlock(twee, vads.setValue(GuhrioStukken.VadsmuntBlok.NUMMER, 2));
        ServerPlayer p = start(helper, startAbs), q = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        zet(helper, p, 9.5, 2);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.vads == 0b100 && s.staat(helper.absolutePos(twee)) == 1, "vadsmunt 2 taken");
        helper.assertTrue(GuhrioKasteel.vadsmunten(p, LEVEL) == 0b100 && GuhrioKasteel.vadsmunten(q, LEVEL) == 0, "kept per player");
        GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
        helper.assertTrue(GuhrioSpel.start(p, startAbs), "again");
        s = GuhrioSpel.sessie(p);
        helper.assertTrue(s.staat(helper.absolutePos(twee)) == 2 && s.vads == 0b100, "the next run: a shadow, still counted");
        zet(helper, p, 9.5, 2);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.staat(helper.absolutePos(twee)) == 2, "a shadow can't be taken");
        zet(helper, p, 6.5, 2);
        GuhrioSpel.tick(p);
        helper.assertTrue(GuhrioKasteel.vadsmunten(p, LEVEL) == 0b101, "the other one too");
        helper.assertTrue(GuhrioKasteel.alleVadsmunten(p) == 0, "(only the castle's own levels count for the eighteen)");
        weg(helper, p, q);
        helper.succeed();
    }

    /** A hidden block is not there until your head finds it; then it is solid for you alone and gave its coin once. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void engineOnzichtbaarBlok(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos blok = new BlockPos(7, 5, Z);
        helper.setBlock(blok, GuhrioFeature.ONZICHTBAAR.get());
        BlockPos abs = helper.absolutePos(blok);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = start(helper, startAbs), q = start(helper, startAbs);
        BlockState state = level.getBlockState(abs);
        helper.assertTrue(state.getCollisionShape(level, abs, CollisionContext.of(p)).isEmpty()
                && state.getCollisionShape(level, abs, CollisionContext.empty()).isEmpty(), "not there for anybody");
        zet(helper, p, 7.5, 2);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.BOTS, abs, 0);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.BOTS, abs, 0);
        helper.assertTrue(GuhrioSpel.sessie(p).munten == 1, "found: its coin, once");
        helper.assertFalse(state.getCollisionShape(level, abs, CollisionContext.of(p)).isEmpty(), "solid for the finder");
        helper.assertTrue(state.getCollisionShape(level, abs, CollisionContext.of(q)).isEmpty(), "still hidden for the other player");
        weg(helper, p, q);
        helper.succeed();
    }

    /** A switch works per player: blue blocks are solid while its channel is on, red ones while it is off; a timed one ticks back. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void engineSchakelaar(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockState schakelaar = GuhrioFeature.SCHAKELAAR.get().defaultBlockState().setValue(GuhrioStukken.KANAAL, 3);
        BlockState blok = GuhrioFeature.SCHAKELBLOK.get().defaultBlockState().setValue(GuhrioStukken.KANAAL, 3);
        BlockPos wissel = new BlockPos(6, 5, Z), tijd = new BlockPos(8, 5, Z), blauw = new BlockPos(12, 2, Z), rood = new BlockPos(14, 2, Z);
        helper.setBlock(wissel, schakelaar);
        helper.setBlock(tijd, schakelaar.setValue(GuhrioStukken.SchakelaarBlok.SOORT, GuhrioStukken.Schakel.TIJD).setValue(GuhrioStukken.KANAAL, 5));
        helper.setBlock(blauw, blok);
        helper.setBlock(rood, blok.setValue(GuhrioStukken.SchakelBlok.AAN, false));
        ServerLevel level = helper.getLevel();
        ServerPlayer p = start(helper, startAbs), q = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        BlockPos blauwAbs = helper.absolutePos(blauw), roodAbs = helper.absolutePos(rood);
        helper.assertTrue(level.getBlockState(blauwAbs).getCollisionShape(level, blauwAbs, CollisionContext.of(p)).isEmpty()
                && !level.getBlockState(roodAbs).getCollisionShape(level, roodAbs, CollisionContext.of(p)).isEmpty(), "off: blue open, red solid");
        zet(helper, p, 6.5, 2);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.BOTS, helper.absolutePos(wissel), 0);
        helper.assertTrue(GuhrioSpel.kanaal(p, 3) && !GuhrioSpel.kanaal(q, 3), "on, for the one who hit it");
        helper.assertTrue(!level.getBlockState(blauwAbs).getCollisionShape(level, blauwAbs, CollisionContext.of(p)).isEmpty()
                && level.getBlockState(roodAbs).getCollisionShape(level, roodAbs, CollisionContext.of(p)).isEmpty(), "on: blue solid, red open");
        helper.assertTrue(level.getBlockState(blauwAbs).getCollisionShape(level, blauwAbs, CollisionContext.of(q)).isEmpty(), "not for the other player");
        helper.assertFalse(level.getBlockState(roodAbs).getCollisionShape(level, roodAbs, CollisionContext.empty()).isEmpty(), "creatures see every channel as off");
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.BOTS, helper.absolutePos(wissel), 0);
        helper.assertTrue(GuhrioSpel.kanaal(p, 3), "it does not listen twice in a row");
        for (int i = 0; i < GuhrioStukken.SchakelaarBlok.RUST + 1; i++) {
            GuhrioSpel.tick(p);
        }
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.STAP, helper.absolutePos(wissel), 0);
        helper.assertFalse(GuhrioSpel.kanaal(p, 3), "hit again (stood on): off");
        // the timed one
        zet(helper, p, 8.5, 2);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.BOTS, helper.absolutePos(tijd), 0);
        helper.assertTrue(GuhrioSpel.kanaal(p, 5), "the timed switch: on");
        for (int i = 0; i < GuhrioSpel.SCHAKEL_TICKS + 1; i++) {
            GuhrioSpel.tick(p);
        }
        helper.assertFalse(GuhrioSpel.kanaal(p, 5), "and off again by itself");
        helper.assertTrue(GuhrioSpel.sessie(p) == s, "still playing");
        weg(helper, p, q);
        helper.succeed();
    }

    // =====================================================================================================================
    // the Vuurpeper and the creatures
    // =====================================================================================================================

    /** The Vuurpeper: big, and a mouse button throws a knabbel (two at most) that flattens the Guhmba it meets. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 2000)
    public static void engineVuurpeperGooit(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos vraag = new BlockPos(5, 5, Z), plek = new BlockPos(12, 2, Z);
        helper.setBlock(vraag, GuhrioFeature.VRAAGBLOK.get().defaultBlockState().setValue(GuhrioBlocks.INHOUD, GuhrioBlocks.Inhoud.VUURPEPER));
        helper.setBlock(new BlockPos(8, 2, Z), GuhrioFeature.BLOK.get());         // two walls: the Guhmba walks between them
        helper.setBlock(new BlockPos(16, 2, Z), GuhrioFeature.BLOK.get());
        helper.setBlock(plek, GuhrioFeature.GUHMBA_PLEK.get());
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        zet(helper, p, 5.5, 2);
        helper.assertFalse(GuhrioSpel.gooi(p, s, 1), "no knabbels without the Vuurpeper");
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.BOTS, helper.absolutePos(vraag), 0);
        helper.assertTrue(s.kracht == GuhrioSpel.Kracht.VUUR && s.kracht.groot(), "the Vuurpeper: big and fiery");
        GuhmbaEntity guhmba = wezen(helper, startAbs, plek);
        zet(helper, p, 9.5, 2);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.GOOI, p.blockPosition(), 1);
        helper.assertTrue(s.actief.los.size() == 1 && s.actief.los.get(0) instanceof KnabbelEntity, "a knabbel flies");
        helper.assertFalse(GuhrioSpel.gooi(p, s, 1), "not two in the same breath");
        helper.succeedWhen(() -> {
            helper.assertTrue(guhmba.plat(), "the knabbel flattens the Guhmba");
            helper.assertTrue(s.actief.los.isEmpty() || s.actief.los.stream().allMatch(Entity::isRemoved), "and is gone");
            for (int i = 0; i < GuhrioSpel.GOOI_RUST; i++) {
                GuhrioSpel.tick(p);
            }
            helper.assertTrue(GuhrioSpel.gooi(p, s, -1), "your hands are free for the next one");
            weg(helper, p);
        });
    }

    /** The Schild-Mika: landed on = into its shell; touched = the shell slides off, flattens Guhmba's and flips a switch for the kicker. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 2000)
    public static void engineSchildMika(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos plek = new BlockPos(8, 2, Z), guhmbaPlek = new BlockPos(13, 2, Z), schakelaar = new BlockPos(18, 2, Z);
        helper.setBlock(new BlockPos(5, 2, Z), GuhrioFeature.BLOK.get());
        helper.setBlock(plek, GuhrioFeature.SCHILD_MIKA_PLEK.get());
        helper.setBlock(guhmbaPlek, GuhrioFeature.GUHMBA_PLEK.get());
        helper.setBlock(schakelaar, GuhrioFeature.SCHAKELAAR.get().defaultBlockState().setValue(GuhrioStukken.KANAAL, 2)
                .setValue(GuhrioStukken.SchakelaarBlok.SOORT, GuhrioStukken.Schakel.AAN));
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        SchildMikaEntity mika = wezen(helper, startAbs, plek);
        GuhmbaEntity guhmba = wezen(helper, startAbs, guhmbaPlek);
        helper.assertTrue(mika != null && guhmba != null && mika.stand() == SchildMikaEntity.LOOPT && mika.gevaarlijk(), "a Schild-Mika, walking");
        helper.runAtTickTime(5, () -> {
            p.snapTo(mika.getX(), mika.getY() + 1, mika.getZ());
            GuhrioSpel.actie(p, GuhrioPayloads.Actie.STAMP, BlockPos.ZERO, mika.getId());
            helper.assertTrue(mika.stand() == SchildMikaEntity.SCHILD && !mika.gevaarlijk() && mika.aanraakbaar(), "landed on: into its shell");
            // touched from the west side: it slides east
            p.snapTo(mika.getX() - 0.7, mika.getY(), mika.getZ());
            Vec3 hier = p.position();
            GuhrioSpel.actie(p, GuhrioPayloads.Actie.GERAAKT, BlockPos.ZERO, mika.getId());
            helper.assertTrue(mika.stand() == SchildMikaEntity.GLIJDT && mika.teken() == 1 && p.position().equals(hier), "kicked: it slides away from you");
            helper.assertFalse(mika.gevaarlijk(), "and does not catch the kicker at once");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(guhmba.plat() || !guhmba.isAlive(), "the sliding shell bowls the Guhmba over");
            helper.assertTrue(GuhrioSpel.kanaal(p, 2), "and flips the switch it bumps into, for whoever kicked it");
            helper.assertTrue(GuhrioSpel.sessie(p) == s, "the kicker is still playing");
            weg(helper, p);
        });
    }

    /** The Plof-Mika drops when a player of the level passes under it, lies still, floats back up; a touch sends you back. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 2000)
    public static void enginePlofMika(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos plek = new BlockPos(10, 6, Z);
        helper.setBlock(plek, GuhrioFeature.PLOF_MIKA_PLEK.get());
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        PlofMikaEntity plof = wezen(helper, startAbs, plek);
        helper.assertTrue(plof != null && plof.stand() == PlofMikaEntity.HANGT && plof.gevaarlijk() && !plof.stampbaar(), "it hangs there, scowling");
        double thuis = plof.getY();
        boolean[] gevallen = {false};
        helper.runAtTickTime(PlofMikaEntity.RUST_TICKS + 10, () -> {
            helper.assertTrue(plof.stand() == PlofMikaEntity.HANGT && Math.abs(plof.getY() - thuis) < 1e-6, "nobody under it: it stays up");
            zet(helper, p, 10.5, 2);
        });
        helper.onEachTick(() -> {
            if (plof.stand() == PlofMikaEntity.LIGT && !gevallen[0]) {
                gevallen[0] = true;
                helper.assertTrue(plof.getY() < thuis - 3.5, "plof: it lies on the ground: " + plof.getY());
                zet(helper, p, 16.5, 2);                         // (step away, and touch it)
                s.veilig = 0;
                p.snapTo(plof.getX() + 1.0, plof.getY(), plof.getZ());
                GuhrioSpel.actie(p, GuhrioPayloads.Actie.GERAAKT, BlockPos.ZERO, plof.getId());
                near(helper, p.getX(), startAbs.getX() + 0.5, "touching it sends you back to your flag");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(gevallen[0] && plof.stand() == PlofMikaEntity.HANGT && Math.abs(plof.getY() - thuis) < 1e-6, "it dropped and floated back up");
            weg(helper, p);
        });
    }

    /** The Hapbloem comes out of its pipe and goes back in; while it is out the pipe is shut and a touch is a kiss. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 2000)
    public static void engineHapbloem(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockState mond = GuhrioFeature.PIJP.get().defaultBlockState().setValue(GuhrioBlocks.PijpBlok.KANAAL, 6);
        BlockPos pijp = new BlockPos(8, 3, Z), ander = new BlockPos(16, 3, Z), plek = pijp.above();
        for (BlockPos m : new BlockPos[]{pijp, ander}) {
            helper.setBlock(m.below(), GuhrioFeature.PIJP_LIJF.get());
            helper.setBlock(m, mond);
        }
        helper.setBlock(plek, GuhrioFeature.HAPBLOEM_PLEK.get());
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        HapbloemEntity bloem = wezen(helper, startAbs, plek);
        helper.assertTrue(bloem != null && !bloem.buiten() && !bloem.gevaarlijk(), "a Hapbloem, down in its pipe");
        boolean[] gekust = {false};
        helper.onEachTick(() -> {
            if (bloem.uit() >= 1f && !gekust[0]) {
                gekust[0] = true;
                // out: the pipe is shut, a touch sends you back
                zet(helper, p, 8.5, 4);
                GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, helper.absolutePos(pijp), 0);
                helper.assertFalse(s.inPijp(), "not into a pipe whose flower is out");
                s.veilig = 0;
                GuhrioSpel.actie(p, GuhrioPayloads.Actie.GERAAKT, BlockPos.ZERO, bloem.getId());
                near(helper, p.getX(), startAbs.getX() + 0.5, "a kiss: back at your flag");
                // a shell or a knabbel scares it down
                helper.assertTrue(bloem.schild(null) && !bloem.buiten(), "scared off: down at once");
                zet(helper, p, 8.5, 4);
                GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, helper.absolutePos(pijp), 0);
                helper.assertTrue(s.inPijp(), "and now the pipe works");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(gekust[0], "the flower came out");
            weg(helper, p);
        });
    }

    /** The grill spit: a skewer that turns with the world's clock; what it touches is worked out from its angle, not its box. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void engineGrillspies(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos naaf = new BlockPos(10, 6, Z);
        helper.setBlock(naaf, GuhrioFeature.GRILLSPIES_PLEK.get().defaultBlockState().setValue(GuhrioStukken.GrillspiesPlek.LENGTE, 3));
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        GrillspiesEntity spies = wezen(helper, startAbs, naaf);
        helper.assertTrue(spies != null && spies.lengte() == 3 && spies.langs() == Direction.EAST, "a spit of three blocks on its hub");
        Vec3 midden = Vec3.atCenterOf(helper.absolutePos(naaf));
        helper.assertTrue(spies.naaf().distanceTo(midden) < 1e-6, "its hub is the middle of the block: " + spies.naaf());
        helper.assertTrue(Math.abs(spies.getBbHeight() - 7) < 1e-6 && Math.abs(spies.getBbWidth() - 7) < 1e-6, "its box covers the whole circle");
        // the tip is three blocks from the hub, in the plane of the lane
        Vec3 punt = spies.punt(3, 0f);
        helper.assertTrue(Math.abs(punt.distanceTo(midden) - 3) < 1e-6 && Math.abs(punt.z - midden.z) < 1e-6, "the tip turns in the lane's plane: " + punt);
        double a = Math.toRadians(spies.hoek(1f));
        Vec3 opSpies = new Vec3(midden.x + Math.cos(a) * 2, midden.y + Math.sin(a) * 2, midden.z);
        Vec3 ernaast = new Vec3(midden.x - Math.cos(a) * 2, midden.y - Math.sin(a) * 2, midden.z);
        helper.assertTrue(spies.raaktVak(AABB.ofSize(opSpies, 0.6, 0.6, 0.6)), "a box on the skewer is touched");
        helper.assertFalse(spies.raaktVak(AABB.ofSize(ernaast, 0.6, 0.6, 0.6)), "a box on the other side of the hub is not");
        // the server believes a touch only when the player is really near the skewer
        p.snapTo(opSpies.x, opSpies.y - 0.9, opSpies.z);
        s.veilig = 0;
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.GERAAKT, BlockPos.ZERO, spies.getId());
        near(helper, p.getX(), startAbs.getX() + 0.5, "touched by the skewer: back at your flag");
        weg(helper, p);
        helper.succeed();
    }

    /** A platform is where the clock says (the same on both sides); a falling block drops once somebody stands on it and comes back. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 2000)
    public static void enginePlatformEnValblok(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos plek = new BlockPos(6, 4, Z), val = new BlockPos(14, 4, Z);
        helper.setBlock(plek, GuhrioFeature.PLATFORM_PLEK.get().defaultBlockState().setValue(GuhrioStukken.PlatformPlek.AFSTAND, 4)
                .setValue(GuhrioStukken.BREED, 3));
        helper.setBlock(val, GuhrioFeature.VALBLOK_PLEK.get().defaultBlockState().setValue(GuhrioStukken.BREED, 2));
        ServerPlayer p = start(helper, startAbs);
        PlatformEntity platform = wezen(helper, startAbs, plek);
        ValblokEntity blok = wezen(helper, startAbs, val);
        helper.assertTrue(platform != null && blok != null && platform.draagt() && platform.canBeCollidedWith(p) && blok.draagt(), "a platform and a falling block");
        BlockPos abs = helper.absolutePos(plek);
        // its rest and its far end: 4 blocks along the lane, the deck level with the top of its row
        long ronde = Math.round(2 * 4 / PlatformEntity.TRAAG);
        Vec3 rust = platform.plek(0), ver = platform.plek(ronde / 2);
        near(helper, rust.x, abs.getX() + 0.5 + 1, "three wide: its middle is one block further");
        near(helper, rust.y, abs.getY() + 1 - PlatformEntity.DIK, "the deck's top is the top of its row");
        near(helper, ver.x - rust.x, 4, "it glides four blocks along the lane");
        near(helper, ver.z, rust.z, "on the lane's line");
        helper.assertTrue(Math.abs(platform.getBbWidth() - 3) < 1e-6, "its box is as wide as it is");
        Vec3 nu = platform.plek(helper.getLevel().getGameTime());
        helper.assertTrue(platform.position().distanceTo(nu) < 0.3, "and it is where the clock says: " + platform.position() + " / " + nu);
        // the falling block
        double rustY = blok.getY();
        helper.assertTrue(blok.stand() == ValblokEntity.RUST, "the block rests");
        boolean[] viel = {false};
        helper.runAtTickTime(10, () -> p.snapTo(blok.getX(), blok.getY() + ValblokEntity.DIK, blok.getZ()));
        helper.onEachTick(() -> {
            if (blok.stand() == ValblokEntity.VALT && blok.getY() < rustY - 1) {
                viel[0] = true;
                zet(helper, p, 3.5, 2);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(viel[0] && blok.stand() == ValblokEntity.RUST && !blok.weg() && Math.abs(blok.getY() - rustY) < 1e-6,
                    "stood on: it dropped, and it is back on its spot");
            weg(helper, p);
        });
    }

    // =====================================================================================================================
    // pipes in every direction
    // =====================================================================================================================

    /** A sideways pipe is entered by walking into its mouth; a one-way mouth and a hanging one only let you out. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void enginePijpenAlleKanten(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockState mond = GuhrioFeature.PIJP.get().defaultBlockState().setValue(GuhrioBlocks.PijpBlok.KANAAL, 9);
        // a sideways mouth at x = 8 that looks west (you walk east into it), its body behind it; a hanging mouth at x = 15
        BlockPos zij = new BlockPos(8, 2, Z), hang = new BlockPos(15, 6, Z);
        for (int dy = 0; dy < 2; dy++) {
            helper.setBlock(zij.above(dy), mond.setValue(GuhrioBlocks.PijpBlok.FACING, Direction.WEST).setValue(GuhrioBlocks.PijpBlok.BOVEN, dy == 1));
            helper.setBlock(zij.above(dy).east(), GuhrioFeature.PIJP_LIJF.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X));
        }
        helper.setBlock(hang, mond.setValue(GuhrioBlocks.PijpBlok.FACING, Direction.DOWN).setValue(GuhrioBlocks.PijpBlok.INGANG, false));
        ServerLevel level = helper.getLevel();
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        BlockPos zijAbs = helper.absolutePos(zij), hangAbs = helper.absolutePos(hang);
        BlockState zijState = level.getBlockState(zijAbs);
        near(helper, GuhrioBlocks.PijpBlok.buiten(level, zijAbs, zijState).x, zijAbs.getX() - 0.5, "you stand in front of a sideways mouth");
        near(helper, GuhrioBlocks.PijpBlok.binnen(level, zijAbs, zijState).x, zijAbs.getX() - 0.5 + GuhrioBlocks.PijpBlok.ZIJ_DIEP, "and go into it sideways");
        helper.assertTrue(GuhrioBlocks.PijpBlok.andere(s.actief, zijAbs, zijState).equals(hangAbs), "the upper block of the mouth is not a mouth of its own");
        // the hanging mouth is a way out only
        zet(helper, p, 15.5, 4);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, hangAbs, 0);
        helper.assertFalse(s.inPijp(), "a hanging one-way mouth does not take you");
        // on top of the sideways pipe: no; in front of it: yes (also when the game reports the upper block)
        zet(helper, p, 8.5, 4);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, zijAbs, 0);
        helper.assertFalse(s.inPijp(), "you must stand in front of the mouth");
        zet(helper, p, 7.5, 2);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, zijAbs.above(), 0);
        helper.assertTrue(s.inPijp(), "walked into the mouth: in");
        for (int i = 0; i < GuhrioSpel.PIJP_TICKS; i++) {
            GuhrioSpel.tick(p);
        }
        near(helper, p.getX(), hangAbs.getX() + 0.5, "you are in the hanging mouth");
        near(helper, p.getY(), hangAbs.getY(), "inside it (your game lets you drop out)");
        helper.assertTrue(GuhrioSpel.sessie(p) == s, "still in the level");
        // turned with the building: the mouth turns too
        helper.assertTrue(zijState.rotate(Rotation.CLOCKWISE_90).getValue(GuhrioBlocks.PijpBlok.FACING) == Direction.NORTH, "a turned template turns its pipes");
        weg(helper, p);
        helper.succeed();
    }

    // =====================================================================================================================
    // Guhshi
    // =====================================================================================================================

    /** Guhshi only carries whoever found his egg; a touch costs Guhshi first; his tongue eats a Guhmba and a coin. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 2000)
    public static void engineGuhshi(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos ei = new BlockPos(5, 2, Z), plek = new BlockPos(7, 2, Z), guhmbaPlek = new BlockPos(11, 2, Z), munt = new BlockPos(16, 3, Z);
        helper.setBlock(ei, GuhrioFeature.GUHSHI_EI.get());
        helper.setBlock(plek, GuhrioFeature.GUHSHI_PLEK.get());
        helper.setBlock(new BlockPos(10, 2, Z), GuhrioFeature.BLOK.get());
        helper.setBlock(new BlockPos(12, 2, Z), GuhrioFeature.BLOK.get());
        helper.setBlock(guhmbaPlek, GuhrioFeature.GUHMBA_PLEK.get());
        helper.setBlock(munt, GuhrioFeature.MUNT.get());
        ServerLevel level = helper.getLevel();
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        zet(helper, p, 7.5, 2);
        GuhrioSpel.tick(p);
        helper.assertFalse(s.guhshi, "no egg, no Guhshi");
        zet(helper, p, 5.5, 2);
        GuhrioSpel.tick(p);
        helper.assertTrue(GuhrioKasteel.heeftEi(p) && s.staat(helper.absolutePos(ei)) == 1, "the egg is found, for good");
        zet(helper, p, 7.5, 2);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.guhshi && s.guhshiDier instanceof GuhEntity guh && guh.getVariant() == GuhVariant.GUHSHI && guh.isNoAi(), "Guhshi carries you");
        helper.assertTrue(s.staat(helper.absolutePos(plek)) == 1, "his spot is empty for you");
        Entity guh = s.guhshiDier;
        zet(helper, p, 9.5, 3);
        GuhrioSpel.tick(p);
        helper.assertTrue(guh.position().distanceTo(p.position()) < 1e-6, "he is where you are");
        // the tongue: the Guhmba in its pen is two blocks ahead
        GuhmbaEntity guhmba = wezen(helper, startAbs, guhmbaPlek);
        p.snapTo(guhmba.getX() - 2, guhmba.getY(), guhmba.getZ());
        GuhrioSpel.tick(p);
        helper.assertTrue(GuhrioSpel.tong(p, s, 1) && guhmba.weg() && !guhmba.gevaarlijk(), "the tongue eats the Guhmba (it is gone for a while)");
        helper.assertFalse(GuhrioSpel.tong(p, s, 1), "not twice in the same breath");
        for (int i = 0; i < GuhrioSpel.TONG_RUST; i++) {
            GuhrioSpel.tick(p);
        }
        zet(helper, p, 13.5, 2);
        GuhrioSpel.tick(p);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.GOOI, p.blockPosition(), 1);
        helper.assertTrue(s.munten == 1 && s.staat(helper.absolutePos(munt)) == 1, "and a coin three blocks ahead (the mouse button is the tongue on Guhshi)");
        // a touch costs Guhshi, not your place
        s.veilig = 0;
        Vec3 hier = p.position();
        GuhrioSpel.geraakt(p, s);
        helper.assertTrue(!s.guhshi && guh.isRemoved() && p.position().equals(hier) && s.staat(helper.absolutePos(plek)) == 0,
                "touched: Guhshi is gone (waiting at his spot again), you stay");
        // leaving the level never leaves a Guhshi behind
        GuhrioSpel.zetGuhshi(p, s, true);
        Entity tweede = s.guhshiDier;
        GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
        helper.assertTrue(tweede != null && tweede.isRemoved(), "out of the level: no Guhshi left behind");
        helper.assertTrue(level.getEntitiesOfClass(GuhEntity.class, new AABB(startAbs).inflate(30), g -> g.getPersistentData().contains(GuhrioSpel.GUHSHI_TAG)).isEmpty(),
                "none at all");
        weg(helper, p);
        helper.succeed();
    }

    // =====================================================================================================================
    // the gate, the castle, a cutscene
    // =====================================================================================================================

    /** A gate starts the level of the start block its block entity points at, counted in the gate's own frame; out = at the entrance. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void enginePoortEnIngang(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        BlockPos poort = new BlockPos(20, 6, 5);
        // the gate faces south; the start block is 18 west and 4 down and 2 north of it: in its frame (+x south, +z west) = (-2, -4, 18)
        helper.setBlock(poort, GuhrioFeature.POORT.get().defaultBlockState().setValue(GuhrioBlocks.PoortBlok.FACING, Direction.SOUTH));
        BlockPos poortAbs = helper.absolutePos(poort);
        GuhrioBlocks.StartBlockEntity be = (GuhrioBlocks.StartBlockEntity) helper.getLevel().getBlockEntity(poortAbs);
        be.zetLevel(LEVEL);
        be.zetNaar(new BlockPos(-2, -4, 18));
        helper.assertTrue(startAbs.equals(GuhrioBlocks.PoortBlok.start(helper.getLevel(), poortAbs, helper.getLevel().getBlockState(poortAbs))),
                "the gate knows where its level starts");
        ServerPlayer p = speler(helper);
        helper.getLevel().getBlockState(poortAbs).useWithoutItem(helper.getLevel(), p,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(poortAbs), Direction.UP, poortAbs, false));
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        helper.assertTrue(s != null && s.level().anker().equals(startAbs), "through the gate: in the level");
        near(helper, p.getX(), startAbs.getX() + 0.5, "standing on its start");
        // stepping out (Q twice) puts you at the level's entrance, not in the middle of the lane
        zet(helper, p, 12.5, 2);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.STOP, BlockPos.ZERO, 0);
        helper.assertTrue(GuhrioSpel.sessie(p) == null, "out");
        near(helper, p.getX(), startAbs.getX() - 1 + 0.5, "at the entrance");
        weg(helper, p);
        helper.succeed();
    }

    /**
     * The castle: a level's gate stays shut until the level before it is done, every flagpole moves the questline on, and
     * the six levels in order are the whole-castle time (own best + the Scorebord, so the Guhdex shows the server record).
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void engineKasteelVolgorde(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        BlockPos[] starts = new BlockPos[GuhrioKasteel.LEVELS.size()];
        for (int i = 0; i < starts.length; i++) {
            String id = GuhrioKasteel.LEVELS.get(i);
            helper.setBlock(new BlockPos(2 + i * 3, 1, Z), GuhrioFeature.GROND.get());
            starts[i] = startblok(helper, new BlockPos(2 + i * 3, 2, Z), id);
            GuhrioLevel def = GuhrioLevel.bestand(level.getServer(), id);
            helper.assertTrue(def != null && def.uitgang() != null && def.ingang() != null, "the level file of " + id + " loads");
            helper.assertTrue(i == 0 ? def.na() == null : GuhrioKasteel.LEVELS.get(i - 1).equals(def.na()), "and names the level before it");
            // (for this test a tiny lane of the same name and order: the real lanes are 96 long and would run through the other tests)
            GuhrioLevel.zet(new GuhrioLevel(id, def.wereld(), List.of(new GuhrioLevel.BaanDef("klein", List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0)),
                    true, 11, 6, -1, 3)), null, null, def.na()));
        }
        helper.assertFalse(GuhrioSpel.start(p, starts[1]), "1-2 is shut until 1-1 is done");
        helper.assertTrue(GuhrioKasteel.LIJN.stap(p) == 0, "the questline has not begun");
        int totaal = 0;
        for (int i = 0; i < starts.length; i++) {
            helper.assertTrue(GuhrioSpel.start(p, starts[i]), "into " + GuhrioKasteel.LEVELS.get(i));
            GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
            for (int t = 0; t < 10 + i; t++) {
                GuhrioSpel.tick(p);
            }
            totaal += s.ticks;
            p.snapTo(starts[i].getX() + 0.5, starts[i].getY(), starts[i].getZ() + 0.5);
            GuhrioSpel.klaar(p, s, starts[i]);
            GuhrioSpel.stop(p, GuhrioSpel.Einde.KLAAR);
            helper.assertTrue(GuhrioKasteel.gehaald(p, GuhrioKasteel.LEVELS.get(i)) && GuhrioKasteel.LIJN.stap(p) == i + 2, "done: step " + (i + 2));
            helper.assertTrue(nl.juiced.guhs.quest.Highscores.personalBest(p, nl.juiced.guhs.quest.Highscores.game(GuhrioKasteel.bord(GuhrioKasteel.LEVELS.get(i))))
                    == s.ticks, "its time is on the highscore board");
        }
        int heel = totaal;
        helper.assertTrue(GuhrioKasteel.aantalGehaald(p) == 6 && GuhrioKasteel.kasteelTijd(p) == heel, "the whole castle in one go: " + GuhrioKasteel.kasteelTijd(p));
        helper.assertTrue(nl.juiced.guhs.quest.Scorebord.top(level.getServer(), GuhrioKasteel.BORD_KASTEEL).stream().anyMatch(e -> e.player().equals(p.getUUID())
                && e.score() == heel), "and on the Scorebord (the server record of the Guhdex)");
        // a level out of order does not count for a whole-castle go; walking out of a level costs its time
        helper.assertTrue(GuhrioSpel.start(p, starts[0]), "1-1 again: a new go");
        for (int t = 0; t < 7; t++) {
            GuhrioSpel.tick(p);
        }
        GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
        helper.assertTrue(GuhrioSpel.start(p, starts[2]), "2-1 instead of finishing 1-1");
        helper.assertFalse(GuhrioSpel.sessie(p).loop, "does not count");
        GuhrioSpel.stop(p, GuhrioSpel.Einde.GESTOPT);
        helper.assertTrue(GuhrioKasteel.kasteelTijd(p) == heel, "the best time stays");
        // the duel
        helper.assertFalse(GuhrioKasteel.duelGewonnen(p), "the duel is not won yet");
        int[] gehoord = {0};
        java.util.function.Consumer<ServerPlayer> luisteraar = wie -> gehoord[0]++;
        GuhrioKasteel.BIJ_DUEL.add(luisteraar);
        helper.assertTrue(GuhrioKasteel.winDuel(p) && !GuhrioKasteel.winDuel(p), "won: true the first time only");
        GuhrioKasteel.BIJ_DUEL.remove(luisteraar);
        helper.assertTrue(gehoord[0] == 2 && GuhrioKasteel.duelGewonnen(p) && GuhrioKasteel.LIJN.klaar(p),
                "the reward slice hears of it, the questline is done");
        // a warp room opens a later level
        ServerPlayer q = speler(helper);
        helper.assertFalse(GuhrioSpel.start(q, starts[3]), "2-2 is shut for a new player");
        GuhrioKasteel.ontgrendel(q, GuhrioKasteel.LEVELS.get(3));
        helper.assertTrue(GuhrioSpel.start(q, starts[3]), "until a warp room opens it");
        weg(helper, p, q);
        for (String id : GuhrioKasteel.LEVELS) {
            GuhrioLevel.vergeet(id);
        }
        helper.succeed();
    }

    /** A cutscene always wins from a level: while it plays the level stands still (no time, no falling, no actions). */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 2000)
    public static void engineCutsceneWint(GameTestHelper helper) {
        BlockPos startAbs = bouw(helper);
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        for (int i = 0; i < 5; i++) {
            GuhrioSpel.tick(p);
        }
        nl.juiced.guhs.feature.verhaal.Cutscene scene = nl.juiced.guhs.feature.verhaal.Cutscene.van("verhaal_demo");
        helper.assertTrue(scene != null && nl.juiced.guhs.feature.verhaal.Cutscenes.speel(p, scene, startAbs, Rotation.NONE, null), "a cutscene starts in a level");
        helper.assertTrue(nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(p), "the player watches");
        zet(helper, p, 8.5, -6);                                 // (wherever you are in the scene: even under the lane)
        GuhrioSpel.tick(p);
        GuhrioSpel.tick(p);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.GOOI, p.blockPosition(), 1);
        helper.assertTrue(s.ticks == 5 && GuhrioSpel.sessie(p) == s, "the level waits: no time, no fall, the level goes on afterwards");
        ServerPlayer q = speler(helper);
        helper.assertTrue(nl.juiced.guhs.feature.verhaal.Cutscenes.speel(q, scene, startAbs, Rotation.NONE, null) && !GuhrioSpel.start(q, startAbs),
                "and nobody enters a level while watching");
        zet(helper, p, 8.5, 2);
        // (a mock player has no tick of its own: this is the tick that the story engine and the level both hang on)
        helper.onEachTick(() -> {
            if (!p.isRemoved()) {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertFalse(nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(p), "the scene is over");
            helper.assertTrue(s.ticks > 5 && s.veilig > 0 && GuhrioSpel.sessie(p) == s, "the level goes on, with a safe moment: " + s.ticks);
            weg(helper, p, q);
        });
    }

    // =====================================================================================================================
    // what the generators wrote
    // =====================================================================================================================

    /** The little level the lane builder made: its start block, lanes, pieces and frame are right, and it plays to the flagpole. */
    @GuhTest(template = SLOT, batch = BATCH)
    public static void engineBaanbouwerLevel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // (template (3, 5, 1): the lane's s = 2 at its row 3; a template's y 0 is helper y 1)
        BlockPos start = new BlockPos(3, 6, 1);
        BlockPos startAbs = helper.absolutePos(start);
        helper.assertTrue(level.getBlockState(startAbs).getBlock() instanceof GuhrioBlocks.StartBlok, "the builder put the start block: " + level.getBlockState(startAbs));
        ServerPlayer p = start(helper, startAbs);
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        helper.assertTrue("SLOT".equals(s.level().level().wereld()) && s.level().banen().size() == 1, "its lane file");
        Baan baan = s.lane();
        helper.assertTrue(baan.richting(0) == Direction.EAST && baan.afstand == 11 && Math.abs(baan.lengte() - 29) < 1e-6, "a lane of thirty, the camera 11 away");
        long vads = s.actief.stukken.stream().filter(x -> x.state().getBlock() instanceof GuhrioStukken.VadsmuntBlok).count();
        long pijpen = s.actief.stukken.stream().filter(x -> x.state().getBlock() instanceof GuhrioBlocks.PijpBlok).count();
        helper.assertTrue(vads == 3 && pijpen == 3, "three vadsmunten, an upward mouth and a sideways one of two blocks: " + vads + " / " + pijpen);
        helper.assertTrue(s.actief.wezens.values().stream().anyMatch(e -> e instanceof GuhmbaEntity), "its Guhmba is there");
        // the coin at s = 5: one row above where you walk
        BlockPos munt = startAbs.offset(3, 1, 0);
        p.snapTo(munt.getX() + 0.5, startAbs.getY(), munt.getZ() + 0.5);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.munten == 1, "the coin of the builder is where the builder said");
        // the sideways pipe at s = 24 looks back along the lane (west) and takes you to the upward one at s = 20
        BlockPos zij = startAbs.offset(22, 0, 0);
        helper.assertTrue(level.getBlockState(zij).getBlock() instanceof GuhrioBlocks.PijpBlok
                && level.getBlockState(zij).getValue(GuhrioBlocks.PijpBlok.FACING) == Direction.WEST, "the sideways mouth: " + level.getBlockState(zij));
        p.snapTo(zij.getX() - 0.5, zij.getY(), zij.getZ() + 0.5);
        GuhrioSpel.actie(p, GuhrioPayloads.Actie.DUIK, zij, 0);
        helper.assertTrue(s.inPijp(), "into the sideways pipe");
        for (int i = 0; i < GuhrioSpel.PIJP_TICKS * 2 + 12; i++) {
            GuhrioSpel.tick(p);
        }
        near(helper, p.getX(), startAbs.getX() + 18 + 0.5, "out of the other mouth");
        // the flagpole at s = 27, and the exit the builder wrote (s = 28)
        BlockPos mast = startAbs.offset(25, 0, 0);
        p.snapTo(mast.getX() + 0.5, mast.getY(), mast.getZ() + 0.5);
        GuhrioSpel.tick(p);
        helper.assertTrue(s.klaar(), "the flagpole");
        GuhrioSpel.stop(p, GuhrioSpel.Einde.KLAAR);
        near(helper, p.getX(), startAbs.getX() + 26 + 0.5, "let go at the exit");
        weg(helper, p);
        helper.succeed();
    }

    /**
     * The castle's tiles hold what the level files say: seven start blocks that name their level, two gate blocks for each
     * that point exactly at it, three big vadsmunten per level, and every lane's camera room is empty.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void engineKasteelKlopt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        java.util.Map<String, BlockPos> starts = new java.util.HashMap<>();
        java.util.Map<BlockPos, BlockState> blokken = new java.util.HashMap<>();
        java.util.List<StructureTemplate.StructureBlockInfo> poorten = new java.util.ArrayList<>();
        java.util.Map<String, Integer> vads = new java.util.HashMap<>();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        int tegels = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                StructureTemplate tegel = level.getStructureManager().get(Guhs.id(GuhrioKasteel.STRUCTUUR + "/stuk_" + i + "_" + j)).orElse(null);
                if (tegel == null) {
                    continue;
                }
                tegels++;
                BlockPos hoek = new BlockPos(i * 32, 0, j * 32);
                for (StructureTemplate.StructureBlockInfo info : tegel.filterBlocks(hoek, settings, GuhrioFeature.STARTBLOK.get())) {
                    starts.put(info.nbt().getStringOr("Level", ""), info.pos());
                    blokken.put(info.pos(), info.state());
                }
                poorten.addAll(tegel.filterBlocks(hoek, settings, GuhrioFeature.POORT.get()));
                for (net.minecraft.world.level.block.Block b : new net.minecraft.world.level.block.Block[]{GuhrioFeature.VADSMUNT.get(), GuhrioFeature.MAST.get()}) {
                    for (StructureTemplate.StructureBlockInfo info : tegel.filterBlocks(hoek, settings, b)) {
                        blokken.put(info.pos(), info.state());
                    }
                }
            }
        }
        helper.assertTrue(tegels == 16, "the castle is sixteen tiles: " + tegels);
        List<String> ids = new java.util.ArrayList<>(GuhrioKasteel.LEVELS);
        ids.add(GuhrioKasteel.DUEL);
        helper.assertTrue(starts.keySet().equals(new java.util.HashSet<>(ids)), "a start block for every level and the duel: " + starts.keySet());
        helper.assertTrue(poorten.size() == 14, "two gate blocks for each: " + poorten.size());
        for (StructureTemplate.StructureBlockInfo poort : poorten) {
            String id = poort.nbt().getStringOr("Level", "");
            BlockPos eigen = new BlockPos(poort.nbt().getIntOr("NaarX", 0), poort.nbt().getIntOr("NaarY", 0), poort.nbt().getIntOr("NaarZ", 0));
            helper.assertFalse(eigen.equals(BlockPos.ZERO), "the gate of " + id + " knows the way");
            BlockPos naar = GuhrioLevel.wereld(poort.pos(), poort.state().getValue(GuhrioBlocks.PoortBlok.FACING), eigen);
            helper.assertTrue(naar.equals(starts.get(id)), "the gate of " + id + " at " + poort.pos() + " leads to its start block: " + naar + " / " + starts.get(id));
        }
        for (String id : ids) {
            GuhrioLevel def = GuhrioLevel.bestand(level.getServer(), id);
            helper.assertTrue(def != null, "the level file of " + id);
            BlockPos start = starts.get(id);
            GuhrioLevel.Geplaatst lvl = def.plaats(level.dimension(), start, blokken.get(start).getValue(GuhrioBlocks.StartBlok.FACING));
            helper.assertTrue(lvl.baanVan(start) == 0, id + ": the start block is on the first lane");
            int munten = 0, masten = 0;
            for (var e : blokken.entrySet()) {
                if (lvl.baanVan(e.getKey()) < 0) {
                    continue;
                }
                if (e.getValue().getBlock() instanceof GuhrioStukken.VadsmuntBlok) {
                    munten |= 1 << e.getValue().getValue(GuhrioStukken.VadsmuntBlok.NUMMER);
                } else if (e.getValue().getBlock() instanceof GuhrioBlocks.MastBlok && e.getValue().getValue(GuhrioBlocks.MastBlok.TOP)) {
                    masten++;
                }
            }
            if (!id.equals(GuhrioKasteel.DUEL)) {
                helper.assertTrue(munten == 7 && masten == 1, id + ": three vadsmunten and one flagpole in its lanes: " + munten + " / " + masten);
            }
            // the exit and the entrance are in the level hall (the tower room for the duel): far from the lane, inside the castle
            for (BlockPos plek : new BlockPos[]{lvl.uitgang(), lvl.ingang()}) {
                helper.assertTrue(plek != null && plek.getX() > 40 && plek.getX() < 86 && plek.getZ() > 36 && plek.getZ() < 80, id + ": " + plek + " is in the keep");
            }
        }
        helper.succeed();
    }
}
