package nl.juiced.guhs.feature.einde2.mikas;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The "honk" of a creature a building placed (NBT {@code Honk}: blocks, build contract 2.7): it stays within that many
 * blocks of where it was put down. The spot is remembered on its first tick. 0 = free to go where it likes.
 */
final class Honk {
    private int straal;
    @Nullable
    private BlockPos thuis;

    int straal() {
        return straal;
    }

    void zet(int straal) {
        this.straal = Math.max(0, straal);
        this.thuis = null;
    }

    void lees(ValueInput input) {
        straal = input.getIntOr("Honk", 0);
        thuis = input.read("HonkThuis", BlockPos.CODEC).orElse(null);
    }

    void bewaar(ValueOutput output) {
        if (straal > 0) {
            output.putInt("Honk", straal);
            output.storeNullable("HonkThuis", BlockPos.CODEC, thuis);
        }
    }

    /** Every server tick: keeps the mob's home in place (the first time: where it stands now). */
    void tick(Mob mob) {
        if (straal <= 0) {
            return;
        }
        if (thuis == null) {
            thuis = mob.blockPosition();
        }
        if (!mob.hasHome() || !mob.getHomePosition().equals(thuis)) {
            mob.setHomeTo(thuis, straal);
        }
    }
}
