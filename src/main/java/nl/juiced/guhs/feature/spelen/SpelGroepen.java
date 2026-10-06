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
 * (1.3.1: or in any place of the Superkompas, saved in the same list as "s:" + structure id)
 * and remembers it ({@code GuhQuests.saved(player)["guhs_bezocht"]}, survives death); the client gets the list with the
 * payload {@code guhs:spelgroepen_data} ({@link #sync}: on login, on opening the Guhdex and on every new visit).
 * Lang: gui.guhs.spelgroep.&lt;id&gt; (name), gui.guhs.spelgroep.&lt;id&gt;.waar (where: biome + building + NPC),
 * gui.guhs.spelgroep.tijdperk.&lt;tijdperk id&gt;.
 */
public final class SpelGroepen {
    /** The saved list of visited groups (ids) in {@link GuhQuests#saved}. */
    public static final String KEY = "guhs_bezocht";
    /**
     * (1.3.1) The prefix of a visited Superkompas place in the same saved list and payload: "s:" + structure id (without
     * namespace). A group id never has a colon, so the two kinds cannot collide.
     */
    public static final String STRUCTUUR = "s:";
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

    /** (1.3.1) Has this player been in this Superkompas place (structure id without namespace)? Server: saved; client: synced. */
    public static boolean structuurBezocht(Player player, String structuur) {
        return bezocht(player, STRUCTUUR + structuur);
    }

    /** (1.3.1) Remembers a visit to a Superkompas place (server); true when it is new. */
    public static boolean bezoekStructuur(ServerPlayer player, String structuur) {
        return bezoek(player, STRUCTUUR + structuur);
    }

    /** The visited ids of this player (server): group ids, and (1.3.1) "s:" + structure id for Superkompas places. */
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
     * (Server, every {@link #CHECK_TICKS} ticks per player) remembers the groups whose structure the player stands in, and
     * (1.3.1) every Superkompas place the player stands in ("s:" + structure id; a group's building counts as its place
     * too). Returns the newly visited ids (for tests). Cheap: nothing happens where no structure is at all.
     */
    public static List<String> kijk(ServerPlayer player) {
        List<String> nieuw = new ArrayList<>();
        if (!(player.level() instanceof ServerLevel level) || !level.structureManager().hasAnyStructureAt(player.blockPosition())) {
            return nieuw;
        }
        List<String> al = lijst(player);
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (Groep g : alle()) {
            if (g.structuur() != null && !al.contains(g.id()) && staatIn(level, structures, player, g.structuur())) {
                nieuw.add(g.id());
            }
        }
        for (String id : KOMPAS_PLEKKEN.get()) {
            if (!al.contains(STRUCTUUR + id) && staatIn(level, structures, player, id)) {
                nieuw.add(STRUCTUUR + id);
            }
        }
        // a group's building that is no Superkompas place (none today) still counts as found
        for (String id : List.copyOf(nieuw)) {
            Groep g = van(id);
            if (g != null && !al.contains(STRUCTUUR + g.structuur()) && !nieuw.contains(STRUCTUUR + g.structuur())) {
                nieuw.add(STRUCTUUR + g.structuur());
            }
        }
        if (!nieuw.isEmpty()) {
            ListTag list = GuhQuests.saved(player).getListOrEmpty(KEY);
            for (String id : nieuw) {
                list.add(StringTag.valueOf(id));
            }
            GuhQuests.saved(player).put(KEY, list);
            sync(player);
        }
        return nieuw;
    }

    /** Every structure id of the Superkompas, once (a place may be in more than one tab). */
    private static final Supplier<List<String>> KOMPAS_PLEKKEN = com.google.common.base.Suppliers.memoize(
            () -> nl.juiced.guhs.item.SuperkompasItem.CATEGORIES.stream().flatMap(c -> c.structures().stream()).distinct().toList())::get;

    /** Stands the player in a piece of this structure? (A structure this world doesn't know: no.) */
    private static boolean staatIn(ServerLevel level, net.minecraft.core.Registry<Structure> structures, ServerPlayer player, String id) {
        Structure structure = structures.getValue(Guhs.id(id));
        return structure != null && level.structureManager().getStructureWithPieceAt(player.blockPosition(), structure).isValid();
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

        /** (1.3.1) Has the player been in this Superkompas place (structure id without namespace)? */
        public static boolean structuurBezocht(String structuur) {
            return BEZOCHT.contains(STRUCTUUR + structuur);
        }

        private Client() {
        }
    }

    private SpelGroepen() {
    }
}
