package nl.juiced.guhs.feature.einde2.ballon;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen;
import nl.juiced.guhs.feature.kaasmijn.LoreItem;
import nl.juiced.guhs.feature.titels.Titels;

/**
 * einde2 phase C, slice trap5_ballon (CONTRACT_E2_B 7.12; DESIGN 6 "Eindproject: de Vredesballon"): the Vredesballon, the
 * big project of tech tier 5, after the Werktuigguh's own projects. Wired in feature/einde2/Einde2 and client/Einde2Client.
 * <ul>
 *   <li>{@link Vredesballon}: the line "vredesballon" (five stages of factory output, the launch, the rewards, a bag of
 *       knabbels a day) and what a click on the board does;</li>
 *   <li>{@link BallonBord}: the board on the building site (clicks; each player's own text display);</li>
 *   <li>{@link BallonLagen}: each player's own balloon on the building site (the overlays, through Schijn);</li>
 *   <li>{@link BallonScene} + {@link BallonActeur}: the launch; {@link BeeldjeBlock}: the statuette;
 *       {@link BallonCommando}: the dev command {@code /guhs einde2 ballon ...}.</li>
 * </ul>
 * Resources: tools/features/einde2_trap5_ballon.py (+ _model, _wiki). The FTB rows: the same module; the open end (tier 6):
 * tools/features/tech_quests.py FTB_SLOT.
 */
public final class E2BallonFeature {
    public static final net.neoforged.neoforge.registries.DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final net.neoforged.neoforge.registries.DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** The statuette, a reward (CONTRACT_E2_B 1.3: block/item guhs:vredesballon). */
    public static final DeferredBlock<BeeldjeBlock> VREDESBALLON = BLOCKS.registerBlock("vredesballon", BeeldjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.0f, 3f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<LoreItem.Block> VREDESBALLON_ITEM = ITEMS.registerItem("vredesballon",
            p -> new LoreItem.Block(VREDESBALLON.get(), p), () -> new Item.Properties().rarity(Rarity.RARE));
    /** The balloon as an actor of its launch scene (made by scenes only, client side; never saved). */
    public static final DeferredHolder<EntityType<?>, EntityType<BallonActeur>> VREDESBALLON_ACTEUR = ENTITY_TYPES.register(
            "einde2_trap5_ballon_vredesballon",
            () -> EntityType.Builder.<BallonActeur>of(BallonActeur::new, MobCategory.MISC).sized(7f, 3f).eyeHeight(1.5f).fireImmune()
                    .clientTrackingRange(16).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("einde2_trap5_ballon_vredesballon"))));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        // the line and the scene (common code: both sides know them)
        Vredesballon.LIJN.id();
        BallonScene.laad();
        GroteVerhalen.zetLijn(Vredesballon.ID, GroteVerhalen.Wereld.GUHEINDE);
        Titels.registreer(new Titels.Titel(Vredesballon.TITEL, "gui.guhs.titel." + Vredesballon.TITEL, ChatFormatting.GOLD, "guhs:vredesballon",
                p -> Vredesballon.LIJN.klaar(p)));
        Schijn.registreer(BallonLagen::vul);
        Vredesballon.luister();
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, true, PlayerInteractEvent.RightClickBlock.class, BallonBord::opKlik);
        NeoForge.EVENT_BUS.addListener(BallonBord::opJoin);
        NeoForge.EVENT_BUS.addListener(E2BallonFeature::opSpelerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                BallonBord.vergeet(p);
                BallonLagen.vergeet(p);
                Vredesballon.vergeet(p.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                BallonBord.weg(p);
                BallonLagen.vergeet(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(BallonCommando::register);
    }

    private static void opSpelerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        int t = p.tickCount + p.getId();
        if (!GuheindeFeature.isGuheinde(p.level())) {
            if (t % 100 == 3) {
                BallonLagen.vergeet(p);          // (a mayfly of ours that survived a restart elsewhere)
            }
            return;
        }
        if (t % 20 == 7) {
            BallonBord.tick(p);
        }
        if (t % 10 == 3) {
            BallonLagen.tick(p);
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(VREDESBALLON_ITEM.get()));
    }

    private E2BallonFeature() {
    }
}
