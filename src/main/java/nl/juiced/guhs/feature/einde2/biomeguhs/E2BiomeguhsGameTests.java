package nl.juiced.guhs.feature.einde2.biomeguhs;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature;
import nl.juiced.guhs.feature.einde2.biomes.Wolken;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * Game tests of the einde2 slice biomeguhs: which guh lives in which biome, the wild ones of the top-up (where they may
 * come, the caps, come-and-go), taming one and its Guhdex page, and the server state of each trick. The rooms are made by
 * tools/features/einde2_biomeguhs.py (a floor of kaaskorst with a rim; one with kruimelzand; one with a kaasgat).
 * Only {@link #einde2BiomeguhsWildEnTam} makes wild ones: the caps count every wild biome guh within 64 blocks, and the
 * tests of one batch stand next to each other.
 */
public class E2BiomeguhsGameTests {
    private static final String BATCH = "einde2_biomeguhs";
    private static final String KAMER = "einde2_biomeguhs_test_kamer", ZAND = "einde2_biomeguhs_test_zand", HEUVEL = "einde2_biomeguhs_test_heuvel";

    private static ServerPlayer speler(GameTestHelper helper, int x, int y, int z) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        zetNeer(helper, player, x, y, z);
        return player;
    }

    private static void zetNeer(GameTestHelper helper, Entity e, int x, int y, int z) {
        BlockPos at = helper.absolutePos(new BlockPos(x, y, z));
        e.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
    }

    private static void weg(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    private static GuhEntity guh(GameTestHelper helper, GuhVariant variant, int x, int y, int z) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(x, y, z));
        guh.setVariant(variant);
        guh.setGuhScale(1f);
        guh.setBaby(false);
        return guh;
    }

    private static GuhEntity tam(GameTestHelper helper, GuhVariant variant, int x, int y, int z, ServerPlayer baas) {
        GuhEntity guh = guh(helper, variant, x, y, z);
        guh.tame(baas);
        return guh;
    }

    /** Every biome of the outer islands has its own guh; the main island (and every chunk from before einde2) has none. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 40)
    public static void einde2BiomeguhsVariantVoor(GameTestHelper helper) {
        helper.assertTrue(Biomeguhs.variantVoor(Einde2Ids.GATENKAASHEUVELS) == GuhVariant.KORSTGUH, "Gatenkaasheuvels: Korstguh");
        helper.assertTrue(Biomeguhs.variantVoor(Einde2Ids.KORSTWOESTIJN) == GuhVariant.DUINGUH, "Korstwoestijn: Duinguh");
        helper.assertTrue(Biomeguhs.variantVoor(Einde2Ids.KRUIMELWOLKEN) == GuhVariant.KRUIMELGUH, "Kruimelwolken: Kruimelguh");
        helper.assertTrue(Biomeguhs.variantVoor(Einde2Ids.SCHIMMELKAASVLAKTE) == GuhVariant.SCHIMMELGUH, "Schimmelkaasvlakte: Schimmelguh");
        helper.assertTrue(Biomeguhs.variantVoor(Einde2Ids.KRISTALKAASKLIPPEN) == GuhVariant.KRISTALGUH, "Kristalkaasklippen: Kristalguh");
        helper.assertTrue(Biomeguhs.variantVoor(Einde2Ids.KNABBELKOORBOS) == GuhVariant.KOORGUH, "Knabbelkoorbos: Koorguh");
        helper.assertTrue(Biomeguhs.variantVoor(Einde2Ids.GUHEINDE) == null, "the main island and old chunks: none");
        helper.assertTrue(Einde2Ids.BUITEN.stream().map(Biomeguhs::variantVoor).distinct().count() == 6, "six biomes, six different guhs");
        for (GuhVariant v : E2BiomeguhsFeature.GUHS) {
            helper.assertTrue(v.isBuitenGuh() && !v.isCharacter() && v.weight == 0, v + " is a real variant that is never rolled in the Guhmensie");
            helper.assertTrue(GuhDex.TAMEABLE.contains(v) && GuhDex.TELLEND.contains(v), v + " has a page that counts and can get a star");
            helper.assertTrue(v == GuhVariant.KRISTALGUH || v == GuhVariant.SCHIMMELGUH || v == GuhVariant.KOORGUH || VariantGedragen.van(v) != null,
                    v + " has its behaviour");
        }
        helper.succeed();
    }

    /** The top-up: where a wild one may come, the caps, come-and-go; then taming one: the page, the star, no longer wild. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2BiomeguhsWildEnTam(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 4, 2, 4);
        var random = level.getRandom();
        helper.assertTrue(Biomeguhs.op(level, helper.absolutePos(new BlockPos(2, 2, 2)), null, random) == null, "no biome of the outer islands: nothing comes");
        helper.assertTrue(Biomeguhs.op(level, helper.absolutePos(new BlockPos(2, 2, 2)), GuhVariant.MINT, random) == null, "only the six biome guhs");
        helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
        helper.assertTrue(Biomeguhs.op(level, helper.absolutePos(new BlockPos(6, 2, 6)), GuhVariant.DUINGUH, random) == null, "not on island ground: nothing comes");
        GuhEntity a = Biomeguhs.op(level, helper.absolutePos(new BlockPos(2, 2, 2)), GuhVariant.KORSTGUH, random);
        helper.assertTrue(a != null && a.getVariant() == GuhVariant.KORSTGUH && Biomeguhs.isWild(a), "a wild Korstguh on kaaskorst");
        helper.assertTrue(a.isKomEnGaGuh() && !a.shouldBeSaved() && a.removeWhenFarAway(999), "it comes and goes: never saved");
        helper.assertTrue(!Biomeguhs.vol(level, p), "one is not full");
        GuhEntity b = Biomeguhs.op(level, helper.absolutePos(new BlockPos(6, 2, 2)), GuhVariant.KOORGUH, random);
        helper.assertTrue(b != null && Biomeguhs.vol(level, p), "two within 64 blocks: full (all kinds count together)");
        helper.assertTrue(Biomeguhs.aanvullen(level, p, random, pos -> GuhVariant.KRISTALGUH) == null, "full: the top-up brings nothing");
        b.discard();
        helper.assertTrue(!Biomeguhs.vol(level, p), "one left: room again");
        // tame it: the Guhdex, no longer wild, kept
        a.tame(p);
        GuhWorldData.PlayerData data = GuhWorldData.get(level.getServer()).player(p.getUUID());
        helper.assertTrue(data.seen.contains(GuhVariant.KORSTGUH) && data.tamed.contains(GuhVariant.KORSTGUH), "the page and its star");
        helper.assertTrue(!Biomeguhs.isWild(a) && !a.isKomEnGaGuh() && a.shouldBeSaved(), "a tamed one stays");
        helper.assertTrue(!Biomeguhs.vol(level, p), "and does not count for the caps");
        helper.succeedWhen(() -> {
            helper.assertTrue(a.getPersistentData().getBooleanOr(Trucs.WELKOM, false), "its owner heard what it can do");
            a.discard();
            weg(helper, p);
        });
    }

    /** The Korstguh rolls up when its owner sneaks next to it, the ball rolls when pushed (unhurt), and it unrolls again. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 500)
    public static void einde2BiomeguhsKorstbal(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 4, 2, 2);
        GuhEntity guh = tam(helper, GuhVariant.KORSTGUH, 4, 2, 4, p);
        GuhEntity wild = guh(helper, GuhVariant.KORSTGUH, 2, 2, 6);
        float gezond = guh.getHealth();
        double[] x0 = new double[1];
        helper.assertTrue(!Korstbal.opgerold(guh) && !Korstbal.duw(guh, new Vec3(1, 0, 0), 0.5, p), "not rolled up: a push does nothing");
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(!Korstbal.opgerold(guh), "its owner stands next to it: nothing yet");
                    p.setShiftKeyDown(true);
                })
                .thenWaitUntil(() -> helper.assertTrue(Korstbal.opgerold(guh), "its owner sneaks next to it: it rolls up"))
                .thenExecute(() -> {
                    p.setShiftKeyDown(false);
                    helper.assertTrue(!Korstbal.opgerold(wild), "a wild one does no tricks");
                    x0[0] = guh.getX();
                    helper.assertTrue(Korstbal.duw(guh, new Vec3(1, 0, 0), 0.5, p), "a push");
                })
                .thenWaitUntil(() -> helper.assertTrue(Math.abs(guh.getX() - x0[0]) > 1.0, "the ball rolls: " + (guh.getX() - x0[0])))
                .thenExecute(() -> helper.assertTrue(guh.getHealth() == gezond && Korstbal.opgerold(guh), "unhurt, still a ball"))
                .thenWaitUntil(() -> helper.assertTrue(!Korstbal.opgerold(guh), "left alone it unrolls"))
                .thenExecute(() -> {
                    BlockPos kamer = helper.absolutePos(BlockPos.ZERO);
                    helper.assertTrue(new AABB(kamer).inflate(9, 4, 9).move(4, 0, 4).contains(guh.position()), "it stayed in the room");
                    weg(helper, p);
                })
                .thenSucceed();
    }

    /** A kaasgat is found, and a Korstguh naps in it rolled up with its eyes shut, then wakes. */
    @GuhTest(template = HEUVEL, batch = BATCH, timeoutTicks = 300)
    public static void einde2BiomeguhsKaasgat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos gat = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.assertTrue(KaasgatDutje.isGat(level, gat), "one block deep, walls all round: a kaasgat");
        helper.assertTrue(!KaasgatDutje.isGat(level, helper.absolutePos(new BlockPos(2, 3, 2))), "the open floor is no hole");
        helper.assertTrue(gat.equals(KaasgatDutje.zoekGat(level, helper.absolutePos(new BlockPos(6, 3, 6)), KaasgatDutje.ZOEK)), "found from the hill");
        helper.assertTrue(KaasgatDutje.zoekGat(level, helper.absolutePos(new BlockPos(6, 3, 6)), 1) == null, "not that close");
        ServerPlayer p = speler(helper, 6, 3, 6);
        GuhEntity guh = tam(helper, GuhVariant.KORSTGUH, 4, 2, 4, p);
        KaasgatDutje.slaap(guh, 40);
        helper.assertTrue(Korstbal.opgerold(guh) && GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT) && GuhHooks.isBezig(guh), "asleep in its hole: a ball, eyes shut");
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> helper.assertTrue(Korstbal.opgerold(guh), "still asleep"))
                .thenWaitUntil(() -> helper.assertTrue(!Korstbal.opgerold(guh) && !GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT), "awake again"))
                .thenExecute(() -> weg(helper, p))
                .thenSucceed();
    }

    /** The Duinguh digs in on kruimelzand (only there), stays while its owner is near and comes out when the owner leaves. */
    @GuhTest(template = ZAND, batch = BATCH, timeoutTicks = 500)
    public static void einde2BiomeguhsDuingraver(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 3, 2, 3);
        GuhEntity guh = tam(helper, GuhVariant.DUINGUH, 2, 2, 2, p);
        GuhEntity opKorst = tam(helper, GuhVariant.DUINGUH, 7, 2, 2, p);
        GuhEntity gewoon = guh(helper, GuhVariant.NORMAL, 7, 2, 7);
        guh.setOrderedToSit(true);
        opKorst.setOrderedToSit(true);
        helper.assertTrue(Biomeguhs.isKruimelzand(E2BiomesFeature.KRUIMELZAND.get().defaultBlockState()) && !Biomeguhs.isKruimelzand(Blocks.SAND.defaultBlockState()),
                "kruimelzand is known by its id");
        helper.assertTrue(Biomeguhs.zaktNiet(guh) && !Biomeguhs.zaktNiet(gewoon), "only the Duinguh never sinks");
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(Duingraver.opZand(guh) && !Duingraver.opZand(opKorst) && !Duingraver.ingegraven(guh), "on the sand, not dug in yet"))
                .thenWaitUntil(() -> helper.assertTrue(Duingraver.ingegraven(guh), "standing still on kruimelzand: it digs in"))
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(Duingraver.ingegraven(guh) && !Duingraver.ingegraven(opKorst), "it stays dug in; the one on kaaskorst never digs");
                    guh.setOrderedToSit(false);
                    zetNeer(helper, p, 7, 2, 7);   // (more than 7 blocks away)
                })
                .thenWaitUntil(() -> helper.assertTrue(!Duingraver.ingegraven(guh), "its owner walked off: it pops up to follow"))
                .thenExecute(() -> weg(helper, p))
                .thenSucceed();
    }

    /** The Kruimelguh floats down slowly (a plain guh drops like a guh) and the kruimelwolken carry it. */
    @GuhTest(template = HEUVEL, batch = BATCH, timeoutTicks = 400)
    public static void einde2BiomeguhsKruimelzweef(GameTestHelper helper) {
        GuhEntity kruimel = guh(helper, GuhVariant.KRUIMELGUH, 2, 8, 2);
        GuhEntity gewoon = guh(helper, GuhVariant.NORMAL, 6, 8, 6);
        helper.assertTrue(Wolken.draagt(kruimel) && !Wolken.draagt(gewoon), "the kruimelwolken carry a Kruimelguh and nobody else");
        helper.startSequence()
                .thenIdle(14)
                .thenExecute(() -> {
                    helper.assertTrue(Kruimelzweef.zweeft(kruimel) && !kruimel.onGround(), "it floats");
                    helper.assertTrue(kruimel.getDeltaMovement().y >= -Kruimelzweef.DAAL - 0.02, "never faster than a crumb: " + kruimel.getDeltaMovement().y);
                    helper.assertTrue(gewoon.getY() < kruimel.getY() - 1.0, "a plain guh is far below it by now: " + gewoon.getY() + " / " + kruimel.getY());
                })
                .thenWaitUntil(() -> helper.assertTrue(kruimel.onGround() && !Kruimelzweef.zweeft(kruimel), "landed: the flag is off"))
                .thenSucceed();
    }

    /** A tamed Schimmelguh is a lamp: the flag, a real light where its head is, and nothing left behind when it goes. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void einde2BiomeguhsSchimmellamp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 3, 2, 3);
        GuhEntity lamp = tam(helper, GuhVariant.SCHIMMELGUH, 2, 2, 6, p);
        GuhEntity wild = guh(helper, GuhVariant.SCHIMMELGUH, 6, 2, 6);
        lamp.setOrderedToSit(true);
        wild.setNoAi(true);
        BlockPos[] plek = new BlockPos[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(Schimmellamp.gloeit(lamp) && Schimmellamp.lichtVan(lamp) != null, "tamed: it glows, with a real light"))
                .thenIdle(15)
                .thenExecute(() -> {
                    // (the light is at its head, or lags at most SPELING blocks behind: it does not move for every step)
                    plek[0] = Schimmellamp.lichtVan(lamp);
                    helper.assertTrue(plek[0] != null && plek[0].distManhattan(Schimmellamp.lichtPlek(lamp)) <= Schimmellamp.SPELING
                            && level.getBlockState(plek[0]).is(E2BiomeguhsFeature.SCHIMMELGUH_LICHT.get()), "the light stays while the guh is there");
                    helper.assertTrue(level.getBrightness(LightLayer.BLOCK, plek[0]) == SchimmelguhLichtBlock.LICHT, "as bright as a lamp: "
                            + level.getBrightness(LightLayer.BLOCK, plek[0]));
                    helper.assertTrue(!Schimmellamp.gloeit(wild) && !level.getBlockState(Schimmellamp.lichtPlek(wild)).is(E2BiomeguhsFeature.SCHIMMELGUH_LICHT.get()),
                            "a wild one is no lamp");
                    lamp.discard();
                })
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockState(plek[0]).isAir(), "the guh is gone: so is its light"))
                .thenExecute(() -> weg(helper, p))
                .thenSucceed();
    }

    /** A tamed Kristalguh finds one kristalkaasbrokje, and no second one on the same day. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2BiomeguhsKristalneus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 3, 2, 3);
        GuhEntity guh = tam(helper, GuhVariant.KRISTALGUH, 5, 2, 5, p);
        GuhEntity wild = guh(helper, GuhVariant.KRISTALGUH, 2, 2, 6);
        guh.setOrderedToSit(true);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(guh.onGround() && wild.onGround(), "they stand"))
                .thenExecute(() -> {
                    helper.assertTrue(!Kristalneus.magVinden(wild) && !Kristalneus.vind(wild), "a wild one finds nothing for anybody");
                    helper.assertTrue(Kristalneus.magVinden(guh) && Kristalneus.vind(guh), "the tamed one finds a brokje");
                    AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(9, 4, 9).move(4, 0, 4);
                    long brokjes = level.getEntitiesOfClass(ItemEntity.class, kamer, i -> i.getItem().is(E2BiomesFeature.KRISTALKAAS_BROKJE.get()))
                            .stream().mapToInt(i -> i.getItem().getCount()).sum();
                    helper.assertTrue(brokjes == 1, "exactly one kristalkaasbrokje: " + brokjes);
                    helper.assertTrue(!Kristalneus.magVinden(guh) && !Kristalneus.vind(guh), "not again today");
                    guh.getPersistentData().putLong(Kristalneus.DAG, Kristalneus.vandaag(guh));   // (as if that was yesterday)
                    helper.assertTrue(Kristalneus.magVinden(guh), "a new day, a new chance");
                    zetNeer(helper, p, 7, 2, 7);
                    p.setGameMode(GameType.SPECTATOR);
                    helper.assertTrue(!Kristalneus.magVinden(guh), "not without its owner around");
                    weg(helper, p);
                })
                .thenSucceed();
    }

    /** A tamed Koorguh sings while a jukebox plays close by, and counts double for the Disco-dynamo. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void einde2BiomeguhsKoorzang(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 3, 2, 3);
        GuhEntity guh = tam(helper, GuhVariant.KOORGUH, 4, 2, 4, p);
        GuhEntity gewoon = guh(helper, GuhVariant.NORMAL, 6, 2, 6);
        guh.setOrderedToSit(true);
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.JUKEBOX);
        JukeboxBlockEntity jukebox = helper.getBlockEntity(pos, JukeboxBlockEntity.class);
        jukebox.setSongItemWithoutPlaying(new ItemStack(Items.MUSIC_DISC_CAT));
        jukebox.getSongPlayer().stop(helper.getLevel(), jukebox.getBlockState());
        helper.assertTrue(Biomeguhs.discoFactor(guh) == 2 && Biomeguhs.discoFactor(gewoon) == 1, "a Koorguh counts double on the Disco-dynamo");
        helper.startSequence()
                .thenIdle(45)
                .thenExecute(() -> {
                    helper.assertTrue(!Koorzang.zingt(guh), "no music: no song");
                    jukebox.tryForcePlaySong();
                })
                .thenWaitUntil(() -> helper.assertTrue(Koorzang.zingt(guh), "the record plays: it sings along"))
                .thenExecute(() -> {
                    helper.assertTrue(!Trucs.doet(gewoon), "a plain guh only dances");
                    jukebox.getSongPlayer().stop(helper.getLevel(), jukebox.getBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(!Koorzang.zingt(guh), "the music stopped, so does the Koorguh"))
                .thenExecute(() -> weg(helper, p))
                .thenSucceed();
    }
}
