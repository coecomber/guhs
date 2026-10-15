package nl.juiced.guhs.feature.einde2.zij;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 zij: the Kyoshi-dorp ({@code kyoshi_dorp}, four steps, water).
 * <ol start="0">
 *   <li>talk to the leader of the Kyoshi-krijgsterguhs before the fan wall in the dojo (she asks for water bending);</li>
 *   <li>the fan dance ({@link #DANS}): stand on the golden tile ({@code dans_speler}) and follow the calls on the beat of
 *       {@code deuntje.waaierdans} ({@link E2Audio#BPM}): BUK (sneak), SPRING (jump), DRAAI (turn round); {@link #GOED} of
 *       {@link #ZETTEN} right and you pass; a miss costs nothing, you simply dance again;</li>
 *   <li>the make-up: the schminktafel under the mirror (a click near {@code schminktafel}): the Kyoshi make-up (guh clothes);</li>
 *   <li>the foaming fan by the gate meets a real Kyoshi warrior (and foams).</li>
 * </ol>
 */
public final class KyoshiDorp {
    static final String Q = Zij.Q + "kyoshi.";
    static final String S = Einde2Ids.KYOSHI_DORP;
    public static final int ZETTEN = 16, GOED = 12;
    /** Before the first call: two bars of the tune. */
    public static final int VOORAF_TELLEN = 4;

    public enum Zet {
        BUK, SPRING, DRAAI
    }

    /** One dance of one player. */
    static final class Dans {
        final long start;
        final Zet[] zetten = new Zet[ZETTEN];
        final boolean[] gehaald = new boolean[ZETTEN];
        int volgende;
        int goed;
        float yawBijRoep;
        double yBijRoep;

        Dans(long start, net.minecraft.util.RandomSource r) {
            this.start = start;
            for (int i = 0; i < ZETTEN; i++) {
                Zet z;
                do {
                    z = Zet.values()[r.nextInt(3)];
                } while (i >= 2 && zetten[i - 1] == z && zetten[i - 2] == z);
                zetten[i] = z;
            }
        }
    }

    private static final Map<UUID, Dans> DANS = new ConcurrentHashMap<>();

    private KyoshiDorp() {
    }

    static Verhaallijn ln() {
        return ZijLijnen.KYOSHI_LIJN;
    }

    /** Has this player the make-up (step 2 done)? */
    public static boolean geschminkt(ServerPlayer p) {
        return ln().stap(p) >= 3;
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.KYOSHI_KRIJGSTERGUH, S + "/krijgster_leider", new LeiderRol());
        NpcRollen.zet(GuhNpcEntity.Kind.FANGUH, S + "/fan_guh", new FanRol());
        nl.juiced.guhs.feature.kleding.KledingBronnen.bron(GuhClothes.EINDE2_ZIJ_KYOSHI_SCHMINK, "einde2_zij");
    }

    /** One beat of the fan dance in ticks. */
    static double tel() {
        return 20.0 * 60.0 / E2Audio.BPM.getOrDefault("deuntje.waaierdans", 90);
    }

    static final class LeiderRol extends QuestRol {
        LeiderRol() {
            super(ZijLijnen.KYOSHI_LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.speel("waaier");
            switch (stap) {
                case 0 -> {
                    if (!Zij.element(p, ZijLijnen.KYOSHI, npc)) {
                        return;
                    }
                    ln().begin(p);
                    scherm(p, npc, Q + "welkom", new Praat.Optie(1, Q + "optie.dans"), new Praat.Optie(2, Q + "optie.wie"));
                }
                case 1 -> {
                    zeg(p, npc, Q + "dans_nog");
                    hint(p, Q + "hint.dans");
                }
                case 2 -> {
                    zeg(p, npc, Q + "schmink");
                    hint(p, Q + "hint.schmink");
                }
                case 3 -> {
                    zeg(p, npc, Q + "fan");
                    hint(p, Q + "hint.fan");
                }
                default -> zeg(p, npc, Q + "dank." + p.getRandom().nextInt(3));
            }
        }

        @Override
        protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
            if (stap != 0) {
                return;
            }
            if (optie == 2) {
                scherm(p, npc, Q + "uitleg", new Praat.Optie(1, Q + "optie.dans"));
            } else if (optie == 1 && verder(p, 0)) {
                zeg(p, npc, Q + "dans");
                hint(p, Q + "hint.dans");
            }
        }
    }

    static final class FanRol extends QuestRol {
        FanRol() {
            super(ZijLijnen.KYOSHI_LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            if (stap == 3) {
                npc.speel("schuim");
                zeg(p, npc, Q + "fan_schuim");
                ServerLevel level = p.level();
                level.sendParticles(ParticleTypes.BUBBLE_POP, npc.getX(), npc.getY() + 1.3, npc.getZ(), 30, 0.3, 0.2, 0.3, 0.05);
                level.playSound(null, npc.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1f, 1.5f);
                ZijEvents.later(40, () -> npc.speel("flauw"));
                ZijEvents.later(260, () -> npc.speel(""));
                if (verder(p, 3)) {
                    Zij.stem(p, Q + "naam.leider", Q + "klaar");
                    Zij.eenmalig(ln(), p, "beloning", new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
                    Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.8f, 1.2f);
                }
            } else if (stap > 3) {
                zeg(p, npc, Q + "fan_later." + p.getRandom().nextInt(3));
            } else {
                zeg(p, npc, "quest.guhs.einde2_cast_volk.praatje.fanguh." + p.getRandom().nextInt(2));
            }
        }
    }

    // --- the fan dance ---------------------------------------------------------------------------------------------------------

    static void tick(ServerPlayer p) {
        UUID id = p.getUUID();
        Dans d = DANS.get(id);
        int stap = ln().stap(p);
        ZijPlek.Kopie kopie = stap == 1 || stap == 2 || d != null ? ZijPlek.bij(p, S, 4) : null;
        if (kopie == null) {
            if (d != null) {
                DANS.remove(id);
            }
            return;
        }
        if (stap == 2) {
            schminkBijTafel(p, kopie);
            return;
        }
        if (d == null) {
            if (stap == 1 && kopie.bij("dans_speler", p.position(), 1.1) && !nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(p)) {
                begin(p);
            }
            return;
        }
        if (!kopie.in("dansvloer", p.position(), 1.0)) {
            DANS.remove(id);
            Zij.melding(p, Q + "dans_weg");
            return;
        }
        dansTick(p, d, kopie);
    }

    static void begin(ServerPlayer p) {
        Dans d = new Dans(p.level().getGameTime() + 10, p.getRandom());
        DANS.put(p.getUUID(), d);
        p.level().playSound(null, p.blockPosition(), E2Audio.geluid("deuntje.waaierdans").get(), SoundSource.RECORDS, 1f, 1f);
        Zij.titel(p, Component.translatable(Q + "dans_titel").withStyle(ChatFormatting.AQUA), Component.translatable(Q + "dans_sub"), 4, 40, 8);
    }

    /** The tick (from the dance's start) of call i: one beat after the call is shown. */
    static int tijdVan(int i) {
        return (int) Math.round((VOORAF_TELLEN + 2 * i + 1) * tel());
    }

    private static void dansTick(ServerPlayer p, Dans d, ZijPlek.Kopie kopie) {
        long t = p.level().getGameTime() - d.start;
        if (t < 0) {
            return;
        }
        int b = (int) Math.round(tel());
        // the warriors flick their fans on every second beat (for everybody: they are only dancing)
        if (t % (2L * b) == 0) {
            BlockPos leider = kopie.pos("krijgster_leider");
            if (leider != null) {
                for (GuhNpcEntity n : p.level().getEntitiesOfClass(GuhNpcEntity.class, new net.minecraft.world.phys.AABB(leider).inflate(16),
                        n -> n.getKind() == GuhNpcEntity.Kind.KYOSHI_KRIJGSTERGUH)) {
                    n.speel("waaier");
                }
            }
        }
        if (d.volgende < ZETTEN) {
            int i = d.volgende;
            int roep = tijdVan(i) - b;
            if (t == roep) {
                d.yawBijRoep = p.getYRot();
                d.yBijRoep = p.getY();
                Zij.titel(p, Component.translatable(Q + "zet." + d.zetten[i].name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GOLD),
                        Component.translatable(Q + "zet_teller", i + 1, ZETTEN), 0, b + 4, 4);
                Zij.geluid(p, SoundEvents.NOTE_BLOCK_HAT.value(), 0.8f, 1.4f);
            }
            if (t >= roep + 1 && t <= tijdVan(i) + 8 && !d.gehaald[i] && gedaan(p, d, d.zetten[i])) {
                d.gehaald[i] = true;
                d.goed++;
                Zij.geluid(p, SoundEvents.NOTE_BLOCK_BELL.value(), 0.7f, 1.0f + 0.04f * (d.goed % 8));
                p.level().sendParticles(p, ParticleTypes.SPLASH, true, false, p.getX(), p.getY() + 1, p.getZ(), 12, 0.6, 0.4, 0.6, 0.1);
                p.sendOverlayMessage(Component.translatable(Q + "zet_goed", d.goed).withStyle(ChatFormatting.AQUA));
            }
            if (t > tijdVan(i) + 8) {
                if (!d.gehaald[i]) {
                    Zij.geluid(p, SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f, 0.6f);
                    p.sendOverlayMessage(Component.translatable(Q + "zet_mis").withStyle(ChatFormatting.GRAY));
                }
                d.volgende++;
            }
            return;
        }
        DANS.remove(p.getUUID());
        boolean geslaagd = d.goed >= GOED;
        Zij.titel(p, Component.translatable(geslaagd ? Q + "dans_goed" : Q + "dans_bijna").withStyle(geslaagd ? ChatFormatting.GOLD : ChatFormatting.YELLOW),
                Component.translatable(Q + "dans_score", d.goed, ZETTEN), 4, 50, 10);
        if (geslaagd && ln().verder(p, 1)) {
            Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.8f, 1.3f);
            Zij.stem(p, Q + "naam.leider", Q + "dans_klaar");
            Zij.hint(p, Q + "hint.schmink");
        } else if (!geslaagd) {
            Zij.stem(p, Q + "naam.leider", Q + "dans_opnieuw", GOED);
        }
    }

    /** Did the player do this move just now? */
    private static boolean gedaan(ServerPlayer p, Dans d, Zet z) {
        return switch (z) {
            case BUK -> p.isShiftKeyDown();
            case SPRING -> !p.onGround() && p.getY() > d.yBijRoep + 0.2;
            case DRAAI -> Math.abs(Mth.wrapDegrees(p.getYRot() - d.yawBijRoep)) >= 70f;
        };
    }

    // --- the make-up -----------------------------------------------------------------------------------------------------------

    /** (step 2) a player who stands at the schminktafel and sneaks: the make-up. */
    private static void schminkBijTafel(ServerPlayer p, ZijPlek.Kopie kopie) {
        if (kopie.bij("schminktafel", p.position(), 2.6) && p.isShiftKeyDown()) {
            schmink(p);
        }
    }

    /** A click on the schminktafel (or a sneak beside it) at step 2. */
    static boolean klikBlok(ServerPlayer p, BlockPos pos) {
        if (ln().stap(p) != 2) {
            return false;
        }
        ZijPlek.Kopie kopie = ZijPlek.bij(p, S, 4);
        if (kopie == null || !kopie.bij("schminktafel", Vec3.atBottomCenterOf(pos), 2.6)) {
            return false;
        }
        schmink(p);
        return true;
    }

    private static void schmink(ServerPlayer p) {
        if (!ln().verder(p, 2)) {
            return;
        }
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false, false));
        Zij.geluid(p, SoundEvents.BRUSH_GENERIC, 1f, 1.2f);
        Zij.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
        Zij.titel(p, Component.translatable(Q + "geschminkt").withStyle(ChatFormatting.RED), Component.translatable(Q + "geschminkt_sub"), 20, 50, 10);
        Zij.eenmalig(ln(), p, "schmink", new ItemStack(ModItems.clothingItem(GuhClothes.EINDE2_ZIJ_KYOSHI_SCHMINK)));
        Zij.stem(p, Q + "naam.leider", Q + "schmink_klaar");
        Zij.hint(p, Q + "hint.fan");
    }

    static void vergeet(UUID id) {
        DANS.remove(id);
    }

    /** (tests) the dance of this player (null: none). */
    static Dans dans(UUID id) {
        return DANS.get(id);
    }
}
