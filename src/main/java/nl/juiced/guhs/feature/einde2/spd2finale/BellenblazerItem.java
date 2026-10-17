package nl.juiced.guhs.feature.einde2.spd2finale;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.Vec3;

/**
 * einde2 "Sponsguh", slice sp_d2_finale: the bubble blaster (CONTRACT_E2_C 7.8). A right click blows a stream of soap bubbles
 * straight ahead; in step 10 a puff at a guh with one of Plankmika's bucket helmets knocks the bucket off ({@link D2fEmmers#blaas}).
 * Kept after the story, just for fun (it never harms anything). Texts: item.guhs.einde2_sp_d2_finale_bellenblazer(.lore, .tooltip).
 */
public class BellenblazerItem extends Item {
    /** Ticks between two puffs. */
    public static final int WACHT = 8;

    public BellenblazerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer p = (ServerPlayer) player;
        stroom(p);
        D2fEmmers.blaas(p);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), WACHT);
        return InteractionResult.SUCCESS;
    }

    /** The stream of bubbles: a line of them along the look, for everybody near. */
    static void stroom(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 oog = p.getEyePosition().add(0, -0.2, 0), kijk = p.getLookAngle();
        for (double d = 0.8; d <= D2fEmmers.BEREIK; d += 0.6) {
            Vec3 q = oog.add(kijk.scale(d));
            level.sendParticles(ParticleTypes.BUBBLE_POP, q.x, q.y, q.z, 1, 0.08 + d * 0.02, 0.08 + d * 0.02, 0.08 + d * 0.02, 0.0);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 0.7f, 1.4f + p.getRandom().nextFloat() * 0.3f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.DARK_AQUA));
    }
}
