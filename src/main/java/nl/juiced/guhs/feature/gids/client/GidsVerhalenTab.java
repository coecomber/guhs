package nl.juiced.guhs.feature.gids.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.gids.VerhalenVoortgang;

/**
 * The Guhdex tab "Verhalen": every questline (the Guhverhalen first, then the older adventures) as a row with its icon,
 * name, status ("nog niet begonnen" / "stap 3 van 7: ..." / "klaar!") and a bar; click a row for its page: what to do
 * now, whom to go to and where, what you still need (with counts), every step ticked off, and the rewards. Both scroll
 * (wheel or bar). Everything comes from {@link VerhalenCache}.
 */
public final class GidsVerhalenTab {
    /** The questline whose page is open (null: the list); remembered while the game runs. */
    @Nullable
    private static String open;
    private static double lijstScroll, paginaScroll;
    private static final int RIJ = 28, KOP = 14;
    private static final int TEKST = 0xFF5A3A4A, DONKER = 0xFF3A1C30, ROZE = 0xFF7A2848, LICHT = 0xFFB0708A, GROEN = 0xFF3E9A5A,
            GRIJS = 0xFF9A8090;

    private final GidsLijst lijst = new GidsLijst();
    private Runnable herbouw = () -> {
    };
    private int left, top, w, h;

    /** (AutoCheck) open this questline's page (null: the list). */
    public static void open(@Nullable String id) {
        open = id;
        paginaScroll = 0;
    }

    @Nullable
    public static String open() {
        return open;
    }

    public void init(int left, int top, int w, int h, Consumer<AbstractWidget> knop, Runnable herbouw) {
        this.left = left;
        this.top = top;
        this.w = w;
        this.h = h;
        this.herbouw = herbouw;
        VerhaalStand v = open == null ? null : VerhalenCache.van(open);
        if (v == null) {
            lijst.plaats(left + 8, top + 40, w - 14, h - 46);
            lijst.zet(lijstRegels());
            lijst.scrollNaar(lijstScroll);
        } else {
            knop.accept(Button.builder(Component.translatable("gui.guhs.verhalen.terug"), b -> {
                paginaScroll = 0;
                open = null;
                this.herbouw.run();
            }).bounds(left + 8, top + 28, 70, 14).build());
            lijst.plaats(left + 8, top + 46, w - 14, h - 52);
            lijst.zet(pagina(v));
            lijst.scrollNaar(paginaScroll);
        }
    }

    public void bewaarScroll() {
        if (open == null) {
            lijstScroll = lijst.scroll();
        } else {
            paginaScroll = lijst.scroll();
        }
    }

    /** (AutoCheck) screenfuls of the current list, and scroll to one. */
    public int paginas() {
        return 1 + (int) Math.ceil(lijst.max() / (double) Math.max(20, h - 80));
    }

    public void scrollNaar(int pagina) {
        lijst.scrollNaar(pagina * Math.max(20, h - 80));
        bewaarScroll();
    }

    // =====================================================================================================================
    // drawing / input
    // =====================================================================================================================

    public void teken(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        VerhaalStand v = open == null ? null : VerhalenCache.van(open);
        if (v == null) {
            List<VerhaalStand> alle = VerhalenCache.verhalen();
            long klaar = alle.stream().filter(VerhaalStand::klaar).count();
            // (the count sits on the line under the tab bar: the tab bar's title needs all its room)
            Component n = Component.literal(klaar + " / " + alle.size() + " ✔");
            GidsTekst.schaal(g, n, left + w - 10, top + 29, 0.75f, ROZE, true);
            int nw = Math.round(GidsTekst.font().width(n) * 0.75f) + 8;
            GidsTekst.passend(g, Component.translatable(alle.isEmpty() ? "gui.guhs.verhalen.laden" : "gui.guhs.verhalen.klik"),
                    left + 10, top + 29, w - 20 - nw, 0.75f, LICHT, false);
        } else {
            // next to the back button: the name of the story
            GidsTekst.passend(g, v.naam().copy().withStyle(ChatFormatting.BOLD), left + 84, top + 31, w - 92, 1f, DONKER, false);
        }
        lijst.teken(g, mouseX, mouseY, 0xFFD27A9C, 0x30D27A9C);
    }

    public boolean wiel(double mx, double my, double delta) {
        boolean r = lijst.wiel(mx, my, delta);
        if (r) {
            bewaarScroll();
        }
        return r;
    }

    public boolean sleep(double my) {
        boolean r = lijst.sleep(my);
        if (r) {
            bewaarScroll();
        }
        return r;
    }

    public void los() {
        lijst.los();
    }

    public boolean klik(double mx, double my, int button) {
        boolean r = lijst.klik(mx, my, button);
        if (r) {
            bewaarScroll();
        }
        return r;
    }

    @Nullable
    public List<Component> tip(double mx, double my) {
        return lijst.tip(mx, my);
    }

    // =====================================================================================================================
    // the list
    // =====================================================================================================================

    private List<GidsLijst.Regel> lijstRegels() {
        List<GidsLijst.Regel> out = new ArrayList<>();
        List<VerhaalStand> alle = VerhalenCache.verhalen();
        String kop = null;
        for (VerhaalStand v : alle) {
            String nieuw = VerhalenVoortgang.NIEUW.contains(v.id()) ? "nieuw" : "oud";
            if (!nieuw.equals(kop)) {
                kop = nieuw;
                out.add(kop(Component.translatable("gui.guhs.verhalen.kop." + nieuw)));
            }
            out.add(new Rij(v));
        }
        return out;
    }

    private static GidsLijst.Regel kop(Component tekst) {
        return new GidsLijst.Regel() {
            @Override
            public int hoogte() {
                return KOP;
            }

            @Override
            public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
                GidsTekst.passend(g, tekst.copy().withStyle(ChatFormatting.BOLD), x + 2, y + 4, w - 4, 0.875f, ROZE, false);
                g.fill(x + 2, y + KOP - 2, x + w - 2, y + KOP - 1, 0x60D27A9C);
            }
        };
    }

    /** One questline in the list. */
    private final class Rij implements GidsLijst.Regel {
        private final VerhaalStand v;
        private final ItemStack icoon;

        Rij(VerhaalStand v) {
            this.v = v;
            this.icoon = stack(v.icoon());
        }

        @Override
        public int hoogte() {
            return RIJ;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x + 1, y + 1, x + w - 1, y + RIJ - 1, hover ? 0x40F7B6CB : 0x18F7B6CB);
            g.item(icoon, x + 5, y + 6);
            if (v.klaar()) {
                GidsTekst.schaal(g, Component.literal("✔"), x + 16, y + 17, 0.75f, GROEN, false);
            }
            int tx = x + 26, bar = 44, tw = w - 26 - bar - 10;
            GidsTekst.passend(g, v.naam().copy().withStyle(ChatFormatting.BOLD), tx, y + 4, tw, 1f, v.status() == VerhaalStand.Status.NIET_BEGONNEN ? GRIJS : DONKER, false);
            GidsTekst.passend(g, statusRegel(v), tx, y + 16, tw, 0.75f, kleur(v), false);
            GidsTekst.balk(g, x + w - bar - 6, y + 11, bar, 5, v.stap() / (float) Math.max(1, v.stappen()));
            GidsTekst.schaal(g, Component.literal(v.stap() + "/" + v.stappen()), x + w - 6, y + 19, 0.5f, LICHT, true);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            lijstScroll = lijst.scroll();
            open(v.id());
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.2f));
            herbouw.run();
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> tip = new ArrayList<>();
            tip.add(v.naam().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            tip.add(v.nu().copy().withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("gui.guhs.verhalen.klik_rij").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return tip;
        }
    }

    /** "Nog niet begonnen" / "Stap 3 van 7: ..." / "Klaar!". */
    public static Component statusRegel(VerhaalStand v) {
        return switch (v.status()) {
            case NIET_BEGONNEN -> Component.translatable("gui.guhs.verhalen.status.niet_begonnen");
            case KLAAR -> Component.translatable("gui.guhs.verhalen.status.klaar");
            case BEZIG -> Component.translatable("gui.guhs.verhalen.status.stap", v.stap() + 1, v.stappen(), v.stapNaam(v.stap()));
        };
    }

    private static int kleur(VerhaalStand v) {
        return switch (v.status()) {
            case NIET_BEGONNEN -> GRIJS;
            case KLAAR -> GROEN;
            case BEZIG -> ROZE;
        };
    }

    // =====================================================================================================================
    // a questline's page
    // =====================================================================================================================

    private List<GidsLijst.Regel> pagina(VerhaalStand v) {
        List<GidsLijst.Regel> out = new ArrayList<>();
        out.add(new Kopregel(v));
        out.add(tekst(v.uitleg().copy().withStyle(ChatFormatting.ITALIC), 0.75f, LICHT, 4));
        out.add(kop(Component.translatable(v.klaar() ? "gui.guhs.verhalen.kop.klaar" : "gui.guhs.verhalen.kop.nu")));
        out.add(tekst(v.nu(), 0.875f, DONKER, 4));
        out.add(tekst(Component.literal("➜ ").append(v.waar()), 0.75f, ROZE, 4));
        if (!v.nodig().isEmpty()) {
            out.add(kop(Component.translatable("gui.guhs.verhalen.kop.nodig")));
            for (VerhaalStand.Nodig n : v.nodig()) {
                out.add(new Ding(stack(n.item()), n.naam(), Component.literal(n.heb() + " / " + n.nodig()), n.genoeg()));
            }
        }
        out.add(kop(Component.translatable("gui.guhs.verhalen.kop.stappen")));
        for (int i = 0; i < v.stappen(); i++) {
            boolean gedaan = i < v.stap(), nu = i == v.stap() && v.status() != VerhaalStand.Status.NIET_BEGONNEN;
            String teken = gedaan ? "✔ " : nu ? "➜ " : "○ ";
            int kleur = gedaan ? GROEN : nu ? ROZE : GRIJS;
            out.add(tekst(Component.literal(teken).append(v.stapNaam(i)), 0.75f, kleur, 6));
        }
        if (!v.beloningen().isEmpty()) {
            out.add(kop(Component.translatable("gui.guhs.verhalen.kop.beloningen")));
            for (VerhaalStand.Beloning b : v.beloningen()) {
                out.add(new Ding(stack(b.item()), b.tekst(), Component.translatable(b.binnen() ? "gui.guhs.verhalen.binnen" : "gui.guhs.verhalen.nog_niet"),
                        b.binnen()));
            }
        }
        return out;
    }

    /** Wrapped text at a scale, indented. */
    private GidsLijst.Regel tekst(Component c, float scale, int kleur, int in) {
        return new GidsLijst.Regel() {
            @Override
            public int hoogte() {
                return GidsTekst.hoogte(c, lijst.rijBreedte() - in - 4, scale) + 3;
            }

            @Override
            public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
                GidsTekst.alinea(g, c, x + in, y + 2, w - in - 4, scale, kleur);
            }
        };
    }

    /** The top of a page: big icon, status, bar. */
    private static final class Kopregel implements GidsLijst.Regel {
        private final VerhaalStand v;
        private final ItemStack icoon;

        Kopregel(VerhaalStand v) {
            this.v = v;
            this.icoon = stack(v.icoon());
        }

        @Override
        public int hoogte() {
            return 24;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x + 1, y + 1, x + w - 1, y + 23, 0x20F7B6CB);
            g.item(icoon, x + 4, y + 4);
            int tx = x + 26, bar = 60;
            GidsTekst.passend(g, statusRegel(v), tx, y + 5, w - 26 - bar - 12, 0.875f, kleur(v), false);
            GidsTekst.passend(g, Component.translatable("gui.guhs.verhalen.stappen_telling", v.stap(), v.stappen()), tx, y + 15, w - 26 - bar - 12, 0.625f, LICHT, false);
            GidsTekst.balk(g, x + w - bar - 6, y + 9, bar, 6, v.stap() / (float) Math.max(1, v.stappen()));
        }
    }

    /** An item row: icon, name, and on the right a count or "binnen" (green when done). */
    private static final class Ding implements GidsLijst.Regel {
        private final ItemStack icoon;
        private final Component naam, rechts;
        private final boolean ok;

        Ding(ItemStack icoon, Component naam, Component rechts, boolean ok) {
            this.icoon = icoon;
            this.naam = naam;
            this.rechts = (ok ? Component.literal("✔ ") : Component.empty()).append(rechts);
            this.ok = ok;
        }

        @Override
        public int hoogte() {
            return 18;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.item(icoon, x + 6, y + 1);
            int rw = Math.round(GidsTekst.font().width(rechts) * 0.75f);
            GidsTekst.passend(g, naam, x + 26, y + 5, w - 26 - rw - 12, 0.875f, ok ? TEKST : DONKER, false);
            GidsTekst.schaal(g, rechts, x + w - 6, y + 6, 0.75f, ok ? GROEN : ROZE, true);
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return mx < x + 24 ? List.of(icoon.getHoverName()) : null;
        }
    }

    static ItemStack stack(String id) {
        var item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(id) == null ? Identifier.withDefaultNamespace("book") : Identifier.tryParse(id));
        return new ItemStack(item == Items.AIR ? Items.BOOK : item);
    }
}
