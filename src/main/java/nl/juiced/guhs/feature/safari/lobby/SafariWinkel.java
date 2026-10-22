package nl.juiced.guhs.feature.safari.lobby;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.Tekst;

/**
 * The giftshop of Safaripark Vadse Bergen: everything is paid with {@link SafariMunten}. The stock is data driven, so the
 * slices that make the things only have to register them:
 * <ul>
 *   <li><b>data/guhs/safari/winkel.json</b>: {@code [{"id", "groep", "item", "prijs", "aantal"?, "max"?}]}: an item by its
 *       registry id; an entry whose item does not exist (yet) is simply not shown (S6 fills in the clothes, safari blocks,
 *       posters/paintings and the safarivoer);</li>
 *   <li>{@link #aanbod(String)} (Java): any offer with its own delivery, e.g. {@link #borden} (S5 sets the board item);</li>
 *   <li>the verblijfsborden: one per species in your Safari-dex, priced by the species' {@code "bord"} class in soorten.json
 *       (gewoon 100 / middel 175 / zeldzaam 300) and only while the 7-day cooldown of {@link SafariSpeler} allows it.</li>
 * </ul>
 * The groups, in screen order: kleding, borden, blokken, kunst, voer, blauwdrukken.
 */
public final class SafariWinkel {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final List<String> GROEPEN = List.of("kleding", "borden", "blokken", "kunst", "voer", "speelgoed", "blauwdrukken");
    public static final Map<String, Integer> BORD_PRIJS = Map.of("gewoon", 100, "middel", 175, "zeldzaam", 300);

    public enum Uitkomst { OK, ONBEKEND, NIET_HIER, MAX, EIS, TE_DUUR, MISLUKT }

    /** One offer. {@code lever}: (player or null in tests, offer) -> delivered? (false: nothing is charged). */
    public record Aanbod(String id, String groep, Supplier<ItemStack> icoon, Component naam, Component uitleg, int prijs, int max,
                         BiFunction<MinecraftServer, UUID, Component> eis, BiFunction<ServerPlayer, Aanbod, Boolean> lever) {
    }

    private static final Map<String, Aanbod> JAVA = new LinkedHashMap<>();
    private static volatile List<Aanbod> json;
    private static final String GEKOCHT = "winkel:";

    /**
     * (S5) the verblijfsbord item of a species (null: there is no board item yet, the species is shown as "binnenkort").
     * The shop calls {@link SafariSpeler#bordGekocht} itself after a successful delivery.
     */
    public static volatile Function<String, ItemStack> borden = soort -> {
        Item i = item("guhs:verblijfsbord_" + soort);
        if (i == null) {
            i = item("guhs:verblijfsbord");
        }
        return i == null ? ItemStack.EMPTY : new ItemStack(i);
    };

    // --- registering ---------------------------------------------------------------------------------------------------------

    public static Bouwer aanbod(String id) {
        return new Bouwer(id);
    }

    public static final class Bouwer {
        private final String id;
        private String groep = "kleding";
        private Supplier<ItemStack> icoon = () -> new ItemStack(Items.PAPER);
        private Component naam, uitleg = Component.empty();
        private int prijs, max;
        private BiFunction<MinecraftServer, UUID, Component> eis = (s, u) -> null;
        private BiFunction<ServerPlayer, Aanbod, Boolean> lever = (p, a) -> false;

        private Bouwer(String id) {
            this.id = id;
            this.naam = Component.literal(id);
        }

        public Bouwer groep(String g) {
            this.groep = g;
            return this;
        }

        public Bouwer icoon(Supplier<ItemStack> i) {
            this.icoon = i;
            return this;
        }

        public Bouwer naam(Component n) {
            this.naam = n;
            return this;
        }

        public Bouwer uitleg(Component u) {
            this.uitleg = u;
            return this;
        }

        public Bouwer prijs(int p) {
            this.prijs = p;
            return this;
        }

        /** 0 = unlimited. */
        public Bouwer max(int m) {
            this.max = m;
            return this;
        }

        /** Why it can not be bought now (null = it can). */
        public Bouwer eis(BiFunction<MinecraftServer, UUID, Component> e) {
            this.eis = e;
            return this;
        }

        public Bouwer lever(BiFunction<ServerPlayer, Aanbod, Boolean> l) {
            this.lever = l;
            return this;
        }

        public Aanbod registreer() {
            Aanbod a = new Aanbod(id, groep, icoon, naam, uitleg, prijs, max, eis, lever);
            JAVA.put(id, a);
            return a;
        }
    }

    /** The fixed offers of the lobby itself. */
    static void registreerVast() {
        // (the park blueprints: deel 2 F9, GereedschapFeature)
        aanbod("voer_komkommer").groep("voer").icoon(() -> new ItemStack(LobbyItems.KOMKOMMER.get()))
                .naam(Component.translatable("item.guhs.safari_komkommer")).uitleg(Component.translatable("gui.guhs.safari.winkel.aaihoekvoer"))
                .prijs(2).lever((p, a) -> geef(p, new ItemStack(LobbyItems.KOMKOMMER.get(), 4))).registreer();
        aanbod("voer_paprika").groep("voer").icoon(() -> new ItemStack(LobbyItems.PAPRIKA.get()))
                .naam(Component.translatable("item.guhs.safari_paprika")).uitleg(Component.translatable("gui.guhs.safari.winkel.aaihoekvoer"))
                .prijs(2).lever((p, a) -> geef(p, new ItemStack(LobbyItems.PAPRIKA.get(), 4))).registreer();
    }

    // --- the stock -----------------------------------------------------------------------------------------------------------

    @Nullable
    static Item item(String id) {
        Identifier rl = Identifier.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
            return null;
        }
        return BuiltInRegistries.ITEM.getValue(rl);
    }

    private static List<Aanbod> json() {
        List<Aanbod> j = json;
        if (j == null) {
            j = new ArrayList<>();
            try (InputStream in = SafariWinkel.class.getResourceAsStream("/data/guhs/safari/winkel.json")) {
                if (in != null) {
                    for (JsonElement e : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray()) {
                        JsonObject o = e.getAsJsonObject();
                        String itemId = o.get("item").getAsString();
                        int aantal = o.has("aantal") ? o.get("aantal").getAsInt() : 1;
                        String groep = o.get("groep").getAsString();
                        j.add(new Aanbod(o.get("id").getAsString(), groep,
                                () -> {
                                    Item it = item(itemId);
                                    return it == null ? ItemStack.EMPTY : new ItemStack(it, aantal);
                                },
                                Component.translatable(o.has("naam") ? o.get("naam").getAsString() : "item." + itemId.replace(':', '.')),
                                o.has("uitleg") ? Component.translatable(o.get("uitleg").getAsString()) : Component.empty(),
                                o.get("prijs").getAsInt(), o.has("max") ? o.get("max").getAsInt() : 0,
                                eis(o.has("eis") ? o.getAsJsonObject("eis") : null),
                                (p, a) -> {
                                    Item it = item(itemId);
                                    return it != null && geef(p, new ItemStack(it, aantal));
                                }));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Safari giftshop: could not read winkel.json", e);
            }
            json = j;
        }
        return j;
    }

    /**
     * G1 (design 1.8 / 2.9: "een deel ervan ontgrendelt via de dex"): an offer's requirement from winkel.json:
     * {@code {"dex": n}} (at least n species photographed) and/or {@code {"zone": "savanne"}} (that zone complete).
     * Null when met (or no requirement), else what is still needed.
     */
    public static BiFunction<MinecraftServer, UUID, Component> eis(@Nullable JsonObject e) {
        if (e == null) {
            return (s, u) -> null;
        }
        int dex = e.has("dex") ? e.get("dex").getAsInt() : 0;
        String zone = e.has("zone") ? e.get("zone").getAsString() : null;
        return (server, speler) -> {
            if (dex > 0) {
                int heeft = (int) nl.juiced.guhs.feature.safari.kern.SafariSpeler.gefotografeerd(server, speler).stream()
                        .filter(nl.juiced.guhs.feature.safari.kern.SafariSpeler::telt).count();
                if (heeft < dex) {
                    return Component.translatable("gui.guhs.safari.winkel.eis.dex", dex, heeft);
                }
            }
            if (zone != null && !nl.juiced.guhs.feature.safari.kern.SafariSpeler.zoneCompleet(server, speler, zone)) {
                return Component.translatable("gui.guhs.safari.winkel.eis.zone", Component.translatable("biome.guhs.safari_" + zone));
            }
            return null;
        };
    }

    private static Map<String, String> bordKlassen;

    /** The board class of every species ("gewoon"/"middel"/"zeldzaam"; null = no board), from soorten.json. */
    static synchronized Map<String, String> bordKlassen() {
        if (bordKlassen == null) {
            Map<String, String> m = new HashMap<>();
            try (InputStream in = SafariWinkel.class.getResourceAsStream("/data/guhs/safari/soorten.json")) {
                if (in != null) {
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                        JsonElement b = e.getValue().getAsJsonObject().get("bord");
                        if (b != null && !b.isJsonNull()) {
                            m.put(e.getKey(), b.getAsString().toLowerCase(Locale.ROOT));
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Safari giftshop: could not read soorten.json", e);
            }
            bordKlassen = m;
        }
        return bordKlassen;
    }

    /** One board offer per species in the player's dex (photographed), in the order of the species list. */
    private static List<Aanbod> borden(MinecraftServer server, UUID speler) {
        List<Aanbod> uit = new ArrayList<>();
        java.util.Set<String> dex = SafariSpeler.gefotografeerd(server, speler);
        for (String soort : ParkData.get().soorten.keySet()) {
            String klas = bordKlassen().get(soort);
            if (klas == null || !dex.contains(soort)) {
                continue;
            }
            int prijs = BORD_PRIJS.getOrDefault(klas, 175);
            uit.add(new Aanbod("bord_" + soort, "borden",
                    () -> {
                        ItemStack s = borden.apply(soort);
                        return s.isEmpty() ? new ItemStack(Items.OAK_SIGN) : s;
                    },
                    Component.translatable("gui.guhs.safari.winkel.bord", Component.translatable("entity.guhs.safari." + soort)),
                    Component.translatable("gui.guhs.safari.winkel.bord.uitleg." + klas),
                    prijs, 0,
                    (s, u) -> {
                        if (borden.apply(soort).isEmpty()) {
                            return Component.translatable("gui.guhs.safari.winkel.binnenkort");
                        }
                        if (!SafariSpeler.kanBordKopen(s, u, soort)) {
                            long ms = SafariSpeler.bordVrijOp(s, u, soort) - nl.juiced.guhs.feature.guhpixel.Klok.nu();
                            long uren = Math.max(1, (ms + 3_599_999L) / 3_600_000L);
                            return uren >= 24 ? Component.translatable("gui.guhs.safari.winkel.bord.wacht_dagen", (uren + 23) / 24)
                                    : Component.translatable("gui.guhs.safari.winkel.bord.wacht_uren", uren);
                        }
                        return null;
                    },
                    (p, a) -> {
                        ItemStack s = borden.apply(soort);
                        if (s.isEmpty()) {
                            return false;
                        }
                        if (p != null) {
                            geef(p, s);
                        }
                        SafariSpeler.bordGekocht(server, speler, soort);
                        return true;
                    }));
        }
        return uit;
    }

    /** Every offer this player sees: by group in screen order; json entries whose item does not exist are left out. */
    public static List<Aanbod> alle(MinecraftServer server, UUID speler) {
        List<Aanbod> alles = new ArrayList<>(JAVA.values());
        for (Aanbod a : json()) {
            if (!a.icoon().get().isEmpty()) {
                alles.add(a);
            }
        }
        alles.addAll(borden(server, speler));
        List<Aanbod> uit = new ArrayList<>();
        for (String g : GROEPEN) {
            alles.stream().filter(a -> a.groep().equals(g)).forEach(uit::add);
        }
        alles.stream().filter(a -> !GROEPEN.contains(a.groep())).forEach(uit::add);
        return uit;
    }

    @Nullable
    public static Aanbod van(MinecraftServer server, UUID speler, String id) {
        for (Aanbod a : alle(server, speler)) {
            if (a.id().equals(id)) {
                return a;
            }
        }
        return null;
    }

    /** How many of this offer the player bought (kept as a quest string "winkel:&lt;id&gt;" of the safari core). */
    public static int gekocht(MinecraftServer server, UUID speler, String id) {
        try {
            String v = SafariSpeler.quest(server, speler, GEKOCHT + id);
            return v == null || v.isEmpty() ? 0 : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Uitkomst kan(MinecraftServer server, UUID speler, @Nullable ServerPlayer p, @Nullable Aanbod a) {
        if (a == null) {
            return Uitkomst.ONBEKEND;
        }
        if (p != null && !Safari.in(p)) {
            return Uitkomst.NIET_HIER;
        }
        if (a.max() > 0 && gekocht(server, speler, a.id()) >= a.max()) {
            return Uitkomst.MAX;
        }
        if (a.eis().apply(server, speler) != null) {
            return Uitkomst.EIS;
        }
        if (!SafariMunten.kan(server, speler, a.prijs())) {
            return Uitkomst.TE_DUUR;
        }
        return Uitkomst.OK;
    }

    /** A buy (server side, everything checked; {@code p} may be null in tests: then nothing goes into pockets). */
    public static Uitkomst koop(MinecraftServer server, UUID speler, @Nullable ServerPlayer p, String id) {
        Aanbod a = van(server, speler, id);
        Uitkomst u = kan(server, speler, p, a);
        if (u != Uitkomst.OK) {
            return u;
        }
        if (!a.lever().apply(p, a)) {
            return Uitkomst.MISLUKT;
        }
        SafariMunten.neem(server, speler, a.prijs());
        SafariSpeler.zetQuest(server, speler, GEKOCHT + id, Integer.toString(gekocht(server, speler, id) + 1));
        if (p != null) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.6f, 1.2f);
        }
        return Uitkomst.OK;
    }

    static boolean geef(@Nullable ServerPlayer p, ItemStack s) {
        if (p != null) {
            Minigames.give(p, s);
        }
        return true;
    }

    // --- the screen ----------------------------------------------------------------------------------------------------------

    public static void open(ServerPlayer p) {
        ModNetworking.sendTo(p, new LobbyPayloads.WinkelOpen(stand(p, null)));
    }

    static CompoundTag stand(ServerPlayer p, @Nullable Component melding) {
        MinecraftServer server = p.level().getServer();
        UUID id = p.getUUID();
        CompoundTag t = new CompoundTag();
        t.putInt("Saldo", SafariMunten.saldo(p));
        if (melding != null) {
            Tekst.put(t, "Melding", melding);
        }
        ListTag lijst = new ListTag();
        for (Aanbod a : alle(server, id)) {
            CompoundTag e = new CompoundTag();
            e.putString("Id", a.id());
            e.putString("Groep", a.groep());
            Tekst.put(e, "Naam", a.naam());
            Tekst.put(e, "Uitleg", a.uitleg());
            e.put("Icoon", Nbt.saveStack(p.registryAccess(), a.icoon().get()));
            e.putInt("Prijs", a.prijs());
            e.putInt("Gekocht", gekocht(server, id, a.id()));
            e.putInt("Max", a.max());
            Uitkomst u = kan(server, id, p, a);
            e.putInt("Kan", u.ordinal());
            Component eis = a.eis().apply(server, id);
            if (eis != null) {
                Tekst.put(e, "Eis", eis);
            }
            lijst.add(e);
        }
        t.put("Aanbod", lijst);
        return t;
    }

    static void opKoop(ServerPlayer p, String id) {
        MinecraftServer server = p.level().getServer();
        Aanbod a = van(server, p.getUUID(), id);
        Uitkomst u = koop(server, p.getUUID(), p, id);
        Component melding = switch (u) {
            case OK -> Component.translatable("gui.guhs.safari.winkel.gekocht", a.naam()).withStyle(ChatFormatting.GREEN);
            case EIS -> {
                Component eis = a == null ? null : a.eis().apply(server, p.getUUID());
                yield (eis == null ? Component.translatable("gui.guhs.safari.winkel.nee.eis") : eis.copy()).withStyle(ChatFormatting.GOLD);
            }
            default -> Component.translatable("gui.guhs.safari.winkel.nee." + u.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GOLD);
        };
        ModNetworking.sendTo(p, new LobbyPayloads.WinkelOpen(stand(p, melding)));
    }

    /** (Tests) removes a Java offer. */
    static void vergeet(String id) {
        JAVA.remove(id);
    }

    private SafariWinkel() {
    }
}
