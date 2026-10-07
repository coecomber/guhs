package nl.juiced.guhs.feature.guhpad.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.gids.client.GidsVerhalenTab;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.Wereld;
import nl.juiced.guhs.feature.guhpad.GuhpadPayloads;
import nl.juiced.guhs.feature.guhpad.GuhpadPayloads.Stand;
import nl.juiced.guhs.feature.verhaal.Reiskaart;
import nl.juiced.guhs.feature.verhaal.Reiskaarten;

/**
 * Het Guhpad in the Guhdex (DESIGN_VERHALENPAD A4 and A6): the layout of the tab Verhalen. Nothing changes in the tab bar;
 * the list of the tab becomes:
 * <ol>
 *   <li>the switch of the objective line (as before);</li>
 *   <li>the path map of the whole Guhpad ({@link Padkaart}): the four stops with a tick where every big story is done, a
 *       lock where you may not go yet, a pip per big story, the counter "Verhalen gevolgd: n van m"; hover a stop for what
 *       it still asks, click it to jump to its stories;</li>
 *   <li>the stories per world, each world a heading you can fold open and shut ({@link WereldKop}): the Guhmensie, the
 *       Guhbarbecuether, the Guheinde, and "Het echte Guheinde": a preview that is locked for everybody ({@link #echt}:
 *       silhouettes, riddles, the counter, and the question-mark row of layer 6 of the Guh-technologie);</li>
 *   <li>the scenes and cards without a questline (as before).</li>
 * </ol>
 * The rows of a questline are the tab's own ({@code GidsVerhalenTab.rij}); a big story gets a gold star. Data:
 * {@link GuhpadPayloads.Client}; pictures: textures/gui/guhpad/padkaart.png and echt.png (tools/features/guhpad_tex.py).
 */
public final class GuhpadTab implements GidsVerhalenTab.Indeling {
    static final Identifier PADKAART = Guhs.id("textures/gui/guhpad/padkaart.png"), ECHT = Guhs.id("textures/gui/guhpad/echt.png");
    /** The pictures' sizes; the four stops' pixels on the path map (the same numbers as in guhpad_tex.py). */
    static final int KAART_W = 256, KAART_H = 76, ECHT_W = 256, ECHT_H = 48;
    static final int[] HALTE_X = {34, 98, 162, 226}, HALTE_Y = {30, 24, 30, 24};
    /** The colours of the Verhalen tab. */
    static final int TEKST = 0xFF5A3A4A, DONKER = 0xFF3A1C30, ROZE = 0xFF7A2848, LICHT = 0xFFB0708A, GROEN = 0xFF3E9A5A, GRIJS = 0xFF9A8090,
            GOUD = 0xFFB8862A, NACHT = 0xFF1A1020;
    /** How many question marks the row of layer 6 has. */
    static final int LAAG6 = 5;
    /** What the player folded open (false) or shut (true) themselves; remembered while the game runs. */
    private static final Map<Wereld, Boolean> GEVOUWEN = new EnumMap<>(Wereld.class);

    /** Is this world's part of the list folded shut? By itself: the Guhmensie and every open world show, the rest is shut. */
    static boolean gevouwen(Wereld w, Stand stand) {
        Boolean zelf = GEVOUWEN.get(w);
        if (zelf != null) {
            return zelf;
        }
        return w != Wereld.GUHMENSIE && (stand.eisen().isEmpty() || !stand.open(w));
    }

    /** (AutoCheck) folds a world open or shut (null: by itself again). */
    public static void vouw(Wereld w, @Nullable Boolean dicht) {
        if (dicht == null) {
            GEVOUWEN.remove(w);
        } else {
            GEVOUWEN.put(w, dicht);
        }
    }

    /** Every big story of this world is finished (and it has at least one). */
    static boolean allesGedaan(Stand stand, Wereld w) {
        List<GuhpadPayloads.Verhaal> v = stand.verhalen(w);
        return !v.isEmpty() && v.stream().allMatch(GuhpadPayloads.Verhaal::klaar);
    }

    /** The stop where the player is now: the last world that is open for them. */
    static Wereld hier(Stand stand) {
        Wereld hier = Wereld.GUHMENSIE;
        for (Wereld w : Wereld.values()) {
            if (!stand.eisen().isEmpty() && stand.open(w)) {
                hier = w;
            }
        }
        return hier;
    }

    @Override
    public List<GidsLijst.Regel> lijst(GidsVerhalenTab tab, List<VerhaalStand> alle) {
        Stand stand = GuhpadPayloads.Client.stand();
        List<GidsLijst.Regel> out = new ArrayList<>();
        out.add(tab.doelSchakelaar());
        out.add(new Padkaart(tab, stand));
        for (Wereld w : Wereld.values()) {
            List<VerhaalStand> lijnen = alle.stream().filter(v -> GroteVerhalen.wereldVan(v.id()) == w).toList();
            out.add(new WereldKop(tab, w, stand));
            if (gevouwen(w, stand)) {
                continue;
            }
            if (w == Wereld.ECHT) {
                out.addAll(echt(tab, stand));
                continue;
            }
            List<GuhpadPayloads.Eis> mist = stand.ontbreekt(w);
            if (!mist.isEmpty()) {
                out.add(tab.tekst(Component.translatable("gui.guhs.guhpad.nog_nodig", lijst(mist)), 0.75f, ROZE, 6));
            }
            boolean kopjes = lijnen.stream().map(v -> GidsVerhalenTab.groepVan(v.id())).distinct().count() > 1;
            String kop = null;
            for (VerhaalStand v : lijnen) {
                String groep = GidsVerhalenTab.groepVan(v.id());
                if (!groep.equals(kop)) {
                    kop = groep;
                    if (kopjes) {
                        out.add(GidsVerhalenTab.kop(Component.translatable("gui.guhs.verhalen.kop." + groep)));
                    }
                    Reiskaart k = Reiskaarten.vanGroep(groep);
                    if (k != null) {
                        out.add(tab.kaartRij(k));
                    }
                }
                GidsLijst.Regel rij = tab.rij(v);
                out.add(stand.isGroot(v.id()) ? new Groot(rij) : rij);
            }
            if (lijnen.isEmpty()) {
                out.add(tab.tekst(Component.translatable("gui.guhs.guhpad.leeg").withStyle(ChatFormatting.ITALIC), 0.75f, GRIJS, 6));
            }
        }
        out.addAll(tab.losseHerbekijk());
        return out;
    }

    /** "A, B en C" of what a world still asks. */
    static MutableComponent lijst(List<GuhpadPayloads.Eis> eisen) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < eisen.size(); i++) {
            if (i > 0) {
                out.append(Component.translatable(i == eisen.size() - 1 ? "gui.guhs.guhpad.lijst.en" : "gui.guhs.guhpad.lijst.komma"));
            }
            out.append(Component.translatable(eisen.get(i).sleutel()));
        }
        return out;
    }

    /** The tooltip of a world: its name, open or locked, its big stories ticked, and what it still asks. */
    static List<Component> uitleg(Stand stand, Wereld w) {
        List<Component> tip = new ArrayList<>();
        tip.add(w.naam().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        tip.add(Component.translatable("gui.guhs.guhpad.wereld." + w.id() + ".uitleg").withStyle(ChatFormatting.GRAY));
        if (stand.eisen().isEmpty()) {
            return tip;
        }
        if (w == Wereld.ECHT) {
            tip.add(Component.translatable("gui.guhs.guhpad.slot.echt").withStyle(ChatFormatting.DARK_PURPLE));
            tip.add(Component.translatable("gui.guhs.guhpad.teller", stand.gevolgd(), stand.totaal()).withStyle(ChatFormatting.GOLD));
            return tip;
        }
        for (GuhpadPayloads.Verhaal v : stand.verhalen(w)) {
            tip.add(Component.literal(v.klaar() ? "✔ " : "○ ").append(Component.translatable(v.naamSleutel()))
                    .withStyle(v.klaar() ? ChatFormatting.GREEN : ChatFormatting.WHITE));
        }
        List<GuhpadPayloads.Eis> mist = stand.ontbreekt(w);
        if (mist.isEmpty()) {
            tip.add(Component.translatable("gui.guhs.guhpad.slot.open").withStyle(ChatFormatting.GREEN));
        } else {
            tip.add(Component.translatable("gui.guhs.guhpad.slot.dicht").withStyle(ChatFormatting.GOLD));
            for (GuhpadPayloads.Eis e : mist) {
                tip.add(Component.literal("  ○ ").append(Component.translatable(e.sleutel())).withStyle(ChatFormatting.GOLD));
            }
        }
        return tip;
    }

    static Font font() {
        return Minecraft.getInstance().font;
    }

    static ItemStack stack(String id) {
        Identifier key = Identifier.tryParse(id);
        var item = key == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(key);
        return new ItemStack(item == Items.AIR ? Items.BOOK : item);
    }

    /** A little padlock, 7 x 8 pixels. */
    static void slotje(GuiGraphicsExtractor g, int x, int y, int kleur, int oog) {
        g.fill(x + 1, y, x + 6, y + 1, kleur);
        g.fill(x + 1, y + 1, x + 2, y + 4, kleur);
        g.fill(x + 5, y + 1, x + 6, y + 4, kleur);
        g.fill(x, y + 3, x + 7, y + 8, kleur);
        g.fill(x + 3, y + 5, x + 4, y + 7, oog);
    }

    /** Text centred on cx at a scale. */
    static void midden(GuiGraphicsExtractor g, Component c, int cx, int y, float scale, int kleur) {
        GidsTekst.schaal(g, c, cx - Math.round(font().width(c) * scale / 2f), y, scale, kleur, false);
    }

    // =====================================================================================================================
    // the path map
    // =====================================================================================================================

    /** The path map of the whole Guhpad. */
    final class Padkaart implements GidsLijst.Regel {
        private final GidsVerhalenTab tab;
        private final Stand stand;
        private final ItemStack[] iconen = new ItemStack[Wereld.values().length];

        Padkaart(GidsVerhalenTab tab, Stand stand) {
            this.tab = tab;
            this.stand = stand;
            for (Wereld w : Wereld.values()) {
                iconen[w.ordinal()] = stack(w.icoon());
            }
        }

        @Override
        public int hoogte() {
            return KAART_H + 8;
        }

        private int kx(int x, int w) {
            return x + (w - KAART_W) / 2;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            int kx = kx(x, w), ky = y + 4;
            g.fill(kx - 2, ky - 2, kx + KAART_W + 2, ky + KAART_H + 2, ROZE);
            g.blit(RenderPipelines.GUI_TEXTURED, PADKAART, kx, ky, 0f, 0f, KAART_W, KAART_H, KAART_W, KAART_H, KAART_W, KAART_H);
            boolean bekend = !stand.eisen().isEmpty();
            // the title and the counter, on the sky of the map
            GidsTekst.schaal(g, Component.translatable("gui.guhs.guhpad.titel").withStyle(ChatFormatting.BOLD), kx + 6, ky + 4, 0.75f, DONKER, false);
            if (bekend) {
                GidsTekst.schaal(g, Component.translatable("gui.guhs.guhpad.teller", stand.gevolgd(), stand.totaal()), kx + KAART_W - 6, ky + 5,
                        0.625f, ROZE, true);
            }
            // the path: dots from stop to stop, gold as far as you may go
            Wereld[] werelden = Wereld.values();
            for (int i = 0; i + 1 < werelden.length; i++) {
                boolean gegaan = bekend && stand.open(werelden[i + 1]);
                double ax = kx + HALTE_X[i], ay = ky + HALTE_Y[i], bx = kx + HALTE_X[i + 1], by = ky + HALTE_Y[i + 1];
                int n = Math.max(2, (int) (Math.hypot(bx - ax, by - ay) / 5));
                for (int j = 2; j < n - 1; j++) {
                    int px = (int) Math.round(Mth.lerp(j / (double) n, ax, bx)), py = (int) Math.round(Mth.lerp(j / (double) n, ay, by));
                    g.fill(px - 1, py - 1, px + 1, py + 1, gegaan ? 0xFFB8862A : 0xB0705848);
                }
            }
            long tijd = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
            Wereld hier = hier(stand);
            for (Wereld wereld : werelden) {
                int i = wereld.ordinal(), px = kx + HALTE_X[i], py = ky + HALTE_Y[i];
                boolean open = bekend && stand.open(wereld), gedaan = allesGedaan(stand, wereld), nu = bekend && wereld == hier;
                int r = nu ? 8 + (int) (tijd / 8 % 2) : 8;
                g.fill(px - r, py - r, px + r, py + r, nu ? 0xFFF7D27A : DONKER);
                g.fill(px - r + 1, py - r + 1, px + r - 1, py + r - 1, wereld == Wereld.ECHT ? NACHT : gedaan ? 0xFF8AD0A0 : open ? 0xFFF8E4EC : 0xFFB8A8B0);
                if (wereld == Wereld.ECHT) {
                    midden(g, Component.literal("?").withStyle(ChatFormatting.BOLD), px, py - 4, 1f, 0xFFB090E0);
                } else {
                    g.pose().pushMatrix();
                    g.pose().translate(px - 6, py - 6);
                    g.pose().scale(0.75f, 0.75f);
                    g.item(iconen[i], 0, 0);
                    g.pose().popMatrix();
                }
                if (gedaan && open) {
                    g.fill(px + 3, py + 2, px + 10, py + 10, GROEN);
                    GidsTekst.schaal(g, Component.literal("✔"), px + 4, py + 3, 0.75f, 0xFFFFFFFF, false);
                } else if (bekend && !open) {
                    g.fill(px + 2, py + 1, px + 11, py + 11, DONKER);
                    slotje(g, px + 3, py + 2, 0xFFF7D27A, DONKER);
                }
                // the name on a little plate, the pips of its big stories under it
                Component naam = Component.translatable("gui.guhs.guhpad.wereld." + wereld.id() + ".kort");
                int nw = Math.round(font().width(naam) * 0.5f);
                g.fill(px - nw / 2 - 2, py + r + 1, px + nw / 2 + 2, py + r + 8, 0xD0FBEBC4);
                midden(g, naam, px, py + r + 2, 0.5f, DONKER);
                List<GuhpadPayloads.Verhaal> verhalen = stand.verhalen(wereld);
                int pw = verhalen.size() * 5 - 1, p0 = px - pw / 2;
                for (int j = 0; j < verhalen.size(); j++) {
                    g.fill(p0 + j * 5 - 1, py + r + 9, p0 + j * 5 + 4, py + r + 14, DONKER);
                    g.fill(p0 + j * 5, py + r + 10, p0 + j * 5 + 3, py + r + 13, verhalen.get(j).klaar() ? 0xFF68D88A : 0xFFD8C8CC);
                }
            }
        }

        @Nullable
        private Wereld onder(double mx, double my, int x, int y, int w) {
            int kx = kx(x, w), ky = y + 4;
            for (Wereld wereld : Wereld.values()) {
                int px = kx + HALTE_X[wereld.ordinal()], py = ky + HALTE_Y[wereld.ordinal()];
                if (Math.abs(mx - px) <= 14 && my >= py - 10 && my <= py + 24) {
                    return wereld;
                }
            }
            return null;
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            Wereld wereld = onder(mx, my, x, y, w);
            if (wereld == null) {
                return false;
            }
            GEVOUWEN.put(wereld, false);
            klikGeluid();
            tab.verbouw();
            tab.scrollNaarRegel(r -> r instanceof WereldKop k && k.wereld == wereld);
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            Wereld wereld = onder(mx, my, x, y, w);
            if (wereld == null) {
                return List.of(Component.translatable("gui.guhs.guhpad.titel").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                        Component.translatable("gui.guhs.guhpad.uitleg").withStyle(ChatFormatting.GRAY));
            }
            List<Component> tip = uitleg(stand, wereld);
            if (!stand.eisen().isEmpty() && wereld == hier(stand)) {
                tip.add(Component.translatable("gui.guhs.guhpad.hier").withStyle(ChatFormatting.YELLOW));
            }
            tip.add(Component.translatable("gui.guhs.guhpad.klik_halte").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return tip;
        }
    }

    // =====================================================================================================================
    // a world's heading
    // =====================================================================================================================

    /** The heading of a world: click to fold its stories open or shut. */
    final class WereldKop implements GidsLijst.Regel {
        private final GidsVerhalenTab tab;
        final Wereld wereld;
        private final Stand stand;
        private final ItemStack icoon;

        WereldKop(GidsVerhalenTab tab, Wereld wereld, Stand stand) {
            this.tab = tab;
            this.wereld = wereld;
            this.stand = stand;
            this.icoon = stack(wereld.icoon());
        }

        @Override
        public int hoogte() {
            return 22;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            boolean bekend = !stand.eisen().isEmpty(), open = bekend && stand.open(wereld), echt = wereld == Wereld.ECHT;
            g.fill(x + 1, y + 2, x + w - 1, y + 21, echt ? (hover ? 0xFF3A2850 : 0xFF2A1C3A) : hover ? 0x70D27A9C : 0x48D27A9C);
            g.fill(x + 1, y + 2, x + w - 1, y + 3, 0x30FFFFFF);
            g.fill(x + 1, y + 20, x + w - 1, y + 21, echt ? 0xFF6A4E9A : 0xFF7A2848);
            int kleur = echt ? 0xFFD8C8F0 : DONKER;
            GidsTekst.schaal(g, Component.literal(gevouwen(wereld, stand) ? "▶" : "▼"), x + 5, y + 8, 0.75f, kleur, false);
            if (echt) {
                midden(g, Component.literal("?").withStyle(ChatFormatting.BOLD), x + 22, y + 7, 1f, 0xFFB090E0);
            } else {
                g.item(icoon, x + 14, y + 3);
            }
            // on the right: locked, or how many of its big stories are done
            int rechts = x + w - 6;
            if (bekend && !open) {
                slotje(g, rechts - 7, y + 7, echt ? 0xFFB090E0 : ROZE, echt ? 0xFF2A1C3A : 0xFFF3C6D6);
                Component slot = Component.translatable("gui.guhs.guhpad.op_slot");
                GidsTekst.schaal(g, slot, rechts - 10, y + 9, 0.625f, echt ? 0xFFB090E0 : ROZE, true);
                rechts -= 14 + Math.round(font().width(slot) * 0.625f);
            } else if (bekend) {
                List<GuhpadPayloads.Verhaal> verhalen = stand.verhalen(wereld);
                long klaar = verhalen.stream().filter(GuhpadPayloads.Verhaal::klaar).count();
                // (a world without big stories of its own yet, the Guheinde: just "open")
                Component telling = verhalen.isEmpty() ? Component.translatable("gui.guhs.guhpad.open")
                        : Component.literal((allesGedaan(stand, wereld) ? "✔ " : "") + klaar + " / " + verhalen.size() + " ★");
                GidsTekst.schaal(g, telling, rechts, y + 8, 0.75f, allesGedaan(stand, wereld) || verhalen.isEmpty() ? GROEN : GOUD, true);
                rechts -= 6 + Math.round(font().width(telling) * 0.75f);
            }
            GidsTekst.passend(g, wereld.naam().withStyle(ChatFormatting.BOLD), x + 34, y + 7, rechts - x - 36, 1f, kleur, false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            GEVOUWEN.put(wereld, !gevouwen(wereld, stand));
            klikGeluid();
            tab.verbouw();
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> tip = uitleg(stand, wereld);
            tip.add(Component.translatable(gevouwen(wereld, stand) ? "gui.guhs.guhpad.vouw_open" : "gui.guhs.guhpad.vouw_dicht")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return tip;
        }
    }

    /** A questline's own row with the gold star of a big story. */
    private static final class Groot implements GidsLijst.Regel {
        private final GidsLijst.Regel rij;

        Groot(GidsLijst.Regel rij) {
            this.rij = rij;
        }

        @Override
        public int hoogte() {
            return rij.hoogte();
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            rij.teken(g, x, y, w, mouseX, mouseY, hover);
            GidsTekst.schaal(g, Component.literal("★"), x + w - 6, y + 2, 0.75f, GOUD, true);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            return rij.klik(mx, my, x, y, w);
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> eigen = rij.tip(mx, my, x, y, w);
            List<Component> tip = new ArrayList<>(eigen == null ? List.of() : eigen);
            tip.add(Component.translatable("gui.guhs.guhpad.groot").withStyle(ChatFormatting.GOLD));
            return tip;
        }
    }

    // =====================================================================================================================
    // Het echte Guheinde: a preview, locked for everybody
    // =====================================================================================================================

    /** The rows of "Het echte Guheinde": no real content, no spoilers. */
    List<GidsLijst.Regel> echt(GidsVerhalenTab tab, Stand stand) {
        List<GidsLijst.Regel> out = new ArrayList<>();
        out.add(new EchtPlaat());
        for (int i = 1; i <= 3; i++) {
            out.add(tab.tekst(Component.translatable("gui.guhs.guhpad.echt.raadsel." + i).withStyle(ChatFormatting.ITALIC), 0.75f, 0xFF6A4E9A, 8));
        }
        out.add(new Teller(stand));
        out.add(tab.tekst(Component.translatable("gui.guhs.guhpad.echt.bekend"), 0.75f, TEKST, 6));
        out.add(new Laag6());
        return out;
    }

    /** The picture: black silhouettes and question marks. */
    private static final class EchtPlaat implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return ECHT_H + 8;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            int kx = x + (w - ECHT_W) / 2, ky = y + 4;
            g.fill(kx - 2, ky - 2, kx + ECHT_W + 2, ky + ECHT_H + 2, 0xFF6A4E9A);
            g.blit(RenderPipelines.GUI_TEXTURED, ECHT, kx, ky, 0f, 0f, ECHT_W, ECHT_H, ECHT_W, ECHT_H, ECHT_W, ECHT_H);
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(Component.translatable("gui.guhs.guhpad.slot.echt").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /** The ONE counter: "Verhalen gevolgd: n van m". */
    private static final class Teller implements GidsLijst.Regel {
        private final Stand stand;

        Teller(Stand stand) {
            this.stand = stand;
        }

        @Override
        public int hoogte() {
            return 22;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x + 4, y + 2, x + w - 4, y + 20, 0xFF2A1C3A);
            g.fill(x + 4, y + 2, x + w - 4, y + 3, 0xFF6A4E9A);
            Component c = Component.translatable("gui.guhs.guhpad.teller", stand.gevolgd(), stand.totaal()).withStyle(ChatFormatting.BOLD);
            int bar = 70;
            GidsTekst.passend(g, c, x + 10, y + 7, w - bar - 30, 1f, 0xFFF7D27A, false);
            GidsTekst.balk(g, x + w - bar - 12, y + 8, bar, 6, stand.totaal() == 0 ? 0f : stand.gevolgd() / (float) stand.totaal());
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.guhs.guhpad.teller.uitleg").withStyle(ChatFormatting.GRAY));
            for (GuhpadPayloads.Verhaal v : stand.verhalen()) {
                tip.add(Component.literal(v.klaar() ? "✔ " : "○ ").append(Component.translatable(v.naamSleutel()))
                        .withStyle(v.klaar() ? ChatFormatting.GREEN : ChatFormatting.WHITE));
            }
            return tip;
        }
    }

    /** The row of layer 6 of the Guh-technologie: question marks only. */
    private static final class Laag6 implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 50;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.guhpad.echt.laag6").withStyle(ChatFormatting.BOLD), x + 6, y + 4, w - 12, 0.875f, 0xFF6A4E9A, false);
            int vak = 20, gat = 8, breed = LAAG6 * vak + (LAAG6 - 1) * gat, x0 = x + (w - breed) / 2, y0 = y + 15;
            for (int i = 0; i < LAAG6; i++) {
                int vx = x0 + i * (vak + gat);
                g.fill(vx, y0, vx + vak, y0 + vak, NACHT);
                for (int d = 0; d < vak; d += 4) {   // a dashed edge: nothing here yet
                    g.fill(vx + d, y0, vx + d + 2, y0 + 1, 0xFF8A6EC0);
                    g.fill(vx + d, y0 + vak - 1, vx + d + 2, y0 + vak, 0xFF8A6EC0);
                    g.fill(vx, y0 + d, vx + 1, y0 + d + 2, 0xFF8A6EC0);
                    g.fill(vx + vak - 1, y0 + d, vx + vak, y0 + d + 2, 0xFF8A6EC0);
                }
                midden(g, Component.literal("?").withStyle(ChatFormatting.BOLD), vx + vak / 2, y0 + 6, 1f, 0xFFB090E0);
                if (i + 1 < LAAG6) {
                    g.fill(vx + vak + 2, y0 + vak / 2, vx + vak + gat - 2, y0 + vak / 2 + 1, 0xFF8A6EC0);
                }
            }
            midden(g, Component.translatable("gui.guhs.guhpad.echt.laag6.tekst"), x + w / 2, y0 + vak + 4, 0.625f, TEKST);
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(Component.translatable("gui.guhs.guhpad.echt.laag6").withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.translatable("gui.guhs.guhpad.echt.laag6.tekst").withStyle(ChatFormatting.GRAY));
        }
    }

    static void klikGeluid() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.2f));
    }
}
