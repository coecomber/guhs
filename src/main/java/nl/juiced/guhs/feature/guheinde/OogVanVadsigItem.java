package nl.juiced.guhs.feature.guheinde;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Het Oog van Vadsig (guhkristal + kaasknabbel + Mika-traan): throw it in the Guhmension and it flies towards the nearest
 * Knabbelkelder, like an eye of ender (and sometimes goes "njeg" and breaks). Put twelve in the knabbelportaalframes
 * of the portal room to open the way to the Guheinde.
 */
public class OogVanVadsigItem extends Item {
    public OogVanVadsigItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof KnabbelportaalframeBlock) || state.getValue(KnabbelportaalframeBlock.OOG)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState filled = state.setValue(KnabbelportaalframeBlock.OOG, true);
        Block.pushEntitiesUp(state, filled, level, pos);
        level.setBlock(pos, filled, 2);
        level.updateNeighbourForOutputSignal(pos, filled.getBlock());
        context.getItemInHand().shrink(1);
        level.levelEvent(1503, pos, 0);
        if (KnabbelportaalframeBlock.tryOpenPortal(level, pos) && context.getPlayer() instanceof ServerPlayer player) {
            GuhAdvancements.grant(player, "guheinde_portaal");
            GuheindeEvents.advancement(player, "guheinde_portaal");   // (the shown one: guheinde/, not quest/)
            player.sendSystemMessage(Component.translatable("gui.guhs.guheinde.portaal_open").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() == HitResult.Type.BLOCK && level.getBlockState(hit.getBlockPos()).getBlock() instanceof KnabbelportaalframeBlock) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        if (level instanceof ServerLevel server) {
            if (server.dimension() != ModDimensions.GUHMENSION) {
                player.displayClientMessage(Component.translatable("gui.guhs.guheinde.oog_alleen_guhmensie").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return InteractionResultHolder.fail(stack);
            }
            BlockPos target = server.findNearestMapStructure(GuheindeFeature.OOG_LOCATED, player.blockPosition(), 100, false);
            if (target == null) {
                player.displayClientMessage(Component.translatable("gui.guhs.guheinde.oog_niks").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return InteractionResultHolder.fail(stack);
            }
            OogVanVadsigEntity oog = new OogVanVadsigEntity(server, player.getX(), player.getY(0.5), player.getZ());
            oog.setItem(stack);
            oog.setOwner(player);
            oog.signalTo(target);
            server.gameEvent(GameEvent.PROJECTILE_SHOOT, oog.position(), GameEvent.Context.of(player));
            server.addFreshEntity(oog);
            if (player instanceof ServerPlayer sp) {
                GuhAdvancements.grant(sp, "guheinde_oog");
                GuheindeEvents.advancement(sp, "guheinde_oog");   // (the shown one: guheinde/, not quest/)
            }
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1f,
                    Mth.lerp(server.random.nextFloat(), 0.33f, 0.5f));
            stack.consume(1, player);
            player.swing(hand, true);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.oog_van_vadsig.lore").withStyle(ChatFormatting.GRAY));
    }
}
