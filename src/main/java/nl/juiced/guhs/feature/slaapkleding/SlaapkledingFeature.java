package nl.juiced.guhs.feature.slaapkleding;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.8.0 Guhdromenland (ronde U2): sleepy clothes.
 * <ul>
 *   <li>six guh clothes ({@link GuhClothes} SLAAPMASKER ... KNUFFELTJE, on existing bones): five are sold in the Zandguh's
 *       droomwinkeltje on the Droomplein (source "droomwinkel"; the shop finds the items guhs:slaapmasker, ...); the knuffeltje
 *       is Snoes' thank-you at the end of his story (source "snoesverhaal", SnoesVerhaal.naFinale);</li>
 *   <li>the player's droompyjama ({@link Droompyjama}): crafted from wool, droomzand and maanbloemetjes (the shop may sell
 *       it too); the whole set gives a small dream bonus.</li>
 * </ul>
 * Resources: tools/features/slaapkleding.py (textures, icons, armour texture, recipes, texts).
 */
public final class SlaapkledingFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** The sources (gui.guhs.kledingbron.&lt;id&gt;). */
    public static final String WINKEL = "droomwinkel", VERHAAL = "snoesverhaal";

    /** The droomwinkeltje's prices in droomzand (the shop agent uses the same numbers). */
    public static final int PRIJS_SLAAPMASKER = 12, PRIJS_NACHTJAPONNETJE = 20, PRIJS_MAANMUTSJE = 18, PRIJS_OORWOLKJES = 14,
            PRIJS_BLUB_SLOFFEN = 16;

    /** The guh clothes of this feature, in order. */
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.SLAAPMASKER, GuhClothes.NACHTJAPONNETJE, GuhClothes.MAANMUTSJE,
            GuhClothes.OORWOLKJES, GuhClothes.BLUB_SLOFFEN, GuhClothes.KNUFFELTJE);

    public static void register(IEventBus modBus) {
        Droompyjama.laad();
        ITEMS.register(modBus);
        winkel(GuhClothes.SLAAPMASKER, PRIJS_SLAAPMASKER);
        winkel(GuhClothes.NACHTJAPONNETJE, PRIJS_NACHTJAPONNETJE);
        winkel(GuhClothes.MAANMUTSJE, PRIJS_MAANMUTSJE);
        winkel(GuhClothes.OORWOLKJES, PRIJS_OORWOLKJES);
        winkel(GuhClothes.BLUB_SLOFFEN, PRIJS_BLUB_SLOFFEN);
        KledingBronnen.bron(GuhClothes.KNUFFELTJE, VERHAAL, KledingBronnen.prijs("gui.guhs.kleding.prijs.hint.snoes_bedankt"));
        NeoForge.EVENT_BUS.addListener(SlaapkledingFeature::onTooltip);
    }

    private static void winkel(GuhClothes c, int droomzand) {
        KledingBronnen.bron(c, WINKEL, KledingBronnen.prijs("gui.guhs.kleding.prijs.munt.droomzand", droomzand));
    }

    public static void creative(Consumer<ItemStack> output) {
        for (GuhClothes c : KLEDING) {
            output.accept(new ItemStack(ModItems.clothingItem(c)));
        }
        output.accept(new ItemStack(Droompyjama.MUTS.get()));
        output.accept(new ItemStack(Droompyjama.JASJE.get()));
        output.accept(new ItemStack(Droompyjama.BROEK.get()));
        output.accept(new ItemStack(Droompyjama.SLOFFEN.get()));
    }

    /** The pyjama pieces tell about the set bonus (and how many pieces you wear now). */
    private static void onTooltip(ItemTooltipEvent event) {
        if (!Droompyjama.isStuk(event.getItemStack().getItem())) {
            return;
        }
        Player p = event.getEntity();
        List<Component> t = event.getToolTip();
        t.add(Component.translatable("item.guhs.droompyjama.lore").withStyle(ChatFormatting.GRAY));
        t.add(Component.translatable("item.guhs.droompyjama.set", Droompyjama.stukken(p)).withStyle(ChatFormatting.LIGHT_PURPLE));
        for (int i = 1; i <= 3; i++) {
            t.add(Component.translatable("item.guhs.droompyjama.bonus." + i)
                    .withStyle(Droompyjama.draagtSet(p) ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        }
    }

    private SlaapkledingFeature() {
    }
}
