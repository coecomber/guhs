package nl.juiced.guhs.feature.guhpixel.reisbureau;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.taal.Tekst;

/**
 * An ansichtkaart ({@code guhs:reisbureau_kaart_<destination>}): what a guh sends home from its trip. Right-click to read
 * it: the text is the destination's own (book.guhs.reisbureau.kaart.&lt;id&gt;, in the guh's voice, translated per reader)
 * and it is signed with the name the guh had when it came back (kept in the stack's custom data {@value #TAG}: {@code Naam}).
 * A card without a name (creative tab, a command) is signed "je guh".
 */
public class KaartItem extends Item {
    public static final String TAG = "GuhsReiskaart";
    private final Bestemming bestemming;

    public KaartItem(Bestemming bestemming, Properties properties) {
        super(properties);
        this.bestemming = bestemming;
    }

    public Bestemming bestemming() {
        return bestemming;
    }

    /** A card from this destination, signed by this guh. */
    public static ItemStack maak(Bestemming b, Component guhNaam) {
        ItemStack stack = new ItemStack(b.kaart());
        CompoundTag kaart = new CompoundTag();
        Tekst.put(kaart, "Naam", guhNaam);
        CompoundTag tag = new CompoundTag();
        tag.put(TAG, kaart);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** Who signed this card ("je guh" when nobody did). */
    public static Component afzender(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            Component naam = Tekst.get(data.copyTag().getCompoundOrEmpty(TAG), "Naam");
            if (!Tekst.empty(naam)) {
                return naam;
            }
        }
        return Component.translatable("gui.guhs.reisbureau.kaart.je_guh");
    }

    /** What the reading screen needs: {@code Bestemming}, {@code Naam}. */
    public CompoundTag leesData(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Bestemming", bestemming.id());
        Tekst.put(tag, "Naam", afzender(stack));
        return tag;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.2f);
            ModNetworking.sendTo(sp, new ReisbureauPayloads.Kaart(leesData(stack)));
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("gui.guhs.reisbureau.kaart.van", afzender(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.accept(Component.translatable("gui.guhs.reisbureau.kaart.lezen").withStyle(ChatFormatting.GRAY));
    }
}
