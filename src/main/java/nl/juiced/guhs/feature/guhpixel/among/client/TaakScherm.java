package nl.juiced.guhs.feature.guhpixel.among.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhpixel.among.AmongPayloads;
import nl.juiced.guhs.taal.Tekst;

/**
 * The placeholder task panel: a bar that fills while you stay at the panel; full = the step is done. Every kind of task
 * (and both repair jobs) uses it until a real mini-game registers its own screen with AmongClient.taakScherm. Closing it
 * early gives the work up (the server forgets the start).
 */
public class TaakScherm extends Screen {
    private static final int W = 240, H = 118;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0, GROEN = 0xFF68D88A;

    private final CompoundTag data;
    private final int duur;
    private int teller;
    private boolean klaar;
    private int left, top;

    public TaakScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.among.taak." + data.getStringOr("Soort", "")));
        this.data = data;
        this.duur = Math.max(1, data.getIntOr("Duur", 100));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.among.taak.stop"), b -> onClose()).bounds(left + W / 2 - 45, top + H - 26, 90, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (klaar) {
            return;
        }
        if (++teller >= duur) {
            klaar = true;
            ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.TAAK_KLAAR, data.getIntOr("Paneel", -1), 0));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f, 0.6f));
            minecraft.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        if (!klaar) {
            ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.TAAK_STOP, data.getIntOr("Paneel", -1), 0));
        }
        super.onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 8, TEKST);
        int stappen = data.getIntOr("Stappen", 1);
        Component waar = stappen > 1
                ? Component.translatable("gui.guhs.among.taak.waar_stap", Tekst.get(data, "Kamer"), data.getIntOr("Stap", 1), stappen)
                : Component.translatable("gui.guhs.among.taak.waar", Tekst.get(data, "Kamer"));
        g.centeredText(font, waar, width / 2, top + 21, DOF);
        int y = top + 38;
        for (var regel : font.split(Component.translatable("gui.guhs.among.taak." + data.getStringOr("Soort", "") + ".bezig"), W - 24)) {
            g.centeredText(font, regel, width / 2, y, GOUD);
            y += 10;
        }
        int bx = left + 20, by = top + 66, bw = W - 40;
        float deel = Math.min(1f, (teller + partialTick) / duur);
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 13, RAND);
        g.fill(bx, by, bx + bw, by + 12, 0xFF1A0E15);
        g.fill(bx, by, bx + (int) (bw * deel), by + 12, GROEN);
        g.centeredText(font, Component.literal((int) (deel * 100) + "%"), width / 2, by + 2, TEKST);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
