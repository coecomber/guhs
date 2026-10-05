package nl.juiced.guhs.feature.klusjes;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.knus.GuhHooks;

import net.minecraft.world.item.component.TooltipDisplay;
/**
 * De klusjes rond het Guhhuisje (2.10 "Lieve vadsjes van elkaar", slice klusjes). The residents of a huisje do chores
 * within its home base ({@link nl.juiced.guhs.feature.huisje.Huisjes#BEREIK}); the owner switches them on or off per
 * resident in the huisje screen. Ten chores, in this order (= the order in the screen):
 * <ol>
 *   <li>{@link OpgravenKlus opgraven}: dig up kaasknabbels in grass, dirt or sand (sometimes something rare);</li>
 *   <li>{@link FarmenKlus farmen}: harvest ripe crops and plant them again, water thirsty guhtuintjes;</li>
 *   <li>{@link OpruimenKlus opruimen}: pick up things lying around, sort the chest into the Bank Guh;</li>
 *   <li>{@link DierenKlus dieren}: pet and feed the farm animals, collect wool, eggs, kaasmelk, harvest knabbelkorven;</li>
 *   <li>{@link BakkenKlus bakken}: knabbelgraan to the guh-molentje, bake pastries in the knabbeloven;</li>
 *   <li>{@link VissenKlus vissen}: fish guhvissen and schelpjes when there is water;</li>
 *   <li>{@link WakenKlus waken}: peep and run to the owner when a Mika or monster comes, push Mika's gently away;</li>
 *   <li>{@link PlukkenKlus plukken}: pick guh flowers, knabbelbessen and sweet berries (the flowers stay, no new ones are planted);</li>
 *   <li>{@link LampjesKlus lampjes}: guh lamps on in the evening, off in the morning (with a yawn);</li>
 *   <li>{@link OppasKlus oppas}: cuddle and feed muisjes and turtles, heal a hurt guh with a snack.</li>
 * </ol>
 * Everything a chore brings in goes through {@link nl.juiced.guhs.feature.huisje.HuisjeOpslag#lever} (a Bank Guh sorts it).
 * Every finished chore: hearts ({@code Reden.KLUSJE}), the moment KLUSJE, the dagboek stats and first times
 * ({@link KlusBeloning}). Guhs are always lief: nothing here ever hurts anything. tools/features/klusjes.py makes the
 * resources.
 */
public final class KlusjesFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    /** Salt of the chores' loot rolls (CONTRACT_210 par. 2). */
    public static final long SALT = 20210301L;

    /** What the lampjes chore may switch (blocks with a LIT property). */
    public static final TagKey<Block> LAMPJES = TagKey.create(Registries.BLOCK, Guhs.id("klusjes/lampjes"));
    /** Where kaasknabbels can be dug up. */
    public static final TagKey<Block> GRAAFGROND = TagKey.create(Registries.BLOCK, Guhs.id("klusjes/graafgrond"));
    /** The rare finds of digging and fishing ("VAHOEG, kijk wat ik vond!"). */
    public static final TagKey<Item> ZELDZAAM = TagKey.create(Registries.ITEM, Guhs.id("klusjes/zeldzaam"));

    public static final DeferredBlock<GuhlampjeBlock> GUHLAMPJE = BLOCKS.registerBlock("klusjes_guhlampje", GuhlampjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.6f).sound(SoundType.LANTERN).noOcclusion()
                    .lightLevel(s -> s.getValue(GuhlampjeBlock.LIT) ? 15 : 0));
    public static final DeferredItem<BlockItem> GUHLAMPJE_ITEM = ITEMS.registerItem("klusjes_guhlampje",
            p -> new BlockItem(GUHLAMPJE.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("block.guhs.klusjes_guhlampje.lore").withStyle(ChatFormatting.GRAY));
                }
            }, () -> new Item.Properties());
    public static final DeferredItem<SchelpjeItem> SCHELPJE = ITEMS.registerItem("klusjes_schelpje", SchelpjeItem::new, () -> new Item.Properties());

    public static final DeferredHolder<SoundEvent, SoundEvent> GRAAF = sound("klusjes.graaf");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIEP = sound("klusjes.piep");
    public static final DeferredHolder<SoundEvent, SoundEvent> KLAAR = sound("klusjes.klaar");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAMPJE = sound("klusjes.lampje");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUW = sound("klusjes.duw");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLONS = sound("klusjes.plons");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZEE = sound("klusjes.schelpje");

    /** A little yellow sparkle: a chore done, something found. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STERRETJE = PARTICLES.register("klusjes_sterretje",
            () -> new SimpleParticleType(false));
    /** A pink "!" over a guh that peeps a warning. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> UITROEP = PARTICLES.register("klusjes_uitroep",
            () -> new SimpleParticleType(true));

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** The ten chores, in CONTRACT_210 par. 5.4 order. */
    public static final List<String> IDS = List.of("opgraven", "farmen", "opruimen", "dieren", "bakken", "vissen", "waken", "plukken",
            "lampjes", "oppas");

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        PARTICLES.register(modBus);
        Klusjes.registreer(new OpgravenKlus());
        Klusjes.registreer(new FarmenKlus());
        Klusjes.registreer(new OpruimenKlus());
        Klusjes.registreer(new DierenKlus());
        Klusjes.registreer(new BakkenKlus());
        Klusjes.registreer(new VissenKlus());
        Klusjes.registreer(new WakenKlus());
        Klusjes.registreer(new PlukkenKlus());
        Klusjes.registreer(new LampjesKlus());
        Klusjes.registreer(new OppasKlus());
        // a guh that was busy with a chore when it was saved: put its little basket away again
        GuhHooks.tick(guh -> {
            if ((guh.tickCount + guh.getId()) % 40 == 0) {
                StappenTaak.opruimenNaLaden(guh);
            }
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GUHLAMPJE_ITEM.get()));
        output.accept(new ItemStack(SCHELPJE.get()));
    }

    private KlusjesFeature() {
    }
}
