package nl.juiced.guhs.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.SleeRailBlock;
import nl.juiced.guhs.block.entity.SleeRailBlockEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhSleeEntity;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.item.GuhCompassItem;
import nl.juiced.guhs.item.SleeRailItem;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.slee.SleePath;

/**
 * 1.2.7: the guh kermis looks after itself on a server with many players (the world is never reset, so all of this also
 * works for a kermis that has been standing for ages):
 * <ul>
 *     <li>it can't be broken (like the other minigame buildings): no breaking, building or blowing up the coaster. Putting
 *     a sled on the rails and riding are fine;</li>
 *     <li>a rider who logs out (or the server stopping) gets off first, so the station's sled stays in the world instead
 *     of being saved along with the player;</li>
 *     <li>the Kermis-guh keeps the station going: the station piece is the finish line again if it lost that (or was
 *     broken), and when fewer than two station sleds are left (older worlds) new ones are put at the station;</li>
 *     <li>the station's sleds come home by themselves ({@link GuhSleeEntity#startHoming()}).</li>
 * </ul>
 */
public final class Kermis {
    /** Where on a station piece a sled stands (as in the structure: 1.5 blocks along the piece). */
    public static final double STATION_T = 0.375;
    /** How many sleds a station has. */
    public static final int SLEDS = 2;
    private static final String MISSING = "KermisSledsMissing";

    /** A kermis: the box it stands in, and its template's origin and rotation in the world. */
    public record Area(BoundingBox box, BlockPos origin, Rotation rotation) {
    }

    /** (Tests) kermissen put down as a plain template: no structure start to ask. */
    public static final Map<BoundingBox, Area> TEST_AREAS = new ConcurrentHashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Kermis::onBreak);
        NeoForge.EVENT_BUS.addListener(Kermis::onPlace);
        NeoForge.EVENT_BUS.addListener(Kermis::onUseBlock);
        NeoForge.EVENT_BUS.addListener(Kermis::onExplosion);
        NeoForge.EVENT_BUS.addListener(Kermis::onMobGriefing);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> offTheSled(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                offTheSled(player);
            }
        });
        Protected.add(Kermis::protectedAt);
    }

    /** A player leaving the game gets off a guh sled first: the sled stays where it is (a vehicle is saved with its only rider). */
    public static void offTheSled(Player player) {
        if (player.getVehicle() instanceof GuhSleeEntity) {
            player.stopRiding();
        }
    }

    // --- where ---------------------------------------------------------------------------------------------------------

    /** The kermis at this spot, or null. */
    @Nullable
    public static Area area(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        for (Area test : TEST_AREAS.values()) {
            if (test.box().isInside(pos)) {
                return test;
            }
        }
        Structure structure = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(GuhCompassItem.GUH_KERMIS);
        if (structure == null) {
            return null;
        }
        StructureStart start = server.structureManager().getStructureAt(pos, structure);
        if (!start.isValid() || start.getPieces().isEmpty() || !(start.getPieces().get(0) instanceof PoolElementStructurePiece piece)) {
            return null;
        }
        return new Area(start.getBoundingBox(), piece.getPosition(), piece.getRotation());
    }

    public static boolean protectedAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        for (BoundingBox test : TEST_AREAS.keySet()) {
            if (test.isInside(pos)) {
                return true;
            }
        }
        Structure structure = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(GuhCompassItem.GUH_KERMIS);
        return structure != null && server.structureManager().getStructureAt(pos, structure).isValid();
    }

    /** The station piece (the finish line) of a kermis as its template has it: where its anchor is and how it lies. */
    @Nullable
    public static SleePath.Placement station(ServerLevel level, Area area) {
        StructureTemplate template = level.getStructureManager().get(Guhs.id("guh_kermis")).orElse(null);
        if (template == null) {
            return null;
        }
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(area.origin(), new StructurePlaceSettings().setRotation(area.rotation()),
                ModBlocks.SLEE_RAIL.get())) {
            if (info.nbt() != null && info.nbt().contains("Finish")) {
                return new SleePath.Placement(info.pos(), info.state().getValue(SleeRailBlock.FACING), info.state().getValue(SleeRailBlock.SHAPE));
            }
        }
        return null;
    }

    // --- protection ----------------------------------------------------------------------------------------------------

    /** May this player change the block at pos? (No, in a kermis: unless in creative.) */
    public static boolean denied(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild || !protectedAt(player.level(), pos)) {
            return false;
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.kermis.no_build").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    private static void onBreak(BreakBlockEvent event) {
        if (denied(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Blocks only: a sled put on the rails is an entity (SledItem), that stays possible. */
    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos()) : entity != null && protectedAt(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /**
     * Using an item on a block of the kermis (laying more rails, stripping the fence posts, a shovel on the grass...): not
     * in survival. A guh sled may be put on the rails (SledItem), and clicking with an empty hand is fine.
     */
    private static void onUseBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getItemStack().isEmpty() || event.getItemStack().getItem() instanceof nl.juiced.guhs.item.SledItem) {
            return;
        }
        if (denied(event.getEntity(), event.getPos())
                || denied(event.getEntity(), event.getPos().relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace()))) {
            event.setUseItem(net.minecraft.util.TriState.FALSE);
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server) {
            event.getAffectedBlocks().removeIf(pos -> protectedAt(server, pos));
        }
    }

    private static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && protectedAt(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    // --- the Kermis-guh keeps the station going ------------------------------------------------------------------------

    /** Every 5 seconds (the Kermis-guh's tick, server side). */
    public static void tick(GuhNpcEntity npc) {
        if (npc.tickCount % 100 != 7 || !(npc.level() instanceof ServerLevel level)) {
            return;
        }
        Area area = area(level, npc.blockPosition());
        if (area != null) {
            look(npc, level, area);
        }
    }

    /** One look at the station (also for the tests): heals the finish line and the sleds. */
    public static void look(GuhNpcEntity npc, ServerLevel level, Area area) {
        SleePath.Placement station = station(level, area);
        if (station == null || !level.isLoaded(station.anchor())) {
            return;
        }
        if (!(level.getBlockState(station.anchor()).getBlock() instanceof SleeRailBlock)) {
            if (!SleeRailItem.canPlace(level, station, null)) {
                return;                                             // something else stands there now: leave it
            }
            SleeRailItem.place(level, station);
        }
        if (level.getBlockEntity(station.anchor()) instanceof SleeRailBlockEntity rail && !rail.isFinish()) {
            rail.setFinish(true);
        }
        SleePath.Piece piece = SleePath.Piece.of(level, station.anchor());
        if (piece == null || !entitiesLoaded(level, area.box())) {
            return;
        }
        BoundingBox b = area.box();
        List<GuhSleeEntity> sleds = level.getEntitiesOfClass(GuhSleeEntity.class,
                new AABB(b.minX() - 4, b.minY() - 8, b.minZ() - 4, b.maxX() + 5, b.maxY() + 16, b.maxZ() + 5), s -> s.isLocked() && s.isAlive());
        if (sleds.size() > SLEDS) {                                 // a sled came back with its rider after a new one was made
            int extra = sleds.size() - SLEDS;
            for (GuhSleeEntity sled : sleds) {
                if (extra > 0 && !sled.isVehicle()) {
                    sled.discard();
                    extra--;
                }
            }
        }
        if (sleds.size() >= SLEDS || sleds.stream().anyMatch(Entity::isVehicle)) {
            npc.roleData.remove(MISSING);
            return;
        }
        int missing = npc.roleData.getIntOr(MISSING, 0) + 1;         // (not at the first look: chunks may still be coming in)
        if (missing < 3) {
            npc.roleData.putInt(MISSING, missing);
            return;
        }
        npc.roleData.remove(MISSING);
        for (int i = sleds.size(); i < SLEDS; i++) {
            newSled(level, piece);
        }
    }

    /** The free spots of a station: on the finish piece, and on the piece after it. */
    public static List<Spot> spots(Level level, SleePath.Piece finish) {
        List<Spot> spots = new ArrayList<>();
        spots.add(new Spot(finish, STATION_T, true));
        SleePath.Next next = SleePath.next(level, finish, true);
        if (next != null) {
            spots.add(new Spot(next.piece(), next.forward() ? STATION_T : 1 - STATION_T, next.forward()));
        }
        return spots;
    }

    /** A place for a sled to stand at the station: the piece, where along it, and which way the sled faces there. */
    public record Spot(SleePath.Piece piece, double t, boolean forward) {
        public Vec3 pos() {
            return piece.at(t).pos();
        }

        /** Is no (other) sled standing here? */
        public boolean free(Level level, @Nullable GuhSleeEntity self) {
            return level.getEntitiesOfClass(GuhSleeEntity.class, new AABB(pos(), pos()).inflate(1.6), s -> s != self && s.isAlive() && !s.isRunning()).isEmpty();
        }
    }

    /** The first free spot at the station of this finish piece (the last one if all are taken). */
    public static Spot freeSpot(Level level, SleePath.Piece finish, @Nullable GuhSleeEntity self) {
        List<Spot> spots = spots(level, finish);
        for (Spot spot : spots) {
            if (spot.free(level, self)) {
                return spot;
            }
        }
        return spots.get(spots.size() - 1);
    }

    private static void newSled(ServerLevel level, SleePath.Piece finish) {
        GuhSleeEntity sled = ModEntities.GUH_SLEE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (sled == null) {
            return;
        }
        Spot spot = freeSpot(level, finish, null);
        SleePath.Point p = spot.piece().at(spot.t());
        sled.snapTo(p.pos());
        sled.setLocked(true);
        sled.setSpeed(2);
        sled.putOn(spot.piece(), p.pos(), spot.forward() ? p.heading() : p.heading().reverse());
        level.addFreshEntity(sled);
    }

    private static boolean entitiesLoaded(ServerLevel level, BoundingBox box) {
        for (int cx = SectionPos.blockToSectionCoord(box.minX()); cx <= SectionPos.blockToSectionCoord(box.maxX()); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(box.minZ()); cz <= SectionPos.blockToSectionCoord(box.maxZ()); cz++) {
                if (!level.areEntitiesLoaded(ChunkPos.pack(cx, cz))) {
                    return false;
                }
            }
        }
        return true;
    }

    private Kermis() {
    }
}
