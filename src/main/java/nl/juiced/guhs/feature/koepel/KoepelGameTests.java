package nl.juiced.guhs.feature.koepel;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.feature.weer.WeerFeature;
import nl.juiced.guhs.feature.weer.WeerVadserBlockEntity;
import nl.juiced.guhs.feature.weer.WeerZones;
import nl.juiced.guhs.feature.weer.Weertje;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of de Koepel (1.8.0 vadskracht, batch "koepel"): the Mega-oven bakes a batch in bulk, the Knabbel-weerstation
 * widens a Weer vadser to 150 blocks, the Sterrenkoepel lights its sterrenbeelden (and stops at a full sky), the Draaimolen
 * makes tame guhs happy, and the Wolkenvanger catches faster the higher it stands.
 */
public class KoepelGameTests {
    private static final String BATCH = "koepel", KAMER = "vadskracht_test_kamer";

    private static void bron(GameTestHelper helper, BlockPos plek, int kracht) {
        helper.setBlock(plek, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, kracht));
    }

    /** One batch of pink buns: 12 roze graan in, 6 roze broodjes out, on 24 vadskracht. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void megaOvenBaktInBulk(GameTestHelper helper) {
        BlockPos oven = new BlockPos(4, 2, 2);
        bron(helper, new BlockPos(4, 2, 1), 3);
        helper.setBlock(oven, KoepelFeature.blok(KoepelBlock.Soort.MEGA_OVEN));
        MegaOvenBlockEntity be = (MegaOvenBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(oven));
        helper.assertTrue(be != null, "the Mega-oven has its block entity");
        be.zetProgramma(0);
        be.vakken().set(0, ItemResource.of(new ItemStack(KoepelVakken.guhs("roze_graan"))), 12);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(KoepelVakken.tel(be.vakken(), KoepelVakken.guhs("roze_broodje"), MegaOvenBlockEntity.IN,
                        MegaOvenBlockEntity.ALLES) == 6, "6 roze broodjes baked"))
                .thenExecute(() -> helper.assertTrue(KoepelVakken.tel(be.vakken(), KoepelVakken.guhs("roze_graan"), 0, MegaOvenBlockEntity.IN) == 0,
                        "the 12 graan are used up"))
                .thenSucceed();
    }

    /** A running Knabbel-weerstation next to a Weer vadser: its zone is 150 blocks wide. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void weerstationVergrootDeZone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos bron = new BlockPos(1, 2, 1), vadser = new BlockPos(2, 2, 1), station = new BlockPos(3, 2, 1);
        bron(helper, bron, 5);
        helper.setBlock(vadser, WeerFeature.WEER_VADSER.get());
        helper.setBlock(station, KoepelFeature.blok(KoepelBlock.Soort.WEERSTATION));
        WeerVadserBlockEntity v = (WeerVadserBlockEntity) level.getBlockEntity(helper.absolutePos(vadser));
        WeerstationBlockEntity s = (WeerstationBlockEntity) level.getBlockEntity(helper.absolutePos(station));
        v.zet(Weertje.HARTJES);
        BlockPos abs = helper.absolutePos(vadser);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(WeerZones.weerOp(level, abs) == Weertje.HARTJES, "the Weer vadser runs"))
                .thenExecute(() -> helper.assertTrue(WeerZones.weerOp(level, abs.offset(100, 0, 0)) == Weertje.NORMAAL, "without a station: not 100 away"))
                .thenExecute(() -> s.vakken().set(0, ItemResource.of(new ItemStack(KoepelVakken.guhs("kaas_knabbels"))), 3))
                .thenWaitUntil(() -> helper.assertTrue(WeerZones.weerOp(level, abs.offset(100, 0, 0)) == Weertje.HARTJES,
                        "with a fed weerstation: 100 blocks away is still the zone"))
                .thenExecute(() -> helper.assertTrue(WeerZones.zones(level).stream().anyMatch(z -> z.straal() == KoepelGetallen.WEERSTATION_STRAAL),
                        "the zone has the station's radius"))
                .thenExecute(() -> v.zet(Weertje.NORMAAL))
                .thenSucceed();
    }

    /** The Sterrenkoepel lights one sterrenbeeld per level, keeps who gave, and stops at a full sky of twelve. */
    @GuhTest(template = KAMER, batch = BATCH)
    @SuppressWarnings("removal")
    public static void sterrenkoepelLichtOp(GameTestHelper helper) {
        BlockPos plek = new BlockPos(4, 2, 2);
        helper.setBlock(plek, KoepelFeature.blok(KoepelBlock.Soort.STERRENKOEPEL));
        SterrenkoepelBlockEntity k = (SterrenkoepelBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(plek));
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(k.niveau() == 0 && k.nogNodig() == KoepelGetallen.kostVoor(1), "starts dark");
        helper.assertTrue(k.voer(p, KoepelGetallen.kostVoor(1) - 1) == 0 && k.niveau() == 0, "one knabbel short: still dark");
        helper.assertTrue(k.voer(p, 1) == 1 && k.niveau() == 1, "the first sterrenbeeld lights up");
        k.voer(p, 1_000_000);
        helper.assertTrue(k.niveau() == KoepelGetallen.STERRENBEELDEN && k.nogNodig() == 0, "a whole sky, no more");
        helper.assertTrue(k.scorebord().size() == 1 && k.scorebord().get(0).getValue() >= KoepelGetallen.kostVoor(1), "the giver is on the scoreboard");
        ItemStack blok = new ItemStack(KoepelVakken.guhs("block_of_kaasknabbels"), 2);
        helper.assertTrue(k.klik(p, blok) && blok.getCount() == 2, "a full sky takes nothing more");
        helper.succeed();
    }

    /** A ticket for the Draaimolen: the tame guh nearby gets happy. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    @SuppressWarnings("removal")
    public static void draaimolenMaaktGuhsBlij(GameTestHelper helper) {
        BlockPos molen = new BlockPos(4, 2, 2);
        bron(helper, new BlockPos(4, 2, 1), 2);
        helper.setBlock(molen, KoepelFeature.blok(KoepelBlock.Soort.DRAAIMOLEN));
        DraaimolenBlockEntity be = (DraaimolenBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(molen));
        ServerPlayer baas = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        GuhEntity guh = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        BlockPos g = helper.absolutePos(new BlockPos(1, 2, 4));
        guh.snapTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
        guh.tame(baas);
        helper.getLevel().addFreshEntity(guh);
        helper.assertFalse(Band.isBlij(guh), "not happy before the ride");
        ItemStack knabbel = new ItemStack(KoepelVakken.guhs("kaas_knabbels"), 2);
        helper.assertTrue(be.klik(baas, knabbel) && knabbel.getCount() == 1 && be.rit() > 0, "a knabbel is a ticket");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(Band.isBlij(guh), "the tame guh rode along and is happy"))
                .thenExecute(guh::discard)
                .thenSucceed();
    }

    /** The higher, the faster; it catches pluis (or white wool while guhs:pluis does not exist yet). */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void wolkenvangerHoogIsSneller(GameTestHelper helper) {
        helper.assertTrue(KoepelGetallen.vangTicks(0) == KoepelGetallen.VANGER_TRAAG && KoepelGetallen.vangTicks(300) == KoepelGetallen.VANGER_SNEL,
                "slow at sea level, fast from y 192");
        helper.assertTrue(KoepelGetallen.vangTicks(100) > KoepelGetallen.vangTicks(150), "higher is faster");
        helper.assertTrue(WolkenvangerBlockEntity.vangst() == KoepelVakken.guhs("pluis") || WolkenvangerBlockEntity.vangst() == Items.WHITE_WOOL,
                "it catches pluis");
        BlockPos plek = new BlockPos(4, 2, 2);
        helper.setBlock(plek, KoepelFeature.blok(KoepelBlock.Soort.WOLKENVANGER));
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(plek)) instanceof WolkenvangerBlockEntity, "the dish stands");
        helper.succeed();
    }
}
