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
 * 8..32, circle or rectangle, show the edge, sleeping spot), Tevredenheid (the bar and the checklist, "Bouw passend verblijf"),
 * Cadeautjes, Info (a fact, your best photo, the cooldown), Bouwopdracht (the family's jobs, deel 2 F10b) and, for the stork,
 * Bezorgen. Someone else's board: look only.
 * <p>
 * Test round 5: half as big again; every button explains itself in a tooltip, a greyed-out one says why; every checklist line
 * says how to tick it; "Rand tonen" toggles the client's own edge ({@link VerblijfRand}); the sleeping spot is pointed at
 * with a click; "Bouw passend verblijf" is a proposal with "Annuleren" / "Goedgekeurd, laat bouwen".
 */
public class VerblijfScreen extends Screen {
    private static final int W = 480, H = 330;
    private static final int PANEEL = 0xF0FFF8EE, RAND = 0xFFC9A46A, KOP = 0xFFF6E7C8, DONKER = 0xFF4A3420, TEKST = 0xFF5A4630,
            LICHT = 0xFF9A8060, GROEN = 0xFF4E8A3A, ROOD = 0xFFB0484A;

    private enum Tab { FAMILIE, VERBLIJF, TEVREDEN, CADEAUTJES, INFO, BOUW, BEZORGEN }

    /** A spot on the screen with a tooltip (the checklist lines, the gift slots). */
    private record Tip(int x, int y, int w, int h, List<Component> tekst) {
        boolean onder(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private CompoundTag data;
    private BlockPos pos;
    private Tab tab = Tab.FAMILIE;
    private int left, top;
    private final List<EditBox> namen = new ArrayList<>();
    private final List<Tip> tips = new ArrayList<>();
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

    private static String sleutel(Tab t) {
        return switch (t) {
            case FAMILIE -> "familie";
            case VERBLIJF -> "verblijf";
            case TEVREDEN -> "tevreden";
            case CADEAUTJES -> "cadeautjes";
            case INFO -> "info";
            case BOUW -> "bouw";
            case BEZORGEN -> "bezorgen";
        };
    }

    // --- buttons with their explanation ---------------------------------------------------------------------------------

    /**
     * A button with its explanation ({@code uitleg}, a text key) as tooltip. Greyed out ({@code reden} not null) it says why
     * instead, with the explanation under it.
     */
    private Button knop(Component tekst, int x, int y, int w, Runnable actie, String uitleg, Component reden) {
        Button b = Button.builder(tekst, x2 -> actie.run()).bounds(x, y, w, 20).build();
        b.active = reden == null;
        Component tip = Component.translatable(uitleg);
        if (reden != null) {
            tip = reden.copy().withStyle(ChatFormatting.RED).append(Component.literal("\n\n")).append(tip.copy().withStyle(ChatFormatting.GRAY));
        }
        b.setTooltip(Tooltip.create(tip));
        return addRenderableWidget(b);
    }

    /** Why a button of this board is greyed out for this player, or null: someone else's board. */
    private Component nietJouw() {
        return mag() ? null : Component.translatable("gui.guhs.verblijf.reden.niet_jouw", data.getStringOr("Eigenaar", "?"));
    }

    /** "This board already has a job: X. Stop it first." or null. */
    private Component alKlus() {
        CompoundTag p = data.getCompoundOrEmpty("Bouw").getCompoundOrEmpty("Project");
        if (!data.getCompoundOrEmpty("Bouw").contains("Project")) {
            return null;
        }
        String taak = p.getStringOr("Taak", "");
        Component naam = Component.translatable(taak.equals("verblijf") ? "gui.guhs.verblijf.tevreden.bouw" : "gui.guhs.verblijf.taak." + taak);
        return Component.translatable(taak.equals("verblijf") ? "gui.guhs.verblijf.reden.al_voorstel" : "gui.guhs.verblijf.reden.al_klus", naam);
    }

    private static Component eerste(Component... redenen) {
        for (Component c : redenen) {
            if (c != null) {
                return c;
            }
        }
        return null;
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
            Button b = Button.builder(Component.translatable("gui.guhs.verblijf.tab." + sleutel(t)), x -> {
                tab = t;
                rebuildWidgets();
            }).bounds(left + 6 + i * tw, top + 22, tw - 2, 20).build();
            b.active = t != tab;
            b.setTooltip(Tooltip.create(Component.translatable("gui.guhs.verblijf.tab." + sleutel(t) + ".uitleg")));
            addRenderableWidget(b);
        }
        int y = top + 54;
        Component nietJouw = nietJouw();
        switch (tab) {
            case FAMILIE -> {
                ListTag dieren = data.getListOrEmpty("Dieren");
                for (int i = 0; i < dieren.size(); i++) {
                    CompoundTag d = dieren.getCompoundOrEmpty(i);
                    EditBox e = new EditBox(font, left + 140, y + 6 + i * 40, 170, 20, Component.translatable("gui.guhs.verblijf.hernoem"));
                    e.setMaxLength(24);
                    e.setValue(d.getStringOr("Naam", ""));
                    e.setHint(Component.translatable("gui.guhs.verblijf." + d.getStringOr("Rol", "baby")).withStyle(ChatFormatting.GRAY));
                    e.setEditable(mag());
                    e.setTooltip(Tooltip.create(Component.translatable("gui.guhs.verblijf.hernoem.veld.uitleg")));
                    namen.add(e);
                    addRenderableWidget(e);
                    final int lid = i;
                    knop(Component.translatable("gui.guhs.verblijf.hernoem"), left + 316, y + 6 + i * 40, 70, () -> doe(Actie.NAAM, lid, e.getValue()),
                            "gui.guhs.verblijf.hernoem.uitleg", nietJouw);
                }
                knop(Component.translatable("gui.guhs.verblijf.terugroepen"), left + 14, top + H - 62, 220, () -> doe(Actie.TERUG, 0, ""),
                        "gui.guhs.verblijf.terugroepen.uitleg", nietJouw);
                knop(Component.translatable("gui.guhs.verblijf.verstoppen"), left + 246, top + H - 62, 220, () -> doe(Actie.VERSTOP, 0, ""),
                        "gui.guhs.verblijf.verstoppen.uitleg", nietJouw);
            }
            case VERBLIJF -> {
                boolean rechthoek = data.getBooleanOr("Rechthoek", false);
                knop(Component.translatable(rechthoek ? "gui.guhs.verblijf.vorm.rechthoek" : "gui.guhs.verblijf.vorm.cirkel"), left + 14, y, 220,
                        () -> doe(Actie.VORM, rechthoek ? 0 : 1, ""), "gui.guhs.verblijf.vorm.uitleg", nietJouw);
                if (rechthoek) {
                    addRenderableWidget(new Schuif(left + 14, y + 30, 220, Actie.BREEDTE, "gui.guhs.verblijf.breedte", data.getIntOr("Breedte", 12), 4));
                    addRenderableWidget(new Schuif(left + 246, y + 30, 220, Actie.DIEPTE, "gui.guhs.verblijf.diepte", data.getIntOr("Diepte", 12), 4));
                } else {
                    addRenderableWidget(new Schuif(left + 14, y + 30, W - 28, Actie.STRAAL, "gui.guhs.verblijf.straal", data.getIntOr("Straal", 12),
                            VerblijfsbordBlockEntity.MIN_STRAAL));
                }
                boolean randAan = VerblijfRand.aan(pos);
                knop(Component.translatable(randAan ? "gui.guhs.verblijf.rand.verstop" : "gui.guhs.verblijf.rand"), left + 246, y, 220, () -> {
                    VerblijfRand.wissel(data);
                    rebuildWidgets();
                }, "gui.guhs.verblijf.rand.uitleg", null);
                knop(Component.translatable("gui.guhs.verblijf.thuisplek.wijs"), left + 14, y + 66, 220, () -> {
                    doe(Actie.SLAAPPLEK_WIJS, 0, "");
                    VerblijfRand.toon(data, VerblijfRand.DUUR);   // (so you see where "inside" is)
                    onClose();
                }, "gui.guhs.verblijf.thuisplek.wijs.uitleg", nietJouw);
                knop(Component.translatable("gui.guhs.verblijf.thuisplek.weghalen"), left + 246, y + 66, 220, () -> doe(Actie.SLAAPPLEK_WEG, 0, ""),
                        "gui.guhs.verblijf.thuisplek.weghalen.uitleg",
                        eerste(nietJouw, data.contains("Slaapplek") ? null : Component.translatable("gui.guhs.verblijf.reden.geen_slaapplek")));
                for (var w : children()) {
                    if (w instanceof Schuif s) {
                        s.active = mag();
                        s.setTooltip(Tooltip.create(nietJouw != null ? nietJouw : Component.translatable("gui.guhs.verblijf.schuif.uitleg")));
                    }
                }
            }
            case TEVREDEN -> {
                // deel 2 (F10b): the verblijf-assistent; test round 5: first a proposal, built only after the OK
                CompoundTag bouw = data.getCompoundOrEmpty("Bouw");
                if (bouw.getBooleanOr("Voorstel", false)) {
                    knop(Component.translatable("gui.guhs.verblijf.voorstel.annuleren"), left + 14, top + H - 62, 160,
                            () -> doe(Actie.VERBLIJF_WEG, 0, ""), "gui.guhs.verblijf.voorstel.annuleren.uitleg", nietJouw);
                    knop(Component.translatable("gui.guhs.verblijf.voorstel.goed").withStyle(ChatFormatting.BOLD), left + W - 254, top + H - 62, 240,
                            () -> doe(Actie.VERBLIJF_GOED, 0, ""), "gui.guhs.verblijf.voorstel.goed.uitleg", nietJouw);
                } else {
                    knop(Component.translatable("gui.guhs.verblijf.tevreden.bouw"), left + W - 254, top + H - 62, 240, () -> doe(Actie.BOUW_VERBLIJF, 0, ""),
                            "gui.guhs.bouwopdracht.verblijf.uitleg", eerste(nietJouw, alKlus()));
                }
            }
            case BOUW -> bouwKnoppen(nietJouw);
            case CADEAUTJES -> {
                boolean iets = false;
                ListTag c = data.getListOrEmpty("Cadeaus");
                for (int i = 0; i < c.size(); i++) {
                    iets |= !c.getCompoundOrEmpty(i).getStringOr("Id", "").isEmpty();
                }
                knop(Component.translatable("gui.guhs.verblijf.cadeautjes.pak"), left + 14, top + H - 62, 220, () -> doe(Actie.CADEAUS, 0, ""),
                        "gui.guhs.verblijf.cadeautjes.pak.uitleg", eerste(nietJouw, iets ? null : Component.translatable("gui.guhs.verblijf.reden.geen_cadeaus")));
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
                knop(naar, left + 14, y + 70, 300, () -> {
                    if (!vrienden.isEmpty()) {
                        vriend = (vriend + 1) % vrienden.size();
                        doe(Actie.BEZORG_KIES, 0, vrienden.getCompoundOrEmpty(vriend).getStringOr("Id", ""));
                    }
                }, "gui.guhs.verblijf.bezorgen.kies.uitleg", eerste(nietJouw, vrienden.isEmpty() ? Component.translatable("gui.guhs.verblijf.reden.geen_vrienden") : null));
                knop(Component.translatable("gui.guhs.verblijf.bezorgen.stuur"), left + 14, y + 98, 300, () -> doe(Actie.BEZORG_STUUR, 0, ""),
                        "gui.guhs.verblijf.bezorgen.stuur.uitleg", eerste(nietJouw,
                                gekozen.isEmpty() ? Component.translatable("gui.guhs.verblijf.reden.geen_bestemming") : null,
                                data.getBooleanOr("Onderweg", false) ? Component.translatable("gui.guhs.verblijf.bezorgen.onderweg") : null));
            }
            default -> {
            }
        }
        Button sluit = Button.builder(Component.translatable("gui.guhs.verblijf.sluit"), b -> onClose()).bounds(left + W - 76, top + H - 30, 66, 20).build();
        addRenderableWidget(sluit);
    }

    /** Deel 2 (F10b): the Bouwopdracht tab: the tasks on the left, the chosen one's buttons and the running job on the right. */
    private void bouwKnoppen(Component nietJouw) {
        CompoundTag b = data.getCompoundOrEmpty("Bouw");
        ListTag taken = b.getListOrEmpty("Taken");
        if (taken.isEmpty()) {
            return;
        }
        String keuze = b.getStringOr("Keuze", "");
        int y = top + 54;
        for (int i = 0; i < taken.size(); i++) {
            String t = taken.getStringOr(i, "");
            final int index = i;
            Button k = Button.builder(Component.translatable("gui.guhs.verblijf.taak." + t), x -> doe(Actie.BOUW_KIES, index, ""))
                    .bounds(left + 14, y + i * 22, 150, 20).build();
            k.active = !t.equals(keuze);
            k.setTooltip(Tooltip.create(Component.translatable("gui.guhs.bouwopdracht.uitleg." + t)));
            addRenderableWidget(k);
        }
        int x = left + 176, w = W - 190;
        CompoundTag project = b.getCompoundOrEmpty("Project");
        boolean bezig = b.contains("Project");
        Component klus = alKlus();
        if (b.getIntOr("Varianten", 1) > 1) {
            knop(Component.translatable("gui.guhs.bouwopdracht.variant",
                            Component.translatable("gui.guhs.bouwopdracht.variant." + keuze + "." + b.getIntOr("Variant", 0))),
                    x, y + 70, w, () -> doe(Actie.BOUW_VARIANT, 0, ""), "gui.guhs.bouwopdracht.variant.uitleg", nietJouw);
        }
        knop(Component.translatable("gui.guhs.bouwopdracht.zoek"), x, y + 96, w / 2 - 2, () -> doe(Actie.BOUW_ZOEK, 0, ""),
                "gui.guhs.bouwopdracht.zoek.uitleg", eerste(nietJouw, klus));
        knop(Component.translatable(b.getIntOr("Punten", 1) == 2 ? "gui.guhs.bouwopdracht.wijs_twee" : "gui.guhs.bouwopdracht.wijs"),
                x + w / 2 + 2, y + 96, w / 2 - 2, () -> {
                    doe(Actie.BOUW_WIJS, 0, "");
                    onClose();
                }, "gui.guhs.bouwopdracht.wijs.uitleg", eerste(nietJouw, klus));
        if (bezig) {
            boolean spook = project.contains("Status");
            boolean bouwt = "BOUWT".equals(project.getStringOr("Status", ""));
            int bw = (w - 8) / 3;
            knop(Component.translatable("gui.guhs.bouwopdracht.bedien"), x, y + 186, bw, () -> {
                doe(Actie.BOUW_BEDIEN, 0, "");
                onClose();
            }, "gui.guhs.bouwopdracht.bedien.uitleg", eerste(nietJouw, spook ? null : Component.translatable("gui.guhs.verblijf.reden.geen_spook")));
            knop(Component.translatable("gui.guhs.bouwopdracht.herbereken"), x + bw + 4, y + 186, bw, () -> doe(Actie.BOUW_HERBEREKEN, 0, ""),
                    "gui.guhs.bouwopdracht.herbereken.uitleg", eerste(nietJouw, !spook ? Component.translatable("gui.guhs.verblijf.reden.geen_spook")
                            : bouwt ? Component.translatable("gui.guhs.verblijf.reden.bouwt_al") : null));
            knop(Component.translatable("gui.guhs.bouwopdracht.stop"), x + 2 * bw + 8, y + 186, bw, () -> doe(Actie.BOUW_STOP, 0, ""),
                    "gui.guhs.bouwopdracht.stop.uitleg", nietJouw);
        }
    }

    /** A slider for one number (the range, a width, a depth), sent when it changes; the edge shows the new size. */
    private class Schuif extends AbstractSliderButton {
        private final Actie actie;
        private final String sleutel;
        private final int min;
        private int laatst;

        Schuif(int x, int y, int w, Actie actie, String sleutel, int waarde, int min) {
            super(x, y, w, 20, Component.empty(), (waarde - min) / (double) (VerblijfsbordBlockEntity.MAX_STRAAL - min));
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
                CompoundTag nu = data.copy();
                nu.putInt(actie == Actie.BREEDTE ? "Breedte" : actie == Actie.DIEPTE ? "Diepte" : "Straal", g);
                VerblijfRand.toon(nu, 20 * 10);   // (the new edge for a moment, or for as long as it was shown)
            }
        }
    }

    // --- drawing ----------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        tips.clear();
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.fill(left, top, left + W, top + 20, KOP);
        g.fill(left, top + 20, left + W, top + 21, RAND);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 6, DONKER, false);
        Component van = Component.translatable("gui.guhs.verblijf.van", data.getStringOr("Eigenaar", "?"));
        if (!mag()) {
            van = van.copy().append(" ").append(Component.translatable("gui.guhs.verblijf.alleen_kijken").withStyle(ChatFormatting.ITALIC));
        }
        GidsTekst.passend(g, van, left + W - 8, top + 6, 220, 0.875f, LICHT, true);
        int x = left + 14, y = top + 54;
        switch (tab) {
            case FAMILIE -> familie(g, x, y);
            case VERBLIJF -> {
                Component plek = data.contains("Slaapplek") ? Component.translatable("gui.guhs.verblijf.thuisplek.is",
                        BlockPos.of(data.getLongOr("Slaapplek", 0L)).toShortString()) : Component.translatable("gui.guhs.verblijf.thuisplek.geen");
                GidsTekst.passend(g, plek, x, y + 96, W - 28, 1f, TEKST, false);
                GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.verblijf.uitleg"), x, y + 116, W - 28, 0.875f, LICHT);
            }
            case TEVREDEN -> tevreden(g, x, y);
            case CADEAUTJES -> cadeautjes(g, x, y);
            case INFO -> info(g, x, y);
            case BOUW -> bouw(g, x, y);
            case BEZORGEN -> {
                GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.bezorgen.uitleg"), x, y, W - 28, 0.875f, TEKST);
                if (data.getBooleanOr("Onderweg", false)) {
                    GidsTekst.passend(g, Component.translatable("gui.guhs.verblijf.bezorgen.onderweg").withStyle(ChatFormatting.ITALIC), x, y + 130, W - 28,
                            1f, GROEN, false);
                }
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        for (Tip t : tips) {
            if (t.onder(mouseX, mouseY)) {
                g.setComponentTooltipForNextFrame(font, t.tekst(), mouseX, mouseY);
                break;
            }
        }
    }

    private void familie(GuiGraphicsExtractor g, int x, int y) {
        ListTag dieren = data.getListOrEmpty("Dieren");
        for (int i = 0; i < dieren.size(); i++) {
            CompoundTag d = dieren.getCompoundOrEmpty(i);
            int ry = y + i * 40;
            g.fill(x - 2, ry, left + W - 14, ry + 32, i % 2 == 0 ? 0x18C9A46A : 0x0CC9A46A);
            g.text(font, Component.translatable("gui.guhs.verblijf." + d.getStringOr("Rol", "baby")).withStyle(ChatFormatting.BOLD), x + 2, ry + 5, DONKER, false);
            String stemming = d.getStringOr("Stemming", "");
            Component s = d.getIntOr("Entity", -1) < 0 ? Component.translatable("gui.guhs.verblijf.weg")
                    : Component.translatable("gui.guhs.verblijf.stemming." + (stemming.isEmpty() ? "tevreden" : stemming));
            GidsTekst.schaal(g, s, x + 2, ry + 18, 0.875f, stemming.equals("mopperig") ? ROOD : LICHT, false);
            if (d.getBooleanOr("Weg", false)) {
                GidsTekst.schaal(g, Component.translatable("gui.guhs.verblijf.verstopt"), left + 394, ry + 12, 0.875f, LICHT, false);
            }
        }
        if (data.getBooleanOr("Rijdier", false)) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.verblijf.rijden"), x, top + H - 86, W - 28, 0.875f, GROEN, false);
        }
    }

    private void tevreden(GuiGraphicsExtractor g, int x, int y) {
        CompoundTag t = data.getCompoundOrEmpty("Tevreden");
        int score = t.getIntOr("Score", 0);
        g.text(font, Component.translatable("gui.guhs.verblijf.tevreden.balk", score).withStyle(ChatFormatting.BOLD), x, y, DONKER, false);
        GidsTekst.balk(g, x + 170, y + 1, W - 200, 8, score / 100f);
        GidsTekst.passend(g, Component.translatable("gui.guhs.verblijf.tevreden.hover").withStyle(ChatFormatting.ITALIC), x, y + 13, W - 28, 0.75f, LICHT, false);
        int ry = y + 28;
        int kolom = (W - 28) / 2;
        ListTag eisen = t.getListOrEmpty("Eisen");
        for (int i = 0; i < eisen.size(); i++) {
            CompoundTag e = eisen.getCompoundOrEmpty(i);
            boolean met = e.getIntOr("Aantal", 0) >= e.getIntOr("Min", 1);
            MutableComponent regel = Component.literal(met ? "✔ " : "✘ ").append(Component.translatable("gui.guhs.verblijf.eis." + e.getStringOr("Id", "")))
                    .append(Component.literal("  " + e.getIntOr("Aantal", 0) + "/" + e.getIntOr("Min", 1)).withStyle(ChatFormatting.GRAY));
            int rx = x + (i % 2) * kolom, rr = ry + (i / 2) * 14;
            GidsTekst.passend(g, regel, rx, rr, kolom - 6, 0.875f, met ? GROEN : ROOD, false);
            tips.add(new Tip(rx, rr - 2, kolom - 6, 13, eisTip(e)));
        }
        ry += ((eisen.size() + 1) / 2) * 14 + 6;
        String speeltje = VerblijfFeature.speeltjeId(data.getStringOr("Speeltje", ""));
        Component speeltjeNaam = Component.translatable("gui.guhs.verblijf.speeltje." + speeltje);
        Component[] extra = {Component.translatable("gui.guhs.verblijf.tevreden.ruimte"), Component.translatable("gui.guhs.verblijf.tevreden.geaaid"),
                Component.translatable("gui.guhs.verblijf.tevreden.speeltje", speeltjeNaam), Component.translatable("gui.guhs.verblijf.tevreden.vrienden")};
        boolean[] ok = {t.getBooleanOr("Ruimte", false), t.getBooleanOr("Geaaid", false), t.getBooleanOr("Speeltje", false), t.getBooleanOr("Vrienden", false)};
        List<List<Component>> uitleg = List.of(
                List.of(Component.translatable("gui.guhs.verblijf.tip.ruimte")),
                List.of(Component.translatable("gui.guhs.verblijf.tip.geaaid")),
                List.of(Component.translatable("gui.guhs.verblijf.tip.speeltje", speeltjeNaam)),
                List.of(Component.translatable("gui.guhs.verblijf.tip.vrienden", vriendNamen(t.getListOrEmpty("VriendSoorten")))));
        for (int i = 0; i < extra.length; i++) {
            int rx = x + (i % 2) * kolom, rr = ry + (i / 2) * 14;
            GidsTekst.passend(g, Component.literal(ok[i] ? "✔ " : "✘ ").append(extra[i]), rx, rr, kolom - 6, 0.875f, ok[i] ? GROEN : LICHT, false);
            tips.add(new Tip(rx, rr - 2, kolom - 6, 13, uitleg.get(i)));
        }
        ry += 2 * 14 + 8;
        List<Component> voer = new ArrayList<>();
        for (int i = 0; i < data.getListOrEmpty("Voer").size(); i++) {
            String id = data.getListOrEmpty("Voer").getStringOr(i, "");
            BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).ifPresent(it -> voer.add(new ItemStack(it).getHoverName()));
        }
        MutableComponent lijst = Component.empty();
        for (int i = 0; i < voer.size(); i++) {
            lijst.append(i == 0 ? Component.empty() : Component.literal(", ")).append(voer.get(i));
        }
        int h = GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.voer.traktatie", lijst), x, ry, W - 28, 0.875f, TEKST);
        if (data.getCompoundOrEmpty("Bouw").getBooleanOr("Voorstel", false)) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.voorstel.uitleg"), x, ry + h + 8, W - 28, 0.875f, GROEN);
        }
    }

    /** How to tick a requirement: "Zet minstens 20 van deze blokken binnen de rand: zand, ..." (water: dig or pour). */
    private List<Component> eisTip(CompoundTag e) {
        List<Component> uit = new ArrayList<>();
        int min = e.getIntOr("Min", 1);
        if ("water".equals(e.getStringOr("Soort", ""))) {
            uit.add(Component.translatable("gui.guhs.verblijf.tip.water", min));
        } else {
            MutableComponent namen = Component.empty();
            ListTag bl = e.getListOrEmpty("Bl");
            for (int i = 0; i < bl.size(); i++) {
                var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(bl.getStringOr(i, "minecraft:air")));
                if (item.isPresent()) {
                    namen.append(i == 0 ? "" : ", ").append(new ItemStack(item.get()).getHoverName());
                }
            }
            uit.add(Component.translatable("gui.guhs.verblijf.tip.blokken", min, namen));
        }
        uit.add(Component.translatable("gui.guhs.verblijf.tip.waar").withStyle(ChatFormatting.GRAY));
        return uit;
    }

    private static Component vriendNamen(ListTag soorten) {
        MutableComponent uit = Component.empty();
        for (int i = 0; i < soorten.size(); i++) {
            String s = soorten.getStringOr(i, "");
            uit.append(i == 0 ? "" : ", ").append(s.equals("*") ? Component.translatable("gui.guhs.verblijf.tip.iedereen")
                    : Component.translatable("entity.guhs.safari." + s));
        }
        return soorten.isEmpty() ? Component.translatable("gui.guhs.verblijf.tip.niemand") : uit;
    }

    private void cadeautjes(GuiGraphicsExtractor g, int x, int y) {
        GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.cadeautjes.uitleg"), x, y, W - 28, 0.875f, TEKST);
        ListTag c = data.getListOrEmpty("Cadeaus");
        boolean iets = false;
        for (int i = 0; i < c.size(); i++) {
            int sx = x + 60 + (i % 9) * 34, sy = y + 40;
            g.fill(sx - 1, sy - 1, sx + 27, sy + 27, 0xFFC9A46A);
            g.fill(sx, sy, sx + 26, sy + 26, 0xFFF8EBD2);
            CompoundTag s = c.getCompoundOrEmpty(i);
            String id = s.getStringOr("Id", "");
            if (!id.isEmpty()) {
                var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(id));
                if (item.isPresent()) {
                    ItemStack st = new ItemStack(item.get(), Math.max(1, s.getIntOr("N", 1)));
                    g.item(st, sx + 5, sy + 5);
                    g.itemDecorations(font, st, sx + 5, sy + 5);
                    tips.add(new Tip(sx, sy, 26, 26, List.of(st.getHoverName())));
                    iets = true;
                }
            }
        }
        if (!iets) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.verblijf.cadeautjes.leeg").withStyle(ChatFormatting.ITALIC), x, y + 80, W - 28, 1f, LICHT, false);
        }
    }

    private void info(GuiGraphicsExtractor g, int x, int y) {
        String soort = data.getStringOr("Soort", "");
        int h = GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.feit." + soort), x, y, W - 28, 1f, TEKST);
        int ry = y + h + 12;
        int sterren = data.getIntOr("Sterren", 0);
        Component foto = sterren <= 0 ? Component.translatable("gui.guhs.verblijf.info.geen_foto")
                : Component.translatable("gui.guhs.verblijf.info.sterren", "★".repeat(sterren) + "☆".repeat(Math.max(0, 3 - sterren)));
        GidsTekst.passend(g, foto, x, ry, W - 28, 1f, sterren > 0 ? GROEN : LICHT, false);
        long cd = data.getLongOr("Cooldown", 0L);
        Component cooldown = cd <= 0 ? Component.translatable("gui.guhs.verblijf.info.cooldown.nu")
                : Component.translatable("gui.guhs.verblijf.info.cooldown", VerblijfWinkel.tijd(cd));
        GidsTekst.passend(g, cooldown, x, ry + 16, W - 28, 1f, TEKST, false);
        // G1 (design 1.9, "je beste foto"): the caption of your best dex photo of this species, in a little photo frame
        CompoundTag beste = nl.juiced.guhs.feature.safari.camera.client.SafariDexTab.dexRegel(soort).getCompoundOrEmpty("Foto");
        if (sterren > 0 && !beste.isEmpty()) {
            int fy = ry + 40;
            boolean goud = beste.getBooleanOr("Goud", false);
            g.fill(x - 2, fy - 2, x + W - 26, fy + 30, goud ? 0xFFE8B830 : 0xFFC9A46A);
            g.fill(x, fy, x + W - 28, fy + 28, 0xFFF8EBD2);
            GidsTekst.alinea(g, nl.juiced.guhs.feature.safari.camera.Foto.onderschrift(beste), x + 4, fy + 3, W - 36, 0.875f, TEKST);
        }
    }

    private void bouw(GuiGraphicsExtractor g, int x, int y) {
        CompoundTag b = data.getCompoundOrEmpty("Bouw");
        ListTag taken = b.getListOrEmpty("Taken");
        if (taken.isEmpty()) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.verblijf.bouw.geen"), x, y, W - 28, 1f, TEKST);
            return;
        }
        int rx = left + 176, w = W - 190;
        String keuze = b.getStringOr("Keuze", "");
        GidsTekst.passend(g, Component.translatable("gui.guhs.verblijf.taak." + keuze).withStyle(ChatFormatting.BOLD), rx, y, w, 1f, DONKER, false);
        GidsTekst.alinea(g, Component.translatable("gui.guhs.bouwopdracht.uitleg." + keuze), rx, y + 14, w, 0.875f, TEKST);
        int grootte = b.getIntOr("Grootte", 0);
        GidsTekst.passend(g, Component.translatable("gui.guhs.bouwopdracht.grootte." + grootte), rx, y + 126, w, 0.875f, grootte == 2 ? GROEN : LICHT, false);
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
            GidsTekst.passend(g, status, rx, y + 142, w, 0.875f, DONKER, false);
            if (p.contains("Totaal")) {
                float f = p.getIntOr("Gedaan", 0) / (float) Math.max(1, p.getIntOr("Totaal", 1));
                GidsTekst.balk(g, rx, y + 156, w, 8, f);
                if ("BOUWT".equals(p.getStringOr("Status", ""))) {
                    GidsTekst.passend(g, Component.translatable(net.minecraft.locale.Language.getInstance().has("gui.guhs.bouwopdracht.werk." + data.getStringOr("Soort", "")) ? "gui.guhs.bouwopdracht.werk." + data.getStringOr("Soort", "") : "gui.guhs.bouwopdracht.werk.anders").withStyle(ChatFormatting.ITALIC),
                            rx, y + 168, w, 0.75f, LICHT, false);
                }
            }
        }
        Component voet = Component.translatable(b.getBooleanOr("Bank", false) ? "gui.guhs.bouwopdracht.bank.ja" : "gui.guhs.bouwopdracht.bank.nee")
                .copy().append(Component.literal("  ")).append(Component.translatable("gui.guhs.bouwopdracht.klaar_teller", b.getIntOr("Klaar", 0)));
        GidsTekst.passend(g, voet, x, top + H - 24, W - 100, 0.75f, LICHT, false);
        tips.add(new Tip(x, top + H - 26, W - 100, 12, List.of(Component.translatable("gui.guhs.bouwopdracht.bank.uitleg"))));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
