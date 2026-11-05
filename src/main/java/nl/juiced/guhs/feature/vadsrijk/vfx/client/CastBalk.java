package nl.juiced.guhs.feature.vadsrijk.vfx.client;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;

/**
 * The cast bar under the boss bar (roadmap 3.1/6.1): "Koning Klodder laadt: <b>Koninklijk Bad</b>", a bar that fills until the
 * attack hits, and for an interruptible cast a purple break meter under it ("Onderbreek!"). When it is stopped: "ONDERBROKEN!"
 * for a moment. Only one cast at a time (the newest).
 */
final class CastBalk {
    private static final class Cast {
        int id, ticks;
        String baas = "", naam = "";
        boolean onderbreek, onderbroken;
        long start, stop = -1;
        float breuk;
    }

    @Nullable
    private static Cast cast;

    private CastBalk() {
    }

    static void nieuw(CompoundTag d, long klok) {
        Cast c = new Cast();
        c.id = d.getIntOr("id", -1);
        c.baas = d.getStringOr("baas", "");
        c.naam = d.getStringOr("naam", "");
        c.ticks = Math.max(1, d.getIntOr("ticks", 40));
        c.onderbreek = d.getBooleanOr("onderbreek", false);
        c.start = klok;
        cast = c;
    }

    static void breuk(CompoundTag d) {
        Cast c = cast;
        if (c != null && c.id == d.getIntOr("id", -2)) {
            c.breuk = d.getFloatOr("breuk", 0);
        }
    }

    static void stop(CompoundTag d, long klok) {
        Cast c = cast;
        if (c != null && c.id == d.getIntOr("id", -2) && c.stop < 0) {
            c.stop = klok;
            c.onderbroken = d.getBooleanOr("onderbroken", false);
            if (c.onderbroken) {
                c.breuk = 1;
            }
        }
    }

    static void wis() {
        cast = null;
    }

    static void tick(long klok) {
        Cast c = cast;
        if (c != null && ((c.stop >= 0 && klok - c.stop > (c.onderbroken ? 40 : 10)) || klok - c.start > c.ticks + 200)) {
            cast = null;
        }
    }

    static void teken(GuiGraphicsExtractor g, DeltaTracker dt) {
        Cast c = cast;
        Minecraft mc = Minecraft.getInstance();
        if (c == null || mc.options.hideGui || mc.player == null) {
            return;
        }
        float pt = dt.getGameTimeDeltaPartialTick(false);
        long klok = VfxClient.klok();
        Font f = mc.font;
        int w = g.guiWidth(), bw = Math.min(200, w - 60), x = w / 2 - bw / 2;
        int y = RijkHud.bovenVrij() > 0 ? RijkHud.bovenVrij() + 2 : 8;
        float vol = c.stop >= 0 ? (c.onderbroken ? (klok - c.start) / (float) c.ticks : 1f) : Math.min(1f, (klok - c.start + pt) / c.ticks);
        float alfa = c.stop >= 0 && !c.onderbroken ? Math.max(0, 1 - (klok - c.stop + pt) / 10f) : 1f;
        int a = (int) (alfa * 255) << 24;
        if (alfa <= 0.02f) {
            return;
        }
        Component titel = c.onderbroken
                ? Component.translatable("vadsrijk.vfx.cast.onderbroken").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                : Component.translatable("vadsrijk.vfx.cast.laadt", Component.translatable(c.baas),
                        Component.translatable(c.naam).withStyle(ChatFormatting.BOLD, c.onderbreek ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD));
        g.text(f, titel, w / 2 - f.width(titel) / 2, y, a | 0xFFFFFF, true);
        int by = y + 11;
        g.fill(x - 1, by - 1, x + bw + 1, by + 6, a | 0x120A0E);
        g.fill(x, by, x + bw, by + 5, a | 0x3A2A1A);
        int kleur = c.onderbroken ? 0x8A8A8A : c.onderbreek ? 0xB060FF : 0xFFB040;
        g.fill(x, by, x + (int) (bw * Math.min(1, vol)), by + 5, a | kleur);
        g.fill(x, by, x + (int) (bw * Math.min(1, vol)), by + 1, a | 0xFFFFFF);
        if (c.onderbreek) {
            int my = by + 7;
            g.fill(x - 1, my - 1, x + bw + 1, my + 4, a | 0x120A0E);
            g.fill(x, my, x + (int) (bw * Math.min(1, c.breuk)), my + 3, a | (c.breuk >= 1 ? 0xFFF060 : 0xFFE0A0));
            if (!c.onderbroken) {
                Component o = Component.translatable("vadsrijk.vfx.cast.onderbreek");
                float knipper = 0.6f + 0.4f * (float) Math.sin((klok + pt) * 0.5f);
                g.text(f, o, w / 2 - f.width(o) / 2, my + 5, ((int) (alfa * knipper * 255) << 24) | 0xE0B0FF, true);
            }
        }
    }
}
