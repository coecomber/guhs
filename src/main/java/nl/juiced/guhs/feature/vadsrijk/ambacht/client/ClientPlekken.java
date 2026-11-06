package nl.juiced.guhs.feature.vadsrijk.ambacht.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht;

/** The client's copy of its own gathering spots (from the server's sync every second) + the HUD values. */
public final class ClientPlekken {
    /** One spot as the server sent it. */
    public record Plek(String id, BlockPos pos, Ambacht ambacht, int tier, boolean leeg, boolean zeldzaam, boolean lvOk, boolean eigen, int raten,
            List<BlockPos> bloemen, String honing) {
        public AABB doos() {
            if (ambacht == Ambacht.VISSEN) {
                return new AABB(pos.getX() - 0.2, pos.getY() + 0.5, pos.getZ() - 0.2, pos.getX() + 1.2, pos.getY() + 1.3, pos.getZ() + 1.2);
            }
            return new AABB(pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1, pos.getX() + 0.9, pos.getY() + 1.0, pos.getZ() + 0.9);
        }
    }

    static volatile List<Plek> PLEKKEN = List.of();
    static volatile String buff = "";
    static volatile long buffOver, gezellig, uitgerust, ontvangen;

    private ClientPlekken() {
    }

    static void zet(CompoundTag d) {
        List<Plek> l = new ArrayList<>();
        ListTag t = d.getListOrEmpty("spots");
        for (int i = 0; i < t.size(); i++) {
            CompoundTag e = t.getCompoundOrEmpty(i);
            Ambacht a = Ambacht.van(e.getStringOr("a", "mijnen"));
            List<BlockPos> bl = new ArrayList<>();
            ListTag b = e.getListOrEmpty("bloemen");
            for (net.minecraft.nbt.Tag x : b) {
                if (x instanceof net.minecraft.nbt.LongTag lt) {
                    bl.add(BlockPos.of(lt.longValue()));
                }
            }
            l.add(new Plek(e.getStringOr("id", ""), BlockPos.of(e.getLongOr("pos", 0L)), a == null ? Ambacht.MIJNEN : a, e.getIntOr("tier", 1),
                    e.getBooleanOr("leeg", false), e.getBooleanOr("zeldzaam", false), e.getBooleanOr("lvOk", true), e.getBooleanOr("eigen", false),
                    e.getIntOr("raten", 0), bl, e.getStringOr("honing", "")));
        }
        PLEKKEN = List.copyOf(l);
        buff = d.getStringOr("buff", "");
        buffOver = d.getLongOr("buffOver", 0L);
        gezellig = d.getLongOr("gezellig", 0L);
        uitgerust = d.getLongOr("uitgerust", 0L);
        ontvangen = System.currentTimeMillis();
    }

    static void wis() {
        PLEKKEN = List.of();
        buff = "";
    }

    /** The spot under the crosshair within reach (null: none). */
    @Nullable
    static Plek bekeken(float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        Vec3 oog = mc.player.getEyePosition(pt);
        Vec3 eind = oog.add(mc.player.getViewVector(pt).scale(5.0));
        Plek beste = null;
        double bd = Double.MAX_VALUE;
        for (Plek p : PLEKKEN) {
            if (p.leeg()) {
                continue;
            }
            var hit = p.doos().clip(oog, eind);
            if (hit.isPresent()) {
                double d = hit.get().distanceToSqr(oog);
                if (d < bd) {
                    bd = d;
                    beste = p;
                }
            }
        }
        return beste;
    }
}
