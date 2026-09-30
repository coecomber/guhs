package nl.juiced.guhs.feature.waterdiertjes;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.tuintjes.TuinBlock;
import nl.juiced.guhs.feature.tuintjes.TuinPlant;
import nl.juiced.guhs.feature.tuintjes.TuintjesFeature;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the waterdiertjes slice: the guhxolotl's colours (gold is rare, babies inherit, saved), taming with a
 * guhvisje, the emmertje round trip (a water bucket and the owner's pick-up keep the band id, owner and colour), a tamed
 * guhxolotl as a guhhuisje resident that never dries out (a wild one on land does, but is never hurt by it), the mama
 * eendje's rijtje (each duckling follows the one in front), the lieveheersbeestje's growth help on a guhtuintje (once, then
 * it rests), the five counting Guhdex pages, and the spawn rules (day/night, water). Template waterdiertjes_test_wei: 14 x 14
 * grass at y 0 (things stand on helper y 2... the floor is helper y 1).
 */
public class WaterdiertjesGameTests {
    private static final String WEI = "waterdiertjes_test_wei";
    private static final String BATCH = "waterdiertjes";

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

    static GuhxolotlEntity xolotl(GameTestHelper helper, BlockPos at, GuhxolotlEntity.Kleur kleur) {
        GuhxolotlEntity x = helper.spawn(WaterdiertjesFeature.GUHXOLOTL.get(), at);
        x.setKleur(kleur);
        return x;
    }

    /** Tames it for p the way a guhvisje does (with "Lief kijken": always). */
    static void tem(GuhxolotlEntity x, ServerPlayer p) {
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(PiepFeature.LIEF_KIJKEN, 200));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUH_VIS.get()));
        x.mobInteract(p, InteractionHand.MAIN_HAND);
        p.removeEffect(PiepFeature.LIEF_KIJKEN);
    }

    // =================================================================================================================

    /** Five colours: gold about 1 in 100 in the wild, the others about equally often; the colour is saved and loaded. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void waterdiertjesKleurenEnGoud(GameTestHelper helper) {
        RandomSource r = RandomSource.create(20301401L);
        Map<GuhxolotlEntity.Kleur, Integer> n = new EnumMap<>(GuhxolotlEntity.Kleur.class);
        int N = 40000;
        for (int i = 0; i < N; i++) {
            n.merge(GuhxolotlEntity.Kleur.rol(r), 1, Integer::sum);
        }
        double goud = n.getOrDefault(GuhxolotlEntity.Kleur.GOUD, 0) / (double) N;
        helper.assertTrue(goud > 0.005 && goud < 0.02, "gold is rare: " + goud);
        for (GuhxolotlEntity.Kleur k : List.of(GuhxolotlEntity.Kleur.ROZE, GuhxolotlEntity.Kleur.MINT, GuhxolotlEntity.Kleur.CHOCO, GuhxolotlEntity.Kleur.WIT)) {
            double f = n.getOrDefault(k, 0) / (double) N;
            helper.assertTrue(f > 0.2 && f < 0.3, k + " about a quarter: " + f);
        }
        GuhxolotlEntity x = xolotl(helper, new BlockPos(3, 2, 3), GuhxolotlEntity.Kleur.GOUD);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        nl.juiced.guhs.storage.Nbt.saveWithoutId(x, tag);
        GuhxolotlEntity kopie = WaterdiertjesFeature.GUHXOLOTL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        nl.juiced.guhs.storage.Nbt.load(kopie, tag);
        helper.assertTrue(kopie.kleur() == GuhxolotlEntity.Kleur.GOUD && "goud".equals(tag.getStringOr("Kleur", "")), "saved and loaded by name");
        // a spawn egg / natural spawn rolls a colour; babies take a parent's colour (or, now and then, gold)
        GuhxolotlEntity a = xolotl(helper, new BlockPos(5, 2, 5), GuhxolotlEntity.Kleur.MINT);
        GuhxolotlEntity b = xolotl(helper, new BlockPos(6, 2, 5), GuhxolotlEntity.Kleur.MINT);
        int goudKleintjes = 0;
        for (int i = 0; i < 400; i++) {
            GuhxolotlEntity baby = (GuhxolotlEntity) a.getBreedOffspring(helper.getLevel(), b);
            helper.assertTrue(baby.kleur() == GuhxolotlEntity.Kleur.MINT || baby.kleur() == GuhxolotlEntity.Kleur.GOUD, "a mint little one");
            if (baby.kleur() == GuhxolotlEntity.Kleur.GOUD) {
                goudKleintjes++;
            }
        }
        helper.assertTrue(goudKleintjes > 0 && goudKleintjes < 40, "now and then a golden little one: " + goudKleintjes);
        helper.succeed();
    }

    /** A guhvisje tames it; a tamed one isn't hostile, has its menu settings, and is a piep-maatje. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void waterdiertjesTemmenMetGuhvisje(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        GuhxolotlEntity x = xolotl(helper, new BlockPos(4, 2, 4), GuhxolotlEntity.Kleur.CHOCO);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS));
        x.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(!x.isTame(), "seeds don't tame it");
        helper.assertTrue(x.isFood(new ItemStack(ModItems.GUH_VIS.get())) && x.isFood(new ItemStack(ModItems.GUH_VIS_BUCKET.get())), "guhvisjes are its food");
        tem(x, p);
        helper.assertTrue(x.isTame() && x.isOwnedBy(p) && Huisjes.kanBewoner(x), "tamed with a guhvisje: a maatje");
        helper.assertTrue(WaterdiertjesEvents.getemdeKleuren(p).contains(GuhxolotlEntity.Kleur.CHOCO), "its colour is remembered");
        helper.assertTrue(!x.wantsToAttack(p, p) && "guhxolotl".equals(x.soort())
                && BuiltInRegistries.ENTITY_TYPE.getKey(x.getType()).getPath().equals(x.soort()), "soort = the entity id");
        x.discard();
        weg(helper, p);
        helper.succeed();
    }

    /** The emmertje: a water bucket scoops it up, putting it down gives the same guhxolotl (band id, owner, colour) + the bucket. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void waterdiertjesEmmertjeHoudtBandId(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        GuhxolotlEntity x = xolotl(helper, new BlockPos(4, 2, 4), GuhxolotlEntity.Kleur.WIT);
        tem(x, p);
        x.setCustomName(net.minecraft.network.chat.Component.literal("Blubje"));
        UUID band = Band.id(x);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        x.mobInteract(p, InteractionHand.MAIN_HAND);
        ItemStack emmer = p.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(x.isRemoved() && emmer.is(WaterdiertjesFeature.GUHXOLOTL_EMMERTJE.get()) && GuhxolotlEmmertje.metEmmer(emmer)
                && GuhxolotlEmmertje.kleur(emmer) == GuhxolotlEntity.Kleur.WIT, "scooped into a real emmertje (white)");
        GuhxolotlEntity terug = PiepDierItem.zetNeer(emmer, level, helper.absoluteVec(new Vec3(7.5, 2, 7.5)), 0, WaterdiertjesFeature.GUHXOLOTL.get());
        helper.assertTrue(terug != null && Band.id(terug).equals(band) && terug.isOwnedBy(p) && terug.kleur() == GuhxolotlEntity.Kleur.WIT
                && "Blubje".equals(terug.getCustomName().getString()), "the same guhxolotl comes back (band id, owner, colour, name)");
        // the owner's pick-up by hand (sneak + empty hand / "Oppakken"): a knuffel-emmertje without a bucket
        helper.assertTrue(PiepDierItem.pakOp(terug, p), "picked up by its owner");
        ItemStack hand = ItemStack.EMPTY;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(WaterdiertjesFeature.GUHXOLOTL_EMMERTJE.get()) && !GuhxolotlEmmertje.metEmmer(s)) {
                hand = s;
            }
        }
        helper.assertTrue(!hand.isEmpty(), "a knuffel-emmertje in the pockets");
        // using the real emmertje on a block: water + the guhxolotl + the empty bucket back
        ServerPlayer q = speler(helper, new BlockPos(10, 2, 10));
        GuhxolotlEntity wild = xolotl(helper, new BlockPos(9, 2, 9), GuhxolotlEntity.Kleur.ROZE);
        q.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        wild.mobInteract(q, InteractionHand.MAIN_HAND);
        helper.assertTrue(wild.isRemoved() && q.getItemInHand(InteractionHand.MAIN_HAND).is(WaterdiertjesFeature.GUHXOLOTL_EMMERTJE.get()),
                "a wild one can be scooped too");
        BlockPos grond = helper.absolutePos(new BlockPos(11, 1, 11));
        q.getItemInHand(InteractionHand.MAIN_HAND).useOn(new net.minecraft.world.item.context.UseOnContext(q, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(grond), Direction.UP, grond, false)));
        helper.assertTrue(q.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the empty bucket comes back");
        helper.assertTrue(level.getBlockState(grond.above()).is(Blocks.WATER), "with a splash of water");
        helper.assertTrue(!level.getEntitiesOfClass(GuhxolotlEntity.class, new net.minecraft.world.phys.AABB(grond).inflate(2)).isEmpty(), "plons!");
        for (GuhxolotlEntity e : level.getEntitiesOfClass(GuhxolotlEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(20))) {
            e.discard();
        }
        weg(helper, p, q);
        helper.succeed();
    }

    /** A tamed guhxolotl lives in a guhhuisje and never dries out there; a wild one on land gets dry (never hurt) and wet again in water. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 200)
    public static void waterdiertjesHuisjeBewonerDroogtNietUit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        Huisje h = HuisjeBlock.bouw(level, helper.absolutePos(new BlockPos(7, 1, 2)), Direction.SOUTH, HuisjeMaat.KLEIN, p.getUUID());
        GuhxolotlEntity thuis = xolotl(helper, new BlockPos(3, 2, 8), GuhxolotlEntity.Kleur.MINT);
        tem(thuis, p);
        helper.assertTrue(Huisjes.trekIn(h, thuis) && Huisjes.isBewoner(thuis), "moved into the huisje");
        thuis.setDroogTicks(GuhxolotlEntity.DROOG_NA - 2);
        GuhxolotlEntity buiten = xolotl(helper, new BlockPos(11, 2, 11), GuhxolotlEntity.Kleur.ROZE);
        buiten.setDroogTicks(GuhxolotlEntity.DROOG_NA - 2);
        buiten.setNoAi(true);
        float leven = buiten.getHealth();
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(!thuis.isDroog() && thuis.droogTicks() == 0, "a huisje resident stays moist");
            helper.assertTrue(buiten.isDroog(), "a wild one on land gets dry after a while");
            helper.assertTrue(buiten.getHealth() >= leven && buiten.isAlive(), "but it is never hurt by it");
            buiten.snapTo(buiten.getX(), buiten.getY(), buiten.getZ());
            helper.setBlock(new BlockPos(11, 2, 11), Blocks.WATER);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(!buiten.isDroog(), "in the water it is fine again");
                Huisjes.trekUit(thuis);
                thuis.discard();
                buiten.discard();
                weg(helper, p);
                helper.succeed();
            });
        });
    }

    /** A wild mama comes with a rijtje; each duckling walks behind the one in front of it, mama first. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 400)
    public static void waterdiertjesKuikentjesVolgenInEenRijtje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhEendjeEntity mama = WaterdiertjesFeature.GUH_EENDJE.get().create(level, EntitySpawnReason.TRIGGERED);
        BlockPos start = helper.absolutePos(new BlockPos(2, 2, 2));
        mama.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 180, 0);
        mama.finalizeSpawn(level, level.getCurrentDifficultyAt(start), EntitySpawnReason.NATURAL, null);
        level.addFreshEntity(mama);
        List<GuhEendjeEntity> rij = mama.rijtje();
        helper.assertTrue(rij.size() >= GuhEendjeEntity.MIN_KUIKENS && rij.size() <= GuhEendjeEntity.MAX_KUIKENS, "a mama with 2-4 kuikentjes: " + rij.size());
        helper.assertTrue(rij.stream().allMatch(k -> k.isBaby() && mama.getUUID().equals(k.mama())), "her own babies");
        helper.assertTrue(rij.get(0).voorganger() == mama && (rij.size() < 2 || rij.get(1).voorganger() == rij.get(0)), "the first follows mama, the next the first");
        // mama walks off to the other corner: the rijtje follows
        mama.snapTo(helper.absolutePos(new BlockPos(11, 2, 11)).getX() + 0.5, start.getY(), helper.absolutePos(new BlockPos(11, 2, 11)).getZ() + 0.5, 0, 0);
        mama.setNoAi(true);
        helper.succeedWhen(() -> {
            for (GuhEendjeEntity k : rij) {
                int i = rij.indexOf(k);
                Entity voor = k.voorganger();
                helper.assertTrue(voor != null && k.distanceTo(voor) < 2.4, "kuikentje " + i + " is right behind the one in front: "
                        + (voor == null ? "-" : k.distanceTo(voor)));
                helper.assertTrue(k.distanceTo(mama) < 2.6 + i * 2.0, "kuikentje " + i + " is in mama's rijtje: " + k.distanceTo(mama));
            }
            for (GuhEendjeEntity k : rij) {
                k.discard();
            }
            mama.discard();
        });
    }

    /** A lieveheersbeestje on a growing guhtuintje helps it one step, then rests; it doesn't help a ripe or empty one. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void waterdiertjesLieveheersbeestjeHelptGroeien(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pot = helper.absolutePos(new BlockPos(5, 2, 5));
        level.setBlockAndUpdate(pot, TuintjesFeature.GUH_BLOEMPOT.get().defaultBlockState().setValue(TuinBlock.PLANT, TuinPlant.KNABBELPLANTJE));
        LieveheersbeestjeEntity lhb = helper.spawn(WaterdiertjesFeature.LIEVEHEERSBEESTJE.get(), new BlockPos(5, 4, 5));
        int voor = level.getBlockState(pot).getValue(TuinBlock.GROEI);
        helper.assertTrue(lhb.help(level, pot), "it helps");
        helper.assertTrue(level.getBlockState(pot).getValue(TuinBlock.GROEI) == voor + 1, "one step further");
        helper.assertTrue(lhb.rust() && !lhb.help(level, pot) && level.getBlockState(pot).getValue(TuinBlock.GROEI) == voor + 1, "then it rests");
        LieveheersbeestjeEntity ander = helper.spawn(WaterdiertjesFeature.LIEVEHEERSBEESTJE.get(), new BlockPos(8, 4, 8));
        BlockPos leeg = helper.absolutePos(new BlockPos(8, 2, 8));
        level.setBlockAndUpdate(leeg, TuintjesFeature.GUH_BLOEMPOT.get().defaultBlockState());
        helper.assertTrue(!ander.help(level, leeg), "an empty pot doesn't grow");
        // it looks for the growing tuintje (not the empty one) to land on, and sits on top of it
        BlockPos plek = lhb.kiesLandplek();
        helper.assertTrue(pot.equals(plek), "it picks the growing tuintje: " + plek);
        helper.assertTrue(lhb.landPunt(pot).y > pot.getY() && lhb.landPunt(pot).y <= pot.getY() + 1.01, "it sits on top of the pot");
        lhb.discard();
        ander.discard();
        helper.succeed();
    }

    /** The five pages are registered counting creature pages; every critter is friendly. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void waterdiertjesGuhdexPaginas(GameTestHelper helper) {
        for (GuhVariant v : WaterdiertjesFeature.PAGINAS) {
            helper.assertTrue(GuhDex.isCreaturePage(v) && GuhDex.TELLEND.contains(v) && v.isCharacter() && !GuhDex.TAMEABLE.contains(v),
                    v + ": a counting creature page");
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Guhs.id(v.id())), v + ": its entity exists (" + v.id() + ")");
            helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.getValue(Guhs.id(v.id())).getCategory().isFriendly(), v + ": friendly");
        }
        helper.succeed();
    }

    /** Spawn rules: butterflies and ladybirds by day near flowers, glimguhtjes only at night, guhxolotls in shallow water, ducks at the water's edge. */
    @GuhTest(template = WEI, batch = BATCH)
    public static void waterdiertjesSpawnRegels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RandomSource r = level.getRandom();
        BlockPos water = helper.absolutePos(new BlockPos(3, 1, 3));
        level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(water.east(), Blocks.WATER.defaultBlockState());
        helper.assertTrue(GuhxolotlEntity.checkSpawn(WaterdiertjesFeature.GUHXOLOTL.get(), level, EntitySpawnReason.SPAWN_ITEM_USE, water.above(5), r),
                "a spawn egg works anywhere");
        helper.assertTrue(!GuhxolotlEntity.checkSpawn(WaterdiertjesFeature.GUHXOLOTL.get(), level, EntitySpawnReason.NATURAL, water.above(3), r),
                "not in the air");
        helper.assertTrue(GuhEendjeEntity.checkSpawn(WaterdiertjesFeature.GUH_EENDJE.get(), level, EntitySpawnReason.SPAWN_ITEM_USE, water.above(), r), "egg");
        boolean dag = level.isBrightOutside();
        BlockPos lucht = helper.absolutePos(new BlockPos(7, 3, 7));
        helper.assertTrue(GlimguhtjeEntity.checkSpawn(WaterdiertjesFeature.GLIMGUHTJE.get(), level, EntitySpawnReason.NATURAL, lucht, r) == (!dag
                && GlimguhtjeEntity.checkSpawn(WaterdiertjesFeature.GLIMGUHTJE.get(), level, EntitySpawnReason.NATURAL, lucht, r)), "glimguhtjes never by day");
        if (dag) {
            helper.assertTrue(!KnabbelvlindertjeEntity.checkSpawn(WaterdiertjesFeature.KNABBELVLINDERTJE.get(), level, EntitySpawnReason.NATURAL, lucht, r),
                    "no flowers: no butterflies");
            level.setBlockAndUpdate(helper.absolutePos(new BlockPos(8, 2, 8)), Blocks.POPPY.defaultBlockState());
            helper.assertTrue(KnabbelvlindertjeEntity.checkSpawn(WaterdiertjesFeature.KNABBELVLINDERTJE.get(), level, EntitySpawnReason.NATURAL, lucht, r)
                    == level.canSeeSky(lucht), "a flower: butterflies (under the open sky)");
        }
        helper.succeed();
    }
}
