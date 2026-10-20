package nl.juiced.guhs.feature.safari;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The park is protected (design 1.3): in the safari dimension nobody but a creative player breaks or places a block; doors,
 * gates, buttons and npcs keep working (right clicks are not touched), explosions and mob griefing do nothing. Fire and
 * fluids from outside: {@link nl.juiced.guhs.feature.Protected} (the whole dimension is added there). Also the server hooks
 * of the park: (re)stamping on start and every tick.
 */
public final class SafariRegels {
    private static boolean vrij(Player p) {
        return p.getAbilities().instabuild;
    }

    private static void nee(Player p) {
        if (p instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.safari.beschermd").withStyle(ChatFormatting.GOLD));
        }
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        Player p = event.getPlayer();
        if (Safari.echt(p.level()) && !vrij(p)) {
            event.setCanceled(true);
            nee(p);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.world.level.Level level) || !Safari.echt(level)) {
            return;
        }
        if (event.getEntity() instanceof Player p) {
            if (vrij(p)) {
                return;
            }
            nee(p);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (Safari.echt(event.getLevel())) {
            event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void onGriefing(EntityMobGriefingEvent event) {
        if (Safari.in(event.getEntity())) {
            event.setCanGrief(false);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ParkBouwer.opStart(event.getServer());
        Populatie.opStart(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ParkBouwer.tick(event.getServer());
    }

    private SafariRegels() {
    }
}
