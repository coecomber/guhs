package nl.juiced.guhs.feature.snuffel.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.snuffel.HondEntity;
import nl.juiced.guhs.feature.snuffel.Honden;
import nl.juiced.guhs.feature.snuffel.Keuze;
import nl.juiced.guhs.feature.snuffel.MaatjeEntity;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.snuffel.SnuffelPayloads;

/**
 * The choice screen of Het Snuffeleiland, in two pages. First your DOG: one of the six breeds, one of its three coats,
 * and its name; the dog stands next to the buttons and can be turned by dragging. Then your COMPANION: the three little
 * forest sprites next to each other (Zweefzaadje, Mos-eikeltje, Zonnepluisje), each with what it is like; you pick the one
 * that comes along. "Klaar" sends the choice to the server, which only takes it when it opened this screen itself (the
 * story decides when you may choose). Esc closes without choosing: whoever opened it will ask again.
 */
public final class KeuzeScherm extends Screen {
    private static final int W = 328, H = 214;
    private static final int PAPIER = 0xFFF6ECD6, RAND = 0xFF8A6A3A, INKT = 0xFF4A3220, ZACHT = 0xFF7A5A3A, VAK = 0xFFEADBB8, GEKOZEN = 0xFFE0992F;

    private int pagina;
    private String ras, kleur, naam, maatje;
    private float draai = 25f;
    private int ticks;
    @Nullable
    private HondEntity hond;
    private final MaatjeEntity[] maatjes = new MaatjeEntity[Honden.MAATJES.size()];
    @Nullable
    private EditBox naamVak;
    private int left, top;

    public KeuzeScherm(CompoundTag nu) {
        super(Component.translatable("gui.guhs.snuffel.keuze.titel"));
        List<Honden.Ras> rassen = Honden.speelbaar();
        Honden.Ras eerste = rassen.isEmpty() ? null : rassen.get(0);
        this.ras = nu.getStringOr("Ras", eerste == null ? "shiba" : eerste.id());
        Honden.Ras r = Honden.ras(ras);
        if (r == null || !r.speelbaar()) {
            this.ras = eerste == null ? "shiba" : eerste.id();
            r = eerste;
        }
        this.kleur = nu.getStringOr("Kleur", "");
        if (r != null && !r.kleuren().contains(kleur)) {
            this.kleur = r.kleuren().get(0);
        }
        Minecraft mc = Minecraft.getInstance();
        this.naam = nu.getStringOr("Naam", mc.player == null ? "" : Keuze.netjes(mc.player.getGameProfile().name(), ""));
        this.maatje = nu.getStringOr("Maatje", "b");
        if (!Honden.MAATJES.contains(maatje)) {
            this.maatje = "b";
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        if (pagina == 0) {
            int x = left + 138, y = top + 34;
            List<Honden.Ras> rassen = Honden.speelbaar();
            for (int i = 0; i < rassen.size(); i++) {
                Honden.Ras r = rassen.get(i);
                addRenderableWidget(Button.builder(r.naam(), b -> kiesRas(r.id())).bounds(x + (i % 2) * 90, y + (i / 2) * 20, 88, 18).build());
            }
            y += ((rassen.size() + 1) / 2) * 20 + 14;
            Honden.Ras r = Honden.ras(ras);
            List<String> kleuren = r == null ? List.of() : r.kleuren();
            for (int i = 0; i < kleuren.size(); i++) {
                String k = kleuren.get(i);
                addRenderableWidget(Button.builder(r.kleurNaam(k), b -> {
                    kleur = k;
                    ververs();
                }).bounds(x, y + i * 20, 178, 18).build());
            }
            naamVak = new EditBox(font, left + 14, top + H - 46, 112, 16, Component.translatable("gui.guhs.snuffel.keuze.naam"));
            naamVak.setMaxLength(Keuze.NAAM_MAX);
            naamVak.setValue(naam);
            naamVak.setResponder(s -> naam = s);
            addRenderableWidget(naamVak);
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.snuffel.keuze.verder"), b -> {
                pagina = 1;
                rebuildWidgets();
            }).bounds(left + W - 108, top + H - 26, 98, 20).build());
        } else {
            naamVak = null;
            int kaart = 98, x0 = left + (W - 3 * kaart - 2 * 6) / 2;
            for (int i = 0; i < Honden.MAATJES.size(); i++) {
                String m = Honden.MAATJES.get(i);
                addRenderableWidget(Button.builder(Honden.maatjeNaam(m), b -> {
                    maatje = m;
                }).bounds(x0 + i * (kaart + 6) + 4, top + 118, kaart - 8, 18).build());
            }
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.snuffel.keuze.terug"), b -> {
                pagina = 0;
                rebuildWidgets();
            }).bounds(left + 10, top + H - 26, 80, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.snuffel.keuze.klaar"), b -> klaar()).bounds(left + W - 118, top + H - 26, 108, 20)
                    .build());
        }
    }

    private void kiesRas(String id) {
        Honden.Ras r = Honden.ras(id);
        if (r == null) {
            return;
        }
        ras = id;
        if (!r.kleuren().contains(kleur)) {
            kleur = r.kleuren().get(0);
        }
        ververs();
        rebuildWidgets();
    }

    /** The dog on the left follows the choice. */
    private void ververs() {
        if (hond != null) {
            hond.zetHond(ras, kleur, false);
        }
    }

    private void klaar() {
        String schoon = Keuze.netjes(naam, minecraft != null && minecraft.player != null ? minecraft.player.getGameProfile().name() : "Snuffel");
        ClientPacketDistributor.sendToServer(new SnuffelPayloads.Kies(ras, kleur, schoon, maatje));
        onClose();
    }

    @Override
    public void tick() {
        ticks++;
        if (hond == null) {
            hond = HondClient.maak(ras, kleur, false);
        }
        if (hond != null) {
            hond.tickCount++;
            hond.loopt = false;
            // (it wags now and then, so every breed shows its tail)
            hond.zetHouding(ticks % 160 > 110 ? nl.juiced.guhs.feature.snuffel.Hondvorm.KWISPELT : 0);
        }
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < maatjes.length; i++) {
            if (maatjes[i] == null && mc.level != null) {
                MaatjeEntity m = SnuffelFeature.SNUFFEL_MAATJE.get().create(mc.level, EntitySpawnReason.TRIGGERED);
                if (m != null) {
                    m.zetSoort(Honden.MAATJES.get(i));
                    maatjes[i] = m;
                }
            }
            if (maatjes[i] != null) {
                maatjes[i].tickCount++;
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, RAND);
        g.fill(left, top, left + W, top + H, PAPIER);
        g.centeredText(font, Component.translatable(pagina == 0 ? "gui.guhs.snuffel.keuze.kop_hond" : "gui.guhs.snuffel.keuze.kop_maatje")
                .withStyle(ChatFormatting.BOLD), left + W / 2, top + 8, INKT);
        g.text(font, Component.translatable("gui.guhs.snuffel.keuze.stap", pagina + 1, 2), left + W - 44, top + 8, ZACHT, false);
        if (pagina == 0) {
            // the dog's stage
            g.fill(left + 10, top + 24, left + 130, top + H - 64, RAND);
            g.fill(left + 11, top + 25, left + 129, top + H - 65, VAK);
            g.text(font, Component.translatable("gui.guhs.snuffel.keuze.ras"), left + 138, top + 24, ZACHT, false);
            List<Honden.Ras> rassen = Honden.speelbaar();
            int yk = top + 34 + ((rassen.size() + 1) / 2) * 20 + 4;
            g.text(font, Component.translatable("gui.guhs.snuffel.keuze.kleur"), left + 138, yk, ZACHT, false);
            g.text(font, Component.translatable("gui.guhs.snuffel.keuze.naam"), left + 14, top + H - 58, ZACHT, false);
            g.text(font, Component.translatable("gui.guhs.snuffel.keuze.draai"), left + 14, top + H - 22, ZACHT, false);
            // a gold edge behind what is chosen
            for (int i = 0; i < rassen.size(); i++) {
                if (rassen.get(i).id().equals(ras)) {
                    int bx = left + 138 + (i % 2) * 90, by = top + 34 + (i / 2) * 20;
                    g.fill(bx - 2, by - 2, bx + 90, by + 20, GEKOZEN);
                }
            }
            Honden.Ras r = Honden.ras(ras);
            if (r != null) {
                int i = r.kleuren().indexOf(kleur);
                if (i >= 0) {
                    int by = yk + 10 + i * 20;
                    g.fill(left + 136, by - 2, left + 138 + 180, by + 20, GEKOZEN);
                }
            }
        } else {
            int kaart = 98, x0 = left + (W - 3 * kaart - 2 * 6) / 2;
            for (int i = 0; i < Honden.MAATJES.size(); i++) {
                int kx = x0 + i * (kaart + 6);
                boolean gekozen = Honden.MAATJES.get(i).equals(maatje);
                g.fill(kx, top + 24, kx + kaart, top + 140, gekozen ? GEKOZEN : RAND);
                g.fill(kx + (gekozen ? 2 : 1), top + 24 + (gekozen ? 2 : 1), kx + kaart - (gekozen ? 2 : 1), top + 140 - (gekozen ? 2 : 1), VAK);
            }
            // what the chosen one is like
            List<FormattedCharSequence> regels = new ArrayList<>(font.split(Component.translatable("gui.guhs.snuffel.maatje." + maatje + ".tekst"), W - 28));
            int y = top + 146;
            for (int i = 0; i < Math.min(3, regels.size()); i++) {
                g.text(font, regels.get(i), left + 14, y + i * 10, INKT, false);
            }
            g.text(font, Component.translatable("gui.guhs.snuffel.keuze.alleen_jij"), left + 14, top + 178, ZACHT, false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (pagina == 0) {
            if (hond != null) {
                int x1 = left + 11, y1 = top + 25, x2 = left + 129, y2 = top + H - 65;
                g.enableScissor(x1, y1, x2, y2);
                Tekenaar.teken(g, x1, y1, x2, y2, hond, draai, -8f, 66f, (y2 - y1) * 0.32f);
                g.disableScissor();
            }
        } else {
            int kaart = 98, x0 = left + (W - 3 * kaart - 2 * 6) / 2;
            for (int i = 0; i < maatjes.length; i++) {
                if (maatjes[i] != null) {
                    int x1 = x0 + i * (kaart + 6) + 2, y1 = top + 26, x2 = x1 + kaart - 4, y2 = top + 116;
                    boolean gekozen = Honden.MAATJES.get(i).equals(maatje);
                    g.enableScissor(x1, y1, x2, y2);
                    Tekenaar.teken(g, x1, y1, x2, y2, maatjes[i], gekozen ? (ticks + partialTick) * 3f : 20f, -6f, 86f, (y2 - y1) * 0.30f);
                    g.disableScissor();
                }
            }
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (pagina == 0 && event.x() >= left + 10 && event.x() <= left + 130 && event.y() >= top + 24 && event.y() <= top + H - 64) {
            draai += (float) dragX * 1.6f;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
