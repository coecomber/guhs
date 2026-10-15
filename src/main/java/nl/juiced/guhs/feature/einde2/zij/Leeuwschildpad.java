package nl.juiced.guhs.feature.einde2.zij;

import java.util.List;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 zij: the Leeuwschildpad-eiland ({@code leeuwschildpad}, three steps, no element): the island is the old lion
 * turtle guh, swimming through the void. One blessing per player.
 * <ol start="0">
 *   <li>walk over his shell to the stone ring on top ({@code schild_top}): he notices the tickle;</li>
 *   <li>go down to his raised paw and stand on the palm ({@code zegen_plek}): he wakes and asks what you seek;</li>
 *   <li>lay a kaasknabbel in his paw (click his face with one): the blessing (scene {@code leeuwschildpad_zegen}).</li>
 * </ol>
 * The blessing is the {@link E2ZijFeature#ZEGENSCHUB}: whoever carries it and falls into the void of the Guheinde is caught
 * by the old turtle and set back on the last firm ground ({@link #vang}). After the line he gives a chonky wisdom a day.
 */
public final class Leeuwschildpad {
    static final String Q = Zij.Q + "schildpad.";
    static final String S = Einde2Ids.LEEUWSCHILDPAD_EILAND;
    static final String SLEUTEL_VRAAG = "einde2_zij_schildpad_vraag";
    static final String DAG = "guhs_einde2_zij_schildpad_dag";
    /** Below this y (from the bottom of the Guheinde) the scale catches you. */
    public static final int VANG_ONDER = 0;

    /** (not saved) the last firm ground of a player in the Guheinde, and when the scale caught them last. */
    private static final Map<UUID, Vec3> VEILIG = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> GEVANGEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> OP_POOT = new ConcurrentHashMap<>();

    private Leeuwschildpad() {
    }

    static Verhaallijn ln() {
        return ZijLijnen.SCHILDPAD;
    }

    static Item knabbel() {
        return ModItems.KAAS_KNABBELS.get();
    }

    static void register() {
        CastWezen.BIJ_KLIK.add(Leeuwschildpad::klik);
        Praat.luister(SLEUTEL_VRAAG, (p, spreker, optie) -> antwoord(p, spreker, optie));
    }

    static void tick(ServerPlayer p) {
        int stap = ln().stap(p);
        if (stap > 1) {
            return;
        }
        ZijPlek.Kopie k = ZijPlek.bij(p, S, 4);
        if (k == null) {
            OP_POOT.remove(p.getUUID());
            return;
        }
        if (stap == 0 && k.bij("schild_top", p.position(), 4.5)) {
            ln().begin(p);
            if (ln().verder(p, 0)) {
                Zij.geluid(p, SoundEvents.SNIFFER_SNIFFING, 1f, 0.4f);
                Zij.geluid(p, SoundEvents.TURTLE_AMBIENT_LAND, 1f, 0.5f);
                Zij.stem(p, naam(), Q + "kietel");
                Zij.hint(p, Q + "hint.poot");
            }
            return;
        }
        boolean op = k.bij("zegen_plek", p.position(), 2.5);
        Boolean was = OP_POOT.put(p.getUUID(), op);
        if (stap == 1 && op && !Boolean.TRUE.equals(was)) {
            CastWezen kop = kop(p.level(), k);
            if (kop != null) {
                kop.zetAnimatie("ontwaak");
                vraag(p, kop);
            } else {
                Zij.stem(p, naam(), Q + "slaap");
            }
        }
    }

    private static void vraag(ServerPlayer p, Entity kop) {
        Zij.geluid(p, SoundEvents.TURTLE_AMBIENT_LAND, 1f, 0.4f);
        Praat.scene(p, SLEUTEL_VRAAG, List.of(new Praat.Regel(kop, Q + "naam", Q + "wakker.1"), new Praat.Regel(kop, Q + "naam", Q + "wakker.2"),
                new Praat.Regel(kop, Q + "naam", Q + "vraag")),
                new Praat.Optie(1, Q + "antwoord.1"), new Praat.Optie(2, Q + "antwoord.2"), new Praat.Optie(3, Q + "antwoord.3"));
    }

    private static void antwoord(ServerPlayer p, Entity kop, int optie) {
        if (optie < 1 || optie > 3 || ln().stap(p) != 1) {
            return;
        }
        ln().teller(p, "keuze", optie);
        Zij.stem(p, naam(), Q + "wijs." + optie);
        if (ln().verder(p, 1)) {
            Zij.stem(p, naam(), Q + "offer");
            Zij.hint(p, Q + "hint.knabbel");
        }
    }

    static boolean klik(CastWezen w, ServerPlayer p) {
        if (!w.wie().equals("leeuwschildpad_guh") || !Zij.plek(w).startsWith(S + "/")) {
            return false;
        }
        int stap = ln().stap(p);
        ZijPlek.Kopie k = ZijPlek.bij(p.level(), w.blockPosition(), S, 32);
        switch (stap) {
            case 0 -> Zij.stem(p, naam(), Q + "slaap");
            case 1 -> {
                if (k != null && k.bij("zegen_plek", p.position(), 2.5)) {
                    vraag(p, w);
                } else {
                    Zij.stem(p, naam(), Q + "kom_op_poot");
                }
            }
            case 2 -> {
                ItemStack hand = p.getItemInHand(InteractionHand.MAIN_HAND);
                if (!hand.is(knabbel())) {
                    Zij.stem(p, naam(), Q + "offer_nog");
                    Zij.hint(p, Q + "hint.knabbel");
                    return true;
                }
                if (k == null || !k.bij("zegen_plek", p.position(), 3.5)) {
                    Zij.stem(p, naam(), Q + "kom_op_poot");
                    return true;
                }
                hand.shrink(1);
                zegen(p, w, k);
            }
            default -> wijsheid(p);
        }
        return true;
    }

    private static void zegen(ServerPlayer p, CastWezen kop, ZijPlek.Kopie k) {
        BlockPos anker = k.pos("zegen_plek");
        kop.zetAnimatie("praat");
        Runnable klaar = () -> {
            if (ln().verder(p, 2)) {
                kop.zetAnimatie("zegen");
                ZijEvents.later(60, () -> kop.zetAnimatie("idle"));
                Zij.eenmalig(ln(), p, "zegen", new ItemStack(E2ZijFeature.ZEGENSCHUB.get()));
                p.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 0.8, 0.6, 0.3);
                Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.8f, 0.8f);
                Zij.stem(p, naam(), Q + "klaar");
            }
        };
        if (anker == null || Cutscenes.bezig(p) || !Cutscenes.speel(p, ZijScenes.ZEGEN, anker, Rotation.NONE, speler -> klaar.run())) {
            klaar.run();
        }
    }

    /** After the line: one chonky wisdom a day (and otherwise a sleepy rumble). */
    private static void wijsheid(ServerPlayer p) {
        long dag = nl.juiced.guhs.world.GuhTime.day(p.level());
        if (GuhQuests.saved(p).getLongOr(DAG, -1L) == dag) {
            Zij.stem(p, naam(), Q + "morgen");
            return;
        }
        GuhQuests.saved(p).putLong(DAG, dag);
        Zij.stem(p, naam(), Q + "dagwijsheid." + Math.floorMod(dag, 8));
    }

    private static CastWezen kop(ServerLevel level, ZijPlek.Kopie k) {
        BlockPos pos = k.pos("kop");
        return pos == null ? null : Zij.wezen(level, Vec3.atCenterOf(pos), "leeuwschildpad_guh", 12);
    }

    static String naam() {
        return Q + "naam";
    }

    // --- the blessing: the scale catches you above the void ---------------------------------------------------------------------

    /** Every tick of a player in the Guheinde: remember firm ground; below the islands with the scale: caught. */
    static void vang(ServerPlayer p) {
        if (p.level().dimension() != GuheindeFeature.GUHEINDE || p.isSpectator() || p.isCreative()) {
            return;
        }
        UUID id = p.getUUID();
        if (p.onGround() && p.tickCount % 10 == 0 && !p.level().getBlockState(p.blockPosition().below()).isAir()) {
            VEILIG.put(id, p.position());
        }
        if (p.getY() > p.level().getMinY() + VANG_ONDER || GuhQuests.count(p, E2ZijFeature.ZEGENSCHUB.get()) <= 0) {
            return;
        }
        long nu = p.level().getGameTime();
        Long laatst = GEVANGEN.get(id);
        if (laatst != null && nu - laatst < 60) {
            return;
        }
        GEVANGEN.put(id, nu);
        Vec3 terug = VEILIG.get(id);
        ServerLevel level = p.level();
        if (terug == null) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GuheindeGevecht.ARRIVAL_X, GuheindeGevecht.ARRIVAL_Z);
            terug = new Vec3(GuheindeGevecht.ARRIVAL_X + 0.5, Math.max(y, level.getMinY() + 64), GuheindeGevecht.ARRIVAL_Z + 0.5);
        }
        p.teleportTo(level, terug.x, terug.y + 0.5, terug.z, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
        level.sendParticles(ParticleTypes.END_ROD, terug.x, terug.y + 1, terug.z, 40, 0.5, 1, 0.5, 0.05);
        Zij.geluid(p, SoundEvents.TURTLE_AMBIENT_LAND, 1f, 0.5f);
        p.sendOverlayMessage(Component.translatable(Q + "gevangen").withStyle(ChatFormatting.AQUA));
    }

    static void vergeet(UUID id) {
        VEILIG.remove(id);
        GEVANGEN.remove(id);
        OP_POOT.remove(id);
    }
}
