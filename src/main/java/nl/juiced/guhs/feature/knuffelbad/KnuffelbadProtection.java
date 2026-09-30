package nl.juiced.guhs.feature.knuffelbad;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import nl.juiced.guhs.Guhs;

/**
 * The Knuffelbad can't be broken (like the other guh buildings): no breaking, building, buckets, fire or explosions, and
 * mobs don't grief it. Using things is fine: the start gates, the wash tubs (their shower), Badmeester Bubbel. Players
 * in creative mode may change it. Protected is every piece of the knuffelbad structure (and, for the game tests, the
 * areas given to {@link #testGebied}).
 */
public final class KnuffelbadProtection {
    public static final ResourceKey<Structure> STRUCTUUR = ResourceKey.create(Registries.STRUCTURE, Guhs.id("knuffelbad"));

    private record Gebied(ResourceKey<Level> dimension, AABB box) {
    }

    private static final List<Gebied> TEST = new CopyOnWriteArrayList<>();

    static void register() {
        NeoForge.EVENT_BUS.addListener(KnuffelbadProtection::onBreak);
        NeoForge.EVENT_BUS.addListener(KnuffelbadProtection::onPlace);
        NeoForge.EVENT_BUS.addListener(KnuffelbadProtection::onUseBlock);
        NeoForge.EVENT_BUS.addListener(KnuffelbadProtection::onUseItem);
        NeoForge.EVENT_BUS.addListener(KnuffelbadProtection::onExplosion);
        NeoForge.EVENT_BUS.addListener(KnuffelbadProtection::onMobGriefing);
        nl.juiced.guhs.feature.Protected.add(KnuffelbadProtection::beschermd);
    }

    /** (Game tests) protect this area as if it were the Knuffelbad. */
    public static void testGebied(Level level, AABB box) {
        TEST.add(new Gebied(level.dimension(), box));
    }

    static void vergeetAlles() {
        TEST.clear();
    }

    public static boolean beschermd(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        for (Gebied g : TEST) {
            if (g.dimension() == level.dimension() && g.box().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                return true;
            }
        }
        Structure structure = server.registryAccess().registryOrThrow(Registries.STRUCTURE).get(STRUCTUUR);
        return structure != null && server.structureManager().getStructureAt(pos, structure).isValid();
    }

    /** Can this player change this block? (If not, they are told why.) */
    public static boolean geweigerd(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild || player.isCreative() || !beschermd(player.level(), pos)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("gui.guhs.knuffelbad.niet_bouwen").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (geweigerd(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? geweigerd(player, event.getPos()) : entity != null && beschermd(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getItemStack().isEmpty()) {
            return;
        }
        // (a bucket of water on a wash tub is its shower: that's fine)
        if (event.getItemStack().is(net.minecraft.world.item.Items.WATER_BUCKET) && event.getLevel().getBlockState(event.getPos()).is(KnuffelbadFeature.GUH_WASTOBBE.get())) {
            return;
        }
        if (geweigerd(event.getEntity(), event.getPos()) || geweigerd(event.getEntity(),
                event.getPos().relative(event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace()))) {
            event.setUseItem(TriState.FALSE);
        }
    }

    private static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide && event.getItemStack().getItem() instanceof net.minecraft.world.item.BucketItem
                && geweigerd(event.getEntity(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide) {
            event.getAffectedBlocks().removeIf(pos -> beschermd(event.getLevel(), pos));
        }
    }

    private static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && beschermd(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private KnuffelbadProtection() {
    }
}
