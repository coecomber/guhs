package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice vetbootje: the three items. The vetfluitje calls your vetbootje, the werfboekje opens the scheepswerf
 * screen (both come with the boat, {@link Vetbootje#geef}), and the folded heteluchtballon unfolds where you put it.
 * Textures and texts: tools/features/einde2_vetbootje.py.
 */
public final class VetbootjeItems {
    /** Ticks between two whistles. */
    public static final int FLUIT_WACHT = 60;

    /** Whistle and your vetbootje glides over, however far it drifted (a lost one is made again, chest and all). */
    public static class Vetfluitje extends Item {
        public Vetfluitje(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(player instanceof ServerPlayer sp)) {
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = player.getItemInHand(hand);
            level.playSound(null, player.blockPosition(), E2VetbootjeFeature.FLUITJE.get(), SoundSource.PLAYERS, 1f, 0.95f + level.getRandom().nextFloat() * 0.1f);
            player.getCooldowns().addCooldown(stack, FLUIT_WACHT);
            if (!Vetbootje.heeft(sp)) {
                // hook fase B: this line points at the questline (a vetschip of the Mika-vesting); nothing gives the boat before that
                sp.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.fluit.geen").withStyle(ChatFormatting.LIGHT_PURPLE));
                return InteractionResult.SUCCESS;
            }
            if (sp.getVehicle() instanceof VetbootjeEntity eigen && eigen.isVan(sp)) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.fluit.aan_boord").withStyle(ChatFormatting.LIGHT_PURPLE));
                return InteractionResult.SUCCESS;
            }
            VetbootjeEntity boot = Vetbootje.roep(sp);
            sp.sendOverlayMessage(boot == null
                    ? Component.translatable("gui.guhs.einde2_vetbootje.fluit.geen_plek").withStyle(ChatFormatting.LIGHT_PURPLE)
                    : Component.translatable("gui.guhs.einde2_vetbootje.fluit.komt", boot.naam()).withStyle(ChatFormatting.GOLD));
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.vetbootje_fluitje.lore").withStyle(ChatFormatting.GOLD));
            tooltip.accept(Component.translatable("item.guhs.vetbootje_fluitje.lore2").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The scheepswerf in your pocket: colour, sail and name of your vetbootje. */
    public static class Werfboekje extends Item {
        public Werfboekje(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(player instanceof ServerPlayer sp)) {
                return InteractionResult.SUCCESS;
            }
            if (!Vetbootje.heeft(sp)) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.fluit.geen").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                Vetbootje.openWerf(sp);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.vetbootje_werfboekje.lore").withStyle(ChatFormatting.GOLD));
            tooltip.accept(Component.translatable("item.guhs.vetbootje_werfboekje.lore2").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The folded heteluchtballon: use it on the ground and it unfolds there. */
    public static class Ballon extends Item {
        public Ballon(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            if (!(level instanceof ServerLevel sl)) {
                return InteractionResult.SUCCESS;
            }
            Player player = context.getPlayer();
            ItemStack stack = context.getItemInHand();
            UUID eigenaar = Heteluchtballon.eigenaar(stack);
            if (!Heteluchtballon.geldig(sl.getServer(), eigenaar, Heteluchtballon.nr(stack))) {
                stack.shrink(1);                                   // an old copy: the new one works
                if (player != null) {
                    player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.ballon.lek").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                return InteractionResult.CONSUME;
            }
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            VoxelShape vorm = level.getBlockState(pos).getCollisionShape(level, pos);
            double y = pos.getY() + (vorm.isEmpty() ? 0 : vorm.max(Direction.Axis.Y));
            HeteluchtballonEntity ballon = Heteluchtballon.vouwUit(sl, new Vec3(pos.getX() + 0.5, y, pos.getZ() + 0.5), player == null ? 0 : player.getYRot(), stack);
            if (ballon == null) {
                if (player != null) {
                    player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.ballon.geen_plek").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                return InteractionResult.FAIL;
            }
            if (player == null || !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (player instanceof ServerPlayer sp && !GuhQuests.saved(sp).getBooleanOr("guhs_einde2_vetbootje_ballon_neergezet", false)) {
                GuhQuests.saved(sp).putBoolean("guhs_einde2_vetbootje_ballon_neergezet", true);
                sp.sendSystemMessage(Component.translatable("gui.guhs.einde2_vetbootje.ballon.neergezet").withStyle(ChatFormatting.AQUA));
            }
            return InteractionResult.CONSUME;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.heteluchtballon.lore").withStyle(ChatFormatting.GOLD));
            tooltip.accept(Component.translatable("item.guhs.heteluchtballon.lore2").withStyle(ChatFormatting.GRAY));
        }
    }

    private VetbootjeItems() {
    }
}
