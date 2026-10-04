package nl.juiced.guhs.feature.knuffeldal;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.Knusfeest;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** The items of the Knuffeldal (2.8). */
public final class KnuffeldalItems {
    private KnuffeldalItems() {
    }

    /** An item with one line of lore (lang: its key + ".lore"). */
    public static class Lore extends Item {
        public Lore(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The same for a block item. */
    public static class LoreBlock extends BlockItem {
        public LoreBlock(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * A sneeuwguhkopje (a little snow guh head with ears): put it on top of two snow blocks and a sneeuwpopguh stands
     * there (in winter it counts for the seizoensplakboek, see {@link Seizoensactiviteiten#sneeuwpop}).
     */
    public static class Sneeuwguhkopje extends Lore {
        public Sneeuwguhkopje(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            BlockPos top = context.getClickedPos();
            BlockPos bottom = top.below();
            if (context.getClickedFace() != Direction.UP || !level.getBlockState(top).is(Blocks.SNOW_BLOCK) || !level.getBlockState(bottom).is(Blocks.SNOW_BLOCK)
                    || !level.getBlockState(top.above()).canBeReplaced()) {
                if (!level.isClientSide() && context.getPlayer() != null) {
                    context.getPlayer().sendOverlayMessage(Component.translatable("item.guhs.sneeuwguhkopje.hoe").withStyle(ChatFormatting.AQUA));
                }
                return InteractionResult.FAIL;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            Player player = context.getPlayer();
            if (player != null && !player.mayUseItemAt(top, Direction.UP, context.getItemInHand())) {
                return InteractionResult.FAIL;
            }
            Direction facing = player == null ? Direction.NORTH : player.getDirection().getOpposite();
            BlockState lower = KnuffeldalFeature.SNEEUWPOPGUH.get().defaultBlockState()
                    .setValue(KnuffeldalBlocks.Sneeuwpopguh.FACING, facing).setValue(KnuffeldalBlocks.Sneeuwpopguh.HALF, DoubleBlockHalf.LOWER);
            // 1.2.7: not recorded as the player placing blocks (EigenWerk: a protected place put the snow blocks back)
            boolean staat = nl.juiced.guhs.feature.EigenWerk.doe(level, () -> {
                level.setBlock(top, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(bottom, lower, Block.UPDATE_ALL);
                level.setBlock(top, lower.setValue(KnuffeldalBlocks.Sneeuwpopguh.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
                return level.getBlockState(bottom).is(KnuffeldalFeature.SNEEUWPOPGUH.get()) && level.getBlockState(top).is(KnuffeldalFeature.SNEEUWPOPGUH.get());
            });
            if (!staat) {
                return InteractionResult.FAIL;   // (it didn't happen: nothing used up, nothing counted)
            }
            context.getItemInHand().consume(1, player);
            level.playSound(null, top, SoundEvents.SNOW_GOLEM_AMBIENT, SoundSource.BLOCKS, 1f, 1.4f);
            ((ServerLevel) level).sendParticles(KnuffeldalFeature.SNEEUWVLOKJE.get(), top.getX() + 0.5, top.getY() + 0.8, top.getZ() + 0.5,
                    30, 0.5, 0.6, 0.5, 0.02);
            if (player instanceof ServerPlayer sp) {
                Seizoensactiviteiten.sneeuwpop(sp, (ServerLevel) level, bottom);
            }
            return InteractionResult.CONSUME;
        }
    }

    /**
     * The knusfeestlijstje, from Burgemeester Vadsema: right-click it to see which feesttaakjes are still open (and
     * where to do them). Its tooltip shows the same.
     */
    public static class Knusfeestlijstje extends Lore {
        public Knusfeestlijstje(Properties properties) {
            super(properties.stacksTo(1));
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer sp) {
                Burgemeester.toonLijstje(sp);
            }
            return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
        }
    }

    /** (the lijstje for a player: a line per task of the current round, with its step) */
    static List<Component> lijstje(ServerPlayer player) {
        List<Component> lines = new java.util.ArrayList<>();
        for (Feesttaak taak : Knusfeest.lijst(Knusfeest.taken(player))) {
            Knusfeest.Stap stap = Knusfeest.stap(player, taak);
            boolean done = stap == Knusfeest.Stap.GEBRACHT;
            lines.add(Component.literal(done ? "✔ " : "• ").append(taak.naam())
                    .append(Component.literal(" - ").append(Component.translatable("gui.guhs.knusfeest.stap." + (stap == null ? "gevraagd"
                            : stap.name().toLowerCase(java.util.Locale.ROOT)))))
                    .withStyle(done ? ChatFormatting.GREEN : stap == Knusfeest.Stap.GESTOLEN ? ChatFormatting.GOLD : ChatFormatting.WHITE));
        }
        return lines;
    }
}
