package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.huisje.Speeltje;
import nl.juiced.guhs.feature.speelgoed.SpeelTaak;

/**
 * einde2 phase B, slice h1_lucht: the luchtscooter, a toy for guhs (DESIGN 5 "Wat je overhoudt": luchtscooter-speelgoed
 * voor je guhs; the reward of chapter 1). A spinning ball of air on a little wooden base: a guh near it trots there and
 * rides round and round it on the wind ({@link Rit}), hopping now and then; residents of a guhhuisje pick it like any toy.
 * Right-click: a gust. Model: models/block/luchtscooter.json.
 */
public class LuchtscooterBlock extends Block {
    public static final MapCodec<LuchtscooterBlock> CODEC = simpleCodec(LuchtscooterBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 2, 13), Block.box(4, 2, 4, 12, 13, 12));

    public LuchtscooterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            zoef(server, pos, 16);
        }
        return InteractionResult.SUCCESS;
    }

    /** A gust out of the ball of air. */
    public static void zoef(ServerLevel level, BlockPos pos, int aantal) {
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, aantal, 0.5, 0.2, 0.5, 0.04);
        level.playSound(null, pos, SoundEvents.BREEZE_IDLE_GROUND, SoundSource.BLOCKS, 0.5f, 1.4f + level.getRandom().nextFloat() * 0.3f);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            double hoek = random.nextDouble() * Math.PI * 2;
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 + Math.cos(hoek) * 0.4, pos.getY() + 0.55, pos.getZ() + 0.5 + Math.sin(hoek) * 0.4,
                    -Math.sin(hoek) * 0.05, 0.01, Math.cos(hoek) * 0.05);
        }
    }

    // =====================================================================================================================
    // the toy kind: a guh rides round the scooter
    // =====================================================================================================================
    /** The id of the toy kind (registry of feature/huisje/Speelgoed). */
    public static final String ID = "einde2_h1_lucht_luchtscooter";

    /** One ride of one guh. */
    public static class Rit extends SpeelTaak {
        public static final int DUUR = 220;
        private final BlockPos scooter;
        private boolean daar;
        private int rit;

        public Rit(Mob mob, ServerLevel level, BlockPos scooter) {
            super(mob, level);
            this.scooter = scooter.immutable();
        }

        @Override
        public int maxTicks() {
            return DUUR + 300;
        }

        @Override
        protected boolean speel() {
            if (!level.getBlockState(scooter).is(E2H1LuchtFeature.LUCHTSCOOTER.get())) {
                return false;
            }
            if (!daar) {
                if (vast()) {
                    return false;
                }
                daar = loopNaar(Vec3.atBottomCenterOf(scooter), 1.15, 2.0);
                return true;
            }
            if (++rit > DUUR) {
                level.sendParticles(BandFeature.HARTJE.get(), mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 3, 0.3, 0.15, 0.3, 0);
                return false;
            }
            // round and round: a point on a circle ahead of the guh, fast
            double hoek = rit * 0.12;
            Vec3 m = Vec3.atBottomCenterOf(scooter);
            Vec3 doel = m.add(Math.cos(hoek) * 1.8, 0, Math.sin(hoek) * 1.8);
            mob.getNavigation().moveTo(doel.x, doel.y, doel.z, 1.6);
            mob.getLookControl().setLookAt(m.x, m.y + 0.5, m.z);
            if (rit % 4 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.1, mob.getZ(), 1, 0.1, 0.02, 0.1, 0.0);
            }
            if (mob.onGround() && rit % 50 == 25) {
                mob.getJumpControl().jump();
                zoef(level, scooter, 6);
            }
            return true;
        }
    }

    public static final Speeltje SOORT = new Speeltje() {
        @Override
        public String id() {
            return ID;
        }

        @Nullable
        @Override
        public KlusTaak zoek(ServerLevel level, Mob speler, BlockPos rond, int bereik) {
            if (!(speler instanceof GuhEntity)) {
                return null;
            }
            return scooters(level, rond, bereik).min(Comparator.comparingDouble(p -> p.distSqr(speler.blockPosition())))
                    .map(p -> (KlusTaak) new Rit(speler, level, p)).orElse(null);
        }

        @Override
        public List<Telling> tel(ServerLevel level, BlockPos rond, int bereik, Predicate<BlockPos> binnen) {
            return List.of(new Telling("luchtscooter", new ItemStack(E2H1LuchtFeature.LUCHTSCOOTER.get()), (int) scooters(level, rond, bereik).filter(binnen).count()));
        }
    };

    /** The scooters within bereik of rond (through their point of interest). */
    public static java.util.stream.Stream<BlockPos> scooters(ServerLevel level, BlockPos rond, int bereik) {
        return level.getPoiManager().getInRange(h -> h.is(E2H1LuchtFeature.LUCHTSCOOTER_POI.getKey()), rond, bereik, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos);
    }
}
