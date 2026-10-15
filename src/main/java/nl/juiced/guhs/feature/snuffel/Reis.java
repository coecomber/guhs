package nl.juiced.guhs.feature.snuffel;

import java.util.Set;

import javax.annotation.Nullable;

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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * Going to Het Snuffeleiland and back.
 * <ul>
 *   <li>{@link #naarEiland}: remembers EXACTLY where the player stands (level, position, where they look: {@code Thuis}) and
 *   brings them to the island: the beach the first time, the harbour when the captain sails, the last spot they stood on
 *   when the Guhstation starts. On the island they are a dog at once ({@link Hondvorm}).</li>
 *   <li>{@link #naarHuis}: remembers the spot on the island and brings the player back to exactly where they left, a
 *   player with all their own things again. Without a usable home (never stored, that level is gone) it is the own
 *   respawn point, else the world spawn.</li>
 * </ul>
 * Both are only the comfortable way: whoever gets in or out another way (a command, a death) is handled by the one rule
 * of {@link Hondvorm#controleer}; a way in that did not come through here still stores the home it started from
 * ({@link SnuffelEvents}).
 */
public final class Reis {
    /** Where a traveller arrives. */
    public enum Aankomst {
        /** Washed ashore: the first arrival. */
        STRAND,
        /** The captain's boat lands. */
        HAVEN,
        /** Where the player last stood on the island (the Guhstation); the beach when they were never there. */
        LAATSTE
    }

    /** Exactly where a player was before they left for the island. */
    public record Thuis(ResourceKey<Level> dim, Vec3 plek, float yaw, float pitch) {
    }

    private Reis() {
    }

    /** Was this player ever on the island? */
    public static boolean bezocht(ServerPlayer p) {
        return SnuffelData.heeft(p) && SnuffelData.van(p).getBooleanOr("Bezocht", false);
    }

    // =====================================================================================================================
    // there
    // =====================================================================================================================

    /** Why this player cannot leave for the island now (null: they can). */
    @Nullable
    public static Component weigering(ServerPlayer p) {
        if (Eiland.plaats(p.level().getServer()) == null) {
            return Component.translatable("gui.guhs.snuffel.reis.geen_eiland");
        }
        if (Hondvorm.actief(p)) {
            return Component.translatable("gui.guhs.snuffel.reis.al_daar");
        }
        if (Minigames.playing(p) != null) {
            return Component.translatable("gui.guhs.snuffel.reis.spel_bezig");
        }
        if (Cutscenes.bezig(p)) {
            return Component.translatable("gui.guhs.snuffel.reis.even_wachten");
        }
        if (p.isPassenger() || p.isVehicle()) {
            return Component.translatable("gui.guhs.snuffel.reis.afstappen");
        }
        if (p.isSleeping() || !p.isAlive()) {
            return Component.translatable("gui.guhs.snuffel.reis.even_wachten");
        }
        return null;
    }

    /** To the island, to the last spot (the beach the first time). False when refused (the reason is on the screen). */
    public static boolean naarEiland(ServerPlayer p) {
        return naarEiland(p, Aankomst.LAATSTE);
    }

    public static boolean naarEiland(ServerPlayer p, Aankomst waar) {
        Component nee = weigering(p);
        if (nee != null) {
            p.sendOverlayMessage(nee.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        Eiland.Plaats plaats = Eiland.plaats(p.level().getServer());
        return plaats != null && naarEiland(p, plaats, waar);
    }

    /** To THIS island (game tests name their own); no questions asked except that the island can be built. */
    public static boolean naarEiland(ServerPlayer p, Eiland.Plaats plaats, Aankomst waar) {
        if (!Eiland.zorg(plaats)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.reis.geen_eiland").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (!Hondvorm.actief(p)) {
            onthoudThuis(p);
        }
        for (Leashable dier : Leashable.leashableLeashedTo(p)) {
            dier.dropLeash();
        }
        Eiland.Punt punt = switch (waar) {
            case STRAND -> plaats.opzet().strand();
            case HAVEN -> plaats.opzet().haven();
            case LAATSTE -> laatsteOfStrand(p, plaats);
        };
        zetOp(p, plaats, punt);
        plaats.level().playSound(null, BlockPos.containing(plaats.wereld(punt.plek())), SnuffelFeature.REIS_GELUID.get(), SoundSource.PLAYERS, 0.7f, 1f);
        return true;
    }

    /** Puts the player on this spot of the island (relative) and makes them the dog they are there. */
    static void zetOp(ServerPlayer p, Eiland.Plaats plaats, Eiland.Punt punt) {
        Vec3 plek = plaats.wereld(punt.plek());
        if (p.isSleeping()) {
            p.stopSleepInBed(true, true);
        }
        p.stopRiding();
        if (p.level() == plaats.level()) {
            p.teleportTo(plaats.level(), plek.x, plek.y, plek.z, Set.of(), punt.yaw(), 0f, true);
        } else {
            p.teleport(new TeleportTransition(plaats.level(), plek, Vec3.ZERO, punt.yaw(), 0f, TeleportTransition.DO_NOTHING));
        }
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        Hondvorm.controleer(p);
    }

    /** The last spot this player stood on this island, when it is still a place to stand; else the beach. */
    static Eiland.Punt laatsteOfStrand(ServerPlayer p, Eiland.Plaats plaats) {
        CompoundTag t = SnuffelData.heeft(p) ? SnuffelData.van(p).getCompound("Eiland").orElse(null) : null;
        if (t != null) {
            Vec3 rel = new Vec3(t.getDoubleOr("X", 0), t.getDoubleOr("Y", 0), t.getDoubleOr("Z", 0));
            Vec3 w = plaats.wereld(rel);
            if (plaats.buiten(w) <= 0 && vrij(plaats.level(), w, Honden.BREEDTE, 0.9f)) {
                return new Eiland.Punt(rel, t.getFloatOr("Yaw", 0f));
            }
        }
        return plaats.opzet().strand();
    }

    /** Remembers where the player stands on this island (for the next visit, and for when the sea carries them off). */
    static void onthoudEiland(ServerPlayer p, Eiland.Plaats plaats) {
        Vec3 rel = plaats.relatief(p.position());
        CompoundTag t = new CompoundTag();
        t.putDouble("X", rel.x);
        t.putDouble("Y", rel.y);
        t.putDouble("Z", rel.z);
        t.putFloat("Yaw", p.getYRot());
        SnuffelData.van(p).put("Eiland", t);
    }

    /** The last spot on the island (relative to the island's corner), null when never stored. */
    @Nullable
    public static Vec3 laatste(ServerPlayer p) {
        CompoundTag t = SnuffelData.heeft(p) ? SnuffelData.van(p).getCompound("Eiland").orElse(null) : null;
        return t == null ? null : new Vec3(t.getDoubleOr("X", 0), t.getDoubleOr("Y", 0), t.getDoubleOr("Z", 0));
    }

    // =====================================================================================================================
    // home
    // =====================================================================================================================

    /** Stores where the player is NOW as their home (called before every way to the island). */
    public static void onthoudThuis(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        t.putString("Dim", p.level().dimension().identifier().toString());
        t.putDouble("X", p.getX());
        t.putDouble("Y", p.getY());
        t.putDouble("Z", p.getZ());
        t.putFloat("Yaw", p.getYRot());
        t.putFloat("Pitch", p.getXRot());
        SnuffelData.van(p).put("Thuis", t);
    }

    /** The stored home (null: none). */
    @Nullable
    public static Thuis thuis(ServerPlayer p) {
        CompoundTag t = SnuffelData.heeft(p) ? SnuffelData.van(p).getCompound("Thuis").orElse(null) : null;
        Identifier id = t == null ? null : Identifier.tryParse(t.getStringOr("Dim", ""));
        if (id == null) {
            return null;
        }
        return new Thuis(ResourceKey.create(Registries.DIMENSION, id), new Vec3(t.getDoubleOr("X", 0), t.getDoubleOr("Y", 0), t.getDoubleOr("Z", 0)),
                t.getFloatOr("Yaw", 0f), t.getFloatOr("Pitch", 0f));
    }

    /**
     * Home: exactly where the player left from, a player again with everything they had. The spot on the island is
     * remembered. False only when the player is no dog on an island (then there is nothing to go home from).
     */
    public static boolean naarHuis(ServerPlayer p) {
        Eiland.Plaats plaats = Eiland.van(p);
        if (plaats == null || !Hondvorm.actief(p)) {
            return false;
        }
        if (p.onGround() && plaats.buiten(p.position()) <= 0) {
            onthoudEiland(p, plaats);
        }
        plaats.level().playSound(null, p.blockPosition(), SnuffelFeature.REIS_GELUID.get(), SoundSource.PLAYERS, 0.7f, 0.85f);
        if (p.isSleeping()) {
            p.stopSleepInBed(true, true);
        }
        p.stopRiding();
        TeleportTransition tr = terug(p);
        if (tr.newLevel() == p.level()) {
            Vec3 v = tr.position();
            p.teleportTo(tr.newLevel(), v.x, v.y, v.z, Set.of(), tr.yRot(), tr.xRot(), true);
        } else {
            p.teleport(tr);
        }
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        Hondvorm.controleer(p);
        p.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.reis.thuis").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** For a player who turned out to be on no island at login: to the stored home (or the respawn point), no questions. */
    static void thuisZonderEiland(ServerPlayer p) {
        TeleportTransition tr = terug(p);
        if (tr.newLevel() == p.level()) {
            Vec3 v = tr.position();
            p.teleportTo(tr.newLevel(), v.x, v.y, v.z, Set.of(), tr.yRot(), tr.xRot(), true);
        } else {
            p.teleport(tr);
        }
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
    }

    /**
     * The way home: the stored spot itself (exactly), a little higher when something was built there since, and without a
     * usable home (none stored, its level is gone, or it lies on an island) the respawn point or the world spawn.
     */
    static TeleportTransition terug(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        Thuis thuis = thuis(p);
        ServerLevel level = thuis == null || server == null ? null : server.getLevel(thuis.dim());
        if (level != null && Eiland.van(level, thuis.plek()) == null) {
            for (int dy = 0; dy <= 8; dy++) {
                Vec3 v = thuis.plek().add(0, dy, 0);
                // (as low as a crawling player: whoever left from a low spot fits there again; the game picks the pose)
                if (vrij(level, v, 0.6f, 0.6f)) {
                    return new TeleportTransition(level, v, Vec3.ZERO, thuis.yaw(), thuis.pitch(), TeleportTransition.DO_NOTHING);
                }
            }
        }
        TeleportTransition spawn = p.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        if (Eiland.van(spawn.newLevel(), spawn.position()) != null && server != null) {
            // (the respawn point itself lies on an island: the world spawn of the overworld is always elsewhere)
            ServerLevel over = server.overworld();
            BlockPos s = over.getRespawnData().pos();
            return new TeleportTransition(over, Vec3.atBottomCenterOf(s), Vec3.ZERO, 0f, 0f, TeleportTransition.DO_NOTHING);
        }
        return spawn;
    }

    /** Room for a box this wide and high with its feet here. */
    static boolean vrij(ServerLevel level, Vec3 v, float breed, float hoog) {
        BlockPos voet = BlockPos.containing(v.x, v.y + 0.01, v.z);
        if (!level.isInWorldBounds(voet) || !level.getWorldBorder().isWithinBounds(voet)) {
            return false;
        }
        AABB doos = new AABB(v.x - breed / 2, v.y + 0.01, v.z - breed / 2, v.x + breed / 2, v.y + hoog, v.z + breed / 2);
        return level.noCollision(doos);
    }
}
