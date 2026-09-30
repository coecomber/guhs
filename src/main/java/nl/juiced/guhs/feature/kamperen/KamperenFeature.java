package nl.juiced.guhs.feature.kamperen;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.sterrenwacht.Buiten;

/**
 * De kampeerplekjes (2.8, slice "buiten"): little campsites in the Knuffeldal and on the Guhweides (the loose
 * structure kampeerplekje, tools/features/kamperen.py): a campfire with log benches, guh-shaped tents and sleeping bags.
 * <ul>
 *   <li>Opa Guh ({@link OpaGuh}, also on his bench by the town's campfire) tells a story at the campfire every night:
 *       twelve different ones, about the Mika's and the kaasknabbels ({@link Verhalen}); each new one goes into your
 *       verhalenbundel (the Knus tab). Guhs nearby come and listen.</li>
 *   <li>At night the guhs around a campfire wear pyjamas ({@link KampvuurPyjama}: the flag GuhHooks.PYJAMA, drawn by
 *       client.KamperenClient).</li>
 *   <li>The {@link SlaapzakBlock}: sleep without a bed (it doesn't set your spawn point); after a whole night in it you
 *       wake up {@link UitgerustEffect uitgerust}.</li>
 *   <li>Marshmallows ({@link KampvuurMarshmallow}, tag guhs:knus/marshmallow): roast one over a burning campfire in the
 *       evening; the guhs come and sit by the fire.</li>
 * </ul>
 */
public final class KamperenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final Buiten.Bescherming BESCHERMING = new Buiten.Bescherming("kampeerplekje", "gui.guhs.kamperen.beschermd");

    public static final DeferredBlock<SlaapzakBlock> SLAAPZAK = BLOCKS.registerBlock("guh_slaapzak", SlaapzakBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.3f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.DESTROY).ignitedByLava());
    public static final DeferredItem<Buiten.LoreBlockItem> SLAAPZAK_ITEM = ITEMS.registerItem("guh_slaapzak",
            p -> new Buiten.LoreBlockItem(SLAAPZAK.get(), p), () -> new Item.Properties().stacksTo(16));

    public static final DeferredHolder<MobEffect, UitgerustEffect> UITGERUST = EFFECTS.register("uitgerust", UitgerustEffect::new);

    /** A little spark from the campfire, drifting up while Opa Guh tells his story. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KAMPVUURVONKJE = PARTICLES.register("kampvuurvonkje",
            () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> OPA_VERHAAL = SOUNDS.register("kamperen.opa_verhaal",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("kamperen.opa_verhaal")));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        EFFECTS.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        BESCHERMING.register();
        NeoForge.EVENT_BUS.register(KamperenEvents.class);
        NeoForge.EVENT_BUS.register(KampvuurMarshmallow.class);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> Verhalen.vergeetAlles());
        GuhHooks.tick(KampvuurPyjama::tick);
        GuhHooks.doelen((guh, goals) -> goals.addGoal(3, new LuisterGoal(guh)));
        KamperenVoortgang.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(SLAAPZAK_ITEM.get()));
    }

    /** Opa Guh (OPA_GUH): at the kampeerplekjes and on his bench in the Knuffeldal town. */
    @Nullable
    public static NpcRole role() {
        return OpaGuh.INSTANCE;
    }

    private KamperenFeature() {
    }
}
