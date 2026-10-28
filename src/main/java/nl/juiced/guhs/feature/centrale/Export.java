package nl.juiced.guhs.feature.centrale;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guhvillagers.GuhVillagerHandel;
import nl.juiced.guhs.feature.guhvillagers.GuhVillagerHandel.Ruil;
import nl.juiced.guhs.feature.guhvillagers.GuhVillagersFeature;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Export (1.8.0, vadskracht theme): the guh villagers of the four 1.7.0 professions BUY bulk from the players' factories
 * (blocks of kaasknabbels, baksels, knuffelsteen, coloured wool) and give rare things back (clothes, knuffels,
 * weerpotjes, droomzand, fossils, Blub clothes). Every in-game day a villager swaps its {@link #PER_DAG} export trades for
 * new ones (when a player talks to it), with high amounts, so the factories always have a buyer. A trade whose item does
 * not exist (yet: the fossils of another branch) is simply skipped ({@link GuhVillagerHandel#offer}).
 * <p>
 * The export trades are recognised by what they ask and give ({@link #isExport}), so vanilla's own trades (which may come
 * after them when the villager levels up) are never touched.
 */
public final class Export {
    /** How many export trades a guh villager has per day. */
    public static final int PER_DAG = 2;
    /** How often one export trade can be done per day. */
    public static final int KEER = 4;
    private static final String DAG = "guhs_export_dag";

    private static Ruil r(String wants, int n, @Nullable String wants2, int n2, String gives, int g, int xp) {
        return new Ruil(wants, n, wants2, n2, gives, g, List.of(), KEER, xp);
    }

    /** Per profession: what it buys in bulk, for what. */
    static final Map<String, List<Ruil>> POOL = Map.of(
            "vadsmonteur", List.of(
                    r("guhs:block_of_kaasknabbels", 64, null, 0, "guhs:droomzand", 12, 20),
                    r("guhs:block_of_kaasknabbels", 64, "guhs:block_of_kaasknabbels", 64, "guhs:weerpotje_regenboog", 1, 30),
                    r("guhs:block_of_kaasknabbels", 48, null, 0, "guhs:weerpotje_bellen", 1, 25),
                    r("guhs:block_of_kaasknabbels", 64, "guhs:knuffelsteen", 64, "guhs:fossiel_tguh", 1, 40),
                    r("guhs:block_of_kaasknabbels", 64, "guhs:knuffelsteen", 64, "guhs:fossiel_pteroguh", 1, 40)),
            "mijnwerker", List.of(
                    r("guhs:knuffelsteen", 64, "guhs:knuffelsteen", 64, "guhs:droomzand", 8, 20),
                    r("guhs:gladde_knuffelsteen", 64, null, 0, "guhs:knuffel_wolk", 1, 25),
                    r("guhs:viltsteen", 64, "guhs:knuffelsteen", 64, "guhs:fossiel_mammoet", 1, 40),
                    r("guhs:knuffelsteen_trap", 64, null, 0, "guhs:fossiel_stokstaartje", 1, 40),
                    r("guhs:knuffelsteen", 64, "guhs:block_of_kaasknabbels", 32, "guhs:knuffel_starry", 1, 30)),
            "kristalslijper", List.of(
                    r("minecraft:pink_wool", 64, "minecraft:white_wool", 64, "guhs:knuffel_rainbow", 1, 30),
                    r("minecraft:light_blue_wool", 64, null, 0, "guhs:blubkleding_sjaaltje", 1, 25),
                    r("minecraft:magenta_wool", 64, null, 0, "guhs:blubkleding_bubbelhelm", 1, 25),
                    r("minecraft:lime_wool", 64, "minecraft:yellow_wool", 64, "guhs:knuffel_pluisguh", 1, 30),
                    r("minecraft:purple_wool", 64, null, 0, "guhs:blubkleding_sterrenkroontje", 1, 25)),
            "toverguh", List.of(
                    r("guhs:roze_broodje", 32, null, 0, "guhs:slaapmutsje", 1, 25),
                    r("guhs:guh_taart", 16, null, 0, "guhs:pyjama_pakje", 1, 30),
                    r("guhs:roze_guh_koek", 24, null, 0, "guhs:badjasje", 1, 25),
                    r("guhs:sterrenkoekje", 32, "guhs:roze_broodje", 32, "guhs:weerpotje_regenboog", 1, 30),
                    r("guhs:roze_knabbelkoekje", 64, null, 0, "guhs:blubkleding_slaapmutsje", 1, 25)));

    private Export() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Export::praat);
        NeoForge.EVENT_BUS.addListener(Export::gehandeld);
    }

    /** The profession's key path for the four guh professions, or null for any other villager. */
    @Nullable
    static String beroep(Villager v) {
        Holder<VillagerProfession> b = v.getVillagerData().profession();
        for (var p : List.of(GuhVillagersFeature.TOVERGUH, GuhVillagersFeature.VADSMONTEUR, GuhVillagersFeature.MIJNWERKER,
                GuhVillagersFeature.KRISTALSLIJPER)) {
            if (b.is(p.getKey())) {
                return p.getId().getPath();
            }
        }
        return null;
    }

    /** A player talks to a villager: a new day = new export trades (before the trade screen opens). */
    private static void praat(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Villager v && event.getLevel() instanceof ServerLevel level) {
            ververs(v, level.getGameTime() / 24_000L);
        }
    }

    /** Gives this villager the export trades of {@code dag} (once per day; also used by tests). Returns true when it did. */
    public static boolean ververs(Villager v, long dag) {
        String beroep = beroep(v);
        if (beroep == null || v.getPersistentData().getLongOr(DAG, -1L) == dag) {
            return false;
        }
        v.getPersistentData().putLong(DAG, dag);
        MerchantOffers offers = v.getOffers();
        offers.removeIf(Export::isExport);
        List<Ruil> pool = new ArrayList<>(POOL.get(beroep));
        RandomSource rnd = RandomSource.create(v.getUUID().getLeastSignificantBits() ^ dag);
        int erbij = 0;
        while (erbij < PER_DAG && !pool.isEmpty()) {
            MerchantOffer offer = GuhVillagerHandel.offer(pool.remove(rnd.nextInt(pool.size())), v.registryAccess(), rnd);
            if (offer != null) {
                offers.add(offer);
                erbij++;
            }
        }
        return true;
    }

    /** Is this one of the export trades (what it asks and gives match a trade of the pool)? */
    public static boolean isExport(MerchantOffer offer) {
        String a = id(offer.getBaseCostA()), u = id(offer.getResult());
        int n = offer.getBaseCostA().getCount();
        for (List<Ruil> lijst : POOL.values()) {
            for (Ruil r : lijst) {
                if (r.wants().equals(a) && r.wantCount() == n && r.gives() != null && r.gives().equals(u)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String id(ItemStack stack) {
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.toString();
    }

    /** A done export trade: the FTB quest's hidden advancement and the visible one. */
    private static void gehandeld(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isExport(event.getMerchantOffer())) {
            GuhAdvancements.grant(p, "centrale_export");
            GidsFeature.grant(p, "techniek/centrale_export");
        }
    }
}
