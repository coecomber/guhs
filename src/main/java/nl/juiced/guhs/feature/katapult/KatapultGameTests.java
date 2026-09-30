package nl.juiced.guhs.feature.katapult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * De Knabbelkatapult: the 12 forts (they load, stand by themselves, have Mika's), the support rule, the levels, stars and
 * katapultsterren (+1 per rule), the shop and clothing sources, the loaned pluisballen, the protection, and a whole run of
 * 12 forts on the real castle (with a real shot).
 */
public class KatapultGameTests {
    private static final String BATCH = "katapult_kasteel";

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer p) {
        KatapultGame.leave(p);
        Minigames.forget(p);
        helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    private static int count(Player p, net.minecraft.world.item.Item item) {
        return p.getInventory().countItem(item);
    }

    /** All 12 forts load from their templates, fit the plot, have a Mika and stand by themselves. */
    @GuhTest(template = "empty")
    public static void katapultFortenStaanStevig(GameTestHelper helper) {
        KatapultFort.forget();
        for (int i = 0; i < KatapultFort.FORTS; i++) {
            List<KatapultFort.Stuk> fort = KatapultFort.load(helper.getLevel(), i);
            helper.assertTrue(!fort.isEmpty(), "fort " + (i + 1) + " loads");
            Set<BlockPos> blocks = new HashSet<>();
            int mikas = 0;
            for (KatapultFort.Stuk s : fort) {
                BlockPos l = s.local();
                helper.assertTrue(l.getX() >= 0 && l.getX() < KatapultFort.W && l.getY() >= 0 && l.getY() < KatapultFort.H && l.getZ() >= 0
                        && l.getZ() < KatapultFort.D, "fort " + (i + 1) + " fits the plot: " + l);
                blocks.add(l);
                mikas += KatapultFort.isMika(s.state()) ? 1 : 0;
            }
            helper.assertTrue(mikas >= 1, "fort " + (i + 1) + " has a Mika");
            helper.assertTrue(KatapultFort.unstable(blocks).isEmpty(), "fort " + (i + 1) + " stands: " + KatapultFort.unstable(blocks));
        }
        // the support rule: a column holds; hanging out sideways holds 3 blocks, not 4; without its column it all falls
        Set<BlockPos> t = new HashSet<>();
        for (int y = 0; y < 4; y++) {
            t.add(new BlockPos(0, y, 0));
        }
        for (int x = 1; x <= 4; x++) {
            t.add(new BlockPos(x, 3, 0));
        }
        helper.assertTrue(KatapultFort.unstable(t).equals(Set.of(new BlockPos(4, 3, 0))), "4 out is too far: " + KatapultFort.unstable(t));
        t.remove(new BlockPos(0, 0, 0));
        helper.assertTrue(KatapultFort.unstable(t).size() == 7, "without its foot the column falls");
        helper.assertTrue(KatapultFort.strength(Blocks.GLASS.defaultBlockState()) == 1 && KatapultFort.strength(Blocks.OAK_PLANKS.defaultBlockState()) == 2
                && KatapultFort.strength(Blocks.STONE_BRICKS.defaultBlockState()) == 4
                && KatapultFort.strength(KatapultFeature.MIKA.get().defaultBlockState()) == 1, "glass 1, wood 2, stone 4, Mika 1");
        // the frame: world <-> template coordinates, every way the plot can face
        for (Direction f : Direction.Plane.HORIZONTAL) {
            BlockPos plek = new BlockPos(100, 60, -40);
            BlockPos w = KatapultFort.world(plek, f, 2, 5, 7);
            helper.assertTrue(new BlockPos(2, 5, 7).equals(KatapultFort.local(plek, f, w)), "frame " + f);
        }
        helper.succeed();
    }

    /** Levels: 5 / 4 / 3 pluisballen; stars; katapultsterren: 1, +1 per 9 stars, +1 record, lastig +50%. */
    @GuhTest(template = "empty")
    public static void katapultNiveausSterrenEnMunten(GameTestHelper helper) {
        helper.assertTrue(KatapultGame.balls(Niveau.MAKKELIJK) == 5 && KatapultGame.balls(Niveau.MEDIUM) == 4 && KatapultGame.balls(Niveau.LASTIG) == 3,
                "5 / 4 / 3 pluisballen");
        helper.assertTrue(KatapultGame.stars(false, true, 3) == 0 && KatapultGame.stars(true, false, 3) == 1 && KatapultGame.stars(true, true, 0) == 2
                && KatapultGame.stars(true, true, 1) == 3, "stars");
        helper.assertTrue(KatapultGame.munten(Niveau.MEDIUM, 0, false) == 1, "a run: 1");
        helper.assertTrue(KatapultGame.munten(Niveau.MEDIUM, 9, false) == 2 && KatapultGame.munten(Niveau.MEDIUM, 18, false) == 3
                && KatapultGame.munten(Niveau.MEDIUM, 36, false) == 5, "+1 per 9 stars");
        helper.assertTrue(KatapultGame.munten(Niveau.MEDIUM, 9, true) == KatapultGame.munten(Niveau.MEDIUM, 9, false) + 1, "+1 for a record");
        helper.assertTrue(KatapultGame.munten(Niveau.LASTIG, 36, true) == 9 && KatapultGame.munten(Niveau.MAKKELIJK, 36, true) == 6, "lastig +50%");
        helper.assertTrue(KatapultGame.board(Niveau.MEDIUM).equals("katapult_medium") && KatapultGame.board(Niveau.LASTIG).equals("katapult_lastig"),
                "boards per level");
        helper.assertTrue(PluisballenItem.richtlijn(PluisballenItem.stack(5, true)) && !PluisballenItem.richtlijn(PluisballenItem.stack(4, false)),
                "only makkelijk gets the aiming line");
        helper.succeed();
    }

    @GuhTest(template = "empty")
    public static void katapultWinkelBronnenEnLeen(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 2, 2));
        npc.setKind(GuhNpcEntity.Kind.KATAPULTGUH);
        var offers = npc.getOffers();
        var results = offers.stream().map(o -> o.getResult().getItem()).toList();
        for (GuhClothes piece : KatapultFeature.KLEDING) {
            helper.assertTrue(results.contains(ModItems.clothingItem(piece)), "the Kapitein sells " + piece);
            helper.assertTrue("katapult".equals(KledingBronnen.bron(piece)) && KledingBronnen.prijs(piece) != null, "one source: katapult, " + piece);
        }
        helper.assertTrue(offers.size() == 3 && offers.stream().allMatch(o -> o.getCostA().is(KatapultFeature.KATAPULTSTER.get())), "for katapultsterren");
        helper.assertTrue(GuhClothes.KATAPULT_OORBELLETJES.slot == GuhClothes.Slot.OREN && GuhClothes.KATAPULT_HELMPJE.slot == GuhClothes.Slot.HEAD
                && GuhClothes.KATAPULT_RIEM.slot == GuhClothes.Slot.BODY, "the slots (the earrings on the ears)");
        ServerPlayer p = player(helper);
        ItemStack balls = PluisballenItem.stack(5, true);
        helper.assertTrue(Features.isLoaned(balls) && !balls.getItem().onDroppedByPlayer(balls, p), "the pluisballen are loaned");
        p.getInventory().add(balls);
        p.getInventory().tick();
        helper.assertTrue(count(p, KatapultFeature.PLUISBALLEN.get()) == 0, "someone not playing loses them at once");
        leave(helper, p);
        helper.succeed();
    }

    @GuhTest(template = "empty")
    public static void katapultBescherming(GameTestHelper helper) {
        BlockPos floor = new BlockPos(2, 1, 2);
        helper.setBlock(floor, Blocks.OAK_PLANKS);
        BlockPos abs = helper.absolutePos(floor);
        BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(4, 4, 4)));
        KatapultProtection.TEST_AREAS.add(box);
        ServerPlayer p = player(helper);
        try {
            helper.assertTrue(Protected.at(helper.getLevel(), abs), "the Knabbelkatapult is protected");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), p)).isCanceled(),
                    "survival players can't break it");
            helper.assertTrue(!KatapultProtection.inKasteel(helper.getLevel(), abs.offset(20, 0, 0)), "outside it isn't");
        } finally {
            KatapultProtection.TEST_AREAS.remove(box);
            leave(helper, p);
        }
        helper.succeed();
    }

    /** A whole run on makkelijk: fort 1 is built, a real pluisbal flies, then all 12 forts fall; stars, sterren, knabbels, board. */
    @GuhTest(template = "knabbelkatapult", timeoutTicks = 600, batch = BATCH)
    public static void katapultRondeOpHetKasteel(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(), n -> n.getKind() == GuhNpcEntity.Kind.KATAPULTGUH);
        helper.assertTrue(npcs.size() == 1, "Kapitein Floepguh is on his wall: " + npcs.size());
        GuhNpcEntity npc = npcs.get(0);
        ServerPlayer p = player(helper);
        p.snapTo(npc.getX() + 1, npc.getY(), npc.getZ());
        KatapultGame.action(npc, p, KatapultGame.START + Niveau.MAKKELIJK.ordinal());
        KatapultGame game = KatapultGame.of(npc);
        helper.assertTrue(game.isPlayedBy(p) && KatapultGame.isPlaying(p) && game.werper() != null && game.plek() != null, "the run is on");
        helper.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(game.werper())) < KatapultGame.REACH, "you stand at the catapult");
        game.testBuildNow(npc, p);
        helper.assertTrue(game.phase() == KatapultGame.Phase.AIM && game.fort() == 0 && game.ballsLeft() == 5, "fort 1 stands, 5 pluisballen");
        helper.assertTrue(game.fortMikas() == 1 && game.fortKisten() == 1 && !game.fortBlocks().isEmpty(), "the Mikahutje: 1 Mika, 1 crate");
        helper.assertTrue(count(p, KatapultFeature.PLUISBALLEN.get()) == 5 && PluisballenItem.richtlijn(p.getMainHandItem()), "5 loaned balls with the aiming line");
        helper.assertTrue(game.wind().lengthSqr() == 0, "no wind on makkelijk");
        // a real shot at the fort
        BlockPos mid = KatapultFort.world(game.plek(), game.plekFacing(), KatapultFort.MIDDEN, 3, 4);
        Vec3 from = KatapultGame.launchPoint(game.werper(), Direction.NORTH);
        Vec3 aim = Vec3.atCenterOf(mid).subtract(from).normalize().add(0, 0.35, 0);
        helper.assertTrue(game.fireWith(p, 0.75f, aim), "FLOEP");
        helper.assertTrue(game.ballsLeft() == 4 && game.phase() == KatapultGame.Phase.FLYING && count(p, KatapultFeature.PLUISBALLEN.get()) == 4, "one ball flies");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(PluisbalEntity.class, helper.getBounds(), b -> true).isEmpty(), "a real pluisbal");
        helper.succeedWhen(() -> {
            helper.assertTrue(game.phase() == KatapultGame.Phase.AIM || game.phase() == KatapultGame.Phase.FORT_DONE, "the ball has landed: " + game.phase());
            if (game.phase() == KatapultGame.Phase.AIM) {
                game.testClearFort(npc, p, true);
            }
            helper.assertTrue(game.phase() == KatapultGame.Phase.FORT_DONE && game.fortStars()[0] >= 1, "fort 1 fell: " + game.fortStars()[0]);
            for (int i = 1; i < KatapultFort.FORTS; i++) {
                game.testNextFort(npc, p);
                helper.assertTrue(game.fort() == i && game.phase() == KatapultGame.Phase.AIM && game.fortMikas() >= 1, "fort " + (i + 1) + " stands");
                game.testClearFort(npc, p, true);
            }
            game.testNextFort(npc, p);
            helper.assertTrue(!game.isRunning() && !KatapultGame.isPlaying(p), "twelve forts: the run is over");
            int stars = game.runStars();
            helper.assertTrue(stars >= 34, "(nearly) all stars: " + stars);
            helper.assertTrue(count(p, KatapultFeature.KATAPULTSTER.get()) == KatapultGame.munten(Niveau.MAKKELIJK, stars, false),
                    "1 + stars/9 katapultsterren: " + count(p, KatapultFeature.KATAPULTSTER.get()));
            helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) >= 23, "the freed knabbels are yours: " + count(p, ModItems.KAAS_KNABBELS.get()));
            helper.assertTrue(KatapultFeature.has(p, "katapult_drie_sterren"), "the three-star advancement");
            helper.assertTrue(count(p, KatapultFeature.PLUISBALLEN.get()) == 0, "the pluisballen went back");
            helper.assertTrue(KatapultFeature.has(p, "katapult_gespeeld") && KatapultFeature.has(p, "katapult_alle_sterren") == (stars == 36)
                    && !KatapultFeature.has(p, "katapult_lastig"), "advancements");
            helper.assertTrue(Scorebord.top(helper.getLevel().getServer(), "katapult_makkelijk").stream().anyMatch(e -> e.player().equals(p.getUUID())),
                    "on the makkelijk board");
            helper.assertTrue(KatapultGame.best(p, Niveau.MAKKELIJK) == game.runScore() && game.runScore() > 30 * 500, "the record: " + game.runScore());
            helper.assertTrue(game.fortMikas() == 1 && !game.fortBlocks().isEmpty(), "and fort 1 is built again for the next one");
            leave(helper, p);
        });
    }
}
