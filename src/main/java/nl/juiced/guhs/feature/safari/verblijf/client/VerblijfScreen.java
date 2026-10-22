package nl.juiced.guhs.feature.safari.verblijf.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfFeature;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfPayloads;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfPayloads.Actie;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfWinkel;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfsbordBlockEntity;

/**
 * Op Safari! step 2 (S5): the verblijfsbord's menu (design 1.9): tabs Familie (names, mood, call back, hide), Verblijf (range
 * 8..32, circle or rectangle, show the edge, sleeping spot), Tevredenheid (the bar and the checklist, "Bouw passend verblijf"), Cadeautjes, Info (a fact, your best photo, the cooldown), Bouwopdracht (the family's
 * jobs, deel 2 F10b) and, for the stork, Bezorgen. Someone else's board: look only.
 */
public class VerblijfScreen extends Screen {
    private static final int W = 320, H = 222;
    private static final int PANEEL = 0xF0FFF8EE, RAND = 0xFFC9A46A, KOP = 0xFFF6E7C8, DONKER = 0xFF4A3420, TEKST = 0xFF5A4630,
            LICHT = 0xFF9A8060, GROEN = 0xFF4E8A3A, ROOD = 0xFFB0484A;

    private enum Tab { FAMILIE, VERBLIJF, TEVREDEN, CADEAUTJES, INFO, BOUW, BEZORGEN }

    private CompoundTag data;
    private BlockPos pos;
    private Tab tab = Tab.FAMILIE;
    private int left, top;
    private final List<EditBox> namen = new ArrayList<>();
    private int vriend;

    public VerblijfScreen(CompoundTag data) {
        super(Component.translatable("gui.guhs.verblijf.titel", Component.translatable("entity.guhs.safari." + data.getStringOr("Soort", ""))));
        this.data = data;
        this.pos = BlockPos.of(data.getLongOr("Pos", 0L));
    }

    public BlockPos pos() {
        return pos;
    }

    /** New state from the server: rebuild, unless a slider or a name box is in use (their values stay). */
    public void update(CompoundTag nieuw) {
        this.data = nieuw;
        for (var w : children()) {
            if (w instanceof AbstractSliderButton s && s.isFocused() || w instanceof EditBox e && e.isFocused()) {
                return;
            }
        }
        rebuildWidgets();
    }

    private boolean mag() {
        return data.getBooleanOr("Mag", false);
    }

    private void doe(Actie a, int waarde, String tekst) {
        ClientPacketDistributor.sendToServer(new VerblijfPayloads.Doe(pos, a.ordinal(), waarde, tekst));
    }

    private List<Tab> tabs() {
        List<Tab> t = new ArrayList<>(List.of(Tab.FAMILIE, Tab.VERBLIJF, Tab.TEVREDEN, Tab.CADEAUTJES, Tab.INFO, Tab.BOUW));
        if (data.getBooleanOr("Bezorger", false)) {
            t.add(Tab.BEZORGEN);
        }
        return t;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        namen.clear();
        List<Tab> tabs = tabs();
        int tw = (W - 12) / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            Button b = Button.builder(Component.translatable("gui.guhs.verblijf.tab." + switch (t) {
                case FAMILIE -> "familie";
                case VERBLIJF -> "verblijf";
                case TEVREDEN -> "tevreden";
                case CADEAUTJES -> "cadeautjes";
                case INFO -> "info";
                case BOUW -> "bouw";
                case BEZORGEN -> "bezorgen";
            }), x -> {
                tab = t;
                rebuildWidgets();
            }).bounds(left + 6 + i * tw, top + 20, tw - 2, 16).build();
            b.active = t != tab;
            addRenderableWidget(b);
        }
        int y = top + 44;
        boolean mag = mag();
        switch (tab) {
            case FAMILIE -> {
                ListTag dieren = data.getListOrEmpty("Dieren");
                for (int i = 0; i < dieren.size(); i++) {
                    CompoundTag d = dieren.getCompoundOrEmpty(i);
                    EditBox e = new EditBox(font, left + 92, y + 2 + i * 26, 110, 16, Component.translatable("gui.guhs.verblijf.hernoem"));
                    e.setMaxLength(24);
                    e.setValue(d.getStringOr("Naam", ""));
                    e.setHint(Component.translatable("gui.guhs.verblijf." + d.getStringOr("Rol", "baby")).withStyle(ChatFormatting.GRAY));
                    e.setEditable(mag);
                    namen.add(e);
                    addRenderableWidget(e);
                    final int lid = i;
                    Button ok = Button.builder(Component.translatable("gui.guhs.verblijf.hernoem"), b -> doe(Actie.NAAM, lid, e.getValue()))
                            .bounds(left + 206, y + 2 + i * 26, 40, 16).build();
                    ok.active = mag;
                    addRenderableWidget(ok);
                }
                Button terug = Button.builder(Component.translatable("gui.guhs.verblijf.terugroepen"), b -> doe(Actie.TERUG, 0, ""))
                        .bounds(left + 10, top + H - 48, 140, 18).build();
                Button verstop = Button.builder(Component.translatable("gui.guhs.verblijf.verstoppen"), b -> doe(Actie.VERSTOP, 0, ""))
                        .bounds(left + 156, top + H - 48, 140, 18).build();
                terug.active = verstop.active = mag;
                addRenderableWidget(terug);
                addRenderableWidget(verstop);
            }
            case VERBLIJF -> {
                boolean rechthoek = data.getBooleanOr("Rechthoek", false);
                Button vorm = Button.builder(Component.translatable(rechthoek ? "gui.guhs.verblijf.vorm.rechthoek" : "gui.guhs.verblijf.vorm.cirkel"),
                        b -> doe(Actie.VORM, rechthoek ? 0 : 1, "")).bounds(left + 10, y, 140, 18).build();
                vorm.active = mag;
                addRenderableWidget(vorm);
                if (rechthoek) {
                    addRenderableWidget(new Schuif(left + 10, y + 24, 140, Actie.BREEDTE, "gui.guhs.verblijf.breedte", data.getIntOr("Breedte", 12), 4));
                    addRenderableWidget(new Schuif(left + 156, y + 24, 140, Actie.DIEPTE, "gui.guhs.verblijf.diepte", data.getIntOr("Diepte", 12), 4));
                } else {
                    addRenderableWidget(new Schuif(left + 10, y + 24, 286, Actie.STRAAL, "gui.guhs.verblijf.straal", data.getIntOr("Straal", 12),
                            VerblijfsbordBlockEntity.MIN_STRAAL));
                }
                Button rand = Button.builder(Component.translatable("gui.guhs.verblijf.rand"), b -> doe(Actie.RAND, 0, ""))
                        .bounds(left + 156, y, 140, 18).build();
                addRenderableWidget(rand);
                Button hier = Button.builder(Component.translatable("gui.guhs.verblijf.thuisplek"), b -> doe(Actie.SLAAPPLEK, 0, ""))
                        .bounds(left + 10, y + 50, 140, 18).build();
                Button weg = Button.builder(Component.translatable("gui.guhs.verblijf.thuisplek.weg"), b -> doe(Actie.SLAAPPLEK_WEG, 0, ""))
                        .bounds(left + 156, y + 50, 140, 18).build();
                hier.active = mag;
                weg.active = mag && data.contains("Slaapplek");
                addRenderableWidget(hier);
                addRenderableWidget(weg);
                for (var w : children()) {
                    if (w instanceof Schuif s) {
                        s.active = mag;
                    }
                }
            }
            case TEVREDEN -> {
                // deel 2 (F10b): the verblijf-assistent puts a fitting home down as a ghost, built by the family
                Button bouw = Button.builder(Component.translatable("gui.guhs.verblijf.tevreden.bouw"), b -> doe(Actie.BOUW_VERBLIJF, 0, ""))
                        .bounds(left + W - 156, top + H - 48, 146, 18).tooltip(Tooltip.create(Component.translatable("gui.guhs.bouwopdracht.verblijf.uitleg"))).build();
                bouw.active = mag && !data.getCompoundOrEmpty("Bouw").contains("Project");
                addRenderableWidget(bouw);
            }
            case BOUW -> bouwKnoppen(mag);
            case CADEAUTJES -> {
                Button pak = Button.builder(Component.translatable("gui.guhs.verblijf.cadeautjes.pak"), b -> doe(Actie.CADEAUS, 0, ""))
                        .bounds(left + 10, top + H - 48, 140, 18).build();
                pak.active = mag;
                addRenderableWidget(pak);
            }
            case BEZORGEN -> {
                ListTag vrienden = data.getListOrEmpty("Vrienden");
                String gekozen = data.getStringOr("Bestemming", "");
                for (int i = 0; i < vrienden.size(); i++) {
                    if (vrienden.getCompoundOrEmpty(i).getStringOr("Id", "").equals(gekozen)) {
                        vriend = i;
                    }
                }
                Component naar = vrienden.isEmpty() ? Component.translatable("gui.guhs.verblijf.bezorgen.niemand")
                        : Component.translatable("gui.guhs.verblijf.bezorgen.naar", vrienden.getCompoundOrEmpty(Math.min(vriend, vrienden.size() - 1)).getStringOr("Naam", "?"));
                Button kies = Button.builder(naar, b -> {
                    if (!vrienden.isEmpty()) {
                        vriend = (vriend + 1) % vrienden.size();
                        doe(Actie.BEZORG_KIES, 0, vrienden.getCompoundOrEmpty(vriend).getStringOr("Id", ""));
                    }
                }).bounds(left + 10, y + 40, 200, 18).build();
                kies.active = mag && !vrienden.isEmpty();
                addRenderableWidget(kies);
                Button stuur = Button.builder(Component.translatable("gui.guhs.verblijf.bezorgen.stuur"), b -> doe(Actie.BEZORG_STUUR, 0, ""))
                        .bounds(left + 10, y + 64, 200, 18).build();
                stuur.active = mag && !gekozen.isEmpty() && !data.getBooleanOr("Onderweg", false);
                addRenderableWidget(stuur);
            }
            default -> {
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.verblijf.sluit"), b -> onClose()).bounds(left + W - 56, top + H - 24, 48, 18).build());
    }

    /** Deel 2 (F10b): the Bouwopdracht tab: the tasks on the left, the chosen one's buttons and the running job on the right. */
    private void bouwKnoppen(boolean mag) {
        CompoundTag b = data.getCompoundOrEmpty("Bouw");
        ListTag taken = b.getListOrEmpty("Taken");
        if (taken.isEmpty()) {
            return;
        }
        String keuze = b.getStringOr("Keuze", "");
        int y = top + 44;
        for (int i = 0; i < taken.size(); i++) {
            String t = taken.getStringOr(i, "");
            final int index = i;
            Button k = Button.builder(Component.translatable("gui.guhs.verblijf.taak." + t), x -> doe(Actie.BOUW_KIES, index, ""))
                    .bounds(left + 10, y + i * 18, 102, 16).build();
            k.active = !t.equals(keuze);
            addRenderableWidget(k);
        }
        int x = left + 118, w = W - 128;
        CompoundTag project = b.getCompoundOrEmpty("Project");
        boolean bezig = b.contains("Project");
        if (b.getIntOr("Varianten", 1) > 1) {
            Button v = Button.builder(Component.translatable("gui.guhs.bouwopdracht.variant",
                            Component.translatable("gui.guhs.bouwopdracht.variant." + keuze + "." + b.getIntOr("Variant", 0))),
                    x0 -> doe(Actie.BOUW_VARIANT, 0, "")).bounds(x, y + 44, w, 16).build();
            v.active = mag;
            addRenderableWidget(v);
        }
        Button zoek = Button.builder(Component.translatable("gui.guhs.bouwopdracht.zoek"), x0 -> doe(Actie.BOUW_ZOEK, 0, ""))
                .bounds(x, y + 64, w / 2 - 1, 18).build();
        Button wijs = Button.builder(Component.translatable(b.getIntOr("Punten", 1) == 2 ? "gui.guhs.bouwopdracht.wijs_twee" : "gui.guhs.bouwopdracht.wijs"),
                x0 -> {
                    doe(Actie.BOUW_WIJS, 0, "");
                    onClose();
                }).bounds(x + w / 2 + 1, y + 64, w / 2 - 1, 18).tooltip(Tooltip.create(Component.translatable("gui.guhs.bouwopdracht.wijs.uitleg"))).build();
        zoek.active = mag && !bezig;
        wijs.active = mag && !bezig;
        addRenderableWidget(zoek);
        addRenderableWidget(wijs);
        if (bezig) {
            boolean spook = project.contains("Status");
            int bw = (w - 4) / 3;
            Button bedien = Button.builder(Component.translatable("gui.guhs.bouwopdracht.bedien"), x0 -> {
                doe(Actie.BOUW_BEDIEN, 0, "");
                onClose();
            }).bounds(x, y + 126, bw, 18).build();
            Button her = Button.builder(Component.translatable("gui.guhs.bouwopdracht.herbereken"), x0 -> doe(Actie.BOUW_HERBEREKEN, 0, ""))
                    .bounds(x + bw + 2, y + 126, bw, 18).tooltip(Tooltip.create(Component.translatable("gui.guhs.bouwopdracht.herbereken.uitleg"))).build();
            Button stop = Button.builder(Component.translatable("gui.guhs.bouwopdracht.stop"), x0 -> doe(Actie.BOUW_STOP, 0, ""))
                    .bounds(x + 2 * bw + 4, y + 126, bw, 18).build();
            bedien.active = mag && spook;
            her.active = mag && spook && !"BOUWT".equals(project.getStringOr("Status", ""));
            stop.active = mag;
            addRenderableWidget(bedien);
            addRenderableWidget(her);
            addRenderableWidget(stop);
        }
    }

    /** A slider for one number (the range, a width, a depth), sent when it changes. */
    private class Schuif extends AbstractSliderButton {
        private final Actie actie;
        private final String sleutel;
        private final int min;
        private int laatst;

        Schuif(int x, int y, int w, Actie actie, String sleutel, int waarde, int min) {
            super(x, y, w, 18, Component.empty(), (waarde - min) / (double) (VerblijfsbordBlockEntity.MAX_STRAAL - min));
            this.actie = actie;
            this.sleutel = sleutel;
            this.min = min;
            this.laatst = waarde;
            updateMessage();
        }

        private int getal() {
            return min + (int) Math.round(value * (VerblijfsbordBlockEntity.MAX_STRAAL - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(sleutel, getal()));
        }

        @Override
        protected void applyValue() {
            int g = getal();
            if (g != laatst) {
                laatst = g;
                doe(actie, g, "");
            }
        }
    }

    // --- drawing ----------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.fill(left, top, left + W, top + 18, KOP);
        g.fill(left, top + 18, left + W, top + 19, RAND);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 5, DONKER, false);
        Component van = Component.translatable("gui.guhs.verblijf.van", data.getStringOr("Eigenaar", "?"));
        if (!mag()) {
            van = van.copy().append(" ").append(Component.translatable("gui.guhs.verblijf.alleen_kijken").withStyle(ChatFormatting.ITALIC));
        }
        GidsTekst.passend(g, van, left + W - 8, top + 5, 150, 0.75f, LICHT, true);
        int x = left + 10, y = top + 44;
        switch (tab) {
            case FAMILIE -> familie(g, x, y);
            case VERBLIJF -> {
                Component plek = data.contains("Slaapplek") ? Component.translatable("gui.guhs.verblijf.thuisplek.is",
                        BlockPos.of(data.getLongOr("Slaapplek", 0L)).toShortString()) : Component.translatable("gui.guhs.verblijf.thuisplek.weg");
                GidsTekst.passend(g, plek, x, y + 74, W - 20, 0.75f, TEKST, false);
                GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.verblijf.uitleg"), x, y + 90, W - 20, 0.75f, LICHT);
            }
            case TEVREDEN -> tevreden(g, x, y);
            case CADEAUTJES -> cadeautjes(g, x, y);
            case INFO -> info(g, x, y);
            case BOUW -> bouw(g, x, y);
            case BEZORGEN -> {
                GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.bezorgen.uitleg"), x, y, W - 20, 0.75f, TEKST);
                if (data.getBooleanOr("Onderweg", false)) {
                    GidsTekst.passend(g, Component.translatable("gui.guhs.verblijf.bezorgen.onderweg").withStyle(ChatFormatting.ITALIC), x, y + 90, W - 20,
                            0.875f, GROEN, false);
                }
            }
        }
    }

    private void familie(GuiGraphicsExtractor g, int x, int y) {
        ListTag dieren = data.getListOrEmpty("Dieren");
        for (int i = 0; i < dieren.size(); i++) {
            CompoundTag d = dieren.getCompoundOrEmpty(i);
            int ry = y + i * 26;
            g.fill(x - 2, ry, left + W - 10, ry + 22, i % 2 == 0 ? 0x18C9A46A : 0x0CC9A46A);
            g.text(font, Component.translatable("gui.guhs.verblijf." + d.getStringOr("Rol", "baby")).withStyle(ChatFormatting.BOLD), x + 2, ry + 3, DONKER, false);
            String stemming = d.getStringOr("Stemming", "");
            Component s = d.getIntOr("Entity", -1) < 0 ? Component.translatable("gui.guhs.verblijf.weg")
                    : Component.translatable("gui.guhs.verblijf.stemming." + (stemming.isEmpty() ? "tevreden" : stemming));
            GidsTekst.schaal(g, s, x + 2, ry + 13, 0.75f, stemming.equals("mopperig") ? ROOD : LICHT, false);
            if (d.getBooleanOr("Weg", false)) {
                GidsTekst.schaal(g, Component.translatable("gui.guhs.verblijf.weg"), left + 250, ry + 7, 0.75f, LICHT, false);
            }
        }
        if (data.getBooleanOr("Rijdier", false)) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.verblijf.rijden"), x, top + H - 64, W - 20, 0.75f, GROEN, false);
        }
    }

    private void tevreden(GuiGraphicsExtractor g, int x, int y) {
        CompoundTag t = data.getCompoundOrEmpty("Tevreden");
        int score = t.getIntOr("Score", 0);
        g.text(font, Component.translatable("gui.guhs.verblijf.tevreden.balk", score).withStyle(ChatFormatting.BOLD), x, y, DONKER, false);
        GidsTekst.balk(g, x + 130, y + 1, W - 150, 7, score / 100f);
        int ry = y + 14;
        ListTag eisen = t.getListOrEmpty("Eisen");
        for (int i = 0; i < eisen.size(); i++) {
            CompoundTag e = eisen.getCompoundOrEmpty(i);
            boolean met = e.getIntOr("Aantal", 0) >= e.getIntOr("Min", 1);
            MutableComponent regel = Component.literal(met ? "✔ " : "✘ ").append(Component.translatable("gui.guhs.verblijf.eis." + e.getStringOr("Id", "")))
                    .append(Component.literal("  " + e.getIntOr("Aantal", 0) + "/" + e.getIntOr("Min", 1)).withStyle(ChatFormatting.GRAY));
            GidsTekst.passend(g, regel, x + (i % 2) * 150, ry + (i / 2) * 11, 146, 0.75f, met ? GROEN : ROOD, false);
        }
        ry += ((eisen.size() + 1) / 2) * 11 + 4;
        String speeltje = VerblijfFeature.speeltjeId(data.getStringOr("Speeltje", ""));
        Component[] extra = {Component.translatable("gui.guhs.verblijf.tevreden.ruimte"), Component.translatable("gui.guhs.verblijf.tevreden.gevoerd"),
                Component.translatable("gui.guhs.verblijf.tevreden.geaaid"),
                Component.translatable("gui.guhs.verblijf.tevreden.speeltje", Component.translatable("gui.guhs.verblijf.speeltje." + speeltje)),
                Component.translatable("gui.guhs.verblijf.tevreden.vrienden")};
        boolean[] ok = {t.getBooleanOr("Ruimte", false), t.getBooleanOr("Gevoerd", false), t.getBooleanOr("Geaaid", false),
                t.getBooleanOr("Speeltje", false), t.getBooleanOr("Vrienden", false)};
        for (int i = 0; i < extra.length; i++) {
            GidsTekst.passend(g, Component.literal(ok[i] ? "✔ " : "✘ ").append(extra[i]), x + (i % 2) * 150, ry + (i / 2) * 11, 146, 0.75f,
                    ok[i] ? GROEN : LICHT, false);
        }
        ry += 3 * 11 + 4;
        List<Component> voer = new ArrayList<>();
        for (int i = 0; i < data.getListOrEmpty("Voer").size(); i++) {
            String id = data.getListOrEmpty("Voer").getStringOr(i, "");
            BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).ifPresent(it -> voer.add(new ItemStack(it).getHoverName()));
        }
        MutableComponent lijst = Component.empty();
        for (int i = 0; i < voer.size(); i++) {
            lijst.append(i == 0 ? Component.empty() : Component.literal(", ")).append(voer.get(i));
        }
        GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.voer", lijst), x, ry, W - 20, 0.75f, TEKST);
    }

    private void cadeautjes(GuiGraphicsExtractor g, int x, int y) {
        GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.cadeautjes.uitleg"), x, y, W - 20, 0.75f, TEKST);
        ListTag c = data.getListOrEmpty("Cadeaus");
        boolean iets = false;
        for (int i = 0; i < c.size(); i++) {
            int sx = x + 40 + (i % 9) * 24, sy = y + 24;
            g.fill(sx - 1, sy - 1, sx + 19, sy + 19, 0xFFC9A46A);
            g.fill(sx, sy, sx + 18, sy + 18, 0xFFF8EBD2);
            CompoundTag s = c.getCompoundOrEmpty(i);
            String id = s.getStringOr("Id", "");
            if (!id.isEmpty()) {
                var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(id));
                if (item.isPresent()) {
                    ItemStack st = new ItemStack(item.get(), Math.max(1, s.getIntOr("N", 1)));
                    g.item(st, sx + 1, sy + 1);
                    g.itemDecorations(font, st, sx + 1, sy + 1);
                    iets = true;
                }
            }
        }
        if (!iets) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.verblijf.cadeautjes.leeg").withStyle(ChatFormatting.ITALIC), x, y + 50, W - 20, 0.875f, LICHT, false);
        }
    }

    private void info(GuiGraphicsExtractor g, int x, int y) {
        String soort = data.getStringOr("Soort", "");
        int h = GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.feit." + soort), x, y, W - 20, 0.875f, TEKST);
        int ry = y + h + 10;
        int sterren = data.getIntOr("Sterren", 0);
        Component foto = sterren <= 0 ? Component.translatable("gui.guhs.verblijf.info.geen_foto")
                : Component.translatable("gui.guhs.verblijf.info.sterren", "★".repeat(sterren) + "☆".repeat(Math.max(0, 3 - sterren)));
        GidsTekst.passend(g, foto, x, ry, W - 20, 0.875f, sterren > 0 ? GROEN : LICHT, false);
        // G1 (design 1.9, "je beste foto"): the caption of your best dex photo of this species, in a little photo frame
        // (under the cooldown line)
        CompoundTag beste = nl.juiced.guhs.feature.safari.camera.client.SafariDexTab.dexRegel(soort).getCompoundOrEmpty("Foto");
        if (sterren > 0 && !beste.isEmpty()) {
            int fy = ry + 30;
            boolean goud = beste.getBooleanOr("Goud", false);
            g.fill(x - 2, fy - 2, x + W - 18, fy + 22, goud ? 0xFFE8B830 : 0xFFC9A46A);
            g.fill(x, fy, x + W - 20, fy + 20, 0xFFF8EBD2);
            GidsTekst.alinea(g, nl.juiced.guhs.feature.safari.camera.Foto.onderschrift(beste), x + 3, fy + 2, W - 26, 0.75f, TEKST);
        }
        long cd = data.getLongOr("Cooldown", 0L);
        Component cooldown = cd <= 0 ? Component.translatable("gui.guhs.verblijf.info.cooldown.nu")
                : Component.translatable("gui.guhs.verblijf.info.cooldown", VerblijfWinkel.tijd(cd));
        GidsTekst.passend(g, cooldown, x, ry + 14, W - 20, 0.875f, TEKST, false);
    }

    private void bouw(GuiGraphicsExtractor g, int x, int y) {
        CompoundTag b = data.getCompoundOrEmpty("Bouw");
        ListTag taken = b.getListOrEmpty("Taken");
        if (taken.isEmpty()) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.bouw.geen"), x, y, W - 20, 0.875f, TEKST);
            return;
        }
        int rx = left + 118, w = W - 128;
        String keuze = b.getStringOr("Keuze", "");
        GidsTekst.passend(g, Component.translatable("gui.guhs.verblijf.taak." + keuze).withStyle(ChatFormatting.BOLD), rx, y, w, 0.875f, DONKER, false);
        GidsTekst.alinea(g, Component.translatable("gui.guhs.bouwopdracht.uitleg." + keuze), rx, y + 12, w, 0.75f, TEKST);
        int grootte = b.getIntOr("Grootte", 0);
        GidsTekst.passend(g, Component.translatable("gui.guhs.bouwopdracht.grootte." + grootte), rx, y + 86, w, 0.75f, grootte == 2 ? GROEN : LICHT, false);
        CompoundTag p = b.getCompoundOrEmpty("Project");
        if (b.contains("Project")) {
            String taak = p.getStringOr("Taak", "");
            Component naam = Component.translatable(taak.equals("verblijf") ? "gui.guhs.verblijf.tevreden.bouw" : "gui.guhs.verblijf.taak." + taak);
            Component status;
            if (!p.contains("Status")) {
                status = Component.translatable("gui.guhs.bouwopdracht.loopt." + p.getStringOr("Soort", "").toLowerCase(java.util.Locale.ROOT), naam);
            } else if (p.getBooleanOr("Lopen", false)) {
                status = Component.translatable("gui.guhs.bouwopdracht.onderweg", naam);
            } else {
                status = Component.translatable("gui.guhs.bouwopdracht.status." + p.getStringOr("Status", "SPOOK").toLowerCase(java.util.Locale.ROOT), naam);
            }
            GidsTekst.passend(g, status, rx, y + 98, w, 0.75f, DONKER, false);
            if (p.contains("Totaal")) {
                float f = p.getIntOr("Gedaan", 0) / (float) Math.max(1, p.getIntOr("Totaal", 1));
                GidsTekst.balk(g, rx, y + 110, w, 7, f);
                if ("BOUWT".equals(p.getStringOr("Status", ""))) {
                    GidsTekst.passend(g, Component.translatable(net.minecraft.locale.Language.getInstance().has("gui.guhs.bouwopdracht.werk." + data.getStringOr("Soort", "")) ? "gui.guhs.bouwopdracht.werk." + data.getStringOr("Soort", "") : "gui.guhs.bouwopdracht.werk.anders").withStyle(ChatFormatting.ITALIC),
                            rx, y + 119, w, 0.625f, LICHT, false);
                }
            }
        }
        Component voet = Component.translatable(b.getBooleanOr("Bank", false) ? "gui.guhs.bouwopdracht.bank.ja" : "gui.guhs.bouwopdracht.bank.nee")
                .copy().append(Component.literal("  ")).append(Component.translatable("gui.guhs.bouwopdracht.klaar_teller", b.getIntOr("Klaar", 0)));
        GidsTekst.passend(g, voet, x, top + H - 20, W - 80, 0.625f, LICHT, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
