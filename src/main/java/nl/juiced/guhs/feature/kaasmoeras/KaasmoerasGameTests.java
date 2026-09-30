package nl.juiced.guhs.feature.kaasmoeras;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhWorldData;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * GameTests of the kaasmoeras: bouncing on borrelende kaassaus, the motknabbel colours, kikkerguhs snapping up
 * kaasmotten, kaasmotten stealing kaasknabbels, the Moerasheks-Mika and her drankjes, the Kaasmoerasguh, the Guhdex
 * pages, the pools (with their knabbelvlotje) and the paalhut template.
 * (The biome itself lives in the Guhmension, which the GameTest server doesn't have: the worldgen is checked with a
 * dev server, see NOTES_kaasmoeras.md.)
 */
public class KaasmoerasGameTests {
    private static final String EMPTY = "empty";
    private static final String POEL = "kaasmoeras_poeltest";
    private static final String HUT = "moerasheks_hut";

    // --- borrelende kaassaus ---------------------------------------------------------------------------------------------

    @GuhTest(template = POEL, timeoutTicks = 120)
    public static void borrelendeKaassausBouncesAndNeverHurts(GameTestHelper helper) {
        // (the template's layers are at y 1..8 here: the grass is at y 3)
        helper.setBlock(new BlockPos(13, 3, 13), KaasmoerasFeature.BORRELENDE_KAASSAUS.get());
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(13, 8, 13));    // (a fall of over 4 blocks: that would hurt)
        float health = pig.getHealth();
        boolean[] bounced = {false};
        double[] fastest = {-9, 99};
        helper.onEachTick(() -> {
            fastest[0] = Math.max(fastest[0], pig.getDeltaMovement().y);
            fastest[1] = Math.min(fastest[1], pig.getY() - helper.absolutePos(BlockPos.ZERO).getY());
            if (pig.getDeltaMovement().y > 0.6) {
                bounced[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(bounced[0], "the pig should bounce up from the bubbling cheese (fastest up " + fastest[0] + ", lowest " + fastest[1]
                    + ")");
            helper.assertTrue(pig.getHealth() == health, "a fall into borrelende kaassaus never hurts: " + pig.getHealth());
            pig.discard();
        });
    }

    @GuhTest(template = EMPTY)
    public static void sneakingWadesThroughBorrelendeKaassaus(GameTestHelper helper) {
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        try {
            BlockPos pos = helper.absolutePos(new BlockPos(2, 0, 2));
            helper.getLevel().setBlockAndUpdate(pos, KaasmoerasFeature.BORRELENDE_KAASSAUS.get().defaultBlockState());
            BlockState state = helper.getLevel().getBlockState(pos);
            player.setDeltaMovement(Vec3.ZERO);
            player.setShiftKeyDown(true);
            state.getBlock().stepOn(helper.getLevel(), pos, state, player);
            helper.assertTrue(player.getDeltaMovement().y < 0.1, "sneaking: no bounce");
            player.setShiftKeyDown(false);
            player.fallDistance = 20;
            state.getBlock().stepOn(helper.getLevel(), pos, state, player);
            helper.assertTrue(player.getDeltaMovement().y == BorrelendeKaassausBlock.BOUNCE, "walking on it: BOING");
            helper.assertTrue(player.fallDistance == 0, "and the fall is forgotten");
            helper.assertTrue(state.getLightEmission(helper.getLevel(), pos) > 0, "it glows a little");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    // --- motknabbels -----------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void motknabbelKeepsItsColour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(2, 1, 2);
        for (MotknabbelBlock.Kleur kleur : MotknabbelBlock.Kleur.values()) {
            ItemStack stack = MotknabbelBlock.stack(kleur, 1);
            helper.assertTrue(MotknabbelBlock.kleur(stack) == kleur, "the item knows its colour: " + kleur);
            BlockState placed = stack.get(DataComponents.BLOCK_STATE).apply(KaasmoerasFeature.MOTKNABBEL.get().defaultBlockState());
            helper.assertTrue(placed.getValue(MotknabbelBlock.KLEUR) == kleur, "it places in its colour: " + kleur);
            helper.setBlock(pos, placed);
            List<ItemStack> drops = Block.getDrops(placed, level, helper.absolutePos(pos), null);
            helper.assertTrue(drops.size() == 1 && MotknabbelBlock.kleur(drops.get(0)) == kleur, "and drops in its colour: " + drops);
            helper.assertTrue(placed.getLightEmission(level, helper.absolutePos(pos)) == 15, "a real lamp");
            helper.assertTrue(stack.getHoverName().getString().length() > 0, "a name");
        }
        helper.succeed();
    }

    // --- kikkerguhs and kaasmotten -------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY, timeoutTicks = 200)
    public static void kikkerguhSnapsUpAKaasmotAndSpitsOutAMotknabbel(GameTestHelper helper) {
        KikkerguhEntity kikker = helper.spawn(KaasmoerasFeature.KIKKERGUH.get(), new BlockPos(1, 1, 1));
        kikker.setKleur(MotknabbelBlock.Kleur.MINT);
        KaasmotEntity mot = helper.spawn(KaasmoerasFeature.KAASMOT.get(), new BlockPos(3, 2, 3));
        helper.succeedWhen(() -> {
            helper.assertTrue(!mot.isAlive(), "the kaasmot should be snapped up");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12),
                    i -> i.getItem().is(KaasmoerasFeature.MOTKNABBEL_ITEM.get()));
            helper.assertTrue(drops.size() == 1 && MotknabbelBlock.kleur(drops.get(0).getItem()) == MotknabbelBlock.Kleur.MINT,
                    "one mint motknabbel: " + drops.size());
            helper.assertTrue(!kikker.canSnap(), "then it rests a while");
            drops.forEach(Entity::discard);
            kikker.discard();
        });
    }

    @GuhTest(template = EMPTY)
    public static void kikkerguhsAreFriendlyAndBreedWithKaasknabbels(GameTestHelper helper) {
        KikkerguhEntity a = helper.spawn(KaasmoerasFeature.KIKKERGUH.get(), new BlockPos(1, 1, 1));
        KikkerguhEntity b = helper.spawn(KaasmoerasFeature.KIKKERGUH.get(), new BlockPos(3, 1, 3));
        a.setKleur(MotknabbelBlock.Kleur.ROZE);
        b.setKleur(MotknabbelBlock.Kleur.GEEL);
        helper.assertTrue(a.isFood(new ItemStack(ModItems.KAAS_KNABBELS.get())), "kaasknabbels are kikkerguh food");
        helper.assertTrue(a.getTarget() == null && a.getAttribute(Attributes.ATTACK_DAMAGE) == null, "a kikkerguh never attacks");
        for (int i = 0; i < 20; i++) {
            KikkerguhEntity baby = (KikkerguhEntity) a.getBreedOffspring(helper.getLevel(), b);
            helper.assertTrue(baby != null && (baby.getKleur() == MotknabbelBlock.Kleur.ROZE || baby.getKleur() == MotknabbelBlock.Kleur.GEEL),
                    "a baby has the colour of a parent");
            baby.discard();
        }
        a.discard();
        b.discard();
        helper.succeed();
    }

    /**
     * 2.8: "een guh die een kikker is": the model has the guh head with both round ears and still all the frog parts the
     * animations move (throat pouch, tongue, four legs); a texture per colour; its hitbox fits the bigger guh head.
     */
    @GuhTest(template = EMPTY)
    public static void kikkerguhIsAGuhThatIsAFrog(GameTestHelper helper) {
        KikkerguhEntity kikker = helper.spawn(KaasmoerasFeature.KIKKERGUH.get(), new BlockPos(1, 1, 1));
        helper.assertTrue(kikker.getBbHeight() >= 0.75f && kikker.getBbWidth() <= 0.7f, "its hitbox: " + kikker.getBbWidth() + " x " + kikker.getBbHeight());
        helper.assertTrue(kikker.getTarget() == null, "friendly");
        var geo = nl.juiced.guhs.feature.reisguh.ReisguhGameTests.json("/assets/guhs/geckolib/models/entity/kikkerguh.geo.json");
        helper.assertTrue(geo != null, "the model");
        var bones = nl.juiced.guhs.feature.reisguh.ReisguhGameTests.bones(geo);
        helper.assertTrue("head".equals(bones.get("ear_left")) && "head".equals(bones.get("ear_right")), "the round guh ears on its head");
        for (String b : new String[]{"body", "head", "throat", "tongue", "leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right", "tail"}) {
            helper.assertTrue(bones.containsKey(b), "frog part " + b);
        }
        var anims = nl.juiced.guhs.feature.reisguh.ReisguhGameTests.json("/assets/guhs/geckolib/animations/entity/kikkerguh.animation.json");
        for (String a : new String[]{"idle", "hop", "swim", "croak", "tongue"}) {
            helper.assertTrue(anims != null && anims.getAsJsonObject("animations").has("animation.kikkerguh." + a), "animation " + a);
        }
        for (MotknabbelBlock.Kleur k : MotknabbelBlock.Kleur.values()) {
            helper.assertTrue(Guhs.class.getResource("/assets/guhs/textures/entity/kikkerguh_" + k.getSerializedName() + ".png") != null, "texture " + k);
        }
        kikker.discard();
        helper.succeed();
    }

    @GuhTest(template = EMPTY, timeoutTicks = 200)
    public static void kaasmotStealsKaasknabbelsAndLovesCheese(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 0), ModBlocks.BLOCK_OF_KAASKNABBELS.get());
        KaasmotEntity mot = helper.spawn(KaasmoerasFeature.KAASMOT.get(), new BlockPos(3, 2, 3));
        helper.assertTrue(helper.absolutePos(new BlockPos(0, 1, 0)).equals(mot.findLure()), "it finds the cheese nearby: " + mot.findLure());
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.1, 1.5));
        ItemEntity snack = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(ModItems.KAAS_KNABBELS.get(), 3));
        snack.setPickUpDelay(10000);
        snack.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(snack);
        helper.succeedWhen(() -> {
            helper.assertTrue(!snack.isAlive() || snack.getItem().getCount() < 3, "a Mika moth can't resist kaasknabbels on the ground");
            snack.discard();
            mot.discard();
        });
    }

    // --- the Moerasheks-Mika ------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void moerasheksThrowsWeakDrankjesAndDropsMoeraskaas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        try {
            player.setGameMode(GameType.SURVIVAL);
            player.snapTo(helper.absoluteVec(new Vec3(2.5, 1, 4.5)));
            MoerasheksMikaEntity heks = helper.spawn(KaasmoerasFeature.MOERASHEKS_MIKA.get(), new BlockPos(2, 1, 1));
            AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8);
            heks.performRangedAttack(player, 1f);
            List<VadsverdrijvendDrankjeEntity> thrown = level.getEntitiesOfClass(VadsverdrijvendDrankjeEntity.class, area);
            helper.assertTrue(thrown.size() == 1 && thrown.get(0).getOwner() == heks, "she throws a drankje: " + thrown.size());
            thrown.forEach(Entity::discard);

            double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            VadsverdrijvendDrankjeEntity drankje = new VadsverdrijvendDrankjeEntity(level, heks);
            drankje.setPos(player.position());
            helper.assertTrue(drankje.splash(level) >= 1, "the drankje hits the player");
            MobEffectInstance effect = player.getEffect(KaasmoerasFeature.ONVAHOEG);
            helper.assertTrue(effect != null && effect.getAmplifier() == 0 && effect.getDuration() <= VadsverdrijvendDrankjeEntity.SECONDS * 20,
                    "a short, weak onvahoeg: " + effect);
            helper.assertTrue(!heks.hasEffect(KaasmoerasFeature.ONVAHOEG), "a Mika is immune to her own recipe");
            double slower = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(slower < speed && slower > speed * 0.8, "a bit slower, not much: " + speed + " -> " + slower);
            // (1.1.0: FoodData has no exhaustion getter any more: read it from the player's save data)
            float exhaustion = nl.juiced.guhs.storage.Nbt.saveWithoutId(player).getFloatOr("foodExhaustionLevel", 0f);
            KaasmoerasFeature.ONVAHOEG.value().applyEffectTick(level, player, 0);
            helper.assertTrue(nl.juiced.guhs.storage.Nbt.saveWithoutId(player).getFloatOr("foodExhaustionLevel", 0f) > exhaustion, "and a rumbling tummy");
            helper.assertTrue(player.getHealth() == player.getMaxHealth(), "it never hurts");
            drankje.discard();

            heks.setHealth(5f);
            helper.assertTrue(heks.nibble() && heks.hasEffect(MobEffects.REGENERATION), "hurt: she nibbles her moeraskaas");
            helper.assertTrue(!heks.nibble(), "but not again right away");

            heks.hurtServer(level, level.damageSources().playerAttack(player), 1000f);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, area, i -> i.getItem().is(KaasmoerasFeature.MOERASKAAS.get()));
            helper.assertTrue(!heks.isAlive() && !drops.isEmpty(), "she drops moeraskaas");
            level.getEntitiesOfClass(ItemEntity.class, area).forEach(Entity::discard);
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    // --- the Kaasmoerasguh and the Guhdex -----------------------------------------------------------------------------------

    @GuhTest(template = EMPTY)
    public static void kaasmoerasguhOnlyComesFromTheKaasmoeras(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(!GuhVariant.KAASMOERASGUH.isCharacter() && GuhVariant.KAASMOERASGUH.weight == 0,
                "a real variant, never rolled anywhere else");
        helper.assertTrue(GuhDex.TAMEABLE.contains(GuhVariant.KAASMOERASGUH), "tameable, with a Guhdex star");
        helper.assertTrue(Guhs.class.getResource("/assets/guhs/textures/entity/guh_kaasmoerasguh.png") != null, "its texture");
        int made = 0;
        for (int i = 0; i < 60; i++) {
            GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (KaasmoerasEvents.decide(guh, true)) {
                made++;
                helper.assertTrue(guh.getVariant() == GuhVariant.KAASMOERASGUH, "it became a Kaasmoerasguh");
            }
            helper.assertTrue(!KaasmoerasEvents.decide(guh, true), "decided only once per guh");
            guh.discard();
        }
        helper.assertTrue(made > 5 && made < 55, "some of the guhs in the kaasmoeras, not all: " + made);
        GuhEntity outside = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        helper.assertTrue(!KaasmoerasEvents.decide(outside, false), "not outside the kaasmoeras");
        GuhEntity baby = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        baby.setAge(-24000);
        helper.assertTrue(!KaasmoerasEvents.decide(baby, true), "not babies (they have their parents' colour)");
        GuhEntity mint = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        mint.setVariant(GuhVariant.MINT);
        helper.assertTrue(!KaasmoerasEvents.decide(mint, true) && mint.getVariant() == GuhVariant.MINT, "other variants stay what they are");
        GuhEntity here = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 2));
        helper.assertTrue(!KaasmoerasEvents.maybeMoerasguh(level, here) || here.getVariant() != GuhVariant.KAASMOERASGUH,
                "no kaasmoeras in the GameTest world");
        for (GuhEntity g : List.of(outside, baby, mint, here)) {
            g.discard();
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void guhdexHasTheKaasmoerasPages(GameTestHelper helper) {
        for (GuhVariant v : List.of(GuhVariant.KIKKERGUH, GuhVariant.KAASMOT, GuhVariant.MOERASHEKS_MIKA)) {
            helper.assertTrue(v.isCharacter() && v.npcKind() == null, v + ": a creature page (not a guh character)");
            helper.assertTrue(GuhDex.ENTRIES.contains(v) && !GuhDex.TAMEABLE.contains(v), v + ": in the Guhdex, can't be tamed");
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getValue(Guhs.id(v.id())) == KaasmoerasEvents.creaturePages().get(v),
                    v + ": the page's id is the entity id");
        }
        helper.assertTrue(GuhVariant.MIJNGUH.npcKind() == GuhNpcEntity.Kind.MIJNGUH, "the guh characters still have their kind");
        helper.assertTrue(GuhDex.ENTRIES.contains(GuhVariant.KAASMOERASGUH), "the Kaasmoerasguh has a page");
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        try {
            KikkerguhEntity kikker = helper.spawn(KaasmoerasFeature.KIKKERGUH.get(), new BlockPos(2, 1, 2));
            player.snapTo(kikker.position().add(0.5, 0, 0));
            KaasmoerasEvents.seeCreatures(player);
            Set<GuhVariant> seen = EnumSet.copyOf(GuhWorldData.get(player.level().getServer()).player(player.getUUID()).seen);
            helper.assertTrue(seen.contains(GuhVariant.KIKKERGUH), "next to a kikkerguh: its page fills in");
            helper.assertTrue(!seen.contains(GuhVariant.KAASMOT), "but not the kaasmot's");
            kikker.discard();
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    // --- the pools ----------------------------------------------------------------------------------------------------------

    @GuhTest(template = POEL, timeoutTicks = 100)
    public static void poolWithAKnabbelvlotjeKeepsItsWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(13, 4, 13));
        boolean made = KaasmoerasPoelFeature.place(level, RandomSource.create(7), origin, KaasmoerasPoelFeature.Kind.VLOTJE, 6, 6, null);
        helper.assertTrue(made, "a pool on flat ground");
        int water = countWater(helper);
        helper.assertTrue(water > 40, "a real pool: " + water);
        int barrels = 0, banners = 0;
        for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(25, 8, 25)))) {
            if (level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity c && KaasmoerasEvents.VLOTJE_LOOT.equals(c.getLootTable())) {
                barrels++;
            }
            if (level.getBlockState(p).is(Blocks.PINK_BANNER)) {
                banners++;
            }
        }
        helper.assertTrue(barrels == 1 && banners == 1, "a knabbelvlotje with its barrel of loot and its guh flag: " + barrels + "/" + banners);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(countWater(helper) == water, "the water stays in its pool: " + water + " -> " + countWater(helper));
            helper.succeed();
        });
    }

    @GuhTest(template = POEL, timeoutTicks = 100)
    public static void borrelplasIsFullOfBubblingCheese(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(13, 4, 13));
        helper.assertTrue(KaasmoerasPoelFeature.place(helper.getLevel(), RandomSource.create(8), origin, KaasmoerasPoelFeature.Kind.BORREL,
                4, 4, null), "a borrelplas on flat ground");
        int borrel = 0;
        for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(25, 8, 25)))) {
            if (helper.getLevel().getBlockState(p).is(KaasmoerasFeature.BORRELENDE_KAASSAUS.get())) {
                borrel++;
            }
        }
        helper.assertTrue(borrel > 20 && countWater(helper) == 0, "bubbling cheese, no water: " + borrel);
        helper.succeed();
    }

    private static int countWater(GameTestHelper helper) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(25, 8, 25)))) {
            if (helper.getLevel().getFluidState(p).is(FluidTags.WATER)) {
                n++;
            }
        }
        return n;
    }

    // --- the paalhut ------------------------------------------------------------------------------------------------------

    @GuhTest(template = HUT, timeoutTicks = 200)
    public static void theHutTemplateIsComplete(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB box = helper.getBounds().inflate(1);
        List<MoerasheksMikaEntity> heks = level.getEntitiesOfClass(MoerasheksMikaEntity.class, box);
        helper.assertTrue(heks.size() == 1 && heks.get(0).isPersistenceRequired(), "one Moerasheks-Mika who lives here: " + heks.size());
        Set<MotknabbelBlock.Kleur> kikkers = EnumSet.noneOf(MotknabbelBlock.Kleur.class);
        level.getEntitiesOfClass(KikkerguhEntity.class, box).forEach(k -> kikkers.add(k.getKleur()));
        helper.assertTrue(kikkers.size() == 3, "kikkerguhs in all three colours: " + kikkers);
        helper.assertTrue(level.getEntitiesOfClass(GuhEntity.class, box, g -> g.getVariant() == GuhVariant.KAASMOERASGUH).size() == 1,
                "a Kaasmoerasguh grazing by the path");
        int chests = 0, vlotjes = 0, borrel = 0;
        Set<MotknabbelBlock.Kleur> lamps = EnumSet.noneOf(MotknabbelBlock.Kleur.class);
        for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(44, 45, 44)))) {
            BlockState state = level.getBlockState(p);
            if (level.getBlockEntity(p) instanceof ChestBlockEntity c && net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.LOOT_TABLE, Guhs.id("chests/moerasheks_hut")).equals(c.getLootTable())) {
                chests++;
            } else if (level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity c && KaasmoerasEvents.VLOTJE_LOOT.equals(c.getLootTable())) {
                vlotjes++;
            }
            if (state.is(KaasmoerasFeature.BORRELENDE_KAASSAUS.get())) {
                borrel++;
            }
            if (state.is(KaasmoerasFeature.MOTKNABBEL.get())) {
                lamps.add(state.getValue(MotknabbelBlock.KLEUR));
            }
        }
        helper.assertTrue(chests == 1 && vlotjes == 1, "the Moerasheks' chest and the barrel on the knabbelvlotje: " + chests + "/" + vlotjes);
        helper.assertTrue(borrel > 20, "a borrelplas: " + borrel);
        helper.assertTrue(lamps.size() == 3, "motknabbel lamps in all colours: " + lamps);
        int water = countHutWater(helper);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(countHutWater(helper) == water, "the pond keeps its water: " + water + " -> " + countHutWater(helper));
            level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
            helper.succeed();
        });
    }

    private static int countHutWater(GameTestHelper helper) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(44, 14, 44)))) {
            if (helper.getLevel().getFluidState(p).is(FluidTags.WATER)) {
                n++;
            }
        }
        return n;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }
}
