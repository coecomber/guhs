package nl.juiced.guhs.feature.verhaal;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * 3.0 (Guhverhalen): the story guhs (Baltoguh, Guhtwo, 626-guh), tameable ONCE per player.
 * <ul>
 *   <li>Per player (in {@code GuhQuests.saved(player)}, lists of ids {@link #VRIJ} / {@link #GETEMD}): the owner slice calls
 *       {@link #geefVrij} when its questline is done; {@link #tem} then gives that player a NEW tamed guh of the variant
 *       (once: the second time it says no). Other players keep their own chance.</li>
 *   <li>The story copies: guhs of the variant placed in a structure or spawned by quest code ({@link #maakKopie}, or a
 *       template entity with {@code NeoForgeData:{guhs_verhaal_guh:"<id>", guhs_verhaal_plek:<long>}}, marked when it joins).
 *       They are never tameable (knabbels do nothing, AnimalTameEvent is cancelled), can't be hurt, never despawn, can't be
 *       pushed, walk back when they wander more than {@link #STRAAL} blocks from their spot, always show their name, can't
 *       be picked up, never live in a huisje and never breed. A right-click goes to the owner slice ({@link #opKlik}).</li>
 * </ul>
 */
public final class VerhaalGuhs {
    /** Player saved-data keys (lists of VerhaalGuh ids). */
    public static final String VRIJ = "guhs_verhaal_vrij", GETEMD = "guhs_verhaal_getemd";
    /** Entity persistent-data keys of a story copy: its VerhaalGuh id, its spot (BlockPos long). */
    public static final String KOPIE = "guhs_verhaal_guh", PLEK = "guhs_verhaal_plek";
    /** A story copy stays within this many blocks of its spot. */
    public static final int STRAAL = 10;
    /** A tamed story guh is this big (rideable). */
    public static final float SCHAAL = 1.25f;

    /** What a click on a story copy does (the owner slice). */
    @FunctionalInterface
    public interface Klik {
        InteractionResult klik(GuhEntity kopie, ServerPlayer p, InteractionHand hand);
    }

    private static final Map<VerhaalGuh, Klik> KLIKKEN = java.util.Collections.synchronizedMap(new EnumMap<>(VerhaalGuh.class));

    // =================================================================================================================
    // per player
    // =================================================================================================================

    /** The questline is done: this player may now tame it (once). */
    public static void geefVrij(ServerPlayer p, VerhaalGuh g) {
        voegToe(p, VRIJ, g);
        nl.juiced.guhs.quest.GuhAdvancements.grant(p, "verhaal_vrij_" + g.id());
    }

    public static boolean isVrij(ServerPlayer p, VerhaalGuh g) {
        return heeft(p, VRIJ, g);
    }

    public static boolean heeftGetemd(ServerPlayer p, VerhaalGuh g) {
        return heeft(p, GETEMD, g);
    }

    public static boolean magTemmen(ServerPlayer p, VerhaalGuh g) {
        return isVrij(p, g) && !heeftGetemd(p, g);
    }

    /**
     * When {@link #magTemmen}: a NEW grown-up guh of the variant at {@code waar} (scale {@link #SCHAAL}: rideable), tamed by p
     * (the normal tame path: Guhdex, Band, moments), with a poof of hearts and a happy sound; marked as tamed for p and the
     * advancement {@code verhalen/<pkg>_getemd}. Null when not allowed (or nothing could be made).
     */
    @Nullable
    public static GuhEntity tem(ServerPlayer p, VerhaalGuh g, Vec3 waar) {
        if (!magTemmen(p, g)) {
            return null;
        }
        ServerLevel level = p.level();
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guh == null) {
            return null;
        }
        guh.setVariant(g.variant());
        guh.setGuhScale(SCHAAL);
        guh.snapTo(waar.x, waar.y, waar.z, p.getYRot() + 180f, 0f);
        guh.setPersistenceRequired();
        level.addFreshEntity(guh);
        guh.tame(p);
        level.broadcastEntityEvent(guh, (byte) 7);   // hearts
        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + 0.8, guh.getZ(), 12, 0.6, 0.5, 0.6, 0.05);
        level.playSound(null, guh, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        voegToe(p, GETEMD, g);
        nl.juiced.guhs.feature.gids.GidsFeature.grant(p, "verhalen/" + g.pkg() + "_getemd");
        nl.juiced.guhs.quest.GuhAdvancements.grant(p, "verhaal_getemd_" + g.id());
        return guh;
    }

    private static boolean heeft(ServerPlayer p, String key, VerhaalGuh g) {
        for (Tag t : GuhQuests.saved(p).getListOrEmpty(key)) {
            if (t.asString().orElse("").equals(g.id())) {
                return true;
            }
        }
        return false;
    }

    private static void voegToe(ServerPlayer p, String key, VerhaalGuh g) {
        if (heeft(p, key, g)) {
            return;
        }
        CompoundTag saved = GuhQuests.saved(p);
        ListTag list = saved.getListOrEmpty(key);
        list.add(StringTag.valueOf(g.id()));
        saved.put(key, list);
    }

    /** (tests / ops) forget everything of this player about this story guh. */
    public static void vergeet(ServerPlayer p, VerhaalGuh g) {
        for (String key : new String[]{VRIJ, GETEMD}) {
            ListTag list = GuhQuests.saved(p).getListOrEmpty(key);
            list.removeIf(t -> t.asString().orElse("").equals(g.id()));
            GuhQuests.saved(p).put(key, list);
        }
    }

    // =================================================================================================================
    // the story copies
    // =================================================================================================================

    /** Spawns a story copy of g standing on plek (marked: untameable, stays there). */
    public static GuhEntity maakKopie(ServerLevel level, VerhaalGuh g, BlockPos plek) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        guh.setVariant(g.variant());
        guh.setGuhScale(SCHAAL);
        guh.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        markeer(guh, g, plek);
        level.addFreshEntity(guh);
        return guh;
    }

    /** Makes a guh a story copy of g with its spot (for entities placed by a template: it happens on join by itself). */
    public static void markeer(GuhEntity guh, VerhaalGuh g, BlockPos plek) {
        guh.getPersistentData().putString(KOPIE, g.id());
        guh.getPersistentData().putLong(PLEK, plek.asLong());
        if (guh.getVariant() != g.variant()) {
            guh.setVariant(g.variant());
        }
        guh.setPersistenceRequired();
        guh.setInvulnerable(true);
        GuhHooks.zet(guh, VerhaalVlaggen.VERHAAL_NPC, true);
    }

    public static boolean isKopie(Entity e) {
        return e instanceof GuhEntity guh && guh.getPersistentData().contains(KOPIE);
    }

    @Nullable
    public static VerhaalGuh kopieVan(Entity e) {
        return isKopie(e) ? VerhaalGuh.byId(e.getPersistentData().getStringOr(KOPIE, "")) : null;
    }

    /**
     * Is this a story copy, as the CLIENT sees it too (the synced VERHAAL_NPC flag; the persistent data is server-only)?
     */
    public static boolean isKopieOveral(GuhEntity guh) {
        return (guh.getKnusVlaggen() & VerhaalVlaggen.VERHAAL_NPC) != 0 || isKopie(guh);
    }

    /** What a right-click on a copy of g does (any hand item, kaas knabbels too): the owner slice. */
    public static void opKlik(VerhaalGuh g, Klik k) {
        KLIKKEN.put(g, k);
    }

    // --- called by GuhEntity and the VerhaalFeature hooks ------------------------------------------------------------

    /** (GuhEntity.mobInteract, both sides) a click on a story copy: never tames; the owner's Klik, or a friendly word. */
    public static InteractionResult klik(GuhEntity kopie, Player player, InteractionHand hand) {
        if (kopie.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        VerhaalGuh g = kopieVan(kopie);
        Klik k = g == null ? null : KLIKKEN.get(g);
        if (k != null) {
            InteractionResult r = k.klik(kopie, sp, hand);
            if (r != InteractionResult.PASS) {
                return r;
            }
        }
        if (hand == InteractionHand.MAIN_HAND) {
            kopie.playSound(ModSounds.GUH_AMBIENT.get(), 1f, kopie.getVoicePitch());
            GuhQuests.say(sp, kopie, "gui.guhs.verhaal.kopie." + (g == null ? "guh" : g.id()));
        }
        return InteractionResult.SUCCESS;
    }

    /** (GuhHooks.tick, server) a copy walks back to its spot; far away (knocked, fell): it hops back. */
    static void tick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 20 != 0 || !isKopie(guh)) {
            return;
        }
        if (!GuhHooks.heeft(guh, VerhaalVlaggen.VERHAAL_NPC)) {
            GuhHooks.zet(guh, VerhaalVlaggen.VERHAAL_NPC, true);
        }
        if (!guh.isInvulnerable()) {
            guh.setInvulnerable(true);
        }
        if (!guh.getPersistentData().contains(PLEK)) {
            guh.getPersistentData().putLong(PLEK, guh.blockPosition().asLong());
        }
        BlockPos plek = BlockPos.of(guh.getPersistentData().getLongOr(PLEK, 0L));
        double d = guh.position().distanceTo(Vec3.atBottomCenterOf(plek));
        if (d > STRAAL * 3 || guh.getY() < guh.level().getMinY() + 4) {
            guh.teleportTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5);
            guh.getNavigation().stop();
        } else if (d > STRAAL && guh.getNavigation().isDone()) {
            guh.getNavigation().moveTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 1.0);
        }
    }

    /** (EntityJoinLevelEvent) a template-placed guh with the persistent keys becomes a real copy. */
    static void opJoin(GuhEntity guh) {
        VerhaalGuh g = kopieVan(guh);
        if (g != null) {
            BlockPos plek = guh.getPersistentData().contains(PLEK) ? BlockPos.of(guh.getPersistentData().getLongOr(PLEK, 0L)) : guh.blockPosition();
            markeer(guh, g, plek);
        }
    }

    private VerhaalGuhs() {
    }
}
