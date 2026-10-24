package nl.juiced.guhs.feature.guhvillagers;

import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhWorkstationBlock;

/**
 * 1.7.0 "Onder de Wol" (design: ontwerp-guhs170-mijnen.md, rounds 32-40): the guh villagers of the Vadsmijnstadje (and of
 * the guh villages above ground).
 * <ul>
 *   <li>four new professions with their workstations: the <b>Toverguh</b> (Toverguh-boekenstandaard; sells
 *       Spreukjesboekjes), the <b>Vadsmonteur</b> (Guhrad-werkbank; vadskracht machines), the <b>Mijnwerker</b>
 *       (Ertsenkist; layer stones, lamps, buys ores) and the <b>Kristalslijper</b> (Kristalslijpsteen; guhkristal and
 *       vahoegiet into crystal blocks). Trades: {@link GuhVillagerHandel}.</li>
 *   <li>the new look of every guh villager (render only, client/GuhVillagerRenderer): model A "Guhkop-villager" for
 *       grown-ups, model C "Knuffel-villager" for children.</li>
 *   <li>picking up any villager (sneak + right-click with an empty hand): {@link VillagerOppakken}.</li>
 *   <li>the <b>IJzerguhlem</b> ({@link IJzerguhlem}): the iron golem of the guh villagers, summoned by them with exactly
 *       the vanilla mechanics (mixin/GuhGolemMixin); vanilla villagers keep the vanilla iron golem.</li>
 * </ul>
 */
public final class GuhVillagersFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    private static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Guhs.MODID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, Guhs.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    // --- the workstations ---------------------------------------------------------------------------------------------
    public static final DeferredBlock<GuhWorkstationBlock> BOEKENSTANDAARD = werkplek("toverguh_boekenstandaard", MapColor.COLOR_PURPLE, SoundType.WOOD);
    public static final DeferredBlock<GuhWorkstationBlock> GUHRAD_WERKBANK = werkplek("guhrad_werkbank", MapColor.COLOR_CYAN, SoundType.WOOD);
    public static final DeferredBlock<GuhWorkstationBlock> ERTSENKIST = werkplek("ertsenkist", MapColor.COLOR_BROWN, SoundType.WOOD);
    public static final DeferredBlock<GuhWorkstationBlock> KRISTALSLIJPSTEEN = werkplek("kristalslijpsteen", MapColor.COLOR_PINK, SoundType.STONE);
    public static final List<DeferredBlock<GuhWorkstationBlock>> WERKPLEKKEN = List.of(BOEKENSTANDAARD, GUHRAD_WERKBANK, ERTSENKIST, KRISTALSLIJPSTEEN);
    static {
        for (DeferredBlock<GuhWorkstationBlock> b : WERKPLEKKEN) {
            ITEMS.registerSimpleBlockItem(b);
        }
    }

    public static final DeferredHolder<PoiType, PoiType> BOEKENSTANDAARD_POI = poi("toverguh_boekenstandaard", BOEKENSTANDAARD);
    public static final DeferredHolder<PoiType, PoiType> GUHRAD_WERKBANK_POI = poi("guhrad_werkbank", GUHRAD_WERKBANK);
    public static final DeferredHolder<PoiType, PoiType> ERTSENKIST_POI = poi("ertsenkist", ERTSENKIST);
    public static final DeferredHolder<PoiType, PoiType> KRISTALSLIJPSTEEN_POI = poi("kristalslijpsteen", KRISTALSLIJPSTEEN);

    // --- the professions (translation key entity.minecraft.villager.guhs.<id>, like ModVillagers) -----------------------
    public static final DeferredHolder<VillagerProfession, VillagerProfession> TOVERGUH = beroep("toverguh", BOEKENSTANDAARD_POI, SoundEvents.VILLAGER_WORK_LIBRARIAN);
    public static final DeferredHolder<VillagerProfession, VillagerProfession> VADSMONTEUR = beroep("vadsmonteur", GUHRAD_WERKBANK_POI, SoundEvents.VILLAGER_WORK_TOOLSMITH);
    public static final DeferredHolder<VillagerProfession, VillagerProfession> MIJNWERKER = beroep("mijnwerker", ERTSENKIST_POI, SoundEvents.VILLAGER_WORK_MASON);
    public static final DeferredHolder<VillagerProfession, VillagerProfession> KRISTALSLIJPER = beroep("kristalslijper", KRISTALSLIJPSTEEN_POI, SoundEvents.VILLAGER_WORK_CARTOGRAPHER);

    // --- a picked-up villager ----------------------------------------------------------------------------------------------
    public static final DeferredItem<OpgepakteVillagerItem> OPGEPAKTE_VILLAGER = ITEMS.registerItem("opgepakte_villager",
            OpgepakteVillagerItem::new, () -> new Item.Properties().stacksTo(1).fireResistant());

    // --- the IJzerguhlem -----------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<IJzerguhlem>> IJZERGUHLEM = ENTITY_TYPES.register("ijzerguhlem",
            () -> EntityType.Builder.of(IJzerguhlem::new, MobCategory.MISC).sized(1.4F, 2.7F).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("ijzerguhlem"))));

    private static DeferredBlock<GuhWorkstationBlock> werkplek(String id, MapColor kleur, SoundType geluid) {
        return BLOCKS.registerBlock(id, GuhWorkstationBlock::new,
                () -> BlockBehaviour.Properties.of().mapColor(kleur).strength(2.5f).sound(geluid));
    }

    private static DeferredHolder<PoiType, PoiType> poi(String id, DeferredBlock<GuhWorkstationBlock> blok) {
        return POI_TYPES.register(id, () -> new PoiType(ImmutableSet.copyOf(blok.get().getStateDefinition().getPossibleStates()), 1, 1));
    }

    private static DeferredHolder<VillagerProfession, VillagerProfession> beroep(String id, DeferredHolder<PoiType, PoiType> poi, SoundEvent geluid) {
        ResourceKey<PoiType> key = poi.getKey();
        return PROFESSIONS.register(id, () -> new VillagerProfession(Component.translatable("entity.minecraft.villager.guhs." + id),
                holder -> holder.is(key), holder -> holder.is(key), ImmutableSet.of(), ImmutableSet.of(), geluid, new Int2ObjectOpenHashMap<>()));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        POI_TYPES.register(modBus);
        PROFESSIONS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(IJZERGUHLEM.get(), IronGolem.createAttributes().build()));
        // like the vanilla iron golem (SpawnPlacements), so SpawnUtil places a summoned IJzerguhlem by the same rules
        modBus.addListener((RegisterSpawnPlacementsEvent e) -> e.register(IJZERGUHLEM.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        NeoForge.EVENT_BUS.addListener(VillagerOppakken::onInteract);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<GuhWorkstationBlock> b : WERKPLEKKEN) {
            output.accept(new ItemStack(b.get()));
        }
    }

    private GuhVillagersFeature() {
    }
}
