package nl.juiced.guhs.feature.bleekwoud;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;

/**
 * Game tests of the Bleekwoud (1.2.8): the hearts (awake only at night and only between two bleekhout logs; exactly one
 * creature), the Kraakguh (frozen while looked at, the wooden hug, no damage either way, kaashars when hit, gone with its
 * heart and by day), the soured heart's Kraak-Mika (a Mika, shoves without damage, gives up after enough hits), the wood
 * set and its sign, kaashars recipes, the oogbloempje, the trees, the two structure templates, the diary for everybody,
 * and the biome's share of the Guhmension. Every test has its own batch (they set their own night inside their own box).
 */
public class BleekwoudGameTests {
    private static final String VELD = "bleekwoud_test_veld", KLEIN = "bleekwoud_test_klein";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("guhs");
    /** (In a game test, y = 1 is the template's bottom layer: the moss floor. Stand on y = 2.) The heart of a test pillar (a log under and above it) in the middle of the field. */
    private static final BlockPos HART = new BlockPos(7, 3, 7);

    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(at);
        player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static AABB box(GameTestHelper helper) {
        return helper.getBounds().inflate(2);
    }

    private static void nacht(GameTestHelper helper, Boolean nacht) {
        BleekwoudBlocks.Nacht.zet(box(helper), nacht);
    }

    /** A heart between two logs, standing on the moss. */
    private static void pilaar(GameTestHelper helper, Block heart) {
        helper.setBlock(HART.below(), BleekwoudFeature.BLEEKHOUT_STAM.get());
        helper.setBlock(HART.above(), BleekwoudFeature.BLEEKHOUT_STAM.get());
        helper.setBlock(HART, heart);
        helper.getLevel().scheduleTick(helper.absolutePos(HART), heart, 1);
    }

    private static CreakingHeartState toestand(GameTestHelper helper) {
        return helper.getBlockState(HART).getValue(GuhhartjeBlock.STATE);
    }

    private static <T extends Mob> List<T> wezens(GameTestHelper helper, Class<T> type) {
        return helper.getLevel().getEntitiesOfClass(type, box(helper).inflate(16), e -> !e.isRemoved());
    }

    private static boolean adv(ServerPlayer p, String path) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id(path));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static int telBlok(GameTestHelper helper, Block block) {
        int n = 0;
        AABB b = helper.getBounds();
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(b.minX, b.minY, b.minZ), BlockPos.containing(b.maxX, b.maxY, b.maxZ))) {
            if (helper.getLevel().getBlockState(p).is(block)) {
                n++;
            }
        }
        return n;
    }

    // --- the heart ---------------------------------------------------------------------------------------------------------

    @GuhTest(template = VELD, batch = "bleekwoud_hart", timeoutTicks = 400)
    public static void hartWaaktAlleenSnachtsTussenTweeStammen(GameTestHelper helper) {
        nacht(helper, false);
        pilaar(helper, BleekwoudFeature.KRAKEND_GUHHARTJE.get());
        // a heart without logs, next to it
        BlockPos los = new BlockPos(3, 2, 3);
        helper.setBlock(los, BleekwoudFeature.KRAKEND_GUHHARTJE.get());
        helper.getLevel().scheduleTick(helper.absolutePos(los), BleekwoudFeature.KRAKEND_GUHHARTJE.get(), 1);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(toestand(helper) == CreakingHeartState.DORMANT, "by day, between two logs: asleep, not " + toestand(helper));
            helper.assertTrue(helper.getBlockState(los).getValue(GuhhartjeBlock.STATE) == CreakingHeartState.UPROOTED, "without logs: uprooted");
            nacht(helper, true);
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(toestand(helper) == CreakingHeartState.AWAKE, "at night: awake, not " + toestand(helper));
            helper.assertTrue(helper.getBlockState(los).getValue(GuhhartjeBlock.STATE) == CreakingHeartState.UPROOTED, "without logs it never wakes");
            helper.assertTrue(wezens(helper, KraakguhEntity.class).isEmpty(), "no player near: no creature");
            helper.setBlock(HART.above(), Blocks.AIR);
        });
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(toestand(helper) == CreakingHeartState.UPROOTED, "a log gone: uprooted, not " + toestand(helper));
            // a wrong log does not count; a bleekhout log lying the wrong way neither
            helper.setBlock(HART.above(), Blocks.OAK_LOG);
            helper.getLevel().scheduleTick(helper.absolutePos(HART), BleekwoudFeature.KRAKEND_GUHHARTJE.get(), 1);
        });
        helper.runAfterDelay(170, () -> {
            helper.assertTrue(toestand(helper) == CreakingHeartState.UPROOTED, "an oak log is no bleekhout");
            helper.setBlock(HART.above(), BleekwoudFeature.BLEEKHOUT_GESTRIPT.get());
            helper.getLevel().scheduleTick(helper.absolutePos(HART), BleekwoudFeature.KRAKEND_GUHHARTJE.get(), 1);
        });
        helper.runAfterDelay(190, () -> {
            helper.assertTrue(toestand(helper) == CreakingHeartState.AWAKE, "a stripped bleekhout log counts: awake again, not " + toestand(helper));
            nacht(helper, null);
            helper.succeed();
        });
    }

    @GuhTest(template = VELD, batch = "bleekwoud_roep", timeoutTicks = 600)
    public static void wakkerHartRoeptPreciesEenKraakguh(GameTestHelper helper) {
        nacht(helper, true);
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        pilaar(helper, BleekwoudFeature.KRAKEND_GUHHARTJE.get());
        helper.runAfterDelay(120, () -> {
            List<KraakguhEntity> w = wezens(helper, KraakguhEntity.class);
            helper.assertTrue(w.size() == 1, "one Kraakguh came: " + w.size());
            helper.assertTrue(helper.absolutePos(HART).equals(w.get(0).hart()), "it knows its heart");
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(HART)) instanceof GuhhartjeBlockEntity be && be.isVan(w.get(0)),
                    "and the heart knows it");
            helper.assertTrue(wezens(helper, KraakMikaEntity.class).isEmpty(), "a plain heart calls no Mika");
        });
        helper.runAfterDelay(360, () -> {
            helper.assertTrue(wezens(helper, KraakguhEntity.class).size() == 1, "never a second one: " + wezens(helper, KraakguhEntity.class).size());
            helper.assertTrue(p.getHealth() == p.getMaxHealth(), "nobody got hurt");
            nacht(helper, null);
            leave(helper, p);
            helper.succeed();
        });
    }

    // --- the Kraakguh --------------------------------------------------------------------------------------------------------

    @GuhTest(template = VELD, batch = "bleekwoud_kijk", timeoutTicks = 900)
    public static void kraakguhBeweegtAlleenAlsNiemandKijktEnKnuffelt(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(12, 2, 7));
        KraakguhEntity guh = helper.spawn(BleekwoudFeature.KRAAKGUH.get(), new BlockPos(2, 2, 7));
        guh.setPersistenceRequired();
        p.lookAt(EntityAnchorArgument.Anchor.EYES, guh.getEyePosition());
        Vec3[] start = new Vec3[1];
        helper.runAfterDelay(10, () -> {
            start[0] = guh.position();
            helper.assertTrue(!guh.magBewegen(), "looked at: frozen");
        });
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(!guh.magBewegen() && guh.position().distanceTo(start[0]) < 0.05,
                    "it did not move while the player looked: " + guh.position().distanceTo(start[0]));
            helper.assertTrue(!p.hasEffect(MobEffects.SLOWNESS), "and no hug from afar");
            Vec3 away = p.getEyePosition().add(p.getEyePosition().subtract(guh.getEyePosition()));
            p.lookAt(EntityAnchorArgument.Anchor.EYES, away);       // the player turns around
        });
        helper.runAfterDelay(130, () -> helper.assertTrue(guh.magBewegen() && guh.wakker(), "not looked at: it may move, and it is awake"));
        helper.runAfterDelay(140, () -> helper.succeedWhen(() -> {
            helper.assertTrue(guh.position().distanceTo(start[0]) > 4, "it sneaks up: " + guh.position().distanceTo(start[0]));
            helper.assertTrue(p.hasEffect(MobEffects.SLOWNESS), "and gives its wooden hug (Slowness)");
            helper.assertTrue(p.getHealth() == p.getMaxHealth(), "a hug never hurts: " + p.getHealth());
            helper.assertTrue(guh.knuffelPauze() > 0 && !guh.magBewegen() && !guh.wakker(), "then it rests, frozen");
            helper.assertTrue(adv(p, "guhmension/bleekwoud_knuffel"), "the hug advancement");
            helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.saved(p).getBooleanOr(KraakguhEntity.EERSTE_KNUFFEL, false), "the first-hug line was shown");
            guh.discard();
            leave(helper, p);
        }));
    }

    @GuhTest(template = VELD, batch = "bleekwoud_klap", timeoutTicks = 600)
    public static void klapDoetGeenPijnEnGeeftKaashars(GameTestHelper helper) {
        nacht(helper, true);
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        pilaar(helper, BleekwoudFeature.KRAKEND_GUHHARTJE.get());
        helper.runAfterDelay(120, () -> {
            List<KraakguhEntity> w = wezens(helper, KraakguhEntity.class);
            helper.assertTrue(w.size() == 1, "a Kraakguh came: " + w.size());
            KraakguhEntity guh = w.get(0);
            helper.assertTrue(telBlok(helper, BleekwoudFeature.KAASHARS.get()) == 0, "no kaashars yet");
            float health = guh.getHealth();
            boolean hit = guh.hurtServer(helper.getLevel(), p.damageSources().playerAttack(p), 20f);
            helper.assertTrue(hit && guh.getHealth() == health && guh.isAlive(), "a hit lands but does nothing to it: " + guh.getHealth());
            int hars = telBlok(helper, BleekwoudFeature.KAASHARS.get());
            helper.assertTrue(hars >= 1, "kaashars drips on the trunk: " + hars);
            helper.assertTrue(!guh.hurtServer(helper.getLevel(), helper.getLevel().damageSources().cactus(), 5f) && guh.getHealth() == health,
                    "the world doesn't hurt it either");
            helper.assertTrue(!guh.canBeSeenAsEnemy() && !guh.canBeLeashed(), "nobody's enemy, and no leash");
        });
        helper.runAfterDelay(140, () -> {
            helper.assertTrue(wezens(helper, KraakguhEntity.class).size() == 1 && p.getHealth() == p.getMaxHealth(), "still there, nobody hurt");
            nacht(helper, null);
            leave(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = VELD, batch = "bleekwoud_kapot", timeoutTicks = 600)
    public static void hartKapotWezenWegEnKaashars(GameTestHelper helper) {
        nacht(helper, true);
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        pilaar(helper, BleekwoudFeature.KRAKEND_GUHHARTJE.get());
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(wezens(helper, KraakguhEntity.class).size() == 1, "a Kraakguh came");
            helper.assertTrue(p.gameMode.destroyBlock(helper.absolutePos(HART)), "the heart breaks");
        });
        helper.runAfterDelay(125, () -> {
            helper.assertTrue(wezens(helper, KraakguhEntity.class).isEmpty(), "its creature crumbled away");
            int hars = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box(helper), e -> e.getItem().is(BleekwoudFeature.KAASHARS.get().asItem()))
                    .stream().mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(hars >= 1 && hars <= 3, "it drops 1-3 kaashars: " + hars);
            helper.assertTrue(adv(p, "guhmension/bleekwoud_hartje"), "the advancement");
        });
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(wezens(helper, KraakguhEntity.class).isEmpty(), "and none comes back");
            nacht(helper, null);
            leave(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = VELD, batch = "bleekwoud_dag", timeoutTicks = 600)
    public static void bijDagIsHetWezenWeg(GameTestHelper helper) {
        nacht(helper, true);
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        pilaar(helper, BleekwoudFeature.KRAKEND_GUHHARTJE.get());
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(wezens(helper, KraakguhEntity.class).size() == 1, "a Kraakguh came at night");
            nacht(helper, false);
        });
        helper.runAfterDelay(180, () -> {
            helper.assertTrue(wezens(helper, KraakguhEntity.class).isEmpty(), "dawn: it crumbled");
            helper.assertTrue(toestand(helper) == CreakingHeartState.DORMANT, "and the heart sleeps");
        });
        helper.runAfterDelay(260, () -> {
            helper.assertTrue(wezens(helper, KraakguhEntity.class).isEmpty(), "none by day");
            nacht(helper, null);
            leave(helper, p);
            helper.succeed();
        });
    }

    // --- the soured heart and the Kraak-Mika ---------------------------------------------------------------------------------

    @GuhTest(template = VELD, batch = "bleekwoud_zuur", timeoutTicks = 900)
    public static void zuurHartRoeptKraakMikaDieAlleenDuwt(GameTestHelper helper) {
        nacht(helper, true);
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        pilaar(helper, BleekwoudFeature.VERZUURD_GUHHARTJE.get());
        KraakMikaEntity[] mika = new KraakMikaEntity[1];
        nl.juiced.guhs.entity.GuhEntity guh = helper.spawn(nl.juiced.guhs.registry.ModEntities.GUH.get(), new BlockPos(2, 2, 12));
        guh.setPersistenceRequired();
        guh.setNoAi(true);
        helper.runAfterDelay(120, () -> {
            List<KraakMikaEntity> w = wezens(helper, KraakMikaEntity.class);
            helper.assertTrue(w.size() == 1 && wezens(helper, KraakguhEntity.class).isEmpty(), "a soured heart calls one Kraak-Mika: " + w.size());
            mika[0] = w.get(0);
            helper.assertTrue(mika[0] instanceof MikaEntity && mika[0].is(BleekwoudFeature.MIKAS), "it is a Mika (class and tag guhs:mikas)");
            float health = p.getHealth();
            p.setDeltaMovement(Vec3.ZERO);
            helper.assertTrue(mika[0].doHurtTarget(helper.getLevel(), p), "it shoves");
            helper.assertTrue(p.getHealth() == health && p.getDeltaMovement().lengthSqr() > 0.01, "a shove: no damage, a push " + p.getDeltaMovement());
            helper.assertTrue(adv(p, "guhmension/bleekwoud_geduwd"), "the shoved advancement");
        });
        // it can't be hurt; after enough hits it has had enough for tonight
        for (int i = 0; i < KraakMikaEntity.KLAPPEN_GENOEG; i++) {
            int n = i;
            helper.runAfterDelay(140 + i * 12, () -> {
                helper.assertTrue(mika[0].isAlive() && mika[0].getHealth() == mika[0].getMaxHealth(), "hit " + n + ": still whole");
                // (the player's hits and those of a guh count alike: an aggressive tame guh never fights it forever)
                mika[0].hurtServer(helper.getLevel(), n % 2 == 0 ? p.damageSources().playerAttack(p) : p.damageSources().mobAttack(guh), 30f);
            });
        }
        int na = 140 + KraakMikaEntity.KLAPPEN_GENOEG * 12;
        helper.runAfterDelay(na, () -> {
            helper.assertTrue(mika[0].isRemoved() && wezens(helper, KraakMikaEntity.class).isEmpty(), "enough hits: it crumbled back into its tree");
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(HART)) instanceof GuhhartjeBlockEntity be && be.isKlaarVoorVannacht(),
                    "the heart remembers");
            helper.assertTrue(telBlok(helper, BleekwoudFeature.KAASHARS.get()) >= 1, "kaashars on the trunk");
        });
        helper.runAfterDelay(na + 120, () -> {
            helper.assertTrue(wezens(helper, KraakMikaEntity.class).isEmpty(), "no new one tonight");
            nacht(helper, false);
        });
        helper.runAfterDelay(na + 170, () -> nacht(helper, true));
        helper.runAfterDelay(na + 330, () -> {
            helper.assertTrue(wezens(helper, KraakMikaEntity.class).size() == 1, "the next night: a new one");
            helper.assertTrue(p.getHealth() == p.getMaxHealth() && guh.getHealth() == guh.getMaxHealth(), "nobody got hurt");
            nacht(helper, null);
            leave(helper, p);
            helper.succeed();
        });
    }

    // --- the wood set, the sign, kaashars ---------------------------------------------------------------------------------------

    @GuhTest(template = KLEIN, batch = "bleekwoud_hout")
    public static void bleekhoutStriptEnHetBordHoudtTekst(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
        try {
            ServerLevel level = helper.getLevel();
            for (var entry : List.of(BleekwoudFeature.BLEEKHOUT_STAM, BleekwoudFeature.BLEEKHOUT_GEZICHT)) {
                BlockPos log = new BlockPos(3, 2, 3);
                helper.setBlock(log, entry.get());
                ItemStack axe = new ItemStack(Items.IRON_AXE);
                player.setItemInHand(InteractionHand.MAIN_HAND, axe);
                BlockPos abs = helper.absolutePos(log);
                helper.assertTrue(level.getBlockState(abs).is(BlockTags.LOGS_THAT_BURN) && level.getBlockState(abs).is(BleekwoudFeature.STAMMEN),
                        entry.getId() + " is a bleekhout log");
                axe.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
                helper.assertBlockPresent(BleekwoudFeature.BLEEKHOUT_GESTRIPT.get(), log);
            }
            // every block of the set places
            int i = 0;
            for (var b : List.of(BleekwoudFeature.BLEEKHOUT_PLANKEN, BleekwoudFeature.BLEEKHOUT_TRAP, BleekwoudFeature.BLEEKHOUT_PLAAT,
                    BleekwoudFeature.BLEEKHOUT_HEK, BleekwoudFeature.BLEEKHOUT_POORT, BleekwoudFeature.BLEEKHOUT_LUIK, BleekwoudFeature.BLEEKHOUT_BLADEREN,
                    BleekwoudFeature.BLEEKMOS_TAPIJT, BleekwoudFeature.HARSSTENEN, BleekwoudFeature.HARSSTENEN_MUUR)) {
                BlockPos pos = new BlockPos(1 + i % 5, 2, 4 + i / 5);
                helper.setBlock(pos, b.get());
                helper.assertBlockPresent(b.get(), pos);
                helper.assertTrue(b.get().asItem() != Items.AIR, b.getId() + " has an item");
                i++;
            }
            helper.assertTrue(new ItemStack(BleekwoudFeature.BLEEKHOUT_PLANKEN.get()).is(ItemTags.PLANKS)
                    && new ItemStack(BleekwoudFeature.BLEEKHOUT_STAM.get()).is(ItemTags.LOGS_THAT_BURN), "planks and logs are in the vanilla tags");
            // the sign: a real sign (block entity, wood type), it holds its text
            BlockPos sign = new BlockPos(5, 2, 1);
            helper.setBlock(sign, BleekwoudFeature.BLEEKHOUT_BORD.get());
            BlockState signState = helper.getBlockState(sign);
            helper.assertTrue(BlockEntityType.SIGN.isValid(signState) && BlockEntityType.SIGN.isValid(BleekwoudFeature.BLEEKHOUT_WANDBORD.get().defaultBlockState()),
                    "the sign block entity accepts our signs");
            helper.assertTrue(level.getBlockEntity(helper.absolutePos(sign)) instanceof SignBlockEntity, "a sign block entity");
            SignBlockEntity be = (SignBlockEntity) level.getBlockEntity(helper.absolutePos(sign));
            be.setText(be.getFrontText().setMessage(1, Component.literal("Krak!")), true);
            helper.assertTrue(((SignBlockEntity) level.getBlockEntity(helper.absolutePos(sign))).getFrontText().getMessage(1, false).getString().equals("Krak!"),
                    "the sign holds its text");
            helper.assertTrue(BleekwoudFeature.BLEEKHOUT_BORD.get().asItem() instanceof net.minecraft.world.item.SignItem
                    && BleekwoudFeature.BLEEKHOUT_WANDBORD.get().asItem() == BleekwoudFeature.BLEEKHOUT_BORD.get().asItem(), "one sign item for both");
            helper.assertTrue(signState.is(BlockTags.STANDING_SIGNS) && BleekwoudFeature.BLEEKHOUT_WANDBORD.get().defaultBlockState().is(BlockTags.WALL_SIGNS),
                    "the sign tags");
            var recipes = level.getServer().getRecipeManager();
            for (String r : List.of("bleekhout_planken", "bleekhout_trap", "bleekhout_plaat", "bleekhout_hek", "bleekhout_poort", "bleekhout_deur",
                    "bleekhout_luik", "bleekhout_bord", "bleekhout_gezicht", "bleekmos_tapijt", "krakend_guhhartje", "verzuurd_guhhartje",
                    "kaashars_blok", "kaashars_uit_blok", "harssteen", "harsstenen", "harsstenen_trap", "harsstenen_plaat", "harsstenen_muur",
                    "gebeitelde_harsstenen", "harsstenen_trap_steenzagen", "harsstenen_plaat_steenzagen", "harsstenen_muur_steenzagen",
                    "gebeitelde_harsstenen_steenzagen", "oogbloempje_grijze_kleurstof", "oogbloempje_oranje_kleurstof")) {
                helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(r))).isPresent(), "a recipe for " + r);
            }
            helper.assertTrue(BleekwoudFeature.HARSSTENEN.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                    && BleekwoudFeature.HARSSTENEN_MUUR.get().defaultBlockState().is(BlockTags.WALLS), "the bricks' tags");
            // the Guhdex pages and the super compass
            helper.assertTrue(GuhDex.ENTRIES.contains(GuhVariant.KRAAKGUH) && GuhDex.ENTRIES.contains(GuhVariant.KRAAK_MIKA)
                    && GuhDex.isCreaturePage(GuhVariant.KRAAKGUH) && GuhDex.EXTRA.contains(GuhVariant.KRAAK_MIKA), "Guhdex pages (bonus pages)");
            helper.assertTrue(SuperkompasItem.allowed("bleke_open_plek") && SuperkompasItem.allowed("houthakkershutje"), "the super compass finds both");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    @GuhTest(template = KLEIN, batch = "bleekwoud_hars")
    public static void kaasharsKlontGeeftEenStukPerKant(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos.east(), BleekwoudFeature.BLEEKHOUT_STAM.get());
        helper.setBlock(pos, BleekwoudFeature.KAASHARS.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(Direction.DOWN), true)
                .setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(Direction.EAST), true));
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(pos)), "the clump breaks");
        helper.runAfterDelay(2, () -> {
            int hars = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box(helper), e -> e.getItem().is(BleekwoudFeature.KAASHARS.get().asItem()))
                    .stream().mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(hars == 2, "two faces, two kaashars: " + hars);
            leave(helper, player);
            helper.succeed();
        });
    }

    // --- the oogbloempje ---------------------------------------------------------------------------------------------------------

    @GuhTest(template = KLEIN, batch = "bleekwoud_oog", timeoutTicks = 300)
    public static void oogbloempjeGaatSnachtsOpen(GameTestHelper helper) {
        nacht(helper, true);
        ServerLevel level = helper.getLevel();
        BlockPos a = new BlockPos(2, 2, 2), b = new BlockPos(4, 2, 3), pot = new BlockPos(5, 2, 5);
        helper.setBlock(a, BleekwoudFeature.OOGBLOEMPJE.get());
        helper.setBlock(b, BleekwoudFeature.OOGBLOEMPJE.get());
        helper.setBlock(pot, BleekwoudFeature.POT_OOGBLOEMPJE.get());
        helper.assertBlockPresent(BleekwoudFeature.OOGBLOEMPJE.get(), a);
        helper.assertTrue(helper.getBlockState(a).is(BlockTags.SMALL_FLOWERS) && new ItemStack(BleekwoudFeature.OPEN_OOGBLOEMPJE.get()).is(ItemTags.SMALL_FLOWERS),
                "a small flower (block and item)");
        helper.getBlockState(a).randomTick(level, helper.absolutePos(a), level.getRandom());
        helper.assertBlockPresent(BleekwoudFeature.OPEN_OOGBLOEMPJE.get(), a);
        helper.getBlockState(pot).randomTick(level, helper.absolutePos(pot), level.getRandom());
        helper.assertBlockPresent(BleekwoudFeature.POT_OPEN_OOGBLOEMPJE.get(), pot);
        helper.runAfterDelay(60, () -> {
            helper.assertBlockPresent(BleekwoudFeature.OPEN_OOGBLOEMPJE.get(), b);       // (its neighbour followed)
            nacht(helper, false);
            helper.getBlockState(a).randomTick(level, helper.absolutePos(a), level.getRandom());
            helper.assertBlockPresent(BleekwoudFeature.OOGBLOEMPJE.get(), a);
        });
        helper.runAfterDelay(120, () -> {
            helper.assertBlockPresent(BleekwoudFeature.OOGBLOEMPJE.get(), b);
            nacht(helper, null);
            helper.succeed();
        });
    }

    // --- trees -----------------------------------------------------------------------------------------------------------------

    @GuhTest(template = VELD, batch = "bleekwoud_boom", timeoutTicks = 200)
    public static void zaailingenGroeienEnSommigeBomenHebbenEenHart(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var generator = level.getChunkSource().getGenerator();
        // one sapling: a little tree
        BlockPos een = new BlockPos(2, 2, 2);
        helper.setBlock(een, BleekwoudFeature.BLEEKHOUT_ZAAILING.get());
        boolean grew = false;
        for (int i = 0; i < 20 && !grew; i++) {
            grew = BleekwoudFeature.GROWER.growTree(level, generator, helper.absolutePos(een), helper.getBlockState(een), level.getRandom());
        }
        helper.assertTrue(grew && helper.getBlockState(een).is(BleekwoudFeature.BLEEKHOUT_STAM.get()), "one sapling grows a little tree");
        // four in a square: a big one
        BlockPos vier = new BlockPos(9, 2, 9);
        for (BlockPos q : List.of(vier, vier.east(), vier.south(), vier.south().east())) {
            helper.setBlock(q, BleekwoudFeature.BLEEKHOUT_ZAAILING.get());
        }
        grew = false;
        for (int i = 0; i < 20 && !grew; i++) {
            grew = BleekwoudFeature.GROWER.growTree(level, generator, helper.absolutePos(vier), helper.getBlockState(vier), level.getRandom());
        }
        helper.assertTrue(grew && helper.getBlockState(vier).is(BleekwoudFeature.BLEEKHOUT_STAM.get())
                && helper.getBlockState(vier.south().east()).is(BleekwoudFeature.BLEEKHOUT_STAM.get()), "four saplings grow a thick tree");
        helper.assertTrue(telBlok(helper, BleekwoudFeature.KRAKEND_GUHHARTJE.get()) + telBlok(helper, BleekwoudFeature.VERZUURD_GUHHARTJE.get()) == 0,
                "a tree from saplings has no heart");
        // the forest's heart tree: the heart sits between two logs, asleep, natural
        var feature = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getValue(Guhs.id("bleekhout_boom_hart"));
        helper.assertTrue(feature != null, "the heart tree exists");
        BlockPos plek = helper.absolutePos(new BlockPos(3, 2, 10));
        boolean placed = false;
        for (int i = 0; i < 20 && !placed; i++) {
            placed = feature.place(level, generator, level.getRandom(), plek);
        }
        helper.assertTrue(placed, "the heart tree grows");
        BlockPos hart = null;
        for (BlockPos q : BlockPos.betweenClosed(plek.offset(-3, 0, -3), plek.offset(4, 14, 4))) {
            if (level.getBlockState(q).getBlock() instanceof GuhhartjeBlock) {
                hart = q.immutable();
            }
        }
        helper.assertTrue(hart != null, "with a heart in its trunk");
        BlockState s = level.getBlockState(hart);
        helper.assertTrue(s.getValue(GuhhartjeBlock.NATURAL) && s.getValue(GuhhartjeBlock.STATE) == CreakingHeartState.DORMANT
                && GuhhartjeBlock.hasRequiredLogs(s, level, hart) && level.getBlockEntity(hart) instanceof GuhhartjeBlockEntity,
                "a natural, sleeping heart between two logs, with its block entity");
        helper.succeed();
    }

    // --- the structures ----------------------------------------------------------------------------------------------------------

    @GuhTest(template = "bleke_open_plek", batch = "bleekwoud_plek", timeoutTicks = 200)
    public static void blekeOpenPlekHeeftHartKistEnBloemen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos hart = new BlockPos(17, 15, 18), kist = new BlockPos(22, 5, 13);
        BlockState s = helper.getBlockState(hart);
        helper.assertTrue(s.is(BleekwoudFeature.KRAKEND_GUHHARTJE.get()) && GuhhartjeBlock.hasRequiredLogs(s, level, helper.absolutePos(hart))
                && level.getBlockEntity(helper.absolutePos(hart)) instanceof GuhhartjeBlockEntity, "the heart high in the old tree: " + s);
        helper.assertTrue(level.getBlockEntity(helper.absolutePos(kist)) instanceof RandomizableContainer c && c.getLootTable() != null
                && c.getLootTable().identifier().equals(Guhs.id("chests/bleke_open_plek")), "the half-buried chest with its loot");
        helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/bleke_open_plek")))
                != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "the loot table exists");
        int bloemen = telBlok(helper, BleekwoudFeature.OOGBLOEMPJE.get());
        helper.assertTrue(bloemen >= 30, "oogbloempjes all over: " + bloemen);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(telBlok(helper, BleekwoudFeature.OOGBLOEMPJE.get()) + telBlok(helper, BleekwoudFeature.OPEN_OOGBLOEMPJE.get()) >= 30,
                    "the flowers stay on the moss");
            helper.assertTrue(telBlok(helper, BleekwoudFeature.BLEEK_HANGMOS.get()) >= 5, "hanging moss stays under the leaves");
            helper.succeed();
        });
    }

    @GuhTest(template = "houthakkershutje", batch = "bleekwoud_hut", timeoutTicks = 200)
    public static void houthakkershutjeHeeftKistEnDagboekVoorIedereen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos kist = new BlockPos(9, 6, 4), lessenaar = new BlockPos(9, 6, 6), bord = new BlockPos(8, 7, 9);
        helper.assertTrue(level.getBlockEntity(helper.absolutePos(kist)) instanceof RandomizableContainer c && c.getLootTable() != null
                && c.getLootTable().identifier().equals(Guhs.id("chests/houthakkershutje")), "the chest with its loot");
        helper.assertTrue(level.getBlockEntity(helper.absolutePos(lessenaar)) instanceof LecternBlockEntity l && l.hasBook() && Dagboek.is(l.getBook()),
                "the diary lies on the lectern");
        ItemStack boek = ((LecternBlockEntity) level.getBlockEntity(helper.absolutePos(lessenaar))).getBook();
        var content = boek.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
        helper.assertTrue(content != null && content.pages().size() == Dagboek.BLADZIJDEN, "with all its pages: " + (content == null ? -1 : content.pages().size()));
        helper.assertTrue(level.getBlockEntity(helper.absolutePos(bord)) instanceof SignBlockEntity, "his note by the door is a sign");
        helper.assertBlockPresent(BleekwoudFeature.BLEEKHOUT_DEUR.get(), new BlockPos(7, 6, 8));
        helper.assertBlockPresent(Blocks.LIGHT_GRAY_BED, new BlockPos(5, 6, 4));
        // the diary: a copy for everybody, never a second while you carry one
        ServerPlayer a = player(helper, new BlockPos(7, 6, 6)), b = player(helper, new BlockPos(6, 6, 6));
        helper.assertTrue(Dagboek.geef(a) && Dagboek.geef(b) && !Dagboek.geef(a), "a copy each, no second one");
        helper.assertTrue(Dagboek.heeft(a) && Dagboek.heeft(b), "both carry the diary");
        helper.assertTrue(adv(a, "guhmension/bleekwoud_dagboek") && adv(b, "guhmension/bleekwoud_dagboek"), "and both have the advancement");
        ItemStack copy = Dagboek.stack();
        helper.assertTrue(ItemStack.isSameItemSameComponents(copy, boek), "the lectern's book is the same book as a copy");
        leave(helper, a, b);
        helper.runAfterDelay(20, () -> {
            List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class, helper.getBounds());
            helper.assertTrue(frames.size() == 1 && frames.get(0).getItem().is(Items.IRON_AXE), "his axe is still in the stump: " + frames.size());
            helper.succeed();
        });
    }

    // --- the biome's share ------------------------------------------------------------------------------------------------------

    /**
     * Samples the Guhmension's biomes at the surface (from the dimension JSON and its noise settings; three seeds, a grid
     * of 16000 x 16000 blocks each, every 32 blocks): the Bleekwoud is about 1% of it, and it lies in real patches of
     * forest, not in slivers. The log line gives the numbers (and, with the environment variable GUHS_BLEEKWOUD_ZOEK set,
     * how high the region noise goes: how TERM in tools/features/bleekwoud.py was tuned).
     */
    @GuhTest(template = "empty", batch = "bleekwoud_aandeel", timeoutTicks = 2400)
    public static void bleekwoudAandeel(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = server.registryAccess();
        JsonObject source;
        try (var reader = server.getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            source = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        List<Climate.TargetPoint[][]> grids = new ArrayList<>();
        for (long seed : new long[]{1L, 20281201L, -778899L}) {
            Climate.Sampler sampler = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed).sampler();
            Climate.TargetPoint[][] grid = new Climate.TargetPoint[N][N];
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    grid[i][j] = sampler.sample(QuartPos.fromBlock(-8000 + i * STAP), QuartPos.fromBlock(100), QuartPos.fromBlock(-8000 + j * STAP));
                }
            }
            grids.add(grid);
        }
        double[] nu = meet(BiomeSource.CODEC.parse(ops, source).getOrThrow(), grids);
        StringBuilder andere = new StringBuilder();
        if (System.getenv("GUHS_BLEEKWOUD_ZOEK") != null) {
            // how much of the world the region noise is above a threshold (before the masks against the other regions)
            var key = ResourceKey.create(Registries.NOISE, Guhs.id("bleekwoud"));
            float[] values = new float[3 * N * N];
            int n = 0;
            for (long seed : new long[]{1L, 20281201L, -778899L}) {
                var noise = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed).getOrCreateNoise(key);
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        values[n++] = (float) noise.getValue(-8000 + i * STAP, 0, -8000 + j * STAP);
                    }
                }
            }
            java.util.Arrays.sort(values);
            for (double part : new double[]{0.008, 0.010, 0.012, 0.015, 0.02, 0.03, 0.05}) {
                andere.append(String.format(java.util.Locale.ROOT, "%n   the top %.1f%% of the noise is above %.3f", 100 * part,
                        values[(int) (values.length * (1 - part))]));
            }
        }
        LOGGER.info("Bleekwoud share of the Guhmension surface ({} samples): {} {}", 3 * N * N, tekst(nu), andere);
        helper.assertTrue(nu[0] >= 0.006 && nu[0] <= 0.016, "the Bleekwoud is about 1% of the Guhmension: " + tekst(nu));
        helper.assertTrue(nu[3] >= 0.8, "most of it lies in patches of a hectare or more (real forests, no specks): " + tekst(nu));
        helper.succeed();
    }

    private static final int STAP = 32, N = 500;

    private static String tekst(double[] m) {
        return String.format(java.util.Locale.ROOT, "%.2f%% in %d patches (median %.1f ha, biggest %.1f ha; %.0f%% of it in patches of 1 ha or more, %.0f%% in 4 ha or more)",
                100 * m[0], (int) m[1], m[2], m[4], 100 * m[3], 100 * m[5]);
    }

    /** {share, patches, median patch in hectares, the part of the area in patches of 1 ha or more, biggest patch in ha, the part in 4 ha or more}. */
    private static double[] meet(BiomeSource source, List<Climate.TargetPoint[][]> grids) {
        MultiNoiseBiomeSource multi = (MultiNoiseBiomeSource) source;
        long total = 0, in = 0;
        List<Integer> patches = new ArrayList<>();
        for (Climate.TargetPoint[][] grid : grids) {
            boolean[][] is = new boolean[N][N];
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    is[i][j] = multi.getNoiseBiome(grid[i][j]).is(BleekwoudFeature.BLEEKWOUD);
                    total++;
                    if (is[i][j]) {
                        in++;
                    }
                }
            }
            java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (!is[i][j]) {
                        continue;
                    }
                    int size = 0;
                    is[i][j] = false;
                    queue.add(new int[]{i, j});
                    while (!queue.isEmpty()) {
                        int[] c = queue.poll();
                        size++;
                        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                            int x = c[0] + d[0], z = c[1] + d[1];
                            if (x >= 0 && z >= 0 && x < N && z < N && is[x][z]) {
                                is[x][z] = false;
                                queue.add(new int[]{x, z});
                            }
                        }
                    }
                    patches.add(size);
                }
            }
        }
        patches.sort(null);
        double cell = STAP * STAP / 10000.0;      // hectares per sample
        long big = 0, bigger = 0;
        for (int size : patches) {
            if (size * cell >= 1.0) {
                big += size;
            }
            if (size * cell >= 4.0) {
                bigger += size;
            }
        }
        double median = patches.isEmpty() ? 0 : patches.get(patches.size() / 2) * cell;
        double biggest = patches.isEmpty() ? 0 : patches.get(patches.size() - 1) * cell;
        return new double[]{in / (double) total, patches.size(), median, in == 0 ? 0 : big / (double) in, biggest, in == 0 ? 0 : bigger / (double) in};
    }
}
