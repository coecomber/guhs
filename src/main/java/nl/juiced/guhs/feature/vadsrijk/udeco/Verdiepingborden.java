package nl.juiced.guhs.feature.vadsrijk.udeco;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;
import nl.juiced.guhs.storage.Nbt;

/**
 * U-DECO (feedback round 4: "a better floor indication on the spiral stair in the tree"): a name board over every landing of
 * the inner spiral stair (trap_bordes_1..6: Schorsstad, Honingtak, Kruin 1, 2, 3, Het Nest) with the floor number and where
 * up/down goes. A text display (always facing you), over the door of the landing; the map adds the colour bands and banners per
 * floor (tools/vadsrijk/zone3/bouw3.py trap_verdiepingen).
 * Only there while a player is near (spawned/removed here, the kern tag keeps them out of the save).
 */
public final class Verdiepingborden {
    public static final int AANTAL = 6;
    private static final String TAG = "guhs_udeco_bord";
    private static final ChatFormatting[] KLEUR = {ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.LIGHT_PURPLE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.WHITE};
    private static final Display.TextDisplay[] BORD = new Display.TextDisplay[AANTAL];

    private Verdiepingborden() {
    }

    /** Where board i (0-based) hangs: over the landing's door, 1.5 blocks in front of the trunk wall; the nest: above the arrival. */
    @Nullable
    public static Vec3 plek(int i) {
        BlockPos b = Z3WereldPlekken.ALLE.get("trap_bordes_" + (i + 1));
        if (b == null) {
            return null;
        }
        double dx = b.getX() + 0.5 - Z3WereldPlekken.STAM_X, dz = b.getZ() + 0.5 - Z3WereldPlekken.STAM_Z;
        double r = Math.hypot(dx, dz);
        if (i == AANTAL - 1 || r < 1) {
            return new Vec3(b.getX() + 0.5, b.getY() + 2.6, b.getZ() + 0.5);
        }
        double k = 21.6 / r;
        return new Vec3(Z3WereldPlekken.STAM_X + dx * k, b.getY() + 4.2, Z3WereldPlekken.STAM_Z + dz * k);
    }

    public static Component tekst(int i) {
        String s = "vadsrijk.udeco.verdieping." + (i + 1);
        MutableComponent t = Component.translatable(s + ".naam").withStyle(KLEUR[i], ChatFormatting.BOLD);
        t.append(Component.literal("\n"));
        t.append(Component.translatable("vadsrijk.udeco.verdieping.nummer", i + 1, AANTAL).withStyle(ChatFormatting.WHITE));
        t.append(Component.literal("\n"));
        t.append(Component.translatable(s + ".richting").withStyle(ChatFormatting.GRAY));
        return t;
    }

    public static void tick(ServerLevel sl) {
        if (sl.getGameTime() % 40 != 29) {
            return;
        }
        for (int i = 0; i < AANTAL; i++) {
            Vec3 p = plek(i);
            if (p == null) {
                continue;
            }
            boolean dichtbij = sl.players().stream().anyMatch(q -> !q.isSpectator() && Math.abs(q.getY() - p.y) < 24
                    && q.distanceToSqr(p.x, q.getY(), p.z) < 48 * 48);
            Display.TextDisplay d = BORD[i];
            if (d != null && (d.isRemoved() || d.level() != sl)) {
                d = BORD[i] = null;
            }
            if (!dichtbij) {
                if (d != null) {
                    d.discard();
                    BORD[i] = null;
                }
                continue;
            }
            if (d == null && sl.isLoaded(BlockPos.containing(p))) {
                BORD[i] = maak(sl, p, tekst(i));
            }
        }
    }

    /** Removes every board (dev command / tests). */
    public static void weg() {
        for (int i = 0; i < AANTAL; i++) {
            if (BORD[i] != null) {
                BORD[i].discard();
                BORD[i] = null;
            }
        }
    }

    @Nullable
    static Display.TextDisplay maak(ServerLevel sl, Vec3 p, Component t) {
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("text", ComponentSerialization.CODEC.encodeStart(sl.registryAccess().createSerializationContext(NbtOps.INSTANCE), t).getOrThrow());
        tag.putString("billboard", "center");
        tag.putInt("background", 0x90301808);
        tag.putBoolean("shadow", true);
        tag.putInt("line_width", 220);
        tag.putFloat("view_range", 1.5f);
        CompoundTag tr = new CompoundTag();
        tr.put("translation", floats(0, 0, 0));
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("scale", floats(1.4f, 1.4f, 1.4f));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", tr);
        Nbt.load(d, tag);
        d.snapTo(p.x, p.y, p.z, 0, 0);
        d.addTag(TAG);
        d.addTag(Getallen.TAG);   // (the kern drops these when loaded from disk: never saved twice)
        sl.addFreshEntity(d);
        return d;
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }
}
