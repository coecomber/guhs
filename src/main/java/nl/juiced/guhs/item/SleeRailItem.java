package nl.juiced.guhs.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.block.SleeRailBlock;
import nl.juiced.guhs.block.SleeRailPartBlock;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.slee.SleePath;

/**
 * A piece of sled rail. Click an existing piece to add the new one to its nearest end; click anywhere else to start a
 * new track in the direction you're looking. Sneak for the other kind: a left curve, a slope or drop going down, a
 * left corkscrew. Corkscrews go down when you look down while laying them.
 */
public class SleeRailItem extends Item {
    public enum Kind { STRAIGHT, CURVE, SLOPE, DROP, SPIRAL, JUMP }

    private final Kind kind;

    public SleeRailItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        boolean sneak = player != null && player.isSecondaryUseActive();
        SleePath.Shape shape = switch (kind) {
            case STRAIGHT -> SleePath.Shape.STRAIGHT;
            case CURVE -> sneak ? SleePath.Shape.CURVE_LEFT : SleePath.Shape.CURVE_RIGHT;
            case SLOPE -> SleePath.Shape.SLOPE;
            case DROP -> SleePath.Shape.DROP;
            case SPIRAL -> sneak ? SleePath.Shape.SPIRAL_LEFT : SleePath.Shape.SPIRAL_RIGHT;
            case JUMP -> SleePath.Shape.JUMP;
        };
        boolean down = kind == Kind.SPIRAL ? player != null && player.getXRot() > 35 : sneak;
        SleePath.Piece clicked = SleePath.Piece.of(level, context.getClickedPos());
        SleePath.Placement placement = clicked != null
                ? SleePath.attachTo(clicked, context.getClickLocation(), shape, down)
                : SleePath.fresh(new BlockPlaceContext(context).getClickedPos(), context.getHorizontalDirection(), shape);
        if (!canPlace(level, placement, player)) {
            if (player != null && level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.guhs.sleerail.blocked").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            place(level, placement);
            level.playSound(null, placement.anchor(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 1f);
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean canPlace(Level level, SleePath.Placement placement, Player player) {
        for (BlockPos pos : placement.blocks()) {
            BlockState state = level.getBlockState(pos);
            if (!level.isInWorldBounds(pos) || !(state.canBeReplaced() || state.isAir())
                    || (player != null && !level.mayInteract(player, pos))) {
                return false;
            }
        }
        return true;
    }

    public static void place(Level level, SleePath.Placement placement) {
        level.setBlock(placement.anchor(), ModBlocks.SLEE_RAIL.get().defaultBlockState()
                .setValue(SleeRailBlock.FACING, placement.facing()).setValue(SleeRailBlock.SHAPE, placement.shape()), 3);
        SleeRailPartBlock part = ModBlocks.SLEE_RAIL_PART.get();
        for (BlockPos pos : placement.blocks()) {
            if (!pos.equals(placement.anchor())) {
                level.setBlock(pos, part.forOffset(pos.subtract(placement.anchor())), 3);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.sleerail.lore").withStyle(ChatFormatting.GRAY));
        if (kind != Kind.STRAIGHT) {
            tooltip.add(Component.translatable("item.guhs.sleerail." + switch (kind) {
                case CURVE -> "curve";
                case DROP -> "drop";
                case SPIRAL -> "spiral";
                case JUMP -> "jump";
                default -> "slope";
            } + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }
}
