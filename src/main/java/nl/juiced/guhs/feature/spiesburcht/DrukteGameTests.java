package nl.juiced.guhs.feature.spiesburcht;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.WildeDieren;

/**
 * 1.4.1, the spawns of the Guhbarbecuether ({@link Drukte}, {@link SpiesburchtEvents}): fewer aggressive mobs in the open
 * (half of vanilla's cap around a player, the buildings and every other spawn left alone), and the Asguhs of the Asdal that
 * were never born (an animal refuses a dark spot) and now come and go.
 */
public class DrukteGameTests {
    private static final String ROOM = "spiesburcht_testkamer";

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static ServerPlayer player(GameTestHelper helper, BlockPos plek) {
        @SuppressWarnings("removal")
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(plek);
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    /** A wild one like the natural spawner makes it (not kept on purpose, unlike helper.spawn), standing still. */
    private static <T extends Mob> T wild(GameTestHelper helper, EntityType<T> type, BlockPos plek) {
        ServerLevel level = helper.getLevel();
        T mob = type.create(level, EntitySpawnReason.NATURAL);
        helper.assertTrue(mob != null, "a " + EntityType.getKey(type));
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(plek));
        mob.snapTo(at.x, at.y, at.z, 0, 0);
        mob.setNoAi(true);
        level.addFreshEntity(mob);
        return mob;
    }

    /**
     * The count behind the rule: the aggressive mobs around the player nearest to a spot, the way vanilla's cap counts them
     * (one that is kept on purpose and a creature do not count). And what the rule leaves alone: every spawn that is not
     * the natural spawner's, every other dimension, a spot outside a building with its own monsters.
     */
    @GuhTest(template = ROOM, timeoutTicks = 100)
    public static void fewerAggressiveMobsInTheOpen(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
        }
        ServerPlayer player = player(helper, new BlockPos(8, 1, 8));
        List<Entity> made = new ArrayList<>();
        made.add(wild(helper, ModEntities.NETHER_MIKA.get(), new BlockPos(5, 1, 5)));
        made.add(wild(helper, SpiesburchtFeature.VONK_MIKA.get(), new BlockPos(11, 1, 5)));
        made.add(wild(helper, SpiesburchtFeature.KNEKEL_MIKA.get(), new BlockPos(5, 1, 11)));
        Mob kept = helper.spawn(ModEntities.NETHER_MIKA.get(), new BlockPos(11, 1, 11));   // (persistent, like a structure's own)
        kept.setNoAi(true);
        made.add(kept);
        made.add(wild(helper, ModEntities.GUH.get(), new BlockPos(8, 1, 5)));
        BlockPos spot = helper.absolutePos(new BlockPos(8, 1, 10));
        Vec3 midden = helper.absoluteVec(new Vec3(8.5, 1, 8.5));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Drukte.isBoos((Mob) made.get(0)) && !Drukte.isBoos(kept) && !Drukte.isBoos((Mob) made.get(4)),
                    "wild Mikas count, a kept one and a guh don't");
            int n = Drukte.tel(level, null, midden, 6, Drukte::isBoos);
            helper.assertTrue(n == 3, "three aggressive mobs in the room: " + n);
            int rond = Drukte.rondSpeler(level, spot, 6, Drukte::isBoos);
            helper.assertTrue(rond == 3, "three around the player nearest to the spot: " + rond);
            helper.assertTrue(Drukte.vol(level, spot, 6, 3) && !Drukte.vol(level, spot, 6, 4), "full at the maximum, not below it");
            helper.assertTrue(Drukte.MAX_BOOS * 2 == MobCategory.MONSTER.getMaxInstancesPerChunk(), "half of vanilla's monster cap: " + Drukte.MAX_BOOS);
            // what the rule leaves alone
            for (EntityType<? extends Mob> type : List.of(ModEntities.NETHER_MIKA.get(), SpiesburchtFeature.VONK_MIKA.get(), SpiesburchtFeature.KNEKEL_MIKA.get())) {
                for (EntitySpawnReason reason : List.of(EntitySpawnReason.SPAWNER, EntitySpawnReason.STRUCTURE, EntitySpawnReason.SPAWN_ITEM_USE,
                        EntitySpawnReason.COMMAND, EntitySpawnReason.CHUNK_GENERATION, EntitySpawnReason.TRIGGERED)) {
                    helper.assertTrue(Drukte.mikaMagKomen(type, level, reason, spot, level.getRandom()), "only the natural spawner is held back: " + reason);
                }
                helper.assertTrue(level.dimension() != BarbecuetherFeature.BARBECUETHER
                        && Drukte.mikaMagKomen(type, level, EntitySpawnReason.NATURAL, spot, level.getRandom()), "only in the Guhbarbecuether");
            }
            helper.assertFalse(Drukte.inEigenGebouw(level, spot), "no building with its own monsters here");
            made.forEach(Entity::discard);
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            helper.succeed();
        });
    }

    /**
     * Why no Asguh was ever born, and that one is now: in the dark an animal itself refuses the spot (the spawner's last
     * question), on the ash of the Asdal that no longer counts; not on other ground, not outside the Asdal, not when
     * enough Asguhs are around.
     */
    @GuhTest(template = ROOM, timeoutTicks = 100)
    public static void asguhsAreBornInTheDark(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        BlockPos cel = new BlockPos(8, 1, 8);
        helper.setBlock(cel.below(), BarbecuetherFeature.AS_BLOK.get());
        for (int dx = -1; dx <= 1; dx++) {                    // (a closed cell of stone: pitch dark inside)
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = 1; y <= 3; y++) {
                    if (dx != 0 || dz != 0 || y == 3) {
                        helper.setBlock(new BlockPos(8 + dx, y, 8 + dz), Blocks.STONE);
                    }
                }
            }
        }
        BlockPos plek = helper.absolutePos(cel);
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.NATURAL);
        guh.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 0, 0);
        helper.runAfterDelay(20, () -> {
            int licht = level.getMaxLocalRawBrightness(plek);
            helper.assertTrue(licht < 12, "dark in the cell: " + licht);
            helper.assertTrue(SpiesburchtEvents.opAs(level, plek), "ash under it");
            helper.assertFalse(guh.checkSpawnRules(level, EntitySpawnReason.NATURAL), "the cause: an animal refuses a dark spot");
            helper.assertTrue(SpiesburchtEvents.asguhPlek(guh, level, true, false) == MobSpawnEvent.PositionCheck.Result.SUCCEED,
                    "on the ash of the Asdal the dark no longer counts");
            helper.assertTrue(SpiesburchtEvents.asguhPlek(guh, level, true, true) == MobSpawnEvent.PositionCheck.Result.FAIL,
                    "not when enough Asguhs are around");
            helper.assertTrue(SpiesburchtEvents.asguhPlek(guh, level, false, false) == MobSpawnEvent.PositionCheck.Result.DEFAULT,
                    "outside the Asdal a guh keeps its own answer");
            helper.setBlock(cel.below(), Blocks.STONE);
            helper.assertTrue(SpiesburchtEvents.asguhPlek(guh, level, true, false) == MobSpawnEvent.PositionCheck.Result.DEFAULT,
                    "only on the ash");
            guh.discard();
            helper.succeed();
        });
    }

    /**
     * An Asguh of the natural spawner comes and goes (never saved, gone when everybody is far away), a tamed one stays; and
     * the count that keeps room for the other creatures only counts the wild ones.
     */
    @GuhTest(template = ROOM, timeoutTicks = 100)
    public static void wildAsguhsComeAndGo(GameTestHelper helper) {
        floor(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, new BlockPos(8, 1, 8));
        GuhEntity wild = wild(helper, ModEntities.GUH.get(), new BlockPos(6, 1, 6));
        wild.setVariant(GuhVariant.ASGUH);
        WildeDieren.markeer(wild);                             // (what SpiesburchtEvents.onFinalizeSpawn does in the Asdal)
        GuhEntity tam = wild(helper, ModEntities.GUH.get(), new BlockPos(10, 1, 10));
        tam.setVariant(GuhVariant.ASGUH);
        WildeDieren.markeer(tam);
        tam.tame(player);
        GuhEntity gewoon = wild(helper, ModEntities.GUH.get(), new BlockPos(6, 1, 10));
        Vec3 midden = helper.absoluteVec(new Vec3(8.5, 1, 8.5));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(WildeDieren.isKomEnGa(wild) && !wild.shouldBeSaved() && WildeDieren.magWeg(wild, 129.0 * 129.0),
                    "a wild Asguh comes and goes: never saved, gone at 128 blocks");
            helper.assertFalse(WildeDieren.magWeg(wild, 100.0 * 100.0), "not while a player is within 128 blocks");
            helper.assertTrue(!WildeDieren.isKomEnGa(tam) && tam.shouldBeSaved() && !WildeDieren.magWeg(tam, 500.0 * 500.0), "a tamed Asguh stays");
            helper.assertTrue(Drukte.isWildeAsguh(wild) && !Drukte.isWildeAsguh(tam) && !Drukte.isWildeAsguh(gewoon), "only the wild Asguh counts");
            int n = Drukte.tel(level, null, midden, 6, Drukte::isWildeAsguh);
            helper.assertTrue(n == 1, "one wild Asguh in the room: " + n);
            helper.assertTrue(Drukte.MAX_ASGUHS < MobCategory.CREATURE.getMaxInstancesPerChunk(), "room left in the creature cap");
            wild.discard();
            tam.discard();
            gewoon.discard();
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            helper.succeed();
        });
    }

    /** The lists of the biomes: in the Asdal the Knekel-Mika is no longer the most common Mika, and the guhs are still on its list. */
    @GuhTest(template = "empty")
    public static void theAsdalListsFewerKnekelMikas(GameTestHelper helper) {
        MobSpawnSettings asdal = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getValue(SpiesburchtFeature.ASDAL).getMobSettings();
        int knekel = 0, nether = 0;
        for (var e : asdal.getMobs(MobCategory.MONSTER).unwrap()) {
            knekel += e.value().type() == SpiesburchtFeature.KNEKEL_MIKA.get() ? e.weight() : 0;
            nether += e.value().type() == ModEntities.NETHER_MIKA.get() ? e.weight() : 0;
        }
        helper.assertTrue(knekel > 0 && knekel < nether, "fewer Knekel-Mika's than Nether-Mika's in the Asdal: " + knekel + " / " + nether);
        helper.assertTrue(asdal.getMobs(MobCategory.CREATURE).unwrap().stream().anyMatch(e -> e.value().type() == ModEntities.GUH.get()), "guhs (Asguhs) in the Asdal");
        helper.succeed();
    }
}
