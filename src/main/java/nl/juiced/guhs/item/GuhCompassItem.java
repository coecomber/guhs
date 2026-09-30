package nl.juiced.guhs.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import nl.juiced.guhs.Guhs;

/**
 * A guh compass: points to the middle of the nearest structure of its kind (the central room of a guh cave, a Mika
 * camp, a guh picnic...) in the dimension you're in. Uses the vanilla compass needle (lodestone target).
 */
public class GuhCompassItem extends Item {
    public static final ResourceKey<Structure> GUH_CAVES = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guh_caves"));
    public static final ResourceKey<Structure> CHALLENGING_GUH_CAVES = ResourceKey.create(Registries.STRUCTURE, Guhs.id("challenging_guh_caves"));
    public static final ResourceKey<Structure> MIKA_KAMP = ResourceKey.create(Registries.STRUCTURE, Guhs.id("mika_kamp"));
    public static final ResourceKey<Structure> VERSTOPGUH_HUIS = ResourceKey.create(Registries.STRUCTURE, Guhs.id("verstopguh_huis"));
    public static final ResourceKey<Structure> GUH_KASTEEL = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guh_kasteel"));
    public static final ResourceKey<Structure> GUH_KERMIS = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guh_kermis"));
    public static final ResourceKey<Structure> VADSIG_HEILIGDOM = ResourceKey.create(Registries.STRUCTURE, Guhs.id("vadsig_heiligdom"));
    private static final int SEARCH_RADIUS_CHUNKS = 100;
    /** The castle is so rare that its compass looks much further. */
    private static final int CASTLE_RADIUS_CHUNKS = 600;
    /** Structures that are so rare (or fussy about where they go) that the compass looks as far as for the castle. */
    private static final java.util.Set<String> FAR = java.util.Set.of("guh_kasteel", "onderwater", "knuffeldal_stadje");
    /** Bumped when the search changes, so compasses in old worlds look again straight away. */
    private static final int SEARCH_VERSION = 5;

    @javax.annotation.Nullable
    private final ResourceKey<Structure> target;

    public GuhCompassItem(@javax.annotation.Nullable ResourceKey<Structure> target, Properties properties) {
        super(properties);
        this.target = target;
    }

    /** What this compass looks for (the super compass: whatever you chose in its menu). */
    @javax.annotation.Nullable
    protected ResourceKey<Structure> target(ItemStack stack) {
        return target;
    }

    /** The old single-purpose compasses, replaced by the Guhmension super compass (kept so old ones still work). */
    public boolean isOld() {
        return target == GUH_CAVES || target == CHALLENGING_GUH_CAVES || target == GUH_KERMIS || target == VERSTOPGUH_HUIS || target == GUH_KASTEEL;
    }

    /**
     * Always points at the nearest structure of its kind in the dimension you're in (looked up again every 16 blocks
     * you walk), and while you hold it, it tells you about how far away that is. Spins where there are none.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 20 != 0) {
            return;
        }
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ResourceKey<Structure> target = target(stack);
        if (target == null) {
            if (entity instanceof net.minecraft.world.entity.player.Player player && (player.getMainHandItem() == stack || player.getOffhandItem() == stack)) {
                player.displayClientMessage(Component.translatable("item.guhs.guhmensie_superkompas.choose").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return;
        }
        String dim = server.dimension().location().toString();
        BlockPos here = entity.blockPosition();
        boolean fresh = data.getInt("SearchVersion") == SEARCH_VERSION && dim.equals(data.getString("SearchedDim")) && data.contains("SearchedAt")
                && BlockPos.of(data.getLong("SearchedAt")).closerThan(here, 16);
        if (!fresh) {
            BlockPos found = findCenter(server, target, here);
            data.putString("SearchedDim", dim);
            data.putInt("SearchVersion", SEARCH_VERSION);
            data.putLong("SearchedAt", here.asLong());
            if (found != null) {
                data.putLong("Target", found.asLong());
                point(stack, server, found);
            } else {
                data.remove("Target");
                stack.remove(DataComponents.LODESTONE_TRACKER);
            }
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        boolean held = entity instanceof net.minecraft.world.entity.player.Player player
                && (player.getMainHandItem() == stack || player.getOffhandItem() == stack);
        if (held) {
            Component text;
            if (data.contains("Target")) {
                BlockPos t = BlockPos.of(data.getLong("Target"));
                int blocks = (int) Math.round(Math.hypot(t.getX() - here.getX(), t.getZ() - here.getZ()) / 10) * 10;
                text = Component.translatable("item.guhs.guh_compass.distance", Math.max(blocks, 5));
            } else {
                text = Component.translatable("item.guhs.guh_compass.none");
            }
            ((net.minecraft.world.entity.player.Player) entity).displayClientMessage(text.copy().withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    private static void point(ItemStack stack, ServerLevel level, BlockPos target) {
        LodestoneTracker tracker = new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), target)), false);
        if (!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER))) {
            stack.set(DataComponents.LODESTONE_TRACKER, tracker);
        }
    }

    /**
     * Middle of the start piece of the nearest structure of this kind, or null. (Vanilla's search goes ring by ring over
     * the placement grid, which isn't always the nearest one: here every possible start chunk within reach is sorted
     * by distance and the first one that really has the structure wins.)
     */
    public static BlockPos findCenter(ServerLevel level, ResourceKey<Structure> key, BlockPos from) {
        Optional<Holder.Reference<Structure>> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key);
        if (holder.isEmpty()) {
            return null;
        }
        var state = level.getChunkSource().getGeneratorState();
        java.util.List<Object[]> candidates = new java.util.ArrayList<>();
        int px = from.getX() >> 4, pz = from.getZ() >> 4;
        for (var placement : state.getPlacementsForStructure(holder.get())) {
            if (placement instanceof net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement rings) {
                // (the Knabbelkelders: a few fixed spots in rings around the middle of the world)
                var spots = state.getRingPositionsFor(rings);
                if (spots != null) {
                    for (net.minecraft.world.level.ChunkPos c : spots) {
                        candidates.add(new Object[]{(long) (c.x - px) * (c.x - px) + (long) (c.z - pz) * (c.z - pz), c, rings});
                    }
                }
                continue;
            }
            if (!(placement instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread)) {
                continue;
            }
            // the rare ones are far apart (and big ones often skip a spot that isn't flat enough): look ten of their grid cells far
            int radius = FAR.contains(key.location().getPath()) ? CASTLE_RADIUS_CHUNKS : Math.max(SEARCH_RADIUS_CHUNKS, Math.min(1000, spread.spacing() * 16));   // (2.7: new biomes made some rarer; 2.8: the Knuffeldal too, e.g. a kaasmijn 10 km away)
            int cells = radius / spread.spacing() + 1;
            int cx = Math.floorDiv(px, spread.spacing()), cz = Math.floorDiv(pz, spread.spacing());
            for (int dx = -cells; dx <= cells; dx++) {
                for (int dz = -cells; dz <= cells; dz++) {
                    net.minecraft.world.level.ChunkPos c = spread.getPotentialStructureChunk(state.getLevelSeed(),
                            (cx + dx) * spread.spacing(), (cz + dz) * spread.spacing()); // takes chunk coordinates, not cell numbers
                    long d = (long) (c.x - px) * (c.x - px) + (long) (c.z - pz) * (c.z - pz);
                    if (d <= (long) radius * radius) {
                        candidates.add(new Object[]{d, c, spread});
                    }
                }
            }
        }
        candidates.sort(java.util.Comparator.comparingLong(o -> (Long) o[0]));
        for (Object[] cand : candidates) {
            net.minecraft.world.level.ChunkPos c = (net.minecraft.world.level.ChunkPos) cand[1];
            var result = level.structureManager().checkStructurePresence(c, holder.get().value(),
                    (net.minecraft.world.level.levelgen.structure.placement.StructurePlacement) cand[2], false);
            if (result == net.minecraft.world.level.levelgen.structure.StructureCheckResult.START_NOT_PRESENT) {
                continue;
            }
            StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(level.getChunk(c.x, c.z, ChunkStatus.STRUCTURE_STARTS)),
                    holder.get().value(), level.getChunk(c.x, c.z, ChunkStatus.STRUCTURE_STARTS));
            if (start != null && start.isValid() && !start.getPieces().isEmpty()) {
                return start.getPieces().get(0).getBoundingBox().getCenter();
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        if (isOld()) {
            tooltip.add(Component.translatable("item.guhs.guh_compass.old").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.guhs.guh_compass.how").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
