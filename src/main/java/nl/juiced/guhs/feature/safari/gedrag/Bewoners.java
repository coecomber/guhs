package nl.juiced.guhs.feature.safari.gedrag;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import org.slf4j.Logger;

/**
 * Round 3 (ronde 46): the "bekende bewoners", a few named park animals per zone ({@code data/guhs/safari/bewoners.json},
 * written by tools/features/safari_dieren.py). When the park is filled, the first grown-up of the first home of that species
 * in that zone gets the name (a visible name tag, {@link SafariDier#zetBewoner}). Photographing it ticks it off in the
 * Safari tab of the Guhdex (player flag {@code bewoner:<id>}).
 */
public final class Bewoners {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String VLAG = "bewoner:";

    public record Bewoner(String id, String soort, String zone, String naam) {
    }

    private static volatile List<Bewoner> lijst;

    public static List<Bewoner> get() {
        List<Bewoner> l = lijst;
        if (l == null) {
            synchronized (Bewoners.class) {
                if (lijst == null) {
                    lijst = laad();
                }
                l = lijst;
            }
        }
        return l;
    }

    private static List<Bewoner> laad() {
        List<Bewoner> uit = new ArrayList<>();
        try (InputStream in = Bewoners.class.getResourceAsStream("/data/guhs/safari/bewoners.json")) {
            if (in != null) {
                try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    for (JsonElement e : JsonParser.parseReader(r).getAsJsonArray()) {
                        JsonObject o = e.getAsJsonObject();
                        uit.add(new Bewoner(o.get("id").getAsString(), o.get("soort").getAsString(), o.get("zone").getAsString(),
                                o.get("naam").getAsString()));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Safari: could not read bewoners.json", e);
        }
        return List.copyOf(uit);
    }

    /** The resident for the {@code index}-th animal of home {@code plek} (null: an ordinary animal). */
    @Nullable
    public static Bewoner voor(ParkData.Plek plek, int index) {
        if (index != 0) {
            return null;
        }
        ParkData d = ParkData.get();
        Set<String> gehad = new HashSet<>();
        for (Bewoner b : get()) {
            if (!b.soort().equals(plek.soort()) || gehad.contains(b.id())) {
                continue;
            }
            // the first home (lowest id) of this species whose centre lies in this zone
            for (ParkData.Plek p : d.plekken) {
                if (p.soort().equals(b.soort()) && d.zoneNaam(d.zone(p.centrum().getX(), p.centrum().getZ())).equals(b.zone())) {
                    if (p.id() == plek.id()) {
                        return b;
                    }
                    gehad.add(b.id());
                    break;
                }
            }
        }
        return null;
    }

    /** (Camera) the player photographed this animal: a resident gets ticked off, with a little cheer the first time. */
    public static void gefotografeerd(ServerPlayer p, @Nullable SafariDier dier) {
        if (dier == null || dier.bewoner().isEmpty()) {
            return;
        }
        String vlag = VLAG + dier.bewoner();
        if (SafariSpeler.vlag(p, vlag)) {
            return;
        }
        SafariSpeler.zetVlag(p, vlag, true);
        p.sendSystemMessage(Component.translatable("gui.guhs.safari.bewoner.gefotografeerd", dier.getCustomName() == null
                ? Component.literal(dier.bewoner()) : dier.getCustomName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    private Bewoners() {
    }
}
