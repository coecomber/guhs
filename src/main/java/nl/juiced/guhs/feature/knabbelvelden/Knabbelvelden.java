package nl.juiced.guhs.feature.knabbelvelden;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * De Eeuwige Knabbelvelden (1.6.0, design ch. 7): the dimension {@code guhs:knabbelvelden}, one flat layer of
 * kaasknabbel blocks under a pink sky where it is always day. Every owner has their own plot, {@link #AFSTAND} blocks
 * apart; at its spawn ({@link #spawn}) stands a Knabbelportaal (placed the first time, {@link #zorgVoorPlot}) and around
 * it their retired guhs live as see-through little ghosts ({@link Geesten}) while they are there.
 * Going in remembers where you came from (like the safari park); {@link #terug} brings you there, else to your respawn point.
 */
public final class Knabbelvelden {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("knabbelvelden"));
    /** Plots this far apart (a row of 64, then the next row). */
    public static final int AFSTAND = 2048, RIJ = 64;
    /** The cheese floor is the layer at y 0: you stand on y 1. */
    public static final int VLOER = 1;
    /** The ghosts stay this close to the plot spawn. */
    public static final int STRAAL = 44;
    public static final String TERUG = "guhs_knabbelvelden_terug";

    public static boolean in(@Nullable Entity e) {
        return e != null && e.level().dimension() == DIM;
    }

    public static boolean echt(@Nullable Level level) {
        return level != null && level.dimension() == DIM;
    }

    @Nullable
    public static ServerLevel level(@Nullable MinecraftServer server) {
        return server == null ? null : server.getLevel(DIM);
    }

    /** Where a plot's spawn is (you stand here, looking north at the portal). */
    public static BlockPos spawn(int plot) {
        return new BlockPos((plot % RIJ) * AFSTAND + 8, VLOER, (plot / RIJ) * AFSTAND + 8);
    }

    /** The lower left part of the plot's portal (4 blocks north of the spawn, along x). */
    public static BlockPos portaal(int plot) {
        return spawn(plot).offset(0, 0, -4);
    }

    /** The plot this spot belongs to (the nearest plot spawn). */
    public static int plotBij(BlockPos pos) {
        int px = Math.max(0, Math.floorDiv(pos.getX() + AFSTAND / 2 - 8, AFSTAND)), pz = Math.max(0, Math.floorDiv(pos.getZ() + AFSTAND / 2 - 8, AFSTAND));
        return pz * RIJ + Math.min(px, RIJ - 1);
    }

    /** The plot's portal stands (placed once; and again when it went missing). */
    public static void zorgVoorPlot(ServerLevel level, int plot) {
        BlockPos p = portaal(plot);
        level.getChunk(p.getX() >> 4, p.getZ() >> 4);
        if (!level.getBlockState(p).is(KnabbelveldenFeature.PORTAAL.get())) {
            KnabbelportaalBlock.zet(level, p, Direction.Axis.X);
        }
    }

    /** To your own plot (remembering where you were). False when the dimension is missing. */
    public static boolean naar(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        ServerLevel level = level(server);
        if (level == null) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.knabbelvelden.storing").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!echt(p.level())) {
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
        int plot = KnabbelveldenData.get(server).plot(p.getUUID());
        zorgVoorPlot(level, plot);
        BlockPos s = spawn(plot);
        p.stopRiding();
        p.teleport(new TeleportTransition(level, Vec3.atBottomCenterOf(s), Vec3.ZERO, 180f, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        p.sendSystemMessage(Component.translatable("gui.guhs.knabbelvelden.welkom").withStyle(ChatFormatting.LIGHT_PURPLE));
        Geesten.bijwerken(level, p.getUUID());
        return true;
    }

    /** Back to where you came from. False when you are not in the Knabbelvelden. */
    public static boolean terug(ServerPlayer p) {
        if (!echt(p.level())) {
            return false;
        }
        MinecraftServer server = p.level().getServer();
        CompoundTag t = GuhQuests.saved(p).getCompound(TERUG).orElse(null);
        p.stopRiding();
        if (t != null) {
            Identifier id = Identifier.tryParse(t.getStringOr("Dim", ""));
            ResourceKey<Level> dim = id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
            ServerLevel level = dim == null || dim == DIM ? null : server.getLevel(dim);
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

    private Knabbelvelden() {
    }
}
