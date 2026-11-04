package nl.juiced.guhs.feature.vadsrijk.client;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.wezens.loot.SetBonussen;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Sets;

/**
 * (fix round 2, g-ui) Gear tooltip lines that need every slice's sets (so they live here, next to {@link VadsrijkClient}):
 * <ul>
 *   <li>jewellery: how to wear it (right-click, or shift-click in the inventory);</li>
 *   <li>armour that is in no set: "Los stuk" (e.g. the zone-1 Kristalhelmpje, which is NOT part of the zone-2 Kristalset);</li>
 *   <li>{@link #stukken}: the "Kristalkroon · Kristalborstplaat · ..." line under a set piece (the sets' own tooltip code calls it),
 *   worn pieces green.</li>
 * </ul>
 */
public final class UitrustingTips {
    private UitrustingTips() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> regels(e));
    }

    private static void regels(ItemTooltipEvent e) {
        Uitrusting u = Rollen.van(e.getItemStack());
        BasisType bt = u == null ? null : u.basisType();
        if (bt == null) {
            return;
        }
        List<Component> t = e.getToolTip();
        if (bt.slot().sieraad()) {
            t.add(Component.translatable("vadsrijk.kern.sieraad.hoe").withStyle(ChatFormatting.DARK_AQUA));
        } else if (bt.slot().harnas() && setVan(u.type()) == null) {
            t.add(Math.max(1, t.size() - 1), Component.translatable("vadsrijk.kern.item.los_stuk").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** The set of a base type (any zone), or null. */
    @Nullable
    public static String setVan(String type) {
        String s = SetBonussen.setVan(type);
        return s != null ? s : Z2Sets.setVan(type);
    }

    /** "  Kristalkroon · Kristalborstplaat · ...": the pieces of a set, the ones {@code p} wears green, the rest dark grey. */
    public static Component stukken(List<String> stukken, @Nullable Player p) {
        MutableComponent c = Component.literal("  ");
        for (int i = 0; i < stukken.size(); i++) {
            if (i > 0) {
                c.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
            }
            String id = stukken.get(i);
            c.append(Component.translatable("vadsrijk.loot." + id).withStyle(draagt(p, id) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
        return c;
    }

    private static boolean draagt(@Nullable Player p, String type) {
        if (p == null) {
            return false;
        }
        for (EquipmentSlot es : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            Uitrusting u = Rollen.van(p.getItemBySlot(es));
            if (u != null && !u.verpluisd() && type.equals(u.type())) {
                return true;
            }
        }
        return false;
    }
}
