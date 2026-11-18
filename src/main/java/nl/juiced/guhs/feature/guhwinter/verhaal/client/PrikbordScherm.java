package nl.juiced.guhs.feature.guhwinter.verhaal.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * The prikbord of goede voornemens: every card on a cork board (the writer's name, their resolution), scrolled with the
 * mouse wheel; "Schrijf jouw voornemen" opens the writing card when you may write (after the six told theirs, during
 * Oud &amp; Nieuw).
 */
public class PrikbordScherm extends Screen {
    private static final int W = 330, H = 232, KOLOMMEN = 3, KW = 98, KH = 62, GAT = 8;
    private static final int KURK = 0xFFB8875A, KURK_DONKER = 0xFF8E6440, RAND = 0xFF6B4528, PAPIER = 0xFFFFF8E8, PAPIER_ROZE = 0xFFFFE4EE,
            INKT = 0xFF3A1E2E, PUNAISE = 0xFFE0384C;

    private record Kaartje(String naam, String tekst) {
    }

    private final CompoundTag data;
    private final List<Kaartje> kaartjes = new ArrayList<>();
    private int left, top, rij;

    public PrikbordScherm(CompoundTag data) {
        super(Component.translatable("guhwinter.verhaal.prikbord.titel"));
        this.data = data;
        for (Tag t : data.getListOrEmpty("Kaartjes")) {
            if (t instanceof CompoundTag c) {
                kaartjes.add(new Kaartje(c.getStringOr("Naam", "?"), c.getStringOr("Tekst", "")));
            }
        }
    }

    private int rijen() {
        return (kaartjes.size() + KOLOMMEN - 1) / KOLOMMEN;
    }

    private int zichtbaar() {
        return 2;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        if (data.getBooleanOr("Mag", false)) {
            boolean heeft = !data.getStringOr("Eigen", "").isEmpty();
            addRenderableWidget(Button.builder(Component.translatable(heeft ? "guhwinter.verhaal.prikbord.aanpassen" : "guhwinter.verhaal.prikbord.schrijf"),
                    b -> minecraft.setScreen(new VoornemenScherm(data))).bounds(left + 10, top + H - 28, 150, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("guhwinter.verhaal.prikbord.sluit"), b -> onClose())
                .bounds(left + W - 90, top + H - 28, 80, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        rij = Mth.clamp(rij - (int) Math.signum(scrollY), 0, Math.max(0, rijen() - zichtbaar()));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, RAND);
        g.fill(left, top, left + W, top + H, KURK);
        for (int i = 0; i < 70; i++) {          // cork speckles (always the same)
            int x = left + 4 + (i * 53) % (W - 8), y = top + 4 + (i * 37) % (H - 8);
            g.fill(x, y, x + 2, y + 1, KURK_DONKER);
        }
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 10, top + 8, 0xFFFFF8E8, true);
        if (kaartjes.isEmpty()) {
            List<FormattedCharSequence> leeg = font.split(Component.translatable("guhwinter.verhaal.prikbord.leeg"), W - 40);
            for (int i = 0; i < leeg.size(); i++) {
                g.text(font, leeg.get(i), left + 20, top + 60 + i * 11, 0xFFFFF8E8, true);
            }
        }
        int x0 = left + (W - (KOLOMMEN * KW + (KOLOMMEN - 1) * GAT)) / 2, y0 = top + 26;
        for (int r = 0; r < zichtbaar(); r++) {
            for (int k = 0; k < KOLOMMEN; k++) {
                int i = (rij + r) * KOLOMMEN + k;
                if (i >= kaartjes.size()) {
                    break;
                }
                kaartje(g, kaartjes.get(i), i, x0 + k * (KW + GAT), y0 + r * (KH + GAT + 4));
            }
        }
        if (rijen() > zichtbaar()) {
            String pagina = (rij + 1) + "-" + Math.min(rijen(), rij + zichtbaar()) + " / " + rijen();
            g.text(font, pagina, left + W - 12 - font.width(pagina), top + 8, 0xFFFFF8E8, true);
        }
    }

    private void kaartje(GuiGraphicsExtractor g, Kaartje k, int i, int x, int y) {
        int scheef = (i % 3) - 1;          // (each card hangs a little differently)
        g.fill(x + 2, y + 2 + scheef, x + KW + 2, y + KH + 2 + scheef, 0x50000000);
        g.fill(x, y + scheef, x + KW, y + KH + scheef, i % 2 == 0 ? PAPIER : PAPIER_ROZE);
        g.fill(x + KW / 2 - 2, y - 2 + scheef, x + KW / 2 + 2, y + 2 + scheef, PUNAISE);
        g.text(font, Component.literal(k.naam()).withStyle(ChatFormatting.BOLD), x + 5, y + 5 + scheef, PUNAISE, false);
        List<FormattedCharSequence> regels = font.split(Component.literal(k.tekst()), KW - 10);
        for (int r = 0; r < Math.min(4, regels.size()); r++) {
            g.text(font, regels.get(r), x + 5, y + 17 + r * 10 + scheef, INKT, false);
        }
    }
}
