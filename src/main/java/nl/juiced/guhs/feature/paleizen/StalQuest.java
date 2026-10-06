package nl.juiced.guhs.feature.paleizen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.boerderij.GuhVoerbakBlock;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Herstel;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (paleizen): "De onrustige Worstzwijntjes", the questline of the Mika-stal (Verhaallijn {@code stalknecht}, per player):
 * <ol start="0">
 *   <li>talk to the Stalknecht-guh in front of the great door;</li>
 *   <li>pet three of the stable's Worstzwijntjes calm: an empty hand, {@link #AAIEN} strokes each (counters {@code aai_<nr>},
 *       flags {@code kalm_<nr>}); then back to him (text variant {@code 1_klaar}) for a sack of feed;</li>
 *   <li>empty the sack into the voerbak at the end of the aisle ({@link #vulVoerbak}: it shows full for half a minute, the
 *       animals eat; flag {@code gevoerd}, variant {@code 2_klaar}); back to him: Knorretje has run away;</li>
 *   <li>find the runaway ({@link #tick} makes one for every player who is looking: a {@link WorstzwijntjeEntity} that hides and
 *       bolts), pick it up ({@link #pak}: variant {@code 3_gevangen}) and bring it to him.</li>
 * </ol>
 * Reward: two Worstzwijntjes in a basket ({@link MandjeItem}). The stable's own animals are never used up: they are only
 * calm "for you".
 */
public final class StalQuest {
    static final String Q = "quest.guhs.paleizen.stal.", GUI = "gui.guhs.paleizen.";
    /** Strokes it takes to calm one Worstzwijntje, and how many of them a player calms. */
    public static final int AAIEN = 3, KALM = 3;
    /** How long the stable's voerbak shows full after a player filled it (ticks). */
    public static final int VOERBAK_TICKS = 600;

    public static final Verhaallijn LIJN = Verhaallijn.maak("stalknecht", "barbecue").stappen(4).icoon("guhs:paleizen_worstzwijntje_mandje")
            .sleutel((p, stap) -> stap == 1 && aantalKalm(p) >= KALM ? "1_klaar" : stap == 2 && gevoerd(p) ? "2_klaar"
                    : stap == 3 && heeftKnorretje(p) ? "3_gevangen" : String.valueOf(stap))
            .extraSleutels("1_klaar", "2_klaar", "3_gevangen")
            .nodig((p, stap) -> stap == 2 && !gevoerd(p)
                    ? List.of(Verhaallijn.nodig("guhs:paleizen_zwijnenvoer", GuhQuests.count(p, PaleizenFeature.ZWIJNENVOER.get()), 1))
                    : stap == 3 ? List.of(Verhaallijn.nodig("guhs:paleizen_gevangen_zwijntje", GuhQuests.count(p, PaleizenFeature.GEVANGEN_ZWIJNTJE.get()), 1))
                    : List.of())
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:paleizen_worstzwijntje_mandje", GUI + "beloning.zwijntjes", klaar(p))))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, PaleisPlekken.STAL, Component.translatable("structure.guhs." + PaleisPlekken.STAL)))
            .registreer();

    static final Rol ROL = new Rol();

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.STALKNECHTGUH, ROL);
        Bezetting.npc("paleizen_stalknecht", PaleisPlekken.STAL, null, PaleisPlekken.Stal.STALKNECHTGUH, GuhNpcEntity.Kind.STALKNECHTGUH, null, -90f);
        for (int nr = 0; nr < PaleisPlekken.Stal.ZWIJNTJES.length; nr++) {
            int n = nr;
            Bezetting.wezen("paleizen_stal_" + nr, PaleisPlekken.STAL, null, PaleisPlekken.Stal.ZWIJNTJES[nr],
                    (level, plek, draai) -> stalzwijntje(level, plek, draai, n), 20);
        }
    }

    /** One of the stable's own Worstzwijntjes (number 4 is the piglet in the paddock), not yet in the world. */
    @Nullable
    static WorstzwijntjeEntity stalzwijntje(ServerLevel level, Vec3 plek, Rotation draai, int nr) {
        WorstzwijntjeEntity z = PaleizenFeature.WORSTZWIJNTJE.get().create(level, EntitySpawnReason.STRUCTURE);
        if (z == null) {
            return null;
        }
        z.setYRot(PaleisPlekken.Stal.ZWIJNTJE_YAW[nr]);
        float yaw = z.rotate(draai);
        z.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        z.setYBodyRot(yaw);
        z.setYHeadRot(yaw);
        z.zetStal(nr, nr == 4);
        return z;
    }

    static boolean klaar(ServerPlayer p) {
        return LIJN.klaar(p);
    }

    /** How many Worstzwijntjes this player has petted calm. */
    public static int aantalKalm(ServerPlayer p) {
        int n = 0;
        for (int nr = 0; nr < PaleisPlekken.Stal.ZWIJNTJES.length; nr++) {
            n += LIJN.vlag(p, "kalm_" + nr) ? 1 : 0;
        }
        return n;
    }

    static boolean gevoerd(ServerPlayer p) {
        return LIJN.vlag(p, "gevoerd");
    }

    static boolean heeftKnorretje(ServerPlayer p) {
        return GuhQuests.count(p, PaleizenFeature.GEVANGEN_ZWIJNTJE.get()) > 0;
    }

    /** Is this player looking for the runaway now (step 3, not caught yet)? */
    public static boolean zoekt(ServerPlayer p) {
        return LIJN.stap(p) == 3 && !heeftKnorretje(p);
    }

    // --- the Stalknecht-guh --------------------------------------------------------------------------------------------------

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
                    scherm(p, npc, Q + "vraag", new Praat.Optie(1, Q + "optie.ja"), new Praat.Optie(2, Q + "optie.wat"));
                }
                case 1 -> {
                    int kalm = aantalKalm(p);
                    if (kalm < KALM) {
                        zeg(p, npc, Q + "aai_nog", KALM - kalm);
                        hint(p, Q + "hint.aai");
                    } else if (verder(p, 1)) {
                        geef(p, new ItemStack(PaleizenFeature.ZWIJNENVOER.get()));
                        zeg(p, npc, Q + "voer");
                        hint(p, Q + "hint.voer");
                    }
                }
                case 2 -> {
                    if (gevoerd(p)) {
                        if (verder(p, 2)) {
                            scherm(p, npc, Q + "ontsnapt", new Praat.Optie(3, Q + "optie.zoek"));
                            hint(p, Q + "hint.zoek");
                        }
                    } else if (geefAlsKwijt(p, PaleizenFeature.ZWIJNENVOER.get())) {
                        zeg(p, npc, Q + "voer_kwijt");
                    } else {
                        zeg(p, npc, Q + "voer_nog");
                        hint(p, Q + "hint.voer");
                    }
                }
                case 3 -> {
                    if (!neem(p, PaleizenFeature.GEVANGEN_ZWIJNTJE.get(), 1)) {
                        zeg(p, npc, Q + "zoek_nog");
                        hint(p, Q + "hint.zoek");
                    } else if (verder(p, 3)) {
                        scherm(p, npc, Q + "klaar", new Praat.Optie(4, Q + "optie.dank"));
                        geefEenmalig(p, "zwijntjes", new ItemStack(PaleizenFeature.MANDJE.get()), new ItemStack(PaleizenFeature.MANDJE.get()));
                        zichtbaar(p, "barbecuether/paleizen_stalknecht");
                        ServerLevel level = p.level();
                        level.playSound(null, npc, PaleizenFeature.KNOR.get(), SoundSource.NEUTRAL, 1f, 1.2f);
                        level.playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.3f);
                        level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.6, npc.getZ(), 6, 0.5, 0.3, 0.5, 0.0);
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
                zeg(p, npc, Q + "aai");
                hint(p, Q + "hint.aai");
            }
        }
    }

    // --- petting ---------------------------------------------------------------------------------------------------------

    /** Is this stable animal restless for somebody near: a player on the petting step who has not calmed it yet? */
    public static boolean onrustig(WorstzwijntjeEntity z) {
        if (!(z.level() instanceof ServerLevel level) || !z.isStal()) {
            return false;
        }
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, z.getBoundingBox().inflate(10))) {
            if (LIJN.stap(p) == 1 && !LIJN.vlag(p, "kalm_" + z.stalNr())) {
                return true;
            }
        }
        return false;
    }

    /** A player strokes one of the stable's Worstzwijntjes (a right-click; the quest only counts an empty hand). */
    public static void aai(ServerPlayer p, WorstzwijntjeEntity z) {
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.HEART, z.getX(), z.getY() + z.getBbHeight() + 0.2, z.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
        z.triggerAnim("actie", "blij");
        z.playSound(PaleizenFeature.KNOR.get(), 0.8f, z.getVoicePitch());
        if (LIJN.stap(p) != 1 || !p.getMainHandItem().isEmpty()) {
            return;
        }
        int nr = z.stalNr();
        if (LIJN.vlag(p, "kalm_" + nr)) {
            bericht(p, Component.translatable(GUI + "aai.al"));
            return;
        }
        int n = LIJN.teller(p, "aai_" + nr) + 1;
        LIJN.teller(p, "aai_" + nr, n);
        if (n < AAIEN) {
            bericht(p, Component.translatable(GUI + "aai." + Math.min(n, 2)));
            return;
        }
        LIJN.vlag(p, "kalm_" + nr, true);
        int kalm = aantalKalm(p);
        level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, false, false, z.getX(), z.getY() + 0.6, z.getZ(), 8, 0.4, 0.3, 0.4, 0.0);
        bericht(p, Component.translatable(kalm >= KALM ? GUI + "aai.klaar" : GUI + "aai.3", kalm));
    }

    // --- the voerbak -----------------------------------------------------------------------------------------------------

    /**
     * A player holds the sack of feed against a voerbak: at the stable's own voerbak, on the feeding step, it is emptied into
     * it (true: the click is ours). The voerbak shows full for a while and the stable's animals eat; nothing is used up for
     * the next player.
     */
    public static boolean vulVoerbak(ServerPlayer p, BlockPos pos, ItemStack zak) {
        ServerLevel level = p.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GuhVoerbakBlock) || !zak.is(PaleizenFeature.ZWIJNENVOER.get())) {
            return false;
        }
        if (LIJN.stap(p) != 2 || gevoerd(p) || !PaleisPlekken.is(level, PaleisPlekken.STAL, pos, PaleisPlekken.Stal.VOERBAK)) {
            return true;   // (the sack only goes into the stable's voerbak; nothing happens anywhere else)
        }
        if (!p.getAbilities().instabuild) {
            zak.shrink(1);
        }
        LIJN.vlag(p, "gevoerd", true);
        level.setBlock(pos, state.setValue(GuhVoerbakBlock.VOER, GuhVoerbakBlock.MAX), Block.UPDATE_ALL);
        Herstel.na(level, pos, state.setValue(GuhVoerbakBlock.VOER, 0), VOERBAK_TICKS);
        level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1f, 1.1f);
        for (WorstzwijntjeEntity z : level.getEntitiesOfClass(WorstzwijntjeEntity.class, new AABB(pos).inflate(24, 8, 24), WorstzwijntjeEntity::isStal)) {
            z.triggerAnim("actie", "eet");
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, z.getX(), z.getY() + 0.7, z.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        }
        level.playSound(null, pos, PaleizenFeature.KNOR.get(), SoundSource.NEUTRAL, 1f, 1.0f);
        bericht(p, Component.translatable(GUI + "voerbak"));
        return true;
    }

    // --- the runaway -----------------------------------------------------------------------------------------------------

    /** The runaway of this player near them (null: none). */
    @Nullable
    public static WorstzwijntjeEntity knorretje(ServerPlayer p) {
        ServerLevel level = p.level();
        for (WorstzwijntjeEntity z : level.getEntitiesOfClass(WorstzwijntjeEntity.class, p.getBoundingBox().inflate(96), e -> p.getUUID().equals(e.ontsnaptVan()))) {
            if (z.isAlive()) {
                return z;
            }
        }
        return null;
    }

    /**
     * Once a second for a player: when they are looking for Knorretje at a Mika-stal and theirs is not there, it turns up
     * at one of its hiding spots. Returns the runaway it made (null: nothing to do).
     */
    @Nullable
    public static WorstzwijntjeEntity tick(ServerPlayer p, @Nullable StructureStart stal) {
        if (stal == null || !zoekt(p) || p.isSpectator() || knorretje(p) != null) {
            return null;
        }
        ServerLevel level = p.level();
        List<Vec3> plekken = new ArrayList<>();
        for (BlockPos lokaal : PaleisPlekken.Stal.SCHUILPLEKKEN) {
            Vec3 plek = PaleisPlekken.voet(stal, lokaal);
            if (plek != null && level.isLoaded(BlockPos.containing(plek))) {
                plekken.add(plek);
            }
        }
        if (plekken.isEmpty()) {
            return null;
        }
        WorstzwijntjeEntity z = PaleizenFeature.WORSTZWIJNTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (z == null) {
            return null;
        }
        int eerste = LIJN.teller(p, "schuil");
        LIJN.teller(p, "schuil", eerste + 1);
        z.zetOntsnapt(p.getUUID(), plekken, eerste);
        Vec3 plek = plekken.get(z.schuilplek());
        z.snapTo(plek.x, plek.y, plek.z, p.getRandom().nextFloat() * 360f, 0f);
        level.addFreshEntity(z);
        return z;
    }

    /** A player right-clicked a runaway: its own player picks it up (it becomes the quest item), anyone else hears whose it is. */
    public static void pak(ServerPlayer p, WorstzwijntjeEntity z) {
        ServerLevel level = p.level();
        if (!p.getUUID().equals(z.ontsnaptVan())) {
            Player van = z.ontsnaptVan() == null ? null : level.getPlayerByUUID(z.ontsnaptVan());
            bericht(p, Component.translatable(GUI + "niet_van_jou", van == null ? Component.literal("?") : van.getDisplayName()));
            return;
        }
        if (!zoekt(p)) {
            z.discard();
            return;
        }
        level.sendParticles(ParticleTypes.POOF, z.getX(), z.getY() + 0.4, z.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.HEART, z.getX(), z.getY() + 0.9, z.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        level.playSound(null, z, PaleizenFeature.KNOR.get(), SoundSource.NEUTRAL, 1f, 1.3f);
        z.discard();
        Minigames.give(p, new ItemStack(PaleizenFeature.GEVANGEN_ZWIJNTJE.get()));
        bericht(p, Component.translatable(GUI + "gevangen"));
    }

    private static void bericht(ServerPlayer p, Component tekst) {
        p.sendOverlayMessage(tekst.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private StalQuest() {
    }
}
