package nl.juiced.guhs.feature.einde2.mikas;

import java.util.List;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.entity.Mikas;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of einde2, slice mikas: the Ender-Mika steals one knabbel and gives it back along every way out (looked at,
 * twenty seconds, hit, shoved, the victim far away, it goes away, it dies), it is shy, a trader does none of that, it
 * trails after a Kaasmijt instead of stealing; the Kaasmijt's tickle and the hik, its two minutes, the fruit hook; the
 * Schuilguh's kusje and its doosschil a day, how it holds on; the Schuildoos keeps its contents through break and place.
 * Template einde2_mikas_test_kamer (a floor of 25 x 25 with a bit of wall and ceiling in one corner). Every test in which a
 * look matters has a batch of its own: a mock player of a neighbouring test would look along.
 */
public class E2MikasGameTests {
    private static final String KAMER = "einde2_mikas_test_kamer";
    private static final BlockPos MIDDEN = new BlockPos(12, 2, 12);
    /** The Ender-Mika's of the tests stay within this many blocks of the middle of the floor. */
    private static final int HONK = 11;

    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);       // (yaw 0: it looks south)
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static EnderMikaEntity mika(GameTestHelper helper, BlockPos at) {
        EnderMikaEntity mika = helper.spawn(E2MikasFeature.ENDER_MIKA.get(), at);
        mika.setHomeTo(helper.absolutePos(MIDDEN), HONK);
        mika.vergeetPauzes();
        return mika;
    }

    static ItemStack knabbels(int n) {
        return new ItemStack(ModItems.KAAS_KNABBELS.get(), n);
    }

    static int opDeGrond(GameTestHelper helper) {
        int n = 0;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(MIDDEN)).inflate(16, 6, 16), Entity::isAlive)) {
            if (item.getItem().is(ModItems.KAAS_KNABBELS.get())) {
                n += item.getItem().getCount();
            }
        }
        return n;
    }

    static int inZak(ServerPlayer p) {
        return p.getInventory().countItem(ModItems.KAAS_KNABBELS.get());
    }

    // =====================================================================================================================
    // the Ender-Mika
    // =====================================================================================================================

    /** It takes exactly one knabbel and nothing else; look at it for two seconds and it drops it at its feet. */
    @GuhTest(template = KAMER, batch = "einde2_mikas_kijk", timeoutTicks = 200)
    public static void einde2MikasJatEenKnabbelEnLaatHemVallenAlsJeKijkt(GameTestHelper helper) {
        ServerPlayer p = speler(helper, MIDDEN);
        EnderMikaEntity mika = mika(helper, new BlockPos(12, 2, 5));          // (7 blocks behind the player's back)
        helper.assertTrue(mika instanceof MikaEntity && Mikas.isMika(mika), "a Mika: class and tag guhs:mikas");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(!mika.jat(p) && p.getMainHandItem().getCount() == 3 && !mika.draagtKnabbel(), "it never takes anything but a knabbel");
        p.setItemInHand(InteractionHand.MAIN_HAND, knabbels(5));
        helper.assertTrue(mika.jat(p), "it takes a knabbel");
        helper.assertTrue(p.getMainHandItem().getCount() == 4 && mika.draagtKnabbel() && mika.getMainHandItem().is(ModItems.KAAS_KNABBELS.get())
                && mika.getMainHandItem().getCount() == 1 && p.getUUID().equals(mika.slachtoffer()), "exactly ONE, and it holds it");
        helper.assertTrue(!mika.jat(p) && p.getMainHandItem().getCount() == 4, "not a second one");
        helper.assertTrue(mika.requiresCustomPersistence(), "it does not despawn with somebody's knabbel");
        long start = helper.getLevel().getGameTime();
        helper.onEachTick(() -> {
            if (mika.isAlive()) {
                p.lookAt(EntityAnchorArgument.Anchor.EYES, mika.getEyePosition());
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!mika.draagtKnabbel(), "it still holds the knabbel (stand " + mika.stand() + ")");
            long duur = helper.getLevel().getGameTime() - start;
            helper.assertTrue(duur >= EnderMikaEntity.KIJK_TIJD - 2 && duur < EnderMikaEntity.DRAAG_TIJD, "after two seconds of looking, not sooner: " + duur);
            helper.assertTrue(opDeGrond(helper) == 1 && mika.getMainHandItem().isEmpty(), "the knabbel lies on the ground: " + opDeGrond(helper));
            helper.assertTrue(p.getHealth() == p.getMaxHealth(), "nobody got hurt");
            weg(helper, p);
        });
    }

    /** Nobody looks: after twenty seconds it gives up by itself. */
    @GuhTest(template = KAMER, batch = "einde2_mikas_tijd", timeoutTicks = 200)
    public static void einde2MikasLaatHemVallenNaTwintigTellen(GameTestHelper helper) {
        ServerPlayer p = speler(helper, MIDDEN);
        EnderMikaEntity mika = mika(helper, new BlockPos(12, 2, 5));
        p.setItemInHand(InteractionHand.OFF_HAND, knabbels(2));
        helper.assertTrue(mika.jat(p) && p.getOffhandItem().getCount() == 1, "it also finds the knabbel in the other hand");
        mika.zetDraagTijd(EnderMikaEntity.DRAAG_TIJD - 30);
        helper.runAfterDelay(10, () -> helper.assertTrue(mika.draagtKnabbel() && mika.stand() == EnderMikaEntity.DRAAGT && opDeGrond(helper) == 0,
                "not looked at: it still shows off its loot"));
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(!mika.draagtKnabbel() && opDeGrond(helper) == 1, "twenty seconds are over: the knabbel lies on the ground");
            weg(helper, p);
            helper.succeed();
        });
    }

    /** Hit it: it drops the knabbel, shoves (never damage) and flits away. Shoved: it lets go too. */
    @GuhTest(template = KAMER, batch = "einde2_mikas_mep", timeoutTicks = 100)
    public static void einde2MikasLaatLosAlsJeMeptOfDuwt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(12, 2, 15));
        p.setYRot(180f);
        EnderMikaEntity mika = mika(helper, MIDDEN);
        p.setItemInHand(InteractionHand.MAIN_HAND, knabbels(6));
        helper.assertTrue(mika.jat(p), "it takes a knabbel");
        Vec3 was = mika.position();
        float leven = mika.getHealth();
        helper.assertTrue(mika.hurtServer(level, level.damageSources().playerAttack(p), 2f) && mika.getHealth() < leven, "a Mika you can hit");
        helper.assertTrue(!mika.draagtKnabbel() && opDeGrond(helper) == 1, "hit: the knabbel lies on the ground");
        helper.assertTrue(p.getHealth() == p.getMaxHealth() && p.getDeltaMovement().y > 0.2, "it shoves back and never hurts: " + p.getDeltaMovement());
        helper.assertTrue(mika.position().distanceTo(was) > 6 && mika.isWithinHome(), "and it flits away: " + mika.position().distanceTo(was));
        // shoved
        mika.vergeetPauzes();
        p.setDeltaMovement(Vec3.ZERO);
        helper.assertTrue(mika.jat(p) && p.getMainHandItem().getCount() == 4, "it takes another one");
        mika.knockback(0.5, 1, 0);
        helper.assertTrue(!mika.draagtKnabbel() && opDeGrond(helper) == 2, "shoved: it lets go");
        // it dies with one in its hand
        mika.vergeetPauzes();
        helper.assertTrue(mika.jat(p), "and another one");
        mika.hurtServer(level, level.damageSources().playerAttack(p), 1000f);
        helper.assertTrue(mika.isDeadOrDying() && opDeGrond(helper) == 3 && mika.getMainHandItem().isEmpty(), "it died: the knabbel is not lost");
        weg(helper, p);
        helper.succeed();
    }

    /** The victim walks off, or the Ender-Mika goes away: the knabbel goes straight back into the pockets. */
    @GuhTest(template = KAMER, batch = "einde2_mikas_terug", timeoutTicks = 100)
    public static void einde2MikasGeeftTerugVoorHetWeggaat(GameTestHelper helper) {
        ServerPlayer p = speler(helper, MIDDEN);
        EnderMikaEntity mika = mika(helper, new BlockPos(12, 2, 5));
        p.setItemInHand(InteractionHand.MAIN_HAND, knabbels(5));
        helper.assertTrue(mika.jat(p) && inZak(p) == 4, "it takes a knabbel");
        mika.discard();
        helper.assertTrue(inZak(p) == 5 && opDeGrond(helper) == 0, "it goes away: the knabbel is back in the pockets: " + inZak(p));
        EnderMikaEntity twee = mika(helper, new BlockPos(12, 2, 5));
        helper.assertTrue(twee.jat(p) && inZak(p) == 4, "the next one takes a knabbel");
        Vec3 ver = p.position().add(0, 0, EnderMikaEntity.TERUG_AFSTAND + 20);
        p.snapTo(ver.x, ver.y, ver.z);
        helper.succeedWhen(() -> {
            helper.assertTrue(!twee.draagtKnabbel() && inZak(p) == 5 && opDeGrond(helper) == 0,
                    "the victim walked off: the knabbel is back in the pockets: " + inZak(p));
            weg(helper, p);
        });
    }

    /** Look at one that holds nothing: it flits away, shyly. */
    @GuhTest(template = KAMER, batch = "einde2_mikas_verlegen", timeoutTicks = 100)
    public static void einde2MikasFlitstVerlegenWeg(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(12, 2, 18));
        EnderMikaEntity mika = mika(helper, MIDDEN);
        Vec3 was = mika.position();
        boolean[] kijkt = {false};
        Vec3[] voor = new Vec3[1];
        helper.assertTrue(mika.kijker() == null, "the player looks the other way");
        helper.onEachTick(() -> {
            if (kijkt[0] && mika.position().distanceTo(voor[0]) < 4) {
                p.lookAt(EntityAnchorArgument.Anchor.EYES, mika.getEyePosition());      // (it may stroll a little: keep looking)
            }
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(mika.position().distanceTo(was) < 5, "nobody looks: it stays around");
            voor[0] = mika.position();
            p.lookAt(EntityAnchorArgument.Anchor.EYES, mika.getEyePosition());
            helper.assertTrue(mika.bekekenDoor(p), "now the player looks at it");
            kijkt[0] = true;
        });
        helper.runAfterDelay(20 + EnderMikaEntity.VERLEGEN_TIJD + 8, () -> {
            helper.assertTrue(mika.position().distanceTo(voor[0]) > 6, "looked at: it flitted away: " + mika.position().distanceTo(voor[0]));
            helper.assertTrue(mika.isWithinHome(), "inside its honk");
            weg(helper, p);
            helper.succeed();
        });
    }

    /** A trader stands still: no stealing, no flitting, can't be hurt. NBT Handelaar: 1b makes one. */
    @GuhTest(template = KAMER, batch = "einde2_mikas_handelaar", timeoutTicks = 100)
    public static void einde2MikasHandelaarDoetNiets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(12, 2, 16));
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "guhs:ender_mika");
        tag.putBoolean("Handelaar", true);
        tag.putInt("Honk", 4);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(MIDDEN));
        Entity e = EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.STRUCTURE, en -> {
            en.snapTo(at.x, at.y, at.z, 0f, 0f);
            return en;
        });
        helper.assertTrue(e instanceof EnderMikaEntity && level.addFreshEntity(e), "summoned from NBT");
        EnderMikaEntity mika = (EnderMikaEntity) e;
        mika.vergeetPauzes();
        helper.assertTrue(mika.isHandelaar() && mika.isPersistenceRequired(), "Handelaar: 1b");
        p.setItemInHand(InteractionHand.MAIN_HAND, knabbels(4));
        helper.assertTrue(!mika.magJatten(p) && !mika.jat(p) && p.getMainHandItem().getCount() == 4, "a trader never steals");
        helper.assertTrue(!mika.hurtServer(level, level.damageSources().playerAttack(p), 5f) && mika.getHealth() == mika.getMaxHealth(), "can't be hurt");
        helper.assertTrue(!mika.flitsWeg(p) && !mika.flits(at.x + 5, at.y, at.z), "never teleports");
        p.lookAt(EntityAnchorArgument.Anchor.EYES, mika.getEyePosition());
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(mika.position().distanceTo(at) < 0.5 && p.getMainHandItem().getCount() == 4, "looked at for a while: it stands where it stood");
            helper.assertTrue(mika.hasHome() && mika.getHomePosition().equals(helper.absolutePos(MIDDEN)), "its honk is where it was put down");
            mika.setHandelaar(false);
            helper.assertTrue(!mika.isHandelaar() && mika.magJatten(p), "an ordinary one again");
            weg(helper, p);
            helper.succeed();
        });
    }

    /** With a knabbel in your hand and your back turned it comes by itself. A Kaasmijt around: it forgets about knabbels. */
    @GuhTest(template = KAMER, batch = "einde2_mikas_sluip", timeoutTicks = 400)
    public static void einde2MikasSluiptVanAchterenMaarNietMetEenKaasmijt(GameTestHelper helper) {
        ServerPlayer p = speler(helper, MIDDEN);
        p.setItemInHand(InteractionHand.MAIN_HAND, knabbels(3));
        EnderMikaEntity mika = mika(helper, new BlockPos(12, 2, 5));
        KaasmijtEntity mijt = helper.spawn(E2MikasFeature.KAASMIJT.get(), new BlockPos(9, 2, 5));
        mijt.setPersistenceRequired();
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(mika.volgt() == mijt, "it trails after the Kaasmijt");
            helper.assertTrue(!mika.draagtKnabbel() && p.getMainHandItem().getCount() == 3, "and leaves the knabbels alone: " + p.getMainHandItem().getCount());
            helper.assertTrue(mika.distanceTo(mijt) < EnderMikaEntity.MIJT_STRAAL, "near it: " + mika.distanceTo(mijt));
            mijt.discard();
            p.removeEffect(E2MikasFeature.HIK);
            p.snapTo(p.getX(), helper.absolutePos(MIDDEN).getY(), p.getZ(), 0f, 0f);
            Vec3 achter = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(12, 2, 5)));
            mika.snapTo(achter.x, achter.y, achter.z);
            mika.vergeetPauzes();
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getLevel().getGameTime() > 0 && !mijt.isAlive(), "the Kaasmijt is still around");
            helper.assertTrue(p.getMainHandItem().getCount() == 2 && p.getUUID().equals(mika.slachtoffer()),
                    "it came up from behind and took one: " + p.getMainHandItem().getCount());
            weg(helper, p);
        });
    }

    /** On the ground of the outer islands, never many together; its loot is modest; its page exists. */
    @GuhTest(template = KAMER, batch = "einde2_mikas")
    public static void einde2MikasSpawnregelsEnBuit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(GuhDex.isCreaturePage(GuhVariant.ENDER_MIKA) && GuhDex.isCreaturePage(GuhVariant.KAASMIJT)
                && GuhDex.isCreaturePage(GuhVariant.SCHUILGUH), "the three Guhdex pages");
        helper.setBlock(new BlockPos(20, 2, 20), Blocks.STONE);
        BlockPos opKorst = helper.absolutePos(new BlockPos(16, 2, 16)), opSteen = helper.absolutePos(new BlockPos(20, 3, 20));
        if (level.getDifficulty() != Difficulty.PEACEFUL) {
            boolean ooit = false;
            for (int i = 0; i < 400; i++) {
                ooit |= EnderMikaEntity.checkEnderSpawnRules(E2MikasFeature.ENDER_MIKA.get(), level, EntitySpawnReason.NATURAL, opKorst, level.getRandom());
                helper.assertTrue(!EnderMikaEntity.checkEnderSpawnRules(E2MikasFeature.ENDER_MIKA.get(), level, EntitySpawnReason.NATURAL, opSteen,
                        level.getRandom()), "never on ground that is not of the islands");
            }
            helper.assertTrue(ooit, "on kaaskorst it may come");
            EnderMikaEntity een = mika(helper, new BlockPos(6, 2, 16)), twee = mika(helper, new BlockPos(6, 2, 18));
            for (int i = 0; i < 200; i++) {
                helper.assertTrue(!EnderMikaEntity.checkEnderSpawnRules(E2MikasFeature.ENDER_MIKA.get(), level, EntitySpawnReason.NATURAL, opKorst,
                        level.getRandom()), "never a third one near two others");
            }
            een.discard();
            twee.discard();
        }
        EnderMikaEntity mika = mika(helper, MIDDEN);
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("entities/ender_mika")));
        var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, mika)
                .withParameter(LootContextParams.ORIGIN, mika.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic()).create(LootContextParamSets.ENTITY);
        int kruimels = 0;
        for (int i = 0; i < 200; i++) {
            List<ItemStack> drops = table.getRandomItems(params);
            int n = 0;
            for (ItemStack s : drops) {
                helper.assertTrue(s.isEmpty() || s.is(E2MikasFeature.FLITSKRUIMEL.get()), "only Flitskruimels: " + s);
                n += s.getCount();
            }
            helper.assertTrue(n <= 1, "0 or 1 at a time");
            kruimels += n;
        }
        helper.assertTrue(kruimels > 50 && kruimels < 150, "about half the time: " + kruimels);
        mika.discard();
        helper.succeed();
    }

    // =====================================================================================================================
    // the Kaasmijt and the hik
    // =====================================================================================================================

    /** It trips up to you and tickles: the hik, a harmless hop every second and a half. After two minutes it is gone. */
    @GuhTest(template = KAMER, batch = "einde2_mikas", timeoutTicks = 300)
    public static void einde2MikasKaasmijtKieteltEnJeHikt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, MIDDEN);
        KaasmijtEntity mijt = helper.spawn(E2MikasFeature.KAASMIJT.get(), new BlockPos(12, 2, 8));
        // (helper.spawn keeps a mob for good: the one that must go comes the way a fruit brings it)
        KaasmijtEntity oud = Kaasmijten.zet(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 2, 20))));
        helper.assertTrue(oud != null && !oud.isPersistenceRequired(), "a Kaasmijt out of a fruit is not kept");
        KaasmijtEntity blijver = helper.spawn(E2MikasFeature.KAASMIJT.get(), new BlockPos(5, 2, 20));
        oud.zetLeeftijd(KaasmijtEntity.LEVEN - 20);
        blijver.zetLeeftijd(KaasmijtEntity.LEVEN - 20);
        blijver.setPersistenceRequired();
        Pig varken = helper.spawn(EntityType.PIG, new BlockPos(18, 2, 18));
        varken.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.0);     // (it stands where it stands)
        double[] hoogst = {0};
        double grond = helper.absolutePos(new BlockPos(18, 2, 18)).getY();
        helper.runAfterDelay(20, () -> varken.addEffect(new MobEffectInstance(E2MikasFeature.HIK, 200, 0)));
        helper.onEachTick(() -> hoogst[0] = Math.max(hoogst[0], varken.getY() - grond));
        helper.succeedWhen(() -> {
            helper.assertTrue(p.hasEffect(E2MikasFeature.HIK), "the Kaasmijt has not tickled yet");
            helper.assertTrue(mijt.pauze() > 0 && !mijt.kietel(p), "it pauses before it tickles again");
            helper.assertTrue(p.getEffect(E2MikasFeature.HIK).getDuration() <= KaasmijtEntity.HIK_TIJD, "the hik lasts eight seconds");
            helper.assertTrue(p.getHealth() == p.getMaxHealth() && varken.getHealth() == varken.getMaxHealth(), "harmless");
            helper.assertTrue(hoogst[0] > 0.15 && hoogst[0] < 1.0, "the hik is a little hop: " + hoogst[0]);
            helper.assertTrue(!oud.isAlive() && blijver.isAlive(), "gone after two minutes, unless it is kept");
            weg(helper, p);
        });
    }

    /** The hook of the knabbelkoorvrucht: about one fruit in eight holds a Kaasmijt. */
    @GuhTest(template = KAMER, batch = "einde2_mikas")
    public static void einde2MikasKaasmijtUitEenVrucht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(12, 2, 20));
        Vec3 plek = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(12, 2, 4)));
        AABB doos = new AABB(plek, plek).inflate(3);
        int ja = 0;
        for (int i = 0; i < 400; i++) {
            if (Kaasmijten.uitVrucht(level, plek, i == 0 ? null : p)) {
                ja++;
            }
        }
        List<KaasmijtEntity> mijten = level.getEntitiesOfClass(KaasmijtEntity.class, doos);
        helper.assertTrue(ja > 20 && ja < 100, "about one in " + Kaasmijten.KANS + ": " + ja + " of 400");
        helper.assertTrue(mijten.size() == ja, "one Kaasmijt for every true: " + mijten.size() + " / " + ja);
        mijten.forEach(Entity::discard);
        weg(helper, p);
        helper.succeed();
    }

    // =====================================================================================================================
    // the Schuilguh
    // =====================================================================================================================

    /** A kusje: three seconds of levitation I. A knabbel: a doosschil, once a day. Hit: nothing, the lid shuts. */
    @GuhTest(template = KAMER, batch = "einde2_mikas", timeoutTicks = 100)
    public static void einde2MikasSchuilguhKusjeEnDoosschil(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(12, 2, 14));
        SchuilguhEntity guh = helper.spawn(E2MikasFeature.SCHUILGUH.get(), MIDDEN);
        helper.assertTrue(guh.attachFace() == Direction.DOWN && guh.isPersistenceRequired() && !guh.removeWhenFarAway(10000), "it sits and stays");
        // a stick: nothing
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        guh.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(!p.hasEffect(MobEffects.LEVITATION) && p.getMainHandItem().getCount() == 1, "a stick does nothing");
        // an empty hand: a kusje
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        guh.mobInteract(p, InteractionHand.MAIN_HAND);
        MobEffectInstance zweef = p.getEffect(MobEffects.LEVITATION);
        helper.assertTrue(zweef != null && zweef.getAmplifier() == 0 && zweef.getDuration() == SchuilguhEntity.ZWEEF_TIJD,
                "a kusje: 3 seconds of levitation I: " + zweef);
        helper.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "and a soft landing");
        p.removeEffect(MobEffects.LEVITATION);
        helper.assertTrue(!guh.kus(p) && !p.hasEffect(MobEffects.LEVITATION), "not two kusjes in a row");
        // a knabbel: a doosschil, once a day
        p.setItemInHand(InteractionHand.MAIN_HAND, knabbels(3));
        guh.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getMainHandItem().getCount() == 2 && p.getInventory().countItem(E2MikasFeature.DOOSSCHIL.get()) == 1 && guh.schilGegeven(),
                "a knabbel for a doosschil");
        guh.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getMainHandItem().getCount() == 2 && p.getInventory().countItem(E2MikasFeature.DOOSSCHIL.get()) == 1,
                "only one a day: the second knabbel stays in the hand");
        guh.vergeetSchil();
        guh.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getMainHandItem().getCount() == 1 && p.getInventory().countItem(E2MikasFeature.DOOSSCHIL.get()) == 2, "the next day: another one");
        // a guh: nothing happens to it
        guh.vergeetSchil();
        helper.assertTrue(!guh.hurtServer(level, level.damageSources().playerAttack(p), 8f) && guh.getHealth() == guh.getMaxHealth(), "can't be hurt");
        helper.assertTrue(guh.stand() == SchuilguhEntity.DICHT && !guh.kus(p) && !guh.voer(p, p.getMainHandItem()) && p.getMainHandItem().getCount() == 1,
                "after a fright the lid is shut: no kusje, no schil");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(guh.stand() == SchuilguhEntity.DICHT, "still shut");
            weg(helper, p);
            helper.succeed();
        });
    }

    /** On a wall, on a ceiling; when its block goes it takes hold of another one or sinks to the floor. The lid follows the player. */
    @GuhTest(template = KAMER, batch = "einde2_mikas", timeoutTicks = 200)
    public static void einde2MikasSchuilguhHoudtZichVast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // the corner of the room: a wall at z 1 (x 1-4, y 2-5) and a ceiling at y 5 (x 1-4, z 2-4)
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "guhs:schuilguh");
        tag.putByte("AttachFace", (byte) 2);            // (2 = north, as the vanilla shulker)
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 3, 2)));
        Entity e = EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.STRUCTURE, en -> {
            en.snapTo(at.x, at.y, at.z, 90f, 0f);
            return en;
        });
        helper.assertTrue(e instanceof SchuilguhEntity && level.addFreshEntity(e), "summoned from NBT");
        SchuilguhEntity muur = (SchuilguhEntity) e;
        SchuilguhEntity plafond = helper.spawn(E2MikasFeature.SCHUILGUH.get(), new BlockPos(4, 4, 3));
        plafond.setAttachFace(Direction.UP);
        SchuilguhEntity ver = helper.spawn(E2MikasFeature.SCHUILGUH.get(), new BlockPos(20, 2, 20));
        ServerPlayer p = speler(helper, new BlockPos(20, 2, 18));
        helper.assertTrue(muur.attachFace() == Direction.NORTH, "AttachFace 2 = north");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(muur.attachFace() == Direction.NORTH && muur.blockPosition().equals(helper.absolutePos(new BlockPos(2, 3, 2))), "it holds on to the wall");
            helper.assertTrue(plafond.attachFace() == Direction.UP && plafond.blockPosition().equals(helper.absolutePos(new BlockPos(4, 4, 3))), "and to the ceiling");
            helper.assertTrue(muur.getYRot() == 0f && plafond.getYRot() == 0f, "the box decides how it sits");
            helper.assertTrue(muur.stand() == SchuilguhEntity.DICHT && ver.stand() == SchuilguhEntity.OPEN, "far away: shut; a player next to it: open");
            helper.assertTrue(muur.canBeCollidedWith(p), "a box you can stand on");
            p.snapTo(p.getX(), p.getY(), p.getZ() - 4);
            helper.setBlock(new BlockPos(2, 3, 1), Blocks.AIR);         // the wall behind it goes
            helper.setBlock(new BlockPos(4, 5, 3), Blocks.AIR);         // and the bit of ceiling above the other one
        });
        helper.runAfterDelay(50, () -> helper.assertTrue(ver.stand() == SchuilguhEntity.GLUUR, "a player a little further off: it peeks"));
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(muur.attachFace() == Direction.DOWN && muur.blockPosition().equals(helper.absolutePos(new BlockPos(2, 2, 2))),
                    "no wall left: it sank to the floor: " + muur.attachFace() + " " + muur.blockPosition());
            helper.assertTrue(plafond.attachFace() != Direction.UP && plafond.isAlive(), "the ceiling one took hold of something else: " + plafond.attachFace());
            weg(helper, p);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the Schuildoos
    // =====================================================================================================================

    /** Break it and place it again: what was inside is still inside. It does not fit inside itself. */
    @GuhTest(template = KAMER, batch = "einde2_mikas")
    public static void einde2MikasSchuildoosHoudtZijnInhoud(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(12, 2, 14));
        BlockPos plek = new BlockPos(12, 2, 12);
        helper.assertTrue(level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Guhs.id("schuildoos"))).isPresent(), "its recipe");
        helper.setBlock(plek, E2MikasFeature.SCHUILDOOS.get());
        helper.assertTrue(level.getBlockEntity(helper.absolutePos(plek)) instanceof SchuildoosBlockEntity, "a block entity");
        SchuildoosBlockEntity doos = (SchuildoosBlockEntity) level.getBlockEntity(helper.absolutePos(plek));
        helper.assertTrue(doos.getContainerSize() == 27, "27 slots");
        doos.setItem(0, knabbels(16));
        doos.setItem(26, new ItemStack(Items.DIAMOND, 3));
        ItemStack leeg = new ItemStack(E2MikasFeature.SCHUILDOOS_ITEM.get());
        helper.assertTrue(!leeg.canFitInsideContainerItems() && !doos.canPlaceItem(5, leeg) && doos.canPlaceItem(5, knabbels(1)), "no Schuildoos in a Schuildoos");
        SchuildoosMenu menu = new SchuildoosMenu(1, p.getInventory(), doos);
        helper.assertTrue(!menu.getSlot(5).mayPlace(leeg) && menu.getSlot(5).mayPlace(knabbels(1)) && menu.slots.size() == 27 + 36, "the menu refuses it too");
        menu.removed(p);
        // broken in survival: one item, with everything in it
        helper.assertTrue(p.gameMode.destroyBlock(helper.absolutePos(plek)), "it breaks");
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(plek)).inflate(3), Entity::isAlive);
        helper.assertTrue(items.size() == 1 && items.get(0).getItem().is(E2MikasFeature.SCHUILDOOS_ITEM.get()), "one Schuildoos drops and nothing spills: " + items);
        ItemStack vol = items.get(0).getItem().copy();
        items.get(0).discard();
        ItemContainerContents inhoud = vol.get(DataComponents.CONTAINER);
        helper.assertTrue(inhoud != null && inhoud.nonEmptyItemCopyStream().mapToInt(ItemStack::getCount).sum() == 19, "the item carries the contents: " + inhoud);
        // placed again (really placed: with the item, by a player)
        p.setItemInHand(InteractionHand.MAIN_HAND, vol);
        BlockPos vloer = helper.absolutePos(new BlockPos(15, 1, 12));
        vol.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(vloer).add(0, 0.5, 0), Direction.UP, vloer, false)));
        helper.assertTrue(level.getBlockEntity(vloer.above()) instanceof SchuildoosBlockEntity, "placed: " + level.getBlockState(vloer.above()));
        SchuildoosBlockEntity terug = (SchuildoosBlockEntity) level.getBlockEntity(vloer.above());
        helper.assertTrue(terug.getItem(0).is(ModItems.KAAS_KNABBELS.get()) && terug.getItem(0).getCount() == 16 && terug.getItem(26).is(Items.DIAMOND)
                && terug.getItem(26).getCount() == 3 && terug.getItem(1).isEmpty(), "everything is where it was");
        BlockState staat = level.getBlockState(vloer.above());
        helper.assertTrue(!staat.getValue(SchuildoosBlock.OPEN) && staat.getValue(SchuildoosBlock.FACING).getAxis().isHorizontal(), "lid down, facing a side");
        // a creative player breaks a full one: the full box still drops
        p.setGameMode(GameType.CREATIVE);
        helper.assertTrue(p.gameMode.destroyBlock(vloer.above()), "creative breaks it");
        List<ItemEntity> weer = level.getEntitiesOfClass(ItemEntity.class, new AABB(vloer.above()).inflate(3), Entity::isAlive);
        helper.assertTrue(weer.size() == 1 && weer.get(0).getItem().has(DataComponents.CONTAINER)
                && weer.get(0).getItem().get(DataComponents.CONTAINER).nonEmptyItemCopyStream().count() == 2, "creative: the full box drops: " + weer);
        weer.forEach(Entity::discard);
        weg(helper, p);
        helper.succeed();
    }
}
