package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.UseCooldown;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * How Vadsrijk potions are drunk (balans 2026-11, second playtest): flat numbers in tiers instead of a % of your max, a 2 s
 * drink (the vanilla drink animation, via the {@link Consumable} component) and one shared 10 s potion cooldown for the
 * Vads/energy potions (the {@link UseCooldown} component with the cooldown group {@link #GROEP}: drinking one puts every potion
 * of the group on the item cooldown overlay). The buff potions (Vahoeg, Snelpootjes) drink in 2 s too but keep their own short
 * cooldown.
 * <pre>
 *   Klein Vadsdrankje   120 Vads              8 brons (zone-1 shops)
 *   Vadsdrankje         300 Vads             40 brons
 *   Groot Vadsdrankje   700 Vads            300 brons (3 zilver, zone 2+)
 *   Warme Chocomelk     250 Vads + thaw / 3 min unfreezable
 *   Energiedrankje      +50 energy
 *   Groot Energiedrankje +120 energy
 * </pre>
 * The item ids did not change (vr_item_vadsdrankje, vr_z2_grote_vadsdrank, ...), only their names/numbers, so old stacks just
 * work; the Klein Vadsdrankje is new (vr_item_klein_vadsdrankje).
 */
public final class Drinken {
    /** The shared cooldown group of the Vads/energy potions. */
    public static final Identifier GROEP = Guhs.id("vr_drankje");
    public static final float DRINK_SECONDEN = 2f;
    public static final float COOLDOWN_SECONDEN = 10f;

    public static final int KLEIN_VADS = 120, VADS = 300, GROOT_VADS = 700, CHOCOMELK_VADS = 250;
    public static final int ENERGIE = 50, GROOT_ENERGIE = 120;

    private Drinken() {
    }

    /** Item properties of a potion: stacks of 16, 2 s drink; {@code gedeeld} = in the shared 10 s potion cooldown. */
    public static Item.Properties eigenschappen(boolean gedeeld) {
        Item.Properties p = new Item.Properties().stacksTo(16).component(DataComponents.CONSUMABLE, Consumable.builder()
                .consumeSeconds(DRINK_SECONDEN).animation(ItemUseAnimation.DRINK).sound(SoundEvents.GENERIC_DRINK).hasConsumeParticles(false).build());
        if (gedeeld) {
            p = p.component(DataComponents.USE_COOLDOWN, new UseCooldown(COOLDOWN_SECONDEN, Optional.of(GROEP)));
        }
        return p;
    }

    /** Outside the Vadsrijk a potion does nothing: true (and a message) there. */
    public static boolean buiten(Player player, String buitenKey) {
        if (Vadsrijk.in(player)) {
            return false;
        }
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable(buitenKey).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    /** Flat energy back (capped at the max). */
    public static void energie(ServerPlayer sp, double n) {
        var d = Spelers.data(sp);
        d.energie = Math.min(Spelers.maxEnergie(sp), Spelers.energie(sp) + n);
        Spelers.sync(sp);
    }
}
