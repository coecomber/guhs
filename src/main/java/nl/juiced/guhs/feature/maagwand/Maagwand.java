package nl.juiced.guhs.feature.maagwand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.8.1: the maagwand. Good guh food is soft locked behind five stomach walls; every player starts on {@link #MIN}
 * (Melkmaagje) and climbs by eating the training food of the next layer. Saved per player in
 * {@code GuhQuests.saved(player)["guhs_maagwand"]}, so it survives death; the client gets a copy through
 * {@link MaagwandPayloads.Sync}.
 * <p>
 * The table holds item ids, not items: food that does not exist (yet) is simply skipped, so every feature can come and go.
 * Food in no table (snacks, kaasknabbels, ordinary fish, most pastry) may always be eaten.
 */
public final class Maagwand {
    public static final int MIN = 1, MAX = 5;
    static final String KEY = "guhs_maagwand", LAAG = "laag", TELLER = "teller";

    /** What it takes to reach a layer, and what that layer opens. */
    public record Stap(int laag, int doel, List<String> trainvoer, List<String> opent) {
    }

    public static final List<Stap> STAPPEN = List.of(
            new Stap(2, 100, List.of("kaas_knabbels", "kaasknabbelsate", "kaasbrok"),
                    List.of("gefrituurde_kaasknabbels", "gebakken_guh_vis", "kaasbolletje", "knabbeltompouce", "moeraskaas",
                            "patatje_kaassaus", "patatje_vads", "patatje_oorlog", "patatje_guhspeciaal")),
            new Stap(3, 64, List.of("gefrituurde_kaasknabbels", "gebakken_guh_vis", "kaasbolletje"),
                    List.of("dubbel_gefrituurde_kaasknabbels", "gegrilde_kaasknabbelsate", "guhbraadworst", "knabbelburger", "kaasfondue",
                            "ring_stoofpotje", "kaaskroket", "frikandel_guhspeciaal")),
            new Stap(4, 48, List.of("dubbel_gefrituurde_kaasknabbels", "gegrilde_kaasknabbelsate", "patatje_kaassaus", "patatje_vads",
                    "patatje_oorlog", "patatje_guhspeciaal"),
                    List.of("driedubbel_gefrituurde_kaasknabbels", "goudkaas", "gouden_kaasknabbel", "ringh6_stukje_knabbelring",
                            "bitterguhllen", "kaassoufflguh")),
            new Stap(5, 32, List.of("driedubbel_gefrituurde_kaasknabbels", "kaaskroket", "frikandel_guhspeciaal"),
                    List.of("perfecte_knabbel", "safari_reuzen_omelet", "einde2_zij_spijtsoep", "gouden_smulknabbel", "vetste_hap")));

    /** The layer the client knows (set by {@link MaagwandPayloads.Sync}; read on the client only). */
    public static volatile int klantLaag = MIN, klantTeller = 0;

    private static Map<Item, Integer> nodig;

    private Maagwand() {
    }

    // --- the table ---------------------------------------------------------------------------------------------------

    static Optional<Item> item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(Guhs.MODID, id));
    }

    public static List<Item> items(List<String> ids) {
        List<Item> uit = new ArrayList<>();
        for (String id : ids) {
            item(id).ifPresent(uit::add);
        }
        return uit;
    }

    /** The step that leads to this layer, or null. */
    public static Stap stapNaar(int laag) {
        return laag > MIN && laag <= MAX ? STAPPEN.get(laag - MIN - 1) : null;
    }

    /** The layer this food needs ({@link #MIN}: always allowed). */
    public static int nodigeLaag(Item item) {
        if (nodig == null) {
            Map<Item, Integer> m = new HashMap<>();
            for (Stap s : STAPPEN) {
                for (Item i : items(s.opent())) {
                    m.put(i, s.laag());
                }
            }
            nodig = m;
        }
        return nodig.getOrDefault(item, MIN);
    }

    /** All food still locked at this layer: the next layer's food first. */
    public static List<ItemStack> opSlot(int laag) {
        List<ItemStack> uit = new ArrayList<>();
        for (Stap s : STAPPEN) {
            if (s.laag() > laag) {
                items(s.opent()).forEach(i -> uit.add(new ItemStack(i)));
            }
        }
        return uit;
    }

    /** "Kaas Knabbels of Kaasbrok": the training food of a step (null: none of it exists). */
    public static Component trainvoerNamen(Stap stap) {
        MutableComponent uit = null;
        for (Item i : items(stap.trainvoer())) {
            uit = uit == null ? Component.empty().append(i.getName(new ItemStack(i)))
                    : uit.append(Component.translatable("gui.guhs.maagwand.of")).append(i.getName(new ItemStack(i)));
        }
        return uit;
    }

    // --- per player ---------------------------------------------------------------------------------------------------

    private static CompoundTag tag(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag t = saved.getCompoundOrEmpty(KEY);
        saved.put(KEY, t);
        return t;
    }

    public static int laag(ServerPlayer p) {
        return Math.clamp(tag(p).getIntOr(LAAG, MIN), MIN, MAX);
    }

    public static int teller(ServerPlayer p) {
        return tag(p).getIntOr(TELLER, 0);
    }

    /** May this player eat it? Creative players always may. */
    public static boolean magEten(ServerPlayer p, ItemStack stack) {
        return p.hasInfiniteMaterials() || laag(p) >= nodigeLaag(stack.getItem());
    }

    /** They ate this: does it count for the next layer? */
    public static void telTrainvoer(ServerPlayer p, Item item) {
        int laag = laag(p);
        Stap stap = stapNaar(laag + 1);
        if (stap == null || !items(stap.trainvoer()).contains(item)) {
            return;
        }
        int teller = teller(p) + 1;
        if (teller < stap.doel()) {
            zet(p, laag, teller);
            return;
        }
        zet(p, stap.laag(), 0);
        MaagwandEten.gestegen(p, stap.laag());
    }

    /** Set a layer and count (also for the test command), and tell the client. */
    public static void zet(ServerPlayer p, int laag, int teller) {
        CompoundTag t = tag(p);
        t.putInt(LAAG, Math.clamp(laag, MIN, MAX));
        t.putInt(TELLER, Math.max(0, teller));
        stuur(p);
        for (int l = MIN + 1; l <= laag(p); l++) {
            GuhAdvancements.grant(p, "maagwand_laag" + l);
        }
    }

    public static void stuur(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new MaagwandPayloads.Sync(laag(p), teller(p)));
    }
}
