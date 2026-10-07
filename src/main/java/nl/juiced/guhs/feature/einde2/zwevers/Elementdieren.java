package nl.juiced.guhs.feature.einde2.zwevers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.world.WildeDieren;

/**
 * einde2, slice zwevers: the wild element animals. Most of them live in the story structures and the side places (the
 * buildings place them); a rare wild one walks in the biome of its element: lucht = Kruimelwolken (vleermuis-lemurguh),
 * water = Schimmelkaasvlakte (otter-pinguïnguh), aarde = Kristalkaasklippen (das-molguh), vuur = Knabbelkoorbos
 * (schildpad-eendguh).
 * <p>
 * A gentle top-up around every player ({@link #aanvullen}): every {@link #TIJD} ticks, 1 in {@link #KANS}, one animal of the
 * biome the player stands in comes 24 to 48 blocks away, on the ground (the schildpad-eendguh also on water), when fewer
 * than {@link #VOL} of its kind are within {@link #BEREIK} blocks. They come and go (WildeDieren). Old chunks keep the
 * biome {@code guhs:guheinde}: there simply none come.
 */
public final class Elementdieren {
    public static final int TIJD = 600, KANS = 3;
    /** No new one when this many of its kind are within {@link #BEREIK} blocks of the player. */
    public static final int VOL = 2, BEREIK = 96;

    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !GuheindeFeature.isGuheinde(level) || level.getGameTime() % TIJD != 310
                || !level.getGameRules().get(GameRules.SPAWN_MOBS)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (level.getRandom().nextInt(KANS) == 0) {
                aanvullen(level, player, level.getRandom());
            }
        }
    }

    /** The element whose biome this is (null: not the biome of an element). */
    @Nullable
    public static Einde2Ids.Element elementVan(Holder<Biome> biome) {
        for (Einde2Ids.Element element : Einde2Ids.Element.values()) {
            if (biome.is(element.biome())) {
                return element;
            }
        }
        return null;
    }

    /** The animal of an element. */
    public static EntityType<? extends Elementdier> soort(Einde2Ids.Element element) {
        return switch (element) {
            case LUCHT -> E2ZweversFeature.VLEERMUIS_LEMURGUH.get();
            case WATER -> E2ZweversFeature.OTTER_PINGUINGUH.get();
            case AARDE -> E2ZweversFeature.DAS_MOLGUH.get();
            case VUUR -> E2ZweversFeature.SCHILDPAD_EENDGUH.get();
        };
    }

    /** May one more of a kind come, with this many wild ones of it around already? */
    public static boolean magErbij(int inDeBuurt) {
        return inDeBuurt < VOL;
    }

    /** How many wild (come-and-go) animals of this kind are within {@link #BEREIK} blocks of the player. */
    public static int inDeBuurt(ServerLevel level, ServerPlayer player, EntityType<? extends Elementdier> soort) {
        AABB rond = player.getBoundingBox().inflate(BEREIK);
        return level.getEntities(soort, rond, d -> d.isAlive() && WildeDieren.isKomEnGa(d)).size();
    }

    /** One wild animal of the element of this player's biome, 24 to 48 blocks away. Or null: none came. */
    @Nullable
    public static Elementdier aanvullen(ServerLevel level, ServerPlayer player, RandomSource random) {
        Einde2Ids.Element element = player.isSpectator() ? null : elementVan(level.getBiome(player.blockPosition()));
        if (element == null || !magErbij(inDeBuurt(level, player, soort(element)))) {
            return null;
        }
        double a = random.nextDouble() * Math.PI * 2, d = 24 + random.nextInt(25);
        int x = (int) Math.floor(player.getX() + Math.cos(a) * d), z = (int) Math.floor(player.getZ() + Math.sin(a) * d);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return null;
        }
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        if (elementVan(level.getBiome(pos)) != element) {
            return null;
        }
        return op(level, pos, soort(element), random);
    }

    /**
     * A wild come-and-go animal on this spot, when there is ground under it (not the void; the schildpad-eendguh also takes
     * water) and room. Or null.
     */
    @Nullable
    public static <T extends Elementdier> T op(ServerLevel level, BlockPos pos, EntityType<T> soort, RandomSource random) {
        if (pos.getY() <= level.getMinY()) {
            return null;
        }
        BlockPos onder = pos.below();
        boolean grond = level.getBlockState(onder).isFaceSturdy(level, onder, Direction.UP);
        boolean water = soort == E2ZweversFeature.SCHILDPAD_EENDGUH.get() && level.getFluidState(onder).is(FluidTags.WATER);
        if (!grond && !water) {
            return null;
        }
        T dier = soort.create(level, EntitySpawnReason.TRIGGERED);
        if (dier == null) {
            return null;
        }
        dier.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360f, 0);
        dier.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        WildeDieren.markeer(dier);
        if (!level.noCollision(dier)) {
            dier.discard();
            return null;
        }
        level.addFreshEntity(dier);
        return dier;
    }

    private Elementdieren() {
    }
}
