package nl.juiced.guhs.feature.fabriek;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.dieren.DierenSlice;
import nl.juiced.guhs.feature.bio.dieren.WolkenschaapjeEntity;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.techmachine.TechBlockEntity;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of de Fabriekshal (1.8.0, batch "fabriek"): the Bouwblokken-fabriek makes stairs of knuffelsteen and works
 * through its bank link, the Scheerstal shears, the Spinmachine spins, the Verfbad dyes, the Kleurmolen grinds with a bonus,
 * the Regenboogpers presses the picked colour, and the Wolkenschaapje is picked up and bred like a maatje. Template
 * techmachine_test_kamer (9 x 6 x 9, a stone floor, things stand at helper y 2).
 */
public class FabriekGameTests {
    private static final String BATCH = "fabriek", KAMER = "techmachine_test_kamer";

    private static BlockPos p(int x, int z) {
        return new BlockPos(x, 2, z);
    }

    /** A machine with its snoet to the north and a test source behind it. */
    @SuppressWarnings("unchecked")
    private static <T extends TechBlockEntity> T machine(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        helper.setBlock(pos.south(), VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 5));
        return (T) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static void leg(TechBlockEntity m, int vak, ItemStack stack) {
        m.vakken().set(vak, ItemResource.of(stack), stack.getCount());
    }

    private static int tel(TechBlockEntity m, Item item) {
        return (int) Kisten.tel(m.vakken(), s -> s.is(item));
    }

    private static Item guhs(String id) {
        Item i = Bio.item(id, Items.AIR);
        if (i == Items.AIR) {
            throw new IllegalStateException("no guhs:" + id);
        }
        return i;
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }

    // =====================================================================================================================

    /** Six knuffelsteen and a knuffelsteen trap as the example: four stairs, the example stays. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void fabriekMaaktTrappenUitKnuffelsteen(GameTestHelper helper) {
        BouwblokkenFabriekBlockEntity f = machine(helper, p(4, 4), FabriekFeature.BOUWBLOKKEN_FABRIEK.get());
        Item steen = guhs("knuffelsteen"), trap = guhs("knuffelsteen_trap");
        leg(f, BouwblokkenFabriekBlockEntity.BASIS, new ItemStack(steen, 7));
        leg(f, BouwblokkenFabriekBlockEntity.VOORBEELD, new ItemStack(trap));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(tel(f, trap) == 5, "4 stairs made (+ the example): " + tel(f, trap)))
                .thenIdle(30)
                .thenExecute(() -> {
                    helper.assertTrue(tel(f, steen) == 1, "6 knuffelsteen used, 1 left over: " + tel(f, steen));
                    helper.assertTrue(tel(f, trap) == 5, "no more stairs without blocks: " + tel(f, trap));
                    helper.assertTrue(f.vakken().getAmountAsInt(BouwblokkenFabriekBlockEntity.VOORBEELD) == 1, "the example stays");
                })
                .thenSucceed();
    }

    /** Linked to a Bank Guh: it takes the blocks from the bank and puts what it makes back into it; a chest link too. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void fabriekBankLink(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(p(1, 1), ModBlocks.BANK_GUH.get());
        BankGuhBlockEntity bank = (BankGuhBlockEntity) level.getBlockEntity(helper.absolutePos(p(1, 1)));
        BouwblokkenFabriekBlockEntity f = machine(helper, p(4, 4), FabriekFeature.BOUWBLOKKEN_FABRIEK.get());
        Item steen = guhs("knuffelsteen"), plaat = guhs("knuffelsteen_plaat");
        bank.getStorage().insert(new ItemStack(steen, 6));
        f.koppelBank(bank.bankId());
        leg(f, BouwblokkenFabriekBlockEntity.VOORBEELD, new ItemStack(plaat));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(bank.getStorage().count(new ItemStack(plaat)) == 12,
                        "the slabs went into the bank: " + bank.getStorage().count(new ItemStack(plaat))))
                .thenExecute(() -> {
                    helper.assertTrue(bank.getStorage().count(new ItemStack(steen)) == 0, "the knuffelsteen came out of the bank");
                    helper.assertTrue(tel(f, plaat) == 1, "nothing in its own slots but the example");
                    // now a chest instead: forget the bank, link the chest
                    f.koppelBank(null);
                    helper.setBlock(p(7, 1), Blocks.CHEST);
                    f.koppelKist(helper.absolutePos(p(7, 1)));
                    Kisten.stop(Kisten.van(level, helper.absolutePos(p(7, 1)), null), new ItemStack(steen, 3));
                })
                .thenWaitUntil(() -> helper.assertTrue(Kisten.tel(Kisten.van(level, helper.absolutePos(p(7, 1)), null), s -> s.is(plaat)) == 6,
                        "the linked chest got 6 slabs"))
                .thenSucceed();
    }

    /** The Scheerstal shears a kept, fluffy wolkenschaapje around it: wolkenpluis in its slots, the schaapje bald. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void fabriekScheerstalScheert(GameTestHelper helper) {
        ScheerstalBlockEntity stal = machine(helper, p(4, 6), FabriekFeature.SCHEERSTAL.get());
        WolkenschaapjeEntity s = helper.spawn(DierenSlice.WOLKENSCHAAPJE.get(), p(4, 3));
        s.houd(null);
        s.setNoAi(true);
        Item pluis = guhs("wolkenpluis");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(tel(stal, pluis) > 0, "wolkenpluis in the Scheerstal"))
                .thenExecute(() -> helper.assertTrue(s.isGeschoren(), "the schaapje is shorn"))
                .thenSucceed();
    }

    /** Four pluis are two white wool. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void fabriekSpinmachineSpintWol(GameTestHelper helper) {
        SpinmachineBlockEntity m = machine(helper, p(4, 4), FabriekFeature.SPINMACHINE.get());
        leg(m, SpinmachineBlockEntity.IN, new ItemStack(FabriekFeature.PLUIS.get(), 4));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(tel(m, Items.WHITE_WOOL) == 2, "two wool: " + tel(m, Items.WHITE_WOOL)))
                .thenExecute(() -> helper.assertTrue(tel(m, FabriekFeature.PLUIS.get()) == 0, "the pluis is used"))
                .thenSucceed();
    }

    /** White wool + red dye = red wool. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void fabriekVerfbadVerftWol(GameTestHelper helper) {
        VerfbadBlockEntity m = machine(helper, p(4, 4), FabriekFeature.VERFBAD.get());
        leg(m, VerfbadBlockEntity.BLOK, new ItemStack(Items.WHITE_WOOL, 2));
        leg(m, VerfbadBlockEntity.VERF, new ItemStack(Items.RED_DYE, 2));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(tel(m, Items.RED_WOOL) == 2, "two red wool: " + tel(m, Items.RED_WOOL)))
                .thenExecute(() -> helper.assertTrue(tel(m, Items.RED_DYE) == 0 && tel(m, Items.WHITE_WOOL) == 0, "wool and dye used"))
                .thenSucceed();
    }

    /** A cornflower is 2 or 3 blue dye; a maanbloemetje is black (a colour without a vanilla flower). */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void fabriekKleurmolenMaaltMetBonus(GameTestHelper helper) {
        KleurmolenBlockEntity m = machine(helper, p(4, 4), FabriekFeature.KLEURMOLEN.get());
        helper.assertTrue(Kleuren.van(helper.getLevel(), new ItemStack(guhs("maanbloemetje"))).is(Items.BLACK_DYE), "maanbloemetje = black");
        for (DyeColor k : DyeColor.values()) {
            helper.assertTrue(Kleuren.kleur(new ItemStack(Kleuren.kleurstof(k))) == k, "dye of " + k);
        }
        leg(m, KleurmolenBlockEntity.IN, new ItemStack(Items.CORNFLOWER));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(tel(m, Items.BLUE_DYE) >= 2, "blue dye"))
                .thenExecute(() -> helper.assertTrue(tel(m, Items.BLUE_DYE) <= 3 && tel(m, Items.CORNFLOWER) == 0,
                        "one cornflower = 2 or 3 blue dye: " + tel(m, Items.BLUE_DYE)))
                .thenSucceed();
    }

    /** Pluis + a bucket of kaassaus, colour lime: lime dye, and the empty bucket comes back. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void fabriekRegenboogpersPerstGekozenKleur(GameTestHelper helper) {
        RegenboogpersBlockEntity m = machine(helper, p(4, 4), FabriekFeature.REGENBOOGPERS.get());
        m.kies(DyeColor.LIME);
        leg(m, RegenboogpersBlockEntity.PLUIS, new ItemStack(FabriekFeature.PLUIS.get(), 2));
        leg(m, RegenboogpersBlockEntity.EMMER, new ItemStack(ModItems.KAAS_SAUS_BUCKET.get()));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(tel(m, Items.LIME_DYE) == 4, "two jobs of 2 lime dye: " + tel(m, Items.LIME_DYE)))
                .thenExecute(() -> {
                    helper.assertTrue(tel(m, Items.BUCKET) == 1, "the empty bucket");
                    helper.assertTrue(m.saus() == 1000 - 2 * FabriekGetallen.PERS_SAUS, "kaassaus used: " + m.saus());
                    leg(m, RegenboogpersBlockEntity.VOORBEELD, new ItemStack(Items.PURPLE_DYE));
                    helper.assertTrue(m.kleur() == DyeColor.PURPLE, "an example dye picks the colour");
                })
                .thenSucceed();
    }

    /** A tame wolkenschaapje is picked up and put down like a maatje; two tame ones with wolkensuikerspin get a lammetje. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 600)
    public static void fabriekSchaapjeOppakkenEnKweken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        WolkenschaapjeEntity s = helper.spawn(DierenSlice.WOLKENSCHAAPJE.get(), p(2, 2));
        // wolkensuikerspin tames it
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(guhs("wolkensuikerspin"), 4));
        s.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(s.isTame() && s.isOwnedBy(p) && s.isGehouden(), "wolkensuikerspin: it is yours (tame)");
        // picked up
        helper.assertTrue(PiepDierItem.pakOp(s, p), "picked up");
        helper.assertTrue(s.isRemoved(), "gone from the world");
        ItemStack item = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(FabriekFeature.WOLKENSCHAAPJE_ITEM.get())) {
                item = p.getInventory().getItem(i);
            }
        }
        helper.assertTrue(!item.isEmpty(), "a wolkenschaapje_item in the pockets");
        WolkenschaapjeEntity terug = PiepDierItem.zetNeer(item, level, helper.absoluteVec(p(2, 2).getCenter()), 0f, DierenSlice.WOLKENSCHAAPJE.get());
        helper.assertTrue(terug != null && terug.isTame() && terug.isOwnedBy(p), "put down again: still tame, still yours");
        // breeding
        WolkenschaapjeEntity b = helper.spawn(DierenSlice.WOLKENSCHAAPJE.get(), p(3, 2));
        b.tame(p);
        b.houd(p);
        terug.setAge(0);
        for (WolkenschaapjeEntity x : new WolkenschaapjeEntity[] {terug, b}) {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(guhs("wolkensuikerspin"), 4));
            x.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(x.isInLove(), "wolkensuikerspin makes a tame schaapje fall in love");
        }
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getEntitiesOfClass(WolkenschaapjeEntity.class, kamer, WolkenschaapjeEntity::isBaby).size() == 1,
                        "a lammetje is born"))
                .thenExecute(() -> {
                    WolkenschaapjeEntity lam = level.getEntitiesOfClass(WolkenschaapjeEntity.class, kamer, WolkenschaapjeEntity::isBaby).get(0);
                    helper.assertTrue(lam.isTame() && lam.isOwnedBy(p), "the lammetje belongs to the same keeper");
                    for (WolkenschaapjeEntity x : level.getEntitiesOfClass(WolkenschaapjeEntity.class, kamer)) {
                        x.discard();
                    }
                    level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
                })
                .thenSucceed();
    }
}
