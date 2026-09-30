package nl.juiced.guhs.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.registry.ModBlockEntities;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The royal guh throne (Koninklijke guhtroon): a seat with a guh head for a backrest. The one in the guh castle is
 * "royal": when its Koningguh has been gone (tamed and taken home) for a few days, a new Koningguh takes the throne.
 * Thrones you take home and place yourself are just very nice chairs.
 */
public class KoningsTroonBlock extends GuhFurnitureBlock implements EntityBlock {
    public static final BooleanProperty ROYAL = BooleanProperty.create("royal");
    /** How long the throne stays empty before a new king comes (3 Minecraft days). */
    public static final long NEW_KING_AFTER = 3 * 24000L;

    public KoningsTroonBlock(Properties properties) {
        super(properties, 0.55, new double[]{1, 0, 1, 15, 9, 15}, new double[]{1, 9, 12, 15, 30, 16});
        registerDefaultState(defaultBlockState().setValue(ROYAL, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(KoningsTroonBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ROYAL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(ROYAL) ? new Entity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !state.getValue(ROYAL) || type != ModBlockEntities.KONINGSTROON.get() ? null
                : (lvl, pos, st, be) -> ((Entity) be).tick((ServerLevel) lvl, pos, st);
    }

    /** Remembers when the throne last had a Koningguh on (or next to) it. */
    public static class Entity extends BlockEntity {
        private long lastKing = Long.MIN_VALUE; // (not seen yet)

        public Entity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.KONINGSTROON.get(), pos, state);
        }

        public void tick(ServerLevel level, BlockPos pos, BlockState state) {
            if (level.getGameTime() % 200 == 0) {
                check(level, pos, state);
            }
        }

        /** Is there a king? If not for 3 days, a new one comes. */
        public void check(ServerLevel level, BlockPos pos, BlockState state) {
            boolean king = !level.getEntitiesOfClass(GuhEntity.class, new AABB(pos).inflate(4),
                    g -> g.getVariant() == GuhVariant.KONING).isEmpty();
            if (king || lastKing == Long.MIN_VALUE) {
                lastKing = level.getGameTime();
                setChanged();
            } else if (level.getGameTime() - lastKing >= NEW_KING_AFTER) {
                crown(level, pos, state);
                lastKing = level.getGameTime();
                setChanged();
            }
        }

        /** For tests: pretend the throne has been empty since `ticksAgo`. */
        public void setLastKing(long gameTime) {
            this.lastKing = gameTime;
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            lastKing = tag.contains("LastKing") ? tag.getLong("LastKing") : Long.MIN_VALUE;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putLong("LastKing", lastKing);
        }
    }

    /** A new Koningguh sits down on the throne, in his royal outfit. */
    public static void crown(ServerLevel level, BlockPos pos, BlockState state) {
        GuhEntity king = ModEntities.GUH.get().create(level);
        if (king == null) {
            return;
        }
        float yaw = state.getValue(FACING).toYRot();
        king.moveTo(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, yaw, 0);
        king.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        makeKing(king);
        king.setYHeadRot(yaw);
        king.setYBodyRot(yaw);
        level.addFreshEntity(king);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                40, 0.8, 1, 0.8, 0.3);
    }

    /** Turns a guh into the Koningguh: the variant, his size, his outfit, sitting on his throne. */
    public static void makeKing(GuhEntity king) {
        king.setVariant(GuhVariant.KONING);
        king.setGuhScale(GuhEntity.KONING_SCALE);
        for (GuhClothes.Slot slot : GuhClothes.Slot.kleding()) {
            king.takeOff(slot);
        }
        king.wear(GuhClothes.KONING_KROON);
        king.wear(GuhClothes.KONING_MANTEL);
        king.wear(GuhClothes.KONING_KETTING);
        king.setPersonality(GuhPersonality.BRAVE);
        king.setPersistenceRequired();
        king.setOrderedToSit(true);
        king.setInSittingPose(true);
    }
}
