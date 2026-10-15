package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.theehuis.TheehuisFeature;

/**
 * The three things of chapter 3 you take home (DESIGN 5 "Wat je overhoudt"):
 * <ul>
 *   <li>{@link Tafel} the playable Pai Sho table (its game: {@link PaiShoTafelBlockEntity});</li>
 *   <li>{@link Theeset} Oom Miroh's theeset: a cup of jasmine tea = a small bonus for a while (once per few minutes per
 *       player), and the team comes for tea when the set stands inside your own guhhuisje's area ({@link H3Theebezoek});</li>
 *   <li>{@link Koolkraam} the koolman's stall: decoration, and a click... you know what he shouts.</li>
 * </ul>
 */
public final class H3Blokken {
    private H3Blokken() {
    }

    /** The shared base: faces where you placed it from, a lower shape. */
    abstract static class Gericht extends HorizontalDirectionalBlock {
        private final VoxelShape vorm;

        Gericht(Properties p, VoxelShape vorm) {
            super(p);
            this.vorm = vorm;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return vorm;
        }
    }

    // =====================================================================================================================
    /** The Pai Sho table: a low round table with the board; right-click to play (see {@link PaiShoTafelBlockEntity}). */
    public static class Tafel extends Gericht implements EntityBlock {
        public static final MapCodec<Tafel> CODEC = simpleCodec(Tafel::new);

        public Tafel(Properties p) {
            super(p, Block.box(0, 0, 0, 16, 12, 16));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new PaiShoTafelBlockEntity(pos, state);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide() || type != E2H3AardeFeature.PAI_SHO_TAFEL_BE.get() ? null
                    : (l, pos, s, be) -> ((PaiShoTafelBlockEntity) be).serverTick();
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer p && level.getBlockEntity(pos) instanceof PaiShoTafelBlockEntity be) {
                be.open(p);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =====================================================================================================================
    /** Oom Miroh's theeset: a tray with a teapot and four little cups. */
    public static class Theeset extends Gericht implements EntityBlock {
        public static final MapCodec<Theeset> CODEC = simpleCodec(Theeset::new);
        /** Ticks between two cups for one player; how long the bonus lasts. */
        public static final int WACHT = 20 * 60 * 3, DUUR = 20 * 90;
        private static final Map<UUID, Long> LAATST = new ConcurrentHashMap<>();

        public Theeset(Properties p) {
            super(p, Block.box(2, 0, 2, 14, 7, 14));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TheesetBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer p) {
                drink(p, (ServerLevel) level, pos);
            }
            return InteractionResult.SUCCESS;
        }

        /** A cup of jasmine tea: a little bonus (a sip of regeneration, a moment of haste, gezellig), then a while of waiting. */
        static void drink(ServerPlayer p, ServerLevel level, BlockPos pos) {
            long nu = level.getGameTime();
            Long laatst = LAATST.get(p.getUUID());
            if (laatst != null && nu - laatst < WACHT) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_h3_aarde.theeset.wacht", (WACHT - (nu - laatst)) / 20 / 60 + 1)
                        .withStyle(ChatFormatting.GOLD));
                return;
            }
            LAATST.put(p.getUUID(), nu);
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 6, 0));
            p.addEffect(new MobEffectInstance(MobEffects.HASTE, DUUR, 0));
            p.addEffect(new MobEffectInstance(TheehuisFeature.GEZELLIG, DUUR, 0));
            level.playSound(null, pos, TheehuisFeature.INSCHENKEN.get(), SoundSource.BLOCKS, 1f, 1f);
            level.sendParticles(TheehuisFeature.THEESTOOM.get(), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.15, 0.1, 0.15, 0.01);
            E2Audio.stinger(p, "deuntje.thee_ting");
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_h3_aarde.theeset.drink." + p.getRandom().nextInt(4)).withStyle(ChatFormatting.GREEN));
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(4) == 0) {
                level.addParticle(TheehuisFeature.THEESTOOM.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2, pos.getY() + 0.55,
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2, 0, 0.02, 0);
            }
        }
    }

    /** The theeset's block entity holds nothing: it tells {@link H3Theebezoek} that a set stands here (and is gone again). */
    public static class TheesetBlockEntity extends BlockEntity {
        /** level -> the loaded sets. */
        static final Map<ResourceKey<Level>, Set<BlockPos>> GELADEN = new ConcurrentHashMap<>();

        public TheesetBlockEntity(BlockPos pos, BlockState state) {
            super(E2H3AardeFeature.THEESET_BE.get(), pos, state);
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel server) {
                GELADEN.computeIfAbsent(server.dimension(), k -> ConcurrentHashMap.newKeySet()).add(worldPosition.immutable());
            }
        }

        @Override
        public void setRemoved() {
            if (level instanceof ServerLevel server) {
                Set<BlockPos> s = GELADEN.get(server.dimension());
                if (s != null) {
                    s.remove(worldPosition);
                }
            }
            super.setRemoved();
        }

        /** The loaded sets in this level. */
        static List<BlockPos> in(ServerLevel level) {
            Set<BlockPos> s = GELADEN.get(level.dimension());
            return s == null ? List.of() : List.copyOf(s);
        }
    }

    // =====================================================================================================================
    /** The koolman's stall (a reward: you saw it break three times). */
    public static class Koolkraam extends Gericht {
        public static final MapCodec<Koolkraam> CODEC = simpleCodec(Koolkraam::new);

        public Koolkraam(Properties p) {
            super(p, Block.box(0, 0, 0, 16, 15, 16));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer p) {
                server.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1f, 0.7f);
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0);
                E2Audio.stinger(server, p.position(), "stinger.kolen");
                p.sendOverlayMessage(Component.translatable("block.guhs.einde2_h3_aarde_koolkraam.klik").withStyle(ChatFormatting.GREEN));
            }
            return InteractionResult.SUCCESS;
        }
    }
}
