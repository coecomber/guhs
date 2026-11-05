package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntPredicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.beleving.Beleving;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.piepvel.Piepvel;

/**
 * The zone-3 boss moments that run without clicks:
 * <ul>
 *   <li>{@link #armFlash}: flash F1, the first flash of the empire. After Graaf Piepvel's defeat a long, thin, grey arm with five
 *       long fingers reaches in through a knothole, grabs the count by his shirt and snatches him away ("Nee nee nee, Keizer, ik
 *       kan het uitleggen!"). Floep. Silence.</li>
 *   <li>{@link #vahoegnirEinde}: the last splinters, Vahoegnir turns pink, the egg hatches (Vahoegje), the pink beam toward
 *       Hoogvads, the dragon flies off along it; then {@link #troonzaal}.</li>
 *   <li>{@link #troonzaal}: flash F2, the Keizer's throne room at night (a small set built above the nest for the scene, removed
 *       after): Piepvel kneels in his shirt, Hofschrijver Krabbel reads the report, long grey fingers tap, "Dan... reik... ik...
 *       verder."</li>
 * </ul>
 * The camera parts are kern RijkScenes (first time per player; later you just see it happen in the world). The npc slice listens
 * through {@link Z3BazenApi#MOMENT}.
 */
public final class Z3Scenes {
    public static final int ARM_TICKS = 150, EINDE_TICKS = 440, TROON_TICKS = 400;
    public static final String H_TROON = "z3_troonzaal";
    private static final List<IntPredicate> LOOPT = new ArrayList<>();
    private static int troonKijkers;

    private Z3Scenes() {
    }

    /** Runs {@code t} every tick (argument = ticks since start) until it returns true. */
    public static void loop(IntPredicate t) {
        LOOPT.add(new IntPredicate() {
            int i;

            @Override
            public boolean test(int ignored) {
                return t.test(i++);
            }
        });
    }

    static void tick(MinecraftServer server) {
        for (Iterator<IntPredicate> it = new ArrayList<>(LOOPT).iterator(); it.hasNext(); ) {
            IntPredicate t = it.next();
            boolean klaar;
            try {
                klaar = t.test(0);
            } catch (RuntimeException e) {
                klaar = true;
            }
            if (klaar) {
                LOOPT.remove(t);
            }
        }
    }

    public static boolean bezig() {
        return !LOOPT.isEmpty();
    }

    private static List<ServerPlayer> rond(ServerLevel sl, Vec3 v, double r) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.position().distanceTo(v) < r) {
                l.add(p);
            }
        }
        return l;
    }

    private static void zeg(List<ServerPlayer> l, Component c) {
        for (ServerPlayer p : l) {
            if (!p.isRemoved()) {
                p.sendSystemMessage(c);
            }
        }
    }

    private static Component spreker(String wie, String key) {
        return Component.translatable(wie).withStyle(ChatFormatting.GOLD).append(Component.literal(": ").withStyle(ChatFormatting.GOLD))
                .append(Component.translatable(key).withStyle(ChatFormatting.WHITE));
    }

    // ---- flash F1: the arm -------------------------------------------------------------------------------------------------

    /** The arm of the Keizer snatches Graaf Piepvel away (he must be beaten: geplofd, in his shirt). */
    public static void armFlash(ServerLevel sl, Piepvel g, List<Z3Figurant> muizen) {
        Vec3 piep = g.position();
        Vec3 richting = Z3BazenPlekken.PIEP_KWASTGAT_RICHTING;   // from the hall toward the knothole
        Vec3 vooruit = richting.scale(-1);                           // the arm reaches this way
        float yaw = (float) Math.toDegrees(Math.atan2(-vooruit.x, vooruit.z));
        double lengte = 175 / 16.0 * 2.5;                            // the model's reach at scale 2.5
        Vec3 ver = piep.add(0, 1.2, 0).subtract(vooruit.scale(lengte + 30)), dichtbij = piep.add(0, 1.2, 0).subtract(vooruit.scale(lengte - 0.5));
        List<ServerPlayer> kijkers = rond(sl, piep, 40);
        for (ServerPlayer p : kijkers) {
            if (Spelers.data(p).gezien.add("z3:f1_arm")) {
                Vec3 zij = new Vec3(-richting.z, 0, richting.x).normalize();
                RijkScenes.speel(p, RijkScene.maak("z3_f1_arm", ARM_TICKS)
                        .camera(0, piep.add(zij.scale(5)).add(0, 1.8, 0), piep.add(0, 1, 0))
                        .camera(45, piep.add(zij.scale(7)).add(0, 2.5, 0).subtract(richting.scale(2)), piep.add(0, 1.5, 0).add(richting.scale(4)))
                        .camera(95, piep.add(zij.scale(9)).add(0, 3, 0).subtract(richting.scale(3)), piep.add(0, 2, 0).add(richting.scale(6)))
                        .camera(ARM_TICKS, piep.add(zij.scale(10)).add(0, 5, 0), piep.add(richting.scale(18)).add(0, 4, 0))
                        .titel("vadsrijk.z3bazen.f1.titel", "vadsrijk.z3bazen.f1.onder", 115, 35, 0x8A8A9A).bouw());
            }
        }
        Z3Figurant[] arm = new Z3Figurant[1];
        loop(t -> {
            if (t == 30) {
                zeg(kijkers, spreker("vadsrijk.z3bazen.piepvel", "vadsrijk.z3bazen.pv.mijn_cape"));
            }
            if (t == 40) {
                sl.playSound(null, piep.x, piep.y, piep.z, SoundEvents.WOOD_BREAK, SoundSource.AMBIENT, 3f, 0.4f);
                for (ServerPlayer p : kijkers) {
                    Beleving.schud(p, 0.8f, 30);
                }
            }
            if (t == 50) {
                arm[0] = Z3Figurant.maak(sl, ver, yaw, "vr_z3_baas_keizerarm", "textures/vadsrijk/z3bazen/keizerarm.png", "vr_z3_baas_keizerarm", "idle", 2.5f);
                sl.playSound(null, piep.x, piep.y, piep.z, SoundEvents.WARDEN_EMERGE, SoundSource.AMBIENT, 2f, 1.4f);
            }
            if (arm[0] != null && t > 50 && t <= 75) {
                arm[0].zet(ver.lerp(dichtbij, (t - 50) / 25.0));
            }
            if (t == 76 && arm[0] != null) {
                arm[0].anim("1:grijp");
                zeg(kijkers, spreker("vadsrijk.z3bazen.piepvel", "vadsrijk.z3bazen.f1.uitleggen"));
            }
            if (arm[0] != null && t > 88 && t <= 118) {
                Vec3 v = dichtbij.lerp(ver, (t - 88) / 30.0);
                arm[0].zet(v);
                Vec3 hand = v.add(vooruit.scale(lengte - 0.5)).add(0, -1.2, 0);
                if (!g.isRemoved()) {
                    g.setPos(hand.x, hand.y, hand.z);
                }
            }
            if (t == 105) {
                zeg(kijkers, Component.translatable("vadsrijk.z3bazen.f1.floep").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                sl.playSound(null, piep.x, piep.y, piep.z, SoundEvents.CHICKEN_EGG, SoundSource.AMBIENT, 2f, 0.5f);
            }
            if (t == 120) {
                if (arm[0] != null) {
                    arm[0].discard();
                }
                if (!g.isRemoved()) {
                    g.discard();
                }
                zeg(kijkers, Component.translatable("vadsrijk.z3bazen.f1.stilte").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
            if (t == ARM_TICKS) {
                for (ServerPlayer p : kijkers) {
                    Z3BazenApi.moment(p, "piepvel_weg");
                }
                return true;
            }
            return false;
        });
    }

    // ---- Vahoegnir: cheered up, the egg hatches, the beam, the throne room ---------------------------------------------------

    public static void vahoegnirEinde(ServerLevel sl, Vahoegnir d, @Nullable ServerPlayer laatste) {
        Vec3 nest = d.zMidden();
        Vec3 ei = d.eiPlek();
        List<ServerPlayer> kijkers = rond(sl, nest, 45);
        boolean herinnering = d.herinnering;
        Vec3 naarHoog = Z3BazenPlekken.HOOGVADS.subtract(d.position()).normalize();
        Vec3 zij = new Vec3(-naarHoog.z, 0, naarHoog.x).normalize();
        for (ServerPlayer p : kijkers) {
            RijkScenes.speel(p, RijkScene.maak("z3_vahoegnir_einde", EINDE_TICKS)
                    .camera(0, d.position().add(zij.scale(10)).add(0, 4, 0), d.position().add(0, 3, 0))
                    .camera(80, d.position().add(zij.scale(8)).add(0, 5, 0), d.position().add(0, 3, 0))
                    .camera(110, ei.add(zij.scale(3.5)).add(0, 1.6, 0), ei.add(0, 0.6, 0))
                    .camera(200, ei.add(zij.scale(3)).add(0, 1.4, 0), ei.add(0, 0.5, 0))
                    .camera(240, d.position().subtract(naarHoog.multiply(1, 0, 1).scale(10)).add(0, 7, 0), d.position().add(naarHoog.scale(40)).add(0, 10, 0))
                    .camera(330, d.position().subtract(naarHoog.multiply(1, 0, 1).scale(12)).add(0, 9, 0), d.position().add(naarHoog.scale(120)).add(0, 30, 0))
                    .camera(EINDE_TICKS, d.position().subtract(naarHoog.multiply(1, 0, 1).scale(14)).add(0, 12, 0), Z3BazenPlekken.HOOGVADS)
                    .titel("vadsrijk.z3bazen.vn.einde.titel", "vadsrijk.z3bazen.vn.einde.onder", 330, 100, 0xFF8AD0).bouw());
        }
        Z3Figurant[] jong = new Z3Figurant[1];
        Vec3 start = d.position();
        loop(t -> {
            Vec3 borst = d.position().add(0, 3, 0);
            if (t == 0 || t == 30) {   // splinters 4 and 5: from every player into his chest
                for (ServerPlayer p : kijkers) {
                    Vec3 a = p.position().add(0, 1.2, 0);
                    for (double s = 0; s <= 1; s += 0.05) {
                        Vec3 v = a.lerp(borst, s);
                        sl.sendParticles(new DustParticleOptions(Z3Baas.ROZE, 1.6f), v.x, v.y, v.z, 1, 0, 0, 0, 0);
                    }
                }
                d.zetSplinters(t == 0 ? 4 : 5);
                sl.sendParticles(ParticleTypes.END_ROD, borst.x, borst.y, borst.z, 60, 0.6, 0.6, 0.6, 0.2);
                sl.playSound(null, borst.x, borst.y, borst.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 3f, t == 0 ? 1.0f : 1.4f);
            }
            if (t >= 30 && t < 90 && t % 3 == 0) {   // the black fluff blows off him like dandelion seeds
                sl.sendParticles(ParticleTypes.WHITE_ASH, borst.x, borst.y, borst.z, 30, 3, 2, 3, 0.2);
                sl.sendParticles(new DustParticleOptions(0x2A2A30, 2f), borst.x, borst.y, borst.z, 10, 3, 2, 3, 0.1);
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, borst.x, borst.y, borst.z, 6, 3, 2, 3, 0.05);
            }
            if (t == 40) {
                ServerPlayer wie = laatste != null && !laatste.isRemoved() ? laatste : kijkers.isEmpty() ? null : kijkers.get(0);
                d.opgevrolijkt(wie);
                for (ServerPlayer p : kijkers) {
                    Z3BazenApi.moment(p, "vahoegnir_opgevrolijkt");
                }
            }
            if (t == 100 && d.ei() != null) {
                d.ei().anim("1:uitkomen");
                sl.playSound(null, ei.x, ei.y, ei.z, SoundEvents.TURTLE_EGG_CRACK, SoundSource.NEUTRAL, 2f, 0.8f);
            }
            if (t == 140) {
                sl.playSound(null, ei.x, ei.y, ei.z, SoundEvents.TURTLE_EGG_HATCH, SoundSource.NEUTRAL, 2f, 1.2f);
                jong[0] = Z3Figurant.maak(sl, ei.add(0, 0.3, 0), 0, "vadsrijk/wezens/vahoegje", "textures/vadsrijk/z3bazen/vahoegje.png", "vr_huisdier_vahoegje", "idle", 0.45f);
                sl.sendParticles(new DustParticleOptions(Z3Baas.ROZE, 1.4f), ei.x, ei.y + 0.6, ei.z, 30, 0.4, 0.3, 0.4, 0);
            }
            if (t == 158) {   // it sneezes
                sl.sendParticles(ParticleTypes.SNEEZE, ei.x, ei.y + 0.6, ei.z, 15, 0.1, 0.05, 0.1, 0.05);
                sl.playSound(null, ei.x, ei.y, ei.z, SoundEvents.PANDA_SNEEZE, SoundSource.NEUTRAL, 1.5f, 1.8f);
                if (d.ei() != null) {
                    d.ei().discard();
                }
            }
            if (t == 170) {   // the dragon giggles: the whole crown trembles
                sl.playSound(null, borst.x, borst.y, borst.z, SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.NEUTRAL, 2f, 1.9f);
                for (ServerPlayer p : kijkers) {
                    Beleving.schud(p, 0.4f, 30);
                    Z3BazenApi.moment(p, "vahoegje");
                }
                if (jong[0] != null && !kijkers.isEmpty()) {
                    jong[0].anim("hop");
                    jong[0].snelheid = 0.08;
                    jong[0].doel = kijkers.get(0).position();
                }
            }
            if (t == 230) {
                d.setYRot((float) Math.toDegrees(Math.atan2(-naarHoog.x, naarHoog.z)));
                d.setYBodyRot(d.getYRot());
                sl.playSound(null, borst.x, borst.y, borst.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.NEUTRAL, 3f, 1.3f);
            }
            if (t >= 240 && t < 330 && t % 3 == 0) {   // the long pink beam over the horizon, to the floating city
                Vec3 kop = d.position().add(0, 4, 0).add(naarHoog.multiply(1, 0, 1).normalize().scale(3));
                double len = Math.min(320, (t - 240) * 6.0);
                for (double s = 2; s < len; s += 2.2) {
                    Vec3 v = kop.add(naarHoog.scale(s));
                    sl.sendParticles(new DustParticleOptions(0xFF8AD0, 3.2f), v.x, v.y, v.z, 1, 0.2, 0.2, 0.2, 0);
                }
                sl.sendParticles(ParticleTypes.END_ROD, kop.x, kop.y, kop.z, 6, 0.3, 0.3, 0.3, 0.05);
            }
            if (t == 300) {
                for (ServerPlayer p : kijkers) {
                    p.sendSystemMessage(Component.translatable("vadsrijk.z3bazen.vn.hoogvads").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
                    Z3BazenApi.moment(p, "hoogvads");
                }
            }
            if (t > 330 && t <= 420 && !d.isRemoved()) {   // he bows his head, then flies off along the beam
                double f = (t - 330) / 90.0;
                Vec3 v = start.add(naarHoog.scale(f * f * 160)).add(0, f * 30, 0);
                d.setPos(v.x, v.y, v.z);
                if (t % 10 == 0) {
                    sl.playSound(null, v.x, v.y, v.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, 3f, 1.1f);
                }
            }
            if (t == 420) {
                if (!d.isRemoved()) {
                    d.discard();
                }
                if (jong[0] != null) {
                    sl.sendParticles(ParticleTypes.HEART, jong[0].getX(), jong[0].getY() + 0.5, jong[0].getZ(), 5, 0.3, 0.3, 0.3, 0);
                    jong[0].discard();
                }
            }
            if (t == EINDE_TICKS) {
                for (ServerPlayer p : kijkers) {
                    if (!p.isRemoved() && !herinnering && Spelers.data(p).gezien.add("z3:f2_troonzaal")) {
                        troonzaal(sl, p);
                    }
                }
                return true;
            }
            return false;
        });
    }

    // ---- flash F2: the throne room -------------------------------------------------------------------------------------------

    /** Where the throne-room set stands: high above the nest (chunks are loaded for the players there). */
    public static BlockPos troonPlek() {
        return Z3BazenPlekken.NEST_CENTRUM.offset(0, 26, -60);
    }

    private static void bouwTroonzaal(ServerLevel sl, BlockPos c) {
        BlockState vloer = Blocks.POLISHED_DEEPSLATE.defaultBlockState(), muur = Blocks.GRAY_CONCRETE.defaultBlockState(),
                rand = Blocks.DEEPSLATE_TILES.defaultBlockState();
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int y = -1; y <= 6; y++) {
                    BlockPos b = c.offset(x, y, z);
                    boolean wand = Math.abs(x) == 6 || Math.abs(z) == 6;
                    BlockState s = y == -1 ? vloer : y == 6 ? rand : wand ? muur : Blocks.AIR.defaultBlockState();
                    if (sl.getBlockState(b).isAir()) {
                        Herstel.zet(sl, H_TROON, b, s);
                    }
                }
            }
        }
        // the throne (too big, grey), a map table, two dim soul lanterns
        Herstel.zet(sl, H_TROON, c.offset(0, 0, -5), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        Herstel.zet(sl, H_TROON, c.offset(0, 1, -5), Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState());
        Herstel.zet(sl, H_TROON, c.offset(0, 2, -6), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        Herstel.zet(sl, H_TROON, c.offset(-1, 1, -5), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
        Herstel.zet(sl, H_TROON, c.offset(1, 1, -5), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
        Herstel.zet(sl, H_TROON, c.offset(4, 0, 0), Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        Herstel.zet(sl, H_TROON, c.offset(-5, 1, -3), Blocks.SOUL_LANTERN.defaultBlockState());
        Herstel.zet(sl, H_TROON, c.offset(5, 1, -3), Blocks.SOUL_LANTERN.defaultBlockState());
    }

    /** Flash F2 (~20 s, no clicks): the Keizer's throne room at night. */
    public static void troonzaal(ServerLevel sl, ServerPlayer p) {
        BlockPos c = troonPlek();
        if (troonKijkers++ == 0) {
            bouwTroonzaal(sl, c);
        }
        Vec3 m = Vec3.atBottomCenterOf(c);
        Vec3 troon = m.add(0, 0, -5);
        Z3Figurant graaf = Z3Figurant.maak(sl, m.add(0, 0, -2), 180, "vr_z3_baas_piepvel", "textures/vadsrijk/z3bazen/piepvel.png", "vr_z3_baas_piepvel", "idle", 1.8f);
        if (graaf != null) {
            graaf.verberg("cape");
        }
        Z3Figurant krabbel = Z3Figurant.maak(sl, m.add(2.5, 0, -1.5), 120, "mika", "textures/vadsrijk/z3bazen/krabbel.png", "guh", "animation.guh.idle", 0.8f);
        // the hand on the armrest: the arm comes from far behind the throne, far too long
        double lengte = 175 / 16.0;
        Z3Figurant hand = Z3Figurant.maak(sl, troon.add(1.2, 1.4, -lengte + 0.6), 0, "vr_z3_baas_keizerarm", "textures/vadsrijk/z3bazen/keizerarm.png",
                "vr_z3_baas_keizerarm", "tik", 1.0f);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, TROON_TICKS + 20, 0, false, false));
        RijkScenes.speel(p, RijkScene.maak("z3_f2_troonzaal", TROON_TICKS)
                .camera(0, m.add(0, 2.2, 4.5), m.add(0, 0.8, -2))
                .camera(90, m.add(1, 1.8, 2.5), m.add(0, 0.8, -2))
                .camera(140, m.add(3.5, 1.6, 0.5), m.add(2.5, 0.6, -1.5))
                .camera(200, troon.add(2.5, 2.0, 2.5), troon.add(1.2, 1.4, 0))
                .camera(270, troon.add(1.5, 1.8, 1.5), troon.add(1.2, 1.4, 0))
                .camera(320, m.add(3, 2.5, 2), m.add(4, 1, 0))
                .camera(TROON_TICKS, m.add(0, 3.5, 5), m.add(0, 1, -5))
                .titel("vadsrijk.z3bazen.f2.titel", "vadsrijk.z3bazen.f2.onder", 330, 70, 0x8A8A9A).bouw());
        List<ServerPlayer> een = List.of(p);
        loop(t -> {
            switch (t) {
                case 40 -> zeg(een, Component.translatable("vadsrijk.z3bazen.f2.rillen").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                case 110 -> zeg(een, spreker("vadsrijk.z3bazen.krabbel", "vadsrijk.z3bazen.f2.rapport1"));
                case 160 -> zeg(een, spreker("vadsrijk.z3bazen.krabbel", "vadsrijk.z3bazen.f2.rapport2"));
                case 200 -> {
                    zeg(een, Component.translatable("vadsrijk.z3bazen.f2.tik").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                    Z3Vast.geluid(p, SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 0.5f);
                }
                case 270 -> zeg(een, spreker("vadsrijk.z3bazen.keizer", "vadsrijk.z3bazen.f2.reik"));
                default -> {
                }
            }
            if (t >= 200 && t < 260 && t % 6 == 0) {
                Z3Vast.geluid(p, SoundEvents.NOTE_BLOCK_HAT.value(), 0.8f, 0.5f + (t % 30) / 60f);
            }
            if (t == TROON_TICKS + 5) {
                for (Z3Figurant f : new Z3Figurant[] {graaf, krabbel, hand}) {
                    if (f != null) {
                        f.discard();
                    }
                }
                if (--troonKijkers <= 0) {
                    troonKijkers = 0;
                    Herstel.terug(sl, H_TROON);
                }
                Z3BazenApi.moment(p, "troonzaal_klaar");
                return true;
            }
            return false;
        });
    }
}
