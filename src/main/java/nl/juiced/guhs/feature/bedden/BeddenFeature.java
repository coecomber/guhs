package nl.juiced.guhs.feature.bedden;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.dromen.SlaapplekBlock;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * 1.8.0 beds (ontwerp-guhs180-dromen.md "Playtest testbuild 2"; the renders in dromen-ontwerp/bedden):
 * <ul>
 *   <li>four player beds ({@link GuhsBedBlock}): Droombed, Guh-hoofdbed, Blub-bed, Wolkenhemelbed;</li>
 *   <li>critter beds: the Guh-en-Blub-mandje (a guh by the sleeping-spot tag, a Blub cuddles up in its vakje), the Stapelbedje
 *       ({@link StapelbedBlock}: two guhs), the Luciferdoosje (pieppiepmuisjes) and the Schelpbedje (Schilly, Poepschilly);</li>
 *   <li>the family beds ({@link FamilieBedBlock}, shapes and vakjes in {@link FamilieVormen}): several blocks, a vakje for
 *       each critter; and the Knuffelnest, the big bed for eight guhs.</li>
 * </ul>
 * Tame critters find their vakje at night by themselves ({@link BedSlaapGoal}). Resources and texts: tools/features/bedden.py.
 */
public final class BeddenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** Every bed block, in creative order. */
    public static final List<DeferredBlock<? extends Block>> ALLE = new ArrayList<>();
    public static final Map<String, DeferredBlock<FamilieBedBlock>> FAMILIE = new LinkedHashMap<>();

    private static BlockBehaviour.Properties zacht(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(0.4f).sound(SoundType.WOOL).noOcclusion();
    }

    private static BlockBehaviour.Properties hout(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(0.8f).sound(SoundType.WOOD).noOcclusion();
    }

    private static Consumer<Component> lore(String id, Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("block.guhs." + id + ".lore").withStyle(ChatFormatting.GRAY));
        return tooltip;
    }

    private static <B extends Block> DeferredBlock<B> blok(String id, Function<BlockBehaviour.Properties, B> maak,
                                                          Supplier<BlockBehaviour.Properties> props, String soortItem) {
        DeferredBlock<B> b = BLOCKS.registerBlock(id, maak, props);
        switch (soortItem) {
            case "bed" -> ITEMS.registerItem(id, p -> new BedItem(b.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    lore(id, tooltip);
                }
            }, () -> new Item.Properties().stacksTo(1).useBlockDescriptionPrefix());
            case "hoog" -> ITEMS.registerItem(id, p -> new DoubleHighBlockItem(b.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    lore(id, tooltip);
                }
            }, () -> new Item.Properties().stacksTo(16).useBlockDescriptionPrefix());
            default -> ITEMS.registerItem(id, p -> new BlockItem(b.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    lore(id, tooltip);
                }
            }, () -> new Item.Properties().stacksTo(soortItem.equals("familie") ? 1 : 64).useBlockDescriptionPrefix());
        }
        ALLE.add(b);
        return b;
    }

    // player beds
    public static final DeferredBlock<GuhsBedBlock> DROOMBED = blok("droombed", p -> new GuhsBedBlock(DyeColor.PURPLE, p), () -> hout(MapColor.COLOR_PURPLE), "bed");
    public static final DeferredBlock<GuhsBedBlock> GUH_HOOFDBED = blok("guh_hoofdbed", p -> new GuhsBedBlock(DyeColor.PINK, p), () -> hout(MapColor.COLOR_PINK), "bed");
    public static final DeferredBlock<GuhsBedBlock> BLUB_BED = blok("blub_bed", p -> new GuhsBedBlock(DyeColor.LIGHT_BLUE, p), () -> zacht(MapColor.COLOR_LIGHT_BLUE), "bed");
    public static final DeferredBlock<GuhsBedBlock> WOLKENHEMELBED = blok("wolkenhemelbed", p -> new GuhsBedBlock(DyeColor.WHITE, p), () -> zacht(MapColor.SNOW), "bed");
    // critter beds
    public static final DeferredBlock<SlaapplekBlock> MANDJE = blok("guh_blub_mandje",
            p -> new SlaapplekBlock(p, DierBedBlock.box(0, 0, 0, 16, 6, 16)) {
                @Override
                protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState s,
                        net.minecraft.world.level.BlockGetter l, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext c) {
                    return Block.box(0, 0, 0, 16, 2, 16);   // (they walk in; only the cushion is solid)
                }
            }, () -> zacht(MapColor.COLOR_BROWN), "blok");
    public static final DeferredBlock<StapelbedBlock> STAPELBEDJE = blok("stapelbedje", StapelbedBlock::new, () -> hout(MapColor.WOOD), "hoog");
    public static final DeferredBlock<DierBedBlock> LUCIFERDOOSJE = blok("luciferdoosje",
            p -> new DierBedBlock(p, DierBedBlock.box(3, 0, 1, 13, 4, 15)), () -> hout(MapColor.COLOR_RED), "blok");
    public static final DeferredBlock<DierBedBlock> SCHELPBEDJE = blok("schelpbedje",
            p -> new DierBedBlock(p, DierBedBlock.box(2, 0, 3, 14, 2, 15, 2, 0, 1.5, 14, 13, 3)), () -> zacht(MapColor.COLOR_PINK).sound(SoundType.CORAL_BLOCK), "blok");

    static {
        for (String id : FamilieVormen.VORMEN.keySet()) {
            FAMILIE.put(id, blok(id, p -> new FamilieBedBlock(p, id), () -> zacht(MapColor.COLOR_PINK), "familie"));
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        GuhHooks.doelen((guh, goals) -> goals.addGoal(4, new BedSlaapGoal(guh, Vakjes.Soort.GUH)));
        NeoForge.EVENT_BUS.addListener(BeddenFeature::opJoin);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(BeddenFeature::vakjes));
    }

    /** The vakjes of every critter and family bed (the critter beds' are small: drawn in the same frame as the models). */
    static void vakjes() {
        Vakjes.zet(MANDJE.get(), new Vakjes.Def(1, 1, 1, List.of(new Vakjes.Vakje(0.72, 0.125, 0.4, Vakjes.Soort.BLUB))));
        Vakjes.zet(LUCIFERDOOSJE.get(), new Vakjes.Def(1, 1, 1, List.of(new Vakjes.Vakje(0.5, 0.19, 0.6, Vakjes.Soort.MUIS))));
        Vakjes.zet(SCHELPBEDJE.get(), new Vakjes.Def(1, 1, 1, List.of(new Vakjes.Vakje(0.36, 0.13, 0.62, Vakjes.Soort.SCHILLY),
                new Vakjes.Vakje(0.66, 0.13, 0.66, Vakjes.Soort.SCHILLY))));
        for (var e : FAMILIE.entrySet()) {
            FamilieVormen.Vorm v = FamilieVormen.VORMEN.get(e.getKey());
            Vakjes.zet(e.getValue().get(), new Vakjes.Def(v.sx(), v.sy(), v.sz(), v.vakjes()));
        }
    }

    /** Blubs, pieppiepmuisjes and the Schilly's learn to find their vakje (once per entity). */
    private static void opJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof TamableAnimal mob) || mob instanceof nl.juiced.guhs.entity.GuhEntity) {
            return;
        }
        Vakjes.Soort soort = Vakjes.soort(mob);
        if (soort == null || mob.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.getGoal() instanceof BedSlaapGoal)) {
            return;
        }
        mob.goalSelector.addGoal(soort == Vakjes.Soort.BLUB ? 1 : 3, new BedSlaapGoal(mob, soort));
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<? extends Block> b : ALLE) {
            output.accept(new ItemStack(b.get()));
        }
    }

    private BeddenFeature() {
    }
}
