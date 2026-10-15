package nl.juiced.guhs.feature.bio.wereld;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bio;

/**
 * biomes3 fix-klein, the Wolkenweide: nobody is hurt by a fall there. The high islands, the cloud castle and the bridges
 * put players 50 to 100 blocks above the meadow, and nothing in these biomes hurts.
 * <p>
 * The rule ({@link #zacht}), asked once per landing that would hurt (a {@link LivingFallEvent} of more than
 * {@link #VANAF} blocks, server side; no ticking, no scan): the spot where a living thing LANDS is soft when
 * <ul>
 *   <li>its biome is the Wolkenweide, or the Wolkenweide lies within {@link #RAND} blocks of it (a jump off an island at
 *       the biome's edge carries you a few blocks out, and the game blurs a biome's edge by a block or two); or</li>
 *   <li>it lies in the column of one of the six cloud buildings ({@link #GEBOUWEN}: their boxes reach from the meadow to
 *       the island, also where an island hangs over the neighbouring biome).</li>
 * </ul>
 * It holds for every living thing: the player, what they ride, what they lead. Where you came from does not count, so
 * it gives nothing outside the biome: a fall that ends more than {@link #RAND} blocks outside hurts as always.
 * <p>
 * It only ever takes damage away (multiplier 0) and runs after the other listeners: a landing that is already harmless
 * (the catch of your own wolkguh, a guh, a wolkenschaapje) is left alone, without a second puff. Cloud and rainbow blocks
 * never get here (their {@code fallOn} asks for no fall damage at all); the wolkenlift, the wolkenstroom, slow falling
 * and the giant's sneeze reset the fall distance themselves, gliding and riding a balloon end no fall.
 */
public final class WolkLanding {
    /** A fall of more than this many blocks can hurt (the game's own safe fall distance). */
    public static final double VANAF = 3.0;
    /** From this fall on, a soft landing makes a puff of cloud and a soft "poef". */
    public static final double POEF_VANAF = 8.0;
    /** The Wolkenweide this near counts too (blocks). */
    public static final int RAND = 6;
    /** The buildings that bring their own islands (slices bouw-wolk1 and bouw-wolk2). */
    public static final Set<Identifier> GEBOUWEN = Set.of(Guhs.id("wolkenhoeder_hut"), Guhs.id("sterrenwacht_ruine"), Guhs.id("luchtballon_haven"),
            Guhs.id("regenboogbrug"), Guhs.id("wolkenkasteeltje"), Guhs.id("bliksemsmidse"));

    /** Does a fall that ends here hurt nobody? */
    public static boolean zacht(ServerLevel level, BlockPos pos) {
        return inWeide(level, pos) || inGebouw(level, pos);
    }

    /** The Wolkenweide here, or within {@link #RAND} blocks. */
    public static boolean inWeide(ServerLevel level, BlockPos pos) {
        if (Bio.in(level, pos, Bio.WOLKENWEIDE)) {
            return true;
        }
        return Bio.in(level, pos.offset(RAND, 0, 0), Bio.WOLKENWEIDE) || Bio.in(level, pos.offset(-RAND, 0, 0), Bio.WOLKENWEIDE)
                || Bio.in(level, pos.offset(0, 0, RAND), Bio.WOLKENWEIDE) || Bio.in(level, pos.offset(0, 0, -RAND), Bio.WOLKENWEIDE);
    }

    /** In the column of a cloud building (the structure starts this chunk knows of; no chunk is loaded for it). */
    public static boolean inGebouw(ServerLevel level, BlockPos pos) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        var structuren = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), s -> {
            Identifier id = structuren.getKey(s);
            return id != null && GEBOUWEN.contains(id);
        })) {
            if (inKolom(start.getBoundingBox(), pos)) {
                return true;
            }
        }
        return false;
    }

    /** Is this spot in or under (or above) the box? */
    static boolean inKolom(BoundingBox doos, BlockPos pos) {
        return pos.getX() >= doos.minX() && pos.getX() <= doos.maxX() && pos.getZ() >= doos.minZ() && pos.getZ() <= doos.maxZ();
    }

    /** (WereldSlice, priority LOW) a living thing lands. */
    static void onVal(LivingFallEvent event) {
        LivingEntity wie = event.getEntity();
        if (event.getDistance() <= VANAF || event.getDamageMultiplier() <= 0f || !(wie.level() instanceof ServerLevel level)
                || !zacht(level, wie.blockPosition())) {
            return;
        }
        event.setDamageMultiplier(0f);
        if (event.getDistance() >= POEF_VANAF) {
            poef(level, wie, event.getDistance());
        }
    }

    /** A little puff of cloud and a soft sound where it landed. */
    static void poef(ServerLevel level, LivingEntity wie, double val) {
        int n = (int) Math.min(18, 6 + val * 0.25);
        level.sendParticles(ParticleTypes.CLOUD, wie.getX(), wie.getY() + 0.1, wie.getZ(), n, 0.35, 0.05, 0.35, 0.02);
        SoundEvent geluid = BuiltInRegistries.SOUND_EVENT.getOptional(Guhs.id("wolkenblok.stap")).orElse(SoundEvents.WOOL_FALL);
        level.playSound(null, wie.getX(), wie.getY(), wie.getZ(), geluid, SoundSource.NEUTRAL, (float) Math.min(1.0, 0.5 + val * 0.01), 0.7f);
    }

    private WolkLanding() {
    }
}
