package nl.juiced.guhs.feature.timmerguh;

import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * 3.0 (Guhverhalen), slice timmerguh: De Timmerguh en de huisjes-introductie (DESIGN_30 par. 1).
 * <ul>
 *   <li>The <b>bouwplaats</b> at the edge of every Knuffeldal town (a jigsaw piece on the west end of hoek_noordwest's street,
 *       template knuffeldal_stadje/bouwplaats from tools/features/timmerguh_bouw.py): a half-built guhhuisje whose roof (the
 *       top of the dome and its two ears) is still see-through ghost tiles ({@link DakplekBlock}), scaffolding, a bouwkeet, a
 *       crane, and the Timmerguh (NPC {@code TIMMERGUH}, role {@link Timmerguh}).</li>
 *   <li>The questline <b>"Samen een huisje bouwen"</b> ({@link Timmerguh}, progress {@link TimmerguhVoortgang}): bring planks and
 *       pink wool, lay the oortjesdak with the loaned {@link DakpluisjeItem dakpluisjes}, let one of your guhs live in the huisje
 *       you get, and receive the {@link #BOUWBOEKJE} (the recipes of all three huisje sizes: it stays in the crafting grid), a
 *       small huisje as a present and the timmermanshelmpje. Optional: put a toy and a guhlampje in the huisje's home area
 *       for the gereedschapsriem.</li>
 *   <li>The huisje recipes need the bouwboekje (tools/features/huisje.py); huisjes that were placed before keep working.</li>
 *   <li>Multiplayer: only the owner edits a huisje; everyone else may look at its screen (grey buttons, "Dit is het huisje
 *       van X": feature.huisje.HuisjeBlock / client.HuisjeScreen, on the fundament's ownership API).</li>
 * </ul>
 * Resources: tools/features/timmerguh.py (+ timmerguh_bouw.py).
 */
public final class TimmerguhFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** A see-through ghost tile of the Timmerguh's roof (deel: dak / oor / binnenoor); can't be broken in survival. */
    public static final DeferredBlock<DakplekBlock> DAKPLEK = BLOCKS.registerBlock("timmerguh_dakplek", DakplekBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(-1.0f, 3600000.0f).noLootTable().noOcclusion()
                    .sound(SoundType.WOOL).pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, e) -> false)
                    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));

    /** The Timmerguh's bouwboekje: the key of the three guhhuisje recipes (stays in the crafting grid). */
    public static final DeferredItem<BouwboekjeItem> BOUWBOEKJE = ITEMS.registerItem("timmerguh_bouwboekje", BouwboekjeItem::new,
            () -> new Item.Properties().stacksTo(1));
    /** A dakpluisje (loaned by the Timmerguh): only fits on a ghost tile of his roof. */
    public static final DeferredItem<DakpluisjeItem> DAKPLUISJE = ITEMS.registerItem("timmerguh_dakpluisje", DakpluisjeItem::new,
            () -> new Item.Properties().stacksTo(64));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NpcRollen.zet(GuhNpcEntity.Kind.TIMMERGUH, Timmerguh.ROLE);
        KledingBronnen.bron(GuhClothes.TIMMER_HELMPJE, "timmerguh");
        KledingBronnen.bron(GuhClothes.TIMMER_GEREEDSCHAPSRIEM, "timmerguh");
        Band.opMoment(Timmerguh::moment);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BOUWBOEKJE.get()));
    }

    private TimmerguhFeature() {
    }
}
