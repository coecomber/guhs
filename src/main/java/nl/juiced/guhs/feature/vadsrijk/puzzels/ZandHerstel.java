package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.TegelBlokken;

/**
 * 1.10.19 (live): repairs collapsed sand. The zone-4 beaches are pink concrete powder with air under it; lighting a campfire gave a
 * block update and whole beaches fell onto the islands below (part of it turned to concrete in the water). Since 1.10.19 nothing
 * falls in Het Vadsrijk ({@code KernEvents}); this puts the map tiles back where falling blocks changed them.
 * <p>{@code /guhs vadsrijk zandherstel <naam> <x1> <z1> <x2> <z2> proef|doe} and {@code zandherstel <naam> herstel} (op only):
 * a cell in the box (only in loaded chunks, y 0-319) is restored when it differs from the tile AND the tile's block or the live
 * block is a falling block (sand, gravel, concrete powder...) or the live block is concrete where the tile has none (powder that
 * landed in water). {@code doe} first writes every cell it changes to {@code <world>/guhs_patches/zand_<naam>.nbt} (refused
 * when it exists); {@code herstel} puts those back. Blocks are set without neighbour updates.
 */
public final class ZandHerstel {
    private ZandHerstel() {
    }

    static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("zandherstel").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.argument("naam", StringArgumentType.word())
                .then(Commands.literal("herstel").executes(ZandHerstel::herstel))
                .then(Commands.argument("x1", IntegerArgumentType.integer()).then(Commands.argument("z1", IntegerArgumentType.integer())
                        .then(Commands.argument("x2", IntegerArgumentType.integer()).then(Commands.argument("z2", IntegerArgumentType.integer())
                                .then(Commands.literal("proef").executes(c -> loop(c, false)))
                                .then(Commands.literal("doe").executes(c -> loop(c, true))))))));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static void zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), true);
    }

    private static Path backup(ServerLevel l, String naam) {
        return l.getServer().getWorldPath(LevelResource.ROOT).resolve("guhs_patches").resolve("zand_" + naam + ".nbt");
    }

    static boolean valt(BlockState s) {
        return s.getBlock() instanceof FallingBlock;
    }

    static boolean beton(BlockState s) {
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().endsWith("_concrete");
    }

    /** Should this live cell go back to the tile? (only damage done by falling blocks) Public for the gametests. */
    public static boolean moetTerug(BlockState nu, BlockState moet) {
        if (nu == moet || (nu.isAir() && moet.isAir())) {
            return false;
        }
        return valt(nu) || valt(moet) || (beton(nu) && !beton(moet));
    }

    private static int loop(CommandContext<CommandSourceStack> c, boolean doe) {
        ServerLevel l = Vadsrijk.level(c.getSource().getServer());
        if (l == null) {
            c.getSource().sendFailure(Component.literal("Geen Vadsrijk op deze server"));
            return 0;
        }
        String naam = StringArgumentType.getString(c, "naam");
        int x1 = Math.min(IntegerArgumentType.getInteger(c, "x1"), IntegerArgumentType.getInteger(c, "x2"));
        int x2 = Math.max(IntegerArgumentType.getInteger(c, "x1"), IntegerArgumentType.getInteger(c, "x2"));
        int z1 = Math.min(IntegerArgumentType.getInteger(c, "z1"), IntegerArgumentType.getInteger(c, "z2"));
        int z2 = Math.max(IntegerArgumentType.getInteger(c, "z1"), IntegerArgumentType.getInteger(c, "z2"));
        if ((long) (x2 - x1 + 1) * (z2 - z1 + 1) > 320L * 320L) {
            c.getSource().sendFailure(Component.literal("Te groot gebied (max 320 x 320)"));
            return 0;
        }
        Path f = backup(l, naam);
        if (doe && Files.exists(f)) {
            c.getSource().sendFailure(Component.literal("Backup " + f.getFileName() + " bestaat al: kies een andere naam (of herstel eerst)"));
            return 0;
        }
        TegelBlokken t = new TegelBlokken(l);
        List<BlockPos> cellen = new ArrayList<>();
        int chunks = 0, overgeslagen = 0;
        int minY = Math.max(0, l.getMinY()), maxY = Math.min(TegelBlokken.H - 1, l.getMaxY());
        for (int cx = x1 >> 4; cx <= x2 >> 4; cx++) {
            for (int cz = z1 >> 4; cz <= z2 >> 4; cz++) {
                if (!l.getChunkSource().hasChunk(cx, cz)) {
                    overgeslagen++;
                    continue;
                }
                chunks++;
                for (int x = Math.max(x1, cx << 4); x <= Math.min(x2, (cx << 4) + 15); x++) {
                    for (int z = Math.max(z1, cz << 4); z <= Math.min(z2, (cz << 4) + 15); z++) {
                        BlockPos kolom = new BlockPos(x, 0, z);
                        if (!t.heeftTegel(kolom)) {
                            continue;
                        }
                        for (int y = minY; y <= maxY; y++) {
                            BlockPos b = new BlockPos(x, y, z);
                            if (moetTerug(l.getBlockState(b), t.staat(b))) {
                                cellen.add(b);
                            }
                        }
                    }
                }
            }
        }
        if (!doe) {
            int x0 = Integer.MAX_VALUE, xm = Integer.MIN_VALUE, zz0 = Integer.MAX_VALUE, zm = Integer.MIN_VALUE;
            for (BlockPos b : cellen) {
                x0 = Math.min(x0, b.getX());
                xm = Math.max(xm, b.getX());
                zz0 = Math.min(zz0, b.getZ());
                zm = Math.max(zm, b.getZ());
            }
            zeg(c, "[zandherstel " + naam + "] PROEF: " + cellen.size() + " cellen te herstellen in " + chunks + " geladen chunks (" + overgeslagen
                    + " niet geladen)" + (cellen.isEmpty() ? "" : ", gebied " + x0 + "," + zz0 + " .. " + xm + "," + zm));
            return cellen.size();
        }
        CompoundTag tag = new CompoundTag();
        ListTag l2 = new ListTag();
        for (BlockPos b : cellen) {
            CompoundTag e = new CompoundTag();
            e.put("p", LongTag.valueOf(b.asLong()));
            e.put("s", NbtUtils.writeBlockState(l.getBlockState(b)));
            l2.add(e);
        }
        tag.put("cellen", l2);
        try {
            Files.createDirectories(f.getParent());
            NbtIo.writeCompressed(tag, f);
        } catch (Exception ex) {
            c.getSource().sendFailure(Component.literal("Backup mislukt: " + ex.getMessage()));
            return 0;
        }
        for (BlockPos b : cellen) {
            l.setBlock(b, t.staat(b), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        zeg(c, "[zandherstel " + naam + "] " + cellen.size() + " cellen hersteld in " + chunks + " chunks (" + overgeslagen + " niet geladen); backup "
                + f.getFileName());
        return cellen.size();
    }

    private static int herstel(CommandContext<CommandSourceStack> c) {
        ServerLevel l = Vadsrijk.level(c.getSource().getServer());
        String naam = StringArgumentType.getString(c, "naam");
        Path f = l == null ? null : backup(l, naam);
        if (f == null || !Files.exists(f)) {
            c.getSource().sendFailure(Component.literal("Geen backup zand_" + naam + ".nbt"));
            return 0;
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(f, NbtAccounter.unlimitedHeap());
            ListTag cellen = tag.getListOrEmpty("cellen");
            var blokken = l.holderLookup(Registries.BLOCK);
            for (int i = 0; i < cellen.size(); i++) {
                CompoundTag e = cellen.getCompoundOrEmpty(i);
                l.setBlock(BlockPos.of(e.getLongOr("p", 0L)), NbtUtils.readBlockState(blokken, e.getCompoundOrEmpty("s")),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            Files.move(f, f.resolveSibling("zand_" + naam + ".teruggezet.nbt"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            zeg(c, "[zandherstel " + naam + "] " + cellen.size() + " cellen teruggezet naar voor het herstel");
            return cellen.size();
        } catch (Exception ex) {
            c.getSource().sendFailure(Component.literal("Herstel mislukt: " + ex.getMessage()));
            return 0;
        }
    }
}
