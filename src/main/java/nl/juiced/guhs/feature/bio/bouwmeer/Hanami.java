package nl.juiced.guhs.feature.bio.bouwmeer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The hanami guhs of a picknickeilandje: wild-looking guhs that sit on the rug under the blossom and look up at it
 * (hanami: looking at flowers). One of them (the bloesemguh) says something sweet when you click it, the plain one points
 * at the treats, the third naps all day; from dusk they all sleep on the rug, at dawn they wake.
 * <p>
 * They are characters (GuhNpcEntity kinds {@code HANAMI_*}: the awake and the sleeping look of a plain guh and of a
 * bloesemguh), not animals: they cannot be tamed, fed, pushed or led away, do not breed, and never walk. Their seats and
 * their day and night belong to the picnic's mand ({@link MandBlock#zorg}); here is what they say and where they look.
 */
public final class Hanami implements NpcRole {
    public static final Hanami ROL = new Hanami();
    /** How many sweet lines the bloesemguh has, how many the plain one, how many sleepy mumbles. */
    public static final int LIEF = 4, GEWOON = 2, SLAAP = 3;
    /** roleData: the point the guh looks at when nobody is close (the blossom above the rug). */
    private static final String KIJK_X = "HanamiX", KIJK_Y = "HanamiY", KIJK_Z = "HanamiZ";
    /** A player this close gets looked at instead of the blossom. */
    private static final double DICHTBIJ = 4.0;

    private Hanami() {
    }

    public static boolean isHanami(GuhNpcEntity.Kind kind) {
        return kind == GuhNpcEntity.Kind.HANAMI_GUH || kind == GuhNpcEntity.Kind.HANAMI_GUH_SLAAPT
                || kind == GuhNpcEntity.Kind.HANAMI_BLOESEMGUH || kind == GuhNpcEntity.Kind.HANAMI_BLOESEMGUH_SLAAPT;
    }

    public static boolean slaapt(GuhNpcEntity.Kind kind) {
        return kind == GuhNpcEntity.Kind.HANAMI_GUH_SLAAPT || kind == GuhNpcEntity.Kind.HANAMI_BLOESEMGUH_SLAAPT;
    }

    /** (MandBlock.zorg) tells the guh on this seat where the blossom is, and keeps it on its seat without a name plate. */
    static void richt(GuhNpcEntity npc, BlockPos mand, Direction kijk, MandBlock.Zitplek z) {
        // the crown of the tree: three blocks towards the tree from the mand's row, well above the rug
        Vec3 bloesem = MeerpaalBlock.punt(mand, kijk, -1.0, 6.5, -3.0);
        npc.roleData.putDouble(KIJK_X, bloesem.x);
        npc.roleData.putDouble(KIJK_Y, bloesem.y);
        npc.roleData.putDouble(KIJK_Z, bloesem.z);
        if (npc.isCustomNameVisible()) {
            npc.setCustomNameVisible(false);
        }
        Vec3 zit = MandBlock.zit(mand, kijk, z);
        if (npc.distanceToSqr(zit) > 0.01) {
            npc.snapTo(zit.x, zit.y, zit.z, npc.getYRot(), npc.getXRot());
        }
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        boolean slaapt = slaapt(npc.getKind());
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, slaapt ? 0.4f : 0.9f, slaapt ? 0.6f : 1.3f);
        GuhAdvancements.grant(p, "hanami_gesproken");
        if (slaapt) {
            GuhQuests.say(p, npc, "gui.guhs.hanami.slaap." + Math.floorMod(npc.getId() + p.tickCount / 60, SLAAP));
            if (npc.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.NOTE, npc.getX(), npc.getY() + 1.3, npc.getZ(), 0, 0.2, 0, 0, 1);
            }
        } else if (npc.getKind() == GuhNpcEntity.Kind.HANAMI_BLOESEMGUH) {
            GuhQuests.say(p, npc, "gui.guhs.hanami.lief." + Math.floorMod(npc.getId() + p.tickCount / 60, LIEF));
            if (npc.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.4, npc.getZ(), 2, 0.25, 0.1, 0.25, 0.01);
            }
        } else {
            GuhQuests.say(p, npc, "gui.guhs.hanami.guh." + Math.floorMod(npc.getId() + p.tickCount / 60, GEWOON));
        }
    }

    /**
     * Awake: looks up at the blossom, or at a player who comes close. Asleep: its head sinks. (The goals GuhNpcEntity
     * gives every character, looking at players ten blocks away and looking around, are taken off once: these guhs came
     * for the blossom.)
     */
    @Override
    public void tick(GuhNpcEntity npc) {
        if (!npc.roleData.contains(KIJK_X)) {
            return;                                   // (not on a picnic: a summoned one just sits)
        }
        if (!npc.goalSelector.getAvailableGoals().isEmpty()) {
            npc.goalSelector.removeAllGoals(g -> true);
        }
        if (slaapt(npc.getKind())) {
            Vec3 voor = npc.position().add(Vec3.directionFromRotation(0, npc.yBodyRot).scale(1.2));
            npc.getLookControl().setLookAt(voor.x, npc.getY() + 0.25, voor.z, 4f, 20f);
            return;
        }
        Player dichtbij = npc.level().getNearestPlayer(npc, DICHTBIJ);
        if (dichtbij != null) {
            npc.getLookControl().setLookAt(dichtbij.getX(), dichtbij.getEyeY(), dichtbij.getZ(), 10f, 40f);
        } else {
            npc.getLookControl().setLookAt(npc.roleData.getDoubleOr(KIJK_X, npc.getX()), npc.roleData.getDoubleOr(KIJK_Y, npc.getY() + 4),
                    npc.roleData.getDoubleOr(KIJK_Z, npc.getZ()), 6f, 40f);
        }
    }
}
