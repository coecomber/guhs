package nl.juiced.guhs.feature.guheinde;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * A Knabbelpoort (the end gateway of the Guheinde): one appears around the island after every win over Opper-Mika. It
 * throws you far out to the outer islands (where the Mika-vestingen are); there a poort back is made. The block entity
 * remembers where it leads (null = not worked out yet, see {@link GuheindeReis#poortDestination}).
 */
public class KnabbelpoortBlock extends BaseEntityBlock implements Portal {
    public static final MapCodec<KnabbelpoortBlock> CODEC = simpleCodec(KnabbelpoortBlock::new);

    public KnabbelpoortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, net.minecraft.world.entity.Entity entity, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Entity poort ? GuheindeReis.poortDestination(level, entity, pos, poort) : null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(new DustParticleOptions(new Vector3f(1f, 0.5f, 0.8f), 1.5f), pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    /**
     * Where the poort leads; exact = land right there (the poort back), otherwise look for solid ground near it.
     * terug (2.8, NBT "Terug", set in the guheinde_terugpoort template): a Terugpoort on the outer islands, it always
     * leads back to the main island ({@link GuheindeReis#terugpoortDestination}).
     */
    public static class Entity extends BlockEntity {
        @Nullable
        public BlockPos exit;
        public boolean exact;
        public boolean terug;

        public Entity(BlockPos pos, BlockState state) {
            super(GuheindeFeature.KNABBELPOORT_BE.get(), pos, state);
        }

        public void setExit(BlockPos exit, boolean exact) {
            this.exit = exit;
            this.exact = exact;
            setChanged();
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            if (exit != null) {
                tag.put("exit_portal", NbtUtils.writeBlockPos(exit));
            }
            tag.putBoolean("ExactTeleport", exact);
            if (terug) {
                tag.putBoolean("Terug", true);
            }
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            exit = NbtUtils.readBlockPos(tag, "exit_portal").orElse(null);
            exact = tag.getBoolean("ExactTeleport");
            terug = tag.getBoolean("Terug");
        }
    }
}
