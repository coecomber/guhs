package nl.juiced.guhs.feature.vadsrijk.zonepoort;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.compat.ExtraVakken;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.ZonePools;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.opslag.Opslag;
import nl.juiced.guhs.feature.vadsrijk.opslag.OpslagData;
import nl.juiced.guhs.feature.vadsrijk.opslag.OpslagRegistry;
import nl.juiced.guhs.feature.vadsrijk.opslag.Vak;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;

/**
 * 1101-balans: ONE-TIME cleanup of out-of-tier Vadsrijk gear (op): {@code /guhs vadsrijk loot opruimen [speler|alle] [--echt]}.
 * A loot-pool bug let quest rewards, dungeon chests and cave elites pick base types of ALL zones, so zone-1 players got zone-2..6
 * gear and named boss items (Gorgonzola's {@code z5baas_moersleutel}). Without {@code --echt} it only lists; with it the items are
 * removed and every removal is logged (INFO).
 * <p><b>Out of tier</b>: {@code ZonePools.zoneVan(type) > hoogste open zone}; zoneVan 0 (crafted / unknown) is never removed.
 * <p><b>Highest open zone</b> ({@link #hoogsteOpen}): the zone gate itself ({@link ZonePoort#open()}, config {@code openZones},
 * {@code /guhs vadsrijk zones open N}) is SERVER-wide; there is no separate per-player gate state. What a player has really
 * unlocked is read from his own story data (the quest slice {@code SpelerData.extra("npc")}, so it also works for offline players
 * from player.dat): zone N counts as reached when the player has started or finished any quest of zone N
 * ({@link ZoneBoeken#zoneVan}) or carries the flag {@code zone<N-1>_klaar} (the airship gates' story checks use the same flags).
 * Zone 1 is always open; the result is never above the server's open zones. Inactive characters (T-KARAKTER,
 * {@code extra.account_karakter.sN.data}) are judged by their OWN quest data.
 * <p><b>Where</b> (online: the live objects; offline: the whole player.dat, written back with {@code --echt} after a one-time
 * backup {@code <uuid>.dat_voor_opruimen}): the live inventory incl. worn armour/offhand, the cursor (menus are closed first), the
 * ender chest, Curios/Accessories slots ({@link ExtraVakken}, online only; offline they are part of player.dat anyway), the stored
 * Vadsrijk inventory ({@code rijk_inventaris}), the stash ({@code gevonden}, where EcoUtil.geefOfBewaar / Inventaris.bewaar put
 * what doesn't fit), the stored home inventory, worn jewellery ({@code sieraden}), every {@code extra} slice (Post, potion belt,
 * inactive characters + their archive...), and the T-OPSLAG attachment (knapzakken, bank, materiaaltas, unreadable leftovers).
 * NOT reached (not per player): market listings, the bond bank, items lying in the world or in placed blocks.
 */
public final class LootOpruimen {
    private static final Logger LOG = LogUtils.getLogger();
    /** At most this many item lines in chat per run (the server log always has all of them). */
    static final int MAX_REGELS = 60;
    static final String NEOFORGE_ATTACHMENTS = "neoforge:attachments";
    static final String KARAKTERS = "account_karakter";

    private LootOpruimen() {
    }

    /** One out-of-tier piece: where, what, and how to remove it. */
    public record Vondst(String plek, String type, Zeldzaamheid zeldzaamheid, int level, int aantal, int itemZone, int open, Runnable weg) {
    }

    /** The scan of one player (or one tag): his highest open zone and the pieces above it. */
    public record Resultaat(int open, List<Vondst> vondsten) {
    }

    // ---- the rule -------------------------------------------------------------------------------------------------------------

    /** Is this piece above zone {@code open}? (zoneVan 0 = crafted / unknown: never) */
    public static boolean teHoog(Uitrusting u, int open) {
        int z = ZonePools.zoneVan(u.type());
        return z > 0 && z > open;
    }

    /** The highest zone this player reached by his story (1..6, NOT capped by the server): see the class comment. */
    public static int spelerZone(@Nullable CompoundTag npc) {
        if (npc == null) {
            return 1;
        }
        int z = 1;
        CompoundTag q = npc.getCompoundOrEmpty("q");
        for (String id : q.keySet()) {
            if (q.getCompoundOrEmpty(id).getIntOr("s", 0) != 0) {
                z = Math.max(z, ZoneBoeken.zoneVan(id));
            }
        }
        for (int n = 1; n < ZonePools.ZONES; n++) {
            if (npc.getBooleanOr("zone" + n + "_klaar", false)) {
                z = Math.max(z, n + 1);
            }
        }
        return Math.max(1, Math.min(ZonePools.ZONES, z));
    }

    /** The highest open zone of a player: his story zone, never above the server's open zones, never below 1. */
    public static int hoogsteOpen(@Nullable CompoundTag npc, int serverOpen) {
        return Math.max(1, Math.min(spelerZone(npc), Math.max(1, serverOpen)));
    }

    // ---- scanning: raw tags (player.dat, SpelerData slices, unreadable leftovers) ------------------------------------------------

    private static String uitrustingSleutel;

    static String uitrustingSleutel() {
        if (uitrustingSleutel == null) {
            Identifier id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(KernRegistry.UITRUSTING.get());
            uitrustingSleutel = id == null ? "guhs:vr_uitrusting" : id.toString();
        }
        return uitrustingSleutel;
    }

    /** The gear rolls of an item tag (ItemStack / ItemStackWithSlot encoding), or null when it is not Vadsrijk gear. */
    @Nullable
    public static Uitrusting gear(CompoundTag c) {
        if (!(c.get("components") instanceof CompoundTag comps)) {
            return null;
        }
        Tag t = comps.get(uitrustingSleutel());
        return t == null ? null : Uitrusting.CODEC.parse(NbtOps.INSTANCE, t).result().orElse(null);
    }

    /**
     * Scans a whole player.dat tag (or any tag holding player data). The open zone comes from the kern attachment's quest slice in
     * it (absent: zone 1), capped by {@code serverOpen}; inactive characters use their own.
     */
    public static Resultaat scanDat(CompoundTag dat, int serverOpen) {
        CompoundTag kern = dat.getCompoundOrEmpty(NEOFORGE_ATTACHMENTS).getCompoundOrEmpty(KernRegistry.SPELER.getId().toString());
        int open = hoogsteOpen(kern.getCompoundOrEmpty("extra").getCompound("npc").orElse(null), serverOpen);
        List<Vondst> uit = new ArrayList<>();
        loop(dat, "", open, serverOpen, uit, null);
        return new Resultaat(open, uit);
    }

    /** Walks any tag: every Vadsrijk gear item in it above {@code open} becomes a finding (removed from its parent by weg). */
    static void loop(Tag tag, String pad, int open, int serverOpen, List<Vondst> uit, @Nullable Runnable weg) {
        if (tag instanceof CompoundTag c) {
            Uitrusting u = gear(c);
            int aantal = c.getIntOr("count", 1);
            if (u == null && c.get("item") instanceof CompoundTag item && c.contains("n")) {   // a materiaaltas entry {item, n}
                u = gear(item);
                aantal = (int) Math.min(Integer.MAX_VALUE, c.getLongOr("n", 1));
            }
            if (u != null) {
                if (weg != null && teHoog(u, open)) {
                    uit.add(vondst(pad, u, aantal, open, weg));
                }
                return;   // (gear holds no other items)
            }
            for (String k : List.copyOf(c.keySet())) {
                Tag kind = c.get(k);
                if (k.equals(KARAKTERS) && kind instanceof CompoundTag lijst) {
                    karakters(lijst, pad, serverOpen, uit);
                    continue;
                }
                loop(kind, bij(pad, naam(k)), open, serverOpen, uit, () -> c.remove(k));
            }
        } else if (tag instanceof ListTag l) {
            for (int i = 0; i < l.size(); i++) {
                Tag el = l.get(i);
                loop(el, bij(pad, plekInLijst(el, i)), open, serverOpen, uit, () -> weg(l, el));
            }
        }
    }

    /** T-KARAKTER: the inactive characters (s0..s2) and the archive, each with its own open zone. */
    static void karakters(CompoundTag lijst, String pad, int serverOpen, List<Vondst> uit) {
        for (String k : List.copyOf(lijst.keySet())) {
            Tag t = lijst.get(k);
            if (k.length() == 2 && k.charAt(0) == 's' && Character.isDigit(k.charAt(1)) && t instanceof CompoundTag s) {
                karakter(s, bij(pad, "karakter " + (k.charAt(1) - '0' + 1)), serverOpen, uit);
            } else if (k.equals("archief") && t instanceof ListTag l) {
                for (int i = 0; i < l.size(); i++) {
                    if (l.get(i) instanceof CompoundTag s) {
                        karakter(s, bij(pad, "karakterarchief #" + (i + 1)), serverOpen, uit);
                    }
                }
            }
        }
    }

    private static void karakter(CompoundTag s, String pad, int serverOpen, List<Vondst> uit) {
        if (!(s.get("data") instanceof CompoundTag data)) {
            return;   // (the active character: its data is the live SpelerData, scanned on its own)
        }
        int open = hoogsteOpen(data.getCompoundOrEmpty("extra").getCompound("npc").orElse(null), serverOpen);
        loop(data, pad + " (open zone " + open + ")", open, serverOpen, uit, null);
    }

    private static void weg(ListTag l, Tag el) {
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i) == el) {
                l.remove(i);
                return;
            }
        }
    }

    private static <T> void wegUit(List<T> l, T el) {
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i) == el) {
                l.remove(i);
                return;
            }
        }
    }

    private static String plekInLijst(Tag el, int i) {
        if (el instanceof CompoundTag c && c.contains("Slot")) {
            return "vak " + c.getIntOr("Slot", i);
        }
        return "#" + (i + 1);
    }

    private static String bij(String pad, String deel) {
        if (deel.isEmpty()) {
            return pad;
        }
        return pad.isEmpty() ? deel : pad + " > " + deel;
    }

    /** A readable name for a key on the way (empty = left out of the path). */
    static String naam(String k) {
        if (k.length() > 3 && k.startsWith("tab") && k.substring(3).chars().allMatch(Character::isDigit)) {
            return "tab " + (Integer.parseInt(k.substring(3)) + 1);
        }
        if (k.equals(KernRegistry.SPELER.getId().toString())) {
            return "";
        }
        if (k.equals(OpslagRegistry.DATA.getId().toString())) {
            return "opslag";
        }
        return switch (k) {
            case NEOFORGE_ATTACHMENTS, "extra", "data" -> "";
            case "Inventory" -> "inventaris";
            case "equipment" -> "aangetrokken";
            case "EnderItems" -> "enderkist";
            case "rijk_inventaris" -> "Vadsrijk-inventaris (opgeborgen)";
            case "gevonden" -> "kist (gevonden)";
            case "opgeborgen" -> "thuis-inventaris (opgeborgen)";
            case "sieraden" -> "sieraden (gedragen)";
            case "knapzakken" -> "knapzak";
            case "tas" -> "materiaaltas";
            case "components" -> "in item";
            default -> k;
        };
    }

    private static Vondst vondst(String pad, Uitrusting u, int aantal, int open, Runnable weg) {
        return new Vondst(pad.isEmpty() ? "?" : pad, u.type(), u.zeldzaamheid(), u.level(), aantal, ZonePools.zoneVan(u.type()), open, weg);
    }

    // ---- scanning: live objects (online players) --------------------------------------------------------------------------------

    /** Every slot of a container. */
    public static void scanContainer(Container c, String plek, int open, List<Vondst> uit) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            Uitrusting u = Rollen.van(s);
            if (u != null && teHoog(u, open)) {
                int slot = i;
                uit.add(vondst(plek + " > vak " + i, u, s.getCount(), open, () -> {
                    c.setItem(slot, ItemStack.EMPTY);
                    c.setChanged();
                }));
            }
        }
    }

    /** A Java list of raw item tags (SpelerData's stored inventories / stash, T-OPSLAG's unreadable leftovers). */
    static void scanTagLijst(List<CompoundTag> lijst, String plek, int open, int serverOpen, List<Vondst> uit) {
        for (int i = 0; i < lijst.size(); i++) {
            CompoundTag t = lijst.get(i);
            loop(t, bij(plek, plekInLijst(t, i)), open, serverOpen, uit, () -> wegUit(lijst, t));
        }
    }

    /** An online player: live inventory, ender chest, accessory slots, SpelerData and the T-OPSLAG attachment. */
    public static Resultaat scanOnline(ServerPlayer p, int serverOpen) {
        SpelerData d = Spelers.data(p);
        int open = hoogsteOpen(d.extra.get("npc"), serverOpen);
        List<Vondst> uit = new ArrayList<>();
        scanContainer(p.getInventory(), d.binnen ? "inventaris (Vadsrijk, live)" : "inventaris (live)", open, uit);
        scanContainer(p.getEnderChestInventory(), "enderkist", open, uit);
        for (ExtraVakken.Bron bron : ExtraVakken.ALLE) {
            List<ExtraVakken.Vak> vakken;
            try {
                vakken = bron.vakken(p);
            } catch (RuntimeException e) {
                continue;
            }
            for (int i = 0; i < vakken.size(); i++) {
                ExtraVakken.Vak v = vakken.get(i);
                ItemStack s = v.get();
                Uitrusting u = s == null ? null : Rollen.van(s);
                if (u != null && teHoog(u, open)) {
                    uit.add(vondst(bron.id() + " > vak " + i, u, s.getCount(), open, () -> v.set(ItemStack.EMPTY)));
                }
            }
        }
        scanTagLijst(d.rijkInventaris, naam("rijk_inventaris"), open, serverOpen, uit);
        scanTagLijst(d.gevonden, naam("gevonden"), open, serverOpen, uit);
        scanTagLijst(d.opgeborgen, naam("opgeborgen"), open, serverOpen, uit);
        for (Map.Entry<String, CompoundTag> e : List.copyOf(d.sieraden.entrySet())) {
            Uitrusting u = gear(e.getValue());
            if (u != null && teHoog(u, open)) {
                String k = e.getKey();
                uit.add(vondst(naam("sieraden") + " > " + k, u, 1, open, () -> {
                    d.sieraden.remove(k);
                    d.sieradenCache.remove(k);
                }));
            }
        }
        for (Map.Entry<String, CompoundTag> e : List.copyOf(d.extra.entrySet())) {
            if (e.getKey().equals(KARAKTERS)) {
                karakters(e.getValue(), "", serverOpen, uit);
            } else {
                loop(e.getValue(), e.getKey(), open, serverOpen, uit, null);
            }
        }
        OpslagData o = Opslag.data(p);
        for (Map.Entry<String, OpslagData.Zak> e : o.knapzakken.entrySet()) {
            List<Vak> tabs = e.getValue().tabs;
            for (int i = 0; i < tabs.size(); i++) {
                scanContainer(tabs.get(i), "opslag > knapzak " + e.getKey() + " > tab " + (i + 1), open, uit);
            }
        }
        for (int i = 0; i < o.bank.size(); i++) {
            scanContainer(o.bank.get(i), "opslag > bank > tab " + (i + 1), open, uit);
        }
        for (OpslagData.Materiaal m : List.copyOf(o.materialen)) {
            Uitrusting u = Rollen.van(m.soort);
            if (u != null && teHoog(u, open)) {
                uit.add(vondst("opslag > materiaaltas", u, (int) Math.min(Integer.MAX_VALUE, m.aantal), open, () -> {
                    wegUit(o.materialen, m);
                    o.tasVersie++;
                }));
            }
        }
        for (Map.Entry<String, List<CompoundTag>> e : o.onleesbaar.entrySet()) {
            scanTagLijst(e.getValue(), "opslag > onleesbaar " + e.getKey(), open, serverOpen, uit);
        }
        return new Resultaat(open, uit);
    }

    /** Removes every finding (each exactly once). Returns how many. */
    public static int verwijder(List<Vondst> vondsten) {
        for (Vondst v : vondsten) {
            v.weg().run();
        }
        return vondsten.size();
    }

    // ---- the command -------------------------------------------------------------------------------------------------------------

    /** Registered from {@link ZonePoortRegistry}. */
    static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> opruimen = Commands.literal("opruimen").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> voer(c.getSource(), "alle", false))
                .then(Commands.literal("--echt").executes(c -> voer(c.getSource(), "alle", true)))
                .then(Commands.argument("speler", StringArgumentType.word())
                        .suggests((c, b) -> {
                            List<String> namen = new ArrayList<>(List.of("alle"));
                            namen.addAll(List.of(c.getSource().getServer().getPlayerNames()));
                            return SharedSuggestionProvider.suggest(namen, b);
                        })
                        .executes(c -> voer(c.getSource(), StringArgumentType.getString(c, "speler"), false))
                        .then(Commands.literal("--echt").executes(c -> voer(c.getSource(), StringArgumentType.getString(c, "speler"), true))));
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("loot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(opruimen)));
    }

    /** One player to clean: online (p) or offline (dat from disk). */
    private record Doel(String naam, UUID id, @Nullable ServerPlayer p, @Nullable CompoundTag dat) {
    }

    private static int voer(CommandSourceStack s, String wie, boolean echt) {
        MinecraftServer server = s.getServer();
        int serverOpen = ZonePoort.open();
        List<Doel> doelen = new ArrayList<>();
        List<String> fouten = new ArrayList<>();
        if (wie.equalsIgnoreCase("alle")) {
            Set<UUID> online = new HashSet<>();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                online.add(p.getUUID());
                doelen.add(new Doel(p.getGameProfile().name(), p.getUUID(), p, null));
            }
            File[] bestanden = spelerMap(server).toFile().listFiles((dir, n) -> n.endsWith(".dat"));
            if (bestanden != null) {
                for (File f : bestanden) {
                    UUID id;
                    try {
                        id = UUID.fromString(f.getName().substring(0, f.getName().length() - 4));
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    if (online.contains(id)) {
                        continue;
                    }
                    String naam = server.services().nameToIdCache().get(id).map(NameAndId::name).orElse(id.toString().substring(0, 8));
                    CompoundTag dat = server.getPlayerList().loadPlayerData(new NameAndId(id, naam)).orElse(null);
                    if (dat == null) {
                        fouten.add(naam + ": spelerbestand onleesbaar, overgeslagen");
                    } else if (heeftVadsrijk(dat)) {
                        doelen.add(new Doel(naam, id, null, dat));
                    }
                }
            }
        } else {
            ServerPlayer p = server.getPlayerList().getPlayerByName(wie);
            if (p != null) {
                doelen.add(new Doel(p.getGameProfile().name(), p.getUUID(), p, null));
            } else {
                NameAndId n = server.services().nameToIdCache().get(wie).orElse(null);
                CompoundTag dat = n == null ? null : server.getPlayerList().loadPlayerData(n).orElse(null);
                if (n == null) {
                    s.sendFailure(Component.literal("Onbekende speler: " + wie + " (niet online en niet in de profielcache)."));
                    return 0;
                }
                if (dat == null) {
                    s.sendFailure(Component.literal(n.name() + " is offline en heeft geen leesbaar spelerbestand: niets te doen."));
                    return 0;
                }
                doelen.add(new Doel(n.name(), n.id(), null, dat));
            }
        }

        s.sendSuccess(() -> Component.literal("Vadsrijk-loot opruimen" + (echt ? " (ECHT: wordt verwijderd)" : " (proefrun: er wordt niets verwijderd)")
                + " - server open t/m zone " + serverOpen + ", " + doelen.size() + " speler(s).").withStyle(ChatFormatting.GOLD), true);
        int regels = 0, totaal = 0, spelersMet = 0;
        for (Doel d : doelen) {
            Resultaat gescand;
            try {
                if (d.p() != null && echt) {
                    Opslag.sluit(d.p());
                    if (d.p().containerMenu != d.p().inventoryMenu) {
                        d.p().closeContainer();
                    }
                    ItemStack cursor = d.p().inventoryMenu.getCarried();   // (back into the inventory, so it is scanned too)
                    if (!cursor.isEmpty()) {
                        d.p().inventoryMenu.setCarried(ItemStack.EMPTY);
                        d.p().getInventory().placeItemBackInInventory(cursor);
                    }
                }
                gescand = d.p() != null ? scanOnline(d.p(), serverOpen) : scanDat(d.dat(), serverOpen);
            } catch (RuntimeException e) {
                LOG.error("Vadsrijk-loot opruimen: scan of {} ({}) failed", d.naam(), d.id(), e);
                fouten.add(d.naam() + ": scan mislukt (" + e + "), zie serverlog");
                continue;
            }
            final Resultaat r = gescand;
            if (r.vondsten().isEmpty()) {
                continue;
            }
            spelersMet++;
            totaal += r.vondsten().size();
            String wat = d.p() != null ? "online" : "offline";
            int open = r.open();
            s.sendSuccess(() -> Component.literal(d.naam() + " (" + wat + ", open t/m zone " + open + "): " + r.vondsten().size() + " item(s)")
                    .withStyle(ChatFormatting.YELLOW), false);
            for (Vondst v : r.vondsten()) {
                LOG.info("Vadsrijk-loot opruimen{}: {} ({}) {}: {} {} lv{} x{} (zone {} > open zone {})", echt ? " REMOVED" : " (dry run)", d.naam(), d.id(),
                        v.plek(), v.type(), v.zeldzaamheid().name().toLowerCase(java.util.Locale.ROOT), v.level(), v.aantal(), v.itemZone(), v.open());
                if (regels++ < MAX_REGELS) {
                    s.sendSuccess(() -> regel(v), false);
                }
            }
            if (echt) {
                verwijder(r.vondsten());
                if (d.p() != null) {
                    d.p().getInventory().setChanged();
                    d.p().inventoryMenu.broadcastChanges();
                    Spelers.sync(d.p());
                } else if (!schrijf(server, d)) {
                    fouten.add(d.naam() + ": offline spelerbestand NIET weggeschreven (zie serverlog); niets verwijderd");
                    totaal -= r.vondsten().size();
                }
            }
        }
        if (regels > MAX_REGELS) {
            int meer = regels - MAX_REGELS;
            s.sendSuccess(() -> Component.literal("... en nog " + meer + " regel(s): zie de serverlog.").withStyle(ChatFormatting.GRAY), false);
        }
        for (String f : fouten) {
            s.sendFailure(Component.literal(f));
        }
        int t = totaal, m = spelersMet;
        s.sendSuccess(() -> Component.literal("Totaal: " + t + " item(s) boven de zone bij " + m + " speler(s)"
                + (echt ? " verwijderd (elk item staat in de serverlog)." : ". Verwijderen: zelfde commando met --echt.")).withStyle(ChatFormatting.GOLD), true);
        if (!echt) {
            s.sendSuccess(() -> Component.literal("Niet bereikt (niet per speler): marktaanbiedingen, de bondsbank, items in de wereld of in geplaatste blokken.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return t;
    }

    private static Component regel(Vondst v) {
        BasisType bt = BasisTypes.van(v.type());
        MutableComponent naam = (bt != null ? Component.translatable(bt.naamKey()) : Component.literal(v.type()))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(v.zeldzaamheid().rgb)));
        return Component.literal("  - " + v.plek() + ": ").withStyle(ChatFormatting.GRAY).append(naam)
                .append(Component.literal(" [" + v.type() + (v.aantal() > 1 ? " x" + v.aantal() : "") + "] zone " + v.itemZone() + " > " + v.open()
                        + ", " + v.zeldzaamheid().name().toLowerCase(java.util.Locale.ROOT) + ", lv " + v.level()).withStyle(ChatFormatting.GRAY));
    }

    static Path spelerMap(MinecraftServer server) {
        return server.getPlayerList().getPlayerIo().getPlayerDir().toPath();
    }

    /** Has this player.dat any Vadsrijk data (kern or storage attachment)? */
    static boolean heeftVadsrijk(CompoundTag dat) {
        CompoundTag a = dat.getCompoundOrEmpty(NEOFORGE_ATTACHMENTS);
        return a.contains(KernRegistry.SPELER.getId().toString()) || a.contains(OpslagRegistry.DATA.getId().toString());
    }

    /** Writes an offline player's edited tag back (first a one-time backup of the original file). */
    private static boolean schrijf(MinecraftServer server, Doel d) {
        if (server.getPlayerList().getPlayer(d.id()) != null) {
            LOG.error("Vadsrijk-loot opruimen: {} logged in during the cleanup, file not written", d.naam());
            return false;
        }
        try {
            Path map = spelerMap(server);
            Path echt = map.resolve(d.id() + ".dat");
            Path backup = map.resolve(d.id() + ".dat_voor_opruimen");
            if (Files.isRegularFile(echt) && !Files.exists(backup)) {
                Files.copy(echt, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
            CompoundTag dat = NbtUtils.addCurrentDataVersion(d.dat());
            Path tmp = Files.createTempFile(map, d.id() + "-", ".dat");
            NbtIo.writeCompressed(dat, tmp);
            Util.safeReplaceFile(echt, tmp, map.resolve(d.id() + ".dat_old"));
            LOG.info("Vadsrijk-loot opruimen: wrote {} ({}), backup {}", d.naam(), echt, backup);
            return true;
        } catch (Exception e) {
            LOG.error("Vadsrijk-loot opruimen: could not write the player file of {} ({})", d.naam(), d.id(), e);
            return false;
        }
    }
}
