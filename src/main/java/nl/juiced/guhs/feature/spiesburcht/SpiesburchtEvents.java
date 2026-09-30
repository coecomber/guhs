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
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Asguh (a guh variant that only lives in the Asdal: grey and sooty with glowing cheeks, fire can't hurt it, sweet
 * and tameable, and extra happy with a kaasknabbel), the Guhdex for the new creatures, and the Rookguh count on login.
 */
public final class SpiesburchtEvents {
    /** The creatures you "find" by standing next to them (quest advancements found_*, for the FTB quests). */
    public static final Set<String> CREATURES = Set.of("rookguh", "vonk_mika", "knekel_mika", "aangebrande_mika");

    static void register() {
        NeoForge.EVENT_BUS.addListener(SpiesburchtEvents::onFinalizeSpawn);
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
        return level.getBiome(pos).is(SpiesburchtFeature.ASDAL) && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && (level.getBlockState(pos.below()).is(nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.AS_BLOK.get())
                || level.getBlockState(pos.below()).is(nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.AS_AARDE.get()));
    }

    /** A guh born in the Asdal is an Asguh. */
    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity() instanceof GuhEntity guh && (event.getSpawnType() == EntitySpawnReason.NATURAL
                || event.getSpawnType() == EntitySpawnReason.CHUNK_GENERATION || event.getSpawnType() == EntitySpawnReason.SPAWNER)
                && event.getLevel().getBiome(BlockPos.containing(event.getX(), event.getY(), event.getZ())).is(SpiesburchtFeature.ASDAL)) {
            guh.setVariant(GuhVariant.ASGUH);
        }
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
