package nl.juiced.guhs.feature.einde2.reizen.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.reizen.E2ReizenPayloads;

/**
 * einde2 fix4-appa (playtest 3): the small dialog after a pick in Appa's menu. You sit on Appa; he waits until you click
 * "Yip yip!" (then he takes off for the place, everybody on his back comes along). Meanwhile other players can climb on
 * (a right-click on him): the dialog counts who is aboard. "Toch niet", Esc or getting off: he stays.
 */
public class YipYipScherm extends Screen {
    private static final int W = 260, H = 104;
    private static final int PANEL = 0xF0202A3A, BORDER = 0xFFF2C46B, TEXT = 0xFFFFF2DC;
    private final CompoundTag doel;
    private boolean gestuurd, zatOp;
    private int wacht;
    private int left, top;

    public YipYipScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.einde2_reizen.yip.titel"));
        this.doel = data.getCompoundOrEmpty("Doel");
    }

    @Override
    protected void init() {
        clearWidgets();
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_reizen.yip.knop").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                b -> stuur(true)).bounds(left + 10, top + H - 28, 130, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_reizen.yip.niet"), b -> stuur(false))
                .bounds(left + W - 10 - 100, top + H - 28, 100, 20).build());
    }

    private void stuur(boolean gaan) {
        if (!gestuurd) {
            gestuurd = true;
            ClientPacketDistributor.sendToServer(new E2ReizenPayloads.YipYip(gaan));
        }
        onClose();
    }

    @Override
    public void tick() {
        super.tick();
        Player p = Minecraft.getInstance().player;
        boolean op = p != null && p.getVehicle() instanceof AppaGuhEntity;
        zatOp |= op;
        wacht++;
        if (!op && (zatOp || wacht > 60)) {
            stuur(false);                                           // (got off: not going; the seat may reach us a bit later)
        }
    }

    @Override
    public void removed() {
        if (!gestuurd) {                                            // (Esc, or another screen came: he stays)
            gestuurd = true;
            ClientPacketDistributor.sendToServer(new E2ReizenPayloads.YipYip(false));
        }
        super.removed();
    }

    private int aanBoord() {
        Player p = Minecraft.getInstance().player;
        if (p == null || !(p.getVehicle() instanceof AppaGuhEntity appa)) {
            return 0;
        }
        int n = 0;
        for (Entity e : appa.getPassengers()) {
            if (e instanceof Player) {
                n++;
            }
        }
        return n;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, PANEL);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 10, top + 8, 0xFFFFD27A, false);
        MutableComponent naar = Component.translatable("gui.guhs.einde2_reizen.yip.naar", E2ReizenClient.naam(doel).withStyle(ChatFormatting.AQUA));
        Component extra = E2ReizenClient.extra(doel);
        if (extra != null) {
            naar.append(Component.literal(" ")).append(extra.copy().withStyle(ChatFormatting.GRAY));
        }
        int y = top + 22;
        for (FormattedCharSequence r : font.split(naar, W - 20)) {
            if (y > top + 34) {
                break;
            }
            g.text(font, r, left + 10, y, TEXT, false);
            y += 10;
        }
        Component boord = Component.translatable("gui.guhs.einde2_reizen.yip.aan_boord", aanBoord());
        List<FormattedCharSequence> regels = font.split(boord, W - 20);
        for (int i = 0; i < regels.size() && i < 2; i++) {
            g.text(font, regels.get(i), left + 10, top + 46 + i * 10, 0xFFB8C4D8, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
