package nl.juiced.guhs.gametest;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.entity.Mikas;
import nl.juiced.guhs.feature.beroepen.BeroepenFeature;
import nl.juiced.guhs.feature.circuit.CircuitFeature;
import nl.juiced.guhs.feature.doolhof.DoolhofFeature;
import nl.juiced.guhs.feature.guheinde.Enderguhs;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.2.8, the balance changes: the Enderguh belongs to the endgame (no more on the Guh Peaks, wild ones only in the
 * Guheinde near players who beat Opper-Mika, flying and taming only for those players), the cheese fountains give modest
 * loot, and an aggressive guh only fights monsters and Mikas.
 */
public class Balans128GameTests {
    private static final String EMPTY = "empty", WIRE_ROOM = "wire_room";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private static ServerPlayer player(GameTestHelper helper, boolean verslagen) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        if (verslagen) {
            GuhQuests.saved(player).putInt(GuheindeGevecht.WINS, 1);
        }
        return player;
    }

    private static void done(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            p.stopRiding();
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    // --- the Enderguh ------------------------------------------------------------------------------------------------------

    /** A wild guh born in the Guhmension is never an Enderguh any more (it was 1 in 30 on the Guh Peaks). */
    @GuhTest(template = EMPTY, batch = "b128_ender_wild")
    public static void noEnderguhIsBornInTheGuhmensionAnyMore(GameTestHelper helper) {
        RandomSource random = RandomSource.create(128);
        Set<GuhVariant> seen = new java.util.HashSet<>();
        for (int i = 0; i < 40000; i++) {
            seen.add(GuhEntity.wildeVariant(random, i % 2 == 0));
        }
        helper.assertTrue(!seen.contains(GuhVariant.ENDER) && !seen.contains(GuhVariant.VAHOEGE_ENDER), "no Enderguh among the wild variants: " + seen);
        helper.assertTrue(seen.contains(GuhVariant.MINT) && seen.contains(GuhVariant.GHOST) && seen.contains(GuhVariant.NORMAL), "the other variants still come: " + seen);
        for (int i = 0; i < 40; i++) {
            GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
            guh.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(guh.blockPosition()), net.minecraft.world.entity.EntitySpawnReason.NATURAL, null);
            helper.assertTrue(!guh.isEnder(), "a naturally spawned guh is no Enderguh");
            guh.discard();
        }
        helper.succeed();
    }

    /** Flying on an Enderguh is for a rider who beat Opper-Mika (the rider counts, not the owner). */
    @GuhTest(template = EMPTY, batch = "b128_ender_vlieg", timeoutTicks = 200)
    public static void flyingOnAnEnderguhIsForWhoBeatOpperMika(GameTestHelper helper) {
        ServerPlayer nieuw = player(helper, false), winnaar = player(helper, true);
        GuhEntity vanNieuw = helper.spawn(ModEntities.GUH.get(), POS);
        vanNieuw.setVariant(GuhVariant.ENDER);
        vanNieuw.tame(nieuw);
        GuhEntity vanWinnaar = helper.spawn(ModEntities.GUH.get(), POS);
        vanWinnaar.setVariant(GuhVariant.VAHOEGE_ENDER);
        vanWinnaar.tame(winnaar);
        GuhEntity gewoon = helper.spawn(ModEntities.GUH.get(), POS);
        gewoon.tame(nieuw);
        helper.assertTrue(!vanNieuw.isVliegSlot() && !vanWinnaar.isVliegSlot(), "nobody on it: no lock");
        // the winner on the Enderguh of somebody who didn't win: it flies
        helper.assertTrue(winnaar.startRiding(vanNieuw) && !vanNieuw.isVliegSlot(), "a winner flies on any Enderguh");
        // who didn't win on the winner's Vahoege Enderguh: it walks
        vanWinnaar.setNoGravity(true);   // (it was flying around by itself)
        helper.assertTrue(nieuw.startRiding(vanWinnaar) && vanWinnaar.isVliegSlot(), "no flying for a rider who didn't beat Opper-Mika");
        helper.assertTrue(!vanWinnaar.isNoGravity(), "it comes down and walks");
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(vanWinnaar.isVliegSlot() && !vanNieuw.isVliegSlot(), "it stays that way while they ride");
            helper.assertTrue(!vlucht(nieuw) && !vlucht(winnaar), "Vadsvlucht is not for getting on (nor for a locked Enderguh)");
            GuhQuests.saved(nieuw).putInt(GuheindeGevecht.WINS, 1);   // ...and then they beat Opper-Mika
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(!vanWinnaar.isVliegSlot(), "after beating Opper-Mika the Enderguh flies with you");
            GuhQuests.saved(nieuw).putInt(GuheindeGevecht.WINS, 0);
            nieuw.stopRiding();
            winnaar.stopRiding();
        });
        helper.runAfterDelay(120, () -> {     // (getting on again takes a moment: the boarding cooldown)
            helper.assertTrue(nieuw.startRiding(gewoon) && !gewoon.isVliegSlot(), "an ordinary guh has no flying lock");
            nieuw.stopRiding();
            helper.assertTrue(!vanWinnaar.isVliegSlot(), "nobody on it any more: no lock");
            done(helper, nieuw, winnaar);
        });
    }

    private static boolean vlucht(ServerPlayer player) {
        var holder = player.level().getServer().getAdvancements().get(Guhs.id("quest/ride_ender"));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** "Vadsvlucht" (quest/ride_ender) is for really flying on your Enderguh: in the air, with the flying lock open. */
    @GuhTest(template = EMPTY, batch = "b128_ender_vlucht", timeoutTicks = 200)
    public static void vadsvluchtIsForReallyFlying(GameTestHelper helper) {
        ServerPlayer winnaar = player(helper, true);
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);   // (the empty template has no floor)
            }
        }
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        guh.setVariant(GuhVariant.ENDER);
        guh.tame(winnaar);
        boolean op = winnaar.startRiding(guh);
        helper.assertTrue(op && !guh.isVliegSlot(), "on it, no lock: " + op + " " + guh.isVliegSlot());
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(!vlucht(winnaar) && !guh.isVliegtMetRuiter(), "sitting on it on the ground is no Vadsvlucht: y " + guh.getY() + " ground " + guh.onGround() + " adv " + vlucht(winnaar)
                    + " flying " + guh.isVliegtMetRuiter() + " below " + helper.getLevel().getBlockState(guh.blockPosition().below()));
            Vec3 hoog = helper.absoluteVec(new Vec3(2.5, 12, 2.5));
            guh.setNoGravity(true);     // (the rider's client steers a flying Enderguh; here the test holds it up)
            guh.snapTo(hoog.x, hoog.y, hoog.z, 0f, 0f);
            guh.setOnGround(false);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 50 && vlucht(winnaar), "flying on the Enderguh: Vadsvlucht; flying=" + guh.isVliegtMetRuiter());
            winnaar.stopRiding();
            helper.getLevel().removePlayerImmediately(winnaar, Entity.RemovalReason.DISCARDED);
        });
    }

    /** Wild Enderguhs only come near a player who beat Opper-Mika: few, come-and-go, and only such a player tames one. */
    @GuhTest(template = EMPTY, batch = "b128_ender_topup", timeoutTicks = 200)
    public static void wildEnderguhsOnlyComeToWhoBeatOpperMika(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer nieuw = player(helper, false), winnaar = player(helper, true);
        level.getEntitiesOfClass(GuhEntity.class, winnaar.getBoundingBox().inflate(160), Enderguhs::isWild).forEach(Entity::discard);
        int cx = winnaar.blockPosition().getX() >> 4, cz = winnaar.blockPosition().getZ() >> 4;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                level.getChunk(cx + dx, cz + dz);   // (the top-up only uses loaded chunks)
            }
        }
        RandomSource random = RandomSource.create(1280);
        for (int i = 0; i < 200; i++) {
            helper.assertTrue(Enderguhs.aanvullen(level, nieuw, random) == null, "nothing for a player who didn't beat Opper-Mika");
        }
        helper.assertTrue(level.getEntitiesOfClass(GuhEntity.class, winnaar.getBoundingBox().inflate(160), Enderguhs::isWild).isEmpty(), "no wild Enderguh yet");
        GuhEntity eerste = null;
        for (int i = 0; i < 400 && eerste == null; i++) {
            eerste = Enderguhs.aanvullen(level, winnaar, random);
        }
        helper.assertTrue(eerste != null, "a wild Enderguh comes to the player who beat Opper-Mika");
        double afstand = Math.sqrt(eerste.distanceToSqr(winnaar.getX(), eerste.getY(), winnaar.getZ()));
        helper.assertTrue(eerste.getVariant() == GuhVariant.ENDER && !eerste.isTame() && eerste.getGuhScale() >= GuhEntity.RIDEABLE_SCALE
                && afstand >= 20 && afstand <= 52, "a big wild Enderguh, a bit away: " + afstand);
        helper.assertTrue(eerste.isKomEnGaGuh() && eerste.removeWhenFarAway(0) && !eerste.shouldBeSaved(), "it comes and goes (never saved)");
        for (int i = 0; i < 600; i++) {
            Enderguhs.aanvullen(level, winnaar, random);
        }
        int wild = level.getEntitiesOfClass(GuhEntity.class, winnaar.getBoundingBox().inflate(160), Enderguhs::isWild).size();
        helper.assertTrue(wild >= 1 && wild <= Enderguhs.VOL_WIJD, "a low cap: " + wild);
        // never while Opper-Mika is there
        GuheindeGevecht gevecht = new GuheindeGevecht();
        helper.assertTrue(!Enderguhs.bezig(gevecht), "no fight: the top-up runs");
        gevecht.fightActive = true;
        helper.assertTrue(Enderguhs.bezig(gevecht), "not during the fight");
        // taming: only who beat Opper-Mika
        nieuw.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 2));
        eerste.mobInteract(nieuw, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(!eerste.isTame() && nieuw.getMainHandItem().getCount() == 2, "not tameable by who didn't beat Opper-Mika");
        nieuw.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 64));
        for (int i = 0; i < 64; i++) {
            eerste.mobInteract(nieuw, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(!eerste.isTame() && nieuw.getMainHandItem().getCount() == 64, "not with ordinary knabbels either (they stay yours)");
        winnaar.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 1));
        eerste.mobInteract(winnaar, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(eerste.isTame() && eerste.isOwnedBy(winnaar) && !eerste.isKomEnGaGuh() && eerste.shouldBeSaved(), "the winner tames it, and then it stays");
        eerste.discard();
        level.getEntitiesOfClass(GuhEntity.class, winnaar.getBoundingBox().inflate(160), Enderguhs::isWild).forEach(Entity::discard);
        done(helper, nieuw, winnaar);
    }

    // --- the cheese fountains --------------------------------------------------------------------------------------------------

    /**
     * The grand cheese fountain's chest only gives kaasknabbels, a few fried ones and a little iron and gold, and there is no
     * vads ore in it any more; the small fountain has no chest at all.
     */
    @GuhTest(template = EMPTY, batch = "b128_fontein")
    public static void cheeseFountainsGiveModestLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Set<Item> mag = Set.of(ModItems.KAAS_KNABBELS.get(), ModItems.GEFRITUURDE_KAASKNABBELS.get(), Items.IRON_NUGGET, Items.GOLD_NUGGET,
                Items.IRON_INGOT, Items.GOLD_INGOT);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(1, 1, 1)))
                .create(LootContextParamSets.CHEST);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/grand_cheese_fountain")));
        helper.assertTrue(table != LootTable.EMPTY, "the loot table of the grand cheese fountain");
        helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/cheese_fountain")))
                == LootTable.EMPTY, "the small fountain has no loot table");
        Set<Item> gezien = new java.util.HashSet<>();
        for (int i = 0; i < 500; i++) {
            int gefrituurd = 0, staven = 0;
            for (ItemStack s : table.getRandomItems(params)) {
                helper.assertTrue(mag.contains(s.getItem()), "not allowed in a fountain chest: " + s);
                gezien.add(s.getItem());
                gefrituurd += s.is(ModItems.GEFRITUURDE_KAASKNABBELS.get()) ? s.getCount() : 0;
                staven += s.is(Items.IRON_INGOT) || s.is(Items.GOLD_INGOT) ? s.getCount() : 0;
            }
            helper.assertTrue(gefrituurd <= 24 && staven <= 12, "a little, not a lot: " + gefrituurd + " fried, " + staven + " ingots");
        }
        helper.assertTrue(gezien.equals(mag), "knabbels, fried knabbels, iron and gold: " + gezien);
        // the templates: no vads ore (the orb of the grand fountain was vads ore), one modest chest in the grand one, none in the small one
        StructurePlaceSettings settings = new StructurePlaceSettings();
        for (String naam : new String[]{"cheese_fountain", "grand_cheese_fountain"}) {
            StructureTemplate t = level.getStructureManager().get(Guhs.id(naam)).orElseThrow();
            List<StructureTemplate.StructureBlockInfo> kisten = t.filterBlocks(BlockPos.ZERO, settings, Blocks.CHEST);
            if (naam.equals("cheese_fountain")) {
                helper.assertTrue(kisten.isEmpty(), "the small fountain has no chest");
            } else {
                helper.assertTrue(kisten.size() == 1 && kisten.get(0).nbt() != null
                        && kisten.get(0).nbt().getStringOr("LootTable", "").equals("guhs:chests/grand_cheese_fountain"), "one chest with its own loot: " + kisten);
            }
            helper.assertTrue(t.filterBlocks(BlockPos.ZERO, settings, ModBlocks.COMPRESSED_SUPER_VAHOEGE_VADS.get()).isEmpty(), naam + ": no vads ore");
            helper.assertTrue(t.filterBlocks(BlockPos.ZERO, settings, Blocks.BARREL).isEmpty(), naam + ": no other loot");
        }
        helper.succeed();
    }

    // --- aggressive guhs ---------------------------------------------------------------------------------------------------

    private static GuhEntity vechtguh(GameTestHelper helper, ServerPlayer owner, GuhEntity.Behavior gedrag) {
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 1));
        guh.tame(owner);
        guh.setTeleportEnabled(false);
        guh.setBehavior(gedrag);
        guh.setAttackRadius(12);
        return guh;
    }

    private static <T extends Mob> T stil(GameTestHelper helper, EntityType<T> type, int x) {
        T mob = helper.spawn(type, new BlockPos(x, 1, 1));
        mob.setNoAi(true);
        return mob;
    }

    /** An aggressive guh next to sweet critters, a passive animal and a monster: it takes the monster and leaves the rest alone. */
    @GuhTest(template = WIRE_ROOM, batch = "b128_agressief", timeoutTicks = 300)
    public static void anAggressiveGuhOnlyFightsMonsters(GameTestHelper helper) {
        ServerPlayer owner = player(helper, false);
        GuhEntity guh = vechtguh(helper, owner, GuhEntity.Behavior.AGGRESSIVE);
        List<LivingEntity> lief = List.of(stil(helper, PiepFeature.PIEPPIEPMUISJE.get(), 4), stil(helper, PiepFeature.SCHILLY.get(), 5),
                stil(helper, EntityType.COW, 6), stil(helper, ModEntities.GUH_SLIME.get(), 7), stil(helper, EntityType.IRON_GOLEM, 12));
        var husk = stil(helper, EntityType.HUSK, 9);
        helper.onEachTick(() -> helper.assertTrue(guh.getTarget() == null || guh.getTarget() == husk, "only the monster is a target: " + guh.getTarget()));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 100, "watch it for a while");
            helper.assertTrue(husk.getHealth() < husk.getMaxHealth() || !husk.isAlive(), "the aggressive guh attacks the husk; target=" + guh.getTarget());
            for (LivingEntity l : lief) {
                helper.assertTrue(l.isAlive() && l.getHealth() == l.getMaxHealth(), "left alone: " + l);
            }
            lief.forEach(Entity::discard);
            helper.getLevel().removePlayerImmediately(owner, Entity.RemovalReason.DISCARDED);
        });
    }

    /** Every kind of Mika is in the tag guhs:mikas, an aggressive guh goes after a Mika, and a target that isn't allowed any more is dropped. */
    @GuhTest(template = WIRE_ROOM, batch = "b128_mika", timeoutTicks = 300)
    public static void anAggressiveGuhFightsEveryMika(GameTestHelper helper) {
        for (EntityType<?> type : List.of(ModEntities.MIKA.get(), ModEntities.NETHER_MIKA.get(), GuheindeFeature.OPPER_MIKA.get(),
                GuheindeFeature.MIKA_LARFJE.get(), KaasmoerasFeature.MOERASHEKS_MIKA.get(), SpiesburchtFeature.VONK_MIKA.get(),
                SpiesburchtFeature.KNEKEL_MIKA.get(), SpiesburchtFeature.AANGEBRANDE_MIKA.get())) {
            helper.assertTrue(Mikas.isMika(type), "in the tag guhs:mikas: " + type);
        }
        for (EntityType<?> type : List.of(ModEntities.GUH.get(), PiepFeature.SCHILLY.get(), PiepFeature.PIEPPIEPMUISJE.get(), EntityType.ZOMBIE, EntityType.COW)) {
            helper.assertTrue(!Mikas.isMika(type), "no Mika: " + type);
        }
        // the Mikas of a minigame or a job can't be hurt: they are not in the tag and no monsters, so a guh leaves them alone
        for (EntityType<?> type : List.of(ModEntities.MIKA_BAAS.get(), KnuffeldalFeature.KRUIMEL_MIKA.get(), DoolhofFeature.MIKA.get(),
                CircuitFeature.MIKAPIKKER.get(), BeroepenFeature.KNABBELDIEF_MIKA.get())) {
            Entity spel = type.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            helper.assertTrue(spel != null && !Mikas.isMika(type) && !Mikas.isVijand(spel), "a game Mika is no enemy: " + type);
            spel.discard();
        }
        ServerPlayer owner = player(helper, false), ander = player(helper, false);
        GuhEntity guh = vechtguh(helper, owner, GuhEntity.Behavior.AGGRESSIVE);
        var muisje = stil(helper, PiepFeature.PIEPPIEPMUISJE.get(), 4);
        var mikaBaas = stil(helper, ModEntities.MIKA_BAAS.get(), 5);   // (closer than the Mika: it would be picked first)
        MikaEntity mika = stil(helper, ModEntities.MIKA.get(), 8);
        guh.setTarget(mikaBaas);
        helper.assertTrue(guh.getTarget() == null, "a game Mika is never set as the target");
        helper.assertTrue(Mikas.isMika(mika) && Mikas.isVijand(mika) && !Mikas.isVijand(muisje) && !Mikas.isMika(guh), "the helper");
        // defending never goes against somebody's pet or another guh
        Wolf hond = stil(helper, EntityType.WOLF, 14);
        hond.tame(ander);
        GuhEntity andereGuh = helper.spawn(ModEntities.GUH.get(), new BlockPos(15, 1, 1));
        andereGuh.setNoAi(true);
        helper.assertTrue(GuhEntity.nooitDoelwit(hond) && GuhEntity.nooitDoelwit(andereGuh) && !GuhEntity.nooitDoelwit(mika), "pets and guhs are never a target");
        guh.setTarget(hond);
        helper.assertTrue(guh.getTarget() == null, "a pet is never set as the target");
        guh.setTarget(andereGuh);
        helper.assertTrue(guh.getTarget() == null, "another guh neither");
        guh.setTarget(muisje);
        helper.assertTrue(guh.getTarget() == null, "a critter neither");
        int[] fase = {0};
        helper.onEachTick(() -> {
            helper.assertTrue(guh.getTarget() != mikaBaas, "the Mika-baas is left alone");
            if (fase[0] == 0 && guh.getTarget() == mika) {
                fase[0] = 1;
                guh.setBehavior(GuhEntity.Behavior.NEUTRAL);   // (neutral: the Mika did nothing to it, so it lets go)
            } else if (fase[0] == 1) {
                fase[0] = guh.getTarget() == null ? 2 : 1;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(fase[0] == 2, "it went after the Mika, and dropped it when it turned neutral: phase " + fase[0] + ", target " + guh.getTarget());
            helper.assertTrue(muisje.getHealth() == muisje.getMaxHealth(), "the muisje is fine");
            muisje.discard();
            mikaBaas.discard();
            hond.discard();
            andereGuh.discard();
            helper.getLevel().removePlayerImmediately(owner, Entity.RemovalReason.DISCARDED);
            helper.getLevel().removePlayerImmediately(ander, Entity.RemovalReason.DISCARDED);
        });
    }
}
