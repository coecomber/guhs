package nl.juiced.guhs.feature.snuffeldorp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.snuffel.Eiland;
import nl.juiced.guhs.feature.snuffel.Rang;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The friendly roadblock: the rest of the island is there to see, but a Snuffelpup does not get in. In the island it is a
 * striped barrier with a sign in the one road through the ridge ("Hier mag je pas door als Snuffelneus"); the RULE is a
 * line ({@code grens_z} of dorp.json): a dog below the rank Snuffelneus that gets north of it, however (swimming around
 * the rocks, a jump nobody thought of), is put back where it last stood south of the line, unharmed, with the same
 * sentence. A builder in creative mode and a spectator pass; so does every dog from the rank Snuffelneus on (the later
 * update's hook: nothing here needs to change then).
 */
public final class Wegversperring {
    /** A spot only counts as "back here" when it is this far south of the line. */
    private static final double MARGE = 2.5;
    private static final Map<UUID, Vec3> GOED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> GEZEGD = new ConcurrentHashMap<>();

    private Wegversperring() {
    }

    /** May this player walk behind the roadblock? */
    public static boolean mag(ServerPlayer p) {
        return p.isCreative() || p.isSpectator() || Rang.van(p).nummer() >= Rang.SNUFFELNEUS.nummer();
    }

    static void tick(ServerPlayer p, Eiland.Plaats plaats, Plekken pl) {
        if (pl.grensZ() == Plekken.GEEN_GRENS) {
            return;
        }
        Vec3 pos = p.position();
        if (!pl.dicht(plaats, pos)) {
            if (!pl.dicht(plaats, pos.add(0, 0, -MARGE)) && (p.onGround() || p.isInWater())) {
                GOED.put(p.getUUID(), pos);
            }
            return;
        }
        if (mag(p)) {
            return;
        }
        Vec3 terug = GOED.get(p.getUUID());
        if (terug == null || pl.dicht(plaats, terug) || Eiland.van(p.level(), terug) == null) {
            terug = pl.midden(plaats, Plekken.VERSPERRING);
        }
        if (terug == null) {
            terug = plaats.strand();
        }
        Duwtje.terug(p, p.level().dimension(), terug, p.getYRot());
        long nu = p.level().getServer().getTickCount();
        Long laatst = GEZEGD.get(p.getUUID());
        if (laatst == null || nu - laatst >= 60) {
            GEZEGD.put(p.getUUID(), nu);
            p.sendOverlayMessage(Component.translatable("gui.guhs.snuffeldorp.versperring").withStyle(ChatFormatting.GOLD));
            GuhAdvancements.grant(p, "snuffel_dorp_versperring");
        }
    }

    static void vergeet(UUID speler) {
        GOED.remove(speler);
        GEZEGD.remove(speler);
    }

    static void wis() {
        GOED.clear();
        GEZEGD.clear();
    }
}
