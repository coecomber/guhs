package nl.juiced.guhs.feature.bleekwoud;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/** The special blocks of the Bleekwoud: the sleepy face in the bark, the leaves, the sapling, the moss and the oogbloempje. */
public final class BleekwoudBlocks {
    /** The orange of an open oogbloempje and an awake heart, and the grey of a closed/sleeping one (vanilla's creaking colours). */
    public static final int ORANJE = 0xFC7812, GRIJS = 0x5F5F5F;

    /**
     * Is it night here (the hearts wake up, the oogbloempjes open)? Vanilla's own answer for the Creaking (the environment
     * attribute creaking_active: the Guhmension follows the overworld's day), so it is right in every dimension.
     */
    public static boolean nacht(Level level, BlockPos pos) {
        Boolean test = Nacht.gezet(level, pos);
        return test != null ? test : level.environmentAttributes().getValue(EnvironmentAttributes.CREAKING_ACTIVE, pos);
    }

    /** Game tests set their own day or night inside their own box (the real clock is shared by every test). */
    public static final class Nacht {
        private static final java.util.Map<AABB, Boolean> GEZET = new java.util.concurrent.ConcurrentHashMap<>();

        public static void zet(AABB box, @Nullable Boolean nacht) {
            if (nacht == null) {
                GEZET.remove(box);
            } else {
                GEZET.put(box, nacht);
            }
        }

        @Nullable
        static Boolean gezet(Level level, BlockPos pos) {
            if (GEZET.isEmpty()) {
                return null;
            }
            for (var e : GEZET.entrySet()) {
                if (e.getKey().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                    return e.getValue();
                }
            }
            return null;
        }

        private Nacht() {
        }
    }

    /**
     * A sleepy little guh face in pale bark: its eyes are closed (everything in the Bleekwoud is asleep by day). Right-click:
     * it snores softly. An axe strips it to a plain stripped log.
     */
    public static class Gezicht extends HorizontalDirectionalBlock {
        public static final MapCodec<Gezicht> CODEC = simpleCodec(Gezicht::new);

        public Gezicht(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                level.playSound(null, pos, nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), SoundSource.BLOCKS, 0.4f, 0.6f + level.getRandom().nextFloat() * 0.2f);
            }
            return InteractionResult.SUCCESS;
        }

        @Nullable
        @Override
        public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
            if (ability == ItemAbilities.AXE_STRIP) {
                return BleekwoudFeature.BLEEKHOUT_GESTRIPT.get().defaultBlockState();
            }
            return super.getToolModifiedState(state, context, ability, simulate);
        }
    }

    /** Bleekhout leaves: now and then a pale leaf floats down (vanilla's pale oak leaf). */
    public static class Bladeren extends UntintedParticleLeavesBlock {
        public Bladeren(Properties properties) {
            super(0.02f, ParticleTypes.PALE_OAK_LEAVES, properties);
        }
    }

    /** One sapling grows a little bleekhout tree; four in a square a big one. Happy on moss and wool too. */
    public static class Zaailing extends SaplingBlock {
        public Zaailing(Properties properties) {
            super(BleekwoudFeature.GROWER, properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos) || (state.isFaceSturdy(level, pos, Direction.UP)
                    && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS));
        }
    }

    /**
     * The oogbloempje: a little flower with a guh eye. Closed (and grey) by day, open (and orange) at night; when one opens
     * or closes, its neighbours follow a moment later, like vanilla's eyeblossom. Never harmful: in a suspicious stew the
     * closed one gives Slow Falling, the open one Night Vision.
     */
    public static class Oogbloempje extends FlowerBlock {
        public static final MapCodec<Oogbloempje> CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(Codec.BOOL.fieldOf("open").forGetter(b -> b.open), propertiesCodec()).apply(i, Oogbloempje::new));
        private final boolean open;

        public Oogbloempje(boolean open, Properties properties) {
            super(open ? MobEffects.NIGHT_VISION : MobEffects.SLOW_FALLING, open ? 7f : 5f, properties);
            this.open = open;
        }

        @Override
        public MapCodec<? extends Oogbloempje> codec() {
            return CODEC;
        }

        public boolean isOpen() {
            return open;
        }

        static Block van(boolean open) {
            return open ? BleekwoudFeature.OPEN_OOGBLOEMPJE.get() : BleekwoudFeature.OOGBLOEMPJE.get();
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(BleekwoudFeature.BLEEKMOS.get()) || state.is(Blocks.PINK_WOOL) || super.mayPlaceOn(state, level, pos);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            wissel(state, level, pos, random);
            super.randomTick(state, level, pos, random);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            wissel(state, level, pos, random);
            super.tick(state, level, pos, random);
        }

        /** Opens at night, closes by day; then the same flowers within 3 blocks follow one by one. True when it changed. */
        public boolean wissel(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean moetOpen = nacht(level, pos);
            if (moetOpen == open) {
                return false;
            }
            level.setBlock(pos, van(moetOpen).defaultBlockState(), Block.UPDATE_ALL);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
            effect(level, pos, random, moetOpen);
            BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3)).forEach(nearby -> {
                if (level.getBlockState(nearby) == state) {
                    double distance = Math.sqrt(pos.distSqr(nearby));
                    level.scheduleTick(nearby, state.getBlock(), random.nextIntBetweenInclusive((int) (distance * 5.0), (int) (distance * 10.0)));
                }
            });
            return true;
        }

        /** The little sound and the spark that floats up when an oogbloempje opens (orange) or closes (grey). */
        static void effect(ServerLevel level, BlockPos pos, RandomSource random, boolean open) {
            level.playSound(null, pos, (open ? BleekwoudFeature.OOGJE_OPEN : BleekwoudFeature.OOGJE_DICHT).get(), SoundSource.BLOCKS, 0.8f,
                    0.9f + random.nextFloat() * 0.3f);
            Vec3 start = pos.getCenter();
            double life = 0.5 + random.nextDouble();
            Vec3 target = start.add(new Vec3(random.nextDouble() - 0.5, random.nextDouble() + 1.0, random.nextDouble() - 0.5).scale(life));
            level.sendParticles(new TrailParticleOption(target, open ? ORANJE : GRIJS, (int) (20.0 * life)), start.x, start.y, start.z, 1, 0, 0, 0, 0);
        }
    }

    /** An oogbloempje in a pot: it opens and closes there too. */
    public static class OogbloemPot extends FlowerPotBlock {
        private final Supplier<? extends Block> bloem;

        public OogbloemPot(Supplier<? extends Block> bloem, Properties properties) {
            super(() -> (FlowerPotBlock) Blocks.FLOWER_POT, bloem, properties);
            this.bloem = bloem;
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean open = bloem.get() == BleekwoudFeature.OPEN_OOGBLOEMPJE.get();
            boolean moetOpen = nacht(level, pos);
            if (open != moetOpen) {
                level.setBlock(pos, (moetOpen ? BleekwoudFeature.POT_OPEN_OOGBLOEMPJE : BleekwoudFeature.POT_OOGBLOEMPJE).get().defaultBlockState(),
                        Block.UPDATE_ALL);
                Oogbloempje.effect(level, pos, random, moetOpen);
            }
        }
    }

    private BleekwoudBlocks() {
    }
}
