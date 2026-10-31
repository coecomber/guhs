package nl.juiced.guhs.feature.kermisritten2;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * De Botsguhs: the anchor lies in the middle of the floor. It keeps {@link #AUTOS} cars on the floor while a player is
 * near (they are never saved), works out who bumps into whom, and sends the guhs in the empty cars driving as soon as a
 * player is at the wheel, so one player alone still has somebody to bump.
 */
public class BotsbaanBlockEntity extends AnkerBlockEntity {
    public static final int AUTOS = 7, HOOGTE = 5;
    /** The inside of the floor from the anchor, in blocks (design: entrance north): half the width, towards and away from the entrance. */
    public static final int ZIJ = 11, VOOR = 7, ACHTER = 8;
    static final double NABIJ = 40;
    /** Ticks without a player near before the cars are put away. */
    static final int LEEG_WEG = 600;

    private int leeg;

    public BotsbaanBlockEntity(BlockPos pos, BlockState state) {
        super(Ritten2Feature.BOTSBAAN_BE.get(), pos, state);
    }

    @Override
    protected Direction ontwerp() {
        return Direction.NORTH;
    }

    /** The floor the cars drive on (world box; minY = the floor's top). */
    public AABB binnen() {
        Rotation r = draai();
        Vec3 a = Anker.punt(getBlockPos(), r, -ZIJ - 0.5, 1, -VOOR - 0.5);
        Vec3 b = Anker.punt(getBlockPos(), r, ZIJ + 0.5, 1 + HOOGTE, ACHTER + 0.5);
        return new AABB(a, b);
    }

    /** Where you stand after a ride: just inside the entrance. */
    public Vec3 uitgang() {
        return Anker.punt(getBlockPos(), draai(), 0, 1, -VOOR);
    }

    @Override
    protected void rit(ServerLevel level) {
        AABB baan = binnen();
        List<BotsguhEntity> autos = level.getEntitiesOfClass(BotsguhEntity.class, baan.inflate(2));
        if (level.getGameTime() % 20 == 0) {
            Vec3 midden = baan.getCenter();
            boolean nabij = level.hasNearbyAlivePlayer(midden.x, midden.y, midden.z, NABIJ);
            leeg = nabij ? 0 : leeg + 20;
            if (nabij) {
                veegEenmaal(level);
                vul(level, baan, autos);
            } else if (leeg >= LEEG_WEG) {
                autos.stream().filter(a -> !a.isVehicle()).forEach(BotsguhEntity::discard);
            }
        }
        boolean actief = autos.stream().anyMatch(BotsguhEntity::heeftRijder);
        for (int i = 0; i < autos.size(); i++) {
            BotsguhEntity a = autos.get(i);
            a.zetActief(actief, autos);
            for (int j = i + 1; j < autos.size(); j++) {
                BotsguhEntity.bots(a, autos.get(j));
            }
        }
    }

    private void vul(ServerLevel level, AABB baan, List<BotsguhEntity> autos) {
        for (int k = autos.size(); k < AUTOS; k++) {
            BotsguhEntity auto = Ritten2Feature.BOTSGUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (auto == null) {
                return;
            }
            // parked in a loose ring round the middle, each in its own colour
            double hoek = k * 2 * Math.PI / AUTOS;
            double x = baan.getCenter().x + Math.cos(hoek) * baan.getXsize() * 0.33;
            double z = baan.getCenter().z + Math.sin(hoek) * baan.getZsize() * 0.33;
            auto.setPos(x, baan.minY, z);
            auto.setYRot(level.getRandom().nextFloat() * 360f);
            auto.zetBaan(getBlockPos(), baan, uitgang(), k);
            level.addFreshEntity(auto);
        }
    }

    /** The cars that stand still in the design are real cars here: the floor is cleared of props, once (the walls stay). */
    @Override
    protected void veeg(ServerLevel level) {
        AABB b = binnen();
        BlockPos van = BlockPos.containing(b.minX + 0.5, b.minY, b.minZ + 0.5);
        BlockPos tot = BlockPos.containing(b.maxX - 0.5, b.minY + HOOGTE - 1, b.maxZ - 0.5);
        for (BlockPos p : BlockPos.betweenClosed(van, tot)) {
            if (!level.getBlockState(p).isAir()) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}
