package nl.juiced.guhs.feature.slaapzorg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * 1.8.0 Guhdromenland, part B "slaapzorg" (ontwerp-guhs180-dromen.md round 5 + 9; dromen-ontwerp/BOUWPLAN_180.md): light,
 * never-required care around a guh's sleep, and cosy things for at home.
 * <ul>
 *   <li>{@link Moeheid}: guhs get sleepy in the evening (a yawn, nodding off, a lone zzz);</li>
 *   <li>{@link SlaapplekGoal}: at night they look for a sleeping spot of the tag {@code #guhs:guh_slaapplekken} themselves
 *       and sleep in through the morning (the nest keeps its own goal; agent A adds the doos and the kattenhuisje);</li>
 *   <li>{@link Slaapliedje}: the Muziekdoosje (and the Mobiel above a bed) sings the guhs around asleep ({@link InslaapGoal});</li>
 *   <li>{@link OntbijtOpBed}: in the morning, wake a sleeping guh with knabbels: yawn, munch, hearts, "uitgerust";</li>
 *   <li>the slaapkamer-set (nachtkastje, droomvanger, mobiel, slaapmasker-kapstok), the maannachtlampjes and the krabpaal
 *       (tame guhs hop on top by day: {@link KrabpaalGoal}).</li>
 * </ul>
 * Resources and texts: tools/features/slaapzorg.py. Test commands: {@link SlaapzorgCommando} (/guhs slaapzorg ...).
 */
public final class SlaapzorgFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    /** Every slaapzorg block, in creative order. */
    public static final List<DeferredBlock<? extends Block>> ALLE = new ArrayList<>();

    private static BlockBehaviour.Properties hout(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(1.0f).sound(SoundType.WOOD).noOcclusion();
    }

    private static BlockBehaviour.Properties lamp(MapColor kleur, int licht) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(0.5f).sound(SoundType.GLASS).noOcclusion()
                .lightLevel(s -> s.getValue(SlaapBlokken.Lampje.LIT) ? licht : 0);
    }

    private static <B extends Block> DeferredBlock<B> blok(String id, java.util.function.Function<BlockBehaviour.Properties, B> maak,
                                                          Supplier<BlockBehaviour.Properties> props) {
        DeferredBlock<B> b = BLOCKS.registerBlock(id, maak, props);
        ITEMS.registerItem(id, p -> new BlockItem(b.get(), p) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                tooltip.accept(Component.translatable("block.guhs." + id + ".lore").withStyle(ChatFormatting.GRAY));
            }
        }, () -> new Item.Properties().useBlockDescriptionPrefix());
        ALLE.add(b);
        return b;
    }

    public static final DeferredBlock<SlaapBlokken.Speeldoos> MUZIEKDOOSJE = blok("muziekdoosje",
            p -> new SlaapBlokken.Speeldoos(p, Slapen.LIEDJE_BEREIK, 0.9f, false, new double[] {4, 0, 5, 12, 6, 11}, new double[] {12, 2, 7, 14, 4, 9}),
            () -> hout(MapColor.COLOR_PINK));
    public static final DeferredBlock<SlaapBlokken.Speeldoos> MOBIEL = blok("mobiel",
            p -> new SlaapBlokken.Speeldoos(p, Slapen.MOBIEL_BEREIK, 0.6f, true, new double[] {4, 4, 4, 12, 16, 12}),
            () -> hout(MapColor.COLOR_LIGHT_BLUE).noCollision());
    public static final DeferredBlock<SlaapBlokken.Lampje> NACHTKASTJE = blok("nachtkastje",
            p -> new SlaapBlokken.Lampje(p, new double[] {1, 0, 1, 15, 13, 15}, new double[] {5, 13, 5, 11, 16, 11}),
            () -> hout(MapColor.COLOR_PINK).lightLevel(s -> s.getValue(SlaapBlokken.Lampje.LIT) ? 10 : 0));
    public static final DeferredBlock<SlaapBlokken.Wand> DROOMVANGER = blok("droomvanger",
            p -> new SlaapBlokken.Wand(p, new double[] {2, 1, 15, 14, 15, 16}),
            () -> hout(MapColor.COLOR_PINK).noCollision().strength(0.3f).sound(SoundType.WOOL));
    public static final DeferredBlock<SlaapBlokken.Deco> SLAAPMASKER_KAPSTOK = blok("slaapmasker_kapstok",
            p -> new SlaapBlokken.Deco(p, new double[] {4, 0, 4, 12, 1, 12}, new double[] {7, 1, 7, 9, 15, 9}, new double[] {3, 12, 7, 13, 13, 9}),
            () -> hout(MapColor.COLOR_PINK));
    public static final DeferredBlock<SlaapBlokken.Lampje> MAANLAMPJE_MAAN = blok("maanlampje_maan",
            p -> new SlaapBlokken.Lampje(p, new double[] {4, 0, 5, 12, 9, 11}), () -> lamp(MapColor.COLOR_YELLOW, 12));
    public static final DeferredBlock<SlaapBlokken.Lampje> MAANLAMPJE_STER = blok("maanlampje_ster",
            p -> new SlaapBlokken.Lampje(p, new double[] {4, 0, 5, 12, 9, 11}), () -> lamp(MapColor.COLOR_YELLOW, 12));
    public static final DeferredBlock<SlaapBlokken.Lampje> MAANLAMPJE_WOLKJE = blok("maanlampje_wolkje",
            p -> new SlaapBlokken.Lampje(p, new double[] {3, 0, 5, 13, 7, 11}), () -> lamp(MapColor.SNOW, 12));
    public static final DeferredBlock<SlaapBlokken.Lampje> MAANLAMPJE_GUH = blok("maanlampje_guh",
            p -> new SlaapBlokken.Lampje(p, new double[] {4, 0, 5, 12, 8, 11}), () -> lamp(MapColor.COLOR_PINK, 12));
    public static final DeferredBlock<SlaapBlokken.Deco> KRABPAAL = blok("krabpaal",
            p -> new SlaapBlokken.Deco(p, new double[] {2, 0, 2, 14, 2, 14}, new double[] {6, 2, 6, 10, 13, 10}, new double[] {2, 13, 2, 14, 16, 14}),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<SlaapBlokken.Deco> KRABPAAL_BLAUW = blok("krabpaal_blauw",
            p -> new SlaapBlokken.Deco(p, new double[] {2, 0, 2, 14, 2, 14}, new double[] {6, 2, 6, 10, 13, 10}, new double[] {2, 13, 2, 14, 16, 14}),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.8f).sound(SoundType.WOOL).noOcclusion());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobielBlockEntity>> MOBIEL_BE = BLOCK_ENTITIES.register(
            "mobiel", () -> new BlockEntityType<>(MobielBlockEntity::new, MOBIEL.get()));

    /** Is this a krabpaal (either colour)? */
    public static boolean isKrabpaal(net.minecraft.world.level.block.state.BlockState s) {
        return s.is(KRABPAAL.get()) || s.is(KRABPAAL_BLAUW.get());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        GuhHooks.doelen((guh, goals) -> {
            goals.addGoal(2, new InslaapGoal(guh));
            goals.addGoal(4, new SlaapplekGoal(guh));
            goals.addGoal(4, new KrabpaalGoal(guh));
        });
        GuhHooks.tick(Moeheid::tick);
        GuhHooks.klik(OntbijtOpBed::klik);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> SlaapzorgCommando.register(event));
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<? extends Block> b : ALLE) {
            output.accept(new ItemStack(b.get()));
        }
    }

    private SlaapzorgFeature() {
    }
}
