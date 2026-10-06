package nl.juiced.guhs.feature.ringh1;

import java.util.Optional;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-h1): the events of chapter 1 (registered by {@link RingH1Feature}) and the op command {@code /guhs ringh1}.
 * <ul>
 *   <li>Once a second per player: the story starts by itself when the Grillguh's barbecue burns, and the walk to the portal
 *       ends at the portal ({@link Feest}). About every two seconds: the camps around are protected boxes.</li>
 *   <li>Nobody tramples Sam-guh's moestuin.</li>
 *   <li>{@code /guhs ringh1 stand | zoek | vuurwerk | wis}, and in a dev run {@code kamp} (the camp with Guhdalf and Sam-guh
 *       where you stand) and {@code gouw} (the whole Knabbelgouw template where you stand): for AutoCheck scripts and dev
 *       checks. The texts are literals (dev tools).</li>
 * </ul>
 */
public final class RingH1Events {
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator()) {
            return;
        }
        int t = p.tickCount + p.getId();
        if (t % 20 == 7) {
            seconde(p);
        }
        if (t % 40 == 13) {
            Gouw.beschermKampen(p.level(), p.blockPosition());
        }
    }

    /** (also for the tests) what happens once a second for this player. */
    static void seconde(ServerPlayer p) {
        Feest.begin(p);
        Feest.portaal(p);
    }

    /** The moestuin of a Knabbelgouw is never trampled (the building is protected, a jump on farmland is no "break"). */
    @SubscribeEvent
    public static void onVertrappen(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level && Gouw.STRUCTUUR.equals(Bescherming.structuurBij(level, event.getPos()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        Gouw.vergeet(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onGestopt(ServerStoppedEvent event) {
        Gouw.wisAlles();
    }

    // =====================================================================================================================
    // /guhs ringh1
    // =====================================================================================================================

    static void commando(RegisterCommandsEvent event) {
        var ringh1 = Commands.literal("ringh1").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(RingH1Events::stand))
                .then(Commands.literal("zoek").executes(RingH1Events::zoek))
                .then(Commands.literal("vuurwerk").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Feest.steekAf(p.level(), p.blockPosition());
                    return 1;
                }))
                .then(Commands.literal("wis").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    wis(p);
                    return zeg(c, "ring_h1 forgotten for " + p.getScoreboardName());
                }));
        if (!FMLEnvironment.isProduction()) {
            // (these change the world: never on a real server)
            ringh1.then(Commands.literal("kamp").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                return zeg(c, zetKamp(p.level(), p.blockPosition().below(), Rotation.NONE) ? "the camp, Guhdalf and Sam-guh are here" : "no template " + Gouw.KAMP_TEMPLATE);
            }));
            ringh1.then(Commands.literal("gouw").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                return zeg(c, zetGouw(p.level(), p.blockPosition().below(Gouw.G + 1)) ? "the Knabbelgouw template is here (no residents: that is Bezetting's work)"
                        : "no template guhs:" + Gouw.STRUCTUUR);
            }));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(ringh1));
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal(tekst), false);
        return 1;
    }

    private static int stand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Verhaallijn l = RingH1Feature.LIJN;
        StringBuilder vlaggen = new StringBuilder();
        for (String v : Feest.KLUSJES) {
            vlaggen.append(' ').append(v).append('=').append(l.vlag(p, v) ? 1 : 0);
        }
        for (String v : Feest.PROVIAND) {
            vlaggen.append(' ').append(v).append('=').append(l.vlag(p, v) ? 1 : 0);
        }
        zeg(c, "ring_h1: step " + l.stap(p) + " of " + l.stappen() + (l.begonnen(p) ? "" : " (not begun)") + ", may begin: " + Feest.magBeginnen(p) + ","
                + vlaggen + ", ring: " + Ring.heeft(p) + ", Sam-guh walks along: " + Sam.looptMee(p) + ", at a portal: " + Feest.bijPortaal(p));
        return 1 + l.stap(p);
    }

    private static int zoek(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        BlockPos daar = Gouw.zoek(p.level(), p.blockPosition());
        Gouw.Plek kamp = Gouw.kampBijPut(p.level(), p.blockPosition());
        zeg(c, "nearest Guhdalf: " + (daar == null ? "none found" : daar.toShortString() + " (" + (int) Math.sqrt(daar.distSqr(p.blockPosition())) + " blocks)")
                + "; camp of the old pit here: " + (kamp == null ? "none" : kamp.hoek().toShortString() + " " + kamp.draai())
                + "; frame here: " + Optional.ofNullable(Gouw.frame(p.level(), p.blockPosition())).map(BlockPos::toShortString).orElse("none"));
        return daar == null ? 0 : 1;
    }

    /** (dev, tests) forget chapter 1 of this player: the questline, the card and the scenes, the camp they started at. */
    static void wis(ServerPlayer p) {
        RingH1Feature.LIJN.wis(p);
        Verteller.vergeet(p, RingH1Feature.KAART);
        Cutscenes.vergeet(p, RingH1Feature.AANKOMST_ID);
        Cutscenes.vergeet(p, RingH1Feature.FEEST_ID);
        CompoundTag saved = GuhQuests.saved(p);
        saved.remove(Feest.THUIS);
        saved.remove(Feest.THUIS_DIM);
        saved.remove(Feest.PORTAAL);
        Gouw.vergeet(p.getUUID());
    }

    /**
     * (dev command, tests) the camp with its corner on this block (the plate's layer), turned, with Guhdalf and a Sam-guh who
     * waits at home. False: the template is not there.
     */
    static boolean zetKamp(ServerLevel level, BlockPos hoek, Rotation draai) {
        Optional<StructureTemplate> template = level.getStructureManager().get(Gouw.KAMP_TEMPLATE);
        if (template.isEmpty()) {
            return false;
        }
        template.get().placeInWorld(level, hoek, hoek, new StructurePlaceSettings().setRotation(draai).setMirror(Mirror.NONE), level.getRandom(), Block.UPDATE_CLIENTS);
        Gouw.Plek kamp = new Gouw.Plek(hoek, draai);
        GuhNpcEntity guhdalf = Gouw.guhdalf(level, Vec3.atBottomCenterOf(kamp.wereld(Gouw.GUHDALF)), draai);
        if (guhdalf != null) {
            level.addFreshEntity(guhdalf);
        }
        GuhEntity sam = Gouw.samThuis(level, kamp.wereld(Gouw.SAM));
        if (sam != null) {
            sam.getPersistentData().putString(nl.juiced.guhs.feature.wereld.Bezetting.TAG, Gouw.SAM_KAMP);
            level.addFreshEntity(sam);
        }
        return true;
    }

    /** (dev command) the whole template of the Knabbelgouw with its corner here (its ground layer is G blocks up). */
    static boolean zetGouw(ServerLevel level, BlockPos hoek) {
        Optional<StructureTemplate> template = level.getStructureManager().get(Guhs.id(Gouw.STRUCTUUR));
        if (template.isEmpty()) {
            return false;
        }
        template.get().placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        return true;
    }

    private RingH1Events() {
    }
}
