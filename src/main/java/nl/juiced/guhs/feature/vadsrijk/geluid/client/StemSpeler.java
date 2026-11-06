package nl.juiced.guhs.feature.vadsrijk.geluid.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.vadsrijk.geluid.StemPlan;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * U-AUDIO2: the NPC voices (the existing guh sounds, see {@link StemPlan}). A "guh!" when you click a Vadsrijk NPC or a
 * conversation opens, a curious "oeh?" on a page that asks something, a giggle on a happy page; now and then a pieppiepmuisje
 * squeak or a blub instead. The pitch is per NPC. Never more than one voice per ~1.2 s, never a greeting twice for one click.
 * Volume: the "stemmen" slider (guhs-vadsrijk-geluid.toml) on top of Minecraft's "Vriendelijke wezens" slider.
 */
public final class StemSpeler {
    static final float VOL = 0.55f;
    private static final RandomSource R = RandomSource.create();
    private static long laatste;
    private static int laatsteKlikNpc = -1;
    private static long laatsteKlik;

    private StemSpeler() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (e.getLevel().isClientSide() && e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND && npc(e.getTarget())) {
                klik(e.getTarget());
            }
        });
    }

    /** A Vadsrijk NPC (vr_npc_* entity types: the guh NPCs of every zone). */
    static boolean npc(Entity e) {
        return e != null && Vadsrijk.in(e.level()) && BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().startsWith("vr_npc");
    }

    private static void klik(Entity e) {
        laatsteKlikNpc = e.getId();
        laatsteKlik = System.currentTimeMillis();
        speel("groet", "guh", "", e, e.getId());
    }

    /**
     * (hook in RijkPraatScherm) A page is shown. nieuw = the conversation just opened. stem/factie/emotie/tekst = what the
     * page says about its speaker.
     */
    public static void pagina(int npcId, boolean nieuw, String stem, String factie, String emotie, String tekst) {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(npcId);
        String wat;
        if (nieuw) {
            if (npcId == laatsteKlikNpc && System.currentTimeMillis() - laatsteKlik < 1500) {
                return;   // (the click already said hello)
            }
            wat = tekst.contains("?") ? "vraag" : "groet";
        } else if ("blij".equals(emotie)) {
            wat = "dank";
        } else if (tekst.trim().endsWith("?") && R.nextFloat() < 0.6f) {
            wat = "vraag";
        } else {
            return;   // (an ordinary next page: the typing blips are enough)
        }
        speel(wat, stem, factie, e, npcId);
    }

    /** Play one voice (dev command too: e may be null, then it sounds at the player). */
    static void speel(String wat, String stem, String factie, Entity e, int npcId) {
        Minecraft mc = Minecraft.getInstance();
        float vol = GeluidConfig.stemmen();
        long nu = System.currentTimeMillis();
        if (vol <= 0 || mc.player == null || nu - laatste < 1200) {
            return;
        }
        String soort = StemPlan.kies(wat, stem, factie, R.nextDouble());
        if (soort == null) {
            return;
        }
        laatste = nu;
        String naam = e != null && e.hasCustomName() ? e.getCustomName().getString() : "npc" + npcId;
        float toon = StemPlan.toon(naam) * (0.96f + R.nextFloat() * 0.08f);
        if ("draak".equals(stem)) {
            toon *= 0.6f;
        }
        Entity bron = e != null ? e : mc.player;
        mc.getSoundManager().play(new SimpleSoundInstance(GeluidSpeler.event(StemPlan.id(soort)), SoundSource.NEUTRAL, VOL * vol, toon,
                R, bron.getX(), bron.getEyeY(), bron.getZ()));
    }

    /** Dev: one voice right now, at the player (the rate limit is skipped). */
    static void voorbeeld(String soort) {
        laatste = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        String stem = soort.equals("mika") ? "mika" : "guh";
        float vol = Math.max(0.3f, GeluidConfig.stemmen());
        mc.getSoundManager().play(new SimpleSoundInstance(GeluidSpeler.event(StemPlan.id(soort)), SoundSource.NEUTRAL, VOL * vol,
                StemPlan.toon(mc.player.getName().getString() + stem), R, mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()));
    }
}
