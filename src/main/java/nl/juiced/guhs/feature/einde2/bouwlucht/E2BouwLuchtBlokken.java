package nl.juiced.guhs.feature.einde2.bouwlucht;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.einde2.Einde2Haken;

/**
 * einde2, slice bouw_lucht: the block classes of the Zuidelijke Luchttempel that are more than a plain cube: the door of
 * the workshop of the Werktuigguh, the arrow tile and the taart of Guhatso. Resources: tools/features/einde2_bouw_lucht.py.
 */
public final class E2BouwLuchtBlokken {
    private E2BouwLuchtBlokken() {
    }

    /**
     * The closed door of the Werktuigguh: a wall for everybody and everything ("nog niet af, njeg"), except for a player
     * {@link Einde2Haken#WERKPLAATS_OPEN} names: that player walks straight through it. The door never opens for all: it
     * stays in the world as it is, which keeps the workshop shut per player.
     * <p>
     * hook fase C: the rule is asked on the server AND on the client (a player's own movement is worked out on the
     * client), so whoever sets WERKPLAATS_OPEN must answer from state both sides know (sync the flag first).
     */
    public static class Werkplaatsdeur extends Block {
        public Werkplaatsdeur(BlockBehaviour.Properties properties) {
            super(properties);
        }

        /** May this entity walk through the door? Only a player the hook names. */
        public static boolean laatDoor(@Nullable Entity entity) {
            return entity instanceof Player player && Einde2Haken.WERKPLAATS_OPEN.test(player);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (context instanceof EntityCollisionContext ecc && laatDoor(ecc.getEntity())) {
                return Shapes.empty();
            }
            return Shapes.block();
        }
    }

    /** A temple tile with the blue arrow on top: it points the way the player looks who lays it. */
    public static class Pijltegel extends HorizontalDirectionalBlock {
        public static final MapCodec<Pijltegel> CODEC = simpleCodec(Pijltegel::new);

        public Pijltegel(BlockBehaviour.Properties properties) {
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
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }
    }

    /** One of the taarten of Guhatso: a low pie on whatever it stands on (decoration: not for eating, njeg). */
    public static class Taart extends Block {
        private static final VoxelShape VORM = Block.box(1, 0, 1, 15, 5, 15);

        public Taart(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }
    }
}
