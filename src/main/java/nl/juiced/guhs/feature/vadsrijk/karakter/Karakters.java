package nl.juiced.guhs.feature.vadsrijk.karakter;

import java.util.Map;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Inventaris;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * T-KARAKTER: the server operations on characters: create ({@link #maak}), switch ({@link #wissel}), rename, lower the mode,
 * retire ({@link #pensioen}). Everything runs on the server thread; a switch holds a per-player lock so nothing can happen in
 * between (no dupes through the account-wide stores).
 * <p>Switching (only in a town, at a campfire, right after entering, or outside the Vadsrijk; never in a boss arena or a scene):
 * <ol>
 *   <li>{@link Karakter#VOOR_WISSEL} and (inside) kern's {@code Haken.BINNEN false}: trades cancelled, mount + pet home;</li>
 *   <li>the live Vadsrijk inventory goes into the old character's stash (kern's own never-lose swap, {@link Inventaris});</li>
 *   <li>the whole SpelerData minus the account part is saved into the old slot, with where the knight stood;</li>
 *   <li>the new slot's part + the account part are loaded; its inventory comes out; it stands where it was left
 *   (a new knight: at the Knuffelkamp arrival, so the story starts fresh);</li>
 *   <li>{@code Haken.BINNEN true} + {@link Karakter#GEWISSELD}.</li>
 * </ol>
 */
public final class Karakters {
    public static final int NAAM_MAX = 16;
    /** Ticks after entering the dimension in which you may switch anywhere (the entrance screen). */
    public static final int AANKOMST_TICKS = 20 * 60;
    /** Blocks around a campfire where you may switch. */
    public static final int KAMPVUUR_STRAAL = 8;

    private static final Map<UUID, Long> BINNENGEKOMEN = new ConcurrentHashMap<>();
    private static final java.util.Set<UUID> BEZIG = ConcurrentHashMap.newKeySet();

    private Karakters() {
    }

    /** Is a switch running for this player right now (kern's BINNEN hooks fire then too)? */
    public static boolean bezig(ServerPlayer p) {
        return BEZIG.contains(p.getUUID());
    }

    static void binnengekomen(ServerPlayer p) {
        BINNENGEKOMEN.put(p.getUUID(), p.level().getGameTime());
    }

    static void vergeet(UUID id) {
        BINNENGEKOMEN.remove(id);
        BEZIG.remove(id);
    }

    // ---- where may you switch -------------------------------------------------------------------------------------------------

    /** Null = you may switch here; else the lang key of the reason why not. */
    @Nullable
    public static String waaromNiet(ServerPlayer p) {
        if (!Vadsrijk.in(p)) {
            return null;   // (outside: you choose before going in)
        }
        if (RijkScenes.bezig(p)) {
            return "vadsrijk.karakter.niet.scene";
        }
        if (Arenas.op(p.getX(), p.getY(), p.getZ()) != null) {
            return "vadsrijk.karakter.niet.arena";
        }
        Long in = BINNENGEKOMEN.get(p.getUUID());
        if (in != null && p.level().getGameTime() - in <= AANKOMST_TICKS) {
            return null;
        }
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
        if (g != null && g.stad()) {
            return null;
        }
        if (bijKampvuur(p)) {
            return null;
        }
        return "vadsrijk.karakter.niet.plek";
    }

    static boolean bijKampvuur(ServerPlayer p) {
        BlockPos c = p.blockPosition();
        for (BlockPos b : BlockPos.betweenClosed(c.offset(-KAMPVUUR_STRAAL, -3, -KAMPVUUR_STRAAL), c.offset(KAMPVUUR_STRAAL, 3, KAMPVUUR_STRAAL))) {
            BlockState s = p.level().getBlockState(b);
            if (s.getBlock() instanceof CampfireBlock) {
                return true;
            }
        }
        return false;
    }

    // ---- create / rename / mode -----------------------------------------------------------------------------------------------

    /** A clean name: trimmed, max {@link #NAAM_MAX}, letters/digits/space/-/_/' only; null when nothing is left. */
    @Nullable
    public static String schoon(String naam) {
        StringBuilder b = new StringBuilder();
        for (char c : naam.trim().toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_' || c == '\'') {
                b.append(c);
            }
        }
        String s = b.toString().replaceAll(" +", " ").trim();
        if (s.length() > NAAM_MAX) {
            s = s.substring(0, NAAM_MAX).trim();
        }
        return s.isEmpty() ? null : s;
    }

    /** The first free slot, or -1. */
    public static int vrijSlot(ServerPlayer p) {
        for (int i = 0; i < Karakter.SLOTS; i++) {
            if (!Karakter.bestaat(p, i)) {
                return i;
            }
        }
        return -1;
    }

    /** Creates a character in {@code slot} (must be free). Does not switch. Returns false (+ message) when it can't. */
    public static boolean maak(ServerPlayer p, int slot, String naam, Modus modus) {
        String n = schoon(naam);
        if (slot < 0 || slot >= Karakter.SLOTS || Karakter.bestaat(p, slot)) {
            meld(p, "vadsrijk.karakter.fout.slot", ChatFormatting.RED);
            return false;
        }
        if (n == null) {
            meld(p, "vadsrijk.karakter.fout.naam", ChatFormatting.RED);
            return false;
        }
        CompoundTag s = new CompoundTag();
        s.putString("naam", n);
        s.putString("modus", modus.id);
        s.putString("begin", modus.id);
        s.putLong("gemaakt", System.currentTimeMillis());
        s.putBoolean("nieuw", true);
        Karakter.lijst(p).put("s" + slot, s);
        p.sendSystemMessage(Component.translatable("vadsrijk.karakter.gemaakt", Component.literal(n).withStyle(ChatFormatting.GOLD),
                Component.translatable(modus.naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
        Badges.ververs(p);
        return true;
    }

    public static boolean hernoem(ServerPlayer p, int slot, String naam) {
        String n = schoon(naam);
        if (!Karakter.bestaat(p, slot) || n == null) {
            meld(p, "vadsrijk.karakter.fout.naam", ChatFormatting.RED);
            return false;
        }
        Karakter.info(p, slot).putString("naam", n);
        Badges.ververs(p);
        return true;
    }

    /** Lowers the mode of a character (only lower: Eénvads -> Normaal -> Knuffelmodus). */
    public static boolean zetModus(ServerPlayer p, int slot, Modus naar, boolean dev) {
        if (!Karakter.bestaat(p, slot)) {
            meld(p, "vadsrijk.karakter.fout.slot", ChatFormatting.RED);
            return false;
        }
        CompoundTag s = Karakter.info(p, slot);
        Modus nu = Modus.van(s.getStringOr("modus", Modus.NORMAAL.id));
        if (nu == naar || (!dev && !nu.magNaar(naar))) {
            meld(p, "vadsrijk.karakter.fout.modus", ChatFormatting.RED);
            return false;
        }
        s.putString("modus", naar.id);
        if (dev && naar.zwaarte > nu.zwaarte) {
            s.putString("begin", naar.id);
            s.remove("pensioen");
        }
        p.sendSystemMessage(Component.translatable("vadsrijk.karakter.modus_gezet", Component.literal(s.getStringOr("naam", "?")),
                Component.translatable(naar.naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
        Badges.ververs(p);
        return true;
    }

    // ---- switching ------------------------------------------------------------------------------------------------------------

    /**
     * Switches to {@code naar}. {@code overal} = ignore the place rule (dev). Returns false (+ message) when it can't.
     */
    public static boolean wissel(ServerPlayer p, int naar, boolean overal) {
        CompoundTag lijst = Karakter.lijst(p);
        int van = lijst.getIntOr("actief", 0);
        if (naar == van) {
            meld(p, "vadsrijk.karakter.fout.al", ChatFormatting.YELLOW);
            return false;
        }
        if (!Karakter.bestaat(p, naar)) {
            meld(p, "vadsrijk.karakter.fout.slot", ChatFormatting.RED);
            return false;
        }
        String niet = overal ? null : waaromNiet(p);
        if (niet != null) {
            meld(p, niet, ChatFormatting.RED);
            return false;
        }
        if (!BEZIG.add(p.getUUID())) {
            return false;
        }
        try {
            doeWissel(p, lijst, van, naar);
        } finally {
            BEZIG.remove(p.getUUID());
        }
        return true;
    }

    private static void doeWissel(ServerPlayer p, CompoundTag lijst, int van, int naar) {
        Karakter.VOOR_WISSEL.forEach(h -> h.accept(p));
        boolean inRijk = Vadsrijk.in(p);
        p.closeContainer();
        if (inRijk) {
            Haken.BINNEN.forEach(h -> h.accept(p, false));
            Statussen.wisAlles(p);
        }
        SpelerData d = Spelers.data(p);
        boolean binnen = d.binnen;
        if (binnen) {
            Inventaris.naarThuis(p);   // the live Vadsrijk inventory -> the old character's stash (the home inventory comes out)
        }
        CompoundTag vol = Karakter.schrijf(p, d);
        // the old character goes to its slot, with where it stood and a snapshot for the list
        CompoundTag oud = lijst.getCompoundOrEmpty("s" + van);
        oud.put("data", Karakter.alleenKarakter(vol));
        oud.putInt("glevel", d.level());
        oud.putInt("level", d.klasseLevel());
        oud.putString("klasse", d.klasse);
        oud.putLong("munten", d.munten);
        oud.remove("plek");
        if (inRijk) {
            CompoundTag plek = new CompoundTag();
            plek.putDouble("x", p.getX());
            plek.putDouble("y", p.getY());
            plek.putDouble("z", p.getZ());
            plek.putFloat("yaw", p.getYRot());
            oud.put("plek", plek);
        }
        lijst.put("s" + van, oud);
        // the new one comes out
        CompoundTag nieuw = lijst.getCompoundOrEmpty("s" + naar);
        boolean eersteKeer = nieuw.getBooleanOr("nieuw", false);
        CompoundTag eigen = nieuw.getCompoundOrEmpty("data");
        CompoundTag plek = nieuw.contains("plek") ? nieuw.getCompoundOrEmpty("plek") : null;
        Karakter.lees(p, d, Karakter.metAccount(eigen, vol));
        nieuw.remove("data");
        nieuw.remove("plek");
        nieuw.remove("nieuw");
        lijst.put("s" + naar, nieuw);
        lijst.putInt("actief", naar);
        d.extra.put(Karakter.LIJST, lijst);   // (the list is account-wide and was just changed: keep the live object)
        if (binnen) {
            Inventaris.naarRijk(p);   // the home inventory back into its stash, the new character's inventory out
        }
        if (inRijk && p.level() instanceof ServerLevel sl) {
            Vec3 waar;
            float yaw = p.getYRot();
            if (plek != null) {
                waar = new Vec3(plek.getDoubleOr("x", 0), plek.getDoubleOr("y", 65), plek.getDoubleOr("z", 0));
                yaw = plek.getFloatOr("yaw", yaw);
            } else if (d.checkpoint != null) {
                waar = Vec3.atBottomCenterOf(d.checkpoint);
            } else {
                waar = Vec3.atBottomCenterOf(Gebieden.AANKOMST);
            }
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
            p.teleport(new TeleportTransition(sl, waar, Vec3.ZERO, yaw, p.getXRot(), Relative.ROTATION, TeleportTransition.DO_NOTHING));
            p.resetFallDistance();
            sl.sendParticles(ParticleTypes.HEART, waar.x, waar.y + 1.5, waar.z, 8, 0.5, 0.5, 0.5, 0);
            sl.playSound(null, BlockPos.containing(waar), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
            Haken.BINNEN.forEach(h -> h.accept(p, true));
            Sieraden.stuur(p);
            Spelers.sync(p);
            RijkScenes.titel(p, Component.literal(nieuw.getStringOr("naam", "?")).withStyle(ChatFormatting.GOLD),
                    Component.translatable(eersteKeer ? "vadsrijk.karakter.titel.nieuw" : "vadsrijk.karakter.titel.terug"), 50, 0xFFB3D1);
        }
        Karakter.Wissel w = new Karakter.Wissel(van, naar, eersteKeer);
        Karakter.GEWISSELD.forEach(h -> h.accept(p, w));
        Badges.ververs(p);
        p.sendSystemMessage(Component.translatable("vadsrijk.karakter.gewisseld", Component.literal(nieuw.getStringOr("naam", "?"))
                .withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // ---- Eénvads: retiring --------------------------------------------------------------------------------------------------

    /**
     * The Eénvads knight fainted outside a town: it retires. Never deleted: it gets the Rustende Ridder badge, plays on as Normaal,
     * and its statue + plaque go into the Rustende Ridders-zaal. Returns true when it retired.
     */
    public static boolean pensioen(ServerPlayer p, String waarom) {
        CompoundTag s = Karakter.info(p, Karakter.slot(p));
        if (Modus.van(s.getStringOr("modus", "")) != Modus.EENVADS) {
            return false;
        }
        SpelerData d = Spelers.data(p);
        s.putString("modus", Modus.NORMAAL.id);
        s.putBoolean("pensioen", true);
        s.putLong("pensioen_op", System.currentTimeMillis());
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
        String gebied = g == null ? "" : g.id();
        RustendeRidders.voegToe(p.level().getServer(), new RustendeRidders.Ridder(p.getUUID(), Karakter.slot(p), s.getStringOr("naam", "?"),
                p.getGameProfile().name(), d.level(), d.klasse, d.klasseLevel(), gebied, waarom, System.currentTimeMillis()));
        // (after the fainting title card, which comes right after this hook)
        nl.juiced.guhs.feature.vadsrijk.wereld.Later.na(70, () -> {
            if (!p.isRemoved()) {
                RijkScenes.titel(p, Component.translatable("vadsrijk.karakter.pensioen.titel").withStyle(ChatFormatting.GOLD),
                        Component.translatable("vadsrijk.karakter.pensioen.onder"), 90, 0xFFD24A);
            }
        });
        if (p.level().getServer() != null) {
            Component bericht = Component.translatable("vadsrijk.karakter.pensioen.iedereen", Component.literal(s.getStringOr("naam", "?"))
                    .withStyle(ChatFormatting.GOLD), p.getName(), d.level()).withStyle(ChatFormatting.YELLOW);
            for (ServerPlayer x : p.level().getServer().getPlayerList().getPlayers()) {
                x.sendSystemMessage(bericht);
            }
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 0.8f);
        Badges.ververs(p);
        return true;
    }

    static void meld(ServerPlayer p, String key, ChatFormatting kleur) {
        p.sendSystemMessage(Component.translatable(key).withStyle(kleur));
    }
}
