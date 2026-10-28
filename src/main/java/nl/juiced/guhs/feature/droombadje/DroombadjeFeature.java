package nl.juiced.guhs.feature.droombadje;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.0 Guhdromenland, island E "badje": het Wolkenbadje (dromen-ontwerp/BOUWPLAN_180.md part E; ontwerp ronde 10: "bad
 * klaarmaken"). The island itself is part A's template stamp ({@code data/guhs/structure/dromen/eiland_badje.nbt} +
 * {@code data/guhs/dromen/eiland/badje.json}); this feature is what happens on it:
 * <ul>
 *   <li>{@link Badje}: step on the bubble pad and a soap bubble floats you along the clouds ({@link BadjeOpzet}: the route
 *       of data/guhs/dromen/badje_route.json); at each of the three clouds it waits until you click the
 *       {@link BadspulletjeBlock} (badeend, zeepje, schuim). With all three, Snoes dares his first bubble bath
 *       ({@link BadjeScene}), and the piece "bad" of his dream ({@code DroomVoortgang.Stukje.BAD}) is yours.</li>
 * </ul>
 * Floating for fun works always, also without the story. Resources: tools/features/droombadje.py.
 */
public final class DroombadjeFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);

    /** The three bath things on the clouds (only the island has them: no item). */
    public static final DeferredBlock<BadspulletjeBlock> BADSPULLETJE = BLOCKS.registerBlock("droom_badspulletje", BadspulletjeBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.YELLOW_WOOL).strength(-1f, 3600000f).sound(SoundType.WOOL).noOcclusion()
                    .lightLevel(s -> 6).noLootTable());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        Badje.register();
        BadjeScene.init();
    }

    private DroombadjeFeature() {
    }
}
