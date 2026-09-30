package nl.juiced.guhs.feature.vogels;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The pluisveertje: a soft pink-white fluff feather of a Pluisvinkje. The Wolkenhoeder of the Hemelkapelletje asks one to make
 * the Knuffelhart beat. Right-click to blow it into the air a little: a puff of pink feathers (it isn't used up).
 */
public class Pluisveertje extends Item {
    public Pluisveertje(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(0.8));
            server.sendParticles(VogelsFeature.VEERTJE.get(), at.x, at.y, at.z, 6, 0.2, 0.1, 0.2, 0.02);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), VogelsFeature.TJIEP.get(), SoundSource.PLAYERS, 0.4f, 1.6f);
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.pluisveertje.lore").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.guhs.pluisveertje.lore2").withStyle(ChatFormatting.GRAY));
    }
}
