package nl.juiced.guhs.feature.bleekwoud;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;

/**
 * The heart's own creature (see {@link GuhhartjeBlock}), with vanilla's cadence: it only looks around once a second (every
 * 20-24 ticks; the emitter of the hurt trail aside), never scans for blocks while ticking, and only calls a creature when a
 * player is within {@link #SPELER_STRAAL} blocks.
 */
public class GuhhartjeBlockEntity extends BlockEntity {
    /** A player this close wakes the creature up; the creature never goes further from its heart than this. */
    public static final int SPELER_STRAAL = 32, STRAAL = 32;
    /** Further away than this (it was pushed, or fell): it crumbles and the heart calls a new one. */
    public static final double TE_VER = 34.0;
    /** How far from the heart (sideways) the creature appears, and how often the heart tries per second. */
    private static final int VERSCHIJN_STRAAL = 10, POGINGEN = 6;
    /** The hurt trail: 100 ticks of little sounds along the trail, particles for the first half. */
    private static final int AU_TICKS = 100, AU_DEELTJES = 50;
    /** A creature that isn't loaded (yet) is forgotten after this many ticks. */
    private static final int GEDULD = 30;

    @Nullable
    private UUID wezenId;
    @Nullable
    private Mob wezen;
    private long ticksSindsId;
    private int ticker;
    private int emitter;
    @Nullable
    private Vec3 emitterDoel;
    private int signaal;
    /** A soured heart whose Kraak-Mika was hit enough: it stays in its tree until the next night. */
    private boolean klaarVoorVannacht;

    public GuhhartjeBlockEntity(BlockPos pos, BlockState state) {
        super(BleekwoudFeature.GUHHARTJE_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GuhhartjeBlockEntity hart) {
        hart.ticksSindsId++;
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (hart.emitter > 0) {
            if (hart.emitter > AU_DEELTJES) {
                hart.spoor(server, 1, true);
                hart.spoor(server, 1, false);
            }
            if (hart.emitter % 10 == 0 && hart.emitterDoel != null) {
                hart.wezen().ifPresent(w -> hart.emitterDoel = w.getBoundingBox().getCenter());
                Vec3 centre = Vec3.atCenterOf(pos);
                float progress = 0.2f + 0.8f * (AU_TICKS - hart.emitter) / (float) AU_TICKS;
                Vec3 at = centre.subtract(hart.emitterDoel).scale(progress).add(hart.emitterDoel);
                server.playSound(null, BlockPos.containing(at), BleekwoudFeature.HART_AU.get(), SoundSource.BLOCKS, hart.emitter / 2f / AU_TICKS + 0.5f, 1f);
            }
            hart.emitter--;
        }
        if (hart.ticker-- >= 0) {
            return;
        }
        hart.ticker = level.getRandom().nextInt(5) + 20;
        int signaal = hart.berekenSignaal();
        if (hart.signaal != signaal) {
            hart.signaal = signaal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        BlockState updated = hart.nieuweToestand(level, state, pos);
        if (updated != state) {
            level.setBlock(pos, updated, 3);
            if (updated.getValue(GuhhartjeBlock.STATE) == CreakingHeartState.UPROOTED) {
                return;
            }
            if (updated.getValue(GuhhartjeBlock.STATE) == CreakingHeartState.AWAKE) {
                level.playSound(null, pos, BleekwoudFeature.HART_WAKKER.get(), SoundSource.BLOCKS, 1f, 1f);
            }
        }
        boolean wakker = updated.getValue(GuhhartjeBlock.STATE) == CreakingHeartState.AWAKE;
        if (!wakker) {
            hart.klaarVoorVannacht = false;      // (a new night, a new Kraak-Mika)
        }
        if (hart.wezenId == null) {
            if (wakker && !hart.klaarVoorVannacht && level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), SPELER_STRAAL, false) != null) {
                hart.roepWezen(server);
            }
        } else {
            Optional<Mob> w = hart.wezen();
            if (w.isPresent() && (!wakker || hart.afstand() > TE_VER || ((KraakWezen) w.get()).spelerZitVast())) {
                hart.wezenWeg(false);
            }
        }
    }

    private BlockState nieuweToestand(Level level, BlockState state, BlockPos pos) {
        if (!GuhhartjeBlock.hasRequiredLogs(state, level, pos) && wezenId == null) {
            return state.setValue(GuhhartjeBlock.STATE, CreakingHeartState.UPROOTED);
        }
        return state.setValue(GuhhartjeBlock.STATE, BleekwoudBlocks.nacht(level, pos) ? CreakingHeartState.AWAKE : CreakingHeartState.DORMANT);
    }

    /** Is this the soured heart (it calls a Kraak-Mika)? */
    public boolean verzuurd() {
        return getBlockState().getBlock() instanceof GuhhartjeBlock b && b.verzuurd();
    }

    // --- its creature ------------------------------------------------------------------------------------------------------

    /** The heart's creature, when it is loaded. A creature that stays away is forgotten (the heart calls a new one). */
    public Optional<Mob> wezen() {
        if (wezenId == null) {
            return Optional.empty();
        }
        if (wezen != null && !wezen.isRemoved()) {
            return Optional.of(wezen);
        }
        if (wezen != null) {
            wezen = null;
            ticksSindsId = 0;
        }
        if (level instanceof ServerLevel server && server.getEntity(wezenId) instanceof Mob mob && mob instanceof KraakWezen && !mob.isRemoved()) {
            wezen = mob;
            return Optional.of(mob);
        }
        if (ticksSindsId >= GEDULD) {
            wezenId = null;
            setChanged();
        }
        return Optional.empty();
    }

    public boolean heeftWezen() {
        return wezenId != null;
    }

    /** Is this mob the creature of this heart (a creature that is not, crumbles)? */
    public boolean isVan(Mob mob) {
        return wezenId != null && wezenId.equals(mob.getUUID());
    }

    private double afstand() {
        return wezen().map(w -> Math.sqrt(w.distanceToSqr(Vec3.atBottomCenterOf(getBlockPos())))).orElse(0.0);
    }

    /** Calls the creature: on the ground near the tree (never on a trunk or in the leaves). Null: no room this time. */
    @Nullable
    public Mob roepWezen(ServerLevel server) {
        EntityType<? extends Mob> type = verzuurd() ? BleekwoudFeature.KRAAK_MIKA.get() : BleekwoudFeature.KRAAKGUH.get();
        RandomSource random = server.getRandom();
        BlockPos pos = getBlockPos();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < POGINGEN; i++) {
            int x = pos.getX() + random.nextIntBetweenInclusive(-VERSCHIJN_STRAAL, VERSCHIJN_STRAAL);
            int z = pos.getZ() + random.nextIntBetweenInclusive(-VERSCHIJN_STRAAL, VERSCHIJN_STRAAL);
            if (!server.isLoaded(p.set(x, pos.getY(), z))) {
                continue;
            }
            int top = Math.min(server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), pos.getY() + 6);
            for (int y = top; y >= Math.max(server.getMinY() + 1, pos.getY() - 24); y--) {
                BlockState floor = server.getBlockState(p.set(x, y - 1, z));
                if (floor.isAir()) {
                    continue;
                }
                boolean ok = floor.isFaceSturdy(server, p, Direction.UP) && !floor.is(BleekwoudFeature.STAMMEN)
                        && !floor.is(net.minecraft.tags.BlockTags.LEAVES) && server.getFluidState(p.set(x, y, z)).isEmpty()
                        && server.noCollision(type.getSpawnAABB(x + 0.5, y, z + 0.5));
                if (ok) {
                    Mob mob = type.create(server, EntitySpawnReason.SPAWNER);
                    if (mob == null) {
                        return null;
                    }
                    mob.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360f, 0f);
                    ((KraakWezen) mob).bind(pos);
                    server.addFreshEntity(mob);
                    server.gameEvent(mob, GameEvent.ENTITY_PLACE, mob.position());
                    wezen = mob;
                    wezenId = mob.getUUID();
                    ticksSindsId = 0;
                    setChanged();
                    mob.playSound(BleekwoudFeature.KRAAK.get(), 1f, 1f);
                    server.playSound(null, pos, BleekwoudFeature.HART_WAKKER.get(), SoundSource.BLOCKS, 1f, 0.8f);
                    return mob;
                }
                break;       // (the first floor from above decides for this column)
            }
        }
        return null;
    }

    /** The creature crumbles away (the heart went to sleep, was broken, or its creature strayed). */
    public void wezenWeg(boolean hartKapot) {
        Optional<Mob> w = wezen();
        w.ifPresent(mob -> ((KraakWezen) mob).verkruimel());
        if (wezenId != null) {
            wezenId = null;
            wezen = null;
            setChanged();
        }
    }

    /** The soured heart's Kraak-Mika had enough for tonight: it crumbles back into its tree, a new one comes next night. */
    public void klaarVoorVannacht() {
        klaarVoorVannacht = true;
        wezenWeg(false);
        setChanged();
    }

    public boolean isKlaarVoorVannacht() {
        return klaarVoorVannacht;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        wezenWeg(true);
    }

    // --- the creature was hit: kaashars drips on the trunk -------------------------------------------------------------------

    /** The creature was hit: a trail of sparks from the creature to the heart, and 2-3 clumps of kaashars on the logs. */
    public void wezenGeraakt() {
        Mob mob = wezen().orElse(null);
        if (mob == null || !(level instanceof ServerLevel server) || emitter > 0) {
            return;
        }
        spoor(server, 20, false);
        if (getBlockState().getValue(GuhhartjeBlock.STATE) == CreakingHeartState.AWAKE) {
            int clumps = server.getRandom().nextIntBetweenInclusive(2, 3);
            for (int i = 0; i < clumps; i++) {
                harsErbij(server).ifPresent(at -> {
                    server.playSound(null, at, SoundEvents.RESIN_PLACE, SoundSource.BLOCKS, 1f, 1f);
                    server.gameEvent(GameEvent.BLOCK_PLACE, at, GameEvent.Context.of(getBlockState()));
                });
            }
        }
        emitter = AU_TICKS;
        emitterDoel = mob.getBoundingBox().getCenter();
    }

    /** One more face of kaashars on a bleekhout log within two logs of the heart (vanilla's resin spread). */
    private Optional<BlockPos> harsErbij(ServerLevel server) {
        RandomSource random = server.getRandom();
        MutableObject<BlockPos> placed = new MutableObject<>(null);
        BlockState hars = BleekwoudFeature.KAASHARS.get().defaultBlockState();
        BlockPos.breadthFirstTraversal(worldPosition, 2, 64, (pos, acceptor) -> {
            for (Direction dir : Util.shuffledCopy(Direction.values(), random)) {
                BlockPos next = pos.relative(dir);
                if (server.getBlockState(next).is(BleekwoudFeature.STAMMEN)) {
                    acceptor.accept(next);
                }
            }
        }, pos -> {
            if (!server.getBlockState(pos).is(BleekwoudFeature.STAMMEN)) {
                return BlockPos.TraversalNodeStatus.ACCEPT;
            }
            for (Direction dir : Util.shuffledCopy(Direction.values(), random)) {
                BlockPos next = pos.relative(dir);
                BlockState state = server.getBlockState(next);
                Direction back = dir.getOpposite();
                if (state.isAir()) {
                    state = hars;
                } else if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                    state = hars.setValue(MultifaceBlock.WATERLOGGED, true);
                }
                if (state.is(hars.getBlock()) && !MultifaceBlock.hasFace(state, back)) {
                    server.setBlock(next, state.setValue(MultifaceBlock.getFaceProperty(back), true), 3);
                    placed.setValue(next);
                    return BlockPos.TraversalNodeStatus.STOP;
                }
            }
            return BlockPos.TraversalNodeStatus.ACCEPT;
        });
        return Optional.ofNullable(placed.get());
    }

    /** Sparks between the creature and the heart (orange towards the creature, grey towards the heart). */
    private void spoor(ServerLevel server, int count, boolean naarWezen) {
        Mob mob = wezen().orElse(null);
        if (mob == null) {
            return;
        }
        RandomSource random = server.getRandom();
        boolean zuur = verzuurd();
        int colour = naarWezen ? (zuur ? 0xB4D23C : BleekwoudBlocks.ORANJE) : BleekwoudBlocks.GRIJS;
        for (int i = 0; i < count; i++) {
            AABB box = mob.getBoundingBox();
            Vec3 from = box.getMinPosition().add(random.nextDouble() * box.getXsize(), random.nextDouble() * box.getYsize(), random.nextDouble() * box.getZsize());
            Vec3 to = Vec3.atLowerCornerOf(getBlockPos()).add(random.nextDouble(), random.nextDouble(), random.nextDouble());
            if (naarWezen) {
                Vec3 t = from;
                from = to;
                to = t;
            }
            server.sendParticles(new TrailParticleOption(to, colour, random.nextInt(40) + 10), true, true, from.x, from.y, from.z, 1, 0, 0, 0, 0);
        }
    }

    // --- comparator ----------------------------------------------------------------------------------------------------------

    public int signaal() {
        return signaal;
    }

    private int berekenSignaal() {
        if (wezenId == null || wezen().isEmpty()) {
            return 0;
        }
        double scaled = Math.clamp(afstand(), 0.0, STRAAL) / STRAAL;
        return 15 - (int) Math.floor(scaled * 15.0);
    }

    // --- saving --------------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        wezenId = input.read("wezen", UUIDUtil.CODEC).orElse(null);
        wezen = null;
        ticksSindsId = 0;
        klaarVoorVannacht = input.getBooleanOr("klaar_voor_vannacht", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (wezenId != null) {
            output.store("wezen", UUIDUtil.CODEC, wezenId);
        }
        output.putBoolean("klaar_voor_vannacht", klaarVoorVannacht);
    }

    /** For the tests and the dev check: the creature's id. */
    @Nullable
    public UUID wezenId() {
        return wezenId;
    }
}
