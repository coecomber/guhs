package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers;
import nl.juiced.guhs.world.ModDimensions;
import org.slf4j.Logger;

/**
 * What the spellen slice needs from kern and plein, in one place (since the 1.10.10 integration these are direct calls):
 * <ul>
 *   <li>the pepernoot: kern's {@link Guhwinter#PEPERNOOT} and its helpers ({@link #pepernoot}, {@link #tel}, {@link #neem}, {@link #geef});</li>
 *   <li>the event phase: {@link Guhwinter#fase} ({@link #fase}, {@link #open}, {@link #oudjaar});</li>
 *   <li>the plaza anchors: plein's {@link GuhwinterAnkers} ({@link #anker}, {@link #ankers}), with this slice's own
 *   fall-backs for a name the file lacks.</li>
 * </ul>
 */
public final class SpelBrug {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String PEPERNOOT_ID = "guhwinter_pepernoot";
    /** The pond of the plaza (fall-back middle of the skating lap). */
    public static final BlockPos VIJVER = new BlockPos(-310, 31, -700);
    /** The plaza (kern's GuhwinterZone.PLEIN): x -350..-276, z -735..-661. */
    public static final int PLEIN_X0 = -350, PLEIN_X1 = -276, PLEIN_Z0 = -735, PLEIN_Z1 = -661, PLEIN_Y = 30;

    private SpelBrug() {
    }

    // --- the pepernoot -------------------------------------------------------------------------------------------------

    @Nullable
    public static Item pepernoot() {
        return Guhwinter.PEPERNOOT.get();
    }

    public static boolean isPepernoot(ItemStack stack) {
        Item p = pepernoot();
        return p != null && !stack.isEmpty() && stack.is(p);
    }

    /** How many pepernoten this player carries. */
    public static int tel(ServerPlayer player) {
        return Guhwinter.telPepernoten(player);
    }

    /** Takes n pepernoten from the player's pockets; false (and nothing taken) when there aren't enough. */
    public static boolean neem(ServerPlayer player, int n) {
        return Guhwinter.neemPepernoten(player, n);
    }

    /** Gives n pepernoten (what doesn't fit lands at the player's feet). */
    public static boolean geef(ServerPlayer player, int n) {
        if (n > 0) {
            Guhwinter.geefPepernoten(player, n);
        }
        return true;
    }

    // --- the phase -----------------------------------------------------------------------------------------------------

    public static final String VOOR = "VOOR", SINT = "SINT", PAKJESAVOND = "PAKJESAVOND", KERST = "KERST", OUDJAAR = "OUDJAAR",
            AFGELOPEN = "AFGELOPEN", ONBEKEND = "ONBEKEND";
    /** kern's GuhwinterFase as its name ({@link #ONBEKEND} is no longer returned; kept for old callers). */
    public static String fase(MinecraftServer server) {
        return Guhwinter.fase(server).name();
    }

    /** The plaza games are open (everything but after the event). */
    public static boolean open(MinecraftServer server) {
        return !AFGELOPEN.equals(fase(server));
    }

    /** Oud & Nieuw (oliebollen at the stall). */
    public static boolean oudjaar(MinecraftServer server) {
        return Guhwinter.fase(server).oudjaar();
    }

    // --- the anchors ---------------------------------------------------------------------------------------------------

    /** An anchor: a position and the way it looks. */
    public record Anker(String naam, Vec3 pos, float yaw) {
        public BlockPos blok() {
            return BlockPos.containing(pos);
        }
    }

    /** (kept for the callers; plein's anchors are read once from the mod jar) */
    public static void vergeet() {
    }

    private static Map<String, Anker> alle(MinecraftServer server) {
        Map<String, Anker> uit = new HashMap<>();
        for (GuhwinterAnkers.Anker a : GuhwinterAnkers.alle().values()) {
            uit.put(a.naam(), new Anker(a.naam(), a.vec(), a.yaw()));
        }
        return uit;
    }

    /** True when plein's anchors file is there. */
    public static boolean heeftAnkers(MinecraftServer server) {
        return !alle(server).isEmpty();
    }

    /** The anchor with this name, or the slice's own fall-back for it (null when there is none). */
    @Nullable
    public static Anker anker(MinecraftServer server, String naam) {
        Anker a = alle(server).get(naam);
        return a != null ? a : terugval(naam);
    }

    /** naam_0, naam_1, ... as long as they exist (from the file; else the fall-backs). */
    public static List<Anker> ankers(MinecraftServer server, String voorvoegsel) {
        Map<String, Anker> alle = alle(server);
        List<Anker> uit = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            Anker a = alle.get(voorvoegsel + "_" + i);
            if (a == null) {
                break;
            }
            uit.add(a);
        }
        if (uit.isEmpty()) {
            for (int i = 0; i < 64; i++) {
                Anker a = terugval(voorvoegsel + "_" + i);
                if (a == null) {
                    break;
                }
                uit.add(a);
            }
        }
        return uit;
    }

    /** The slice's own spots while plein's file isn't there: a ring round the pond and a few spots on the plaza. */
    @Nullable
    static Anker terugval(String naam) {
        if (naam.startsWith("vijver_route_")) {
            int i = Integer.parseInt(naam.substring("vijver_route_".length()));
            int n = 8;
            if (i >= n) {
                return null;
            }
            double hoek = Math.PI * 2 * i / n;
            return new Anker(naam, new Vec3(VIJVER.getX() + 0.5 + Math.cos(hoek) * 9, VIJVER.getY(), VIJVER.getZ() + 0.5 + Math.sin(hoek) * 9), 0f);
        }
        if (naam.startsWith("pepernoot_verstop_")) {
            int i = Integer.parseInt(naam.substring("pepernoot_verstop_".length()));
            int[][] plekken = {{-345, -730}, {-281, -730}, {-345, -666}, {-281, -666}, {-330, -690}, {-295, -715}};
            if (i >= plekken.length) {
                return null;
            }
            return new Anker(naam, new Vec3(plekken[i][0] + 0.5, PLEIN_Y + 1, plekken[i][1] + 0.5), 0f);
        }
        return switch (naam) {
            case "vijver_midden" -> new Anker(naam, Vec3.atBottomCenterOf(VIJVER), 0f);
            case "arcadekast" -> new Anker(naam, new Vec3(-300.5, PLEIN_Y + 1, -680.5), 180f);
            case "highscorebord_race" -> new Anker(naam, new Vec3(-298.5, PLEIN_Y + 1, -680.5), 180f);
            case "highscorebord_schaats" -> new Anker(naam, new Vec3(-318.5, PLEIN_Y + 1, -688.5), 180f);
            case "winkel" -> new Anker(naam, new Vec3(-290.5, PLEIN_Y + 1, -690.5), 90f);
            case "oliebollenkraam" -> new Anker(naam, new Vec3(-290.5, PLEIN_Y + 1, -700.5), 90f);
            default -> null;
        };
    }

    /** The world the plaza is in (the Guhmension; the overworld in a test world without it). */
    public static ServerLevel plein(MinecraftServer server) {
        ServerLevel l = server.getLevel(ModDimensions.GUHMENSION);
        return l != null ? l : server.overworld();
    }
}
