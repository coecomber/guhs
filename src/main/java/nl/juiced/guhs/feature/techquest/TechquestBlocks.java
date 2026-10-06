package nl.juiced.guhs.feature.techquest;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (tech-quests): the blocks and items of the slice.
 * <ul>
 *   <li>{@link Knabbelmachine}: the kern of De Grote Knabbelmachine (guhs:grote_knabbelmachine): the bowl under its mouth.
 *       The machine itself is drawn on it by the client, for the stage the LOCAL player has built
 *       (client.KnabbelmachineRenderer): every player has their own, and the building never changes.</li>
 *   <li>{@link Deel}: never placed; its block states are the models of the machine's parts (one per part and cell).</li>
 *   <li>{@link Beeldje}: the statuette, the reward of the machine.</li>
 *   <li>{@link Receptkaart}: a recipe card of the Uitvinder-guh: an ingredient that stays in the crafting grid.</li>
 * </ul>
 */
public final class TechquestBlocks {
    private TechquestBlocks() {
    }

    /** The kern (the bowl): click it for the day's perfect knabbel once your own machine is finished. */
    public static class Knabbelmachine extends HorizontalDirectionalBlock implements EntityBlock {
        public static final MapCodec<Knabbelmachine> CODEC = simpleCodec(Knabbelmachine::new);
        private static final VoxelShape VORM = Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(5, 4, 5, 11, 10, 11), Block.box(0, 10, 0, 16, 15, 16));

        public Knabbelmachine(Properties properties) {
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

        /** The mouth looks at whoever puts it down. */
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Kern(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer p) {
                nl.juiced.guhs.feature.techquest.Knabbelmachine.klik(p, pos);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** The kern's block entity: it only exists so the client has something to draw the machine on. */
    public static class Kern extends BlockEntity {
        public Kern(BlockPos pos, BlockState state) {
            super(TechquestFeature.KNABBELMACHINE_BE.get(), pos, state);
        }
    }

    /**
     * The parts of the machine as block states (never placed in a world): NR = part * 36 + cell, the same numbering as
     * tools/features/tech_quests_modellen.py nummer(). The renderer asks the block model of a state and draws it.
     */
    public static class Deel extends Block {
        /** Cells per part: 3 (x) * 4 (y) * 3 (z). */
        public static final int NI = 3, NJ = 4, NK = 3, CELLEN = NI * NJ * NK;
        public static final IntegerProperty NR = IntegerProperty.create("nr", 0, Onderdeel.values().length * CELLEN - 1);

        public Deel(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(NR, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(NR);
        }
    }

    /**
     * The parts of the machine, in the order of tech_quests_modellen.PARTS, with the stages (the step of the questline
     * "knabbelmachine", 0..6) in which a part shows: tech_quests_modellen.FASEN (the generator compares).
     */
    public enum Onderdeel {
        FUNDERING(2, 6), KETEL(3, 6), MAAG(4, 6), SNOET(5, 6), OGEN_DICHT(5, 5), OGEN_OPEN(6, 6), KAAK(5, 6), STEIGER(1, 5), KNABBEL(6, 6);

        public final int van, tot;

        Onderdeel(int van, int tot) {
            this.van = van;
            this.tot = tot;
        }

        public boolean zichtbaar(int fase) {
            return van <= fase && fase <= tot;
        }
    }

    /** The statuette of the machine: it gnaws when you click it. */
    public static class Beeldje extends HorizontalDirectionalBlock {
        public static final MapCodec<Beeldje> CODEC = simpleCodec(Beeldje::new);
        private static final VoxelShape VORM = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(4, 3, 4, 12, 15, 12));

        public Beeldje(Properties properties) {
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
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (placer instanceof ServerPlayer player) {
                GuhAdvancements.grant(player, "tech_quests_beeldje");
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server) {
                long nu = server.getGameTime();
                if (nu - player.getPersistentData().getLongOr("guhs_techquest_beeldje", 0L) > 30) {
                    player.getPersistentData().putLong("guhs_techquest_beeldje", nu);
                    server.playSound(null, pos, ModSounds.GUH_EAT.get(), SoundSource.BLOCKS, 0.6f, 1.5f);
                    player.sendOverlayMessage(Component.translatable("block.guhs.knabbelmachine_beeldje.klik").withStyle(ChatFormatting.YELLOW));
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** A recipe card: the machines on it can only be crafted with the card in the grid, and the card stays (like the Timmerguh's bouwboekje). */
    public static class Receptkaart extends Item {
        public Receptkaart(Properties properties) {
            super(properties);
        }

        @Override
        public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
            return new ItemStackTemplate(instance.typeHolder(), 1, instance instanceof ItemStack stack
                    ? stack.getComponentsPatch() : net.minecraft.core.component.DataComponentPatch.EMPTY);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("item.guhs.techquest_recept.blijft").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
