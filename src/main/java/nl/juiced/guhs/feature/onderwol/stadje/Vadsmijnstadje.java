package nl.juiced.guhs.feature.onderwol.stadje;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * 1.7.0 "Onder de Wol" (rounds 32-43): the Vadsmijnstadje, an underground town of guh villagers in a big cave hall in the
 * knuffelsteen layer, with the Guhrad-centrale on its plaza and a wolkenlift to an entrance hut on the surface. It is a
 * {@code guhs:grot_bouw} made entirely by {@code tools/features/onderwol_stadje.py} (template, pools, structure, set about
 * every 1500 blocks, loot, texts); Java only adds it to the Superkompas tab "ondergrond".
 * <p>
 * The villagers in the template are real villagers of type {@code guhs:guh} with a guh profession each (the four of
 * feature/guhvillagers and five older guh jobs), Xp 1 so they keep their job, each with its workstation and bed in its own
 * house; two children, two IJzerguhlems.
 * <p>
 * 1.7.0 (ronde 45-47): the town is not protected, so its working vadskracht is early tier only: the centrale's two
 * Guhraden power every lantern (Vadslampen over Guhdraad) and the Kristalslijper's Glasslijperij (Vadsmolen + Guhoven); the
 * Ertslijn at the depot has its own Guhrad (a chest cart of raw ore over a hopper, a Guhoven, a Vadsmolen, hoppers into the
 * Mijnwerkerskist). Checked by VadsmijnstadjeGameTests with KrachtProef.
 * <p>
 * 1.7.0: the hidden {@link Bijvuller} under the plaza keeps that showcase running (molens topped up with gravel, the cart's
 * ore recycled from the bars in the Mijnwerkerskist, the outputs trimmed), without ever making ore.
 */
public final class Vadsmijnstadje {
    public static final String NAAM = "vadsmijnstadje";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    /** {@code guhs:stadje_bijvuller}: the hidden refiller in the rock under the plaza (no item, drops nothing). */
    public static final DeferredBlock<Bijvuller> BIJVULLER = BLOCKS.registerBlock("stadje_bijvuller", Bijvuller::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).sound(SoundType.STONE).noLootTable()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Bijvuller.Kern>> BIJVULLER_BE = BLOCK_ENTITIES.register(
            "stadje_bijvuller", () -> new BlockEntityType<>(Bijvuller.Kern::new, BIJVULLER.get()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SuperkompasItem.voegToe("ondergrond", NAAM);
    }

    private Vadsmijnstadje() {
    }
}
