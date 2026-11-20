package nl.juiced.guhs.feature.bouwhulp.kern;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * Test round 5: "Exporteer naar lijstje": the blocks a ghost still needs as a plain written book (no paper needed): per
 * line the exact item name and how many, most first, {@link #PER_BLAD} lines a page. Handy next to the crafting table.
 */
public final class Lijstje {
    static final int PER_BLAD = 10;

    /** Gives (or drops) the book. False when nothing is missing. */
    public static boolean geef(ServerPlayer p, String project, Map<Item, Integer> nodig) {
        return geef(p, project, nodig, List.of());
    }

    /** 1.10.10: with the lines of what the keet crafts itself ("12× Eiken trap (worden gemaakt van 3× Eiken stam)"). */
    public static boolean geef(ServerPlayer p, String project, Map<Item, Integer> nodig, List<Werkplan.Gemaakt> gemaakt) {
        return geef(p, project, nodig, gemaakt, -1);
    }

    /** 1.10.10: {@code dekking} >= 0: the book also says how much is there (the 80% rule). */
    public static boolean geef(ServerPlayer p, String project, Map<Item, Integer> nodig, List<Werkplan.Gemaakt> gemaakt, int dekking) {
        if (nodig.isEmpty()) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.lijstje.niets").withStyle(ChatFormatting.GREEN));
            return false;
        }
        ItemStack boek = boek(project, p.getGameProfile().name(), nodig, gemaakt, dekking);
        if (!p.getInventory().add(boek)) {
            p.drop(boek, false);
        }
        p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.lijstje.gegeven").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    public static ItemStack boek(String project, String schrijver, Map<Item, Integer> nodig) {
        return boek(project, schrijver, nodig, List.of());
    }

    public static ItemStack boek(String project, String schrijver, Map<Item, Integer> nodig, List<Werkplan.Gemaakt> gemaakt) {
        return boek(project, schrijver, nodig, gemaakt, -1);
    }

    public static ItemStack boek(String project, String schrijver, Map<Item, Integer> nodig, List<Werkplan.Gemaakt> gemaakt, int dekking) {
        List<Map.Entry<Item, Integer>> lijst = new ArrayList<>(nodig.entrySet());
        lijst.sort(Map.Entry.<Item, Integer>comparingByValue().reversed());
        int totaal = lijst.stream().mapToInt(Map.Entry::getValue).sum();
        List<Filterable<Component>> bladen = new ArrayList<>();
        MutableComponent blad = Component.empty()
                .append(Component.translatable("gui.guhs.bouwhulp.lijstje.kop", project).withStyle(ChatFormatting.BOLD)).append("\n")
                .append(Component.translatable("gui.guhs.bouwhulp.lijstje.totaal", totaal).withStyle(ChatFormatting.DARK_GRAY)).append("\n");
        int regels = 3;
        if (dekking >= 0) {
            blad.append(Component.translatable("gui.guhs.bouwhulp.lijstje.dekking", dekking, Spoken.MIN_DEKKING)
                    .withStyle(dekking >= Spoken.MIN_DEKKING ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)).append("\n");
            regels += 2;
        }
        blad.append("\n");
        for (Map.Entry<Item, Integer> e : lijst) {
            if (regels >= PER_BLAD + 3) {
                bladen.add(Filterable.passThrough(blad));
                blad = Component.empty();
                regels = 0;
            }
            int n = e.getValue(), stapel = e.getKey().getDefaultInstance().getMaxStackSize();
            MutableComponent regel = e.getKey() == Items.WATER_BUCKET
                    ? Component.translatable("gui.guhs.bouwhulp.water.regel", n).withStyle(ChatFormatting.DARK_BLUE)
                    : Component.literal(n + "× ").withStyle(ChatFormatting.DARK_RED)
                    .append(e.getKey().getDefaultInstance().getHoverName().copy().withStyle(ChatFormatting.BLACK));
            if (stapel > 1 && n >= stapel) {
                regel.append(Component.literal(" (" + (n / stapel) + "×" + stapel + (n % stapel > 0 ? "+" + n % stapel : "") + ")")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            blad.append(regel).append("\n");
            regels++;
        }
        if (!gemaakt.isEmpty()) {
            // 1.10.10: what the keet crafts itself, on a page of its own
            bladen.add(Filterable.passThrough(blad));
            blad = Component.empty().append(Component.translatable("gui.guhs.bouwhulp.lijstje.maakt").withStyle(ChatFormatting.BOLD)).append("\n\n");
            int r = 2;
            for (Werkplan.Gemaakt g : gemaakt) {
                if (r >= PER_BLAD) {
                    bladen.add(Filterable.passThrough(blad));
                    blad = Component.empty();
                    r = 0;
                }
                blad.append(Werkplan.regel(g).copy().withStyle(ChatFormatting.DARK_GREEN)).append("\n");
                r += 2;
            }
        }
        bladen.add(Filterable.passThrough(blad));
        String titel = Component.translatable("gui.guhs.bouwhulp.lijstje.titel").getString();
        ItemStack boek = new ItemStack(Items.WRITTEN_BOOK);
        boek.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(titel.length() > 32 ? titel.substring(0, 32) : titel),
                schrijver, 0, bladen, true));
        return boek;
    }

    private Lijstje() {
    }
}
