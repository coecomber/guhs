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
            if (geefSterrennacht(sp)) {
                player.getItemInHand(hand).shrink(1);
            } else {
                sp.sendSystemMessage(Component.translatable("vadsrijk.vint.z6raid.halster.al").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }

    /** V-INT: the Sterrennacht look of the Bultguh = the Bultguh (when you have none yet) + the coat {@code vacht:bultguh:sterrennacht}, worn. */
    public static final String VACHT = "vacht:bultguh:sterrennacht";

    /** V-INT: true when you own the Bultguh with the Sterrennacht coat. */
    public static boolean heeftSterrennacht(ServerPlayer p) {
        nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.Variant v = nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.vanSleutel(VACHT);
        return Rijdieren.heeft(p, nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.BULTGUH)
                && (v == null || nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.heeft(p, v));
    }

    /**
     * V-INT (the user's mount-drop rule): gives the Bultguh + the Sterrennacht coat and puts it on. Used by the raid's mount roll
     * ({@code RijdierBuit} source "z6raid_sterrennacht", 10 % per clear + pity) and by this halter item. False when you had both.
     */
    public static boolean geefSterrennacht(ServerPlayer p) {
        if (heeftSterrennacht(p)) {
            return false;
        }
        Rijdieren.geef(p, nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.BULTGUH);
        nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.geef(p, VACHT);
        nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.kies(p, nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.BULTGUH,
                nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.As.VACHT, "sterrennacht");
        p.sendSystemMessage(Component.translatable("vadsrijk.vint.z6raid.halster.ok").withStyle(ChatFormatting.GOLD));
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, t, flag);
        t.accept(Component.translatable("vadsrijk.vint.z6raid.halster.uitleg").withStyle(ChatFormatting.GRAY));
    }
}
