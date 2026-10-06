package nl.juiced.guhs.feature.ringh2;

import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * bbq2 (ring-h2): the op commands {@code /guhs ringh2 ...} (dev checks and the AutoCheck script
 * tools/autocheck/bbq2_ring-h2.txt; the texts are plain literals: nobody but an op ever reads them).
 * <pre>
 * /guhs ringh2 stand               the player's step, who they met, the copy of Guhvendel they stand at
 * /guhs ringh2 stap &lt;0-6&gt;          sets the chapter to that step (chapter 1 is finished first; 0 also forgets the chapter)
 * /guhs ringh2 bel                 rings the council bell of the copy the player stands at (what a click on it does)
 * /guhs ringh2 scene raad|genootschap      plays that cutscene at the copy's council ring (no step changes)
 * /guhs ringh2 ga bel|hal|poort|keuken     puts the player at that spot of the copy
 * /guhs ringh2 bouw                (dev runs only) places the template with its corner at the player's feet, unturned, and
 *                                  lets it count as a copy until the server stops (a flat test world has no Barbecuether)
 * </pre>
 * The engine's own {@code /guhs verhaal stap|scene|kaart|herbekijk ...} work on this chapter too.
 */
final class RingH2Commands {
    static void register(RegisterCommandsEvent event) {
        var h2 = Commands.literal("ringh2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        h2.then(Commands.literal("stand").executes(RingH2Commands::stand));
        for (int n = 0; n <= Guhvendel.STAPPEN; n++) {
            int stap = n;
            h2.then(Commands.literal("stap").then(Commands.literal(String.valueOf(n)).executes(c -> stap(c, stap))));
        }
        h2.then(Commands.literal("bel").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Guhvendel.Oord o = Guhvendel.oord(p.level(), p.blockPosition());
            return o == null ? zeg(c, "no copy of Guhvendel here") : zeg(c, "the bell: council started = " + Guhvendel.luid(p, o));
        }));
        for (Cutscene scene : Guhvendel.scenes()) {
            h2.then(Commands.literal("scene").then(Commands.literal(scene.id().substring("ringh2_".length())).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Guhvendel.Oord o = Guhvendel.oord(p.level(), p.blockPosition());
                if (o == null) {
                    return zeg(c, "no copy of Guhvendel here");
                }
                return zeg(c, scene.id() + ": " + Cutscenes.speel(p, scene, o.anker(), o.draai(), null));
            })));
        }
        ga(h2, "bel", Guhvendel.BEL.offset(1, -1, 1));
        ga(h2, "hal", new BlockPos(24, 6, 26));
        ga(h2, "poort", new BlockPos(30, 5, 58));
        ga(h2, "keuken", new BlockPos(41, 5, 44));
        if (!FMLEnvironment.isProduction()) {
            h2.then(Commands.literal("bouw").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                return zeg(c, bouw(p.level(), p.blockPosition()) ? "Guhvendel stands here (a test copy): its gate is " + Guhvendel.MIDDEN.getX() + " east, 58 south of you"
                        : "no template guhs:" + Guhvendel.STRUCTUUR);
            }));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(h2));
    }

    private static void ga(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> h2, String naam, BlockPos lokaal) {
        h2.then(Commands.literal("ga").then(Commands.literal(naam).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            StructureStart start = nl.juiced.guhs.feature.wereld.Bezetting.start(p.level(), Guhvendel.STRUCTUUR, p.blockPosition());
            BlockPos plek = start == null ? null : Kopieen.wereld(start, null, lokaal);
            if (plek == null) {
                return zeg(c, "no copy of Guhvendel here");
            }
            p.teleportTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5);
            return zeg(c, naam + ": " + plek.toShortString());
        })));
    }

    private static int stand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Verhaallijn lijn = RingH2Feature.LIJN;
        StringBuilder wie = new StringBuilder();
        for (GuhNpcEntity.Kind kind : Guhvendel.GEZELSCHAP) {
            wie.append(' ').append(kind.id()).append(Guhvendel.heeftOntmoet(p, kind) ? "+" : "-");
        }
        Guhvendel.Oord o = Guhvendel.oord(p.level(), p.blockPosition());
        zeg(c, "ring_h2: step " + lijn.stap(p) + " of " + lijn.stappen() + (lijn.aanDeBeurt(p) ? "" : " (chapter 1 is not done)") + ", met" + wie);
        return zeg(c, o == null ? "no copy of Guhvendel here" : "copy: council ring " + o.anker().toShortString() + ", bell " + o.bel().toShortString()
                + ", turned " + o.draai() + ", in the cirque: " + o.inKom(p.position()));
    }

    private static int stap(CommandContext<CommandSourceStack> c, int stap) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Ring.lijn(1).begin(p);
        Ring.lijn(1).zet(p, Ring.lijn(1).stappen());
        if (stap == 0) {
            Guhvendel.wis(p);
        } else {
            RingH2Feature.LIJN.zet(p, stap);
        }
        return zeg(c, "ring_h2: step " + RingH2Feature.LIJN.stap(p) + " (steps only go forward: step 0 starts the chapter over)");
    }

    /**
     * (dev command, tests) The template with its corner here, unturned, with its characters, and a structure start around it
     * that counts as a copy of guhs:guhvendel in this level until the server stops.
     */
    static boolean bouw(ServerLevel level, BlockPos hoek) {
        Optional<StructureTemplate> template = level.getStructureManager().get(Guhs.id(Guhvendel.STRUCTUUR));
        if (template.isEmpty()) {
            return false;
        }
        template.get().placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), 2);
        kopie(level, hoek, Rotation.NONE, null);
        return true;
    }

    /**
     * A structure start of guhs:guhvendel made by hand: one piece of the template with its corner at {@code hoek}, turned
     * (nothing is placed). {@code doos}: the box the piece claims (null: the template's own). Counts as a copy
     * ({@code Kopieen.test}) until {@code Kopieen.testWissen}.
     */
    static StructureStart kopie(ServerLevel level, BlockPos hoek, Rotation draai, @javax.annotation.Nullable BoundingBox doos) {
        Structure structure = Kopieen.structuur(level, Guhvendel.STRUCTUUR);
        if (structure == null) {
            throw new IllegalStateException("no structure guhs:" + Guhvendel.STRUCTUUR);
        }
        StructurePoolElement element = StructurePoolElement.single("guhs:" + Guhvendel.STRUCTUUR).apply(StructureTemplatePool.Projection.RIGID);
        BoundingBox box = doos != null ? doos : element.getBoundingBox(level.getStructureManager(), hoek, draai);
        StructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, hoek, 0, draai, box, LiquidSettings.IGNORE_WATERLOGGING);
        StructureStart start = new StructureStart(structure, ChunkPos.containing(hoek), 0, new PiecesContainer(List.of(piece)));
        Kopieen.test(level, start);
        return start;
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal("[ringh2] " + tekst), false);
        return 1;
    }

    private RingH2Commands() {
    }
}
