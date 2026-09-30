package nl.juiced.guhs.feature.ballon;

import java.util.Set;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.sterrenwacht.Buiten;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The round flight with Kapitein Wolkje (the game "ballon"), server side: the guide texts on the way (take-off, a
 * line in between, "kijk, daar!" and the viewpoint itself, landing), a little show at each viewpoint (clouds, a
 * rainbow, hearts, a burner jump...), and after landing the stamps on your ballonstempelkaart (Knus collection
 * {@code ballonstempels}, 8 viewpoints) and ballonmunten ({@link #ballonmunten}).
 */
public final class BallonVlucht {
    public static final String STEMPELS = "ballonstempels";
    // Knus counters
    public static final String GEVONDEN = "ballon.gevonden", VLUCHTEN = "ballon.vluchten", AANTAL_STEMPELS = "ballon.stempels";
    private static final String KEY = "guhs_ballon";
    /** (Tests) how much faster than normal a flight goes, and how big the round is (1 = normal). */
    static double tempo = 1.0, schaal = 1.0;

    private BallonVlucht() {
    }

    public static double tempo() {
        return tempo;
    }

    /** (Tests) speeds all flights up (1 = normal). */
    public static void setTempo(double t) {
        tempo = t;
    }

    /** (Tests) makes every round smaller (1 = normal), so a test flight stays inside the loaded test area. */
    public static void setSchaal(double s) {
        schaal = s;
    }

    public static double schaal() {
        return schaal;
    }

    public static Component kapitein() {
        return Component.translatable("entity.guhs.guh_npc.ballonguh");
    }

    /** Every reward rule gives one more ("+1 per reward"): 1 per flight, 1 per new stamp, + 1. */
    public static int ballonmunten(int nieuweStempels) {
        return 1 + nieuweStempels + 1;
    }

    /** The route of this player's next flight: the first one with a viewpoint not stamped yet, else the next in turn. */
    public static BallonRoute volgendeRoute(ServerPlayer player) {
        Set<String> kaart = KnusVoortgang.ontdekt(player, STEMPELS);
        for (BallonRoute r : BallonRoute.values()) {
            if (!kaart.contains(r.eerste.id()) || !kaart.contains(r.tweede.id())) {
                return r;
            }
        }
        return BallonRoute.byIndex(KnusVoortgang.teller(player, VLUCHTEN));
    }

    /** Take-off: Kapitein Wolkje says where we go. */
    static void vertrek(ServerPlayer player, LuchtballonEntity ballon) {
        Buiten.zeg(player, kapitein(), "quest.guhs.ballon.vertrek." + ballon.route().id());
        player.displayClientMessage(Component.translatable("gui.guhs.ballon.route", Component.translatable("gui.guhs.ballon.route." + ballon.route().id()))
                .withStyle(net.minecraft.ChatFormatting.AQUA), true);
        ballon.level().playSound(null, ballon, BallonFeature.BRANDER.get(), SoundSource.NEUTRAL, 1f, 1f);
    }

    /** Server, every tick of a flight: the guide texts at their spots. Returns the new phase. */
    static int onderweg(LuchtballonEntity ballon, BallonRoute.Pad pad, double d, int fase) {
        BallonRoute route = pad.route;
        double a = pad.bijPunt(route.eerstePunt), b = pad.bijPunt(route.tweedePunt);
        double[] drempels = {0, 20, a - 14, a, (a + b) / 2, b - 14, b, pad.lengte() - 22};
        while (fase < drempels.length && d >= drempels[fase]) {
            switch (fase) {
                case 1 -> alle(ballon, "quest.guhs.ballon.onderweg." + route.id() + ".0");
                case 2 -> alle(ballon, "quest.guhs.ballon.uitzicht." + route.eerste.id() + ".komt");
                case 3 -> uitzicht(ballon, route.eerste);
                case 4 -> alle(ballon, "quest.guhs.ballon.onderweg." + route.id() + ".1");
                case 5 -> alle(ballon, "quest.guhs.ballon.uitzicht." + route.tweede.id() + ".komt");
                case 6 -> uitzicht(ballon, route.tweede);
                case 7 -> alle(ballon, "quest.guhs.ballon.landen");
                default -> {
                }
            }
            fase++;
        }
        return fase;
    }

    private static void alle(LuchtballonEntity ballon, String key) {
        for (Entity e : ballon.getPassengers()) {
            if (e instanceof ServerPlayer player) {
                Buiten.zeg(player, kapitein(), key);
            }
        }
    }

    /** A viewpoint: the Kapitein tells about it and there's something to see. */
    static void uitzicht(LuchtballonEntity ballon, BallonRoute.Uitzicht u) {
        alle(ballon, "quest.guhs.ballon.uitzicht." + u.id());
        for (Entity e : ballon.getPassengers()) {
            if (e instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("gui.guhs.ballon.stempel_onderweg",
                        Component.translatable("gui.guhs.knus." + STEMPELS + "." + u.id())).withStyle(net.minecraft.ChatFormatting.GOLD), true);
            }
        }
        if (!(ballon.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 c = ballon.position().add(0, 3, 0);
        Vec3 voor = Vec3.directionFromRotation(0, ballon.getYRot());
        switch (u) {
            case GUHGEZICHTVELD -> {
                level.sendParticles(ParticleTypes.HEART, c.x, c.y - 4, c.z, 16, 3, 1, 3, 0.02);
                level.playSound(null, ballon, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 1f, 1.2f);
            }
            case WOLKENPOORT -> level.sendParticles(BallonFeature.BALLONWOLKJE.get(), c.x, c.y, c.z, 90, 5, 3, 5, 0.01);
            case HOOGSTE_PUNTJE -> {
                level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 3, c.z, 30, 2, 1, 2, 0.05);
                level.playSound(null, ballon, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.4f);
            }
            case REGENBOOGBOCHT -> {
                int[] kleuren = {0xFF6A7A, 0xFFB45A, 0xFFE86A, 0x7AE89A, 0x6AB8FF, 0xB88AFF};
                for (int k = 0; k < kleuren.length; k++) {
                    ParticleOptions dust = new DustParticleOptions(new Vector3f(((kleuren[k] >> 16) & 255) / 255f, ((kleuren[k] >> 8) & 255) / 255f,
                            (kleuren[k] & 255) / 255f), 2.5f);
                    double r = 9 - k * 0.7;
                    for (int s = 0; s <= 24; s++) {
                        double h = Math.PI * s / 24;
                        Vec3 zij = new Vec3(-voor.z, 0, voor.x);
                        Vec3 p = c.add(voor.scale(12)).add(zij.scale(Math.cos(h) * r)).add(0, Math.sin(h) * r - 4, 0);
                        level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    }
                }
            }
            case REUZENGUH -> {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y - 6, c.z, 20, 4, 2, 4, 0.02);
                level.playSound(null, ballon, nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 0.7f);
            }
            case HARTJESWOLKJE -> {
                for (int s = 0; s < 40; s++) {
                    double t = Math.PI * 2 * s / 40;
                    double hx = 16 * Math.pow(Math.sin(t), 3) / 16 * 3, hy = (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 16 * 3;
                    Vec3 zij = new Vec3(-voor.z, 0, voor.x);
                    Vec3 p = c.add(voor.scale(8)).add(zij.scale(hx)).add(0, hy, 0);
                    level.sendParticles(ParticleTypes.HEART, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
            case BRANDERSPRONG -> {
                level.sendParticles(ParticleTypes.FLAME, c.x, c.y - 1, c.z, 40, 0.3, 0.6, 0.3, 0.08);
                level.playSound(null, ballon, BallonFeature.BRANDER.get(), SoundSource.NEUTRAL, 1.5f, 0.7f);
                level.playSound(null, ballon, SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.8f, 1.2f);
            }
            case GUHMENSIE_UITZICHT -> {
                level.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x, c.y + 2, c.z, 40, 6, 2, 6, 0.01);
                level.playSound(null, ballon, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1f, 0.9f);
            }
        }
    }

    /** After landing (or a flight broken off: geslaagd = false). Returns the ballonmunten given. */
    static int geland(ServerPlayer player, LuchtballonEntity ballon, BallonRoute route, boolean geslaagd) {
        if (!geslaagd) {
            Buiten.zeg(player, kapitein(), "quest.guhs.ballon.afgebroken");
            return 0;
        }
        int nieuw = 0;
        for (BallonRoute.Uitzicht u : new BallonRoute.Uitzicht[]{route.eerste, route.tweede}) {
            if (KnusVoortgang.ontdek(player, STEMPELS, u.id())) {
                nieuw++;
            }
        }
        int munten = ballonmunten(nieuw);
        Minigames.give(player, new ItemStack(BallonFeature.BALLONMUNT.get(), munten));
        int kaart = KnusVoortgang.ontdekt(player, STEMPELS).size();
        Buiten.zeg(player, kapitein(), nieuw > 0 ? "quest.guhs.ballon.geland_stempels" : "quest.guhs.ballon.geland", nieuw, munten, kaart,
                BallonRoute.Uitzicht.values().length);
        KnusVoortgang.tel(player, VLUCHTEN, 1);
        KnusVoortgang.hoogste(player, AANTAL_STEMPELS, kaart);
        GuhAdvancements.grant(player, "ballon_eerste_vlucht");
        Buiten.toon(player, "ballon_eerste_vlucht");
        if (kaart >= BallonRoute.Uitzicht.values().length) {
            GuhAdvancements.grant(player, "ballon_alle_stempels");
            Buiten.toon(player, "ballon_alle_stempels");
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
        return munten;
    }

    /** (Tests / save) the player's data. */
    static net.minecraft.nbt.CompoundTag data(ServerPlayer player) {
        var saved = nl.juiced.guhs.quest.GuhQuests.saved(player);
        if (!saved.contains(KEY)) {
            saved.put(KEY, new net.minecraft.nbt.CompoundTag());
        }
        return saved.getCompound(KEY);
    }
}
