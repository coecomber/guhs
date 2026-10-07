package nl.juiced.guhs.feature.techklus;

import java.util.List;
import java.util.UUID;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.HapluikjeBlockEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.guhoven.GuhOvenBlockEntity;
import nl.juiced.guhs.feature.guhoven.GuhovenFeature;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeOverzicht;
import nl.juiced.guhs.feature.huisje.HuisjePayloads;
import nl.juiced.guhs.feature.huisje.Klus;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.klusjes.FarmenKlus;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.KlusjesGameTests;
import nl.juiced.guhs.feature.klusjes.Voorraad;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;
import nl.juiced.guhs.feature.techmachine.Bouwtekeningen;
import nl.juiced.guhs.feature.techmachine.KnutselmachineBlockEntity;
import nl.juiced.guhs.feature.techmachine.OogsterBlockEntity;
import nl.juiced.guhs.feature.techmachine.PlantagebakBlockEntity;
import nl.juiced.guhs.feature.techmachine.TechmachineFeature;
import nl.juiced.guhs.feature.techsaus.BrouwautomaatBlockEntity;
import nl.juiced.guhs.feature.techsaus.TechsausFeature;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.BankStorage;

/**
 * Game tests of the chore slice (bbq2), each with a real Guhhuisje and a real resident (template techklus_test_tuin:
 * 24 x 24 grass, 20 high; the huisje in the middle, its chest right next to it; the helpers of {@link KlusjesGameTests}):
 * <ul>
 *   <li>machines: a Guh Oven that was shown raw iron is filled from the chest and emptied into it (the logs in the chest are
 *       never baked), a stranger's machine is left alone; a Knutselmachine gets exactly its drawing's ingredients and its
 *       results are fetched; a machine remembers per slot what it was shown, also when the slot is empty again, and the
 *       look of once a second learns without anybody asking;</li>
 *   <li>farmen: a pumpkin on its stem (not the one somebody put down), sugar cane down to its bottom piece, cocoa within
 *       reach (not the pod high up), nether wart, and one scheutje per scheutjesplant, which keeps standing and rests;</li>
 *   <li>plantage: saplings from the chest into an empty Plantagebak, the tree chopped and the wood in the chest, the bak
 *       plants the next one itself; a stranger's bak is left alone;</li>
 *   <li>the Hapluikje: with no Bank Guh in the klus-area chore output goes through a luikje to its bank above the area, the
 *       rest (the bank's cap) into the chest; with a Bank Guh in the area that one goes first; the huisje screen and the
 *       overview know about the luikje;</li>
 *   <li>the overview "Wat kan hier?": both chores are listed with what there is to do.</li>
 * </ul>
 * Every test in its own batch (a home base is bigger than a test plot).
 */
public class TechklusGameTests {
    private static final String TUIN = "techklus_test_tuin";

    private static void bron(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 5));
    }

    private static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    private static boolean behaald(ServerPlayer p, String id) {
        AdvancementHolder holder = p.level().getServer().getAdvancements().get(Guhs.id(id));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static String stand(GameTestHelper helper, Huisje h, String klus) {
        KlusGebied.vergeet();
        KlusStand s = Klusjes.van(klus).stand(helper.getLevel(), h);
        return s.staat() + ":" + s.reden() + ":" + s.aantal();
    }

    private static CompoundTag rij(CompoundTag data, String lijst, String id) {
        ListTag l = data.getListOrEmpty(lijst);
        for (int i = 0; i < l.size(); i++) {
            if (l.getCompoundOrEmpty(i).getStringOr("Id", "").equals(id)) {
                return l.getCompoundOrEmpty(i);
            }
        }
        throw new IllegalStateException("no row " + id + " in " + lijst);
    }

    private static List<String> wil(ServerLevel level, BlockPos pos) {
        return Klusmachines.wensen(level, pos).stream().map(w -> w.voorbeeld().getItem().toString()).toList();
    }

    // =====================================================================================================================
    // machines
    // =====================================================================================================================

    @GuhTest(template = TUIN, batch = "techklus_oven", timeoutTicks = 1600)
    public static void techklusOvenBijvullenEnLeeghalen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = KlusjesGameTests.kist(helper);
        kist.setItem(0, new ItemStack(Items.RAW_IRON, 32));
        kist.setItem(1, new ItemStack(Items.OAK_LOG, 16));
        BlockPos ovenPlek = new BlockPos(16, 2, 15), ovenAbs = helper.absolutePos(ovenPlek);
        helper.setBlock(ovenPlek, GuhovenFeature.GUH_OVEN.get());
        bron(helper, ovenPlek.east());
        GuhOvenBlockEntity oven = (GuhOvenBlockEntity) level.getBlockEntity(ovenAbs);
        oven.setItem(0, new ItemStack(Items.RAW_IRON, 4));     // (shown once, by hand)
        oven.setItem(2, new ItemStack(Items.IRON_INGOT, 5));   // (ready)
        // somebody else's Oogster with harvest in it, in the same klus-area
        BlockPos vreemdPlek = new BlockPos(6, 2, 15), vreemdAbs = helper.absolutePos(vreemdPlek);
        helper.setBlock(vreemdPlek, TechmachineFeature.OOGSTER.get());
        OogsterBlockEntity vreemd = (OogsterBlockEntity) level.getBlockEntity(vreemdAbs);
        vreemd.zetEigenaar(UUID.randomUUID());
        vreemd.vakken().set(0, ItemResource.of(Items.WHEAT), 7);
        Huisje h = KlusjesGameTests.huisje(helper, p);
        helper.assertTrue(Klusmachines.mag(level, h, ovenAbs), "the oven is served");
        helper.assertTrue(!Klusmachines.mag(level, h, vreemdAbs) && Klusmachines.heeftUitvoer(level, vreemdAbs), "a stranger's machine is not");
        gelijk(helper, List.of("minecraft:raw_iron"), wil(level, ovenAbs), "the oven was shown raw iron");
        helper.assertTrue(Klusmachines.heeftUitvoer(level, ovenAbs), "bars lie ready");
        gelijk(helper, 1, Klusmachines.teVullen(level, h, ovenAbs).size(), "and the chest has what it wants");
        gelijk(helper, "JA:halen:1", stand(helper, h, "machines"), "the overview");
        // a chest right next to a machine is a chest; a machine right next to the huisje is not
        helper.assertTrue(Voorraad.kisten(level, h).stream().noneMatch(q -> q.equals(ovenAbs)), "a machine is never one of the huisje's chests");
        GuhEntity guh = KlusjesGameTests.bewoner(helper, h, p, new BlockPos(12, 2, 14), "machines");
        UUID id = Band.id(guh);
        helper.succeedWhen(() -> {
            String waar = KlusjesGameTests.staat(helper, guh);
            int staven = KlusjesGameTests.telKist(kist, s -> s.is(Items.IRON_INGOT));
            int ruw = oven.getItem(0).is(Items.RAW_IRON) ? oven.getItem(0).getCount() : 0;
            int klaar = oven.getItem(2).is(Items.IRON_INGOT) ? oven.getItem(2).getCount() : 0;
            helper.assertTrue(staven >= 6, "the bars that lay ready and at least one it baked since are in the chest: " + staven + waar);
            gelijk(helper, 0, KlusjesGameTests.telKist(kist, s -> s.is(Items.RAW_IRON)), "the raw iron went to the oven" + waar);
            gelijk(helper, 41, staven + ruw + klaar, "nothing lost, nothing made up (chest + oven)" + waar);
            gelijk(helper, 16, KlusjesGameTests.telKist(kist, s -> s.is(Items.OAK_LOG)), "the logs were never shown to the oven: they stay");
            helper.assertTrue(oven.getItem(0).isEmpty() || oven.getItem(0).is(Items.RAW_IRON), "only raw iron in the oven");
            gelijk(helper, 7, vreemd.vakken().getAmountAsInt(0), "the stranger's harvest was not touched");
            helper.assertTrue(Dagboek.heeftEersteKeer(level.getServer(), p.getUUID(), id, "klusjes_machines"), "the first time is in its dagboek");
            helper.assertTrue(behaald(p, "quest/tech_klusjes_machines") && behaald(p, "techniek/tech_klusjes_machines"), "the owner's advancements");
            KlusjesGameTests.weg(helper, h, p);
        });
    }

    @GuhTest(template = TUIN, batch = "techklus_knutsel", timeoutTicks = 1600)
    public static void techklusKnutselmachineKrijgtZijnTekening(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = KlusjesGameTests.kist(helper);
        kist.setItem(0, new ItemStack(Items.OAK_PLANKS, 40));
        kist.setItem(1, new ItemStack(Items.BIRCH_PLANKS, 8));
        BlockPos plek = new BlockPos(16, 2, 15), abs = helper.absolutePos(plek);
        helper.setBlock(plek, TechmachineFeature.KNUTSELMACHINE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        bron(helper, plek.east());
        KnutselmachineBlockEntity machine = (KnutselmachineBlockEntity) level.getBlockEntity(abs);
        machine.zetEigenaar(p.getUUID());
        Huisje h = KlusjesGameTests.huisje(helper, p);
        gelijk(helper, List.of(), wil(level, abs), "no drawing: it wants nothing");
        gelijk(helper, "STRAKS:rustig:1", stand(helper, h, "machines"), "nothing to do without a drawing");
        // a chest: eight oak planks in a ring
        ItemStack plank = new ItemStack(Items.OAK_PLANKS);
        ItemStack tekening = Bouwtekeningen.teken(level, List.of(plank, plank, plank, plank, ItemStack.EMPTY, plank, plank, plank, plank));
        helper.assertTrue(Bouwtekeningen.lees(tekening) != null, "a drawing of a chest");
        machine.vakken().set(KnutselmachineBlockEntity.TEKENING, ItemResource.of(tekening), 1);
        gelijk(helper, List.of("minecraft:oak_planks"), wil(level, abs), "it wants what the drawing shows");
        gelijk(helper, "JA:vullen:1", stand(helper, h, "machines"), "the chest has planks for it");
        GuhEntity guh = KlusjesGameTests.bewoner(helper, h, p, new BlockPos(12, 2, 14), "machines");
        helper.succeedWhen(() -> {
            String waar = KlusjesGameTests.staat(helper, guh);
            gelijk(helper, 5, KlusjesGameTests.telKist(kist, s -> s.is(Items.CHEST)), "forty planks are five chests, fetched and in the huisje's chest" + waar);
            gelijk(helper, 0, KlusjesGameTests.telKist(kist, s -> s.is(Items.OAK_PLANKS)) + machine.inVoorraad(plank), "every oak plank was used");
            gelijk(helper, 8, KlusjesGameTests.telKist(kist, s -> s.is(Items.BIRCH_PLANKS)), "birch is not on the drawing: it stays in the chest");
            helper.assertTrue(machine.tekening() != null, "the drawing stays in the machine");
            gelijk(helper, "NEE:geen_voorraad:1", stand(helper, h, "machines"), "now the chest has no planks left");
            KlusjesGameTests.weg(helper, h, p);
        });
    }

    /** Per slot the kind that was shown; an empty slot keeps what it knew; the look of once a second learns by itself. */
    @GuhTest(template = TUIN, batch = "techklus_onthoud", timeoutTicks = 400)
    public static void techklusMachineOnthoudtPerVakje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = KlusjesGameTests.kist(helper);
        BlockPos plek = new BlockPos(16, 2, 15), abs = helper.absolutePos(plek);
        // (no vadskracht: it brews nothing, so the test decides what lies in it)
        helper.setBlock(plek, TechsausFeature.BROUWAUTOMAAT.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        BrouwautomaatBlockEntity automaat = (BrouwautomaatBlockEntity) level.getBlockEntity(abs);
        automaat.zetEigenaar(p.getUUID());
        ItemStack knabbels = new ItemStack(ModItems.KAAS_KNABBELS.get());
        helper.assertTrue(Brouwsel.forIngredient(knabbels) != null, "kaasknabbels are an ingredient");
        Huisje h = KlusjesGameTests.huisje(helper, p);
        kist.setItem(0, new ItemStack(ModItems.KAAS_KNABBELS.get(), 40));
        kist.setItem(1, new ItemStack(Items.GLASS_BOTTLE, 20));
        gelijk(helper, List.of(), wil(level, abs), "nothing shown yet: it wants nothing, whatever the chest holds");
        gelijk(helper, 0, Klusmachines.teVullen(level, h, abs).size(), "so nothing is brought");
        gelijk(helper, "STRAKS:rustig:1", stand(helper, h, "machines"), "the overview");
        automaat.vakken().set(BrouwautomaatBlockEntity.INGREDIENT, ItemResource.of(knabbels), 2);
        gelijk(helper, List.of("guhs:kaas_knabbels"), wil(level, abs), "shown kaasknabbels");
        automaat.vakken().set(BrouwautomaatBlockEntity.FLESJES, ItemResource.of(Items.GLASS_BOTTLE), 60);
        gelijk(helper, List.of("guhs:kaas_knabbels", "minecraft:glass_bottle"), wil(level, abs), "and bottles: a kind per slot");
        // the ingredient slot is used up: it still knows; the bottle slot is nearly full: not worth a walk
        automaat.vakken().set(BrouwautomaatBlockEntity.INGREDIENT, ItemResource.EMPTY, 0);
        gelijk(helper, List.of("guhs:kaas_knabbels", "minecraft:glass_bottle"), wil(level, abs), "an empty slot keeps what it knew");
        List<Klusmachines.Vraag> vragen = Klusmachines.teVullen(level, h, abs);
        helper.assertTrue(vragen.size() == 1 && vragen.get(0).wens().past(knabbels) && vragen.get(0).aantal() == 40,
                "only the knabbels are brought (four bottles of room is no reason to walk): " + vragen);
        automaat.vakken().set(BrouwautomaatBlockEntity.FLESJES, ItemResource.of(Items.GLASS_BOTTLE), 30);
        gelijk(helper, 2, Klusmachines.teVullen(level, h, abs).size(), "a half empty slot is topped up");
        // another kind in the slot = that kind from now on
        ItemStack gruis = new ItemStack(BarbecuetherFeature.GLOEIKOOLGRUIS.get());
        helper.assertTrue(Brouwsel.forIngredient(gruis) != null, "gloeikoolgruis is an ingredient too");
        automaat.vakken().set(BrouwautomaatBlockEntity.INGREDIENT, ItemResource.of(gruis), 1);
        gelijk(helper, List.of("guhs:gloeikoolgruis", "minecraft:glass_bottle"), wil(level, abs), "shown something else");
        Klusmachines.vergeet(level, abs);
        automaat.vakken().set(BrouwautomaatBlockEntity.INGREDIENT, ItemResource.EMPTY, 0);
        automaat.vakken().set(BrouwautomaatBlockEntity.FLESJES, ItemResource.EMPTY, 0);
        gelijk(helper, List.of(), wil(level, abs), "forgotten");
        // the look of once a second (only around a huisje with residents): nobody asks, it learns all the same
        GuhEntity guh = helper.spawn(nl.juiced.guhs.registry.ModEntities.GUH.get(), new BlockPos(12, 2, 14));
        guh.tame(p);
        KlusjesGameTests.alleen(helper, h, guh, "geen");
        automaat.vakken().set(BrouwautomaatBlockEntity.FLESJES, ItemResource.of(Items.GLASS_BOTTLE), 3);
        helper.succeedWhen(() -> {
            helper.assertTrue(automaat.getPersistentData().getCompoundOrEmpty(Klusmachines.WENS).getStringOr("1", "").equals("minecraft:glass_bottle"),
                    "the machine learned its bottles by itself: " + automaat.getPersistentData());
            KlusjesGameTests.weg(helper, h, p);
        });
    }

    // =====================================================================================================================
    // farmen: more harvesting
    // =====================================================================================================================

    @GuhTest(template = TUIN, batch = "techklus_oogst", timeoutTicks = 2400)
    public static void techklusFarmenOogstMeer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FarmenKlus.vergeet();
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = KlusjesGameTests.kist(helper);
        // a pumpkin on its stem, and one somebody put down
        BlockPos stengel = new BlockPos(6, 2, 16), pompoen = new BlockPos(7, 2, 16), los = new BlockPos(15, 2, 18);
        helper.setBlock(stengel.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        helper.setBlock(pompoen, Blocks.PUMPKIN);
        helper.setBlock(stengel, Blocks.ATTACHED_PUMPKIN_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING, Direction.EAST));
        helper.setBlock(los, Blocks.PUMPKIN);
        // sugar cane, three high, next to water
        BlockPos riet = new BlockPos(9, 2, 17);
        helper.setBlock(new BlockPos(9, 1, 18), Blocks.WATER);
        for (int i = 0; i < 3; i++) {
            helper.setBlock(riet.above(i), Blocks.SUGAR_CANE);
        }
        // cocoa: one pod within reach, one high up the trunk
        BlockPos stam = new BlockPos(17, 2, 15);
        for (int i = 0; i < 6; i++) {
            helper.setBlock(stam.above(i), Blocks.JUNGLE_LOG);
        }
        BlockState cacao = Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, Direction.WEST).setValue(CocoaBlock.AGE, CocoaBlock.MAX_AGE);
        BlockPos laag = stam.above(1).east(), hoog = stam.above(5).east();
        helper.setBlock(laag, cacao);
        helper.setBlock(hoog, cacao);
        // nether wart
        BlockPos wrat = new BlockPos(13, 2, 17);
        helper.setBlock(wrat.below(), Blocks.SOUL_SAND);
        helper.setBlock(wrat, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE));
        // two scheutjesplanten
        BlockPos[] scheutjes = {new BlockPos(5, 2, 13), new BlockPos(6, 2, 13)};
        helper.setBlock(scheutjes[0], BarbecuetherFeature.PINDASCHEUTJES.get());
        helper.setBlock(scheutjes[1], BarbecuetherFeature.MOSTERDSCHEUTJES.get());
        Huisje h = KlusjesGameTests.huisje(helper, p);
        helper.assertTrue(helper.getBlockState(stengel).getBlock() instanceof AttachedStemBlock && helper.getBlockState(laag).is(Blocks.COCOA)
                && helper.getBlockState(riet.above(2)).is(Blocks.SUGAR_CANE) && helper.getBlockState(scheutjes[0]).is(BarbecuetherFeature.PINDASCHEUTJES.get()),
                "the garden stands");
        helper.assertTrue(KlusGebied.rijp(level, helper.absolutePos(pompoen)) && !KlusGebied.rijp(level, helper.absolutePos(los)),
                "a pumpkin on a stem is a crop, one that was put down is not");
        helper.assertTrue(KlusGebied.rijp(level, helper.absolutePos(laag)) && !KlusGebied.rijp(level, helper.absolutePos(hoog)),
                "cocoa within reach is a crop, a pod high up the trunk is not");
        helper.assertTrue(!KlusGebied.rijp(level, helper.absolutePos(riet)) && KlusGebied.rijp(level, helper.absolutePos(riet.above())),
                "the bottom piece of sugar cane stays");
        // pumpkin, two cane pieces, the low pod, the wart, two scheutjes
        gelijk(helper, "JA:rijp:7", stand(helper, h, "farmen"), "the overview counts them");
        GuhEntity guh = KlusjesGameTests.bewoner(helper, h, p, new BlockPos(12, 2, 14), "farmen");
        helper.succeedWhen(() -> {
            String waar = KlusjesGameTests.staat(helper, guh);
            helper.assertBlockPresent(Blocks.AIR, pompoen);
            helper.assertTrue(helper.getBlockState(stengel).getBlock() instanceof StemBlock, "the stem stays (and grows the next one): " + helper.getBlockState(stengel));
            helper.assertBlockPresent(Blocks.PUMPKIN, los);
            helper.assertTrue(helper.getBlockState(riet).is(Blocks.SUGAR_CANE) && helper.getBlockState(riet.above()).isAir(), "cane cut down to its bottom piece" + waar);
            helper.assertTrue(helper.getBlockState(laag).is(Blocks.COCOA) && helper.getBlockState(laag).getValue(CocoaBlock.AGE) == 0, "the low pod starts again" + waar);
            helper.assertTrue(helper.getBlockState(hoog).getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE, "the high pod hangs where it hung");
            helper.assertTrue(helper.getBlockState(wrat).is(Blocks.NETHER_WART) && helper.getBlockState(wrat).getValue(NetherWartBlock.AGE) == 0, "the wart starts again" + waar);
            helper.assertBlockPresent(BarbecuetherFeature.PINDASCHEUTJES.get(), scheutjes[0]);
            helper.assertBlockPresent(BarbecuetherFeature.MOSTERDSCHEUTJES.get(), scheutjes[1]);
            gelijk(helper, 1, KlusjesGameTests.telKist(kist, s -> s.is(Items.PUMPKIN)), "the pumpkin is in the chest" + waar);
            gelijk(helper, 2, KlusjesGameTests.telKist(kist, s -> s.is(Items.SUGAR_CANE)), "two pieces of cane");
            helper.assertTrue(KlusjesGameTests.telKist(kist, s -> s.is(Items.COCOA_BEANS)) >= 1, "cocoa beans");
            helper.assertTrue(KlusjesGameTests.telKist(kist, s -> s.is(Items.NETHER_WART)) >= 1, "nether wart");
            gelijk(helper, 1, KlusjesGameTests.telKist(kist, s -> s.is(BarbecuetherFeature.PINDASCHEUTJES.get().asItem())), "one pindascheutje: the plant rests");
            gelijk(helper, 1, KlusjesGameTests.telKist(kist, s -> s.is(BarbecuetherFeature.MOSTERDSCHEUTJES.get().asItem())), "one mosterdscheutje");
            helper.assertTrue(!FarmenKlus.scheutRijp(level, helper.absolutePos(scheutjes[0])), "a picked plant rests");
            // stem, cane, two pods, wart, two resting scheutjes: all there, nothing ripe
            gelijk(helper, "STRAKS:groeit:7", stand(helper, h, "farmen"), "the overview afterwards");
            helper.assertTrue(behaald(p, "techniek/tech_klusjes_oogst") && behaald(p, "quest/tech_klusjes_oogst"), "the owner's advancements");
            KlusjesGameTests.weg(helper, h, p);
        });
    }

    // =====================================================================================================================
    // plantage
    // =====================================================================================================================

    @GuhTest(template = TUIN, batch = "techklus_plantage", timeoutTicks = 3600, skyAccess = true)
    public static void techklusPlantagePlantenEnHakken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = KlusjesGameTests.kist(helper);
        kist.setItem(0, new ItemStack(Items.OAK_SAPLING, 6));
        // the bak: kern at the front (north) edge, the bed runs south; a source in front of its corner
        BlockPos kern = new BlockPos(17, 2, 15);
        helper.setBlock(kern, TechmachineFeature.PLANTAGEBAK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        bron(helper, kern.north().west());
        PlantagebakBlockEntity bak = (PlantagebakBlockEntity) level.getBlockEntity(helper.absolutePos(kern));
        bak.zetEigenaar(p.getUUID());
        // a stranger's bak with a tree-to-be: never touched
        BlockPos vreemdKern = new BlockPos(5, 2, 15);
        helper.setBlock(vreemdKern, TechmachineFeature.PLANTAGEBAK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        PlantagebakBlockEntity vreemd = (PlantagebakBlockEntity) level.getBlockEntity(helper.absolutePos(vreemdKern));
        vreemd.zetEigenaar(UUID.randomUUID());
        Huisje h = KlusjesGameTests.huisje(helper, p);
        gelijk(helper, 1, PlantageKlus.bakken(level, h).size(), "one bak of the owner's");
        gelijk(helper, "JA:planten:1", stand(helper, h, "plantage"), "an empty bak and saplings in the chest");
        GuhEntity guh = KlusjesGameTests.bewoner(helper, h, p, new BlockPos(12, 2, 14), "plantage");
        boolean[] gegroeid = new boolean[1];
        int[] gehakt = new int[1];
        helper.onEachTick(() -> {
            // (the tree takes its 45 seconds of work: the test server runs them in a blink)
            if (bak.heeftBoom()) {
                gegroeid[0] = true;
            } else if (gegroeid[0] && gehakt[0] == 0) {
                gehakt[0] = 1;
            }
        });
        helper.succeedWhen(() -> {
            String waar = KlusjesGameTests.staat(helper, guh);
            helper.assertTrue(gegroeid[0], "the resident brought saplings and a tree grew: " + bak.stand() + ", " + bak.voorraad() + waar);
            helper.assertTrue(gehakt[0] > 0, "and the resident chopped it" + waar);
            int hout = KlusjesGameTests.telKist(kist, s -> s.is(ItemTags.LOGS));
            helper.assertTrue(hout >= 4, "the wood is in the chest: " + hout + waar);
            // four went to the bak; the two that stayed plus the spare saplings of the chop are somewhere, never lost
            helper.assertTrue(KlusjesGameTests.telKist(kist, s -> s.is(Items.OAK_SAPLING)) >= 2, "the chest kept the saplings the bak did not need");
            helper.assertTrue(bak.stand() != PlantagebakBlockEntity.Stand.LEEG || !bak.voorraad().isEmpty(), "the bak goes on by itself: " + bak.stand());
            helper.assertTrue(vreemd.stand() == PlantagebakBlockEntity.Stand.LEEG && vreemd.voorraad().isEmpty(), "the stranger's bak got nothing");
            helper.assertTrue(Dagboek.heeftEersteKeer(level.getServer(), p.getUUID(), Band.id(guh), "klusjes_plantage"), "the first time is in its dagboek");
            helper.assertTrue(behaald(p, "techniek/tech_klusjes_plantage"), "the owner's advancement");
            for (BlockPos q : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(23, 1, 23))) {
                helper.assertTrue(!helper.getBlockState(q).isAir(), "the garden's ground is whole at " + q);
            }
            KlusjesGameTests.weg(helper, h, p);
        });
    }

    // =====================================================================================================================
    // the Hapluikje
    // =====================================================================================================================

    // (the test waits for a SECOND kaasknabbel to be dug up, and the loot of opgraven is random: 69 in 100 digs give one.
    // With 1600 ticks the resident dug two or three times and the test failed about once in eight runs: plenty of time now)
    @GuhTest(template = TUIN, batch = "techklus_luikje", timeoutTicks = 4800)
    public static void techklusBuitDoorHetHapluikje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = KlusjesGameTests.kist(helper);
        // the Bank Guh stands ABOVE the klus-area (more than 8 blocks over the huisje), the luikje inside it
        BlockPos bankPlek = new BlockPos(3, 13, 3);
        helper.setBlock(bankPlek.below(), Blocks.STONE);
        helper.setBlock(bankPlek, ModBlocks.BANK_GUH.get());
        BankGuhBlockEntity bank = (BankGuhBlockEntity) level.getBlockEntity(helper.absolutePos(bankPlek));
        bank.meld();
        BlockPos luikjePlek = new BlockPos(16, 2, 14);
        helper.setBlock(luikjePlek, BankFeature.HAPLUIKJE.get());
        HapluikjeBlockEntity luikje = (HapluikjeBlockEntity) level.getBlockEntity(helper.absolutePos(luikjePlek));
        Huisje h = KlusjesGameTests.huisje(helper, p);
        helper.assertTrue(!h.inGebied(helper.absolutePos(bankPlek)) && h.inGebied(helper.absolutePos(luikjePlek)), "the bank is outside the klus-area, the luikje inside");
        // asleep and not linked: it does not count
        gelijk(helper, 0, Voorraad.luikjes(level, h).size(), "a luikje without vadskracht is no storage");
        gelijk(helper, helper.absolutePos(KlusjesGameTests.KIST), Voorraad.afleverPlek(level, h), "so the chest it is");
        helper.assertTrue(!HuisjePayloads.data(p, h).getBooleanOr("Luikje", true), "the huisje screen knows of no luikje");
        gelijk(helper, 0, rij(HuisjeOverzicht.data(level, h), "Dingen", "hapluikje").getIntOr("Aantal", -1), "the overview lists it with 0");
        bron(helper, luikjePlek.east());
        // the resident moves in now, with every chore off (onEachTick may not be called from a delayed callback); it starts digging later
        GuhEntity guh = KlusjesGameTests.bewoner(helper, h, p, new BlockPos(12, 2, 14), "geen");
        luikje.koppel(bank.bankId());
        // the bank is nearly full of kaasknabbels: only one more fits, the rest of what is dug up must go into the chest
        ItemStack knabbels = new ItemStack(ModItems.KAAS_KNABBELS.get());
        bank.getStorage().insert(knabbels, BankStorage.CAP - 1);
        boolean[] eens = new boolean[1];
        String[] fout = new String[1];
        helper.runAfterDelay(30, () -> {
            gelijk(helper, HapluikjeBlockEntity.Stand.KLAAR, luikje.stand(), "the luikje works");
            gelijk(helper, 1, Voorraad.luikjes(level, h).size(), "and counts as storage");
            gelijk(helper, helper.absolutePos(luikjePlek), Voorraad.afleverPlek(level, h), "residents bring their spoils to the luikje");
            gelijk(helper, helper.absolutePos(KlusjesGameTests.KIST), Voorraad.brengPlek(level, h), "the stock is still taken from the chest");
            helper.assertTrue(HuisjePayloads.data(p, h).getBooleanOr("Luikje", false) && !HuisjePayloads.data(p, h).getBooleanOr("Bank", true),
                    "the huisje screen says so");
            CompoundTag ding = rij(HuisjeOverzicht.data(level, h), "Dingen", "hapluikje");
            helper.assertTrue(ding.getIntOr("Aantal", -1) == 1 && ding.getStringOr("Naam", "").equals("block.guhs.hapluikje"), "the overview counts it: " + ding);
            helper.assertTrue(Voorraad.heeftOpslag(level, h) && Voorraad.past(level, h, new ItemStack(Items.APPLE, 64)), "past: an apple fits (through the luikje)");
            // straight through lever: the bank's cap, a loaned thing never
            Voorraad.Geleverd g = Voorraad.lever(level, h, new ItemStack(Items.APPLE, 3));
            helper.assertTrue(g.luikje() && !g.bank() && bank.getStorage().count(new ItemStack(Items.APPLE)) == 3, "three apples went through the luikje: " + g + ", " + bank.getStorage().count(new ItemStack(Items.APPLE)) + ", " + luikje.stand() + ", " + luikje.doel());
            h.zetKlus(Band.id(guh), "opgraven", true);
            helper.succeedWhen(() -> {
                String waar = KlusjesGameTests.staat(helper, guh);
                long inBank = bank.getStorage().count(knabbels);
                int inKist = KlusjesGameTests.telKist(kist, s -> s.is(ModItems.KAAS_KNABBELS.get()));
                gelijk(helper, (long) BankStorage.CAP, inBank, "the bank took the one kaasknabbel it had room for" + waar);
                helper.assertTrue(inKist >= 1, "what the bank is full of went into the chest: " + inKist + waar);
                helper.assertTrue(behaald(p, "techniek/tech_klusjes_luikje") && behaald(p, "quest/tech_klusjes_luikje"), "the owner's advancements");
                if (!eens[0]) {
                    eens[0] = true;   // (the world is changed here: once)
                    fout[0] = bankInHetGebied(helper, h, bank);
                }
                helper.assertTrue(fout[0] == null, String.valueOf(fout[0]));
                KlusjesGameTests.weg(helper, h, p);
            });
        });
    }

    /** A Bank Guh in the klus-area goes before the luikje; what it is full of goes on through the luikje. Null = fine. */
    private static String bankInHetGebied(GameTestHelper helper, Huisje h, BankGuhBlockEntity ver) {
        ServerLevel level = helper.getLevel();
        BlockPos dichtbij = new BlockPos(8, 2, 14);
        helper.setBlock(dichtbij, ModBlocks.BANK_GUH.get());
        BankGuhBlockEntity eigen = (BankGuhBlockEntity) level.getBlockEntity(helper.absolutePos(dichtbij));
        if (!helper.absolutePos(dichtbij).equals(Voorraad.afleverPlek(level, h))) {
            return "with a Bank Guh in the area the spoils go there, not to " + Voorraad.afleverPlek(level, h);
        }
        Voorraad.Geleverd g = Voorraad.lever(level, h, new ItemStack(Items.CARROT, 4));
        if (!g.bank() || g.luikje() || eigen.getStorage().count(new ItemStack(Items.CARROT)) != 4 || ver.getStorage().count(new ItemStack(Items.CARROT)) != 0) {
            return "the Bank Guh of the area should take the carrots: " + g + ", " + eigen.getStorage().count(new ItemStack(Items.CARROT));
        }
        eigen.getStorage().insert(new ItemStack(Items.POTATO), BankStorage.CAP - 1);
        g = Voorraad.lever(level, h, new ItemStack(Items.POTATO, 3));
        if (!g.bank() || !g.luikje() || eigen.getStorage().count(new ItemStack(Items.POTATO)) != BankStorage.CAP
                || ver.getStorage().count(new ItemStack(Items.POTATO)) != 2) {
            return "one potato in the full bank, two through the luikje: " + g + ", " + eigen.getStorage().count(new ItemStack(Items.POTATO)) + " / "
                    + ver.getStorage().count(new ItemStack(Items.POTATO));
        }
        // a loaned thing never goes into a bank, not through a luikje either: it lands in the chest
        ItemStack geleend = geleend();
        if (geleend != null) {
            g = Voorraad.lever(level, h, geleend);
            if (g.bank() || g.luikje()) {
                return "a loaned thing went into a bank: " + g;
            }
        }
        return null;
    }

    /** Something of the item tag guhs:loaned (null when the tag is empty). */
    private static ItemStack geleend() {
        for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(nl.juiced.guhs.feature.Features.LOANED)) {
            return new ItemStack(holder);
        }
        return null;
    }

    // =====================================================================================================================
    // the overview
    // =====================================================================================================================

    @GuhTest(template = TUIN, batch = "techklus_overzicht", timeoutTicks = 200)
    public static void techklusOverzichtKentDeNieuweKlusjes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = KlusjesGameTests.speler(helper, new BlockPos(18, 2, 18));
        Huisje h = KlusjesGameTests.huisje(helper, p);
        Klus machines = Klusjes.van(MachineKlus.ID), plantage = Klusjes.van(PlantageKlus.ID);
        helper.assertTrue(machines == TechklusFeature.MACHINE_KLUS && plantage == TechklusFeature.PLANTAGE_KLUS, "both chores are registered");
        GuhEntity guh = helper.spawn(nl.juiced.guhs.registry.ModEntities.GUH.get(), new BlockPos(14, 2, 14));
        helper.assertTrue(machines.kan(guh) && plantage.kan(guh) && !machines.icoon().isEmpty() && !plantage.icoon().isEmpty(), "guhs can do them");
        // a resident that just moved in: the plantage is on, the machines wait until the owner switches them on
        guh.tame(p);
        guh.setNoAi(true);
        helper.assertTrue(nl.juiced.guhs.feature.huisje.Huisjes.trekIn(h, guh), "moves in");
        helper.assertTrue(h.klusAan(guh, "plantage") && !h.klusAan(guh, "machines"), "plantage is on by default, machines is off");
        KlusGebied.vergeet();
        CompoundTag data = HuisjeOverzicht.vraag(p, h.pos());
        helper.assertTrue(data != null, "the owner gets an answer");
        CompoundTag rij = rij(data, "Klusjes", "machines");
        helper.assertTrue(rij.getStringOr("Reden", "").equals("geen") && rij.getStringOr("Wie", "").equals("guhs")
                && rij.getStringOr("Icoon", "").equals("guhs:guh_oven"), "machines: none here: " + rij);
        helper.assertTrue(rij.getIntOr("Kunnen", -1) == 1 && rij.getIntOr("Aan", -1) == 0, "one resident can, none has it switched on: " + rij);
        helper.assertTrue(rij(data, "Klusjes", "plantage").getIntOr("Aan", -1) == 1, "plantage is switched on for it");
        gelijk(helper, "NEE:geen:0", stand(helper, h, "plantage"), "no Plantagebak");
        // a Plantagebak without vadskracht, no saplings anywhere
        BlockPos kern = new BlockPos(17, 2, 15);
        helper.setBlock(kern, TechmachineFeature.PLANTAGEBAK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        PlantagebakBlockEntity bak = (PlantagebakBlockEntity) level.getBlockEntity(helper.absolutePos(kern));
        gelijk(helper, "NEE:geen_zaailing:1", stand(helper, h, "plantage"), "an empty bak, no saplings in the chest");
        ChestBlockEntity kist = KlusjesGameTests.kist(helper);
        kist.setItem(0, new ItemStack(Items.BIRCH_SAPLING, 2));
        gelijk(helper, "JA:planten:1", stand(helper, h, "plantage"), "saplings in the chest");
        ItemStack hand = new ItemStack(Items.BIRCH_SAPLING, 2);
        bak.plant(hand);
        gelijk(helper, "NEE:geen_kracht:1", stand(helper, h, "plantage"), "saplings in the bak, but it sleeps");
        // an oven: nothing shown, nothing in it
        BlockPos oven = new BlockPos(6, 2, 15);
        helper.setBlock(oven, GuhovenFeature.GUH_OVEN.get());
        gelijk(helper, "STRAKS:rustig:1", stand(helper, h, "machines"), "an empty oven");
        ((GuhOvenBlockEntity) level.getBlockEntity(helper.absolutePos(oven))).setItem(0, new ItemStack(Items.STICK, 3));
        gelijk(helper, List.of(), wil(level, helper.absolutePos(oven)), "a stick does not bake: the oven learns nothing from it");
        ((GuhOvenBlockEntity) level.getBlockEntity(helper.absolutePos(oven))).setItem(0, new ItemStack(Items.RAW_COPPER, 3));
        gelijk(helper, "NEE:geen_voorraad:1", stand(helper, h, "machines"), "shown copper, none in the chest");
        kist.setItem(1, new ItemStack(Items.RAW_COPPER, 9));
        gelijk(helper, "JA:vullen:1", stand(helper, h, "machines"), "copper in the chest");
        // every chore of the game has a row, in the screen's order, with the two new ones at the end
        data = HuisjeOverzicht.data(level, h);
        ListTag rijen = data.getListOrEmpty("Klusjes");
        gelijk(helper, Klusjes.alle().size(), rijen.size(), "a row per chore");
        int nr = -1;
        for (int i = 0; i < rijen.size(); i++) {
            if (rijen.getCompoundOrEmpty(i).getStringOr("Id", "").equals("machines")) {
                nr = i;
            }
        }
        helper.assertTrue(nr > 9 && rijen.getCompoundOrEmpty(nr + 1).getStringOr("Id", "").equals("plantage"), "machines and plantage come after the old chores: " + nr);
        helper.assertTrue(level.getBlockState(helper.absolutePos(oven)).is(TechklusFeature.MACHINES)
                && TechmachineFeature.OOGSTER.get().defaultBlockState().is(TechklusFeature.MACHINES)
                && !TechmachineFeature.PLANTAGEBAK.get().defaultBlockState().is(TechklusFeature.MACHINES)
                && !ModBlocks.BANK_GUH.get().defaultBlockState().is(TechklusFeature.MACHINES)
                && BarbecuetherFeature.PINDASCHEUTJES.get().defaultBlockState().is(nl.juiced.guhs.feature.klusjes.KlusjesFeature.SCHEUTJES)
                && !Blocks.OAK_LOG.defaultBlockState().is(BlockTags.SAPLINGS), "the tags are loaded");
        KlusjesGameTests.weg(helper, h, p);
        helper.succeed();
    }
}
