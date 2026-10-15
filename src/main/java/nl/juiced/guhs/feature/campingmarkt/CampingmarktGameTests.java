package nl.juiced.guhs.feature.campingmarkt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuePutStructure;
import nl.juiced.guhs.feature.kamperen.LuisterGoal;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.spiesburcht.NetherMikaRuil;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Herstel;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.feature.wereldleven.WereldlevenFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GegarandeerdPlacement;
import nl.juiced.guhs.world.grond.GrondPoolElement;

/**
 * bbq2 (camping-markt): the Grillcamping and the Nether-Mika-ruilmarkt. What matters most is tested with two players at the
 * same blocks: each has a turn of their own and what a player changed comes back by itself (a tent, the fire, a split log,
 * the scales). The test server has no Guhbarbecuether: the worldgen side is checked through the registries and the
 * templates themselves. Template campingmarkt_test_kamer: 24 x 24 houtskoolsteen at y 0 (the floor is helper y 1, things
 * stand at helper y 2). Tests that move the clock of {@link Herstel} run in a batch of their own.
 */
public class CampingmarktGameTests {
    private static final String KAMER = "campingmarkt_test_kamer";
    private static final String BATCH = "campingmarkt";

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        CampingmarktFeature.CAMPING.wis(p);
        CampingmarktFeature.RUILMARKT.wis(p);
        GuhQuests.saved(p).remove(Kamperen.HARING_PLEKKEN);
        Ruilmarkt.vergeet(p.getUUID());
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Ruilmarkt.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, BlockPos at) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        BlockPos abs = helper.absolutePos(at);
        npc.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(npc);
        return npc;
    }

    private static int tel(ServerPlayer p, Item item) {
        return p.getInventory().countItem(item);
    }

    private static void inHand(ServerPlayer p, ItemStack stack) {
        p.getInventory().setSelectedSlot(0);
        p.getInventory().setItem(0, stack);
    }

    private static BlockHitResult raak(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static BlockPos bord(GameTestHelper helper, BlockPos at, Direction deur) {
        helper.setBlock(at, CampingmarktFeature.KAMPEERPLEK.get().defaultBlockState().setValue(CampingmarktBlocks.Kampeerplek.FACING, deur));
        return helper.absolutePos(at);
    }

    /** The world spot of a block of the small templates for a pitch whose sign stands at {@code bord}. */
    private static BlockPos inPlek(ServerLevel level, BlockPos bord, int x, int y, int z) {
        Rotation draai = Kamperen.draai(level.getBlockState(bord).getValue(CampingmarktBlocks.Kampeerplek.FACING));
        return Kamperen.hoek(bord, draai).offset(StructureTemplate.transform(new BlockPos(x, y, z), Mirror.NONE, draai, BlockPos.ZERO));
    }

    // =================================================================================================================
    // the camping
    // =================================================================================================================

    /**
     * Two players pitch a tent: only with the tent bag and on the right step; the tent stands turned the way its sign looks;
     * a taken pitch refuses the next player, who takes another one; the pegs count per player and per tent; four pegs are
     * the step; and after a while every tent is folded up again, the pitch as it was.
     */
    @GuhTest(template = KAMER, batch = "campingmarkt_tent")
    public static void campingmarktTentPerSpeler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(2, 2, 2)), b = speler(helper, new BlockPos(3, 2, 2));
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.KAMPBAASGUH, new BlockPos(2, 2, 21));
        try {
            Verhaallijn lijn = CampingmarktFeature.CAMPING;
            var rol = NpcRollen.van(npc);
            helper.assertTrue(rol instanceof Kamperen.KampbaasRol, "the Kampbaas-guh has his role");
            BlockPos zuid = bord(helper, new BlockPos(8, 2, 10), Direction.SOUTH), oost = bord(helper, new BlockPos(17, 2, 18), Direction.EAST);
            BlockState leeg = level.getBlockState(zuid);
            Item zak = CampingmarktFeature.TENTZAK.get();

            // not begun, and no bag: nothing happens
            Kamperen.zetOp(a, zuid);
            helper.assertTrue(level.getBlockState(zuid) == leeg && !lijn.vlag(a, Kamperen.TENT_VLAG), "no tent before the talk");
            rol.talk(npc, a);
            helper.assertTrue(lijn.stap(a) == 1 && tel(a, zak) == 1, "a began and has the tent bag");
            a.getInventory().clearContent();
            Kamperen.zetOp(a, zuid);
            helper.assertTrue(level.getBlockState(zuid) == leeg, "no bag, no tent");
            rol.talk(npc, a);
            helper.assertTrue(tel(a, zak) == 1, "the lost bag is given again");

            // the tent: the real click, with the bag in the hand
            inHand(a, new ItemStack(zak));
            level.getBlockState(zuid).useItemOn(a.getMainHandItem(), level, a, InteractionHand.MAIN_HAND, raak(zuid));
            BlockState bezet = level.getBlockState(zuid);
            helper.assertTrue(bezet.getValue(CampingmarktBlocks.Kampeerplek.BEZET) && bezet.getValue(CampingmarktBlocks.Kampeerplek.FACING) == Direction.SOUTH,
                    "the sign says the pitch is taken: " + bezet);
            helper.assertTrue(lijn.vlag(a, Kamperen.TENT_VLAG) && Herstel.wacht(level, Kamperen.hoek(zuid, Rotation.NONE)), "a's tent stands, and will be folded up");
            BlockPos[] pennen = {inPlek(level, zuid, 0, 0, 0), inPlek(level, zuid, 6, 0, 0), inPlek(level, zuid, 0, 0, 3), inPlek(level, zuid, 6, 0, 3)};
            for (BlockPos pen : pennen) {
                helper.assertTrue(level.getBlockState(pen).is(CampingmarktFeature.HARING.get()) && !level.getBlockState(pen).getValue(CampingmarktBlocks.Haring.VAST),
                        "a loose peg at " + pen);
            }
            helper.assertTrue(level.getBlockState(inPlek(level, zuid, 3, 2, 1)).is(CampingmarktFeature.TENTDOEK.get("geel").plaat().get()), "the ridge of the tent");
            helper.assertTrue(level.getBlockState(inPlek(level, zuid, 1, 0, 1)).is(CampingmarktFeature.TENTDOEK.get("geel").trap().get()), "a slope of the tent");
            helper.assertTrue(level.getBlockState(zuid.north()).isAir(), "the door is open");

            // b did not talk to him: a peg goes in, nothing is counted
            Kamperen.slaHaring(b, pennen[3], level.getBlockState(pennen[3]));
            helper.assertTrue(level.getBlockState(pennen[3]).getValue(CampingmarktBlocks.Haring.VAST) && Kamperen.haringen(b) == 0 && lijn.stap(b) == 0, "a tap for fun");
            // a: the same peg twice counts once; a peg somebody else hit counts for a too
            Kamperen.slaHaring(a, pennen[0], level.getBlockState(pennen[0]));
            Kamperen.slaHaring(a, pennen[0], level.getBlockState(pennen[0]));
            helper.assertTrue(Kamperen.haringen(a) == 1 && lijn.stap(a) == 1, "one peg: " + Kamperen.haringen(a));
            Kamperen.slaHaring(a, pennen[1], level.getBlockState(pennen[1]));
            Kamperen.slaHaring(a, pennen[2], level.getBlockState(pennen[2]));
            helper.assertTrue(lijn.stap(a) == 1, "three pegs: not yet");
            Kamperen.slaHaring(a, pennen[3], level.getBlockState(pennen[3]));
            helper.assertTrue(lijn.stap(a) == 2 && tel(a, zak) == 0 && !lijn.vlag(a, Kamperen.TENT_VLAG), "four pegs: the tent stands, the bag is used up");

            // b: a's pitch is taken, the other one is free; the tent is turned with its sign
            rol.talk(npc, b);
            Kamperen.zetOp(b, zuid);
            helper.assertTrue(!lijn.vlag(b, Kamperen.TENT_VLAG), "a taken pitch refuses b");
            Kamperen.slaHaring(b, pennen[0], level.getBlockState(pennen[0]));
            helper.assertTrue(Kamperen.haringen(b) == 0, "the pegs of somebody else's tent don't count before b pitched one");
            Kamperen.zetOp(b, oost);
            helper.assertTrue(lijn.vlag(b, Kamperen.TENT_VLAG) && level.getBlockState(oost).getValue(CampingmarktBlocks.Kampeerplek.BEZET)
                    && level.getBlockState(oost).getValue(CampingmarktBlocks.Kampeerplek.FACING) == Direction.EAST, "b's tent stands on the other pitch");
            BoundingBox doos = new BoundingBox(oost.getX() - 4, oost.getY(), oost.getZ() - 3, oost.getX(), oost.getY() + 2, oost.getZ() + 3);
            int pennenOost = 0, doek = 0;
            for (BlockPos q : BlockPos.betweenClosed(doos.minX(), doos.minY(), doos.minZ(), doos.maxX(), doos.maxY(), doos.maxZ())) {
                pennenOost += level.getBlockState(q).is(CampingmarktFeature.HARING.get()) ? 1 : 0;
                doek += level.getBlockState(q).getBlock().getDescriptionId().contains("campingmarkt_tentdoek") ? 1 : 0;
            }
            helper.assertTrue(pennenOost == 4 && doek >= 20, "the turned tent lies west of its sign: " + pennenOost + " pegs, " + doek + " canvas");
            helper.assertTrue(level.getBlockState(oost.west()).isAir() && level.getBlockState(inPlek(level, oost, 3, 2, 1)).is(
                    CampingmarktFeature.TENTDOEK.get("geel").plaat().get()), "its door looks east");
            for (int[] pen : new int[][]{{0, 0}, {6, 0}, {0, 3}, {6, 3}}) {
                BlockPos q = inPlek(level, oost, pen[0], 0, pen[1]);
                Kamperen.slaHaring(b, q, level.getBlockState(q));
            }
            helper.assertTrue(lijn.stap(b) == 2, "b's own four pegs");

            // two minutes later both tents are folded up: the pitches are free again
            Herstel.verschuif(level, Kamperen.TENT_TICKS);
            Herstel.verwerk(level);
            helper.assertTrue(level.getBlockState(zuid) == leeg && level.getBlockState(pennen[0]).isAir() && level.getBlockState(inPlek(level, zuid, 3, 2, 1)).isAir(),
                    "a's pitch is as it was");
            helper.assertTrue(!level.getBlockState(oost).getValue(CampingmarktBlocks.Kampeerplek.BEZET)
                    && level.getBlockState(oost).getValue(CampingmarktBlocks.Kampeerplek.FACING) == Direction.EAST
                    && level.getBlockState(inPlek(level, oost, 3, 2, 1)).isAir(), "b's pitch too, its sign still looking east");
            helper.assertTrue(Kamperen.plaatsTent(level, zuid) && !Kamperen.plaatsTent(level, zuid), "and a tent can be pitched there again (once)");
            helper.assertTrue(!Kamperen.nodig(a, 2).isEmpty() && !Kamperen.nodig(b, 1).isEmpty(), "the Guhdex knows what is needed");
        } finally {
            Herstel.verschuif(level, Kamperen.TENT_TICKS);   // (nothing of this test is put back later, in somebody else's room)
            Herstel.verwerk(level);
            npc.discard();
            weg(helper, a, b);
        }
        helper.succeed();
    }

    /**
     * The fire wood, the camp fire party and the marshmallows: chopping counts only after the Houthakker-guh said so, six
     * logs are a bundle, the bundle lights the fire, the residents come to it, roasting is a matter of timing (too early
     * and too late cost nothing), three golden ones are the rewards (once), and the fire goes out by itself.
     */
    @GuhTest(template = KAMER, batch = "campingmarkt_vuur", timeoutTicks = 200)
    public static void campingmarktHoutVuurEnMarshmallows(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(10, 2, 9)), b = speler(helper, new BlockPos(11, 2, 9));
        GuhNpcEntity baas = npc(helper, GuhNpcEntity.Kind.KAMPBAASGUH, new BlockPos(2, 2, 21));
        GuhNpcEntity hakker = npc(helper, GuhNpcEntity.Kind.HOUTHAKKERGUH, new BlockPos(21, 2, 21));
        Verhaallijn lijn = CampingmarktFeature.CAMPING;
        helper.setBlock(new BlockPos(18, 2, 18), CampingmarktFeature.HAKBLOK.get());
        helper.setBlock(new BlockPos(10, 2, 12), CampingmarktFeature.KAMPVUUR.get());
        BlockPos blok = helper.absolutePos(new BlockPos(18, 2, 18)), vuur = helper.absolutePos(new BlockPos(10, 2, 12));
        GuhEntity bewoner = (GuhEntity) Kamperen.kampeerder(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(16, 2, 12))), Rotation.NONE, 2);
        bewoner.getPersistentData().putString(Bezetting.TAG, Kamperen.KAMPEERDER_ID + 2);
        level.addFreshEntity(bewoner);
        try {
            var baasRol = NpcRollen.van(baas);
            var hakRol = NpcRollen.van(hakker);
            helper.assertTrue(hakRol instanceof Kamperen.HouthakkerRol, "the Houthakker-guh has his role");
            lijn.zet(a, 2);
            // the real click: the log splits and a new one is put on a moment later; before the talk nothing counts
            level.getBlockState(blok).useItemOn(ItemStack.EMPTY, level, a, InteractionHand.MAIN_HAND, raak(blok));
            helper.assertTrue(!level.getBlockState(blok).getValue(CampingmarktBlocks.Hakblok.STAM) && level.getBlockTicks().hasScheduledTick(blok,
                    CampingmarktFeature.HAKBLOK.get()) && lijn.teller(a, Kamperen.HOUT) == 0, "split, a new log is on its way, not counted yet");
            level.getBlockState(blok).useItemOn(ItemStack.EMPTY, level, a, InteractionHand.MAIN_HAND, raak(blok));
            level.getBlockState(blok).tick(level, blok, level.getRandom());
            helper.assertTrue(level.getBlockState(blok).getValue(CampingmarktBlocks.Hakblok.STAM), "the next log stands on the block");
            hakRol.talk(hakker, a);
            for (int i = 0; i < Kamperen.HOUT_NODIG - 1; i++) {
                Kamperen.gehakt(a, blok);
            }
            helper.assertTrue(lijn.stap(a) == 2 && tel(a, CampingmarktFeature.BRANDHOUT.get()) == 0, "five logs: not yet");
            Kamperen.gehakt(b, blok);
            helper.assertTrue(lijn.teller(b, Kamperen.HOUT) == 0, "b is not chopping for him");
            Kamperen.gehakt(a, blok);
            helper.assertTrue(lijn.stap(a) == 3 && tel(a, CampingmarktFeature.BRANDHOUT.get()) == 1, "six logs: a bundle of fire wood");
            a.getInventory().clearContent();
            hakRol.talk(hakker, a);
            helper.assertTrue(tel(a, CampingmarktFeature.BRANDHOUT.get()) == 1, "a lost bundle is given again");

            // the fire: only the wood lights it
            a.getInventory().setSelectedSlot(3);
            level.getBlockState(vuur).useItemOn(ItemStack.EMPTY, level, a, InteractionHand.MAIN_HAND, raak(vuur));
            helper.assertTrue(!level.getBlockState(vuur).getValue(CampingmarktBlocks.KampvuurBlock.BRANDT) && lijn.stap(a) == 3, "an empty hand lights nothing");
            inHand(a, new ItemStack(CampingmarktFeature.BRANDHOUT.get()));
            a.getInventory().setSelectedSlot(0);
            level.getBlockState(vuur).useItemOn(a.getMainHandItem(), level, a, InteractionHand.MAIN_HAND, raak(vuur));
            helper.assertTrue(level.getBlockState(vuur).getValue(CampingmarktBlocks.KampvuurBlock.BRANDT) && lijn.stap(a) == 4
                    && tel(a, CampingmarktFeature.BRANDHOUT.get()) == 0 && Herstel.wacht(level, vuur), "the fire burns, the party is on, and it will go out");
            helper.assertTrue(level.getBlockState(vuur).getLightEmission(level, vuur) == 15 && Kampvuur.brandt(level, vuur), "it gives light");

            // the marshmallows: the stick and a bag from the Kampbaas-guh, and timing
            Item stok = CampingmarktFeature.ROOSTERSTOK.get(), mm = WereldlevenFeature.MARSHMALLOW_KNABBEL.get();
            helper.assertTrue(!RoosterstokItem.begin(a, InteractionHand.MAIN_HAND, vuur), "no marshmallow, no roasting");
            baasRol.talk(baas, a);
            helper.assertTrue(tel(a, stok) == 1 && tel(a, mm) == 4, "the roasting stick and four marshmallows");
            inHand(a, new ItemStack(stok));
            a.getInventory().add(new ItemStack(mm, 4));
            int had = tel(a, mm);
            helper.assertTrue(RoosterstokItem.begin(a, InteractionHand.MAIN_HAND, vuur) && a.isUsingItem(), "a holds the stick over the fire");
            a.stopUsingItem();
            helper.assertTrue(RoosterstokItem.uitkomst(RoosterstokItem.GOUD_VAN - 1) == RoosterstokItem.Uitkomst.KOUD
                    && RoosterstokItem.uitkomst(RoosterstokItem.GOUD_VAN) == RoosterstokItem.Uitkomst.GOUDBRUIN
                    && RoosterstokItem.uitkomst(RoosterstokItem.GOUD_TOT) == RoosterstokItem.Uitkomst.GOUDBRUIN
                    && RoosterstokItem.uitkomst(RoosterstokItem.GOUD_TOT + 1) == RoosterstokItem.Uitkomst.VERKOOLD, "the golden window");
            float gezond = a.getHealth();
            helper.assertTrue(RoosterstokItem.rooster(a, 10) == RoosterstokItem.Uitkomst.KOUD && tel(a, mm) == had && lijn.teller(a, Kamperen.GEROOSTERD) == 0,
                    "too early: cold, nothing lost");
            helper.assertTrue(RoosterstokItem.rooster(a, RoosterstokItem.GOUD_TOT + 20) == RoosterstokItem.Uitkomst.VERKOOLD && tel(a, mm) == had
                    && lijn.teller(a, Kamperen.GEROOSTERD) == 0 && a.getHealth() == gezond, "too late: burnt, nothing lost and nobody hurt");
            helper.assertTrue(RoosterstokItem.rooster(a, 60) == RoosterstokItem.Uitkomst.GOUDBRUIN && tel(a, mm) == had - 1
                    && lijn.teller(a, Kamperen.GEROOSTERD) == 1, "golden brown: eaten and counted");
            baasRol.talk(baas, a);
            helper.assertTrue(lijn.stap(a) == 4, "one is not three");
            RoosterstokItem.rooster(a, 50);
            RoosterstokItem.rooster(a, 70);
            helper.assertTrue(lijn.teller(a, Kamperen.GEROOSTERD) == 3 && lijn.stap(a) == 4, "three golden ones: tell him");
            baasRol.talk(baas, a);
            helper.assertTrue(lijn.klaar(a) && tel(a, CampingmarktFeature.RECEPT_PLANTAGEBAK.get()) == 1, "done: the recipe card of the Plantagebak");
            for (GuhClothes c : List.of(GuhClothes.CAMPINGMARKT_HOEDJE, GuhClothes.CAMPINGMARKT_HALSDOEK, GuhClothes.CAMPINGMARKT_RUGZAK)) {
                helper.assertTrue(tel(a, ModItems.clothingItem(c)) == 1 && "camping_markt".equals(KledingBronnen.bron(c)), "the camping outfit: " + c);
            }
            baasRol.talk(baas, a);
            helper.assertTrue(tel(a, CampingmarktFeature.RECEPT_PLANTAGEBAK.get()) == 1 && tel(a, ModItems.clothingItem(GuhClothes.CAMPINGMARKT_HOEDJE)) == 1,
                    "the rewards only once");
            a.getInventory().clearContent();
            baasRol.talk(baas, a);
            helper.assertTrue(tel(a, CampingmarktFeature.RECEPT_PLANTAGEBAK.get()) == 1 && baasRol.offers(baas) != null && !baasRol.offers(baas).isEmpty(),
                    "a lost recipe card is given again, and he has a little shop");
            helper.assertTrue(lijn.stap(b) == 0 && !CampingmarktFeature.CAMPING.stand(a).beloningen().isEmpty(), "b is where b was");
            // after the questline roasting still works (for fun), and whoever did step 3 pokes a dead fire up again
            a.getInventory().add(new ItemStack(mm, 2));
            helper.assertTrue(RoosterstokItem.rooster(a, 60) == RoosterstokItem.Uitkomst.GOUDBRUIN && tel(a, mm) == 1, "roasting for fun");
        } finally {
            baas.discard();
            hakker.discard();
            weg(helper, b);
        }
        // the burning fire calls the residents (it ticks by itself); then, two minutes later, it has gone out
        boolean[] uit = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(LuisterGoal.luistert(bewoner), "the resident comes to the fire");
            if (!uit[0]) {
                uit[0] = true;
                Herstel.verschuif(level, Kampvuur.BRAND_TICKS);
                Herstel.verwerk(level);
                helper.assertTrue(!level.getBlockState(vuur).getValue(CampingmarktBlocks.KampvuurBlock.BRANDT), "the fire went out by itself");
                Kampvuur.klik(a, vuur, level.getBlockState(vuur), InteractionHand.MAIN_HAND);
                helper.assertTrue(level.getBlockState(vuur).getValue(CampingmarktBlocks.KampvuurBlock.BRANDT), "a pokes it up again");
                Herstel.verschuif(level, Kampvuur.BRAND_TICKS);   // (nothing of this test is put back later, in somebody else's room)
                Herstel.verwerk(level);
                bewoner.discard();
                weg(helper, a);
            }
        });
    }

    /** One player does the whole questline of the camping at this pitch, this chopping block and this fire, with the real step calls. */
    private static void kampeer(GameTestHelper helper, ServerPlayer p, GuhNpcEntity baas, GuhNpcEntity hakker, BlockPos sign, BlockPos blok, BlockPos vuur, String wie) {
        ServerLevel level = helper.getLevel();
        Verhaallijn lijn = CampingmarktFeature.CAMPING;
        var baasRol = NpcRollen.van(baas);
        var hakRol = NpcRollen.van(hakker);
        baasRol.talk(baas, p);
        helper.assertTrue(lijn.stap(p) == 1 && tel(p, CampingmarktFeature.TENTZAK.get()) == 1, wie + " begins: the tent bag");
        Kamperen.zetOp(p, sign);
        helper.assertTrue(lijn.vlag(p, Kamperen.TENT_VLAG) && level.getBlockState(sign).getValue(CampingmarktBlocks.Kampeerplek.BEZET), wie + " pitches a tent on the pitch");
        for (int[] pen : new int[][]{{0, 0}, {6, 0}, {0, 3}, {6, 3}}) {
            BlockPos q = inPlek(level, sign, pen[0], 0, pen[1]);
            Kamperen.slaHaring(p, q, level.getBlockState(q));
        }
        helper.assertTrue(lijn.stap(p) == 2, wie + ": four pegs");
        hakRol.talk(hakker, p);
        for (int i = 0; i < Kamperen.HOUT_NODIG; i++) {
            Kamperen.gehakt(p, blok);
        }
        helper.assertTrue(lijn.stap(p) == 3 && tel(p, CampingmarktFeature.BRANDHOUT.get()) == 1, wie + ": a bundle of fire wood");
        inHand(p, new ItemStack(CampingmarktFeature.BRANDHOUT.get()));
        Kampvuur.klik(p, vuur, level.getBlockState(vuur), InteractionHand.MAIN_HAND);
        helper.assertTrue(lijn.stap(p) == 4 && Kampvuur.brandt(level, vuur), wie + ": the wood is on the fire, the party is on");
        p.getInventory().clearContent();
        baasRol.talk(baas, p);
        helper.assertTrue(tel(p, CampingmarktFeature.ROOSTERSTOK.get()) == 1 && Kamperen.marshmallows(p) == 4, wie + ": the roasting stick and marshmallows");
        for (int i = 0; i < Kamperen.MARSHMALLOWS; i++) {
            helper.assertTrue(RoosterstokItem.rooster(p, RoosterstokItem.GOUD_VAN + 5) == RoosterstokItem.Uitkomst.GOUDBRUIN, wie + ": golden brown " + (i + 1));
        }
        baasRol.talk(baas, p);
        helper.assertTrue(lijn.klaar(p) && tel(p, CampingmarktFeature.RECEPT_PLANTAGEBAK.get()) == 1
                && tel(p, ModItems.clothingItem(GuhClothes.CAMPINGMARKT_HOEDJE)) == 1, wie + " is done: the recipe card and the camping outfit");
    }

    /**
     * Any number of players, one after the other: a first player does the whole questline of the camping; a second one who
     * only begins then does it all again, at the same pitch (folded up by itself), the same chopping block and the fire that
     * still burns from the first player's party; and a third one after the fire went out.
     */
    @GuhTest(template = KAMER, batch = "campingmarkt_tweede")
    public static void campingmarktTweedeSpelerNaDeEerste(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(10, 2, 9)), b = speler(helper, new BlockPos(11, 2, 9)), c = speler(helper, new BlockPos(12, 2, 9));
        GuhNpcEntity baas = npc(helper, GuhNpcEntity.Kind.KAMPBAASGUH, new BlockPos(2, 2, 21));
        GuhNpcEntity hakker = npc(helper, GuhNpcEntity.Kind.HOUTHAKKERGUH, new BlockPos(21, 2, 21));
        try {
            Verhaallijn lijn = CampingmarktFeature.CAMPING;
            helper.setBlock(new BlockPos(18, 2, 18), CampingmarktFeature.HAKBLOK.get());
            helper.setBlock(new BlockPos(10, 2, 12), CampingmarktFeature.KAMPVUUR.get());
            BlockPos blok = helper.absolutePos(new BlockPos(18, 2, 18)), vuur = helper.absolutePos(new BlockPos(10, 2, 12));
            BlockPos sign = bord(helper, new BlockPos(8, 2, 20), Direction.SOUTH);
            BlockState leeg = level.getBlockState(sign);
            kampeer(helper, a, baas, hakker, sign, blok, vuur, "a");
            helper.assertTrue(lijn.stap(b) == 0 && lijn.stap(c) == 0 && !lijn.begonnen(b), "nothing a did moved the story of anybody else");
            // b begins after a is done. The tent of a still stands on the only pitch: taken, until it is folded up by itself
            NpcRollen.van(baas).talk(baas, b);
            Kamperen.zetOp(b, sign);
            helper.assertTrue(!lijn.vlag(b, Kamperen.TENT_VLAG), "the pitch is still taken by the tent of a");
            Herstel.verschuif(level, Kamperen.TENT_TICKS);
            Herstel.verwerk(level);
            helper.assertTrue(level.getBlockState(sign) == leeg && !Kampvuur.brandt(level, vuur), "two minutes later the pitch is free and the fire is out");
            lijn.wis(b);
            b.getInventory().clearContent();
            kampeer(helper, b, baas, hakker, sign, blok, vuur, "b (after a)");
            // c begins while a fire already burns (the pitch is free again): wood on a burning fire counts for c too
            Herstel.verschuif(level, Kamperen.TENT_TICKS);
            Herstel.verwerk(level);
            helper.assertTrue(level.getBlockState(sign) == leeg, "(the tent of b is folded up)");
            Kampvuur.klik(b, vuur, level.getBlockState(vuur), InteractionHand.OFF_HAND);
            helper.assertTrue(Kampvuur.brandt(level, vuur), "b (who had the party) pokes the fire up again");
            kampeer(helper, c, baas, hakker, sign, blok, vuur, "c (after a and b, at a fire that already burns)");
            helper.assertTrue(lijn.klaar(a) && lijn.klaar(b) && lijn.klaar(c), "all three are done");
        } finally {
            Herstel.verschuif(level, Kamperen.TENT_TICKS + Kampvuur.BRAND_TICKS);   // (nothing of this test is put back later, in somebody else's room)
            Herstel.verwerk(level);
            baas.discard();
            hakker.discard();
            weg(helper, a, b, c);
        }
        helper.succeed();
    }

    /**
     * A vads bar that is THROWN on the ground for a wild Nether-Mika: the extra present of the ruilmarkt belongs to its
     * thrower, when that is a certified customer, and to nobody else (not to a certified customer who happens to stand
     * closest). A bar that nobody gave it (a dispenser) still counts for the customer who stands closest.
     */
    @GuhTest(template = KAMER, batch = "campingmarkt_ruil")
    public static void campingmarktGegooideStaaf(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer klant = speler(helper, new BlockPos(7, 2, 6)), vreemde = speler(helper, new BlockPos(12, 2, 6));
        MikaEntity wild = ModEntities.NETHER_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(24, 10, 24);
        try {
            BlockPos bij = helper.absolutePos(new BlockPos(6, 2, 6));
            wild.snapTo(bij.getX() + 0.5, bij.getY(), bij.getZ() + 0.5);
            wild.setNoAi(true);
            level.addFreshEntity(wild);
            CampingmarktFeature.RUILMARKT.zet(klant, CampingmarktFeature.RUILMARKT.stappen());
            CompoundTag data = wild.getPersistentData();
            // the bar of somebody who is no customer, with a customer right next to the Mika: no extra for anybody
            data.store(Ruilmarkt.RUIL_PARTNER, UUIDUtil.CODEC, vreemde.getUUID());
            Ruilmarkt.snuffelt(wild);
            helper.assertTrue(Ruilmarkt.GEEN_KLANT.equals(data.getStringOr(Ruilmarkt.KLANT, "")), "the Mika sniffs a bar that is not a customer's");
            BehaviorUtils.throwItem(wild, new ItemStack(Items.CHARCOAL), vreemde.position().add(0, 1, 0));
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, kamer).size() == 1 && data.getStringOr(Ruilmarkt.KLANT, "").isEmpty(),
                    "one present, no extra for the customer who only stood closest: " + level.getEntitiesOfClass(ItemEntity.class, kamer).size());
            level.getEntitiesOfClass(ItemEntity.class, kamer).forEach(Entity::discard);
            // the bar a customer threw from across the room, while the stranger stands next to the Mika: the extra is the customer's
            klant.snapTo(bij.getX() + 10.5, bij.getY(), bij.getZ() + 0.5);
            vreemde.snapTo(bij.getX() + 1.5, bij.getY(), bij.getZ() + 0.5);
            data.remove(Ruilmarkt.EXTRA_TIJD);
            data.store(Ruilmarkt.RUIL_PARTNER, UUIDUtil.CODEC, klant.getUUID());
            Ruilmarkt.snuffelt(wild);
            helper.assertTrue(klant.getUUID().toString().equals(data.getStringOr(Ruilmarkt.KLANT, "")), "the Mika knows whose bar it sniffs");
            BehaviorUtils.throwItem(wild, new ItemStack(Items.CHARCOAL), klant.position().add(0, 1, 0));
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, kamer).size() >= 2, "the thrower gets the second present");
            level.getEntitiesOfClass(ItemEntity.class, kamer).forEach(Entity::discard);
            // a bar from nobody (a dispenser): the customer who stands closest, as before
            klant.snapTo(bij.getX() + 3.5, bij.getY(), bij.getZ() + 0.5);
            data.remove(Ruilmarkt.EXTRA_TIJD);
            data.remove(Ruilmarkt.RUIL_PARTNER);
            Ruilmarkt.snuffelt(wild);
            helper.assertTrue(data.getStringOr(Ruilmarkt.KLANT, "").isEmpty(), "nobody's bar");
            BehaviorUtils.throwItem(wild, new ItemStack(Items.CHARCOAL), klant.position().add(0, 1, 0));
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, kamer).size() >= 2, "a bar from a dispenser: the nearest customer");
            level.getEntitiesOfClass(ItemEntity.class, kamer).forEach(Entity::discard);
            // the real thing: the Mika takes the stranger's bar (the Nether-Mika's own code writes down whose it is), and the
            // game tells us a tick later that it holds one
            data.remove(Ruilmarkt.EXTRA_TIJD);
            helper.assertTrue(NetherMikaRuil.offer(wild, vreemde, new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get())), "the Mika takes the stranger's bar");
            helper.assertTrue(data.read(Ruilmarkt.RUIL_PARTNER, UUIDUtil.CODEC).map(vreemde.getUUID()::equals).orElse(false),
                    "feature/spiesburcht keeps its partner where the ruilmarkt looks for it");
        } catch (RuntimeException | Error e) {
            wild.discard();
            weg(helper, klant, vreemde);
            throw e;
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(Ruilmarkt.GEEN_KLANT.equals(wild.getPersistentData().getStringOr(Ruilmarkt.KLANT, "")), "the bar in its paw was noticed by itself");
            wild.discard();
            weg(helper, klant, vreemde);
        });
    }

    /** The residents: who they are, dressed for camping, never tamed or fed, and they keep to the camping. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void campingmarktKampeerders(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        GuhEntity gewoon = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        try {
            BlockPos thuis = helper.absolutePos(new BlockPos(12, 2, 12));
            for (int i = 0; i < Kamperen.KAMPEERDERS.size(); i++) {
                Kamperen.Kampeerder k = Kamperen.KAMPEERDERS.get(i);
                GuhEntity guh = (GuhEntity) Kamperen.kampeerder(level, Vec3.atBottomCenterOf(thuis), Rotation.NONE, i);
                helper.assertTrue(guh != null && guh.getVariant() == GuhVariant.byId(k.variant()) && guh.isOrderedToSit() == k.zit()
                        && Math.abs(guh.getGuhScale() - k.schaal()) < 0.01f, "resident " + i + " is who the builder says");
                helper.assertTrue(guh.getClothes(GuhClothes.Slot.HEAD) == k.hoofd() && guh.getClothes(GuhClothes.Slot.NECK) == k.nek()
                        && guh.getClothes(GuhClothes.Slot.BACK) == k.rug(), "resident " + i + " wears its camping clothes");
                helper.assertTrue(Kamperen.kampeerder(guh) == -1, "not a resident before it has its tag");
                guh.getPersistentData().putString(Bezetting.TAG, Kamperen.KAMPEERDER_ID + i + "@12345");
                helper.assertTrue(Kamperen.kampeerder(guh) == i, "resident " + i + " is known by its tag");
                if (i == 2) {
                    level.addFreshEntity(guh);
                    // a click: a chat, never a taming (not even with knabbels in the hand)
                    inHand(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 8));
                    helper.assertTrue(guh.mobInteract(p, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && !guh.isTame()
                            && tel(p, ModItems.KAAS_KNABBELS.get()) == 8, "a resident is never fed or tamed");
                    // its first spot is its home; strayed far, it is put back
                    Kamperen.houdThuis(guh);
                    helper.assertTrue(guh.hasHome() && guh.getHomePosition().equals(thuis), "it lives where it stood");
                    guh.snapTo(thuis.getX() + 0.5 + Kamperen.THUIS_TERUG + 6, thuis.getY(), thuis.getZ() + 0.5);
                    Kamperen.houdThuis(guh);
                    helper.assertTrue(guh.blockPosition().equals(thuis), "strayed: put back at " + guh.blockPosition());
                    guh.discard();
                }
            }
            gewoon.snapTo(thuis.getX() + 0.5, thuis.getY(), thuis.getZ() + 2.5);
            level.addFreshEntity(gewoon);
            helper.assertTrue(Kamperen.bewonerKlik(gewoon, p, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "any other guh is left alone");

            // the Knabbelring: a wild guh trots after its bearer, drooling; a resident of the camping stays where it is
            GuhEntity bewoner = (GuhEntity) Kamperen.kampeerder(level, Vec3.atBottomCenterOf(thuis.offset(2, 0, 0)), Rotation.NONE, 3);
            bewoner.getPersistentData().putString(Bezetting.TAG, Kamperen.KAMPEERDER_ID + 3 + "@12345");
            level.addFreshEntity(bewoner);
            p.snapTo(thuis.getX() + 0.5, thuis.getY(), thuis.getZ() - 6.5);
            helper.assertTrue(!bewoner.isOrderedToSit() && !Kamperen.nietKwijlen(bewoner) && !GuhHooks.isBezig(bewoner), "nobody with a ring near: the resident has its own day");
            helper.assertTrue(Ring.geef(p) && Ring.heeft(p), "p carries the Knabbelring");
            p.tickCount = 40 - p.getId() % 20;
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));          // (the ring's own once-a-second look: p is a ring bearer now)
            // half a second before the ring's hook looks at this guh, the camping's hook marked it
            bewoner.tickCount = 50 - bewoner.getId() % 40;
            GuhHooks.runTick(bewoner);
            helper.assertTrue(GuhHooks.isBezig(bewoner), "a ring bearer within " + Kamperen.RING_BEREIK + " blocks: the resident is busy");
            bewoner.tickCount += 10;
            gewoon.tickCount = 40 - gewoon.getId() % 20;
            bewoner.setOnGround(true);   // (neither guh has ticked yet, and a mob only looks for a path while it stands on something)
            gewoon.setOnGround(true);
            GuhHooks.runTick(bewoner);
            GuhHooks.runTick(gewoon);
            helper.assertTrue(!gewoon.getNavigation().isDone(), "(a wild guh does walk to the ring: the hook is live in this test)");
            helper.assertTrue(bewoner.getNavigation().isDone(), "the resident does not trot after the ring");
            Ring.neem(p);
            p.snapTo(thuis.getX() + 0.5 + Kamperen.RING_BEREIK + 4, thuis.getY(), thuis.getZ() + 0.5);
            helper.assertTrue(!Kamperen.nietKwijlen(bewoner), "no ring, or far away: not marked again");
            helper.assertTrue(bewoner.getPersistentData().getLongOr("guhs_knus_bezig_tot", 0L) <= level.getGameTime() + Kamperen.RING_BEZIG, "and busy for a few seconds only");
            p.tickCount = 40 - p.getId() % 20;
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
            bewoner.discard();
        } finally {
            gewoon.discard();
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the market
    // =================================================================================================================

    /** Haggling: the fitting answer lowers the price, three wrong ones get you shoved away (unhurt), and a deal is the step. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void campingmarktAfdingen(GameTestHelper helper) {
        ServerPlayer a = speler(helper, new BlockPos(11, 2, 13)), b = speler(helper, new BlockPos(13, 2, 13));
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.MARKTMEESTER_MIKA, new BlockPos(12, 2, 12));
        try {
            Verhaallijn lijn = CampingmarktFeature.RUILMARKT;
            var rol = NpcRollen.van(npc);
            helper.assertTrue(rol instanceof Ruilmarkt.MarktmeesterRol, "the Marktmeester-Mika has his role");
            rol.talk(npc, a);
            helper.assertTrue(lijn.stap(a) == 1 && Ruilmarkt.bezig(a) == null, "a began");
            rol.talk(npc, a);
            Ruilmarkt.Afdingen h = Ruilmarkt.bezig(a);
            helper.assertTrue(h != null && h.prijs == Ruilmarkt.BEGINPRIJS && h.geduld == Ruilmarkt.GEDULD, "the haggle starts at his price");
            // one wrong answer costs patience, not price; the fitting ones bring the price down
            int fout = (h.stemming + 1) % 3 + 1;
            helper.assertTrue(Ruilmarkt.zet(npc, a, fout) == Ruilmarkt.Zet.VERDER && h.prijs == Ruilmarkt.BEGINPRIJS && h.geduld == Ruilmarkt.GEDULD - 1,
                    "a wrong answer");
            int oud = h.stemming;
            helper.assertTrue(Ruilmarkt.zet(npc, a, h.stemming + 1) == Ruilmarkt.Zet.VERDER && h.prijs == Ruilmarkt.BEGINPRIJS - Ruilmarkt.KORTING,
                    "the fitting answer");
            helper.assertTrue(h.stemming != oud, "never the same mood twice in a row");
            helper.assertTrue(Ruilmarkt.zet(npc, a, h.stemming + 1) == Ruilmarkt.Zet.VERDER, "again");
            helper.assertTrue(Ruilmarkt.zet(npc, a, h.stemming + 1) == Ruilmarkt.Zet.GEWONNEN && h.prijs <= Ruilmarkt.DOELPRIJS && lijn.stap(a) == 1,
                    "low enough: the deal screen");
            helper.assertTrue(Ruilmarkt.zet(npc, a, 2) == Ruilmarkt.Zet.NIETS && Ruilmarkt.zet(npc, a, 1) == Ruilmarkt.Zet.DEAL, "only 'Deal!' closes it");
            helper.assertTrue(lijn.stap(a) == 2 && tel(a, CampingmarktFeature.KEURSTEMPEL.get()) == 1 && Ruilmarkt.bezig(a) == null, "learnt: the Keurstempel");
            int nep = Ruilmarkt.nep(a);
            helper.assertTrue(nep >= 1 && nep <= Ruilmarkt.STAPELS && Ruilmarkt.nep(a) == nep, "a's fake stack is drawn, and stays");

            // b only gives wrong answers: sent away with a shove, unhurt, nothing lost, and simply starts again
            rol.talk(npc, b);
            rol.talk(npc, b);
            b.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 30));
            float gezond = b.getHealth();
            Ruilmarkt.Afdingen hb = Ruilmarkt.bezig(b);
            Ruilmarkt.Zet laatste = Ruilmarkt.Zet.NIETS;
            for (int i = 0; i < Ruilmarkt.GEDULD; i++) {
                laatste = Ruilmarkt.zet(npc, b, (hb.stemming + 1) % 3 + 1);
            }
            helper.assertTrue(laatste == Ruilmarkt.Zet.WEGGESTUURD && Ruilmarkt.bezig(b) == null && lijn.stap(b) == 1, "out of patience: sent away");
            helper.assertTrue(b.getHealth() == gezond && tel(b, ModItems.KAAS_KNABBELS.get()) == 30, "unhurt, and haggling costs no knabbels");
            rol.talk(npc, b);
            helper.assertTrue(Ruilmarkt.bezig(b) != null && Ruilmarkt.bezig(b).prijs == Ruilmarkt.BEGINPRIJS && Ruilmarkt.bezig(b).geduld == Ruilmarkt.GEDULD,
                    "b starts again with a clean slate");
            helper.assertTrue(!Ruilmarkt.nodig(b, 1).isEmpty() && !Ruilmarkt.nodig(a, 2).isEmpty(), "the Guhdex knows what is needed");
        } finally {
            npc.discard();
            weg(helper, a, b);
        }
        helper.succeed();
    }

    /**
     * The fake vads: weighing two stacks shows the lighter one on the scales, stamping needs a weighing first, a wrong
     * stamp costs nothing but the stacks are swapped, the right one is the step; every player has a fake of their own. Then
     * the reward (once), the scales at home, and the bargain of the day.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void campingmarktNepvads(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(6, 2, 8)), b = speler(helper, new BlockPos(7, 2, 8));
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.MARKTMEESTER_MIKA, new BlockPos(3, 2, 20));
        try {
            Verhaallijn lijn = CampingmarktFeature.RUILMARKT;
            var rol = NpcRollen.van(npc);
            BlockPos[] stapel = new BlockPos[Ruilmarkt.STAPELS + 1];
            for (int n = 1; n <= Ruilmarkt.STAPELS; n++) {
                helper.setBlock(new BlockPos(3 + n, 2, 4), CampingmarktFeature.VADSSTAPEL.get().defaultBlockState().setValue(CampingmarktBlocks.Vadsstapel.NUMMER, n));
                stapel[n] = helper.absolutePos(new BlockPos(3 + n, 2, 4));
            }
            helper.setBlock(new BlockPos(6, 2, 6), CampingmarktFeature.WEEGSCHAAL.get());
            BlockPos schaal = helper.absolutePos(new BlockPos(6, 2, 6));
            ItemStack stempel = new ItemStack(CampingmarktFeature.KEURSTEMPEL.get());
            // the pure rule: the fake is lighter
            helper.assertTrue(Ruilmarkt.weeg(3, 3, 4) == CampingmarktBlocks.Stand.RECHTS && Ruilmarkt.weeg(3, 5, 3) == CampingmarktBlocks.Stand.LINKS
                    && Ruilmarkt.weeg(3, 1, 2) == CampingmarktBlocks.Stand.MIDDEN, "the scales go down on the heavy side");
            // not yet at this step: hands off
            Ruilmarkt.klikStapel(a, stapel[1], 1, ItemStack.EMPTY);
            helper.assertTrue(lijn.teller(a, Ruilmarkt.WEGINGEN) == 0, "nothing before the haggling is learnt");
            lijn.zet(a, 2);
            lijn.zet(b, 2);
            lijn.teller(a, Ruilmarkt.NEP, 3);
            lijn.teller(b, Ruilmarkt.NEP, 5);
            a.getInventory().add(stempel.copy());
            // stamping without weighing is refused
            Ruilmarkt.klikStapel(a, stapel[3], 3, stempel);
            helper.assertTrue(lijn.stap(a) == 2 && Ruilmarkt.nep(a) == 3, "first weigh, then stamp");
            // 1 against 2: level
            level.getBlockState(stapel[1]).useItemOn(ItemStack.EMPTY, level, a, InteractionHand.MAIN_HAND, raak(stapel[1]));
            helper.assertTrue(lijn.teller(a, Ruilmarkt.WEGINGEN) == 0, "one stack on the scales is no weighing yet");
            level.getBlockState(stapel[2]).useItemOn(ItemStack.EMPTY, level, a, InteractionHand.MAIN_HAND, raak(stapel[2]));
            helper.assertTrue(lijn.teller(a, Ruilmarkt.WEGINGEN) == 1 && level.getBlockState(schaal).getValue(CampingmarktBlocks.Weegschaal.STAND)
                    == CampingmarktBlocks.Stand.MIDDEN, "two real stacks: level");
            // 3 (the fake, left) against 4: down on the right; the scales swing back by themselves
            Ruilmarkt.klikStapel(a, stapel[3], 3, ItemStack.EMPTY);
            Ruilmarkt.klikStapel(a, stapel[4], 4, ItemStack.EMPTY);
            helper.assertTrue(level.getBlockState(schaal).getValue(CampingmarktBlocks.Weegschaal.STAND) == CampingmarktBlocks.Stand.RECHTS
                    && level.getBlockTicks().hasScheduledTick(schaal, CampingmarktFeature.WEEGSCHAAL.get()), "the fake on the left: down on the right");
            level.getBlockState(schaal).tick(level, schaal, level.getRandom());
            helper.assertTrue(level.getBlockState(schaal).getValue(CampingmarktBlocks.Weegschaal.STAND) == CampingmarktBlocks.Stand.MIDDEN, "swung back");
            // b weighs the very same stacks and sees something else: b's fake is another one
            Ruilmarkt.klikStapel(b, stapel[3], 3, ItemStack.EMPTY);
            Ruilmarkt.klikStapel(b, stapel[5], 5, ItemStack.EMPTY);
            helper.assertTrue(level.getBlockState(schaal).getValue(CampingmarktBlocks.Weegschaal.STAND) == CampingmarktBlocks.Stand.LINKS, "b's fake is stack 5");
            // a stamps the wrong stack: nothing lost, but the stacks are swapped and a has to weigh again
            Ruilmarkt.klikStapel(a, stapel[1], 1, stempel);
            helper.assertTrue(lijn.stap(a) == 2 && Ruilmarkt.nep(a) != 1 && lijn.teller(a, Ruilmarkt.WEGINGEN) == 0
                    && tel(a, CampingmarktFeature.KEURSTEMPEL.get()) == 1, "a wrong stamp: weigh again");
            int nep = Ruilmarkt.nep(a);
            int ander = nep == 5 ? 4 : nep + 1;
            Ruilmarkt.klikStapel(a, stapel[nep], nep, stempel);
            helper.assertTrue(lijn.stap(a) == 2, "still: first weigh");
            Ruilmarkt.klikStapel(a, stapel[ander], ander, ItemStack.EMPTY);
            Ruilmarkt.klikStapel(a, stapel[ander], ander, ItemStack.EMPTY);
            helper.assertTrue(lijn.teller(a, Ruilmarkt.WEGINGEN) == 0, "the same stack twice takes it off the scales again");
            Ruilmarkt.klikStapel(a, stapel[ander], ander, ItemStack.EMPTY);
            Ruilmarkt.klikStapel(a, stapel[nep], nep, ItemStack.EMPTY);
            Ruilmarkt.klikStapel(a, stapel[nep], nep, stempel);
            helper.assertTrue(lijn.stap(a) == 3 && lijn.stap(b) == 2 && Ruilmarkt.nep(b) == 5, "unmasked! (and b's puzzle is untouched)");
            for (int n = 1; n <= Ruilmarkt.STAPELS; n++) {
                helper.assertTrue(level.getBlockState(stapel[n]).getValue(CampingmarktBlocks.Vadsstapel.NUMMER) == n, "the stacks themselves never change");
            }
            // the Marktmeester: the scales, once; the stamp goes back
            rol.talk(npc, a);
            helper.assertTrue(lijn.klaar(a) && tel(a, CampingmarktFeature.WEEGSCHAAL_ITEM.get()) == 1 && tel(a, CampingmarktFeature.KEURSTEMPEL.get()) == 0,
                    "done: the scales");
            // b goes on now that a is done: the same stacks, b's own fake (stack 5), the same reward
            helper.assertTrue(!Ruilmarkt.koopjeVrij(b), "(no bargain of the day before the questline is done)");
            b.getInventory().add(stempel.copy());
            Ruilmarkt.klikStapel(b, stapel[4], 4, ItemStack.EMPTY);
            Ruilmarkt.klikStapel(b, stapel[5], 5, ItemStack.EMPTY);
            Ruilmarkt.klikStapel(b, stapel[5], 5, stempel);
            helper.assertTrue(lijn.stap(b) == 3, "b unmasks their own fake after a did");
            rol.talk(npc, b);
            helper.assertTrue(lijn.klaar(b) && tel(b, CampingmarktFeature.WEEGSCHAAL_ITEM.get()) == 1 && tel(b, CampingmarktFeature.KEURSTEMPEL.get()) == 0,
                    "the second player finishes after the first, with the same reward");
            // and somebody who only arrives now: haggling, a fake of their own, the scales
            ServerPlayer c = speler(helper, new BlockPos(8, 2, 8));
            try {
                rol.talk(npc, c);
                rol.talk(npc, c);
                Ruilmarkt.Afdingen hc = Ruilmarkt.bezig(c);
                helper.assertTrue(lijn.stap(c) == 1 && hc != null && hc.prijs == Ruilmarkt.BEGINPRIJS, "the newcomer haggles from his first price");
                Ruilmarkt.Zet zc = Ruilmarkt.Zet.VERDER;
                for (int i = 0; i < 6 && zc == Ruilmarkt.Zet.VERDER; i++) {
                    zc = Ruilmarkt.zet(npc, c, hc.stemming + 1);
                }
                helper.assertTrue(zc == Ruilmarkt.Zet.GEWONNEN && Ruilmarkt.zet(npc, c, 1) == Ruilmarkt.Zet.DEAL && lijn.stap(c) == 2
                        && tel(c, CampingmarktFeature.KEURSTEMPEL.get()) == 1, "learnt: the Keurstempel");
                int nepC = Ruilmarkt.nep(c), anderC = nepC == 5 ? 4 : nepC + 1;
                Ruilmarkt.klikStapel(c, stapel[anderC], anderC, ItemStack.EMPTY);
                Ruilmarkt.klikStapel(c, stapel[nepC], nepC, ItemStack.EMPTY);
                Ruilmarkt.klikStapel(c, stapel[nepC], nepC, stempel);
                rol.talk(npc, c);
                helper.assertTrue(lijn.klaar(c) && tel(c, CampingmarktFeature.WEEGSCHAAL_ITEM.get()) == 1, "and is done: any number of players, one after the other");
            } finally {
                weg(helper, c);
            }
            // at home the scales weigh what you hold: the fuller hand is the heavier
            inHand(a, new ItemStack(Items.STICK, 2));
            a.getInventory().setItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND, new ItemStack(Items.STICK, 5));
            level.getBlockState(schaal).useItemOn(a.getMainHandItem(), level, a, InteractionHand.MAIN_HAND, raak(schaal));
            helper.assertTrue(level.getBlockState(schaal).getValue(CampingmarktBlocks.Weegschaal.STAND) == CampingmarktBlocks.Stand.RECHTS, "2 against 5: down on the right");
            a.getInventory().clearContent();
            // the bargain of the day: once a day, for a haggle won
            helper.assertTrue(Ruilmarkt.koopjeVrij(a), "a may haggle for the bargain of the day");
            rol.talk(npc, a);
            helper.assertTrue(Ruilmarkt.bezig(a) == null, "he asks first: the bargain, or the shop?");
            rol.antwoord(npc, a, Ruilmarkt.KOOPJE_OPTIE);
            Ruilmarkt.Afdingen h = Ruilmarkt.bezig(a);
            helper.assertTrue(h != null, "a haggle for the bargain");
            Ruilmarkt.Zet z = Ruilmarkt.Zet.VERDER;
            for (int i = 0; i < 6 && z == Ruilmarkt.Zet.VERDER; i++) {
                z = Ruilmarkt.zet(npc, a, h.stemming + 1);
            }
            helper.assertTrue(z == Ruilmarkt.Zet.GEWONNEN && Ruilmarkt.zet(npc, a, 1) == Ruilmarkt.Zet.DEAL, "won");
            int stuks = 0;
            for (ItemStack s : a.getInventory().getNonEquipmentItems()) {
                stuks += s.getCount();
            }
            helper.assertTrue(stuks >= 1 && stuks <= 10 && !Ruilmarkt.koopjeVrij(a) && tel(a, CampingmarktFeature.WEEGSCHAAL_ITEM.get()) == 0,
                    "a modest bargain, once a day: " + stuks);
            helper.assertTrue(rol.offers(npc) != null && !rol.offers(npc).isEmpty() && !CampingmarktFeature.RUILMARKT.stand(a).beloningen().isEmpty(),
                    "he has a shop, and the Guhdex knows the rewards");
        } finally {
            npc.discard();
            weg(helper, a, b);
        }
        helper.succeed();
    }

    private static int geteld(ServerLevel level, AABB waar) {
        return level.getEntitiesOfClass(ItemEntity.class, waar).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    /**
     * Better barter rates: a stall holder barters on the spot, is no monster and can't be hurt; a certified customer gets a
     * second present there, and from a wild Nether-Mika too (once per barter, never for a barter that was broken off).
     */
    @GuhTest(template = KAMER, batch = "campingmarkt_ruil")
    public static void campingmarktRuilenMetExtraatje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(6, 2, 6)), b = speler(helper, new BlockPos(18, 2, 18));
        KraamMikaEntity kraam = CampingmarktFeature.KRAAM_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
        MikaEntity wild = ModEntities.NETHER_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
        try {
            AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(24, 10, 24);
            BlockPos bijA = helper.absolutePos(new BlockPos(6, 2, 8)), bijB = helper.absolutePos(new BlockPos(18, 2, 20));
            kraam.setKraam(1);
            kraam.snapTo(bijA.getX() + 0.5, bijA.getY(), bijA.getZ() + 0.5);
            level.addFreshEntity(kraam);
            helper.assertTrue(!(kraam instanceof Enemy) && !kraam.removeWhenFarAway(1000) && kraam.getKraam() == 1 && kraam.hasCustomName(),
                    "a stall holder is no monster and never leaves");
            float gezond = kraam.getHealth();
            kraam.hurtServer(level, level.damageSources().playerAttack(a), 8f);
            helper.assertTrue(kraam.getHealth() == gezond, "it can't be hurt");
            // without vads: a chat; with a bar: a present, at once
            Ruilmarkt.kraam(a, kraam, InteractionHand.MAIN_HAND);
            helper.assertTrue(geteld(level, kamer) == 0, "no bar, no barter");
            inHand(a, new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get(), 3));
            kraam.getPersistentData().remove(Ruilmarkt.EXTRA_TIJD);
            helper.assertTrue(kraam.mobInteract(a, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "a click on the stall holder");
            int eerste = geteld(level, kamer);
            helper.assertTrue(tel(a, ModItems.VAHOEGE_VADS_INGOT.get()) == 2 && eerste >= 1, "one bar, one present: " + eerste);
            level.getEntitiesOfClass(ItemEntity.class, kamer).forEach(Entity::discard);
            // a certified customer: two presents for one bar
            CampingmarktFeature.RUILMARKT.zet(a, CampingmarktFeature.RUILMARKT.stappen());
            kraam.getPersistentData().remove(Ruilmarkt.EXTRA_TIJD);
            Ruilmarkt.kraam(a, kraam, InteractionHand.MAIN_HAND);
            helper.assertTrue(tel(a, ModItems.VAHOEGE_VADS_INGOT.get()) == 1 && level.getEntitiesOfClass(ItemEntity.class, kamer).size() >= 2,
                    "certified: a second present");
            level.getEntitiesOfClass(ItemEntity.class, kamer).forEach(Entity::discard);

            // a wild Nether-Mika: it remembers the certified customer who holds out the bar, and what it throws is doubled
            wild.snapTo(bijB.getX() + 0.5, bijB.getY(), bijB.getZ() + 0.5);
            wild.setNoAi(true);
            level.addFreshEntity(wild);
            ItemStack staaf = new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get());
            Ruilmarkt.klant(b, wild, staaf);
            helper.assertTrue(wild.getPersistentData().getStringOr(Ruilmarkt.KLANT, "").isEmpty(), "b is no certified customer");
            Ruilmarkt.klant(a, wild, new ItemStack(Items.STICK));
            helper.assertTrue(wild.getPersistentData().getStringOr(Ruilmarkt.KLANT, "").isEmpty(), "a stick is no barter");
            a.snapTo(bijB.getX() + 2.5, bijB.getY(), bijB.getZ() + 0.5);
            Ruilmarkt.klant(a, wild, staaf);
            helper.assertTrue(wild.getPersistentData().getStringOr(Ruilmarkt.KLANT, "").equals(a.getUUID().toString()), "the Mika knows its customer");
            // (the real thing: the Mika throws its present, the item comes into the world, the event doubles it)
            BehaviorUtils.throwItem(wild, new ItemStack(Items.CHARCOAL), a.position().add(0, 1, 0));
            List<ItemEntity> gegooid = level.getEntitiesOfClass(ItemEntity.class, kamer);
            helper.assertTrue(gegooid.size() >= 2 && wild.getPersistentData().getStringOr(Ruilmarkt.KLANT, "").isEmpty(), "a second present: " + gegooid.size());
            int nu = gegooid.size();
            BehaviorUtils.throwItem(wild, new ItemStack(Items.CHARCOAL), a.position().add(0, 1, 0));
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, kamer).size() == nu + 1, "once per barter");
            // a barter that is broken off (the bar comes back) gives nothing extra
            level.getEntitiesOfClass(ItemEntity.class, kamer).forEach(Entity::discard);
            wild.getPersistentData().remove(Ruilmarkt.EXTRA_TIJD);
            Ruilmarkt.klant(a, wild, staaf);
            BehaviorUtils.throwItem(wild, staaf.copy(), a.position().add(0, 1, 0));
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, kamer).size() == 1, "the bar itself is no present");
        } finally {
            level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(24, 10, 24)).forEach(Entity::discard);
            kraam.discard();
            wild.discard();
            weg(helper, a, b);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the buildings: protection, the templates, the worldgen data
    // =================================================================================================================

    private static StructureStart kopie(GameTestHelper helper, String structuur, BlockPos hoek) {
        ServerLevel level = helper.getLevel();
        Structure structure = Kopieen.structuur(level, structuur);
        helper.assertTrue(structure != null, "the structure guhs:" + structuur + " exists");
        BlockPos pos = helper.absolutePos(hoek);
        StructurePoolElement element = StructurePoolElement.single("guhs:" + KAMER).apply(StructureTemplatePool.Projection.RIGID);
        BoundingBox box = element.getBoundingBox(level.getStructureManager(), pos, Rotation.NONE);
        StructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, pos, 0, Rotation.NONE, box, LiquidSettings.IGNORE_WATERLOGGING);
        StructureStart start = new StructureStart(structure, ChunkPos.containing(pos), 0, new PiecesContainer(List.of(piece)));
        Kopieen.test(level, start);
        return start;
    }

    /**
     * Inside a Grillcamping nothing can be broken or placed, but a tent is pitched all the same (by the real click, with the
     * tent bag in the hand), and the characters of the building belong at their spots of every copy.
     */
    @GuhTest(template = KAMER, batch = "campingmarkt_gebouw")
    public static void campingmarktBeschermdEnBewoond(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        try {
            StructureStart start = kopie(helper, CampingmarktFeature.CAMPING_STRUCTUUR, new BlockPos(0, 1, 0));
            helper.setBlock(new BlockPos(18, 2, 5), CampingmarktFeature.TENTDOEK.get("rood").blok().get());
            BlockPos doek = helper.absolutePos(new BlockPos(18, 2, 5));
            BlockPos sign = bord(helper, new BlockPos(8, 2, 10), Direction.SOUTH);
            helper.assertTrue(Bescherming.beschermd(level, doek) && CampingmarktFeature.CAMPING_STRUCTUUR.equals(Bescherming.structuurBij(level, doek))
                    && !Bescherming.mag(p, doek), "the camping is protected");
            p.gameMode.destroyBlock(doek);
            helper.assertTrue(level.getBlockState(doek).is(CampingmarktFeature.TENTDOEK.get("rood").blok().get()), "a tent of a resident stays whole");
            CampingmarktFeature.CAMPING.zet(p, 1);
            inHand(p, new ItemStack(CampingmarktFeature.TENTZAK.get()));
            p.gameMode.useItemOn(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND, raak(sign));
            helper.assertTrue(level.getBlockState(sign).getValue(CampingmarktBlocks.Kampeerplek.BEZET), "a tent is pitched inside the protected camping");
            BlockPos pen = inPlek(level, sign, 0, 0, 0);
            p.gameMode.useItemOn(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND, raak(pen));
            helper.assertTrue(level.getBlockState(pen).getValue(CampingmarktBlocks.Haring.VAST) && Kamperen.haringen(p) == 1, "and its pegs can be hammered in");
            p.gameMode.destroyBlock(inPlek(level, sign, 3, 2, 1));
            helper.assertTrue(!level.getBlockState(inPlek(level, sign, 3, 2, 1)).isAir(), "the pitched tent can't be taken apart");
            // the characters and the residents map into the copy
            helper.assertTrue(Bezetting.start(level, CampingmarktFeature.CAMPING_STRUCTUUR, doek) == start, "the copy is found");
            for (BlockPos plek : List.of(CampingmarktFeature.KAMPBAAS_PLEK, CampingmarktFeature.HOUTHAKKER_PLEK, Kamperen.KAMPEERDERS.get(0).plek())) {
                helper.assertTrue(Bezetting.wereld(start, null, plek) != null, "the spot " + plek + " maps into the copy");
            }
        } finally {
            Kopieen.testWissen(level);
            Herstel.verschuif(level, Kamperen.TENT_TICKS);
            Herstel.verwerk(level);
            weg(helper, p);
        }
        helper.succeed();
    }

    /**
     * A copy of the Grillcamping without its people (an old one, or one that lost them): the Kampbaas-guh, the Houthakker-guh
     * and the five residents come by themselves, each once; and the two that are taken away come back, once.
     */
    @GuhTest(template = KAMER, batch = "campingmarkt_volk", timeoutTicks = 3000)
    public static void campingmarktVolkKomtEnKomtTerug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // (the spots of the seven lie between template 12..33 / 9..23: this corner puts them round the middle of the room)
        StructureStart start = kopie(helper, CampingmarktFeature.CAMPING_STRUCTUUR, new BlockPos(-10, -2, -4));
        BlockPos bij = helper.absolutePos(new BlockPos(12, 2, 12));
        // a resident that walks is looked for 26 blocks around its spot: those chunks have to be there, with their entities
        ChunkPos van = ChunkPos.containing(bij.offset(-48, 0, -48)), tot = ChunkPos.containing(bij.offset(48, 0, 48));
        for (int cx = van.x(); cx <= tot.x(); cx++) {
            for (int cz = van.z(); cz <= tot.z(); cz++) {
                level.setChunkForced(cx, cz, true);
            }
        }
        AABB ruim = new AABB(bij).inflate(48, 16, 48);
        java.util.function.Supplier<List<Entity>> volk = () -> level.getEntitiesOfClass(Entity.class, ruim, e -> e.isAlive() && Bezetting.isBezetting(e));
        int alle = 2 + Kamperen.KAMPEERDERS.size();
        int[] fase = {0}, gekomen = {0};
        helper.succeedWhen(() -> {
            if (fase[0] == 0) {
                Bezetting.controleer(level, bij);
                Bezetting.bevestigAlles(level);
                gekomen[0] += Bezetting.controleer(level, bij);
                List<Entity> er = volk.get();
                helper.assertTrue(gekomen[0] == alle && er.size() == alle, "the two guhs of the camping and its five residents come: " + gekomen[0] + " made, " + er.size() + " there");
                GuhNpcEntity baas = er.stream().filter(e -> e instanceof GuhNpcEntity n && n.getKind() == GuhNpcEntity.Kind.KAMPBAASGUH).map(e -> (GuhNpcEntity) e).findFirst().orElse(null);
                GuhEntity drie = er.stream().filter(e -> e instanceof GuhEntity g && Kamperen.kampeerder(g) == 3).map(e -> (GuhEntity) e).findFirst().orElse(null);
                helper.assertTrue(baas != null && NpcRollen.van(baas) instanceof Kamperen.KampbaasRol && baas.isInvulnerable() && drie != null && !drie.isTame()
                        && drie.isPersistenceRequired() && baas.blockPosition().equals(Bezetting.wereld(start, null, CampingmarktFeature.KAMPBAAS_PLEK)),
                        "the Kampbaas-guh at his spot with his talk, resident 3 known by its tag");
                Bezetting.bevestigAlles(level);
                helper.assertTrue(Bezetting.controleer(level, bij) == 0 && volk.get().size() == alle, "nobody twice");
                baas.discard();
                drie.discard();
                gekomen[0] = 0;
                fase[0] = 1;
            }
            Bezetting.controleer(level, bij);
            Bezetting.bevestigAlles(level);
            gekomen[0] += Bezetting.controleer(level, bij);
            helper.assertTrue(gekomen[0] == 2 && volk.get().size() == alle, "the two that were lost are back: " + gekomen[0] + " made, " + volk.get().size() + " there");
            Bezetting.bevestigAlles(level);
            helper.assertTrue(Bezetting.controleer(level, bij) == 0 && volk.get().size() == alle, "and no doubles");
            volk.get().forEach(Entity::discard);
            Kopieen.testWissen(level);
            for (int cx = van.x(); cx <= tot.x(); cx++) {
                for (int cz = van.z(); cz <= tot.z(); cz++) {
                    level.setChunkForced(cx, cz, false);
                }
            }
        });
    }

    private static int tel(StructureTemplate template, Block block) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();
    }

    /** The entities of a template: their Bezetting tag -> their block position. */
    private static Map<String, BlockPos> bewoners(StructureTemplate template) {
        Map<String, BlockPos> out = new HashMap<>();
        for (Tag t : template.save(new CompoundTag()).getListOrEmpty("entities")) {
            CompoundTag e = t.asCompound().orElse(new CompoundTag());
            ListTag pos = e.getListOrEmpty("blockPos");
            String tag = e.getCompoundOrEmpty("nbt").getCompoundOrEmpty("NeoForgeData").getStringOr(Bezetting.TAG, "");
            if (!tag.isEmpty()) {
                out.put(tag, new BlockPos(pos.getIntOr(0, -1), pos.getIntOr(1, -1), pos.getIntOr(2, -1)));
            }
        }
        return out;
    }

    /**
     * The templates hold what the questlines need, their characters, residents and stall holders stand where the Java side
     * says, every pitch of the camping has room for a tent, and the worldgen data: both structures are cave buildings with
     * a guaranteed copy in new terrain, their start pool says where the ground is.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void campingmarktSjablonenEnWereld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate camping = level.getStructureManager().get(Guhs.id(CampingmarktFeature.CAMPING_STRUCTUUR)).orElse(null);
        StructureTemplate markt = level.getStructureManager().get(Guhs.id(CampingmarktFeature.MARKT_STRUCTUUR)).orElse(null);
        StructureTemplate plek = level.getStructureManager().get(Kamperen.PLEK).orElse(null), tent = level.getStructureManager().get(Kamperen.TENT).orElse(null);
        helper.assertTrue(camping != null && markt != null && plek != null && tent != null, "the four templates exist");
        helper.assertTrue(tel(camping, CampingmarktFeature.KAMPVUUR.get()) == 1 && tel(camping, CampingmarktFeature.HAKBLOK.get()) >= 2
                && tel(camping, CampingmarktFeature.KAMPEERPLEK.get()) >= 3, "the camping: one big fire, two chopping blocks, pitches for everybody");
        helper.assertTrue(tel(markt, CampingmarktFeature.WEEGSCHAAL.get()) == 1 && tel(markt, CampingmarktFeature.VADSSTAPEL.get()) == Ruilmarkt.STAPELS,
                "the market: the scales and five stacks of vads");
        java.util.Set<Integer> nummers = new java.util.HashSet<>();
        for (StructureTemplate.StructureBlockInfo info : markt.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), CampingmarktFeature.VADSSTAPEL.get())) {
            nummers.add(info.state().getValue(CampingmarktBlocks.Vadsstapel.NUMMER));
        }
        helper.assertTrue(nummers.size() == Ruilmarkt.STAPELS, "the stacks are numbered 1..5: " + nummers);
        // the two small templates: the same box, the sign at the same spot, the tent with four pegs
        helper.assertTrue(plek.getSize().equals(tent.getSize()) && plek.getSize().equals(new net.minecraft.core.Vec3i(7, 3, 5)), "a pitch is 7 x 3 x 5: " + plek.getSize());
        for (StructureTemplate t : List.of(plek, tent)) {
            List<StructureTemplate.StructureBlockInfo> borden = t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), CampingmarktFeature.KAMPEERPLEK.get());
            helper.assertTrue(borden.size() == 1 && borden.get(0).pos().equals(Kamperen.PLEK_BORD)
                    && borden.get(0).state().getValue(CampingmarktBlocks.Kampeerplek.FACING) == Direction.SOUTH
                    && borden.get(0).state().getValue(CampingmarktBlocks.Kampeerplek.BEZET) == (t == tent), "the sign of a pitch: " + borden);
        }
        helper.assertTrue(tel(tent, CampingmarktFeature.HARING.get()) == Kamperen.HARINGEN && tel(plek, CampingmarktFeature.HARING.get()) == 0, "four pegs");
        // every pitch of the camping: the box of a tent is free (only its sign), on solid ground
        Map<BlockPos, BlockState> blokken = new HashMap<>();
        for (Block b : List.of(Blocks.AIR)) {
            for (StructureTemplate.StructureBlockInfo info : camping.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), b)) {
                blokken.put(info.pos(), info.state());
            }
        }
        for (StructureTemplate.StructureBlockInfo info : camping.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), CampingmarktFeature.KAMPEERPLEK.get())) {
            Rotation draai = Kamperen.draai(info.state().getValue(CampingmarktBlocks.Kampeerplek.FACING));
            BlockPos hoek = Kamperen.hoek(info.pos(), draai);
            for (BlockPos lokaal : BlockPos.betweenClosed(0, 0, 0, 6, 2, 4)) {
                BlockPos q = hoek.offset(StructureTemplate.transform(lokaal, Mirror.NONE, draai, BlockPos.ZERO));
                helper.assertTrue(q.equals(info.pos()) || blokken.containsKey(q), "the pitch at " + info.pos() + " is free at " + q);
            }
        }
        // who lives where
        Map<String, BlockPos> opCamping = bewoners(camping), opMarkt = bewoners(markt);
        helper.assertTrue(CampingmarktFeature.KAMPBAAS_PLEK.equals(opCamping.get(CampingmarktFeature.KAMPBAAS_ID))
                && CampingmarktFeature.HOUTHAKKER_PLEK.equals(opCamping.get(CampingmarktFeature.HOUTHAKKER_ID)), "the two guhs of the camping sit where Java says: " + opCamping);
        for (int i = 0; i < Kamperen.KAMPEERDERS.size(); i++) {
            helper.assertTrue(Kamperen.KAMPEERDERS.get(i).plek().equals(opCamping.get(Kamperen.KAMPEERDER_ID + i)), "resident " + i + " lives where Java says: " + opCamping);
        }
        helper.assertTrue(CampingmarktFeature.MARKTMEESTER_PLEK.equals(opMarkt.get(CampingmarktFeature.MARKTMEESTER_ID)), "the Marktmeester stands where Java says: " + opMarkt);
        for (int i = 0; i < CampingmarktFeature.KRAAM_PLEKKEN.size(); i++) {
            helper.assertTrue(CampingmarktFeature.KRAAM_PLEKKEN.get(i).equals(opMarkt.get(CampingmarktFeature.KRAAM_ID + i)), "stall holder " + i + " stands where Java says: " + opMarkt);
        }
        // (the stall holders are this slice's own peaceful Mika's with their stall number, the residents are guhs)
        int kraamMikas = 0, guhs = 0;
        for (Tag t : markt.save(new CompoundTag()).getListOrEmpty("entities")) {
            CompoundTag nbt = t.asCompound().orElse(new CompoundTag()).getCompoundOrEmpty("nbt");
            String tag = nbt.getCompoundOrEmpty("NeoForgeData").getStringOr(Bezetting.TAG, "");
            if (tag.startsWith(CampingmarktFeature.KRAAM_ID)) {
                helper.assertTrue("guhs:campingmarkt_kraam_mika".equals(nbt.getStringOr("id", "")) && (CampingmarktFeature.KRAAM_ID + nbt.getIntOr("Kraam", -1)).equals(tag),
                        "a stall holder of the template: " + nbt);
                kraamMikas++;
            }
        }
        for (Tag t : camping.save(new CompoundTag()).getListOrEmpty("entities")) {
            CompoundTag nbt = t.asCompound().orElse(new CompoundTag()).getCompoundOrEmpty("nbt");
            guhs += "guhs:guh".equals(nbt.getStringOr("id", "")) ? 1 : 0;
        }
        helper.assertTrue(kraamMikas == KraamMikaEntity.AANTAL && guhs == Kamperen.KAMPEERDERS.size(), kraamMikas + " stall holders, " + guhs + " residents");
        // worldgen
        for (StructureTemplate t : List.of(camping, markt)) {
            List<StructureTemplate.StructureBlockInfo> jigsaws = t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW);
            helper.assertTrue(jigsaws.size() == 1 && jigsaws.get(0).pos().getY() == 0, "one centre jigsaw, in layer 0");
        }
        var pools = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (String naam : List.of(CampingmarktFeature.CAMPING_STRUCTUUR, CampingmarktFeature.MARKT_STRUCTUUR)) {
            helper.assertTrue(Kopieen.structuur(level, naam) instanceof BarbecuePutStructure, naam + " is a cave building");
            var pool = pools.getValue(Guhs.id(naam + "/start"));
            helper.assertTrue(pool != null, naam + ": its start pool");
            StructurePoolElement element = pool.getRandomTemplate(level.getRandom());
            helper.assertTrue(element instanceof GrondPoolElement g && g.groundLevelDelta() == 4, naam + ": the pool says where its ground is: " + element);
            helper.assertTrue(sets.getValue(Guhs.id(naam)) != null, naam + ": a spread set");
            var gegarandeerd = sets.getValue(Guhs.id(naam + "_gegarandeerd"));
            helper.assertTrue(gegarandeerd != null && gegarandeerd.placement() instanceof GegarandeerdPlacement g && g.alleenNieuw(),
                    naam + ": one guaranteed copy, in new terrain");
        }
        // the recipe of the Plantagebak needs the Kampbaas-guh's card, and the card stays in the grid
        ItemStack kaart = new ItemStack(CampingmarktFeature.RECEPT_PLANTAGEBAK.get());
        helper.assertTrue(kaart.getItem().getCraftingRemainder(kaart) != null, "the recipe card stays in the crafting grid");
        helper.succeed();
    }
}
