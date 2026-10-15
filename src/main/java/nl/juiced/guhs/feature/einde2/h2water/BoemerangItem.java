package nl.juiced.guhs.feature.einde2.h2water;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Sokka-guh's boemerang (DESIGN 5 "Wat je overhoudt": "komt altijd terug"): throw it and it flies out and back
 * ({@link BoemerangEntity}), bringing whatever it touched on the way. It is never used up and never lost: it stays in your
 * inventory while its flight is in the air (one flight at a time per player).
 */
public class BoemerangItem extends Item {
    public BoemerangItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            boolean alInDeLucht = !server.getEntitiesOfClass(BoemerangEntity.class, player.getBoundingBox().inflate(BoemerangEntity.MAX_AFSTAND * 2),
                    b -> b.getOwner() == player).isEmpty();
            if (alInDeLucht) {
                return InteractionResult.FAIL;
            }
            BoemerangEntity b = new BoemerangEntity(level, player);
            b.setItem(stack.copyWithCount(1));
            b.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.3f, 0.5f);
            server.addFreshEntity(b);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.6f, 1.5f);
        }
        player.getCooldowns().addCooldown(stack, 20);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.boemerang.lore").withStyle(ChatFormatting.GRAY));
    }
}
