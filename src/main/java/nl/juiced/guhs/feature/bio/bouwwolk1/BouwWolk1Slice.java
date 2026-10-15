package nl.juiced.guhs.feature.bio.bouwwolk1;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.BioZelftest;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.guhpixel.blok.MuurDecoBlock;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * biomes3 slice "bouw-wolk1": three findable places high above the Wolkenweide, each on an island of its own
 * (structures of type guhs:bio_plek, kind "lucht"; templates and data: tools/features/bio_bouw_wolk1.py).
 * <ul>
 *   <li><b>wolkenhoeder_hut</b> (Superkompas tab knus): the rare big island with its lake and waterfall, the hut, the fold
 *       with its herd ({@link Kudde}) and the wolkenhoeder, who teaches making cloud blocks and gives every player one
 *       wolkenschaapje to take home ({@link Hoeder});</li>
 *   <li><b>sterrenwacht_ruine</b> (tab wonderen): the half-fallen observatory; the sterrenkijkerguh who is only awake
 *       in the dark, sterrenstof from the telescope once a night ({@link Sterrenkijker}), more falling stars in the sky
 *       at night (client);</li>
 *   <li><b>luchtballon_haven</b> (tab knus): the jetty with the balloons; one ride per day per player down to the meadow
 *       ({@link Ballonvaarder}, {@link HavenBallonEntity}).</li>
 * </ul>
 * Dev commands: {@code /guhs bio bouw-wolk1 ...} ({@link BouwWolk1Commando}); self test {@code /guhs bio zelftest bouw_wolk1}.
 */
public final class BouwWolk1Slice {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Guhs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final String[] STRUCTUREN = {"wolkenhoeder_hut", "sterrenwacht_ruine", "luchtballon_haven"};

    /** The haven's balloon (the festival balloon's size and tracking). */
    public static final DeferredHolder<EntityType<?>, EntityType<HavenBallonEntity>> HAVEN_BALLON = ENTITY_TYPES.register("luchtballon_haven_ballon",
            () -> EntityType.Builder.<HavenBallonEntity>of(HavenBallonEntity::new, MobCategory.MISC).sized(1.8f, 1.2f)
                    .clientTrackingRange(16).updateInterval(3).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("luchtballon_haven_ballon"))));

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<WolkvoetProcessor>> WOLKVOET =
            PROCESSORS.register("wolkenhoeder_hut_wolkvoet", () -> () -> WolkvoetProcessor.CODEC);

    /** The star chart on the observatory's wall (a wall decoration; made with sterrenstof and paper). */
    public static final DeferredBlock<MuurDecoBlock> STERRENKAART = BLOCKS.registerBlock("sterrenwacht_ruine_sterrenkaart",
            p -> new MuurDecoBlock(p, Block.box(1, 1, 15, 15, 15, 16)), () -> MuurDecoBlock.props());
    public static final DeferredItem<LoreBlockItem> STERRENKAART_ITEM = ITEMS.registerItem("sterrenwacht_ruine_sterrenkaart",
            p -> new LoreBlockItem(STERRENKAART.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        PROCESSORS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        // the wolkenhoeder keeps his role at the hemelkapelletje; at the hut (RoleData guhs_plek) he is the cloud teacher
        NpcRollen.zet(GuhNpcEntity.Kind.WOLKENHOEDER, Hoeder.PLEK, new Hoeder());
        NpcRollen.zet(GuhNpcEntity.Kind.STERRENWACHT_RUINE_STERRENKIJKER, new Sterrenkijker());
        NpcRollen.zet(GuhNpcEntity.Kind.BALLONVAARDERGUH, new Ballonvaarder());
        SuperkompasItem.voegToe("knus", "wolkenhoeder_hut");
        SuperkompasItem.voegToe("wonderen", "sterrenwacht_ruine");
        SuperkompasItem.voegToe("knus", "luchtballon_haven");
        NeoForge.EVENT_BUS.register(BouwWolk1Events.class);
        BioZelftest.registreer("bouw_wolk1", BouwWolk1Commando::zelftest);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(STERRENKAART_ITEM.get()));
    }

    private BouwWolk1Slice() {
    }
}
