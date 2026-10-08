package nl.juiced.guhs.feature.spiesburcht;

import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.WildeDieren;

/**
 * The Asguh (a guh variant that only lives in the Asdal: grey and sooty with glowing cheeks, fire can't hurt it, sweet
 * and tameable, and extra happy with a kaasknabbel), the Guhdex for the new creatures, and the Rookguh count on login.
 */
public final class SpiesburchtEvents {
    /** The creatures you "find" by standing next to them (quest advancements found_*, for the FTB quests). */
    public static final Set<String> CREATURES = Set.of("rookguh", "vonk_mika", "knekel_mika", "aangebrande_mika");

    static void register() {
        NeoForge.EVENT_BUS.addListener(SpiesburchtEvents::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, SpiesburchtEvents::onPositionCheck);   // (after the ones that forbid a spot)
        NeoForge.EVENT_BUS.addListener(Drukte::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, SpiesburchtEvents::onDespawnCheck);   // (before world/WildeDieren)
        NeoForge.EVENT_BUS.addListener(SpiesburchtEvents::onHurt);
        NeoForge.EVENT_BUS.addListener(SpiesburchtEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(SpiesburchtEvents::onFeed);
        NeoForge.EVENT_BUS.addListener(SpiesburchtEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SpiesburchtStats.sync(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SpiesburchtStats.sync(player);
            }
        });
    }

    public static boolean isAsguh(Entity entity) {
        return entity instanceof GuhEntity guh && guh.getVariant() == GuhVariant.ASGUH;
    }

    /** Guhs may be born in the dark Asdal (on its ash), where a normal guh would find it too dark. */
    public static boolean asguhMaySpawn(EntityType<GuhEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getBiome(pos).is(SpiesburchtFeature.ASDAL) && Mob.checkMobSpawnRules(type, level, reason, pos, random) && opAs(level, pos);
    }

    /** Does this spot have the ash of the Asdal under it? */
    public static boolean opAs(LevelReader level, BlockPos pos) {
        BlockState grond = level.getBlockState(pos.below());
        return grond.is(BarbecuetherFeature.AS_BLOK.get()) || grond.is(BarbecuetherFeature.AS_AARDE.get());
    }

    /**
     * 1.4.1: why no Asguh was ever born. The spawn rule above lets a guh through in the dark, but after it the spawner asks
     * the animal itself (Mob#checkSpawnRules), and every animal answers with Animal#getWalkTargetValue: no grass under it
     * means "is it light here?", and in the Guhbarbecuether (no sky, ambient light 0.1) that only says yes at block light 12
     * or more. So in the dark Asdal the answer was always no. For a guh that the natural spawner brings on the ash of the
     * Asdal the light no longer counts (the room it needs still does), and no more come than {@link Drukte#MAX_ASGUHS}
     * around a player. Chunk generation stays as it was: such guhs would stay for ever and fill the creature cap.
     */
    static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getResult() != MobSpawnEvent.PositionCheck.Result.DEFAULT || event.getSpawnType() != EntitySpawnReason.NATURAL
                || !(event.getEntity() instanceof GuhEntity guh) || guh.getType() != ModEntities.GUH.get()) {
            return;
        }
        boolean inAsdal = event.getLevel().getBiome(guh.blockPosition()).is(SpiesburchtFeature.ASDAL);
        MobSpawnEvent.PositionCheck.Result antwoord = asguhPlek(guh, event.getLevel(), inAsdal,
                inAsdal && !Drukte.asguhMagErbij(event.getLevel().getLevel(), guh.blockPosition()));
        if (antwoord != MobSpawnEvent.PositionCheck.Result.DEFAULT) {
            event.setResult(antwoord);
        }
    }

    /**
     * The answer for a guh that the natural spawner wants to put here: FAIL when enough wild Asguhs are around already
     * ({@code vol}), SUCCEED on the ash of the Asdal with room for it (however dark), DEFAULT (the animal's own answer)
     * everywhere else.
     */
    static MobSpawnEvent.PositionCheck.Result asguhPlek(GuhEntity guh, ServerLevelAccessor level, boolean inAsdal, boolean vol) {
        if (!inAsdal) {
            return MobSpawnEvent.PositionCheck.Result.DEFAULT;
        }
        if (vol) {
            return MobSpawnEvent.PositionCheck.Result.FAIL;
        }
        return opAs(level, guh.blockPosition()) && guh.checkSpawnObstruction(level) ? MobSpawnEvent.PositionCheck.Result.SUCCEED
                : MobSpawnEvent.PositionCheck.Result.DEFAULT;
    }

    /**
     * A guh born in the Asdal is an Asguh. 1.4.1: one that the natural spawner brings comes and goes like the other wild
     * animals (world/WildeDieren: never saved, gone when everybody is more than 128 blocks away; a tamed or named one
     * stays), so they never pile up and never keep the creature cap full for the Rookguhs, Sauslopers and Sausblubjes.
     */
    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity() instanceof GuhEntity guh && (event.getSpawnType() == EntitySpawnReason.NATURAL
                || event.getSpawnType() == EntitySpawnReason.CHUNK_GENERATION || event.getSpawnType() == EntitySpawnReason.SPAWNER)
                && event.getLevel().getBiome(BlockPos.containing(event.getX(), event.getY(), event.getZ())).is(SpiesburchtFeature.ASDAL)) {
            guh.setVariant(GuhVariant.ASGUH);
            if (event.getSpawnType() == EntitySpawnReason.NATURAL && guh.getType() == ModEntities.GUH.get()) {
                WildeDieren.markeer(guh);
            }
        }
    }

    /**
     * 1.4.1: a wild Asguh that comes and goes does not wander off while you are on your way to it (the other come-and-go
     * animals now and then despawn from 32 blocks, like a monster): it only goes when every player is far away.
     */
    static void onDespawnCheck(MobDespawnEvent event) {
        if (event.getResult() != MobDespawnEvent.Result.DEFAULT || !(event.getEntity() instanceof GuhEntity guh)) {
            return;
        }
        Entity speler = guh.level().getNearestPlayer(guh, -1.0);
        if (speler != null && blijftNog(guh, speler.distanceToSqr(guh))) {
            event.setResult(MobDespawnEvent.Result.DENY);
        }
    }

    /** A wild come-and-go Asguh with the nearest player this far away (squared): does it stay for now? */
    static boolean blijftNog(GuhEntity guh, double afstandSqr) {
        return isAsguh(guh) && WildeDieren.isKomEnGa(guh) && !WildeDieren.magWeg(guh, afstandSqr);
    }

    /** Fire, frying sauce and hot ash: an Asguh doesn't feel a thing. */
    static void onHurt(LivingIncomingDamageEvent event) {
        if (isAsguh(event.getEntity()) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            event.getEntity().clearFire();
        }
    }

    static void onEntityTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!(e instanceof GuhEntity) || !isAsguh(e)) {
            return;
        }
        if (e.isOnFire()) {
            e.clearFire();
        }
        if (e.level().isClientSide() && e.getRandom().nextInt(12) == 0) {
            e.level().addParticle(ParticleTypes.ASH, e.getRandomX(0.5), e.getY() + e.getBbHeight() * 0.6, e.getRandomZ(0.5), 0, 0, 0);
            if (e.getRandom().nextInt(3) == 0) {
                e.level().addParticle(ParticleTypes.SMALL_FLAME, e.getRandomX(0.4), e.getY() + e.getBbHeight() * 0.5, e.getRandomZ(0.4), 0, 0.01, 0);
            }
        }
    }

    /** An Asguh with a kaasknabbel: it glows with joy (the guh itself still eats it as usual). */
    static void onFeed(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !isAsguh(event.getTarget()) || !event.getItemStack().is(ModItems.KAAS_KNABBELS.get())
                || !(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity guh = event.getTarget();
        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 5, 0.4, 0.2, 0.4, 0.02);
        level.sendParticles(ParticleTypes.FLAME, guh.getX(), guh.getY() + guh.getBbHeight() * 0.5, guh.getZ(), 12, 0.4, 0.3, 0.4, 0.02);
        level.sendParticles(ParticleTypes.LAVA, guh.getX(), guh.getY() + guh.getBbHeight() * 0.5, guh.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        level.playSound(null, guh.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.4f, 1.8f);
        player.sendOverlayMessage(Component.translatable("quest.guhs.asguh.blij").withStyle(ChatFormatting.GOLD));
        GuhAdvancements.grant(player, "asguh_gevoerd");
    }

    /** Standing right next to one of the new creatures counts as having found it. */
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 7) {
            return;
        }
        for (Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(3))) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
            if (CREATURES.contains(id) && BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals("guhs")) {
                GuhAdvancements.grant(player, "found_" + id);
            }
        }
    }

    private SpiesburchtEvents() {
    }
}
