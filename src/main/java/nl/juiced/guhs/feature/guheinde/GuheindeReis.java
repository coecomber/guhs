package nl.juiced.guhs.feature.guheinde;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Getting into and out of the Guheinde:
 * <ul>
 *   <li>the portal in the Knabbelkelder takes you to a little platform on the island (the Guhmension spot next to the
 *       portal is remembered for the way back);</li>
 *   <li>the terugportaal on the Knabbelberg takes you back there (or to the middle of the Guhmension);</li>
 *   <li>Knabbelpoorten throw you out to the outer islands and back ({@link #poortDestination});</li>
 *   <li>die in the Guheinde and you keep your things: the Mika's only take your kaasknabbels ("vergevend").</li>
 * </ul>
 */
public final class GuheindeReis {
    /** Player data (GuhQuests.saved): where to come back in the Guhmension. */
    public static final String TERUG = "guhs_guheinde_terug";
    /** Things kept while dying in the Guheinde, until you respawn. */
    private static final String BEWAARD = "guhs_guheinde_bewaard";
    private static final Map<UUID, List<ItemStack>> KEPT = new HashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GuheindeReis::onDrops);
        NeoForge.EVENT_BUS.addListener(GuheindeReis::onRespawn);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player && event.getTo() == GuheindeFeature.GUHEINDE) {
                welcome(player);
            }
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // the portal
    // ------------------------------------------------------------------------------------------------------------

    @Nullable
    public static TeleportTransition portalDestination(ServerLevel from, Entity entity, BlockPos portal) {
        if (GuheindeFeature.isGuheinde(from)) {
            ServerLevel guhmension = from.getServer().getLevel(ModDimensions.GUHMENSION);
            if (guhmension == null) {
                return null;
            }
            Vec3 back = null;
            if (entity instanceof ServerPlayer player && GuhQuests.saved(player).contains(TERUG)) {
                CompoundTag t = GuhQuests.saved(player).getCompoundOrEmpty(TERUG);
                back = new Vec3(t.getDoubleOr("X", 0.0), t.getDoubleOr("Y", 0.0), t.getDoubleOr("Z", 0.0));
            }
            if (back == null) {
                guhmension.getChunk(0, 0);
                back = new Vec3(0.5, guhmension.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0) + 1, 0.5);
            }
            return new TeleportTransition(guhmension, back, Vec3.ZERO, entity.getYRot(), entity.getXRot(),
                    TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET).then(nl.juiced.guhs.quest.GuhDex.GIVE_ON_ARRIVAL));
        }
        ServerLevel guheinde = from.getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (guheinde == null) {
            return null;
        }
        if (entity instanceof ServerPlayer player) {
            BlockPos spot = besidePortal(from, portal);
            CompoundTag t = new CompoundTag();
            t.putDouble("X", spot.getX() + 0.5);
            t.putDouble("Y", spot.getY());
            t.putDouble("Z", spot.getZ() + 0.5);
            GuhQuests.saved(player).put(TERUG, t);
        }
        Vec3 arrival = arrival(guheinde);
        return new TeleportTransition(guheinde, arrival, Vec3.ZERO, Direction.WEST.toYRot(), 0f,
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
    }

    /** The arrival platform on the island (made fresh every time, like the obsidian platform of the End). */
    public static Vec3 arrival(ServerLevel level) {
        int x = GuheindeGevecht.ARRIVAL_X, z = GuheindeGevecht.ARRIVAL_Z;
        level.getChunk(x >> 4, z >> 4);
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int y = ground > level.getMinY() + 8 ? ground : 60;
        BlockState floor = GuheindeFeature.KAASKORST_STENEN.get().defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(new BlockPos(x + dx, y - 1, z + dz), floor, 3);
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlock(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        return new Vec3(x + 0.5, y, z + 0.5);
    }

    /** A spot to stand next to the portal in the Knabbelkelder (not in it, or you'd go right back). */
    public static BlockPos besidePortal(ServerLevel level, BlockPos portal) {
        BlockPos c = portal;
        for (BlockPos p : BlockPos.betweenClosed(portal.offset(-2, 0, -2), portal.offset(2, 0, 2))) {
            if (level.getBlockState(p).is(GuheindeFeature.GUHEINDE_PORTAAL.get()) && isPortalMiddle(level, p)) {
                c = p.immutable();
                break;
            }
        }
        for (int r = 3; r <= 7; r++) {
            for (int dy = 0; dy <= 3; dy++) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos p = c.relative(d, r).above(dy);
                    if (level.getBlockState(p.below()).isSolid() && level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) {
                        return p;
                    }
                }
            }
        }
        return c.above(2);
    }

    private static boolean isPortalMiddle(ServerLevel level, BlockPos p) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(p.relative(d)).is(GuheindeFeature.GUHEINDE_PORTAAL.get())) {
                return false;
            }
        }
        return true;
    }

    private static void welcome(ServerPlayer player) {
        GuheindeEvents.advancement(player, "guheinde_binnen");
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("gui.guhs.guheinde.titel").withStyle(ChatFormatting.LIGHT_PURPLE)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("gui.guhs.guheinde.subtitel").withStyle(ChatFormatting.GOLD)));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Knabbelpoorten
    // ------------------------------------------------------------------------------------------------------------

    /** A Knabbelpoort on a 5x5 pad of kaaskorststenen, leading to exit (null: worked out when someone goes in). */
    public static void buildPoort(ServerLevel level, BlockPos poort, @Nullable BlockPos exit, boolean exact) {
        BlockState pad = GuheindeFeature.KAASKORST_STENEN.get().defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(poort.offset(dx, -1, dz), pad, 3);
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlock(poort.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(poort.above(), pad, 3);
        level.setBlock(poort, GuheindeFeature.KNABBELPOORT.get().defaultBlockState(), 3);
        if (exit != null && level.getBlockEntity(poort) instanceof KnabbelpoortBlock.Entity be) {
            be.setExit(exit, exact);
        }
    }

    /**
     * Where a Knabbelpoort throws you. A poort around the main island leads (the first time: worked out now) about 1200
     * blocks further out in the same direction, to the first outer island there, where a poort back is built.
     */
    @Nullable
    public static TeleportTransition poortDestination(ServerLevel level, Entity entity, BlockPos pos, KnabbelpoortBlock.Entity poort) {
        if (poort.terug) {
            return terugpoortDestination(level, entity);
        }
        if (poort.exit == null) {
            BlockPos landing = findOuterIsland(level, pos);
            buildPoort(level, landing, pos, true);
            poort.setExit(landing, true);
        }
        BlockPos exit = poort.exit;
        // land on the pad, two blocks from the poort (never in it)
        BlockPos stand = exit.offset(2, 0, 0);
        level.getChunk(stand.getX() >> 4, stand.getZ() >> 4);
        if (entity instanceof ServerPlayer player) {
            GuheindeEvents.advancement(player, "guheinde_poort");
        }
        return new TeleportTransition(level, Vec3.atBottomCenterOf(stand), Vec3.ZERO, entity.getYRot(), entity.getXRot(),
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
    }

    /**
     * 2.8: a Terugpoort (structure guhs:guheinde_terugpoort, everywhere on the outer islands) brings you back to the main
     * island: onto the arrival platform next to the Knabbelberg (made fresh, like when you come in: never the void).
     */
    public static TeleportTransition terugpoortDestination(ServerLevel level, Entity entity) {
        Vec3 landing = terugLanding(level);
        if (entity instanceof ServerPlayer player) {
            GuheindeEvents.advancement(player, "guheinde_terugpoort");
            player.sendOverlayMessage(Component.translatable("gui.guhs.guheinde.terugpoort").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return new TeleportTransition(level, landing, Vec3.ZERO, Direction.WEST.toYRot(), 0f,
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
    }

    /** Where a Terugpoort lands you: on the arrival platform of the main island (in the Guheinde; elsewhere its spawn). */
    public static Vec3 terugLanding(ServerLevel level) {
        if (GuheindeFeature.isGuheinde(level)) {
            return arrival(level);
        }
        BlockPos spawn = level.getRespawnData().pos();
        return Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn));
    }

    /** Searches outwards from ~1200 blocks along the poort's direction for a column with ground; the poort goes on top. */
    public static BlockPos findOuterIsland(ServerLevel level, BlockPos from) {
        Vec3 dir = new Vec3(from.getX(), 0, from.getZ()).normalize();
        if (dir.lengthSqr() < 0.5) {
            dir = new Vec3(1, 0, 0);
        }
        for (int d = 1200; d < 1800; d += 16) {
            int x = (int) (dir.x * d), z = (int) (dir.z * d);
            level.getChunk(x >> 4, z >> 4);
            for (int ox = 0; ox < 16; ox += 4) {
                for (int oz = 0; oz < 16; oz += 4) {
                    int bx = (x & ~15) + ox, bz = (z & ~15) + oz;
                    int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
                    if (h > level.getMinY() + 20) {
                        return new BlockPos(bx, h + 1, bz);
                    }
                }
            }
        }
        // no island found: make a little one
        int x = (int) (dir.x * 1200), z = (int) (dir.z * 1200);
        BlockState korst = GuheindeFeature.KAASKORST.get().defaultBlockState();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz <= 18) {
                    for (int dy = -3; dy <= 0; dy++) {
                        level.setBlock(new BlockPos(x + dx, 60 + dy, z + dz), korst, 3);
                    }
                }
            }
        }
        return new BlockPos(x, 61, z);
    }

    // ------------------------------------------------------------------------------------------------------------
    // dying in the Guheinde: the Mika's only take the kaasknabbels
    // ------------------------------------------------------------------------------------------------------------

    public static boolean isKnabbel(ItemStack stack) {
        return stack.is(ModItems.KAAS_KNABBELS.get()) || stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get());
    }

    private static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !GuheindeFeature.isGuheinde(player.level())) {
            return;
        }
        List<ItemStack> kept = new ArrayList<>();
        event.getDrops().removeIf(drop -> {
            ItemStack stack = drop.getItem();
            if (isKnabbel(stack)) {
                return false;
            }
            kept.add(stack.copy());
            return true;
        });
        if (kept.isEmpty()) {
            return;
        }
        KEPT.put(player.getUUID(), kept);
        ListTag list = new ListTag();
        for (ItemStack stack : kept) {
            list.add(nl.juiced.guhs.storage.Nbt.saveStack(player.registryAccess(), stack));
        }
        GuhQuests.saved(player).put(BEWAARD, list);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag saved = GuhQuests.saved(player);
        List<ItemStack> kept = KEPT.remove(player.getUUID());
        if (kept == null && saved.contains(BEWAARD)) {
            kept = new ArrayList<>();
            for (Tag t : saved.getListOrEmpty(BEWAARD)) {
                if (t instanceof CompoundTag c) {
                    ItemStack stack = nl.juiced.guhs.storage.Nbt.parseStack(player.registryAccess(), c);
                    if (!stack.isEmpty()) {
                        kept.add(stack);
                    }
                }
            }
        }
        saved.remove(BEWAARD);
        if (kept == null || kept.isEmpty()) {
            return;
        }
        for (ItemStack stack : kept) {
            if (!player.getInventory().add(stack)) {
                ItemEntity item = player.drop(stack, false);
                if (item != null) {
                    item.setNoPickUpDelay();
                }
            }
        }
        player.sendSystemMessage(Component.translatable("gui.guhs.guheinde.bewaard").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** (game tests) what a player keeps from dying in the Guheinde */
    static List<ItemStack> keptFor(UUID player) {
        return KEPT.getOrDefault(player, List.of());
    }

    private GuheindeReis() {
    }
}
