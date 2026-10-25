package nl.juiced.guhs.feature.mijnspellen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.feature.spelen.Spelhal;

/**
 * What the two cave minigames of 1.7.0 share (Karretjes-wissels and the Kegelgrot): titles and sounds for one player,
 * and the protection of their buildings (the cave hall with its own walls and the entrance hut: no breaking, no building,
 * no buckets, no explosions; levers, doors and chairs still work; creative players may change them). Not the rock around
 * the hall, the extra entrance tunnels or the rock between the hall and the hut ({@link Spelhal}).
 */
public final class Mijnspel {
    /** The structures (their pieces are protected). */
    public static final List<ResourceKey<Structure>> STRUCTUREN = new CopyOnWriteArrayList<>();
    /** Extra protected boxes, only for the GameTests (the test world has no generated buildings). */
    public static final List<BoundingBox> TEST_GEBIEDEN = new CopyOnWriteArrayList<>();

    /** Is this spot part of a piece of one of the buildings (server side)? */
    public static boolean inGebouw(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        for (BoundingBox box : TEST_GEBIEDEN) {
            if (box.isInside(pos)) {
                return true;
            }
        }
        return server.dimension() == nl.juiced.guhs.world.ModDimensions.GUHMENSION && Spelhal.beschermd(server, pos, STRUCTUREN);
    }

    private static boolean geweigerd(Player player, BlockPos pos, boolean stil) {
        if (player.getAbilities().instabuild || !inGebouw(player.level(), pos)) {
            return false;
        }
        if (!stil) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.mijnspellen.niet_bouwen").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (geweigerd(event.getPlayer(), event.getPos(), false)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player ? geweigerd(player, event.getPos(), false) : entity != null && inGebouw(entity.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getLevel().isClientSide() || stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem || stack.is(Items.BONE_MEAL))) {
            return;
        }
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (geweigerd(event.getEntity(), event.getPos(), true) || geweigerd(event.getEntity(), event.getPos().relative(face), false)) {
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel server) {
            event.getAffectedBlocks().removeIf(pos -> inGebouw(server, pos));
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !(event.getEntity() instanceof Player) && inGebouw(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    // --- for one player ------------------------------------------------------------------------------------------------

    public static void titel(ServerPlayer p, Component title, @Nullable Component sub, int in, int stay, int out) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub == null ? Component.empty() : sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** A sound only this player hears (26.1: ServerPlayer#playNotifySound is gone). */
    public static void geluid(ServerPlayer p, SoundEvent sound, float volume, float pitch) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS,
                p.getX(), p.getY(), p.getZ(), volume, pitch, p.getRandom().nextLong()));
    }

    /** The biggest horizontal distance (a square, like the other minigames' "walked away"). */
    public static double afstand(Entity a, Entity b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
    }

    private Mijnspel() {
    }
}
