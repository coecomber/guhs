package nl.juiced.guhs.feature.guhriobeloning;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.gatenkaas.Knabbelgeluid;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * A trip through a green pipe ({@link PijpBlock}): from the mouth you stand on to the nearest other mouth of the same
 * colour within {@link #BEREIK} blocks (in loaded chunks, with two blocks of room above it).
 * <ul>
 *     <li>It starts when you sneak on a mouth ({@link #tick}, every tick of a player) or click the pipe you stand on. You
 *     must let go of sneak before the next trip, so you do not bounce straight back.</li>
 *     <li>The little film: {@link #IN_TICKS} ticks you slide down into the pipe (the client draws you sinking, your own
 *     screen closes like an iris and your keys do nothing; {@link GuhrioBeloningPayloads.Film}), then you stand on the
 *     other mouth and rise out of it for {@link #UIT_TICKS} ticks. The classic three notes go down and come back up.</li>
 *     <li>Nothing is lost and nobody is hurt: no fall, no damage on the way. Riders get off first; a player in a
 *     minigame (a Guhrio level too) or a cutscene does not travel.</li>
 * </ul>
 * The partner is looked up at the moment you go in, by the palettes of the chunk sections around you (no list of pipes is
 * kept anywhere, so a pipe placed by a building's template works like one you placed yourself).
 */
public final class Pijpreis {
    /** How far apart two mouths of a pair may be (blocks, straight line). */
    public static final int BEREIK = 50;
    public static final int IN_TICKS = 10, UIT_TICKS = 10;
    /** Who is this near a traveller sees the film. */
    private static final double ZICHT = 64;

    /** A trip under way. */
    private static final class Reis {
        final ResourceKey<Level> dim;
        final BlockPos van, naar;
        int tick;

        Reis(ResourceKey<Level> dim, BlockPos van, BlockPos naar) {
            this.dim = dim;
            this.van = van;
            this.naar = naar;
        }
    }

    /** After a trip a pipe leaves you alone for this many ticks, whatever your keys say. */
    public static final int RUST = 20;

    private static final Map<UUID, Reis> REIZEN = new ConcurrentHashMap<>();
    /** Per player: the game time until which no pipe takes them. */
    private static final Map<UUID, Long> RUST_TOT = new ConcurrentHashMap<>();
    /** Players who must let go of sneak before a pipe takes them again; players who were told "no partner" for this sneak. */
    private static final Set<UUID> MOET_LOS = ConcurrentHashMap.newKeySet(), GEMELD = ConcurrentHashMap.newKeySet();

    private Pijpreis() {
    }

    /** Is this player inside a pipe right now? */
    public static boolean onderweg(ServerPlayer p) {
        return REIZEN.containsKey(p.getUUID());
    }

    /** The pipe mouth this player stands on (feet on its top, standing on the ground), or null. */
    @Nullable
    public static BlockPos onder(ServerPlayer p) {
        if (!p.onGround()) {
            return null;
        }
        BlockPos pos = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        BlockState state = p.level().getBlockState(pos);
        return state.getBlock() instanceof PijpBlock && state.getValue(PijpBlock.MOND) && Math.abs(p.getY() - (pos.getY() + 1)) < 0.05 ? pos : null;
    }

    /**
     * The mouth a trip from this mouth ends at: the nearest other mouth of the same colour within {@link #BEREIK} blocks
     * that has room for a player above it. Null: this pipe leads nowhere (yet).
     */
    @Nullable
    public static BlockPos partner(ServerLevel level, BlockPos mond) {
        BlockState eigen = level.getBlockState(mond);
        if (!(eigen.getBlock() instanceof PijpBlock)) {
            return null;
        }
        DyeColor kleur = eigen.getValue(PijpBlock.KLEUR);
        BlockPos beste = null;
        double afstand = Double.MAX_VALUE;
        for (BlockPos pos : Knabbelgeluid.find(level, mond, BEREIK, s -> s.getBlock() instanceof PijpBlock && s.getValue(PijpBlock.MOND)
                && s.getValue(PijpBlock.KLEUR) == kleur)) {
            if (pos.equals(mond)) {
                continue;
            }
            double d = pos.distSqr(mond);
            if (d < afstand && vrij(level, pos)) {
                beste = pos;
                afstand = d;
            }
        }
        return beste;
    }

    /** Two blocks of room above this mouth? */
    private static boolean vrij(ServerLevel level, BlockPos mond) {
        return level.getBlockState(mond.above()).getCollisionShape(level, mond.above()).isEmpty()
                && level.getBlockState(mond.above(2)).getCollisionShape(level, mond.above(2)).isEmpty();
    }

    /**
     * This player wants into the mouth they stand on. True when the trip started. {@code zeg}: tell them why not (a click;
     * while sneaking it is said once per sneak).
     */
    public static boolean probeer(ServerPlayer p, BlockPos mond, boolean zeg) {
        if (onderweg(p) || p.isSpectator() || !p.isAlive() || p.level().getGameTime() < RUST_TOT.getOrDefault(p.getUUID(), 0L)) {
            return false;
        }
        String waarom = null;
        BlockPos naar = null;
        if (p.isPassenger()) {
            waarom = "afstappen";
        } else if (Minigames.playing(p) != null || Cutscenes.bezig(p)) {
            waarom = "bezig";
        } else {
            naar = partner(p.level(), mond);
            if (naar == null) {
                waarom = "geen_partner";
            }
        }
        if (waarom != null) {
            if (zeg || GEMELD.add(p.getUUID())) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.guhriobeloning.pijp." + waarom, BEREIK).withStyle(ChatFormatting.YELLOW));
                p.level().playSound(null, mond, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.BLOCKS, 0.6f, 0.6f);
            }
            return false;
        }
        ServerLevel level = p.level();
        REIZEN.put(p.getUUID(), new Reis(level.dimension(), mond.immutable(), naar.immutable()));
        MOET_LOS.add(p.getUUID());
        p.teleportTo(mond.getX() + 0.5, mond.getY() + 1, mond.getZ() + 0.5);
        p.setDeltaMovement(Vec3.ZERO);
        film(p, GuhrioBeloningPayloads.Film.IN, IN_TICKS);
        level.playSound(null, mond, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 0.6f, 0.8f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mond.getX() + 0.5, mond.getY() + 1.1, mond.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.0);
        return true;
    }

    /** (every tick of a player) sneaking on a mouth starts a trip; a trip under way goes on. */
    public static void tick(ServerPlayer p) {
        UUID id = p.getUUID();
        Reis reis = REIZEN.get(id);
        if (reis != null) {
            stap(p, reis);
            return;
        }
        if (!p.isShiftKeyDown()) {
            MOET_LOS.remove(id);
            GEMELD.remove(id);
            return;
        }
        if (MOET_LOS.contains(id)) {
            return;
        }
        BlockPos mond = onder(p);
        if (mond != null) {
            probeer(p, mond, false);
        }
    }

    private static void stap(ServerPlayer p, Reis reis) {
        ServerLevel level = p.level();
        if (!p.isAlive() || level.dimension() != reis.dim) {
            REIZEN.remove(p.getUUID());
            return;
        }
        reis.tick++;
        p.resetFallDistance();
        p.setDeltaMovement(Vec3.ZERO);
        // the three notes: down on the way in, up on the way out
        if (reis.tick <= IN_TICKS && reis.tick % 3 == 1) {
            level.playSound(null, reis.van, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.8f, 1.2f - 0.2f * (reis.tick / 3));
        } else if (reis.tick > IN_TICKS && (reis.tick - IN_TICKS) % 3 == 1) {
            level.playSound(null, reis.naar, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.8f, 0.6f + 0.2f * ((reis.tick - IN_TICKS) / 3));
        }
        if (reis.tick == IN_TICKS) {
            BlockState doel = level.getBlockState(reis.naar);
            // (the other pipe was broken while you slid down: you simply come up where you went in)
            BlockPos uit = doel.getBlock() instanceof PijpBlock && vrij(level, reis.naar) ? reis.naar : reis.van;
            p.teleportTo(level, uit.getX() + 0.5, uit.getY() + 1, uit.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), false);
            p.resetFallDistance();
            film(p, GuhrioBeloningPayloads.Film.UIT, UIT_TICKS);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, uit.getX() + 0.5, uit.getY() + 1.1, uit.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.0);
            if (uit.equals(reis.naar)) {
                GuhAdvancements.grant(p, "guhrio_beloning_pijp");
                nl.juiced.guhs.feature.gids.GidsFeature.grant(p, "guhrio/guhrio_beloning_pijp");
            }
        } else if (reis.tick >= IN_TICKS + UIT_TICKS) {
            REIZEN.remove(p.getUUID());
            RUST_TOT.put(p.getUUID(), level.getGameTime() + RUST);
        }
    }

    /** Tells the traveller's own game and everybody near them to play a half of the film. */
    private static void film(ServerPlayer p, int fase, int ticks) {
        GuhrioBeloningPayloads.Film film = new GuhrioBeloningPayloads.Film(p.getId(), fase, ticks);
        for (ServerPlayer kijker : p.level().players()) {
            if (kijker == p || kijker.distanceToSqr(p) <= ZICHT * ZICHT) {
                ModNetworking.sendTo(kijker, film);               // (skips players whose game does not know the film)
            }
        }
    }

    /** (logout) */
    static void wis(UUID speler) {
        REIZEN.remove(speler);
        MOET_LOS.remove(speler);
        GEMELD.remove(speler);
        RUST_TOT.remove(speler);
    }

    /** (server stop) */
    static void wisAlles() {
        REIZEN.clear();
        MOET_LOS.clear();
        GEMELD.clear();
        RUST_TOT.clear();
    }
}
