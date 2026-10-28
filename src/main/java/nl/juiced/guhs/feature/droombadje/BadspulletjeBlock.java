package nl.juiced.guhs.feature.droombadje;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A bath thing on a cloud of the Wolkenbadje: click it (from the bubble, or standing on the cloud) to take it along for Snoes. */
public class BadspulletjeBlock extends Block {
    public enum Soort implements StringRepresentable {
        BADEEND, ZEEPJE, SCHUIM;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String getSerializedName() {
            return id();
        }

        public static Soort van(String id) {
            for (Soort s : values()) {
                if (s.id().equals(id)) {
                    return s;
                }
            }
            return null;
        }
    }

    public static final EnumProperty<Soort> SOORT = EnumProperty.create("soort", Soort.class);
    private static final VoxelShape VORM = Block.box(3, 0, 3, 13, 13, 13);

    public BadspulletjeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SOORT, Soort.BADEEND));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOORT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Badje.pak(sp, state.getValue(SOORT), pos);
        }
        return InteractionResult.SUCCESS;
    }
}
