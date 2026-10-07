package nl.juiced.guhs.feature.einde2.vesting;

import java.util.function.Consumer;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Bescherming;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.knus.PleinSlot;

/**
 * einde2, slice vesting: the modular Mika-vesting. The structure keeps its id, its set and its placement
 * ({@code guhs:mika_vesting}, the Guheinde's own feature/guheinde); what is new is what it is made of: jigsaw pieces
 * {@code einde2_vesting/<stuk>} (a keep with the kapitein's wachtlokaal, the kitchen of the spijtige Mika-kok, a throne
 * room with the schatkamer, a cellenblok, bridges to side towers, a steiger with one of three vetschepen), in new chunks
 * only: a vesting that stood already is its old tower and stays that.
 * <ul>
 *   <li>{@link VestingCellen}: the magere guhs in the cells, for every player, with the count the title of phase B asks;</li>
 *   <li>{@link VestingVleugels}: the Guhvleugels on the ship, a pair for every player;</li>
 *   <li>protection ({@link Einde2Bescherming}) of the modular copies only ({@link #modulair});</li>
 *   <li>the spots of the kapitein, the kok and the vetbootje notice are markers ({@code Plekken.van("mika_vesting")}); the
 *       Schuilguhs on the walls are entities in the templates.</li>
 * </ul>
 * Resources: tools/features/einde2_vesting.py (the pieces: einde2_vesting_bouw.py).
 */
public final class E2VestingFeature {
    /** The templates of the modular vesting all start with this. */
    public static final String STUKKEN = "einde2_vesting/";
    /** Blocks around the boxes of its pieces that are protected too. */
    public static final int RAND = 2;
    /** How deep the jigsaw of a vesting goes (tools/features/einde2_vesting_bouw.py DIEPTE). */
    public static final int DIEPTE = 7;

    public static void register(IEventBus modBus) {
        Einde2Bescherming.registreer(Einde2Ids.MIKA_VESTING, RAND, E2VestingFeature::modulair);
        NeoForge.EVENT_BUS.addListener(VestingCellen::onPlayerTick);
        // (the Guheinde feeds a magere guh at HIGH: one listener before it, one after everything)
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VestingCellen::voorKlik);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, VestingCellen::naKlik);
        NeoForge.EVENT_BUS.addListener(VestingVleugels::onInteract);
        NeoForge.EVENT_BUS.addListener(VestingVleugels::onInteractGewoon);
        NeoForge.EVENT_BUS.addListener(VestingVleugels::onAttack);
        NeoForge.EVENT_BUS.addListener(E2VestingFeature::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    /**
     * Is this copy a modular vesting (put together from our pieces)? The towers generated before einde2 (templates
     * {@code mika_vesting} and {@code mika_vesting_schip}) are not: they stay as they are, breakable and all.
     */
    public static boolean modulair(StructureStart start) {
        for (StructurePiece piece : start.getPieces()) {
            Identifier template = PleinSlot.template(piece);
            if (template != null && template.getNamespace().equals(Guhs.MODID) && template.getPath().startsWith(STUKKEN)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code /guhs einde2 vesting bouw}: puts a whole vesting together where you stand (its wachtlokaal at your feet),
     * {@code /guhs einde2 vesting stuk <naam>}: one piece. They build in the world, so they only exist in a dev run.
     */
    private static void commando(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("vesting")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("bouw").executes(c -> bouw(c.getSource())))
                .then(Commands.literal("stuk").then(Commands.argument("naam", StringArgumentType.word())
                        .executes(c -> stuk(c.getSource(), StringArgumentType.getString(c, "naam"))))))));
    }

    private static int bouw(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Holder<StructureTemplatePool> pool = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL)
                .getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, Guhs.id(STUKKEN + "start")));
        boolean gelukt = JigsawPlacement.generateJigsaw(level, pool, Guhs.id("mika_vesting_midden"), DIEPTE, pos, false);
        source.sendSuccess(() -> Component.literal(gelukt ? "Een Mika-vesting op " + pos.toShortString() : "Dat lukte niet"), false);
        return gelukt ? 1 : 0;
    }

    private static int stuk(CommandSourceStack source, String naam) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        StructureTemplate template = level.getStructureManager().get(Guhs.id(STUKKEN + naam)).orElse(null);
        if (template == null) {
            source.sendFailure(Component.literal("Geen stuk " + STUKKEN + naam));
            return 0;
        }
        template.placeInWorld(level, pos, pos, new StructurePlaceSettings(), level.getRandom(), 2);
        source.sendSuccess(() -> Component.literal(naam + " op " + pos.toShortString() + " (" + template.getSize().toShortString() + ")"), false);
        return 1;
    }

    private E2VestingFeature() {
    }
}
