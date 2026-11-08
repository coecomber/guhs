package nl.juiced.guhs.feature.vadsrijk.menu.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.juiced.guhs.feature.vadsrijk.dialoog.client.DialoogConfig;
import nl.juiced.guhs.feature.vadsrijk.geluid.client.GeluidConfig;
import nl.juiced.guhs.feature.vadsrijk.kern.beleving.client.BelevingConfig;
import nl.juiced.guhs.feature.vadsrijk.vfx.client.VfxConfig;
import nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client.SfeerConfig;

/**
 * U-TABS: the Instellingen-tab of the Vadsrijk-menu: the Vadsrijk client options as pink sliders and choices, saved at once in
 * the same config files as Mods &gt; Guhs &gt; Config (audio: area music, boss music, stingers, ambience, voices, town music;
 * picture: effects, atmosphere, screen shake, text speed). Buttons: the key bindings, and the HUD editor when a slice registered
 * the sub-tab {@code "hud"} under "instellingen" ({@code VadsrijkMenu.subtab("instellingen", "hud", ...)}).
 */
public class InstellingenPaneel extends MenuPaneel {
    private static final String K = "vadsrijk.menu.instellingen.";

    /** One option row. */
    private interface Optie {
        Component naam();

        @Nullable
        default Component uitleg() {
            return null;
        }
    }

    private record Schuif(String id, ModConfigSpec.IntValue v, int min, int max, int standaard, boolean procent) implements Optie {
        @Override
        public Component naam() {
            return Component.translatable(K + id);
        }

        @Override
        public Component uitleg() {
            return Component.translatable(K + id + ".uitleg");
        }

        int waarde() {
            return veilig(() -> v.get(), standaard);
        }

        void zet(int n) {
            try {
                v.set(Math.max(min, Math.min(max, n)));
                v.save();
            } catch (RuntimeException e) {
                // (config not loaded yet: ignore)
            }
        }
    }

    private record Keuze<E extends Enum<E>>(String id, ModConfigSpec.EnumValue<E> v, E standaard) implements Optie {
        @Override
        public Component naam() {
            return Component.translatable(K + id);
        }

        @Override
        public Component uitleg() {
            return Component.translatable(K + id + ".uitleg");
        }

        E waarde() {
            return veilig(() -> v.get(), standaard);
        }

        void zet(E e) {
            try {
                v.set(e);
                v.save();
            } catch (RuntimeException ex) {
                // (config not loaded yet)
            }
        }
    }

    private record AanUit(String id, ModConfigSpec.BooleanValue v, boolean standaard) implements Optie {
        @Override
        public Component naam() {
            return Component.translatable(K + id);
        }

        @Override
        public Component uitleg() {
            return Component.translatable(K + id + ".uitleg");
        }
    }

    private record Kop(String id) implements Optie {
        @Override
        public Component naam() {
            return Component.translatable(K + id);
        }
    }

    private static <T> T veilig(java.util.function.Supplier<T> s, T standaard) {
        try {
            return s.get();
        } catch (RuntimeException e) {
            return standaard;
        }
    }

    private final List<Optie> opties = new ArrayList<>();
    @Nullable
    private Schuif sleep;
    private int sleepX, sleepW;

    public InstellingenPaneel() {
        super("instellingen", Component.translatable("vadsrijk.menu.tab.instellingen"), new CompoundTag());
        opties.add(new Kop("kop.geluid"));
        opties.add(new Schuif("gebiedsmuziek", GeluidConfig.GEBIEDSMUZIEK, 0, 100, 70, true));
        opties.add(new Schuif("bazenmuziek", GeluidConfig.BAZENMUZIEK, 0, 100, 85, true));
        opties.add(new Schuif("stingers", GeluidConfig.STINGERS, 0, 100, 80, true));
        opties.add(new Schuif("omgeving", GeluidConfig.OMGEVING, 0, 100, 70, true));
        opties.add(new Schuif("stemmen", DialoogConfig.STEMMEN, 0, 100, 60, true));
        opties.add(new AanUit("stadsmuziek", SfeerConfig.STADSMUZIEK, true));
        opties.add(new Kop("kop.beeld"));
        opties.add(new Keuze<>("effecten", VfxConfig.NIVEAU, VfxConfig.Niveau.VOL));
        opties.add(new Keuze<>("sfeer", SfeerConfig.NIVEAU, SfeerConfig.Niveau.VOL));
        opties.add(new Schuif("weer", nl.juiced.guhs.feature.vadsrijk.ritme.client.RitmeConfig.WEER, 0, 100, 70, true));   // U-LEVEN-RITME
        opties.add(new Keuze<>("stadsleven", nl.juiced.guhs.feature.vadsrijk.stadsleven.client.StadslevenConfig.NIVEAU,   // U-LEVEN-STAD
                nl.juiced.guhs.feature.vadsrijk.stadsleven.client.StadslevenConfig.Niveau.VOL));
        opties.add(new Schuif("schermschud", BelevingConfig.SCHUD, 0, 100, 100, true));
        opties.add(new Schuif("tekstsnelheid", DialoogConfig.SNELHEID, 0, 3, 2, false));
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int kolom = (w - 24) / 2;
        int x = left + 10, y = top + 24, rij = 16;
        int helft = (opties.size() + 1) / 2;
        // two columns: audio left, picture right (split at the second heading)
        int split = 0;
        for (int i = 1; i < opties.size(); i++) {
            if (opties.get(i) instanceof Kop) {
                split = i;
            }
        }
        if (split == 0) {
            split = helft;
        }
        for (int i = 0; i < opties.size(); i++) {
            if (i == split) {
                x = left + 14 + kolom;
                y = top + 24;
            }
            teken(g, mx, my, opties.get(i), x, y, kolom - 6);
            y += opties.get(i) instanceof Kop ? 13 : rij + 4;
        }
        // buttons at the bottom
        int by = top + h - 26;
        Minecraft mc = Minecraft.getInstance();
        knop(g, mx, my, left + 10, by, 110, 16, Component.translatable(K + "toetsen"), true,
                () -> mc.setScreen(new net.minecraft.client.gui.screens.options.controls.KeyBindsScreen(this, mc.options)));
        nl.juiced.guhs.feature.vadsrijk.menu.MenuIndeling.Tab hud = VadsrijkMenu.vind("hud");
        if (hud != null) {
            knop(g, mx, my, left + 126, by, 110, 16, Component.translatable(K + "hud"), true, () -> VadsrijkMenu.wissel(hud));
        }
        if (mc.player != null && mc.player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)
                && mc.getConnection() != null) {   // U-QUESTLOPEN: the admin main-line walker (OP only; the command checks again)
            knop(g, mx, my, left + 10, by - 20, 110, 16, Component.translatable("vadsrijk.questlopen.knop.menu"), true,
                    () -> {
                        mc.setScreen(null);
                        mc.getConnection().sendCommand("guhs vadsrijk questlopen");
                    });
        }
        for (var regel : font.split(Component.translatable(K + "meer"), w - 260)) {
            g.text(font, regel, left + 246, by - 2, ZACHT, false);
            by += 10;
        }
    }

    private void teken(GuiGraphicsExtractor g, int mx, int my, Optie o, int x, int y, int bw) {
        if (o instanceof Kop) {
            kop(g, o.naam(), x, y);
            g.fill(x, y + 10, x + bw, y + 11, 0xFFFFB0D8);
            return;
        }
        int lw = bw * 9 / 20;
        String waardeTekst = o instanceof Schuif s0 ? (s0.procent() ? s0.waarde() + "%" : tekstSnelheid(s0.waarde())) : "";
        g.text(font, font.plainSubstrByWidth(o.naam().getString(), lw - 8 - font.width(waardeTekst)), x, y + 4, TEKST, false);
        g.text(font, waardeTekst, x + lw - 6 - font.width(waardeTekst), y + 4, ZACHT, false);
        if (o.uitleg() != null) {
            tip(mx, my, x, y, lw, 16, List.of(o.naam().copy().withStyle(ChatFormatting.LIGHT_PURPLE), o.uitleg().copy().withStyle(ChatFormatting.GRAY)));
        }
        int cx = x + lw, cw = bw - lw;
        if (o instanceof Schuif s) {
            int v = s.waarde();
            float f = (v - s.min()) / (float) (s.max() - s.min());
            g.fill(cx, y + 6, cx + cw, y + 10, 0xFFF0D8E4);
            g.fill(cx, y + 6, cx + Math.round(cw * f), y + 10, 0xFFFF5FAF);
            int hx = cx + Math.round((cw - 6) * f);
            boolean over = binnen(mx, my, cx - 2, y + 1, cw + 4, 14);
            g.fill(hx, y + 2, hx + 6, y + 14, over ? 0xFFC0287C : 0xFFE0489A);
            g.fill(hx + 1, y + 3, hx + 5, y + 13, over ? 0xFFFF8CC8 : 0xFFFF6FB5);
            klikvlak(cx - 2, y + 1, cw + 4, 14, () -> {
                sleep = s;
                sleepX = cx;
                sleepW = cw;
                zetSchuif((int) Math.round(huidigeMuis()));
            });
        } else if (o instanceof Keuze<?> k) {
            keuzeKnoppen(g, mx, my, k, cx, y, cw);
        } else if (o instanceof AanUit a) {
            boolean aan = veilig(() -> a.v().get(), a.standaard());
            int half = (cw - 4) / 2;
            knop(g, mx, my, cx, y, half, 15, Component.translatable(K + "aan"), !aan, () -> zet(a, true));
            knop(g, mx, my, cx + half + 4, y, half, 15, Component.translatable(K + "uit"), aan, () -> zet(a, false));
        }
    }

    private <E extends Enum<E>> void keuzeKnoppen(GuiGraphicsExtractor g, int mx, int my, Keuze<E> k, int cx, int y, int cw) {
        E[] alle = k.standaard().getDeclaringClass().getEnumConstants();
        E nu = k.waarde();
        int bw = (cw - 3 * (alle.length - 1)) / alle.length;
        for (int i = 0; i < alle.length; i++) {
            E e = alle[i];
            Component c = Component.translatable(K + "niveau." + e.name().toLowerCase(java.util.Locale.ROOT));
            knop(g, mx, my, cx + i * (bw + 3), y, bw, 15, e == nu ? c.copy().withStyle(ChatFormatting.UNDERLINE) : c, e != nu, () -> k.zet(e));
        }
    }

    private static void zet(AanUit a, boolean b) {
        try {
            a.v().set(b);
            a.v().save();
        } catch (RuntimeException e) {
            // (config not loaded yet)
        }
    }

    private static String tekstSnelheid(int v) {
        return Component.translatable(K + "snelheid." + v).getString();
    }

    private double huidigeMuis() {
        Minecraft mc = Minecraft.getInstance();
        double s = menuSchaal();
        return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth() / s;
    }

    private void zetSchuif(int muisX) {
        if (sleep == null) {
            return;
        }
        float f = Math.max(0f, Math.min(1f, (muisX - sleepX) / (float) Math.max(1, sleepW)));
        int stap = sleep.procent() ? 5 : 1;
        int v = sleep.min() + Math.round(f * (sleep.max() - sleep.min()) / stap) * stap;
        sleep.zet(v);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (sleep != null) {
            zetSchuif((int) event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        sleep = null;
        return super.mouseReleased(event);
    }
}
