package nl.juiced.guhs.world;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.MaagPortalBlock;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * The guh stomachs (guhmaag dimension): one shared dimension. The mouth (public lobby) sits at 0,0; every player's
 * stomach is a plot of its own, {@link #PLOT_SPACING} blocks apart, walled in by stomach walls and invisible barriers.
 * <ul>
 *     <li>A stomach starts at 48x48 and grows (64, 80, 96) with the Tandarts-guh's jobs in the mouth.</li>
 *     <li>In the middle of every stomach: a portal "to the mouth" (the lobby) and one "to the intestines" (back to where
 *     you came from in the world), the signs, and the Maagenzym-guh (the settings).</li>
 *     <li>The mouth has a portal for every stomach there is; you get in if it's public (or you're on its whitelist).</li>
 * </ul>
 */
public final class MaagManager {
    public static final ResourceKey<Level> GUHMAAG = ResourceKey.create(Registries.DIMENSION, Guhs.id("guhmaag"));
    public static final int PLOT_SPACING = 1024;
    public static final int FLOOR_Y = 64;
    public static final int[] SIZES = {48, 64, 80, 96, 112, 128};
    public static final int[] HEIGHTS = {24, 28, 32, 36, 40, 44};
    /** The mouth: interior from -32 to 31 (x and z), 22 high. */
    public static final int LOBBY_HALF = 32, LOBBY_HEIGHT = 22;
    /** Stomach portals in the mouth: 9 per row, 6 apart; rows 12 apart. */
    public static final int SLOT_COLS = 9, SLOT_ROWS = 4, SLOT_X0 = -24, SLOT_DX = 6, SLOT_Z0 = -18, SLOT_DZ = 12;

    public static boolean isGuhmaag(Level level) {
        return level.dimension() == GUHMAAG;
    }

    @Nullable
    public static ServerLevel level(net.minecraft.server.MinecraftServer server) {
        return server.getLevel(GUHMAAG);
    }

    public static BlockPos center(int index) {
        return new BlockPos((index + 1) * PLOT_SPACING, FLOOR_Y, 0);
    }

    public static int sizeLevel(int size) {
        for (int i = 0; i < SIZES.length; i++) {
            if (SIZES[i] == size) {
                return i;
            }
        }
        return 0;
    }

    /** Which stomach plot a position is in (or -1 for the mouth / nowhere). */
    public static int plotAt(BlockPos pos) {
        int index = Math.floorDiv(pos.getX() + PLOT_SPACING / 2, PLOT_SPACING) - 1;
        return index >= 0 && Math.abs(pos.getZ()) < PLOT_SPACING / 2 ? index : -1;
    }

    public static boolean inLobby(BlockPos pos) {
        return Math.abs(pos.getX()) <= LOBBY_HALF + 3 && Math.abs(pos.getZ()) <= LOBBY_HALF + 3;
    }

    public static Vec3 entryPoint(int index) {
        BlockPos c = center(index);
        return new Vec3(c.getX() + 0.5, FLOOR_Y, c.getZ() + 4.5);
    }

    public static Vec3 lobbySpawn() {
        return new Vec3(0.5, FLOOR_Y, LOBBY_HALF - 5.5);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Travelling
    // ------------------------------------------------------------------------------------------------------------

    /** Whistle / key: from anywhere to your own stomach; from inside the stomachs, back out to the world. */
    public static void whistle(ServerPlayer player) {
        GuhWorldData data = GuhWorldData.get(player.level().getServer());
        if (!data.player(player.getUUID()).maagUnlocked()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.maag.locked").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (isGuhmaag(player.level())) {
            goBack(player);
        } else {
            goToOwnMaag(player);
        }
    }

    public static void goToOwnMaag(ServerPlayer player) {
        ServerLevel maagLevel = level(player.level().getServer());
        if (maagLevel == null) {
            return;
        }
        GuhWorldData data = GuhWorldData.get(player.level().getServer());
        GuhWorldData.Maag maag = ensureMaag(maagLevel, player);
        rememberReturn(player, data);
        Vec3 to = entryPoint(maag.index);
        player.teleportTo(maagLevel, to.x, to.y, to.z, java.util.Set.of(), 180f, 0f, true);
        maagLevel.playSound(null, to.x, to.y, to.z, SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1f, 0.8f);
    }

    public static GuhWorldData.Maag ensureMaag(ServerLevel maagLevel, Player owner) {
        GuhWorldData data = GuhWorldData.get(maagLevel.getServer());
        GuhWorldData.Maag maag = data.createMaag(owner.getUUID(), owner.getGameProfile().name());
        if (!data.isLobbyBuilt()) {
            buildLobby(maagLevel);
            data.setLobbyBuilt();
        }
        if (!maag.built) {
            buildMaag(maagLevel, maag);
            buildLobbySlot(maagLevel, maag);
            maag.built = true;
            data.setDirty();
        }
        return maag;
    }

    /** Before entering the stomachs from the world, remember where to come back to. */
    private static void rememberReturn(ServerPlayer player, GuhWorldData data) {
        if (!isGuhmaag(player.level())) {
            GuhWorldData.PlayerData p = data.player(player.getUUID());
            p.returnDimension = player.level().dimension();
            p.returnPos = player.position();
            p.returnYaw = player.getYRot();
            data.setDirty();
        }
    }

    /** Back to where you came from in the world ("to the intestines"). */
    public static void goBack(ServerPlayer player) {
        TeleportTransition back = backTransition(player);
        if (back != null) {
            player.teleport(back);
        }
    }

    @Nullable
    private static TeleportTransition backTransition(Entity entity) {
        net.minecraft.server.MinecraftServer server = entity.level().getServer();
        if (server == null) {
            return null;
        }
        GuhWorldData.PlayerData p = GuhWorldData.get(server).player(entity.getUUID());
        ServerLevel target = p.returnDimension == null ? null : server.getLevel(p.returnDimension);
        Vec3 pos = p.returnPos;
        if (target == null) {
            target = server.overworld();
            BlockPos spawn = target.getRespawnData().pos();
            pos = new Vec3(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        }
        return new TeleportTransition(target, pos, Vec3.ZERO, p.returnYaw, 0f,
                e -> e.level().playSound(null, e.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.5f, 0.6f));
    }

    /** Where a stomach portal takes you. */
    @Nullable
    public static TeleportTransition portalDestination(ServerLevel level, Entity entity, BlockPos pos, MaagPortalBlock.Kind kind) {
        ServerLevel maagLevel = level(level.getServer());
        if (maagLevel == null) {
            return null;
        }
        switch (kind) {
            case MOND -> {
                Vec3 to = lobbySpawn();
                return new TeleportTransition(maagLevel, to, Vec3.ZERO, 180f, 0f, TeleportTransition.DO_NOTHING);
            }
            case DARM, EXIT -> {
                return backTransition(entity);
            }
            case MAAG -> {
                int col = Math.floorDiv(pos.getX() - SLOT_X0, SLOT_DX);
                int row = Math.floorDiv(pos.getZ() - SLOT_Z0 + SLOT_DZ / 2, SLOT_DZ);
                GuhWorldData.Maag maag = GuhWorldData.get(level.getServer()).maagByIndex(row * SLOT_COLS + col);
                if (maag == null) {
                    return null;
                }
                if (!maag.mayVisit(entity.getUUID())) {
                    if (entity instanceof Player player && level.getGameTime() % 40 == 0) {
                        player.sendOverlayMessage(Component.translatable("gui.guhs.maag.private", maag.ownerName)
                                .withStyle(ChatFormatting.RED));
                    }
                    return null;
                }
                Vec3 to = entryPoint(maag.index);
                return new TeleportTransition(maagLevel, to, Vec3.ZERO, 180f, 0f, TeleportTransition.DO_NOTHING);
            }
            default -> {
                return null;
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Building a stomach
    // ------------------------------------------------------------------------------------------------------------

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 2 | 16);
    }

    /** The walls, floor and ceiling of a stomach of a given size (removing the old, smaller shell if it grew). */
    public static void buildShell(ServerLevel level, BlockPos c, int size, int height, int oldSize, int oldHeight) {
        BlockState wall = ModBlocks.MAAGWAND.get().defaultBlockState();
        BlockState floor = ModBlocks.MAAGBODEM.get().defaultBlockState();
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        if (oldSize > 0) {
            // take down the old walls, ceiling and barrier (the floor and everything inside stays)
            int h0 = oldSize / 2;
            for (int x = -h0 - 2; x <= h0 + 1; x++) {
                for (int z = -h0 - 2; z <= h0 + 1; z++) {
                    boolean edge = x < -h0 || x >= h0 || z < -h0 || z >= h0;
                    for (int y = FLOOR_Y; y <= FLOOR_Y + oldHeight + 1; y++) {
                        if (edge || y >= FLOOR_Y + oldHeight) {
                            set(level, c.getX() + x, y, c.getZ() + z, air);
                        }
                    }
                    if (edge) {
                        set(level, c.getX() + x, FLOOR_Y - 3, c.getZ() + z, air);
                    }
                }
            }
        }
        int h = size / 2;
        for (int x = -h - 2; x <= h + 1; x++) {
            for (int z = -h - 2; z <= h + 1; z++) {
                boolean outer = x == -h - 2 || x == h + 1 || z == -h - 2 || z == h + 1;
                boolean edge = x < -h || x >= h || z < -h || z >= h;
                int wx = c.getX() + x, wz = c.getZ() + z;
                if (outer) {
                    for (int y = FLOOR_Y - 3; y <= FLOOR_Y + height + 1; y++) {
                        set(level, wx, y, wz, barrier);
                    }
                    continue;
                }
                set(level, wx, FLOOR_Y - 3, wz, barrier);
                set(level, wx, FLOOR_Y + height + 1, wz, barrier);
                if (edge) {
                    for (int y = FLOOR_Y - 2; y <= FLOOR_Y + height; y++) {
                        set(level, wx, y, wz, wall);
                    }
                } else {
                    set(level, wx, FLOOR_Y + height, wz, wall);
                    if (level.getBlockState(new BlockPos(wx, FLOOR_Y - 1, wz)).isAir()) {
                        set(level, wx, FLOOR_Y - 2, wz, floor);
                        set(level, wx, FLOOR_Y - 1, wz, floor);
                    }
                }
            }
        }
    }

    public static void buildMaag(ServerLevel level, GuhWorldData.Maag maag) {
        BlockPos c = center(maag.index);
        int lvl = sizeLevel(maag.size);
        buildShell(level, c, maag.size, HEIGHTS[lvl], 0, 0);
        RandomSource random = RandomSource.create(maag.owner.getMostSignificantBits());
        // the portals in the middle: "to the mouth" (left) and "to the intestines" (right)
        buildPortal(level, c.offset(-3, 0, 0), MaagPortalBlock.Kind.MOND, Component.translatable("sign.guhs.maag.to_mouth"));
        buildPortal(level, c.offset(2, 0, 0), MaagPortalBlock.Kind.DARM, Component.translatable("sign.guhs.maag.to_intestines"));
        // half-digested kaasknabbels and little pools of stomach acid
        digestedKnabbel(level, c.offset(-14, 0, -12), random, Direction.Axis.X);
        digestedKnabbel(level, c.offset(12, 0, 11), random, Direction.Axis.Z);
        acidPool(level, c.offset(13, 0, -13), 2.6);
        acidPool(level, c.offset(-12, 0, 13), 2.2);
        // the Maagenzym-guh: the stomach's settings
        GuhNpcEntity enzyme = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (enzyme != null) {
            enzyme.setKind(GuhNpcEntity.Kind.MAAGENZYM);
            enzyme.setMaagOwner(maag.owner);
            enzyme.snapTo(c.getX() + 5.5, FLOOR_Y, c.getZ() + 3.5, 180f, 0f);
            level.addFreshEntity(enzyme);
        }
    }

    /** Makes the stomach bigger (next size), keeping everything inside. */
    public static boolean grow(ServerLevel level, GuhWorldData.Maag maag) {
        int lvl = sizeLevel(maag.size);
        if (lvl >= SIZES.length - 1) {
            return false;
        }
        buildShell(level, center(maag.index), SIZES[lvl + 1], HEIGHTS[lvl + 1], SIZES[lvl], HEIGHTS[lvl]);
        maag.size = SIZES[lvl + 1];
        GuhWorldData.get(level.getServer()).setDirty();
        return true;
    }

    /** A 2x3 portal in a frame of teeth, facing south, with a sign in front. {@code corner} = bottom-left portal block. */
    private static void buildPortal(ServerLevel level, BlockPos corner, MaagPortalBlock.Kind kind, Component label) {
        BlockState tooth = ModBlocks.TAND.get().defaultBlockState();
        BlockState portal = ModBlocks.MAAG_PORTAL.get().defaultBlockState().setValue(MaagPortalBlock.KIND, kind);
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                boolean frame = dx == -1 || dx == 2 || dy == -1 || dy == 3;
                level.setBlock(corner.offset(dx, dy, 0), frame ? tooth : portal, 2 | 16);
            }
        }
        sign(level, corner.offset(0, 0, 2), label);
    }

    private static void sign(ServerLevel level, BlockPos pos, Component... lines) {
        level.setBlock(pos, Blocks.CHERRY_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8), 2);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int i = 0; i < lines.length && i < 4; i++) {
                text = text.setMessage(i + 1, lines[i]);
            }
            sign.setText(text, true);
            sign.setText(text, false);
            sign.setWaxed(true);
            sign.setChanged();
            level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), 3);
        }
    }

    /** A half-digested kaasknabbel: a curled arch of kaasknabbels, full of holes and gone soft in places. */
    private static void digestedKnabbel(ServerLevel level, BlockPos base, RandomSource random, Direction.Axis axis) {
        BlockState knabbel = ModBlocks.BLOCK_OF_KAASKNABBELS.get().defaultBlockState();
        BlockState digested = ModBlocks.VERTEERDE_KAASKNABBELS.get().defaultBlockState();
        double radius = 4.5, tube = 1.6;
        for (double a = 0; a <= Math.PI; a += 0.05) {
            double cx = Math.cos(a) * radius, cy = Math.sin(a) * radius;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (dx * dx + dy * dy + dz * dz > tube * tube) {
                            continue;
                        }
                        if (random.nextInt(6) == 0) {
                            continue; // eaten away
                        }
                        int along = (int) Math.round(cx) + dx, up = (int) Math.round(cy) + dy;
                        BlockPos p = axis == Direction.Axis.X ? base.offset(along, up, dz) : base.offset(dz, up, along);
                        if (p.getY() >= FLOOR_Y && level.getBlockState(p).isAir()) {
                            level.setBlock(p, random.nextInt(3) == 0 ? digested : knabbel, 2);
                        }
                    }
                }
            }
        }
    }

    /** A little pool of stomach acid, sunk into the floor. */
    private static void acidPool(ServerLevel level, BlockPos center, double radius) {
        BlockState acid = ModBlocks.MAAGZUUR.get().defaultBlockState();
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    level.setBlock(center.offset(dx, -1, dz), acid, 3);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The mouth (public lobby)
    // ------------------------------------------------------------------------------------------------------------

    public static void buildLobby(ServerLevel level) {
        BlockState wall = ModBlocks.MAAGWAND.get().defaultBlockState();
        BlockState tongue = ModBlocks.TONG.get().defaultBlockState();
        BlockState tooth = ModBlocks.TAND.get().defaultBlockState();
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        int h = LOBBY_HALF;
        for (int x = -h - 2; x <= h + 1; x++) {
            for (int z = -h - 2; z <= h + 1; z++) {
                boolean outer = x == -h - 2 || x == h + 1 || z == -h - 2 || z == h + 1;
                boolean edge = x < -h || x >= h || z < -h || z >= h;
                for (int y = FLOOR_Y - 3; y <= FLOOR_Y + LOBBY_HEIGHT + 1; y++) {
                    BlockState state;
                    if (outer || y == FLOOR_Y - 3 || y == FLOOR_Y + LOBBY_HEIGHT + 1) {
                        state = barrier;
                    } else if (edge || y == FLOOR_Y + LOBBY_HEIGHT) {
                        state = wall;
                    } else if (y < FLOOR_Y) {
                        state = tongue;
                    } else {
                        continue;
                    }
                    set(level, x, y, z, state);
                }
                // a row of teeth all around the edge of the floor
                boolean rim = !edge && (x == -h || x == h - 1 || z == -h || z == h - 1);
                if (rim && (x + z) % 2 == 0) {
                    set(level, x, FLOOR_Y, z, tooth);
                    set(level, x, FLOOR_Y + 1, z, tooth);
                }
                // and the upper teeth hanging from the palate
                if (rim && (x + z) % 2 != 0) {
                    set(level, x, FLOOR_Y + LOBBY_HEIGHT - 1, z, tooth);
                    set(level, x, FLOOR_Y + LOBBY_HEIGHT - 2, z, tooth);
                }
                // the groove down the middle of the tongue
                if (x == 0 && !edge) {
                    set(level, x, FLOOR_Y - 1, z, ModBlocks.MAAGBODEM.get().defaultBlockState());
                }
            }
        }
        // the uvula hanging at the back of the throat
        for (int y = 0; y < 7; y++) {
            int r = y > 4 ? 2 : 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    set(level, dx, FLOOR_Y + LOBBY_HEIGHT - 1 - y, -h + 4 + dz, wall);
                }
            }
        }
        // the way back to the world, and the Tandarts-guh
        buildPortal(level, new BlockPos(-1, FLOOR_Y, h - 3), MaagPortalBlock.Kind.EXIT, Component.translatable("sign.guhs.maag.to_world"));
        GuhNpcEntity dentist = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (dentist != null) {
            dentist.setKind(GuhNpcEntity.Kind.TANDARTS);
            dentist.snapTo(4.5, FLOOR_Y, h - 7.5, 180f, 0f);
            level.addFreshEntity(dentist);
        }
        sign(level, new BlockPos(0, FLOOR_Y, h - 9), Component.translatable("sign.guhs.maag.lobby1"), Component.translatable("sign.guhs.maag.lobby2"));
    }

    /** The portal in the mouth that leads to one stomach, with its owner's name. */
    public static void buildLobbySlot(ServerLevel level, GuhWorldData.Maag maag) {
        int col = maag.index % SLOT_COLS, row = maag.index / SLOT_COLS;
        if (row >= SLOT_ROWS) {
            return; // the mouth is full; the whistle still works
        }
        BlockPos corner = new BlockPos(SLOT_X0 + col * SLOT_DX, FLOOR_Y, SLOT_Z0 + row * SLOT_DZ);
        buildPortal(level, corner, MaagPortalBlock.Kind.MAAG, Component.literal(maag.ownerName).withStyle(ChatFormatting.DARK_RED));
    }

    public static boolean isOwner(Player player, int plot) {
        GuhWorldData.Maag maag = GuhWorldData.get(player.level().getServer()).maagByIndex(plot);
        return maag != null && maag.owner.equals(player.getUUID());
    }

    @Nullable
    public static GuhWorldData.Maag maagAt(net.minecraft.server.MinecraftServer server, BlockPos pos) {
        int plot = plotAt(pos);
        return plot < 0 ? null : GuhWorldData.get(server).maagByIndex(plot);
    }

    /** May this player build / break / use things here? */
    public static boolean mayBuild(Player player, BlockPos pos) {
        if (player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER) && player.isCreative()) {
            return true;
        }
        if (player.level().getServer() == null) {
            return true;
        }
        GuhWorldData.Maag maag = maagAt(player.level().getServer(), pos);
        return maag != null && maag.mayBuild(player.getUUID());
    }

    public static UUID ownerOf(GuhWorldData.Maag maag) {
        return maag.owner;
    }

    private MaagManager() {
    }
}
