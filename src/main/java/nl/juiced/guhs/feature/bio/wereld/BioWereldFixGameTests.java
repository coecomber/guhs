package nl.juiced.guhs.feature.bio.wereld;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/** biomes3 fix-klein: the lake's own bed blocks ({@link MeerBodem}) and the soft landing of the Wolkenweide ({@link WolkLanding}). */
public final class BioWereldFixGameTests {
    private static final String BATCH = "bio_wereld_fix", EMPTY = "empty";

    /** The bed as it was laid before (calcite, wool, concrete): the band a column fell in, 0 = sand .. 4 = the darkest. */
    private static int oudeBand(int diepte, int x, int z) {
        long h = BioModel.mix(x * 0x9E3779B97F4A7C15L ^ BioModel.mix(z * 0xC2B2AE3D27D4EB4FL + 77));
        double v = diepte + (BioModel.kans(h, 0) + BioModel.kans(h, 1) - 1.0);
        return v < 1.7 ? 0 : v < 3.1 ? 1 : v < 4.9 ? 2 : v < 6.3 ? 3 : 4;
    }

    /** The four sediments: dug with a shovel, by hand too, they drop themselves, they do not fall; and the bed's gradient is the old one, block for block. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldFixBodemBlokken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        List<BlockState> trap = MeerBodem.trap();
        helper.assertTrue(trap.size() == 4, "four sediments");
        for (BlockState s : trap) {
            String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
            helper.assertTrue(id.startsWith("bloesemmeertje_meer"), "our own block: " + id);
            helper.assertTrue(s.is(BlockTags.MINEABLE_WITH_SHOVEL) && !s.is(BlockTags.MINEABLE_WITH_PICKAXE), id + " is dug with a shovel");
            helper.assertTrue(!s.requiresCorrectToolForDrops() && s.getDestroySpeed(level, pos) == 0.5f, id + " is as soft as sand, also by hand");
            helper.assertTrue(!(s.getBlock() instanceof FallingBlock) && s.isCollisionShapeFullBlock(level, pos), id + " is a plain full block that stays put");
            List<ItemStack> drops = Block.getDrops(s, level, pos, null);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(s.getBlock().asItem()) && drops.get(0).getCount() == 1, id + " drops itself: " + drops);
            helper.assertTrue(!s.is(BlockTags.WOOL), id + " is no wool");
        }
        // the gradient: the same band in every column as the bed of calcite, wool and concrete had
        List<Block> volgorde = List.of(Blocks.SAND, trap.get(0).getBlock(), trap.get(1).getBlock(), trap.get(2).getBlock(), trap.get(3).getBlock());
        int[] telling = new int[5];
        for (int d = 1; d <= MeerTerrein.DIEP_MAX; d++) {
            for (int i = 0; i < 6000; i++) {
                int x = i * 7 - 9000 + d * 131, z = i * 13 + 500 - d * 977;
                int nu = volgorde.indexOf(MeerVulling.bodem(d, x, z).getBlock());
                helper.assertTrue(nu == oudeBand(d, x, z), "the band at depth " + d + ", " + x + " " + z + ": " + nu + ", was " + oudeBand(d, x, z));
                telling[nu]++;
            }
        }
        for (int i = 0; i < 5; i++) {
            helper.assertTrue(telling[i] > 2000, "every band is laid: " + i + " " + telling[i]);
        }
        helper.succeed();
    }

    /** Their use: any meerslib bakes into meertegels (with slab and stairs), meerzand melts into glass like sand. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldFixMeertegels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var recepten = level.getServer().getRecipeManager();
        for (String id : List.of("bloesemmeertje_meertegels", "bloesemmeertje_meertegels_plaat", "bloesemmeertje_meertegels_trap",
                "bloesemmeertje_meertegels_plaat_steenzagen", "bloesemmeertje_meertegels_trap_steenzagen")) {
            helper.assertTrue(recepten.byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(id))).isPresent(), "a recipe " + id);
        }
        var slib = net.minecraft.tags.TagKey.create(Registries.ITEM, Guhs.id("bloesemmeertje_meerslib"));
        for (DeferredBlock<Block> b : List.of(MeerBodem.SLIB_LICHT, MeerBodem.SLIB, MeerBodem.SLIB_DIEP)) {
            helper.assertTrue(new ItemStack(b.get()).is(slib), b.getId() + " is meerslib (the furnace takes the tag)");
        }
        helper.assertTrue(!new ItemStack(MeerBodem.MEERZAND.get()).is(slib) && new ItemStack(MeerBodem.MEERZAND.get()).is(ItemTags.SMELTS_TO_GLASS),
                "meerzand melts into glass");
        for (DeferredBlock<? extends Block> b : List.of(MeerBodem.TEGELS, MeerBodem.TEGELS_PLAAT, MeerBodem.TEGELS_TRAP)) {
            BlockState s = b.get().defaultBlockState();
            helper.assertTrue(s.is(BlockTags.MINEABLE_WITH_PICKAXE) && s.requiresCorrectToolForDrops(), b.getId() + " is stone: a pickaxe");
            List<ItemStack> drops = Block.getDrops(s, level, pos, null);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(b.get().asItem()), b.getId() + " drops itself");
        }
        helper.assertTrue(MeerBodem.TEGELS_PLAAT.get().defaultBlockState().is(BlockTags.SLABS) && MeerBodem.TEGELS_TRAP.get().defaultBlockState().is(BlockTags.STAIRS),
                "slab and stairs are in their tags");
        helper.assertTrue(Block.getDrops(MeerBodem.TEGELS_PLAAT.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), level, pos, null).get(0).getCount() == 2,
                "a double slab drops two");
        helper.succeed();
    }

    private static float val(LivingEntity wie, double hoogte) {
        return CommonHooks.onLivingFall(wie, hoogte, 1f).getDamageMultiplier();
    }

    /**
     * A fall of 100 blocks that ends in the Wolkenweide hurts nobody (player, guh, wolkenschaapje, a plain pig); the same fall
     * 20 blocks outside it hurts as always; landings that are harmless already are left alone.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldFixZachteLanding(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos midden = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos buiten = midden.offset(26, 0, 0), ver = midden.offset(0, 0, -40);
        Holder<Biome> was = level.getBiome(midden);
        Holder<Biome> weide = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Bio.WOLKENWEIDE);
        BlockPos van = midden.offset(-6, -6, -6), tot = midden.offset(6, 30, 6);
        helper.assertTrue(!WolkLanding.zacht(level, midden) && !WolkLanding.zacht(level, buiten), "no Wolkenweide here yet");
        helper.assertTrue(FillBiomeCommand.fill(level, van, tot, weide).left().isPresent(), "the test's own patch of Wolkenweide");
        try {
            helper.assertTrue(Bio.in(level, midden, Bio.WOLKENWEIDE) && WolkLanding.zacht(level, midden), "the patch is Wolkenweide and soft");
            helper.assertTrue(!WolkLanding.inWeide(level, buiten) && !WolkLanding.zacht(level, buiten), "20 blocks outside the patch is not");
            helper.assertTrue(!WolkLanding.zacht(level, ver), "nor far away");

            ServerPlayer speler = GuhMockPlayer.of(helper);
            speler.setGameMode(GameType.SURVIVAL);
            LivingEntity guh = (LivingEntity) ModEntities.GUH.get().create(level, EntitySpawnReason.COMMAND);
            EntityType<?> schaapType = BuiltInRegistries.ENTITY_TYPE.getOptional(Guhs.id("wolkenschaapje")).orElseThrow();
            LivingEntity schaap = (LivingEntity) schaapType.create(level, EntitySpawnReason.COMMAND);
            LivingEntity varken = EntityType.PIG.create(level, EntitySpawnReason.COMMAND);
            List<LivingEntity> allen = List.of(speler, guh, schaap, varken);
            for (LivingEntity e : allen) {
                if (e != speler) {
                    e.snapTo(midden.getX() + 0.5, midden.getY(), midden.getZ() + 0.5);
                    level.addFreshEntity(e);
                }
            }
            try {
                for (LivingEntity e : allen) {
                    String naam = e.getType().toShortString();
                    // --- in the Wolkenweide: 100 blocks, nothing ---
                    e.snapTo(midden.getX() + 0.5, midden.getY(), midden.getZ() + 0.5);
                    float heel = e.getHealth();
                    helper.assertTrue(val(e, 100) == 0f, naam + ": a fall of 100 that ends in the Wolkenweide has no damage left");
                    helper.assertTrue(val(e, 2.5) == 1f, naam + ": a hop is not touched (it never hurt)");
                    e.causeFallDamage(100, 1f, level.damageSources().fall());
                    helper.assertTrue(e.getHealth() == heel && e.isAlive(), naam + ": unhurt after a real fall of 100 in the Wolkenweide");
                    // --- just outside: as always ---
                    e.snapTo(buiten.getX() + 0.5, buiten.getY(), buiten.getZ() + 0.5);
                    helper.assertTrue(val(e, 100) == 1f, naam + ": the same fall outside the Wolkenweide is not softened");
                }
                // what a fall outside really does to a plain animal (guhs and wolkenschaapjes never take fall damage anywhere)
                varken.snapTo(buiten.getX() + 0.5, buiten.getY(), buiten.getZ() + 0.5);
                varken.causeFallDamage(12, 1f, level.damageSources().fall());
                helper.assertTrue(varken.getHealth() < varken.getMaxHealth(), "a pig that falls 12 blocks outside the Wolkenweide is hurt");
                // slow falling (the giant's sneeze) is not our business: nothing is added or taken
                speler.snapTo(midden.getX() + 0.5, midden.getY(), midden.getZ() + 0.5);
                speler.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false));
                helper.assertTrue(val(speler, 0.0) == 1f && speler.hasEffect(MobEffects.SLOW_FALLING), "a landing without a fall is left alone");
            } finally {
                level.removePlayerImmediately(speler, Entity.RemovalReason.DISCARDED);
                guh.discard();
                schaap.discard();
                varken.discard();
            }
        } finally {
            FillBiomeCommand.fill(level, van, tot, was);
        }
        helper.assertTrue(!WolkLanding.zacht(level, midden), "the patch is put back");
        // the buildings: a spot counts when it is in the box's column, at any height
        BoundingBox doos = new BoundingBox(10, 70, 20, 40, 150, 60);
        helper.assertTrue(WolkLanding.inKolom(doos, new BlockPos(10, 64, 60)) && WolkLanding.inKolom(doos, new BlockPos(40, 200, 20))
                && !WolkLanding.inKolom(doos, new BlockPos(41, 100, 30)) && !WolkLanding.inKolom(doos, new BlockPos(20, 100, 19)), "the column of a building's box");
        helper.assertTrue(WolkLanding.GEBOUWEN.size() == 6 && !WolkLanding.inGebouw(level, midden), "six cloud buildings, none here");
        var structuren = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (var id : WolkLanding.GEBOUWEN) {
            helper.assertTrue(structuren.containsKey(id), "the structure exists: " + id);
        }
        helper.succeed();
    }

    private BioWereldFixGameTests() {
    }
}
