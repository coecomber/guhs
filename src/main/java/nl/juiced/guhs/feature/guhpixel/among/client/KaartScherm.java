package nl.juiced.guhs.feature.guhpixel.among.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhpixel.among.AmongPayloads;
import nl.juiced.guhs.taal.Tekst;

/**
 * The Mika's ship map. From the Saboteerkaart: Licht uit, Knabbelalarm, or the doors of one room. From a vent: the other
 * vents of that network, to crawl to. The server checks everything again (cooldowns, the role, the distance).
 */
public class KaartScherm extends Screen {
    private static final int W = 260, H = 196;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0;

    private final CompoundTag data;
    private final int luik;
    private int afkoel;
    private int left, top;

    public KaartScherm(CompoundTag data) {
        super(Component.translatable(data.getIntOr("Luik", -1) >= 0 ? "gui.guhs.among.kaart.luik" : "gui.guhs.among.kaart.sabotage"));
        this.data = data;
        this.luik = data.getIntOr("Luik", -1);
        this.afkoel = data.getIntOr("Afkoel", 0);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        ListTag kamers = data.getListOrEmpty("Kamers");
        boolean kan = afkoel == 0;
        if (luik >= 0) {
            for (int i = 0; i < kamers.size(); i++) {
                CompoundTag k = kamers.getCompoundOrEmpty(i);
                int naar = k.getIntOr("Idx", -1);
                addRenderableWidget(Button.builder(Component.translatable("gui.guhs.among.kaart.naar", Tekst.get(k, "Naam")), b -> {
                    ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.LUIK, luik, naar));
                    onClose();
                }).bounds(left + 40, top + 40 + i * 24, W - 80, 20).build());
            }
        } else {
            Button licht = Button.builder(Component.translatable("gui.guhs.among.kaart.licht"), b -> saboteer(1, -1)).bounds(left + 12, top + 34, 116, 20).build();
            Button alarm = Button.builder(Component.translatable("gui.guhs.among.kaart.alarm"), b -> saboteer(2, -1)).bounds(left + 132, top + 34, 116, 20).build();
            licht.active = alarm.active = kan;
            addRenderableWidget(licht);
            addRenderableWidget(alarm);
            for (int i = 0; i < kamers.size(); i++) {
                CompoundTag k = kamers.getCompoundOrEmpty(i);
                int kamer = k.getIntOr("Idx", -1);
                Button deur = Button.builder(Tekst.get(k, "Naam"), b -> saboteer(3, kamer)).bounds(left + 12 + (i % 3) * 80, top + 76 + (i / 3) * 24, 76, 20).build();
                deur.active = kan;
                addRenderableWidget(deur);
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(left + W / 2 - 40, top + H - 26, 80, 20).build());
    }

    private void saboteer(int soort, int kamer) {
        ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.SABOTEER, soort, kamer));
        onClose();
    }

    @Override
    public void tick() {
        super.tick();
        if (afkoel > 0 && --afkoel == 0) {
            rebuildWidgets();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 8, TEKST);
        if (luik >= 0) {
            g.centeredText(font, Component.translatable("gui.guhs.among.kaart.luik.uitleg"), width / 2, top + 22, DOF);
        } else {
            Component stand = afkoel < 0 ? Component.translatable("gui.guhs.among.kaart.bezig")
                    : afkoel > 0 ? Component.translatable("gui.guhs.among.kaart.afkoel", AmongClient.seconden(afkoel)) : Component.translatable("gui.guhs.among.kaart.kies");
            g.centeredText(font, stand, width / 2, top + 21, afkoel == 0 ? GOUD : DOF);
            g.text(font, Component.translatable("gui.guhs.among.kaart.deuren"), left + 12, top + 63, TEKST, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
