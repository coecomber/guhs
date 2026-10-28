package nl.juiced.guhs.feature.snoesverhaal;

import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.dromen.DroomVoortgang.Stukje;
import nl.juiced.guhs.feature.dromen.Wekkerguh;
import nl.juiced.guhs.feature.droomwezens.DroomNpc;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.8.0 Guhdromenland part F: the story of Snoes ({@link SnoesVerhaal}), its finale scene ({@link SnoesScenes}), the dream
 * sources of the weerpotjes ({@link DroomBronnen}) and the test commands ({@link SnoesCommando}, TESTEN_SNOES.md). Texts,
 * hidden advancements and the FTB chapter guhs_dromen: tools/features/snoesverhaal.py.
 */
public final class SnoesVerhaalFeature {
    private SnoesVerhaalFeature() {
    }

    public static void payloads(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) {
        SnoesHud.register(registrar);
    }

    public static void register(IEventBus modBus) {
        SnoesScenes.init();
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> SnoesVerhaal.onKlik(e));
        // playtest: the bed asks "gewoon slapen / dromen" (DroomSlapen); Snoes is a dream buddy there
        nl.juiced.guhs.feature.dromen.DroomSlapen.SNOES_BIJ_BED = SnoesVerhaal::snoesBijBed;
        nl.juiced.guhs.feature.dromen.DroomSlapen.DROOM_MET_SNOES = SnoesVerhaal::droomMetSnoes;
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> SnoesVerhaal.onDimensie(e));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> SnoesVerhaal.tick(e));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 20 == 9) {
                SnoesHud.tick(e.getServer());                 // (playtest) the story HUD, when it changed
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
                SnoesHud.sync(sp, true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
                SnoesHud.uitgelogd(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> SnoesCommando.register(e));
        DroomVoortgang.luister(SnoesVerhaal::opVoortgang);
        // the story's talk on top of the dream NPCs' jokes (DroomNpc.zet)
        DroomNpc.zet(Kind.SNOES, SnoesVerhaal::praatSnoes);
        DroomNpc.zet(Kind.SNOES_BLIJ, SnoesVerhaal::praatSnoesBlij);
        // (integration) the molen island talks first (outside it praatZandguh returns false), then the story hint
        DroomNpc.zet(Kind.ZANDGUH, (p, n) -> nl.juiced.guhs.feature.molentje.Molen.praatZandguh(p, n) || SnoesVerhaal.hint(p, n, Stukje.DROOMZAND));
        DroomNpc.zet(Kind.DROOM_OMAGUH, (p, n) -> SnoesVerhaal.hint(p, n, Stukje.HERINNERING));
        DroomNpc.zet(Kind.ROMMELGUH, (p, n) -> SnoesVerhaal.hint(p, n, Stukje.HERINNERING));
        DroomNpc.zet(Kind.KUSSENGEVECHT_GUH, (p, n) -> SnoesVerhaal.hint(p, n, Stukje.STER));
        DroomNpc.zet(Kind.WEKKERGUH, SnoesVerhaal::praatWekker);
        // (integration A + C) the plein's Wekkerguh wears his own model from part C
        Wekkerguh.MAKER = level -> {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
            if (npc != null) {
                npc.setKind(Kind.WEKKERGUH);
            }
            return npc;
        };
    }
}
