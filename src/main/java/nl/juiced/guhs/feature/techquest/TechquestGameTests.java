package nl.juiced.guhs.feature.techquest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.fossielmijn.FossielmijnFeature;
import nl.juiced.guhs.feature.guhoven.GuhovenFeature;
import nl.juiced.guhs.feature.guhpolder.GuhpolderFeature;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.feature.techbron.AangebrandeMika;
import nl.juiced.guhs.feature.techbuis.BuisStukBlock;
import nl.juiced.guhs.feature.techbuis.FilterBlockEntity;
import nl.juiced.guhs.feature.techbuis.TechbuisFeature;
import nl.juiced.guhs.feature.techmachine.TechmachineFeature;
import nl.juiced.guhs.feature.techsaus.SausvatBlockEntity;
import nl.juiced.guhs.feature.techsaus.TechsausFeature;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.vadskracht.Sauzen;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2 (tech-quests): the practice hall of the Oude Guhrad-centrale ({@link Centrale}), the Uitvinder-guh's two questlines
 * ({@link UitvinderRol}, {@link Knabbelmachine}), the recipe cards and the building.
 * <p>
 * The practice hall is furnished by code, so the tests need no building: a {@link Centrale#proef} copy lays the template's
 * coordinates over the test room (techquest_test_kamer: a stone floor two blocks thick; techquest_test_draai for a copy that
 * is turned a quarter), and {@link Centrale#tik} is called by hand with the players that stand there. The setups are the
 * real blocks: the wheels really give vadskracht, the tubes really carry the cardboard knabbels, the pump really pumps.
 */
public final class TechquestGameTests {
    private static final String KAMER = "techquest_test_kamer", DRAAI = "techquest_test_draai", BATCH = "techquest";
    private static final Verhaallijn LIJN = TechquestFeature.TECHNIEK, MACHINE = TechquestFeature.KNABBELMACHINE;

    private TechquestGameTests() {
    }

    /** The copy whose practice hall lies in the test room: template (7, 3, 9), a block of the ground layer, is the corner of the room's floor. */
    private static Centrale.Kopie kopie(GameTestHelper helper) {
        return Centrale.proef(helper.getLevel(), helper.absolutePos(new BlockPos(-7, -1, -9)), Rotation.NONE, false);
    }

    private static ServerPlayer speler(GameTestHelper helper, BlockPos wereld) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        LIJN.wis(p);
        MACHINE.wis(p);
        p.snapTo(wereld.getX() + 0.5, wereld.getY(), wereld.getZ() + 0.5);
        return p;
    }

    private static void naar(ServerPlayer p, BlockPos wereld) {
        p.snapTo(wereld.getX() + 0.5, wereld.getY(), wereld.getZ() + 0.5);
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static int tel(ServerLevel level, BlockPos pos, Item item) {
        int n = 0;
        if (level.getBlockEntity(pos) instanceof Container c) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                if (c.getItem(i).is(item)) {
                    n += c.getItem(i).getCount();
                }
            }
        }
        return n;
    }

    private static boolean klik(ServerPlayer p, BlockPos op, Direction kant) {
        var event = new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, op, new BlockHitResult(Vec3.atCenterOf(op), kant, op, false));
        Centrale.opKlik(event);
        return event.isCanceled();
    }

    private static void rust(Centrale.Kopie k) {
        for (int i = 0; i < Centrale.RUST; i++) {
            Centrale.tik(k, List.of());
        }
    }

    // =====================================================================================================================
    // the practice hall
    // =====================================================================================================================

    /** A copy is furnished the first time it is looked at: five running wheels with a guh nobody owns, five broken setups. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void techquestOefenhalWordtIngericht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Centrale.Kopie k = kopie(helper);
        helper.assertTrue(k.w(Centrale.RADEREN[0]).equals(helper.absolutePos(new BlockPos(4, 3, 1))), "template coordinates lie over the room");
        Centrale.richtIn(k);
        for (BlockPos rad : Centrale.RADEREN) {
            BlockPos pos = k.w(rad);
            BlockState state = level.getBlockState(pos);
            helper.assertTrue(state.is(ModBlocks.GUH_WHEEL.get()) && state.getValue(GuhWheelBlock.FACING) == Direction.SOUTH && state.getValue(GuhWheelBlock.RUNNING),
                    "a running wheel at " + rad);
            for (BlockPos deel : GuhWheelBlock.partPositions(pos, Direction.SOUTH)) {
                helper.assertTrue(level.getBlockState(deel).is(ModBlocks.GUH_WHEEL_PART.get()), "the wheel's other blocks");
            }
            GuhWheelBlockEntity wiel = (GuhWheelBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(wiel != null && wiel.hasGuh() && wiel.getGuhOwner() == null && wiel.vadsAanbod() == VadsGetallen.GUHRAD,
                    "an old guh in it that nobody owns (so nobody takes it out), giving vadskracht");
        }
        for (Centrale.Opstelling o : Centrale.Opstelling.values()) {
            helper.assertTrue(!Centrale.werkt(k, o), o + " is broken");
        }
        helper.assertTrue(level.getBlockState(k.w(Centrale.S1_OVEN[0])).is(GuhovenFeature.GUH_OVEN.get()) && level.getBlockState(k.w(Centrale.S1_GAT[0])).isAir(),
                "setup 1: an oven and a gap in the wire");
        helper.assertTrue(VadsKracht.net(level, k.w(Centrale.RADEREN[1])).vraag() == 2 * VadsGetallen.GUH_OVEN + VadsGetallen.MOLEN
                && !VadsKracht.net(level, k.w(Centrale.RADEREN[1])).draait(), "setup 2 asks more than one wheel gives: it stands still");
        helper.assertTrue(level.getBlockState(k.w(Centrale.S2_MOLEN[0]).above()).is(nl.juiced.guhs.feature.vadskracht.VadskrachtFeature.MACHINE_DEEL.get()),
                "the Vadsmolen is two blocks high");
        helper.assertTrue(tel(level, k.w(Centrale.S3_VAT_A[0]), TechquestFeature.OEFENKNABBEL.get()) == Centrale.OEFENKNABBELS
                && tel(level, k.w(Centrale.S3_VAT_B[0]), TechquestFeature.OEFENKNABBEL.get()) == 0, "setup 3: the knabbels are in the first barrel");
        helper.assertTrue(level.getBlockState(k.w(Centrale.S3_STUK[0])).getValue(BuisStukBlock.FACING) == Direction.WEST, "and the piece points the wrong way");
        helper.assertTrue(tel(level, k.w(Centrale.S4_VAT_A[0]), TechquestFeature.OEFENKNABBEL.get()) == Centrale.OEFENKNABBELS / 2
                && tel(level, k.w(Centrale.S4_VAT_A[0]), TechquestFeature.OEFENROMMEL.get()) == Centrale.OEFENROMMEL, "setup 4: knabbels and paper");
        helper.assertTrue(level.getFluidState(k.w(Centrale.S5_BRON[0])).isSource() && level.getBlockState(k.w(Centrale.S5_POMP[0])).is(TechsausFeature.SAUSPOMP.get())
                && level.getBlockState(k.w(Centrale.S5_GAT[0])).isAir(), "setup 5: a pump on sauce and a gap in the hose");
        helper.assertTrue(!level.getBlockState(k.w(Centrale.TEKENTAFEL[0])).is(TechmachineFeature.TEKENTAFEL.get()),
                "(a copy that is only the practice hall gets nothing outside it)");
        // looking again changes nothing
        Centrale.tik(k, List.of());
        helper.assertTrue(tel(level, k.w(Centrale.S3_VAT_A[0]), TechquestFeature.OEFENKNABBEL.get()) == Centrale.OEFENKNABBELS, "nothing piles up");
        helper.succeed();
    }

    /**
     * Setups 1 and 2, per player: a lays Guhdraad in the gap (through the click the protected building would refuse) and
     * goes one step on, b who stands elsewhere does not; a cuts a wire of setup 2; when everybody has walked away both
     * setups are broken again for the next player.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void techquestDraadEnTeZwaarPerSpeler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Centrale.Kopie k = kopie(helper);
        BlockPos gat = k.w(Centrale.S1_GAT[0]);
        ServerPlayer a = speler(helper, gat.south(2)), b = speler(helper, k.w(Centrale.S5_SAUSVAT[0]).south(3));
        Centrale.proefAan(k);
        try {
            Centrale.richtIn(k);
            LIJN.zet(a, 1);
            LIJN.zet(b, 1);
            Centrale.tik(k, List.of(a, b));
            helper.assertTrue(LIJN.stap(a) == 1, "a broken setup does nothing");
            // any other block item in the hand: not ours (the building's protection says no)
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
            helper.assertTrue(!klik(a, gat.below(), Direction.UP) && level.getBlockState(gat).isAir(), "stone is not laid in the gap");
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.GUH_WIRE.get().asItem(), 2));
            helper.assertTrue(!klik(a, gat.below().east(), Direction.UP), "Guhdraad next to the gap: not ours either");
            helper.assertTrue(klik(a, gat.below(), Direction.UP) && level.getBlockState(gat).is(ModBlocks.GUH_WIRE.get()), "Guhdraad in the gap");
            helper.assertTrue(a.getMainHandItem().getCount() == 1, "it cost one piece");
            helper.assertTrue(Centrale.werkt(k, Centrale.Opstelling.DRAAD) && VadsKracht.net(level, k.w(Centrale.S1_OVEN[0])).draait(), "the oven has vadskracht");
            Centrale.tik(k, List.of(a, b));
            helper.assertTrue(LIJN.stap(a) == 2 && LIJN.stap(b) == 1, "it counts for a (who stands there), not for b (who doesn't)");
            helper.assertTrue(Centrale.magBreken(a, gat) && !Centrale.magBreken(a, k.w(Centrale.S1_DRAAD[0])), "the piece in the gap may be taken out again, the rest not");
            // setup 2: too heavy until one machine is cut off
            BlockPos knip = k.w(Centrale.S2_KNIP[1]);
            naar(a, knip.south(2));
            helper.assertTrue(Centrale.magBreken(a, knip) && Centrale.magBreken(a, k.w(Centrale.S2_KNIP[0])) && !Centrale.magBreken(a, k.w(Centrale.S2_DRAAD[0])),
                    "only the two marked wires may be cut");
            helper.assertTrue(!Centrale.magBreken(a, k.w(Centrale.S2_OVENS[0])), "never a machine");
            level.destroyBlock(knip, false);
            helper.assertTrue(Centrale.werkt(k, Centrale.Opstelling.TE_ZWAAR), "one oven off: the wheel can carry the rest");
            Centrale.tik(k, List.of(a, b));
            helper.assertTrue(LIJN.stap(a) == 3 && LIJN.stap(b) == 1, "a is at the salt step");
            // b comes to setup 1 while it still works: fine, it counts for b too (friends do it together)
            naar(b, gat.south(2));
            Centrale.tik(k, List.of(a, b));
            helper.assertTrue(LIJN.stap(b) == 2, "a setup that works counts for whoever stands at it at that step (and setup 2, a bay further, does not)");
            // everybody walks away: after a few looks the setups are broken again
            naar(a, k.w(Centrale.S5_SAUSVAT[0]).south(3));
            naar(b, k.w(Centrale.S5_SAUSVAT[0]).south(3));
            Centrale.tik(k, List.of(a, b));
            helper.assertTrue(level.getBlockState(gat).is(ModBlocks.GUH_WIRE.get()), "not at once");
            for (int i = 0; i < Centrale.RUST; i++) {
                Centrale.tik(k, List.of(a, b));
            }
            helper.assertTrue(level.getBlockState(gat).isAir() && level.getBlockState(knip).is(ModBlocks.GUH_WIRE.get()), "the gap is back, the cut wire is whole");
            helper.assertTrue(!Centrale.werkt(k, Centrale.Opstelling.DRAAD) && !Centrale.werkt(k, Centrale.Opstelling.TE_ZWAAR), "both broken for the next player");
        } finally {
            Centrale.proefUit(k);
            weg(helper, a, b);
        }
        helper.succeed();
    }

    /**
     * Setups 3 and 4 with the real tubes: turning the Richtingstuk (sneak + empty hand) lets the cardboard knabbels roll
     * into the other barrel; a Filterstuk that knows the knabbel lets the paper lie. Then both are broken again and the
     * knabbels are back.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 600)
    public static void techquestBuisEnFilter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Centrale.Kopie k = kopie(helper);
        BlockPos stuk = k.w(Centrale.S3_STUK[0]), filter = k.w(Centrale.S4_FILTER[0]);
        ServerPlayer a = speler(helper, stuk.south(2));
        Centrale.richtIn(k);
        LIJN.zet(a, 4);
        a.setShiftKeyDown(true);
        level.getBlockState(stuk).useWithoutItem(level, a, new BlockHitResult(Vec3.atCenterOf(stuk), Direction.SOUTH, stuk, false));
        a.setShiftKeyDown(false);
        helper.assertTrue(level.getBlockState(stuk).getValue(BuisStukBlock.FACING) == Direction.EAST && Centrale.werkt(k, Centrale.Opstelling.BUIS),
                "sneak + click turned the piece around");
        Centrale.tik(k, List.of(a));
        helper.assertTrue(LIJN.stap(a) == 5, "setup 3 done");
        naar(a, filter.south(2));
        FilterBlockEntity f = (FilterBlockEntity) level.getBlockEntity(filter);
        helper.assertTrue(f.filter().behalve() && !f.filter().isLeeg() && !Centrale.werkt(k, Centrale.Opstelling.FILTER),
                "the filter stands on \"alles behalve\" the knabbel: only paper gets through");
        f.filter().items().clearContent();
        f.filter().zetBehalve(false);
        helper.assertTrue(!Centrale.werkt(k, Centrale.Opstelling.FILTER), "an empty list lets everything through: not mended either");
        f.filter().voegToe(new ItemStack(TechquestFeature.OEFENROMMEL.get()));
        helper.assertTrue(!Centrale.werkt(k, Centrale.Opstelling.FILTER), "nor is a list with the paper on it");
        f.filter().items().clearContent();
        f.filter().voegToe(new ItemStack(TechquestFeature.OEFENKNABBEL.get()));
        helper.assertTrue(Centrale.werkt(k, Centrale.Opstelling.FILTER), "\"alleen deze\" with the knabbel on the list: mended");
        Centrale.tik(k, List.of(a));
        helper.assertTrue(LIJN.stap(a) == 6, "setup 4 done");
        boolean[] klaar = {false};
        helper.succeedWhen(() -> {
            if (klaar[0]) {
                return;
            }
            Item knabbel = TechquestFeature.OEFENKNABBEL.get(), rommel = TechquestFeature.OEFENROMMEL.get();
            helper.assertTrue(tel(level, k.w(Centrale.S3_VAT_B[0]), knabbel) >= 3, "knabbels roll through the tube of setup 3");
            helper.assertTrue(tel(level, k.w(Centrale.S4_VAT_B[0]), knabbel) == Centrale.OEFENKNABBELS / 2, "all knabbels of setup 4 came through the filter");
            helper.assertTrue(tel(level, k.w(Centrale.S4_VAT_B[0]), rommel) == 0 && tel(level, k.w(Centrale.S4_VAT_A[0]), rommel) == Centrale.OEFENROMMEL,
                    "the paper stayed behind");
            klaar[0] = true;
            rust(k);
            helper.assertTrue(level.getBlockState(stuk).getValue(BuisStukBlock.FACING) == Direction.WEST && !Centrale.werkt(k, Centrale.Opstelling.BUIS),
                    "setup 3 is broken again");
            helper.assertTrue(f.filter().behalve() && !Centrale.werkt(k, Centrale.Opstelling.FILTER), "the filter stands wrong again");
            helper.assertTrue(tel(level, k.w(Centrale.S4_VAT_A[0]), knabbel) == Centrale.OEFENKNABBELS / 2 && tel(level, k.w(Centrale.S4_VAT_B[0]), knabbel) == 0,
                    "the knabbels of setup 4 are back in the first barrel");
            helper.assertTrue(tel(level, k.w(Centrale.S3_VAT_A[0]), knabbel) + tel(level, k.w(Centrale.S3_VAT_B[0]), knabbel) <= Centrale.OEFENKNABBELS
                    && tel(level, k.w(Centrale.S3_VAT_B[0]), knabbel) == 0, "and those of setup 3 too (never more than there were)");
            weg(helper, a);
        });
    }

    /** Setup 5 with the real pump: a Sausslang in the gap, and the vat fills. Broken again: the hose is gone, the vat empty. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 600)
    public static void techquestSaus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Centrale.Kopie k = kopie(helper);
        BlockPos gat = k.w(Centrale.S5_GAT[0]), vat = k.w(Centrale.S5_SAUSVAT[0]);
        ServerPlayer a = speler(helper, gat.east(2));
        Centrale.proefAan(k);
        Centrale.richtIn(k);
        LIJN.zet(a, 6);
        a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TechsausFeature.SAUSSLANG_ITEM.get()));
        helper.assertTrue(klik(a, gat.below(), Direction.UP) && level.getBlockState(gat).is(TechsausFeature.SAUSSLANG.get()) && a.getMainHandItem().isEmpty(),
                "the hose lies in the gap");
        helper.assertTrue(Centrale.magBreken(a, gat) && !Centrale.magBreken(a, k.w(Centrale.S5_SLANG[0])), "it may be taken out again, the other piece not");
        Centrale.proefUit(k);
        boolean[] klaar = {false};
        helper.succeedWhen(() -> {
            if (klaar[0]) {
                return;
            }
            SausvatBlockEntity sausvat = (SausvatBlockEntity) level.getBlockEntity(vat);
            helper.assertTrue(sausvat != null && sausvat.tank().inhoud() >= Sauzen.EMMER, "the pump fills the vat through the hose");
            helper.assertTrue(Centrale.werkt(k, Centrale.Opstelling.SAUS), "setup 5 works");
            klaar[0] = true;
            Centrale.tik(k, List.of(a));
            helper.assertTrue(LIJN.stap(a) == 7, "setup 5 done: back to the Uitvinder-guh");
            rust(k);
            helper.assertTrue(level.getBlockState(gat).isAir() && sausvat.tank().isLeeg() && !Centrale.werkt(k, Centrale.Opstelling.SAUS), "broken again");
            weg(helper, a);
        });
    }

    /** A copy that is turned a quarter: the wheels lie along the other axis and still run, and a setup still works. */
    @GuhTest(template = DRAAI, batch = BATCH)
    public static void techquestGedraaideKopie(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Rotation draai = Rotation.CLOCKWISE_90;
        Centrale.Kopie k = Centrale.proef(level, helper.absolutePos(new BlockPos(17, -1, -7)), draai, false);
        helper.assertTrue(k.w(Centrale.RADEREN[0]).equals(helper.absolutePos(new BlockPos(7, 3, 4))) && k.kant(Direction.SOUTH) == Direction.WEST,
                "template coordinates, turned");
        Centrale.richtIn(k);
        for (BlockPos rad : Centrale.RADEREN) {
            BlockPos pos = k.w(rad);
            BlockState state = level.getBlockState(pos);
            helper.assertTrue(state.is(ModBlocks.GUH_WHEEL.get()) && state.getValue(GuhWheelBlock.FACING) == Direction.WEST, "a wheel looking west at " + pos);
            helper.assertTrue(level.getBlockState(pos.north()).is(ModBlocks.GUH_WHEEL_PART.get()) && level.getBlockState(pos.south().above(2)).is(ModBlocks.GUH_WHEEL_PART.get())
                    && !level.getBlockState(pos.east()).is(ModBlocks.GUH_WHEEL_PART.get()), "its other blocks lie north and south of it");
            helper.assertTrue(VadsKracht.net(level, pos).aanbod() >= VadsGetallen.GUHRAD, "and it gives vadskracht");
        }
        helper.assertTrue(level.getBlockState(k.w(Centrale.S1_OVEN[0])).getValue(HorizontalDirectionalBlock.FACING) == Direction.WEST, "the oven looks at the aisle");
        helper.assertTrue(k.w(Centrale.S1_OVEN[0]).equals(k.w(Centrale.RADEREN[0]).west(4)), "four blocks in front of its wheel");
        BlockPos gat = k.w(Centrale.S1_GAT[0]);
        level.setBlock(gat, Block.updateFromNeighbourShapes(ModBlocks.GUH_WIRE.get().defaultBlockState(), level, gat), Block.UPDATE_ALL);
        helper.assertTrue(Centrale.werkt(k, Centrale.Opstelling.DRAAD), "the wire closes the turned setup");
        helper.assertTrue(level.getBlockState(k.w(Centrale.S3_STUK[0])).getValue(BuisStukBlock.FACING) == draai.rotate(Direction.WEST)
                && level.getBlockState(k.w(Centrale.S4_FILTER[0])).getValue(BuisStukBlock.FACING) == draai.rotate(Direction.EAST), "the tube pieces are turned too");
        level.destroyBlock(k.w(Centrale.S2_KNIP[0]), false);
        helper.assertTrue(Centrale.werkt(k, Centrale.Opstelling.TE_ZWAAR), "and setup 2 can be mended");
        helper.succeed();
    }

    // =====================================================================================================================
    // the Uitvinder-guh
    // =====================================================================================================================

    private static GuhNpcEntity uitvinder(GameTestHelper helper, BlockPos at) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.UITVINDERGUH);
        BlockPos abs = helper.absolutePos(at);
        npc.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    private static int heeft(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    /** The first questline at the NPC: the loan pieces, the salt, the reward once, and nothing of it for the player next to you. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void techquestUitvinderEnBeloning(GameTestHelper helper) {
        ServerPlayer a = speler(helper, helper.absolutePos(new BlockPos(2, 3, 8))), b = speler(helper, helper.absolutePos(new BlockPos(3, 3, 8)));
        GuhNpcEntity npc = uitvinder(helper, new BlockPos(4, 3, 9));
        UitvinderRol rol = new UitvinderRol();
        Item draad = ModBlocks.GUH_WIRE.get().asItem(), zout = FossielmijnFeature.ZOUTKRISTAL.get();
        try {
            rol.talk(npc, a);
            helper.assertTrue(LIJN.stap(a) == 1 && LIJN.stap(b) == 0 && heeft(a, draad) == 2, "a began and got two pieces of Guhdraad");
            rol.talk(npc, a);
            helper.assertTrue(LIJN.stap(a) == 1 && heeft(a, draad) == 2, "talking does not mend a setup, and gives no more wire to who has some");
            a.getInventory().clearContent();
            rol.talk(npc, a);
            helper.assertTrue(heeft(a, draad) == 1, "a lost piece is given again");
            LIJN.zet(a, 3);
            a.getInventory().add(new ItemStack(zout, UitvinderRol.ZOUT - 1));
            rol.talk(npc, a);
            helper.assertTrue(LIJN.stap(a) == 3 && heeft(a, zout) == UitvinderRol.ZOUT - 1, "too little salt: nothing is taken");
            a.getInventory().add(new ItemStack(zout, 3));
            rol.talk(npc, a);
            helper.assertTrue(LIJN.stap(a) == 4 && heeft(a, zout) == 2, "the salt is taken: on to the tubes");
            LIJN.zet(a, 6);
            rol.talk(npc, a);
            helper.assertTrue(heeft(a, TechsausFeature.SAUSSLANG_ITEM.get()) == 1 && LIJN.stap(a) == 6, "he lends the Sausslang for setup 5");
            LIJN.zet(a, 7);
            rol.talk(npc, a);
            helper.assertTrue(LIJN.klaar(a) && heeft(a, BankFeature.BANK_UPGRADE.get()) == 1 && heeft(a, TechquestFeature.RECEPT_SAUS.get()) == 1
                    && heeft(a, TechquestFeature.RECEPT_MACHINES.get()) == 1 && heeft(a, TechquestFeature.RECEPT_BEZORG.get()) == 1,
                    "done: the Bodemloos Knabbelmaagje and the three recipe cards");
            rol.talk(npc, a);
            helper.assertTrue(heeft(a, BankFeature.BANK_UPGRADE.get()) == 1 && MACHINE.stap(a) == 0, "talking again: no second reward");
            LIJN.zet(b, 7);
            rol.talk(npc, b);
            rol.talk(npc, b);
            helper.assertTrue(heeft(b, BankFeature.BANK_UPGRADE.get()) == 1, "b gets a reward of their own, once (talking again starts the second questline)");
            helper.assertTrue(MACHINE.stap(b) == 0 && !AangebrandeMika.verslagen(b), "which waits for the Aangebrande Mika");
            helper.assertTrue(rol.offers(npc) != null && rol.offers(npc).size() == 4, "a lost card can be bought again");
        } finally {
            npc.discard();
            weg(helper, a, b);
        }
        helper.succeed();
    }

    /**
     * De Grote Knabbelmachine: only after the practice hall and the Aangebrande Mika; deliveries in portions, counted per
     * player; five stages; then the statuette, the title and one perfect knabbel a day.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void techquestGroteKnabbelmachine(GameTestHelper helper) {
        ServerPlayer a = speler(helper, helper.absolutePos(new BlockPos(2, 3, 8))), b = speler(helper, helper.absolutePos(new BlockPos(3, 3, 8)));
        GuhNpcEntity npc = uitvinder(helper, new BlockPos(4, 3, 9));
        UitvinderRol rol = new UitvinderRol();
        BlockPos bak = helper.absolutePos(new BlockPos(6, 3, 9));
        try {
            helper.getLevel().setBlock(bak, TechquestFeature.GROTE_KNABBELMACHINE.get().defaultBlockState(), Block.UPDATE_ALL);
            helper.assertTrue(helper.getLevel().getBlockEntity(bak) instanceof TechquestBlocks.Kern, "the kern has its block entity (for the renderer)");
            LIJN.zet(a, LIJN.stappen());
            LIJN.zet(b, LIJN.stappen());
            GuhQuests.saved(a).remove(AangebrandeMika.SLEUTEL);
            rol.talk(npc, a);
            helper.assertTrue(MACHINE.stap(a) == 0, "not before the Aangebrande Mika is beaten");
            AangebrandeMika.zet(a);
            rol.talk(npc, a);
            helper.assertTrue(MACHINE.stap(a) == 1 && Knabbelmachine.fase(a) == 1 && MACHINE.stap(b) == 0, "the plan: stage 1 for a only");
            // a portion: he takes what he needs and counts it
            a.getInventory().add(new ItemStack(Items.COBBLESTONE, 64));
            a.getInventory().add(new ItemStack(Items.BLACKSTONE, 36));
            a.getInventory().add(new ItemStack(Items.OAK_LOG, 64));
            a.getInventory().add(new ItemStack(Items.SPRUCE_LOG, 64));
            a.getInventory().add(new ItemStack(Items.DIAMOND, 3));
            rol.talk(npc, a);
            var eerste = Knabbelmachine.FASEN.get(0);
            helper.assertTrue(MACHINE.stap(a) == 1 && Knabbelmachine.geleverd(a, 1, eerste.get(0)) == 100 && Knabbelmachine.geleverd(a, 1, eerste.get(1)) == 128,
                    "100 of 256 stone and all the logs are in");
            helper.assertTrue(heeft(a, Items.COBBLESTONE) == 0 && heeft(a, Items.OAK_LOG) == 0 && heeft(a, Items.DIAMOND) == 3, "taken from the pockets, nothing else");
            var nodig = Knabbelmachine.nodig(a, 1);
            helper.assertTrue(nodig.size() == 2 && nodig.get(0).heb() == 100 && nodig.get(0).nodig() == 256, "the Guhdex shows what is still needed");
            a.getInventory().add(new ItemStack(Items.COBBLESTONE, 64));
            a.getInventory().add(new ItemStack(Items.COBBLESTONE, 64));
            a.getInventory().add(new ItemStack(Items.COBBLED_DEEPSLATE, 40));
            rol.talk(npc, a);
            helper.assertTrue(MACHINE.stap(a) == 2 && heeft(a, Items.COBBLED_DEEPSLATE) == 12, "the foundation lies; he took no more than he needed");
            // the other four stages in one go each
            geef(a, new ItemStack(BarbecuetherFeature.GRILLKOOL.get().asItem()), 96);
            geef(a, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get()), 128);
            rol.talk(npc, a);
            helper.assertTrue(MACHINE.stap(a) == 3, "the boiler stands");
            geef(a, new ItemStack(GuhpolderFeature.KNABBELMEEL.get()), 128);
            geef(a, new ItemStack(ModBlocks.GUH_WIRE.get().asItem()), 64);
            rol.talk(npc, a);
            helper.assertTrue(MACHINE.stap(a) == 4, "the stomach is in");
            for (int i = 0; i < 12; i++) {
                a.getInventory().add(new ItemStack(SpiesburchtFeature.ROOKLOOPDRANKJE.get()));
            }
            geef(a, new ItemStack(Items.GLASS), 128);
            rol.talk(npc, a);
            helper.assertTrue(MACHINE.stap(a) == 5, "it has a snoet");
            geef(a, new ItemStack(SpiesburchtFeature.GLOEISTER.get()), 1);
            geef(a, new ItemStack(ModItems.KAAS_KNABBELS.get()), 256);
            rol.talk(npc, a);
            helper.assertTrue(MACHINE.klaar(a) && Knabbelmachine.staat(a) && heeft(a, TechquestFeature.KNABBELMACHINE_BEELDJE_ITEM.get()) == 1,
                    "the heart glows: the machine stands, and the statuette is a's");
            Titels.Titel titel = Titels.van(TechquestFeature.KNABBELMACHINIST);
            helper.assertTrue(titel != null && titel.behaald().test(a) && !titel.behaald().test(b), "a is a Knabbelmachinist, b is not");
            // one perfect knabbel a day, per player
            Item knabbel = TechquestFeature.PERFECTE_KNABBEL.get();
            helper.assertTrue(Knabbelmachine.knabbelKlaar(a) && !Knabbelmachine.knabbelKlaar(b), "a knabbel lies ready for a");
            Knabbelmachine.klik(b, bak);
            helper.assertTrue(heeft(b, knabbel) == 0, "nothing for who has no machine");
            Knabbelmachine.klik(a, bak);
            Knabbelmachine.klik(a, bak);
            helper.assertTrue(heeft(a, knabbel) == 1 && !Knabbelmachine.knabbelKlaar(a), "one a day");
            GuhQuests.saved(a).putLong(Knabbelmachine.DAG, Knabbelmachine.dag(a) - 1);
            Knabbelmachine.klik(a, bak);
            helper.assertTrue(heeft(a, knabbel) == 2, "the next day: another one");
            rol.talk(npc, a);
            helper.assertTrue(heeft(a, TechquestFeature.KNABBELMACHINE_BEELDJE_ITEM.get()) == 1, "the statuette only once");
        } finally {
            helper.getLevel().setBlock(bak, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            npc.discard();
            weg(helper, a, b);
        }
        helper.succeed();
    }

    private static void geef(ServerPlayer p, ItemStack soort, int n) {
        while (n > 0) {
            int k = Math.min(n, soort.getMaxStackSize());
            p.getInventory().add(soort.copyWithCount(k));
            n -= k;
        }
    }

    // =====================================================================================================================
    // the recipe cards, the building
    // =====================================================================================================================

    /** The machines of the "Saus" tier can only be crafted with their recipe card in the grid, and the card stays. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void techquestReceptkaarten(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var recepten = level.recipeAccess();
        var kaarten = java.util.Map.of(
                TechquestFeature.RECEPT_SAUS.get(), List.of("sauspomp", "sausvat", "brouwautomaat", "frituurautomaat", "grillkoolpers", "blubkacheltje"),
                TechquestFeature.RECEPT_MACHINES.get(), List.of("knabbelaar", "neerzetter", "knutselmachine", "tekentafel"),
                TechquestFeature.RECEPT_BEZORG.get(), List.of("stepstation", "haltepaaltje"));
        kaarten.forEach((kaart, namen) -> {
            for (String naam : namen) {
                var r = recepten.byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(naam)));
                helper.assertTrue(r.isPresent() && r.get().value().placementInfo().ingredients().stream().anyMatch(i -> i.test(new ItemStack(kaart))),
                        naam + " needs its recipe card");
            }
        });
        for (String vrij : List.of("guh_wheel", "guh_oven", "vadsmolen", "knabbelbuis", "hapluikje", "oogster", "sausslang", "gloeisterkern")) {
            var r = recepten.byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(vrij)));
            helper.assertTrue(r.isPresent() && r.get().value().placementInfo().ingredients().stream().noneMatch(i -> i.test(new ItemStack(TechquestFeature.RECEPT_SAUS.get()))
                    || i.test(new ItemStack(TechquestFeature.RECEPT_MACHINES.get())) || i.test(new ItemStack(TechquestFeature.RECEPT_BEZORG.get()))),
                    vrij + " needs no card");
        }
        // the Knabbelaar: card, pickaxe, iron / grillspies, Guhdraad, grillspies / stone, knabbels, stone
        ItemStack ijzer = new ItemStack(Items.IRON_INGOT), spies = new ItemStack(SpiesburchtFeature.GRILLSPIES.get()), steen = new ItemStack(Items.COBBLESTONE);
        List<ItemStack> zonder = new ArrayList<>(List.of(ijzer, new ItemStack(Items.IRON_PICKAXE), ijzer, spies, new ItemStack(ModBlocks.GUH_WIRE.get().asItem()), spies,
                steen, new ItemStack(ModItems.KAAS_KNABBELS.get()), steen));
        List<ItemStack> met = new ArrayList<>(zonder);
        met.set(0, new ItemStack(TechquestFeature.RECEPT_MACHINES.get()));
        helper.assertTrue(recepten.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, zonder), level).isEmpty(), "no Knabbelaar without the card");
        var recept = recepten.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, met), level);
        helper.assertTrue(recept.isPresent() && recept.get().value().assemble(CraftingInput.of(3, 3, met)).is(TechmachineFeature.KNABBELAAR_ITEM.get()),
                "with the card: a Knabbelaar");
        helper.assertTrue(recept.get().value().getRemainingItems(CraftingInput.of(3, 3, met)).stream().anyMatch(s -> s.is(TechquestFeature.RECEPT_MACHINES.get())),
                "the card stays in the grid");
        helper.succeed();
    }

    private static int tel(StructureTemplate template, Block block) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();
    }

    /**
     * The building: its template holds the kern of the machine and the Uitvinder-guh but none of the guh machines (those do
     * not turn with a template: Centrale puts them down); a generated copy maps the template's coordinates, is protected,
     * and lets a player cut only the practice hall's own wires.
     */
    @GuhTest(template = KAMER, batch = "techquest_gebouw")
    public static void techquestGebouw(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate template = level.getStructureManager().get(Guhs.id(Centrale.STRUCTUUR)).orElse(null);
        helper.assertTrue(template != null && template.getSize().getX() == 47 && template.getSize().getZ() == 47, "the template of the Oude Guhrad-centrale");
        helper.assertTrue(tel(template, TechquestFeature.GROTE_KNABBELMACHINE.get()) == 1 && tel(template, Blocks.BARRIER) > 50, "the kern of the machine and its closed-off body");
        var kern = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), TechquestFeature.GROTE_KNABBELMACHINE.get()).get(0);
        helper.assertTrue(kern.pos().equals(Centrale.KERN[0]) && kern.state().getValue(HorizontalDirectionalBlock.FACING) == Direction.WEST, "at its spot, looking at the aisle");
        for (Block nooit : List.of(ModBlocks.GUH_WHEEL.get(), ModBlocks.GUH_WHEEL_PART.get(), ModBlocks.GUH_WIRE.get(), GuhovenFeature.GUH_OVEN.get(),
                TechbuisFeature.KNABBELBUIS.get(), TechsausFeature.SAUSSLANG.get(), TechmachineFeature.TEKENTAFEL.get())) {
            helper.assertTrue(tel(template, nooit) == 0, "no " + nooit + " in the template");
        }
        boolean npc = false;
        for (Tag t : template.save(new CompoundTag()).getListOrEmpty("entities")) {
            CompoundTag e = t.asCompound().orElse(new CompoundTag());
            var pos = e.getListOrEmpty("blockPos");
            BlockPos plek = Centrale.UITVINDER[0];
            npc |= e.getCompoundOrEmpty("nbt").getCompoundOrEmpty("NeoForgeData").getStringOr(Bezetting.TAG, "").equals(TechquestFeature.UITVINDER_ID)
                    && pos.getIntOr(0, -1) == plek.getX() && pos.getIntOr(1, -1) == plek.getY() && pos.getIntOr(2, -1) == plek.getZ();
        }
        helper.assertTrue(npc, "the Uitvinder-guh sits in the template, with his Bezetting tag");
        // a generated copy (a structure start with the test room as its piece: template (7, 3, 9) is the corner of the room's floor)
        Structure structure = Kopieen.structuur(level, Centrale.STRUCTUUR);
        helper.assertTrue(structure != null, "the structure guhs:" + Centrale.STRUCTUUR + " exists");
        BlockPos pos = helper.absolutePos(new BlockPos(-7, -1, -9));
        StructurePoolElement element = StructurePoolElement.single("guhs:" + KAMER).apply(StructureTemplatePool.Projection.RIGID);
        BoundingBox box = element.getBoundingBox(level.getStructureManager(), pos, Rotation.NONE);
        StructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, pos, 0, Rotation.NONE, box, LiquidSettings.IGNORE_WATERLOGGING);
        StructureStart start = new StructureStart(structure, ChunkPos.containing(pos), 0, new PiecesContainer(List.of(piece)));
        Kopieen.test(level, start);
        ServerPlayer p = speler(helper, helper.absolutePos(new BlockPos(10, 3, 6)));
        try {
            Centrale.Kopie k = Centrale.van(level, start);
            helper.assertTrue(k != null && k.w(Centrale.RADEREN[0]).equals(helper.absolutePos(new BlockPos(4, 3, 1))) && k.draai() == Rotation.NONE, "the copy maps template coordinates");
            // (only the practice hall is furnished: the test room is no bigger)
            Centrale.richtIn(new Centrale.Kopie(level, k.sleutel(), k.plek(), k.draai(), false));
            BlockPos knip = k.w(Centrale.S2_KNIP[0]), draad = k.w(Centrale.S2_DRAAD[0]);
            Centrale.Kopie gevonden = Centrale.bij(level, knip);
            helper.assertTrue(gevonden != null && gevonden.w(Centrale.S1_GAT[0]).equals(k.w(Centrale.S1_GAT[0])), "the copy is found from a spot in it");
            helper.assertTrue(Bescherming.beschermd(level, knip) && Centrale.STRUCTUUR.equals(Bescherming.structuurBij(level, draad)), "the practice hall is protected");
            helper.assertTrue(Bescherming.mag(p, knip) && !Bescherming.mag(p, draad) && !Bescherming.mag(p, k.w(Centrale.RADEREN[1])),
                    "a player may cut the marked wire, and nothing else");
            p.gameMode.destroyBlock(draad);
            p.gameMode.destroyBlock(k.w(Centrale.S2_OVENS[0]));
            helper.assertTrue(level.getBlockState(draad).is(ModBlocks.GUH_WIRE.get()) && level.getBlockState(k.w(Centrale.S2_OVENS[0])).is(GuhovenFeature.GUH_OVEN.get()),
                    "the building stays whole");
            p.gameMode.destroyBlock(knip);
            helper.assertTrue(level.getBlockState(knip).isAir() && Centrale.werkt(k, Centrale.Opstelling.TE_ZWAAR), "the marked wire goes: setup 2 works");
            // the old guh stays in its wheel
            BlockPos rad = k.w(Centrale.RADEREN[0]);
            level.getBlockState(rad).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(rad), Direction.SOUTH, rad, false));
            helper.assertTrue(((GuhWheelBlockEntity) level.getBlockEntity(rad)).hasGuh(), "nobody takes an old guh out of its wheel");
        } finally {
            Kopieen.testWissen(level);
            weg(helper, p);
        }
        helper.succeed();
    }
}
