package nl.juiced.guhs.feature.beverbeek;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.SafariFeature;
import nl.juiced.guhs.world.WildeDieren;

/**
 * 1.6.2: wild beavers in the Beverbeek (the user: "een stuk meer bevers"). Families (a pair, often with one or two
 * little ones) are brought to a brook or pool near a player standing in the Beverbeek, the way the birds are topped up:
 * <ul>
 *   <li>they are safari bevers ({@link SafariDier}, soort bever, home at their bit of water) with the tags
 *       {@link SafariDier#WILD} and {@link WildeDieren#KOM_EN_GA}: never saved, gone when every player is far away
 *       (1.1.1 rule: wild things never pile up), and never in the safari dex;</li>
 *   <li>at most {@link #PER_SPELER} around one player and {@link #MAX_WERELD} (+ per player) in the whole level;</li>
 *   <li>pettable like Spaander ({@link Aaibever#aaibaar}), never tameable or takeable;</li>
 *   <li>now and then a beaver near wood gnaws at it (bouw_knaag), or carries a twig around (draag).</li>
 * </ul>
 */
public final class WildeBevers {
    /** The tag of a wild Beverbeek beaver (next to SafariDier.WILD and KOM_EN_GA). */
    public static final String TAG = "guhs_wilde_bever";
    public static final int ELKE = 20 * 8, PER_SPELER = 10, MAX_WERELD = 30, MAX_PER_SPELER = 12;
    static final int OM_DE = 20 * 4;
    /** How far from the player a family turns up (never right in front of you). */
    static final int VAN = 20, TOT = 44;

    private WildeBevers() {
    }

    static void registreer() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            int t = e.getServer().getTickCount();
            if (t % ELKE != 7 && t % OM_DE != 3) {
                return;
            }
            for (ServerLevel level : e.getServer().getAllLevels()) {
                if (level.players().isEmpty()) {
                    continue;
                }
                if (t % OM_DE == 3) {
                    bezig(level);
                }
                if (t % ELKE == 7) {
                    for (ServerPlayer p : level.players()) {
                        if (!p.isSpectator() && level.getBiome(p.blockPosition()).is(BeverbeekFeature.BIOME)) {
                            aanvullen(level, p, level.getRandom());
                        }
                    }
                }
            }
        });
    }

    public static boolean is(net.minecraft.world.entity.Entity e) {
        return e instanceof SafariDier d && d.entityTags().contains(TAG);
    }

    /** Brings a family when there are fewer than {@link #PER_SPELER} wild beavers around this player. Returns how many came. */
    public static int aanvullen(ServerLevel level, ServerPlayer p, RandomSource rnd) {
        if (level.getEntitiesOfClass(SafariDier.class, p.getBoundingBox().inflate(64, 32, 64), WildeBevers::is).size() >= PER_SPELER) {
            return 0;
        }
        int wereld = 0;
        for (var e : level.getAllEntities()) {
            if (is(e)) {
                wereld++;
            }
        }
        if (wereld >= MAX_WERELD + MAX_PER_SPELER * level.players().size()) {
            return 0;
        }
        BlockPos plek = waterplek(level, p.blockPosition(), rnd);
        return plek == null ? 0 : gezin(level, plek, rnd);
    }

    /** A spot on the bank of a Beverbeek brook or pool VAN..TOT blocks from {@code bij}, or null (a few probes). */
    @Nullable
    static BlockPos waterplek(ServerLevel level, BlockPos bij, RandomSource rnd) {
        for (int i = 0; i < 16; i++) {
            double hoek = rnd.nextDouble() * Math.PI * 2, d = VAN + rnd.nextDouble() * (TOT - VAN);
            int x = bij.getX() + (int) (Math.cos(hoek) * d), z = bij.getZ() + (int) (Math.sin(hoek) * d);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getBiome(top).is(BeverbeekFeature.BIOME)) {
                continue;
            }
            // water within 3 blocks: stand on the dry bank next to it
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos w = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, top.offset(dx, 0, dz)).below();
                    if (level.getFluidState(w).isSource() || level.getFluidState(w).is(net.minecraft.tags.FluidTags.WATER)) {
                        BlockPos oever = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, top);
                        if (level.getFluidState(oever.below()).isEmpty()) {
                            return oever;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** A pair and 0-2 little ones at this spot. Returns how many. */
    static int gezin(ServerLevel level, BlockPos plek, RandomSource rnd) {
        List<String> varianten = new java.util.ArrayList<>(List.of("man", "vrouw"));
        for (int i = rnd.nextInt(3); i > 0; i--) {
            varianten.add("baby");
        }
        int n = 0;
        for (String variant : varianten) {
            SafariDier bever = SafariFeature.DIER.get().create(level, EntitySpawnReason.NATURAL);
            if (bever == null) {
                continue;
            }
            bever.addTag(SafariDier.WILD);
            bever.addTag(WildeDieren.KOM_EN_GA);
            bever.addTag(TAG);
            bever.zet("bever", variant, plek, 12, -1, -1, 0);
            double x = plek.getX() + 0.5 + rnd.nextInt(3) - 1, z = plek.getZ() + 0.5 + rnd.nextInt(3) - 1;
            bever.snapTo(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)), z,
                    rnd.nextFloat() * 360f, 0f);
            if (level.noCollision(bever)) {
                level.addFreshEntity(bever);
                n++;
            }
        }
        return n;
    }

    /** Now and then: a wild beaver next to wood gnaws at it, another carries a twig around. */
    static void bezig(ServerLevel level) {
        RandomSource rnd = level.getRandom();
        for (ServerPlayer p : level.players()) {
            for (SafariDier b : level.getEntitiesOfClass(SafariDier.class, p.getBoundingBox().inflate(48, 24, 48), WildeBevers::is)) {
                if (rnd.nextInt(5) != 0 || b.isInWater()) {
                    continue;
                }
                if (houtDichtbij(level, b.blockPosition())) {
                    b.speelActie("bouw_knaag", 60 + rnd.nextInt(40));
                } else if (rnd.nextBoolean()) {
                    b.speelActie("draag", 80 + rnd.nextInt(60));
                }
            }
        }
    }

    static boolean houtDichtbij(ServerLevel level, BlockPos pos) {
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 2, 2))) {
            if (level.getBlockState(q).is(BlockTags.LOGS)) {
                return true;
            }
        }
        return false;
    }

    /** (Tests) the wild beavers in this box. */
    public static List<SafariDier> in(ServerLevel level, AABB box) {
        return level.getEntitiesOfClass(SafariDier.class, box, WildeBevers::is);
    }
}
