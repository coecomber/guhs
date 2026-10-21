package nl.juiced.guhs.feature.safari.spullen.client;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.spullen.SafariGeluid;

/**
 * The ambience of Safaripark Vadse Bergen (client only, S6): every zone its own soft loop (savanne crickets, jungle calls,
 * icy wind...), chosen by the biome under the player ({@code guhs:safari_<zone>}), crossfading when you walk into another
 * zone. In the entreedorp the cheerful lobby tune plays as well. Outside the safari dimension everything fades out.
 * <p>
 * F12: inside the Kriebelhuis (its halls from {@code kriebelhuis.json} "secties") the hall's own loop plays instead, and the
 * lobby tune stops: insect hum and drips in the entreehal, terrarium and Kriebelzaal, crickets in the Nachtzaal, deep bubbling
 * in the Diepzeezaal and around the big tank.
 */
public final class SafariSfeer {
    private static final float SFEER_VOLUME = 0.55f, MUZIEK_VOLUME = 0.35f;
    private static final Map<SoundEvent, Lus> LUSSEN = new HashMap<>();

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SafariSfeer::onTick);
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        String zone = null;
        if (level != null && mc.player != null && level.dimension() == Safari.DIM) {
            zone = zone(level.getBiome(mc.player.blockPosition()));
            String hal = kriebelhal(mc.player.blockPosition());
            if (hal != null) {
                zone = hal;
            }
        }
        SoundEvent sfeer = zone == null ? null : SafariGeluid.SFEER.containsKey(zone) ? SafariGeluid.SFEER.get(zone).get() : null;
        SoundEvent muziek = "entree".equals(zone) ? SafariGeluid.MUZIEK_LOBBY.get() : null;
        if (mc.isPaused()) {
            return;
        }
        for (Lus l : LUSSEN.values()) {
            l.doel = 0f;
        }
        zet(mc, sfeer, SFEER_VOLUME);
        zet(mc, muziek, MUZIEK_VOLUME);
        LUSSEN.values().removeIf(l -> l.isStopped());
    }

    private static void zet(Minecraft mc, SoundEvent event, float volume) {
        if (event == null) {
            return;
        }
        Lus l = LUSSEN.get(event);
        if (l == null || l.isStopped() || !mc.getSoundManager().isActive(l)) {
            l = new Lus(event);
            LUSSEN.put(event, l);
            mc.getSoundManager().play(l);
        }
        l.doel = volume;
    }

    private record Hal(String sfeer, int x1, int y1, int z1, int x2, int y2, int z2) {
        boolean bevat(BlockPos p) {
            return p.getX() >= x1 && p.getX() <= x2 && p.getY() >= y1 && p.getY() <= y2 && p.getZ() >= z1 && p.getZ() <= z2;
        }
    }

    private static volatile List<Hal> hallen;

    /** The Kriebelhuis hall the player stands in, as an ambience zone (null: not inside). */
    private static String kriebelhal(BlockPos p) {
        List<Hal> h = hallen;
        if (h == null) {
            h = new ArrayList<>();
            try (InputStream in = SafariSfeer.class.getResourceAsStream("/data/guhs/safari/kriebelhuis.json")) {
                if (in != null) {
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (o.has("secties")) {
                        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("secties").entrySet()) {
                            String sfeer = switch (e.getKey()) {
                                case "entreehal", "terrarium", "kriebelzaal" -> "kriebelhuis_kriebel";
                                case "nachtzaal" -> "kriebelhuis_nacht";
                                case "diepzeezaal", "aquarium_buiten" -> "kriebelhuis_diepzee";
                                default -> null;
                            };
                            var a = e.getValue().getAsJsonArray();
                            if (sfeer != null && a.size() == 6) {
                                h.add(new Hal(sfeer, a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsInt(),
                                        a.get(4).getAsInt(), a.get(5).getAsInt()));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // no halls then: the lobby's own ambience
            }
            hallen = h;
        }
        for (Hal hal : h) {
            if (hal.bevat(p)) {
                return hal.sfeer();
            }
        }
        return null;
    }

    private static String zone(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.identifier().getPath()).filter(p -> p.startsWith("safari_")).map(p -> p.substring(7)).orElse(null);
    }

    /** A looping, non-positional ambience sound that eases its volume towards {@link #doel} and stops when silent. */
    private static final class Lus extends AbstractTickableSoundInstance {
        float doel = 0f;

        Lus(SoundEvent event) {
            super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            volume = Mth.lerp(0.04f, volume, doel);
            if (doel == 0f && volume < 0.01f) {
                stop();
            }
        }
    }

    private SafariSfeer() {
    }
}
