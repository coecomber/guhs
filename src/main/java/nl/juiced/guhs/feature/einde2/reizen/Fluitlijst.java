package nl.juiced.guhs.feature.einde2.reizen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2 phase B (CONTRACT_E2_B 7.10, DESIGN "Reizen en ligging"), slice reizen: Appa's whistle list. Blow the Appa-fluitje
 * while you sit on your own Appa's head (in the Guheinde) and a screen asks "Waar vliegen we heen?": every building you know
 * ({@link E2Verhaal#bekendeGebouwen}: the story structures you unlocked, then every Guheinde building you discovered), with
 * how far it is. A story flight that has not landed yet comes first, with a star. Pick one and Appa flies you there (you may
 * steer; let go and he flies himself), your team on his back.
 */
public final class Fluitlijst {
    private Fluitlijst() {
    }

    /** The whistle blown on Appa's head (Appa.OP_ZIJN_KOP): opens the list. False: not here (then the old line). */
    static boolean opZijnKop(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level) || !GuheindeFeature.isGuheinde(level)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.fluit.niet_hier").withStyle(ChatFormatting.LIGHT_PURPLE));
            return true;
        }
        open(p);
        return true;
    }

    /** Sends the list to the player's screen. */
    static void open(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        ListTag lijst = new ListTag();
        String[] wacht = E2Reizen.wachtend(p);
        if (wacht != null) {
            lijst.add(rij(level, p, wacht[0], true));
        }
        for (String s : bestemmingen(p)) {
            if (wacht == null || !wacht[0].equals(s)) {
                lijst.add(rij(level, p, s, false));
            }
        }
        CompoundTag data = new CompoundTag();
        data.put("Haltes", lijst);
        ModNetworking.sendTo(p, new E2ReizenPayloads.Bestemmingen(data));
    }

    /** Every place this player may fly to (the whistle list; story structures that are not there yet left out). */
    public static List<String> bestemmingen(ServerPlayer p) {
        ServerLevel level = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        return E2Verhaal.bekendeGebouwen(p, false).stream()
                .filter(s -> level != null && Plekken.bekend(level, s) && Plekken.aankomst(level, s) != null).toList();
    }

    private static CompoundTag rij(ServerLevel level, ServerPlayer p, String s, boolean verder) {
        CompoundTag t = new CompoundTag();
        t.putString("Id", s);
        t.putString("Naam", "structure.guhs." + s);
        Vec3 a = Plekken.bekend(level, s) ? Plekken.aankomst(level, s) : null;
        t.putInt("Afstand", a == null ? -1 : (int) Math.round(Math.sqrt(p.distanceToSqr(a))));
        t.putBoolean("Verder", verder);
        return t;
    }

    /** The player picked a place on the list: checked again, then the flight. */
    static void kies(ServerPlayer p, String structuur) {
        if (!(p.getVehicle() instanceof AppaGuhEntity appa) || !appa.isEigenaar(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.fluit.niet_op_appa").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        String[] wacht = E2Reizen.wachtend(p);
        if (wacht != null && wacht[0].equals(structuur)) {
            if (!E2Reizen.hervat(p)) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.fluit.kan_niet").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return;
        }
        if (!bestemmingen(p).contains(structuur)) {
            return;
        }
        if (E2Reizen.vlieg(p, structuur, E2Reizen.Vlucht.VRIJ, null)) {
            GuhAdvancements.grant(p, "einde2_reizen_fluitvlucht");
            p.sendSystemMessage(Component.translatable("gui.guhs.einde2_reizen.fluit.op_weg", Component.translatable("structure.guhs." + structuur))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.fluit.kan_niet").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
