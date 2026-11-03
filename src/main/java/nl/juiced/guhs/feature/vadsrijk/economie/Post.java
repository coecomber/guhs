package nl.juiced.guhs.feature.vadsrijk.economie;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * The Vadsrijk post: cosmetics bought inside wait here ({@code SpelerData.extra("economie").post}, saved in the player file together
 * with the inventory) and are handed into the NORMAL inventory once the player is home (outside the dimension, swap done). Never
 * through the inventory swap itself. What doesn't fit stays in the post and comes on a later check.
 */
public final class Post {
    private Post() {
    }

    /** Adds a stack to the post (call from the server thread). */
    public static void stuur(ServerPlayer p, ItemStack s) {
        CompoundTag t = EcoUtil.tag(p.registryAccess(), s);
        if (t == null) {
            return;
        }
        CompoundTag e = Spelers.data(p).extra("economie");
        ListTag l = e.getListOrEmpty("post").copy();
        l.add(t);
        e.put("post", l);
    }

    public static int aantal(ServerPlayer p) {
        return Spelers.data(p).extra("economie").getListOrEmpty("post").size();
    }

    /** Outside the dimension: hands out what fits. Returns how many stacks came. */
    public static int bezorg(ServerPlayer p, boolean meldVol) {
        if (!Cosmetica.thuis(p)) {
            return 0;
        }
        CompoundTag e = Spelers.data(p).extra("economie");
        ListTag l = e.getListOrEmpty("post");
        if (l.isEmpty()) {
            return 0;
        }
        ListTag rest = new ListTag();
        int n = 0;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            ItemStack s = EcoUtil.stack(p.registryAccess(), t);
            if (s.isEmpty()) {
                rest.add(t);                  // (unreadable now: it stays, never thrown away)
                continue;
            }
            p.getInventory().add(s);
            if (s.isEmpty()) {
                n++;
            } else {
                CompoundTag over = EcoUtil.tag(p.registryAccess(), s);
                rest.add(over != null ? over : t);
            }
        }
        e.put("post", rest);
        if (n > 0) {
            EcoUtil.geluid(p, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8f, 1.5f);
            EcoUtil.chat(p, "vadsrijk.economie.post.aangekomen", n);
        }
        if (meldVol && !rest.isEmpty() && n == 0) {
            EcoUtil.zeg(p, "vadsrijk.economie.post.vol", rest.size());
        }
        return n;
    }
}
