package nl.juiced.guhs.feature.sausdieren;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * The Sausloper-stal (structure guhs:sausloper_stal, one template of the same name): where its things are, in template
 * coordinates, and where that is in the world for the copy a Verzorger-guh sits in. The numbers come from
 * tools/features/sausdieren_bouw.py (PLEKKEN); the generator's self-check compares them with this file, so keep the
 * {@code new BlockPos(x, y, z)} lines in this shape.
 * <p>
 * A Verzorger-guh that sits in no copy (a spawn egg, a game test) counts as sitting on {@link #NPC} of an unturned stable:
 * the lap is then laid out around him.
 */
public final class Stal {
    public static final String STRUCTUUR = "sausloper_stal";
    /** The Bezetting ids (and the tags of the template's own entities). */
    public static final String VERZORGER = "sausdieren_verzorger", BEWONER = "sausdieren_bewoner_";
    /** The template's ground layer. */
    public static final int G = 3;

    public static final BlockPos NPC = new BlockPos(15, 4, 15);
    public static final float NPC_YAW = -90.0f;
    /** Where the Sausloper of a test lap waits, next to the pier. */
    public static final BlockPos START = new BlockPos(22, 4, 13);
    /** The four gates of the lap, in order (north, east, south, the finish in the west). */
    public static final List<BlockPos> POORTEN = List.of(new BlockPos(27, 4, 8), new BlockPos(33, 4, 15), new BlockPos(27, 4, 21), new BlockPos(22, 4, 15));
    /** The stable's own Sauslopers: two in the basin, one in the middle stall. */
    public static final List<BlockPos> BEWONERS = List.of(new BlockPos(24, 4, 8), new BlockPos(32, 4, 19), new BlockPos(7, 4, 12));
    /** The middle of the basin. */
    public static final BlockPos BAK = new BlockPos(27, 4, 15);

    /** The lap of one copy, in the world. */
    public record Baan(Vec3 start, float startYaw, List<Vec3> poorten, Vec3 npc) {
    }

    /** The lap that belongs to this Verzorger-guh. */
    public static Baan baan(ServerLevel level, GuhNpcEntity npc) {
        StructureStart start = Bezetting.start(level, STRUCTUUR, npc.blockPosition());
        if (start != null) {
            BlockPos s = Bezetting.wereld(start, null, START);
            if (s != null) {
                List<Vec3> poorten = new ArrayList<>();
                for (BlockPos p : POORTEN) {
                    BlockPos w = Bezetting.wereld(start, null, p);
                    poorten.add(Vec3.atBottomCenterOf(w == null ? s : w));
                }
                BlockPos n = Bezetting.wereld(start, null, NPC);
                Rotation draai = Kopieen.draai(start, null);
                // (the Sausloper starts looking north in the template: towards the first gate)
                return new Baan(Vec3.atBottomCenterOf(s), draai.rotate(net.minecraft.core.Direction.NORTH).toYRot(), poorten,
                        Vec3.atBottomCenterOf(n == null ? npc.blockPosition() : n));
            }
        }
        BlockPos hoek = npc.blockPosition().subtract(NPC);
        List<Vec3> poorten = new ArrayList<>();
        for (BlockPos p : POORTEN) {
            poorten.add(Vec3.atBottomCenterOf(hoek.offset(p)));
        }
        return new Baan(Vec3.atBottomCenterOf(hoek.offset(START)), 180f, poorten, Vec3.atBottomCenterOf(npc.blockPosition()));
    }

    /** The Verzorger-guh nearest to this spot within {@code bereik} blocks (null: none). */
    @Nullable
    public static GuhNpcEntity verzorger(ServerLevel level, BlockPos bij, double bereik) {
        GuhNpcEntity beste = null;
        double d = Double.MAX_VALUE;
        for (GuhNpcEntity npc : level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(bij).inflate(bereik),
                e -> e.isAlive() && e.getKind() == GuhNpcEntity.Kind.VERZORGERGUH)) {
            double a = npc.distanceToSqr(Vec3.atCenterOf(bij));
            if (a < d) {
                d = a;
                beste = npc;
            }
        }
        return beste;
    }

    /** (dev command, tests) Places the stable's template with its corner here, unturned, with its entities. */
    public static boolean plaats(ServerLevel level, BlockPos hoek) {
        Optional<StructureTemplate> template = level.getStructureManager().get(Guhs.id(STRUCTUUR));
        if (template.isEmpty()) {
            return false;
        }
        template.get().placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), 2);
        return true;
    }

    private Stal() {
    }
}
