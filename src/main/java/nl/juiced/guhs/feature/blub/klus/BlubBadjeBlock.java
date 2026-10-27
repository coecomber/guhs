package nl.juiced.guhs.feature.blub.klus;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGeluid;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;

/**
 * The Blub-bubbelbadje (1.8.0 ronde 3b): a little pink tub (deco, craftable). Every {@value #TIK} ticks it looks around:
 * <ul>
 *   <li>a Blub-guh nearby (free, awake) now and then hops in and splashes about: bubbles, its own sound (the "bellenbad
 *       thuis" from the design) - this works with or without the chore;</li>
 *   <li>with {@link #SCHUIM foam} (a Blub with the chore Bubbelbadmeester got it ready: {@link BadKlus}) a tame guh nearby
 *       now and then walks in and gets {@link FrisEnBlijEffect fris en blij}; the foam is gone after a few minutes.</li>
 * </ul>
 * You can walk through it (only its floor is solid): that is how a guh or a Blub gets in.
 */
public class BlubBadjeBlock extends Block {
    public static final MapCodec<BlubBadjeBlock> CODEC = simpleCodec(BlubBadjeBlock::new);
    public static final BooleanProperty SCHUIM = BooleanProperty.create("schuim");
    /** Ticks between two looks around. */
    public static final int TIK = 100;
    /** How far it looks for bathers. */
    public static final double BEREIK = 6;
    /** Fris en blij lasts 2 minutes. */
    public static final int FRIS_TICKS = 20 * 120;
    private static final VoxelShape VORM = Block.box(1, 0, 1, 15, 8, 15);
    private static final VoxelShape BODEM = Block.box(1, 0, 1, 15, 3, 15);

    public BlubBadjeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SCHUIM, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SCHUIM);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BODEM;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide() && !old.is(this)) {
            level.scheduleTick(pos, this, TIK);
        }
    }

    /** Foam on: a Blub got it ready. */
    public static void schuim(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof BlubBadjeBlock) {
            level.setBlock(pos, s.setValue(SCHUIM, true), Block.UPDATE_ALL);
            bubbels(level, pos, 16);
        }
    }

    static void bubbels(ServerLevel level, BlockPos pos, int n) {
        level.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, n, 0.3, 0.15, 0.3, 0.02);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, n / 4, 0.25, 0.05, 0.25, 0.0);
    }

    /** Is this entity (horizontally) in the tub? */
    public static boolean inBad(Entity e, BlockPos pos) {
        Vec3 m = Vec3.atBottomCenterOf(pos);
        double dx = e.getX() - m.x, dz = e.getZ() - m.z;
        return dx * dx + dz * dz < 0.7 * 0.7 && Math.abs(e.getY() - pos.getY()) < 1.0;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, TIK);
        boolean schuim = state.getValue(SCHUIM);
        // a Blub nearby: in for a splash (with or without the chore)
        for (BlubGuhEntity b : level.getEntitiesOfClass(BlubGuhEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(BEREIK))) {
            if (inBad(b, pos)) {
                bubbels(level, pos, 10);
                BlubGeluid.KLUS_BADDEREN.speel(b);
                b.emotie(BlubGuhEntity.RECHTOP, 60);
            } else if (b.magZelfTruc() && random.nextInt(4) == 0) {
                b.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.1);
            }
        }
        if (!schuim) {
            return;
        }
        if (random.nextInt(30) == 0) {                       // (the foam is gone after a few minutes)
            level.setBlock(pos, state.setValue(SCHUIM, false), Block.UPDATE_ALL);
            return;
        }
        bubbels(level, pos, 4);
        List<GuhEntity> guhs = level.getEntitiesOfClass(GuhEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(BEREIK),
                g -> g.isAlive() && g.isTame() && !g.isOrderedToSit() && !g.hasEffect(BlubFeature.FRIS_EN_BLIJ));
        for (GuhEntity g : guhs) {
            if (inBad(g, pos)) {
                badderen(level, pos, g);
            } else if (g.emotes.current() == null && !nl.juiced.guhs.feature.knus.GuhHooks.isBezig(g) && random.nextInt(3) == 0) {
                g.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
            }
        }
    }

    /** A guh in the foam: fris en blij for a while, bubbles, hearts and a happy dance. */
    public static void badderen(ServerLevel level, BlockPos pos, GuhEntity g) {
        g.addEffect(new MobEffectInstance(BlubFeature.FRIS_EN_BLIJ, FRIS_TICKS, 0, false, true, true));
        bubbels(level, pos, 20);
        level.sendParticles(ParticleTypes.HEART, g.getX(), g.getY() + g.getBbHeight() + 0.2, g.getZ(), 4, 0.3, 0.2, 0.3, 0);
        g.emotes.start(Emote.KNUFFELDANSJE, false, GuhEmotes.Source.SELF);
    }
}
