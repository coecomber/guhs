package nl.juiced.guhs.feature.guhwinter.spellen;

import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.world.ModDimensions;
import org.slf4j.Logger;

/**
 * What the spellen slice needs from the other Guhwinter slices (kern, plein), without a compile dependency on them (they
 * are built at the same time):
 * <ul>
 *   <li>the pepernoot, kern's item {@code guhs:guhwinter_pepernoot}, looked up by its id ({@link #pepernoot});</li>
 *   <li>the event phase, kern's {@code Guhwinter.fase(MinecraftServer)}, by reflection ({@link #fase}); "ONBEKEND" while kern
 *   isn't there (then everything is open, for testing);</li>
 *   <li>the plaza anchors, plein's {@code data/guhs/guhwinter/ankers.json} ({@link #anker}, {@link #ankers}), with this
 *   slice's own fall-backs while that file doesn't exist yet.</li>
 * </ul>
 * Integration may replace the insides of these methods with direct calls; the rest of the slice only uses them.
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
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Guhs.id(PEPERNOOT_ID));
        return item.orElse(null);
    }

    public static boolean isPepernoot(ItemStack stack) {
        Item p = pepernoot();
        return p != null && !stack.isEmpty() && stack.is(p);
    }

    /** How many pepernoten this player carries. */
    public static int tel(ServerPlayer player) {
        Item p = pepernoot();
        if (p == null) {
            return 0;
        }
        Inventory inv = player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(p)) {
                n += s.getCount();
            }
        }
        return n;
    }

    /** Takes n pepernoten from the player's pockets; false (and nothing taken) when there aren't enough. */
    public static boolean neem(ServerPlayer player, int n) {
        Item p = pepernoot();
        if (p == null || tel(player) < n) {
            return false;
        }
        Inventory inv = player.getInventory();
        int rest = n;
        for (int i = 0; i < inv.getContainerSize() && rest > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(p)) {
                int weg = Math.min(rest, s.getCount());
                s.shrink(weg);
                rest -= weg;
            }
        }
        inv.setChanged();
        return true;
    }

    /** Gives n pepernoten (what doesn't fit lands at the player's feet). False when the pepernoot doesn't exist (kern missing). */
    public static boolean geef(ServerPlayer player, int n) {
        Item p = pepernoot();
        if (n <= 0) {
            return true;
        }
        if (p == null) {
            LOG.warn("Guhwinter spellen: no item guhs:{} (kern missing?), {} pepernoten for {} not given", PEPERNOOT_ID, n,
                    player.getGameProfile().name());
            player.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.geen_pepernoot").withStyle(ChatFormatting.GRAY));
            return false;
        }
        int rest = n;
        while (rest > 0) {
            int stuk = Math.min(rest, p.getDefaultMaxStackSize());
            Minigames.give(player, new ItemStack(p, stuk));
            rest -= stuk;
        }
        return true;
    }

    // --- the phase -----------------------------------------------------------------------------------------------------

    public static final String VOOR = "VOOR", SINT = "SINT", PAKJESAVOND = "PAKJESAVOND", KERST = "KERST", OUDJAAR = "OUDJAAR",
            AFGELOPEN = "AFGELOPEN", ONBEKEND = "ONBEKEND";
    @Nullable
    private static Method fase;
    private static boolean faseGezocht;

    /** kern's GuhwinterFase as its name, or {@link #ONBEKEND} without kern. */
    public static String fase(MinecraftServer server) {
        if (!faseGezocht) {
            faseGezocht = true;
            try {
                fase = Class.forName("nl.juiced.guhs.feature.guhwinter.Guhwinter").getMethod("fase", MinecraftServer.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                LOG.info("Guhwinter spellen: no kern phase API yet ({}), every game is open", e.toString());
            }
        }
        if (fase == null) {
            return ONBEKEND;
        }
        try {
            Object f = fase.invoke(null, server);
            return f instanceof Enum<?> e ? e.name() : String.valueOf(f);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return ONBEKEND;
        }
    }

    /** The plaza games are open (everything but after the event). */
    public static boolean open(MinecraftServer server) {
        return !AFGELOPEN.equals(fase(server));
    }

    /** Oud & Nieuw (oliebollen at the stall); without kern always (testing). */
    public static boolean oudjaar(MinecraftServer server) {
        String f = fase(server);
        return OUDJAAR.equals(f) || ONBEKEND.equals(f);
    }

    // --- the anchors ---------------------------------------------------------------------------------------------------

    /** An anchor: a position and the way it looks. */
    public record Anker(String naam, Vec3 pos, float yaw) {
        public BlockPos blok() {
            return BlockPos.containing(pos);
        }
    }

    private static Map<String, Anker> ankers;
    private static long geladen;

    /** Forget the read file (the next question reads it again; e.g. after /reload or a plein change). */
    public static void vergeet() {
        ankers = null;
    }

    private static Map<String, Anker> alle(MinecraftServer server) {
        if (ankers != null && System.currentTimeMillis() - geladen < 60_000) {
            return ankers;
        }
        Map<String, Anker> uit = new HashMap<>();
        try {
            var res = server.getResourceManager().getResource(Guhs.id("guhwinter/ankers.json"));
            if (res.isPresent()) {
                try (Reader r = res.get().openAsReader()) {
                    JsonElement el = JsonParser.parseReader(r);
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.has("ankers") && obj.get("ankers").isJsonObject()) {
                        obj = obj.getAsJsonObject("ankers");
                    }
                    for (var e : obj.entrySet()) {
                        if (!e.getValue().isJsonObject()) {
                            continue;
                        }
                        JsonObject a = e.getValue().getAsJsonObject();
                        if (a.has("x") && a.has("y") && a.has("z")) {
                            uit.put(e.getKey(), new Anker(e.getKey(), new Vec3(a.get("x").getAsDouble(), a.get("y").getAsDouble(), a.get("z").getAsDouble()),
                                    a.has("yaw") ? a.get("yaw").getAsFloat() : 0f));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Guhwinter spellen: could not read guhs:guhwinter/ankers.json", e);
        }
        ankers = uit;
        geladen = System.currentTimeMillis();
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
