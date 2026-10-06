package nl.juiced.guhs.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.HapluikjeBlockEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.bestaand.BestaandFeature;
import nl.juiced.guhs.feature.campingmarkt.CampingmarktFeature;
import nl.juiced.guhs.feature.fossielmijn.FossielmijnFeature;
import nl.juiced.guhs.feature.paleizen.PaleizenFeature;
import nl.juiced.guhs.feature.sausdieren.SausdierenFeature;
import nl.juiced.guhs.feature.techbezorg.HaltepaaltjeBlock;
import nl.juiced.guhs.feature.techbezorg.HaltepaaltjeBlockEntity;
import nl.juiced.guhs.feature.techbezorg.StepstationBlockEntity;
import nl.juiced.guhs.feature.techbezorg.TechbezorgFeature;
import nl.juiced.guhs.feature.techbron.TechbronFeature;
import nl.juiced.guhs.feature.techbuis.BuisStukBlock;
import nl.juiced.guhs.feature.techbuis.BuisStukBlockEntity;
import nl.juiced.guhs.feature.techbuis.FilterBlockEntity;
import nl.juiced.guhs.feature.techbuis.TechbuisFeature;
import nl.juiced.guhs.feature.techbezorg.FluitjeItem;
import nl.juiced.guhs.feature.techmachine.KnabbelaarBlockEntity;
import nl.juiced.guhs.feature.techmachine.Oogst;
import nl.juiced.guhs.feature.techmachine.TechmachineFeature;
import nl.juiced.guhs.feature.techsaus.TechsausFeature;
import nl.juiced.guhs.feature.torenpeper.PeperSoort;
import nl.juiced.guhs.feature.torenpeper.PeperplantBlock;
import nl.juiced.guhs.feature.torenpeper.TorenpeperFeature;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.WildeDieren;

/**
 * bbq2, written at the merge of the tech slices: the seams BETWEEN slices that no slice could test alone (batch
 * "techsamen"). Knabbelbuizen at a real Bank Guh (in up to the cap, out only when upgraded, the Filterstuk's "laat liggen
 * N"), the Bezorgguhtje at a Hapluikje and at an upgraded Bank Guh, the item tags that point at another slice's items, and
 * what the Knabbelaar never eats of the other slices. Template techbezorg_test_kamer: 15 x 6 x 15 with a stone floor
 * (things stand at helper y 2). Added at the merge of the buildings (paleizen, bestaand, camping-markt, toren-peper): the
 * Oogster's harvest of the peperplant, the camping's recipe card in the Plantagebak recipe, the keeper's real whistle, and
 * the quest props of those buildings that a Knabbelaar leaves alone.
 */
public class TechSamenGameTests {
    private static final String BATCH = "techsamen", KAMER = "techbezorg_test_kamer";

    private static BlockPos p(int x, int z) {
        return new BlockPos(x, 2, z);
    }

    private static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    private static Item knabbel() {
        return ModItems.KAAS_KNABBELS.get();
    }

    private static ItemStack kei(int n) {
        return new ItemStack(Items.COBBLESTONE, n);
    }

    private static BankGuhBlockEntity bank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.BANK_GUH.get());
        return (BankGuhBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static ChestBlockEntity kist(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    /** A source of 10 vadskracht. */
    private static void bron(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 1));
    }

    private static int rollend(BuisStukBlockEntity stuk) {
        int n = 0;
        for (BuisStukBlockEntity.Rit rit : stuk.onderweg()) {
            n += rit.stack().getCount();
        }
        for (ItemStack stack : stuk.terug()) {
            n += stack.getCount();
        }
        return n;
    }

    private static StepstationBlockEntity station(GameTestHelper helper, BlockPos pos) {
        bron(helper, pos.south());
        helper.setBlock(pos, TechbezorgFeature.STEPSTATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        return (StepstationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    /** A pole south of the block it serves, linked to this station. */
    private static HaltepaaltjeBlockEntity paal(GameTestHelper helper, BlockPos pos, boolean ophalen, StepstationBlockEntity station) {
        helper.setBlock(pos, TechbezorgFeature.HALTEPAALTJE.get().defaultBlockState().setValue(HaltepaaltjeBlock.FACING, Direction.NORTH)
                .setValue(HaltepaaltjeBlock.OPHALEN, ophalen));
        HaltepaaltjeBlockEntity halte = (HaltepaaltjeBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(station.koppel(halte), "the pole at " + pos + " joins the station");
        return halte;
    }

    // =====================================================================================================================
    // Knabbelbuizen and the Bank Guh
    // =====================================================================================================================

    /**
     * Chest - Richtingstuk - tube - Bank Guh that already holds 250 cobblestone: six more go in (the cap), the other ten
     * stay in the chest, and at every tick all sixteen are somewhere. Dirt has its own room and all of it arrives.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 800)
    public static void techsamenBuisVultDeBankTotDeCap(GameTestHelper helper) {
        BankGuhBlockEntity bank = bank(helper, p(5, 2));
        gelijk(helper, 250L, bank.getStorage().insert(kei(1), 250), "the bank starts with 250 cobblestone");
        ChestBlockEntity bron = kist(helper, p(2, 2));
        bron.setItem(0, kei(16));
        bron.setItem(1, new ItemStack(Items.DIRT, 8));
        helper.setBlock(p(4, 2), TechbuisFeature.KNABBELBUIS.get());
        helper.setBlock(p(3, 2), TechbuisFeature.KNABBELBUIS_RICHTING.get().defaultBlockState().setValue(BuisStukBlock.FACING, Direction.EAST));
        BuisStukBlockEntity stuk = (BuisStukBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(p(3, 2)));
        gelijk(helper, 1, stuk.routes().size(), "the tube ends at the Bank Guh");
        gelijk(helper, helper.absolutePos(p(5, 2)), stuk.routes().get(0).doel(), "the Bank Guh is the place to send to");
        helper.onEachTick(() -> {
            long inBank = bank.getStorage().count(kei(1)) - 250;
            int inKist = bron.countItem(Items.COBBLESTONE);
            int onderweg = 0;
            for (BuisStukBlockEntity.Rit rit : stuk.onderweg()) {
                onderweg += rit.stack().is(Items.COBBLESTONE) ? rit.stack().getCount() : 0;
            }
            for (ItemStack stack : stuk.terug()) {
                onderweg += stack.is(Items.COBBLESTONE) ? stack.getCount() : 0;
            }
            gelijk(helper, 16L, inBank + inKist + onderweg, "no cobblestone lost (chest " + inKist + ", rolling " + onderweg + ", bank +" + inBank + ")");
            helper.assertTrue(bank.getStorage().count(kei(1)) <= 256, "never over the cap: " + bank.getStorage().count(kei(1)));
        });
        helper.succeedWhen(() -> {
            gelijk(helper, 256L, bank.getStorage().count(kei(1)), "the bank is full of cobblestone");
            gelijk(helper, 8L, bank.getStorage().count(new ItemStack(Items.DIRT)), "all the dirt arrived");
            gelijk(helper, 10, bron.countItem(Items.COBBLESTONE), "the cobblestone that does not fit stays in the chest");
            gelijk(helper, 0, rollend(stuk), "nothing left in the tube");
        });
    }

    /**
     * A Filterstuk behind a Bank Guh with 40 wheat, "laat liggen 10": nothing comes out of a bank without its upgrade;
     * upgraded, 30 go to the chest and 10 stay.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 900)
    public static void techsamenBankAlleenLeegMetUpgrade(GameTestHelper helper) {
        BankGuhBlockEntity bank = bank(helper, p(2, 6));
        ItemStack tarwe = new ItemStack(Items.WHEAT);
        gelijk(helper, 40L, bank.getStorage().insert(tarwe, 40), "40 wheat in the bank");
        gelijk(helper, 12L, bank.getStorage().insert(kei(1), 12), "and 12 cobblestone");
        helper.setBlock(p(3, 6), TechbuisFeature.KNABBELBUIS_FILTER.get().defaultBlockState().setValue(BuisStukBlock.FACING, Direction.EAST));
        FilterBlockEntity filter = (FilterBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(p(3, 6)));
        helper.assertTrue(filter.filter().voegToe(tarwe), "wheat on the list");
        filter.filter().zetGetal(10);
        helper.setBlock(p(4, 6), TechbuisFeature.KNABBELBUIS.get());
        ChestBlockEntity doel = kist(helper, p(5, 6));
        bron(helper, p(3, 7));
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(filter.heeftKracht(), "the Filterstuk has vadskracht");
            gelijk(helper, 0, doel.countItem(Items.WHEAT) + rollend(filter), "nothing comes out of a bank without its upgrade");
            gelijk(helper, 40L, bank.getStorage().count(tarwe), "the bank still holds its wheat");
            bank.getStorage().setUpgraded(true);
            helper.succeedWhen(() -> {
                gelijk(helper, 30, doel.countItem(Items.WHEAT), "upgraded: the wheat above ten went to the chest");
                gelijk(helper, 10L, bank.getStorage().count(tarwe), "ten wheat stay in the bank");
                gelijk(helper, 12L, bank.getStorage().count(kei(1)), "cobblestone is not on the list");
                gelijk(helper, 0, rollend(filter), "nothing left in the tube");
            });
        });
    }

    // =====================================================================================================================
    // the Bezorgguhtje, the Hapluikje and the Bank Guh
    // =====================================================================================================================

    /**
     * Chest -> Bezorgguhtje -> Hapluikje -> its Bank Guh. The bank already holds 250 cobblestone: of the 20 in the chest
     * only six may come along (the Hapluikje says what still fits), the knabbels all arrive, and nothing stays behind in
     * the backpack.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 1200)
    public static void techsamenBezorgguhtjeBrengtNaarHetHapluikje(GameTestHelper helper) {
        BankGuhBlockEntity bank = bank(helper, p(12, 12));
        gelijk(helper, 250L, bank.getStorage().insert(kei(1), 250), "the bank starts with 250 cobblestone");
        StepstationBlockEntity station = station(helper, p(7, 7));
        ChestBlockEntity a = kist(helper, p(2, 3));
        a.setItem(0, new ItemStack(knabbel(), 40));
        a.setItem(1, kei(20));
        helper.setBlock(p(12, 3), BankFeature.HAPLUIKJE.get());
        HapluikjeBlockEntity luikje = (HapluikjeBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(p(12, 3)));
        luikje.koppel(bank.bankId());
        bron(helper, p(13, 3));
        paal(helper, p(2, 4), true, station);
        paal(helper, p(12, 4), false, station);
        helper.succeedWhen(() -> {
            gelijk(helper, 40L, bank.getStorage().count(new ItemStack(knabbel())), "kaasknabbels in the bank");
            gelijk(helper, 256L, bank.getStorage().count(kei(1)), "cobblestone in the bank: the cap");
            gelijk(helper, 14, a.countItem(Items.COBBLESTONE), "the cobblestone that does not fit stays in the chest");
            gelijk(helper, 0, a.countItem(knabbel()), "no knabbels left in the chest");
            helper.assertTrue(station.rugzakLeeg(), "the backpack is empty");
        });
    }

    /**
     * An "ophalen" pole at a Bank Guh: nothing leaves a bank without its upgrade; from an upgraded bank the Bezorgguhtje
     * brings what the other stop asks for.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 1500)
    public static void techsamenBezorgguhtjeHaaltUitEenOpgevoerdeBank(GameTestHelper helper) {
        BankGuhBlockEntity bank = bank(helper, p(2, 3));
        gelijk(helper, 30L, bank.getStorage().insert(new ItemStack(knabbel()), 30), "30 knabbels in the bank");
        gelijk(helper, 9L, bank.getStorage().insert(kei(1), 9), "and 9 cobblestone");
        StepstationBlockEntity station = station(helper, p(7, 7));
        ChestBlockEntity b = kist(helper, p(12, 3));
        paal(helper, p(2, 4), true, station);
        HaltepaaltjeBlockEntity paalB = paal(helper, p(12, 4), false, station);
        paalB.zetFilter(0, new ItemStack(knabbel()));
        helper.runAfterDelay(300, () -> {
            gelijk(helper, 0, b.countItem(knabbel()), "nothing comes out of a bank without its upgrade");
            gelijk(helper, 30L, bank.getStorage().count(new ItemStack(knabbel())), "the bank still holds its knabbels");
            helper.assertTrue(station.rugzakLeeg(), "and the backpack is empty");
            bank.getStorage().setUpgraded(true);
            helper.succeedWhen(() -> {
                gelijk(helper, 30, b.countItem(knabbel()), "upgraded: the knabbels are in the chest");
                gelijk(helper, 0L, bank.getStorage().count(new ItemStack(knabbel())), "and out of the bank");
                gelijk(helper, 9L, bank.getStorage().count(kei(1)), "the cobblestone nobody asks for stays");
                helper.assertTrue(station.rugzakLeeg(), "the backpack is empty");
            });
        });
    }

    // =====================================================================================================================
    // ids and tags that cross slices
    // =====================================================================================================================

    /**
     * The tags of one slice that name another slice's things, on the merged tree: the Blubkacheltje's jar and food, what
     * the Knabbelaar never eats, the wild sausdieren in the tidy-up, and the Zout / Saus tier items that the recipes need.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void techsamenTagsOverSlices(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(new ItemStack(SausdierenFeature.SAUSBLUBJE_POTJE.get()).is(TechbronFeature.BLUBJE_IN_POT), "the real jar goes into the Blubkacheltje");
        helper.assertTrue(new ItemStack(knabbel()).is(TechbronFeature.BLUBVOER), "a blubje in a stove eats kaasknabbels");
        helper.assertTrue(new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get()).is(TechbronFeature.BLUBVOER),
                "and what a wild Sausblubje eats (the optional tag entry of sausdieren)");
        helper.assertTrue(!new ItemStack(Items.COBBLESTONE).is(TechbronFeature.BLUBVOER), "not just anything");
        // the Knabbelaar: never a machine part of another slice, never the vein or the rubble of the Zoutkristalmijn
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        List<BlockState> nooit = List.of(TechbuisFeature.KNABBELBUIS.get().defaultBlockState(), TechsausFeature.SAUSSLANG.get().defaultBlockState(),
                TechmachineFeature.TEKENTAFEL.get().defaultBlockState(), FossielmijnFeature.ZOUTADER.get().defaultBlockState(),
                FossielmijnFeature.PUIN.get().defaultBlockState(), BankFeature.HAPLUIKJE.get().defaultBlockState(),
                ModBlocks.BANK_GUH.get().defaultBlockState(), TechbuisFeature.KNABBELBUIS_RICHTING.get().defaultBlockState(),
                TechbezorgFeature.HALTEPAALTJE.get().defaultBlockState(), TechsausFeature.SAUSVAT.get().defaultBlockState(),
                TechbronFeature.KNUFFELGENERATOR.get().defaultBlockState(), FossielmijnFeature.BOTTENZAND.get().defaultBlockState(),
                FossielmijnFeature.SKELETREK.get().defaultBlockState(),
                // the buildings: quest props that stand outside a protected box (the Spiesburcht stays breakable) or that nobody can make
                BestaandFeature.VUURKORF.get().defaultBlockState(), PaleizenFeature.BREIWERK.get().defaultBlockState(),
                CampingmarktFeature.KAMPEERPLEK.get().defaultBlockState(), CampingmarktFeature.VADSSTAPEL.get().defaultBlockState(),
                TorenpeperFeature.VUURTORENLAMP.get().defaultBlockState(), TorenpeperFeature.KWEEKBAK.get().defaultBlockState());
        for (BlockState state : nooit) {
            helper.assertTrue(!KnabbelaarBlockEntity.magKnabbelen(level, pos, state), "the Knabbelaar never eats " + state.getBlock());
        }
        helper.assertTrue(KnabbelaarBlockEntity.magKnabbelen(level, pos, FossielmijnFeature.ZOUTKRISTALERTS.get().defaultBlockState()),
                "zoutkristalerts in the open world is fair game");
        helper.assertTrue(KnabbelaarBlockEntity.magKnabbelen(level, pos, CampingmarktFeature.TENTDOEK.get("rood").blok().get().defaultBlockState())
                && KnabbelaarBlockEntity.magKnabbelen(level, pos, PaleizenFeature.BRUGPLANK.get().defaultBlockState()),
                "the building blocks a player gets from the buildings (tent canvas, bridge planks) are ordinary blocks to it");
        helper.assertTrue(WildeDieren.soorten().contains(SausdierenFeature.SAUSLOPER.get()) && WildeDieren.soorten().contains(SausdierenFeature.SAUSBLUBJE.get()),
                "the tidy-up of wild animals knows the Sausloper and the Sausblubje");
        // every recipe of the merged slices loads (an unknown item id in a recipe drops it without a crash)
        for (String recept : List.of("hapluikje", "bank_sleutel", "knabbelbatterij", "knuffelgenerator", "disco_dynamo", "blubkacheltje", "gloeisterkern",
                "knabbelbuis", "knabbelbuis_filter", "knabbelbuis_richting", "opzuiger", "voorraadmeter", "snuffelsensor", "guhklok", "guhteller",
                "oogster", "knabbelaar", "neerzetter", "knutselmachine", "tekentafel", "plantagebak", "vadsmolen",
                "sauspomp", "sausslang", "sausvat", "brouwautomaat", "frituurautomaat", "grillkoolpers", "stepstation", "haltepaaltje")) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, nl.juiced.guhs.Guhs.id(recept));
            helper.assertTrue(level.getServer().getRecipeManager().byKey(key).isPresent(), "the recipe guhs:" + recept + " loads");
        }
        // the Plantagebak (tech-machines) asks for the recipe card that the Grillcamping gives (camping-markt); the whistle the
        // Torenwachter-guh gives (toren-peper) is tech-bezorg's real one
        var bak = level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.RECIPE, nl.juiced.guhs.Guhs.id("plantagebak")));
        ItemStack kaart = new ItemStack(CampingmarktFeature.RECEPT_PLANTAGEBAK.get());
        helper.assertTrue(bak.isPresent() && bak.get().value().placementInfo().ingredients().stream().anyMatch(i -> i.test(kaart)),
                "the Plantagebak recipe holds the camping's recipe card");
        helper.assertTrue(kaart.getItem().getCraftingRemainder(kaart) != null, "and the card stays in the grid");
        helper.assertTrue(TechbezorgFeature.BEZORGGUHTJE_FLUITJE.get() instanceof FluitjeItem, "the Bezorgguhtje-fluitje is the real whistle, not a placeholder");
        helper.succeed();
    }

    // =====================================================================================================================
    // the Oogster and the peperplant of the Pepertuin
    // =====================================================================================================================

    /**
     * The Oogster's rules ({@link Oogst}) on a ripe peperplant (toren-peper, a CropBlock): on hot ground it cuts red
     * Vahoegpepers, a seed goes back into the ground, and the young plant it leaves knows its ground again (the kind is
     * corrected when the plant is placed) and has its clock, so the field keeps growing without anybody. A young plant is
     * left alone.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void techsamenOogsterOogstPepers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PeperplantBlock plant = TorenpeperFeature.PEPERPLANT.get();
        helper.setBlock(p(7, 7), Blocks.MAGMA_BLOCK);
        helper.setBlock(p(9, 7), BarbecuetherFeature.AS_AARDE.get());
        BlockPos heet = p(7, 7).above(), gewoon = p(9, 7).above();
        helper.setBlock(heet, plant.getStateForAge(PeperplantBlock.MAX_AGE));
        helper.setBlock(gewoon, plant.getStateForAge(1));
        BlockPos plek = helper.absolutePos(heet);
        gelijk(helper, PeperSoort.ROOD, level.getBlockState(plek).getValue(PeperplantBlock.SOORT), "a plant on magma carries red peppers");
        helper.assertTrue(Oogst.isRijp(level, plek), "the Oogster sees the ripe peperplant");
        helper.assertTrue(!Oogst.isRijp(level, helper.absolutePos(gewoon)) && Oogst.bekijk(level, helper.absolutePos(gewoon)) == null,
                "and leaves a young one alone");
        Oogst.Pluk pluk = Oogst.bekijk(level, plek);
        helper.assertTrue(pluk != null, "a cut is offered");
        int rood = 0, zaad = 0, anders = 0;
        for (ItemStack stack : pluk.oogst()) {
            if (stack.is(TorenpeperFeature.VAHOEGPEPER.get())) {
                rood += stack.getCount();
            } else if (stack.is(TorenpeperFeature.PEPERZAADJES.get())) {
                zaad += stack.getCount();
            } else {
                anders += stack.getCount();
            }
        }
        helper.assertTrue(rood >= PeperplantBlock.PLUK_MIN && rood <= PeperplantBlock.PLUK_MAX, "2 or 3 Vahoegpepers: " + rood);
        helper.assertTrue(zaad <= 1 && anders == 0, "at most one spare seed (one went back into the ground) and nothing else: " + pluk.oogst());
        helper.assertTrue(Oogst.doe(level, pluk), "the cut is carried out");
        BlockState jong = level.getBlockState(plek);
        helper.assertTrue(jong.is(plant) && jong.getValue(PeperplantBlock.AGE) == 0, "a young plant stands there: " + jong);
        gelijk(helper, PeperSoort.ROOD, jong.getValue(PeperplantBlock.SOORT), "that still knows its hot ground");
        helper.assertTrue(level.getBlockTicks().hasScheduledTick(plek, plant), "and grows on by itself (its clock runs)");
        helper.assertTrue(!Oogst.isRijp(level, plek), "nothing to cut again right away");
        helper.succeed();
    }
}
