package nl.juiced.guhs.feature.safari.rit.client;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.juiced.guhs.feature.safari.rit.SafariRitten;
import nl.juiced.guhs.feature.safari.rit.TruckEntity;

/**
 * Safari FM (round 3, J2): the truck radio. While you ride a safari truck it plays its three little tunes in turn, and
 * between them the presenter reads a short silly ad in the action bar (gui.guhs.safari.fm.reclame.0..{@link #RECLAMES}-1).
 * Only for you (client side, quietly), and it stops the moment you get off.
 */
public final class SafariFm {
    public static final int RECLAMES = 8;
    /** Ticks per round: a tune (up to ~20 s) and then an ad with a little silence. */
    private static final int RONDE = 520, RECLAME_OP = 420;

    @Nullable
    private static SoundInstance speelt;
    private static int klok = -1, liedje, reclame;

    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean inTruck = mc.player != null && mc.player.getVehicle() instanceof TruckEntity;
        if (!inTruck) {
            if (speelt != null) {
                mc.getSoundManager().stop(speelt);
                speelt = null;
            }
            klok = -1;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        klok++;
        int fase = klok % RONDE;
        if (fase == 0) {
            if (klok == 0) {
                mc.gui.setOverlayMessage(Component.translatable("gui.guhs.safari.fm.aan").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
            speelt = SimpleSoundInstance.forUI(SafariRitten.FM.get(liedje % SafariRitten.FM.size()).get(), 1.0f, 0.35f);
            mc.getSoundManager().play(speelt);
            liedje++;
        } else if (fase == RECLAME_OP) {
            mc.gui.setOverlayMessage(Component.translatable("gui.guhs.safari.fm.reclame",
                    Component.translatable("gui.guhs.safari.fm.reclame." + (reclame % RECLAMES))).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            reclame++;
        }
    }

    private SafariFm() {
    }
}
