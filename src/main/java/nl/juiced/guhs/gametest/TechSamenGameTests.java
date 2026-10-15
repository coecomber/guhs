package nl.juiced.guhs.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.HapluikjeBlockEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.bestaand.BestaandFeature;
import nl.juiced.guhs.feature.campingmarkt.CampingmarktFeature;
import nl.juiced.guhs.feature.fossielmijn.FossielmijnFeature;
import nl.juiced.guhs.feature.guhoven.GuhOvenBlockEntity;
import nl.juiced.guhs.feature.guhoven.GuhovenFeature;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.KlusjesGameTests;
import nl.juiced.guhs.feature.paleizen.PaleizenFeature;
import nl.juiced.guhs.feature.ring.RingFeature;
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
import nl.juiced.guhs.feature.techklus.Klusmachines;
import nl.juiced.guhs.feature.techklus.TechklusFeature;
import nl.juiced.guhs.feature.techmachine.KnabbelaarBlockEntity;
import nl.juiced.guhs.feature.techmachine.Oogst;
import nl.juiced.guhs.feature.techmachine.TechmachineFeature;
import nl.juiced.guhs.feature.techquest.TechquestFeature;
import nl.juiced.guhs.feature.techsaus.TechsausFeature;
import nl.juiced.guhs.feature.torenpeper.PeperSoort;
import nl.juiced.guhs.feature.torenpeper.PeperplantBlock;
import nl.juiced.guhs.feature.torenpeper.TorenpeperFeature;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.taal.NlTekst;
import nl.juiced.guhs.world.WildeDieren;

/**
 * bbq2, written at the merge of the tech slices: the seams BETWEEN slices that no slice could test alone (batch
 * "techsamen"). Knabbelbuizen at a real Bank Guh (in up to the cap, out only when upgraded, the Filterstuk's "laat liggen
 * N"), the Bezorgguhtje at a Hapluikje and at an upgraded Bank Guh, the item tags that point at another slice's items, and
 * what the Knabbelaar never eats of the other slices. Template techbezorg_test_kamer: 15 x 6 x 15 with a stone floor
 * (things stand at helper y 2). Added at the merge of the buildings (paleizen, bestaand, camping-markt, toren-peper): the
 * Oogster's harvest of the peperplant, the camping's recipe card in the Plantagebak recipe, the keeper's real whistle, and
 * the quest props of those buildings that a Knabbelaar leaves alone. Added at the merge of tech-klusjes and tech-quests:
 * the recipe cards of the Uitvinder-guh in the twelve recipes of the other tech slices, the machines the chore "machines"
 * serves, and (batch "techsamen_klus", template techklus_test_tuin, a real Guhhuisje with a resident) the farmen chore on a
 * peperplant and the chores that stay out of a protected quest building.
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
     * A Filterstuk behind a Bank Guh with 40 wheat, "laat liggen 10": nothing comes out of a bank without its upgrade (and
     * the piece says why); upgraded, 30 go to the chest and 10 stay. The Filterstuk is the ONLY thing that takes from a
     * bank (the user's decision B6): see {@link #techsamenBankGeeftAlleenAanEenFilterstuk}.
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
            gelijk(helper, "Deze Bank Guh geeft pas iets met het Bodemloos Knabbelmaagje", NlTekst.tekst(filter.stand()), "what the Filterstuk says");
            bank.getStorage().setUpgraded(true);
            helper.succeedWhen(() -> {
                gelijk(helper, 30, doel.countItem(Items.WHEAT), "upgraded: the wheat above ten went to the chest");
                gelijk(helper, 10L, bank.getStorage().count(tarwe), "ten wheat stay in the bank");
                gelijk(helper, 12L, bank.getStorage().count(kei(1)), "cobblestone is not on the list");
                gelijk(helper, 0, rollend(filter), "nothing left in the tube");
            });
        });
    }

    /**
     * The user's decision B6: an UPGRADED Bank Guh gives nothing to a plain Richtingstuk behind it (which says so) and
     * nothing to a hopper under it, however long they try; the Filterstuk at its other side takes what is on its list and
     * leaves what it must. A Filterstuk without a list takes every kind, down to "laat liggen". And the Richtingstuk still
     * puts things INTO the bank through its own tube.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 1500)
    public static void techsamenBankGeeftAlleenAanEenFilterstuk(GameTestHelper helper) {
        BlockPos plek = new BlockPos(7, 3, 7);
        helper.setBlock(plek.below(), Blocks.HOPPER);
        BankGuhBlockEntity bank = bank(helper, plek);
        HopperBlockEntity trechter = (HopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(plek.below()));
        ItemStack tarwe = new ItemStack(Items.WHEAT);
        bank.getStorage().setUpgraded(true);
        gelijk(helper, 300L, bank.getStorage().insert(tarwe, 300), "300 wheat in the upgraded bank");
        gelijk(helper, 20L, bank.getStorage().insert(kei(1), 20), "and 20 cobblestone");
        // west of the bank: a plain Richtingstuk that bites out of the bank, a tube, a chest
        helper.setBlock(plek.west(), TechbuisFeature.KNABBELBUIS_RICHTING.get().defaultBlockState().setValue(BuisStukBlock.FACING, Direction.WEST));
        BuisStukBlockEntity richting = (BuisStukBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(plek.west()));
        helper.setBlock(plek.west(2), TechbuisFeature.KNABBELBUIS.get());
        ChestBlockEntity links = kist(helper, plek.west(3));
        // east of the bank: a Filterstuk (wheat, laat liggen 100), a tube, a chest; its vadskracht from a source next to it
        helper.setBlock(plek.east(), TechbuisFeature.KNABBELBUIS_FILTER.get().defaultBlockState().setValue(BuisStukBlock.FACING, Direction.EAST));
        FilterBlockEntity filter = (FilterBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(plek.east()));
        helper.assertTrue(filter.filter().voegToe(tarwe), "wheat on the list");
        filter.filter().zetGetal(100);
        helper.setBlock(plek.east(2), TechbuisFeature.KNABBELBUIS.get());
        ChestBlockEntity rechts = kist(helper, plek.east(3));
        bron(helper, plek.east().north());
        gelijk(helper, 1, richting.routes().size(), "the Richtingstuk's tube ends at the chest");
        helper.succeedWhen(() -> {
            gelijk(helper, 200, rechts.countItem(Items.WHEAT), "the Filterstuk took the wheat above a hundred");
            gelijk(helper, 100L, bank.getStorage().count(tarwe), "a hundred wheat stay in the bank");
            gelijk(helper, 0, rollend(filter), "nothing left in the Filterstuk's tube");
            // all that time (hundreds of ticks) the Richtingstuk and the hopper tried too
            helper.assertTrue(links.isEmpty() && rollend(richting) == 0, "the plain Richtingstuk got nothing out of the upgraded bank");
            helper.assertTrue(trechter.isEmpty(), "the hopper under the upgraded bank got nothing: " + trechter.getItem(0));
            gelijk(helper, 20L, bank.getStorage().count(kei(1)), "the cobblestone (not on the list) is all there");
            gelijk(helper, "Uit een Bank Guh hapt alleen een Filterstuk, njeg", NlTekst.tekst(richting.stand()), "what the Richtingstuk says");
            helper.assertTrue(rechts.countItem(Items.COBBLESTONE) == 0 && !filter.verstopt(), "and the Filterstuk took nothing that is not on its list");
        });
    }

    /**
     * B6, the other half: a Filterstuk with an EMPTY list ("laat alles door") at an upgraded bank takes every kind, each
     * down to "laat liggen"; and a Richtingstuk whose tube ends at an upgraded bank still fills it.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 1500)
    public static void techsamenFilterstukZonderLijstEnBankVullen(GameTestHelper helper) {
        BlockPos plek = new BlockPos(7, 2, 3);
        BankGuhBlockEntity bank = bank(helper, plek);
        bank.getStorage().setUpgraded(true);
        gelijk(helper, 40L, bank.getStorage().insert(new ItemStack(Items.WHEAT), 40), "40 wheat in the upgraded bank");
        gelijk(helper, 30L, bank.getStorage().insert(kei(1), 30), "and 30 cobblestone");
        helper.setBlock(plek.east(), TechbuisFeature.KNABBELBUIS_FILTER.get().defaultBlockState().setValue(BuisStukBlock.FACING, Direction.EAST));
        FilterBlockEntity filter = (FilterBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(plek.east()));
        filter.filter().zetGetal(25);
        helper.setBlock(plek.east(2), TechbuisFeature.KNABBELBUIS.get());
        ChestBlockEntity uit = kist(helper, plek.east(3));
        bron(helper, plek.east().north());
        // a chest whose Richtingstuk sends into the bank from the west
        ChestBlockEntity in = kist(helper, plek.west(3));
        in.setItem(0, new ItemStack(Items.DIRT, 12));
        helper.setBlock(plek.west(2), TechbuisFeature.KNABBELBUIS_RICHTING.get().defaultBlockState().setValue(BuisStukBlock.FACING, Direction.EAST));
        helper.setBlock(plek.west(), TechbuisFeature.KNABBELBUIS.get());
        helper.succeedWhen(() -> {
            gelijk(helper, 25L, bank.getStorage().count(new ItemStack(Items.WHEAT)), "25 wheat stay");
            gelijk(helper, 25L, bank.getStorage().count(kei(1)), "25 cobblestone stay");
            gelijk(helper, 15, uit.countItem(Items.WHEAT), "the other wheat is in the chest");
            gelijk(helper, 5, uit.countItem(Items.COBBLESTONE), "the other cobblestone too");
            // (the dirt comes in through the other tube and has fewer than 25: it never leaves again)
            gelijk(helper, 12L, bank.getStorage().count(new ItemStack(Items.DIRT)), "the dirt of the other chest went INTO the bank");
            helper.assertTrue(in.isEmpty() && uit.countItem(Items.DIRT) == 0, "and stays there: under 'laat liggen'");
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
     * An "ophalen" pole at a Bank Guh: nothing leaves a bank without its upgrade, and (the user's decision B6) nothing
     * leaves an UPGRADED bank either: a Bezorgguhtje never fetches from a bank, it only delivers there.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 1500)
    public static void techsamenBezorgguhtjeHaaltNietsUitEenBank(GameTestHelper helper) {
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
        });
        // (the guhtje keeps riding its round: four hundred more ticks are several visits to the pole at the bank)
        helper.runAfterDelay(700, () -> {
            helper.assertTrue(bank.isUpgraded(), "(the bank is upgraded by now)");
            gelijk(helper, 0, b.countItem(knabbel()), "upgraded: still nothing comes out of the bank for a pole");
            gelijk(helper, 30L, bank.getStorage().count(new ItemStack(knabbel())), "upgraded: the bank keeps its knabbels");
            gelijk(helper, 9L, bank.getStorage().count(kei(1)), "and its cobblestone");
            helper.assertTrue(station.rugzakLeeg(), "the backpack is empty");
            helper.succeed();
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
        // the Knabbelring (ring-kern) stays with its bearer: it is a loaned thing, which is what the Knabbelbuis, the Opzuiger, the
        // Hapluikje and the Bank Guh ask before they take a stack; the lamp of the Lichtflesje (a vanilla light block that walks
        // along with its holder) is nothing a Knabbelaar can eat; the hook and the rest fire can be made
        helper.assertTrue(Features.isLoaned(new ItemStack(RingFeature.KNABBELRING.get())), "no machine takes the Knabbelring");
        helper.assertTrue(!KnabbelaarBlockEntity.magKnabbelen(level, pos, Blocks.LIGHT.defaultBlockState()), "the Knabbelaar never eats the lamp of the Lichtflesje");
        for (String recept : List.of("elfentouw_haak", "ring_rustvuur")) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, nl.juiced.guhs.Guhs.id(recept));
            helper.assertTrue(level.getServer().getRecipeManager().byKey(key).isPresent(), "the recipe guhs:" + recept + " loads");
        }
        // the "Saus" tier (tech-quests): the three recipe cards of the Uitvinder-guh sit in twelve recipes that tech-bronnen,
        // tech-machines, tech-vloeistof and tech-bezorg wrote, and a card stays in the grid; the Plantagebak keeps the camping's
        // card (above) and gets no second one; the Sausslang stays free (the practice hall lends one)
        java.util.Map<Item, List<String>> kaarten = java.util.Map.of(
                TechquestFeature.RECEPT_SAUS.get(), List.of("sauspomp", "sausvat", "brouwautomaat", "frituurautomaat", "grillkoolpers", "blubkacheltje"),
                TechquestFeature.RECEPT_MACHINES.get(), List.of("knabbelaar", "neerzetter", "knutselmachine", "tekentafel"),
                TechquestFeature.RECEPT_BEZORG.get(), List.of("stepstation", "haltepaaltje"));
        for (var e : kaarten.entrySet()) {
            ItemStack k = new ItemStack(e.getKey());
            helper.assertTrue(k.getItem().getCraftingRemainder(k) != null, "the card " + e.getKey() + " stays in the grid");
            for (String recept : e.getValue()) {
                var r = level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.RECIPE, nl.juiced.guhs.Guhs.id(recept)));
                helper.assertTrue(r.isPresent() && r.get().value().placementInfo().ingredients().stream().anyMatch(i -> i.test(k)),
                        "the recipe guhs:" + recept + " asks for " + e.getKey());
            }
        }
        for (String recept : List.of("plantagebak", "sausslang", "guh_oven", "vadsmolen", "oogster", "knabbelbuis", "hapluikje", "gloeisterkern")) {
            var r = level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.RECIPE, nl.juiced.guhs.Guhs.id(recept)));
            helper.assertTrue(r.isPresent(), "the recipe guhs:" + recept + " loads");
            for (Item k : kaarten.keySet()) {
                helper.assertTrue(r.get().value().placementInfo().ingredients().stream().noneMatch(i -> i.test(new ItemStack(k))),
                        "the recipe guhs:" + recept + " needs no card of the Uitvinder-guh");
            }
        }
        // the chore "machines" (tech-klusjes) serves the ten machines of the other slices through their item capability; De
        // Grote Knabbelmachine (tech-quests) is no machine to serve and nothing a Knabbelaar eats
        for (var machine : List.of(GuhovenFeature.GUH_OVEN, TechmachineFeature.KNUTSELMACHINE, TechmachineFeature.VADSMOLEN, TechmachineFeature.OOGSTER,
                TechmachineFeature.KNABBELAAR, TechmachineFeature.NEERZETTER, TechsausFeature.BROUWAUTOMAAT, TechsausFeature.FRITUURAUTOMAAT,
                TechsausFeature.GRILLKOOLPERS, TechbuisFeature.OPZUIGER)) {
            helper.assertTrue(machine.get().defaultBlockState().is(TechklusFeature.MACHINES), machine.getId() + " is a machine the residents serve");
        }
        BlockState kern = TechquestFeature.GROTE_KNABBELMACHINE.get().defaultBlockState();
        helper.assertTrue(!kern.is(TechklusFeature.MACHINES) && !TechmachineFeature.PLANTAGEBAK.get().defaultBlockState().is(TechklusFeature.MACHINES),
                "De Grote Knabbelmachine and the Plantagebak (its own chore) are not in the machines tag");
        helper.assertTrue(!KnabbelaarBlockEntity.magKnabbelen(level, pos, kern)
                && !KnabbelaarBlockEntity.magKnabbelen(level, pos, TechquestFeature.KNABBELMACHINE_DEEL.get().defaultBlockState()),
                "the Knabbelaar never eats De Grote Knabbelmachine");
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

    // =====================================================================================================================
    // the chores of the Guhhuisje (tech-klusjes) and what the other slices put in the world
    // =====================================================================================================================

    /**
     * A real Guhhuisje with one resident that does the chores farmen and machines.
     * <ul>
     *   <li>Peppers (toren-peper was merged after the chore slice branched): the old crop path of farmen takes a ripe
     *       peperplant. On sweet ground it gives pink Snoeppepers, they land in the chest, and the plant stands young again
     *       with its kind and its clock.</li>
     *   <li>A machine in a protected quest building (the practice hall of the Oude Guhrad-centrale holds real Guh Ovens
     *       that nobody placed, and a klus-area reaches 16 blocks): while its spot is protected the residents do not serve it,
     *       the overview does not count it and its bars stay where they are. Once the spot is free the same oven is emptied
     *       like any other.</li>
     * </ul>
     */
    @GuhTest(template = "techklus_test_tuin", batch = "techsamen_klus", timeoutTicks = 1600)
    public static void techsamenKlusguhPepersEnBeschermdeMachines(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        String doos = "techsamen_proef";
        Bescherming.wisDozen(level, doos);
        ServerPlayer speler = KlusjesGameTests.speler(helper, new BlockPos(21, 2, 21));
        ChestBlockEntity kist = KlusjesGameTests.kist(helper);
        // a ripe peperplant on pindasaus-nylium
        PeperplantBlock plant = TorenpeperFeature.PEPERPLANT.get();
        BlockPos peper = new BlockPos(7, 2, 15), peperAbs = helper.absolutePos(peper);
        helper.setBlock(peper.below(), BarbecuetherFeature.PINDASAUS_NYLIUM.get());
        helper.setBlock(peper, plant.getStateForAge(PeperplantBlock.MAX_AGE));
        gelijk(helper, PeperSoort.ROZE, level.getBlockState(peperAbs).getValue(PeperplantBlock.SOORT), "a plant on pindasaus-nylium carries pink peppers");
        // a Guh Oven that nobody placed, with bars lying ready, inside a protected box
        BlockPos ovenPlek = new BlockPos(16, 2, 15), ovenAbs = helper.absolutePos(ovenPlek);
        helper.setBlock(ovenPlek, GuhovenFeature.GUH_OVEN.get());
        helper.setBlock(ovenPlek.east(), VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 5));
        GuhOvenBlockEntity oven = (GuhOvenBlockEntity) level.getBlockEntity(ovenAbs);
        oven.setItem(2, new ItemStack(Items.IRON_INGOT, 5));
        Bescherming.zetDoos(level, doos, new BoundingBox(ovenAbs.getX() - 1, ovenAbs.getY() - 1, ovenAbs.getZ() - 1,
                ovenAbs.getX() + 1, ovenAbs.getY() + 1, ovenAbs.getZ() + 1));
        // a second ripe peperplant INSIDE that box (a show bed of a quest building): no chore of the older kind works there either
        BlockPos binnen = ovenPlek.north(), binnenAbs = helper.absolutePos(binnen);
        helper.setBlock(binnen.below(), BarbecuetherFeature.PINDASAUS_NYLIUM.get());
        helper.setBlock(binnen, plant.getStateForAge(PeperplantBlock.MAX_AGE));
        Huisje h = KlusjesGameTests.huisje(helper, speler);
        helper.assertTrue(Bescherming.beschermd(level, binnenAbs) && h.inGebied(binnenAbs) && h.inGebied(peperAbs), "both plants stand in the home base, one in the box");
        KlusGebied.vergeet();
        List<BlockPos> gewas = KlusGebied.van(level, h, KlusGebied.Soort.GEWAS);
        helper.assertTrue(gewas.contains(peperAbs) && !gewas.contains(binnenAbs), "the scan of the home base leaves the protected plant out: " + gewas);
        helper.assertTrue(Bescherming.beschermd(level, ovenAbs) && !Bescherming.beschermd(level, peperAbs), "the oven stands in a protected box, the plant does not");
        helper.assertTrue(Klusmachines.heeftUitvoer(level, ovenAbs), "bars lie ready in the oven");
        helper.assertTrue(!Klusmachines.mag(level, h, ovenAbs) && Klusmachines.rond(level, h).isEmpty(), "a machine in a protected building is not served");
        KlusGebied.vergeet();
        KlusStand stand = Klusjes.van("machines").stand(level, h);
        gelijk(helper, "NEE:geen:0", stand.staat() + ":" + stand.reden() + ":" + stand.aantal(), "the overview does not count it");
        helper.assertTrue(KlusGebied.rijp(level, peperAbs), "the farmen chore sees the ripe peperplant");
        GuhEntity guh = KlusjesGameTests.bewoner(helper, h, speler, new BlockPos(12, 2, 14), "farmen");
        h.zetKlus(Band.id(guh), "machines", true);
        boolean[] vrij = {false};
        helper.succeedWhen(() -> {
            String waar = KlusjesGameTests.staat(helper, guh);
            int pepers = KlusjesGameTests.telKist(kist, s -> s.is(TorenpeperFeature.SNOEPPEPER.get()));
            if (!vrij[0]) {
                helper.assertTrue(pepers >= PeperplantBlock.PLUK_MIN && pepers <= PeperplantBlock.PLUK_MAX, "2 or 3 Snoeppepers in the chest: " + pepers + waar);
                BlockState inDoos = level.getBlockState(binnenAbs);
                helper.assertTrue(inDoos.is(plant) && inDoos.getValue(PeperplantBlock.AGE) == PeperplantBlock.MAX_AGE,
                        "the ripe plant inside the protected building was left alone: " + inDoos + waar);
                BlockState jong = level.getBlockState(peperAbs);
                helper.assertTrue(jong.is(plant) && jong.getValue(PeperplantBlock.AGE) == 0, "a young plant stands there: " + jong);
                gelijk(helper, PeperSoort.ROZE, jong.getValue(PeperplantBlock.SOORT), "that still knows its sweet ground");
                helper.assertTrue(level.getBlockTicks().hasScheduledTick(peperAbs, plant), "and grows on by itself (its clock runs)");
                gelijk(helper, 0, KlusjesGameTests.telKist(kist, s -> s.is(TorenpeperFeature.NJEGPEPER.get()) || s.is(TorenpeperFeature.VAHOEGPEPER.get())),
                        "no pepper of another kind");
                helper.assertTrue(KlusjesGameTests.telKist(kist, s -> s.is(TorenpeperFeature.PEPERZAADJES.get())) <= 1,
                        "at most one spare seed (one went back into the ground)");
                // all this time the resident had the chore machines too, and the oven kept its bars
                gelijk(helper, 5, oven.getItem(2).getCount(), "the bars in the protected oven were not touched" + waar);
                gelijk(helper, 0, KlusjesGameTests.telKist(kist, s -> s.is(Items.IRON_INGOT)), "no bars in the chest");
                Bescherming.wisDozen(level, doos);
                KlusGebied.vergeet();
                vrij[0] = true;
                helper.assertTrue(!Bescherming.beschermd(level, ovenAbs) && Klusmachines.mag(level, h, ovenAbs), "the same oven outside a protected box is served");
            }
            gelijk(helper, 5, KlusjesGameTests.telKist(kist, s -> s.is(Items.IRON_INGOT)), "now the resident fetched the bars" + waar);
            helper.assertTrue(oven.getItem(2).isEmpty(), "and the oven is empty");
            BlockState nuVrij = level.getBlockState(binnenAbs);
            helper.assertTrue(nuVrij.is(plant) && nuVrij.getValue(PeperplantBlock.AGE) == 0 && pepers >= 2 * PeperplantBlock.PLUK_MIN
                    && pepers <= 2 * PeperplantBlock.PLUK_MAX, "and cut the plant that is no longer protected: " + nuVrij + ", " + pepers + " peppers" + waar);
            KlusjesGameTests.weg(helper, h, speler);
        });
    }

    // =====================================================================================================================
    // phase 3 (fix-tech): the tiers, the faces, the loot, and the seams that were found after the merges
    // =====================================================================================================================

    private static net.minecraft.world.item.crafting.Recipe<?> recept(GameTestHelper helper, String naam) {
        var r = helper.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.RECIPE, nl.juiced.guhs.Guhs.id(naam)));
        helper.assertTrue(r.isPresent(), "the recipe guhs:" + naam + " loads");
        return r.get().value();
    }

    private static boolean vraagt(net.minecraft.world.item.crafting.Recipe<?> recept, Item item) {
        ItemStack stack = new ItemStack(item);
        return recept.placementInfo().ingredients().stream().anyMatch(i -> i.test(stack));
    }

    private static Item item(GameTestHelper helper, String id) {
        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(nl.juiced.guhs.Guhs.id(id));
        helper.assertTrue(item != Items.AIR, "the item guhs:" + id + " exists");
        return item;
    }

    /**
     * ONE table for the four tiers of the Guh-technologie (DESIGN_130 2, "Progression"), on the merged tree: every recipe
     * sits in its tier.
     * <ol>
     * <li>Knutselen: nothing of the Guhbarbecuether in it (no zoutkristal, grillspies, blubroom, grillkool, gloeister, no card).</li>
     * <li>Zout: needs zoutkristal (after chapter 1 of the Knabbelring: the grill portal), no recipe card.</li>
     * <li>Saus: needs its recipe card (the Uitvinder-guh's three after his questline; the Plantagebak the Grillcamping's).</li>
     * <li>Gloeister: needs a gloeister (only the Aangebrande Mika drops one).</li>
     * </ol>
     * The Vadsmolen is not named in DESIGN: as built it is tier 1. The Sausslang is tier 3 in DESIGN and needs a grillspies
     * but no card (the practice hall lends one; it is of no use without a pump or a vat, which do need the card).
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void techsamenElkReceptInZijnLaag(GameTestHelper helper) {
        Item zout = FossielmijnFeature.ZOUTKRISTAL.get(), gloeister = item(helper, "gloeister");
        Item saus = TechquestFeature.RECEPT_SAUS.get(), machines = TechquestFeature.RECEPT_MACHINES.get(), bezorg = TechquestFeature.RECEPT_BEZORG.get();
        Item camping = CampingmarktFeature.RECEPT_PLANTAGEBAK.get();
        List<Item> kaarten = List.of(saus, machines, bezorg, camping);
        List<Item> barbecue = new java.util.ArrayList<>(kaarten);
        barbecue.addAll(List.of(zout, gloeister, item(helper, "grillspies"), item(helper, "blubroom"), item(helper, "grillkool"), item(helper, "houtskoolsteen")));
        // tier 1
        for (String naam : List.of("guh_wheel", "guh_wire", "guh_oven", "knuffelgenerator", "disco_dynamo", "vadsmolen")) {
            var r = recept(helper, naam);
            for (Item b : barbecue) {
                helper.assertTrue(!vraagt(r, b), "tier 1 (Knutselen): guhs:" + naam + " needs nothing of the Guhbarbecuether, but asks for " + b);
            }
        }
        // tier 2
        for (String naam : List.of("knabbelbuis", "knabbelbuis_richting", "knabbelbuis_filter", "hapluikje", "bank_sleutel", "opzuiger", "oogster",
                "voorraadmeter", "snuffelsensor", "guhklok", "guhteller", "knabbelbatterij")) {
            var r = recept(helper, naam);
            helper.assertTrue(vraagt(r, zout), "tier 2 (Zout): guhs:" + naam + " needs zoutkristal");
            helper.assertTrue(!vraagt(r, gloeister) && kaarten.stream().noneMatch(k -> vraagt(r, k)), "tier 2 (Zout): guhs:" + naam + " needs no card and no gloeister");
        }
        // tier 3
        java.util.Map<String, Item> laag3 = new java.util.LinkedHashMap<>();
        for (String naam : List.of("sauspomp", "sausvat", "brouwautomaat", "frituurautomaat", "grillkoolpers", "blubkacheltje")) {
            laag3.put(naam, saus);
        }
        for (String naam : List.of("knabbelaar", "neerzetter", "knutselmachine", "tekentafel")) {
            laag3.put(naam, machines);
        }
        laag3.put("stepstation", bezorg);
        laag3.put("haltepaaltje", bezorg);
        laag3.put("plantagebak", camping);
        gelijk(helper, 13, laag3.size(), "the twelve recipes with a card of the Uitvinder-guh and the Plantagebak");
        for (var e : laag3.entrySet()) {
            var r = recept(helper, e.getKey());
            for (Item k : kaarten) {
                gelijk(helper, k == e.getValue(), vraagt(r, k), "tier 3 (Saus): guhs:" + e.getKey() + " and the card " + k);
            }
            helper.assertTrue(!vraagt(r, gloeister), "tier 3 (Saus): guhs:" + e.getKey() + " needs no gloeister");
            ItemStack kaart = new ItemStack(e.getValue());
            helper.assertTrue(kaart.getItem().getCraftingRemainder(kaart) != null, "the card " + e.getValue() + " stays in the grid");
        }
        var slang = recept(helper, "sausslang");
        helper.assertTrue(vraagt(slang, item(helper, "grillspies")) && kaarten.stream().noneMatch(k -> vraagt(slang, k)),
                "the Sausslang: a grillspies of the Guhbarbecuether, no card (as built)");
        // tier 4
        var kern = recept(helper, "gloeisterkern");
        helper.assertTrue(vraagt(kern, gloeister) && vraagt(kern, zout), "tier 4 (Gloeister): the Gloeisterkern needs a gloeister");
        // the cards and the gloeister come from nowhere else: no recipe makes them, no chest of a building holds them
        var recepten = helper.getLevel().getServer().getRecipeManager();
        var context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(helper.getLevel());
        int bekeken = 0;
        for (var houder : recepten.getRecipes()) {
            if (!houder.id().identifier().getNamespace().equals(nl.juiced.guhs.Guhs.MODID)) {
                continue;
            }
            bekeken++;
            List<ItemStack> uit = houder.value().display().stream().flatMap(d -> d.result().resolveForStacks(context).stream()).toList();
            for (Item gesloten : List.of(saus, machines, bezorg, camping, gloeister)) {
                helper.assertTrue(uit.stream().noneMatch(st -> st.is(gesloten)), "no recipe makes " + gesloten + ": " + houder.id().identifier());
            }
        }
        helper.assertTrue(bekeken > 100, "(the recipes of the mod were looked at: " + bekeken + ")");
        // the questline of tier 4 behind the Aangebrande Mika: TechquestGameTests (techquestGroteKnabbelmachine, techquestTweedeSpelerNaDeEerste)
        helper.succeed();
    }

    /** The models a blockstate file uses for snoet=&lt;state&gt; (variants, or multipart with a plain / AND / OR condition). */
    private static java.util.Set<String> modellen(com.google.gson.JsonObject bs, String snoet) {
        java.util.Set<String> uit = new java.util.TreeSet<>();
        if (bs.has("variants")) {
            for (var e : bs.getAsJsonObject("variants").entrySet()) {
                boolean past = true;
                for (String deel : e.getKey().split(",")) {
                    if (deel.startsWith("snoet=") && !deel.equals("snoet=" + snoet)) {
                        past = false;
                    }
                }
                if (past) {
                    voegModellen(uit, e.getValue());
                }
            }
        }
        if (bs.has("multipart")) {
            for (var deel : bs.getAsJsonArray("multipart")) {
                com.google.gson.JsonObject o = deel.getAsJsonObject();
                if (!o.has("when") || past(o.get("when"), snoet)) {
                    voegModellen(uit, o.get("apply"));
                }
            }
        }
        return uit;
    }

    private static boolean past(com.google.gson.JsonElement when, String snoet) {
        com.google.gson.JsonObject o = when.getAsJsonObject();
        for (String groep : List.of("AND", "OR")) {
            if (o.has(groep)) {
                boolean alle = true, een = false;
                for (var sub : o.getAsJsonArray(groep)) {
                    boolean p = past(sub, snoet);
                    alle &= p;
                    een |= p;
                }
                return groep.equals("AND") ? alle : een;
            }
        }
        return !o.has("snoet") || List.of(o.get("snoet").getAsString().split("\\|")).contains(snoet);
    }

    private static void voegModellen(java.util.Set<String> uit, com.google.gson.JsonElement apply) {
        if (apply.isJsonArray()) {
            apply.getAsJsonArray().forEach(e -> uit.add(e.getAsJsonObject().get("model").getAsString()));
        } else {
            uit.add(apply.getAsJsonObject().get("model").getAsString());
        }
    }

    private static com.google.gson.JsonObject bron(GameTestHelper helper, String pad) {
        try (var in = TechSamenGameTests.class.getResourceAsStream(pad)) {
            helper.assertTrue(in != null, "the resource " + pad + " exists");
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * EVERY machine has a snoet with three faces (DESIGN_130 2 "Look"): asleep without vadskracht, happy at work, surprised
     * when full. For each of them: the block has the property, its blockstate file tells the three faces apart (three
     * different sets of models, every model file there), and a machine that uses vadskracht really wakes up when its net
     * runs and falls asleep again when the source is gone. The sources sleep when they give nothing or do not count
     * (TechbronGameTests); the Guhrad's face is the guh that runs in it. No face on purpose: Knabbelbuis, Richtingstuk,
     * Sausslang, Sausvat, Haltepaaltje, Knabbelbatterij (asserted too, so a face that is added there is noticed).
     */
    @GuhTest(template = KAMER, batch = "techsamen_snoet", timeoutTicks = 400)
    public static void techsamenElkeMachineHeeftDrieSnoeten(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<net.neoforged.neoforge.registries.DeferredBlock<? extends net.minecraft.world.level.block.Block>> verbruikers = List.of(
                GuhovenFeature.GUH_OVEN, TechmachineFeature.VADSMOLEN, TechmachineFeature.OOGSTER, TechmachineFeature.KNABBELAAR, TechmachineFeature.NEERZETTER,
                TechmachineFeature.KNUTSELMACHINE, TechmachineFeature.PLANTAGEBAK, TechsausFeature.SAUSPOMP, TechsausFeature.BROUWAUTOMAAT,
                TechsausFeature.FRITUURAUTOMAAT, TechsausFeature.GRILLKOOLPERS, TechbuisFeature.KNABBELBUIS_FILTER, TechbuisFeature.OPZUIGER,
                TechbuisFeature.VOORRAADMETER, TechbuisFeature.SNUFFELSENSOR, TechbuisFeature.GUHKLOK, TechbuisFeature.GUHTELLER,
                TechbezorgFeature.STEPSTATION, BankFeature.HAPLUIKJE);
        List<net.neoforged.neoforge.registries.DeferredBlock<? extends net.minecraft.world.level.block.Block>> bronnen = List.of(
                TechbronFeature.KNUFFELGENERATOR, TechbronFeature.DISCO_DYNAMO, TechbronFeature.BLUBKACHELTJE, TechbronFeature.GLOEISTERKERN);
        List<net.neoforged.neoforge.registries.DeferredBlock<? extends net.minecraft.world.level.block.Block>> alle = new java.util.ArrayList<>(verbruikers);
        alle.addAll(bronnen);
        alle.add(TechmachineFeature.TEKENTAFEL);
        for (var blok : alle) {
            String id = blok.getId().getPath();
            helper.assertTrue(blok.get().defaultBlockState().hasProperty(MachineBlock.SNOET), id + " has a snoet");
            gelijk(helper, nl.juiced.guhs.feature.vadskracht.Snoet.SLAAPT, blok.get().defaultBlockState().getValue(MachineBlock.SNOET), id + " is put down asleep");
            com.google.gson.JsonObject bs = bron(helper, "/assets/guhs/blockstates/" + id + ".json");
            java.util.Set<String> slaapt = modellen(bs, "slaapt"), werkt = modellen(bs, "werkt"), vol = modellen(bs, "vol");
            helper.assertTrue(!slaapt.isEmpty() && !werkt.isEmpty() && !vol.isEmpty(), id + ": a model for each face");
            helper.assertTrue(!slaapt.equals(werkt) && !werkt.equals(vol) && !slaapt.equals(vol),
                    id + ": three different faces in its blockstate: " + slaapt + " / " + werkt + " / " + vol);
            for (String model : java.util.stream.Stream.of(slaapt, werkt, vol).flatMap(java.util.Set::stream).toList()) {
                helper.assertTrue(model.startsWith("guhs:"), id + ": its own model " + model);
                bron(helper, "/assets/guhs/models/" + model.substring(5) + ".json");
            }
        }
        for (var blok : List.of(TechbuisFeature.KNABBELBUIS, TechbuisFeature.KNABBELBUIS_RICHTING, TechsausFeature.SAUSSLANG, TechsausFeature.SAUSVAT,
                TechbezorgFeature.HALTEPAALTJE, TechbronFeature.KNABBELBATTERIJ)) {
            helper.assertTrue(!blok.get().defaultBlockState().hasProperty(MachineBlock.SNOET), blok.getId().getPath() + " has no face (on purpose)");
        }
        // the users of vadskracht: each on a source of its own (under it), three blocks apart
        List<BlockPos> plekken = new java.util.ArrayList<>();
        for (int i = 0; i < verbruikers.size(); i++) {
            BlockPos plek = p(1 + 3 * (i % 5), 1 + 3 * (i / 5));
            plekken.add(plek);
            helper.setBlock(plek.below(), VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 2));
            helper.setBlock(plek, verbruikers.get(i).get());
        }
        boolean[] uit = {false};
        helper.succeedWhen(() -> {
            if (!uit[0]) {
                for (int i = 0; i < plekken.size(); i++) {
                    helper.assertTrue(helper.getBlockState(plekken.get(i)).getValue(MachineBlock.SNOET) != nl.juiced.guhs.feature.vadskracht.Snoet.SLAAPT,
                            verbruikers.get(i).getId().getPath() + " wakes up when its net runs: " + helper.getBlockState(plekken.get(i)));
                }
                for (BlockPos plek : plekken) {
                    helper.setBlock(plek.below(), Blocks.STONE);
                }
                uit[0] = true;
            }
            for (int i = 0; i < plekken.size(); i++) {
                gelijk(helper, nl.juiced.guhs.feature.vadskracht.Snoet.SLAAPT, helper.getBlockState(plekken.get(i)).getValue(MachineBlock.SNOET),
                        verbruikers.get(i).getId().getPath() + " falls asleep without vadskracht");
            }
            for (BlockPos plek : plekken) {
                helper.setBlock(plek, Blocks.AIR);
            }
            level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(p(0, 0)))
                    .expandTowards(15, 5, 15), e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(net.minecraft.world.entity.Entity::discard);
        });
    }

    /**
     * Every block of the tech slices that a player can hold drops something when it is broken (the placeholder rows that
     * once gave every fixed id a loot table are gone: each owner writes its own). Not asked of a part block or a test block
     * (no item of its own) nor of a block that cannot be broken (De Grote Knabbelmachine).
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void techsamenElkBlokHeeftZijnBuit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        List<net.neoforged.neoforge.registries.DeferredRegister.Blocks> registers = List.of(BankFeature.BLOCKS, TechbronFeature.BLOCKS, TechbuisFeature.BLOCKS,
                TechmachineFeature.BLOCKS, TechsausFeature.BLOCKS, TechbezorgFeature.BLOCKS, TechquestFeature.BLOCKS, VadskrachtFeature.BLOCKS, GuhovenFeature.BLOCKS);
        List<net.minecraft.world.level.block.Block> blokken = new java.util.ArrayList<>();
        registers.forEach(r -> r.getEntries().forEach(e -> blokken.add(e.get())));
        blokken.addAll(List.of(ModBlocks.BANK_GUH.get(), ModBlocks.GUH_WHEEL.get(), ModBlocks.GUH_WIRE.get()));
        int metItem = 0;
        List<String> zonder = new java.util.ArrayList<>();
        for (var blok : blokken) {
            String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blok).getPath();
            if (blok.asItem() == Items.AIR) {
                zonder.add(id);
                continue;
            }
            metItem++;
            if (blok.defaultDestroyTime() < 0) {
                helper.assertTrue(blok == TechquestFeature.GROTE_KNABBELMACHINE.get(), "only De Grote Knabbelmachine cannot be broken, not " + id);
                continue;
            }
            helper.assertTrue(blok.getLootTable().isPresent(), id + " has a loot table");
            List<ItemStack> buit = net.minecraft.world.level.block.Block.getDrops(blok.defaultBlockState(), level, pos, null);
            helper.assertTrue(buit.stream().anyMatch(st -> !st.isEmpty()), id + " drops something when it is broken (its own loot table)");
            helper.assertTrue(buit.stream().anyMatch(st -> st.is(blok.asItem())), id + " drops itself: " + buit);
        }
        helper.assertTrue(metItem >= 30, "all the blocks of the tech slices were looked at: " + metItem);
        for (String id : zonder) {
            helper.assertTrue(id.endsWith("_deel") || id.startsWith("vadskracht_test") || id.equals("machine_deel"), "a block without an item is a part or a test block: " + id);
        }
        helper.succeed();
    }

    /**
     * An "ophalen" pole at an upgraded Bank Guh that holds hundreds of kinds: a look walks at most
     * {@link StepstationBlockEntity#MAX_ZOEK} slots (it walked all of them, and asked every "afleveren" stop about each)
     * and goes on where it stopped. Since the user's decision B6 a pole gets nothing out of a bank, so the kind the other
     * stop asks for, at the very end of the bank, is LOOKED at (the walk comes round) and stays where it is.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 3000)
    public static void techsamenBezorgguhtjeBijEenBankMetHonderdenSoorten(GameTestHelper helper) {
        BankGuhBlockEntity bank = bank(helper, p(2, 3));
        int soorten = 0;
        for (Item soort : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (soort == Items.AIR || soort == knabbel() || Features.isLoaned(new ItemStack(soort))) {
                continue;
            }
            bank.getStorage().insert(new ItemStack(soort), 3);
            if (++soorten == 300) {
                break;
            }
        }
        gelijk(helper, 300, soorten, "(three hundred kinds of item exist)");
        gelijk(helper, 20L, bank.getStorage().insert(new ItemStack(knabbel()), 20), "20 knabbels go in last: the last slot of the bank");
        bank.getStorage().setUpgraded(true);
        gelijk(helper, 302, bank.handler().size(), "301 kinds and one free slot");
        StepstationBlockEntity station = station(helper, p(7, 7));
        ChestBlockEntity b = kist(helper, p(12, 3));
        paal(helper, p(2, 4), true, station);
        HaltepaaltjeBlockEntity paalB = paal(helper, p(12, 4), false, station);
        paalB.zetFilter(0, new ItemStack(knabbel()));
        HaltepaaltjeBlockEntity paalA = (HaltepaaltjeBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(p(2, 4)));
        int[] meeste = {0};
        java.util.Set<Integer> begonnen = new java.util.TreeSet<>();
        helper.onEachTick(() -> {
            meeste[0] = Math.max(meeste[0], station.bekeken());
            begonnen.add(paalA.zoekVan());
        });
        helper.succeedWhen(() -> {
            // the walk came all the way round the 302 slots: six stretches of 54, each begun where the one before ended, and
            // the LAST one (from slot 270, the knabbels in it) was walked too and gave nothing: the next one begins past the end
            helper.assertTrue(begonnen.containsAll(List.of(0, 54, 108, 162, 216, 270, 324)),
                    "the looks walked on through the whole bank, a stretch at a time: " + begonnen);
            gelijk(helper, 0, b.countItem(knabbel()), "nothing comes out of the bank for a pole, upgraded or not");
            gelijk(helper, 20L, bank.getStorage().count(new ItemStack(knabbel())), "the knabbels at the end of the bank stay");
            gelijk(helper, 301, bank.getStorage().snapshot().entries().size(), "every kind stays in the bank");
            helper.assertTrue(station.rugzakLeeg(), "the backpack is empty");
            helper.assertTrue(meeste[0] > 0 && meeste[0] <= StepstationBlockEntity.MAX_ZOEK, "no look walked more than " + StepstationBlockEntity.MAX_ZOEK
                    + " slots of the bank: " + meeste[0]);
        });
    }

    /**
     * A sneaking player with a Sausblubje jar clicks a Blubkacheltje: the blubje goes INTO the stove (vanilla skips the
     * block for a sneaking player with an item, and the jar then let the blubje out next to the stove).
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void techsamenBlubjeInDeKachelOokAlsJeSluipt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer speler = GuhMockPlayer.of(helper);
        speler.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        speler.getInventory().clearContent();
        BlockPos kachel = p(7, 7), abs = helper.absolutePos(kachel);
        speler.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 2.5);
        helper.setBlock(kachel, TechbronFeature.BLUBKACHELTJE.get());
        var be = (nl.juiced.guhs.feature.techbron.BlubkacheltjeBlockEntity) level.getBlockEntity(abs);
        var klik = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs), Direction.SOUTH, abs, false);
        try {
            speler.setShiftKeyDown(true);
            speler.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(SausdierenFeature.SAUSBLUBJE_POTJE.get()));
            var uitkomst = speler.gameMode.useItemOn(speler, level, speler.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, klik);
            helper.assertTrue(uitkomst.consumesAction() && be.heeftBlubje() && speler.getMainHandItem().isEmpty(), "sneaking: the blubje is in the stove: " + uitkomst);
            gelijk(helper, 0, level.getEntitiesOfClass(nl.juiced.guhs.feature.sausdieren.SausblubjeEntity.class,
                    new net.minecraft.world.phys.AABB(abs).inflate(6)).size(), "and no blubje hops around next to it");
            // sneaking with food: into the bakje
            speler.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(knabbel(), 3));
            speler.gameMode.useItemOn(speler, level, speler.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, klik);
            helper.assertTrue(speler.getMainHandItem().isEmpty() && be.voorraad() + (be.warm() ? 1 : 0) == 3, "sneaking with knabbels feeds it: " + be.voorraad());
            // a jar on any other block still lets the blubje out
            speler.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(SausdierenFeature.SAUSBLUBJE_POTJE.get()));
            BlockPos vloer = helper.absolutePos(new BlockPos(3, 1, 3));
            speler.gameMode.useItemOn(speler, level, speler.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(vloer), Direction.UP, vloer, false));
            var los = level.getEntitiesOfClass(nl.juiced.guhs.feature.sausdieren.SausblubjeEntity.class, new net.minecraft.world.phys.AABB(abs).inflate(8));
            gelijk(helper, 1, los.size(), "on the floor the jar lets its blubje out as before");
            los.forEach(net.minecraft.world.entity.Entity::discard);
        } finally {
            helper.setBlock(kachel, Blocks.AIR);
            level.removePlayerImmediately(speler, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(abs).inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    /**
     * A Haltepaaltje only serves a block its owner may touch (like the machines that change the world): not the chest of a
     * protected quest building. And a pole a Neerzetter puts down belongs to the Neerzetter's owner.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techsamenHaltepaaltjeVraagtBescherming(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        String doos = "techsamen_halte";
        Bescherming.wisDozen(level, doos);
        StepstationBlockEntity station = station(helper, p(7, 7));
        ChestBlockEntity kist = kist(helper, p(2, 3));
        kist.setItem(0, kei(5));
        HaltepaaltjeBlockEntity halte = paal(helper, p(2, 4), true, station);
        helper.assertTrue(halte.mag() && halte.uitKant() != null && halte.inKant() != null && halte.heeftKist(), "a pole at a chest in the open serves it");
        BlockPos kistAbs = helper.absolutePos(p(2, 3));
        Bescherming.zetDoos(level, doos, new BoundingBox(kistAbs.getX(), kistAbs.getY(), kistAbs.getZ(), kistAbs.getX(), kistAbs.getY(), kistAbs.getZ()));
        java.util.UUID baas = java.util.UUID.randomUUID();
        halte.zetEigenaar(baas);   // (asks again)
        helper.assertTrue(Bescherming.beschermd(level, kistAbs) && !halte.mag(), "the chest of a protected building: the pole may not");
        helper.assertTrue(halte.uitKant() == null && halte.inKant() == null && !halte.heeftKist(), "so it serves nothing there");
        Bescherming.wisDozen(level, doos);
        halte.zetEigenaar(baas);
        helper.assertTrue(halte.mag() && halte.uitKant() != null, "the protection gone: it serves the chest again");
        // a Neerzetter of that owner puts down a pole
        BlockPos zetter = p(11, 11);
        helper.setBlock(zetter.south(), VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 2));
        helper.setBlock(zetter, TechmachineFeature.NEERZETTER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        var neerzetter = (nl.juiced.guhs.feature.techmachine.NeerzetterBlockEntity) level.getBlockEntity(helper.absolutePos(zetter));
        neerzetter.zetEigenaar(baas);
        neerzetter.vakken().set(0, ItemResource.of(TechbezorgFeature.HALTEPAALTJE.get().asItem()), 1);
        helper.succeedWhen(() -> helper.assertTrue(level.getBlockEntity(helper.absolutePos(zetter.north())) instanceof HaltepaaltjeBlockEntity nieuw
                && baas.equals(nieuw.eigenaar()), "the pole the Neerzetter placed belongs to the Neerzetter's owner"));
    }
}
