package nl.juiced.guhs.feature.einde2.deco;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.balto.BaltoBlocks;

/**
 * einde2, slice deco: the small decoration buildings of the outer islands of the Guheinde. Twenty little places, each
 * bound to the biome it belongs to and each a wink at the Guhvatar's world (an abandoned luchtbizon-stal, the pond of the
 * two fish, a wrecked koolkraam, a tea stall whose owner went looking for his nephew...) or something the Mika's left
 * behind. They are scenery: a sign with a joke, now and then a modest chest, no residents with errands and nothing a
 * quest needs. They are rare, give way to every other building (BouwRuimte) and are not protected: take what you like.
 * <p>
 * The buildings themselves are data: tools/features/einde2_deco.py (the table, the worldgen, the texts) and
 * einde2_deco_bouw.py (the templates {@code einde2_deco/<id>}), each a structure of the type {@link E2DecoStructure} with its
 * own set. This class has that type and the four little blocks the buildings are dressed with ({@link E2DecoBlokken}).
 */
public final class E2DecoFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);

    /** The structure type of every decoration building: a one-piece jigsaw that looks at the ground under its piece. */
    public static final DeferredHolder<StructureType<?>, StructureType<E2DecoStructure>> BOUWSEL =
            STRUCTURE_TYPES.register("einde2_deco_bouwsel", () -> () -> E2DecoStructure.CODEC);

    /** Every decoration building (structure id = set id = template einde2_deco/&lt;id&gt;), in the order of their salts. */
    public static final List<String> BOUWSELS = List.of(
            "deco_luchtbizonstal", "deco_meditatiepaal", "deco_luchtbalveld_ruine",
            "deco_geestenoase_vijver", "deco_ijsvissershutje", "deco_waterstam_kano",
            "deco_das_molguh_grot", "deco_aardrommel_ring", "deco_koolkraam_wrak",
            "deco_sintelstrand_huisje", "deco_zonnekrijger_altaar", "deco_theekraampje",
            "deco_mika_wachtpost", "deco_vetschipwrak", "deco_karavaanresten",
            "deco_bevroren_kaasbol", "deco_geheime_tunnel", "deco_boemerangboom", "deco_pai_sho_tafeltje", "deco_appa_afdruk");
    /** The salt of the first set; the others lie {@link #SALT_STAP} apart (close salts put sets with the same grid side by side). */
    public static final int EERSTE_SALT = 21400801;
    public static final int SALT_STAP = 29767;

    public static final DeferredBlock<E2DecoBlokken.Kool> KOOL = BLOCKS.registerBlock("einde2_deco_kool", E2DecoBlokken.Kool::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.4f).sound(SoundType.MOSS).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<E2DecoBlokken.Windgong> WINDGONG = BLOCKS.registerBlock("einde2_deco_windgong", E2DecoBlokken.Windgong::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.CHAIN).noOcclusion().noCollision()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<E2DecoBlokken.EeuwigeVlam> EEUWIGE_VLAM = BLOCKS.registerBlock("einde2_deco_eeuwige_vlam", E2DecoBlokken.EeuwigeVlam::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instabreak().sound(SoundType.WOOL).noOcclusion().noCollision()
                    .lightLevel(s -> 15).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<E2DecoBlokken.Lotustegel> LOTUSTEGEL = BLOCKS.registerBlock("einde2_deco_lotustegel", E2DecoBlokken.Lotustegel::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.3f).sound(SoundType.DECORATED_POT).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    /** Every block (each has an item with a grey lore line), in creative-tab order. */
    public static final List<DeferredBlock<? extends Block>> BLOCK_ITEMS = List.of(KOOL, WINDGONG, EEUWIGE_VLAM, LOTUSTEGEL);

    public static final DeferredItem<BlockItem> KOOL_ITEM = blokItem(KOOL);
    public static final DeferredItem<BlockItem> WINDGONG_ITEM = blokItem(WINDGONG);
    public static final DeferredItem<BlockItem> EEUWIGE_VLAM_ITEM = blokItem(EEUWIGE_VLAM);
    public static final DeferredItem<BlockItem> LOTUSTEGEL_ITEM = blokItem(LOTUSTEGEL);

    private static DeferredItem<BlockItem> blokItem(DeferredBlock<? extends Block> blok) {
        return ITEMS.registerItem(blok.getId().getPath(), p -> new BaltoBlocks.LoreBlock(blok.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener(E2DecoCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<? extends Block> blok : BLOCK_ITEMS) {
            output.accept(new ItemStack(blok.get()));
        }
    }

    private E2DecoFeature() {
    }
}
