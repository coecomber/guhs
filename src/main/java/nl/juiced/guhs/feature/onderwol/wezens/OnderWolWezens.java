package nl.juiced.guhs.feature.onderwol.wezens;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.zwevers.DasMolguhEntity;
import nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature;
import nl.juiced.guhs.feature.onderwol.OnderWolFeature;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.ModDimensions;

/**
 * 1.7.0 "Onder de Wol": who lives in the new caves of the Guhmension (design rounds 10, 11, 23). Nothing hostile.
 * <ul>
 *   <li>the <b>Grotguh</b> ({@link GuhVariant#GROTGUH}): a wild guh with sparkly deep-pink fur, in every cave, more of them
 *       in the Glitterspelonk. Tamed with kaasknabbels like any guh; a (bonus) Guhdex page.</li>
 *   <li>the <b>Molguh</b>: the das-molguh of the Guheinde ({@link DasMolguhEntity}), in the caves of the viltsteen and
 *       knuffelsteen layers. It digs into that stone like into earth (the block tag {@link #MOLGUH_GROND}) and pops up
 *       again a bit further; where it pops up it sometimes leaves something behind ({@link #achterlaten}).</li>
 *   <li>the <b>glimwormpjes</b> ({@link GrotGlimwormEntity}): only in the Glitterspelonk, hanging from the ceiling in
 *       little groups.</li>
 * </ul>
 * The caves get no mob generation of their own: like the outer islands' biome guhs, they come by a gentle per-player
 * top-up ({@link #aanvullen}) and go again when nobody is near (the guh: a wild guh of the Guhmension; the molguh: the
 * come-and-go tag of WildeDieren; the glimworm: never saved). Chunks made before 1.7.0 have no new caves, so there
 * mostly nothing comes.
 */
public final class OnderWolWezens {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<GrotGlimwormEntity>> GROTGLIMWORM = ENTITY_TYPES.register("grotglimworm",
            () -> EntityType.Builder.of(GrotGlimwormEntity::new, MobCategory.AMBIENT).sized(0.3f, 0.35f).eyeHeight(0.15f)
                    .clientTrackingRange(6).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("grotglimworm"))));

    /** Stone the molguh digs through like earth (the viltsteen and knuffelsteen layers; tools/features/onderwol_wezens.py). */
    public static final TagKey<Block> MOLGUH_GROND = DasMolguhEntity.ONDERWOL_GROND;
    /** Entity tags: a wild cave guh, a cave molguh (their caps; the molguh's leaving-something-behind). */
    public static final String GROT = "guhs_onderwol_grot", MOL = "guhs_onderwol_mol";
    private static final String WAS_ONDER = "guhs_onderwol_was_onder";

    /** The top-up looks every this many ticks per player; 1 in {@link #KANS} looks brings something. */
    public static final int TIJD = 200, KANS = 2;
    /** At most this many within 48 blocks of a player: cave guhs (in the Glitterspelonk: {@link #GUHS_GLITTER}), molguhs, glimworms. */
    public static final int GUHS = 3, GUHS_GLITTER = 5, MOLGUHS = 2, GLIMWORMEN = 10;
    /** The layers (y) of the molguh: viltsteen and knuffelsteen. */
    public static final int MOL_ONDER = 25, MOL_BOVEN = 64;
    /** Chance that a cave molguh leaves something behind when it pops up. */
    public static final float ACHTERLAAT_KANS = 0.35f;

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(GROTGLIMWORM.get(), GrotGlimwormEntity.createAttributes().build()));
        NeoForge.EVENT_BUS.addListener(OnderWolWezens::onLevelTick);
        NeoForge.EVENT_BUS.addListener(OnderWolWezens::onEntityTick);
    }

    // --- the top-up ------------------------------------------------------------------------------------------------------------

    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != ModDimensions.GUHMENSION || level.getGameTime() % TIJD != 137
                || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && level.getRandom().nextInt(KANS) == 0) {
                aanvullen(level, player, level.getRandom());
            }
        }
    }

    /** Maybe one creature (or a little group of glimworms) in a cave near this player. Returns what came ("" for nothing). */
    public static String aanvullen(ServerLevel level, ServerPlayer player, RandomSource random) {
        for (int poging = 0; poging < 4; poging++) {
            BlockPos vloer = grotplek(level, player.blockPosition(), random);
            if (vloer == null) {
                continue;
            }
            boolean glitter = level.getBiome(vloer).is(OnderWolFeature.GLITTERSPELONK);
            if (glitter && random.nextBoolean()) {
                return glimwormen(level, vloer, random) > 0 ? "glimworm" : "";
            }
            boolean molLaag = vloer.getY() >= MOL_ONDER && vloer.getY() <= MOL_BOVEN && level.getBlockState(vloer.below()).is(MOLGUH_GROND);
            if (!glitter && molLaag && random.nextFloat() < 0.4f) {
                return molguh(level, vloer, random) != null ? "molguh" : "";
            }
            return grotguh(level, vloer, glitter, random) != null ? "grotguh" : "";
        }
        return "";
    }

    /**
     * A random cave floor 12-40 blocks from this spot, within 20 blocks up or down: air with a solid floor, out of the sky,
     * at least 6 blocks under the surface. Or null.
     */
    @Nullable
    public static BlockPos grotplek(ServerLevel level, BlockPos bij, RandomSource random) {
        double a = random.nextDouble() * Math.PI * 2, d = 12 + random.nextInt(29);
        int x = (int) Math.floor(bij.getX() + Math.cos(a) * d), z = (int) Math.floor(bij.getZ() + Math.sin(a) * d);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return null;
        }
        int oppervlak = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int y0 = Math.max(6, Math.min(oppervlak - 6, bij.getY() + random.nextInt(41) - 20));
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y0, z);
        for (int dy = 0; dy < 16; dy++) {
            p.setY(y0 - dy);
            if (p.getY() < 6) {
                break;
            }
            if (isGrotVloer(level, p, oppervlak)) {
                return p.immutable();
            }
        }
        return null;
    }

    static boolean isGrotVloer(ServerLevel level, BlockPos p, int oppervlak) {
        return p.getY() <= oppervlak - 6 && level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP) && level.getFluidState(p).isEmpty()
                && !level.canSeeSky(p);
    }

    /** A wild Grotguh on this cave floor (null: too many around, or no room). */
    @Nullable
    public static GuhEntity grotguh(ServerLevel level, BlockPos vloer, boolean glitter, RandomSource random) {
        if (aantal(level, GuhEntity.class, vloer, 48, g -> g.entityTags().contains(GROT) && !g.isTame()) >= (glitter ? GUHS_GLITTER : GUHS)) {
            return null;
        }
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.NATURAL);
        if (guh == null) {
            return null;
        }
        guh.snapTo(vloer.getX() + 0.5, vloer.getY(), vloer.getZ() + 0.5, random.nextFloat() * 360f, 0);
        guh.finalizeSpawn(level, level.getCurrentDifficultyAt(vloer), EntitySpawnReason.NATURAL, null);
        guh.setVariant(GuhVariant.GROTGUH);
        guh.addTag(GROT);
        if (!level.noCollision(guh)) {
            guh.discard();
            return null;
        }
        level.addFreshEntity(guh);
        return guh;
    }

    /** A wild cave molguh on this floor (null: too many around, or no room). */
    @Nullable
    public static DasMolguhEntity molguh(ServerLevel level, BlockPos vloer, RandomSource random) {
        if (aantal(level, DasMolguhEntity.class, vloer, 48, m -> m.entityTags().contains(MOL)) >= MOLGUHS) {
            return null;
        }
        DasMolguhEntity mol = E2ZweversFeature.DAS_MOLGUH.get().create(level, EntitySpawnReason.NATURAL);
        if (mol == null) {
            return null;
        }
        mol.snapTo(vloer.getX() + 0.5, vloer.getY(), vloer.getZ() + 0.5, random.nextFloat() * 360f, 0);
        mol.finalizeSpawn(level, level.getCurrentDifficultyAt(vloer), EntitySpawnReason.NATURAL, null);   // (WildeDieren: comes and goes)
        mol.addTag(MOL);
        if (!level.noCollision(mol)) {
            mol.discard();
            return null;
        }
        level.addFreshEntity(mol);
        return mol;
    }

    /** Two to four glimworms hanging from the ceiling above and around this floor. Returns how many came. */
    public static int glimwormen(ServerLevel level, BlockPos vloer, RandomSource random) {
        if (aantal(level, GrotGlimwormEntity.class, vloer, 48, w -> true) >= GLIMWORMEN) {
            return 0;
        }
        int n = 0;
        for (int i = 0, wil = 2 + random.nextInt(3); i < wil * 3 && n < wil; i++) {
            BlockPos plafond = plafond(level, vloer.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3));
            if (plafond == null) {
                continue;
            }
            GrotGlimwormEntity worm = GROTGLIMWORM.get().create(level, EntitySpawnReason.NATURAL);
            if (worm == null) {
                break;
            }
            // (hanging on its thread: its top touches the ceiling)
            worm.snapTo(plafond.getX() + 0.3 + random.nextDouble() * 0.4, plafond.getY() - worm.getBbHeight() - 0.05,
                    plafond.getZ() + 0.3 + random.nextDouble() * 0.4, random.nextFloat() * 360f, 0);
            if (level.noCollision(worm) && worm.hangt()) {
                level.addFreshEntity(worm);
                n++;
            }
        }
        return n;
    }

    /** The cave ceiling (the first solid block) at most 12 above this spot, when the spot itself is open cave. Or null. */
    @Nullable
    static BlockPos plafond(ServerLevel level, BlockPos van) {
        BlockPos.MutableBlockPos p = van.mutable();
        if (!level.getBlockState(p).isAir() || level.canSeeSky(p)) {
            return null;
        }
        for (int dy = 1; dy <= 12; dy++) {
            p.setY(van.getY() + dy);
            if (!level.getBlockState(p).isAir()) {
                return dy >= 3 && level.getBlockState(p).isFaceSturdy(level, p, Direction.DOWN) ? p.immutable() : null;
            }
        }
        return null;
    }

    private static <T extends Mob> int aantal(ServerLevel level, Class<T> soort, BlockPos bij, int straal, java.util.function.Predicate<T> wie) {
        return level.getEntitiesOfClass(soort, new AABB(bij).inflate(straal), e -> e.isAlive() && wie.test(e)).size();
    }

    // --- the molguh leaves something behind where it pops up ---------------------------------------------------------------------

    static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof DasMolguhEntity mol) || !(mol.level() instanceof ServerLevel level) || !mol.entityTags().contains(MOL)) {
            return;
        }
        boolean onder = mol.isOnder(), was = mol.getPersistentData().getBooleanOr(WAS_ONDER, false);
        if (onder != was) {
            mol.getPersistentData().putBoolean(WAS_ONDER, onder);
            if (was && level.getRandom().nextFloat() < ACHTERLAAT_KANS) {
                achterlaten(level, mol, level.getRandom());
            }
        }
    }

    /** What a cave molguh brings up: mostly a clay ball, sometimes a dye (pigment), very rarely a vahoegiet. */
    public static ItemStack vondst(RandomSource random) {
        float r = random.nextFloat();
        if (r < 0.03f) {
            return new ItemStack(OnderWolFeature.VAHOEGIET.get());
        } else if (r < 0.38f) {
            DyeColor kleur = DyeColor.values()[random.nextInt(DyeColor.values().length)];
            return new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(kleur.getSerializedName() + "_dye")));
        }
        return new ItemStack(Items.CLAY_BALL, 1 + random.nextInt(2));
    }

    static void achterlaten(ServerLevel level, DasMolguhEntity mol, RandomSource random) {
        ItemStack vondst = vondst(random);
        ItemEntity item = new ItemEntity(level, mol.getX(), mol.getY() + 0.3, mol.getZ(), vondst);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        BlockPos onder = mol.getBlockPosBelowThatAffectsMyMovement();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(onder)), mol.getX(), mol.getY() + 0.2, mol.getZ(),
                12, 0.3, 0.1, 0.3, 0.05);
    }

    private OnderWolWezens() {
    }
}
