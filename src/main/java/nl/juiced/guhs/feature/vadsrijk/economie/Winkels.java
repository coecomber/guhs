package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/**
 * The Vadsmunten shops (design 9). API for the npc slice: {@link #open(ServerPlayer, String)} with one of the ids
 * {@link #DRANKJES}, {@link #UITRUSTING}, {@link #SMIDWAREN}, {@link #COSMETICA}, {@link #ALGEMEEN}.
 * Every shop except the cosmetica one also BUYS loot (value by rarity and level: {@link Waarde}).
 * Cosmetics are never handed out here: they go to the {@link Post} and arrive at home.
 */
public final class Winkels {
    public static final String DRANKJES = "drankjes", UITRUSTING = "uitrusting", SMIDWAREN = "smidwaren", COSMETICA = "cosmetica", ALGEMEEN = "algemeen";
    /** The level tiers of the basic gear. */
    public static final int[] NIVEAUS = {1, 5, 10, 15};

    public enum Soort {
        /** An item into your bag (stackable: buy 1 or 5). */
        ITEM,
        /** A plain (gewoon) piece of gear of the chosen level tier. */
        GEAR,
        /** A cosmetic item, sent by post (arrives at home). */
        POST,
        /** A title. */
        TITEL
    }

    public record Waar(String id, Soort soort, long prijs, Supplier<ItemStack> toon, String extra) {
    }

    public record Winkel(String id, boolean koopt, List<Waar> waren) {
        public String titelKey() {
            return "vadsrijk.economie.winkel." + id;
        }

        @Nullable
        public Waar waar(String id) {
            for (Waar w : waren) {
                if (w.id().equals(id)) {
                    return w;
                }
            }
            return null;
        }
    }

    private static final Map<String, Winkel> WINKELS = new LinkedHashMap<>();

    private Winkels() {
    }

    static void registreer() {
        List<Waar> drank = List.of(item("vadsdrankje", 15, () -> new ItemStack(EcoSpul.VADSDRANKJE.get())),
                item("energiedrankje", 20, () -> new ItemStack(EcoSpul.ENERGIEDRANKJE.get())),
                item("snelpootjesdrankje", 45, () -> new ItemStack(EcoSpul.SNELPOOTJESDRANKJE.get())),
                item("vahoegdrankje", 60, () -> new ItemStack(EcoSpul.VAHOEGDRANKJE.get())));
        List<Waar> smid = List.of(item("verbetersteentje", 50, () -> new ItemStack(EcoSpul.VERBETERSTEENTJE.get())),
                item("pluisdraad", 30, () -> new ItemStack(EcoSpul.PLUISDRAAD.get())),
                item("herrolknabbel", 600, () -> new ItemStack(EcoSpul.HERROLKNABBEL.get())),
                item("rijfluitje", 25, () -> new ItemStack(EcoSpul.RIJFLUITJE.get())));
        List<Waar> gear = new ArrayList<>();
        for (String t : new String[] {"kussenzwaard", "pluisboog", "rozenstaf", "pluishelm", "knuffelharnas", "steelbroek", "plakpantoffels",
                "klodderkapje", "zwamhoed"}) {
            gear.add(new Waar(t, Soort.GEAR, 30, () -> toonGear(t), t));
        }
        List<Waar> cos = new ArrayList<>();
        long[] kledingPrijs = {300, 300, 2500, 500, 200};
        for (int i = 0; i < Cosmetica.KLEDING.size(); i++) {
            GuhClothes c = Cosmetica.KLEDING.get(i);
            cos.add(new Waar(c.id(), Soort.POST, kledingPrijs[i], () -> Cosmetica.kledingItem(c), ""));
        }
        long[] decoPrijs = {800, 800, 500, 200};
        for (int i = 0; i < EcoSpul.DECO.size(); i++) {
            var b = EcoSpul.DECO.get(i);
            cos.add(new Waar(b.getId().getPath(), Soort.POST, decoPrijs[i], () -> new ItemStack(b.get()), ""));
        }
        cos.add(new Waar(Cosmetica.RIDDER, Soort.TITEL, 1000, () -> new ItemStack(EcoSpul.MUNT_ZILVER.get()), Cosmetica.RIDDER));
        cos.add(new Waar(Cosmetica.BARON, Soort.TITEL, 10000, () -> new ItemStack(EcoSpul.MUNT_GOUD.get()), Cosmetica.BARON));
        List<Waar> alg = new ArrayList<>(drank);
        alg.addAll(smid);
        zet(new Winkel(DRANKJES, true, drank));
        zet(new Winkel(SMIDWAREN, true, smid));
        zet(new Winkel(UITRUSTING, true, gear));
        zet(new Winkel(COSMETICA, false, cos));
        zet(new Winkel(ALGEMEEN, true, alg));
    }

    private static void zet(Winkel w) {
        WINKELS.put(w.id(), w);
    }

    private static Waar item(String id, long prijs, Supplier<ItemStack> s) {
        return new Waar(id, Soort.ITEM, prijs, s, "");
    }

    @Nullable
    public static Winkel winkel(String id) {
        return WINKELS.get(id);
    }

    public static java.util.Set<String> ids() {
        return WINKELS.keySet();
    }

    /** The shop price of one of this item (0 = not sold anywhere): selling gives half. */
    public static long koopPrijs(Item i) {
        for (Winkel w : WINKELS.values()) {
            for (Waar x : w.waren()) {
                if (x.soort() == Soort.ITEM && x.toon().get().is(i)) {
                    return x.prijs();
                }
            }
        }
        return 0;
    }

    /** Price of a gear piece at this tier. */
    public static long gearPrijs(Waar w, int level) {
        return w.prijs() + 25L * level;
    }

    private static ItemStack toonGear(String type) {
        BasisType t = BasisTypes.van(type);
        if (t == null) {
            return ItemStack.EMPTY;
        }
        return Rollen.stack(new Uitrusting(t.id(), Zeldzaamheid.GEWOON, 1, List.of(), t.element(), 0, 0, false, false, 0));
    }

    // =====================================================================================================================
    // the screen
    // =====================================================================================================================

    /** API (npc slice): opens this shop for the player. Only in the Vadsrijk. */
    public static void open(ServerPlayer p, String winkelId) {
        Winkel w = WINKELS.get(winkelId);
        if (w == null || !Vadsrijk.in(p)) {
            return;
        }
        stuur(p, w, "", "");
    }

    private static void stuur(ServerPlayer p, Winkel w, String melding, String meldingArg) {
        CompoundTag d = new CompoundTag();
        List<ItemStack> items = new ArrayList<>();
        d.putString("winkel", w.id());
        d.putBoolean("koopt", w.koopt());
        d.putLong("munten", Spelers.data(p).munten);
        d.putString("melding", melding);
        d.putString("meldingArg", meldingArg);
        ListTag waren = new ListTag();
        for (Waar x : w.waren()) {
            CompoundTag t = new CompoundTag();
            t.putString("id", x.id());
            t.putString("soort", x.soort().name());
            t.putLong("prijs", x.prijs());
            t.putBoolean("al", x.soort() == Soort.TITEL && Cosmetica.heeftTitel(p, x.extra()));
            if (x.soort() == Soort.TITEL) {
                t.putString("titel", "gui.guhs.titels.naam." + x.extra());
            }
            t.putInt("item", items.size());
            items.add(x.toon().get());
            waren.add(t);
        }
        d.put("waren", waren);
        if (w.koopt()) {
            ListTag tas = new ListTag();
            for (int i = 0; i < EcoUtil.TAS; i++) {
                ItemStack s = p.getInventory().getItem(i);
                long v = Waarde.verkoop(s);
                if (v > 0) {
                    CompoundTag t = new CompoundTag();
                    t.putInt("slot", i);
                    t.putLong("waarde", v);
                    t.putInt("item", items.size());
                    items.add(s.copy());
                    tas.add(t);
                }
            }
            d.put("tas", tas);
        }
        d.putInt("post", Post.aantal(p));
        EcoPayloads.open(p, EcoPayloads.WINKEL, w.id(), d, items);
    }

    static void doe(ServerPlayer p, String winkelId, String actie, CompoundTag a) {
        Winkel w = WINKELS.get(winkelId);
        if (w == null || !Vadsrijk.in(p)) {
            return;
        }
        String[] uit = switch (actie) {
            case "koop" -> koop(p, w, a.getStringOr("waar", ""), a.getIntOr("aantal", 1), a.getIntOr("level", 1));
            case "verkoop" -> verkoop(p, w, a.getIntOr("slot", -1));
            case "verkoopgrijs" -> verkoopGrijs(p, w);
            default -> new String[] {"", ""};
        };
        stuur(p, w, uit[0], uit[1]);
    }

    /** Buys; returns {message key, argument}. Everything is checked before anything is taken. */
    static String[] koop(ServerPlayer p, Winkel w, String waarId, int aantal, int level) {
        Waar x = w.waar(waarId);
        if (x == null) {
            return new String[] {"", ""};
        }
        aantal = x.soort() == Soort.ITEM ? Math.max(1, Math.min(16, aantal)) : 1;
        long prijs;
        ItemStack krijg;
        switch (x.soort()) {
            case ITEM -> {
                krijg = x.toon().get().copyWithCount(aantal);
                prijs = x.prijs() * aantal;
                if (EcoUtil.vrij(p.getInventory()) == 0 && !past(p, krijg)) {
                    return new String[] {"vadsrijk.economie.winkel.tas_vol", ""};
                }
            }
            case GEAR -> {
                boolean geldig = false;
                for (int n : NIVEAUS) {
                    geldig |= n == level;
                }
                BasisType t = BasisTypes.van(x.extra());
                if (!geldig || t == null) {
                    return new String[] {"", ""};
                }
                if (EcoUtil.vrij(p.getInventory()) == 0) {
                    return new String[] {"vadsrijk.economie.winkel.tas_vol", ""};
                }
                prijs = gearPrijs(x, level);
                krijg = Rollen.stack(Rollen.rol(t, level, Zeldzaamheid.GEWOON, p.getRandom()));
            }
            case POST -> {
                krijg = x.toon().get();
                prijs = x.prijs();
            }
            case TITEL -> {
                if (Cosmetica.heeftTitel(p, x.extra())) {
                    return new String[] {"vadsrijk.economie.winkel.al_titel", ""};
                }
                krijg = ItemStack.EMPTY;
                prijs = x.prijs();
            }
            default -> {
                return new String[] {"", ""};
            }
        }
        if (!Spelers.neemMunten(p, prijs)) {
            EcoUtil.geluid(p, SoundEvents.VILLAGER_NO, 0.6f, 1.4f);
            return new String[] {"vadsrijk.economie.winkel.te_weinig", Spelers.munten(prijs)};
        }
        switch (x.soort()) {
            case POST -> {
                Post.stuur(p, krijg);
                EcoUtil.geluid(p, SoundEvents.UI_TOAST_IN, 0.8f, 1.3f);
                return new String[] {"vadsrijk.economie.winkel.per_post", krijg.getHoverName().getString()};
            }
            case TITEL -> {
                Cosmetica.geefTitel(p, x.extra());
                return new String[] {"vadsrijk.economie.winkel.titel_gekocht", ""};
            }
            default -> {
                EcoUtil.geefOfBewaar(p, krijg);
                EcoUtil.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
                return new String[] {"vadsrijk.economie.winkel.gekocht", Spelers.munten(prijs)};
            }
        }
    }

    private static boolean past(ServerPlayer p, ItemStack s) {
        int nodig = s.getCount();
        for (int i = 0; i < EcoUtil.TAS; i++) {
            ItemStack x = p.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(x, s)) {
                nodig -= x.getMaxStackSize() - x.getCount();
            }
        }
        return nodig <= 0;
    }

    static String[] verkoop(ServerPlayer p, Winkel w, int slot) {
        if (!w.koopt() || slot < 0 || slot >= EcoUtil.TAS) {
            return new String[] {"", ""};
        }
        ItemStack s = p.getInventory().getItem(slot);
        long v = Waarde.verkoop(s);
        if (v <= 0) {
            return new String[] {"", ""};
        }
        p.getInventory().setItem(slot, ItemStack.EMPTY);
        Spelers.geefMunten(p, v);
        EcoUtil.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
        return new String[] {"vadsrijk.economie.winkel.verkocht", Spelers.munten(v)};
    }

    /** Sells every plain (gewoon, grey) piece without stars in the bag at once. */
    static String[] verkoopGrijs(ServerPlayer p, Winkel w) {
        if (!w.koopt()) {
            return new String[] {"", ""};
        }
        long totaal = 0;
        for (int i = 0; i < EcoUtil.TAS; i++) {
            ItemStack s = p.getInventory().getItem(i);
            Uitrusting u = Rollen.van(s);
            if (u != null && u.zeldzaamheid() == Zeldzaamheid.GEWOON && u.sterren() == 0 && !u.verpluisd()) {
                totaal += Waarde.gear(u);
                p.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        if (totaal == 0) {
            return new String[] {"vadsrijk.economie.winkel.geen_grijs", ""};
        }
        Spelers.geefMunten(p, totaal);
        EcoUtil.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
        return new String[] {"vadsrijk.economie.winkel.verkocht", Spelers.munten(totaal)};
    }
}
