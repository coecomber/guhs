package nl.juiced.guhs.feature.knus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.knus.KnusPayloads;
import nl.juiced.guhs.feature.knus.KnusVoortgang;

/** Client side of the Knus framework: toasts for new milestones and collection entries; the Guhdex's Knus tab refreshes. */
public final class KnusClient {
    public static void init(IEventBus modBus) {
    }

    /** guhs:knus_data arrived (the data itself is already in KnusVoortgang.Client). */
    public static void knusData(KnusPayloads.KnusData payload) {
        Minecraft mc = Minecraft.getInstance();
        for (Tag t : payload.extra().getListOrEmpty("Toasts")) {
            CompoundTag m = (CompoundTag) t;
            String id = m.getStringOr("Id", "");
            if (m.getStringOr("Soort", "").equals("mijlpaal")) {
                KnusVoortgang.Mijlpaal mijlpaal = KnusVoortgang.mijlpaal(id);
                if (mijlpaal != null) {
                    ItemStack icon = safe(mijlpaal.beloning().get());
                    mc.getToasts().addToast(new KnusToast(Component.translatable("gui.guhs.knus.toast.mijlpaal"), mijlpaal.naam(), icon));
                }
            } else {
                KnusVoortgang.Verzameling v = KnusVoortgang.verzameling(id);
                if (v != null) {
                    ItemStack icon = safe(v.icoon().apply(m.getStringOr("Item", "")));
                    mc.getToasts().addToast(new KnusToast(v.naam(), v.item(m.getStringOr("Item", "")), icon));
                }
            }
        }
        if (mc.screen instanceof nl.juiced.guhs.client.screen.GuhDexScreen screen) {
            screen.knusChanged();
        }
    }

    private static ItemStack safe(ItemStack stack) {
        return stack == null || stack.isEmpty() ? new ItemStack(nl.juiced.guhs.registry.ModItems.GUHDEX.get()) : stack;
    }

    private KnusClient() {
    }
}
