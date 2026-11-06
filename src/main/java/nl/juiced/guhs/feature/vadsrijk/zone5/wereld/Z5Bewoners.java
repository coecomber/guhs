package nl.juiced.guhs.feature.vadsrijk.zone5.wereld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Figurant;

/**
 * T-Z5WERELD: the townsfolk of Hoogvads. Every figure of the concept renders ({@link Z5WereldPlekken#BEWONERS}: shoppers on the
 * Marktlaan, refugees at the Grote Soepkeuken, students in the courtyard, knights of the Wolkwacht drilling on the oefenplein,
 * churchgoers in the pews...) becomes a passive guh prop ({@link Z3Figurant}: no physics, no hitbox, never saved) while a player is
 * within {@link #ZICHT} blocks; they vanish again when nobody is near. At most {@link #MAX} at once. They are decoration, not NPCs
 * (the story's NPCs are t-z5verhaal's); sitting figures sit, knights stand guard as Wolkwacht-ridders (T-WOLKWAL: {@code Wolkwacht}).
 */
public final class Z5Bewoners {
    public static final int ZICHT = 56, WEG = 80, MAX = 160;
    public static volatile boolean aan = true;
    private static final Map<Integer, Z3Figurant> NU = new HashMap<>();

    private Z5Bewoners() {
    }

    public static int aantal() {
        return NU.size();
    }

    static String textuur(Z5WereldPlekken.Bewoner b) {
        if (b.soort() == 2) {
            return "textures/entity/npc_wachterguh.png";
        }
        return "textures/entity/" + b.skin() + ".png";
    }

    /**
     * U-Z5: every skin on the model it was made for. The npc_* skins are sitting-guh characters (their own guh_npc_* model, the
     * sitting guh's animations), the konijntje has its own model, the uiltje is a bird (a snow guh here); the rest is guh fur on
     * the guh model (the renderer hides the saddle/armour/outfit bones, see Z3BazenClient.FigurantRenderer).
     */
    static String[] vorm(Z5WereldPlekken.Bewoner b) {
        String s = b.skin();
        boolean zit = b.soort() == 1;
        if (s.startsWith("npc_")) {
            return new String[]{"guh_npc_" + s.substring(4), "textures/entity/" + s + ".png", "guh_sitting", "animation.guh_sitting.idle"};
        }
        if (s.startsWith("guh_konijntje")) {
            return new String[]{"guh_konijntje", "textures/entity/" + s + ".png", "guh_konijntje", zit ? "zit" : "idle"};
        }
        if (s.equals("guh_uiltje")) {
            s = "guh_snow";
        }
        return new String[]{"guh", "textures/entity/" + s + ".png", "guh", zit ? "animation.guh.sit" : "animation.guh.idle"};
    }

    private static Z3Figurant maak(ServerLevel sl, Vec3 v, Z5WereldPlekken.Bewoner b) {
        String[] m = vorm(b);
        return Z3Figurant.maak(sl, v, b.yaw(), m[0], m[1], m[2], m[3], b.soort() == 3 ? 1.0f : 0.8f);
    }

    static void tick(ServerLevel sl) {
        if (!aan) {
            return;
        }
        List<ServerPlayer> spelers = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && Z5WereldPlekken.inZone5(p.getX(), p.getY(), p.getZ())) {
                spelers.add(p);
            }
        }
        // drop the ones nobody is near (or that got removed)
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
        List<Z5WereldPlekken.Bewoner> lijst = Z5WereldPlekken.BEWONERS;
        for (int i = 0; i < lijst.size() && NU.size() < MAX; i++) {
            if (NU.containsKey(i)) {
                continue;
            }
            Z5WereldPlekken.Bewoner b = lijst.get(i);
            Vec3 v = new Vec3(b.x(), b.y(), b.z());
            boolean dicht = false;
            for (ServerPlayer p : spelers) {
                if (p.position().distanceToSqr(v) < ZICHT * ZICHT) {
                    dicht = true;
                    break;
                }
            }
            if (!dicht || !sl.isLoaded(net.minecraft.core.BlockPos.containing(v))) {
                continue;
            }
            Z3Figurant f = b.soort() == 2   // T-WOLKWAL: the knights are Wolkwacht-ridders (own model)
                    ? nl.juiced.guhs.feature.vadsrijk.zone5.wolkwal.Wolkwacht.prop(sl, v, b.yaw(), nl.juiced.guhs.feature.vadsrijk.zone5.wolkwal.Wolkwacht.vacht(i), false, 0.85f)
                    : maak(sl, v, b);
            if (f != null) {
                NU.put(i, f);
            }
        }
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
