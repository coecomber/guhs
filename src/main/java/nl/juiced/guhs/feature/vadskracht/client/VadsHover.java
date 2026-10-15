package nl.juiced.guhs.feature.vadskracht.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsPayloads;

/**
 * The vadskracht readout under the crosshair: ALWAYS there while you look at a block that makes, carries or uses
 * vadskracht (block tag {@code guhs:vadskracht}); no meter block, no item in the hand, no other mod needed. While the
 * crosshair is on such a block the client asks the server every {@link #VRAAG_ELKE} ticks ({@code guhs:vadskracht_kijk})
 * and draws the lines that come back ({@code guhs:vadskracht_stand}): "Deze opstelling gebruikt 16/20 vadskracht", why it
 * stands still, and what this block itself gives or asks.
 */
public final class VadsHover {
    private static final int VRAAG_ELKE = 10;
    /** Lines of another block stay this many ticks while the answer for the new block is on its way. */
    private static final int OUD_MAG = 15;
    private static final int ACHTER = 0x90201020, MAX_BREED = 250;

    @Nullable
    private static BlockPos kijkt;
    @Nullable
    private static BlockPos regelsVan;
    private static List<Component> regels = List.of();
    private static int wacht, oud;

    private VadsHover() {
    }

    /** The lines on screen right now (empty: nothing shown). For the autocheck. */
    public static List<Component> regels() {
        return kijkt == null ? List.of() : regels;
    }

    static void ontvang(VadsPayloads.Stand stand) {
        Minecraft.getInstance().execute(() -> {
            if (stand.pos().equals(kijkt)) {
                regels = List.copyOf(stand.regels());
                regelsVan = stand.pos();
                oud = 0;
            }
        });
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        BlockPos nu = null;
        if (mc.level != null && mc.player != null && mc.screen == null && mc.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK && mc.level.getBlockState(hit.getBlockPos()).is(VadsKracht.TOON)) {
            nu = hit.getBlockPos();
        }
        if (nu == null) {
            kijkt = null;
            regelsVan = null;
            regels = List.of();
            return;
        }
        if (!nu.equals(kijkt)) {
            kijkt = nu.immutable();
            wacht = 0;   // another block: ask at once
        }
        if (--wacht <= 0) {
            wacht = VRAAG_ELKE;
            ClientPacketDistributor.sendToServer(new VadsPayloads.Kijk(kijkt));
        }
        if (!kijkt.equals(regelsVan) && ++oud > OUD_MAG) {
            regels = List.of();
        }
    }

    static void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (kijkt == null || regels.isEmpty() || mc.options.hideGui || mc.screen != null) {
            return;
        }
        Font font = mc.font;
        int sw = g.guiWidth(), sh = g.guiHeight();
        int max = Math.min(MAX_BREED, sw - 16);
        List<FormattedCharSequence> rijen = new ArrayList<>();
        int breed = 0;
        for (Component regel : regels) {
            for (FormattedCharSequence rij : font.split(regel, max)) {
                rijen.add(rij);
                breed = Math.max(breed, font.width(rij));
            }
        }
        int x = sw / 2, y = sh / 2 + 16, hoogte = rijen.size() * 10;
        g.fill(x - breed / 2 - 4, y - 3, x + breed / 2 + 4, y + hoogte + 1, ACHTER);
        for (FormattedCharSequence rij : rijen) {
            g.centeredText(font, rij, x, y, 0xFFFFFFFF);
            y += 10;
        }
    }
}
