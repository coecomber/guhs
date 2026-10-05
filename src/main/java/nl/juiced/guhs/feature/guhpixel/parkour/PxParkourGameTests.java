package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.PxTest;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.speelgoed.SpeelDeelBlock;
import nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature;
import nl.juiced.guhs.feature.speelgoed.ToestelBlock;
import nl.juiced.guhs.feature.speelgoed.ZitjeEntity;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.taal.Tekst;

/**
 * Game tests of the Guh-parkour (batch px_parkour). Laying out a route by clicking with the Startpaaltje (numbers, the
 * Finishpaaltje last, a second click takes a piece out, too far, too many, the item and the post both remember it); the
 * post screen's actions are checked on the server (owner, distance, index, own guh, at most four); two guhs really run the
 * whole demo course, twice (over the horde, through the air off the springplank, a bump in the kruiptunnel, a pit stop),
 * with lap times on the post, the Scorebord, the player's stats and the quest advancements; the existing toys
 * (glijbaantje, pluizige tunnel, wip) are used as pieces with their own seat and session; a post that goes frees its
 * guhs; everything per player (two players, two courses).
 * (Rooms: guhparkour_test_baan 36 x 8 x 9, guhparkour_test_klein 18 x 8 x 12: a floor on relative y 1.)
 */
public class PxParkourGameTests {
    private static final String BAAN = "guhparkour_test_baan", KLEIN = "guhparkour_test_klein", BATCH = "px_parkour";

    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = PxTest.speler(helper);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static GuhEntity guh(GameTestHelper helper, ServerPlayer baas, BlockPos at, String naam) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(baas);
        guh.setCustomName(Component.literal(naam));
        return guh;
    }

    private static StartpaalBlockEntity paal(GameTestHelper helper, BlockPos at, ServerPlayer eigenaar) {
        BlockPos pos = helper.absolutePos(at);
        helper.getLevel().setBlock(pos, ParkourSlice.STARTPAALTJE.get().defaultBlockState(), 3);
        StartpaalBlockEntity paal = (StartpaalBlockEntity) helper.getLevel().getBlockEntity(pos);
        paal.zetEigenaar(eigenaar);
        return paal;
    }

    private static BlockPos obstakel(GameTestHelper helper, Obstakel soort, BlockPos at, Direction facing) {
        BlockPos pos = helper.absolutePos(at);
        ToestelBlock.bouw(helper.getLevel(), pos, ParkourSlice.OBSTAKELS.get(soort).get(), facing);
        return pos;
    }

    private static BlockPos finish(GameTestHelper helper, BlockPos at) {
        BlockPos pos = helper.absolutePos(at);
        helper.getLevel().setBlock(pos, ParkourSlice.FINISHPAALTJE.get().defaultBlockState(), 3);
        return pos;
    }

    private static boolean heeft(ServerPlayer p, String adv) {
        AdvancementHolder holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + adv));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static String sleutel(Component c) {
        return c != null && c.getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(c);
    }

    private static void opruimen(GameTestHelper helper, List<GuhEntity> guhs, ServerPlayer... spelers) {
        for (GuhEntity g : guhs) {
            g.discard();
        }
        for (ServerPlayer p : spelers) {
            Uitzetten.stop(p, false);
        }
        PxTest.klaar(helper, spelers);
    }

    // =====================================================================================================================
    // laying out a route
    // =====================================================================================================================

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void routeUitzettenMetHetStartpaaltjeInDeHand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        BlockPos horde = obstakel(helper, Obstakel.HORDE, new BlockPos(4, 2, 4), Direction.WEST);
        BlockPos tunnel = obstakel(helper, Obstakel.KRUIPTUNNEL, new BlockPos(8, 2, 4), Direction.WEST);
        BlockPos tafel = obstakel(helper, Obstakel.KNABBELTAFELTJE, new BlockPos(12, 2, 4), Direction.WEST);
        BlockPos einde = finish(helper, new BlockPos(14, 2, 4));
        helper.assertTrue(level.getBlockState(tunnel.east()).getBlock() instanceof SpeelDeelBlock && level.getBlockState(tunnel.west()).getBlock() instanceof SpeelDeelBlock,
                "the kruiptunnel is three blocks long");
        helper.assertTrue(tunnel.equals(Routes.stuk(level, tunnel.east())), "a click on a part means the whole obstacle");
        helper.assertTrue(Routes.stuk(level, horde.below()) == null, "the floor is no piece");

        ItemStack stok = new ItemStack(ParkourSlice.STARTPAALTJE_ITEM.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, stok);
        // the real click: the event is cancelled (so the post is not placed against the toy) and the item remembers the piece
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(horde), Direction.UP, horde, false);
        boolean geannuleerd = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, horde, hit)).isCanceled();
        helper.assertTrue(geannuleerd && StartpaalBlock.route(stok).equals(List.of(horde)), "piece 1 by a real right-click: " + StartpaalBlock.route(stok));
        helper.assertTrue(Uitzetten.klikMetItem(p, stok, einde) == Routes.Uitkomst.FINISH, "the Finishpaaltje closes the route");
        helper.assertTrue(Uitzetten.klikMetItem(p, stok, tunnel) == Routes.Uitkomst.ERBIJ && StartpaalBlock.route(stok).equals(List.of(horde, tunnel, einde)),
                "a later piece goes in before the finish: " + StartpaalBlock.route(stok));
        helper.assertTrue(Uitzetten.klikMetItem(p, stok, tafel) == Routes.Uitkomst.ERBIJ && Routes.nummer(StartpaalBlock.route(stok), tafel) == 3, "piece 3");
        helper.assertTrue(Uitzetten.klikMetItem(p, stok, tunnel) == Routes.Uitkomst.ERUIT && StartpaalBlock.route(stok).equals(List.of(horde, tafel, einde)),
                "a second click takes a piece out");
        helper.assertTrue(Uitzetten.klikMetItem(p, stok, horde.below()) == Routes.Uitkomst.GEEN, "not a piece: nothing happens");
        // with an empty hand (and not laying out) a click on an obstacle is just a click
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertFalse(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, horde, hit)).isCanceled(),
                "no Startpaaltje in the hand: the click is not taken");

        // placing the post: it takes the route over and knows its owner
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 4));
        BlockState state = ParkourSlice.STARTPAALTJE.get().defaultBlockState();
        level.setBlock(pos, state, 3);
        state.getBlock().setPlacedBy(level, pos, state, p, stok);
        StartpaalBlockEntity paal = (StartpaalBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(paal.stukken().equals(List.of(horde, tafel, einde)) && p.getUUID().equals(paal.eigenaar()), "the post has the route and its owner");
        helper.assertTrue(Routes.heeftFinish(level, paal.stukken()) && Routes.aantal(level, paal.stukken()) == 2, "two pieces and a finish");
        // broken, the item remembers the route
        List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, paal);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(ParkourSlice.STARTPAALTJE_ITEM.get()) && StartpaalBlock.route(drops.get(0)).equals(paal.stukken()),
                "the dropped Startpaaltje remembers its route: " + drops);
        // pieces further than 32 blocks from where the post is placed do not come along
        ItemStack ver = new ItemStack(ParkourSlice.STARTPAALTJE_ITEM.get());
        StartpaalBlock.zetRoute(ver, List.of(horde, horde.offset(60, 0, 0)));
        BlockPos pos2 = helper.absolutePos(new BlockPos(2, 2, 8));
        level.setBlock(pos2, state, 3);
        state.getBlock().setPlacedBy(level, pos2, state, p, ver);
        helper.assertTrue(((StartpaalBlockEntity) level.getBlockEntity(pos2)).stukken().equals(List.of(horde)), "a piece too far away is left out");
        opruimen(helper, List.of(), p);
        helper.succeed();
    }

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void routeRegelsEnUitzettenOpEenGeplaatstPaaltje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        ServerPlayer ander = speler(helper, new BlockPos(3, 2, 2));
        StartpaalBlockEntity paal = paal(helper, new BlockPos(1, 2, 1), p);
        // laying out on the placed post: only the owner
        helper.assertFalse(Uitzetten.start(ander, paal), "somebody else cannot lay out on your post");
        helper.assertTrue(Uitzetten.start(p, paal) && Uitzetten.bezig(p, paal.getBlockPos()), "the owner lays out");
        List<BlockPos> hordes = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            hordes.add(obstakel(helper, Obstakel.HORDE, new BlockPos(3 + (i % 6) * 2, 2, 3 + (i / 6) * 3), Direction.NORTH));
        }
        BlockPos einde = finish(helper, new BlockPos(16, 2, 10));
        // a real click while laying out (empty hand): taken, and it lands in the post
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(hordes.get(0)), Direction.UP, hordes.get(0), false);
        helper.assertTrue(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, hordes.get(0), hit)).isCanceled()
                && paal.stukken().equals(List.of(hordes.get(0))), "laying out: the click is piece 1 of the post");
        helper.assertFalse(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(ander, InteractionHand.MAIN_HAND, hordes.get(1), hit)).isCanceled(),
                "the other player is not laying out");
        for (int i = 1; i < Routes.MAX_STUKKEN; i++) {
            helper.assertTrue(Uitzetten.klikInModus(p, paal, hordes.get(i)) == Routes.Uitkomst.ERBIJ, "piece " + (i + 1));
        }
        helper.assertTrue(Uitzetten.klikInModus(p, paal, hordes.get(16)) == Routes.Uitkomst.VOL && paal.stukken().size() == Routes.MAX_STUKKEN, "sixteen is full");
        helper.assertTrue(Uitzetten.klikInModus(p, paal, einde) == Routes.Uitkomst.FINISH && paal.stukken().size() == Routes.MAX_STUKKEN + 1,
                "the Finishpaaltje still fits: it does not count as a piece");
        BlockPos ver = helper.absolutePos(new BlockPos(1, 2, 1)).above(Routes.BEREIK + 3);     // (straight up: inside this test's own column)
        level.setBlock(ver, ParkourSlice.FINISHPAALTJE.get().defaultBlockState(), 3);
        helper.assertTrue(Uitzetten.klikInModus(p, paal, ver) == Routes.Uitkomst.TE_VER, "further than 32 blocks from the start");
        level.setBlock(ver, Blocks.AIR.defaultBlockState(), 3);
        // a Scorebord clicked while laying out is linked to this post
        BlockPos bordPos = helper.absolutePos(new BlockPos(16, 2, 1));
        level.setBlock(bordPos, ParkourSlice.SCOREBORD.get().defaultBlockState(), 3);
        BlockHitResult bordHit = new BlockHitResult(Vec3.atCenterOf(bordPos), Direction.NORTH, bordPos, false);
        helper.assertTrue(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, bordPos, bordHit)).isCanceled()
                && paal.getBlockPos().equals(((ScorebordBlockEntity) level.getBlockEntity(bordPos)).paal()), "the Scorebord is linked by a click");
        Uitzetten.stop(p, false);
        helper.assertFalse(Uitzetten.bezig(p), "done laying out");

        // the screen's list: up, down, out; the finish stays last
        helper.assertTrue(paal.verplaats(1, -1) && paal.stukken().get(0).equals(hordes.get(1)) && paal.stukken().get(1).equals(hordes.get(0)), "piece 2 moved up");
        helper.assertFalse(paal.verplaats(0, -1), "the first cannot go further up");
        helper.assertFalse(paal.verplaats(Routes.MAX_STUKKEN - 1, 1), "the last piece cannot pass the finish");
        helper.assertFalse(paal.verplaats(Routes.MAX_STUKKEN, -1), "the finish cannot move");
        helper.assertTrue(paal.verwijder(0) && paal.stukken().size() == Routes.MAX_STUKKEN && Routes.heeftFinish(level, paal.stukken()), "a piece taken out");
        helper.assertFalse(paal.verwijder(99), "no such piece");
        // a piece that was broken shows as gone and is no piece any more
        level.setBlock(hordes.get(2), Blocks.AIR.defaultBlockState(), 3);
        CompoundTag stand = ParkourPayloads.stand(p, paal, null);
        ListTag stukken = stand.getListOrEmpty("Stukken");
        int weg = 0;
        for (int i = 0; i < stukken.size(); i++) {
            weg += stukken.getCompoundOrEmpty(i).getBooleanOr("Weg", false) ? 1 : 0;
        }
        helper.assertTrue(weg == 1 && stukken.getCompoundOrEmpty(stukken.size() - 1).getBooleanOr("Finish", false) && stand.getBooleanOr("Mag", false)
                && stand.getIntOr("Max", 0) == Routes.MAX_STUKKEN, "the screen shows the broken piece as gone and the finish last");
        helper.assertFalse(ParkourPayloads.stand(ander, paal, null).getBooleanOr("Mag", true), "the other player may only look");
        opruimen(helper, List.of(), p, ander);
        helper.succeed();
    }

    // =====================================================================================================================
    // the screen's actions, checked on the server
    // =====================================================================================================================

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void schermActiesWordenOpDeServerGecontroleerd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(3, 2, 3));
        ServerPlayer ander = speler(helper, new BlockPos(4, 2, 3));
        StartpaalBlockEntity paal = paal(helper, new BlockPos(2, 2, 2), p);
        BlockPos pos = paal.getBlockPos();
        BlockPos horde = obstakel(helper, Obstakel.HORDE, new BlockPos(6, 2, 2), Direction.WEST);
        BlockPos tafel = obstakel(helper, Obstakel.KNABBELTAFELTJE, new BlockPos(9, 2, 2), Direction.WEST);
        paal.klik(horde);
        paal.klik(tafel);
        List<GuhEntity> guhs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            guhs.add(guh(helper, p, new BlockPos(3 + i, 2, 8), "Vadsje" + i));
        }
        GuhEntity vreemd = guh(helper, ander, new BlockPos(10, 2, 8), "Vreemd");
        guhs.add(vreemd);
        UUID nul = new UUID(0L, 0L);
        // somebody else: may not change anything
        helper.assertTrue(sleutel(ParkourPayloads.doe(ander, pos, ParkourPayloads.WEG, 0, nul)).equals("gui.guhs.guhparkour.niet_van_jou")
                && paal.stukken().size() == 2, "not the owner: the piece stays");
        helper.assertTrue(sleutel(ParkourPayloads.doe(ander, pos, ParkourPayloads.GUH_OP, 0, vreemd.getUUID())).equals("gui.guhs.guhparkour.niet_van_jou")
                && paal.guhs().isEmpty(), "not the owner: no guh on the route");
        // the owner, but too far from the post: nothing at all
        Vec3 was = p.position();
        p.snapTo(was.x + 12, was.y, was.z);
        helper.assertTrue(ParkourPayloads.doe(p, pos, ParkourPayloads.WEG, 0, nul) == null && paal.stukken().size() == 2, "too far from the post: ignored");
        p.snapTo(was.x, was.y, was.z);
        // indices are checked
        ParkourPayloads.doe(p, pos, ParkourPayloads.OMHOOG, 7, nul);
        ParkourPayloads.doe(p, pos, ParkourPayloads.WEG, -1, nul);
        helper.assertTrue(paal.stukken().equals(List.of(horde, tafel)), "bad indices change nothing");
        ParkourPayloads.doe(p, pos, ParkourPayloads.OMLAAG, 0, nul);
        helper.assertTrue(paal.stukken().equals(List.of(tafel, horde)), "piece 1 moved down");
        // guhs: only your own, near, free, at most four
        helper.assertTrue(sleutel(ParkourPayloads.doe(p, pos, ParkourPayloads.GUH_OP, 0, vreemd.getUUID())).equals("gui.guhs.guhparkour.scherm.uit.ver")
                && paal.guhs().isEmpty(), "somebody else's guh cannot be put on");
        helper.assertTrue(sleutel(ParkourPayloads.doe(p, pos, ParkourPayloads.GUH_OP, 0, UUID.randomUUID())).equals("gui.guhs.guhparkour.scherm.uit.ver"),
                "an unknown guh");
        GuhKiezer.claim(guhs.get(4), "guhkade");
        helper.assertTrue(sleutel(ParkourPayloads.doe(p, pos, ParkourPayloads.GUH_OP, 0, guhs.get(4).getUUID())).equals("gui.guhs.guhpixel.kiezer.bezet.bezig")
                && paal.guhs().isEmpty(), "a guh that is busy with another px feature");
        GuhKiezer.los(guhs.get(4));
        guhs.get(0).setOrderedToSit(true);
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(sleutel(ParkourPayloads.doe(p, pos, ParkourPayloads.GUH_OP, 0, guhs.get(i).getUUID())).equals("gui.guhs.guhparkour.scherm.melding.op"),
                    "guh " + i + " put on the route");
        }
        helper.assertTrue(paal.guhs().size() == 4 && !guhs.get(0).isOrderedToSit() && ParkourSlice.NS.equals(GuhKiezer.geclaimd(guhs.get(0)))
                && guhs.get(0).getPersistentData().getLongOr(RouteGoal.PAAL, 0L) == pos.asLong(), "four guhs: standing up, claimed, they know their post");
        helper.assertTrue(sleutel(ParkourPayloads.doe(p, pos, ParkourPayloads.GUH_OP, 0, guhs.get(4).getUUID())).equals("gui.guhs.guhparkour.scherm.melding.vol")
                && paal.guhs().size() == 4, "the fifth does not fit");
        helper.assertTrue(sleutel(ParkourPayloads.doe(p, pos, ParkourPayloads.GUH_OP, 0, guhs.get(1).getUUID())).equals("gui.guhs.guhparkour.scherm.melding.al_op"),
                "the same guh twice");
        // the screen's state: who is on, who cannot
        CompoundTag stand = ParkourPayloads.stand(p, paal, null);
        helper.assertTrue(stand.getListOrEmpty("Leden").size() == 4 && stand.getListOrEmpty("Guhs").size() == 5, "four members, five own guhs in the list");
        // a second post: a guh that runs here cannot also run there
        StartpaalBlockEntity tweede = paal(helper, new BlockPos(2, 2, 10), p);
        helper.assertTrue(sleutel(ParkourPayloads.guhOp(p, tweede, guhs.get(1).getUUID())).equals("gui.guhs.guhpixel.kiezer.bezet.bezig") && tweede.guhs().isEmpty(),
                "one route per guh");
        // taking one off frees it
        ParkourPayloads.doe(p, pos, ParkourPayloads.GUH_AF, 0, guhs.get(1).getUUID());
        helper.assertTrue(paal.guhs().size() == 3 && !guhs.get(1).getPersistentData().contains(RouteGoal.PAAL) && GuhKiezer.geclaimd(guhs.get(1)).isEmpty()
                && GuhKiezer.bezet(guhs.get(1)) == null, "taken off: free again");
        // laying out from the screen
        ParkourPayloads.doe(p, pos, ParkourPayloads.UITZETTEN, 0, nul);
        helper.assertTrue(Uitzetten.bezig(p, pos), "the screen's button starts laying out");
        opruimen(helper, guhs, p, ander);
        helper.succeed();
    }

    // =====================================================================================================================
    // the runner
    // =====================================================================================================================

    @GuhTest(template = BAAN, batch = BATCH, timeoutTicks = 6000)
    public static void tweeGuhsLopenDeHeleProefbaanTweeKeer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        BlockPos start = helper.absolutePos(new BlockPos(3, 2, 4));
        StartpaalBlockEntity paal = ParkourCommando.proefbaan(level, start, Direction.EAST, p);
        helper.assertTrue(paal != null && paal.stukken().size() == 7 && Routes.heeftFinish(level, paal.stukken()), "the demo course: six obstacles and a finish");
        BlockPos bordPos = start.relative(Direction.SOUTH, 2);
        ScorebordBlockEntity bord = (ScorebordBlockEntity) level.getBlockEntity(bordPos);
        helper.assertTrue(bord != null && start.equals(bord.paal()), "the Scorebord belongs to the post");
        GuhEntity een = guh(helper, p, new BlockPos(2, 2, 6), "Vadsje");
        GuhEntity twee = guh(helper, p, new BlockPos(4, 2, 6), "Knabbel");
        helper.assertTrue(paal.zetOp(een) && paal.zetOp(twee), "two guhs on the route");
        BlockPos horde = start.relative(Direction.EAST, ParkourCommando.HORDE), plank = start.relative(Direction.EAST, ParkourCommando.SPRINGPLANK),
                tunnel = start.relative(Direction.EAST, ParkourCommando.KRUIPTUNNEL), tafel = start.relative(Direction.EAST, ParkourCommando.TAFEL);
        boolean[] gezien = new boolean[5];   // over the horde, through the air, a bump, eating, the route flag
        int[] hartjes = {0};
        float[] leven = {een.getHealth(), twee.getHealth()};
        helper.onEachTick(() -> {
            for (GuhEntity g : List.of(een, twee)) {
                double hoog = g.getY() - start.getY();
                gezien[0] |= Math.abs(g.getX() - (horde.getX() + 0.5)) < 0.6 && hoog > 0.45;
                gezien[1] |= g.getX() > plank.getX() + 1.5 && g.getX() < plank.getX() + 3.5 && hoog > 1.0;
                gezien[3] |= g.blockPosition().closerThan(tafel, 1.6) && g.getNavigation().isDone() && g.getDeltaMovement().horizontalDistanceSqr() < 1.0e-4;
                gezien[4] |= GuhHooks.heeft(g, PxVlaggen.OP_ROUTE);
                int wie = g == een ? 0 : 1;
                helper.assertTrue(g.isAlive() && g.getHealth() >= leven[wie] - 0.01f, "the parkour never hurts a guh");
                leven[wie] = g.getHealth();
                helper.assertTrue(Math.abs(g.getZ() - (start.getZ() + 0.5)) < 6 && hoog > -0.6 && hoog < 4, "it stays on the course: " + g.position());
            }
            gezien[2] |= level.getBlockState(tunnel).getValue(ObstakelBlock.Kruiptunnel.BOBBEL) > 0;
            int h = Band.hartjes(een);
            helper.assertTrue(h >= hartjes[0], "hearts never go down");
            hartjes[0] = h;
        });
        helper.succeedWhen(() -> {
            StartpaalBlockEntity.Score a = paal.scores().get(een.getUUID()), b = paal.scores().get(twee.getUUID());
            helper.assertTrue(a != null && b != null && a.rondjes() >= 2 && b.rondjes() >= 1, "laps: " + a + " / " + b + "; " + een.position() + " " + twee.position());
            helper.assertTrue(gezien[0] && gezien[1] && gezien[2] && gezien[3] && gezien[4],
                    "it jumped the horde, flew off the springplank, was a bump in the tunnel, ate at the table, carried the route flag: "
                            + java.util.Arrays.toString(gezien));
            helper.assertTrue(a.beste() > 150 && a.beste() <= a.laatste() && a.beste() < 2500 && a.naam().getString().equals("Vadsje"),
                    "a believable lap time in ticks, the best is not slower than the last: " + a);
            // the Scorebord has both, best first
            ListTag rijen = bord.rijen();
            helper.assertTrue(rijen.size() == 2 && rijen.getCompoundOrEmpty(0).getIntOr("Beste", 0) <= rijen.getCompoundOrEmpty(1).getIntOr("Beste", 0)
                    && rijen.getCompoundOrEmpty(0).getIntOr("Rondjes", 0) >= 1 && !Tekst.empty(Tekst.get(rijen.getCompoundOrEmpty(0), "Naam")),
                    "the Scorebord shows both guhs, the fastest on top: " + rijen);
            // the owner: quests, stats, the Guhdex section
            helper.assertTrue(heeft(p, "guhparkour_rondje") && !heeft(p, "guhparkour_groot"), "the lap quest, not the big one (six pieces)");
            helper.assertTrue(ParkourStats.rondjes(p) >= 3 && ParkourStats.routes(p) == 1 && ParkourStats.langste(p) == 6
                    && ParkourStats.beste(p, een.getUUID()) == a.beste(), "the player's stats");
            helper.assertTrue(GidsBlad.stand(p).toString().contains("guhparkour.gids.kop"), "the Guhdex section is there");
            helper.assertTrue(Band.hartjes(een) >= 1, "a lap gives a heart");
            opruimen(helper, List.of(een, twee), p);
        });
    }

    @GuhTest(template = KLEIN, batch = BATCH, timeoutTicks = 4000)
    public static void bestaandSpeelgoedIsEenStukVanDeRoute(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        StartpaalBlockEntity paal = paal(helper, new BlockPos(2, 2, 6), p);
        BlockPos glijbaan = helper.absolutePos(new BlockPos(6, 2, 6));
        ToestelBlock.bouw(level, glijbaan, SpeelgoedFeature.GLIJBAANTJE.get(), Direction.NORTH);
        BlockPos wip = helper.absolutePos(new BlockPos(14, 2, 6));
        ToestelBlock.bouw(level, wip, SpeelgoedFeature.WIP.get(), Direction.NORTH);
        List<BlockPos> tunnel = new ArrayList<>();
        for (int x = 9; x <= 11; x++) {
            BlockPos t = helper.absolutePos(new BlockPos(x, 2, 3));
            level.setBlock(t, SpeelgoedFeature.TUNNEL.get().defaultBlockState().setValue(nl.juiced.guhs.feature.speelgoed.TunnelBlock.AXIS, Direction.Axis.X), 3);
            tunnel.add(t);
        }
        for (BlockPos t : tunnel) {
            level.setBlock(t, nl.juiced.guhs.feature.speelgoed.TunnelBlock.vorm(level.getBlockState(t), level, t), 3);
        }
        BlockPos einde = finish(helper, new BlockPos(16, 2, 9));
        helper.assertTrue(paal.klik(Routes.stuk(level, glijbaan.north())) == Routes.Uitkomst.ERBIJ && paal.stukken().get(0).equals(glijbaan), "a click on a part of the glijbaantje: piece 1");
        helper.assertTrue(paal.klik(tunnel.get(1)) == Routes.Uitkomst.ERBIJ && paal.klik(wip) == Routes.Uitkomst.ERBIJ && paal.klik(einde) == Routes.Uitkomst.FINISH,
                "the tunnel, the wip, the finish");
        helper.assertTrue(Routes.soort(level, glijbaan) == Routes.Soort.GLIJBAAN && Routes.soort(level, wip) == Routes.Soort.WIP_SCHOMMEL
                && Routes.soort(level, tunnel.get(1)) == Routes.Soort.TUNNEL, "the toys are pieces");
        GuhEntity guh = guh(helper, p, new BlockPos(3, 2, 8), "Glijer");
        paal.zetOp(guh);
        boolean[] gezien = new boolean[3];   // on the slide, inside the tunnel, on the wip
        helper.onEachTick(() -> {
            if (guh.getVehicle() instanceof ZitjeEntity z) {
                gezien[0] |= z.toestel().equals(glijbaan);
                gezien[2] |= z.toestel().equals(wip);
            }
            gezien[1] |= guh.noPhysics && tunnel.contains(guh.blockPosition());
        });
        helper.succeedWhen(() -> {
            StartpaalBlockEntity.Score s = paal.scores().get(guh.getUUID());
            helper.assertTrue(s != null && s.rondjes() >= 1, "a lap over the toys: " + s + " at " + guh.position() + " seen " + java.util.Arrays.toString(gezien));
            helper.assertTrue(gezien[0] && gezien[1] && gezien[2], "it slid down the glijbaantje, ran through the tunnel and sat on the wip: "
                    + java.util.Arrays.toString(gezien));
            helper.assertTrue(!guh.noPhysics && !guh.isNoGravity() && !guh.isPassenger(), "on its own feet again after the lap");
            opruimen(helper, List.of(guh), p);
        });
    }

    @GuhTest(template = KLEIN, batch = BATCH, timeoutTicks = 1200)
    public static void paaltjeWegOfGuhEraf_dePootjesWeerOpDeGrond(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        StartpaalBlockEntity paal = paal(helper, new BlockPos(2, 2, 6), p);
        BlockPos tunnel = obstakel(helper, Obstakel.KRUIPTUNNEL, new BlockPos(8, 2, 6), Direction.WEST);
        paal.klik(tunnel);
        GuhEntity guh = guh(helper, p, new BlockPos(3, 2, 6), "Bobbel");
        GuhEntity ander = guh(helper, p, new BlockPos(3, 2, 9), "Wachter");
        paal.zetOp(guh);
        paal.zetOp(ander);
        ander.setOrderedToSit(true);      // (sitting on command pauses a guh: it stays on the list but does not run)
        BlockPos bordPos = helper.absolutePos(new BlockPos(2, 2, 9));
        BlockState bordState = ParkourSlice.SCOREBORD.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        level.setBlock(bordPos, bordState, 3);
        bordState.getBlock().setPlacedBy(level, bordPos, bordState, p, new ItemStack(ParkourSlice.SCOREBORD.get()));
        ScorebordBlockEntity bord = (ScorebordBlockEntity) level.getBlockEntity(bordPos);
        helper.assertTrue(paal.getBlockPos().equals(bord.paal()), "a Scorebord placed near a post links itself");
        int[] stap = {0};
        helper.succeedWhen(() -> {
            if (stap[0] == 0) {
                // wait until it crawls inside the tunnel, then take it off halfway
                helper.assertTrue(guh.noPhysics && level.getBlockState(tunnel).getValue(ObstakelBlock.Kruiptunnel.BOBBEL) > 0, "in the tunnel: " + guh.position());
                helper.assertTrue(ander.isOrderedToSit() && ander.distanceToSqr(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 2, 9)))) < 2.5,
                        "the sitting guh waits where it sat");
                paal.haalAf(guh.getUUID());
                stap[0] = 1;
                helper.assertTrue(false, "taken off: wait a few ticks");
            }
            if (stap[0] == 1) {
                helper.assertTrue(!guh.noPhysics && !guh.isNoGravity() && !guh.getPersistentData().contains(RouteStukken.ZWEEF)
                        && !GuhHooks.heeft(guh, PxVlaggen.OP_ROUTE) && level.getBlockState(tunnel).getValue(ObstakelBlock.Kruiptunnel.BOBBEL) == 0
                        && level.noCollision(guh), "taken off halfway: physics back, no bump left, not stuck in anything: " + guh.position());
                helper.assertTrue(GuhKiezer.bezet(guh) == null, "and free for anything else");
                // the post goes: the other guh is free too, the Scorebord is on its own
                level.destroyBlock(paal.getBlockPos(), false);
                stap[0] = 2;
            }
            helper.assertTrue(!ander.getPersistentData().contains(RouteGoal.PAAL) && GuhKiezer.geclaimd(ander).isEmpty(), "the post is gone: its guhs are free");
            helper.assertTrue(bord.paal() == null && bord.rijen().isEmpty(), "and the Scorebord is empty");
            helper.assertTrue(level.getBlockState(tunnel).getBlock() instanceof ObstakelBlock, "the obstacle itself stays");
            opruimen(helper, List.of(guh, ander), p);
        });
    }

    // =====================================================================================================================
    // scores, per player
    // =====================================================================================================================

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void scoresPerGuhEnPerSpeler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        ServerPlayer q = speler(helper, new BlockPos(12, 2, 2));
        StartpaalBlockEntity paal = paal(helper, new BlockPos(2, 2, 4), p);
        StartpaalBlockEntity paalQ = paal(helper, new BlockPos(12, 2, 4), q);
        List<BlockPos> stukken = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            stukken.add(obstakel(helper, Obstakel.HORDE, new BlockPos(2 + i * 2, 2, 8), Direction.NORTH));
            paal.klik(stukken.get(i));
        }
        paalQ.klik(stukken.get(0));
        GuhEntity guh = guh(helper, p, new BlockPos(3, 2, 6), "Snelle");
        GuhEntity guhQ = guh(helper, q, new BlockPos(13, 2, 6), "Trage");
        paal.zetOp(guh);
        paalQ.zetOp(guhQ);
        guh.setOrderedToSit(true);
        guhQ.setOrderedToSit(true);
        // a lap of 247 ticks = 12,35 s
        Component tijd = ParkourStats.tijd(247);
        Object[] args = ((TranslatableContents) tijd.getContents()).getArgs();
        helper.assertTrue(args[0].equals("12") && args[1].equals("35"), "247 ticks is 12 seconds and 35 hundredths: " + List.of(args));
        paal.rondje(guh, 247);
        helper.assertTrue(heeft(p, "guhparkour_rondje") && !heeft(p, "guhparkour_groot"), "eight pieces but no Finishpaaltje: not the big quest yet");
        paal.klik(finish(helper, new BlockPos(16, 2, 10)));
        paal.rondje(guh, 300);
        paal.rondje(guh, 200);
        StartpaalBlockEntity.Score s = paal.scores().get(guh.getUUID());
        helper.assertTrue(s.rondjes() == 3 && s.beste() == 200 && s.laatste() == 200, "three laps, the best is kept: " + s);
        paal.rondje(guh, 260);
        s = paal.scores().get(guh.getUUID());
        helper.assertTrue(s.rondjes() == 4 && s.beste() == 200 && s.laatste() == 260, "a slower lap does not spoil the record: " + s);
        helper.assertTrue(heeft(p, "guhparkour_groot"), "eight pieces and a finish: the big quest");
        helper.assertTrue(ParkourStats.rondjes(p) == 4 && ParkourStats.routes(p) == 1 && ParkourStats.langste(p) == 8 && ParkourStats.beste(p, guh.getUUID()) == 200,
                "the owner's stats");
        // the other player: own post, own guh, own numbers
        helper.assertTrue(ParkourStats.rondjes(q) == 0 && !heeft(q, "guhparkour_rondje") && paalQ.scores().isEmpty(), "nothing of this is the other player's");
        paalQ.rondje(guhQ, 900);
        helper.assertTrue(ParkourStats.rondjes(q) == 1 && ParkourStats.langste(q) == 1 && heeft(q, "guhparkour_rondje") && !heeft(q, "guhparkour_groot")
                && ParkourStats.rondjes(p) == 4 && paal.scores().size() == 1 && paalQ.scores().get(guhQ.getUUID()).beste() == 900, "per player, per post");
        // a renamed guh shows its new name after its next lap; the scores of a guh that was taken off stay on the board
        guh.setCustomName(Component.literal("Bliksem"));
        paal.rondje(guh, 210);
        paal.haalAf(guh.getUUID());
        helper.assertTrue(paal.scores().get(guh.getUUID()).naam().getString().equals("Bliksem") && paal.rijen().size() == 1, "renamed, taken off, still on the board");
        paal.wisScores();
        helper.assertTrue(paal.scores().isEmpty() && paal.rijen().isEmpty() && ParkourStats.rondjes(p) == 5, "wiping the board does not wipe the player's stats");
        opruimen(helper, List.of(guh, guhQ), p, q);
        helper.succeed();
    }
}
