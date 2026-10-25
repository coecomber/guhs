package nl.juiced.guhs.feature.onderwol.bouw;

import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.7.0 "Onder de Wol": the buildings under the ground (design: ontwerp-guhs170-mijnen.md chapter 2.4). The shared framework
 * for every underground structure of this update, and the first seven buildings on it.
 * <p>
 * <b>The framework</b> (other slices add their buildings with the python helper {@code tools/features/onderwol_bouw.py},
 * no Java needed):
 * <ul>
 *   <li>structure type {@code guhs:grot_bouw} ({@link GrotBouwStructure}): picks a floor height in the structure's own band,
 *       on the floor of an existing cave when one is there, places the template(s) (a jigsaw, any depth) with their start
 *       jigsaw on that floor, optionally carves a natural hall around them ({@link GrotZaalPiece}), and optionally puts an
 *       entrance hut on the surface right above a marker in the build, with a shaft down ({@link GrotSchachtPiece}: a
 *       ladder, a wolkenlift up and one down). Never into the sky or the seas, never through the bottom; the room rules of
 *       {@link nl.juiced.guhs.world.BouwRuimte} (no two buildings in each other) as for every guhs structure;</li>
 *   <li>processor {@code guhs:onderwol_grot_schil} ({@link GrotSchilProcessor}, processor list of the same name): a
 *       template's bedrock marks the rock around its rooms: it keeps the world's own block there, and only where the world
 *       is open (a cave, a lake) it becomes the layer stone of that spot. So a template made from a render (its cave air
 *       is air, its rock is either nothing or this marker) sits in any layer with walls of the right stone;</li>
 *   <li>the talking guhs of the buildings ({@link #PRAAT}): an NPC of an existing kind with {@code RoleData:{guhs_plek:
 *       "<plek>"}} says one of the numbered lines {@code quest.guhs.onderwol_bouw.<plek>.<1..n>}.</li>
 * </ul>
 */
public final class OnderWolBouw {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Guhs.MODID);
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Guhs.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<GrotBouwStructure>> GROT_BOUW =
            STRUCTURE_TYPES.register("grot_bouw", () -> () -> GrotBouwStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ZAAL =
            PIECES.register("grot_zaal", () -> (StructurePieceType.ContextlessType) GrotZaalPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SCHACHT =
            PIECES.register("grot_schacht", () -> (StructurePieceType.ContextlessType) GrotSchachtPiece::new);
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<GrotSchilProcessor>> SCHIL =
            PROCESSORS.register("onderwol_grot_schil", () -> () -> GrotSchilProcessor.CODEC);
    /** The layer stone for the old underground structures (guh caves, kaasmijn, gatenkaas mine and larder): OudeBouwLaagProcessor. */
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<OudeBouwLaagProcessor>> OUDE_BOUW =
            PROCESSORS.register("onderwol_oude_bouw", () -> () -> OudeBouwLaagProcessor.CODEC);

    /** The structures of this slice (tools/features/onderwol_structuren1.py), all in the Superkompas tab "ondergrond". */
    public static final List<String> STRUCTUREN = List.of("mijnguhdorp", "knuffelslaapzaal", "paddenstoelendorpje", "verlaten_guhmijn",
            "grotschilderingen", "sausmeer_vissershutje", "mijningang_hutje");

    /**
     * The talking guhs: plek -> (kind, number of lines). The lines are {@code quest.guhs.onderwol_bouw.<plek>.<1..n>}
     * (tools/features/onderwol_structuren1.py PRAAT writes them, the gametest checks that they exist).
     */
    public static final Map<String, Praat> PRAAT = Map.of(
            "onderwol_dorp", new Praat(GuhNpcEntity.Kind.MIJNWERKERGUH, 5),
            "onderwol_ingang", new Praat(GuhNpcEntity.Kind.MIJNWERKERGUH, 4),
            "onderwol_mijn", new Praat(GuhNpcEntity.Kind.MIJNWERKERGUH, 4),
            "onderwol_slaapzaal", new Praat(GuhNpcEntity.Kind.OMAGUH, 4),
            "onderwol_paddenstoel", new Praat(GuhNpcEntity.Kind.HANAMI_BLOESEMGUH, 4),
            "onderwol_visser", new Praat(GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH, 4),
            "onderwol_gids", new Praat(GuhNpcEntity.Kind.OMAGUH, 5));

    /** A talking guh: its kind (the look, an existing one) and how many lines it has. */
    public record Praat(GuhNpcEntity.Kind kind, int regels) {
    }

    /** The role of a talking guh of this plek: a random one of its lines. */
    public static NpcRole rol(String plek, int regels) {
        return (npc, player) -> GuhQuests.say(player, npc, "quest.guhs.onderwol_bouw." + plek + "." + (1 + player.getRandom().nextInt(regels)));
    }

    public static void register(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        PIECES.register(modBus);
        PROCESSORS.register(modBus);
        PRAAT.forEach((plek, p) -> NpcRollen.zet(p.kind(), plek, rol(plek, p.regels())));
        for (String s : STRUCTUREN) {
            SuperkompasItem.voegToe("ondergrond", s);
        }
    }

    private OnderWolBouw() {
    }
}
