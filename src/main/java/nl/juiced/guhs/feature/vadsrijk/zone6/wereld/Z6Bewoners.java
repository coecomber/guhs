package nl.juiced.guhs.feature.vadsrijk.zone6.wereld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Figurant;

/**
 * U-Z6WERELD: the townsfolk and the mounts of the Zijdeland. Every figure of the map ({@link Z6WereldPlekken#BEWONERS}: shoppers on
 * the Rozenplein and in the bazaar, caravan riders, guards...) and every mount ({@link Z6WereldPlekken#RIJDIEREN}: bultguhs, flying
 * carpets and Pluimguhs at the stalls, the karavanserai, the camps and in the caravans; the user wants all three visible at NPCs,
 * stalls and structures) becomes a passive prop ({@link Z3Figurant}: no physics, no hitbox, never saved) while a player is within
 * {@link #ZICHT} blocks. At most {@link #MAX} at once.
 * <p>Mount models: the Pluimguh exists (vr_eco_pluimguh). The bultguh and the flying carpet get their model from the mount slices
 * (u-tapijt: vr_eco_tapijt; the bultguh model is not there yet): they register it in {@link #MODELLEN}; until then only the rider
 * (a sitting guh, also in BEWONERS) shows.
 */
public final class Z6Bewoners {
    public static final int ZICHT = 56, WEG = 80, MAX = 180;
    public static volatile boolean aan = true;

    /** soort -> model: geo + animation file name, texture per variant ("%s" = the variant), the idle animation, the scale. */
    public record Model(String geo, String textuur, String anim, float schaal) {
    }

    public static final Map<String, Model> MODELLEN = new ConcurrentHashMap<>(Map.of(
            "pluimguh", new Model("vr_eco_pluimguh", "textures/entity/vr_eco_pluimguh_%s.png", "idle", 1.0f)));
    private static final Map<Integer, Z3Figurant> NU = new HashMap<>();

    private Z6Bewoners() {
    }

    public static int aantal() {
        return NU.size();
    }

    static String pluimKleur(String v) {
        return switch (v) {
            case "roze", "mint", "honing", "lavendel", "goud", "wolkwit", "sterrennacht", "lampion", "kristal" -> v;
            default -> "roze";
        };
    }

    static void tick(ServerLevel sl) {
        if (!aan) {
            return;
        }
        List<ServerPlayer> spelers = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && Z6WereldPlekken.inZone6(p.getX(), p.getZ())) {
                spelers.add(p);
            }
        }
        NU.entrySet().removeIf(e -> {
            Z3Figurant f = e.getValue();
            boolean weg = f == null || f.isRemoved() || spelers.stream().noneMatch(p -> p.position().distanceToSqr(f.position()) < WEG * WEG);
            if (weg && f != null && !f.isRemoved()) {
                f.discard();
            }
            return weg;
        });
        if (spelers.isEmpty()) {
            return;
        }
        List<Z6WereldPlekken.Bewoner> lijst = Z6WereldPlekken.BEWONERS;
        int n = lijst.size();
        for (int i = 0; i < n + Z6WereldPlekken.RIJDIEREN.size() && NU.size() < MAX; i++) {
            if (NU.containsKey(i)) {
                continue;
            }
            Vec3 v;
            if (i < n) {
                Z6WereldPlekken.Bewoner b = lijst.get(i);
                v = new Vec3(b.x(), b.y(), b.z());
            } else {
                Z6WereldPlekken.Rijdier r = Z6WereldPlekken.RIJDIEREN.get(i - n);
                if (!MODELLEN.containsKey(r.soort())) {
                    continue;
                }
                v = new Vec3(r.x(), r.y(), r.z());
            }
            if (!dichtbij(spelers, v) || !sl.isLoaded(BlockPos.containing(v))) {
                continue;
            }
            Z3Figurant f;
            if (i < n) {
                Z6WereldPlekken.Bewoner b = lijst.get(i);
                f = Z3Figurant.maak(sl, v, b.yaw(), "guh", "textures/entity/" + b.skin() + ".png", "guh",
                        b.soort() == 1 ? "animation.guh.sit" : "animation.guh.idle", 0.8f);
            } else {
                Z6WereldPlekken.Rijdier r = Z6WereldPlekken.RIJDIEREN.get(i - n);
                Model m = MODELLEN.get(r.soort());
                String var = r.soort().equals("pluimguh") ? pluimKleur(r.variant()) : r.variant();
                f = Z3Figurant.maak(sl, v, r.yaw(), m.geo(), String.format(m.textuur(), var), m.geo(), m.anim(), m.schaal());
            }
            if (f != null) {
                NU.put(i, f);
            }
        }
    }

    private static boolean dichtbij(List<ServerPlayer> spelers, Vec3 v) {
        for (ServerPlayer p : spelers) {
            if (p.position().distanceToSqr(v) < ZICHT * ZICHT) {
                return true;
            }
        }
        return false;
    }

    public static void wis() {
        for (Z3Figurant f : NU.values()) {
            if (f != null && !f.isRemoved()) {
                f.discard();
            }
        }
        NU.clear();
    }
}
