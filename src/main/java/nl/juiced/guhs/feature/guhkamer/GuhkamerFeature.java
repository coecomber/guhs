package nl.juiced.guhs.feature.guhkamer;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.world.GuhWorldData;
import nl.juiced.guhs.world.MaagManager;

/**
 * De Guhkamer in je Guhmaag + de Guhbel (2.10 "Lieve vadsjes van elkaar", slice speelgoed): a guest room behind a door in
 * your maag where your tamed guhs can stay when they don't come along ({@link Guhkamer}), the Guhbel to send them there
 * and call them back ({@link GuhbelItem}), the door ({@link GuhkamerDeurBlock}), guests that stay in the room
 * ({@link KamerGoal}). tools/features/guhkamer.py makes the door and bell models/textures, recipe, sounds, texts,
 * advancements and FTB quests.
 */
public final class GuhkamerFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredBlock<GuhkamerDeurBlock> DEUR = BLOCKS.registerBlock("guhkamer_deur", GuhkamerDeurBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(-1f, 3600000f).sound(SoundType.WOOL).noOcclusion()
                    .noCollission().noLootTable().pushReaction(PushReaction.BLOCK).lightLevel(s -> 6));
    public static final DeferredItem<GuhbelItem> GUHBEL = ITEMS.registerItem("guhbel", GuhbelItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredHolder<SoundEvent, SoundEvent> BEL = geluid("guhkamer.bel");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEG = geluid("guhkamer.weg");
    public static final DeferredHolder<SoundEvent, SoundEvent> TERUG = geluid("guhkamer.terug");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEUR_GELUID = geluid("guhkamer.deur");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROEI = geluid("guhkamer.groei");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Guhs.id(id)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        GuhHooks.doelen((guh, goals) -> goals.addGoal(3, new KamerGoal(guh)));
        GuhHooks.tick(guh -> {   // "waar is mijn guh": in the Guhkamer (after the band's own check of the same tick)
            if ((guh.tickCount + guh.getId()) % 100 == 0 && Guhkamer.isGast(guh) && Band.isBandGuh(guh)) {
                GuhVolger.zet(guh, PlekSoort.GUHKAMER, "");
            }
            // 2.10.1: the synced guest flag (the Guh menu's button) follows the server-side mark (guests from 2.10.0 too)
            if (!guh.level().isClientSide() && (guh.tickCount + guh.getId()) % 20 == 0
                    && nl.juiced.guhs.feature.band.BandVlaggen.heeft(guh, nl.juiced.guhs.feature.band.BandVlaggen.GUHKAMER_GAST) != Guhkamer.isGast(guh)) {
                nl.juiced.guhs.feature.band.BandVlaggen.zet(guh, nl.juiced.guhs.feature.band.BandVlaggen.GUHKAMER_GAST, Guhkamer.isGast(guh));
            }
        });
        Band.opNiveau((eigenaar, guh, bandId, niveau) -> Guhkamer.nieuwNiveau(eigenaar, niveau));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            var s = event.getServer();
            if (s.getTickCount() % 10 == 0) {
                Guhkamer.tick(s);
            }
            if (s.getTickCount() % 100 == 50) {   // every maag somebody is in gets its door to the Guhkamer
                ServerLevel maag = MaagManager.level(s);
                if (maag != null) {
                    for (ServerPlayer p : maag.players()) {
                        GuhWorldData.Maag m = MaagManager.maagAt(s, p.blockPosition());
                        if (m != null && m.built) {
                            Guhkamer.zorgMaagDeur(maag, m);
                        }
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> Guhkamer.allesBewaren(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> Guhkamer.TEST_PLEK.clear());
    }

    public static void payloads(PayloadRegistrar registrar) {
        GuhkamerPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GUHBEL.get()));
    }

    private GuhkamerFeature() {
    }
}
