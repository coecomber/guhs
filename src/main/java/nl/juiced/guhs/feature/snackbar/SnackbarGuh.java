package nl.juiced.guhs.feature.snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.frituur.FrituurFeature;
import nl.juiced.guhs.feature.frituur.SnackRecepten;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Snackbar-guh behind the counter of a Guhsnackbar (SNACKBARGUH): sells snacks over the counter and teaches the recipes
 * of {@link SnackRecepten} for Kaas Knabbels. A recipe is learned for good: after that you fry it at home in your own pan.
 */
public final class SnackbarGuh implements NpcRole {
    public static final SnackbarGuh ROL = new SnackbarGuh();
    /** What a recipe costs in Kaas Knabbels (tools/features/snackbar.py shows the same numbers on the buttons). */
    public static final Map<String, Integer> PRIJZEN = Map.of("patatje_vads", 24, "patatje_oorlog", 24, "bitterguhllen", 32, "kaassoufflguh", 32,
            "kaaskroket", 32, "patatje_guhspeciaal", 40, "frikandel_guhspeciaal", 48, "vetste_hap", 256);
    private static final int PER_PAGINA = 4;
    private static final int BESTELLEN = 1, RECEPTEN = 2, UITLEG = 3, TERUG = 4, DOEI = 9, RECEPT = 100, PAGINA = 1000;
    private static final String T = "gui.guhs.snackbar.guh.";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        Praat.open(p, npc, null, T + "hallo", new Object[0], new Praat.Optie(BESTELLEN, T + "optie.bestellen"),
                new Praat.Optie(RECEPTEN, T + "optie.recepten"), new Praat.Optie(UITLEG, T + "optie.uitleg"), new Praat.Optie(DOEI, T + "optie.doei"));
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
        switch (optie) {
            case BESTELLEN -> {
                Praat.sluit(p);
                npc.openShop(p);
            }
            case RECEPTEN -> recepten(npc, p, 0);
            case UITLEG -> Praat.open(p, npc, null, T + "uitleg", new Object[0], new Praat.Optie(TERUG, T + "optie.terug"), new Praat.Optie(DOEI, T + "optie.doei"));
            case TERUG -> talk(npc, p);
            case DOEI -> Praat.sluit(p);
            default -> {
                if (optie >= PAGINA) {
                    recepten(npc, p, optie - PAGINA);
                } else if (optie >= RECEPT && optie - RECEPT < SnackRecepten.ALLE.size()) {
                    koop(npc, p, SnackRecepten.ALLE.get(optie - RECEPT));
                }
            }
        }
    }

    /** The recipes this player doesn't know yet, cheapest first, a few per page. */
    private static void recepten(GuhNpcEntity npc, ServerPlayer p, int vanaf) {
        List<String> open = nogTeLeren(p);
        if (open.isEmpty()) {
            Praat.open(p, npc, null, T + "alles", new Object[0], new Praat.Optie(TERUG, T + "optie.terug"), new Praat.Optie(DOEI, T + "optie.doei"));
            return;
        }
        int start = vanaf < open.size() ? vanaf : 0;
        List<Praat.Optie> opties = new ArrayList<>();
        for (String id : open.subList(start, Math.min(open.size(), start + PER_PAGINA))) {
            opties.add(new Praat.Optie(RECEPT + SnackRecepten.ALLE.indexOf(id), "gui.guhs.snackbar.recept." + id));
        }
        if (open.size() > PER_PAGINA) {
            opties.add(new Praat.Optie(PAGINA + start + PER_PAGINA, T + "optie.volgende"));
        }
        opties.add(new Praat.Optie(TERUG, T + "optie.terug"));
        Praat.open(p, npc, null, T + "recepten", new Object[0], opties.toArray(new Praat.Optie[0]));
    }

    /** (Also for the tests) what this player can still learn, cheapest first. */
    static List<String> nogTeLeren(ServerPlayer p) {
        return SnackRecepten.ALLE.stream().filter(id -> !SnackRecepten.kent(p, id))
                .sorted(java.util.Comparator.comparingInt((String id) -> prijs(id)).thenComparing(id -> id)).toList();
    }

    private static void koop(GuhNpcEntity npc, ServerPlayer p, String id) {
        if (leer(p, id)) {
            p.level().playSound(null, npc.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.5f);
            p.sendSystemMessage(Component.translatable(T + "geleerd", Component.translatable("item.guhs." + id)).withStyle(ChatFormatting.LIGHT_PURPLE));
            recepten(npc, p, 0);
        } else {
            Praat.open(p, npc, null, T + "te_duur", new Object[]{prijs(id)}, new Praat.Optie(RECEPTEN, T + "optie.ander"), new Praat.Optie(DOEI, T + "optie.doei"));
        }
    }

    /** Takes the knabbels and teaches the recipe; false (and nothing happens) when the player knows it or can't pay. */
    static boolean leer(ServerPlayer p, String id) {
        int prijs = prijs(id);
        Item knabbel = ModItems.KAAS_KNABBELS.get();
        if (SnackRecepten.kent(p, id) || !p.hasInfiniteMaterials() && p.getInventory().countItem(knabbel) < prijs) {
            return false;
        }
        if (!p.hasInfiniteMaterials()) {
            p.getInventory().clearOrCountMatchingItems(stack -> stack.is(knabbel), prijs, p.inventoryMenu.getCraftSlots());
        }
        SnackRecepten.leer(p, id);
        return true;
    }

    private static int prijs(String id) {
        return PRIJZEN.getOrDefault(id, 64);
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(snack(5, FrituurFeature.PATATJE_KAASSAUS.get()));
        offers.add(snack(7, FrituurFeature.PATATJE_VADS.get()));
        offers.add(snack(8, FrituurFeature.PATATJE_OORLOG.get()));
        offers.add(snack(9, FrituurFeature.PATATJE_GUHSPECIAAL.get()));
        offers.add(snack(8, FrituurFeature.KAASKROKET.get()));
        offers.add(snack(9, FrituurFeature.FRIKANDEL.get()));
        offers.add(snack(7, FrituurFeature.BITTERGUHLLEN.get()));
        offers.add(snack(7, FrituurFeature.KAASSOUFFLGUH.get()));
        return offers;
    }

    private static MerchantOffer snack(int prijs, Item item) {
        return new MerchantOffer(new ItemCost(ModItems.KAAS_KNABBELS.get(), prijs), new ItemStack(item), Integer.MAX_VALUE, 0, 0);
    }

    private SnackbarGuh() {
    }
}
