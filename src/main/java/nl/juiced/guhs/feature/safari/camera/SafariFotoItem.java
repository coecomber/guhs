package nl.juiced.guhs.feature.safari.camera;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.component.DataComponents;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;

/**
 * A printed safari photo (S1, "Afdrukken" in the Guhdex's Safari tab): a little photo card of the best dex photo of a
 * species, with its stars, caption and who took it. Printing costs a sheet of paper (nothing in creative).
 */
public class SafariFotoItem extends Item {
    static final String FOTO = "SafariFoto";

    public SafariFotoItem(Properties properties) {
        super(properties);
    }

    public static CompoundTag foto(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompoundOrEmpty(FOTO);
    }

    public static ItemStack van(CompoundTag foto) {
        ItemStack stack = new ItemStack(CameraFeature.FOTO.get());
        CompoundTag t = new CompoundTag();
        t.put(FOTO, foto.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return stack;
    }

    /** The server side of the print button. */
    static void afdrukken(ServerPlayer p, String soort) {
        SafariSpeler.DexRegel r = SafariSpeler.dex(p, soort);
        if (r.sterren() <= 0 || r.foto().isEmpty()) {
            return;
        }
        if (!p.getAbilities().instabuild) {
            int slot = p.getInventory().findSlotMatchingItem(new ItemStack(Items.PAPER));
            if (slot < 0) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.safari.afdrukken.papier").withStyle(ChatFormatting.RED));
                return;
            }
            p.getInventory().removeItem(slot, 1);
        }
        p.getInventory().placeItemBackInInventory(van(r.foto()));
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 0.8f, 1.2f);
        p.sendOverlayMessage(Component.translatable("gui.guhs.safari.afdrukken.klaar", Component.translatable("entity.guhs.safari." + soort))
                .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag f = foto(stack);
        String soort = f.getStringOr("Soort", "");
        return soort.isEmpty() ? super.getName(stack)
                : Component.translatable("item.guhs.safari_foto.van", Component.translatable("entity.guhs.safari." + soort));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        CompoundTag f = foto(stack);
        if (f.isEmpty()) {
            tooltip.accept(Component.translatable("item.guhs.safari_foto.leeg").withStyle(ChatFormatting.GRAY));
            return;
        }
        int sterren = f.getIntOr("Sterren", 1);
        tooltip.accept(Component.literal("★".repeat(sterren) + "☆".repeat(Math.max(0, 3 - sterren))).withStyle(ChatFormatting.GOLD));
        tooltip.accept(Foto.onderschrift(f).copy().withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
        String wie = f.getStringOr("Fotograaf", "");
        if (!wie.isEmpty()) {
            tooltip.accept(Component.translatable("item.guhs.safari_foto.door", wie).withStyle(ChatFormatting.GRAY));
        }
    }
}
