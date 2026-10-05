package nl.juiced.guhs.feature.barbecuether;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.ScheduledTickAccess;
/** The barbecue portal: flickering flames inside a grillkool frame. Guhmensie <-> Barbecuether, 1:8 like the Nether. */
public class GrillPortalBlock extends Block implements Portal {
    public static final MapCodec<GrillPortalBlock> CODEC = simpleCodec(GrillPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final VoxelShape X_AXIS_AABB = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_AXIS_AABB = Block.box(6, 0, 0, 10, 16, 16);
    private static final DustParticleOptions EMBER = new DustParticleOptions(0xFF801A /* 1.0, 0.5, 0.1 */, 1.0f);
    private static final DustParticleOptions CHEESE = new DustParticleOptions(0xFFD140 /* 1.0, 0.82, 0.25 */, 0.9f);

    public GrillPortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AXIS_AABB : X_AXIS_AABB;
    }

    /** Goes out (chain reaction) as soon as the frame isn't whole any more. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {
        Direction.Axis portalAxis = state.getValue(AXIS);
        boolean sideways = facing.getAxis() != portalAxis && facing.getAxis().isHorizontal();
        if (!sideways && !facingState.is(this) && level instanceof LevelAccessor la && !new GrillPortalShape(la, pos, portalAxis).isComplete()) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, facing, facingPos, facingState, random);
    }

    /**
     * bbq2 (CONTRACT_130 §6.2.10): the portal locks. Each is asked when something wants to go from the Guhmensie to the
     * Barbecuether: a non-null Component = refused, with that message (shown once per attempt). The way back is never
     * asked. Add yours from your Feature.register:
     * {@code GrillPortalBlock.SLOTEN.add((level, entity) -> entity instanceof ServerPlayer p && !klaar(p) ? Component.translatable("...") : null);}
     */
    public static final java.util.List<java.util.function.BiFunction<ServerLevel, Entity, Component>> SLOTEN = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** How long (ticks) a refused player isn't told again: one message per attempt, not one per tick in the flames. */
    public static final int SLOT_BERICHT_TICKS = 100;
    private static final java.util.Map<java.util.UUID, Long> GEWEIGERD_OP = new java.util.concurrent.ConcurrentHashMap<>();

    /** Why this entity may not go to the Barbecuether from here (null: it may; always null on the way back). */
    @Nullable
    public static Component slot(ServerLevel level, Entity entity) {
        return slot(level.dimension(), level, entity);
    }

    /** The same with the dimension the portal stands in passed in (the game tests have no Guhmensie). */
    @Nullable
    public static Component slot(net.minecraft.resources.ResourceKey<Level> dim, ServerLevel level, Entity entity) {
        if (dim != nl.juiced.guhs.world.ModDimensions.GUHMENSION) {
            return null;
        }
        for (java.util.function.BiFunction<ServerLevel, Entity, Component> slot : SLOTEN) {
            Component nee = slot.apply(level, entity);
            if (nee != null) {
                return nee;
            }
        }
        return null;
    }

    /** Refused (with the message, once per attempt)? */
    public static boolean geweigerd(ServerLevel level, Entity entity) {
        Component nee = slot(level, entity);
        if (nee == null) {
            return false;
        }
        if (entity instanceof net.minecraft.server.level.ServerPlayer p) {
            long nu = level.getServer().getTickCount();
            Long vorige = GEWEIGERD_OP.get(p.getUUID());
            if (vorige == null || nu - vorige >= SLOT_BERICHT_TICKS || nu < vorige) {
                p.sendSystemMessage(nee.copy().withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            }
            GEWEIGERD_OP.put(p.getUUID(), nu);   // (still standing in the flames: the same attempt)
        }
        return true;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level instanceof ServerLevel server && !SLOTEN.isEmpty() && entity.canUsePortal(false) && geweigerd(server, entity)) {
            return;   // (bbq2: a portal lock says no)
        }
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    /** Like a nether portal: 4 seconds for a player in survival, at once in creative. */
    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return entity instanceof Player player
                ? Math.max(1, level.getGameRules().get(player.getAbilities().invulnerable
                ? GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY))
                : 0;
    }

    @Nullable
    @Override
    public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        if (!SLOTEN.isEmpty() && slot(level, entity) != null) {
            return null;   // (bbq2: locked; entityInside already said why)
        }
        return GrillPortalForcer.getDestination(level, entity, pos);
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
                    0.4f, random.nextFloat() * 0.3f + 0.6f, false);
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    0.5f, random.nextFloat() * 0.4f + 0.8f, false);
        }
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(random.nextInt(3) == 0 ? CHEESE : EMBER, x, y, z, 0, 0.03, 0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.04, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }
}
