package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;

/**
 * U-HUIZEN: the way in. In EVERY hub town a Kwartiermeester guh stands near the square (Zwamzoet, Kandijkom, Honingtak,
 * Lampionhaven, Hoogvads). The spot is found at runtime in a ring round the square (free, standable, lit floor, away from the other
 * NPCs) the first time a player is near, then it joins the npc slice's keeper ({@link Npcs#voegToe}). Clicking it: {@link Menus#poort}.
 */
public final class Poorten {
    public static final String PREFIX = "kk_poort_";

    /** Hub id, zone, POI of its square. */
    public record Hub(String id, int zone, String poi) {
        public String npc() {
            return PREFIX + id;
        }
    }

    public static final List<Hub> HUBS = List.of(new Hub("zwamzoet", 1, "plein"), new Hub("kandijkom", 2, "plein"), new Hub("honingtak", 3, "plein"),
            new Hub("lampionhaven", 4, "plein"), new Hub("hoogvads", 5, "markt_fontein"));
    private static final Map<String, BlockPos> GEZET = new ConcurrentHashMap<>();

    private Poorten() {
    }

    /** The square of a hub (null: not in this map). */
    @Nullable
    public static BlockPos plek(String hub) {
        for (Hub h : HUBS) {
            if (h.id().equals(hub)) {
                return anker(h);
            }
        }
        return null;
    }

    @Nullable
    static BlockPos anker(Hub h) {
        try {
            return h.zone() == 5 ? nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken.plek(h.poi()) : AmbachtPlekken.plek(h.zone(), h.poi());
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Nullable
    public static BlockPos waar(String hub) {
        return GEZET.get(hub);
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 23) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || sl.players().isEmpty()) {
            return;
        }
        for (Hub h : HUBS) {
            if (GEZET.containsKey(h.id())) {
                continue;
            }
            BlockPos a = anker(h);
            if (a == null || !sl.isLoaded(a) || !spelerBij(sl, a)) {
                continue;
            }
            BlockPos plek = zoek(sl, a, h);
            GEZET.put(h.id(), plek);
            float yaw = (float) (Math.toDegrees(Math.atan2(a.getZ() - plek.getZ(), a.getX() - plek.getX())) - 90);
            Npcs.voegToe(new NpcDef(h.npc(), plek, yaw, GuhVariant.SNOW, List.of(GuhClothes.KNIGHT_HELMET, GuhClothes.ROYAL_CAPE, GuhClothes.MONOCLE),
                    NpcDef.Gedrag.STAAN, null, 1.05f));
        }
    }

    private static boolean spelerBij(ServerLevel sl, BlockPos a) {
        for (ServerPlayer p : sl.players()) {
            if (p.blockPosition().distSqr(a) < 96 * 96) {
                return true;
            }
        }
        return false;
    }

    /** A free, standable, lit spot 6..13 blocks from the square, at least 3 from every other NPC. */
    static BlockPos zoek(ServerLevel sl, BlockPos a, Hub h) {
        for (int poging = 0; poging < 64; poging++) {
            double hoek = (200 + poging * 41) * Math.PI / 180, r = 6 + (poging % 8);
            BlockPos c = a.offset((int) Math.round(Math.cos(hoek) * r), 0, (int) Math.round(Math.sin(hoek) * r));
            BlockPos s = Npcs.staanPlek(sl, c);
            if (Math.abs(s.getY() - a.getY()) > 4 || !sl.getFluidState(s).isEmpty() || !sl.getBlockState(s.below()).isCollisionShapeFullBlock(sl, s.below())
                    || !sl.getBlockState(s).getCollisionShape(sl, s).isEmpty() || sl.getMaxLocalRawBrightness(s) == 0) {
                continue;
            }
            boolean vrij = true;
            for (NpcDef d : Npcs.alle()) {
                vrij &= d.plek().distSqr(s) >= 9;
            }
            if (vrij) {
                return s;
            }
        }
        return Npcs.staanPlek(sl, a.offset(4, 0, -4));   // (never without a Kwartiermeester)
    }

    /** Dev: find the spots again (after a map change). */
    static void vergeet() {
        GEZET.clear();
    }
}
