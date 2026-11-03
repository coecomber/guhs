package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.storage.Nbt;

/**
 * Jewellery (RING, KETTING, OORBEL): there are no vanilla slots for them, so they live in the player data
 * ({@link SpelerData#sieraden}, raw item tags, Vadsrijk only: they never touch the live inventory, so the inventory swap can't lose
 * them). Put one on: right-click it in the dimension (the one you wore goes into your hand). Take one off: click its slot
 * next to the inventory screen. {@link Spelers#stats} counts them (level requirement as for all gear).
 */
public final class Sieraden {
    private Sieraden() {
    }

    public static ItemStack get(ServerPlayer p, Slot slot) {
        SpelerData d = Spelers.data(p);
        ItemStack c = d.sieradenCache.get(slot.name());
        if (c == null) {
            CompoundTag t = d.sieraden.get(slot.name());
            c = t == null ? ItemStack.EMPTY : Nbt.parseStack(p.registryAccess(), t);
            d.sieradenCache.put(slot.name(), c);
        }
        return c;
    }

    static void zet(ServerPlayer p, Slot slot, ItemStack s) {
        SpelerData d = Spelers.data(p);
        if (s.isEmpty()) {
            d.sieraden.remove(slot.name());
        } else {
            d.sieraden.put(slot.name(), Nbt.saveStack(p.registryAccess(), s));
        }
        d.sieradenCache.put(slot.name(), s.copy());
    }

    /** Right-click with a piece of jewellery: wear it (swap with the worn one). */
    public static boolean draag(ServerPlayer p, InteractionHand hand) {
        ItemStack inHand = p.getItemInHand(hand);
        Uitrusting u = Rollen.van(inHand);
        BasisType t = u == null ? null : u.basisType();
        if (t == null || !t.slot().sieraad() || !Vadsrijk.in(p)) {
            return false;
        }
        if (u.level() > Spelers.level(p)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.kern.sieraad.te_hoog", u.level()).withStyle(ChatFormatting.GRAY));
            return true;
        }
        ItemStack oud = get(p, t.slot());
        zet(p, t.slot(), inHand.copyWithCount(1));
        inHand.shrink(1);
        if (!oud.isEmpty()) {
            if (inHand.isEmpty()) {
                p.setItemInHand(hand, oud);
            } else if (!p.getInventory().add(oud)) {
                p.drop(oud, false);
            }
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1f, 1.3f);
        p.sendOverlayMessage(Component.translatable("vadsrijk.kern.sieraad.om", inHand.isEmpty() ? get(p, t.slot()).getHoverName() : get(p, t.slot()).getHoverName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        stuur(p);
        Spelers.sync(p);
        return true;
    }

    /** Take a piece off (into the inventory; stays on when the inventory is full). */
    public static void af(ServerPlayer p, Slot slot) {
        if (!slot.sieraad() || !Vadsrijk.in(p)) {
            return;
        }
        ItemStack s = get(p, slot);
        if (s.isEmpty()) {
            return;
        }
        ItemStack kopie = s.copy();
        if (!p.getInventory().add(kopie) || !kopie.isEmpty()) {
            if (kopie.getCount() == s.getCount()) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.kern.sieraad.vol").withStyle(ChatFormatting.GRAY));
                return;
            }
        }
        zet(p, slot, ItemStack.EMPTY);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1f, 1.1f);
        stuur(p);
        Spelers.sync(p);
    }

    /** The worn pieces to the client (for the slots next to the inventory screen). */
    public static void stuur(ServerPlayer p) {
        List<ItemStack> l = new ArrayList<>();
        for (Slot s : Slot.SIERADEN) {
            l.add(Vadsrijk.in(p) ? get(p, s).copy() : ItemStack.EMPTY);
        }
        KernPayloads.stuur(p, new KernPayloads.SieradenSync(l));
    }
}
