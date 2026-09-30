package nl.juiced.guhs.feature.mewtwo;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * A lab note of Professor Knabbelkloon (numbered 1..6 in its custom data {@code guhs_mewtwo_notitie}): right-click to read its
 * bit of the story (the talking screen, no speaker). Found at the note spots of the kloon-eiland ({@link MewtwoBlokken.Notitieplek}).
 */
public class LabnotitieItem extends Item {
    public static final String NUMMER = "guhs_mewtwo_notitie";

    public LabnotitieItem(Properties properties) {
        super(properties);
    }

    public static ItemStack maak(int n) {
        ItemStack s = new ItemStack(MewtwoFeature.LABNOTITIE.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt(NUMMER, n);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        s.set(DataComponents.ITEM_NAME, Component.translatable("item.guhs.mewtwo_labnotitie.nummer", n));
        return s;
    }

    public static int nummer(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0 : d.copyTag().getIntOr(NUMMER, 0);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        int n = nummer(s);
        if (n >= 1 && n <= MewtwoFeature.NOTITIES) {
            if (player instanceof ServerPlayer sp) {
                level.playSound(null, player.blockPosition(), MewtwoFeature.NOTITIE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.1f);
                MewtwoVerhaal.leesNotitie(sp, n);
            }
            return InteractionResult.SUCCESS.heldItemTransformedTo(s);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.mewtwo_labnotitie.lore").withStyle(ChatFormatting.GRAY));
    }
}
