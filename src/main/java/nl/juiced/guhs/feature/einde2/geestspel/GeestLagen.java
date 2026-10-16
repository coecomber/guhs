package nl.juiced.guhs.feature.einde2.geestspel;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;

/**
 * einde2 phase B, slice geest_spel: the overlays of the Geestenwereld build that only ONE player sees (CONTRACT_E2_B 4.5,
 * 7.9), shown through {@link Schijn} (the world itself never changes for one player's story):
 * <ul>
 *   <li>{@value #WOUD} (the blooming forest, after Hei Bai-guh's acorn) and {@value #BOOM} (the lit Lantaarnboom, after the
 *       eight lanterns);</li>
 *   <li>{@value #PAD_8} (the path to the eighth island, {@link GeestLijnen#pad8}). A path of blocks only one client knows
 *       holds that player up (the client walks on what it sees); the server would call that floating, so while such a
 *       player is near the path the server lets them "fly" ({@code mayfly}), and takes that back when they leave it.</li>
 * </ul>
 * The blocks come from the overlay templates of the build ({@code lagen} in the sidecar), read once. Only blocks within
 * {@value #STRAAL} blocks of the player are sent, so a big overlay costs little.
 */
public final class GeestLagen {
    public static final String WOUD = "laag_woud_bloei", BOOM = "laag_lantaarnboom_licht", PAD_8 = "laag_pad_8";
    /** Only overlay blocks this near the player are shown (the client has the rest of the chunks too, but a second is fine). */
    public static final int STRAAL = 40;

    /** overlay -> its blocks in the world (not air, not structure_void); empty when the build has no such overlay. */
    private static final Map<String, Map<BlockPos, BlockState>> BLOKKEN = new ConcurrentHashMap<>();
    /** The players whose mayfly we switched on for path 8. */
    private static final Set<UUID> ZWEEFT = ConcurrentHashMap.newKeySet();

    private GeestLagen() {
    }

    /** Which overlays does this player see now? */
    static boolean ziet(ServerPlayer p, String laag) {
        return switch (laag) {
            case WOUD -> GeestLijnen.heiBai().klaar(p);
            case BOOM -> GeestLijnen.lantaarns().klaar(p);
            case PAD_8 -> GeestLijnen.pad8(p);
            default -> false;
        };
    }

    /** Schijn source: the overlays this player has earned, near them. */
    static void vul(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        if (p.level().dimension() != Geestenwereld.DIM) {
            return;
        }
        for (String laag : List.of(WOUD, BOOM, PAD_8)) {
            if (!ziet(p, laag)) {
                continue;
            }
            for (Map.Entry<BlockPos, BlockState> e : blokken(p.level(), laag).entrySet()) {
                BlockPos at = e.getKey();
                if (Math.abs(at.getX() - p.getBlockX()) <= STRAAL && Math.abs(at.getZ() - p.getBlockZ()) <= STRAAL
                        && Math.abs(at.getY() - p.getBlockY()) <= STRAAL && p.level().isLoaded(at)) {
                    gewenst.put(at, e.getValue());
                }
            }
        }
    }

    /** The blocks of an overlay in the world (read from its template once). */
    public static Map<BlockPos, BlockState> blokken(ServerLevel level, String laag) {
        return BLOKKEN.computeIfAbsent(laag, l -> lees(level, l));
    }

    private static Field palettes;

    @SuppressWarnings("unchecked")
    private static Map<BlockPos, BlockState> lees(ServerLevel level, String naam) {
        Plekken.Laag laag = Plekken.blad(Geestenwereld.NAAM).lagen().get(naam);
        Map<BlockPos, BlockState> uit = new HashMap<>();
        if (laag == null) {
            return uit;
        }
        StructureTemplate t = level.getStructureManager().get(laag.template()).orElse(null);
        if (t == null) {
            return uit;
        }
        try {
            if (palettes == null) {
                Field f = StructureTemplate.class.getDeclaredField("palettes");
                f.setAccessible(true);
                palettes = f;
            }
            List<StructureTemplate.Palette> lijst = (List<StructureTemplate.Palette>) palettes.get(t);
            if (lijst.isEmpty()) {
                return uit;
            }
            BlockPos o = Geestenwereld.OORSPRONG.offset(laag.pos());
            StructurePlaceSettings settings = new StructurePlaceSettings();
            for (StructureTemplate.StructureBlockInfo info : settings.getRandomPalette(lijst, o).blocks()) {
                BlockState s = info.state();
                if (s.isAir() || s.is(Blocks.STRUCTURE_VOID) || s.is(Blocks.JIGSAW) || s.is(Blocks.STRUCTURE_BLOCK)) {
                    continue;
                }
                uit.put(o.offset(info.pos()), s);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            com.mojang.logging.LogUtils.getLogger().warn("Guhs einde2 geest_spel: can't read the overlay {}", naam, e);
        }
        return uit;
    }

    /** (tests, dev) forget what was read (a new build). */
    static void vergeet() {
        BLOKKEN.clear();
    }

    // =====================================================================================================================
    // path 8: walking on blocks only your own client knows
    // =====================================================================================================================

    /** The box round path 8 (its blocks + a margin), or null when the build has no path. */
    @Nullable
    static AABB padDoos(ServerLevel level) {
        Map<BlockPos, BlockState> b = blokken(level, PAD_8);
        if (b.isEmpty()) {
            return null;
        }
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (BlockPos q : b.keySet()) {
            x0 = Math.min(x0, q.getX());
            y0 = Math.min(y0, q.getY());
            z0 = Math.min(z0, q.getZ());
            x1 = Math.max(x1, q.getX());
            y1 = Math.max(y1, q.getY());
            z1 = Math.max(z1, q.getZ());
        }
        return new AABB(x0 - 2, y0 - 3, z0 - 2, x1 + 3, y1 + 5, z1 + 3);
    }

    static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 10 != 3) {
            return;
        }
        boolean wil = false;
        if (p.level().dimension() == Geestenwereld.DIM && GeestLijnen.pad8(p) && !p.isSpectator() && !p.getAbilities().instabuild) {
            AABB d = padDoos(p.level());
            wil = d != null && d.contains(p.position());
        }
        if (wil && !p.getAbilities().mayfly) {
            p.getAbilities().mayfly = true;
            p.onUpdateAbilities();
            ZWEEFT.add(p.getUUID());
        } else if (!wil && ZWEEFT.remove(p.getUUID()) && !p.getAbilities().instabuild && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
    }

    /** Is the player standing on (or right above) a block of path 8 that they see? */
    static boolean opPad(ServerPlayer p) {
        Map<BlockPos, BlockState> b = blokken(p.level(), PAD_8);
        BlockPos onder = BlockPos.containing(p.position().add(0, -0.2, 0));
        return b.containsKey(onder) || b.containsKey(onder.below());
    }

    static void opWeg(ServerPlayer p) {
        if (ZWEEFT.remove(p.getUUID()) && !p.getAbilities().instabuild && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
    }

    /** The middle of an overlay (for a camera or a sparkle), or null. */
    @Nullable
    static Vec3 midden(ServerLevel level, String laag) {
        Map<BlockPos, BlockState> b = blokken(level, laag);
        if (b.isEmpty()) {
            return null;
        }
        double x = 0, y = 0, z = 0;
        for (BlockPos q : b.keySet()) {
            x += q.getX() + 0.5;
            y += q.getY();
            z += q.getZ() + 0.5;
        }
        return new Vec3(x / b.size(), y / b.size(), z / b.size());
    }
}
