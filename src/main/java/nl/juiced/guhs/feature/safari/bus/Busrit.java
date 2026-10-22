package nl.juiced.guhs.feature.safari.bus;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.SafariToegang;
import nl.juiced.guhs.feature.safari.rit.SchemaRit;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.6.0 (F1b): a ride on the Guh-safaribus. Free, no ticket, no link with the Reisguh or the Reisbureau (design 1.2).
 * <ul>
 *   <li>{@link #instappen} at a busstation: the station is remembered, SafariToegang takes the player into the park
 *   (remembering where they stood, so the way back ends there), every own guh that follows them comes along (as many as
 *   there are, "on the roof") and gets a safari hat when its head is bare, and the way there plays;</li>
 *   <li>{@link #uitstappen} at the lobby's bus: back to where they got on, the guhs along (the hat the bus gave comes
 *   off again), and the way back plays at that station.</li>
 * </ul>
 */
public final class Busrit {
    /** The player's saved data: the station they got on at (dimension, scene anchor, turn). */
    public static final String STATION = "guhs_safari_bus_station";
    /** A guh's persistent data: the bus gave it this safari hat. */
    public static final String HOEDJE = "guhs_safari_bus_hoedje";
    /** How far around the player own guhs come along. */
    public static final double VOLG_STRAAL = 16;
    /** (Tests: the test server has no park) instead of SafariToegang.naarPark / terug; null = the real thing. */
    @Nullable
    static volatile Predicate<ServerPlayer> testHeen, testTerug;

    private Busrit() {
    }

    /** Gets on at this station (the bus, its bell or the chauffeur's "Instappen!"). */
    public static boolean instappen(ServerPlayer p, Busstation.Oord o) {
        if (Cutscenes.bezig(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.bus.al_bezig").withStyle(ChatFormatting.YELLOW));
            return false;
        }
        List<GuhEntity> guhs = SchemaRit.volgers(p, VOLG_STRAAL);
        CompoundTag t = new CompoundTag();
        t.putString("Dim", p.level().dimension().identifier().toString());
        t.putInt("X", o.anker().getX());
        t.putInt("Y", o.anker().getY());
        t.putInt("Z", o.anker().getZ());
        t.putInt("Draai", o.draai().ordinal());
        GuhQuests.saved(p).put(STATION, t);
        p.sendSystemMessage(Component.translatable("gui.guhs.safari.bus.instappen").withStyle(ChatFormatting.GOLD));
        Predicate<ServerPlayer> test = testHeen;
        if (!(test != null ? test.test(p) : SafariToegang.naarPark(p))) {
            GuhQuests.saved(p).remove(STATION);
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.bus.storing").withStyle(ChatFormatting.RED));
            return false;
        }
        int n = neemMee(p, guhs, true);
        if (n > 0) {
            p.sendSystemMessage(Component.translatable("gui.guhs.safari.bus.guhs_mee", n).withStyle(ChatFormatting.YELLOW));
        }
        BlockPos anker = BusScenes.parkAnker();
        if (test == null && anker != null && Safari.echt(p.level()) && !BusScenes.HEEN.isEmpty()) {
            Cutscenes.speel(p, BusScenes.heen(p.getRandom()), anker, Rotation.NONE, null);
        }
        return true;
    }

    /** Gets on the bus in the park, back to the station (false: not in the park). */
    public static boolean uitstappen(ServerPlayer p) {
        if (Cutscenes.bezig(p)) {
            return false;
        }
        List<GuhEntity> guhs = SchemaRit.volgers(p, VOLG_STRAAL + 8);
        CompoundTag st = GuhQuests.saved(p).getCompound(STATION).orElse(null);
        Predicate<ServerPlayer> test = testTerug;
        if (!(test != null ? test.test(p) : SafariToegang.terug(p))) {
            return false;
        }
        neemMee(p, guhs, false);
        GuhQuests.saved(p).remove(STATION);
        if (st != null && test == null && BusScenes.TERUG != null) {
            Identifier dim = Identifier.tryParse(st.getStringOr("Dim", ""));
            BlockPos anker = new BlockPos(st.getIntOr("X", 0), st.getIntOr("Y", 0), st.getIntOr("Z", 0));
            int draai = st.getIntOr("Draai", 0);
            if (dim != null && p.level().dimension() == ResourceKey.create(Registries.DIMENSION, dim)
                    && anker.distToCenterSqr(p.position()) < 64 * 64 && draai >= 0 && draai < Rotation.values().length) {
                Cutscenes.speel(p, BusScenes.TERUG, anker, Rotation.values()[draai], null);
            }
        }
        return true;
    }

    /** Puts these guhs next to the player (another dimension too); into the park they get a hat, out of it the bus' hat comes off. */
    static int neemMee(ServerPlayer p, List<GuhEntity> guhs, boolean park) {
        ServerLevel level = (ServerLevel) p.level();
        int n = 0;
        for (GuhEntity g : guhs) {
            double a = n * 2.4;
            Vec3 plek = p.position().add(Math.cos(a) * 1.6, 0, Math.sin(a) * 1.6);
            Entity e;
            if (g.level() == level) {
                g.teleportTo(plek.x, plek.y, plek.z);
                e = g;
            } else {
                e = g.teleport(new TeleportTransition(level, plek, Vec3.ZERO, p.getYRot(), 0f, TeleportTransition.DO_NOTHING));
            }
            if (e instanceof GuhEntity mee) {
                hoedje(mee, park);
                n++;
            }
        }
        return n;
    }

    /** In the park every own guh wears a safari hat (unless it wears something on its head already); the bus takes its own hat back. */
    static void hoedje(GuhEntity g, boolean op) {
        CompoundTag data = g.getPersistentData();
        if (op) {
            if (g.getClothes(GuhClothes.Slot.HEAD) == null) {
                g.wear(GuhClothes.SAFARI_HOEDJE);
                data.putBoolean(HOEDJE, true);
            }
        } else if (data.getBooleanOr(HOEDJE, false)) {
            if (g.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.SAFARI_HOEDJE) {
                g.takeOff(GuhClothes.Slot.HEAD);
            }
            data.remove(HOEDJE);
        }
    }

    /** (Debug/tests) the station this player got on at, or null. */
    @Nullable
    static CompoundTag station(ServerPlayer p) {
        return GuhQuests.saved(p).getCompound(STATION).orElse(null);
    }

    static boolean inPark(Level level) {
        return Safari.echt(level);
    }
}
