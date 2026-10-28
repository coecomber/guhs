package nl.juiced.guhs.feature.dromen;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.storage.Nbt;

/**
 * Into the dream and back (ontwerp ronde 3):
 * <ul>
 *   <li>{@link #naarDroom}: remembers exactly where you lay (your home: dimension, spot, look), builds the islands when
 *       needed, and puts you on the island of the dream with a soft fade (a moment of blindness and a title). You get a
 *       droomkussen when you have none.</li>
 *   <li>{@link #wakker}: back to your home spot, the guh you tucked in wakes up with a yawn next to you, and what you
 *       gave it comes back. Ways to wake: the droomkussen, the Wekkerguh, and dying in a dream (no loss, see DroomRegels).</li>
 *   <li>{@link #zweef}: from island to island: a big bubble carries you (a short fade, bubbles and a plop).</li>
 * </ul>
 * Saved per player in {@code GuhQuests.saved(player)["guhs_droom"]}: Thuis (dim, X/Y/Z, Yaw, Pitch), Mee (the item given
 * along, an item tag), Guh (the tucked-in guh's UUID) and Laatste (the island the player last stood on).
 */
public final class DroomReis {
    static final String KEY = "guhs_droom";

    private DroomReis() {
    }

    static CompoundTag data(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag t = saved.getCompoundOrEmpty(KEY);
        saved.put(KEY, t);
        return t;
    }

    public static boolean inDroom(@Nullable Player p) {
        return p != null && DromenFeature.isDroom(p.level());
    }

    // =====================================================================================================================
    // there
    // =====================================================================================================================

    /** Why this player cannot go dreaming now (null: they can). */
    @Nullable
    public static Component weigering(ServerPlayer p) {
        if (p.level().getServer().getLevel(DromenFeature.DIM) == null) {
            return Component.translatable("gui.guhs.dromen.reis.geen_droom");
        }
        if (Minigames.playing(p) != null) {
            return Component.translatable("gui.guhs.dromen.reis.spel_bezig");
        }
        return null;
    }

    /** To the dream, starting on this island (from home: remembers home first). False when it can't. */
    public static boolean naarDroom(ServerPlayer p, String eiland) {
        Component nee = weigering(p);
        if (nee != null) {
            p.sendSystemMessage(nee.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        ServerLevel droom = p.level().getServer().getLevel(DromenFeature.DIM);
        if (!inDroom(p)) {
            onthoudThuis(p);
        }
        if (p.isSleeping()) {
            p.stopSleepInBed(true, true);
        }
        DroomEilanden.zorg(droom);
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, false));
        zetOp(p, droom, eiland, DroomEilanden.opzet(eiland).aankomst());
        titel(p, Component.translatable("gui.guhs.dromen.titel").withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("gui.guhs.dromen.eiland." + eiland).withStyle(ChatFormatting.WHITE));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.7f);
        if (!heeftKussen(p)) {
            Minigames.give(p, new ItemStack(DromenFeature.DROOMKUSSEN.get()));
            p.sendSystemMessage(Component.translatable("gui.guhs.dromen.reis.kussen").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    /** Puts a player on a spot of an island (same level: a plain teleport; else a dimension change). */
    static void zetOp(ServerPlayer p, ServerLevel level, String eiland, DroomEilanden.Punt punt) {
        Vec3 plek = DroomEilanden.wereld(eiland, punt.plek());
        p.stopRiding();
        if (p.level() == level) {
            p.teleportTo(level, plek.x, plek.y, plek.z, Set.of(), punt.yaw(), 0f, true);
        } else {
            p.teleport(new TeleportTransition(level, plek, Vec3.ZERO, punt.yaw(), 0f, TeleportTransition.DO_NOTHING));
        }
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        data(p).putString("Laatste", eiland);
        data(p).putLong("Aangekomen", level.getGameTime());
    }

    /** Floats a dreaming player to another island (a steiger, the bubble out of the sea, the Droomposthuisje later). */
    public static void zweef(ServerPlayer p, String eiland) {
        if (!(p.level() instanceof ServerLevel level) || !DromenFeature.isDroom(level)) {
            return;
        }
        DroomEilanden.zorg(level, eiland);
        level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 1, p.getZ(), 30, 0.6, 0.8, 0.6, 0.05);
        level.playSound(null, p.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 1f, 1.2f);
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false, false));
        zetOp(p, level, eiland, DroomEilanden.opzet(eiland).aankomst());
        level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 1, p.getZ(), 30, 0.6, 0.8, 0.6, 0.05);
        level.playSound(null, p.blockPosition(), SoundEvents.BUBBLE_POP, SoundSource.PLAYERS, 1f, 1.1f);
        p.sendOverlayMessage(Component.translatable("gui.guhs.dromen.eiland." + eiland).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Ticks since this player arrived somewhere in the dream (steigers wait a moment after an arrival). */
    static long sindsAankomst(ServerPlayer p) {
        return p.level().getGameTime() - data(p).getLongOr("Aangekomen", 0L);
    }

    // =====================================================================================================================
    // back
    // =====================================================================================================================

    private static void onthoudThuis(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        BlockPos bed = p.getSleepingPos().orElse(null);
        Vec3 plek = bed != null ? Vec3.atBottomCenterOf(bed).add(0, 0.6, 0) : p.position();
        t.putString("Dim", p.level().dimension().identifier().toString());
        t.putDouble("X", plek.x);
        t.putDouble("Y", plek.y);
        t.putDouble("Z", plek.z);
        t.putFloat("Yaw", p.getYRot());
        t.putFloat("Pitch", p.getXRot());
        data(p).put("Thuis", t);
    }

    /** Wakes a dreaming player: home again, the tucked-in guh wakes beside you, the given item comes back. */
    public static boolean wakker(ServerPlayer p) {
        if (!inDroom(p)) {
            return false;
        }
        TeleportTransition tr = thuis(p);
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false, false));
        p.teleport(tr);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        DroomRegels.weg(p);
        DroomSlapen.wekGuh(p, true);
        geefMeeTerug(p);
        titel(p, Component.translatable("gui.guhs.dromen.wakker.titel").withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.guhs.dromen.wakker.sub").withStyle(ChatFormatting.WHITE));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6f, 1.6f);
        return true;
    }

    /** Where home is (the saved spot; else the own respawn point; else the world spawn). */
    static TeleportTransition thuis(ServerPlayer p) {
        CompoundTag t = data(p).getCompoundOrEmpty("Thuis");
        if (t.contains("Dim")) {
            Identifier dim = Identifier.tryParse(t.getStringOr("Dim", ""));
            ServerLevel level = dim == null ? null : p.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
            if (level != null && !DromenFeature.isDroom(level)) {
                return new TeleportTransition(level, new Vec3(t.getDoubleOr("X", 0), t.getDoubleOr("Y", 0), t.getDoubleOr("Z", 0)), Vec3.ZERO,
                        t.getFloatOr("Yaw", 0f), t.getFloatOr("Pitch", 0f), TeleportTransition.DO_NOTHING);
            }
        }
        return p.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
    }

    /** What the player gave their guh when tucking it in (empty: nothing), and taking it back. */
    static void bewaarMee(ServerPlayer p, ItemStack stack) {
        if (stack.isEmpty()) {
            data(p).remove("Mee");
        } else {
            data(p).put("Mee", Nbt.saveStack(p.registryAccess(), stack));
        }
    }

    static ItemStack mee(ServerPlayer p) {
        return Nbt.parseStack(p.registryAccess(), data(p).getCompoundOrEmpty("Mee"));
    }

    static void geefMeeTerug(ServerPlayer p) {
        ItemStack stack = mee(p);
        data(p).remove("Mee");
        if (!stack.isEmpty()) {
            Minigames.give(p, stack);
        }
    }

    /** Right-click with the droomkussen: in a dream you wake up; at home it only says what it is for. */
    static InteractionResult kussen(Level level, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            if (inDroom(sp)) {
                wakker(sp);
            } else {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.dromen.kussen.thuis").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean heeftKussen(ServerPlayer p) {
        return p.getInventory().contains(new ItemStack(DromenFeature.DROOMKUSSEN.get()));
    }

    static void titel(ServerPlayer p, Component titel, Component sub) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(titel));
    }
}
