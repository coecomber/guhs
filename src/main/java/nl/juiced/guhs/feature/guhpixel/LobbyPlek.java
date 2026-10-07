package nl.juiced.guhs.feature.guhpixel;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The fixed anchor points of the lobby (world coordinates, feet position; CONTRACT_PX 2.1). Each has a 5 x 5 pad (anchor
 * +-2 in x and z) with a solid floor at y 99 and at least 4 air blocks above; the lobby template keeps the pads free and
 * does NOT contain the anchors' NPCs ({@link LobbyNpcs} places them). yaw: 0 = looking south (+z), 180 = north,
 * -90 = east, 90 = west. tools/features/guhpixel_lobby_bouw.py mirrors this table (its self-check reads this file).
 */
public enum LobbyPlek {
    SPAWN(0, 100, 0, 180),
    WELKOM(3, 100, -5, 0),
    SPEL_SKYBLOK(-24, 100, -16, 0),
    SPEL_BEDWARS(-16, 100, -21, 0),
    SPEL_VADSNITE(-8, 100, -24, 0),
    SPEL_AMONG(0, 100, -26, 0),
    SPEL_GUHMON(8, 100, -24, 0),
    SPEL_BZG(16, 100, -21, 0),
    SPEL_RESERVE(24, 100, -16, 0),
    BORD_AMONG(4, 100, -29, 0),
    WINKEL(-28, 100, 6, -90),
    BORD_STATS(-10, 100, 14, 180),
    BORD_ONLINE(10, 100, 14, 180),
    PARKOUR_START(28, 100, 6, -90),
    UITGANG(0, 100, 28, 0);

    private final int x, y, z;
    private final float yaw;

    LobbyPlek(int x, int y, int z, float yaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
    }

    public BlockPos blok() {
        return new BlockPos(x, y, z);
    }

    /** The feet position (the middle of the block). */
    public Vec3 pos() {
        return new Vec3(x + 0.5, y, z + 0.5);
    }

    public float yaw() {
        return yaw;
    }

    /** 3 blocks in front of the anchor: where a player is put after a game. */
    public Vec3 voor() {
        float rad = yaw * Mth.DEG_TO_RAD;
        return pos().add(-Mth.sin(rad) * 3, 0, Mth.cos(rad) * 3);
    }

    /** Looking at the anchor from {@link #voor}. */
    public float voorYaw() {
        return Mth.wrapDegrees(yaw + 180f);
    }
}
