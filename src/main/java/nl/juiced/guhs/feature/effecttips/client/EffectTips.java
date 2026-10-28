package nl.juiced.guhs.feature.effecttips.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.GatherEffectScreenTooltipsEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.0: hover an effect in the inventory's effect list and a little tooltip says what it does. The text is the lang key
 * {@code <effect's description id>.uitleg} (so {@code effect.guhs.bubbelig.uitleg}, {@code effect.minecraft.speed.uitleg});
 * a guhs effect without one gets {@link #ONBEKEND}, a vanilla one without one keeps vanilla's own tooltip. New effects
 * take part by themselves; TaalsleutelsGameTests.effectUitleg reports guhs effects that still miss their text.
 */
public final class EffectTips {
    /** The suffix of an effect's explanation key. */
    public static final String UITLEG = ".uitleg";
    /** The text for a guhs effect without its own explanation. */
    public static final String ONBEKEND = "effect.guhs.uitleg.onbekend";
    /** How wide a tooltip line may get (in pixels) before it wraps. */
    private static final int BREEDTE = 200;

    private EffectTips() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(EffectTips::tooltip);
    }

    static void tooltip(GatherEffectScreenTooltipsEvent event) {
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        String sleutel = effect.value().getDescriptionId() + UITLEG;
        boolean vanOns = effect.unwrapKey().map(k -> k.identifier().getNamespace().equals(Guhs.MODID)).orElse(false);
        if (!I18n.exists(sleutel)) {
            if (!vanOns) {
                return;
            }
            sleutel = ONBEKEND;
        }
        List<Component> regels = event.getTooltip();
        if (regels.isEmpty()) {           // (the effect's name is fully visible: vanilla shows no tooltip, so start with the name)
            regels.add(effect.value().getDisplayName().copy().withStyle(ChatFormatting.WHITE));
        }
        Style grijs = Style.EMPTY.withColor(ChatFormatting.GRAY);
        Minecraft.getInstance().font.getSplitter().splitLines(Component.translatable(sleutel), BREEDTE, grijs)
                .forEach(regel -> regels.add(Component.literal(regel.getString()).withStyle(grijs)));
    }
}
