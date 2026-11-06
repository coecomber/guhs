package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;

/**
 * U-Z6RAID: the Sterrennacht-halster (rare raid drop): right-click = the raid colour of the bultguh mount. The bultguh itself is
 * the z6-rijdieren slice's; this asks {@code Rijdieren.geef(p, "bultguh:sterrennacht")} (then "bultguh_sterrennacht"). When the
 * mount does not exist yet on this build, the halter stays in your bag ("bewaar hem goed").
 */
public class Z6RaidHalster extends Item {
    public Z6RaidHalster(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            boolean ok = Rijdieren.geef(sp, Z6RaidLoot.HALSTER_RIJDIER) || Rijdieren.geef(sp, "bultguh_sterrennacht");
            if (ok) {
                player.getItemInHand(hand).shrink(1);
                sp.sendSystemMessage(Component.translatable("vadsrijk.z6raid.halster.ok").withStyle(ChatFormatting.GOLD));
            } else {
                sp.sendSystemMessage(Component.translatable("vadsrijk.z6raid.halster.later").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, t, flag);
        t.accept(Component.translatable("item.guhs.vr_z6raid_halster.uitleg").withStyle(ChatFormatting.GRAY));
    }
}
