package nl.juiced.guhs.feature.paleizen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.spiesburcht.NetherMikaRuil;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2 (paleizen): "Soep van Mika-oma", the questline of the Mika-woonblokken (Verhaallijn {@code mika_oma}, per player):
 * <ol start="0">
 *   <li>talk to Mika-oma on the top gallery: she gives three bowls of worstsoep;</li>
 *   <li>bring a bowl to each of the grumpy three ({@link MopperMikaEntity} 0, 1, 2: right-click them with the soup; flags
 *       {@code soep_<nr>}), then tell her (text variant {@code 1_klaar});</li>
 *   <li>find her knitting: the basket on the roof garden ({@link BreiwerkBlock}; the block stays for the next player);</li>
 *   <li>bring it back: she knits the Mika hat, and from then on every third barter with a Nether-Mika gives the ingot back
 *       ({@link #geruild}).</li>
 * </ol>
 * A quest item that got lost comes again from where it came (the soup from oma, the knitting from the basket).
 */
public final class OmaQuest {
    static final String Q = "quest.guhs.paleizen.oma.", MOP = "quest.guhs.paleizen.mopper.", GUI = "gui.guhs.paleizen.";
    /** Every this many barters with a Nether-Mika is free for whoever helped Mika-oma. */
    public static final int KORTING_ELKE = 3;

    public static final Verhaallijn LIJN = Verhaallijn.maak("mika_oma", "barbecue").stappen(4).icoon("guhs:paleizen_omasoep")
            .sleutel((p, stap) -> stap == 1 && soepOver(p) == 0 ? "1_klaar" : String.valueOf(stap))
            .extraSleutels("1_klaar")
            .nodig((p, stap) -> stap == 1 && soepOver(p) > 0
                    ? List.of(Verhaallijn.nodig("guhs:paleizen_omasoep", GuhQuests.count(p, PaleizenFeature.OMASOEP.get()), soepOver(p)))
                    : stap == 3 ? List.of(Verhaallijn.nodig("guhs:paleizen_breiwerkje", GuhQuests.count(p, PaleizenFeature.BREIWERKJE.get()), 1)) : List.of())
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:paleizen_mikamuts", klaar(p)),
                    Verhaallijn.beloning("guhs:vahoege_vads_ingot", GUI + "beloning.korting", klaar(p))))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, PaleisPlekken.WOONBLOKKEN,
                    Component.translatable("structure.guhs." + PaleisPlekken.WOONBLOKKEN)))
            .registreer();

    static final Rol ROL = new Rol();

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.MIKA_OMA, ROL);
        Bezetting.npc("paleizen_mika_oma", PaleisPlekken.WOONBLOKKEN, null, PaleisPlekken.Woon.MIKA_OMA, GuhNpcEntity.Kind.MIKA_OMA, null, 0f);
        for (int nr = 0; nr < PaleisPlekken.Woon.MOPPERS.length; nr++) {
            int n = nr;
            Bezetting.wezen("paleizen_mopper_" + nr, PaleisPlekken.WOONBLOKKEN, null, PaleisPlekken.Woon.MOPPERS[nr], (level, plek, draai) -> {
                MopperMikaEntity m = PaleizenFeature.MOPPER_MIKA.get().create(level, EntitySpawnReason.STRUCTURE);
                if (m == null) {
                    return null;
                }
                m.setNr(n);
                m.setYRot(PaleisPlekken.Woon.MOPPER_YAW[n]);
                float yaw = m.rotate(draai);
                m.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
                m.setYBodyRot(yaw);
                m.setYHeadRot(yaw);
                return m;
            }, 6);
        }
    }

    static boolean klaar(ServerPlayer p) {
        return LIJN.klaar(p);
    }

    /** How many of the grumpy three still wait for this player's soup. */
    public static int soepOver(ServerPlayer p) {
        int over = 0;
        for (int nr = 0; nr < MopperMikaEntity.MOPPERAARS; nr++) {
            over += LIJN.vlag(p, "soep_" + nr) ? 0 : 1;
        }
        return over;
    }

    /** Does neighbour {@code nr} still grumble at this player (they are on the soup round and it has had none)? */
    public static boolean moppertTegen(ServerPlayer p, int nr) {
        return nr < MopperMikaEntity.MOPPERAARS && LIJN.stap(p) == 1 && !LIJN.vlag(p, "soep_" + nr);
    }

    // --- Mika-oma --------------------------------------------------------------------------------------------------------

    static final class Rol extends QuestRol {
        Rol() {
            super(LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            switch (stap) {
                case 0 -> {
                    LIJN.begin(p);
                    zeg(p, npc, Q + "hallo");
                    scherm(p, npc, Q + "vraag", new Praat.Optie(1, Q + "optie.ja"), new Praat.Optie(2, Q + "optie.wie"));
                }
                case 1 -> {
                    int over = soepOver(p);
                    if (over == 0) {
                        if (verder(p, 1)) {
                            scherm(p, npc, Q + "breiwerk", new Praat.Optie(3, Q + "optie.zoek"));
                            hint(p, Q + "hint.breiwerk");
                        }
                        return;
                    }
                    int heb = GuhQuests.count(p, PaleizenFeature.OMASOEP.get());
                    if (heb < over) {
                        geef(p, new ItemStack(PaleizenFeature.OMASOEP.get(), over - heb));
                        zeg(p, npc, Q + "soep_kwijt");
                    } else {
                        zeg(p, npc, Q + "soep_nog", over);
                    }
                    hint(p, Q + "hint.soep");
                }
                case 2 -> {
                    zeg(p, npc, Q + "breiwerk_nog");
                    hint(p, Q + "hint.breiwerk");
                }
                case 3 -> {
                    if (!neem(p, PaleizenFeature.BREIWERKJE.get(), 1)) {
                        zeg(p, npc, Q + "breiwerk_nog");   // (lost on the way: the basket gives it again)
                        hint(p, Q + "hint.breiwerk");
                    } else if (verder(p, 3)) {
                        scherm(p, npc, Q + "klaar", new Praat.Optie(4, Q + "optie.dank"));
                        geefEenmalig(p, "muts", new ItemStack(ModItems.clothingItem(GuhClothes.PALEIZEN_MIKAMUTS)));
                        zichtbaar(p, "barbecuether/paleizen_mika_oma");
                        p.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.3f);
                    }
                }
                default -> zeg(p, npc, Q + "dank" + p.getRandom().nextInt(3));
            }
        }

        @Override
        protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
            if (stap != 0) {
                return;
            }
            if (optie == 2) {
                scherm(p, npc, Q + "uitleg", new Praat.Optie(1, Q + "optie.ja"));
            } else if (optie == 1 && verder(p, 0)) {
                geef(p, new ItemStack(PaleizenFeature.OMASOEP.get(), MopperMikaEntity.MOPPERAARS));
                zeg(p, npc, Q + "soep");
                hint(p, Q + "hint.soep");
            }
        }
    }

    // --- the neighbours ----------------------------------------------------------------------------------------------------

    /** A player right-clicked a neighbour: a grumble, a line, or (on the soup round, with soup in that hand) a happy slurp. */
    public static void klik(ServerPlayer p, MopperMikaEntity m, InteractionHand hand) {
        int nr = m.getNr();
        if (!m.isMopperaar()) {
            GuhQuests.say(p, m, MOP + nr + (p.getRandom().nextBoolean() ? ".a" : ".b"));
            return;
        }
        if (LIJN.vlag(p, "soep_" + nr)) {
            GuhQuests.say(p, m, MOP + nr + ".tevreden");
            return;
        }
        ItemStack stack = p.getItemInHand(hand);
        boolean soep = stack.is(PaleizenFeature.OMASOEP.get());
        if (LIJN.stap(p) != 1) {
            GuhQuests.say(p, m, soep ? MOP + "vreemde" : MOP + nr + ".mopper");
            return;
        }
        if (!soep) {
            GuhQuests.say(p, m, MOP + nr + ".mopper");
            GuhQuests.hint(p, MOP + "geen_soep");
            return;
        }
        if (!p.getAbilities().instabuild) {
            stack.shrink(1);
        }
        LIJN.vlag(p, "soep_" + nr, true);
        GuhQuests.say(p, m, MOP + nr + ".soep");
        m.blij();
        ServerLevel level = p.level();
        level.playSound(null, m, SoundEvents.GENERIC_DRINK.value(), SoundSource.NEUTRAL, 0.8f, 0.9f);
        level.sendParticles(p, ParticleTypes.HEART, false, false, m.getX(), m.getY() + m.getBbHeight() + 0.3, m.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        int over = soepOver(p);
        p.sendOverlayMessage((over > 0 ? Component.translatable(GUI + "soep.nog", over) : Component.translatable(GUI + "soep.klaar"))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // --- the knitting ------------------------------------------------------------------------------------------------------

    /** A player right-clicked the knitting basket: on the right step they take the knitting out (the basket stays). */
    public static void breiwerk(ServerPlayer p, BlockPos pos) {
        int stap = LIJN.stap(p);
        String tekst;
        if (stap < 2) {
            tekst = "breiwerk.niet_nu";
        } else if (stap > 3) {
            tekst = "breiwerk.klaar";
        } else if (stap == 3 && GuhQuests.count(p, PaleizenFeature.BREIWERKJE.get()) > 0) {
            tekst = "breiwerk.al";
        } else {
            LIJN.verder(p, 2);
            Minigames.give(p, new ItemStack(PaleizenFeature.BREIWERKJE.get()));
            p.level().playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.9f, 1.3f);
            GuhQuests.hint(p, Q + "hint.terug");
            tekst = "breiwerk.gevonden";
        }
        p.sendOverlayMessage(Component.translatable(GUI + tekst).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // --- the discount ------------------------------------------------------------------------------------------------------

    /**
     * A friend of Mika-oma holds out an ingot to a Nether-Mika (called before the Nether-Mika's own handler,
     * {@link NetherMikaRuil#offer}): when the Mika is going to take it (it is not angry at them and not sniffing at another
     * ingot), the barter counts for the discount.
     */
    static void ruil(ServerPlayer p, Entity target, ItemStack stack) {
        if (klaar(p) && NetherMikaRuil.isNetherMika(target) && stack.is(ModItems.VAHOEGE_VADS_INGOT.get()) && target instanceof MikaEntity mika
                && !NetherMikaRuil.isAngryAt(mika, p) && !NetherMikaRuil.isAdmiring(mika)) {
            geruild(p);
        }
    }

    /** A barter of a friend of Mika-oma: every {@link #KORTING_ELKE}th one the ingot comes back. True when it did. */
    public static boolean geruild(ServerPlayer p) {
        if (!klaar(p)) {
            return false;
        }
        int n = LIJN.teller(p, "ruil") + 1;
        LIJN.teller(p, "ruil", n);
        if (n % KORTING_ELKE != 0) {
            return false;
        }
        Minigames.give(p, new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get()));
        p.sendOverlayMessage(Component.translatable(GUI + "korting").withStyle(ChatFormatting.GOLD));
        GuhAdvancements.grant(p, "paleizen_korting");
        return true;
    }

    private OmaQuest() {
    }
}
