package nl.juiced.guhs.feature.einde2.h3aarde;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.bouwaarde.E2BouwAardeFeature;

/**
 * The cabbage man (DESIGN 5 h3: "MIJN KOLEN!"), a running joke of the chapter: his stall breaks three times, once in every
 * ring, each time a little later in your story: in the lower ring once your passport is stamped, in the middle ring when
 * you come for tea, in the upper ring on your way to the Aardekoning. Every player gets their own three wrecks (the stall
 * is theirs to see, {@link H3Schijn}); the koolman shouts each time, the first time from his own spot and after that from
 * wherever he set his stall up again ({@link H3Acteurs}: he only stays a few seconds). Afterwards he gives the stall away
 * ({@link H3Rollen}): "Bij jou gaat hij vast minder snel stuk."
 */
public final class H3Kool {
    /** From which step stall nr breaks for you (index nr - 1), and how near you must come (blocks). */
    static final int[] VANAF = {E2H3Aarde.AARDROMMEL, E2H3Aarde.THEEHUIS, E2H3Aarde.KONING};
    static final double NABIJ = 3.5;
    /** How long the koolman of stall 2 and 3 stays (ticks), then he is off to find a new spot. */
    static final int BLIJFT = 120;

    private H3Kool() {
    }

    /** How many stalls broke for this player so far. */
    public static int kapot(ServerPlayer p) {
        int n = 0;
        for (int nr = 1; nr <= 3; nr++) {
            n += E2H3Aarde.LIJN.vlag(p, E2H3Aarde.V_KOOL + nr) ? 1 : 0;
        }
        return n;
    }

    static void tik(ServerPlayer p, E2H3Aarde.Stad stad, int stap) {
        for (int nr = 1; nr <= 3; nr++) {
            Entity tijdelijk = H3Acteurs.van(p, "koolman_" + nr);
            if (tijdelijk != null && tijdelijk.tickCount > BLIJFT) {
                H3Acteurs.weg(p, "koolman_" + nr);
            }
            if (stap < VANAF[nr - 1] || stap >= E2H3Aarde.STAPPEN || E2H3Aarde.LIJN.vlag(p, E2H3Aarde.V_KOOL + nr)) {
                continue;
            }
            BlockPos hoek = stad.pos("koolkraam_" + nr);
            if (hoek == null) {
                continue;
            }
            AABB kraam = new AABB(hoek).expandTowards(4, 3, 3).inflate(NABIJ);
            if (kraam.contains(p.position()) && p.getVehicle() == null) {
                breek(p, stad, nr, hoek);
            }
        }
    }

    /** The stall of spot nr breaks for this player: cabbages everywhere, and the koolman's cry. */
    static void breek(ServerPlayer p, E2H3Aarde.Stad stad, int nr, BlockPos hoek) {
        E2H3Aarde.LIJN.vlag(p, E2H3Aarde.V_KOOL + nr, true);
        H3Schijn.ververs(p);
        Vec3 midden = Vec3.atCenterOf(hoek.offset(2, 0, 1));
        Schijn.deeltjes(p, new BlockParticleOption(ParticleTypes.BLOCK, E2BouwAardeFeature.KOOL.get().defaultBlockState()), midden, 40, 1.2, 0.2);
        Schijn.geluid(p, SoundEvents.WOOD_BREAK, hoek, 1f, 0.8f);
        Schijn.geluid(p, SoundEvents.SLIME_SQUISH, hoek, 1f, 0.7f);
        E2Audio.stinger(p, "stinger.kolen");
        GuhNpcEntity koolman = nr == 1 ? eigen(stad) : null;
        if (koolman == null) {
            Vec3 waar = Vec3.atBottomCenterOf(hoek.offset(5, 0, 1));
            koolman = H3Acteurs.npc(p, "koolman_" + nr, GuhNpcEntity.Kind.KOOLMAN, stad.level(), waar, 90f, E2H3Aarde.STRUCTUUR + "/koolman", false);
        }
        if (koolman != null) {
            koolman.speel("mijn_kolen");
            E2H3Aarde.zeg(p, koolman, "kool.kapot." + nr);
        }
        E2H3Aarde.verteller(p, "kool.oorzaak." + nr);
    }

    /** The koolman of the lower ring (the resident on his marker), if he stands there. */
    private static GuhNpcEntity eigen(E2H3Aarde.Stad stad) {
        Vec3 plek = stad.voet("koolman");
        if (plek == null) {
            return null;
        }
        for (GuhNpcEntity npc : stad.level().getEntitiesOfClass(GuhNpcEntity.class, new AABB(BlockPos.containing(plek)).inflate(6),
                n -> n.getKind() == GuhNpcEntity.Kind.KOOLMAN && !n.entityTags().contains(H3Acteurs.TAG))) {
            return npc;
        }
        return null;
    }
}
