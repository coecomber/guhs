package nl.juiced.guhs.feature.huisje;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guhpixel.Stempel;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.registry.ModEntities;
import org.slf4j.Logger;

/**
 * 1.3.2: the dev commands of "Huisje betreden", for gamemasters (and the console, and the AutoCheck scripts), under
 * {@code /guhs huisje}: {@code binnen} (into the nearest huisje within 32 blocks, without the fade) · {@code buiten} ·
 * {@code demo <klein|medium|groot>} (builds a huisje of your own next to you with a full house of example guhs, most of
 * them asleep inside, and brings you in) · {@code nacht <aan|uit>} (the nearest huisje thinks it is night, so its residents
 * go to bed) · {@code herstempel} (stamps the room you are in again) · {@code zelftest} (no player needed: the dimension
 * exists, the three room templates stamp and hold what {@code kamers.json} says; lines "[huisje-zelftest] ...").
 */
public final class BinnenCommando {
    private static final Logger LOGGER = LogUtils.getLogger();

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> huisje = Commands.literal("huisje").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        huisje.then(Commands.literal("binnen").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Huisje h = dichtstbij(p);
            if (h == null) {
                ctx.getSource().sendFailure(Component.literal("geen huisje binnen 32 blokken"));
                return 0;
            }
            return Binnen.betreed(p, h) ? 1 : 0;
        }));
        huisje.then(Commands.literal("buiten").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (!Binnen.in(p)) {
                ctx.getSource().sendFailure(Component.literal("je bent niet in een huisje"));
                return 0;
            }
            Binnen.naarBuiten(p, Component.translatable("gui.guhs.huisje.binnen.buiten"));
            return 1;
        }));
        for (HuisjeMaat maat : HuisjeMaat.values()) {
            huisje.then(Commands.literal("demo").then(Commands.literal(maat.id()).executes(ctx -> demo(ctx.getSource().getPlayerOrException(), maat))));
        }
        huisje.then(Commands.literal("nacht")
                .then(Commands.literal("aan").executes(ctx -> nacht(ctx.getSource(), true)))
                .then(Commands.literal("uit").executes(ctx -> nacht(ctx.getSource(), false))));
        huisje.then(Commands.literal("herstempel").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Huisje h = Binnen.binnenIn(p);
            if (h == null) {
                ctx.getSource().sendFailure(Component.literal("je bent niet in een huisje"));
                return 0;
            }
            h.celVersie = 0;
            return Binnen.zorg(p.level().getServer(), h) ? 1 : 0;
        }));
        huisje.then(Commands.literal("zelftest").executes(ctx -> {
            List<String> regels = zelftest(ctx.getSource().getServer());
            boolean console = ctx.getSource().getEntity() == null;
            for (String regel : regels) {
                if (!console) {
                    LOGGER.info(regel);   // (the console prints what it is told already)
                }
                ctx.getSource().sendSuccess(() -> Component.literal(regel), false);
            }
            return regels.get(regels.size() - 1).contains(" 0 FOUT") ? 1 : 0;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(huisje));
    }

    @Nullable
    private static Huisje dichtstbij(ServerPlayer p) {
        return Huisjes.rond((ServerLevel) p.level(), p.blockPosition(), 32).stream()
                .min(Comparator.comparingDouble(h -> h.pos().distSqr(p.blockPosition()))).orElse(null);
    }

    private static int nacht(CommandSourceStack source, boolean aan) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Huisje h = dichtstbij(source.getPlayerOrException());
        if (h == null) {
            source.sendFailure(Component.literal("geen huisje binnen 32 blokken"));
            return 0;
        }
        if (aan) {
            HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.NACHT);
        } else {
            HuisjeGoal.TEST_DAGDEEL.remove(h.pos());
        }
        source.sendSuccess(() -> Component.literal(h.naam() + (aan ? ": het is nu nacht voor de bewoners" : ": weer de gewone klok")), false);
        return 1;
    }

    /** A huisje of the player four blocks east of them, full of example guhs: two in three asleep inside, the rest sitting outside. */
    private static int demo(ServerPlayer p, HuisjeMaat maat) {
        ServerLevel level = (ServerLevel) p.level();
        BlockPos pos = p.blockPosition().east(4 + maat.breedte() / 2);
        for (BlockPos b : Huisje.blokken(pos, Direction.SOUTH, maat)) {
            if (!level.getBlockState(b).canBeReplaced()) {
                level.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        Huisje h = HuisjeBlock.bouw(level, pos, Direction.SOUTH, maat, p.getUUID());
        GuhVariant[] soorten = GuhVariant.values();
        GuhClothes[] kleren = GuhClothes.values();
        String[] namen = {"Vadsje", "Knabbel", "Njegje", "Pluis", "Snoet", "Kaasje", "Vahoeg", "Dutje"};
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.NACHT);
        for (int i = 0; i < maat.plekken(); i++) {
            GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (guh == null) {
                continue;
            }
            BlockPos d = h.deur().south(1 + i / 3).east(i % 3 - 1);
            guh.snapTo(d.getX() + 0.5, d.getY(), d.getZ() + 0.5, 0f, 0f);
            guh.setVariant(soorten[(i * 5) % Math.min(soorten.length, 12)]);
            guh.setCustomName(Component.literal(namen[i % namen.length]));
            level.addFreshEntity(guh);
            guh.tame(p);
            if (i % 2 == 0) {
                guh.wear(kleren[(i * 7 + 3) % kleren.length]);
            }
            Huisjes.trekIn(h, guh);
            if (i % 3 == 2) {
                guh.setOrderedToSit(true);
                guh.setInSittingPose(true);
            } else {
                Huisjes.naarBinnen(guh, h);
            }
        }
        return Binnen.betreed(p, h) ? 1 : 0;
    }

    /** The self test of the real dimension; the last line is "[huisje-zelftest] klaar: n OK, m FOUT". */
    public static List<String> zelftest(MinecraftServer server) {
        List<String> uit = new ArrayList<>();
        int[] tel = new int[2];
        java.util.function.BiConsumer<Boolean, String> check = (goed, tekst) -> {
            uit.add("[huisje-zelftest] " + (goed ? "OK " : "FOUT ") + tekst);
            tel[goed ? 0 : 1]++;
        };
        ServerLevel level = server.getLevel(Binnen.DIM);
        check.accept(level != null, "the dimension guhs:huisje_binnen exists");
        check.accept(Binnen.oorsprong(0).equals(new BlockPos(0, Binnen.Y, 0)) && Binnen.oorsprong(Binnen.PER_RIJ + 3).equals(
                new BlockPos(3 * Binnen.CEL, Binnen.Y, Binnen.CEL)), "grid maths");
        if (level != null) {
            check.accept(!level.dimensionType().hasSkyLight() || level.dimensionType().hasFixedTime(), "a fixed light (no day and night)");
            int cel = Binnen.PER_RIJ * Binnen.PER_RIJ - 1;   // (a far corner no huisje will ever get)
            for (HuisjeMaat maat : HuisjeMaat.values()) {
                BinnenKamer k = BinnenKamer.van(server, maat);
                check.accept(k != null && k.bedden().size() == maat.plekken(), maat.id() + ": kamers.json has " + maat.plekken() + " beds");
                if (k == null) {
                    continue;
                }
                BlockPos o = Binnen.oorsprong(cel--);
                Stempel.leeg(level, o, BinnenKamer.MAX);
                boolean gestempeld = Stempel.plaats(level, BinnenKamer.template(maat), o);
                check.accept(gestempeld, maat.id() + ": the template stamps");
                if (gestempeld) {
                    int bedden = 0, borden = 0;
                    for (BinnenKamer.Bed b : k.bedden()) {
                        bedden += level.getBlockState(o.offset(b.bed())).is(HuisjeFeature.BEDJE.get()) ? 1 : 0;
                        borden += level.getBlockEntity(o.offset(b.bord())) instanceof SignBlockEntity ? 1 : 0;
                    }
                    check.accept(bedden == maat.plekken() && borden == maat.plekken(), maat.id() + ": " + bedden + " beds and " + borden + " name signs stand");
                    boolean deur = k.deur().stream().allMatch(d -> level.getBlockState(o.offset(d)).getBlock() instanceof DoorBlock);
                    BlockState hoofd = level.getBlockState(o.offset(k.logeerHoofd())), voet = level.getBlockState(o.offset(k.logeerVoet()));
                    check.accept(deur && hoofd.getBlock() instanceof BedBlock && voet.getBlock() instanceof BedBlock
                            && level.getBlockEntity(o.offset(k.prikbord())) instanceof SignBlockEntity, maat.id() + ": the door, the logeerbedje and the prikbord stand");
                    BlockPos mat = o.offset(k.mat());
                    check.accept(level.getBlockState(mat.below()).isCollisionShapeFullBlock(level, mat.below())
                            && level.getBlockState(mat.above()).isAir(), maat.id() + ": the doormat has a floor and headroom");
                    boolean dicht = true;   // (nobody looks or falls out: the shell is closed all around the room)
                    for (int x = 1; x < k.maat().getX() - 1 && dicht; x++) {
                        for (int z = 1; z < k.maat().getZ() - 2 && dicht; z++) {
                            dicht = !level.getBlockState(o.offset(x, 0, z)).isAir() && !level.getBlockState(o.offset(x, k.maat().getY() - 1, z)).isAir();
                        }
                    }
                    check.accept(dicht, maat.id() + ": floor and ceiling are closed");
                }
                if (gestempeld && maat == HuisjeMaat.KLEIN) {
                    int dingen = BinnenInrichting.proef(level, net.minecraft.world.phys.Vec3.atBottomCenterOf(o.offset(k.mat())));
                    check.accept(dingen == 3, "a blanket, a note and a thing on a table can stand in the dimension: " + dingen + " of 3");
                }
                Stempel.ruim(level, Stempel.doos(o, BinnenKamer.MAX).inflate(1));
                Stempel.leeg(level, o, BinnenKamer.MAX);
            }
        }
        uit.add("[huisje-zelftest] klaar: " + tel[0] + " OK, " + tel[1] + " FOUT");
        return uit;
    }

    private BinnenCommando() {
    }
}
