package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The post of the village to the Leverkisten in the main world ({@link nl.juiced.guhs.feature.erfenis.LeverkistPost}). Per owner
 * a saved queue ({@link KoppelingData#post}); a Leverkist is filled when its chunk is loaded, the owner's chests in the order
 * they were placed (the first until it is full, then the next); what does not fit waits. Every {@value #ELKE} ticks the queues
 * are looked at (and right away on {@link #verstuur}).
 */
public final class Leverkisten {
    static final int ELKE = 40;

    private Leverkisten() {
    }

    public static void verstuur(MinecraftServer s, UUID eigenaar, List<ItemStack> spul) {
        KoppelingData d = KoppelingData.get(s);
        List<ItemStack> rij = d.post(eigenaar);
        for (ItemStack st : spul) {
            if (!st.isEmpty()) {
                rij.add(st.copy());
            }
        }
        d.setDirty();
        bezorg(s, eigenaar);
    }

    public static int wacht(MinecraftServer s, UUID eigenaar) {
        List<ItemStack> rij = KoppelingData.get(s).post.get(eigenaar);
        return rij == null ? 0 : rij.stream().mapToInt(ItemStack::getCount).sum();
    }

    static void tick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (s.getTickCount() % ELKE != 17) {
            return;
        }
        KoppelingData d = KoppelingData.get(s);
        for (Map.Entry<UUID, List<ItemStack>> e : List.copyOf(d.post.entrySet())) {
            if (!e.getValue().isEmpty()) {
                bezorg(s, e.getKey());
            }
        }
    }

    /** Delivers what waits for this owner into their loaded Leverkisten. Returns how many items went in. */
    public static int bezorg(MinecraftServer s, UUID eigenaar) {
        KoppelingData d = KoppelingData.get(s);
        List<ItemStack> rij = d.post.get(eigenaar);
        if (rij == null || rij.isEmpty()) {
            return 0;
        }
        int gezet = 0;
        List<GlobalPos> kisten = d.kisten(eigenaar);
        for (Iterator<GlobalPos> it = kisten.iterator(); it.hasNext() && !rij.isEmpty(); ) {
            GlobalPos gp = it.next();
            ServerLevel level = s.getLevel(gp.dimension());
            if (level == null || !level.isLoaded(gp.pos())) {
                continue;
            }
            if (!(level.getBlockEntity(gp.pos()) instanceof Leverkist.BlockEntityImpl kist)) {
                it.remove();   // (the chest is gone: its chunk is loaded and there is something else)
                d.setDirty();
                continue;
            }
            int voor = gezet;
            for (int i = 0; i < rij.size(); i++) {
                ItemStack st = rij.get(i);
                int was = st.getCount();
                ItemStack rest = stop(kist, st);
                gezet += was - rest.getCount();
                rij.set(i, rest);
            }
            rij.removeIf(ItemStack::isEmpty);
            if (gezet > voor) {
                kist.setChanged();
            }
        }
        if (gezet > 0) {
            d.setDirty();
            ServerPlayer p = s.getPlayerList().getPlayer(eigenaar);
            if (p != null) {
                p.sendOverlayMessage(Component.translatable(rij.isEmpty() ? "erfenis.koppeling.leverkist.post" : "erfenis.koppeling.leverkist.post_vol",
                        gezet).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        return gezet;
    }

    /** Puts as much of the stack as fits into the container (first onto the same items, then into empty slots); returns the rest. */
    static ItemStack stop(Container c, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
            for (int i = 0; i < c.getContainerSize() && !rest.isEmpty(); i++) {
                ItemStack in = c.getItem(i);
                int max = c instanceof Leverkist.BlockEntityImpl ? c.getMaxStackSize()   // (1.9.5: big stacks in a Leverkist)
                        : Math.min(c.getMaxStackSize(), rest.getMaxStackSize());
                if (pass == 0 && !in.isEmpty() && ItemStack.isSameItemSameComponents(in, rest) && in.getCount() < max) {
                    int n = Math.min(max - in.getCount(), rest.getCount());
                    in.grow(n);
                    rest.shrink(n);
                } else if (pass == 1 && in.isEmpty()) {
                    int n = Math.min(max, rest.getCount());
                    c.setItem(i, rest.split(n));
                }
            }
        }
        return rest;
    }

    /** (tests, commands) the owner's Leverkisten in order. */
    public static List<GlobalPos> kisten(MinecraftServer s, UUID eigenaar) {
        return new ArrayList<>(KoppelingData.get(s).kisten(eigenaar));
    }
}
