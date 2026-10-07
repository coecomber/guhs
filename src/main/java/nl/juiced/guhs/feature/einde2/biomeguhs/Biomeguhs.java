package nl.juiced.guhs.feature.einde2.biomeguhs;

import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.Einde2Bescherming;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.guheinde.Enderguhs;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2, slice biomeguhs: the guh variant of each biome of the outer islands and the wild ones that come around players.
 * <ul>
 *   <li>{@link #variantVoor}: which guh lives where.</li>
 *   <li>The Guheinde has no mob generation, so the wild ones come by a gentle per-player top-up ({@link #onLevelTick},
 *       {@link #aanvullen}): for every player on the outer islands (no Opper-Mika needed), a low cap for all six kinds
 *       together, only on island ground, never inside a protected einde2 structure. They come and go like the wild
 *       Enderguhs (the tag {@link Enderguhs#WILD}: never saved, gone when everybody is far away) and carry {@link #WILD}.
 *       Chunks made before einde2 keep the biome guhs:guheinde: there simply none come.</li>
 *   <li>You tame one with kaasknabbels like any guh; its tricks are in {@link Trucs}.</li>
 * </ul>
 */
public final class Biomeguhs {
    /** The entity tag of a wild biome guh of the top-up (next to Enderguhs.WILD, which makes it come and go). */
    public static final String WILD = "guhs_einde2_biomeguhs_wild";
    /** The top-up looks every this many ticks, per player; 1 in {@link #KANS} looks brings one. */
    public static final int TIJD = 600, KANS = 3;
    /** No new one when this many wild biome guhs (all six kinds together) are within 64 / 128 blocks of the player. */
    public static final int VOL = 2, VOL_WIJD = 3;

    /** The variant that lives in this biome (null: none, e.g. the main island and every chunk made before einde2). */
    @Nullable
    public static GuhVariant variantVoor(ResourceKey<Biome> biome) {
        if (biome == Einde2Ids.GATENKAASHEUVELS) {
            return GuhVariant.KORSTGUH;
        } else if (biome == Einde2Ids.KORSTWOESTIJN) {
            return GuhVariant.DUINGUH;
        } else if (biome == Einde2Ids.KRUIMELWOLKEN) {
            return GuhVariant.KRUIMELGUH;
        } else if (biome == Einde2Ids.SCHIMMELKAASVLAKTE) {
            return GuhVariant.SCHIMMELGUH;
        } else if (biome == Einde2Ids.KRISTALKAASKLIPPEN) {
            return GuhVariant.KRISTALGUH;
        } else if (biome == Einde2Ids.KNABBELKOORBOS) {
            return GuhVariant.KOORGUH;
        }
        return null;
    }

    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !GuheindeFeature.isGuheinde(level) || level.getGameTime() % TIJD != 310
                || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (level.getRandom().nextInt(KANS) == 0) {
                aanvullen(level, player, level.getRandom());
            }
        }
    }

    /** The per-player top-up: maybe a wild biome guh near this player (null: none came). */
    @Nullable
    public static GuhEntity aanvullen(ServerLevel level, ServerPlayer player, RandomSource random) {
        return aanvullen(level, player, random, pos -> level.getBiome(pos).unwrapKey().map(Biomeguhs::variantVoor).orElse(null));
    }

    /** The same with the biome question passed in (the headless test server has no Guheinde). */
    @Nullable
    static GuhEntity aanvullen(ServerLevel level, ServerPlayer player, RandomSource random, Function<BlockPos, GuhVariant> streek) {
        if (player.isSpectator() || vol(level, player)) {
            return null;
        }
        double a = random.nextDouble() * Math.PI * 2, d = 24 + random.nextInt(25);
        int x = (int) Math.floor(player.getX() + Math.cos(a) * d), z = (int) Math.floor(player.getZ() + Math.sin(a) * d);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return null;
        }
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        return op(level, pos, streek.apply(pos), random);
    }

    /** Are there enough wild biome guhs around this player already? */
    public static boolean vol(ServerLevel level, Player player) {
        return level.getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(64), Biomeguhs::isWild).size() >= VOL
                || level.getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(128), Biomeguhs::isWild).size() >= VOL_WIJD;
    }

    /**
     * A wild come-and-go guh of this variant on this spot, when it stands on island ground ({@code #guhs:einde2/eilandgrond}),
     * outside the protected einde2 structures, and there is room. Or null.
     */
    @Nullable
    public static GuhEntity op(ServerLevel level, BlockPos pos, @Nullable GuhVariant variant, RandomSource random) {
        if (variant == null || !variant.isBuitenGuh() || pos.getY() <= level.getMinY() || !level.getBlockState(pos.below()).is(Einde2Ids.EILANDGROND)
                || Einde2Bescherming.beschermd(level, pos)) {
            return null;
        }
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guh == null) {
            return null;
        }
        guh.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360f, 0);
        guh.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        guh.setVariant(variant);
        guh.addTag(Enderguhs.WILD);   // (GuhEntity#removeWhenFarAway / #shouldBeSaved: it comes and goes)
        guh.addTag(WILD);
        if (!level.noCollision(guh)) {
            guh.discard();
            return null;
        }
        level.addFreshEntity(guh);
        return guh;
    }

    /** A wild biome guh of the top-up (any of the six kinds). */
    public static boolean isWild(GuhEntity guh) {
        return guh.entityTags().contains(WILD) && !guh.isTame() && guh.isAlive();
    }

    /** Is this kruimelzand (the block or its layers)? By id: the biomes slice owns the blocks. */
    public static boolean isKruimelzand(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return id.equals("kruimelzand") || id.equals("kruimelzandlaag");
    }

    /** Whoever never sinks in kruimelzand: the Duinguh (the kruimelzand may ask this; the Duinguh also frees itself, see {@link Duingraver}). */
    public static boolean zaktNiet(Entity e) {
        return e instanceof GuhEntity guh && guh.getVariant() == GuhVariant.DUINGUH;
    }

    /** hook fase C: how much this guh counts on the Disco-dynamo (2 for a Koorguh, else 1). */
    public static int discoFactor(GuhEntity guh) {
        return guh.getVariant() == GuhVariant.KOORGUH ? 2 : 1;
    }

    private Biomeguhs() {
    }
}
