package nl.juiced.guhs.feature.einde2.reizen;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2 phase B (CONTRACT_E2_B 7.10, DESIGN "Reizen en ligging"), slice reizen: Appa's menu "Waar vliegen we heen?".
 * <p>
 * It opens when his owner right-clicks him (einde2 fix-reizen, playtest 1 item 10; sneak + click is still his saddle chest)
 * and when they blow the Appa-fluitje while sitting on his head. It lists every building they know
 * ({@link E2Verhaal#bekendeGebouwen}: the story structures they unlocked, then every Guheinde building they discovered), with
 * how far it is; the place where their story goes on ({@link E2Reizen#verhaalDoel}: a story flight that has not landed yet,
 * else the main building of their chapter) comes first, as one gold row with a star: "Verder met het verhaal: vlieg naar
 * <gebouw>". Right-clicked from the ground, the first row is "Opstappen": just climb on and fly yourself. Pick a place and
 * Appa flies you there (you may steer; let go and he flies himself), your team on his back, and lands on the place's
 * official landing spot ({@link Landingen}). einde2 fix3-reizen (playtest 2 item 4): the story row is a normal flight to the
 * chapter building's landing platform; the old second row "meteen naar je stap" ({@code E2Hervat.verder}, a teleport to
 * the step's own spot: a door, a ship) is gone from this menu (the kaasbol keeps it). In another dimension the same list
 * works: Appa takes you through the void to the landing spot ({@link AppaReis}); in the Geestenwereld he cannot follow you.
 * <p>
 * einde2 fix4-appa (playtest 3): the player's own Appa-platforms ({@link AppaPlatform}) are always the top rows; after the
 * whole story every einde2 building is on the list ({@link #bestemmingen}); and a pick no longer takes off at once: it seats
 * the player on Appa and opens a small dialog with "Yip yip!" ({@link #VERTREK}, {@link #yipYip}). Until they click it other
 * players can climb on and ride along; closing the dialog or getting off cancels it. The story flights themselves
 * ({@link E2Reizen#vlieg} from the chapters) do not wait for it.
 */
public final class Fluitlijst {
    /** The id of the row "Opstappen" (climb on, no flight). */
    static final String OPSTAPPEN = "";
    /**
     * The id the old row "meteen naar je stap" sent (merge_fix1). fix3-reizen: that row is gone; a pick with this id (an old
     * screen still open) now flies to the story's place like the gold row, never straight to the step.
     */
    static final String VERDER = "#verder";
    /** How far (blocks) from Appa a pick from his menu still counts (he is big, and they just clicked him). */
    static final double BEREIK = 16;

    private Fluitlijst() {
    }

    /** The whistle blown on Appa's head (Appa.OP_ZIJN_KOP): opens the menu. */
    static boolean opZijnKop(ServerPlayer p) {
        if (AppaReis.inGeestenwereld(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.fluit.niet_hier").withStyle(ChatFormatting.LIGHT_PURPLE));
            return true;
        }
        open(p);
        return true;
    }

    /** His owner right-clicked him from the ground (Appa.KLIK): opens the menu, with "Opstappen" on top. */
    static boolean klik(ServerPlayer p) {
        if (AppaReis.inGeestenwereld(p)) {
            return false;                                       // (there he is just a mount: climb on)
        }
        open(p);
        return true;
    }

    /** Sends the menu to the player's screen. */
    static void open(ServerPlayer p) {
        CompoundTag data = new CompoundTag();
        data.put("Haltes", rijen(p));
        ModNetworking.sendTo(p, new E2ReizenPayloads.Bestemmingen(data));
    }

    /**
     * The rows of the menu: "Opstappen" (when not on him yet), then the story's place (gold, "Verhaal": a flight to that
     * building's landing spot, the same as any other row), then every other place they know.
     */
    static ListTag rijen(ServerPlayer p) {
        ServerLevel guheinde = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        boolean hier = guheinde != null && p.level() == guheinde;
        ListTag lijst = new ListTag();
        // fix4-appa (playtest 3): the player's own Appa-platforms always on top (the one in this dimension first)
        java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, net.minecraft.core.BlockPos> platforms = AppaPlatform.alle(p);
        List<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> dims = new java.util.ArrayList<>(platforms.keySet());
        dims.sort(java.util.Comparator.comparing(d -> d == p.level().dimension() ? 0 : 1));
        for (var dim : dims) {
            lijst.add(platformRij(p, dim, platforms.get(dim)));
        }
        if (!(p.getVehicle() instanceof AppaGuhEntity appa && appa.isEigenaar(p))) {
            CompoundTag op = new CompoundTag();
            op.putString("Id", OPSTAPPEN);
            op.putString("Naam", "gui.guhs.einde2_reizen.fluit.opstappen");
            op.putInt("Afstand", -1);
            op.putBoolean("Opstappen", true);
            lijst.add(op);
        }
        String[] wacht = E2Reizen.wachtend(p);
        String verhaal = E2Reizen.verhaalDoel(p);
        if (verhaal != null) {
            // (also when its place is not worked out yet: the pick then says "nog niet" and starts the search)
            lijst.add(rij(guheinde, hier, p, verhaal, wacht != null && wacht[0].equals(verhaal), true));
        }
        for (String s : bestemmingen(p)) {
            if (!s.equals(verhaal)) {
                lijst.add(rij(guheinde, hier, p, s, false, false));
            }
        }
        return lijst;
    }

    /**
     * Every place this player may fly to (the menu; story structures that are not there yet left out). fix4-appa (playtest
     * 3): once the whole story is done (the finale, the challenge "De Guhvatar") every building einde2 placed in the Guheinde
     * is on it ({@link Einde2Ids#LEEGTE_STRUCTUREN}: the four main buildings, the eight side places with the Leegtehaven and
     * the ring race, the Sponsguh pair), also the ones never visited; the Mika-vestingen are not (many copies, no landing
     * field).
     */
    public static List<String> bestemmingen(ServerPlayer p) {
        ServerLevel level = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        List<String> namen = new java.util.ArrayList<>(E2Verhaal.bekendeGebouwen(p, false));
        if (E2Verhaal.finaleKlaar(p)) {
            List<String> alle = new java.util.ArrayList<>(Einde2Ids.LEEGTE_STRUCTUREN);
            for (String s : namen) {
                if (!alle.contains(s)) {
                    alle.add(s);
                }
            }
            namen = alle;
        }
        return namen.stream().filter(s -> level != null && Plekken.bekend(level, s) && Plekken.aankomst(level, s) != null).toList();
    }

    /** A platform row: "<naam>s Appa-platform", with the distance here or the dimension it is in ("door de leegte"). */
    private static CompoundTag platformRij(ServerPlayer p, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,
                                           net.minecraft.core.BlockPos pos) {
        CompoundTag t = new CompoundTag();
        boolean hier = dim == p.level().dimension();
        t.putString("Id", AppaPlatform.rijId(dim));
        t.putBoolean("Platform", true);
        t.putString("Speler", p.getGameProfile().name());
        t.putString("Dim", dim.identifier().toString());
        t.putInt("Afstand", hier ? (int) Math.round(Math.sqrt(p.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos)))) : -1);
        t.putBoolean("Leegte", !hier);
        return t;
    }

    /** May this player pick this place (it is on their menu)? */
    static boolean mag(ServerPlayer p, String structuur) {
        var dim = AppaPlatform.dimVan(structuur);
        if (dim != null) {
            return AppaPlatform.van(p, dim) != null;
        }
        return structuur.equals(E2Reizen.verhaalDoel(p)) || bestemmingen(p).contains(structuur);
    }

    private static CompoundTag rij(@Nullable ServerLevel level, boolean hier, ServerPlayer p, String s, boolean verder, boolean verhaal) {
        CompoundTag t = new CompoundTag();
        t.putString("Id", s);
        t.putString("Naam", "structure.guhs." + s);
        Vec3 a = level != null && Plekken.bekend(level, s) ? Plekken.aankomst(level, s) : null;
        t.putInt("Afstand", a == null || !hier ? -1 : (int) Math.round(Math.sqrt(p.distanceToSqr(a))));
        t.putBoolean("Verder", verder);
        t.putBoolean("Verhaal", verhaal);
        t.putBoolean("Leegte", !hier);
        return t;
    }

    /** The player picked a row of the menu: checked again, then climbing on or the flight. */
    static void kies(ServerPlayer p, String structuur) {
        boolean op = p.getVehicle() instanceof AppaGuhEntity appa && appa.isEigenaar(p);
        AppaGuhEntity appa = Appa.geladen(p);
        boolean dichtbij = appa != null && appa.level() == p.level() && appa.distanceTo(p) <= BEREIK;
        if (structuur.equals(VERDER)) {
            String verhaal = E2Reizen.verhaalDoel(p);           // (fix3-reizen: the story's landing spot, never the step)
            if (verhaal == null) {
                return;
            }
            structuur = verhaal;
        }
        if (structuur.equals(OPSTAPPEN)) {
            if (!op && dichtbij && p.startRiding(appa)) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_appa.opstappen", appa.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return;
        }
        if (!op && !dichtbij) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.fluit.ver").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (!mag(p, structuur)) {
            return;
        }
        // fix4-appa (playtest 3): first on Appa; he takes off only with "Yip yip!" (meanwhile friends can climb on)
        if (!op && (appa == null || !p.startRiding(appa, true, false))) {
            return;
        }
        VERTREK.put(p.getUUID(), structuur);
        CompoundTag data = new CompoundTag();
        data.put("Doel", doelRij(p, structuur));
        ModNetworking.sendTo(p, new E2ReizenPayloads.Vertrek(data));
    }

    /**
     * Player -> the place they picked in Appa's menu and wait to leave for (fix4-appa): they sit on him, the small dialog
     * "Yip yip!" is open, other players may climb on. Gone when they leave, dismount or close the dialog.
     */
    static final java.util.Map<java.util.UUID, String> VERTREK = new java.util.concurrent.ConcurrentHashMap<>();

    /** The row of the place picked, for the "Yip yip!" dialog (its name and whether it goes through the void). */
    private static CompoundTag doelRij(ServerPlayer p, String structuur) {
        var dim = AppaPlatform.dimVan(structuur);
        if (dim != null) {
            net.minecraft.core.BlockPos pos = AppaPlatform.van(p, dim);
            return platformRij(p, dim, pos == null ? p.blockPosition() : pos);
        }
        ServerLevel guheinde = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        return rij(guheinde, guheinde != null && p.level() == guheinde, p, structuur, false, structuur.equals(E2Reizen.verhaalDoel(p)));
    }

    /**
     * The "Yip yip!" dialog answered (fix4-appa): {@code gaan} = Appa takes off for the place picked (a flight, or the dive
     * through the void; the riders come along); else (closed, "Toch niet") he stays, the player stays on him.
     */
    static void yipYip(ServerPlayer p, boolean gaan) {
        String doel = VERTREK.remove(p.getUUID());
        if (doel == null) {
            return;
        }
        if (!gaan) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.yip.blijft").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (!(p.getVehicle() instanceof AppaGuhEntity appa && appa.isEigenaar(p))) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.yip.niet_op").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (!mag(p, doel)) {
            return;
        }
        var dim = AppaPlatform.dimVan(doel);
        boolean ok = dim != null ? AppaReis.naarPlatform(p, dim) : AppaReis.vliegNaar(p, doel);
        if (ok) {
            GuhAdvancements.grant(p, "einde2_reizen_fluitvlucht");
        }
    }

    /** Every 10 ticks: a pick whose player left or got off Appa is forgotten (getting off = not going). */
    static void vertrekTick(net.minecraft.server.MinecraftServer server) {
        for (var e : VERTREK.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || !(p.getVehicle() instanceof AppaGuhEntity appa && appa.isEigenaar(p))) {
                VERTREK.remove(e.getKey(), e.getValue());
            }
        }
    }
}
