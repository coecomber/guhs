package nl.juiced.guhs.feature.hemel.client;

import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandNiveau;
import nl.juiced.guhs.feature.band.client.GuhPop;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.hemel.HemelPayloads;

import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The Knuffelhart's screen: every tamed guh of yours that is in the wolkjes (name, variant, hearts level, how long, and a
 * little stand-in with its clothes, turnable by dragging). Pick one and "Haal ... terug ♥": free, as often as you like.
 * Soft pink and gold, with little hearts floating up. Closes when you walk away from the heart.
 */
public class HemelScherm extends Screen {
    private static final int W = 320, H = 204, RIJ = 22, LIJST_W = 124, LIJST_H = 132;
    private static final int PANEL = 0xF6FFF6FB, RAND = 0xFFF2C14E, ROZE = 0xFFE0629E, DONKER = 0xFF5A2A48, GRIJS = 0xFF9A7890;

    private record Guh(UUID id, Component naam, String variant, int hartjes, int niveau, CompoundTag looks, long doodDag) {
    }

    private record Hartje(float x, float y, float snel, float fase, int kleur) {
    }

    private CompoundTag data;
    private final List<Guh> guhs = new ArrayList<>();
    private int gekozen, scroll, left, top;
    private float draai = 20, kanteling = 8;
    private GuhEntity pop;
    private UUID popVan;
    private Component netNaam;
    private int netTijd;
    private int tijd;
    private final List<Hartje> hartjes = new ArrayList<>();
    private final RandomSource rng = RandomSource.create();
    private Button knop;

    public HemelScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.hemel.scherm.titel"));
        update(data);
    }

    public void update(CompoundTag nieuw) {
        UUID was = guhs.isEmpty() ? null : guhs.get(Mth.clamp(gekozen, 0, guhs.size() - 1)).id();
        this.data = nieuw;
        guhs.clear();
        for (Tag t : nieuw.getListOrEmpty("Guhs")) {
            CompoundTag g = (CompoundTag) t;
            guhs.add(new Guh(g.read("Id", UUIDUtil.CODEC).orElseThrow(), nl.juiced.guhs.taal.Tekst.get(g, "Naam"), g.getStringOr("Variant", ""), g.getIntOr("Hartjes", 0), g.getIntOr("Niveau", 0),
                    g.getCompoundOrEmpty("Looks"), g.getLongOr("DoodDag", 0L)));
        }
        gekozen = 0;
        for (int i = 0; i < guhs.size(); i++) {
            if (guhs.get(i).id().equals(was)) {
                gekozen = i;
            }
        }
        if (nieuw.contains("Net")) {
            netNaam = nl.juiced.guhs.taal.Tekst.get(nieuw, "Net");
            netTijd = 100;
            for (int i = 0; i < 24; i++) {
                nieuwHartje(true);
            }
        }
        scroll = Mth.clamp(scroll, 0, Math.max(0, guhs.size() - LIJST_H / RIJ));
        if (width > 0) { // 1.1.0: minecraft is set in the constructor now; before init() the first init builds the widgets
            rebuildWidgets();
        }
    }

    private BlockPos hart() {
        return BlockPos.of(data.getLongOr("Pos", 0L));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        knop = addRenderableWidget(Button.builder(Component.empty(), b -> haalTerug())
                .bounds(left + 144, top + H - 28, W - 144 - 10, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knuffeldal.doei"), b -> onClose())
                .bounds(left + 10, top + H - 28, LIJST_W, 20).build());
        werkKnopBij();
    }

    private void werkKnopBij() {
        if (knop == null) {
            return;
        }
        knop.visible = !guhs.isEmpty();
        if (!guhs.isEmpty()) {
            knop.setMessage(Component.translatable("gui.guhs.hemel.scherm.knop", guhs.get(gekozen).naam()));
        }
    }

    private void haalTerug() {
        if (guhs.isEmpty()) {
            return;
        }
        ClientPacketDistributor.sendToServer(new HemelPayloads.Terug(data.getLongOr("Pos", 0L), guhs.get(gekozen).id()));
    }

    // --- input ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        int lx = left + 10, ly = top + 42;
        if (mx >= lx && mx < lx + LIJST_W && my >= ly && my < ly + LIJST_H) {
            int i = scroll + (int) ((my - ly) / RIJ);
            if (i >= 0 && i < guhs.size()) {
                gekozen = i;
                werkKnopBij();
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        nl.juiced.guhs.feature.hemel.HemelFeature.STER.get(), 1.6f, 0.4f));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (mx >= left + 144 && mx < left + W - 10 && my >= top + 42 && my < top + 42 + 96) {
            draai += (float) dx * 2.5f;
            kanteling = Mth.clamp(kanteling + (float) dy, -30, 40);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Mth.clamp(scroll - (int) Math.signum(sy), 0, Math.max(0, guhs.size() - LIJST_H / RIJ));
        return true;
    }

    // --- drawing -------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, RAND);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFFFE9A8);
        g.fill(left, top, left + W, top + H, PANEL);
        g.fillGradient(left, top, left + W, top + 34, 0xFFFFD6EA, PANEL);
        // little clouds along the bottom
        for (int i = 0; i < 9; i++) {
            int cx = left + 18 + i * 36, cy = top + H - 3;
            g.fill(cx - 12, cy - 5, cx + 12, cy, 0x40FFFFFF);
            g.fill(cx - 7, cy - 9, cx + 7, cy - 5, 0x40FFFFFF);
        }
        for (Hartje h : hartjes) {
            float y = h.y() - (tijd + partialTick) * h.snel();
            int a = (int) (Mth.clamp((y - top) / 40f, 0, 1) * 160);
            if (y > top && y < top + H) {
                g.text(font, "♥", (int) (h.x() + Mth.sin((tijd + partialTick) * 0.1f + h.fase()) * 3), (int) y, (a << 24) | h.kleur(), false);
            }
        }
        Component titel = Component.literal("♥ ").append(title).append(" ♥").withStyle(ChatFormatting.BOLD);
        g.text(font, titel, left + (W - font.width(titel)) / 2, top + 8, ROZE, false);
        Component sub = Component.translatable("gui.guhs.hemel.scherm.sub");
        g.text(font, sub, left + (W - font.width(sub)) / 2, top + 21, GRIJS, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (guhs.isEmpty()) {
            leeg(g);
            return;
        }
        lijst(g, mouseX, mouseY);
        kaart(g);
    }

    private void leeg(GuiGraphicsExtractor g) {
        int cy = top + 80;
        String hart = "♥";
        g.pose().pushMatrix();
        g.pose().translate(left + W / 2f, cy);
        float s = 3f + 0.25f * Mth.sin(tijd * 0.25f);
        g.pose().scale(s, s);
        g.text(font, hart, -font.width(hart) / 2, -4, ROZE, false);
        g.pose().popMatrix();
        Component a = Component.translatable("gui.guhs.hemel.scherm.leeg"), b = Component.translatable("gui.guhs.hemel.scherm.leeg2");
        g.text(font, a, left + (W - font.width(a)) / 2, cy + 26, DONKER, false);
        g.text(font, b, left + (W - font.width(b)) / 2, cy + 38, GRIJS, false);
        if (netNaam != null && netTijd > 0) {
            Component n = Component.translatable("gui.guhs.hemel.scherm.net", netNaam);
            g.text(font, n, left + (W - font.width(n)) / 2, cy + 56, 0xFFD89A1A, false);
        }
    }

    private void lijst(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int lx = left + 10, ly = top + 42;
        g.fill(lx - 1, ly - 1, lx + LIJST_W + 1, ly + LIJST_H + 1, 0xFFF4C6DC);
        g.fill(lx, ly, lx + LIJST_W, ly + LIJST_H, 0xFFFFFBFD);
        g.enableScissor(lx, ly, lx + LIJST_W, ly + LIJST_H);
        for (int i = scroll; i < guhs.size() && (i - scroll) * RIJ < LIJST_H; i++) {
            Guh guh = guhs.get(i);
            int y = ly + (i - scroll) * RIJ;
            boolean over = mouseX >= lx && mouseX < lx + LIJST_W && mouseY >= y && mouseY < y + RIJ;
            if (i == gekozen) {
                g.fill(lx, y, lx + LIJST_W, y + RIJ, 0xFFFFD6EA);
                g.fill(lx, y, lx + 2, y + RIJ, ROZE);
            } else if (over) {
                g.fill(lx, y, lx + LIJST_W, y + RIJ, 0xFFFFEEF6);
            }
            g.text(font, "☁", lx + 5, y + 7, 0xFFB8D4F0, false);
            g.text(font, font.plainSubstrByWidth(guh.naam().getString(), LIJST_W - 24), lx + 17, y + 3, DONKER, false);
            Component v = nl.juiced.guhs.entity.GuhVariant.byId(guh.variant()).displayName();
            g.pose().pushMatrix();
            g.pose().translate(lx + 17, y + 13);
            g.pose().scale(0.75f, 0.75f);
            g.text(font, font.plainSubstrByWidth(v.getString(), (int) ((LIJST_W - 24) / 0.75f)), 0, 0, GRIJS, false);
            g.pose().popMatrix();
        }
        g.disableScissor();
        if (guhs.size() * RIJ > LIJST_H) {
            int n = guhs.size() - LIJST_H / RIJ;
            int bar = Math.max(12, LIJST_H * (LIJST_H / RIJ) / guhs.size());
            int by = ly + (int) ((LIJST_H - bar) * (scroll / (float) Math.max(1, n)));
            g.fill(lx + LIJST_W - 3, by, lx + LIJST_W - 1, by + bar, 0xFFE8A0C4);
        }
    }

    private void kaart(GuiGraphicsExtractor g) {
        Guh guh = guhs.get(Mth.clamp(gekozen, 0, guhs.size() - 1));
        int kx = left + 144, ky = top + 42, kw = W - 144 - 10;
        // (3.0 QA: the card is 86 high, so the three text lines below it fit above the button without overlapping)
        g.fillGradient(kx, ky, kx + kw, ky + 86, 0xFFE8F4FF, 0xFFFFE6F2);
        // soft clouds under its feet
        g.fill(kx + 20, ky + 70, kx + kw - 20, ky + 82, 0x70FFFFFF);
        g.fill(kx + 32, ky + 64, kx + kw - 32, ky + 70, 0x70FFFFFF);
        if (pop == null || !guh.id().equals(popVan)) {
            pop = GuhPop.van(guh.looks());
            popVan = guh.id();
        }
        if (pop != null) {
            pop.tickCount = tijd;
            GuhPop.teken(g, kx, ky + 2, kx + kw, ky + 84, pop, draai, kanteling);
        }
        int y = ky + 89;
        GidsTekst.passend(g, guh.naam().copy().withStyle(ChatFormatting.BOLD), kx, y, kw, 1f, DONKER, false);
        Component niveau = BandNiveau.byIndex(guh.niveau()).naam();
        GidsTekst.passend(g, Component.translatable("gui.guhs.hemel.scherm.hartjes", guh.hartjes()).append(" · ").append(niveau), kx, y + 11, kw, 1f, ROZE, false);
        long dag = data.getLongOr("Dag", 0L);
        Component sinds = guh.doodDag() >= dag || guh.doodDag() < 0 ? Component.translatable("gui.guhs.hemel.scherm.vandaag")
                : Component.translatable("gui.guhs.hemel.scherm.sinds", guh.doodDag());
        GidsTekst.passend(g, sinds, kx, y + 22, kw, 1f, GRIJS, false);
        // "free, as often as you like": small, on the clouds at the bottom of the card
        Component gratis = Component.translatable("gui.guhs.hemel.scherm.gratis").withStyle(ChatFormatting.ITALIC);
        int gw = Math.round(font.width(gratis) * 0.75f);
        GidsTekst.passend(g, gratis, kx + (kw - Math.min(gw, kw - 8)) / 2, ky + 75, kw - 8, 0.75f, 0xFFC99A2E, false);
        if (netNaam != null && netTijd > 0) {
            Component n = Component.translatable("gui.guhs.hemel.scherm.net", netNaam);
            int nx = kx + (kw - font.width(n)) / 2;
            g.fill(nx - 4, ky + 3, nx + font.width(n) + 4, ky + 15, 0xE0FFF4C0);
            g.text(font, n, nx, ky + 5, 0xFFB07A10, false);
        }
    }

    // --- ticking -------------------------------------------------------------------------------------------------------

    private void nieuwHartje(boolean veel) {
        int[] kleuren = {0xFF7ABF, 0xFFB3DA, 0xF7C85A, 0xFF5FB4};
        float snel = 0.35f + rng.nextFloat() * 0.5f;
        hartjes.add(new Hartje(left + 8 + rng.nextFloat() * (W - 16), top + H - 8 + (veel ? rng.nextFloat() * 20 : 0) + tijd * snel,
                snel, rng.nextFloat() * 6f, kleuren[rng.nextInt(kleuren.length)]));
    }

    @Override
    public void tick() {
        tijd++;
        if (netTijd > 0) {
            netTijd--;
        }
        if (tijd % 12 == 0) {
            nieuwHartje(false);
        }
        hartjes.removeIf(h -> h.y() - tijd * h.snel() < top - 10);
        if (minecraft.player == null || minecraft.player.position().distanceTo(Vec3.atCenterOf(hart())) > 10) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
