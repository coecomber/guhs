package nl.juiced.guhs.feature.erfenis;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.feature.erfenis.sherlock.Guhrrit;

/**
 * De Erfenis: the short intro up to Hoeve 2 (ontwerp, intro): makelaar Guhrrit waits in your village ("bouwkundig rapport:
 * vads") and walks you through it, driven only by the kern's moments:
 * <ol>
 *   <li>{@code aangekomen}: welcome, clear a first plukje;</li>
 *   <li>{@code plukje_opgeruimd}: build the (free) Kippenhok;</li>
 *   <li>{@code gebouw_gebouwd:kippenhok}: wait for the first resident (a Guhhutje gives room);</li>
 *   <li>{@code bewoners}: deliver a first order to a neighbour village (map / Marktplein);</li>
 *   <li>{@code bestelling_geleverd}: upgrade the Hoeve to level 2;</li>
 *   <li>{@code hoeve_level:2+}: goodbye: he goes on holiday and leaves his bust and the letterbox in the decoschuurtje.</li>
 * </ol>
 * In the village he appears next to you and talks (Praat screen); elsewhere the step comes as a chat line. A click on him
 * repeats the current step. A later moment (Hoeve 2 straight away) skips ahead; nothing ever blocks.
 */
public final class Makelaarstour {
    static final int KLAAR = 6;

    private Makelaarstour() {
    }

    static void init() {
        ErfenisMomenten.luister(Makelaarstour::opMoment);
        Guhrrit.bijKlik((p, g) -> {
            Base b = ErfenisData.get(p.level().getServer()).van(p.getUUID());
            if (b == null || b.tutorial <= 0 || b.tutorial >= KLAAR || !Erfenis.in(p) || p.distanceTo(g) > 8) {
                return false;
            }
            zeg(p, b, g);
            return true;
        });
    }

    static void opMoment(ServerPlayer p, String m) {
        MinecraftServer server = p.level().getServer();
        Base b = ErfenisData.get(server).van(p.getUUID());
        if (b == null || b.npc || b.tutorial >= KLAAR) {
            return;
        }
        int nieuw = b.tutorial;
        if (m.startsWith("hoeve_level:")) {
            if (getal(m) >= 2 && b.tutorial >= 1) {
                nieuw = KLAAR;
            }
        } else if (m.equals("aangekomen") && b.tutorial == 0) {
            nieuw = 1;
        } else if (m.startsWith("plukje_opgeruimd:") && b.tutorial == 1) {
            nieuw = 2;
        } else if (m.equals("gebouw_gebouwd:kippenhok") && b.tutorial == 2) {
            nieuw = 3;
        } else if (m.startsWith("bewoners:") && b.tutorial == 3) {
            nieuw = 4;
        } else if (m.startsWith("bestelling_geleverd:") && b.tutorial == 4) {
            nieuw = 5;
        }
        if (nieuw == b.tutorial) {
            return;
        }
        b.tutorial = nieuw;
        if (nieuw == KLAAR) {
            ErfenisDeco.geef(server, p.getUUID(), "borstbeeld_makelaar");
            ErfenisDeco.geef(server, p.getUUID(), "brievenbus");
        }
        ErfenisData.get(server).setDirty();
        Entity g = null;
        if (Erfenis.in(p) && Erfenis.onder(p) == b) {
            g = Guhrrit.verschijn((ServerLevel) p.level(), voor(p), p.getYRot() + 180f, nieuw == KLAAR);
        }
        zeg(p, b, g);
    }

    /** The current step: in the talking screen when he stands there, else as a chat line. */
    private static void zeg(ServerPlayer p, Base b, Entity g) {
        String k = "erfenis.tut." + b.tutorial;
        if (g != null) {
            List<String> regels = b.tutorial == 1 ? List.of("erfenis.tut.welkom", k) : List.of(k);
            Guhrrit.zeg(p, g, "erfenis_tut", regels);   // (he leaves by himself when nobody is near: the cast is temporary)
        } else {
            p.sendSystemMessage(Component.translatable("erfenis.tut.chat", Component.translatable(k)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static BlockPos voor(ServerPlayer p) {
        net.minecraft.world.phys.Vec3 kijk = p.getLookAngle();
        return BlockPos.containing(p.getX() + kijk.x * 2.5, p.getY(), p.getZ() + kijk.z * 2.5);
    }

    private static int getal(String m) {
        try {
            return Integer.parseInt(m.substring(m.lastIndexOf(':') + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
