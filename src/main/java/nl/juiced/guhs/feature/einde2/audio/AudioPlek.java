package nl.juiced.guhs.feature.einde2.audio;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.network.ModNetworking;

/**
 * einde2, slice audio: which music area a player is in (CONTRACT_E2_B 6.2), worked out by the server every
 * {@link #INTERVAL} ticks and sent to the player's client only when it changes ({@link E2AudioPayloads.Plek}).
 * <ul>
 *   <li>the Geestenwereld (the dimension): {@code geest};</li>
 *   <li>in the Guheinde, inside the box of a story structure (grown {@link #RAND_XZ} sideways, {@link #RAND_Y} up and down)
 *       that is open for the player ({@code E2Verhaal.open}: behind a closed veil there is no music): its element;</li>
 *   <li>the same for the side places with an element ({@code Einde2Ids.ELEMENT_VAN}: the Western Air Temple is lucht, ...);</li>
 *   <li>anywhere else: "" (vanilla music as always).</li>
 * </ul>
 */
public final class AudioPlek {
    public static final int INTERVAL = 20;
    public static final int RAND_XZ = 16, RAND_Y = 24;
    private static final Map<UUID, String> GEBIED = new ConcurrentHashMap<>();
    /** (Dev command) a forced area per player ("" = none, absent = as the world says). */
    private static final Map<UUID, String> GEFORCEERD = new ConcurrentHashMap<>();
    /** (Game tests) boxes that count as these structures, instead of the real ones (structure -> box). */
    static final Map<String, BoundingBox> TEST_DOZEN = new ConcurrentHashMap<>();

    private AudioPlek() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(AudioPlek::onPlayerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            GEBIED.remove(event.getEntity().getUUID());
            GEFORCEERD.remove(event.getEntity().getUUID());
            E2Audio.vergeet(event.getEntity().getUUID());
        });
        // a fresh connection starts without an area on the client: tell it again
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> GEBIED.remove(event.getEntity().getUUID()));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % INTERVAL != 0) {
            return;
        }
        werkBij(p);
    }

    /** Works out the area of this player now and tells the client when it changed; returns the area. */
    public static String werkBij(ServerPlayer p) {
        String dwang = GEFORCEERD.get(p.getUUID());
        String nu = dwang != null ? dwang : gebied(p);
        String was = GEBIED.put(p.getUUID(), nu);
        if (!nu.equals(was)) {
            ModNetworking.sendTo(p, new E2AudioPayloads.Plek(nu));
        }
        return nu;
    }

    /** The area the server told this player last ("" = none). */
    public static String gebiedNu(ServerPlayer p) {
        return GEBIED.getOrDefault(p.getUUID(), "");
    }

    /** (Dev command) force an area for this player (null: back to what the world says). */
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
        if (Geestenwereld.in(p)) {
            return AudioNamen.GEESTENWERELD;
        }
        Map<String, BoundingBox> dozen = dozen(p.level());
        if (dozen.isEmpty()) {
            return "";
        }
        return gebied(dozen, p.blockPosition(), s -> E2Verhaal.open(p, s));
    }

    /** The pure rule: the area of the first box (grown by the margins) around {@code pos} whose structure is open. */
    public static String gebied(Map<String, BoundingBox> dozen, BlockPos pos, Predicate<String> open) {
        for (Map.Entry<String, BoundingBox> e : dozen.entrySet()) {
            BoundingBox b = e.getValue();
            if (pos.getX() >= b.minX() - RAND_XZ && pos.getX() <= b.maxX() + RAND_XZ && pos.getZ() >= b.minZ() - RAND_XZ
                    && pos.getZ() <= b.maxZ() + RAND_XZ && pos.getY() >= b.minY() - RAND_Y && pos.getY() <= b.maxY() + RAND_Y) {
                String gebied = gebiedVan(e.getKey());
                if (gebied != null && (!AudioNamen.GEBIED_VAN_VERHAAL.containsKey(e.getKey()) || open.test(e.getKey()))) {
                    return gebied;
                }
            }
        }
        return "";
    }

    /** The music area of a structure (story structure or side place with an element), or null. */
    @Nullable
    public static String gebiedVan(String structuur) {
        String g = AudioNamen.GEBIED_VAN_VERHAAL.get(structuur);
        if (g != null) {
            return g;
        }
        Einde2Ids.Element e = Einde2Ids.ELEMENT_VAN.get(structuur);
        return e == null ? null : AudioNamen.gebied(e);
    }

    /** The boxes of the places with music in this level (the Guheinde only; places whose spot is not worked out yet are left out). */
    private static Map<String, BoundingBox> dozen(net.minecraft.world.level.Level level) {
        Map<String, BoundingBox> uit = new LinkedHashMap<>();
        if (!TEST_DOZEN.isEmpty()) {
            uit.putAll(TEST_DOZEN);
            return uit;
        }
        if (!(level instanceof ServerLevel s) || !GuheindeFeature.isGuheinde(s)) {
            return uit;
        }
        for (String structuur : Einde2Ids.VERHAAL_STRUCTUREN) {
            voegToe(s, structuur, uit);
        }
        for (String structuur : Einde2Ids.ELEMENT_VAN.keySet()) {
            voegToe(s, structuur, uit);
        }
        return uit;
    }

    private static void voegToe(ServerLevel s, String structuur, Map<String, BoundingBox> uit) {
        if (Plekken.bekend(s, structuur)) {
            BoundingBox b = Plekken.doos(s, structuur);
            if (b != null) {
                uit.put(structuur, b);
            }
        }
    }
}
