package nl.juiced.guhs.feature.vogels;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * Game tests of the vogels (3.0): never hostile, the Guhdex pages count, the pluisveertje (seeds, by itself, not twice
 * right after each other), the spawn rules (place, day/night, biome modifiers), the kaasmeesje hanging under a leaf, the
 * owl sleeping by day and hooting at night, the gulls ("Mijn! Mijn!", fish on the ground, not afraid of food), the bird
 * feeder, and the flock that flies up together.
 * Template vogels_test_wei: 12 x 12 grass (floor at helper y 1: birds stand at y 2), a log with a 3 x 3 leaf roof at the
 * corner (leaves at helper y 5 over x/z 0..2), a sand strip at x 11.
 */
public class VogelsGameTests {
    private static final String WEI = "vogels_test_wei";
    private static final String BATCH = "vogels";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static <T extends Vogeltje> T vogel(GameTestHelper helper, EntityType<T> type, double x, double y, double z) {
        T v = type.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        v.snapTo(at.x, at.y, at.z, 0, 0);
        v.zetThuis(BlockPos.containing(at));
        helper.getLevel().addFreshEntity(v);
        return v;
    }

    static void tijd(ServerLevel level, long t) {
        level.setDayTime(t);
        level.updateSkyBrightness();
    }

    /** Birds never attack, never aim at anyone; hitting one only makes it fly up. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 200)
    public static void vogelsNooitBoos(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(6, 2, 6));
        List<Vogeltje> vogels = List.of(vogel(helper, VogelsFeature.PLUISVINKJE.get(), 5.5, 2, 6.5), vogel(helper, VogelsFeature.KAASMEESJE.get(), 7.5, 2, 6.5),
                vogel(helper, VogelsFeature.GUH_UILTJE.get(), 6.5, 2, 5.5), vogel(helper, VogelsFeature.ZEEMEEUWTJE.get(), 6.5, 2, 7.5));
        float hp = p.getHealth();
        for (Vogeltje v : vogels) {
            v.hurt(helper.getLevel().damageSources().playerAttack(p), 0.5f);
            v.setTarget(p);
        }
        helper.runAfterDelay(60, () -> {
            for (Vogeltje v : vogels) {
                helper.assertTrue(v.getTarget() == null, v.naam() + " has no target");
                helper.assertTrue(v.vliegt(), v.naam() + " flew up after being hit");
                helper.assertTrue(!v.doHurtTarget(helper.getLevel(), p), v.naam() + " can't hurt");
            }
            helper.assertTrue(p.getHealth() >= hp, "the player wasn't hurt");
            vogels.forEach(Entity::discard);
            weg(helper, p);
            helper.succeed();
        });
    }

    /** The four pages are counting creature pages, seen from 8 blocks; the Guhdex fills them and the advancements follow. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void vogelsPaginasTellen(GameTestHelper helper) {
        List<GuhVariant> pages = List.of(GuhVariant.PLUISVINKJE, GuhVariant.KAASMEESJE, GuhVariant.GUH_UILTJE, GuhVariant.ZEEMEEUWTJE);
        for (GuhVariant v : pages) {
            helper.assertTrue(GuhDex.TELLEND.contains(v) && GuhDex.ENTRIES.contains(v) && !GuhDex.TAMEABLE.contains(v)
                    && GuhDex.isCreaturePage(v) && v.isCharacter(), v.id() + " is a counting creature page");
        }
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 6));
        GuhWorldData data = GuhWorldData.get(p.level().getServer());
        data.player(p.getUUID()).seen.removeAll(pages);
        List<Vogeltje> vogels = List.of(vogel(helper, VogelsFeature.PLUISVINKJE.get(), 7.5, 2, 6.5), vogel(helper, VogelsFeature.KAASMEESJE.get(), 7.5, 2, 5.5),
                vogel(helper, VogelsFeature.GUH_UILTJE.get(), 7.5, 2, 7.5), vogel(helper, VogelsFeature.ZEEMEEUWTJE.get(), 8.5, 2, 6.5));
        vogels.forEach(v -> v.setNoAi(true));
        GuhDex.seeCreatures(p);
        for (GuhVariant v : pages) {
            helper.assertTrue(data.player(p.getUUID()).seen.contains(v), v.id() + " seen from ~6-7 blocks");
        }
        VogelsEvents.controleer(p);
        helper.assertTrue(GidsFeature.heeft(p, "diertjes/vogels_pluisvinkje") && GidsFeature.heeft(p, "diertjes/vogels_zeemeeuwtje")
                && GidsFeature.heeft(p, "diertjes/vogels_alle") && GidsFeature.heeft(p, "diertjes/root"), "the advancements");
        vogels.forEach(Entity::discard);
        weg(helper, p);
        helper.succeed();
    }

    /** Seeds for a pluisvinkje: a feather (not again right away); by itself it drops one too. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 100)
    public static void vogelsPluisveertje(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 6));
        PluisvinkjeEntity vink = vogel(helper, VogelsFeature.PLUISVINKJE.get(), 6.5, 2, 6.5);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 5));
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
        p.interactOn(vink, InteractionHand.MAIN_HAND, vink.position());
        helper.assertTrue(p.getMainHandItem().getCount() == 4, "a seed eaten");
        helper.assertTrue(veertjes(helper, box) == 1, "one pluisveertje for the seeds");
        helper.assertTrue(!vink.magVoerVeertje() && vink.vertrouwen() > 0 && GidsFeature.heeft(p, "diertjes/vogels_voeren"), "fed: trusts you, cooldown");
        p.interactOn(vink, InteractionHand.MAIN_HAND, vink.position());
        helper.assertTrue(veertjes(helper, box) == 1 && p.getMainHandItem().getCount() == 3, "no second feather right away");
        vink.veertjeNu();
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(veertjes(helper, box) == 2, "a feather by itself");
            helper.assertTrue(VogelsFeature.PLUISVEERTJE.getId().equals(Guhs.id("pluisveertje")), "the id the hemel quest asks");
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).forEach(Entity::discard);
            vink.discard();
            weg(helper, p);
            helper.succeed();
        });
    }

    static int veertjes(GameTestHelper helper, AABB box) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, i -> i.getItem().is(VogelsFeature.PLUISVEERTJE.get()))
                .stream().mapToInt(i -> i.getItem().getCount()).sum();
    }

    /** Spawn rules: on top of the world on something to sit on; day birds by day, the owl at night (with the world always);
     *  the biome modifiers put each bird in its biomes. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void vogelsSpawnRegels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long was = level.getDayTime();
        RandomSource r = RandomSource.create(1);
        BlockPos gras = helper.absolutePos(new BlockPos(6, 2, 6));
        BlockPos binnen = helper.absolutePos(new BlockPos(6, 1, 6));
        BlockPos zand = helper.absolutePos(new BlockPos(11, 2, 6));
        BlockPos blad = helper.absolutePos(new BlockPos(0, 6, 0));
        BlockPos onderBlad = helper.absolutePos(new BlockPos(0, 4, 0));
        EntityType<PluisvinkjeEntity> vink = VogelsFeature.PLUISVINKJE.get();
        EntityType<GuhUiltjeEntity> uil = VogelsFeature.GUH_UILTJE.get();
        EntityType<ZeemeeuwtjeEntity> meeuw = VogelsFeature.ZEEMEEUWTJE.get();
        helper.assertTrue(VogelSpawns.plekOk(vink, level, gras) && VogelSpawns.plekOk(vink, level, blad), "on grass and on top of the leaves: grass y " + gras.getY() + " top " + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, gras.getX(), gras.getZ())
                + " leaf-top y " + blad.getY() + " top " + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, blad.getX(), blad.getZ())
                + " light " + level.getRawBrightness(gras, 0) + " below " + level.getBlockState(gras.below()) + " " + VogelSpawns.plekOk(vink, level, gras));
        helper.assertTrue(!VogelSpawns.plekOk(vink, level, binnen) && !VogelSpawns.plekOk(vink, level, onderBlad), "not in the ground, not under a roof");
        helper.assertTrue(VogelSpawns.plekOk(meeuw, level, zand), "the gull on the sand");
        tijd(level, 6000);
        helper.assertTrue(VogelSpawns.check(vink, level, EntitySpawnReason.NATURAL, gras, r) && !VogelSpawns.check(uil, level, EntitySpawnReason.NATURAL, gras, r),
                "by day: vinkjes, no owls");
        helper.assertTrue(VogelSpawns.check(uil, level, EntitySpawnReason.CHUNK_GENERATION, gras, r), "with the world an owl may come by day (it sleeps)");
        tijd(level, 18000);
        helper.assertTrue(!VogelSpawns.check(vink, level, EntitySpawnReason.NATURAL, gras, r) && VogelSpawns.check(uil, level, EntitySpawnReason.NATURAL, gras, r),
                "at night: owls, no vinkjes");
        tijd(level, was);
        // not too many
        List<Vogeltje> veel = new ArrayList<>();
        for (int i = 0; i < VogelSpawns.max(meeuw); i++) {
            Vogeltje m = vogel(helper, meeuw, 3.5 + i % 5, 2, 3.5 + i / 5);
            m.setNoAi(true);
            veel.add(m);
        }
        helper.assertTrue(!VogelSpawns.check(meeuw, level, EntitySpawnReason.CHUNK_GENERATION, zand, r), "a crowd of gulls: no more");
        veel.forEach(Entity::discard);
        // the biome modifiers (applied to the biome registry at server start)
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        helper.assertTrue(spawnt(biomes.get(ResourceKey.create(Registries.BIOME, Guhs.id("guh_fields"))), vink), "vinkjes in the Guhvelden");
        helper.assertTrue(spawnt(biomes.get(ResourceKey.create(Registries.BIOME, Guhs.id("pink_puffs"))), vink), "vinkjes in the Roze pluisjes");
        helper.assertTrue(spawnt(biomes.get(ResourceKey.create(Registries.BIOME, Guhs.id("vadswoud"))), VogelsFeature.KAASMEESJE.get())
                && spawnt(biomes.get(ResourceKey.create(Registries.BIOME, Guhs.id("kaas_flats"))), VogelsFeature.KAASMEESJE.get()), "meesjes");
        helper.assertTrue(spawnt(biomes.get(ResourceKey.create(Registries.BIOME, Guhs.id("guh_peaks"))), uil)
                && spawnt(biomes.get(ResourceKey.create(Registries.BIOME, Guhs.id("vadswoud"))), uil), "owls");
        helper.assertTrue(spawnt(biomes.get(ResourceKey.create(Registries.BIOME, Guhs.id("guh_sea"))), meeuw)
                && spawnt(biomes.get(ResourceKey.create(Registries.BIOME, Guhs.id("guhwaii"))), meeuw), "gulls at the sea and on Guhwai'i");
        helper.assertTrue(!spawnt(biomes.get(ResourceKey.create(Registries.BIOME, Guhs.id("guh_fields"))), uil)
                && !spawnt(biomes.get(ResourceKey.create(Registries.BIOME, Guhs.id("vadswoud"))), meeuw), "not where they don't belong");
        // the top-up only brings a biome's own birds: none in the game test's plains
        ServerPlayer p = speler(helper, new BlockPos(6, 2, 6));
        for (int i = 0; i < 20; i++) {
            helper.assertTrue(VogelSpawns.aanvullen(p, r).isEmpty(), "no birds in a biome without them");
        }
        weg(helper, p);
        helper.succeed();
    }

    static boolean spawnt(Biome biome, EntityType<?> type) {
        return biome != null && biome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream().anyMatch(d -> d.type == type);
    }

    /** The kaasmeesje finds the underside of the leaves, hangs there upside down (no gravity) and lets go when the leaf is gone. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 120)
    public static void vogelsKaasmeesjeHangt(GameTestHelper helper) {
        KaasmeesjeEntity mees = vogel(helper, VogelsFeature.KAASMEESJE.get(), 3.5, 2, 3.5);
        BlockPos hang = null;
        for (int i = 0; i < 40 && hang == null; i++) {
            hang = mees.hangplek(mees.blockPosition(), 4);
        }
        helper.assertTrue(hang != null, "a spot under the leaves");
        final BlockPos plek = hang;
        helper.assertTrue(helper.getLevel().getBlockState(plek.above()).is(net.minecraft.tags.BlockTags.LEAVES)
                && helper.getLevel().getBlockState(plek).isAir(), "right under a leaf");
        ServerPlayer p = speler(helper, new BlockPos(3, 2, 3));
        p.setShiftKeyDown(true);
        mees.zetHouding(Vogeltje.HANGT);
        Vec3 at = mees.landDoel(plek);
        mees.snapTo(at.x, at.y, at.z);
        mees.land();
        mees.geland(plek);
        helper.assertTrue(GidsFeature.heeft(p, "diertjes/vogels_ondersteboven"), "a player saw it hang");
        double y = mees.getY();
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(mees.hangt() && !mees.vliegt() && mees.isNoGravity() && Math.abs(mees.getY() - y) < 0.05, "still hanging, no falling");
            helper.getLevel().setBlockAndUpdate(plek.above(), Blocks.AIR.defaultBlockState());
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(mees.vliegt() && !mees.hangt(), "the leaf is gone: it lets go and flies");
                mees.discard();
                weg(helper, p);
                helper.succeed();
            });
        });
    }

    /** The owl sleeps by day (and its head turns far), wakes at night and its "oehoe" is an advancement at night. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 120)
    public static void vogelsUiltjeSlaaptOverdag(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long was = level.getDayTime();
        tijd(level, 6000);
        ServerPlayer p = speler(helper, new BlockPos(9, 2, 9));
        p.setShiftKeyDown(true);
        GuhUiltjeEntity uil = vogel(helper, VogelsFeature.GUH_UILTJE.get(), 6.5, 2, 6.5);
        helper.assertTrue(uil.getMaxHeadYRot() >= 170, "its head turns (nearly) all the way round");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(uil.slaapt() && !uil.vliegt(), "asleep by day");
            uil.oehoe();
            helper.assertTrue(!GidsFeature.heeft(p, "diertjes/vogels_oehoe"), "a hoot by day doesn't count");
            tijd(level, 18000);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!uil.slaapt(), "awake at night");
                uil.oehoe();
                helper.assertTrue(GidsFeature.heeft(p, "diertjes/vogels_oehoe"), "heard it at night");
                tijd(level, was);
                uil.discard();
                weg(helper, p);
                helper.succeed();
            });
        });
    }

    /** Gulls: not afraid of someone with fish, they come for it ("Mijn! Mijn!"), fish on the ground is snatched, a player
     *  without food scares them, a sneaking one doesn't. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 120)
    public static void vogelsZeemeeuwMijn(GameTestHelper helper) {
        ZeemeeuwtjeEntity meeuw = vogel(helper, VogelsFeature.ZEEMEEUWTJE.get(), 6.5, 2, 6.5);
        ServerPlayer p = speler(helper, new BlockPos(8, 2, 6));
        helper.assertTrue(meeuw.schrikVan() == p, "an empty-handed player close by scares it");
        p.setShiftKeyDown(true);
        helper.assertTrue(meeuw.schrikVan() == null, "a sneaking one doesn't");
        p.setShiftKeyDown(false);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD, 3));
        helper.assertTrue(meeuw.schrikVan() == null && meeuw.voerder() == p, "with fish: it wants it");
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(meeuw.vliegt(), "it comes flying for the fish");
            meeuw.mijnMijn(p);
            helper.assertTrue(GidsFeature.heeft(p, "diertjes/vogels_mijn"), "Mijn! Mijn!");
            ItemEntity vis = new ItemEntity(helper.getLevel(), meeuw.getX(), meeuw.getY(), meeuw.getZ(), new ItemStack(Items.SALMON, 2));
            helper.getLevel().addFreshEntity(vis);
            ItemEntity steen = new ItemEntity(helper.getLevel(), meeuw.getX(), meeuw.getY(), meeuw.getZ(), new ItemStack(Items.DIAMOND));
            helper.getLevel().addFreshEntity(steen);
            meeuw.hap(vis);
            helper.assertTrue(vis.getItem().getCount() == 1, "one salmon snatched");
            helper.assertTrue(!meeuw.lekker(steen.getItem()) && meeuw.lekker(new ItemStack(Items.BREAD)), "only fish and bread, never your other things");
            vis.discard();
            steen.discard();
            meeuw.discard();
            weg(helper, p);
            helper.succeed();
        });
    }

    /** The bird feeder: seeds fill it, birds find it, pecks eat it empty, then it isn't found any more. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void vogelsVoerhuisje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(6, 2, 6));
        level.setBlockAndUpdate(pos, VogelsFeature.VOERHUISJE.get().defaultBlockState());
        helper.assertTrue(VoerhuisjeBlock.dichtbij(level, pos.east(3), 16) == null, "empty: no birds come");
        VoerhuisjeBlock.vul(level, pos, level.getBlockState(pos));
        helper.assertTrue(level.getBlockState(pos).getValue(VoerhuisjeBlock.VOER) == 1, "a scoop of seeds");
        for (int i = 0; i < 5; i++) {
            VoerhuisjeBlock.vul(level, pos, level.getBlockState(pos));
        }
        helper.assertTrue(level.getBlockState(pos).getValue(VoerhuisjeBlock.VOER) == VoerhuisjeBlock.MAX, "full, not more");
        helper.assertTrue(pos.equals(VoerhuisjeBlock.dichtbij(level, pos.east(3), 16)), "found with food on it");
        helper.assertTrue(VoerhuisjeBlock.dichtbij(level, pos.east(30), 16) == null, "not from far away");
        PluisvinkjeEntity vink = vogel(helper, VogelsFeature.PLUISVINKJE.get(), 6.5, 2 + VoerhuisjeBlock.TAFEL_HOOGTE, 6.5);
        vink.setNoAi(true);
        helper.assertTrue(pos.equals(vink.opVoerhuisje()), "the vinkje sits on the feeder");
        int pikken = 0;
        while (level.getBlockState(pos).getValue(VoerhuisjeBlock.VOER) > 0 && pikken < 400) {
            helper.assertTrue(VoerhuisjeBlock.pik(level, pos, vink), "there's food");
            pikken++;
        }
        helper.assertTrue(pikken >= VoerhuisjeBlock.MAX && pikken < 400, "eaten empty bit by bit (" + pikken + " pecks)");
        helper.assertTrue(!VoerhuisjeBlock.pik(level, pos, vink) && VoerhuisjeBlock.dichtbij(level, pos, 16) == null, "empty again");
        vink.discard();
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** A flock has one leader (the lowest id); when one gets a fright the whole flock flies up. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void vogelsZwerm(GameTestHelper helper) {
        List<PluisvinkjeEntity> zwerm = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            zwerm.add(vogel(helper, VogelsFeature.PLUISVINKJE.get(), 4.5 + i, 2, 6.5));
        }
        int min = zwerm.stream().mapToInt(Entity::getId).min().orElse(-1);
        for (PluisvinkjeEntity v : zwerm) {
            helper.assertTrue(v.leider().getId() == min, "one leader for the flock");
            helper.assertTrue(!v.vliegt(), "sitting");
        }
        zwerm.get(2).schrik(zwerm.get(2).position().add(1, 0, 0));
        for (PluisvinkjeEntity v : zwerm) {
            helper.assertTrue(v.vliegt() && v.isNoGravity(), "the whole flock flies up");
        }
        zwerm.forEach(Entity::discard);
        helper.succeed();
    }
}
