package nl.juiced.guhs.feature.ringh4;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.spiesburcht.BurchtStructure;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * bbq2 (ring-h4): the op commands {@code /guhs ringh4 ...} (dev checks and the AutoCheck script
 * tools/autocheck/bbq2_ring-h4.txt; the texts are plain literals: nobody but an op ever reads them).
 * <pre>
 * /guhs ringh4 stand            the player's step, the copy of the city around them, the boats near them
 * /guhs ringh4 stap &lt;0-7&gt;       the story up to chapter 4 at this step (chapters 1-3 done; 7 = chapter 4 done too)
 * /guhs ringh4 ga &lt;plek&gt;        to a spot of the copy the player stands in (poort, zaal, gast, spiegel, dal, steiger, aanleg,
 *                               beeld_noord, uitgang ...: the names of plekken.json)
 * /guhs ringh4 kaart            the narrator card;  /guhs ringh4 scene: the mirror scene on the mirror of this copy
 * /guhs ringh4 boot             into the guide boat of this copy (as a click on it)
 * /guhs ringh4 bouw             (dev runs only) builds the whole city here, the gate at the player's feet, as a try-out copy
 * </pre>
 */
final class RingH4Commands {
    static void register(RegisterCommandsEvent event) {
        var cmd = Commands.literal("ringh4").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        cmd.then(Commands.literal("stand").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Boomstad.Kopie k = Boomstad.bij(p.level(), p.blockPosition());
            ElfenbootjeEntity boot = Vaart.boot(p.level(), p.position(), ElfenbootjeEntity.GIDS);
            return zeg(c, "step " + RingH4Feature.LIJN.stap(p) + "/" + RingH4Feature.LIJN.stappen() + (RingH4Feature.LIJN.aanDeBeurt(p) ? "" : " (chapter 3 not done)")
                    + ", copy " + (k == null ? "none here" : "anchor " + k.anker().toShortString() + " " + k.draai()) + ", in its box: "
                    + Boomstad.binnen(p.level(), p.blockPosition()) + ", guide boat " + (boot == null ? "none near" : boot.blockPosition().toShortString()
                    + (boot.isWeg() ? " (under way)" : "")));
        }));
        cmd.then(Commands.literal("stap").then(Commands.argument("n", IntegerArgumentType.integer(0, 7)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = IntegerArgumentType.getInteger(c, "n");
            Ring.lijn(1).begin(p);
            for (int i = 1; i <= 3; i++) {
                Verhaallijn l = Ring.lijn(i);
                l.zet(p, l.stappen());
            }
            RingH4Feature.LIJN.wis(p);
            RingH4Feature.LIJN.zet(p, n);
            return zeg(c, "chapter 4 at step " + RingH4Feature.LIJN.stap(p));
        })));
        cmd.then(Commands.literal("ga").then(Commands.argument("plek", StringArgumentType.word()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String plek = StringArgumentType.getString(c, "plek");
            Boomstad.Kopie k = Boomstad.bij(p.level(), p.blockPosition());
            if (k == null || !Plekken.heeft(plek)) {
                return zeg(c, k == null ? "no copy of the tree city here" : "no such spot: " + plek);
            }
            Vec3 daar = k.punt(plek);
            p.teleportTo(daar.x, daar.y + 0.1, daar.z);
            return zeg(c, plek + ": " + BlockPos.containing(daar).toShortString());
        })));
        cmd.then(Commands.literal("kaart").executes(c -> zeg(c, "card: " + Verteller.toon(c.getSource().getPlayerOrException(), RingH4Feature.KAART, null))));
        cmd.then(Commands.literal("scene").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Boomstad.Kopie k = Boomstad.bij(p.level(), p.blockPosition());
            if (k == null) {
                return zeg(c, "no copy of the tree city here");
            }
            Spiegel.kijk(p, k.blok("spiegel"));
            return zeg(c, "the mirror at " + k.blok("spiegel").toShortString());
        }));
        cmd.then(Commands.literal("boot").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Boomstad.Kopie k = Boomstad.bij(p.level(), p.blockPosition());
            ElfenbootjeEntity boot = k == null ? null : Vaart.boot(p.level(), k.punt("boot"), ElfenbootjeEntity.GIDS);
            if (boot == null) {
                return zeg(c, "no guide boat here");
            }
            Vec3 steiger = k.punt("steiger");
            p.teleportTo(steiger.x, steiger.y + 0.1, steiger.z);
            return zeg(c, "in the boat: " + Vaart.stapIn(p, boot));
        }));
        if (!FMLEnvironment.isProduction()) {
            cmd.then(Commands.literal("bouw").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                StructureStart start = bouw(p.level(), p.blockPosition());
                Ring.OVERAL = true;        // (a try-out copy may stand in any dimension)
                return zeg(c, "the tree city, " + start.getPieces().size() + " tiles, the gate at your feet");
            }));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(cmd));
    }

    /**
     * (dev runs) the whole city as a structure start made by hand, with the very tiles worldgen places, not turned, the
     * gate's path at {@code bij}; {@code Kopieen.test} makes the story, the protection and the inhabitants treat it as real
     * until the server stops.
     */
    static StructureStart bouw(ServerLevel level, BlockPos bij) {
        Structure structure = Kopieen.structuur(level, Boomstad.STRUCTUUR);
        if (!(structure instanceof BurchtStructure burcht)) {
            throw new IllegalStateException("no structure guhs:" + Boomstad.STRUCTUUR);
        }
        BlockPos nul = bij.subtract(Plekken.blok("poort"));
        List<StructurePiece> pieces = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Identifier id = burcht.tile(i, j);
                if (level.getStructureManager().get(id).isPresent()) {
                    BlockPos offset = new BlockPos(i * 32, 0, j * 32);
                    pieces.add(new BurchtStructure.Piece(level.getStructureManager(), id, nul.offset(offset), Rotation.NONE, burcht.anchor().subtract(offset)));
                }
            }
        }
        StructureStart start = new StructureStart(structure, ChunkPos.containing(bij), 0, new PiecesContainer(pieces));
        for (StructurePiece piece : pieces) {
            BoundingBox box = piece.getBoundingBox();
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), box, ChunkPos.containing(bij), nul);
        }
        Kopieen.test(level, start);
        return start;
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal("[ringh4] " + tekst), false);
        return 1;
    }

    private RingH4Commands() {
    }
}
