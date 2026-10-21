package nl.juiced.guhs.feature.safari.lobby;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.safari.ParkBouwer;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The visitor guhs of the lobby: a few wild guhs in safari outfits that stroll around the square (design 1.5: many in
 * the lobby, few in the park). They are part of the park: you can not pick them up or tame them (a click gives a line).
 * Kept at {@link #AANTAL}; one that strays too far from the square is put back.
 */
public final class Bezoekers {
    public static final String TAG = "guhs_safari_bezoeker";
    static final int AANTAL = 8;
    private static final int ELKE = 200;

    /** Safari clothes when S6 made them (first existing name per slot), else the closest older ones. */
    private static final List<List<String>> PAKJES = List.of(
            List.of("SAFARI_HOEDJE", "SAFARIHOEDJE", "SAFARI_HOED", "STRAW_HAT"),
            List.of("KAKI_VESTJE", "SAFARI_VESTJE", "SAFARI_VEST", "VISVEST"),
            List.of("VERREKIJKER", "SAFARI_VERREKIJKER", "GUH_BACKPACK"));

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % ELKE != 7) {
            return;
        }
        ServerLevel level = Safari.level(server);
        if (level == null || level.players().isEmpty() || !ParkBouwer.klaar(server)) {
            return;
        }
        LobbyPlekken l = LobbyPlekken.get();
        BlockPos c = l.plein;
        if (c.equals(BlockPos.ZERO) || !level.hasChunk(c.getX() >> 4, c.getZ() >> 4)) {
            return;
        }
        int r = l.pleinStraal;
        List<GuhEntity> er = level.getEntitiesOfClass(GuhEntity.class, new AABB(c).inflate(r + 40, 30, r + 40), g -> g.entityTags().contains(TAG));
        for (GuhEntity g : er) {
            if (g.distanceToSqr(c.getX() + 0.5, g.getY(), c.getZ() + 0.5) > (double) (r + 12) * (r + 12) || g.getY() < c.getY() - 6) {
                BlockPos p = plek(level, c, r, g.getRandom());
                g.teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            }
        }
        for (int i = er.size(); i < AANTAL; i++) {
            maak(level, plek(level, c, r, level.getRandom()));
        }
    }

    private static BlockPos plek(ServerLevel level, BlockPos c, int r, RandomSource rnd) {
        for (int t = 0; t < 12; t++) {
            int x = c.getX() + rnd.nextInt(2 * r + 1) - r, z = c.getZ() + rnd.nextInt(2 * r + 1) - r;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - c.getY()) <= 2 && level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                return new BlockPos(x, y, z);
            }
        }
        return c;
    }

    static void maak(ServerLevel level, BlockPos p) {
        maak(level, p, TAG);
    }

    /** A visitor guh in safari clothes with this tag (the square's visitors, or the speeltuin's players). */
    static void maak(ServerLevel level, BlockPos p, String tag) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guh == null) {
            return;
        }
        guh.setVariant(GuhVariant.roll(level.getRandom()));
        for (List<String> slot : PAKJES) {
            for (String naam : slot) {
                try {
                    guh.wear(GuhClothes.valueOf(naam));
                    break;
                } catch (IllegalArgumentException ignored) {
                    // not made (yet): the next name
                }
            }
        }
        guh.addTag(tag);
        guh.setPersistenceRequired();
        guh.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        level.addFreshEntity(guh);
    }

    static boolean isBezoeker(Entity e) {
        return e instanceof GuhEntity && (e.entityTags().contains(TAG) || e.entityTags().contains(Speeltuin.TAG));
    }

    static void opInteractie(PlayerInteractEvent.EntityInteract event) {
        if (!isBezoeker(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer p && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            int i = Math.floorMod(event.getTarget().getUUID().hashCode() + (int) (p.level().getGameTime() / 100), 5);
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.bezoeker." + i).withStyle(ChatFormatting.YELLOW));
        }
    }

    static void opInteractieSpecifiek(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isBezoeker(event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private Bezoekers() {
    }
}
