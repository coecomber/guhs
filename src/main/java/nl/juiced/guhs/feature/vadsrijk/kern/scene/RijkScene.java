package nl.juiced.guhs.feature.vadsrijk.kern.scene;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * A non-click camera scene of the dimension (cutscenes there just run, design: "standaard NIET click-to-continue"): camera
 * keyframes in world coordinates (the camera glides between them, smoothstep), black bars, and an optional big title card
 * from tick {@code titelOp} for {@code titelDuur} ticks. While it plays the player can't act and takes no damage.
 * Build with {@link #maak}; play with {@link RijkScenes#speel}.
 */
public record RijkScene(String id, int duur, List<Punt> punten, String titelKey, String ondertitelKey, int titelOp, int titelDuur, int kleur) {
    /** A keyframe: at tick t the camera is at pos and looks at kijk; {@code volgEntity} >= 0: look at that entity instead. */
    public record Punt(int t, Vec3 pos, Vec3 kijk, int volgEntity) {
    }

    public static Builder maak(String id, int duur) {
        return new Builder(id, duur);
    }

    public static final class Builder {
        private final String id;
        private final int duur;
        private final List<Punt> punten = new ArrayList<>();
        private String titel = "", onder = "";
        private int titelOp, titelDuur, kleur = 0xFFFFFF;

        Builder(String id, int duur) {
            this.id = id;
            this.duur = duur;
        }

        public Builder camera(int t, Vec3 pos, Vec3 kijk) {
            punten.add(new Punt(t, pos, kijk, -1));
            return this;
        }

        public Builder cameraVolgt(int t, Vec3 pos, int entityId) {
            punten.add(new Punt(t, pos, pos, entityId));
            return this;
        }

        public Builder titel(String titelKey, String ondertitelKey, int op, int duur, int kleur) {
            this.titel = titelKey;
            this.onder = ondertitelKey;
            this.titelOp = op;
            this.titelDuur = duur;
            this.kleur = kleur;
            return this;
        }

        public RijkScene bouw() {
            return new RijkScene(id, duur, List.copyOf(punten), titel, onder, titelOp, titelDuur, kleur);
        }
    }

    public void schrijf(FriendlyByteBuf b) {
        b.writeUtf(id);
        b.writeVarInt(duur);
        b.writeVarInt(punten.size());
        for (Punt p : punten) {
            b.writeVarInt(p.t());
            vec(b, p.pos());
            vec(b, p.kijk());
            b.writeVarInt(p.volgEntity() + 1);
        }
        b.writeUtf(titelKey);
        b.writeUtf(ondertitelKey);
        b.writeVarInt(titelOp);
        b.writeVarInt(titelDuur);
        b.writeInt(kleur);
    }

    public static RijkScene lees(FriendlyByteBuf b) {
        String id = b.readUtf();
        int duur = b.readVarInt();
        int n = b.readVarInt();
        List<Punt> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            l.add(new Punt(b.readVarInt(), vec(b), vec(b), b.readVarInt() - 1));
        }
        return new RijkScene(id, duur, l, b.readUtf(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readInt());
    }

    private static void vec(FriendlyByteBuf b, Vec3 v) {
        b.writeDouble(v.x);
        b.writeDouble(v.y);
        b.writeDouble(v.z);
    }

    private static Vec3 vec(FriendlyByteBuf b) {
        return new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
    }
}
