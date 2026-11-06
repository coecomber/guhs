package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Recepten.Recept;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * U-AMBACHT2: the Opdrachtenbord, player-to-player crafting orders (VOORSTEL_PROFESSIONS "spelersbestellingen", deferred by
 * t-ambacht). A player asks for N of a recipe's result (a dish, a potion, a Maatwerk piece or a boss core) and puts up a reward in
 * Vadsmunten; any OTHER player who knows the recipe hands the items in and gets the reward (+ {@link Knutselwinkel#OPDRACHT}
 * Knutselpenningen); the asker picks the items up at a board guh or in the Ambachten book (also when they were offline).
 * <p><b>Dupe-safe</b> (the Markt rule): every action is one call on the server thread; the reward is taken from the asker WHEN the
 * order is posted (escrow) and only moves once; items leave the crafter's inventory before the order changes state and are kept as
 * saved tags in this SavedData ({@code guhs:vr_ambacht_opdrachten}) until picked up; a withdrawn or expired order refunds exactly
 * its escrow. Meesterwerk is bound and post recipes go home, so those can't be asked for.
 */
public final class Opdrachten extends SavedData {
    public static final SavedDataType<Opdrachten> TYPE = GuhSavedData.tagType("vr_ambacht_opdrachten", Opdrachten::new, Opdrachten::load, Opdrachten::save);
    public static final int MAX_OPEN = 5, MAX_TONEN = 40;
    /** Minimum reward per item (brons) and the maximum per order. */
    public static final long MIN_PER_STUK = 25, MAX_BELONING = 10_000_000L;
    /** An open order expires after this long (real time): its escrow goes back to the asker. */
    public static final long VERLOOP_MS = 7L * 24 * 3600 * 1000;

    /** One order. {@code spullen}: the delivered items (saved tags) once delivered. */
    static final class Opdracht {
        long id;
        UUID vrager;
        String vragerNaam = "";
        String recept = "";
        int aantal;
        long beloning;
        long sinds;
        boolean geleverd;
        String makerNaam = "";
        ListTag spullen = new ListTag();
    }

    final List<Opdracht> lijst = new ArrayList<>();
    /** Refunds of expired orders, paid when the asker next opens the board. */
    final Map<UUID, Long> tegoed = new HashMap<>();
    long volgende = 1;

    public static Opdrachten get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private static Opdrachten van(ServerPlayer p) {
        return get(p.level().getServer());
    }

    // ---- what can be asked for -------------------------------------------------------------------------------------------------

    /** Recipes whose result may be asked for (dishes, potions, Maatwerk, boss cores). */
    public static boolean vraagbaar(@Nullable Recept r) {
        return r != null && switch (r.soort()) {
            case "eten", "drank", "gear", "kern" -> true;
            default -> false;
        };
    }

    public static List<Recept> vraagbaar() {
        List<Recept> l = new ArrayList<>();
        for (Recept r : Recepten.ALLE.values()) {
            if (vraagbaar(r)) {
                l.add(r);
            }
        }
        return l;
    }

    /** How many may be asked at once (gear one, cores a few, food/potions a stack). */
    public static int maxAantal(Recept r) {
        return switch (r.soort()) {
            case "gear" -> 1;
            case "kern" -> 5;
            default -> 64;
        };
    }

    /** The items that fulfil a recipe's order (Maatwerk: that kind and tier, never Meesterwerk). */
    static Predicate<ItemStack> past(Recept r) {
        String doel = r.doel();
        return switch (r.soort()) {
            case "eten" -> s -> s.getItem() instanceof Eten.EtenItem e && e.gerecht.id().equals(doel);
            case "drank" -> s -> s.getItem() instanceof Brouwsels.DrankItem d && d.drank.id().equals(doel);
            case "kern" -> s -> Materialen.KERN.containsKey(doel) && s.is(Materialen.KERN.get(doel).get());
            case "gear" -> s -> {
                Maatwerk.Soort so = Maatwerk.SOORTEN.get(doel);
                Maatwerk.Info i = Maatwerk.info(s);
                Uitrusting u = Rollen.van(s);
                return so != null && i != null && !i.meesterwerk() && u != null && u.type().equals(so.basisId(r.tier()));
            };
            default -> s -> false;
        };
    }

    // ---- actions (server thread) -------------------------------------------------------------------------------------------------

    /** Posts an order; the reward is taken now (escrow). Returns a message key. */
    public static String plaats(ServerPlayer p, String receptId, int aantal, long beloning) {
        Recept r = Recepten.van(receptId);
        if (!vraagbaar(r)) {
            return "vadsrijk.ambacht.opdracht.onbekend";
        }
        Opdrachten o = van(p);
        o.verloop(p.level().getServer());
        if (o.open(p.getUUID()) >= MAX_OPEN) {
            return "vadsrijk.ambacht.opdracht.te_veel";
        }
        if (aantal < 1 || aantal > maxAantal(r)) {
            return "vadsrijk.ambacht.opdracht.aantal";
        }
        if (beloning < MIN_PER_STUK * aantal || beloning > MAX_BELONING) {
            return "vadsrijk.ambacht.opdracht.beloning";
        }
        if (!Spelers.neemMunten(p, beloning)) {
            return "vadsrijk.ambacht.opdracht.geen_geld";
        }
        Opdracht x = new Opdracht();
        x.id = o.volgende++;
        x.vrager = p.getUUID();
        x.vragerNaam = p.getGameProfile().name();
        x.recept = r.id();
        x.aantal = aantal;
        x.beloning = beloning;
        x.sinds = AmbachtData.nu();
        o.lijst.add(x);
        o.setDirty();
        EcoUtil.geluid(p, SoundEvents.BOOK_PAGE_TURN, 0.8f, 1.2f);
        return "vadsrijk.ambacht.opdracht.geplaatst";
    }

    /** A crafter hands in an order. Returns a message key. */
    public static String lever(ServerPlayer p, long id) {
        Opdrachten o = van(p);
        Opdracht x = o.zoek(id);
        if (x == null || x.geleverd) {
            return "vadsrijk.ambacht.opdracht.weg";
        }
        if (x.vrager.equals(p.getUUID())) {
            return "vadsrijk.ambacht.opdracht.eigen";
        }
        Recept r = Recepten.van(x.recept);
        if (r == null) {
            return "vadsrijk.ambacht.opdracht.weg";
        }
        if (!Werkplaats.kent(p, r)) {
            return "vadsrijk.ambacht.opdracht.kent_niet";
        }
        Predicate<ItemStack> past = past(r);
        if (MateriaalTas.tel(p, past) < x.aantal) {
            return "vadsrijk.ambacht.opdracht.niet_genoeg";
        }
        // 1) the items leave the crafter (counted again: whatever came out is what is kept, never more)
        List<ItemStack> genomen = MateriaalTas.neem(p, past, x.aantal);
        int n = 0;
        ListTag spullen = new ListTag();
        for (ItemStack s : genomen) {
            CompoundTag t = EcoUtil.tag(p.registryAccess(), s);
            if (t != null) {
                spullen.add(t);
                n += s.getCount();
            }
        }
        if (n < x.aantal) {
            // (can't happen in one tick; if it does: give back what was taken, nothing changes)
            for (ItemStack s : genomen) {
                EcoUtil.geefOfBewaar(p, s);
            }
            return "vadsrijk.ambacht.opdracht.niet_genoeg";
        }
        // 2) the order changes state, 3) the escrow goes to the crafter
        x.geleverd = true;
        x.makerNaam = p.getGameProfile().name();
        x.spullen = spullen;
        o.setDirty();
        Spelers.geefMunten(p, x.beloning);
        Knutselwinkel.geef(p, Knutselwinkel.OPDRACHT);
        EcoUtil.geluid(p, SoundEvents.VILLAGER_YES, 0.6f, 1.4f);
        ServerPlayer vrager = p.level().getServer().getPlayerList().getPlayer(x.vrager);
        if (vrager != null) {
            vrager.sendSystemMessage(Component.translatable("vadsrijk.ambacht.opdracht.klaar_melding", x.makerNaam, Component.translatable(r.naamKey()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return "vadsrijk.ambacht.opdracht.geleverd";
    }

    /** The asker withdraws an open order: the escrow comes back. */
    public static String trekIn(ServerPlayer p, long id) {
        Opdrachten o = van(p);
        Opdracht x = o.zoek(id);
        if (x == null || x.geleverd || !x.vrager.equals(p.getUUID())) {
            return "vadsrijk.ambacht.opdracht.weg";
        }
        o.lijst.remove(x);
        o.setDirty();
        Spelers.geefMunten(p, x.beloning);
        return "vadsrijk.ambacht.opdracht.ingetrokken";
    }

    /** Picks up every delivered order of this player (+ refunds of expired ones). */
    public static String haalOp(ServerPlayer p) {
        Opdrachten o = van(p);
        o.verloop(p.level().getServer());
        int n = 0;
        for (Opdracht x : List.copyOf(o.lijst)) {
            if (x.geleverd && x.vrager.equals(p.getUUID())) {
                o.lijst.remove(x);
                o.setDirty();
                for (int i = 0; i < x.spullen.size(); i++) {
                    ItemStack s = EcoUtil.stack(p.registryAccess(), x.spullen.getCompoundOrEmpty(i));
                    if (!s.isEmpty()) {
                        EcoUtil.geefOfBewaar(p, s);
                    }
                }
                n++;
            }
        }
        long terug = o.betaalTegoed(p);
        return n == 0 && terug == 0 ? "vadsrijk.ambacht.opdracht.niks_op_te_halen" : "vadsrijk.ambacht.opdracht.opgehaald";
    }

    /** Dev: an order from a made-up asker ("Testguh Tobias"), so one player can try delivering; its escrow is free. */
    public static String nep(MinecraftServer server, String receptId) {
        Recept r = Recepten.van(receptId);
        if (!vraagbaar(r)) {
            return "Onbekend recept: " + receptId;
        }
        Opdrachten o = get(server);
        Opdracht x = new Opdracht();
        x.id = o.volgende++;
        x.vrager = UUID.nameUUIDFromBytes("guhs:testguh".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        x.vragerNaam = "Testguh Tobias";
        x.recept = r.id();
        x.aantal = Math.min(2, maxAantal(r));
        x.beloning = 500;
        x.sinds = AmbachtData.nu();
        o.lijst.add(x);
        o.setDirty();
        return "Testopdracht #" + x.id + ": " + x.aantal + "x " + r.id();
    }

    // ---- bookkeeping ---------------------------------------------------------------------------------------------------------------

    @Nullable
    Opdracht zoek(long id) {
        for (Opdracht x : lijst) {
            if (x.id == id) {
                return x;
            }
        }
        return null;
    }

    int open(UUID u) {
        int n = 0;
        for (Opdracht x : lijst) {
            if (!x.geleverd && x.vrager.equals(u)) {
                n++;
            }
        }
        return n;
    }

    /** Open orders older than {@link #VERLOOP_MS}: removed, their escrow becomes the asker's tegoed. */
    void verloop(MinecraftServer server) {
        long nu = AmbachtData.nu();
        for (Opdracht x : List.copyOf(lijst)) {
            if (!x.geleverd && nu - x.sinds > VERLOOP_MS) {
                lijst.remove(x);
                tegoed.merge(x.vrager, x.beloning, Long::sum);
                setDirty();
            }
        }
    }

    long betaalTegoed(ServerPlayer p) {
        Long t = tegoed.remove(p.getUUID());
        if (t == null || t <= 0) {
            return 0;
        }
        setDirty();
        Spelers.geefMunten(p, t);
        p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.opdracht.tegoed", Spelers.munten(t)).withStyle(ChatFormatting.YELLOW));
        return t;
    }

    /** The Opdrachten tab's data. */
    static void scherm(ServerPlayer p, CompoundTag d, List<ItemStack> items) {
        Opdrachten o = van(p);
        o.verloop(p.level().getServer());
        o.betaalTegoed(p);
        ListTag l = new ListTag();
        int ophalen = 0;
        List<Opdracht> volgorde = new ArrayList<>();
        for (Opdracht x : o.lijst) {
            if (x.vrager.equals(p.getUUID())) {
                volgorde.add(x);   // your own first (open + delivered)
                if (x.geleverd) {
                    ophalen++;
                }
            }
        }
        for (Opdracht x : o.lijst) {
            if (!x.geleverd && !x.vrager.equals(p.getUUID())) {
                volgorde.add(x);
            }
        }
        for (Opdracht x : volgorde) {
            if (l.size() >= MAX_TONEN) {
                break;
            }
            Recept r = Recepten.van(x.recept);
            if (r == null) {
                continue;
            }
            boolean eigen = x.vrager.equals(p.getUUID());
            CompoundTag e = new CompoundTag();
            e.putLong("id", x.id);
            e.putString("naam", r.naamKey());
            e.putString("ambacht", r.ambacht().id);
            e.putInt("aantal", x.aantal);
            e.putLong("beloning", x.beloning);
            e.putString("vrager", x.vragerNaam);
            e.putString("maker", x.makerNaam);
            e.putBoolean("eigen", eigen);
            e.putBoolean("geleverd", x.geleverd);
            int heeft = eigen ? 0 : MateriaalTas.tel(p, past(r));
            e.putInt("heeft", heeft);
            e.putBoolean("kent", Werkplaats.kent(p, r));
            e.putInt("item", items.size());
            ItemStack icoon = Recepten.toon(r).copy();
            icoon.setCount(1);
            items.add(icoon);
            l.add(e);
        }
        d.put("opdrachten", l);
        d.putInt("ophalen", ophalen);
        d.putInt("mijnOpen", o.open(p.getUUID()));
        d.putInt("maxOpen", MAX_OPEN);
        d.putLong("minPerStuk", MIN_PER_STUK);
        ListTag k = new ListTag();
        for (Recept r : vraagbaar()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", r.id());
            c.putString("naam", r.naamKey());
            c.putInt("max", maxAantal(r));
            c.putInt("level", r.level());
            c.putString("ambacht", r.ambacht().id);
            c.putInt("item", items.size());
            ItemStack icoon = Recepten.toon(r).copy();
            icoon.setCount(1);
            items.add(icoon);
            k.add(c);
        }
        d.put("kiesbaar", k);
    }

    // ---- saving ----------------------------------------------------------------------------------------------------------------------

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("volgende", volgende);
        ListTag l = new ListTag();
        for (Opdracht x : lijst) {
            CompoundTag c = new CompoundTag();
            c.putLong("id", x.id);
            c.store("vrager", UUIDUtil.CODEC, x.vrager);
            c.putString("vragerNaam", x.vragerNaam);
            c.putString("recept", x.recept);
            c.putInt("aantal", x.aantal);
            c.putLong("beloning", x.beloning);
            c.putLong("sinds", x.sinds);
            c.putBoolean("geleverd", x.geleverd);
            c.putString("makerNaam", x.makerNaam);
            c.put("spullen", x.spullen.copy());
            l.add(c);
        }
        t.put("lijst", l);
        CompoundTag g = new CompoundTag();
        tegoed.forEach((u, n) -> g.putLong(u.toString(), n));
        t.put("tegoed", g);
        return t;
    }

    private static Opdrachten load(CompoundTag t) {
        Opdrachten o = new Opdrachten();
        o.volgende = t.getLongOr("volgende", 1L);
        ListTag l = t.getListOrEmpty("lijst");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompoundOrEmpty(i);
            Opdracht x = new Opdracht();
            x.id = c.getLongOr("id", 0L);
            x.vrager = c.read("vrager", UUIDUtil.CODEC).orElse(null);
            if (x.vrager == null) {
                continue;
            }
            x.vragerNaam = c.getStringOr("vragerNaam", "");
            x.recept = c.getStringOr("recept", "");
            x.aantal = c.getIntOr("aantal", 1);
            x.beloning = c.getLongOr("beloning", 0L);
            x.sinds = c.getLongOr("sinds", 0L);
            x.geleverd = c.getBooleanOr("geleverd", false);
            x.makerNaam = c.getStringOr("makerNaam", "");
            x.spullen = c.getListOrEmpty("spullen").copy();
            o.lijst.add(x);
            o.volgende = Math.max(o.volgende, x.id + 1);
        }
        CompoundTag g = t.getCompoundOrEmpty("tegoed");
        for (String k : g.keySet()) {
            try {
                o.tegoed.put(UUID.fromString(k), g.getLongOr(k, 0L));
            } catch (IllegalArgumentException e) {
                // (ignored)
            }
        }
        return o;
    }
}
