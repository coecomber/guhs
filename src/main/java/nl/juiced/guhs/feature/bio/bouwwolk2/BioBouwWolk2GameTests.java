package nl.juiced.guhs.feature.bio.bouwwolk2;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.world.GuhTime;
import nl.juiced.guhs.world.Terugkeer;

/**
 * Game tests of the biomes3 slice "bouw-wolk2". The test server has no Guhmensie, so the giant is put on a bare floor
 * (template reuzenguh_test_hal: his bed at helper x 3..13, z 6..12; he lies at helper (8.5, 3, 9.5), head west; with the
 * castle not turned his hall is x 1.5..16.5, z 0.5..17.5 and the landing in front of the gate (9, 2, 23)), and the three
 * building templates are read as data. The giant is ticked by hand, so a whole visit fits in one game tick.
 */
public class BioBouwWolk2GameTests {
    private static final String BATCH = "bio_bouw_wolk2", HAL = "reuzenguh_test_hal";
    private static final Vec3 BED = new Vec3(8.5, 3, 9.5);

    private static ServerPlayer speler(GameTestHelper helper, double x, double y, double z) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        zet(helper, p, x, y, z);
        p.setOnGround(true);
        return p;
    }

    private static void zet(GameTestHelper helper, ServerPlayer p, double x, double y, double z) {
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        p.snapTo(at.x, at.y, at.z);
    }

    private static ReuzenguhEntity reus(GameTestHelper helper, Rotation draai) {
        ReuzenguhEntity reus = Bewakers.nieuweReus(helper.getLevel(), helper.absoluteVec(BED), draai);
        reus.tick();
        return reus;
    }

    /** The player walks to (x, z) on the floor in steps of at most `stap` blocks; the giant listens after every step. */
    private static void loop(GameTestHelper helper, ReuzenguhEntity reus, ServerPlayer p, double x, double z, double stap) {
        Vec3 van = p.position(), naar = helper.absoluteVec(new Vec3(x, 2, z));
        int n = Math.max(1, (int) Math.ceil(van.distanceTo(naar) / stap));
        for (int i = 1; i <= n; i++) {
            Vec3 q = van.lerp(naar, i / (double) n);
            p.snapTo(q.x, q.y, q.z);
            p.setOnGround(true);
            reus.tick();
        }
    }

    private static void wacht(ReuzenguhEntity reus, int ticks) {
        for (int i = 0; i < ticks; i++) {
            reus.tick();
        }
    }

    private static boolean bewijs(ServerPlayer p, String naam) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static void weg(GameTestHelper helper, Entity... entities) {
        for (Entity e : entities) {
            if (e instanceof ServerPlayer p) {
                helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            } else if (e != null) {
                e.discard();
            }
        }
        for (ReuzenguhEntity r : helper.getLevel().getEntitiesOfClass(ReuzenguhEntity.class, helper.getBounds().inflate(2))) {
            r.discard();
        }
    }

    // --- the giant ----------------------------------------------------------------------------------------------------------

    /** Sneaking past his head to the hoard: he sleeps on, the crumb is yours, and that is the proof of getting past him. */
    @GuhTest(template = HAL, batch = BATCH)
    public static void bioBouwWolk2SluipenLukt(GameTestHelper helper) {
        ReuzenguhEntity reus = reus(helper, Rotation.NONE);
        ServerPlayer p = speler(helper, 9, 2, 16.5);
        reus.tick();
        p.setPose(Pose.CROUCHING);
        helper.assertTrue(p.isCrouching() && reus.inHal(p.position()), "the sneaking player stands in the hall");
        loop(helper, reus, p, 2.5, 16.5, 0.065);
        loop(helper, reus, p, 2.5, 9.5, 0.065);
        helper.assertTrue(Kasteel.bijOor(reus.thuis(), reus.draai(), p.position()), "the way to the back leads right past his ear");
        loop(helper, reus, p, 2.5, 3.5, 0.065);
        loop(helper, reus, p, 7.5, 3.5, 0.065);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.SLAAP, "sneaking all the way: he sleeps on (" + reus.staat() + ")");
        BlockPos schat = BlockPos.containing(reus.thuis()).offset(Kasteel.SCHAT);
        helper.assertTrue(SchatBlock.pak(p, schat), "the hoard gives its crumb");
        helper.assertTrue(GuhQuests.count(p, BouwWolk2Slice.KRUIMEL_ITEM.get()) == 1, "one gouden knabbelkruimel");
        helper.assertTrue(bewijs(p, "wolkenkasteeltje_langs_reus") && !bewijs(p, "reuzenguh_weggeblazen"), "the proof of getting past him, and not of being blown out");
        helper.assertTrue(p.position().distanceTo(helper.absoluteVec(new Vec3(7.5, 2, 3.5))) < 0.01, "nobody moved the player");
        weg(helper, p, reus);
        helper.succeed();
    }

    /** Running: a warning first (he peeks, the runner stays), then the sneeze: out onto the cloud, unharmed, and he sleeps again. */
    @GuhTest(template = HAL, batch = BATCH)
    public static void bioBouwWolk2RennenWaarschuwtDanBlaast(GameTestHelper helper) {
        ReuzenguhEntity reus = reus(helper, Rotation.NONE);
        ServerPlayer p = speler(helper, 9, 2, 16.5);
        reus.tick();
        float gezond = p.getHealth();
        // walking upright, away from his head: he does not mind
        loop(helper, reus, p, 14.5, 16.5, 0.2);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.SLAAP, "walking in the hall, far from his head, is quiet enough");
        // running
        p.setSprinting(true);
        loop(helper, reus, p, 14.5, 15.5, 0.28);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.LOER, "running: he stirs and peeks (" + reus.staat() + ")");
        helper.assertTrue(reus.inHal(p.position()), "the warning leaves the runner where they are");
        // still running during the grace: nothing yet
        loop(helper, reus, p, 14.5, 13.0, 0.28);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.LOER, "he gives you a moment to stop");
        // stop: he falls asleep again by himself
        p.setSprinting(false);
        wacht(reus, ReuzenguhEntity.LOER_TICKS + 2);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.SLAAP && reus.inHal(p.position()), "quiet after the warning: he sleeps on and you stay");
        // run, and keep running
        p.setSprinting(true);
        loop(helper, reus, p, 14.5, 12.0, 0.28);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.LOER, "the second run: a warning again");
        for (int i = 0; i < ReuzenguhEntity.GENADE + 4 && reus.staat() == ReuzenguhEntity.Staat.LOER; i++) {
            loop(helper, reus, p, 14.5, i % 2 == 0 ? 12.3 : 12.0, 0.3);
        }
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.NIES, "running on after the warning: he sneezes (" + reus.staat() + ")");
        p.setSprinting(false);
        wacht(reus, ReuzenguhEntity.BLAAS_OP + 1);
        Vec3 landing = helper.absoluteVec(new Vec3(9.0, 2, 23.0));
        helper.assertTrue(p.position().distanceTo(landing) < 0.6, "blown out onto the cloud in front of the gate: " + p.position() + " / " + landing);
        helper.assertTrue(!reus.inHal(p.position()), "which is outside the hall");
        helper.assertTrue(p.getHealth() == gezond && p.fallDistance == 0 && p.hasEffect(MobEffects.SLOW_FALLING), "unharmed, and floating down like a feather");
        helper.assertTrue(!helper.getLevel().getBlockState(BlockPos.containing(landing).below()).isAir(), "there is cloud under the landing");
        helper.assertTrue(bewijs(p, "reuzenguh_weggeblazen"), "the proof of being blown out");
        wacht(reus, ReuzenguhEntity.NIES_TICKS);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.SLAAP, "and he is asleep again at once");
        helper.assertTrue(reus.position().distanceTo(helper.absoluteVec(BED)) < 1.0E-6, "he never left his bed");
        // the player can walk right back in and take the crumb
        zet(helper, p, 9, 2, 16.5);
        p.setPose(Pose.CROUCHING);
        reus.tick();
        loop(helper, reus, p, 9, 14.0, 0.065);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.SLAAP, "he always resets: the next try starts fresh");
        weg(helper, p, reus);
        helper.succeed();
    }

    /** What else he hears: a jump, a block broken in his hall, walking upright past his ear. And what he does not. */
    @GuhTest(template = HAL, batch = BATCH)
    public static void bioBouwWolk2WatHijHoort(GameTestHelper helper) {
        ReuzenguhEntity reus = reus(helper, Rotation.NONE);
        ServerPlayer p = speler(helper, 14.5, 2, 16.5);
        reus.tick();
        // a jump
        Vec3 at = p.position();
        p.snapTo(at.x, at.y + 0.42, at.z);
        p.setOnGround(false);
        reus.tick();
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.LOER, "a jump wakes him a little");
        p.snapTo(at.x, at.y, at.z);
        p.setOnGround(true);
        wacht(reus, ReuzenguhEntity.LOER_TICKS + 2);
        // a broken block
        BlockPos blok = helper.absolutePos(new BlockPos(14, 1, 14));
        NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), blok, helper.getLevel().getBlockState(blok), p));
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.LOER, "breaking a block in his hall too");
        wacht(reus, ReuzenguhEntity.LOER_TICKS + 2);
        BlockPos buiten = helper.absolutePos(new BlockPos(8, 1, 22));
        NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), buiten, helper.getLevel().getBlockState(buiten), p));
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.SLAAP, "outside his hall you may dig");
        // upright past his ear
        zet(helper, p, 2.5, 2, 16.5);
        reus.tick();
        loop(helper, reus, p, 2.5, 14.5, 0.2);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.SLAAP, "upright is fine until you come near his head");
        loop(helper, reus, p, 2.5, 10.0, 0.2);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.LOER, "walking upright past his ear wakes him a little");
        wacht(reus, ReuzenguhEntity.LOER_TICKS + 2);
        // standing still, sneaking, and anything outside the hall: nothing
        wacht(reus, 40);
        zet(helper, p, 9, 2, 22);
        reus.tick();
        p.setSprinting(true);
        loop(helper, reus, p, 9, 19, 0.28);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.SLAAP, "running outside the gate is nobody's business");
        // a spectator is never heard
        p.setSprinting(false);
        weg(helper, p, reus);
        helper.succeed();
    }

    /** Two players: only the noisy one is blown out; the one who sneaks stays and still gets a crumb. */
    @GuhTest(template = HAL, batch = BATCH)
    public static void bioBouwWolk2AlleenDeLawaaimaker(GameTestHelper helper) {
        ReuzenguhEntity reus = reus(helper, Rotation.NONE);
        ServerPlayer stil = speler(helper, 2.5, 2, 3.5), luid = speler(helper, 14.5, 2, 16.5);
        stil.setPose(Pose.CROUCHING);
        reus.tick();
        reus.hoor(luid, ReuzenguhEntity.Lawaai.RENNEN);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.LOER, "noise: a warning");
        reus.hoor(luid, ReuzenguhEntity.Lawaai.RENNEN);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.LOER, "noise within the grace does nothing");
        wacht(reus, ReuzenguhEntity.GENADE);
        reus.hoor(luid, ReuzenguhEntity.Lawaai.SPRINGEN);
        helper.assertTrue(reus.staat() == ReuzenguhEntity.Staat.NIES, "noise after it: the sneeze");
        wacht(reus, ReuzenguhEntity.BLAAS_OP);
        helper.assertTrue(!reus.inHal(luid.position()) && reus.inHal(stil.position()), "the noisy one is out, the quiet one stays");
        helper.assertTrue(bewijs(luid, "reuzenguh_weggeblazen") && !bewijs(stil, "reuzenguh_weggeblazen"), "and only the noisy one has that proof");
        wacht(reus, ReuzenguhEntity.NIES_TICKS);
        helper.assertTrue(SchatBlock.pak(stil, BlockPos.containing(reus.thuis()).offset(Kasteel.SCHAT)), "the quiet one takes a crumb");
        weg(helper, stil, luid, reus);
        helper.succeed();
    }

    /** He cannot be hurt or pushed, is saved with his place, and there is exactly one: an extra goes, a missing one comes back. */
    @GuhTest(template = HAL, batch = BATCH)
    public static void bioBouwWolk2ReusBlijftEnIsAlleen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 bed = helper.absoluteVec(BED);
        ReuzenguhEntity reus = reus(helper, Rotation.CLOCKWISE_90);
        ServerPlayer p = speler(helper, 9, 2, 16.5);
        float leven = reus.getHealth();
        helper.assertTrue(!reus.hurtServer(level, p.damageSources().playerAttack(p), 50f) && reus.getHealth() == leven, "he cannot be hurt");
        helper.assertTrue(!reus.hurtServer(level, level.damageSources().lava(), 50f) && !reus.hurtServer(level, level.damageSources().fall(), 50f), "by nothing");
        reus.push(3.0, 1.0, 3.0);
        reus.knockback(2.0, 1.0, 0.0);
        reus.push(p);
        reus.snapTo(bed.x + 4, bed.y, bed.z);
        reus.tick();
        helper.assertTrue(reus.position().distanceTo(bed) < 1.0E-6 && reus.getDeltaMovement().lengthSqr() == 0, "pushed or moved, he lies on his bed again the next tick");
        helper.assertTrue(!reus.isPushable() && !reus.canBeLeashed() && !reus.removeWhenFarAway(1.0E6) && reus.isPersistenceRequired() && reus.isNoGravity(),
                "not pushable, not leashable, never despawns");
        helper.assertTrue(reus.draai() == Rotation.CLOCKWISE_90 && reus.getYRot() == Kasteel.yaw(Rotation.CLOCKWISE_90), "he lies the way his castle is turned");
        // saved with his place
        CompoundTag tag = Nbt.saveWithoutId(reus);
        ReuzenguhEntity kopie = BouwWolk2Slice.REUZENGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        Nbt.load(kopie, tag);
        helper.assertTrue(kopie.thuis().distanceTo(bed) < 1.0E-6 && kopie.draai() == Rotation.CLOCKWISE_90 && kopie.staat() == ReuzenguhEntity.Staat.SLAAP,
                "saved and loaded: the same bed, the same turn, asleep");
        kopie.discard();
        // exactly one
        helper.assertTrue(Bewakers.reus(level, bed, Rotation.CLOCKWISE_90) == null && giants(helper) == 1, "one giant is there: nothing happens");
        Bewakers.nieuweReus(level, bed, Rotation.CLOCKWISE_90);
        Bewakers.nieuweReus(level, bed.add(1, 0, 1), Rotation.CLOCKWISE_90);
        helper.assertTrue(giants(helper) == 3, "(two more put there by hand)");
        Bewakers.reus(level, bed, Rotation.CLOCKWISE_90);
        helper.assertTrue(giants(helper) == 1, "extra giants are removed: " + giants(helper));
        // gone: seen missing once is not enough, twice a while apart brings a new one, and never two
        for (ReuzenguhEntity r : level.getEntitiesOfClass(ReuzenguhEntity.class, AABB.ofSize(bed, 20, 20, 20))) {
            r.discard();
        }
        BlockPos plek = BlockPos.containing(bed);
        Terugkeer.zetGezien(level, Bewakers.SOORT_REUS, plek, null);
        helper.assertTrue(Bewakers.reus(level, bed, Rotation.CLOCKWISE_90) == null && giants(helper) == 0, "missing for the first look: not yet");
        Terugkeer.zetGemist(level, Bewakers.SOORT_REUS, plek, Terugkeer.BEVESTIG + 5);
        ReuzenguhEntity nieuw = Bewakers.reus(level, bed, Rotation.CLOCKWISE_90);
        helper.assertTrue(nieuw != null && giants(helper) == 1, "still missing a little later: a new giant on the bed");
        helper.assertTrue(nieuw.thuis().distanceTo(bed) < 1.0E-6 && nieuw.draai() == Rotation.CLOCKWISE_90 && nieuw.staat() == ReuzenguhEntity.Staat.SLAAP,
                "in the right place, turned right, asleep");
        Terugkeer.zetGemist(level, Bewakers.SOORT_REUS, plek, Terugkeer.BEVESTIG + 5);
        helper.assertTrue(Bewakers.reus(level, bed, Rotation.CLOCKWISE_90) == null && giants(helper) == 1, "and never a second one");
        Terugkeer.zetGezien(level, Bewakers.SOORT_REUS, plek, null);
        weg(helper, p, reus, nieuw);
        helper.succeed();
    }

    private static int giants(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ReuzenguhEntity.class, helper.getBounds().inflate(2), Entity::isAlive).size();
    }

    /** The castle's measures turn with the castle, the same way the game turns a template. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bioBouwWolk2KasteelDraaitMee(GameTestHelper helper) {
        BlockPos hoek = new BlockPos(100, 60, -40);
        for (Rotation r : Rotation.values()) {
            Vec3 thuis = Bewakers.wereld(hoek, r, Kasteel.REUS_IN_TEMPLATE);
            helper.assertTrue(Kasteel.draai(Kasteel.yaw(r)) == r, "yaw and turn are each other's inverse (" + r + ")");
            for (Vec3 offset : List.of(Kasteel.LANDING, Kasteel.KOP, Kasteel.HAL_MIN, Kasteel.HAL_MAX, new Vec3(3, 0, -5))) {
                Vec3 viaTemplate = Bewakers.wereld(hoek, r, Kasteel.REUS_IN_TEMPLATE.add(offset));
                Vec3 viaReus = Kasteel.wereld(thuis, r, offset);
                helper.assertTrue(viaTemplate.distanceTo(viaReus) < 1.0E-6, "an offset to the giant lands where the template has it (" + r + " " + offset + ")");
                helper.assertTrue(Kasteel.lokaal(thuis, r, viaReus).distanceTo(offset) < 1.0E-6, "and back");
            }
            helper.assertTrue(Kasteel.inHal(thuis, r, Kasteel.wereld(thuis, r, new Vec3(-6, 0, 7))) && !Kasteel.inHal(thuis, r, Kasteel.wereld(thuis, r, Kasteel.LANDING)),
                    "the hall holds its corner and not the landing (" + r + ")");
        }
        helper.succeed();
    }

    // --- the templates ------------------------------------------------------------------------------------------------------

    private static ListTag entiteiten(GameTestHelper helper, String naam) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id(naam)).orElseThrow();
        return t.save(new CompoundTag()).getListOrEmpty("entities");
    }

    private static String pad(BlockState s) {
        return s == null ? "-" : BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
    }

    /** The castle template has what the code expects where it expects it: the giant, the hoard, the hall, the gate, the landing. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bioBouwWolk2KasteelTemplate(GameTestHelper helper) {
        Map<BlockPos, BlockState> k = Brug.blokken(helper.getLevel(), "wolkenkasteeltje");
        helper.assertTrue(k.size() > 5000, "the template is there (" + k.size() + " blocks)");
        ListTag e = entiteiten(helper, "wolkenkasteeltje");
        helper.assertTrue(e.size() == 1 && e.getCompoundOrEmpty(0).getCompoundOrEmpty("nbt").getStringOr("id", "").equals("guhs:reuzenguh"), "one entity: the giant");
        ListTag pos = e.getCompoundOrEmpty(0).getListOrEmpty("pos");
        Vec3 reus = new Vec3(pos.getDoubleOr(0, 0), pos.getDoubleOr(1, 0), pos.getDoubleOr(2, 0));
        helper.assertTrue(reus.distanceTo(Kasteel.REUS_IN_TEMPLATE) < 1.0E-6, "where Kasteel says he lies: " + reus);
        float yaw = e.getCompoundOrEmpty(0).getCompoundOrEmpty("nbt").getListOrEmpty("Rotation").getFloatOr(0, -1f);
        helper.assertTrue(yaw == Kasteel.REUS_YAW, "his head west: " + yaw);
        BlockPos thuis = BlockPos.containing(reus);
        helper.assertTrue(pad(k.get(thuis.below())).startsWith("wolkenblok") && k.get(thuis) == null, "he lies on his bed of cloud");
        helper.assertTrue(k.get(thuis.offset(Kasteel.SCHAT)) != null && k.get(thuis.offset(Kasteel.SCHAT)).is(BouwWolk2Slice.SCHAT.get()), "the hoard at the back of the hall");
        BlockPos landing = BlockPos.containing(reus.add(Kasteel.LANDING));
        helper.assertTrue(pad(k.get(landing.below())).startsWith("wolkenblok") && k.get(landing) == null && k.get(landing.above()) == null,
                "the landing: cloud with air above it");
        helper.assertTrue(!Kasteel.inHal(reus, Rotation.NONE, reus.add(Kasteel.LANDING)), "and outside the hall");
        // the hall's floor is whole, and walking (no jumping, no digging) the only way out of it is the gate: three wide, south
        int vloer = 0, poort = 0;
        int x0 = (int) (reus.x + Kasteel.HAL_MIN.x), x1 = (int) (reus.x + Kasteel.HAL_MAX.x) - 1, z0 = (int) (reus.z + Kasteel.HAL_MIN.z), z1 = (int) (reus.z + Kasteel.HAL_MAX.z) - 1;
        int y = thuis.getY() - 2;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                vloer += k.get(new BlockPos(x, y, z)) == null ? 1 : 0;
            }
        }
        helper.assertTrue(vloer == 0, "no hole in the hall's floor (" + vloer + ")");
        java.util.Set<BlockPos> gezien = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> rij = new java.util.ArrayDeque<>();
        BlockPos start = new BlockPos(thuis.getX(), y + 1, z1 - 1);
        helper.assertTrue(k.get(start) == null && k.get(start.above()) == null, "(the flood starts in the open, inside the gate)");
        rij.add(start);
        gezien.add(start);
        int buiten = 0;
        while (!rij.isEmpty()) {
            BlockPos p = rij.poll();
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos q = p.relative(d);
                if (gezien.contains(q) || k.get(q) != null || k.get(q.above()) != null) {
                    continue;
                }
                if (q.getX() < x0 - 1 || q.getX() > x1 + 1 || q.getZ() < z0 - 1 || q.getZ() > z1 + 1) {
                    buiten++;                                       // (one step outside the walls: do not go on)
                    if (q.getZ() == z1 + 2 && p.getZ() == z1 + 1) {
                        poort++;
                    }
                    gezien.add(q);
                    continue;
                }
                gezien.add(q);
                rij.add(q);
            }
        }
        helper.assertTrue(poort == 3 && buiten == 3, "one way out on foot: the gate, three wide, in the south wall (through the gate " + poort + ", ways out " + buiten + ")");
        // the way from the gate to the hoard goes past his ear: the other side is blocked
        int open = 0;
        for (int z = thuis.getZ() - 4; z <= thuis.getZ() + 3; z++) {
            open += k.get(new BlockPos(x1, y + 1, z)) == null || k.get(new BlockPos(x1, y + 2, z)) == null ? 1 : 0;
        }
        helper.assertTrue(open == 0, "the east side of the hall is blocked (" + open + " open)");
        helper.assertTrue(Kasteel.bijOor(reus, Rotation.NONE, new Vec3(x0 + 0.5, y + 1, reus.z)) && Kasteel.bijOor(reus, Rotation.NONE, new Vec3(x0 + 1.5, y + 1, reus.z)),
                "and the west side is within his earshot");
        // the smid in his forge, where Bewakers puts him back
        ListTag s = entiteiten(helper, "bliksemsmidse");
        ListTag sp = s.getCompoundOrEmpty(0).getListOrEmpty("pos");
        helper.assertTrue(s.size() == 1 && s.getCompoundOrEmpty(0).getCompoundOrEmpty("nbt").getStringOr("Kind", "").equals("smidguh"), "the forge has the smid-guh");
        helper.assertTrue(new Vec3(sp.getDoubleOr(0, 0), sp.getDoubleOr(1, 0), sp.getDoubleOr(2, 0)).distanceTo(Bewakers.SMID_IN_TEMPLATE) < 1.0E-6, "where Bewakers says he stands");
        helper.succeed();
    }

    /** The rainbow can be walked both ways on all three lanes, its ends stand on the banks, and under the span there is cloud. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bioBouwWolk2BrugLoopbaar(GameTestHelper helper) {
        Map<BlockPos, BlockState> b = Brug.blokken(helper.getLevel(), "regenboogbrug");
        helper.assertTrue(b.size() > 2000, "the template is there (" + b.size() + " blocks)");
        Block regenboog = Bio.blok("regenboogblok", Blocks.AIR), plaat = Bio.blok("regenboogblok_plaat", Blocks.AIR), trap = Bio.blok("regenboogblok_trap", Blocks.AIR);
        String[] stroken = {"a", "b", "c"};
        for (int baan = 0; baan < Brug.BANEN; baan++) {
            int z = Brug.BOOG_Z + baan;
            // from two blocks onto the west bank to two blocks onto the east bank
            Brug.Loop loop = Brug.loop(b, Brug.BOOG_X0 - 2, Brug.BOOG_X1 + 2, z, Brug.DEK - 1, Brug.DEK + 14);
            helper.assertTrue(loop.goed(), "lane " + baan + " is walkable: " + loop);
            helper.assertTrue(loop.top() >= Brug.DEK + 1 + 8, "and it is a real arc: " + loop.top());
            int stukken = 0;
            for (int x = Brug.BOOG_X0; x <= Brug.BOOG_X1; x++) {
                boolean heeft = false;
                for (int y = Brug.DEK + 1; y <= Brug.DEK + 12; y++) {
                    BlockState s = b.get(new BlockPos(x, y, z));
                    if (s != null && (s.is(regenboog) || s.is(plaat) || s.is(trap))) {
                        heeft = true;
                        stukken++;
                        helper.assertTrue(pad(s).equals("regenboogblok_trap") || s.toString().contains("axis=x"), "its stripes run along the bridge: " + s);
                        helper.assertTrue(s.toString().contains("strook=" + stroken[baan]), "one rainbow over the three lanes: " + s);
                    }
                }
                helper.assertTrue(heeft, "a piece of rainbow in column " + x + " of lane " + baan);
                // whoever steps off (this lane, or one beside the bridge) lands on cloud not far below
                for (int zz : new int[] {z, Brug.BOOG_Z - 1, Brug.BOOG_Z + Brug.BANEN}) {
                    helper.assertTrue(Brug.vangnet(b, x, zz, Brug.DEK + 12, 20), "something to land on under column " + x + ", z " + zz);
                }
            }
            helper.assertTrue(stukken >= Brug.BOOG_X1 - Brug.BOOG_X0 + 1, "the arc is one block thick or a little more (" + stukken + ")");
            // the feet stand on the banks
            for (int x : new int[] {Brug.BOOG_X0, Brug.BOOG_X1}) {
                BlockState onder = b.get(new BlockPos(x, Brug.DEK, z));
                helper.assertTrue(onder != null, "the arc's foot at x " + x + " stands on the bank: " + pad(onder));
            }
        }
        // a gap would be found: take one piece out and walk again
        Map<BlockPos, BlockState> stuk = new java.util.HashMap<>(b);
        for (int y = Brug.DEK; y <= Brug.DEK + 14; y++) {
            stuk.remove(new BlockPos(30, y, Brug.BOOG_Z + 1));
        }
        helper.assertTrue(!Brug.loop(stuk, Brug.BOOG_X0 - 2, Brug.BOOG_X1 + 2, Brug.BOOG_Z + 1, Brug.DEK + 1, Brug.DEK + 14).goed(), "(the check finds a hole)");
        BlockState pot = b.get(Brug.POT);
        helper.assertTrue(pot != null && pot.is(BouwWolk2Slice.POT.get()) && pad(b.get(Brug.POT.below())).startsWith("wolkenblok"), "the pot stands on the east bank");
        // every building has its two lift columns from the foot to the deck
        for (Map.Entry<String, Integer> t : Map.of("regenboogbrug", 36, "wolkenkasteeltje", 102, "bliksemsmidse", 24).entrySet()) {
            Map<BlockPos, BlockState> m = t.getKey().equals("regenboogbrug") ? b : Brug.blokken(helper.getLevel(), t.getKey());
            int pads = 0, stroom = 0;
            for (Map.Entry<BlockPos, BlockState> e : m.entrySet()) {
                String id = pad(e.getValue());
                if (id.equals("wolkenlift")) {
                    pads++;
                    helper.assertTrue(e.getKey().getY() == 1, "the lift pads lie one above the meadow (template y 1): " + e.getKey());
                    for (int y = 2; y <= t.getValue(); y++) {
                        helper.assertTrue(pad(m.get(new BlockPos(e.getKey().getX(), y, e.getKey().getZ()))).equals("wolkenstroom"),
                                t.getKey() + ": the stream runs unbroken from the pad to the deck (y " + y + ")");
                    }
                } else if (id.equals("wolkenstroom")) {
                    stroom++;
                }
            }
            helper.assertTrue(pads == 18 && stroom >= 18 * (t.getValue() - 1), t.getKey() + ": two lift columns of nine (" + pads + " pads, " + stroom + " stream blocks)");
        }
        helper.succeed();
    }

    /** The rainbow is a building material: taken pieces grow back after a while, in air only. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bioBouwWolk2RegenboogGroeitTerug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // the bridge's template corner, chosen so that the arc's first piece (the key of its "last seen whole") hangs in the
        // air above this test's own plot; the rest of the arc reaches out over the neighbours, high up
        BlockPos eerste = Bewakers.boog(level, BlockPos.ZERO, Rotation.NONE).get(0).plek();
        BlockPos hoek = helper.absolutePos(new BlockPos(1, 31, 1)).subtract(eerste);
        List<Bewakers.Stuk> boog = Bewakers.boog(level, hoek, Rotation.NONE);
        for (Bewakers.Stuk st : boog) {
            level.getChunk(st.plek().getX() >> 4, st.plek().getZ() >> 4);
        }
        BlockPos sleutel = Bewakers.sleutel(boog);
        try {
            helper.assertTrue(boog.size() >= 3 * (Brug.BOOG_X1 - Brug.BOOG_X0 + 1), "the arc's pieces are read from the template (" + boog.size() + ")");
            helper.assertTrue(Bewakers.mist(level, boog) == boog.size(), "(none of it is in the world yet)");
            Terugkeer.zetGezien(level, Bewakers.SOORT_BOOG, sleutel, null);
            helper.assertTrue(Bewakers.hergroei(level, hoek, Rotation.NONE, Bewakers.HERGROEI) == 0, "seen broken once: not yet");
            Terugkeer.zetGemist(level, Bewakers.SOORT_BOOG, sleutel, Terugkeer.BEVESTIG + 5);
            helper.assertTrue(Bewakers.hergroei(level, hoek, Rotation.NONE, Bewakers.HERGROEI) == boog.size() && Bewakers.mist(level, boog) == 0,
                    "a bridge nobody ever saw whole grows at once");
            helper.assertTrue(Bewakers.hergroei(level, hoek, Rotation.NONE, Bewakers.HERGROEI) == 0, "whole: nothing to do");
            // a player takes five pieces, and builds a block of their own in the place of one of them
            for (int i = 0; i < 5; i++) {
                level.setBlock(boog.get(i * 7).plek(), Blocks.AIR.defaultBlockState(), 2);
            }
            level.setBlock(boog.get(0).plek(), Blocks.GLASS.defaultBlockState(), 2);
            Terugkeer.zetGemist(level, Bewakers.SOORT_BOOG, sleutel, Terugkeer.BEVESTIG + 5);
            helper.assertTrue(Bewakers.hergroei(level, hoek, Rotation.NONE, Bewakers.HERGROEI) == 0 && Bewakers.mist(level, boog) == 5, "just taken: it does not grow back at once");
            Terugkeer.zetGezien(level, Bewakers.SOORT_BOOG, sleutel, level.getGameTime() - Bewakers.HERGROEI - 10);
            Terugkeer.zetGemist(level, Bewakers.SOORT_BOOG, sleutel, Terugkeer.BEVESTIG + 5);
            helper.assertTrue(Bewakers.hergroei(level, hoek, Rotation.NONE, Bewakers.HERGROEI) == 4, "three days after it was last whole: the four open places grow back");
            helper.assertTrue(level.getBlockState(boog.get(0).plek()).is(Blocks.GLASS) && Bewakers.mist(level, boog) == 1, "what a player built there stays");
            helper.assertTrue(Bewakers.HERGROEI == 3 * GuhTime.DAY, "three days");
        } finally {
            for (Bewakers.Stuk s : boog) {
                level.setBlock(s.plek(), Blocks.AIR.defaultBlockState(), 2);
            }
            Terugkeer.zetGezien(level, Bewakers.SOORT_BOOG, sleutel, null);
        }
        helper.succeed();
    }

    // --- the treats ---------------------------------------------------------------------------------------------------------

    /** The pot and the hoard: once a day per player, each player their own turn, again the next day. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bioBouwWolk2TraktatiePerSpelerPerDag(GameTestHelper helper) {
        ServerPlayer a = speler(helper, 1.5, 2, 1.5), b = speler(helper, 2.5, 2, 2.5);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        long vandaag = GuhTime.day(helper.getLevel());
        helper.assertTrue(Dagtraktatie.mag(-1, 0) && Dagtraktatie.mag(4, 5) && !Dagtraktatie.mag(5, 5) && Dagtraktatie.mag(9, 5), "the rule: not on the day you took it");
        Item regenboog = Bio.item("regenboogblok", net.minecraft.world.item.Items.AIR);
        // the pot
        helper.assertTrue(PotBlock.pak(a, pos), "a takes from the pot");
        int knabbels = GuhQuests.count(a, ModItems.KAAS_KNABBELS.get());
        helper.assertTrue(knabbels >= PotBlock.KNABBELS && knabbels <= PotBlock.KNABBELS + PotBlock.KNABBELS_EXTRA && GuhQuests.count(a, regenboog) == PotBlock.REGENBOOG,
                "a handful of kaasknabbels and four blocks of rainbow: " + knabbels + ", " + GuhQuests.count(a, regenboog));
        helper.assertTrue(!PotBlock.pak(a, pos) && GuhQuests.count(a, ModItems.KAAS_KNABBELS.get()) == knabbels, "not a second time today");
        helper.assertTrue(bewijs(a, "regenboogbrug_pot") && !bewijs(b, "regenboogbrug_pot"), "the proof is a's");
        helper.assertTrue(PotBlock.pak(b, pos) && GuhQuests.count(b, regenboog) == PotBlock.REGENBOOG, "b has a turn of their own");
        helper.assertTrue(!PotBlock.pak(b, pos), "once");
        Dagtraktatie.zet(a, Dagtraktatie.POT, vandaag - 1);
        helper.assertTrue(PotBlock.pak(a, pos) && !PotBlock.pak(b, pos), "the next day a may again; b, who took today, may not");
        // the hoard (its own day: taking from the pot does not use it up)
        helper.assertTrue(SchatBlock.pak(a, pos) && SchatBlock.pak(b, pos), "each a crumb from the hoard");
        helper.assertTrue(!SchatBlock.pak(a, pos) && !SchatBlock.pak(b, pos), "one a day");
        helper.assertTrue(GuhQuests.count(a, BouwWolk2Slice.KRUIMEL_ITEM.get()) == 1 && GuhQuests.count(b, BouwWolk2Slice.KRUIMEL_ITEM.get()) == 1, "one each");
        helper.assertTrue(!bewijs(a, "wolkenkasteeltje_langs_reus"), "(no giant here: taking a crumb is no proof of getting past him)");
        Dagtraktatie.zet(b, Dagtraktatie.SCHAT, vandaag - 1);
        helper.assertTrue(SchatBlock.pak(b, pos) && !SchatBlock.pak(a, pos) && GuhQuests.count(b, BouwWolk2Slice.KRUIMEL_ITEM.get()) == 2, "and again the next day");
        // the pot and the hoard are blocks you click
        ItemStack kruimel = new ItemStack(BouwWolk2Slice.KRUIMEL_ITEM.get());
        helper.assertTrue(kruimel.has(net.minecraft.core.component.DataComponents.FOOD) && kruimel.has(net.minecraft.core.component.DataComponents.CONSUMABLE)
                && kruimel.getItem() instanceof net.minecraft.world.item.BlockItem, "the crumb is a snack and a thing to put down");
        weg(helper, a, b);
        helper.succeed();
    }

    // --- the smid -----------------------------------------------------------------------------------------------------------

    /** His shop: every trade costs wolkenpluis and gives a real thing at the stated price; a real trade is the proof. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 60)
    public static void bioBouwWolk2SmidRuilt(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 2, 2));
        npc.setKind(GuhNpcEntity.Kind.SMIDGUH);
        helper.assertTrue(Features.role(GuhNpcEntity.Kind.SMIDGUH) != null, "the kind has its role");
        Item pluis = Smidguh.pluis();
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(pluis).equals(Guhs.id("wolkenpluis")), "he is paid in wolkenpluis");
        var offers = npc.getOffers();
        helper.assertTrue(offers.size() == Smidguh.AANBOD.size() && offers.size() == 10, "ten trades: " + offers.size());
        Map<String, int[]> prijs = Map.of("wolkenblok_wit", new int[]{4, 4}, "wolkenblok_roze", new int[]{4, 4}, "bliksemsmidse_wolk", new int[]{5, 4},
                "regenboogblok", new int[]{8, 6}, "wolkenlamp", new int[]{5, 1}, "wolkenbank", new int[]{8, 1}, "wolkenbed", new int[]{10, 1},
                "bliksemsmidse_wolkentafel", new int[]{6, 1}, "bliksemsmidse_wolkenplank", new int[]{5, 1}, "bliksemsmidse_onweerswolkje", new int[]{12, 1});
        for (MerchantOffer o : offers) {
            String id = BuiltInRegistries.ITEM.getKey(o.getResult().getItem()).getPath();
            int[] p = prijs.get(id);
            helper.assertTrue(p != null && BuiltInRegistries.ITEM.getKey(o.getResult().getItem()).getNamespace().equals(Guhs.MODID), "a trade for " + id);
            helper.assertTrue(o.getItemCostA().item().value() == pluis && o.getItemCostA().count() == p[0] && o.getResult().getCount() == p[1] && o.getItemCostB().isEmpty(),
                    id + ": " + p[0] + " wolkenpluis for " + p[1]);
            helper.assertTrue(!o.isOutOfStock(), "never sold out");
        }
        // the three pieces of cloud furniture the design asks for
        for (String id : List.of("wolkenbank", "wolkenbed", "wolkenlamp")) {
            helper.assertTrue(offers.stream().anyMatch(o -> BuiltInRegistries.ITEM.getKey(o.getResult().getItem()).getPath().equals(id)), "he sells the " + id);
        }
        // a real trade through the shop screen
        ServerPlayer p = speler(helper, 3.5, 2, 2.5);
        Smidguh.ROL.talk(npc, p);
        helper.assertTrue(p.containerMenu instanceof MerchantMenu, "talking to him opens his shop");
        MerchantMenu m = (MerchantMenu) p.containerMenu;
        int bed = -1;
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).getResult().is(Bio.item("wolkenbed", net.minecraft.world.item.Items.AIR))) {
                bed = i;
            }
        }
        m.setSelectionHint(bed);
        m.getSlot(0).set(new ItemStack(pluis, 9));
        helper.assertTrue(m.getSlot(2).getItem().isEmpty() || !m.getSlot(2).getItem().is(Bio.item("wolkenbed", net.minecraft.world.item.Items.AIR)), "nine wolkenpluis buys no bed");
        m.getSlot(0).set(new ItemStack(pluis, 12));
        m.tryMoveItems(bed);
        helper.assertTrue(m.getSlot(2).getItem().is(Bio.item("wolkenbed", net.minecraft.world.item.Items.AIR)), "ten do: " + m.getSlot(2).getItem());
        helper.assertTrue(!bewijs(p, "smidguh_eerste_ruil"), "(no proof before the trade)");
        m.quickMoveStack(p, 2);
        helper.assertTrue(GuhQuests.count(p, Bio.item("wolkenbed", net.minecraft.world.item.Items.AIR)) == 1, "the bed is bought");
        helper.assertTrue(GuhQuests.count(p, pluis) + m.getSlot(0).getItem().getCount() == 2, "for ten of the twelve");
        helper.succeedWhen(() -> {
            helper.assertTrue(bewijs(p, "smidguh_eerste_ruil"), "the first trade is the proof");
            p.closeContainer();
            npc.discard();
            weg(helper, p);
        });
    }

    // --- blocks, items, the Superkompas -------------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH)
    public static void bioBouwWolk2BlokkenEnKompas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (var b : BouwWolk2Slice.blokken()) {
            helper.assertTrue(b.get().asItem() != net.minecraft.world.item.Items.AIR, "an item for " + b.getId());
        }
        BlockPos hoog = helper.absolutePos(new BlockPos(1, 3, 1));
        for (var b : List.of(BouwWolk2Slice.ONWEERSWOLK, BouwWolk2Slice.ONWEERSWOLK_PLAAT, BouwWolk2Slice.ONWEERSWOLK_TRAP, BouwWolk2Slice.WOLKENTAFEL,
                BouwWolk2Slice.WOLKENPLANK, BouwWolk2Slice.ONWEERSWOLKJE, BouwWolk2Slice.KRUIMEL)) {
            helper.assertTrue(Block.getDrops(b.get().defaultBlockState(), level, hoog, null).stream().anyMatch(d -> d.is(b.get().asItem())), b.getId() + " drops itself");
        }
        for (var b : List.of(BouwWolk2Slice.POT, BouwWolk2Slice.SCHAT)) {
            helper.assertTrue(b.get().defaultBlockState().getDestroySpeed(level, hoog) < 0 && Block.getDrops(b.get().defaultBlockState(), level, hoog, null).isEmpty(),
                    b.getId() + " is part of its place: it cannot be broken");
        }
        helper.assertTrue(BouwWolk2Slice.ONWEERSWOLKJE.get().defaultBlockState().getCollisionShape(level, hoog).isEmpty()
                && BouwWolk2Slice.ONWEERSWOLKJE.get().defaultBlockState().canSurvive(level, hoog), "the onweerswolkje floats and nobody bumps into it");
        for (String id : List.of("bliksemsmidse_wolk", "bliksemsmidse_wolk_plaat", "bliksemsmidse_wolk_trap")) {
            helper.assertTrue(level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(id))).isPresent(), "recipe " + id);
        }
        helper.assertTrue(level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Guhs.id("regenboogblok"))).isPresent(),
                "the rainbow block has its recipe from wolkenpluis and dye (slice blokken-wolk): a survival route besides the pot and the bridge");
        for (String s : BouwWolk2Slice.STRUCTUREN) {
            helper.assertTrue(SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.id().equals("wonderen") && c.structures().contains(s)), s + " is in the tab wonderen");
            helper.assertTrue(level.getStructureManager().get(Guhs.id(s)).isPresent(), "template " + s);
        }
        for (String a : List.of("regenboogbrug_gevonden", "wolkenkasteeltje_gevonden", "bliksemsmidse_gevonden", "regenboogbrug_pot", "wolkenkasteeltje_langs_reus",
                "reuzenguh_weggeblazen", "smidguh_eerste_ruil")) {
            helper.assertTrue(level.getServer().getAdvancements().get(Guhs.id("quest/" + a)) != null, "proof advancement " + a);
        }
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getKey(BouwWolk2Slice.REUZENGUH.get()).equals(Guhs.id("reuzenguh")), "the giant's id");
        helper.succeed();
    }
}
