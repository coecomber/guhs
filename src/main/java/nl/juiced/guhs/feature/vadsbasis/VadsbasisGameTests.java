package nl.juiced.guhs.feature.vadsbasis;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature;
import nl.juiced.guhs.feature.techsaus.KnuffelsteenGeneratorBlockEntity;
import nl.juiced.guhs.feature.techsaus.SausGetallen;
import nl.juiced.guhs.feature.techsaus.SauspompBlockEntity;
import nl.juiced.guhs.feature.techsaus.SaustankBlockEntity;
import nl.juiced.guhs.feature.techsaus.TechsausFeature;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.Sauzen;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.feature.weer.WeerFeature;
import nl.juiced.guhs.feature.weer.WeerVadserBlockEntity;
import nl.juiced.guhs.feature.weer.WeerZones;
import nl.juiced.guhs.feature.weer.Weertje;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.taal.NlTekst;

/**
 * Game tests of the slice vadsbasis (1.8.0 vadskracht, batch "vadsbasis"): the Knabbelketel, the Wolkenantennes on a Weer
 * vadser, the Knuffelsteen-generator and the Sausbuis + Saustank. The afnemende opbrengst of the Guhraden is tested in
 * VadskrachtGameTests (vadskrachtAfnemendeGuhraden). Template techsaus_test_kamer: the top layer of the floor is helper y 2,
 * things stand at helper y 3.
 */
public class VadsbasisGameTests {
    private static final String BATCH = "vadsbasis", KAMER = "techsaus_test_kamer";

    private static BlockPos p(int x, int z) {
        return new BlockPos(x, 3, z);
    }

    private static <T> T zet(GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.Block block, Class<T> soort) {
        helper.setBlock(pos, block);
        return soort.cast(helper.getLevel().getBlockEntity(helper.absolutePos(pos)));
    }

    /** A test source of kracht x 10 vadskracht. */
    private static void kracht(GameTestHelper helper, BlockPos pos, int kracht) {
        helper.setBlock(pos, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, kracht));
    }

    private static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    private static List<String> lees(GameTestHelper helper, BlockPos pos) {
        return VadsKracht.regels(helper.getLevel(), helper.absolutePos(pos)).stream().map(NlTekst::tekst).toList();
    }

    /** The Knabbelketel burns knabbels (a block nine times as long), gives 80 VK while it burns, takes only fuel from pipes. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void vadsbasisKnabbelketelStookt(GameTestHelper helper) {
        KnabbelketelBlockEntity ketel = zet(helper, p(3, 3), VadsbasisFeature.KNABBELKETEL.get(), KnabbelketelBlockEntity.class);
        gelijk(helper, 0, ketel.vadsAanbod(), "a cold boiler gives nothing");
        ItemStack knabbels = new ItemStack(ModItems.KAAS_KNABBELS.get(), 3);
        gelijk(helper, 3, ketel.voer(knabbels, false), "three knabbels go in");
        helper.assertTrue(knabbels.isEmpty(), "and leave the hand");
        gelijk(helper, 0, ketel.voer(new ItemStack(Items.DIRT), false), "no dirt");
        ItemStack rest = Kisten.stop(ketel.handler(null), new ItemStack(Items.COBBLESTONE, 4));
        gelijk(helper, 4, rest.getCount(), "a pipe cannot put stone in");
        helper.assertTrue(Kisten.neem(ketel.handler(null), s -> true, 64).isEmpty(), "nothing comes out of the boiler");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ketel.brandt(), "it lights up"))
                .thenExecute(() -> {
                    gelijk(helper, VadsGetallen.KNABBELKETEL, ketel.vadsAanbod(), "80 VK while it burns");
                    gelijk(helper, 2, ketel.voorraad(), "it ate one knabbel");
                    helper.assertTrue(ketel.brandTicks() <= VadsGetallen.KETEL_KNABBEL_TIKKEN, "one knabbel burns " + VadsGetallen.KETEL_KNABBEL_TIKKEN + " ticks");
                    helper.assertTrue(lees(helper, p(3, 3)).contains("Dit geeft 80 vadskracht"), "readout: " + lees(helper, p(3, 3)));
                    helper.assertTrue(VadsGetallen.KETEL_KNABBELS_PER_UUR >= 200 && VadsGetallen.KETEL_KNABBELS_PER_UUR <= 1000,
                            "a few hundred to a thousand knabbels an hour: " + VadsGetallen.KETEL_KNABBELS_PER_UUR);
                    ketel.zetBrandt(1);      // this knabbel is done: the next one
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    gelijk(helper, 1, ketel.voorraad(), "the next knabbel");
                    gelijk(helper, VadsGetallen.KETEL_KNABBEL_TIKKEN - 1, ketel.brandTicks(), "a fresh knabbel");
                    // empty the bakje, then a block of kaasknabbels: nine times as long
                    ketel.zetBrandt(1);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    ketel.zetBrandt(1);
                    gelijk(helper, 1, ketel.voer(new ItemStack(ModBlocks.BLOCK_OF_KAASKNABBELS.get()), false), "a block goes in");
                    gelijk(helper, VadsGetallen.KETEL_BLOK, ketel.voorraad(), "a block counts as nine");
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(ketel.brandTicks() > VadsGetallen.KETEL_KNABBEL_TIKKEN * (VadsGetallen.KETEL_BLOK - 1),
                            "a block burns nine times as long: " + ketel.brandTicks());
                    gelijk(helper, 0, ketel.voorraad(), "the bakje is empty");
                })
                .thenSucceed();
    }

    /** A Weer vadser reaches 10 blocks; Wolkenantennes make it 20, 35, 60 and it asks more; one next to it counts too. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 60)
    public static void vadsbasisWolkenantenneStraal(GameTestHelper helper) {
        WeerVadserBlockEntity vadser = zet(helper, p(3, 3), WeerFeature.WEER_VADSER.get(), WeerVadserBlockEntity.class);
        vadser.zet(Weertje.ZON);
        gelijk(helper, 10, vadser.straal(), "standard radius");
        gelijk(helper, WeerFeature.KRACHT, vadser.vadsVraag(), "standard cost");
        helper.setBlock(p(4, 3), WeerFeature.ANTENNES.get(0).get());
        gelijk(helper, 20, vadser.straal(), "an antenna I next to it");
        gelijk(helper, 30, vadser.vadsVraag(), "costs more");
        helper.setBlock(p(4, 3), Blocks.AIR);
        ServerPlayer speler = GuhMockPlayer.of(helper);
        speler.setGameMode(GameType.SURVIVAL);
        speler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WeerFeature.ANTENNE_ITEMS.get(1).get()));
        helper.useBlock(p(3, 3), speler);
        gelijk(helper, 0, vadser.niveau(), "II before I is refused");
        speler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WeerFeature.ANTENNE_ITEMS.get(0).get()));
        helper.useBlock(p(3, 3), speler);
        gelijk(helper, 1, vadser.niveau(), "I goes in");
        helper.assertTrue(speler.getMainHandItem().isEmpty(), "the antenna left the hand");
        vadser.zetAntenne(2);
        gelijk(helper, 35, vadser.straal(), "antenna II");
        gelijk(helper, 45, vadser.vadsVraag(), "antenna II costs");
        vadser.zetAntenne(3);
        gelijk(helper, 60, vadser.straal(), "antenna III");
        gelijk(helper, 70, vadser.vadsVraag(), "antenna III costs");
        gelijk(helper, 60, WeerZones.straal(3), "the zones agree");
        helper.getLevel().removePlayerImmediately(speler, Entity.RemovalReason.DISCARDED);
        vadser.zet(Weertje.NORMAAL);
        helper.succeed();
    }

    /** Kaassaus + a water source next to it + vadskracht = knuffelsteen, into the chest next to it; the source stays. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 20 * SausGetallen.GEN_TIKKEN + 200)
    public static void vadsbasisGeneratorMaaktKnuffelsteen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KnuffelsteenGeneratorBlockEntity gen = zet(helper, p(3, 3), TechsausFeature.KNUFFELSTEEN_GENERATOR.get(), KnuffelsteenGeneratorBlockEntity.class);
        helper.setBlock(p(2, 3), Blocks.WATER);
        ChestBlockEntity kist = zet(helper, p(4, 3), Blocks.CHEST, ChestBlockEntity.class);
        gen.saus().zet(Sauzen.kaassaus(), 4 * SausGetallen.GEN_SAUS);
        kracht(helper, p(3, 2), 1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(kist.countItem(KnuffeldalFeature.KNUFFELSTEEN.get().asItem()) >= 4,
                        "knuffelsteen in the chest: " + kist.countItem(KnuffeldalFeature.KNUFFELSTEEN.get().asItem())))
                .thenExecute(() -> {
                    gelijk(helper, 4L, gen.gemaakt(), "four made");
                    helper.assertTrue(gen.saus().isLeeg(), "the sauce is used up");
                    gelijk(helper, 0, gen.water().inhoud(), "the water came from the source");
                    helper.assertTrue(level.getFluidState(helper.absolutePos(p(2, 3))).isSource(), "the source is still there");
                })
                .thenIdle(3 * SausGetallen.GEN_TIKKEN)
                .thenExecute(() -> gelijk(helper, 4L, gen.gemaakt(), "no sauce, no stone"))
                .thenSucceed();
    }

    /** Pump - three Sausbuizen - Saustank: the tank fills; it holds 64 buckets; the pipe counts 1/16 of a hose. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void vadsbasisBuisVultTank(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.KAAS_SAUS.get());
        SauspompBlockEntity pomp = zet(helper, p(2, 2), TechsausFeature.SAUSPOMP.get(), SauspompBlockEntity.class);
        for (int x = 3; x <= 5; x++) {
            helper.setBlock(p(x, 2), TechsausFeature.SAUSBUIS.get());
        }
        SaustankBlockEntity tank = zet(helper, p(6, 2), TechsausFeature.SAUSTANK.get(), SaustankBlockEntity.class);
        kracht(helper, p(2, 1), 1);
        gelijk(helper, SausGetallen.TANK, tank.tank().max(), "64 buckets");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(tank.tank().inhoud() >= Sauzen.EMMER, "the tank fills: " + tank.tank().inhoud()))
                .thenExecute(() -> {
                    gelijk(helper, Sauzen.kaassaus(), tank.tank().saus(), "kaassaus");
                    helper.assertTrue(pomp.heeftKracht(), "the pump runs");
                })
                .thenSucceed();
    }
}
