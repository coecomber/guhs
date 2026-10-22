package nl.juiced.guhs.feature.bouwhulp.client;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Optie;
import nl.juiced.guhs.feature.gids.client.GidsTekst;

/**
 * The settings of a tool as rows of controls (the settings screen and the keet's Vormen/Daken tabs draw the same list from
 * the server's description, {@code gereedschap.Opties}): a choice cycles (shift: back, locked ones are skipped), a number
 * has − and + (shift: 5), on/off toggles, a block or mix shows its blocks and opens the hotbar picker, a text is a text box.
 */
final class OptieLijst {
    /** Where the controls go (a Screen, or the keet's menu). */
    interface Houder {
        Button knop(Component tekst, int x, int y, int w, Runnable actie, boolean actief);

        <T extends AbstractWidget> T voegToe(T widget);

        Font font();

        /** Rebuild the controls (the hotbar picker opened or closed). */
        void opnieuw();
    }

    interface Stuur {
        void zet(String key, int waarde, String tekst);
    }

    static final int RIJ = 20;
    /** From this many options a choice opens the {@link KeuzeRaster} (shift-click still steps back through them). */
    static final int RASTER_VANAF = 6;
    /** The block/mix setting whose hotbar picker is open (null: none). */
    @Nullable
    static String kiezer;

    private static int kolommen(int w) {
        return w >= 300 ? 2 : 1;
    }

    static void knoppen(Houder h, CompoundTag spec, int x, int y, int w, int hoog, boolean mag, Stuur s) {
        ListTag opties = spec.getListOrEmpty("Opties");
        int kol = kolommen(w), kw = w / kol;
        boolean kiezerGeldig = false;
        for (int i = 0; i < opties.size(); i++) {
            CompoundTag o = opties.getCompoundOrEmpty(i);
            String key = o.getStringOr("Key", "");
            int cx = x + (i % kol) * kw, cy = y + (i / kol) * RIJ;
            int bx = cx + (int) (kw * 0.44), bw = (int) (kw * 0.54);
            int waarde = o.getIntOr("Waarde", 0);
            switch (Optie.Soort.values()[o.getIntOr("Soort", 0)]) {
                case KEUZE -> {
                    ListTag k = o.getListOrEmpty("Keuzes");
                    CompoundTag nu = k.getCompoundOrEmpty(Math.min(waarde, k.size() - 1));
                    Button b = h.knop(keuze(key, nu), bx, cy, bw, () -> {
                        // test round 5: many options (shapes, roofs, patterns): a grid to pick from instead of clicking through
                        if (k.size() >= RASTER_VANAF && !Minecraft.getInstance().hasShiftDown()) {
                            Minecraft mc = Minecraft.getInstance();
                            mc.setScreen(new KeuzeRaster(mc.screen, key, k, waarde, j -> s.zet(key, j, "")));
                            return;
                        }
                        int stap = Minecraft.getInstance().hasShiftDown() ? -1 : 1;
                        for (int n = 1; n <= k.size(); n++) {
                            int j = Math.floorMod(waarde + stap * n, k.size());
                            if (!k.getCompoundOrEmpty(j).contains("Dicht")) {
                                s.zet(key, j, "");
                                return;
                            }
                        }
                    }, mag);
                    if (nu.contains("Dicht")) {
                        b.setTooltip(Tooltip.create(Component.translatable(nu.getStringOr("Dicht", ""))));
                    } else if (k.size() >= RASTER_VANAF) {
                        b.setTooltip(Tooltip.create(Component.translatable("gui.guhs.bouwhulp.raster.knop")));
                    }
                }
                case GETAL -> {
                    int min = o.getIntOr("Min", 0), max = o.getIntOr("Max", 0);
                    h.knop(Component.literal("−"), bx, cy, 18, () -> s.zet(key, Math.max(min, waarde - (Minecraft.getInstance().hasShiftDown() ? 5 : 1)), ""),
                            mag && waarde > min);
                    h.knop(Component.literal("+"), bx + bw - 18, cy, 18, () -> s.zet(key, Math.min(max, waarde + (Minecraft.getInstance().hasShiftDown() ? 5 : 1)), ""),
                            mag && waarde < max);
                }
                case AAN_UIT -> h.knop(Component.translatable(waarde != 0 ? "gui.guhs.bouwhulp.aan" : "gui.guhs.bouwhulp.uit"), bx, cy, bw,
                        () -> s.zet(key, waarde != 0 ? 0 : 1, ""), mag);
                case BLOK, MIX -> {
                    boolean mix = o.getIntOr("Soort", 0) == Optie.Soort.MIX.ordinal();
                    int kw2 = mix ? 18 : 22;
                    h.knop(Component.literal(key.equals(kiezer) ? "…" : "+"), bx + bw - (mix ? 38 : 22), cy, kw2,
                            () -> {
                                kiezer = key.equals(kiezer) ? null : key;
                                h.opnieuw();
                            }, mag).setTooltip(Tooltip.create(Component.translatable("gui.guhs.bouwhulp.kies.uitleg")));
                    if (mix) {
                        h.knop(Component.literal("✕"), bx + bw - 18, cy, 18, () -> s.zet(key, -2, ""), mag && !o.getListOrEmpty("Blokken").isEmpty());
                    }
                    if (key.equals(kiezer)) {
                        kiezerGeldig = true;
                    }
                }
                case TEKST -> {
                    EditBox e = new EditBox(h.font(), bx, cy, bw, 18, Component.translatable("gui.guhs.bouwhulp.optie." + key));
                    e.setMaxLength(24);
                    e.setValue(o.getStringOr("Tekst", ""));
                    e.setResponder(v -> s.zet(key, 0, v));
                    e.setEditable(mag);
                    h.voegToe(e);
                }
            }
        }
        if (kiezerGeldig && mag) {
            // the hotbar picker along the bottom: the nine hotbar slots and the offhand
            var speler = Minecraft.getInstance().player;
            int py = y + hoog - 19, px = x + w - 10 * 19;
            for (int i = 0; i < 10; i++) {
                int slot = i < 9 ? i : -1;
                ItemStack st = speler == null ? ItemStack.EMPTY : slot >= 0 ? speler.getInventory().getItem(slot) : speler.getOffhandItem();
                String key = kiezer;
                h.voegToe(new ItemKnop(px + i * 19, py, st, () -> {
                    s.zet(key, slot, "");
                    kiezer = null;
                }));
            }
        } else if (!kiezerGeldig) {
            kiezer = null;
        }
    }

    static void teken(GuiGraphicsExtractor g, CompoundTag spec, int x, int y, int w, int hoog, int tekst, int licht) {
        ListTag opties = spec.getListOrEmpty("Opties");
        int kol = kolommen(w), kw = w / kol;
        for (int i = 0; i < opties.size(); i++) {
            CompoundTag o = opties.getCompoundOrEmpty(i);
            String key = o.getStringOr("Key", "");
            int cx = x + (i % kol) * kw, cy = y + (i / kol) * RIJ;
            int bx = cx + (int) (kw * 0.44), bw = (int) (kw * 0.54);
            GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.optie." + key), cx, cy + 5, (int) (kw * 0.42), 0.75f, tekst, false);
            switch (Optie.Soort.values()[o.getIntOr("Soort", 0)]) {
                case GETAL -> {
                    String v = String.valueOf(o.getIntOr("Waarde", 0));
                    GidsTekst.schaal(g, Component.literal(v).withStyle(ChatFormatting.BOLD), bx + bw / 2 - v.length() * 3, cy + 5, 0.875f, tekst, false);
                }
                case BLOK, MIX -> {
                    ListTag b = o.getListOrEmpty("Blokken");
                    if (b.isEmpty()) {
                        GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.standaard").withStyle(ChatFormatting.ITALIC), bx, cy + 5,
                                bw - 40, 0.7f, licht, false);
                    }
                    for (int j = 0; j < b.size(); j++) {
                        var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(b.getStringOr(j, "minecraft:air")));
                        if (item.isPresent()) {
                            g.item(new ItemStack(item.get()), bx + j * 17, cy + 1);
                        }
                    }
                }
                default -> {
                }
            }
        }
        if (kiezer != null) {
            int py = y + hoog - 19, px = x + w - 10 * 19;
            g.fill(x, py - 2, x + w, py + 19, 0xE0F3E7D3);
            GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.kies"), x + 2, py + 5, px - x - 6, 0.7f, tekst, false);
        }
    }

    /** How many rows the list takes (for sizing a screen). */
    static int rijen(CompoundTag spec, int w) {
        int n = spec.getListOrEmpty("Opties").size();
        int kol = kolommen(w);
        return (n + kol - 1) / kol;
    }

    private static Component keuze(String key, CompoundTag k) {
        Component c = Component.translatable("gui.guhs.bouwhulp.keuze." + key + "." + k.getStringOr("Id", ""));
        return k.contains("Dicht") ? c.copy().withStyle(ChatFormatting.DARK_GRAY) : c;
    }

    private OptieLijst() {
    }
}
