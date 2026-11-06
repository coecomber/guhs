package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.economie.Post;

/**
 * U-AMBACHT2: the Knutselpenningen and their shop (VOORSTEL_PROFESSIONS "Knutselpenningen + ambachtswinkel", deferred by t-ambacht).
 * Penningen are earned with orders ONLY: every Bestelbord order ({@code 1 + tier}), the weekly Ambachtskist ({@link #WEEKKIST}), a
 * player crafting order you fulfil ({@link #OPDRACHT}) and a story step ({@link #VERHAAL}). They are not tradeable (a number in
 * {@code AmbachtData "penningen"}), so they can't be farmed through the market. The shop sells what is otherwise only luck:
 * kernsplinters, the rare gathering materials, boss cores, the Ambachtskist, a bit of Uitgerust, and decoration for home (A2).
 * A purchase is one server-thread call: check, take the penningen, give (nothing can get lost or doubled).
 */
public final class Knutselwinkel {
    public static final int WEEKKIST = 10, OPDRACHT = 3, VERHAAL = 5;
    /** Uitgerust bought per "Uitrustthee" (s). */
    public static final int THEE = 1800;

    /** One thing for sale: id, price in penningen, what it gives ({@code soort}: tas = materials, item = the inventory, post = home, thee). */
    public record Waar(String id, int prijs, String soort, Supplier<ItemStack> wat) {
    }

    public static final Map<String, Waar> WAREN = new LinkedHashMap<>();

    static {
        w("kernsplinter", 6, "item", () -> new ItemStack(Materialen.KERNSPLINTER.get()));
        for (String r : List.of("glinstergruis", "oerbast", "gouden_klaver", "parelschelp")) {
            w(r, 8, "tas", () -> Materialen.stack(r, 0, 1));
        }
        w("koninginnengelei", 10, "tas", () -> Materialen.stack("koninginnengelei", 0, 1));
        w("thee", 12, "thee", () -> new ItemStack(Items.CLOCK));
        int[] kernPrijs = {25, 30, 35, 45, 55};
        for (int i = 0; i < Materialen.KERNEN.size(); i++) {
            String k = Materialen.KERNEN.get(i);
            w(k, kernPrijs[Math.min(i, kernPrijs.length - 1)], "item", () -> new ItemStack(Materialen.KERN.get(k).get()));
        }
        w("ambachtskist", 40, "item", () -> new ItemStack(Materialen.AMBACHTSKIST.get()));
        w("vaandel", 15, "post", () -> Recepten.POST.get("vaandel").get());
        w("vlieger", 20, "post", () -> Recepten.POST.get("vlieger").get());
        w("klodderknuffel", 25, "post", () -> Recepten.POST.get("klodderknuffel").get());
    }

    private Knutselwinkel() {
    }

    private static void w(String id, int prijs, String soort, Supplier<ItemStack> wat) {
        WAREN.put(id, new Waar(id, prijs, soort, wat));
    }

    public static int penningen(ServerPlayer p) {
        return AmbachtData.tag(p).getIntOr("penningen", 0);
    }

    static void zet(ServerPlayer p, int n) {
        AmbachtData.tag(p).putInt("penningen", Math.max(0, n));
    }

    /** Earned penningen (shown over the hotbar). */
    public static void geef(ServerPlayer p, int n) {
        if (n <= 0) {
            return;
        }
        zet(p, penningen(p) + n);
        p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.penning.erbij", n, penningen(p)).withStyle(ChatFormatting.YELLOW));
    }

    /** Buys one. Returns the message key for the screen. */
    public static String koop(ServerPlayer p, String id) {
        Waar w = WAREN.get(id);
        if (w == null) {
            return "vadsrijk.ambacht.winkel.onbekend";
        }
        int heeft = penningen(p);
        if (heeft < w.prijs()) {
            return "vadsrijk.ambacht.winkel.te_weinig";
        }
        if (w.soort().equals("thee") && AmbachtData.uitgerust(p) + THEE > AmbachtXp.UITGERUST_MAX) {
            return "vadsrijk.ambacht.winkel.uitgerust_vol";
        }
        if (w.soort().equals("item") && EcoUtil.vrij(p.getInventory()) == 0) {
            return "vadsrijk.economie.markt.tas_vol";
        }
        zet(p, heeft - w.prijs());
        switch (w.soort()) {
            case "thee" -> AmbachtData.zetUitgerust(p, AmbachtData.uitgerust(p) + THEE);
            case "tas" -> MateriaalTas.geef(p, w.wat().get());
            case "post" -> Post.stuur(p, w.wat().get());
            default -> EcoUtil.geefOfBewaar(p, w.wat().get());
        }
        EcoUtil.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);
        return w.soort().equals("post") ? "vadsrijk.ambacht.winkel.post" : "vadsrijk.ambacht.winkel.gekocht";
    }

    /** The shop tab's data. */
    static void scherm(ServerPlayer p, CompoundTag d, List<ItemStack> items) {
        ListTag l = new ListTag();
        int heeft = penningen(p);
        for (Waar w : WAREN.values()) {
            CompoundTag e = new CompoundTag();
            ItemStack s = w.wat().get();
            e.putString("id", w.id());
            e.putString("naam", w.soort().equals("thee") ? "vadsrijk.ambacht.winkel.thee" : s.getItem().getDescriptionId());
            e.putString("soort", w.soort());
            e.putInt("prijs", w.prijs());
            e.putBoolean("kan", heeft >= w.prijs());
            e.putInt("item", items.size());
            items.add(s);
            l.add(e);
        }
        d.put("winkel", l);
    }

    @Nullable
    static Waar van(String id) {
        return WAREN.get(id);
    }

    /** (for the gametests) */
    static List<String> ids() {
        return new ArrayList<>(WAREN.keySet());
    }
}
