package nl.juiced.guhs.feature.theehuis;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * Het Knabbelthee-huisje (2.8, plein slot "theehuis" of the Knuffeldal town): a giant teapot with a guh-face lid, where
 * Mevrouw Theelepel pours tea. Resources: tools/features/theehuis.py.
 * <ul>
 *   <li>The theekransje ({@link Theekransje}): your tamed guhs sit down on the chairs around the theetafel and chat
 *       (emotes, hearts); now and then one wants tea or something sweet (a little bubble above its head). Pour them tea
 *       and serve cake (#guhs:knus/gebak from the Knabbelbakkerij counts extra: zelfgebakken!). A gezellige tafel gives
 *       everyone the effect guhs:gezellig and Knus milestones. While the Burgemeester's feesttaakje THEESERVIES is open,
 *       Mevrouw Theelepel lends you her feest_theeservies after a gezellig kransje.</li>
 *   <li>The theepotje (make tea from kaasknabbels, kaasmelk, theekruid or guhbloemetjes) and four teas (the Knus
 *       collection "theesoorten"), the theetafel, and the theemutsje (a tea cosy hat).</li>
 * </ul>
 * No coin and no shop: the tea house is for being knus, not for winning.
 */
public final class TheehuisFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Guhs.MODID);

    // --- blocks -------------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<TheeBlocks.Theetafel> THEETAFEL = BLOCKS.registerBlock("theetafel", TheeBlocks.Theetafel::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredBlock<TheeBlocks.Theepotje> THEEPOTJE = BLOCKS.registerBlock("theepotje", TheeBlocks.Theepotje::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.6f).sound(SoundType.DECORATED_POT).noOcclusion());
    public static final DeferredItem<BlockItem> THEETAFEL_ITEM = ITEMS.registerItem("theetafel", p -> new TheeBlocks.LoreBlock(THEETAFEL.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> THEEPOTJE_ITEM = ITEMS.registerItem("theepotje", p -> new TheeBlocks.LoreBlock(THEEPOTJE.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());

    // --- items --------------------------------------------------------------------------------------------------------------
    /** Mevrouw Theelepel's best tea set: lent for the Grote Knusfeest (tag guhs:knus/theeservies). */
    public static final DeferredItem<Item> FEEST_THEESERVIES = ITEMS.registerItem("feest_theeservies", TheeBlocks.Lore::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final Map<TheeBlocks.Soort, DeferredItem<TheeBlocks.Thee>> THEE = new EnumMap<>(TheeBlocks.Soort.class);

    static {
        for (TheeBlocks.Soort soort : TheeBlocks.Soort.values()) {
            THEE.put(soort, ITEMS.registerItem(soort.id(), p -> new TheeBlocks.Thee(soort, p), () -> new Item.Properties().stacksTo(16)));
        }
    }

    // --- the effect, particles, sounds ---------------------------------------------------------------------------------------
    public static final DeferredHolder<MobEffect, MobEffect> GEZELLIG = MOB_EFFECTS.register("gezellig", TheeBlocks.Gezellig::new);
    /** Steam curling up from a teapot or a cup. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> THEESTOOM = PARTICLES.register("theestoom", () -> new SimpleParticleType(false));
    /** A little pink heart: the table is gezellig. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GEZELLIG_HARTJE = PARTICLES.register("gezellig_hartje",
            () -> new SimpleParticleType(false));
    public static final DeferredHolder<SoundEvent, SoundEvent> INSCHENKEN = sound("theehuis.inschenken");
    public static final DeferredHolder<SoundEvent, SoundEvent> KOPJES_KLINK = sound("theehuis.kopjes_klink");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** Mevrouw Theelepel: talks (her screen), holds the theekransje. */
    public static final NpcRole THEELEPEL = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            Theekransje.talk(npc, player);
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            Theekransje.of(npc).tick(npc);
        }
    };

    public static Item thee(TheeBlocks.Soort soort) {
        return THEE.get(soort).get();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        MOB_EFFECTS.register(modBus);
        Minigames.registerGame(Minigames.THEEHUIS, Theekransje::isGastheer);
        GuhHooks.klik(Theekransje::klikOpGuh);
        GuhHooks.item(Theekransje::isVoorGast);
        NeoForge.EVENT_BUS.addListener(Theekransje::onLogout);
        NeoForge.EVENT_BUS.addListener(Theekransje::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(Theekransje::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Theekransje::onServerStopped);
        TheehuisVoortgang.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
        TheehuisPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(THEETAFEL_ITEM.get()));
        output.accept(new ItemStack(THEEPOTJE_ITEM.get()));
        for (TheeBlocks.Soort soort : TheeBlocks.Soort.values()) {
            output.accept(new ItemStack(thee(soort)));
        }
        output.accept(new ItemStack(FEEST_THEESERVIES.get()));
    }

    @Nullable
    public static NpcRole role() {
        return THEELEPEL;
    }

    private TheehuisFeature() {
    }
}
