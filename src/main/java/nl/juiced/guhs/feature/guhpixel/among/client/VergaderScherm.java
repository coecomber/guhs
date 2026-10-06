package nl.juiced.guhs.feature.guhpixel.among.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhpixel.among.AmongPayloads;
import nl.juiced.guhs.feature.guhpixel.among.model.Uitspraak;
import nl.juiced.guhs.taal.Tekst;

/**
 * The meeting. Left: every participant in its colour (asleep, voted, your fellow Mika) with a vote button while the voting
 * runs, and "Overslaan". Right: what was said, newest at the bottom, and under it the ready-made statements: pick a kind,
 * then (when it needs them) a participant and a room. "Chatten" closes the screen so you can type; the Stembriefje in your
 * hotbar opens it again. The server keeps the clock and decides everything; this screen only shows and asks.
 */
public class VergaderScherm extends Screen {
    private static final int W = 400, H = 232, LINKS = 150, RIJ = 16;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, VAK = 0x40000000, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0,
            GROEN = 0xFF68D88A, ROOD = 0xFFFF6B6B;
    private static final Uitspraak.Soort[] KIES = {Uitspraak.Soort.WAAR_IK, Uitspraak.Soort.GEZIEN, Uitspraak.Soort.BIJ_SLAPER, Uitspraak.Soort.VERDENK,
            Uitspraak.Soort.STA_IN, Uitspraak.Soort.DUW, Uitspraak.Soort.LUIK, Uitspraak.Soort.NIKS};

    private CompoundTag data;
    private int over;
    private int left, top;
    /** Building a statement: the kind (null: none), the participant picked for it (-1: not yet). */
    private Uitspraak.Soort soort;
    private int wie = -1;

    public VergaderScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.among.vergadering.titel"));
        this.data = data;
        this.over = data.getIntOr("Over", 0);
    }

    public void update(CompoundTag nieuw) {
        this.data = nieuw;
        this.over = nieuw.getIntOr("Over", 0);
        if (width > 0) {
            rebuildWidgets();
        }
    }

    private int stap() {
        return data.getIntOr("Stap", 0);
    }

    private boolean wakker() {
        return data.getBooleanOr("Wakker", false);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        ListTag deelnemers = data.getListOrEmpty("Deelnemers");
        int ik = data.getIntOr("Ik", -1);
        boolean magStemmen = stap() == 1 && wakker() && data.getIntOr("MijnStem", -2) == -2;
        for (int i = 0; i < deelnemers.size(); i++) {
            CompoundTag d = deelnemers.getCompoundOrEmpty(i);
            int idx = d.getIntOr("Idx", i);
            if (magStemmen && idx != ik && d.getBooleanOr("Wakker", false) && !d.getBooleanOr("Weg", false)) {
                addRenderableWidget(Button.builder(Component.translatable("gui.guhs.among.vergadering.stem"),
                        b -> ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.STEM, idx, 0))).bounds(left + LINKS - 40, top + 30 + i * RIJ, 36, 14).build());
            }
        }
        if (magStemmen) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.among.vergadering.overslaan"),
                    b -> ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.STEM, -1, 0))).bounds(left + 8, top + H - 26, LINKS - 12, 20).build());
        }
        int rx = left + LINKS + 6, rw = W - LINKS - 14, ky = top + H - 74;
        boolean magZeggen = stap() != 2 && wakker() && data.getIntOr("Zeggen", 0) > 0;
        if (magZeggen && soort == null) {
            for (int i = 0; i < KIES.length; i++) {
                Uitspraak.Soort s = KIES[i];
                addRenderableWidget(Button.builder(Component.translatable("gui.guhs.among.zeg." + s.id()), b -> kies(s))
                        .bounds(rx + (i % 4) * (rw / 4), ky + (i / 4) * 22, rw / 4 - 2, 20).build());
            }
        } else if (magZeggen && soort.overIemand() && wie < 0) {
            int n = 0;
            for (int i = 0; i < deelnemers.size(); i++) {
                CompoundTag d = deelnemers.getCompoundOrEmpty(i);
                int idx = d.getIntOr("Idx", i);
                if (idx == ik || d.getBooleanOr("Weg", false)) {
                    continue;
                }
                addRenderableWidget(Button.builder(Tekst.get(d, "Naam"), b -> {
                    wie = idx;
                    klaarOfVerder();
                }).bounds(rx + (n % 5) * (rw / 5), ky + (n / 5) * 22, rw / 5 - 2, 20).build());
                n++;
            }
        } else if (magZeggen && soort.metZone()) {
            ListTag kamers = data.getListOrEmpty("Kamers");
            for (int i = 0; i < kamers.size(); i++) {
                CompoundTag k = kamers.getCompoundOrEmpty(i);
                int zone = k.getIntOr("Idx", -1);
                addRenderableWidget(Button.builder(Tekst.get(k, "Naam"), b -> zeg(zone)).bounds(rx + (i % 5) * (rw / 5), ky + (i / 5) * 22, rw / 5 - 2, 20).build());
            }
        }
        if (soort != null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> {
                soort = null;
                wie = -1;
                rebuildWidgets();
            }).bounds(rx, top + H - 26, 60, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.among.vergadering.chatten"), b -> onClose())
                .bounds(left + W - 88, top + H - 26, 80, 20).build());
    }

    private void kies(Uitspraak.Soort s) {
        soort = s;
        wie = -1;
        klaarOfVerder();
    }

    private void klaarOfVerder() {
        if ((soort.overIemand() && wie < 0) || soort.metZone()) {
            rebuildWidgets();
        } else {
            zeg(-1);
        }
    }

    private void zeg(int zone) {
        CompoundTag extra = new CompoundTag();
        extra.putInt("Zone", zone);
        ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.ZEG, soort.ordinal(), wie, extra));
        soort = null;
        wie = -1;
        rebuildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if (over > 0) {
            over--;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        int stap = stap();
        Component kop = data.contains("Onderwerp") ? Tekst.get(data, "Onderwerp") : title;
        g.text(font, kop.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 7, TEKST, false);
        Component fase = stap == 2 ? Component.translatable("gui.guhs.among.vergadering.uitslag")
                : Component.translatable(stap == 0 ? "gui.guhs.among.vergadering.bespreken" : "gui.guhs.among.vergadering.stemmen", AmongClient.seconden(over));
        g.text(font, fase, left + W - 8 - font.width(fase), top + 7, stap == 1 ? GOUD : TEKST, false);
        if (!wakker()) {
            g.text(font, Component.translatable("gui.guhs.among.vergadering.droomguh"), left + 8, top + 18, DOF, false);
        }
        // the participants
        ListTag deelnemers = data.getListOrEmpty("Deelnemers");
        int ik = data.getIntOr("Ik", -1), mijnStem = data.getIntOr("MijnStem", -2);
        for (int i = 0; i < deelnemers.size(); i++) {
            CompoundTag d = deelnemers.getCompoundOrEmpty(i);
            int y = top + 30 + i * RIJ, idx = d.getIntOr("Idx", i);
            boolean wakker = d.getBooleanOr("Wakker", false), weg = d.getBooleanOr("Weg", false);
            g.fill(left + 6, y, left + LINKS, y + RIJ - 2, mijnStem == idx ? 0x60FFD27A : VAK);
            g.fill(left + 8, y + 2, left + 18, y + RIJ - 4, wakker ? 0xFF000000 | d.getIntOr("Kleur", 0xFFFFFF) : 0xFF5A5058);
            Component naam = Tekst.get(d, "Naam");
            if (idx == ik) {
                naam = Component.translatable("gui.guhs.among.vergadering.jij", naam);
            }
            g.text(font, naam, left + 22, y + 3, wakker ? TEKST : DOF, false);
            Component stand = null;
            int kleur = DOF;
            if (weg) {
                stand = Component.translatable("gui.guhs.among.vergadering.weg");
            } else if (!wakker) {
                stand = Component.translatable("gui.guhs.among.vergadering.slaapt");
            } else if (stap == 2) {
                stand = Component.translatable("gui.guhs.among.vergadering.stemmen_n", d.getIntOr("Stemmen", 0));
                kleur = d.getIntOr("Stemmen", 0) > 0 ? GOUD : DOF;
            } else if (d.getBooleanOr("Gestemd", false)) {
                stand = Component.literal("✔");
                kleur = GROEN;
            } else if (d.getBooleanOr("Maat", false)) {
                stand = Component.translatable("gui.guhs.among.vergadering.maat");
                kleur = ROOD;
            }
            boolean knop = stap == 1 && wakker() && mijnStem == -2 && idx != ik && wakker && !weg;
            if (stand != null && !knop) {
                g.text(font, stand, left + LINKS - 4 - font.width(stand), y + 3, kleur, false);
            }
        }
        if (stap == 2) {
            Component uitslag;
            int weg = data.getIntOr("Weg", -1);
            if (data.contains("UitslagTekst")) {
                uitslag = Tekst.get(data, "UitslagTekst");       // (the oefenrondje has its own verdict)
            } else if (weg < 0) {
                uitslag = Component.translatable(data.getBooleanOr("Gelijk", false) ? "gui.guhs.among.uitslag.gelijk" : "gui.guhs.among.uitslag.niemand");
            } else {
                Component naam = Component.empty();
                for (int i = 0; i < deelnemers.size(); i++) {
                    if (deelnemers.getCompoundOrEmpty(i).getIntOr("Idx", i) == weg) {
                        naam = Tekst.get(deelnemers.getCompoundOrEmpty(i), "Naam");
                    }
                }
                uitslag = Component.translatable(data.getBooleanOr("WegMika", false) ? "gui.guhs.among.uitslag.mika" : "gui.guhs.among.uitslag.geen_mika", naam);
            }
            List<FormattedCharSequence> uitslagRegels = font.split(uitslag, LINKS - 12);
            int y = top + H - 30 - Math.max(0, uitslagRegels.size() - 2) * 10;
            for (FormattedCharSequence regel : uitslagRegels) {
                g.text(font, regel, left + 8, y, GOUD, false);
                y += 10;
            }
            if (uitslagRegels.size() <= 2) {
                g.text(font, Component.translatable("gui.guhs.among.vergadering.overgeslagen", data.getIntOr("Overgeslagen", 0)), left + 8, top + H - 42, DOF, false);
            }
        } else if (stap == 1 && mijnStem != -2) {
            g.text(font, Component.translatable("gui.guhs.among.vergadering.gestemd"), left + 8, top + H - 20, GROEN, false);
        } else if (stap == 0) {
            g.text(font, Component.translatable("gui.guhs.among.vergadering.straks"), left + 8, top + H - 20, DOF, false);
        }
        // what was said: the newest lines that fit
        int rx = left + LINKS + 6, rw = W - LINKS - 14, ly0 = top + 22, ly1 = top + H - 90;
        g.fill(rx - 2, ly0, rx + rw + 2, ly1, VAK);
        ListTag uitspraken = data.getListOrEmpty("Uitspraken");
        List<FormattedCharSequence> regels = new ArrayList<>();
        for (int i = 0; i < uitspraken.size(); i++) {
            CompoundTag u = uitspraken.getCompoundOrEmpty(i);
            Component naam = Component.empty();
            int spreker = u.getIntOr("Spreker", -1);
            for (int k = 0; k < deelnemers.size(); k++) {
                if (deelnemers.getCompoundOrEmpty(k).getIntOr("Idx", k) == spreker) {
                    naam = Tekst.get(deelnemers.getCompoundOrEmpty(k), "Naam");
                }
            }
            regels.addAll(font.split(Component.translatable("gui.guhs.among.zegt", naam, Tekst.get(u, "Tekst")), rw - 4));
        }
        int passen = (ly1 - ly0 - 4) / 10;
        int van = Math.max(0, regels.size() - passen);
        for (int i = van; i < regels.size(); i++) {
            g.text(font, regels.get(i), rx + 2, ly0 + 3 + (i - van) * 10, TEKST, false);
        }
        // the statement builder
        Component hulp;
        if (!wakker() || stap == 2) {
            hulp = Component.empty();
        } else if (data.getIntOr("Zeggen", 0) <= 0) {
            hulp = Component.translatable("gui.guhs.among.zeg.op");
        } else if (soort == null) {
            hulp = Component.translatable("gui.guhs.among.zeg.kies", data.getIntOr("Zeggen", 0));
        } else if (soort.overIemand() && wie < 0) {
            hulp = Component.translatable("gui.guhs.among.zeg.wie", Component.translatable("gui.guhs.among.zeg." + soort.id()));
        } else {
            hulp = Component.translatable("gui.guhs.among.zeg.waar", Component.translatable("gui.guhs.among.zeg." + soort.id()));
        }
        g.text(font, hulp, rx, top + H - 86, GOUD, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
