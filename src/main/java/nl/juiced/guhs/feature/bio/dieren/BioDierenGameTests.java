package nl.juiced.guhs.feature.bio.dieren;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.boerderij.BoerderijFeature;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature;
import nl.juiced.guhs.feature.kaasmoeras.KikkerguhEntity;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.wereldleven.WereldlevenFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.world.WildeDieren;

/**
 * Game tests of the biomes3 slice "dieren". The test server has no Guhmensie, so everything that asks for a biome is
 * tested through the plain rules ({@link DierenRegels}, {@link BiomeGuhs#kies}) and the entity tag
 * {@link KikkerBlad#BLADKIKKER}. Templates: biodieren_test_vijver (12 x 12, a pool two deep at x/z 2..9, lily pads at
 * (4, 4) and (7, 6); the water surface is helper y 3, the bank and the lily pads helper y 4) and biodieren_test_wei
 * (12 x 12 grass, things stand on helper y 2).
 */
public class BioDierenGameTests {
    private static final String VIJVER = "biodieren_test_vijver", WEI = "biodieren_test_wei";
    private static final String BATCH = "bio_dieren";

    /** A new mob of this type, spawned like the given spawner would (the FinalizeSpawnEvent included). */
    static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> type, EntitySpawnReason reden, double x, double y, double z) {
        ServerLevel level = helper.getLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
        }
        T mob = type.create(level, EntitySpawnReason.TRIGGERED);
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        mob.snapTo(at.x, at.y, at.z, 0, 0);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(BlockPos.containing(at)), reden, null);
        level.addFreshEntity(mob);
        return mob;
    }

    static ServerPlayer speler(GameTestHelper helper, double x, double y, double z) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        p.snapTo(at.x, at.y, at.z);
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

    static void ruimOp(GameTestHelper helper) {
        // (only this test's own plot: the tests of a batch stand next to each other)
        for (Mob m : helper.getLevel().getEntitiesOfClass(Mob.class, helper.getBounds().inflate(1),
                m -> m instanceof KoiEntity || m instanceof WolkenschaapjeEntity || m instanceof KikkerguhEntity || m instanceof GuhEntity)) {
            m.discard();
        }
    }

    static List<KoiEntity> koi(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(KoiEntity.class, helper.getBounds().inflate(1), Entity::isAlive);
    }

    /** A wild koi comes and goes; scooped up and released it keeps its colour and size, is yours and survives saving. */
    @GuhTest(template = VIJVER, batch = BATCH, timeoutTicks = 60)
    public static void bioDierenKoiEmmerHoudtKleurEnBlijft(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 1.5, 4, 1.5);
        KoiEntity wild = spawn(helper, DierenSlice.KOI.get(), EntitySpawnReason.NATURAL, 5.5, 2.2, 5.5);
        wild.setKleur(KoiEntity.Kleur.GOUD);
        wild.setKlein(true);
        wild.setGroei(5000);
        helper.assertTrue(WildeDieren.isKomEnGa(wild) && !wild.shouldBeSaved() && wild.removeWhenFarAway(1e6), "a wild koi from the natural spawner comes and goes (never saved)");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        wild.mobInteract(p, InteractionHand.MAIN_HAND);
        ItemStack emmer = p.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(wild.isRemoved() && emmer.is(DierenSlice.KOI_EMMER.get()), "a water bucket scoops it up: " + emmer);
        helper.assertTrue(KoiEmmerItem.kleur(emmer) == KoiEntity.Kleur.GOUD && KoiEmmerItem.klein(emmer), "the emmer knows its koi (golden, a kleintje)");
        BlockPos water = helper.absolutePos(new BlockPos(7, 2, 7));
        DierenSlice.KOI_EMMER.get().checkExtraContent(p, level, emmer, water);
        List<KoiEntity> uit = level.getEntitiesOfClass(KoiEntity.class, new AABB(water).inflate(1.5), Entity::isAlive);
        helper.assertTrue(uit.size() == 1, "one koi comes out of the emmer: " + uit.size());
        KoiEntity mijn = uit.get(0);
        helper.assertTrue(mijn.kleur() == KoiEntity.Kleur.GOUD && mijn.isKlein() && mijn.groei() == 5000, "the same koi: golden, still a kleintje, as far grown");
        helper.assertTrue(mijn.fromBucket() && !WildeDieren.isKomEnGa(mijn) && !WildeDieren.isWild(mijn) && mijn.shouldBeSaved() && !mijn.removeWhenFarAway(1e6)
                && mijn.requiresCustomPersistence() && !WildeDieren.magWeg(mijn, 1e9), "released from the emmer it is yours: saved, never despawns");
        // through its saved data and back (what a chunk reload does)
        CompoundTag tag = Nbt.saveWithoutId(mijn);
        KoiEntity terug = DierenSlice.KOI.get().create(level, EntitySpawnReason.LOAD);
        Nbt.load(terug, tag);
        helper.assertTrue(terug.kleur() == KoiEntity.Kleur.GOUD && terug.isKlein() && terug.fromBucket() && terug.shouldBeSaved(),
                "after saving and loading: the same golden kleintje, still yours");
        // every colour survives the emmer; an emmer that never held a koi gives a random one
        for (KoiEntity.Kleur k : KoiEntity.Kleur.values()) {
            DierenSlice.KOI_EMMER.get().checkExtraContent(p, level, KoiEmmerItem.met(k, false), water);
        }
        List<KoiEntity.Kleur> kleuren = new ArrayList<>();
        for (KoiEntity k : level.getEntitiesOfClass(KoiEntity.class, new AABB(water).inflate(1.5), e -> e.isAlive() && !e.isKlein())) {
            kleuren.add(k.kleur());
        }
        helper.assertTrue(kleuren.size() == 5 && kleuren.containsAll(List.of(KoiEntity.Kleur.values())), "all five colours come out as they went in: " + kleuren);
        terug.discard();
        ruimOp(helper);
        weg(helper, p);
        helper.succeed();
    }

    /** Feeding: hearts for everyone, one kleintje for a rested pair, none while they rest or when the pond is full. */
    @GuhTest(template = VIJVER, batch = BATCH, timeoutTicks = 60)
    public static void bioDierenKoiVoerenMetMaat(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 1.5, 4, 1.5);
        KoiEntity a = spawn(helper, DierenSlice.KOI.get(), EntitySpawnReason.NATURAL, 5.5, 2.2, 5.5);
        KoiEntity b = spawn(helper, DierenSlice.KOI.get(), EntitySpawnReason.NATURAL, 6.5, 2.2, 5.5);
        a.setKleur(KoiEntity.Kleur.ROZE);
        b.setKleur(KoiEntity.Kleur.BLAUW);
        helper.assertTrue(a.voer(p) == null && a.netGevoerd(), "one fed koi: hearts, no kleintje by itself");
        KoiEntity kleintje = b.voer(p);
        helper.assertTrue(kleintje != null && kleintje.isKlein() && kleintje.groei() == KoiEntity.GROEITIJD, "two fed koi get one kleintje");
        helper.assertTrue(WildeDieren.isKomEnGa(kleintje) && !kleintje.shouldBeSaved(), "the kleintje of two wild koi is wild too (comes and goes)");
        helper.assertTrue(!a.uitgerust() && !b.uitgerust() && a.voer(p) == null && b.voer(p) == null && koi(helper).size() == 3,
                "fed again right away: no second kleintje (both rest for " + DierenRegels.KLEINTJE_WACHT + " ticks)");
        helper.assertTrue(kleintje.voer(p) == null && kleintje.groei() < KoiEntity.GROEITIJD, "a kleintje never gets one, feeding makes it grow faster");
        // kept parents: their kleintje is kept too
        a.vergeetKleintje();
        b.vergeetKleintje();
        a.setFromBucket(true);
        KoiEntity tweede = a.voer(p);   // (b was fed a moment ago: still counts)
        helper.assertTrue(tweede != null && tweede.fromBucket() && tweede.shouldBeSaved() && !WildeDieren.isKomEnGa(tweede), "a kept koi's kleintje is kept (saved)");
        // a full pond: no more
        while (koi(helper).size() < DierenRegels.KOI_MAX_VIJVER) {
            spawn(helper, DierenSlice.KOI.get(), EntitySpawnReason.SPAWN_ITEM_USE, 4.5 + koi(helper).size() % 4, 2.2, 7.5);
        }
        a.vergeetKleintje();
        b.vergeetKleintje();
        helper.assertTrue(a.voer(p) == null && b.voer(p) == null && koi(helper).size() == DierenRegels.KOI_MAX_VIJVER, "a pond with " + DierenRegels.KOI_MAX_VIJVER + " koi gets no kleintje");
        helper.assertTrue(DierenRegels.kleintjeMag(true, true, true, DierenRegels.KOI_MAX_VIJVER - 1) && !DierenRegels.kleintjeMag(true, true, true, DierenRegels.KOI_MAX_VIJVER)
                && !DierenRegels.kleintjeMag(false, true, true, 2) && !DierenRegels.kleintjeMag(true, false, true, 2) && !DierenRegels.kleintjeMag(true, true, false, 2),
                "the kleintje rule: grown, just fed, rested, room in the pond");
        ruimOp(helper);
        weg(helper, p);
        helper.succeed();
    }

    /** Holding koivoer on the bank brings a koi up to the surface towards you; sprinkled koivoer is eaten. */
    @GuhTest(template = VIJVER, batch = BATCH, timeoutTicks = 400)
    public static void bioDierenKoiKomtNaarKoivoer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 1.5, 4, 5.5);
        KoiEntity k = spawn(helper, DierenSlice.KOI.get(), EntitySpawnReason.SPAWN_ITEM_USE, 8.5, 2.1, 5.5);
        helper.assertTrue(!WildeDieren.isKomEnGa(k), "a koi from a spawn egg is not come-and-go");
        double start = k.distanceTo(p);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DierenSlice.KOIVOER.get()));
        double boven = helper.absoluteVec(new Vec3(0, 3.3, 0)).y;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(k.getY() > boven && k.distanceTo(p) < start - 3.0,
                        "the koi comes up to the surface near the player: y " + k.getY() + " (surface above " + boven + "), distance " + k.distanceTo(p) + " from " + start))
                .thenExecute(() -> {
                    p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    Vec3 plek = helper.absoluteVec(new Vec3(6.5, 3.9, 7.5));
                    helper.assertTrue(KoivoerItem.strooi(level, p, plek) == 1 && k.voerActief(), "sprinkled koivoer lures the koi");
                })
                .thenWaitUntil(() -> helper.assertTrue(k.netGevoerd() && !k.voerActief(), "the koi swims to the koivoer and eats it"))
                .thenExecute(() -> {
                    ruimOp(helper);
                    weg(helper, p);
                })
                .thenSucceed();
    }

    /** The wolkenschaapje: wild ones come and go, shears give wolkenpluis and the fluff grows back, knabbelvoer or a lead keeps it. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 200)
    public static void bioDierenWolkenschaapje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 2.5, 2, 2.5);
        WolkenschaapjeEntity wild = spawn(helper, DierenSlice.WOLKENSCHAAPJE.get(), EntitySpawnReason.NATURAL, 5.5, 2, 5.5);
        WolkenschaapjeEntity aanLijn = spawn(helper, DierenSlice.WOLKENSCHAAPJE.get(), EntitySpawnReason.NATURAL, 8.5, 2, 5.5);
        WolkenschaapjeEntity wereld = spawn(helper, DierenSlice.WOLKENSCHAAPJE.get(), EntitySpawnReason.CHUNK_GENERATION, 8.5, 2, 8.5);
        helper.assertTrue(WildeDieren.isKomEnGa(wild) && !wild.shouldBeSaved() && WildeDieren.magWeg(wild, 200.0 * 200.0), "a wild wolkenschaapje comes and goes (never saved)");
        helper.assertTrue(!WildeDieren.isKomEnGa(wereld) && wereld.shouldBeSaved(), "one the world was made with stays, like the other animals");
        // shears
        ItemStack schaar = new ItemStack(Items.SHEARS);
        helper.assertTrue(wild.isShearable(p, schaar, level, wild.blockPosition()), "fluffy: it can be shorn");
        List<ItemStack> pluis = wild.onSheared(p, schaar, level, wild.blockPosition());
        helper.assertTrue(pluis.size() == 1 && pluis.get(0).is(Bio.item("wolkenpluis", Items.WHITE_WOOL)) && pluis.get(0).getCount() >= 1 && pluis.get(0).getCount() <= 3,
                "shears give 1 to 3 wolkenpluis: " + pluis);
        helper.assertTrue(wild.isGeschoren() && !wild.isShearable(p, schaar, level, wild.blockPosition()), "shorn: nothing more to shear");
        helper.assertTrue(WildeDieren.isKomEnGa(wild), "shearing a wild one does not make it yours");
        wild.setPluisTerug(5);
        // knabbelvoer: it is yours
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BoerderijFeature.KNABBELVOER.get(), 4));
        wild.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(wild.isGehouden() && wild.isPersistenceRequired() && !WildeDieren.isKomEnGa(wild) && wild.shouldBeSaved() && !WildeDieren.magWeg(wild, 1e9),
                "knabbelvoer makes it yours: saved, never despawns");
        helper.assertTrue(p.getMainHandItem().getCount() == 3, "it ate one knabbelvoer: " + p.getMainHandItem().getCount());
        CompoundTag tag = Nbt.saveWithoutId(wild);
        WolkenschaapjeEntity terug = DierenSlice.WOLKENSCHAAPJE.get().create(level, EntitySpawnReason.LOAD);
        Nbt.load(terug, tag);
        helper.assertTrue(terug.isGehouden() && terug.isGeschoren() && terug.shouldBeSaved(), "after saving and loading: still yours, still shorn");
        terug.discard();
        // a lead
        aanLijn.setLeashedTo(p, true);
        helper.assertTrue(!WildeDieren.isKomEnGa(aanLijn) && aanLijn.shouldBeSaved(), "on a lead it is saved at once");
        // the rules
        helper.assertTrue(DierenRegels.schaapjeMag(EntitySpawnReason.NATURAL, true, true, true, DierenRegels.SCHAAPJE_MAX_DICHTBIJ - 1)
                && !DierenRegels.schaapjeMag(EntitySpawnReason.NATURAL, true, true, true, DierenRegels.SCHAAPJE_MAX_DICHTBIJ)
                && !DierenRegels.schaapjeMag(EntitySpawnReason.NATURAL, true, true, false, 0) && !DierenRegels.schaapjeMag(EntitySpawnReason.CHUNK_GENERATION, true, true, false, 0)
                && !DierenRegels.schaapjeMag(EntitySpawnReason.NATURAL, false, true, true, 0) && !DierenRegels.schaapjeMag(EntitySpawnReason.NATURAL, true, false, true, 0)
                && DierenRegels.schaapjeMag(EntitySpawnReason.SPAWN_ITEM_USE, false, false, false, 99),
                "a wolkenschaapje spawns by itself only in the Wolkenweide, on ground, in the light, under the cap; an egg works anywhere");
        helper.assertTrue(!WolkenschaapjeEntity.checkSpawn(DierenSlice.WOLKENSCHAAPJE.get(), level, EntitySpawnReason.NATURAL, helper.absolutePos(new BlockPos(3, 2, 9)), level.getRandom()),
                "no natural spawn here: this is not the Wolkenweide");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!wild.isGeschoren() && wild.isShearable(p, schaar, level, wild.blockPosition()), "the fluff grows back"))
                .thenWaitUntil(() -> helper.assertTrue(aanLijn.isGehouden() && aanLijn.isPersistenceRequired(), "a lead makes it yours for good"))
                .thenExecute(() -> {
                    aanLijn.removeLeash();
                    helper.assertTrue(aanLijn.shouldBeSaved() && !WildeDieren.isKomEnGa(aanLijn), "off the lead again: still yours");
                    helper.assertTrue(wild.onGround() || wild.getDeltaMovement().y >= -WolkenschaapjeEntity.VALSNELHEID - 1.0e-6, "it never falls fast");
                    ruimOp(helper);
                    weg(helper, p);
                })
                .thenSucceed();
    }

    /** The lake kikkerguh: hops onto a lily pad, leaps into the water when a player comes close, comes back when they left. */
    @GuhTest(template = VIJVER, batch = BATCH, timeoutTicks = 900)
    public static void bioDierenKikkerOpBlad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KikkerguhEntity kikker = spawn(helper, KaasmoerasFeature.KIKKERGUH.get(), EntitySpawnReason.SPAWN_ITEM_USE, 1.5, 4, 4.5);
        KikkerguhEntity gewoon = spawn(helper, KaasmoerasFeature.KIKKERGUH.get(), EntitySpawnReason.SPAWN_ITEM_USE, 10.5, 4, 10.5);
        helper.assertTrue(!KikkerBlad.inGebied(gewoon) && gewoon.getAmbientSoundInterval() == 160, "an ordinary kikkerguh outside the new biomes is unchanged");
        kikker.addTag(KikkerBlad.BLADKIKKER);
        helper.assertTrue(KikkerBlad.inGebied(kikker) && KikkerBlad.isBlad(level.getBlockState(helper.absolutePos(new BlockPos(4, 4, 4)))), "a lake kikkerguh, a lily pad");
        helper.assertTrue(KikkerBlad.kwaakInterval(Dagdeel.AVOND, 160) == KikkerBlad.KWAAK_AVOND && KikkerBlad.KWAAK_AVOND < KikkerBlad.KWAAK_NACHT
                && KikkerBlad.kwaakInterval(Dagdeel.NACHT, 160) == KikkerBlad.KWAAK_NACHT && KikkerBlad.kwaakInterval(Dagdeel.DAG, 160) == 160
                && kikker.getAmbientSoundInterval() == KikkerBlad.kwaakInterval(Dagdeel.huidig(level), 160), "it croaks much more often in the evening");
        ServerPlayer[] p = new ServerPlayer[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(KikkerBlad.opBlad(kikker) && kikker.getDeltaMovement().horizontalDistanceSqr() < 1.0e-4,
                        "the kikkerguh hops onto a lily pad and sits: at " + kikker.position() + " " + kikker.blockPosition()))
                .thenExecute(() -> {
                    helper.assertTrue(!KikkerBlad.opBlad(gewoon), "the ordinary one does not care about lily pads");
                    Vec3 bij = kikker.position();
                    p[0] = speler(helper, 0, 0, 0);
                    p[0].snapTo(bij.x - 2.5, bij.y, bij.z - 0.3);
                    helper.assertTrue(KikkerBlad.schrik(kikker) == p[0], "a player this close scares it");
                    p[0].setShiftKeyDown(true);
                    helper.assertTrue(KikkerBlad.schrik(kikker) == null, "sneaking you can come closer");
                    p[0].setShiftKeyDown(false);
                    p[0].setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get()));
                    helper.assertTrue(KikkerBlad.schrik(kikker) == null, "with kaasknabbels in your hand it trusts you");
                    p[0].setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> helper.assertTrue(!KikkerBlad.opBlad(kikker) && KikkerBlad.opDeVlucht(kikker) && kikker.isInWater(),
                        "it leaps off its leaf into the water"))
                .thenExecute(() -> {
                    helper.assertTrue(kikker.isAlive() && kikker.getHealth() == kikker.getMaxHealth(), "nothing hurt it");
                    weg(helper, p[0]);
                })
                .thenWaitUntil(() -> helper.assertTrue(KikkerBlad.opBlad(kikker) && !KikkerBlad.opDeVlucht(kikker), "the coast is clear: back on a leaf"))
                .thenExecute(() -> ruimOp(helper))
                .thenSucceed();
    }

    /** Where what spawns: the plain rules, the biome guhs, no Mikas, the caps and the tidy-up, the Guhdex bonus pages. */
    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 60)
    public static void bioDierenSpawnRegelsEnGuhdex(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // koi: water, their two biomes, the cap; an egg or a bucket anywhere in water
        helper.assertTrue(DierenRegels.koiMag(EntitySpawnReason.NATURAL, true, true, DierenRegels.KOI_MAX_DICHTBIJ - 1)
                && !DierenRegels.koiMag(EntitySpawnReason.NATURAL, true, true, DierenRegels.KOI_MAX_DICHTBIJ)
                && !DierenRegels.koiMag(EntitySpawnReason.NATURAL, true, false, 0) && !DierenRegels.koiMag(EntitySpawnReason.CHUNK_GENERATION, true, false, 0)
                && !DierenRegels.koiMag(EntitySpawnReason.NATURAL, false, true, 0) && DierenRegels.koiMag(EntitySpawnReason.BUCKET, true, false, 99)
                && DierenRegels.koiMag(EntitySpawnReason.SPAWN_ITEM_USE, true, false, 99) && !DierenRegels.koiMag(EntitySpawnReason.SPAWN_ITEM_USE, false, false, 0),
                "a koi spawns by itself only in water in its biomes, under the cap");
        helper.assertTrue(!KoiEntity.checkSpawn(DierenSlice.KOI.get(), level, EntitySpawnReason.NATURAL, helper.absolutePos(new BlockPos(3, 2, 3)), level.getRandom()),
                "no koi on dry land");
        // the kikkerguh: elsewhere unchanged, by the lake only near water and a few together
        helper.assertTrue(DierenRegels.kikkerMag(EntitySpawnReason.NATURAL, false, false, 99) && DierenRegels.kikkerMag(EntitySpawnReason.SPAWN_ITEM_USE, true, false, 99)
                && DierenRegels.kikkerMag(EntitySpawnReason.NATURAL, true, true, DierenRegels.KIKKER_MAX_DICHTBIJ - 1)
                && !DierenRegels.kikkerMag(EntitySpawnReason.NATURAL, true, true, DierenRegels.KIKKER_MAX_DICHTBIJ)
                && !DierenRegels.kikkerMag(EntitySpawnReason.NATURAL, true, false, 0), "the kikkerguh's extra rule only counts in the new biomes");
        helper.assertTrue(KikkerBlad.spawnMag(level, EntitySpawnReason.NATURAL, helper.absolutePos(new BlockPos(3, 2, 3))), "outside the new biomes a kikkerguh spawns as before");
        // no Mika is born in the three biomes
        helper.assertTrue(DierenEvents.magNietGeboren(ModEntities.MIKA.get(), EntitySpawnReason.NATURAL, true)
                && DierenEvents.magNietGeboren(KaasmoerasFeature.MOERASHEKS_MIKA.get(), EntitySpawnReason.SPAWNER, true)
                && !DierenEvents.magNietGeboren(ModEntities.MIKA.get(), EntitySpawnReason.NATURAL, false)
                && !DierenEvents.magNietGeboren(ModEntities.MIKA.get(), EntitySpawnReason.SPAWN_ITEM_USE, true)
                && !DierenEvents.magNietGeboren(DierenSlice.KOI.get(), EntitySpawnReason.NATURAL, true) && !DierenEvents.magNietGeboren(ModEntities.GUH.get(), EntitySpawnReason.NATURAL, true),
                "no Mika spawns by itself in the new biomes (everything else does)");
        // the biome guhs: each only in its own biome
        helper.assertTrue(BiomeGuhs.variantVoor(Bio.BLOESEMMEERTJE) == GuhVariant.BLOESEMGUH && BiomeGuhs.variantVoor(Bio.KLATERDAL) == GuhVariant.TANUKIGUH
                && BiomeGuhs.variantVoor(Bio.WOLKENWEIDE) == GuhVariant.WOLK && BiomeGuhs.variantVoor(null) == null
                && BiomeGuhs.variantVoor(nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature.KNUFFELDAL) == null
                && BiomeGuhs.nieuwBiome(level, helper.absolutePos(new BlockPos(3, 2, 3))) == null, "each biome its guh, nowhere else");
        int[] n = new int[4];
        for (int i = 0; i < 400; i++) {
            GuhEntity a = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED), b = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED),
                    c = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED), d = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            n[0] += BiomeGuhs.kies(a, Bio.BLOESEMMEERTJE) && a.getVariant() == GuhVariant.BLOESEMGUH ? 1 : 0;
            n[1] += BiomeGuhs.kies(b, Bio.KLATERDAL) && b.getVariant() == GuhVariant.TANUKIGUH ? 1 : 0;
            n[2] += BiomeGuhs.kies(c, Bio.WOLKENWEIDE) && c.getVariant() == GuhVariant.WOLK ? 1 : 0;
            n[3] += BiomeGuhs.kies(d, null) || d.getVariant() != GuhVariant.NORMAL ? 1 : 0;
            helper.assertTrue(a.getVariant() == GuhVariant.NORMAL || a.getVariant() == GuhVariant.BLOESEMGUH, "by the lake: a plain guh or a Bloesemguh");
            helper.assertTrue(!BiomeGuhs.kies(a, Bio.KLATERDAL) && !BiomeGuhs.kies(d, Bio.KLATERDAL), "decided once per guh");
        }
        helper.assertTrue(Math.abs(n[0] - 400 * BiomeGuhs.BLOESEMGUH_KANS) < 45 && Math.abs(n[1] - 400 * BiomeGuhs.TANUKIGUH_KANS) < 45
                && Math.abs(n[2] - 400 * BiomeGuhs.WOLKGUH_KANS) < 35 && n[2] > 0 && n[3] == 0,
                "about a quarter Bloesemguh / Tanukiguh, an eighth Wolkguh, none outside their biome: " + n[0] + " " + n[1] + " " + n[2] + " " + n[3]);
        GuhEntity mint = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        mint.setVariant(GuhVariant.MINT);
        GuhEntity kleintje = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        kleintje.setBaby(true);
        for (int i = 0; i < 1; i++) {
            helper.assertTrue(!BiomeGuhs.kies(mint, Bio.KLATERDAL) && mint.getVariant() == GuhVariant.MINT && !BiomeGuhs.kies(kleintje, Bio.KLATERDAL),
                    "only a plain grown wild guh may become the biome's guh");
        }
        // the weights stay 0 (the roll table is asserted elsewhere), the bones belong to their variant
        helper.assertTrue(GuhVariant.BLOESEMGUH.weight == 0 && GuhVariant.TANUKIGUH.weight == 0 && GuhVariant.KOI.weight == 0 && GuhVariant.WOLKENSCHAAPJE.weight == 0,
                "never rolled from the table");
        helper.assertTrue(GuhVariant.BLOESEMGUH.shows("bloesem_bloem") && GuhVariant.TANUKIGUH.shows("tanuki_staart") && !GuhVariant.NORMAL.shows("tanuki_blad")
                && !GuhVariant.BLOESEMGUH.shows("tanuki_ringen") && !GuhVariant.TANUKIGUH.shows("bloesem_hart") && !GuhVariant.PLUISGUH.shows("bloesem_blaadjes"),
                "the blossom and the ringed tail only on their own guh");
        helper.assertTrue(!GuhVariant.BLOESEMGUH.isCharacter() && !GuhVariant.TANUKIGUH.isCharacter() && GuhVariant.KOI.isCharacter() && GuhVariant.WOLKENSCHAAPJE.isCharacter()
                && GuhVariant.byId("tanukiguh") == GuhVariant.TANUKIGUH, "two real variants, two creature pages");
        // the Guhdex: four bonus pages, a complete Guhdex stays complete
        List<GuhVariant> nieuw = List.of(GuhVariant.BLOESEMGUH, GuhVariant.TANUKIGUH, GuhVariant.KOI, GuhVariant.WOLKENSCHAAPJE);
        helper.assertTrue(GuhDex.ENTRIES.containsAll(nieuw) && GuhDex.EXTRA.containsAll(nieuw) && nieuw.stream().noneMatch(GuhDex.TELLEND::contains),
                "in the Guhdex as bonus pages (they do not count for a full Guhdex)");
        helper.assertTrue(GuhDex.TAMEABLE.contains(GuhVariant.BLOESEMGUH) && GuhDex.TAMEABLE.contains(GuhVariant.TANUKIGUH)
                && GuhDex.MILESTONES.get(GuhDex.MILESTONES.size() - 1).tamed() == GuhDex.TAMEABLE.size() - 2, "tameable, but the crown does not ask for them");
        helper.assertTrue(GuhDex.isCreaturePage(GuhVariant.KOI) && GuhDex.isCreaturePage(GuhVariant.WOLKENSCHAAPJE), "creature pages");
        helper.assertTrue(!WereldlevenFeature.KNUFFEL_IDS.contains("bloesemguh") && !WereldlevenFeature.KNUFFEL_IDS.contains("tanukiguh"), "no plush: the knuffelkast stays what it was");
        // come and go, and the tidy-up above the cap
        helper.assertTrue(DierenRegels.komtEnGaat(EntitySpawnReason.NATURAL) && DierenRegels.komtEnGaat(EntitySpawnReason.SPAWNER) && !DierenRegels.komtEnGaat(EntitySpawnReason.CHUNK_GENERATION)
                && !DierenRegels.komtEnGaat(EntitySpawnReason.BUCKET) && !DierenRegels.komtEnGaat(EntitySpawnReason.BREEDING), "what comes and goes");
        helper.assertTrue(DierenRegels.max(0) == DierenRegels.MAX_BASIS && DierenRegels.max(2) == DierenRegels.MAX_BASIS + 2 * DierenRegels.MAX_PER_SPELER, "the cap per level");
        List<Mob> wild = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            wild.add(spawn(helper, DierenSlice.WOLKENSCHAAPJE.get(), EntitySpawnReason.NATURAL, 2.5 + i, 2, 6.5));
        }
        WolkenschaapjeEntity mijn = spawn(helper, DierenSlice.WOLKENSCHAAPJE.get(), EntitySpawnReason.NATURAL, 5.5, 2, 9.5);
        mijn.houd(null);
        helper.assertTrue(DierenRegels.wilde(level, DierenSlice.WOLKENSCHAAPJE.get()).containsAll(wild) && !DierenRegels.wilde(level, DierenSlice.WOLKENSCHAAPJE.get()).contains(mijn),
                "the tidy-up only ever looks at the wild come-and-go ones");
        helper.assertTrue(WildeDieren.opruimen(wild, List.of(), 4) == 2 && wild.stream().filter(Entity::isAlive).count() == 4 && mijn.isAlive(),
                "above the cap the far ones go, down to the cap; a kept one never");
        // the evening nap of the wild guhs
        helper.assertTrue(BiomeGuhs.dutjesTijd(Dagdeel.AVOND) && BiomeGuhs.dutjesTijd(Dagdeel.NACHT) && !BiomeGuhs.dutjesTijd(Dagdeel.DAG) && !BiomeGuhs.dutjesTijd(Dagdeel.OCHTEND),
                "nap time: the evening and the night");
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 2, 3));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(guh.onGround() && BiomeGuhs.slaap(guh), "a wild guh curls up"))
                .thenExecute(() -> {
                    helper.assertTrue(guh.emotes.current() == Emote.SLAPEN && BiomeGuhs.dut(guh), "asleep (the SLAPEN emote, eyes closed)");
                    if (!BiomeGuhs.dutjesTijd(Dagdeel.huidig(level))) {
                        for (int i = 0; i < 41; i++) {
                            guh.tickCount++;
                            BiomeGuhs.tick(guh);
                        }
                        helper.assertTrue(!BiomeGuhs.dut(guh) && guh.emotes.current() != Emote.SLAPEN, "by day it wakes up again");
                    } else {
                        BiomeGuhs.wakker(guh);
                        helper.assertTrue(!BiomeGuhs.dut(guh) && guh.emotes.current() != Emote.SLAPEN, "woken");
                    }
                    ruimOp(helper);
                })
                .thenSucceed();
    }
}
