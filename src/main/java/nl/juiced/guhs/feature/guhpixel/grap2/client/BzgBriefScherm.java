package nl.juiced.guhs.feature.guhpixel.grap2.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhpixel.grap2.Bzg;
import nl.juiced.guhs.feature.guhpixel.grap2.Grap2Payloads;
import nl.juiced.guhs.feature.guhpixel.grap2.Grap2Slice;

/**
 * A letter on cream paper. Two uses: the three letters for Boer Guhrrit from the mailbox of the show ("Scherm" = brieven:
 * a button per letter, a tick when it was read; every letter you open is reported to the server), and the framed
 * thank-you letter of the keepsake (brief: one letter, addressed to the reader).
 */
public class BzgBriefScherm extends Screen {
    private static final int W = 300, H = 184;
    private static final int PAPIER = 0xFFFFF6DC, RAND = 0xFFC9A46A, INKT = 0xFF4A3424, DOF = 0xFF9A8060, ROZE = 0xFFE0629E;

    private final boolean brieven, telt;
    private int gelezen, brief;
    private int left, top;

    public BzgBriefScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.bzg.brief.titel"));
        this.brieven = "brieven".equals(data.getStringOr("Scherm", ""));
        this.telt = data.getBooleanOr("Telt", false);
        this.gelezen = data.getIntOr("Gelezen", 0);
        // start with the first letter that was not read yet
        for (int i = Bzg.BRIEVEN - 1; i >= 0; i--) {
            if ((gelezen & (1 << i)) == 0) {
                brief = i;
            }
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        if (brieven) {
            int bw = (W - 16 - 8) / 3;
            for (int i = 0; i < Bzg.BRIEVEN; i++) {
                int n = i;
                Component label = Component.translatable("gui.guhs.bzg.brief.knop", i + 1);
                if ((gelezen & (1 << i)) != 0 || i == brief) {
                    label = Component.literal("✔ ").append(label);
                }
                Button b = addRenderableWidget(Button.builder(label, x -> open(n)).bounds(left + 8 + i * (bw + 4), top - 24, bw, 20).build());
                b.active = i != brief;
            }
            lees(brief);
        }
        addRenderableWidget(Button.builder(Component.translatable(brieven && brief < Bzg.BRIEVEN - 1 ? "gui.guhs.bzg.brief.volgende" : "gui.done"), b -> {
            if (brieven && brief < Bzg.BRIEVEN - 1) {
                open(brief + 1);
            } else {
                onClose();
            }
        }).bounds(left + W / 2 - 50, top + H + 6, 100, 20).build());
    }

    private void open(int i) {
        brief = i;
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Grap2Slice.GELUID_BRIEF.get(), 1f));
        }
        rebuildWidgets();
    }

    private void lees(int i) {
        if ((gelezen & (1 << i)) != 0) {
            return;
        }
        gelezen |= 1 << i;
        if (telt && minecraft != null && minecraft.getConnection() != null) {
            ClientPacketDistributor.sendToServer(new Grap2Payloads.BzgDoe(Grap2Payloads.GELEZEN, i));
        }
    }

    @Override
    public void onClose() {
        if (brieven && telt && minecraft != null && minecraft.getConnection() != null) {
            ClientPacketDistributor.sendToServer(new Grap2Payloads.BzgDoe(Grap2Payloads.BRIEVEN_DICHT, 0));
        }
        super.onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, RAND);
        g.fill(left, top, left + W, top + H, PAPIER);
        for (int y = top + 34; y < top + H - 30; y += 10) {
            g.fill(left + 12, y + 9, left + W - 12, y + 10, 0x30A08050);   // (ruled paper)
        }
        String basis = brieven ? "book.guhs.bzg.brief." + (brief + 1) : "book.guhs.bzg.bedankbrief";
        Component speler = minecraft != null && minecraft.player != null ? minecraft.player.getName() : Component.literal("guh");
        // a stamp with a little heart, top right
        g.fill(left + W - 34, top + 8, left + W - 10, top + 30, ROZE);
        g.fill(left + W - 32, top + 10, left + W - 12, top + 28, 0xFFFFE6EE);
        g.centeredText(font, Component.literal("❤"), left + W - 22, top + 15, ROZE);
        g.text(font, Component.translatable(basis + ".aanhef", speler).withStyle(ChatFormatting.ITALIC), left + 14, top + 14, INKT, false);
        List<FormattedCharSequence> regels = font.split(Component.translatable(basis + ".tekst", speler), W - 28);
        int y = top + 34;
        for (FormattedCharSequence r : regels) {
            if (y > top + H - 34) {
                break;
            }
            g.text(font, r, left + 14, y, INKT, false);
            y += 10;
        }
        Component groet = Component.translatable(basis + ".groet").withStyle(ChatFormatting.ITALIC);
        g.text(font, groet, left + W - 14 - font.width(groet), top + H - 22, INKT, false);
        if (brieven) {
            g.text(font, Component.translatable("gui.guhs.bzg.brief.teller", brief + 1, Bzg.BRIEVEN), left + 14, top + H - 22, DOF, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
