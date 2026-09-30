package nl.juiced.guhs.feature.evenementen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The sterrenregen (at night): stars fall around the players. Each one lies glowing where it landed for a while (walk
 * into it for a bit of sterrenstof), and at some of them a starry guh turns up: it can be tamed (more easily than a
 * normal guh) for {@link #TAME_WINDOW} ticks; then it goes back up to the stars. A tamed one stays yours for good.
 */
public class Sterrenregen extends Evenement {
    public static final int DURATION = 20 * 120;
    /** How long a starry guh waits to be tamed. Stars stop falling this long before the end. */
    public static final int TAME_WINDOW = 20 * 60;
    /** A star every so many ticks (plus a random bit). */
    public static final int EVERY = 70, EVERY_RANDOM = 50;
    /** 1 in this many stars brings a starry guh (and the third star at the latest). */
    public static final int GUH_CHANCE = 3;
    public static final int LIE_TICKS = 20 * 30;
    /** Stars come from this far away (up and to the side). */
    public static final double FALL_FROM = 45;
    /** Stars land this far from a player. */
    public double minDist = 7, maxDist = 20;
    final List<VallendeSterEntity> stars = new ArrayList<>();
    final Set<UUID> landed = new HashSet<>();
    final Set<UUID> starGuhs = new HashSet<>();
    int starCount, guhCount, nextStar = 20;

    public Sterrenregen(ServerLevel level, Vec3 center) {
        super(EvenementType.STERRENREGEN, level, center, DURATION);
    }

    public List<VallendeSterEntity> stars() {
        stars.removeIf(Entity::isRemoved);
        return stars;
    }

    public Set<UUID> starGuhs() {
        return starGuhs;
    }

    @Override
    protected void tickEvent() {
        if (age % 10 == 0) {
            followParticipants();
        }
        if (spontaneous && age >= nextStar && age < duration - TAME_WINDOW) {
            nextStar = age + EVERY + level.random.nextInt(EVERY_RANDOM);
            spawnStar();
        }
        for (VallendeSterEntity star : List.copyOf(stars())) {
            star.serverStep();
            if (star.landed() && landed.add(star.getUUID())) {
                land(star);
            }
            if (star.landedTicks > LIE_TICKS) {
                level.sendParticles(ParticleTypes.END_ROD, star.getX(), star.getY() + 0.3, star.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
                star.discard();
                continue;
            }
            if (star.landed()) {
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, star.getBoundingBox().inflate(0.5),
                        p -> p.isAlive() && !p.isSpectator())) {
                    pickUp(player, star);
                    break;
                }
            }
        }
        if (age % 10 == 5) {
            checkStarGuhs();
        }
    }

    /** A star shoots down towards a spot near someone; null when there was no good spot. */
    @Nullable
    public VallendeSterEntity spawnStar() {
        Vec3 around = aroundSomeone();
        double angle = level.random.nextDouble() * Math.PI * 2, dist = minDist + level.random.nextDouble() * (maxDist - minDist);
        BlockPos ground = Evenementen.ground(level, Mth.floor(around.x + Math.cos(angle) * dist), Mth.floor(around.z + Math.sin(angle) * dist),
                around.y, true);
        return ground == null ? null : dropStar(ground, FALL_FROM);
    }

    /** A star falls onto this spot from this far away (high up, at a slant). */
    public VallendeSterEntity dropStar(BlockPos ground, double from) {
        VallendeSterEntity star = EvenementenFeature.VALLENDE_STER.get().create(level);
        Vec3 target = new Vec3(ground.getX() + 0.5, ground.getY() + 0.1, ground.getZ() + 0.5);
        double angle = level.random.nextDouble() * Math.PI * 2;
        Vec3 start = target.add(Math.cos(angle) * from * 0.6, from * 0.8, Math.sin(angle) * from * 0.6);
        start = new Vec3(start.x, Math.min(start.y, level.getMaxBuildHeight() - 2), start.z);
        star.aimAt(target);
        star.moveTo(start.x, start.y, start.z, 0, 0);
        entities.add(star.getUUID());
        level.addFreshEntity(star);
        stars.add(star);
        starCount++;
        level.playSound(null, target.x, target.y + 10, target.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 2f, 0.6f);
        return star;
    }

    /** A star lands: a flash, a chime, and sometimes a starry guh. */
    void land(VallendeSterEntity star) {
        level.sendParticles(ParticleTypes.FIREWORK, star.getX(), star.getY() + 0.3, star.getZ(), 30, 0.3, 0.3, 0.3, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, star.getX(), star.getY() + 0.3, star.getZ(), 15, 0.6, 0.4, 0.6, 0.05);
        level.playSound(null, star.getX(), star.getY(), star.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 2f, 1.4f);
        level.playSound(null, star.getX(), star.getY(), star.getZ(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.AMBIENT, 1.5f, 1.2f);
        boolean guh = level.random.nextInt(GUH_CHANCE) == 0 || (guhCount == 0 && landed.size() >= 3);
        if (guh) {
            spawnStarGuh(star.position());
        }
    }

    /** A starry guh turns up here, tameable for a little while. */
    public GuhEntity spawnStarGuh(Vec3 at) {
        GuhEntity guh = ModEntities.GUH.get().create(level);
        guh.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        guh.setVariant(GuhVariant.STARRY);
        guh.setPersonality(GuhPersonality.random(level.random));
        guh.setGuhScale(0.6f + level.random.nextFloat() * 0.6f);
        guh.setGlowingTag(true);
        guh.getPersistentData().putLong(Evenementen.STER, level.getGameTime() + TAME_WINDOW);
        entities.add(guh.getUUID());
        starGuhs.add(guh.getUUID());
        level.addFreshEntity(guh);
        guhCount++;
        for (ServerPlayer player : players()) {
            if (player.distanceToSqr(at) < 48 * 48) {
                player.displayClientMessage(Component.translatable("gui.guhs.evenement.sterrenregen.guh").withStyle(ChatFormatting.AQUA), true);
            }
        }
        return guh;
    }

    /** Walked into a landed star: a bit of sterrenstof. */
    void pickUp(ServerPlayer player, VallendeSterEntity star) {
        star.discard();
        player.getInventory().placeItemBackInInventory(new ItemStack(EvenementenFeature.STERRENSTOF.get()));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 1.5f);
    }

    /** Tamed starry guhs stay (for good); the others go back to the stars when their time is up. */
    void checkStarGuhs() {
        for (UUID id : List.copyOf(starGuhs)) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof GuhEntity guh) || guh.isRemoved()) {
                if (entity == null || entity.isRemoved()) {
                    starGuhs.remove(id);
                }
                continue;
            }
            if (guh.isTame()) {
                keep(guh);
            } else if (level.getGameTime() >= guh.getPersistentData().getLong(Evenementen.STER)) {
                backToTheStars(guh);
            }
        }
    }

    /** A tamed starry guh: no longer part of the event. */
    void keep(GuhEntity guh) {
        starGuhs.remove(guh.getUUID());
        entities.remove(guh.getUUID());
        guh.getPersistentData().remove(Evenementen.STER);
        guh.setGlowingTag(false);
        level.sendParticles(ParticleTypes.END_ROD, guh.getX(), guh.getY() + 0.5, guh.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        if (guh.getOwner() instanceof ServerPlayer owner) {
            owner.sendSystemMessage(announce(Component.translatable("gui.guhs.evenement.sterrenregen.tamed")));
            GuhAdvancements.grant(owner, "evenement_sterrenguh");
        }
    }

    void backToTheStars(GuhEntity guh) {
        starGuhs.remove(guh.getUUID());
        level.sendParticles(ParticleTypes.END_ROD, guh.getX(), guh.getY() + 0.5, guh.getZ(), 25, 0.3, 1.5, 0.3, 0.08);
        level.playSound(null, guh.getX(), guh.getY(), guh.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5f, 0.8f);
        guh.discard();
    }

    @Override
    protected void cleanUp(Entity entity) {
        if (entity instanceof GuhEntity guh) {
            if (guh.isTame()) {
                keep(guh);
            } else {
                backToTheStars(guh);
            }
            return;
        }
        level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 0.3, entity.getZ(), 4, 0.2, 0.2, 0.2, 0.03);
        entity.discard();
    }

    @Override
    protected void finish(boolean completed) {
        say(completed ? "gui.guhs.evenement.sterrenregen.end" : "gui.guhs.evenement.stopped", type.displayName());
    }
}
