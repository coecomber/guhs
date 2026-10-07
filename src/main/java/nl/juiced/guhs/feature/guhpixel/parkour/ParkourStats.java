package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.guhpixel.Bouwer;
import nl.juiced.guhs.feature.guhpixel.GidsSectie;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.taal.Tekst;

/**
 * What a player's guhs did on the Guh-parkour (per player, PxData slice "parkour"): laps in total, routes that saw a
 * finished lap, the longest route, and per guh its laps and best lap time. Shown in the Guhdex tab "Guhpixel &amp; uitjes"
 * (also before the Guhpixel unlock: the Guh-parkour has nothing to do with the dimension).
 */
public final class ParkourStats {
    private static final String SLICE = "parkour";
    /** At most this many guhs are remembered per player (the slowest of the least active goes when it is full). */
    private static final int MAX_GUHS = 24;

    /** A lap time for people: ticks as seconds with two decimals ("12,35 s" in Dutch through the lang key). */
    public static Component tijd(int ticks) {
        return Component.translatable("gui.guhs.guhparkour.tijd", String.format(Locale.ROOT, "%d", ticks / 20),
                String.format(Locale.ROOT, "%02d", (ticks % 20) * 5));
    }

    static void rondje(ServerPlayer baas, GuhEntity guh, int ticks, int stukken, boolean nieuweRoute) {
        CompoundTag d = PxData.deel(baas, SLICE);
        d.putInt("Rondjes", d.getIntOr("Rondjes", 0) + 1);
        if (nieuweRoute) {
            d.putInt("Routes", d.getIntOr("Routes", 0) + 1);
        }
        d.putInt("Langste", Math.max(d.getIntOr("Langste", 0), stukken));
        CompoundTag guhs = PxData.sub(d, "Guhs");
        String key = guh.getUUID().toString();
        CompoundTag g = guhs.getCompoundOrEmpty(key);
        g.store("Id", UUIDUtil.CODEC, guh.getUUID());
        Tekst.put(g, "Naam", guh.getName().copy());
        g.putInt("Rondjes", g.getIntOr("Rondjes", 0) + 1);
        int beste = g.getIntOr("Beste", 0);
        g.putInt("Beste", beste <= 0 ? ticks : Math.min(beste, ticks));
        guhs.put(key, g);
        if (guhs.size() > MAX_GUHS) {
            guhs.keySet().stream().filter(k -> !k.equals(key)).min(Comparator.comparingInt(k -> guhs.getCompoundOrEmpty(k).getIntOr("Rondjes", 0)))
                    .ifPresent(guhs::remove);
        }
        PxData.vuil(baas.level().getServer());
    }

    public static int rondjes(ServerPlayer p) {
        return PxData.deel(p, SLICE).getIntOr("Rondjes", 0);
    }

    public static int routes(ServerPlayer p) {
        return PxData.deel(p, SLICE).getIntOr("Routes", 0);
    }

    public static int langste(ServerPlayer p) {
        return PxData.deel(p, SLICE).getIntOr("Langste", 0);
    }

    /** The best lap of this guh for this player (ticks; 0: none yet). */
    public static int beste(ServerPlayer p, UUID guh) {
        return PxData.deel(p, SLICE).getCompoundOrEmpty("Guhs").getCompoundOrEmpty(guh.toString()).getIntOr("Beste", 0);
    }

    static final GidsSectie SECTIE = new GidsSectie() {
        @Override
        public String id() {
            return "guhparkour";
        }

        @Override
        public int volgorde() {
            return 90;
        }

        @Override
        public boolean zichtbaar(ServerPlayer p) {
            return true;
        }

        @Override
        public boolean zonderToegang() {
            return true;
        }

        @Override
        public void vul(ServerPlayer p, Bouwer b) {
            CompoundTag d = PxData.deel(p, SLICE);
            b.kop(Component.translatable("gui.guhs.guhparkour.gids.kop"));
            b.regel(Component.translatable("gui.guhs.guhparkour.gids.uitleg"));
            b.stat(Component.translatable("gui.guhs.guhparkour.gids.routes"), Component.literal(Integer.toString(d.getIntOr("Routes", 0))));
            b.stat(Component.translatable("gui.guhs.guhparkour.gids.rondjes"), Component.literal(Integer.toString(d.getIntOr("Rondjes", 0))));
            b.voortgang(Component.translatable("gui.guhs.guhparkour.gids.langste"), Math.min(Routes.MAX_STUKKEN, d.getIntOr("Langste", 0)), Routes.MAX_STUKKEN);
            CompoundTag guhs = d.getCompoundOrEmpty("Guhs");
            List<CompoundTag> lijst = new ArrayList<>();
            for (String k : guhs.keySet()) {
                lijst.add(guhs.getCompoundOrEmpty(k));
            }
            lijst.sort(Comparator.comparingInt(t -> t.getIntOr("Beste", Integer.MAX_VALUE)));
            if (lijst.isEmpty()) {
                b.regel(Component.translatable("gui.guhs.guhparkour.gids.nog_niets"));
                return;
            }
            b.regel(Component.translatable("gui.guhs.guhparkour.gids.beste"));
            for (int i = 0; i < Math.min(8, lijst.size()); i++) {
                CompoundTag g = lijst.get(i);
                b.stat(Tekst.get(g, "Naam"), Component.translatable("gui.guhs.guhparkour.gids.guh", tijd(g.getIntOr("Beste", 0)), g.getIntOr("Rondjes", 0)));
            }
        }
    };

    private ParkourStats() {
    }
}
