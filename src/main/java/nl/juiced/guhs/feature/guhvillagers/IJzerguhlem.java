package nl.juiced.guhs.feature.guhvillagers;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;

/**
 * The IJzerguhlem ("Pluizige IJzerguhlem", model B of the render): the iron golem of the guh villagers. It IS a vanilla
 * iron golem (every goal, the anger, the repairs with iron ingots, villagers and zombies treat it as one): only its looks
 * (client/IJzerguhlemRenderer: lavender iron, a round guh head, a fluffy pink collar, a flower crown, a bunch of pluisgras;
 * when it offers a flower it is pluisgras) and its loot (loot_table/entities/ijzerguhlem: iron ingots and pluisgras
 * instead of poppies) differ.
 * <p>
 * Guh villagers summon it instead of an iron golem by exactly the vanilla rules (mixin/GuhGolemMixin swaps only the
 * entity type in Villager#spawnGolemIfNeeded; mixin/GuhGolemSensorMixin lets villagers see it as "a golem nearby"), so an
 * iron farm with guh villagers works the same. Players building iron blocks + a carved pumpkin still get the vanilla
 * iron golem.
 */
public class IJzerguhlem extends IronGolem {
    public IJzerguhlem(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
    }
}
