package nl.juiced.guhs.feature.spelen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.Structure;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The groups of the Minigames tab of the Guhdex (2.9): one per minigame building, each in its era ({@link Tijdperk}),
 * with its icon, its structure, its guh character and its Highscores rows (ids of {@code Highscores.GAMES}, in display
 * order). Phase 1 registers every group ({@link SpelenFeature}); the data is static and the same on both sides.
 * <p>
 * Visited: every 40 ticks the server looks whether a player stands in the structure of a group they haven't visited yet
 * and remembers it ({@code GuhQuests.saved(player)["guhs_bezocht"]}, survives death); the client gets the list with the
 * payload {@code guhs:spelgroepen_data} ({@link #sync}: on login, on opening the Guhdex and on every new visit).
 * Lang: gui.guhs.spelgroep.&lt;id&gt; (name), gui.guhs.spelgroep.&lt;id&gt;.waar (where: biome + building + NPC),
 * gui.guhs.spelgroep.tijdperk.&lt;tijdperk id&gt;.
 */
public final class SpelGroepen {
    /** The saved list of visited groups (ids) in {@link GuhQuests#saved}. */
    public static final String KEY = "guhs_bezocht";
    /** How often (ticks) the server looks whether a player is in a group's structure. */
    public static final int CHECK_TICKS = 40;

    /** The eras of the Minigames tab, in tab order. */
    public enum Tijdperk {
        KLASSIEKERS, KNUFFELDAL, GROTE_GUHSPELEN,
        /** 3.0 (Guhverhalen): the games of the stories (the Nomguh sledesprint, surfing and hula on Guhwai'i). */
        VERHALEN;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** gui.guhs.spelgroep.tijdperk.&lt;id&gt;. */
        public Component naam() {
            return Component.translatable("gui.guhs.spelgroep.tijdperk." + id());
        }
    }

    /**
     * A group: id (also the {@link KledingBronnen} source id of its clothes), era, icon, the structure it lives in (id
     * without namespace, guhs:; null: none), its guh character (null: none), and its Highscores rows in display order.
     */
    public record Groep(String id, Tijdperk tijdperk, Supplier<ItemStack> icoon, @Nullable String structuur,
                        @Nullable GuhNpcEntity.Kind npc, List<String> spellen) {
        public Groep {
            spellen = List.copyOf(spellen);
        }

        /** gui.guhs.spelgroep.&lt;id&gt;. */
        public Component naam() {
            return Component.translatable("gui.guhs.spelgroep." + id);
        }

        /** gui.guhs.spelgroep.&lt;id&gt;.waar: where to find it (biome + building + NPC, Dutch). */
        public Component waar() {
            return Component.translatable("gui.guhs.spelgroep." + id + ".waar");
        }
    }

    private static final Map<String, Groep> GROEPEN = new LinkedHashMap<>();

    /** Registers a group (call from your Feature.register; phase 1 registers all of them). The same id again replaces it in place. */
    public static synchronized void groep(Groep groep) {
        GROEPEN.put(groep.id(), groep);
    }

    /** All groups, in registration order (= the order of the tab within each era). */
    public static synchronized List<Groep> alle() {
        return List.copyOf(GROEPEN.values());
    }

    /** The groups of one era, in order. */
    public static List<Groep> van(Tijdperk tijdperk) {
        return alle().stream().filter(g -> g.tijdperk() == tijdperk).toList();
    }

    @Nullable
    public static synchronized Groep van(String id) {
        return GROEPEN.get(id);
    }

    /** The clothes whose {@link KledingBronnen} source is this group, in enum order. */
    public static List<GuhClothes> kleding(String groepId) {
        List<GuhClothes> out = new ArrayList<>();
        for (GuhClothes c : GuhClothes.values()) {
            if (groepId.equals(KledingBronnen.bron(c))) {
                out.add(c);
            }
        }
        return out;
    }

    /** An icon by registry id (guhs:&lt;id&gt;), or the stand-in while that item doesn't exist (yet). */
    public static Supplier<ItemStack> icoon(String id, Item standIn) {
        return () -> {
            Item item = BuiltInRegistries.ITEM.getValue(Guhs.id(id));
            return new ItemStack(item == Items.AIR ? standIn : item);
        };
    }

    // --- visited -----------------------------------------------------------------------------------------------------

    /** Has this player been in the group's building? Server: the saved data; client: the synced list. */
    public static boolean bezocht(Player player, String groepId) {
        if (player.level().isClientSide()) {
            return Client.BEZOCHT.contains(groepId);
        }
        return lijst(player).contains(groepId);
    }

    /** The visited group ids of this player (server). */
    public static List<String> bezocht(Player player) {
        return lijst(player);
    }

    /** Remembers a visit (server); true when it is new (then the client hears it too). */
    public static boolean bezoek(ServerPlayer player, String groepId) {
        List<String> ids = lijst(player);
        if (ids.contains(groepId)) {
            return false;
        }
        ListTag list = GuhQuests.saved(player).getListOrEmpty(KEY);
        list.add(StringTag.valueOf(groepId));
        GuhQuests.saved(player).put(KEY, list);
        sync(player);
        return true;
    }

    private static List<String> lijst(Player player) {
        ListTag list = GuhQuests.saved(player).getListOrEmpty(KEY);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getStringOr(i, ""));
        }
        return out;
    }

    /** Sends the visited list to the player (payload guhs:spelgroepen_data). */
    public static void sync(ServerPlayer player) {
        ModNetworking.sendTo(player, new SpelenPayloads.SpelgroepenData(lijst(player)));
    }

    /**
     * (Server, every {@link #CHECK_TICKS} ticks per player) remembers the groups whose structure the player stands in.
     * Returns the newly visited group ids (for tests).
     */
    public static List<String> kijk(ServerPlayer player) {
        List<String> nieuw = new ArrayList<>();
        if (!(player.level() instanceof ServerLevel level)) {
            return nieuw;
        }
        List<String> al = lijst(player);
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (Groep g : alle()) {
            if (g.structuur() == null || al.contains(g.id())) {
                continue;
            }
            Structure structure = structures.getValue(Guhs.id(g.structuur()));
            if (structure != null && level.structureManager().getStructureWithPieceAt(player.blockPosition(), structure).isValid()) {
                if (bezoek(player, g.id())) {
                    nieuw.add(g.id());
                }
            }
        }
        return nieuw;
    }

    /** The client's copy of the player's visited groups. */
    public static final class Client {
        static final Set<String> BEZOCHT = ConcurrentHashMap.newKeySet();

        public static void set(List<String> ids) {
            BEZOCHT.clear();
            BEZOCHT.addAll(ids);
        }

        public static boolean bezocht(String groepId) {
            return BEZOCHT.contains(groepId);
        }

        private Client() {
        }
    }

    private SpelGroepen() {
    }
}
