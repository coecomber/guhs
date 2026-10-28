package nl.juiced.guhs.feature.vliegdroom;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomReis;
import nl.juiced.guhs.feature.spiesburcht.RookguhEntity;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.8.0 uitbreiding: De Vliegdroom, the island high above the Droomplein (DroomEilanden.VLIEG). You only get there on a
 * Rookguh: one hovers at its little steigertje on the plein ({@link #zorgSteigertje}: three by three pink clouds and two
 * lanterns, laid on the plein by code). Up there: the stal with Rookguhs you fly yourself through the wolkjesringen-parcours
 * ({@link VliegParcours}), the highest cloud with the view, guh-ballonnetjes, and a Rookguh that takes you back down.
 * <p>
 * The Rookguhs are kept by code ({@link #zorgRookguhs}): a missing one comes back. Its island template and spots:
 * tools/features/vliegdroom.py. Test commands: /guhs vlieg ga|reset|info (TESTEN_VLIEGDROOM.md).
 */
public final class VliegdroomFeature {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DroomRookguhEntity>> ROOKGUH = ENTITY_TYPES.register("droom_rookguh",
            () -> EntityType.Builder.of(DroomRookguhEntity::new, MobCategory.MISC).sized(1.6f, 1.8f).eyeHeight(1.3f).fireImmune()
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("droom_rookguh"))));

    /** The Vlieg-diploma: your first whole round of the parcours. */
    public static final DeferredItem<Item> DIPLOMA = ITEMS.registerItem("vlieg_diploma", p -> new Item(p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.vlieg_diploma.lore").withStyle(ChatFormatting.GRAY));
        }
    }, () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    /** How often the Rookguhs and the steigertje are looked after (ticks). */
    static final int ELKE = 40;

    private VliegdroomFeature() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(ROOKGUH.get(), RookguhEntity.createAttributes().build()));
        NeoForge.EVENT_BUS.addListener(VliegdroomFeature::tick);
        NeoForge.EVENT_BUS.addListener(VliegdroomFeature::commands);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(DIPLOMA.get()));
    }

    private static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % ELKE != 21) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(DromenFeature.DIM);
        if (level == null || level.players().isEmpty()) {
            return;
        }
        if (DroomEilanden.gebouwd(level, DroomEilanden.PLEIN) >= 1) {
            zorgSteigertje(level);
        }
        zorgRookguhs(level);
    }

    // --- the little steigertje on the plein ------------------------------------------------------------------------------

    /** The centre block of the steigertje's floor (the block under the Rookguh's feet spot). */
    public static BlockPos steigertje() {
        return BlockPos.containing(VliegPlekken.plein(VliegPlekken.get().pleinSteiger())).below();
    }

    /** Lays the steigertje when it is not there (a new plein stamp clears it): 3x3 pink clouds, two lanterns. */
    public static boolean zorgSteigertje(ServerLevel level) {
        BlockPos m = steigertje();
        if (!level.isLoaded(m)) {
            return false;
        }
        BlockState wolk = blok("guhs:wolkenblok_roze", Blocks.PINK_WOOL);
        if (level.getBlockState(m).is(wolk.getBlock())) {
            return true;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(m.offset(dx, 0, dz), wolk, 2);
                if (!level.getBlockState(m.offset(dx, 1, dz)).isAir() && !level.getBlockState(m.offset(dx, 1, dz)).isSolid()) {
                    level.setBlock(m.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        BlockState licht = blok("guhs:sterrenlantaarn", Blocks.LANTERN);
        for (BlockPos l : new BlockPos[]{m.offset(-1, 1, -1), m.offset(1, 1, -1)}) {
            if (level.getBlockState(l).isAir()) {
                level.setBlock(l, licht, 2);
            }
        }
        return true;
    }

    private static BlockState blok(String id, Block anders) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(anders).defaultBlockState();
    }

    // --- the Rookguhs -----------------------------------------------------------------------------------------------------

    /** Every Rookguh spot (kind, world spot, yaw). */
    public record Plek(DroomRookguhEntity.Rol rol, Vec3 plek, float yaw) {
    }

    public static List<Plek> plekken() {
        var p = VliegPlekken.get();
        List<Plek> uit = new ArrayList<>();
        uit.add(new Plek(DroomRookguhEntity.Rol.STEIGER, VliegPlekken.plein(p.pleinSteiger()).add(0, 0.6, 0), 180f));
        uit.add(new Plek(DroomRookguhEntity.Rol.TERUG, VliegPlekken.vlieg(p.terug()), 0f));
        for (Vec3 s : p.stal()) {
            uit.add(new Plek(DroomRookguhEntity.Rol.RIJ, VliegPlekken.vlieg(s), 180f));
        }
        return uit;
    }

    /** A missing Rookguh comes back (only where its spot is loaded, and the island it belongs to stands). */
    public static int zorgRookguhs(ServerLevel level) {
        int nieuw = 0;
        boolean vlieg = DroomEilanden.gebouwd(level, DroomEilanden.VLIEG) >= 1;
        boolean plein = DroomEilanden.gebouwd(level, DroomEilanden.PLEIN) >= 1;
        for (Plek pl : plekken()) {
            boolean opPlein = pl.rol() == DroomRookguhEntity.Rol.STEIGER;
            if ((opPlein && !plein) || (!opPlein && !vlieg) || !level.isLoaded(BlockPos.containing(pl.plek()))) {
                continue;
            }
            List<DroomRookguhEntity> er = level.getEntitiesOfClass(DroomRookguhEntity.class, new AABB(BlockPos.containing(pl.plek())).inflate(200),
                    g -> g.rol() == pl.rol() && g.thuis().distanceToSqr(pl.plek()) < 0.25);
            if (!er.isEmpty()) {
                continue;
            }
            DroomRookguhEntity g = DroomRookguhEntity.maak(level, pl.rol(), pl.plek(), pl.yaw());
            if (g != null && level.addFreshEntity(g)) {
                nieuw++;
            }
        }
        return nieuw;
    }

    // --- test commands ------------------------------------------------------------------------------------------------------

    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("vlieg").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ga").executes(c -> ga(c.getSource())))
                .then(Commands.literal("reset").executes(c -> reset(c.getSource())))
                .then(Commands.literal("info").executes(c -> info(c.getSource())))));
    }

    private static int ga(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (DroomReis.inDroom(p)) {
            DroomReis.zweef(p, DroomEilanden.VLIEG);
            return 1;
        }
        return DroomReis.naarDroom(p, DroomEilanden.VLIEG) ? 1 : 0;
    }

    private static int reset(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        GuhQuests.saved(p).remove(VliegParcours.BESTE);
        GuhQuests.saved(p).remove(VliegParcours.DIPLOMA);
        GuhQuests.saved(p).remove(VliegParcours.DAG);
        VliegParcours.stop(p.getUUID());
        source.sendSuccess(() -> Component.literal("Vliegdroom: record, diploma en dag-beloning gewist."), false);
        return 1;
    }

    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int beste = GuhQuests.saved(p).getIntOr(VliegParcours.BESTE, -1);
        source.sendSuccess(() -> Component.literal("Vliegdroom: record " + (beste < 0 ? "-" : VliegParcours.tekstTijd(beste)) + ", diploma "
                + GuhQuests.saved(p).getBooleanOr(VliegParcours.DIPLOMA, false) + ", ringen " + VliegPlekken.get().ringen().size()
                + ", volgende ring " + VliegParcours.volgende(p.getUUID())), false);
        return 1;
    }
}
