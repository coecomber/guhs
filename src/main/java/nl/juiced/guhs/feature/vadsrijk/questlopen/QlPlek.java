package nl.juiced.guhs.feature.vadsrijk.questlopen;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPlekken;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;

/**
 * U-QUESTLOPEN: where a step happens, and a teleport to a spot you can stand on near it (floor below, two free blocks, no
 * fluid; around an NPC 2-3 blocks away so you don't stand inside it). Boss steps: outside the arena first (at its door), "in the
 * fight" = between the door and the boss.
 */
public final class QlPlek {
    private QlPlek() {
    }

    /** Where this step points (null: anywhere, e.g. "reach level n": no teleport). */
    @Nullable
    public static BlockPos van(ServerPlayer p, QlStap s) {
        ServerLevel sl = level(p);
        Quest q = s.q();
        if (s.soort() == QlStap.Soort.OPENING || q == null) {
            return NpcPlekken.KAMPPLEIN;
        }
        if (s.soort() == QlStap.Soort.NEEM) {
            return QuestStap.npcPos(p, q.gever());
        }
        if (s.soort() == QlStap.Soort.LEVER) {
            return QuestStap.npcPos(p, q.inleveren());
        }
        Quest.Doel d = s.d();
        if (d == null) {
            return null;
        }
        if (s.baas()) {
            BlockPos b = baasPos(s);
            if (b != null) {
                Arena a = Arenas.op(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
                return a != null ? a.deur() : b;
            }
        }
        if (!d.plek().isEmpty()) {
            BlockPos b = QuestPlek.pos(sl, d.plek());
            if (b != null) {
                return b;
            }
        }
        return switch (d.type()) {
            case PRAAT, ESCORTE -> QuestStap.npcPos(p, d.doel());
            case GEBIED, GEHEIM -> {
                if (d.doel().startsWith("poi:")) {
                    yield poi(d.doel().substring(4));
                }
                if (d.type() == Quest.Doel.Type.GEBIED) {
                    yield gebied(d.doel().split("\\|")[0]);
                }
                var m = switch (d.doel()) {
                    case "knuffel" -> NpcPlekken.KNUFFEL;
                    case "aanwijzing" -> NpcPlekken.AANWIJZING;
                    case "schat" -> NpcPlekken.SCHAT;
                    default -> java.util.Map.<String, BlockPos>of();
                };
                yield m.isEmpty() ? null : m.values().iterator().next();
            }
            case KAMPVUUR -> d.doel().equals("*") ? NpcPlekken.KAMPPLEIN : NpcPlekken.KAMPVUUR.get(d.doel());
            case OPVROLIJK, ELITE -> d.doel().startsWith("gebied:") ? gebied(d.doel().substring(7).split("\\|")[0]) : null;
            case VERZAMEL -> d.bron().startsWith("gebied:") ? gebied(d.bron().substring(7).split("\\|")[0]) : null;
            case BAAS -> baasPos(s);
            case KLASSE -> QuestStap.npcPos(p, "priesteres");
            case POP -> NpcPlekken.TRAINING;
            case LEVEL -> null;
            case MELD -> {
                if (d.doel().startsWith("opruimen:")) {   // 1.10.1: the camp shop (kok) / camp smithy (sergeant)
                    yield QuestStap.npcPos(p, d.doel().equals("opruimen:verkocht") ? "kok" : "rekruteerder");
                }
                if (d.doel().startsWith("quiz:")) {
                    yield QuestStap.npcPos(p, q.gever());
                }
                if (d.doel().equals("zwamtempel:deur")) {
                    yield poi("tempelpoort");
                }
                yield null;
            }
        };
    }

    /** The boss's spot for a boss step (the arena centre, as the tracker knows it). */
    @Nullable
    public static BlockPos baasPos(QlStap s) {
        Quest.Doel d = s.d();
        if (d == null) {
            return null;
        }
        String id;
        if (d.plek().startsWith("baas:")) {
            id = d.plek().substring(5);
        } else if (d.doel().startsWith("gebied:")) {
            id = d.doel().substring(7).split("\\|")[0];
        } else {
            id = d.doel().substring(d.doel().indexOf(':') + 1);
        }
        BlockPos b = QuestPlek.BAAS.get(id);
        return b != null ? b : NpcPlekken.BAAS.get(id);
    }

    @Nullable
    static BlockPos poi(String id) {
        BlockPos b = QuestPlek.POI.get(id);
        return b != null ? b : NpcPlekken.poi(id);
    }

    @Nullable
    static BlockPos gebied(String id) {
        BlockPos b = QuestPlek.GEBIED.get(id);
        return b != null ? b : NpcPlekken.GEBIED_DOEL.get(id);
    }

    @Nullable
    static ServerLevel level(ServerPlayer p) {
        return p.level().getServer() == null ? null : Vadsrijk.level(p.level().getServer());
    }

    // ---- standing + teleporting ----------------------------------------------------------------------------------------------------

    /** Can you stand here (feet in this block)? */
    static boolean staanOp(ServerLevel sl, BlockPos b) {
        return !sl.getBlockState(b.below()).getCollisionShape(sl, b.below()).isEmpty()
                && sl.getBlockState(b).getCollisionShape(sl, b).isEmpty() && sl.getFluidState(b).isEmpty()
                && sl.getBlockState(b.above()).getCollisionShape(sl, b.above()).isEmpty() && sl.getFluidState(b.above()).isEmpty();
    }

    /** A spot to stand near {@code doel}: {@code afstand} blocks away preferred (0 = the spot itself), within 7. */
    public static BlockPos staanPlek(ServerLevel sl, BlockPos doel, int afstand) {
        sl.getChunk(doel.getX() >> 4, doel.getZ() >> 4);
        BlockPos beste = null;
        double score = Double.MAX_VALUE;
        for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3, 4, -4, 6, -6}) {
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    BlockPos b = doel.offset(dx, dy, dz);
                    double r = Math.sqrt(dx * dx + dz * dz);
                    double s = Math.abs(r - afstand) * 2 + Math.abs(dy) * 1.5 + r * 0.1;
                    if (s < score && staanOp(sl, b)) {
                        score = s;
                        beste = b;
                    }
                }
            }
            if (beste != null && score < 3) {
                break;
            }
        }
        return beste != null ? beste : nl.juiced.guhs.feature.vadsrijk.npc.Npcs.staanPlek(sl, doel);
    }

    /** Teleports into the Vadsrijk next to this spot, looking at it. */
    public static void naar(ServerPlayer p, BlockPos doel, int afstand) {
        ServerLevel sl = level(p);
        if (sl == null) {
            return;
        }
        BlockPos b = staanPlek(sl, doel, afstand);
        Vec3 v = Vec3.atBottomCenterOf(b);
        double dx = doel.getX() + 0.5 - v.x, dz = doel.getZ() + 0.5 - v.z;
        float yaw = dx * dx + dz * dz < 0.01 ? p.getYRot() : (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        if (p.isPassenger()) {
            p.stopRiding();
        }
        p.teleport(new TeleportTransition(sl, v, Vec3.ZERO, yaw, 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
    }

    /** A spot inside the arena between its door and the boss (the arena's intro starts there). */
    public static BlockPos inArena(Arena a) {
        BlockPos c = a.centrum(), d = a.deur();
        for (double f : new double[] {0.55, 0.45, 0.65, 0.35, 0.75, 0.25}) {
            BlockPos b = BlockPos.containing(c.getX() + (d.getX() - c.getX()) * f, c.getY(), c.getZ() + (d.getZ() - c.getZ()) * f);
            if (a.binnen(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5)) {
                return b;
            }
        }
        return c.offset(Math.max(3, a.straal() / 2), 0, 0);
    }

    public static double afstand(ServerPlayer p, BlockPos b) {
        return Math.sqrt(p.distanceToSqr(b.getX() + 0.5, b.getY(), b.getZ() + 0.5));
    }
}
