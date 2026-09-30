package nl.juiced.guhs.feature.bibliotheek;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.Guhs;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * The guh library can't be broken (like the verstopguh house): no breaking, building, emptying buckets, lighting fires
 * or blowing it up, mobs don't grief it, and the decorations stay put (no taking books out of the chiseled bookshelves,
 * no eating the cake on the desk, no blowing out candles). The lecterns with guh books open the book for reading (see
 * {@link Leeszaal}): the book never leaves the lectern. Doors, chests, seats, the secret bookcase and the book stand work
 * as usual. Players in creative mode may change it.
 */
public final class BibliotheekProtection {
    public static final ResourceKey<Structure> LIBRARY = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guhbibliotheek"));
    /** Extra protected boxes, only for the GameTests (the headless test world has no generated library). */
    public static final List<BoundingBox> TEST_AREAS = new CopyOnWriteArrayList<>();

    /** Is this spot part of a guh library? */
    public static boolean inLibrary(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        for (BoundingBox box : TEST_AREAS) {
            if (box.isInside(pos)) {
                return true;
            }
        }
        if (server.dimension() != nl.juiced.guhs.world.ModDimensions.GUHMENSION) {
            return false;                                      // (it only generates in the Guhmension)
        }
        Structure structure = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(LIBRARY);
        return structure != null && server.structureManager().getStructureAt(pos, structure).isValid();
    }

    private static boolean denied(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild || !inLibrary(player.level(), pos)) {
            return false;
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.bieb.no_build").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Decorations you could take apart with a right-click. */
    private static boolean decoration(Block block) {
        return block instanceof ChiseledBookShelfBlock || block instanceof LecternBlock || block instanceof FlowerPotBlock
                || block instanceof DecoratedPotBlock || block instanceof CandleBlock || block instanceof CakeBlock;
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (denied(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? denied(player, event.getPos()) : entity != null && inLibrary(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Using an item on a block (buckets, flint and steel, axes...) is not allowed; opening things is fine. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        // a guh book on a library lectern: open it for reading (the book itself never leaves the lectern)
        if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof LecternBlock && player instanceof ServerPlayer reader) {
            Guhboek book = Leeszaal.bookAt(event.getLevel(), event.getPos());
            if (book != null) {
                if (event.getHand() == InteractionHand.MAIN_HAND) {
                    Leeszaal.open(reader, event.getPos(), book);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
        if (decoration(event.getLevel().getBlockState(event.getPos()).getBlock()) && denied(player, event.getPos())) {
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
            return;
        }
        if (event.getItemStack().isEmpty()) {
            return;
        }
        if (denied(player, event.getPos()) || denied(player, event.getPos().relative(event.getFace() == null ? Direction.UP : event.getFace()))) {
            event.setUseItem(TriState.FALSE);
        }
    }

    /** Buckets are used "in the air" too. */
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof net.minecraft.world.item.BucketItem
                && denied(event.getEntity(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server) {
            event.getAffectedBlocks().removeIf(pos -> inLibrary(server, pos));
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && inLibrary(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private BibliotheekProtection() {
    }
}
