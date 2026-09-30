package nl.juiced.guhs.feature.gatenkaas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.quest.GuhQuests;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * The noises of the Stille Voorraadkelder. A player who walks (not sneaking), chews or digs makes a noise:
 * <ul>
 *   <li>every Vadswaker within earshot hears it ({@link VadswakerEntity#hear}) - chewing loudest of all;</li>
 *   <li>every knabbelsensor within {@link KnabbelsensorBlock#RANGE} blocks lights up, and a knabbelschreeuwer near
 *       such a sensor screams: one warning for that player (at most one per {@link #WARN_COOLDOWN} ticks);</li>
 *   <li>the {@link #SUMMON_AT}rd warning at a Mika-built schreeuwer wakes the Vadswaker. Warnings fade, one per
 *       {@link #FADE} ticks of quiet.</li>
 * </ul>
 * Creative and spectator players make no noise, and neither does anyone with guhs:stil.
 */
public final class Knabbelgeluid {
    public static final String WARNINGS = "guhs_knabbel_waarschuwingen", LAST_WARNING = "guhs_knabbel_laatste", FADE_FROM = "guhs_knabbel_vervaag";
    public static final int WARN_COOLDOWN = 200, FADE = 20 * 60 * 10, SUMMON_AT = 3;
    /** A Vadswaker within this many blocks of a screaming schreeuwer comes over instead of a second one waking up. */
    public static final int ONE_VADSWAKER_RANGE = 48;

    /** A kind of noise: how angry it makes a Vadswaker and how far he hears it. */
    public enum Noise {
        STAP(35, 16), KAUWEN(60, 24), HAKKEN(40, 16), BOUWEN(25, 12);

        public final int anger, range;

        Noise(int anger, int range) {
            this.anger = anger;
            this.range = range;
        }
    }

    private static final Map<UUID, Vec3> LAST_STEP = new HashMap<>();

    public static void register(IEventBus bus) {
        bus.addListener(Knabbelgeluid::onPlayerTick);
        bus.addListener(Knabbelgeluid::onChew);
        bus.addListener(Knabbelgeluid::onBreak);
        bus.addListener(Knabbelgeluid::onPlace);
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> LAST_STEP.remove(event.getEntity().getUUID()));
    }

    /** Can this player be heard at all? */
    public static boolean audible(ServerPlayer player) {
        return player.isAlive() && !ghostly(player) && !StilEffect.isStil(player);
    }

    /** Creative or spectator: nothing hears you and nothing chases you. (By game mode: test players say "creative".) */
    public static boolean ghostly(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer sp) {
            var mode = sp.gameMode.getGameModeForPlayer();
            return mode == net.minecraft.world.level.GameType.CREATIVE || mode == net.minecraft.world.level.GameType.SPECTATOR;
        }
        return player.isCreative() || player.isSpectator();
    }

    // --- the noises ----------------------------------------------------------------------------------------------------

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 8 != 0) {
            return;
        }
        Vec3 now = player.position();
        Vec3 before = LAST_STEP.put(player.getUUID(), now);
        if (before == null || !player.onGround() || player.isSteppingCarefully() || player.isPassenger()) {
            return;
        }
        double dx = now.x - before.x, dz = now.z - before.z;
        if (dx * dx + dz * dz > 0.3 * 0.3 && dx * dx + dz * dz < 64) {
            noise(player, player.blockPosition(), Noise.STAP);
        }
    }

    /** Chewing: the whole time you eat, every few ticks. (Drinking is quiet.) */
    private static void onChew(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().has(DataComponents.FOOD) && event.getDuration() % 6 == 0) {
            noise(player, player.blockPosition(), Noise.KAUWEN);
        }
    }

    private static void onBreak(BreakBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            noise(player, event.getPos(), Noise.HAKKEN);
        }
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            noise(player, event.getPos(), Noise.BOUWEN);
        }
    }

    /** A player made a noise here: Vadswakers hear it, sensors light up, a schreeuwer may scream. */
    public static void noise(ServerPlayer player, BlockPos at, Noise noise) {
        if (!audible(player)) {
            return;
        }
        ServerLevel level = player.level();
        for (VadswakerEntity vadswaker : level.getEntitiesOfClass(VadswakerEntity.class, new AABB(at).inflate(noise.range))) {
            vadswaker.hear(player, at, noise.anger);
        }
        List<BlockPos> heard = new ArrayList<>();
        for (BlockPos sensor : find(level, at, KnabbelsensorBlock.RANGE, s -> s.getBlock() instanceof KnabbelsensorBlock)) {
            if (KnabbelsensorBlock.activate(level, sensor)) {
                heard.add(sensor);
            }
        }
        if (heard.isEmpty()) {
            return;
        }
        BlockPos shrieker = nearestShrieker(level, heard);
        if (shrieker != null) {
            warn(player, shrieker);
        }
    }

    @Nullable
    private static BlockPos nearestShrieker(ServerLevel level, List<BlockPos> sensors) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos sensor : sensors) {
            for (BlockPos s : find(level, sensor, KnabbelschreeuwerBlock.RANGE, st -> st.getBlock() instanceof KnabbelschreeuwerBlock)) {
                double d = s.distSqr(sensor);
                if (d < bestDist) {
                    best = s;
                    bestDist = d;
                }
            }
        }
        return best;
    }

    // --- warnings ------------------------------------------------------------------------------------------------------

    /** How many warnings this player has now (after fading). */
    public static int warnings(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        long now = player.level().getGameTime();
        int w = saved.getIntOr(WARNINGS, 0);
        long from = saved.getLongOr(FADE_FROM, 0L);
        if (w > 0 && now - from >= FADE) {
            int steps = (int) Math.min(w, (now - from) / FADE);
            w -= steps;
            saved.putInt(WARNINGS, w);
            saved.putLong(FADE_FROM, from + (long) steps * FADE);
        }
        return w;
    }

    /** A schreeuwer heard this player: scream, count a warning, and at the third one wake the Vadswaker. */
    public static void warn(ServerPlayer player, BlockPos shrieker) {
        ServerLevel level = player.level();
        CompoundTag saved = GuhQuests.saved(player);
        long now = level.getGameTime();
        int w = warnings(player);
        if (saved.contains(LAST_WARNING) && now - saved.getLongOr(LAST_WARNING, 0L) < WARN_COOLDOWN && now >= saved.getLongOr(LAST_WARNING, 0L)) {
            return;
        }
        BlockState state = level.getBlockState(shrieker);
        if (!KnabbelschreeuwerBlock.shriek(level, shrieker)) {
            return;
        }
        if (!state.getValue(KnabbelschreeuwerBlock.CAN_SUMMON)) {
            return;                                      // (a home-made schreeuwer only screams)
        }
        w = Math.min(SUMMON_AT, w + 1);
        saved.putInt(WARNINGS, w);
        saved.putLong(LAST_WARNING, now);
        saved.putLong(FADE_FROM, now);
        GatenkaasEvents.grant(player, "gatenkaas_geschreeuw");
        player.sendOverlayMessage(Component.translatable("quest.guhs.gatenkaas.warning." + w).withStyle(ChatFormatting.GOLD));
        // it gets dark around the schreeuwer: the lights of the larder flicker
        for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class, new AABB(shrieker).inflate(40), Knabbelgeluid::audible)) {
            near.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 12, 0, false, false), null);
        }
        if (w >= SUMMON_AT) {
            summon(level, shrieker, player);
        }
    }

    /** Wakes the Vadswaker next to this schreeuwer (or sends the one that's already awake nearby). */
    @Nullable
    public static VadswakerEntity summon(ServerLevel level, BlockPos shrieker, ServerPlayer player) {
        List<VadswakerEntity> awake = level.getEntitiesOfClass(VadswakerEntity.class, new AABB(shrieker).inflate(ONE_VADSWAKER_RANGE));
        if (!awake.isEmpty()) {
            awake.get(0).hear(player, player.blockPosition(), Noise.KAUWEN.anger);
            return null;
        }
        BlockPos spot = spawnSpot(level, shrieker);
        if (spot == null) {
            return null;
        }
        VadswakerEntity vadswaker = GatenkaasFeature.VADSWAKER.get().spawn(level, spot, EntitySpawnReason.TRIGGERED);
        if (vadswaker != null) {
            vadswaker.hear(player, player.blockPosition(), 40);
            player.sendSystemMessage(Component.translatable("quest.guhs.gatenkaas.vadswaker_wakker").withStyle(ChatFormatting.RED));
            GatenkaasEvents.grant(player, "gatenkaas_vadswaker_gewekt");
            GatenkaasEvents.award(player, "guhmension/vadswaker_gewekt");
        }
        return vadswaker;
    }

    /** A place near the schreeuwer with solid ground and room for him. */
    @Nullable
    public static BlockPos spawnSpot(ServerLevel level, BlockPos near) {
        var type = GatenkaasFeature.VADSWAKER.get();
        for (int attempt = 0; attempt < 40; attempt++) {
            int r = attempt < 10 ? 3 : 6;
            BlockPos column = near.offset(level.getRandom().nextInt(2 * r + 1) - r, 0, level.getRandom().nextInt(2 * r + 1) - r);
            for (int dy = 3; dy >= -5; dy--) {
                BlockPos p = column.above(dy);
                if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
                        && level.noCollision(type.getSpawnAABB(p.getX() + 0.5, p.getY(), p.getZ() + 0.5))) {
                    return p;
                }
            }
        }
        return null;
    }

    // --- finding blocks fast: sections without them are skipped by their palette --------------------------------------

    /** All blocks matching the test within this radius (a sphere) of the centre, in loaded chunks. */
    public static List<BlockPos> find(ServerLevel level, BlockPos centre, int radius, Predicate<BlockState> test) {
        List<BlockPos> out = new ArrayList<>();
        int r2 = radius * radius;
        int minY = Math.max(level.getMinY(), centre.getY() - radius), maxY = Math.min(level.getMaxY() + 1 - 1, centre.getY() + radius);
        for (int sx = SectionPos.blockToSectionCoord(centre.getX() - radius); sx <= SectionPos.blockToSectionCoord(centre.getX() + radius); sx++) {
            for (int sz = SectionPos.blockToSectionCoord(centre.getZ() - radius); sz <= SectionPos.blockToSectionCoord(centre.getZ() + radius); sz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(sx, sz);
                if (chunk == null) {
                    continue;
                }
                for (int sy = SectionPos.blockToSectionCoord(minY); sy <= SectionPos.blockToSectionCoord(maxY); sy++) {
                    int index = chunk.getSectionIndexFromSectionY(sy);
                    if (index < 0 || index >= chunk.getSections().length) {
                        continue;
                    }
                    LevelChunkSection section = chunk.getSection(index);
                    if (section.hasOnlyAir() || !section.maybeHas(test)) {
                        continue;
                    }
                    for (int x = Math.max(sx << 4, centre.getX() - radius); x <= Math.min((sx << 4) + 15, centre.getX() + radius); x++) {
                        for (int z = Math.max(sz << 4, centre.getZ() - radius); z <= Math.min((sz << 4) + 15, centre.getZ() + radius); z++) {
                            for (int y = Math.max(sy << 4, minY); y <= Math.min((sy << 4) + 15, maxY); y++) {
                                int dx = x - centre.getX(), dy = y - centre.getY(), dz = z - centre.getZ();
                                if (dx * dx + dy * dy + dz * dz <= r2 && test.test(section.getBlockState(x & 15, y & 15, z & 15))) {
                                    out.add(new BlockPos(x, y, z));
                                }
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    private Knabbelgeluid() {
    }
}
