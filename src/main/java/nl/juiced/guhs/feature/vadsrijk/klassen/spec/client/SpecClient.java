package nl.juiced.guhs.feature.vadsrijk.klassen.spec.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.GearVergelijk;

/**
 * U-SPECS client: the better/worse arrows in the Vadsrijk gear tooltips ({@link GearVergelijk}): "Vergeleken met je ...", per
 * stat ▲ green / ▼ red, and the verdict. Weapons compare with the weapon in your main hand (same class only), armour with the
 * worn piece, jewellery with the worn one (rings: the weaker of your two). Kern's jewellery receiver is wrapped to keep a copy.
 */
public final class SpecClient {
    private static List<ItemStack> sieraden = new ArrayList<>();
    private static final int MAX_REGELS = 6;

    private SpecClient() {
    }

    public static void init(IEventBus modBus) {
        var oud = KernPayloads.sieradenOntvanger;
        KernPayloads.sieradenOntvanger = s -> {
            oud.accept(s);
            sieraden = new ArrayList<>(s.items());
        };
        RijkItem.TOOLTIP_EXTRA.add(SpecClient::tooltip);
    }

    @Nullable
    private static Uitrusting sieraad(Slot s) {
        for (int i = 0; i < Slot.SIERADEN.length && i < sieraden.size(); i++) {
            if (Slot.SIERADEN[i] == s) {
                return Rollen.van(sieraden.get(i));
            }
        }
        return null;
    }

    /** What the player wears in the slot of {@code u} (null = nothing / not comparable); {@code leeg[0]} = the slot is empty. */
    @Nullable
    static Uitrusting gedragen(Player p, Uitrusting u, BasisType bt, boolean[] leeg, @Nullable String klasse) {
        Slot slot = bt.slot();
        Uitrusting oud;
        if (slot == Slot.WAPEN) {
            oud = Rollen.van(p.getMainHandItem());
            if (oud == null || oud.basisType() == null || !java.util.Objects.equals(Spelers.wapenKlasse(oud.basisType()), Spelers.wapenKlasse(bt))) {
                return null;   // (another class's weapon: no fair comparison)
            }
        } else if (slot.vanilla != null) {
            oud = Rollen.van(p.getItemBySlot(slot.vanilla));
        } else if (slot == Slot.RING || slot == Slot.RING2) {
            Uitrusting a = sieraad(Slot.RING), b = sieraad(Slot.RING2);
            if (u.equals(a) || u.equals(b)) {
                return null;
            }
            if (a == null || b == null) {
                oud = null;
            } else {
                oud = GearVergelijk.vergelijk(a, b, klasse).score() < 0 ? a : b;   // the weaker ring is the one you'd swap
            }
        } else {
            oud = sieraad(slot);
        }
        if (oud == null) {
            leeg[0] = true;
            return null;
        }
        return oud;
    }

    static void tooltip(Uitrusting u, Consumer<Component> t) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        BasisType bt = u.basisType();
        if (p == null || bt == null || !Vadsrijk.in(p) || u.verpluisd()) {
            return;
        }
        String klasse = bt.slot() == Slot.WAPEN ? Spelers.wapenKlasse(bt) : Spelers.klasse(p).id();
        boolean[] leeg = {false};
        Uitrusting oud = gedragen(p, u, bt, leeg, klasse);
        if (oud != null && oud.equals(u)) {
            t.accept(Component.translatable("vadsrijk.klassen.vergelijk.gedragen").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (oud == null && !leeg[0]) {
            return;
        }
        GearVergelijk.Uitslag uit = GearVergelijk.vergelijk(u, oud, klasse);
        t.accept(Component.translatable(leeg[0] ? "vadsrijk.klassen.vergelijk.leeg" : "vadsrijk.klassen.vergelijk.kop",
                Component.translatable("vadsrijk.klassen.vergelijk.slot." + bt.slot().name().toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.GRAY));
        int n = 0;
        for (GearVergelijk.Verschil v : uit.regels()) {
            if (n++ >= MAX_REGELS) {
                break;
            }
            boolean op = v.verschil() > 0;
            String getal = v.stat() != null && v.stat().procent ? String.format(Locale.ROOT, "%+.0f%%", v.verschil())
                    : String.format(Locale.ROOT, "%+.0f", v.verschil());
            MutableComponent naam = v.stat() == null ? Component.translatable("vadsrijk.klassen.vergelijk.schade") : Component.translatable(v.stat().key());
            t.accept(Component.literal(" " + (op ? "▲ " : "▼ ") + getal + " ").withStyle(op ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(naam.withStyle(ChatFormatting.GRAY)));
        }
        String oordeel = uit.oordeel() > 0 ? "beter" : uit.oordeel() < 0 ? "slechter" : "gelijk";
        t.accept(Component.translatable("vadsrijk.klassen.vergelijk." + oordeel)
                .withStyle(uit.oordeel() > 0 ? ChatFormatting.GREEN : uit.oordeel() < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW, ChatFormatting.BOLD));
    }
}
