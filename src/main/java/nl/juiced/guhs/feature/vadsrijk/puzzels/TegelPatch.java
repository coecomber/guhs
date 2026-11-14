package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.TegelBlokken;

/**
 * 1.10.3 (npc-vloer): one-time live repairs of a spot where a puzzle stamp was put over map content, by restoring the map tiles.
 * {@code /guhs vadsrijk tegelpatch <naam> proef|doe|herstel} (op only):
 * <ul>
 *   <li>{@code proef}: dry run, changes nothing: the stamp, whether the campfire is there, how many cells differ from the tiles;</li>
 *   <li>{@code doe}: backs up every cell it will change ({@code <world>/guhs_patches/<naam>.nbt}, refused when that file exists),
 *   forgets the puzzle's spot, puts the tile blocks back in the stamp's area (box + 9 around for its stairs, 18 below for its
 *   fill, 9 above; other puzzles' boxes untouched) and stamps the puzzle again at once (the new search keeps 2 blocks away from
 *   every campfire and NPC spot);</li>
 *   <li>{@code herstel}: undo: the new stamp's area back to the tiles, the backed-up cells back, the old puzzle spot back.</li>
 * </ul>
 * Built self-contained (named patches in {@link #PATCHES}) so it can be folded into a general patch registry later.
 * Player progress is untouched: puzzle progress is keyed by puzzle id, campfire ticks by the campfire position (which stays).
 */
public final class TegelPatch {
    /** A patch: the puzzle stamped over map content, and the campfire (or other spot) it must free. */
    public record Patch(String naam, String puzzel, BlockPos vrij) {
    }

    public static final Map<String, Patch> PATCHES = Map.of(
            // z2_ijsschuif (De Ijsblokken) was stamped at 602 100 -988 over kampvuur_knuffelmeer_1 (Het Bevroren Knuffelmeer)
            "kampvuur_knuffelmeer", new Patch("kampvuur_knuffelmeer", "z2_ijsschuif", vuur("kampvuur_knuffelmeer_1", 604, 100, -986)));
    static final int RAND = 9, ONDER = 18, BOVEN = 9;

    private TegelPatch() {
    }

    private static BlockPos vuur(String id, int x, int y, int z) {
        BlockPos b = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.plek(id);
        return b != null ? b : new BlockPos(x, y, z);
    }

    static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("tegelpatch").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (Patch p : PATCHES.values()) {
            k.then(Commands.literal(p.naam())
                    .then(Commands.literal("proef").executes(c -> proef(c, p)))
                    .then(Commands.literal("doe").executes(c -> doe(c, p)))
                    .then(Commands.literal("herstel").executes(c -> herstel(c, p))));
        }
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static void zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), true);
    }

    private static Path backup(ServerLevel l, Patch p) {
        return l.getServer().getWorldPath(LevelResource.ROOT).resolve("guhs_patches").resolve(p.naam() + ".nbt");
    }

    /** Does the stamp (box + 1) cover this spot? */
    static boolean raakt(PuzzelData.Plek pl, BlockPos b) {
        BlockPos o = pl.origin();
        return b.getX() >= o.getX() - 1 && b.getX() <= o.getX() + pl.breed() && b.getZ() >= o.getZ() - 1 && b.getZ() <= o.getZ() + pl.diep()
                && b.getY() >= o.getY() - ONDER && b.getY() <= o.getY() + pl.hoogte();
    }

    /** The cells of the stamp's area that differ from the map tiles (other puzzles' boxes and air-vs-air left out). */
    static List<BlockPos> verschil(ServerLevel l, TegelBlokken t, PuzzelData.Plek pl, String puzzel) {
        List<PuzzelData.Plek> anderen = new ArrayList<>();
        PuzzelData.get(l.getServer()).alle().forEach((id, x) -> {
            if (!id.equals(puzzel)) {
                anderen.add(x);
            }
        });
        BlockPos o = pl.origin();
        List<BlockPos> uit = new ArrayList<>();
        for (int x = o.getX() - RAND; x < o.getX() + pl.breed() + RAND; x++) {
            for (int z = o.getZ() - RAND; z < o.getZ() + pl.diep() + RAND; z++) {
                l.getChunk(x >> 4, z >> 4);
                for (int y = Math.max(l.getMinY(), o.getY() - ONDER); y <= o.getY() + pl.hoogte() + BOVEN; y++) {
                    BlockPos b = new BlockPos(x, y, z);
                    BlockState nu = l.getBlockState(b), moet = t.staat(b);
                    if (nu == moet || nu.isAir() && moet.isAir()) {
                        continue;
                    }
                    boolean ander = false;
                    for (PuzzelData.Plek a : anderen) {
                        ander |= raakt(a, b);
                    }
                    if (!ander) {
                        uit.add(b);
                    }
                }
            }
        }
        return uit;
    }

    private static boolean vuurDaar(ServerLevel l, BlockPos b) {
        for (BlockPos x : BlockPos.betweenClosed(b.offset(-1, -1, -1), b.offset(1, 1, 1))) {
            if (l.getBlockState(x).getBlock() instanceof CampfireBlock) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static ServerLevel rijk(CommandContext<CommandSourceStack> c) {
        ServerLevel l = Vadsrijk.level(c.getSource().getServer());
        if (l == null) {
            c.getSource().sendFailure(Component.literal("Geen Vadsrijk op deze server"));
        }
        return l;
    }

    static int proef(CommandContext<CommandSourceStack> c, Patch p) {
        ServerLevel l = rijk(c);
        if (l == null) {
            return 0;
        }
        PuzzelData.Plek pl = PuzzelData.get(l.getServer()).plek(p.puzzel());
        zeg(c, "[tegelpatch " + p.naam() + "] PROEF (verandert niets). Kampvuur bij " + p.vrij().toShortString() + ": "
                + (vuurDaar(l, p.vrij()) ? "aanwezig" : "WEG") + "; backup " + (Files.exists(backup(l, p)) ? "bestaat al (patch al gedaan?)" : "nog niet"));
        if (pl == null) {
            zeg(c, "Puzzel " + p.puzzel() + " is (nog) niet gestempeld: niets te doen.");
            return 0;
        }
        zeg(c, "Puzzel " + p.puzzel() + " staat op " + pl.origin().toShortString() + " (" + pl.breed() + "x" + pl.diep() + "), raakt de plek: " + raakt(pl, p.vrij()));
        List<BlockPos> v = verschil(l, new TegelBlokken(l), pl, p.puzzel());
        Map<String, Integer> soorten = new TreeMap<>();
        for (BlockPos b : v) {
            soorten.merge(BuiltInRegistries.BLOCK.getKey(l.getBlockState(b).getBlock()).getPath(), 1, Integer::sum);
        }
        zeg(c, v.size() + " cellen wijken af van de kaarttegels en gaan terug; nu: " + soorten);
        return v.size();
    }

    static int doe(CommandContext<CommandSourceStack> c, Patch p) {
        ServerLevel l = rijk(c);
        if (l == null) {
            return 0;
        }
        Path f = backup(l, p);
        if (Files.exists(f)) {
            c.getSource().sendFailure(Component.literal("Backup " + f + " bestaat al: de patch is al gedaan (eerst herstel, of verwijder het bestand)"));
            return 0;
        }
        PuzzelData data = PuzzelData.get(l.getServer());
        PuzzelData.Plek pl = data.plek(p.puzzel());
        if (pl == null || !raakt(pl, p.vrij())) {
            c.getSource().sendFailure(Component.literal("Puzzel " + p.puzzel() + " ligt niet over " + p.vrij().toShortString() + ": niets te doen"));
            return 0;
        }
        TegelBlokken t = new TegelBlokken(l);
        List<BlockPos> v = verschil(l, t, pl, p.puzzel());
        CompoundTag tag = new CompoundTag();
        tag.put("puzzel", plekTag(pl));
        ListTag cellen = new ListTag();
        for (BlockPos b : v) {
            CompoundTag e = new CompoundTag();
            e.putInt("x", b.getX());
            e.putInt("y", b.getY());
            e.putInt("z", b.getZ());
            e.put("s", NbtUtils.writeBlockState(l.getBlockState(b)));
            cellen.add(e);
        }
        tag.put("cellen", cellen);
        try {
            Files.createDirectories(f.getParent());
            NbtIo.writeCompressed(tag, f);
        } catch (IOException e) {
            c.getSource().sendFailure(Component.literal("Backup schrijven mislukt: " + e));
            return 0;
        }
        Puzzels.verwijder(p.puzzel());
        data.zet(p.puzzel(), null);
        for (BlockPos b : v) {
            l.setBlock(b, t.staat(b), 2);
        }
        String nieuw = "niet (geen anker)";
        PuzzelDef d = Puzzels.def(p.puzzel());
        if (d != null) {
            BlockPos o = PuzzelStempel.plaats(l, d, false);
            nieuw = o == null ? "niet" : o.toShortString();
        }
        zeg(c, "[tegelpatch " + p.naam() + "] " + v.size() + " cellen terug naar de kaarttegels (backup " + f.getFileName() + "); kampvuur nu "
                + (vuurDaar(l, p.vrij()) ? "aanwezig" : "NOG STEEDS WEG") + "; puzzel opnieuw gestempeld op " + nieuw);
        return v.size();
    }

    static int herstel(CommandContext<CommandSourceStack> c, Patch p) {
        ServerLevel l = rijk(c);
        if (l == null) {
            return 0;
        }
        Path f = backup(l, p);
        CompoundTag tag;
        try {
            tag = NbtIo.readCompressed(f, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            c.getSource().sendFailure(Component.literal("Geen backup " + f + ": " + e.getMessage()));
            return 0;
        }
        PuzzelData data = PuzzelData.get(l.getServer());
        PuzzelData.Plek nu = data.plek(p.puzzel());
        TegelBlokken t = new TegelBlokken(l);
        int weg = 0;
        if (nu != null) {   // the new stamp's area back to the tiles
            Puzzels.verwijder(p.puzzel());
            data.zet(p.puzzel(), null);
            for (BlockPos b : verschil(l, t, nu, p.puzzel())) {
                l.setBlock(b, t.staat(b), 2);
                weg++;
            }
        }
        var blokken = l.holderLookup(Registries.BLOCK);
        ListTag cellen = tag.getListOrEmpty("cellen");
        for (int i = 0; i < cellen.size(); i++) {
            CompoundTag e = cellen.getCompoundOrEmpty(i);
            BlockPos b = new BlockPos(e.getIntOr("x", 0), e.getIntOr("y", 0), e.getIntOr("z", 0));
            l.getChunk(b);
            l.setBlock(b, NbtUtils.readBlockState(blokken, e.getCompoundOrEmpty("s")), 2);
        }
        CompoundTag pt = tag.getCompoundOrEmpty("puzzel");
        PuzzelData.Plek oud = new PuzzelData.Plek(new BlockPos(pt.getIntOr("x", 0), pt.getIntOr("y", 0), pt.getIntOr("z", 0)), pt.getIntOr("b", 0),
                pt.getIntOr("d", 0), pt.getIntOr("h", 0), pt.getIntOr("v", 0));
        data.zet(p.puzzel(), oud);
        PuzzelDef d = Puzzels.def(p.puzzel());
        if (d != null) {
            Puzzels.activeer(d, l, oud.origin(), false);
        }
        try {
            Files.move(f, f.resolveSibling(p.naam() + ".hersteld-" + System.currentTimeMillis() + ".nbt"));
        } catch (IOException e) {
            // (the next doe refuses while it exists: say so)
            c.getSource().sendFailure(Component.literal("Backup niet hernoemd: " + e.getMessage()));
        }
        zeg(c, "[tegelpatch " + p.naam() + "] hersteld: " + cellen.size() + " cellen terug, nieuwe stempel weg (" + weg + " cellen), puzzel weer op "
                + oud.origin().toShortString());
        return cellen.size();
    }

    private static CompoundTag plekTag(PuzzelData.Plek p) {
        CompoundTag c = new CompoundTag();
        c.putInt("x", p.origin().getX());
        c.putInt("y", p.origin().getY());
        c.putInt("z", p.origin().getZ());
        c.putInt("b", p.breed());
        c.putInt("d", p.diep());
        c.putInt("h", p.hoogte());
        c.putInt("v", p.vingerafdruk());
        return c;
    }
}
