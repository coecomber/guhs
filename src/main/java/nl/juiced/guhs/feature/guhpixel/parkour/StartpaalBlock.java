package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Het Startpaaltje: the post a Guh-parkour starts at. In your hand you click toys and obstacles one by one to lay out the
 * route ({@link Uitzetten}); placed, it keeps the route, its guhs and their scores ({@link StartpaalBlockEntity}).
 * Right-click: the post screen; sneak + right-click (the owner): lay out more pieces. Broken, the item remembers the route.
 */
public class StartpaalBlock extends BaseEntityBlock {
    public static final MapCodec<StartpaalBlock> CODEC = simpleCodec(StartpaalBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    static final VoxelShape VORM = Shapes.or(Block.box(5, 0, 5, 11, 2, 11), Block.box(6.5, 2, 6.5, 9.5, 16, 9.5));
    /** The key of the route inside the item's custom data. */
    private static final String ROUTE = "guhs_px_parkour";

    public StartpaalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StartpaalBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof StartpaalBlockEntity paal)) {
            return;
        }
        if (placer instanceof Player speler) {
            paal.zetEigenaar(speler);
        }
        List<BlockPos> route = route(stack);
        if (!route.isEmpty()) {
            int teVer = paal.neemOver(route);
            if (placer instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable(teVer > 0 ? "gui.guhs.guhparkour.uitzet.geplaatst_te_ver" : "gui.guhs.guhparkour.uitzet.geplaatst",
                        paal.stukken().size(), teVer).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else if (placer instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.guhparkour.uitzet.leeg_geplaatst").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        paal.stuurBorden();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof StartpaalBlockEntity paal) {
            if (Uitzetten.bezig(sp, pos)) {
                Uitzetten.stop(sp, true);
            } else if (sp.isShiftKeyDown()) {
                Uitzetten.start(sp, paal);
            } else {
                ParkourPayloads.open(sp, paal);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The item that drops remembers the route. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof StartpaalBlockEntity paal && !paal.stukken().isEmpty()) {
            for (ItemStack s : drops) {
                if (s.is(ParkourSlice.STARTPAALTJE_ITEM.get())) {
                    zetRoute(s, paal.stukken());
                }
            }
        }
        return drops;
    }

    // --- the route in the item -------------------------------------------------------------------------------------------------

    public static List<BlockPos> route(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return new ArrayList<>();
        }
        return StartpaalBlockEntity.uitRouteTag(data.copyTag().getCompoundOrEmpty(ROUTE));
    }

    public static void zetRoute(ItemStack stack, List<BlockPos> stukken) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        if (stukken.isEmpty()) {
            tag.remove(ROUTE);
        } else {
            tag.put(ROUTE, StartpaalBlockEntity.routeTag(stukken));
        }
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /** The Startpaaltje in your hand: its tooltip says how to lay out a route and how many pieces it remembers. */
    public static class Artikel extends BlockItem {
        public Artikel(Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            String key = getBlock().getDescriptionId();
            tooltip.accept(Component.translatable(key + ".lore").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.accept(Component.translatable(key + ".uitleg").withStyle(ChatFormatting.GRAY));
            int n = route(stack).size();
            if (n > 0) {
                tooltip.accept(Component.translatable("gui.guhs.guhparkour.item.route", n).withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
