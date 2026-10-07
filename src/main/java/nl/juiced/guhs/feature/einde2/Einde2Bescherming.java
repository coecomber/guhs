package nl.juiced.guhs.feature.einde2;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;

/**
 * einde2: the big places of the outer islands can't be broken or built in, so every player finds them whole: no
 * breaking, no placing blocks, no buckets, no explosions, no griefing mobs inside the boxes of a registered structure's
 * pieces (plus a rim), and no fire or fluids from outside ({@link Protected}). Doors, chests, buttons and riding still
 * work; creative players may change everything. The empty rim piece of a story structure protects nothing.
 * <p>
 * hook fase B: the same shape as the sister update's {@code feature/wereld/Bescherming}; after the merge the bodies here
 * become a delegation to it. {@link #uitzondering} is where a quest lets a player change some blocks.
 */
public final class Einde2Bescherming {
    /** Extra protected boxes, only for the game tests (the test world has no generated structures). */
    public static final List<BoundingBox> TEST_AREAS = new CopyOnWriteArrayList<>();
    /** The name the {@link #TEST_AREAS} are protected under (for {@link #uitzondering} in a test). */
    public static final String TEST = "einde2_test";

    private record Regel(int rand, Predicate<StructureStart> welke) {
    }

    private static final Map<String, Regel> REGELS = new ConcurrentHashMap<>();
    private static final Map<String, List<BiPredicate<ServerPlayer, BlockPos>>> UITZONDERINGEN = new ConcurrentHashMap<>();

    private Einde2Bescherming() {
    }

    /** Nobody breaks, places, explodes, floods or griefs inside the boxes of this structure's pieces (+ rand blocks). */
    public static void registreer(String structuur, int rand) {
        registreer(structuur, rand, start -> true);
    }

    /** The same for only some copies (the modular Mika-vestingen: the ones generated before einde2 stay as they are). */
    public static void registreer(String structuur, int rand, Predicate<StructureStart> welke) {
        REGELS.put(structuur, new Regel(rand, welke));
    }

    /** hook fase B: blocks of this structure a player may change after all (a quest block, a door that opens for them). */
    public static void uitzondering(String structuur, BiPredicate<ServerPlayer, BlockPos> mag) {
        UITZONDERINGEN.computeIfAbsent(structuur, k -> new CopyOnWriteArrayList<>()).add(mag);
    }

    /** Is this spot part of a protected einde2 structure (server side; false on the client)? */
    public static boolean beschermd(Level level, BlockPos pos) {
        return structuurBij(level, pos) != null;
    }

    /** The protected structure this spot belongs to (null: none). */
    @Nullable
    public static String structuurBij(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return null;
        }
        for (BoundingBox box : TEST_AREAS) {
            if (box.isInside(pos)) {
                return TEST;
            }
        }
        if (REGELS.isEmpty() || server.dimension() != GuheindeFeature.GUHEINDE || !server.isLoaded(pos)) {
            return null;
        }
        // (the chunk's structure references: every start whose box reaches into this chunk)
        Map<Structure, LongSet> hier = server.structureManager().getAllStructuresAt(pos);
        if (hier.isEmpty()) {
            return null;
        }
        var registry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (Map.Entry<Structure, LongSet> e : hier.entrySet()) {
            Identifier key = registry.getKey(e.getKey());
            Regel regel = key == null || !key.getNamespace().equals(Guhs.MODID) ? null : REGELS.get(key.getPath());
            if (regel == null) {
                continue;
            }
            boolean[] in = {false};
            server.structureManager().fillStartsForStructure(e.getKey(), e.getValue(), start -> {
                if (!in[0] && start.isValid() && regel.welke.test(start)) {
                    for (StructurePiece piece : start.getPieces()) {
                        if (!(piece instanceof LeegteBouwStructure.Rand) && binnen(piece.getBoundingBox(), pos, regel.rand)) {
                            in[0] = true;
                            return;
                        }
                    }
                }
            });
            if (in[0]) {
                return key.getPath();
            }
        }
        return null;
    }

    private static boolean binnen(BoundingBox b, BlockPos p, int rand) {
        return p.getX() >= b.minX() - rand && p.getX() <= b.maxX() + rand && p.getY() >= b.minY() - rand && p.getY() <= b.maxY() + rand
                && p.getZ() >= b.minZ() - rand && p.getZ() <= b.maxZ() + rand;
    }

    private static boolean denied(Player player, BlockPos pos, boolean quiet) {
        if (player.getAbilities().instabuild) {
            return false;
        }
        String structuur = structuurBij(player.level(), pos);
        if (structuur == null) {
            return false;
        }
        if (player instanceof ServerPlayer sp) {
            for (BiPredicate<ServerPlayer, BlockPos> mag : UITZONDERINGEN.getOrDefault(structuur, List.of())) {
                if (mag.test(sp, pos)) {
                    return false;
                }
            }
        }
        if (!quiet) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.einde2.beschermd").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onBreak);
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onPlace);
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onUseBlock);
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onUseItem);
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onExplosion);
        NeoForge.EVENT_BUS.addListener(Einde2Bescherming::onMobGriefing);
        Protected.add(Einde2Bescherming::beschermd);
    }

    private static void onBreak(BreakBlockEvent event) {
        if (denied(event.getPlayer(), event.getPos(), false)) {
            event.setCanceled(true);
        }
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos(), false) : entity != null && beschermd(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getLevel().isClientSide() || stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem || stack.is(Items.BONE_MEAL))) {
            return;
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (denied(event.getEntity(), event.getPos(), true) || denied(event.getEntity(), event.getPos().relative(face), false)) {
            event.setUseItem(TriState.FALSE);
        }
    }

    private static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof BucketItem
                && denied(event.getEntity(), event.getEntity().blockPosition(), false)) {
            event.setCanceled(true);
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server) {
            event.getAffectedBlocks().removeIf(pos -> beschermd(server, pos));
        }
    }

    private static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && beschermd(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }
}
