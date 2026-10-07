package nl.juiced.guhs.feature.einde2.appa;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * einde2, slice appa: the Appa-fluitje. Blow it and your Appa-guh comes, wherever he is (dead, far away, in another
 * dimension: {@link Appa#roep}); blow it while sneaking and your Momo-guh comes back to your shoulder ({@link Momo#roep}).
 * Without an Appa it only makes a sound nobody hears; while he is kept away ({@link Appa#houdWeg}) it says so.
 */
public class AppaFluitjeItem extends Item {
    /** Ticks between two whistles. */
    public static final int WACHT = 60;

    public AppaFluitjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        blaas(sp, player.isSecondaryUseActive());
        player.getCooldowns().addCooldown(stack, WACHT);
        return InteractionResult.SUCCESS;
    }

    /** Server: the whistle itself (the item's use, without the cooldown). */
    public static void blaas(ServerPlayer p, boolean momo) {
        p.level().playSound(null, p.blockPosition(), E2AppaFeature.FLUITJE.get(), SoundSource.PLAYERS, 0.9f, momo ? 1.3f : 1f);
        if (momo) {
            if (!Momo.heeft(p)) {
                zeg(p, Component.translatable("gui.guhs.einde2_appa.fluitje.momo_geen"));
            } else if (Momo.opSchouder(p)) {
                zeg(p, Component.translatable("gui.guhs.einde2_appa.fluitje.momo_al", Momo.naam(p)));
            } else if (Momo.roep(p) != null) {
                zeg(p, Component.translatable("gui.guhs.einde2_appa.fluitje.momo_komt", Momo.naam(p)));
            }
            return;
        }
        if (!Appa.heeft(p)) {
            zeg(p, Component.translatable("gui.guhs.einde2_appa.fluitje.geen"));
        } else if (Appa.isWeg(p)) {
            zeg(p, Component.translatable("gui.guhs.einde2_appa.fluitje.weg"));
        } else if (p.getVehicle() instanceof AppaGuhEntity appa && appa.isEigenaar(p)) {
            zeg(p, Component.translatable("gui.guhs.einde2_appa.fluitje.zit_al"));
        } else {
            AppaGuhEntity appa = Appa.roep(p);
            if (appa != null) {
                appa.brul();
                zeg(p, Component.translatable("gui.guhs.einde2_appa.fluitje.komt", appa.getDisplayName()));
            }
        }
    }

    private static void zeg(ServerPlayer p, Component tekst) {
        p.sendOverlayMessage(tekst.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.appa_fluitje.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.appa_fluitje.tooltip").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable("item.guhs.appa_fluitje.tooltip.momo").withStyle(ChatFormatting.DARK_AQUA));
    }
}
