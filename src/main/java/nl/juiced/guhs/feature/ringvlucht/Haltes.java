package nl.juiced.guhs.feature.ringvlucht;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * 1.4.2 (rookguh): the stops of the Rookguh-vlucht, in story order. The single source is tools/features/ring_vlucht.py
 * (HALTES): it writes {@code data/guhs/ringvlucht/haltes.json}, read here straight from the jar (both sides, like
 * {@code ringh4.Plekken}), and checks every spot against the generated templates.
 * <p>
 * A stop belongs to a questline: the END of that line puts a Rookguh at the stop's exit ({@link #klaar}), and the chapter
 * of the line decides whether the player has REACHED the stop ({@link #bereikt}: the same rule as Guhdalfs sluier, so the
 * Rookguh never flies anybody to a place that is still hidden for them).
 */
public final class Haltes {
    private static final String PAD = "/data/guhs/ringvlucht/haltes.json";
    /** The Gouw of chapter 1 (the player's own Guhdalf) and the landing of the elf boats (the end of the Guhduin). */
    public static final String GOUW = "gouw", AANLEG = "aanleg";

    /** An entrance: the cell you stand in there (template coordinates) and the way out of the structure (one of the four directions). */
    public record Ingang(BlockPos plek, int dx, int dz) {
    }

    /**
     * A stop. {@code rookguh}: where its Rookguh hovers (template coordinates of the structure's own template; for a
     * guhs:burcht of the whole build), null: none of its own. {@code afstand}: how far in front of the entrance the player is
     * put down (0: at the entrance itself).
     */
    public record Halte(String id, String lijn, String structuur, @Nullable BlockPos rookguh, List<Ingang> ingangen, int afstand) {
        /** The chapter of its questline: 1..6, or {@link Ring#SAUSUMAN_NR} for the Toren van Sausuman. */
        public int hoofdstuk() {
            return lijn.equals("ring_sausuman") ? Ring.SAUSUMAN_NR : lijn.charAt(lijn.length() - 1) - '0';
        }

        public Verhaallijn verhaallijn() {
            return Ring.lijn(hoofdstuk());
        }

        public ResourceKey<Level> dimensie() {
            return Ring.dimensie(structuur);
        }

        /** Its name on the Rookguh's screen and in the chat. */
        public String naamKey() {
            return "gui.guhs.ringvlucht.plek." + id;
        }
    }

    /** Every stop, in story order. */
    public static final List<Halte> ALLE;
    /** The Rookguh at a camp of Guhdalf (camp-local coordinates of template ringh1_kamp; in a Knabbelgouw from its camp corner). */
    public static final BlockPos KAMP_ROOKGUH;

    static {
        try (InputStream in = Haltes.class.getResourceAsStream(PAD)) {
            if (in == null) {
                throw new IllegalStateException("ringvlucht: " + PAD + " is missing (run tools/make_resources.py)");
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Halte> alle = new ArrayList<>();
            for (JsonElement e : root.getAsJsonArray("haltes")) {
                JsonObject o = e.getAsJsonObject();
                List<Ingang> ingangen = new ArrayList<>();
                for (JsonElement i : o.getAsJsonArray("ingangen")) {
                    JsonObject io = i.getAsJsonObject();
                    JsonArray r = io.getAsJsonArray("richting");
                    ingangen.add(new Ingang(blok(io.getAsJsonArray("plek")), r.get(0).getAsInt(), r.get(1).getAsInt()));
                }
                JsonElement rook = o.get("rookguh");
                alle.add(new Halte(o.get("id").getAsString(), o.get("lijn").getAsString(), o.get("structuur").getAsString(),
                        rook == null || rook.isJsonNull() ? null : blok(rook.getAsJsonArray()), List.copyOf(ingangen), o.get("afstand").getAsInt()));
            }
            ALLE = List.copyOf(alle);
            KAMP_ROOKGUH = blok(root.getAsJsonArray("kamp_rookguh"));
        } catch (IOException e) {
            throw new IllegalStateException("ringvlucht: can't read " + PAD, e);
        }
    }

    private static BlockPos blok(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    /** The stop with this id (null: none). */
    @Nullable
    public static Halte van(@Nullable String id) {
        for (Halte h : ALLE) {
            if (h.id().equals(id)) {
                return h;
            }
        }
        return null;
    }

    /**
     * Did this player's story reach this stop? The Gouw: once the story began (they met Guhdalf there). The landing of the
     * elf boats: once chapter 4 is done (the boat brought them there). Every other stop: once its chapter is open for them
     * ({@link Ring#bereikt}, the rule of Guhdalfs sluier), and its sluier says so too.
     */
    public static boolean bereikt(ServerPlayer p, Halte h) {
        if (h.id().equals(GOUW)) {
            return Ring.begonnen(p);
        }
        if (h.id().equals(AANLEG)) {
            return Ring.lijn(4).klaar(p) && Sluiers.open(p, h.structuur());
        }
        return Ring.bereikt(p, h.hoofdstuk()) && Sluiers.open(p, h.structuur());
    }

    /** Is the story part of this stop done for this player (then its Rookguh is there for them)? */
    public static boolean klaar(ServerPlayer p, Halte h) {
        return h.verhaallijn().klaar(p);
    }

    private Haltes() {
    }
}
