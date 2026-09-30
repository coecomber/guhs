package nl.juiced.guhs.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.boerderij.BoerderijFeature;
import nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.vogels.VogelsFeature;
import nl.juiced.guhs.feature.waterdiertjes.WaterdiertjesFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.1.2: the save and despawn rules of the wild Guhs animals, critters and Mikas ({@link WildeDieren}): the ones a spawner
 * brings while playing come and go (never saved, despawn when far, tidied away above the cap); the ones the world is made
 * with stay like vanilla animals; everything you keep (tame, named, leashed, from a bucket, persistent, bosses, bred,
 * structure) stays exactly as before.
 */
public class WildeDierenGameTests {
    private static final String BATCH = "wildedieren";

    /** A new mob of this type, spawned like the given spawner would (the FinalizeSpawnEvent included). */
    static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> type, EntitySpawnReason reason, double x, double z) {
        ServerLevel level = helper.getLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            level.getServer().setDifficulty(Difficulty.NORMAL, true);   // (the test server starts peaceful: no Mikas there)
        }
        T mob = type.create(level, EntitySpawnReason.TRIGGERED);
        Vec3 at = helper.absoluteVec(new Vec3(x, 2, z));
        mob.snapTo(at.x, at.y, at.z, 0, 0);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(BlockPos.containing(at)), reason, null);
        level.addFreshEntity(mob);
        return mob;
    }

    static String info(Mob m) {
        return " [alive " + m.isAlive() + ", removed " + m.getRemovalReason() + ", persistent " + m.isPersistenceRequired() + ", custom "
                + m.requiresCustomPersistence() + ", named " + m.hasCustomName() + ", tags " + m.entityTags() + ", wild " + WildeDieren.isWild(m)
                + ", difficulty " + m.level().getDifficulty() + "]";
    }

    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static void weg(GameTestHelper helper, Entity... entities) {
        for (Entity e : entities) {
            if (e instanceof ServerPlayer p) {
                helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            } else if (e != null) {
                e.discard();
            }
        }
    }

    /** Natural and spawner-block spawns come and go (not saved); chunk generation, structures and breeding stay. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 40)
    public static void wildeDierenKomEnGaNietBewaard(GameTestHelper helper) {
        List<Mob> komEnGa = new ArrayList<>(List.of(
                spawn(helper, VogelsFeature.PLUISVINKJE.get(), EntitySpawnReason.NATURAL, 1, 1),
                spawn(helper, VogelsFeature.ZEEMEEUWTJE.get(), EntitySpawnReason.NATURAL, 2, 1),
                spawn(helper, ModEntities.GUH_BEE.get(), EntitySpawnReason.NATURAL, 3, 1),
                spawn(helper, WaterdiertjesFeature.GLIMGUHTJE.get(), EntitySpawnReason.NATURAL, 4, 1),
                spawn(helper, LanddiertjesFeature.GUH_KONIJNTJE.get(), EntitySpawnReason.NATURAL, 5, 1),
                spawn(helper, KaasmoerasFeature.KAASMOT.get(), EntitySpawnReason.NATURAL, 6, 1),
                spawn(helper, ModEntities.MIKA.get(), EntitySpawnReason.NATURAL, 7, 1),
                spawn(helper, ModEntities.MIKA.get(), EntitySpawnReason.SPAWNER, 8, 1),
                spawn(helper, WaterdiertjesFeature.GUH_EENDJE.get(), EntitySpawnReason.NATURAL, 9, 1)));
        List<Mob> blijven = new ArrayList<>(List.of(
                spawn(helper, VogelsFeature.PLUISVINKJE.get(), EntitySpawnReason.CHUNK_GENERATION, 1, 4),
                spawn(helper, ModEntities.GUH_BEE.get(), EntitySpawnReason.CHUNK_GENERATION, 2, 4),
                spawn(helper, LanddiertjesFeature.GUH_KONIJNTJE.get(), EntitySpawnReason.CHUNK_GENERATION, 3, 4),
                spawn(helper, ModEntities.MIKA.get(), EntitySpawnReason.STRUCTURE, 4, 4),
                spawn(helper, KaasmoerasFeature.KIKKERGUH.get(), EntitySpawnReason.BREEDING, 5, 4),
                spawn(helper, VogelsFeature.KAASMEESJE.get(), EntitySpawnReason.SPAWN_ITEM_USE, 6, 4),
                // (not ours: the guhs have their own rule, the farm animals are never come-and-go)
                spawn(helper, ModEntities.GUH.get(), EntitySpawnReason.NATURAL, 7, 4),
                spawn(helper, BoerderijFeature.GUHSCHAAPJE.get(), EntitySpawnReason.NATURAL, 8, 4)));
        helper.runAfterDelay(2, () -> {
            for (Mob m : komEnGa) {
                helper.assertTrue(m.isAlive() && WildeDieren.isKomEnGa(m), m.getType() + " (natural/spawner) comes and goes" + info(m));
                helper.assertTrue(!m.shouldBeSaved(), m.getType() + " (natural/spawner) is not saved with its chunk");
            }
            var mama = komEnGa.get(komEnGa.size() - 1);
            var rijtje = helper.getLevel().getEntitiesOfClass(nl.juiced.guhs.feature.waterdiertjes.GuhEendjeEntity.class, mama.getBoundingBox().inflate(8),
                    e -> mama.getUUID().equals(e.mama()));
            helper.assertTrue(!rijtje.isEmpty() && rijtje.stream().allMatch(e -> WildeDieren.isKomEnGa(e) && !e.shouldBeSaved()),
                    "a come-and-go mama's ducklings come and go with her (" + rijtje.size() + ")");
            for (Mob m : blijven) {
                helper.assertTrue(!WildeDieren.isKomEnGa(m) && !m.entityTags().contains(WildeDieren.KOM_EN_GA), m.getType() + " is not come-and-go");
                helper.assertTrue(m.shouldBeSaved() || m instanceof GuhEntity g && g.isKomEnGaGuh(), m.getType() + " is saved as before");
            }
            rijtje.forEach(Entity::discard);
            komEnGa.forEach(Entity::discard);
            blijven.forEach(Entity::discard);
            helper.succeed();
        });
    }

    /** A come-and-go animal you keep (name, leash, tame, bucket, persistence, boss) is saved again, as before. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 40)
    public static void wildeDierenHouden(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        var genoemd = spawn(helper, VogelsFeature.PLUISVINKJE.get(), EntitySpawnReason.NATURAL, 2, 2);
        var aanLijn = spawn(helper, LanddiertjesFeature.GUH_KONIJNTJE.get(), EntitySpawnReason.NATURAL, 3, 2);
        var tam = spawn(helper, LanddiertjesFeature.PLUISEGELTJE.get(), EntitySpawnReason.NATURAL, 4, 2);
        var emmer = spawn(helper, ModEntities.GUH_VIS.get(), EntitySpawnReason.NATURAL, 5, 2);
        var vast = spawn(helper, WaterdiertjesFeature.KNABBELVLINDERTJE.get(), EntitySpawnReason.NATURAL, 6, 2);
        var baas = spawn(helper, ModEntities.MIKA.get(), EntitySpawnReason.SPAWNER, 7, 2);
        helper.runAfterDelay(2, () -> {
            for (Mob m : List.of(genoemd, aanLijn, tam, emmer, vast, baas)) {
                helper.assertTrue(WildeDieren.isKomEnGa(m) && !m.shouldBeSaved(), m.getType() + " starts as come-and-go");
            }
            genoemd.setCustomName(Component.literal("Pluisje"));
            aanLijn.setLeashedTo(p, true);
            tam.tame(p);
            emmer.setFromBucket(true);
            vast.setPersistenceRequired();
            baas.makeBoss();
            for (Mob m : List.of(genoemd, aanLijn, tam, emmer, vast, baas)) {
                helper.assertTrue(!WildeDieren.isKomEnGa(m) && !WildeDieren.isWild(m) && m.shouldBeSaved(), m.getType() + " you keep is saved");
                helper.assertTrue(!WildeDieren.magWeg(m, 1e6) && !WildeDieren.opruimbaar(m), m.getType() + " you keep never despawns or is tidied");
            }
            aanLijn.removeLeash();
            weg(helper, genoemd, aanLijn, tam, emmer, vast, baas, p);
            helper.succeed();
        });
    }

    /** Far from every player a come-and-go one despawns; a world (chunk generation) bird doesn't; near a player nothing does. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 40)
    public static void wildeDierenDespawn(GameTestHelper helper) {
        var komEnGa = spawn(helper, VogelsFeature.ZEEMEEUWTJE.get(), EntitySpawnReason.NATURAL, 2, 2);
        var wereld = spawn(helper, VogelsFeature.ZEEMEEUWTJE.get(), EntitySpawnReason.CHUNK_GENERATION, 4, 2);
        var konijn = spawn(helper, LanddiertjesFeature.GUH_KONIJNTJE.get(), EntitySpawnReason.NATURAL, 6, 2);
        ServerPlayer p = speler(helper, new BlockPos(3, 2, 3));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(WildeDieren.magWeg(komEnGa, 200 * 200) && WildeDieren.magWeg(konijn, 200 * 200), "come-and-go: gone beyond 128 blocks");
            helper.assertTrue(!WildeDieren.magWeg(komEnGa, 100 * 100) && !WildeDieren.magWeg(konijn, 20 * 20), "not within 128 blocks");
            helper.assertTrue(!WildeDieren.magWeg(wereld, 1e8), "a world bird stays (like a vanilla animal)");
            komEnGa.checkDespawn();
            konijn.checkDespawn();
            wereld.checkDespawn();
            helper.assertTrue(komEnGa.isAlive() && konijn.isAlive() && wereld.isAlive(), "nothing despawns next to a player");
            weg(helper, komEnGa, wereld, konijn, p);
            helper.succeed();
        });
    }

    /**
     * The tidy-up: above the cap the wild ones far from every player go, farthest first; near a player none. Candidates:
     * come-and-go ones, wild birds and wild Mikas (also old saved ones), never a world rabbit, a named bird or Big Mika.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 40)
    public static void wildeDierenOpruimen(GameTestHelper helper) {
        List<Mob> vogels = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            vogels.add(spawn(helper, VogelsFeature.PLUISVINKJE.get(), i % 2 == 0 ? EntitySpawnReason.NATURAL : EntitySpawnReason.CHUNK_GENERATION, 1 + i, 3));
        }
        var wereldKonijn = spawn(helper, LanddiertjesFeature.GUH_KONIJNTJE.get(), EntitySpawnReason.CHUNK_GENERATION, 2, 6);
        var komEnGaKonijn = spawn(helper, LanddiertjesFeature.GUH_KONIJNTJE.get(), EntitySpawnReason.NATURAL, 3, 6);
        var oudeMika = spawn(helper, ModEntities.MIKA.get(), EntitySpawnReason.CHUNK_GENERATION, 4, 6);
        var bigMika = spawn(helper, ModEntities.MIKA.get(), EntitySpawnReason.STRUCTURE, 5, 6);
        var genoemd = spawn(helper, VogelsFeature.KAASMEESJE.get(), EntitySpawnReason.NATURAL, 6, 6);
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        helper.runAfterDelay(2, () -> {
            bigMika.makeBoss();
            genoemd.setCustomName(Component.literal("Mees"));
            helper.assertTrue(vogels.stream().allMatch(WildeDieren::opruimbaar), "wild birds (top-up and old saved ones) may be tidied");
            helper.assertTrue(WildeDieren.opruimbaar(komEnGaKonijn) && WildeDieren.opruimbaar(oudeMika), "a come-and-go rabbit, a wild Mika");
            helper.assertTrue(!WildeDieren.opruimbaar(wereldKonijn) && !WildeDieren.opruimbaar(bigMika) && !WildeDieren.opruimbaar(genoemd),
                    "never a world rabbit, Big Mika or a named bird");
            // a player right here: nothing goes, whatever the cap
            helper.assertTrue(WildeDieren.opruimen(vogels, List.of(p), 3) == 0 && vogels.stream().allMatch(Entity::isAlive), "not near a player");
            // the player far away: all but 3 go, the farthest first
            p.snapTo(p.getX() - 300, p.getY(), p.getZ());
            helper.assertTrue(WildeDieren.opruimen(vogels, List.of(p), 3) == 5, "5 above the cap went");
            helper.assertTrue(vogels.stream().filter(Entity::isAlive).count() == 3, "3 stay");
            for (int i = 0; i < 3; i++) {
                helper.assertTrue(vogels.get(i).isAlive(), "the nearest stay (" + i + ")");
            }
            helper.assertTrue(WildeDieren.opruimen(vogels.stream().filter(Entity::isAlive).toList(), List.of(p), 3) == 0, "at the cap: none");
            helper.assertTrue(WildeDieren.max(helper.getLevel()) >= WildeDieren.MAX_BASIS, "the cap");
            weg(helper, wereldKonijn, komEnGaKonijn, oudeMika, bigMika, genoemd, p);
            vogels.forEach(Entity::discard);
            helper.succeed();
        });
    }

    /** Every kind in the list really is a Guhs mob type (and the guhs themselves and the farm animals are not in it). */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 20)
    public static void wildeDierenSoorten(GameTestHelper helper) {
        helper.assertTrue(WildeDieren.soorten().size() >= 27, "all wild kinds");
        helper.assertTrue(!WildeDieren.soorten().contains(ModEntities.GUH.get()) && !WildeDieren.soorten().contains(BoerderijFeature.GUHKOE.get()),
                "not the guhs, not the farm animals");
        helper.assertTrue(WildeDieren.soorten().contains(ModEntities.NETHER_MIKA.get()) && WildeDieren.soorten().contains(ModEntities.MIKA.get()), "the Mikas");
        helper.succeed();
    }
}
