package nl.juiced.guhs.feature.wereldleven;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Brings IJscoguh Tingeling around, like the wandering trader: every few minutes, per player in the Guhmensie, a chance
 * that he rides up (when there isn't one close already). In a Knuffeldal town he comes to the plein. Guhs run after his
 * cart (IJscoguhEntity.Volgen). Ops: /guhs ijscoguh (he comes to you now).
 */
public final class IJscoguhSpawner {
    /**
     * How often (ticks) a player gets a chance (every 10-15 minutes), and how big it is; after he came to a player, that
     * player gets no new chance until he rode away again plus one more wait (so about one visit an hour of playing).
     */
    public static final int POGING = 20 * 60 * 10;
    public static final float KANS = 0.25f;
    public static final int NA_BEZOEK = IJscoguhEntity.BLIJFT + POGING;
    /** Not when there is one within this many blocks already. */
    public static final double AL_EEN = 160;
    private static final Map<UUID, Long> VOLGENDE = new ConcurrentHashMap<>();

    static void register() {
        GuhHooks.doelen((guh, goals) -> goals.addGoal(5, new IJscoguhEntity.Volgen(guh)));
        NeoForge.EVENT_BUS.addListener(IJscoguhSpawner::onTick);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            VOLGENDE.clear();
            IJscoguhEntity.LEVEND.clear();
        });
        NeoForge.EVENT_BUS.addListener(IJscoguhSpawner::commands);
    }

    private static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 200 != 17) {
            return;
        }
        ServerLevel level = server.getLevel(ModDimensions.GUHMENSION);
        if (level == null || !level.getGameRules().get(GameRules.SPAWN_WANDERING_TRADERS)) {
            return;
        }
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            long next = VOLGENDE.computeIfAbsent(player.getUUID(), id -> now + POGING / 2);
            if (now < next) {
                continue;
            }
            VOLGENDE.put(player.getUUID(), now + POGING + level.getRandom().nextInt(POGING / 2));
            if (nearest(level, player.blockPosition(), AL_EEN) != null) {
                continue;    // (he's around already: the next chance comes later)
            }
            if (level.getRandom().nextFloat() < KANS && spawnBij(level, player, level.getRandom()) != null) {
                VOLGENDE.put(player.getUUID(), now + NA_BEZOEK + level.getRandom().nextInt(POGING / 2));
            }
        }
    }

    @Nullable
    static IJscoguhEntity nearest(ServerLevel level, BlockPos pos, double range) {
        for (IJscoguhEntity e : IJscoguhEntity.LEVEND) {
            if (e.isAlive() && e.level() == level && e.blockPosition().distSqr(pos) < range * range) {
                return e;
            }
        }
        return null;
    }

    /**
     * IJscoguh Tingeling comes to this player: in a Knuffeldal town to its plein, otherwise 20-36 blocks away on the
     * surface. Returns him, or null when there was no good spot.
     */
    @Nullable
    public static IJscoguhEntity spawnBij(ServerLevel level, ServerPlayer player, RandomSource random) {
        BlockPos doel = player.blockPosition();
        BlockPos spot = null;
        StructureStart stadje = PleinSlot.stadje(level, player.blockPosition());
        if (stadje != null && !stadje.getPieces().isEmpty()) {
            doel = stadje.getPieces().get(0).getBoundingBox().getCenter();
            spot = surface(level, doel.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4));
        }
        for (int tries = 0; spot == null && tries < 12; tries++) {
            double a = random.nextDouble() * Math.PI * 2;
            int r = 20 + random.nextInt(17);
            spot = surface(level, player.blockPosition().offset((int) (Math.cos(a) * r), 0, (int) (Math.sin(a) * r)));
        }
        if (spot == null) {
            return null;
        }
        IJscoguhEntity ijsco = WereldlevenFeature.IJSCOGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (ijsco == null) {
            return null;
        }
        ijsco.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        ijsco.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), EntitySpawnReason.EVENT, null);
        ijsco.setDoel(surfaceOr(level, doel));
        level.addFreshEntity(ijsco);
        ijsco.bel();
        player.sendOverlayMessage(Component.translatable("gui.guhs.wereldleven.ijscoguh_komt").withStyle(ChatFormatting.LIGHT_PURPLE));
        return ijsco;
    }

    private static BlockPos surfaceOr(ServerLevel level, BlockPos pos) {
        BlockPos s = surface(level, pos);
        return s == null ? pos : s;
    }

    /** The top of the ground at x/z (standing room, no liquid, not on leaves), or null. */
    @Nullable
    static BlockPos surface(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return null;
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
        if (!level.getFluidState(top).isEmpty() || !level.getFluidState(top.below()).isEmpty()
                || level.getBlockState(top.below()).getCollisionShape(level, top.below()).isEmpty()
                || !level.getBlockState(top.above()).getCollisionShape(level, top.above()).isEmpty()
                || !level.getBlockState(top.above(2)).getCollisionShape(level, top.above(2)).isEmpty()) {
            return null;
        }
        return top;
    }

    private static void commands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("ijscoguh").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
            ServerPlayer player = c.getSource().getPlayerOrException();
            IJscoguhEntity ijsco = spawnBij(player.level(), player, player.getRandom());
            if (ijsco == null) {
                c.getSource().sendFailure(Component.translatable("gui.guhs.wereldleven.ijscoguh_geen_plek"));
                return 0;
            }
            c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.wereldleven.ijscoguh_komt").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return 1;
        });
        event.getDispatcher().register(Commands.literal("guhs").then(cmd));
    }

    private IJscoguhSpawner() {
    }
}
