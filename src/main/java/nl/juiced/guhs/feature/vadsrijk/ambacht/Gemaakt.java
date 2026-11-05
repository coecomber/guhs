package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * What every made thing carries (VOORSTEL §3.3/§3.8): its Vadsigheid 0-100 % (component {@code guhs:vr_amb_vadsigheid}: food and
 * potions work up to 30 % better, gear rolls higher; 100 % = "Vadsig" and a glint) and its maker (component
 * {@code guhs:vr_amb_maker}: "Gemaakt door Juiced", the FF14 signature).
 */
public final class Gemaakt {
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VADSIGHEID = Materialen.COMPONENTS.registerComponentType(
            "vr_amb_vadsigheid", b -> b.persistent(Codec.intRange(0, 100)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MAKER = Materialen.COMPONENTS.registerComponentType(
            "vr_amb_maker", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private Gemaakt() {
    }

    public static int vadsigheid(ItemStack s) {
        Integer v = s.get(VADSIGHEID.get());
        return v == null ? 0 : v;
    }

    /** The effect factor of food/potions: 1.0 .. 1.3. */
    public static double factor(ItemStack s) {
        return 1 + 0.3 * vadsigheid(s) / 100.0;
    }

    public static void zet(ItemStack s, int vadsigheid, String maker) {
        s.set(VADSIGHEID.get(), Math.max(0, Math.min(100, vadsigheid)));
        if (!maker.isEmpty()) {
            s.set(MAKER.get(), maker);
        }
        if (vadsigheid >= 100) {
            s.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
    }

    /** The two tooltip lines (pink bar + maker). */
    public static void tooltip(ItemStack s, Consumer<Component> t) {
        Integer v = s.get(VADSIGHEID.get());
        if (v != null) {
            int vol = Math.round(v / 10f);
            t.accept(Component.translatable("vadsrijk.ambacht.vadsigheid", "▮".repeat(vol) + "▯".repeat(10 - vol), v)
                    .withStyle(v >= 100 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE));
        }
        String m = s.get(MAKER.get());
        if (m != null) {
            t.accept(Component.translatable("vadsrijk.ambacht.gemaakt_door", m).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
