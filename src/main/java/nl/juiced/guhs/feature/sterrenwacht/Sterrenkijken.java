package nl.juiced.guhs.feature.sterrenwacht;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.evenementen.EvenementType;
import nl.juiced.guhs.feature.evenementen.Evenementen;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Looking through a guh telescope (the game "sterrenwacht"): at night the telescope shows a piece of sky with one guh
 * constellation hidden among the other stars; you connect its stars (client.TelescoopScherm) and the server checks
 * the lines. A new one goes into your sterrenatlas; every one gives wenssterren ({@link #wenssterren}). You can find
 * {@value #MAX_PER_NACHT} per night (then your eyes are tired, njeg); the three rare ones only show during a
 * sterrenregen. While the Knusfeest task "sterrenlantaarns" is open, Professor Sterretje gives three sterrenlantaarns.
 * <p>
 * Per player (GuhQuests.saved, {@value #KEY}): the night of the last constellation and how many that night.
 */
public final class Sterrenkijken {
    public static final String KEY = "guhs_sterrenwacht";
    /** How many constellations per night. */
    public static final int MAX_PER_NACHT = 3;
    /** How long a look through the telescope may last (then the screen closes by itself). */
    public static final int KIJK_TICKS = 20 * 60 * 5;
    /** How many sterrenlantaarns the Professor gives for the Knusfeest. */
    public static final int LANTAARNS = 3;

    // Knus counters
    public static final String GEVONDEN = "sterrenwacht.gevonden", STERRENBEELDEN = "sterrenwacht.sterrenbeelden", ATLAS = "sterrenwacht.atlas",
            ZELDZAAM = "sterrenwacht.zeldzaam", WENSEN = "sterrenwacht.wensen";
    public static final String VERZAMELING = "sterrenatlas";

    /** A look through a telescope: which constellation, where the telescope stands, until when. */
    public record Sessie(Sterrenbeeld beeld, BlockPos telescoop, long tot, int seed) {
    }

    private static final Map<UUID, Sessie> SESSIES = new ConcurrentHashMap<>();

    private Sterrenkijken() {
    }

    /** Is the player looking through a telescope right now (Minigames: one game at a time)? */
    public static boolean kijkt(ServerPlayer player) {
        Sessie s = SESSIES.get(player.getUUID());
        if (s == null) {
            return false;
        }
        if (s.tot() < player.level().getGameTime()) {
            SESSIES.remove(player.getUUID());
            return false;
        }
        return true;
    }

    @Nullable
    public static Sessie sessie(ServerPlayer player) {
        return kijkt(player) ? SESSIES.get(player.getUUID()) : null;
    }

    /** Is it dark enough for stars here (a sky, and the sun gone)? */
    public static boolean donker(Level level) {
        long t = Math.floorMod(level.getDayTime(), 24000L);
        return level.dimensionType().hasSkyLight() && t >= 12600 && t < 23400;
    }

    /** The night number (a night runs from noon to noon, so it doesn't change at midnight). */
    public static long nacht(Level level) {
        return Math.floorDiv(level.getDayTime() + 12000L, 24000L);
    }

    /** Is there a sterrenregen going on for this player? */
    public static boolean sterrenregen(ServerPlayer player) {
        var event = Evenementen.eventOf(player);
        return event != null && event.type == EvenementType.STERRENREGEN;
    }

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (!saved.contains(KEY)) {
            saved.put(KEY, new CompoundTag());
        }
        return saved.getCompoundOrEmpty(KEY);
    }

    /** How many constellations the player found this night. */
    public static int vannacht(ServerPlayer player) {
        CompoundTag d = data(player);
        return d.getLongOr("Nacht", 0L) == nacht(player.level()) ? d.getIntOr("Aantal", 0) : 0;
    }

    private static void telVannacht(ServerPlayer player) {
        CompoundTag d = data(player);
        long n = nacht(player.level());
        if (d.getLongOr("Nacht", 0L) != n) {
            d.putLong("Nacht", n);
            d.putInt("Aantal", 0);
        }
        d.putInt("Aantal", d.getIntOr("Aantal", 0) + 1);
    }

    /** (Tests) a new night for this player. */
    public static void nieuweNacht(ServerPlayer player) {
        data(player).putInt("Aantal", 0);
    }

    /**
     * Which constellation the sky shows: one you haven't found yet if there is one (during a sterrenregen the rare
     * ones first), otherwise any one you know (the rare ones only during a sterrenregen).
     */
    public static Sterrenbeeld kies(ServerPlayer player, boolean regen, RandomSource random) {
        Set<String> atlas = KnusVoortgang.ontdekt(player, VERZAMELING);
        List<Sterrenbeeld> nieuw = new ArrayList<>();
        if (regen) {
            for (Sterrenbeeld b : Sterrenbeeld.zeldzame()) {
                if (!atlas.contains(b.id())) {
                    nieuw.add(b);
                }
            }
        }
        if (nieuw.isEmpty()) {
            for (Sterrenbeeld b : Sterrenbeeld.gewoon()) {
                if (!atlas.contains(b.id())) {
                    nieuw.add(b);
                }
            }
        }
        if (!nieuw.isEmpty()) {
            return nieuw.get(random.nextInt(nieuw.size()));
        }
        List<Sterrenbeeld> alle = new ArrayList<>(Sterrenbeeld.gewoon());
        if (regen) {
            alle.addAll(Sterrenbeeld.zeldzame());
        }
        return alle.get(random.nextInt(alle.size()));
    }

    private static Component professor() {
        return Component.translatable("entity.guhs.guh_npc.sterrenkijkerguh");
    }

    /** Right-clicked a telescope: at night (and not too tired) the sky opens. */
    public static void kijk(ServerPlayer player, BlockPos telescoop) {
        if (Minigames.busyElsewhere(player, Minigames.STERRENWACHT)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.sterrenwacht.bezig").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        boolean regen = sterrenregen(player);
        if (!donker(player.level()) && !regen) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.sterrenwacht.overdag").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (vannacht(player) >= MAX_PER_NACHT) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.sterrenwacht.moe", MAX_PER_NACHT).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        start(player, telescoop, kies(player, regen, player.getRandom()));
    }

    /** Starts a look at this constellation (and opens the screen on the player's side). */
    public static Sessie start(ServerPlayer player, BlockPos telescoop, Sterrenbeeld beeld) {
        Sessie s = new Sessie(beeld, telescoop.immutable(), player.level().getGameTime() + KIJK_TICKS, player.getRandom().nextInt());
        SESSIES.put(player.getUUID(), s);
        Minigames.startKeeping(player);
        boolean nieuw = !KnusVoortgang.heeft(player, VERZAMELING, beeld.id());
        player.level().playSound(null, telescoop, SterrenwachtFeature.STER_KLIK.get(), SoundSource.BLOCKS, 0.8f, 0.8f);
        ModNetworking.sendTo(player, new SterrenwachtPayloads.Open(telescoop, beeld.ordinal(), s.seed(), nieuw, vannacht(player)));
        return s;
    }

    /** The screen was closed without finishing. */
    public static void stop(ServerPlayer player) {
        if (SESSIES.remove(player.getUUID()) != null) {
            Minigames.forget(player);
        }
    }

    /**
     * The player connected these lines (pairs of star indices) of the constellation the session shows: when they are
     * exactly its lines, the reward. Returns the number of wenssterren given (-1: not right / no session). The screen
     * only sends this when it thinks you're done and then closes, so a "no" always ends the look with a short message
     * (the look took too long, or the lines don't match the sky).
     */
    public static int klaar(ServerPlayer player, int beeldIndex, List<Integer> lijnen) {
        Sessie s = sessie(player);
        if (s == null) {
            SESSIES.remove(player.getUUID());
            Minigames.forget(player);
            melding(player, "gui.guhs.sterrenwacht.te_laat");
            return -1;
        }
        if (!klopt(s.beeld(), beeldIndex, lijnen)) {
            stop(player);
            melding(player, "gui.guhs.sterrenwacht.klopt_niet");
            return -1;
        }
        SESSIES.remove(player.getUUID());
        Minigames.forget(player);
        return beloon(player, s.beeld(), s.telescoop());
    }

    /** Are these lines (a0, b0, a1, b1, ...) exactly the lines of this constellation (any order, any direction)? */
    static boolean klopt(Sterrenbeeld beeld, int beeldIndex, List<Integer> lijnen) {
        if (beeld.ordinal() != beeldIndex || lijnen.size() % 2 != 0) {
            return false;
        }
        Set<Integer> getrokken = new HashSet<>();
        for (int i = 0; i + 1 < lijnen.size(); i += 2) {
            int a = lijnen.get(i), b = lijnen.get(i + 1);
            if (!beeld.isLijn(a, b)) {
                return false;
            }
            getrokken.add(Sterrenbeeld.sleutel(a, b));
        }
        return getrokken.equals(beeld.sleutels());
    }

    private static void melding(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /**
     * The Grote Knusfeest: a sterrenlantaarn came to a player some other way than the Professor's feest-gift (his shop,
     * the crafting recipe, a wish, or handed in at the Burgemeester). Every sterrenlantaarn is made with wenssterren,
     * which only a constellation gives, so while the task is asked it counts as made too (Knusfeest.gemaakt:
     * idempotent; after delivery it only grants the quest advancement that was skipped).
     */
    public static void lantaarnGekregen(ServerPlayer player) {
        if (Knusfeest.stap(player, Feesttaak.STERRENLANTAARNS) != null) {
            Knusfeest.gemaakt(player, Feesttaak.STERRENLANTAARNS);
        }
    }

    /**
     * Every reward rule gives one more than it would (2.7: "+1 per reward"): a known constellation 1 + 1, a new one
     * 2 + 1, a rare one 2 more.
     */
    public static int wenssterren(boolean nieuw, boolean zeldzaam) {
        return (nieuw ? 2 : 1) + (zeldzaam ? 2 : 0) + 1;
    }

    /** The reward for a connected constellation (the atlas, wenssterren, counters, the Knusfeest's lanterns). */
    public static int beloon(ServerPlayer player, Sterrenbeeld beeld, @Nullable BlockPos telescoop) {
        boolean nieuw = KnusVoortgang.ontdek(player, VERZAMELING, beeld.id());
        int sterren = wenssterren(nieuw, beeld.zeldzaam);
        telVannacht(player);
        Minigames.give(player, new ItemStack(SterrenwachtFeature.WENSSTER.get(), sterren));
        Buiten.zeg(player, professor(), nieuw ? "quest.guhs.sterrenwacht.gevonden_nieuw" : "quest.guhs.sterrenwacht.gevonden_weer", beeld.naam(), sterren);
        KnusVoortgang.tel(player, STERRENBEELDEN, 1);
        int atlas = KnusVoortgang.ontdekt(player, VERZAMELING).size();
        KnusVoortgang.hoogste(player, ATLAS, atlas);
        if (nieuw && beeld.zeldzaam) {
            KnusVoortgang.tel(player, ZELDZAAM, 1);
            GuhAdvancements.grant(player, "sterrenwacht_zeldzaam");
            Buiten.toon(player, "sterrenwacht_zeldzaam");
        }
        GuhAdvancements.grant(player, "sterrenwacht_sterrenbeeld");
        Buiten.toon(player, "sterrenwacht_sterrenbeeld");
        if (atlas >= 6) {
            GuhAdvancements.grant(player, "sterrenwacht_zes");
        }
        if (atlas >= Sterrenbeeld.values().length) {
            GuhAdvancements.grant(player, "sterrenwacht_atlas_vol");
            Buiten.toon(player, "sterrenwacht_atlas_vol");
        }
        // the Grote Knusfeest: three sterrenlantaarns for the feest
        if (Knusfeest.open(player, Feesttaak.STERRENLANTAARNS)
                && (Knusfeest.stap(player, Feesttaak.STERRENLANTAARNS) == Knusfeest.Stap.GEVRAAGD || lantaarnsBij(player) < LANTAARNS)) {
            Minigames.give(player, new ItemStack(SterrenwachtFeature.STERRENLANTAARN_ITEM.get(), LANTAARNS));
            Knusfeest.gemaakt(player, Feesttaak.STERRENLANTAARNS);
            Buiten.zeg(player, professor(), "quest.guhs.sterrenwacht.lantaarns", LANTAARNS);
        }
        ServerLevel level = player.level();
        BlockPos at = telescoop != null ? telescoop : player.blockPosition();
        level.sendParticles(SterrenwachtFeature.WENSSTER_DEELTJE.get(), at.getX() + 0.5, at.getY() + 1.5, at.getZ() + 0.5, 30, 0.8, 0.8, 0.8, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 1.5, at.getZ() + 0.5, 8, 0.5, 0.5, 0.5, 0.02);
        level.playSound(null, at, SterrenwachtFeature.STERRENBEELD_GELUID.get(), SoundSource.PLAYERS, 1f, nieuw ? 1.2f : 1f);
        return sterren;
    }

    private static int lantaarnsBij(ServerPlayer player) {
        int n = 0;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.is(SterrenwachtFeature.STERRENLANTAARN_ITEM.get())) {
                n += s.getCount();
            }
        }
        return n;
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIES.remove(event.getEntity().getUUID());
    }

    static void vergeetAlles() {
        SESSIES.clear();
    }
}
