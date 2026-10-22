package nl.juiced.guhs.feature.safari;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Into the park and back (step 1: only the dev command; the bus comes later). Going in remembers where you were (per
 * player, like Guhpixel's way home); {@link #terug} brings you there, else to your own respawn point.
 */
public final class SafariToegang {
    public static final String TERUG = "guhs_safari_terug";

    /** To the entrance of the park (stamping its tile first when needed). False when the dimension is missing. */
    public static boolean naarPark(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        ServerLevel level = Safari.level(server);
        if (level == null || !ParkBouwer.zorg(server)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.storing").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!Safari.echt(p.level())) {
            CompoundTag t = new CompoundTag();
            t.putString("Dim", p.level().dimension().identifier().toString());
            t.putDouble("X", p.getX());
            t.putDouble("Y", p.getY());
            t.putDouble("Z", p.getZ());
            t.putFloat("Yaw", p.getYRot());
            t.putFloat("Pitch", p.getXRot());
            GuhQuests.saved(p).put(TERUG, t);
        }
        for (Leashable dier : Leashable.leashableLeashedTo(p)) {
            dier.dropLeash();
        }
        Vec3 aankomst = aankomst(level);
        p.stopRiding();
        p.teleport(new TeleportTransition(level, aankomst, Vec3.ZERO, aankomstYaw(), 0f, TeleportTransition.DO_NOTHING));
        p.sendSystemMessage(Component.translatable("gui.guhs.safari.welkom").withStyle(ChatFormatting.GOLD));
        return true;
    }

    /**
     * F1b: where you step out of the bus: on the busplein beside the lobby's Guh-safaribus (its west side, the middle of
     * its length), on the ground. (The park's ingang is the middle of the bus itself: you used to land on its roof.)
     * Without a bus in lobby.json: the ingang.
     */
    public static Vec3 aankomst(ServerLevel level) {
        net.minecraft.world.phys.AABB bus = nl.juiced.guhs.feature.safari.lobby.LobbyPlekken.get().bus;
        BlockPos in = ParkData.get().ingang;
        int x = in.getX(), z = in.getZ(), y0 = in.getY();
        if (bus != null) {
            x = (int) Math.floor(bus.minX) - 6;
            z = (int) Math.floor(bus.minZ) + 8;
            y0 = (int) Math.floor(bus.minY);
        }
        level.getChunk(x >> 4, z >> 4);
        int y = Math.max(y0, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
        return new Vec3(x + 0.5, y, z + 0.5);
    }

    /** Looking north, at the lobby (F1b). */
    public static float aankomstYaw() {
        return nl.juiced.guhs.feature.safari.lobby.LobbyPlekken.get().bus != null ? 180f : ParkData.get().ingangYaw;
    }

    /** Back to where the player came from. False when they are not in the park. */
    public static boolean terug(ServerPlayer p) {
        if (!Safari.echt(p.level())) {
            return false;
        }
        MinecraftServer server = p.level().getServer();
        CompoundTag t = GuhQuests.saved(p).getCompound(TERUG).orElse(null);
        p.stopRiding();
        if (t != null) {
            Identifier id = Identifier.tryParse(t.getStringOr("Dim", ""));
            ResourceKey<Level> dim = id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
            ServerLevel level = dim == null || dim == Safari.DIM ? null : server.getLevel(dim);
            if (level != null) {
                GuhQuests.saved(p).remove(TERUG);
                p.teleport(new TeleportTransition(level, new Vec3(t.getDoubleOr("X", 0), t.getDoubleOr("Y", 80), t.getDoubleOr("Z", 0)), Vec3.ZERO,
                        t.getFloatOr("Yaw", 0f), t.getFloatOr("Pitch", 0f), TeleportTransition.DO_NOTHING));
                return true;
            }
        }
        p.teleport(p.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING));
        return true;
    }

    private SafariToegang() {
    }
}
