package nl.juiced.guhs.feature.sterrenwacht;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.Knusfeest;

/**
 * De Guh-Sterrenwacht (2.8, slice "buiten"): a big observatory with a guh-head dome and a huge telescope, high on the
 * Guhpieken and the Vadskliffen (the loose structure guh_sterrenwacht, tools/features/sterrenwacht.py). Professor
 * Sterretje ({@link SterrenwachtRole}) lives there.
 * <ul>
 *   <li>The guh_telescoop ({@link TelescoopBlock}): at night you look through it and connect the stars into a guh
 *       constellation ({@link Sterrenbeeld}; the screen is client.TelescoopScherm, the game {@link Sterrenkijken}).
 *       Twelve are always there, three rare ones only during a sterrenregen. Each one you find goes into the
 *       sterrenatlas (the Knus tab of the Guhdex).</li>
 *   <li>A constellation gives wenssterren ({@link WenssterItem}): make a wish (a little present), or spend them in the
 *       Professor's shop (the sterrenkijkersmuts, the sterrencape, sterrenlantaarns, your own telescope).</li>
 *   <li>The Grote Knusfeest: while the task "sterrenlantaarns" is open, a constellation gives three
 *       {@link SterrenlantaarnBlock}s for the feest (Knusfeest.gemaakt).</li>
 * </ul>
 */
public final class SterrenwachtFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** The loose structure (and its protection). */
    public static final Buiten.Bescherming BESCHERMING = new Buiten.Bescherming("guh_sterrenwacht", "gui.guhs.sterrenwacht.beschermd");

    public static final DeferredBlock<TelescoopBlock> TELESCOOP = BLOCKS.registerBlock("guh_telescoop", TelescoopBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.5f).sound(SoundType.COPPER).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<SterrenlantaarnBlock> STERRENLANTAARN = BLOCKS.registerBlock("sterrenlantaarn", SterrenlantaarnBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.8f).sound(SoundType.LANTERN).noOcclusion()
                    .lightLevel(s -> 15).pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<Buiten.LoreBlockItem> TELESCOOP_ITEM = ITEMS.registerItem("guh_telescoop",
            p -> new Buiten.LoreBlockItem(TELESCOOP.get(), p), new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Buiten.LoreBlockItem> STERRENLANTAARN_ITEM = ITEMS.registerItem("sterrenlantaarn",
            p -> new Buiten.LoreBlockItem(STERRENLANTAARN.get(), p), new Item.Properties());
    /** The reward of a constellation: make a wish, or pay the Professor with it. */
    public static final DeferredItem<WenssterItem> WENSSTER = ITEMS.registerItem("wensster", WenssterItem::new,
            new Item.Properties().rarity(Rarity.UNCOMMON));

    /** A twinkling little star (the telescope, wishes, the sterrenlantaarn). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WENSSTER_DEELTJE = PARTICLES.register("wensster",
            () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> STER_KLIK = sound("sterrenwacht.ster_klik");
    public static final DeferredHolder<SoundEvent, SoundEvent> STERRENBEELD_GELUID = sound("sterrenwacht.sterrenbeeld");

    /** The sterrenwacht only starts high on the mountains ({@link HogeJigsawStructure}, the inner jigsaw of its flat_jigsaw). */
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);
    public static final DeferredHolder<StructureType<?>, StructureType<HogeJigsawStructure>> HOGE_JIGSAW =
            STRUCTURE_TYPES.register("sterrenwacht_hoog", () -> () -> HogeJigsawStructure.CODEC);

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        BESCHERMING.register();
        Minigames.registerGame(Minigames.STERRENWACHT, Sterrenkijken::kijkt);
        SterrenwachtVoortgang.register();
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> Sterrenkijken.vergeetAlles());
        NeoForge.EVENT_BUS.addListener(Sterrenkijken::onLogout);
        // the Grote Knusfeest: sterrenlantaarns from the recipe, and lanterns handed in (e.g. bought in the Professor's
        // shop) while the task was only asked, still count as made (Sterrenkijken.lantaarnGekregen)
        NeoForge.EVENT_BUS.addListener((PlayerEvent.ItemCraftedEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player && event.getCrafting().is(STERRENLANTAARN_ITEM.get())) {
                Sterrenkijken.lantaarnGekregen(player);
            }
        });
        Knusfeest.luister((player, taak, stap) -> {
            if (taak == Feesttaak.STERRENLANTAARNS && stap == Knusfeest.Stap.GEBRACHT) {
                Sterrenkijken.lantaarnGekregen(player);
            }
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        SterrenwachtPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(TELESCOOP_ITEM.get()));
        output.accept(new ItemStack(STERRENLANTAARN_ITEM.get()));
        output.accept(new ItemStack(WENSSTER.get()));
    }

    /** Professor Sterretje (STERRENKIJKERGUH). */
    @Nullable
    public static NpcRole role() {
        return SterrenwachtRole.INSTANCE;
    }

    private SterrenwachtFeature() {
    }
}
