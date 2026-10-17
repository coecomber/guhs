package nl.juiced.guhs.feature.einde2.spaudio;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.bouwbikinibodem.Leegtemist;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.network.ModNetworking;

/**
 * einde2 "Sponsguh", slice sp_audio: the music area of each player (CONTRACT_E2_C 7.9), worked out by the server every
 * {@link #INTERVAL} ticks and sent to that player's client only when it changes ({@link SpAudioPayloads.Plek}):
 * <ul>
 *   <li>{@code bikinibodem}: under the dome of Bikinibodem ({@code Leegtemist.inKoepel});</li>
 *   <li>{@code loopgraaf}: in the box of Schelpenstad between the two rims of the trench ({@code loopgraaf_rand_west} ..
 *       {@code loopgraaf_rand_oost}, across its whole width);</li>
 *   <li>{@code schelpenstad}: anywhere else in the box of Schelpenstad (grown {@link #RAND} blocks: the road, De Ruige Kroeg,
 *       the town, the beach, the pier);</li>
 *   <li>"" anywhere else, and always while the place is behind its closed sluier for this player (no music from a place
 *       you cannot see; creative operators pass as they pass the sluier).</li>
 * </ul>
 * This is the Sponsguh counterpart of feature/einde2/audio's AudioPlek: its own payload and its own client listener
 * (client/SpAudioSpeler), so the Guhvatar music system stays untouched.
 */
public final class SpAudioPlek {
    public static final String BIKINIBODEM = "bikinibodem", SCHELPENSTAD = "schelpenstad", LOOPGRAAF = "loopgraaf";
    public static final List<String> GEBIEDEN = List.of(BIKINIBODEM, SCHELPENSTAD, LOOPGRAAF);
    public static final int INTERVAL = 20;
    public static final int RAND = 8;
    private static final Map<UUID, String> GEBIED = new ConcurrentHashMap<>();
    private static final Map<UUID, String> GEFORCEERD = new ConcurrentHashMap<>();

    private SpAudioPlek() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(SpAudioPlek::onPlayerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            GEBIED.remove(event.getEntity().getUUID());
            GEFORCEERD.remove(event.getEntity().getUUID());
            SpAudio.vergeet(event.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> GEBIED.remove(event.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                SpAudio.stilVoor(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                SpAudio.stilVoor(p);
            }
        });
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.level().getGameTime() + p.getId()) % INTERVAL == 0) {
            werkBij(p);
        }
    }

    /** Works out this player's area now, tells the client when it changed; returns it. */
    public static String werkBij(ServerPlayer p) {
        String dwang = GEFORCEERD.get(p.getUUID());
        String nu = dwang != null ? dwang : gebied(p);
        String was = GEBIED.put(p.getUUID(), nu);
        if (!nu.equals(was)) {
            ModNetworking.sendTo(p, new SpAudioPayloads.Plek(nu));
            SpAudio.gebiedVeranderd(p, nu);
        }
        return nu;
    }

    /** The area the server told this player last ("" = none). */
    public static String gebiedNu(ServerPlayer p) {
        return GEBIED.getOrDefault(p.getUUID(), "");
    }

    /** (Dev command) force an area (null: back to what the world says). */
    static void forceer(ServerPlayer p, @Nullable String gebied) {
        if (gebied == null) {
            GEFORCEERD.remove(p.getUUID());
        } else {
            GEFORCEERD.put(p.getUUID(), gebied);
        }
        werkBij(p);
    }

    /** The area this player stands in now (see the class comment). */
    public static String gebied(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel s) || !GuheindeFeature.isGuheinde(s)) {
            return "";
        }
        BlockPos pos = p.blockPosition();
        boolean open = SpVerhaal.open(p) || Sluiers.passeert(p);
        boolean koepel = Plekken.bekend(s, SpVerhaal.BIKINIBODEM) && Leegtemist.inKoepel(s, pos);
        boolean doos = false, graaf = false;
        if (!koepel && Plekken.bekend(s, SpVerhaal.SCHELPENSTAD)) {
            BoundingBox b = Plekken.doos(s, SpVerhaal.SCHELPENSTAD);
            BlockPos o = Plekken.oorsprong(s, SpVerhaal.SCHELPENSTAD);
            if (b != null && o != null && b.inflatedBy(RAND).isInside(pos)) {
                doos = true;
                graaf = inLoopgraaf(pos.getX() - o.getX());
            }
        }
        return gebied(koepel, doos, graaf, open);
    }

    /** The pure rule. */
    public static String gebied(boolean inKoepel, boolean inSchelpenstad, boolean inLoopgraaf, boolean open) {
        if (!open) {
            return "";
        }
        if (inKoepel) {
            return BIKINIBODEM;
        }
        if (inSchelpenstad) {
            return inLoopgraaf ? LOOPGRAAF : SCHELPENSTAD;
        }
        return "";
    }

    /** Is this x (template coordinates of Schelpenstad) between the two rims of the loopgraaf? */
    static boolean inLoopgraaf(int lokaalX) {
        Plekken.Plek w = Plekken.plek(SpVerhaal.SCHELPENSTAD, "loopgraaf_rand_west");
        Plekken.Plek e = Plekken.plek(SpVerhaal.SCHELPENSTAD, "loopgraaf_rand_oost");
        if (w == null || e == null) {
            return false;
        }
        int a = Math.min(w.pos().getX(), e.pos().getX()), z = Math.max(w.pos().getX(), e.pos().getX());
        return lokaalX >= a && lokaalX <= z;
    }
}
