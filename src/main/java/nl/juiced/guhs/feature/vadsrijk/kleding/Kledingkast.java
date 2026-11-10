package nl.juiced.guhs.feature.vadsrijk.kleding;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * T-KLEDING: the Kledingkast, server side. Account-wide (see {@link KledingData}). What you can pick:
 * <ul>
 *   <li><b>transmog</b> per armour piece: any look (equipment asset) you ever had in your bag or on you, or "verborgen" (hide that
 *   piece). Only drawn in the Vadsrijk (it dresses Vadsrijk gear; at home your armour is your armour);</li>
 *   <li><b>back</b>: wings ({@link Cosmetica.Soort#VLEUGEL}) OR a cosmetic cape ({@link Cosmetica.Soort#KAAP}), drawn over the stat
 *   cape of t-items; wings show everywhere, their trick only works in the Vadsrijk ({@link Zweven});</li>
 *   <li><b>head accessory</b>, <b>dye</b> (tints wings + accessory), <b>name colour</b>, the <b>title</b> (the existing titles
 *   feature), and the three effects ({@link Cosmetica.Soort#PLOF}, {@link Cosmetica.Soort#SPOOR}, {@link Cosmetica.Soort#AURA}).</li>
 * </ul>
 * Opening: the key (KledingClient, default O), the tailors in every hub (an extra dialogue choice), the collection tab, or
 * {@code /guhs vadsrijk kledingkast}.
 */
public final class Kledingkast {
    public static final String VERBORGEN = "verborgen";
    public static final List<EquipmentSlot> HARNAS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private Kledingkast() {
    }

    // ---- unlocks -------------------------------------------------------------------------------------------------------------------

    public static boolean heeft(ServerPlayer p, String id) {
        return KledingData.deel(p, "heeft").getBooleanOr(id, false);
    }

    public static List<String> alleHeeft(ServerPlayer p) {
        return new ArrayList<>(KledingData.deel(p, "heeft").keySet());
    }

    /**
     * Unlocks a cosmetic for this account. With {@code melding}: a chat line + chime (+ a title card for wings). Returns false when it
     * was already there or doesn't exist. Wings the first time: put on at once (and the gliding tip).
     */
    public static boolean ontgrendel(ServerPlayer p, String id, boolean melding) {
        Cosmetica.Ding d = Cosmetica.van(id);
        if (d == null || heeft(p, id)) {
            return false;
        }
        KledingData.deel(p, "heeft").putBoolean(id, true);
        boolean eersteVleugel = d.soort() == Cosmetica.Soort.VLEUGEL && keuze(p, "rug").isEmpty();
        if (eersteVleugel) {
            KledingData.deel(p, "keuze").putString("rug", id);
        }
        KledingData.bewaard(p);
        if (melding) {
            Component naam = Component.translatable(d.naamKey()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            p.sendSystemMessage(Component.translatable("vadsrijk.kleding.nieuw", naam, Component.translatable("vadsrijk.kleding.soort." + d.soort().id))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
            if (d.soort() == Cosmetica.Soort.VLEUGEL && Vadsrijk.in(p) && !RijkScenes.bezig(p)) {
                RijkScenes.titel(p, Component.translatable(d.naamKey()).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.translatable("vadsrijk.kleding.vleugel.onder." + (d == Cosmetica.VAHOEGVEERTJES ? "eerste" : "kracht"),
                                Component.translatable("vadsrijk.kleding.kracht." + d.kracht().name().toLowerCase(java.util.Locale.ROOT))),
                        80, 0xFF8CC8);
            }
        }
        if (eersteVleugel) {
            stuurUiterlijk(p);
        }
        Zweven.stuurStaat(p);
        return true;
    }

    /** Takes a cosmetic away again (dev). */
    public static void vergeet(ServerPlayer p, String id) {
        KledingData.deel(p, "heeft").remove(id);
        CompoundTag k = KledingData.deel(p, "keuze");
        for (String s : List.copyOf(k.keySet())) {
            if (k.getStringOr(s, "").equals(id)) {
                k.remove(s);
            }
        }
        KledingData.bewaard(p);
        stuurUiterlijk(p);
        Zweven.stuurStaat(p);
    }

    /** Every cosmetic with source "geheim:&lt;id&gt;" (for t-puzzels and the secrets here). True when anything was new. */
    public static boolean geheim(ServerPlayer p, String geheimId) {
        boolean nieuw = false;
        for (Cosmetica.Ding d : Cosmetica.alle()) {
            if (d.bron().equals("geheim:" + geheimId)) {
                nieuw |= ontgrendel(p, d.id(), true);
            }
        }
        return nieuw;
    }

    /** Gives every cosmetic with this exact source (hooks: "vahoeg", "vahoegkill", "verhaal:vahoegnir"). */
    public static boolean geefBron(ServerPlayer p, String bron) {
        boolean nieuw = false;
        for (Cosmetica.Ding d : Cosmetica.alle()) {
            if (d.bron().equals(bron)) {
                nieuw |= ontgrendel(p, d.id(), true);
            }
        }
        return nieuw;
    }

    /** Has this account unlocked gliding (the Vahoegveertjes)? */
    public static boolean zweven(ServerPlayer p) {
        return heeft(p, Cosmetica.VAHOEGVEERTJES.id());
    }

    public static int kills(ServerPlayer p) {
        return KledingData.kast(p).getIntOr("kills", 0);
    }

    static void telKill(ServerPlayer p) {
        KledingData.kast(p).putInt("kills", kills(p) + 1);
        KledingData.bewaard(p);
    }

    // ---- choices -------------------------------------------------------------------------------------------------------------------

    /** A choice ("rug", "hoofd", "verf", "plof", "spoor", "aura", "naamkleur", "tm.head"...), "" when nothing. */
    public static String keuze(ServerPlayer p, String sleutel) {
        return KledingData.deel(p, "keuze").getStringOr(sleutel, "");
    }

    /**
     * Picks a cosmetic of a kind ({@code id} "" = take it off). Only unlocked ones; wings and capes share the back. True when changed.
     */
    public static boolean kies(ServerPlayer p, Cosmetica.Soort soort, String id) {
        if (!id.isEmpty()) {
            Cosmetica.Ding d = Cosmetica.van(id);
            if (d == null || d.soort() != soort || !heeft(p, id)) {
                return false;
            }
        }
        CompoundTag k = KledingData.deel(p, "keuze");
        if (id.isEmpty()) {
            if (soort.keuze().equals("rug")) {
                Cosmetica.Ding nu = Cosmetica.van(k.getStringOr("rug", ""));
                if (nu != null && nu.soort() != soort) {
                    return false;   // (taking off "capes" while wearing wings: nothing to do)
                }
            }
            k.remove(soort.keuze());
        } else {
            k.putString(soort.keuze(), id);
        }
        veranderd(p, soort == Cosmetica.Soort.NAAMKLEUR);
        return true;
    }

    /** Transmog: a look (an item id whose look you collected), {@link #VERBORGEN}, or "" (your own piece). */
    public static boolean kiesLook(ServerPlayer p, EquipmentSlot slot, String item) {
        if (!HARNAS.contains(slot)) {
            return false;
        }
        CompoundTag k = KledingData.deel(p, "keuze");
        String sleutel = "tm." + slot.getName();
        if (item.isEmpty()) {
            k.remove(sleutel);
        } else if (item.equals(VERBORGEN)) {
            k.putString(sleutel, VERBORGEN);
        } else {
            if (!looks(p, slot).contains(item)) {
                return false;
            }
            k.putString(sleutel, item);
        }
        veranderd(p, false);
        return true;
    }

    private static void veranderd(ServerPlayer p, boolean naam) {
        KledingData.bewaard(p);
        stuurUiterlijk(p);
        Zweven.stuurStaat(p);
        if (naam) {
            p.refreshDisplayName();
            p.refreshTabListName();
        }
    }

    // ---- transmog looks ------------------------------------------------------------------------------------------------------------

    /** The look of an armour item: "slot|asset", or null when it isn't armour with a look. */
    @Nullable
    public static String lookSleutel(ItemStack s) {
        if (s.isEmpty()) {
            return null;
        }
        Equippable e = s.get(DataComponents.EQUIPPABLE);
        if (e == null || e.assetId().isEmpty() || !HARNAS.contains(e.slot())) {
            return null;
        }
        return e.slot().getName() + "|" + e.assetId().get().identifier();
    }

    /** Remembers the looks of every armour piece on/in this player (the check pass; and on loot). Returns how many were new. */
    public static int verzamel(ServerPlayer p) {
        CompoundTag looks = KledingData.deel(p, "looks");
        int nieuw = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            nieuw += onthoud(looks, inv.getItem(i));
        }
        for (EquipmentSlot s : HARNAS) {
            nieuw += onthoud(looks, p.getItemBySlot(s));
        }
        if (nieuw > 0) {
            KledingData.bewaard(p);
        }
        return nieuw;
    }

    static int onthoud(CompoundTag looks, ItemStack s) {
        String k = lookSleutel(s);
        if (k == null || looks.contains(k) || !vadsrijkHarnas(s)) {   // Y-COSMETICA: only Vadsrijk armour becomes a look
            return 0;
        }
        looks.putString(k, BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
        return 1;
    }

    /** Y-COSMETICA: Vadsrijk armour ({@code guhs:vr_*}); survival armour never feeds the Vadsrijk transmog. */
    static boolean vadsrijkHarnas(ItemStack s) {
        Identifier id = BuiltInRegistries.ITEM.getKey(s.getItem());
        return id.getNamespace().equals("guhs") && id.getPath().startsWith("vr_");
    }

    /** The item ids of the looks this account collected for a slot. */
    public static List<String> looks(ServerPlayer p, EquipmentSlot slot) {
        List<String> l = new ArrayList<>();
        CompoundTag looks = KledingData.deel(p, "looks");
        for (String k : looks.keySet()) {
            if (k.startsWith(slot.getName() + "|")) {
                l.add(looks.getStringOr(k, ""));
            }
        }
        l.sort(String::compareTo);
        return l;
    }

    /** Dev: every Vadsrijk armour look (every registered guhs item with a guhs equipment asset). */
    public static int alleLooks(ServerPlayer p) {
        CompoundTag looks = KledingData.deel(p, "looks");
        int n = 0;
        for (Item it : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(it);
            if (id.getNamespace().equals("guhs") && id.getPath().startsWith("vr_")) {
                n += onthoud(looks, new ItemStack(it));
            }
        }
        KledingData.bewaard(p);
        return n;
    }

    // ---- what others see ---------------------------------------------------------------------------------------------------------

    /** The look of this player for every client: the choices that are drawn (no unlock lists). */
    public static CompoundTag uiterlijk(ServerPlayer p) {
        CompoundTag k = KledingData.deel(p, "keuze");
        CompoundTag u = new CompoundTag();
        for (String s : List.of("rug", "hoofd", "plof", "spoor", "aura")) {
            String v = k.getStringOr(s, "");
            if (!v.isEmpty() && heeft(p, v)) {
                u.putString(s, v);
            }
        }
        for (EquipmentSlot s : HARNAS) {
            String v = k.getStringOr("tm." + s.getName(), "");
            if (!v.isEmpty()) {
                u.putString("tm." + s.getName(), v);
            }
        }
        Cosmetica.Ding verf = Cosmetica.van(k.getStringOr("verf", ""));
        if (verf != null && heeft(p, verf.id())) {
            u.putInt("verf", verf.kleur());
        }
        int nk = naamkleur(p);
        if (nk >= 0) {
            u.putInt("naamkleur", nk);
        }
        return u;
    }

    /** The chosen name colour (RGB), or -1. */
    public static int naamkleur(ServerPlayer p) {
        Cosmetica.Ding d = Cosmetica.van(keuze(p, "naamkleur"));
        return d != null && heeft(p, d.id()) ? d.kleur() : -1;
    }

    /** Tells every online player (incl. this one) how this player looks now. */
    public static void stuurUiterlijk(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        if (s == null) {
            return;
        }
        KledingPayloads.Uiterlijk u = new KledingPayloads.Uiterlijk(p.getUUID(), uiterlijk(p));
        for (ServerPlayer ander : s.getPlayerList().getPlayers()) {
            KledingPayloads.stuur(ander, u);
        }
    }

    /** A newcomer learns how everybody looks (and everybody learns the newcomer). */
    static void login(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        if (s == null) {
            return;
        }
        for (ServerPlayer ander : s.getPlayerList().getPlayers()) {
            if (ander != p) {
                KledingPayloads.stuur(p, new KledingPayloads.Uiterlijk(ander.getUUID(), uiterlijk(ander)));
            }
        }
        stuurUiterlijk(p);
        Zweven.stuurStaat(p);
    }

    // ---- the screen ----------------------------------------------------------------------------------------------------------------

    /** Opens (or refreshes) the wardrobe screen. {@code via}: an NPC id when opened at a tailor (the secret counts those). */
    public static void open(ServerPlayer p, @Nullable String via) {
        open(p, via, Vadsrijk.in(p));
    }

    /**
     * Y-COSMETICA: {@code binnen} = in the Vadsrijk. Only there does opening also check the sources and collect armour looks; at home
     * (the Garderobe) it only shows what the Vadsrijk already gave you: unlocks never come from survival.
     */
    static void open(ServerPlayer p, @Nullable String via, boolean binnen) {
        if (via != null && !via.isEmpty()) {
            CompoundTag b = KledingData.deel(p, "bezocht");
            if (!b.getBooleanOr(via, false)) {
                b.putBoolean(via, true);
                KledingData.bewaard(p);
                if (KledingRegistry.KLEERMAKERS.stream().allMatch(n -> b.getBooleanOr(n, false))) {
                    geheim(p, "kleermakers");
                }
            }
        }
        if (binnen) {   // Y-COSMETICA
            verzamel(p);
            Bronnen.check(p);
        }
        KledingPayloads.stuur(p, new KledingPayloads.Scherm(schermData(p)));
    }

    public static CompoundTag schermData(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        ListTag heeft = new ListTag();
        for (String s : alleHeeft(p)) {
            heeft.add(StringTag.valueOf(s));
        }
        t.put("heeft", heeft);
        t.put("keuze", KledingData.deel(p, "keuze").copy());
        CompoundTag looks = new CompoundTag();
        for (EquipmentSlot s : HARNAS) {
            ListTag l = new ListTag();
            for (String it : looks(p, s)) {
                l.add(StringTag.valueOf(it));
            }
            looks.put(s.getName(), l);
        }
        t.put("looks", looks);
        ListTag titels = new ListTag();
        for (Titels.Titel ti : Titels.behaald(p)) {
            CompoundTag x = new CompoundTag();
            x.putString("id", ti.id());
            x.putString("naam", ti.naamSleutel());
            x.putInt("kleur", ti.kleur().getColor() == null ? 0xFFFFFF : ti.kleur().getColor());
            titels.add(x);
        }
        t.put("titels", titels);
        Titels.Titel actief = Titels.actief(p);
        t.putString("titel", actief == null ? "" : actief.id());
        t.putBoolean("zweven", zweven(p));
        t.putInt("kills", kills(p));
        t.putBoolean("binnen", Vadsrijk.in(p));
        t.putInt("poort", Garderobe.poort());   // Y-COSMETICA: locked pieces of closed zones are not listed
        return t;
    }

    /** A click in the screen. */
    static void actie(ServerPlayer p, String actie, String a, String b) {
        boolean ok = switch (actie) {
            case "kies" -> {
                Cosmetica.Soort s = Cosmetica.Soort.van(a);
                yield s != null && kies(p, s, b);
            }
            case "tm" -> {
                EquipmentSlot slot = null;
                for (EquipmentSlot s : HARNAS) {
                    if (s.getName().equals(a)) {
                        slot = s;
                    }
                }
                yield slot != null && kiesLook(p, slot, b);
            }
            case "titel" -> Titels.kies(p, b.isEmpty() ? Titels.GEEN : b);
            case "ververs" -> true;
            default -> false;
        };
        if (ok && !actie.equals("dicht")) {
            KledingPayloads.stuur(p, new KledingPayloads.Scherm(schermData(p)));
        }
    }
}
