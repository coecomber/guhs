package nl.juiced.guhs.feature.guheinde;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.ModDimensions;
import nl.juiced.guhs.world.Terugkeer;

/**
 * 1.2.7: the magere guhs in the cells of a Knabbelkelder are there for every player. The grey guh in a cell stays (it
 * remembers who fed it: once per player); the colourful, freed guh is a new one that runs off. A cell without its magere
 * guh (a world from before 1.2.7, where feeding one used it up) gets one back while somebody is in the kelder.
 */
public final class MagereCellen {
    public static final ResourceKey<Structure> KELDER = ResourceKey.create(Registries.STRUCTURE, Guhs.id("knabbelkelder"));
    /** Where the ten cell guhs stand in the template (tools/features/guheinde_bouw.py, kelder_kerker). */
    public static final List<BlockPos> CELLEN = List.of(
            new BlockPos(30, 4, 50), new BlockPos(29, 4, 54), new BlockPos(29, 4, 58), new BlockPos(30, 4, 62), new BlockPos(29, 4, 66),
            new BlockPos(37, 4, 50), new BlockPos(36, 4, 54), new BlockPos(37, 4, 58), new BlockPos(37, 4, 62), new BlockPos(36, 4, 66));
    /** Entity data of a magere guh: its cell (a block position) and who fed it (player UUIDs). */
    public static final String THUIS = "guhs_cel_thuis", GEVOERD = "guhs_cel_gevoerd";
    public static final String SOORT = "cel";
    /** A cell whose guh is gone (somebody hit it too hard, njeg) gets a new one after this long. */
    public static final long BIJVUL_WACHT = 20 * 60 * 5;
    /** Cells this close to a player are looked at. */
    public static final double BEREIK = 48;
    /** A cell guh further than this from its cell goes back (somebody left the door open). */
    public static final double LOS = 3.0;
    /** The freed guh runs around for this long, then it's off home (unless somebody tamed it). */
    public static final int VRIJ_TICKS = 20 * 45;

    private record Vrij(GuhEntity guh, long tot) {
    }

    private static final List<Vrij> VRIJ = new CopyOnWriteArrayList<>();

    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % 100 != 0 || player.isSpectator()
                || player.level().dimension() != ModDimensions.GUHMENSION) {
            return;
        }
        ServerLevel level = player.level();
        for (PoolElementStructurePiece stuk : Terugkeer.stukken(level, KELDER, player.blockPosition(), "knabbelkelder")) {
            List<BlockPos> cellen = new ArrayList<>();
            for (BlockPos lokaal : CELLEN) {
                cellen.add(Terugkeer.wereld(stuk, lokaal));
            }
            controleer(level, cellen, player.position());
        }
    }

    static void onServerTick(ServerTickEvent.Post event) {
        for (Vrij v : VRIJ) {
            GuhEntity guh = v.guh();
            if (!guh.isAlive() || guh.isTame()) {
                VRIJ.remove(v);
            } else if (guh.level().getGameTime() >= v.tot()) {
                if (guh.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.5, guh.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
                }
                guh.discard();
                VRIJ.remove(v);
            }
        }
    }

    /**
     * Looks at these cells (world positions): every magere guh nearby knows its cell (the nearest one), strays go back,
     * and a cell near {@code bij} that has had no guh for a while gets a new one. Returns the new guhs.
     */
    public static List<GuhEntity> controleer(ServerLevel level, List<BlockPos> cellen, Vec3 bij) {
        List<GuhEntity> nieuw = new ArrayList<>();
        if (cellen.isEmpty()) {
            return nieuw;
        }
        AABB blok = new AABB(cellen.get(0));
        for (BlockPos cel : cellen) {
            blok = blok.minmax(new AABB(cel));
        }
        List<GuhEntity> mager = level.getEntitiesOfClass(GuhEntity.class, blok.inflate(12, 6, 12),
                g -> g.getVariant() == GuhVariant.MAGER && g.isAlive());
        for (GuhEntity guh : mager) {
            if (thuis(guh) == null) {
                BlockPos best = null;
                for (BlockPos cel : cellen) {
                    if (best == null || cel.distToCenterSqr(guh.position()) < best.distToCenterSqr(guh.position())) {
                        best = cel;
                    }
                }
                guh.getPersistentData().putLong(THUIS, best.asLong());
            }
        }
        for (BlockPos cel : cellen) {
            if (cel.distToCenterSqr(bij) > BEREIK * BEREIK) {
                continue;
            }
            GuhEntity bewoner = null;
            for (GuhEntity guh : mager) {
                if (cel.equals(thuis(guh))) {
                    bewoner = guh;
                    break;
                }
            }
            if (Terugkeer.moetTerug(level, SOORT, cel, bewoner != null, BIJVUL_WACHT)) {
                nieuw.add(nieuweMagere(level, cel));
            } else if (bewoner != null && !bewoner.isPassenger() && !bewoner.isLeashed()
                    && bewoner.distanceToSqr(cel.getX() + 0.5, cel.getY(), cel.getZ() + 0.5) > LOS * LOS) {
                bewoner.getNavigation().stop();
                bewoner.teleportTo(cel.getX() + 0.5, cel.getY(), cel.getZ() + 0.5);
            }
        }
        return nieuw;
    }

    /** The cell of this magere guh (null: not known yet). */
    @Nullable
    public static BlockPos thuis(GuhEntity guh) {
        CompoundTag data = guh.getPersistentData();
        return data.contains(THUIS) ? BlockPos.of(data.getLongOr(THUIS, 0L)) : null;
    }

    /** A new grey, skinny guh in this cell. */
    public static GuhEntity nieuweMagere(ServerLevel level, BlockPos cel) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        guh.snapTo(cel.getX() + 0.5, cel.getY(), cel.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
        guh.setVariant(GuhVariant.MAGER);
        guh.setPersistenceRequired();
        guh.getPersistentData().putLong(THUIS, cel.asLong());
        level.addFreshEntity(guh);
        return guh;
    }

    /** Did this player feed this magere guh already? */
    public static boolean heeftGevoerd(GuhEntity guh, UUID player) {
        return guh.getPersistentData().getCompoundOrEmpty(GEVOERD).contains(player.toString());
    }

    static void onthoud(GuhEntity guh, UUID player) {
        CompoundTag data = guh.getPersistentData();
        CompoundTag gevoerd = data.getCompoundOrEmpty(GEVOERD);
        gevoerd.putBoolean(player.toString(), true);
        data.put(GEVOERD, gevoerd);
    }

    /**
     * VAHOEG! The colourful guh this knabbel frees: a new guh (a random colour) that hops out of the grey one and runs
     * off. It's wild: tame it if you like, otherwise it's off home after a while.
     */
    public static GuhEntity bevrijd(ServerPlayer player, GuhEntity mager) {
        ServerLevel level = player.level();
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        guh.snapTo(mager.getX(), mager.getY(), mager.getZ(), mager.getYRot(), 0);
        guh.setVariant(GuhVariant.roll(mager.getRandom()));
        level.addFreshEntity(guh);
        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 8, 0.4, 0.3, 0.4, 0);
        level.sendParticles(new DustParticleOptions(0xFF8CBF /* 1, 0.55, 0.75 */, 1.5f), guh.getX(), guh.getY() + 0.5, guh.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
        level.playSound(null, guh, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1.2f, 1.2f);
        guh.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 2));
        Vec3 away = guh.position().subtract(player.position()).normalize();
        guh.setDeltaMovement(away.x * 0.6, 0.5, away.z * 0.6);
        guh.hurtMarked = true;
        GuhQuests.say(player, guh, "gui.guhs.guheinde.mager.vahoeg");
        VRIJ.add(new Vrij(guh, level.getGameTime() + VRIJ_TICKS));
        return guh;
    }

    /** "You fed me already!" (no knabbel eaten, nothing counted). */
    static void alGevoerd(ServerPlayer player, GuhEntity mager) {
        player.sendOverlayMessage(Component.translatable("gui.guhs.guheinde.mager.al_gevoerd").withStyle(ChatFormatting.LIGHT_PURPLE));
        mager.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.3f);
    }

    /** (Tests) the freed guhs that are still running around. */
    public static List<GuhEntity> vrij() {
        List<GuhEntity> uit = new ArrayList<>();
        for (Iterator<Vrij> it = VRIJ.iterator(); it.hasNext(); ) {
            uit.add(it.next().guh());
        }
        return uit;
    }

    private MagereCellen() {
    }
}
