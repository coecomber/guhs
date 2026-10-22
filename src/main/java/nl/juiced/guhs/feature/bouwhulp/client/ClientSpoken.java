package nl.juiced.guhs.feature.bouwhulp.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bouwhulp.kern.SpookPayloads;

/**
 * The ghosts the client knows (its dimension's, from {@link SpookPayloads}), and every half second the list of ghost
 * blocks to draw: per ghost within {@link #ZICHT} blocks, the spots where the world is not yet what the ghost wants,
 * nearest first, at most {@link #MAX_BLOKKEN} in all, each with the faces that are not hidden by a neighbour.
 */
public final class ClientSpoken {
    public static final int ZICHT = 128, MAX_BLOKKEN = 4000;

    /** One ghost as the client has it: every block where it lands. */
    public static final class Spook {
        final UUID id;
        final UUID eigenaar;
        final String eigenaarNaam, naam;
        final List<UUID> helpers = new ArrayList<>();
        final int status;
        int gedaan, rest, mist;
        @Nullable
        final BlockPos bouwer;
        /** Test round 5: the builder's reach (square, in x and z around {@link #bouwer}); MAX_VALUE: no limit. */
        final int bereik;
        final int buiten;
        final Long2ObjectOpenHashMap<BlockState> blokken = new Long2ObjectOpenHashMap<>();
        final BlockPos midden;

        Spook(CompoundTag t) {
            id = t.read("Id", UUIDUtil.CODEC).orElse(new UUID(0, 0));
            eigenaar = t.read("EigenaarId", UUIDUtil.CODEC).orElse(new UUID(0, 0));
            eigenaarNaam = t.getStringOr("Eigenaar", "");
            naam = t.getStringOr("Naam", "");
            status = t.getIntOr("Status", 0);
            gedaan = t.getIntOr("Gedaan", 0);
            rest = t.getIntOr("Rest", 0);
            bouwer = t.contains("Bouwer") ? BlockPos.of(t.getLongOr("Bouwer", 0L)) : null;
            bereik = t.getIntOr("Bereik", Integer.MAX_VALUE);
            buiten = t.getIntOr("Buiten", 0);
            var h = t.getListOrEmpty("Helpers");
            for (int i = 0; i < h.size(); i++) {
                try {
                    helpers.add(UUID.fromString(h.getStringOr(i, "")));
                } catch (IllegalArgumentException ignored) {
                    // (skip)
                }
            }
            BlockPos oorsprong = BlockPos.of(t.getLongOr("Oorsprong", 0L));
            Rotation draai = SpookPayloads.draai(t.getIntOr("Draai", 0));
            Mirror spiegel = SpookPayloads.spiegel(t.getIntOr("Spiegel", 0));
            int[] g = t.getIntArray("Grootte").orElse(new int[] {0, 0, 0});
            int[] palet = t.getIntArray("Palet").orElse(new int[0]);
            byte[] raster = t.getByteArray("Raster").orElse(new byte[0]);
            boolean breed = t.getBooleanOr("Breed", false);
            BlockState[] staten = new BlockState[palet.length];
            for (int i = 0; i < palet.length; i++) {
                staten[i] = Block.stateById(palet[i]).mirror(spiegel).rotate(draai);
            }
            BlockPos spil = new BlockPos((g[0] - 1) / 2, 0, (g[2] - 1) / 2);
            long sx = 0, sy = 0, sz = 0;
            for (int x = 0; x < g[0]; x++) {
                for (int y = 0; y < g[1]; y++) {
                    for (int z = 0; z < g[2]; z++) {
                        int i = (x * g[1] + y) * g[2] + z;
                        int v = breed ? (i * 2 + 1 < raster.length ? (raster[i * 2] & 0xFF) << 8 | raster[i * 2 + 1] & 0xFF : 0)
                                : (i < raster.length ? raster[i] & 0xFF : 0);
                        if (v <= 0 || v > staten.length) {
                            continue;
                        }
                        BlockPos p = StructureTemplate.transform(new BlockPos(x, y, z), spiegel, draai, spil).offset(oorsprong);
                        blokken.put(p.asLong(), staten[v - 1]);
                        sx += p.getX();
                        sy += p.getY();
                        sz += p.getZ();
                    }
                }
            }
            int n = Math.max(1, blokken.size());
            midden = new BlockPos((int) (sx / n), (int) (sy / n), (int) (sz / n));
        }

        public UUID id() {
            return id;
        }

        public String naam() {
            return naam;
        }

        public int gedaan() {
            return gedaan;
        }

        public int rest() {
            return rest;
        }

        @Nullable
        public BlockPos bouwer() {
            return bouwer;
        }

        public int status() {
            return status;
        }

        public int bereik() {
            return bereik;
        }

        /** Is this spot out of the builder's reach (the ghost block is drawn red)? */
        boolean buiten(BlockPos p) {
            return bouwer != null && bereik != Integer.MAX_VALUE
                    && (Math.abs(p.getX() - bouwer.getX()) > bereik || Math.abs(p.getZ() - bouwer.getZ()) > bereik);
        }

        /** Does the player own it or help with it (it is drawn a little stronger)? */
        boolean vanMij(UUID speler) {
            return eigenaar.equals(speler) || helpers.contains(speler);
        }
    }

    /** One block to draw: where, what, which faces (bit per {@link Direction#ordinal}), how strong. */
    record Teken(BlockPos pos, BlockState staat, int vlakken, boolean eigen, boolean overlapt, boolean buiten) {
    }

    private static final Map<UUID, Spook> SPOKEN = new ConcurrentHashMap<>();
    private static volatile List<Teken> teken = List.of();

    static void ontvang(CompoundTag t) {
        Spook s = new Spook(t);
        SPOKEN.put(s.id, s);
        herbereken();
    }

    static void weg(UUID id) {
        SPOKEN.remove(id);
        herbereken();
    }

    static void wegAlles() {
        SPOKEN.clear();
        teken = List.of();
    }

    static void voortgang(CompoundTag t) {
        t.read("Id", UUIDUtil.CODEC).map(SPOKEN::get).ifPresent(s -> {
            s.gedaan = t.getIntOr("Gedaan", s.gedaan);
            s.rest = t.getIntOr("Rest", s.rest);
            s.mist = t.getIntOr("Mist", 0);
        });
    }

    public static Collection<Spook> alle() {
        return SPOKEN.values();
    }

    static List<Teken> teken() {
        return teken;
    }

    /** Every half second (and when a ghost changes): which ghost blocks to draw. */
    static void herbereken() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || SPOKEN.isEmpty()) {
            teken = List.of();
            return;
        }
        Vec3 oog = mc.gameRenderer.getMainCamera().position();
        UUID ik = mc.player.getUUID();
        List<Teken> lijst = new ArrayList<>();
        List<Double> afstand = new ArrayList<>();
        for (Spook s : SPOKEN.values()) {
            if (!s.midden.closerToCenterThan(oog, ZICHT + 64)) {
                continue;
            }
            boolean eigen = s.vanMij(ik);
            BlockPos.MutableBlockPos buur = new BlockPos.MutableBlockPos();
            for (var e : s.blokken.long2ObjectEntrySet()) {
                BlockState doel = e.getValue();
                if (doel.isAir()) {
                    continue;
                }
                BlockPos p = BlockPos.of(e.getLongKey());
                double d = p.distToCenterSqr(oog);
                if (d > ZICHT * ZICHT || !level.isLoaded(p)) {
                    continue;
                }
                BlockState nu = level.getBlockState(p);
                if (nu == doel) {
                    continue;
                }
                int vlakken = 0;
                for (Direction dir : Direction.values()) {
                    buur.setWithOffset(p, dir);
                    BlockState b = s.blokken.get(buur.asLong()), wereld = level.getBlockState(buur);
                    boolean verstopt = b != null && b.canOcclude() && wereld != b || wereld.canOcclude();
                    if (!verstopt) {
                        vlakken |= 1 << dir.ordinal();
                    }
                }
                if (vlakken == 0) {
                    continue;
                }
                lijst.add(new Teken(p, doel, vlakken, eigen, !nu.isAir(), s.buiten(p)));
                afstand.add(d);
            }
        }
        if (lijst.size() > MAX_BLOKKEN) {
            Integer[] idx = new Integer[lijst.size()];
            for (int i = 0; i < idx.length; i++) {
                idx[i] = i;
            }
            java.util.Arrays.sort(idx, (a, b) -> Double.compare(afstand.get(a), afstand.get(b)));
            List<Teken> kort = new ArrayList<>(MAX_BLOKKEN);
            for (int i = 0; i < MAX_BLOKKEN; i++) {
                kort.add(lijst.get(idx[i]));
            }
            lijst = kort;
        }
        teken = List.copyOf(lijst);
    }

    private static final net.minecraft.core.particles.DustParticleOptions RAND =
            new net.minecraft.core.particles.DustParticleOptions(0xFF4060, 1.5f);

    /**
     * Test round 5: the keet's reach as a red-pink line on the ground near the ghost (within 40 blocks of it), while you move
     * it (its control panel or the keet menu is open) or while part of it lies outside.
     */
    static void bereikRand() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || level.getGameTime() % 5 != 0) {
            return;
        }
        boolean bezig = mc.screen instanceof SpookScherm || mc.screen instanceof BouwkeetScreen;
        for (Spook s : SPOKEN.values()) {
            if (s.bouwer == null || s.bereik == Integer.MAX_VALUE || !s.vanMij(mc.player.getUUID()) || !(bezig || s.buiten > 0)
                    || !s.midden.closerToCenterThan(mc.player.position(), 96)) {
                continue;
            }
            int x0 = s.bouwer.getX() - s.bereik, x1 = s.bouwer.getX() + s.bereik, z0 = s.bouwer.getZ() - s.bereik, z1 = s.bouwer.getZ() + s.bereik;
            for (int i = -40; i <= 40; i += 2) {
                for (int[] p : new int[][] {{s.midden.getX() + i, z0}, {s.midden.getX() + i, z1}, {x0, s.midden.getZ() + i}, {x1, s.midden.getZ() + i}}) {
                    if (p[0] < x0 || p[0] > x1 || p[1] < z0 || p[1] > z1 || !level.isLoaded(new BlockPos(p[0], s.midden.getY(), p[1]))) {
                        continue;
                    }
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p[0], p[1]);
                    level.addParticle(RAND, p[0] + 0.5, y + 0.3, p[1] + 0.5, 0, 0.02, 0);
                }
            }
        }
    }

    private ClientSpoken() {
    }
}
