package nl.juiced.guhs.feature.bio;

import javax.annotation.Nullable;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.spelen.SpelGroepen;

/**
 * "Has this player ever been there?" for the dimensions and biomes of {@link BiomeLijst}. Kept in the visited list of
 * 1.3.1 ({@link SpelGroepen}: saved with the player, synced to the client at login and at every new entry), next to the
 * groups and the "s:" places: {@code "d:" + section id} for a dimension, {@code "b:" + biome id} for a biome. So both
 * questions work on both sides without a payload of their own.
 * <p>
 * A dimension counts from the moment the player is in it (login, arrival), or when the section's advancement proves an
 * older visit. A biome counts once the player stood in it (looked at every {@link SpelGroepen#CHECK_TICKS} ticks, only
 * in the listed dimensions: one biome lookup).
 */
public final class Bezocht {
    public static final String DIMENSIE = "d:", BIOME = "b:";

    static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                bijwerken(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                bijwerken(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                bijwerken(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % SpelGroepen.CHECK_TICKS == 7 && !p.isSpectator()) {
                kijk(p);
            }
        });
    }

    /** Has this player ever been in the dimension of this section (both sides)? A teaser section: never. */
    public static boolean dimensie(Player player, String sectie) {
        return SpelGroepen.bezocht(player, DIMENSIE + sectie);
    }

    /** Has this player ever been in this dimension (both sides)? A dimension without a section: false. */
    public static boolean dimensie(Player player, ResourceKey<Level> dimensie) {
        BiomeLijst.Sectie s = BiomeLijst.van(dimensie);
        return s != null && dimensie(player, s.id());
    }

    /** Has this player ever stood in this biome (guhs id without namespace; both sides)? */
    public static boolean biome(Player player, String biome) {
        return SpelGroepen.bezocht(player, BIOME + biome);
    }

    /** (Server) remembers the dimension of a section; true when it is new. Also for dev commands and tests. */
    public static boolean zetDimensie(ServerPlayer player, String sectie) {
        BiomeLijst.Sectie s = BiomeLijst.sectie(sectie);
        return s != null && !s.teaser() && SpelGroepen.bezoek(player, DIMENSIE + sectie);
    }

    /** (Server) remembers a biome; true when it is new. Also for dev commands and tests. */
    public static boolean zetBiome(ServerPlayer player, String biome) {
        return SpelGroepen.bezoek(player, BIOME + biome);
    }

    /** (Server) the dimension the player is in now, and every section an advancement proves; then the biome. */
    public static void bijwerken(ServerPlayer player) {
        for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
            if (s.teaser() || dimensie(player, s.id())) {
                continue;
            }
            if (player.level().dimension().equals(s.dimensie()) || behaald(player, s.bewijs())) {
                SpelGroepen.bezoek(player, DIMENSIE + s.id());
            }
        }
        kijk(player);
    }

    /** (Server) remembers the biome the player stands in, when its dimension has a section that lists it; the new id or null. */
    @Nullable
    public static String kijk(ServerPlayer player) {
        BiomeLijst.Sectie s = BiomeLijst.van(player.level().dimension());
        if (s == null) {
            return null;
        }
        var key = player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null);
        return key == null ? null : zie(player, s, key.identifier());
    }

    /** (Server; the rule of {@link #kijk}, apart for the tests) the player stands in this biome of this section's dimension. */
    @Nullable
    public static String zie(ServerPlayer player, BiomeLijst.Sectie sectie, Identifier biome) {
        if (sectie.teaser() || !Guhs.MODID.equals(biome.getNamespace()) || !BiomeLijst.heeft(sectie.id(), biome.getPath())) {
            return null;
        }
        SpelGroepen.bezoek(player, DIMENSIE + sectie.id());
        return SpelGroepen.bezoek(player, BIOME + biome.getPath()) ? biome.getPath() : null;
    }

    private static boolean behaald(ServerPlayer player, @Nullable Identifier advancement) {
        if (advancement == null) {
            return false;
        }
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(advancement);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private Bezocht() {
    }
}
