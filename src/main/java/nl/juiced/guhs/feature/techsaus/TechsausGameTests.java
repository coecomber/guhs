package nl.juiced.guhs.feature.techsaus;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.FryingPanBlockEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;
import nl.juiced.guhs.feature.spiesburcht.GuhbrouwketelBlockEntity;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.Sauzen;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsNet;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.taal.NlTekst;

/**
 * Game tests of the sauce machines (bbq2 tech-vloeistof, batch "techsaus"): the pump lifts sauce out of a source that stays,
 * hoses carry it (round a bend, to two vats, and not past a cut), the vat is tapped and filled with buckets and keeps its
 * sauce as an item, the Brouwautomaat, the Frituurautomaat and the Grillkoolpers do their work on vadskracht and stand still
 * without it, pipes only reach the right slots, and the Guhbrouwketel and the frying pan still work by hand.
 * Template techsaus_test_kamer: 11 x 6 x 7 with a stone floor two blocks thick: the top layer of the floor is helper y 2
 * (a source is dug into it), things stand at helper y 3.
 */
public class TechsausGameTests {
    private static final String BATCH = "techsaus", KAMER = "techsaus_test_kamer";

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** A spot on the floor of the test room. */
    private static BlockPos p(int x, int z) {
        return new BlockPos(x, 3, z);
    }

    /** A source block of this sauce, dug into the floor under p(x, z). */
    private static void bron(GameTestHelper helper, int x, int z, Block saus) {
        helper.setBlock(new BlockPos(x, 2, z), saus);
    }

    /** A test source of kracht x 10 vadskracht. */
    private static void kracht(GameTestHelper helper, BlockPos pos, int kracht) {
        helper.setBlock(pos, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, kracht));
    }

    private static <T> T zet(GameTestHelper helper, BlockPos pos, Block block, Class<T> soort) {
        helper.setBlock(pos, block);
        return soort.cast(helper.getLevel().getBlockEntity(helper.absolutePos(pos)));
    }

    private static SausvatBlockEntity vat(GameTestHelper helper, BlockPos pos, FluidResource saus, int mb) {
        SausvatBlockEntity vat = zet(helper, pos, TechsausFeature.SAUSVAT.get(), SausvatBlockEntity.class);
        if (mb > 0) {
            vat.tank().zet(saus, mb);
        }
        return vat;
    }

    private static void slang(GameTestHelper helper, BlockPos... plekken) {
        for (BlockPos pos : plekken) {
            helper.setBlock(pos, TechsausFeature.SAUSSLANG.get());
        }
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer speler = GuhMockPlayer.of(helper);
        speler.setGameMode(GameType.SURVIVAL);
        return speler;
    }

    private static void weg(GameTestHelper helper, ServerPlayer speler) {
        helper.getLevel().removePlayerImmediately(speler, Entity.RemovalReason.DISCARDED);
    }

    /** A right-click on the block with this in the main hand. */
    private static void klik(GameTestHelper helper, ServerPlayer speler, BlockPos pos, ItemStack stack) {
        speler.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.useBlock(pos, speler);
    }

    private static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    private static Snoet snoet(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(MachineBlock.SNOET);
    }

    /** The hover readout of this block as a Dutch player reads it. */
    private static List<String> lees(GameTestHelper helper, BlockPos pos) {
        return VadsKracht.regels(helper.getLevel(), helper.absolutePos(pos)).stream().map(NlTekst::tekst).toList();
    }

    private static boolean heeftSleutel(List<Component> regels, String sleutel) {
        return regels.stream().anyMatch(r -> r.getContents() instanceof TranslatableContents t && t.getKey().equals(sleutel));
    }

    private static int tel(SausMachineBlockEntity machine, int van, int tot, ItemStack soort) {
        int n = 0;
        for (int vak = van; vak < tot; vak++) {
            if (machine.vakken().getResource(vak).matches(soort)) {
                n += machine.vakken().getAmountAsInt(vak);
            }
        }
        return n;
    }

    // =====================================================================================================================
    // the pump
    // =====================================================================================================================

    /** Without vadskracht nothing; with it a bucket per two seconds; the source block stays; a full tank looks surprised. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techsausPompSlurptUitEenBron(GameTestHelper helper) {
        bron(helper, 2, 2, Blocks.WATER);
        SauspompBlockEntity pomp = zet(helper, p(2, 2), TechsausFeature.SAUSPOMP.get(), SauspompBlockEntity.class);
        SauspompBlockEntity droog = zet(helper, p(6, 2), TechsausFeature.SAUSPOMP.get(), SauspompBlockEntity.class);
        kracht(helper, p(6, 1), 1);
        gelijk(helper, Sauzen.water(), pomp.bron(), "the pump sees the water under it");
        helper.assertTrue(droog.bron() == null, "stone is no source");
        helper.assertTrue(helper.getBlockState(p(2, 2)).is(VadsKracht.TOON), "the pump is in the tag guhs:vadskracht");
        helper.startSequence()
                .thenIdle(30)
                .thenExecute(() -> {
                    helper.assertTrue(pomp.tank().isLeeg() && !pomp.heeftKracht(), "no vadskracht: nothing is pumped");
                    gelijk(helper, Snoet.SLAAPT, snoet(helper, p(2, 2)), "asleep without vadskracht");
                    helper.assertTrue(droog.tank().isLeeg() && droog.heeftKracht(), "vadskracht but no source: nothing is pumped");
                    helper.assertTrue(lees(helper, p(6, 2)).contains("Wacht op een bron: zet de pomp bovenop saus of water"),
                            "the readout says what it waits for: " + lees(helper, p(6, 2)));
                    kracht(helper, p(2, 1), 1);
                })
                .thenWaitUntil(() -> helper.assertTrue(pomp.tank().inhoud() >= Sauzen.EMMER, "the first bucket: " + pomp.tank().inhoud()))
                .thenExecute(() -> {
                    gelijk(helper, Sauzen.water(), pomp.tank().saus(), "it is water");
                    gelijk(helper, Snoet.WERKT, snoet(helper, p(2, 2)), "happy while it pumps");
                    helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(2, 2, 2))).isSource(), "the source stays");
                    helper.assertTrue(pomp.bezig(), "bezig (for the drips)");
                    gelijk(helper, 0, Sauzen.stop(Sauzen.van(helper.getLevel(), helper.absolutePos(p(2, 2)), Direction.EAST), Sauzen.water(), 500, false),
                            "a hose cannot push sauce INTO a pump");
                })
                .thenWaitUntil(() -> gelijk(helper, SausGetallen.POMP_TANK, pomp.tank().inhoud(), "the tank fills up"))
                .thenIdle(3)
                .thenExecute(() -> {
                    gelijk(helper, Snoet.VOL, snoet(helper, p(2, 2)), "surprised when full");
                    gelijk(helper, 15, helper.getBlockState(p(2, 2)).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(p(2, 2)), Direction.NORTH),
                            "a comparator reads a full tank");
                    helper.assertTrue(lees(helper, p(2, 2)).contains("4,0 van de 4,0 emmers water"), "the readout shows the tank: " + lees(helper, p(2, 2)));
                    // a bucket by hand
                    ServerPlayer speler = speler(helper);
                    klik(helper, speler, p(2, 2), new ItemStack(Items.BUCKET));
                    helper.assertTrue(speler.getMainHandItem().is(Items.WATER_BUCKET), "an empty bucket scoops from the pump: " + speler.getMainHandItem());
                    gelijk(helper, SausGetallen.POMP_TANK - Sauzen.EMMER, pomp.tank().inhoud(), "a bucket less");
                    weg(helper, speler);
                })
                .thenSucceed();
    }

    // =====================================================================================================================
    // hoses
    // =====================================================================================================================

    /** Pump - hose with a bend and a fork - two vats: both fill, evenly; cut the hose and it stops. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 500)
    public static void techsausSlangBrengtSausNaarHetVat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        bron(helper, 2, 2, ModBlocks.KAAS_SAUS.get());
        SauspompBlockEntity pomp = zet(helper, p(2, 2), TechsausFeature.SAUSPOMP.get(), SauspompBlockEntity.class);
        slang(helper, p(3, 2), p(4, 2), p(4, 3));
        SausvatBlockEntity ver = vat(helper, p(4, 4), FluidResource.EMPTY, 0);        // round the bend
        SausvatBlockEntity dichtbij = vat(helper, p(5, 2), FluidResource.EMPTY, 0);   // at the fork
        SausvatBlockEntity los = vat(helper, p(8, 4), FluidResource.EMPTY, 0);        // joined to nothing
        kracht(helper, p(2, 1), 1);
        // the hose joined what is around it, whatever was placed first
        BlockState bocht = helper.getBlockState(p(4, 2));
        helper.assertTrue(bocht.getValue(SausslangBlock.kant(Direction.WEST)) && bocht.getValue(SausslangBlock.kant(Direction.SOUTH))
                && bocht.getValue(SausslangBlock.kant(Direction.EAST)), "the fork joins west (hose), south (hose) and east (vat): " + bocht);
        helper.assertTrue(!bocht.getValue(SausslangBlock.kant(Direction.NORTH)) && !bocht.getValue(SausslangBlock.kant(Direction.UP))
                && !bocht.getValue(SausslangBlock.kant(Direction.DOWN)), "and nothing else (stone holds no sauce): " + bocht);
        helper.assertTrue(helper.getBlockState(p(3, 2)).getValue(SausslangBlock.kant(Direction.WEST)), "the first hose joins the pump");
        gelijk(helper, 2, (int) Slangen.aansluitingen(level, helper.absolutePos(p(2, 2))).stream().filter(a -> a.handler(level) != null).count(),
                "the pump reaches two vats");
        int[] toen = new int[2];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ver.tank().inhoud() >= 2 * Sauzen.EMMER, "the far vat fills: " + ver.tank().inhoud()))
                .thenExecute(() -> {
                    gelijk(helper, Sauzen.kaassaus(), ver.tank().saus(), "kaassaus");
                    helper.assertTrue(Math.abs(ver.tank().inhoud() - dichtbij.tank().inhoud()) <= SausGetallen.SLANG_PER_KEER,
                            "both vats get an equal share: " + ver.tank().inhoud() + " / " + dichtbij.tank().inhoud());
                    helper.assertTrue(los.tank().isLeeg(), "a vat that is not joined gets nothing");
                    gelijk(helper, SausvatBlock.Saus.KAASSAUS, helper.getBlockState(p(4, 4)).getValue(SausvatBlock.SAUS), "the gauge shows kaassaus");
                    gelijk(helper, 1, helper.getBlockState(p(4, 4)).getValue(SausvatBlock.NIVEAU), "2 of 16 buckets: the first third");
                    // cut the hose behind the fork
                    helper.setBlock(p(4, 3), Blocks.AIR);
                    helper.assertFalse(helper.getBlockState(p(4, 2)).getValue(SausslangBlock.kant(Direction.SOUTH)), "the fork lets go of the cut side");
                    toen[0] = ver.tank().inhoud();
                    toen[1] = dichtbij.tank().inhoud();
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    gelijk(helper, toen[0], ver.tank().inhoud(), "nothing comes past the cut");
                    helper.assertTrue(dichtbij.tank().inhoud() > toen[1], "the other vat gets everything now");
                    helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(2, 2, 2))).isSource(), "the source is still there");
                    helper.assertTrue(pomp.heeftKracht(), "the pump still runs");
                })
                .thenSucceed();
    }

    // =====================================================================================================================
    // the vat
    // =====================================================================================================================

    /** Buckets in and out (all four sauces, one at a time, never lava), the gauge, the face, a comparator, and the item keeps the sauce. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void techsausVatTaptMetEenEmmer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plek = p(2, 2);
        SausvatBlockEntity vat = vat(helper, plek, FluidResource.EMPTY, 0);
        ServerPlayer speler = speler(helper);
        gelijk(helper, SausvatBlock.Saus.LEEG, helper.getBlockState(plek).getValue(SausvatBlock.SAUS), "an empty vat");
        klik(helper, speler, plek, new ItemStack(Items.BUCKET));
        helper.assertTrue(speler.getMainHandItem().is(Items.BUCKET) && vat.tank().isLeeg(), "an empty bucket on an empty vat: nothing");
        klik(helper, speler, plek, new ItemStack(Items.LAVA_BUCKET));
        helper.assertTrue(speler.getMainHandItem().is(Items.LAVA_BUCKET) && vat.tank().isLeeg(), "lava is no sauce");
        helper.assertTrue(level.getBlockState(helper.absolutePos(plek).above()).isAir() && level.getBlockState(helper.absolutePos(plek).north()).isAir(),
                "and the refused bucket is not poured into the world");
        for (ItemStack emmer : List.of(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.MILK_BUCKET), new ItemStack(ModItems.KAAS_SAUS_BUCKET.get()),
                new ItemStack(BarbecuetherFeature.KAASFRITUURSAUS_BUCKET.get()))) {
            klik(helper, speler, plek, emmer.copy());
            helper.assertTrue(speler.getMainHandItem().is(Items.BUCKET), emmer + " is poured in: " + speler.getMainHandItem());
            gelijk(helper, Sauzen.EMMER, vat.tank().inhoud(), "a bucket in the vat");
            helper.assertTrue(helper.getBlockState(plek).getValue(SausvatBlock.SAUS) != SausvatBlock.Saus.LEEG
                    && helper.getBlockState(plek).getValue(SausvatBlock.NIVEAU) == 1, "the gauge shows it: " + helper.getBlockState(plek));
            // one sauce at a time
            ItemStack anders = emmer.is(Items.WATER_BUCKET) ? new ItemStack(Items.MILK_BUCKET) : new ItemStack(Items.WATER_BUCKET);
            klik(helper, speler, plek, anders.copy());
            helper.assertTrue(ItemStack.isSameItem(speler.getMainHandItem(), anders) && vat.tank().inhoud() == Sauzen.EMMER, "another sauce is refused");
            klik(helper, speler, plek, new ItemStack(Items.BUCKET));
            helper.assertTrue(ItemStack.isSameItem(speler.getMainHandItem(), emmer), "tapped out again: " + speler.getMainHandItem());
            helper.assertTrue(vat.tank().isLeeg() && helper.getBlockState(plek).getValue(SausvatBlock.NIVEAU) == 0, "and the vat is empty");
        }
        helper.assertTrue(GidsFeature.heeft(speler, "techniek/tech_vloeistof_getapt"), "tapping with a bucket is rewarded");
        // the gauge and the comparator
        gelijk(helper, 0, helper.getBlockState(plek).getAnalogOutputSignal(level, helper.absolutePos(plek), Direction.NORTH), "comparator: empty");
        gelijk(helper, 1, SausvatBlock.niveau(1, SausGetallen.VAT), "niveau: a drop");
        gelijk(helper, 2, SausvatBlock.niveau(SausGetallen.VAT / 2, SausGetallen.VAT), "niveau: half");
        gelijk(helper, 3, SausvatBlock.niveau(SausGetallen.VAT - 1, SausGetallen.VAT), "niveau: almost full is not full");
        vat.tank().zet(Sauzen.melk(), SausGetallen.VAT);
        gelijk(helper, 4, helper.getBlockState(plek).getValue(SausvatBlock.NIVEAU), "full");
        gelijk(helper, SausvatBlock.Saus.MELK, helper.getBlockState(plek).getValue(SausvatBlock.SAUS), "milk");
        gelijk(helper, 15, helper.getBlockState(plek).getAnalogOutputSignal(level, helper.absolutePos(plek), Direction.NORTH), "comparator: full");
        klik(helper, speler, plek, new ItemStack(Items.MILK_BUCKET));
        helper.assertTrue(speler.getMainHandItem().is(Items.MILK_BUCKET) && vat.tank().inhoud() == SausGetallen.VAT, "a full vat takes no more");
        // broken: the sauce goes with the item, and comes back when it is placed again
        vat.tank().zet(Sauzen.kaassaus(), 5500);
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(plek), level, helper.absolutePos(plek), vat);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(TechsausFeature.SAUSVAT_ITEM.get()), "the vat drops itself: " + drops);
        SimpleFluidContent inhoud = drops.get(0).getOrDefault(TechsausFeature.INHOUD.get(), SimpleFluidContent.EMPTY);
        helper.assertTrue(inhoud.getAmount() == 5500 && FluidResource.of(inhoud.copy()).equals(Sauzen.kaassaus()), "with 5,5 buckets of kaassaus on it");
        helper.setBlock(plek, Blocks.AIR);
        SausvatBlockEntity terug = zet(helper, p(4, 2), TechsausFeature.SAUSVAT.get(), SausvatBlockEntity.class);
        terug.applyComponentsFromItemStack(drops.get(0));
        helper.assertTrue(terug.tank().inhoud() == 5500 && terug.tank().saus().equals(Sauzen.kaassaus()), "placed again: the sauce is back");
        gelijk(helper, 2, helper.getBlockState(p(4, 2)).getValue(SausvatBlock.NIVEAU), "and the gauge shows it");
        ItemStack leeg = Block.getDrops(TechsausFeature.SAUSVAT.get().defaultBlockState(), level, helper.absolutePos(p(6, 2)), null).get(0);
        helper.assertTrue(!leeg.has(TechsausFeature.INHOUD.get()), "an empty vat is a plain item (it stacks)");
        // what you read on the item
        List<String> tip = drops.get(0).getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(level), null, net.minecraft.world.item.TooltipFlag.NORMAL)
                .stream().map(NlTekst::tekst).toList();
        helper.assertTrue(tip.contains("Inhoud: 5,5 emmers kaassaus") && tip.stream().anyMatch(r -> r.startsWith("Bewaart 16 emmers")), "the tooltip: " + tip);
        // the op command of the AutoCheck script
        BlockPos abs = helper.absolutePos(p(4, 2));
        var bron = level.getServer().createCommandSourceStack().withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(bron, "guhs techsaus vul " + abs.getX() + " " + abs.getY() + " " + abs.getZ() + " water 3000");
        helper.assertTrue(terug.tank().inhoud() == 3000 && terug.tank().saus().equals(Sauzen.water()), "/guhs techsaus vul sets the tank: " + terug.tank().inhoud());
        level.getServer().getCommands().performPrefixedCommand(bron, "guhs techsaus vul " + abs.getX() + " " + abs.getY() + " " + abs.getZ() + " leeg");
        helper.assertTrue(terug.tank().isLeeg(), "/guhs techsaus vul ... leeg empties it");
        level.getServer().getCommands().performPrefixedCommand(bron, "guhs techsaus stand " + abs.getX() + " " + abs.getY() + " " + abs.getZ());
        weg(helper, speler);
        helper.succeed();
    }

    // =====================================================================================================================
    // the Brouwautomaat
    // =====================================================================================================================

    /** Vat - hose - Brouwautomaat: kaassaus + an ingredient + three bottles become three Guhdrankjes, twice; by hand and through pipes. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 2 * SausGetallen.BROUW_TIKKEN + 400)
    public static void techsausBrouwautomaatBrouwtVanzelf(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SausvatBlockEntity vat = vat(helper, p(3, 2), Sauzen.kaassaus(), 2 * Sauzen.EMMER);
        slang(helper, p(4, 2));
        BrouwautomaatBlockEntity automaat = zet(helper, p(5, 2), TechsausFeature.BROUWAUTOMAAT.get(), BrouwautomaatBlockEntity.class);
        ServerPlayer speler = speler(helper);
        automaat.zetEigenaar(speler.getUUID());
        Brouwsel brouwsel = Brouwsel.forIngredient(new ItemStack(ModItems.KAAS_KNABBELS.get()));
        helper.assertTrue(brouwsel != null, "kaas knabbels are an ingredient");
        ItemStack drankje = brouwsel.drankje();
        // by hand: the ingredient and the bottles go in, anything else does not
        klik(helper, speler, p(5, 2), new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
        helper.assertTrue(speler.getMainHandItem().isEmpty() && automaat.vakken().getAmountAsInt(BrouwautomaatBlockEntity.INGREDIENT) == 2, "two ingredients in");
        klik(helper, speler, p(5, 2), new ItemStack(Items.COBBLESTONE, 3));
        helper.assertTrue(speler.getMainHandItem().getCount() == 3, "cobblestone is not for brewing");
        // through a pipe: only the right slot, only what belongs there
        ResourceHandler<ItemResource> pijp = Kisten.van(level, helper.absolutePos(p(5, 2)), Direction.UP);
        helper.assertTrue(pijp != null, "the machine has the item capability");
        gelijk(helper, 0, Kisten.stop(pijp, new ItemStack(Items.GLASS_BOTTLE, 6)).getCount(), "six bottles through a pipe");
        gelijk(helper, 6, automaat.vakken().getAmountAsInt(BrouwautomaatBlockEntity.FLESJES), "in the bottle slot");
        gelijk(helper, 5, Kisten.stop(pijp, new ItemStack(Items.DIRT, 5)).getCount(), "dirt is refused");
        helper.assertTrue(Kisten.neem(pijp, s -> true, 64).isEmpty(), "a pipe cannot take the ingredients back out");
        helper.startSequence()
                .thenIdle(30)
                .thenExecute(() -> {
                    helper.assertTrue(automaat.tank().isLeeg() && automaat.brouwsel() == null, "no vadskracht: it slurps nothing and brews nothing");
                    gelijk(helper, Snoet.SLAAPT, snoet(helper, p(5, 2)), "asleep");
                    kracht(helper, p(5, 1), 1);
                })
                .thenWaitUntil(() -> helper.assertTrue(automaat.brouwsel() == brouwsel, "it starts to brew"))
                .thenExecute(() -> {
                    gelijk(helper, 1, automaat.vakken().getAmountAsInt(BrouwautomaatBlockEntity.INGREDIENT), "one ingredient per pan");
                    gelijk(helper, 3, automaat.vakken().getAmountAsInt(BrouwautomaatBlockEntity.FLESJES), "three bottles per pan");
                    gelijk(helper, Snoet.WERKT, snoet(helper, p(5, 2)), "happy");
                    helper.assertTrue(heeftSleutel(VadsKracht.regels(level, helper.absolutePos(p(5, 2))), "gui.guhs.techsaus.brouwt"), "the readout says it bubbles");
                })
                .thenWaitUntil(() -> gelijk(helper, 3, tel(automaat, BrouwautomaatBlockEntity.UIT_VAN, BrouwautomaatBlockEntity.UIT_TOT, drankje), "the first three"))
                .thenWaitUntil(() -> gelijk(helper, 6, tel(automaat, BrouwautomaatBlockEntity.UIT_VAN, BrouwautomaatBlockEntity.UIT_TOT, drankje), "the second three"))
                .thenExecute(() -> {
                    gelijk(helper, 0, vat.tank().inhoud() + automaat.tank().inhoud(), "two pans used the two buckets of kaassaus");
                    helper.assertTrue(automaat.vakken().getAmountAsInt(BrouwautomaatBlockEntity.INGREDIENT) == 0
                            && automaat.vakken().getAmountAsInt(BrouwautomaatBlockEntity.FLESJES) == 0, "ingredients and bottles are used up");
                    helper.assertTrue(lees(helper, p(5, 2)).contains("Wacht op een ingrediënt"), "it says what it waits for: " + lees(helper, p(5, 2)));
                    helper.assertTrue(GidsFeature.heeft(speler, "techniek/tech_vloeistof_gebrouwen"), "the owner is rewarded");
                    // a pipe takes one, a hand takes the rest
                    ItemStack eentje = Kisten.neem(pijp, s -> true, 1);
                    helper.assertTrue(ItemStack.isSameItem(eentje, drankje), "a pipe takes a drankje out: " + eentje);
                    klik(helper, speler, p(5, 2), ItemStack.EMPTY);
                    gelijk(helper, 5, speler.getInventory().countItem(drankje.getItem()), "an empty hand takes the drankjes");
                    gelijk(helper, 0, tel(automaat, BrouwautomaatBlockEntity.UIT_VAN, BrouwautomaatBlockEntity.UIT_TOT, drankje), "the machine is empty");
                    weg(helper, speler);
                })
                .thenSucceed();
    }

    /** Full is full: with no room for three more drankjes it does not start, and it looks surprised. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void techsausBrouwautomaatVol(GameTestHelper helper) {
        BrouwautomaatBlockEntity automaat = zet(helper, p(5, 2), TechsausFeature.BROUWAUTOMAAT.get(), BrouwautomaatBlockEntity.class);
        kracht(helper, p(5, 1), 1);
        automaat.tank().zet(Sauzen.kaassaus(), SausGetallen.BROUW_TANK);
        automaat.vakken().set(BrouwautomaatBlockEntity.INGREDIENT, ItemResource.of(ModItems.KAAS_KNABBELS.get()), 4);
        automaat.vakken().set(BrouwautomaatBlockEntity.FLESJES, ItemResource.of(Items.GLASS_BOTTLE), 16);
        for (int vak = BrouwautomaatBlockEntity.UIT_VAN; vak < BrouwautomaatBlockEntity.UIT_TOT; vak++) {
            automaat.vakken().set(vak, ItemResource.of(Items.DIAMOND), 64);
        }
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(automaat.brouwsel() == null && automaat.tank().inhoud() == SausGetallen.BROUW_TANK, "no room: it does not start");
                    gelijk(helper, Snoet.VOL, snoet(helper, p(5, 2)), "surprised");
                    helper.assertTrue(lees(helper, p(5, 2)).contains("Wacht op iemand die hem leeghaalt: hij zit vol, njeg!"), "it says so: " + lees(helper, p(5, 2)));
                    automaat.vakken().set(BrouwautomaatBlockEntity.UIT_VAN, ItemResource.EMPTY, 0);
                })
                .thenWaitUntil(() -> helper.assertTrue(automaat.brouwsel() != null, "one slot free: it brews"))
                .thenExecute(() -> gelijk(helper, Snoet.WERKT, snoet(helper, p(5, 2)), "happy again"))
                .thenSucceed();
    }

    // =====================================================================================================================
    // the Frituurautomaat
    // =====================================================================================================================

    /** A bucket of frituursaus by hand, knabbels in, gefrituurde knabbels out, 10 mB each; then a fish through a pipe. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techsausFrituurautomaatFrituurt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FrituurautomaatBlockEntity automaat = zet(helper, p(3, 2), TechsausFeature.FRITUURAUTOMAAT.get(), FrituurautomaatBlockEntity.class);
        kracht(helper, p(3, 1), 1);
        ServerPlayer speler = speler(helper);
        klik(helper, speler, p(3, 2), new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(speler.getMainHandItem().is(Items.WATER_BUCKET) && automaat.tank().isLeeg(), "water does not fry");
        klik(helper, speler, p(3, 2), new ItemStack(BarbecuetherFeature.KAASFRITUURSAUS_BUCKET.get()));
        helper.assertTrue(speler.getMainHandItem().is(Items.BUCKET) && automaat.tank().inhoud() == Sauzen.EMMER, "a bucket of frituursaus by hand");
        klik(helper, speler, p(3, 2), new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
        helper.assertTrue(speler.getMainHandItem().isEmpty(), "five knabbels in");
        ResourceHandler<ItemResource> pijp = Kisten.van(level, helper.absolutePos(p(3, 2)), Direction.DOWN);
        ItemStack gefrituurd = new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get()), vis = new ItemStack(ModItems.GEBAKKEN_GUH_VIS.get());
        helper.startSequence()
                .thenWaitUntil(() -> gelijk(helper, 5, tel(automaat, FrituurautomaatBlockEntity.UIT_VAN, FrituurautomaatBlockEntity.UIT_TOT, gefrituurd), "five fried"))
                .thenExecute(() -> {
                    gelijk(helper, Sauzen.EMMER - 5 * SausGetallen.FRITUUR_SAUS, automaat.tank().inhoud(), "10 mB of sauce per snack");
                    gelijk(helper, 0, Kisten.stop(pijp, new ItemStack(ModItems.GUH_VIS.get(), 2)).getCount(), "two fish through a pipe");
                    gelijk(helper, 3, Kisten.stop(pijp, new ItemStack(Items.COD, 3)).getCount(), "a cod is not ours to fry");
                })
                .thenWaitUntil(() -> gelijk(helper, 2, tel(automaat, FrituurautomaatBlockEntity.UIT_VAN, FrituurautomaatBlockEntity.UIT_TOT, vis), "two fish fried"))
                .thenIdle(12)
                .thenExecute(() -> {
                    gelijk(helper, 5, tel(automaat, FrituurautomaatBlockEntity.UIT_VAN, FrituurautomaatBlockEntity.UIT_TOT, gefrituurd), "the knabbels are still there");
                    helper.assertTrue(lees(helper, p(3, 2)).contains("Wacht op kaasknabbels of een guhvis om te frituren"), "it waits for snacks: " + lees(helper, p(3, 2)));
                    klik(helper, speler, p(3, 2), ItemStack.EMPTY);
                    helper.assertTrue(speler.getInventory().countItem(gefrituurd.getItem()) == 5 && speler.getInventory().countItem(vis.getItem()) == 2,
                            "an empty hand takes both kinds");
                    helper.assertTrue(GidsFeature.heeft(speler, "techniek/tech_vloeistof_gefrituurd"), "rewarded");
                    weg(helper, speler);
                })
                .thenSucceed();
    }

    // =====================================================================================================================
    // the Grillkoolpers
    // =====================================================================================================================

    /** ONE hose brings frituursaus and water from two vats; a bucket of each becomes a block of grillkool; two blocks high. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 3 * SausGetallen.PERS_TIKKEN + 400)
    public static void techsausGrillkoolpersPerst(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SausvatBlockEntity water = vat(helper, p(3, 3), Sauzen.water(), 2 * Sauzen.EMMER);
        SausvatBlockEntity saus = vat(helper, p(4, 4), Sauzen.frituursaus(), 2 * Sauzen.EMMER);
        slang(helper, p(4, 3));
        GrillkoolpersBlockEntity pers = zet(helper, p(5, 3), TechsausFeature.GRILLKOOLPERS.get(), GrillkoolpersBlockEntity.class);
        kracht(helper, p(5, 2), 1);
        BlockPos boven = p(5, 3).above();
        helper.assertTrue(helper.getBlockState(boven).is(VadskrachtFeature.MACHINE_DEEL.get()), "the press is two blocks high: " + helper.getBlockState(boven));
        ResourceHandler<FluidResource> buiten = Sauzen.van(level, helper.absolutePos(p(5, 3)), Direction.WEST);
        helper.assertTrue(buiten != null && Sauzen.van(level, helper.absolutePos(boven), Direction.UP) == buiten, "its upper block gives the same tanks");
        gelijk(helper, 300, Sauzen.stop(buiten, Sauzen.water(), 300, false), "a hose may push water in");
        gelijk(helper, 300, pers.water().inhoud(), "into the water tank");
        gelijk(helper, 0, Sauzen.stop(buiten, Sauzen.melk(), 300, false), "but no milk");
        gelijk(helper, 0, Sauzen.neem(buiten, Sauzen.water(), 300, false), "and nobody drinks from a machine");
        pers.water().zet(FluidResource.EMPTY, 0);
        boolean[] gezien = new boolean[1];
        helper.onEachTick(() -> gezien[0] |= helper.getBlockState(p(5, 3)).getValue(GrillkoolpersBlock.PERST));
        ItemStack grillkool = new ItemStack(BarbecuetherFeature.GRILLKOOL_ITEM.get());
        helper.startSequence()
                .thenWaitUntil(() -> gelijk(helper, 1, tel(pers, 0, 1, grillkool), "the first block of grillkool"))
                .thenExecute(() -> {
                    helper.assertTrue(gezien[0], "the stamp came down while it pressed");
                    gelijk(helper, 2 * Sauzen.EMMER - SausGetallen.PERS_WATER, water.tank().inhoud() + pers.water().inhoud(), "a bucket of water used");
                    gelijk(helper, 2 * Sauzen.EMMER - SausGetallen.PERS_SAUS, saus.tank().inhoud() + pers.saus().inhoud(), "a bucket of frituursaus used");
                    helper.assertTrue(water.tank().saus().isEmpty() || water.tank().saus().equals(Sauzen.water()), "the water vat holds only water");
                })
                .thenWaitUntil(() -> gelijk(helper, 2, tel(pers, 0, 1, grillkool), "the second block"))
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(water.tank().isLeeg() && saus.tank().isLeeg() && pers.water().isLeeg() && pers.saus().isLeeg(), "everything is used up");
                    helper.assertFalse(helper.getBlockState(p(5, 3)).getValue(GrillkoolpersBlock.PERST), "the stamp is up again");
                    helper.assertTrue(lees(helper, p(5, 3)).contains("Wacht op kaasfrituursaus"), "it waits for sauce: " + lees(helper, p(5, 3)));
                    helper.assertTrue(lees(helper, boven).equals(lees(helper, p(5, 3))), "looking at the upper block reads the same");
                    ItemStack eruit = Kisten.neem(Kisten.van(level, helper.absolutePos(boven), Direction.UP), s -> true, 64);
                    helper.assertTrue(eruit.is(BarbecuetherFeature.GRILLKOOL_ITEM.get()) && eruit.getCount() == 2, "a pipe on the upper block takes the grillkool: " + eruit);
                    // breaking the upper block takes the whole press away
                    level.destroyBlock(helper.absolutePos(boven), false);
                    helper.assertTrue(helper.getBlockState(p(5, 3)).isAir(), "the kern goes with its upper block");
                })
                .thenSucceed();
    }

    // =====================================================================================================================
    // one net, too heavy
    // =====================================================================================================================

    /** Pump + Brouwautomaat + Frituurautomaat + Grillkoolpers ask 30 vadskracht: with 20 everything stands still, with 30 all run. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void techsausTeZwaarStaatAllesStil(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        bron(helper, 2, 3, Blocks.WATER);
        kracht(helper, p(1, 2), 2);
        for (int x = 2; x <= 8; x++) {
            helper.setBlock(p(x, 2), ModBlocks.GUH_WIRE.get());
        }
        SauspompBlockEntity pomp = zet(helper, p(2, 3), TechsausFeature.SAUSPOMP.get(), SauspompBlockEntity.class);
        zet(helper, p(4, 3), TechsausFeature.BROUWAUTOMAAT.get(), BrouwautomaatBlockEntity.class);
        FrituurautomaatBlockEntity frituur = zet(helper, p(6, 3), TechsausFeature.FRITUURAUTOMAAT.get(), FrituurautomaatBlockEntity.class);
        zet(helper, p(8, 3), TechsausFeature.GRILLKOOLPERS.get(), GrillkoolpersBlockEntity.class);
        frituur.tank().zet(Sauzen.frituursaus(), Sauzen.EMMER);
        frituur.vakken().set(FrituurautomaatBlockEntity.IN, ItemResource.of(ModItems.KAAS_KNABBELS.get()), 3);
        int vraag = SausGetallen.POMP + SausGetallen.BROUWAUTOMAAT + SausGetallen.FRITUURAUTOMAAT + SausGetallen.GRILLKOOLPERS;
        VadsNet net = VadsKracht.net(level, helper.absolutePos(p(4, 3)));
        gelijk(helper, vraag, net.vraag(), "what the four ask together");
        gelijk(helper, VadsNet.Status.TE_ZWAAR, net.status(), "20 is not enough");
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(pomp.tank().isLeeg() && frituur.vakken().getAmountAsInt(FrituurautomaatBlockEntity.IN) == 3, "everything stands still");
                    for (int x = 2; x <= 8; x += 2) {
                        gelijk(helper, Snoet.SLAAPT, snoet(helper, p(x, 3)), "asleep at x " + x);
                    }
                    List<String> regels = lees(helper, p(6, 3));
                    helper.assertTrue(regels.get(0).equals("Deze opstelling gebruikt " + vraag + "/20 vadskracht") && regels.contains("Dit gebruikt "
                            + SausGetallen.FRITUURAUTOMAAT + " vadskracht") && regels.contains("1,0 van de 4,0 emmers kaasfrituursaus"), "the readout: " + regels);
                    kracht(helper, p(1, 2), 3);
                })
                .thenWaitUntil(() -> helper.assertTrue(pomp.tank().inhoud() > 0 && frituur.vakken().getAmountAsInt(FrituurautomaatBlockEntity.IN) == 0, "with 30 all run"))
                .thenExecute(() -> gelijk(helper, VadsNet.Status.DRAAIT, VadsKracht.net(level, helper.absolutePos(p(4, 3))).status(), "the net runs"))
                .thenSucceed();
    }

    // =====================================================================================================================
    // by hand it still works, and the datapack
    // =====================================================================================================================

    /** The Guhbrouwketel and the frying pan you work by hand are what they were; the recipes and the numbers are there. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void techsausMetDeHandWerktHetNog(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer speler = speler(helper);
        GuhbrouwketelBlockEntity ketel = zet(helper, p(2, 2), SpiesburchtFeature.GUHBROUWKETEL.get(), GuhbrouwketelBlockEntity.class);
        speler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_SAUS_BUCKET.get()));
        helper.assertTrue(ketel.use(speler, InteractionHand.MAIN_HAND) && ketel.portions() == GuhbrouwketelBlockEntity.PORTIONS, "the Guhbrouwketel takes a bucket by hand");
        helper.assertTrue(Sauzen.van(level, helper.absolutePos(p(2, 2)), Direction.UP) == null, "and it is not a tank for hoses");
        FryingPanBlockEntity pan = zet(helper, p(4, 2), ModBlocks.FRYING_PAN.get(), FryingPanBlockEntity.class);
        klik(helper, speler, p(4, 2), new ItemStack(ModItems.MIKA_VET.get()));
        klik(helper, speler, p(4, 2), new ItemStack(ModItems.KAAS_KNABBELS.get(), 4));
        helper.assertTrue(speler.getInventory().countItem(ModItems.GEFRITUURDE_KAASKNABBELS.get()) == 4 && pan.getCharges() == 60, "the frying pan fries on Mika's vet");
        // what the automaat fries is what the pan fries
        helper.assertTrue(Frituur.resultaat(new ItemStack(ModItems.KAAS_KNABBELS.get())).is(ModItems.GEFRITUURDE_KAASKNABBELS.get())
                && Frituur.resultaat(new ItemStack(ModItems.GUH_VIS.get())).is(ModItems.GEBAKKEN_GUH_VIS.get())
                && Frituur.resultaat(new ItemStack(Items.COD)).isEmpty(), "Frituur.resultaat");
        // the recipes (the Saus tier), and the pump only lifts what the tag allows
        for (String id : List.of("sauspomp", "sausslang", "sausvat", "brouwautomaat", "frituurautomaat", "grillkoolpers")) {
            helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(id))).isPresent(), "recipe guhs:" + id);
        }
        helper.setBlock(new BlockPos(7, 2, 2), Blocks.LAVA);
        SauspompBlockEntity pomp = zet(helper, p(7, 2), TechsausFeature.SAUSPOMP.get(), SauspompBlockEntity.class);
        helper.assertTrue(pomp.bron() == null && !Sauzen.isTechniek(FluidResource.of(Fluids.LAVA)), "a pump does not lift lava");
        helper.setBlock(new BlockPos(7, 2, 2), Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 3));
        helper.assertTrue(pomp.bron() == null, "nor water that only flows by: it wants a source");
        helper.setBlock(new BlockPos(7, 2, 2), Blocks.STONE);
        weg(helper, speler);
        helper.succeed();
    }
}
