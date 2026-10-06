package nl.juiced.guhs.feature.ring;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-kern): the cast of the Knabbelring as characters in the world (their models, textures and the named cutscene
 * animations: client.RingClient / client.CastAnimaties and tools/features/ring_modellen.py).
 * <ul>
 *   <li>The kinds ({@link #KINDS}): Guhdalf, Smikagol (as a sitting character; the walking one is {@link SmikagolEntity}),
 *       Araguh, Leguhlas, Gimguh, Boromika, Merrie, Pippguh, Guhrond and Guhladriel.</li>
 *   <li>In a <b>cutscene</b> a chapter just names them ({@code Cutscene.Builder.npc("guhdalf", Kind.GUHDALF, ...)}): those
 *       actors only exist in the viewer's game for the length of the scene.</li>
 *   <li>For a scene you walk around in (the council, the tree city) a chapter puts a real character down with
 *       {@link #zetBij} (or in its template: python {@code ring.cast(...)}): it only exists for players whose own step of
 *       that questline is in the given range ("the fellowship, ONLY in their own scenes"), so two players at different
 *       points of the story each see what belongs to their story.</li>
 *   <li>Every kind has a default role ({@link Rol}): a line that fits where the player is in the story (before, on the
 *       trip, afterwards: "characters live there with new chats"). A chapter gives a character its own role for a scene
 *       with {@code NpcRollen.zet(kind, "<plek>", rol)} and the plek in {@link #zet} / the template.</li>
 * </ul>
 */
public final class Cast {
    public static final List<GuhNpcEntity.Kind> KINDS = List.of(GuhNpcEntity.Kind.GUHDALF, GuhNpcEntity.Kind.SMIKAGOL, GuhNpcEntity.Kind.ARAGUH,
            GuhNpcEntity.Kind.LEGUHLAS, GuhNpcEntity.Kind.GIMGUH, GuhNpcEntity.Kind.BOROMIKA, GuhNpcEntity.Kind.MERRIE, GuhNpcEntity.Kind.PIPPGUH,
            GuhNpcEntity.Kind.GUHROND, GuhNpcEntity.Kind.GUHLADRIEL);
    /** The phases of the default lines, and how many lines each kind has per phase (quest.guhs.ring.cast.&lt;kind&gt;.&lt;fase&gt;.&lt;n&gt;). */
    public static final String VOOR = "voor", REIS = "reis", NA = "na";
    public static final int REGELS = 2;
    /** roleData of a Guhdalf: "wit" / "grijs" pins his look (else: white for a viewer whose chapter 3 is done). */
    public static final String GUHDALF_LOOK = "guhs_ring_look";

    static void registreer() {
        for (GuhNpcEntity.Kind kind : KINDS) {
            NpcRollen.zet(kind, new Rol(kind));
        }
    }

    /** Where this player is in the story, for a character's small talk. */
    public static String fase(ServerPlayer p) {
        return Ring.klaar(p) ? NA : Ring.begonnen(p) ? REIS : VOOR;
    }

    /**
     * Puts a real cast character in the world: it can't be hurt, never leaves and talks with the role of {@code plek}
     * (null: the default role of its kind). Everybody sees it; see {@link #zetBij} for "only in its own scene".
     */
    @Nullable
    public static GuhNpcEntity zet(ServerLevel level, GuhNpcEntity.Kind kind, Vec3 plek, float yaw, @Nullable String plekRol) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (npc == null) {
            return null;
        }
        npc.setKind(kind);
        npc.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setInvulnerable(true);
        if (plekRol != null) {
            npc.roleData.putString(NpcRollen.PLEK, plekRol);
        }
        level.addFreshEntity(npc);
        return npc;
    }

    /**
     * {@link #zet}, but the character only exists for players whose step of {@code lijn} is van..tot (inclusive; tot 99 =
     * from van on for ever, e.g. "lives here after the chapter"). Their game is told the moment their story gets there.
     */
    @Nullable
    public static GuhNpcEntity zetBij(ServerLevel level, GuhNpcEntity.Kind kind, Vec3 plek, float yaw, @Nullable String plekRol, String lijn, int van, int tot) {
        GuhNpcEntity npc = zet(level, kind, plek, yaw, plekRol);
        if (npc != null) {
            Zicht.alleenBij(npc, lijn, van, tot);
        }
        return npc;
    }

    /** The default role of a cast kind: small talk that fits the player's place in the story; Guhdalf also says what to do. */
    public record Rol(GuhNpcEntity.Kind kind) implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            npc.level().playSound(null, npc, isMika() ? ModSounds.MIKA_AMBIENT.get() : ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f,
                    kind == GuhNpcEntity.Kind.SMIKAGOL ? 1.7f : kind == GuhNpcEntity.Kind.GUHDALF ? 0.75f : 1f);
            String fase = fase(p);
            GuhQuests.say(p, npc, "quest.guhs.ring.cast." + kind.id() + "." + fase + "." + p.getRandom().nextInt(REGELS));
            if (kind == GuhNpcEntity.Kind.GUHDALF) {
                Ring.behaald(p, "ring_guhdalf");
                if (fase.equals(REIS)) {
                    Ring.vertelDoel(p, npc);
                }
            }
        }

        private boolean isMika() {
            return kind == GuhNpcEntity.Kind.SMIKAGOL || kind == GuhNpcEntity.Kind.BOROMIKA;
        }
    }

    private Cast() {
    }
}
