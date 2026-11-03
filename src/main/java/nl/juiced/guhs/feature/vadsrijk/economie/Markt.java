package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * The market / veilinghuis at the marktmeester (design 9: "handel: ruilen + markt"). API for the npc slice: {@link #open}.
 * <p>Exploit rules: every change happens in ONE server-thread call that first checks everything (listing still there, buyer has
 * the munten and an empty slot, the item tag reads back), then moves the item and the munten together. An item is in exactly
 * one place at any time: the seller's bag, the listing, or the buyer's bag. Gebonden items and coins can't be listed.
 * A listing fee ({@link #FEE_PROCENT} %, at least {@link #FEE_MIN} brons) is paid when listing and not refunded.
 */
public final class Markt {
    public static final int MAX_PER_SPELER = 12, FEE_PROCENT = 2, FEE_MIN = 5;
    public static final long MAX_PRIJS = 100_000_000L;
    /** Listings sent to a screen (newest first). */
    public static final int MAX_TONEN = 300;

    private Markt() {
    }

    public static long fee(long prijs) {
        return Math.max(FEE_MIN, prijs * FEE_PROCENT / 100);
    }

    // ---- the actions (also called by the gametests) -------------------------------------------------------------------------

    /** Puts the stack in bag slot {@code slot} on the market for {@code prijs} brons. Returns "" or a message key. */
    public static String zetTeKoop(ServerPlayer p, MarktData m, int slot, long prijs) {
        if (slot < 0 || slot >= EcoUtil.TAS) {
            return "vadsrijk.economie.markt.niks";
        }
        ItemStack s = p.getInventory().getItem(slot);
        if (s.isEmpty() || s.getItem() instanceof EcoItems.Munt) {
            return "vadsrijk.economie.markt.niks";
        }
        if (!EcoUtil.verhandelbaar(s)) {
            return "vadsrijk.economie.markt.gebonden";
        }
        if (prijs < 1 || prijs > MAX_PRIJS) {
            return "vadsrijk.economie.markt.prijs";
        }
        if (m.aantalVan(p.getUUID()) >= MAX_PER_SPELER) {
            return "vadsrijk.economie.markt.te_veel";
        }
        CompoundTag tag = EcoUtil.tag(p.registryAccess(), s);
        if (tag == null) {
            return "vadsrijk.economie.markt.niks";
        }
        if (!Spelers.neemMunten(p, fee(prijs))) {
            return "vadsrijk.economie.markt.geen_fee";
        }
        p.getInventory().setItem(slot, ItemStack.EMPTY);
        Uitrusting u = Rollen.van(s);
        BasisType t = u == null ? null : u.basisType();
        m.aanbod.add(new MarktData.Aanbod(m.volgende++, p.getUUID(), p.getGameProfile().name(), tag, prijs,
                t == null ? "" : t.slot().name().toLowerCase(Locale.ROOT), t == null || t.klasse() == null ? "" : t.klasse(),
                u == null ? -1 : u.zeldzaamheid().ordinal(), u == null ? -1 : u.level(), s.getHoverName().getString().toLowerCase(Locale.ROOT),
                p.level().getGameTime()));
        m.setDirty();
        return "";
    }

    /** Buys listing {@code id}. Returns "" or a message key. */
    public static String koop(ServerPlayer p, MarktData m, long id) {
        MarktData.Aanbod a = m.aanbod(id);
        if (a == null) {
            return "vadsrijk.economie.markt.weg";
        }
        if (a.verkoper().equals(p.getUUID())) {
            return "vadsrijk.economie.markt.eigen";
        }
        if (EcoUtil.vrij(p.getInventory()) == 0) {
            return "vadsrijk.economie.markt.tas_vol";
        }
        ItemStack s = EcoUtil.stack(p.registryAccess(), a.item());
        if (s.isEmpty()) {
            return "vadsrijk.economie.markt.kapot";
        }
        if (!Spelers.neemMunten(p, a.prijs())) {
            return "vadsrijk.economie.markt.te_weinig";
        }
        m.aanbod.remove(a);
        EcoUtil.inLegeSlot(p.getInventory(), s);
        m.opbrengst.merge(a.verkoper(), a.prijs(), Long::sum);
        int n = m.verkocht.merge(a.verkoper(), 1, Integer::sum);
        m.setDirty();
        ServerPlayer verkoper = p.level().getServer().getPlayerList().getPlayer(a.verkoper());
        if (verkoper != null) {
            EcoUtil.chat(verkoper, "vadsrijk.economie.markt.verkocht_melding", s.getHoverName().getString(), Spelers.munten(a.prijs()));
            if (n >= Cosmetica.VERKOCHT_VOOR_TITEL) {
                Cosmetica.geefTitel(verkoper, Cosmetica.MARKTKOOPMAN);
            }
        }
        return "";
    }

    /** Takes your own listing back (the fee stays paid). */
    public static String trekIn(ServerPlayer p, MarktData m, long id) {
        MarktData.Aanbod a = m.aanbod(id);
        if (a == null || !a.verkoper().equals(p.getUUID())) {
            return "vadsrijk.economie.markt.weg";
        }
        if (EcoUtil.vrij(p.getInventory()) == 0) {
            return "vadsrijk.economie.markt.tas_vol";
        }
        ItemStack s = EcoUtil.stack(p.registryAccess(), a.item());
        if (s.isEmpty()) {
            return "vadsrijk.economie.markt.kapot";
        }
        m.aanbod.remove(a);
        EcoUtil.inLegeSlot(p.getInventory(), s);
        m.setDirty();
        return "";
    }

    /** Collects the proceeds waiting for this player. Returns the amount. */
    public static long haalOp(ServerPlayer p, MarktData m) {
        Long n = m.opbrengst.remove(p.getUUID());
        if (n == null || n <= 0) {
            return 0;
        }
        m.setDirty();
        Spelers.geefMunten(p, n);
        if (m.verkocht(p.getUUID()) >= Cosmetica.VERKOCHT_VOOR_TITEL) {
            Cosmetica.geefTitel(p, Cosmetica.MARKTKOOPMAN);
        }
        return n;
    }

    // ---- the screen ----------------------------------------------------------------------------------------------------------

    /** API (npc slice): opens the market at the marktmeester. Only in the Vadsrijk. */
    public static void open(ServerPlayer p) {
        if (Vadsrijk.in(p)) {
            stuur(p, "", "");
        }
    }

    private static void stuur(ServerPlayer p, String melding, String arg) {
        MarktData m = MarktData.get(p.level().getServer());
        CompoundTag d = new CompoundTag();
        List<ItemStack> items = new ArrayList<>();
        List<MarktData.Aanbod> lijst = new ArrayList<>(m.alle());
        lijst.sort(Comparator.comparingLong(MarktData.Aanbod::id).reversed());
        ListTag l = new ListTag();
        int n = 0;
        for (MarktData.Aanbod a : lijst) {
            boolean eigen = a.verkoper().equals(p.getUUID());
            if (n >= MAX_TONEN && !eigen) {
                continue;
            }
            ItemStack s = EcoUtil.stack(p.registryAccess(), a.item());
            CompoundTag t = new CompoundTag();
            t.putLong("id", a.id());
            t.putString("naam", a.naam());
            t.putLong("prijs", a.prijs());
            t.putString("slot", a.slot());
            t.putString("klasse", a.klasse());
            t.putInt("zeldzaamheid", a.zeldzaamheid());
            t.putInt("level", a.level());
            t.putString("zoek", a.zoek());
            t.putBoolean("eigen", eigen);
            t.putInt("item", items.size());
            items.add(s);
            l.add(t);
            n++;
        }
        d.put("aanbod", l);
        ListTag tas = new ListTag();
        for (int i = 0; i < EcoUtil.TAS; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && !(s.getItem() instanceof EcoItems.Munt)) {
                CompoundTag t = new CompoundTag();
                t.putInt("slot", i);
                t.putBoolean("gebonden", !EcoUtil.verhandelbaar(s));
                t.putLong("waarde", Waarde.verkoop(s));
                t.putInt("item", items.size());
                items.add(s.copy());
                tas.add(t);
            }
        }
        d.put("tas", tas);
        d.putLong("munten", Spelers.data(p).munten);
        d.putLong("opbrengst", m.opbrengst(p.getUUID()));
        d.putInt("mijn", m.aantalVan(p.getUUID()));
        d.putString("melding", melding);
        d.putString("meldingArg", arg);
        EcoPayloads.open(p, EcoPayloads.MARKT, "", d, items);
    }

    static void doe(ServerPlayer p, String actie, CompoundTag a) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        MarktData m = MarktData.get(p.level().getServer());
        String melding = "";
        String arg = "";
        switch (actie) {
            case "zet" -> {
                long prijs = a.getLongOr("prijs", 0);
                melding = zetTeKoop(p, m, a.getIntOr("slot", -1), prijs);
                if (melding.isEmpty()) {
                    melding = "vadsrijk.economie.markt.gezet";
                    arg = Spelers.munten(fee(prijs));
                    EcoUtil.geluid(p, SoundEvents.UI_TOAST_IN, 0.8f, 1.2f);
                }
            }
            case "koop" -> {
                melding = koop(p, m, a.getLongOr("id", -1));
                if (melding.isEmpty()) {
                    melding = "vadsrijk.economie.markt.gekocht";
                    EcoUtil.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.4f);
                }
            }
            case "trekin" -> {
                melding = trekIn(p, m, a.getLongOr("id", -1));
                if (melding.isEmpty()) {
                    melding = "vadsrijk.economie.markt.ingetrokken";
                }
            }
            case "haalop" -> {
                long n = haalOp(p, m);
                melding = n > 0 ? "vadsrijk.economie.markt.opgehaald" : "vadsrijk.economie.markt.niks_op";
                arg = Spelers.munten(n);
                if (n > 0) {
                    EcoUtil.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.6f);
                }
            }
            default -> {
            }
        }
        stuur(p, melding, arg);
    }

    /** At login: tell the seller that proceeds are waiting. */
    static void meld(ServerPlayer p) {
        long n = MarktData.get(p.level().getServer()).opbrengst(p.getUUID());
        if (n > 0) {
            EcoUtil.chat(p, "vadsrijk.economie.markt.wacht", Spelers.munten(n));
        }
    }
}
