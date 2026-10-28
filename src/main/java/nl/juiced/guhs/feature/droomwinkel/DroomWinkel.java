package nl.juiced.guhs.feature.droomwinkel;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.blub.BlubTrekking;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.Tekst;

/**
 * 1.8.0: the Zandguh's droomwinkeltje on the Droomplein. You pay with droomzand (loose, or a droomzandblok = 9; the shop gives
 * change). Made after the safari giftshop: the server decides everything and answers with a fresh list and a message.
 * <ul>
 *   <li><b>data/guhs/dromen/winkel.json</b>: {@code [{"id", "groep", "item", "prijs", "aantal"?, "max"?}]}. An item that does
 *       not exist (yet: another 1.8 branch not merged) is skipped with one warning in the log, so the shop always works;</li>
 *   <li>{@link #aanbod} (Java): offers with their own delivery, e.g. a wild Blub-knuffel (random dice of a wild Blub).</li>
 * </ul>
 * What a player bought (for the {@code max} offers) is kept in {@link GuhQuests#saved} under {@value #GEKOCHT}.
 */
public final class DroomWinkel {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** The groups, in screen order. */
    public static final List<String> GROEPEN = List.of("kleding", "pyjama", "blubkleding", "meubels", "bedden", "knuffels", "weer");
    static final String GEKOCHT = "guhs_droomwinkel";

    public enum Uitkomst { OK, ONBEKEND, MAX, TE_DUUR, MISLUKT }

    /** One offer; {@code lever}: (player, offer) -> delivered? (false: nothing is charged). */
    public record Aanbod(String id, String groep, Supplier<ItemStack> icoon, Component naam, Component uitleg, int prijs, int max,
                         BiFunction<ServerPlayer, Aanbod, Boolean> lever) {
    }

    private static final Map<String, Aanbod> JAVA = new LinkedHashMap<>();
    private static volatile List<Aanbod> json;
    private static final Set<String> GEWAARSCHUWD = new HashSet<>();

    private DroomWinkel() {
    }

    /** A Java offer (replaces one with the same id). */
    public static void aanbod(Aanbod a) {
        JAVA.put(a.id(), a);
    }

    static void registreerVast() {
        // a Blub-knuffel with the dice of a wild Blub (always a surprise which one you get)
        aanbod(new Aanbod("blub_knuffel_wild", "knuffels", () -> knuffel(null),
                Component.translatable("gui.guhs.droomwinkel.blub_knuffel"), Component.translatable("gui.guhs.droomwinkel.blub_knuffel.uitleg"),
                12, 0, (p, a) -> geef(p, knuffel(p.getRandom()))));
    }

    /** A Blub-knuffel item: with random wild dice, or (random null: the icon) a plain blue one. */
    static ItemStack knuffel(@Nullable RandomSource random) {
        Item it = item("guhs:blub_knuffel");
        if (it == null) {
            return ItemStack.EMPTY;
        }
        ItemStack s = new ItemStack(it);
        BlubTrekking.Waarden w = random == null ? new BlubTrekking.Waarden(BlubTrekking.METER_MIDDEN, BlubTrekking.Soort.BLAUW, 0, 1f)
                : BlubTrekking.trek(random);
        CompoundTag t = new CompoundTag();
        t.putInt("Meter", w.meter());
        t.putString("BlubSoort", w.soort().id());
        t.putInt("Tint", w.tint());
        t.putInt("Grootte", BlubTrekking.procent(w.grootte()));
        nl.juiced.guhs.feature.blub.knuffel.BlubKnuffelBlockEntity.schrijf(s, t);
        return s;
    }

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
            try (InputStream in = DroomWinkel.class.getResourceAsStream("/data/guhs/dromen/winkel.json")) {
                if (in != null) {
                    for (JsonElement e : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray()) {
                        JsonObject o = e.getAsJsonObject();
                        String itemId = o.get("item").getAsString();
                        int aantal = o.has("aantal") ? o.get("aantal").getAsInt() : 1;
                        j.add(new Aanbod(o.get("id").getAsString(), o.get("groep").getAsString(),
                                () -> {
                                    Item it = item(itemId);
                                    return it == null ? ItemStack.EMPTY : new ItemStack(it, aantal);
                                },
                                Component.translatable(o.has("naam") ? o.get("naam").getAsString() : "item." + itemId.replace(':', '.')),
                                o.has("uitleg") ? Component.translatable(o.get("uitleg").getAsString()) : Component.empty(),
                                o.get("prijs").getAsInt(), o.has("max") ? o.get("max").getAsInt() : 0,
                                (p, a) -> {
                                    Item it = item(itemId);
                                    return it != null && geef(p, new ItemStack(it, aantal));
                                }));
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Droomwinkel: data/guhs/dromen/winkel.json could not be read", ex);
            }
            json = j;
        }
        return j;
    }

    /**
     * The new 1.8 beds come from another branch: everything in the item tag {@code guhs:droomwinkel/bedden} (optional entries,
     * so a bed that does not exist yet is simply not in it) is on sale for {@value #BED_PRIJS} droomzand.
     */
    public static final TagKey<Item> BEDDEN = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("guhs", "droomwinkel/bedden"));
    static final int BED_PRIJS = 16;

    private static List<Aanbod> bedden() {
        List<Aanbod> uit = new ArrayList<>();
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(BEDDEN)) {
            Item it = holder.value();
            String id = BuiltInRegistries.ITEM.getKey(it).getPath();
            uit.add(new Aanbod("bed_" + id, "bedden", () -> new ItemStack(it), it.getName(new ItemStack(it)),
                    Component.translatable("gui.guhs.droomwinkel.bed.uitleg"), BED_PRIJS, 0, (p, a) -> geef(p, new ItemStack(it))));
        }
        return uit;
    }

    /** Everything on sale now, in group order (offers whose item does not exist are left out, with one warning each). */
    public static List<Aanbod> alle() {
        List<Aanbod> uit = new ArrayList<>();
        List<Aanbod> bedden = bedden();
        for (String g : GROEPEN) {
            for (Aanbod a : json()) {
                if (a.groep().equals(g) && bestaat(a)) {
                    uit.add(a);
                }
            }
            for (Aanbod a : JAVA.values()) {
                if (a.groep().equals(g) && bestaat(a)) {
                    uit.add(a);
                }
            }
            for (Aanbod a : bedden) {
                if (a.groep().equals(g)) {
                    uit.add(a);
                }
            }
        }
        return uit;
    }

    private static boolean bestaat(Aanbod a) {
        if (!a.icoon().get().isEmpty()) {
            return true;
        }
        synchronized (GEWAARSCHUWD) {
            if (GEWAARSCHUWD.add(a.id())) {
                LOGGER.warn("Droomwinkel: offer '{}' skipped, its item does not exist (yet)", a.id());
            }
        }
        return false;
    }

    @Nullable
    public static Aanbod van(String id) {
        for (Aanbod a : alle()) {
            if (a.id().equals(id)) {
                return a;
            }
        }
        return null;
    }

    // --- droomzand ------------------------------------------------------------------------------------------------------------

    /** All droomzand the player carries (a droomzandblok counts 9). */
    public static int saldo(Player p) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(DroomblokkenFeature.DROOMZAND.get())) {
                n += s.getCount();
            } else if (s.is(DroomblokkenFeature.DROOMZANDBLOK.get().asItem())) {
                n += s.getCount() * 9;
            }
        }
        return n;
    }

    /** Takes this much droomzand: loose sand first, then blocks (the change comes back as loose sand). False: too little. */
    static boolean betaal(ServerPlayer p, int prijs) {
        if (saldo(p) < prijs) {
            return false;
        }
        int nog = prijs;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && nog > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(DroomblokkenFeature.DROOMZAND.get())) {
                int neem = Math.min(nog, s.getCount());
                s.shrink(neem);
                nog -= neem;
            }
        }
        for (int i = 0; i < inv.getContainerSize() && nog > 0; i++) {
            ItemStack s = inv.getItem(i);
            while (nog > 0 && !s.isEmpty() && s.is(DroomblokkenFeature.DROOMZANDBLOK.get().asItem())) {
                s.shrink(1);
                int wissel = 9 - Math.min(9, nog);
                nog -= Math.min(9, nog);
                if (wissel > 0) {
                    Minigames.give(p, new ItemStack(DroomblokkenFeature.DROOMZAND.get(), wissel));
                }
            }
        }
        inv.setChanged();
        return true;
    }

    // --- buying ---------------------------------------------------------------------------------------------------------------

    public static int gekocht(Player p, String id) {
        return GuhQuests.saved(p).getCompoundOrEmpty(GEKOCHT).getIntOr(id, 0);
    }

    public static Uitkomst kan(Player p, @Nullable Aanbod a) {
        if (a == null) {
            return Uitkomst.ONBEKEND;
        }
        if (a.max() > 0 && gekocht(p, a.id()) >= a.max()) {
            return Uitkomst.MAX;
        }
        if (saldo(p) < a.prijs()) {
            return Uitkomst.TE_DUUR;
        }
        return Uitkomst.OK;
    }

    public static Uitkomst koop(ServerPlayer p, String id) {
        Aanbod a = van(id);
        Uitkomst u = kan(p, a);
        if (u != Uitkomst.OK) {
            return u;
        }
        if (!betaal(p, a.prijs())) {
            return Uitkomst.TE_DUUR;
        }
        if (!a.lever().apply(p, a)) {
            Minigames.give(p, new ItemStack(DroomblokkenFeature.DROOMZAND.get(), a.prijs()));   // money back
            return Uitkomst.MISLUKT;
        }
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag g = saved.getCompoundOrEmpty(GEKOCHT);
        g.putInt(a.id(), g.getIntOr(a.id(), 0) + 1);
        saved.put(GEKOCHT, g);
        GuhAdvancements.grant(p, "droomwinkel_gekocht");
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.3f);
        return Uitkomst.OK;
    }

    static boolean geef(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        Minigames.give(p, s);
        return true;
    }

    // --- the screen -----------------------------------------------------------------------------------------------------------

    public static void open(ServerPlayer p) {
        ModNetworking.sendTo(p, new DroomWinkelPayloads.Open(stand(p, null)));
    }

    static CompoundTag stand(ServerPlayer p, @Nullable Component melding) {
        CompoundTag t = new CompoundTag();
        t.putInt("Saldo", saldo(p));
        if (melding != null) {
            Tekst.put(t, "Melding", melding);
        }
        ListTag lijst = new ListTag();
        for (Aanbod a : alle()) {
            CompoundTag e = new CompoundTag();
            e.putString("Id", a.id());
            e.putString("Groep", a.groep());
            Tekst.put(e, "Naam", a.naam());
            Tekst.put(e, "Uitleg", a.uitleg());
            e.put("Icoon", Nbt.saveStack(p.registryAccess(), a.icoon().get()));
            e.putInt("Prijs", a.prijs());
            e.putInt("Gekocht", gekocht(p, a.id()));
            e.putInt("Max", a.max());
            e.putInt("Kan", kan(p, a).ordinal());
            lijst.add(e);
        }
        t.put("Aanbod", lijst);
        return t;
    }

    static void opKoop(ServerPlayer p, String id) {
        Aanbod a = van(id);
        Uitkomst u = koop(p, id);
        Component melding = u == Uitkomst.OK
                ? Component.translatable("gui.guhs.droomwinkel.gekocht", a.naam()).withStyle(ChatFormatting.LIGHT_PURPLE)
                : Component.translatable("gui.guhs.droomwinkel.nee." + u.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GOLD);
        ModNetworking.sendTo(p, new DroomWinkelPayloads.Open(stand(p, melding)));
    }

    /** (Tests) forget the read json (so a test sees the shop as the game does). */
    static void vergeetJson() {
        json = null;
    }

    /** (Tests) the paper icon a test offer uses. */
    static ItemStack papier() {
        return new ItemStack(Items.PAPER);
    }
}
