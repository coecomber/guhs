package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * One line of the map's "Nieuws" feed: a lang key, its arguments and the real time (millis). An argument that starts with
 * {@code #} is itself a lang key (a building's name), the rest is shown as it is (village and owner names).
 */
public record KaartNieuws(String key, List<String> args, long tijd) {
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("K", key);
        t.putLong("T", tijd);
        ListTag l = new ListTag();
        for (String a : args) {
            l.add(StringTag.valueOf(a));
        }
        t.put("A", l);
        return t;
    }

    public static KaartNieuws load(CompoundTag t) {
        List<String> a = new ArrayList<>();
        for (Tag e : t.getListOrEmpty("A")) {
            e.asString().ifPresent(a::add);
        }
        return new KaartNieuws(t.getStringOr("K", ""), a, t.getLongOr("T", 0L));
    }

    void write(FriendlyByteBuf buf) {
        buf.writeUtf(key, 128);
        buf.writeVarInt(args.size());
        for (String a : args) {
            buf.writeUtf(a, 128);
        }
        buf.writeLong(tijd);
    }

    static KaartNieuws read(FriendlyByteBuf buf) {
        String k = buf.readUtf(128);
        int n = Math.min(8, buf.readVarInt());
        List<String> a = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            a.add(buf.readUtf(128));
        }
        return new KaartNieuws(k, a, buf.readLong());
    }
}
