package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Recepten.Ingredient;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Recepten.Recept;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.economie.Post;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;

/**
 * Making things at a workshop (server side; the screen is {@code client.WerkplaatsScherm}). All checks first, then take the
 * ingredients and give the result in one server-thread call (the Markt rule: nothing can get lost or doubled).
 * <ul>
 *   <li>Vadsigheid = 30 + 2 per level above the recipe + 15 per average ingredient quality step + 12 per minigame step (0-100);
 *   "Maak 10x" skips the minigame and always gives {@link #MASSA} %;</li>
 *   <li>xp = 0.7 x the ingredient units x the tier's gathering xp (so a maker rises about as fast as the gatherer that feeds them),
 *   +10 % per minigame step;</li>
 *   <li>Maatwerk/Meesterwerk: the chosen stats come with the request; a Meesterwerk also names its property.</li>
 * </ul>
 */
public final class Werkplaats {
    public static final int MASSA = 60, MASSA_AANTAL = 10;

    private Werkplaats() {
    }

    /**
     * U-AMBACHT2 (Hoogvads is the professions capital): T1-T3 recipes work at every workshop; T4 only in Hoogvads' Ambachtsgilde
     * (the z5_leermeester_* teachers) or Lampionhaven's workshop ({@code z4_werkplaats}); T5 only at the Gilde. {@code npc} null =
     * the dev command (anywhere).
     */
    public static boolean magHier(@Nullable String npc, Recept r) {
        if (r.tier() <= 3 || npc == null || npc.startsWith("z5_leermeester") || npc.equals("z5_gildemeester")) {
            return true;
        }
        return r.tier() == 4 && npc.equals("z4_werkplaats");
    }

    public static boolean kent(ServerPlayer p, Recept r) {
        return AmbachtData.level(p, r.ambacht()) >= r.level();
    }

    /** How many times the ingredients are there (crystal cutting: per kind, the best kind). */
    public static int kan(ServerPlayer p, Recept r, @Nullable String kristal) {
        int n = Integer.MAX_VALUE;
        for (Ingredient i : r.in()) {
            n = Math.min(n, MateriaalTas.tel(p, i.past()) / i.aantal());
        }
        if (r.soort().equals("slijp")) {
            int q = Integer.parseInt(r.doel());
            n = Math.min(n, kristal == null ? 0 : MateriaalTas.tel(p, kristalVan(kristal, q)) / 3);
        }
        return n == Integer.MAX_VALUE ? 0 : n;
    }

    static java.util.function.Predicate<ItemStack> kristalVan(String soort, int q) {
        return s -> s.getItem() instanceof Kristallen.KristalItem k && k.soort.equals(soort) && s.getOrDefault(Kristallen.KWALITEIT.get(), 0) == q;
    }

    /**
     * Makes {@code aantal} (1, or {@link #MASSA_AANTAL} without minigame) of a recipe. {@code keuzes}: the chosen stats (gear),
     * {@code eigenschap}: the Meesterwerk property, {@code kristal}: the crystal kind (cutting). Returns "" or a message key.
     */
    public static String maak(ServerPlayer p, String receptId, List<Stat> keuzes, @Nullable String eigenschap, @Nullable String kristal, int score,
            int aantal) {
        Recept r = Recepten.van(receptId);
        if (r == null) {
            return "vadsrijk.ambacht.werk.onbekend";
        }
        if (!kent(p, r)) {
            return "vadsrijk.ambacht.werk.te_laag";
        }
        boolean massa = aantal > 1;
        if (massa && r.kiest()) {
            return "vadsrijk.ambacht.werk.geen_massa";
        }
        aantal = massa ? MASSA_AANTAL : 1;
        if (r.kiest() && keuzes.size() < r.keuzes()) {
            return "vadsrijk.ambacht.werk.kies";
        }
        Maatwerk.Eigenschap e = null;
        if (r.soort().equals("meester")) {
            e = eigenschap == null ? null : Maatwerk.Eigenschap.van(eigenschap);
            if (e == null) {
                return "vadsrijk.ambacht.werk.eigenschap";
            }
        }
        if (r.soort().equals("slijp") && (kristal == null || Kristallen.SOORTEN.get(kristal) == null)) {
            return "vadsrijk.ambacht.werk.kristal";
        }
        if (kan(p, r, kristal) < aantal) {
            return "vadsrijk.ambacht.werk.te_weinig";
        }
        if (r.soort().equals("post") || r.kiest()) {
            if (!r.soort().equals("post") && EcoUtil.vrij(p.getInventory()) == 0) {
                return "vadsrijk.economie.markt.tas_vol";
            }
        }
        // take everything
        int qSom = 0, qN = 0;
        for (Ingredient i : r.in()) {
            for (ItemStack s : MateriaalTas.neem(p, i.past(), i.aantal() * aantal)) {
                qSom += Materialen.kwaliteit(s) * s.getCount();
                qN += s.getCount();
            }
        }
        if (r.soort().equals("slijp")) {
            MateriaalTas.neem(p, kristalVan(kristal, Integer.parseInt(r.doel())), 3 * aantal);
        }
        int lv = AmbachtData.level(p, r.ambacht());
        int q = Math.max(0, Math.min(2, score));
        double gemQ = qN == 0 ? 0 : qSom / (double) qN;
        int vads = massa ? MASSA : (int) Math.max(0, Math.min(100, Math.round(30 + 2.0 * (lv - r.level()) + 15 * gemQ + 12 * q)));
        String maker = p.getGameProfile().name();
        // give
        List<ItemStack> uit = new ArrayList<>();
        switch (r.soort()) {
            case "eten" -> uit.add(gemaakt(new ItemStack(Eten.ITEM.get(r.doel()).get(), r.aantal() * aantal), vads, maker));
            case "drank" -> uit.add(gemaakt(new ItemStack(Brouwsels.ITEM.get(r.doel()).get(), r.aantal() * aantal), vads, maker));
            case "gear", "meester" -> uit.add(Maatwerk.maak(Maatwerk.SOORTEN.get(r.doel()), r.tier(), keuzes, vads, maker, e, p.getRandom()));
            case "slijp" -> uit.add(Kristallen.stack(kristal, Integer.parseInt(r.doel()) + 1, aantal));
            case "kern" -> uit.add(new ItemStack(Materialen.KERN.get(r.doel()).get(), aantal));
            case "post" -> {
                for (int k = 0; k < aantal; k++) {
                    Post.stuur(p, Recepten.POST.get(r.doel()).get());
                }
                p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.werk.post", Component.translatable(r.naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            default -> {
            }
        }
        for (ItemStack s : uit) {
            EcoUtil.geefOfBewaar(p, s);
        }
        double xp = 0.7 * r.eenheden() * AmbachtXp.VERZAMEL[Math.max(1, Math.min(AmbachtXp.VERZAMEL.length - 1, r.tier()))] * aantal * (1 + 0.1 * q);
        if (lv > AmbachtXp.TIER_MAX[r.tier()] + AmbachtXp.TE_HOOG) {
            xp /= 2;
        }
        AmbachtData.geefXp(p, r.ambacht(), Math.round(xp), true);
        AmbachtData.voegToe(p, "gemaakt", r.id());
        Bestelbord.gemaakt(p, r, r.aantal() * aantal);
        p.level().playSound(null, p.blockPosition(), switch (r.ambacht()) {
            case SMEDEN -> SoundEvents.ANVIL_USE;
            case NAAIEN -> SoundEvents.WOOL_PLACE;
            case KOKEN -> SoundEvents.FURNACE_FIRE_CRACKLE;
            default -> SoundEvents.BREWING_STAND_BREW;
        }, SoundSource.PLAYERS, 0.6f, 1.2f);
        return vads >= 100 ? "vadsrijk.ambacht.werk.vadsig" : "vadsrijk.ambacht.werk.klaar";
    }

    private static ItemStack gemaakt(ItemStack s, int vads, String maker) {
        Gemaakt.zet(s, vads, maker);
        return s;
    }
}
