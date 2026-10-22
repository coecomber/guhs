package nl.juiced.guhs.feature.knabbelvelden;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.registry.ModCreativeTabs;

/**
 * 1.6.0: De Eeuwige Knabbelvelden (design ch. 7). Retire tamed guhs you don't take along any more: "Njeg, niet dood hoor,
 * gewoon met pensioen!". The {@link KnabbelportaalBlock Knabbelportaal} (crafted from kaasknabbel blocks and kaasknabbels)
 * opens the big screen: your guhs at home and in the Knabbelvelden side by side, send them ({@link Pensioen#stuur}) and
 * bring them home as a picked-up guh ({@link Pensioen#terug}); the button "Naar de Knabbelvelden" takes you to your own
 * plot in the dimension ({@link Knabbelvelden}), where your retired guhs live as see-through ghosts with a halo
 * ({@link Geesten}). Nobody gets hurt or hungry there, and nothing can be broken or built (only in creative).
 * Resources: tools/features/knabbelvelden.py (dimension, biome, portal block, recipe, texts).
 */
public final class KnabbelveldenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<KnabbelportaalBlock> PORTAAL = BLOCKS.registerBlock("knabbelportaal", KnabbelportaalBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5f, 6f).sound(SoundType.WOOL).noOcclusion()
                    .lightLevel(s -> 11).pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, e) -> false)
                    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> PORTAAL_ITEM = ITEMS.registerSimpleBlockItem(PORTAAL);

    /** Server: per player in the Knabbelvelden the food level and saturation they keep. */
    private static final Map<UUID, float[]> ETEN = new HashMap<>();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(KnabbelPayloads::register);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == ModCreativeTabs.GUHS_TAB.getKey()) {
                e.accept(portaal());
            }
        });
        GuhHooks.tick(Geesten::leef);
        GuhHooks.klik(Geesten::klik);
        NeoForge.EVENT_BUS.addListener(Geesten::opJoin);
        NeoForge.EVENT_BUS.addListener(Geesten::tick);
        NeoForge.EVENT_BUS.addListener(Pensioen::tick);
        NeoForge.EVENT_BUS.addListener(KnabbelveldenFeature::opSchade);
        NeoForge.EVENT_BUS.addListener(KnabbelveldenFeature::opSpelerTick);
        NeoForge.EVENT_BUS.addListener(KnabbelveldenFeature::opBreken);
        NeoForge.EVENT_BUS.addListener(KnabbelveldenFeature::opPlaatsen);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            Pensioen.vergeet(e.getEntity().getUUID());
            ETEN.remove(e.getEntity().getUUID());
            if (e.getEntity().level() instanceof ServerLevel level && Knabbelvelden.level(level.getServer()) instanceof ServerLevel velden) {
                for (Entity g : com.google.common.collect.Lists.newArrayList(velden.getAllEntities())) {   // (a copy: discard while iterating crashes)
                    Geesten.Geest geest = Geesten.isGeest(g) ? Geesten.geest(g) : null;
                    if (geest != null && geest.eigenaar().equals(e.getEntity().getUUID())) {
                        g.discard();
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            Geesten.allesWeg(e.getServer());
            Pensioen.stopAlles();
            ETEN.clear();
        });
    }

    /** Nobody gets hurt in the Knabbelvelden (only /kill and the void go through; falling out puts you back on your plot). */
    private static void opSchade(LivingIncomingDamageEvent event) {
        Entity e = event.getEntity();
        if (Knabbelvelden.in(e) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
            e.resetFallDistance();
        }
    }

    /** Never hungry, never out of breath; whoever falls off the cheese is back at their plot spawn. */
    private static void opSpelerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 5 != 0) {
            return;
        }
        if (!Knabbelvelden.in(p)) {
            ETEN.remove(p.getUUID());
            return;
        }
        var food = p.getFoodData();
        float[] vloer = ETEN.computeIfAbsent(p.getUUID(), id -> new float[] {food.getFoodLevel(), food.getSaturationLevel()});
        vloer[0] = Math.max(vloer[0], food.getFoodLevel());
        vloer[1] = Math.max(vloer[1], food.getSaturationLevel());
        food.setFoodLevel((int) vloer[0]);
        food.setSaturation(Math.min(vloer[1], vloer[0]));
        p.setAirSupply(p.getMaxAirSupply());
        if (p.getY() < Knabbelvelden.VLOER - 8) {
            BlockPos s = Knabbelvelden.spawn(KnabbelveldenData.get(p.level().getServer()).plot(p.getUUID()));
            p.teleportTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5);
            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            p.resetFallDistance();
        }
    }

    private static boolean beschermd(net.minecraft.world.level.LevelAccessor level, Player p) {
        return level instanceof ServerLevel sl && Knabbelvelden.echt(sl) && (p == null || !p.getAbilities().instabuild);
    }

    private static void opBreken(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        if (beschermd(event.getLevel(), event.getPlayer())) {
            event.setCanceled(true);
            if (event.getPlayer() instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.knabbelvelden.beschermd")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    private static void opPlaatsen(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player p ? beschermd(event.getLevel(), p) : beschermd(event.getLevel(), null)) {
            event.setCanceled(true);
        }
    }

    public static ItemStack portaal() {
        return new ItemStack(PORTAAL_ITEM.get());
    }

    private KnabbelveldenFeature() {
    }
}
