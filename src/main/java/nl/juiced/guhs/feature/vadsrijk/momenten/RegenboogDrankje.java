package nl.juiced.guhs.feature.vadsrijk.momenten;

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
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * The Regenboogdrankje (vr_mom_regenboogdrankje): the caravan's rare potion. Full Vads + energy, and Vahoeg, Snelpootjes and a
 * Knuffelschild for 90 s. Only works in the Vadsrijk. Sold by the caravan, and a rare thank-you after an attack.
 */
public class RegenboogDrankje extends Item {
    public RegenboogDrankje(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        if (!Vadsrijk.in(sp)) {
            sp.sendSystemMessage(Component.translatable("vadsrijk.momenten.drankje.buiten").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        ItemStack stack = sp.getItemInHand(hand);
        Schade.heal(sp, sp, Spelers.maxVads(sp));
        Spelers.data(sp).energie = Spelers.maxEnergie(sp);
        Spelers.sync(sp);
        Statussen.geef(sp, Status.VAHOEG, 20 * 90, 1, sp);
        Statussen.geef(sp, Status.SNELPOOTJES, 20 * 90, 1, sp);
        Statussen.geef(sp, Status.KNUFFELSCHILD, 20 * 90, Spelers.maxVads(sp) * 0.3, sp);
        if (!sp.isCreative()) {
            stack.shrink(1);
        }
        sp.getCooldowns().addCooldown(stack, 200);
        level.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
        level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, sp.getX(), sp.getY() + 1.4, sp.getZ(), 8, 0.5, 0.3, 0.5, 0.02);
            sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1, sp.getZ(), 20, 0.5, 0.6, 0.5, 0.05);
        }
        sp.sendSystemMessage(Component.translatable("vadsrijk.momenten.drankje.op").withStyle(ChatFormatting.LIGHT_PURPLE));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".uitleg").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("vadsrijk.economie.alleen_rijk").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
