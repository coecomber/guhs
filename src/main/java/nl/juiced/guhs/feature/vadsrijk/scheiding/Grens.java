package nl.juiced.guhs.feature.vadsrijk.scheiding;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Y-SCHEIDING: the border itself (channels 4, 7, 9 and 10 of SCHEIDING_RAPPORT.md).
 * <ul>
 *   <li><b>Nothing but players crosses.</b> Any non-player entity (dropped items, xp orbs, minecarts with chests or hoppers, pets,
 *   mounts, leashed animals, armour stands...) that would travel into or out of the Vadsrijk stays where it is (the travel is
 *   cancelled, so nothing is lost). kern.Buitenstaanders already kept the rest of the mod's guhs out; this also keeps vanilla
 *   and other-mod entities out, and keeps Vadsrijk entities in.</li>
 *   <li><b>Players only enter through the Vadsrijk's own flows</b> ({@code /guhs vadsrijk}, the entry, parties, dungeons...):
 *   a cross-dimension teleport INTO the Vadsrijk is only allowed when Guhs Vadsrijk code is on the call stack ({@link #VAN_ONS}).
 *   FTB Essentials {@code /home}, {@code /back}, {@code /tpa}, {@code /warp}, {@code /rtp} into the Vadsrijk are refused, and
 *   so is any other mod's teleport. Leaving is always allowed (the swap happens on the dimension change: kern.KernEvents.verzoen).
 *   A rider is put down first (vanilla does that too), passengers get off, and leashes held by the player stay behind.</li>
 *   <li><b>Teleport commands inside:</b> FTB Essentials teleports inside the Vadsrijk would skip the zone gates (and
 *   {@code /enderchest} is the survival ender chest): refused for non-ops ({@link #GEWEIGERD}).</li>
 *   <li><b>No respawn point inside:</b> the dimension type already says beds and anchors don't work; any other way to set a
 *   spawn point in the Vadsrijk (another mod) is refused for non-ops.</li>
 * </ul>
 * Ops (permission 2) skip the player rules, for testing ({@link Scheiding#vrijgesteld}). Entities never skip.
 */
public final class Grens {
    /** Class name prefixes whose code may take a player into the Vadsrijk. */
    public static final List<String> VAN_ONS = new CopyOnWriteArrayList<>(List.of("nl.juiced.guhs.feature.vadsrijk."));
    /** Root command names refused for non-ops inside the Vadsrijk (FTB Essentials; with or without its namespace). */
    public static final Set<String> GEWEIGERD = Set.of("home", "sethome", "delhome", "back", "warp", "rtp", "tpa", "tpahere", "tpaccept", "tpl",
            "tpx", "enderchest", "ec", "spawn");

    private Grens() {
    }

    static void reis(EntityTravelToDimensionEvent e) {
        Entity ent = e.getEntity();
        ResourceKey<Level> van = ent.level().dimension(), naar = e.getDimension();
        if (ent.level().isClientSide() || van == naar) {
            return;
        }
        if (!magReizen(ent, van, naar, vanOns())) {
            e.setCanceled(true);
            if (ent instanceof ServerPlayer p) {
                p.sendSystemMessage(Component.translatable("vadsrijk.scheiding.reis.geweigerd").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return;
        }
        if (ent instanceof ServerPlayer p && (van == Vadsrijk.DIM || naar == Vadsrijk.DIM)) {
            losLaten(p);
        }
    }

    /**
     * May this entity travel from {@code van} to {@code naar}? {@code vanOns} = Vadsrijk code asked for it. Only a crossing of the
     * Vadsrijk border is ever refused.
     */
    public static boolean magReizen(Entity e, ResourceKey<Level> van, ResourceKey<Level> naar, boolean vanOns) {
        boolean grens = van != naar && (van == Vadsrijk.DIM || naar == Vadsrijk.DIM);
        if (!grens) {
            return true;
        }
        if (!(e instanceof ServerPlayer p)) {
            return false;   // (items, orbs, carts, pets, mounts...: nothing but players crosses)
        }
        if (van == Vadsrijk.DIM) {
            return true;   // (leaving: always; kern swaps the inventory, Lichaam the body)
        }
        return vanOns || Scheiding.vrijgesteld(p);
    }

    /** Is Guhs Vadsrijk code (other than this border) on the call stack? */
    static boolean vanOns() {
        return StackWalker.getInstance().walk(s -> s.map(StackWalker.StackFrame::getClassName)
                .filter(c -> !c.startsWith(Grens.class.getPackageName() + "."))
                .anyMatch(c -> VAN_ONS.stream().anyMatch(c::startsWith)));
    }

    /** Crossing the border: off the mount, passengers off, leashes stay behind (they would break anyway). */
    static void losLaten(ServerPlayer p) {
        if (p.isPassenger()) {
            p.stopRiding();
        }
        p.ejectPassengers();
        for (Leashable l : Leashable.leashableLeashedTo(p)) {
            l.dropLeash();
        }
    }

    static void spawnpunt(PlayerSetSpawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getSpawnLevel() == Vadsrijk.DIM
                && !Scheiding.vrijgesteld(p)) {
            e.setCanceled(true);
        }
    }

    static void commando(CommandEvent e) {
        var src = e.getParseResults().getContext().getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || !Scheiding.binnen(p) || Scheiding.vrijgesteld(p)) {
            return;
        }
        String wortel = wortel(e.getParseResults().getContext().getNodes().stream().map(n -> n.getNode().getName()).toList());
        if (wortel != null && GEWEIGERD.contains(wortel)) {
            e.setCanceled(true);
            p.sendSystemMessage(Component.translatable("vadsrijk.scheiding.commando.geweigerd", "/" + wortel).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The command's name from its node names ("ftbessentials home" -> "home"). */
    @Nullable
    static String wortel(List<String> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }
        String w = nodes.get(0).toLowerCase(Locale.ROOT);
        if (w.equals("ftbessentials") && nodes.size() > 1) {
            w = nodes.get(1).toLowerCase(Locale.ROOT);
        }
        return w;
    }
}
