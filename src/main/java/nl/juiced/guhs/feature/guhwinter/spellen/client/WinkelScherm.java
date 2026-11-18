package nl.juiced.guhs.feature.guhwinter.spellen.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhwinter.spellen.GuhwinterWinkel;
import nl.juiced.guhs.feature.guhwinter.spellen.SpelBrug;
import nl.juiced.guhs.feature.guhwinter.spellen.SpellenPayloads;

/**
 * Het pepernotenwinkeltje: a list of what's for sale (icon, name, a line about it, the price in pepernoten) and a
 * "Koop" button per line; your pepernoten at the top. The server takes the pepernoten and answers with your new count.
 */
public class WinkelScherm extends Screen {
    private static final int B = 300, RIJ = 26, RAND = 0xFFE0384C, PANEEL = 0xF0FFF4E6, TEKST = 0xFF3A1E2E, DOF = 0xFF8A6A5A;

    private int pepernoten;
    private final List<Integer> prijzen;
    private final List<Button> knoppen = new ArrayList<>();
    private int left, top0, hoog;
    private int flits = -1, flitsTicks;
    private boolean flitsGoed;

    public WinkelScherm(SpellenPayloads.WinkelOpen p) {
        super(Component.translatable("gui.guhs.guhwinter_spellen.winkel.titel"));
        this.pepernoten = p.pepernoten();
        this.prijzen = new ArrayList<>(p.prijzen());
    }

    public void stand(SpellenPayloads.WinkelStand s) {
        pepernoten = s.pepernoten();
        flits = s.waar();
        flitsGoed = s.gelukt();
        flitsTicks = 20;
        knoppenBij();
    }

    private int prijs(GuhwinterWinkel.Waar w) {
        return w.ordinal() < prijzen.size() ? prijzen.get(w.ordinal()) : w.prijs;
    }

    @Override
    protected void init() {
        int n = GuhwinterWinkel.Waar.values().length;
        hoog = 40 + n * RIJ + 30;
        left = (width - B) / 2;
        top0 = Math.max(4, (height - hoog) / 2);
        knoppen.clear();
        for (GuhwinterWinkel.Waar w : GuhwinterWinkel.Waar.values()) {
            int y = top0 + 34 + w.ordinal() * RIJ;
            knoppen.add(addRenderableWidget(Button.builder(Component.translatable("gui.guhs.guhwinter_spellen.winkel.koop"),
                    b -> ClientPacketDistributor.sendToServer(new SpellenPayloads.WinkelKoop(w.ordinal()))).bounds(left + B - 54, y + 3, 46, 18).build()));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.guhwinter_spellen.winkel.doei"), b -> onClose())
                .bounds(left + B / 2 - 40, top0 + hoog - 26, 80, 20).build());
        knoppenBij();
    }

    private void knoppenBij() {
        for (GuhwinterWinkel.Waar w : GuhwinterWinkel.Waar.values()) {
            if (w.ordinal() < knoppen.size()) {
                knoppen.get(w.ordinal()).active = pepernoten >= prijs(w);
            }
        }
    }

    @Override
    public void tick() {
        if (flitsTicks > 0) {
            flitsTicks--;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top0 - 2, left + B + 2, top0 + hoog + 2, RAND);
        g.fill(left, top0, left + B, top0 + hoog, PANEEL);
        for (int x = left; x < left + B; x += 16) {          // a striped awning on top
            g.fill(x, top0, Math.min(x + 8, left + B), top0 + 6, RAND);
        }
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 10, top0 + 12, RAND, false);
        String naam = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getGameProfile().name() : "Guh";
        ItemStack noot = SpelBrug.pepernoot() != null ? new ItemStack(SpelBrug.pepernoot()) : ItemStack.EMPTY;
        String jij = Integer.toString(pepernoten);
        int jx = left + B - 10 - font.width(jij);
        g.text(font, jij, jx, top0 + 12, TEKST, false);
        if (!noot.isEmpty()) {
            g.item(noot, jx - 18, top0 + 8);
        }
        for (GuhwinterWinkel.Waar w : GuhwinterWinkel.Waar.values()) {
            int y = top0 + 34 + w.ordinal() * RIJ;
            if (flits == w.ordinal() && flitsTicks > 0) {
                g.fill(left + 4, y, left + B - 4, y + RIJ - 2, flitsGoed ? 0x6060D070 : 0x60E0384C);
            } else if (w.ordinal() % 2 == 0) {
                g.fill(left + 4, y, left + B - 4, y + RIJ - 2, 0x20E0384C);
            }
            g.item(w.maak(naam), left + 8, y + 4);
            g.text(font, Component.translatable("gui.guhs.guhwinter_spellen.winkel." + w.id).withStyle(ChatFormatting.BOLD), left + 30, y + 3, TEKST, false);
            List<FormattedCharSequence> uitleg = font.split(Component.translatable("gui.guhs.guhwinter_spellen.winkel." + w.id + ".uitleg"), B - 30 - 110);
            if (!uitleg.isEmpty()) {
                g.text(font, uitleg.get(0), left + 30, y + 13, DOF, false);
            }
            String prijs = Integer.toString(prijs(w));
            int px = left + B - 62 - font.width(prijs);
            g.text(font, prijs, px, y + 8, pepernoten >= prijs(w) ? TEKST : 0xFFB04040, false);
            if (!noot.isEmpty()) {
                g.pose().pushMatrix();
                g.pose().translate(px - 12, y + 6);
                g.pose().scale(0.625f, 0.625f);
                g.item(noot, 0, 0);
                g.pose().popMatrix();
            }
            if (mouseX >= left + 8 && mouseX < left + 24 && mouseY >= y + 4 && mouseY < y + 20) {
                g.setTooltipForNextFrame(font, w.maak(naam).getHoverName(), mouseX, mouseY);
            }
        }
        if (noot.isEmpty()) {
            g.text(font, Component.translatable("gui.guhs.guhwinter_spellen.geen_pepernoot").withStyle(ChatFormatting.ITALIC), left + 10, top0 + hoog - 40, DOF, false);
        }
    }
}
