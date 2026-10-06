package nl.juiced.guhs.feature.ringh3;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-h3): the fellowship in the mine. They stand in the template ({@link Plekken#CAST}; python ring.cast) and every one
 * of them only exists for a player whose own step of ring_h3 is in that character's range ({@link Zicht#alleenBij}): at the
 * gate while you puzzle over it, in the lever hall, round the well, and outside the east gate when it is over. A copy that
 * lost one gets it back ({@link Bezetting}).
 * <p>
 * Who has something to say that matters (a role per "plek"; everybody else keeps the small talk of ring-kern's cast):
 * Guhdalf at the gate (hints for the riddle, each one a little clearer), Guhdalf in the lever hall, Gimguh at his door (what
 * to knock on), Pippguh and Guhdalf at the well, and Araguh outside: talking to him ends the chapter.
 */
public final class Rollen {
    private Rollen() {
    }

    private static Verhaallijn lijn() {
        return RingH3Feature.LIJN;
    }

    static void registreer() {
        NpcRollen.zet(GuhNpcEntity.Kind.GUHDALF, "ringh3_poort", (NpcRole) Rollen::guhdalfPoort);
        NpcRollen.zet(GuhNpcEntity.Kind.GUHDALF, "ringh3_hal", (NpcRole) (npc, p) -> zeg(npc, p, "quest.guhs.ringh3.guhdalf.hal." + p.getRandom().nextInt(2)));
        NpcRollen.zet(GuhNpcEntity.Kind.GUHDALF, "ringh3_put_guhdalf", (NpcRole) (npc, p) -> zeg(npc, p, "quest.guhs.ringh3.guhdalf.put"));
        NpcRollen.zet(GuhNpcEntity.Kind.PIPPGUH, "ringh3_put", (NpcRole) (npc, p) -> zeg(npc, p, "quest.guhs.ringh3.pippguh.put." + p.getRandom().nextInt(3)));
        NpcRollen.zet(GuhNpcEntity.Kind.GIMGUH, "ringh3_gang", (NpcRole) Rollen::gimguh);
        NpcRollen.zet(GuhNpcEntity.Kind.ARAGUH, "ringh3_buiten", (NpcRole) Rollen::araguh);
        for (Plekken.Rol rol : Plekken.CAST) {
            Bezetting.wezen(rol.id(), Mijn.STRUCTUUR, null, rol.plek(), (level, plek, draai) -> maak(level, rol, plek, draai));
        }
    }

    private static void zeg(GuhNpcEntity npc, ServerPlayer p, String key, Object... args) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, npc.getKind() == GuhNpcEntity.Kind.GUHDALF ? 0.75f : 1f);
        GuhQuests.say(p, npc, key, args);
    }

    /** Guhdalf at the gate: three attempts of his own, then he starts to think aloud; after the riddle he is just pleased. */
    private static void guhdalfPoort(GuhNpcEntity npc, ServerPlayer p) {
        Ring.behaald(p, "ring_guhdalf");
        if (!Ring.aanZet(p, lijn(), 1)) {
            zeg(npc, p, lijn().stap(p) > 1 ? "quest.guhs.ringh3.guhdalf.poort.open" : "quest.guhs.ringh3.guhdalf.poort.0");
            return;
        }
        int n = lijn().teller(p, "guhdalf");
        lijn().teller(p, "guhdalf", n + 1);
        zeg(npc, p, "quest.guhs.ringh3.guhdalf.poort." + Math.min(n, 4));
    }

    /** Gimguh at his door: what to do, and (when you come back empty-handed) where to look. */
    private static void gimguh(GuhNpcEntity npc, ServerPlayer p) {
        int stap = lijn().stap(p);
        if (stap < 4) {
            zeg(npc, p, "quest.guhs.ringh3.gimguh.wacht");
        } else if (stap == 4) {
            int n = lijn().teller(p, "gimguh");
            lijn().teller(p, "gimguh", n + 1);
            zeg(npc, p, "quest.guhs.ringh3.gimguh.deur." + Math.min(n, 2));
        } else {
            zeg(npc, p, "quest.guhs.ringh3.gimguh.open");
        }
    }

    /** Araguh outside the east gate: the end of the chapter, and where the road goes on. */
    private static void araguh(GuhNpcEntity npc, ServerPlayer p) {
        if (Ring.aanZet(p, lijn(), 6) && lijn().verder(p, 6)) {
            zeg(npc, p, "quest.guhs.ringh3.araguh.einde");
            Ring.behaald(p, "ring_h3_klaar");
            nl.juiced.guhs.feature.Minigames.give(p, new net.minecraft.world.item.ItemStack(RingH3Feature.RUNE_ITEM.get(), 4));
            p.sendSystemMessage(Component.translatable("quest.guhs.ringh3.klaar").withStyle(ChatFormatting.GOLD));
            Ring.vertelDoel(p, npc);
            return;
        }
        zeg(npc, p, "quest.guhs.ringh3.araguh.na");
    }

    /** A cast character for a copy that lost it (what {@link Bezetting} asks for): not yet in the world. */
    @Nullable
    private static Entity maak(ServerLevel level, Plekken.Rol rol, Vec3 plek, Rotation draai) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
        if (npc == null) {
            return null;
        }
        for (GuhNpcEntity.Kind kind : GuhNpcEntity.Kind.values()) {
            if (kind.id().equals(rol.kind())) {
                npc.setKind(kind);
            }
        }
        float yaw = Cutscene.wereldYaw(draai, rol.yaw());
        npc.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setInvulnerable(true);
        if (rol.rol() != null) {
            npc.roleData.putString(NpcRollen.PLEK, rol.rol());
        }
        Zicht.alleenBij(npc, lijn().id(), rol.van(), rol.tot());
        return npc;
    }
}
