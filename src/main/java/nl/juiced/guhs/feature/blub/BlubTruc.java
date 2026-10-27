package nl.juiced.guhs.feature.blub;

import java.util.Locale;
import java.util.function.BiConsumer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.8.0: the Blub-guh's tricks and emotes, all behind its "Trucjes" button (client.BlubTrucScreen, like the guh's emote
 * picker). One list: a new trick is one line here (its lang keys gui.guhs.blubguh.truc.&lt;id&gt; and .&lt;id&gt;.tooltip in
 * tools/features/blub.py). Every trick is a guhs:blub_actie TRUC with its ordinal (append only, the order is the
 * screen's order).
 */
public enum BlubTruc {
    ZITTEN((b, p) -> b.truc(p, 0)),
    RONDJE((b, p) -> b.truc(p, 1)),
    DOOD((b, p) -> b.truc(p, 2)),
    SPRONG((b, p) -> b.truc(p, 3)),
    HUPSEN((b, p) -> b.hupsen(p)),
    SLAAPLIEDJE((b, p) -> b.slaapliedje(p)),
    VERSTOPPERTJE((b, p) -> b.verstoppertje(p)),
    SQUISH((b, p) -> b.squish(p)),
    OOR_BLIJ((b, p) -> b.oortjes(BlubGuhEntity.RECHTOP)),
    OOR_PLAT((b, p) -> b.oortjes(BlubGuhEntity.PLAT)),
    OOR_KRUL((b, p) -> b.oortjes(BlubGuhEntity.KRUL));

    private final BiConsumer<BlubGuhEntity, ServerPlayer> doe;

    BlubTruc(BiConsumer<BlubGuhEntity, ServerPlayer> doe) {
        this.doe = doe;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component naam() {
        return Component.translatable("gui.guhs.blubguh.truc." + id());
    }

    public Component uitleg() {
        return Component.translatable("gui.guhs.blubguh.truc." + id() + ".tooltip");
    }

    public void doe(BlubGuhEntity blub, ServerPlayer player) {
        doe.accept(blub, player);
    }

    public static BlubTruc byIndex(int i) {
        BlubTruc[] all = values();
        return all[Math.floorMod(i, all.length)];
    }
}
