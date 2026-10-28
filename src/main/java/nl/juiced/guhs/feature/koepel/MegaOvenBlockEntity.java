package nl.juiced.guhs.feature.koepel;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.registry.ModItems;

/**
 * De Mega-oven (guhs:mega_oven, 3 x 2 x 2): an industrial guh bakery. It bakes ONE programme in bulk (click with an empty
 * hand: the next programme), a batch every {@link KoepelGetallen#BAK_TICKS} ticks on {@link KoepelGetallen#MEGA_OVEN}
 * vadskracht. Ingredients come in by pipe or hand (slots 0-5), the baking goes out by pipe (slots 6-8). It bakes more per
 * ingredient than a crafting table (that is the point of a mega-oven) and it needs no milk buckets; it eats a lot of
 * knabbels and graan, for guh parties and the guh villagers' trades.
 */
public class MegaOvenBlockEntity extends MachineBlockEntity implements KoepelBlock.KoepelMachine {
    static final int IN = 6, ALLES = 9;

    /** A programme: what one batch eats (item id, count) and what comes out. */
    public record Programma(String id, List<Ingr> in, String uit, int aantal) {
        boolean bestaat() {
            return KoepelVakken.bestaat(item(uit)) && in.stream().allMatch(i -> KoepelVakken.bestaat(item(i.item)));
        }
    }

    public record Ingr(String item, int aantal) {
    }

    /** Ids without a namespace are guhs items; "minecraft:..." vanilla. */
    static Item item(String id) {
        return id.startsWith("minecraft:") ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.parse(id)) : KoepelVakken.guhs(id);
    }

    public static final List<Programma> PROGRAMMAS = List.of(
            new Programma("roze_broodjes", List.of(new Ingr("roze_graan", 12)), "roze_broodje", 6),
            new Programma("knabbelkoekjes", List.of(new Ingr("roze_graan", 4), new Ingr("kaas_knabbels", 2)), "roze_knabbelkoekje", 8),
            new Programma("guh_taarten", List.of(new Ingr("kaas_knabbels", 9), new Ingr("minecraft:wheat", 6), new Ingr("minecraft:sugar", 3)),
                    "guh_taart", 3),
            new Programma("cupcakes", List.of(new Ingr("kaas_knabbels", 4), new Ingr("minecraft:wheat", 4), new Ingr("minecraft:sugar", 2)),
                    "guh_cupcake", 8),
            new Programma("broden", List.of(new Ingr("minecraft:wheat", 6)), "minecraft:bread", 3));

    private int programma;
    private int voortgang;

    public MegaOvenBlockEntity(BlockPos pos, BlockState state) {
        super(KoepelFeature.MEGA_OVEN_BE.get(), pos, state, KoepelGetallen.MEGA_OVEN, ALLES);
    }

    public Programma programma() {
        return PROGRAMMAS.get(Math.floorMod(programma, PROGRAMMAS.size()));
    }

    public void zetProgramma(int i) {
        programma = Math.floorMod(i, PROGRAMMAS.size());
        voortgang = 0;
        sync();
    }

    private boolean heeftAlles(Programma p) {
        for (Ingr i : p.in) {
            if (KoepelVakken.tel(vakken(), item(i.item), 0, IN) < i.aantal) {
                return false;
            }
        }
        return true;
    }

    private ItemStack opbrengst(Programma p) {
        return new ItemStack(item(p.uit), p.aantal);
    }

    @Override
    protected boolean kanWerken() {
        Programma p = programma();
        return p.bestaat() && heeftAlles(p) && KoepelVakken.past(vakken(), opbrengst(p), IN, ALLES);
    }

    @Override
    protected boolean isVol() {
        Programma p = programma();
        return p.bestaat() && !KoepelVakken.past(vakken(), opbrengst(p), IN, ALLES);
    }

    @Override
    protected void werk() {
        if (++voortgang < KoepelGetallen.BAK_TICKS) {
            if (level instanceof ServerLevel sl && voortgang % 20 == 0) {
                sl.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 2.3, worldPosition.getZ() + 1.0, 3, 0.3, 0.1, 0.3, 0.01);
            }
            return;
        }
        voortgang = 0;
        Programma p = programma();
        for (Ingr i : p.in) {
            KoepelVakken.neem(vakken(), item(i.item), i.aantal, 0, IN);
        }
        KoepelVakken.stop(vakken(), opbrengst(p), IN, ALLES);
        if (level instanceof ServerLevel sl) {
            sl.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8f, 1.4f);
            sl.sendParticles(ParticleTypes.HEART, worldPosition.getX() + 0.5, worldPosition.getY() + 1.6, worldPosition.getZ() - 0.2, 4, 0.5, 0.2, 0.2, 0);
        }
    }

    @Override
    protected boolean isInvoer(int vak) {
        return vak < IN;
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return vak >= IN;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        if (vak >= IN) {
            return true;
        }
        for (Programma p : PROGRAMMAS) {
            for (Ingr i : p.in) {
                if (wat.is(item(i.item))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean klik(ServerPlayer speler, ItemStack inHand) {
        if (inHand.isEmpty()) {
            if (speler.isShiftKeyDown()) {               // sneak + empty hand: the baking comes out
                for (int i = IN; i < ALLES; i++) {
                    ItemResource r = vakken().getResource(i);
                    if (!r.isEmpty()) {
                        speler.getInventory().placeItemBackInInventory(r.toStack(vakken().getAmountAsInt(i)));
                        vakken().set(i, ItemResource.EMPTY, 0);
                    }
                }
                return true;
            }
            zetProgramma(programma + 1);
            Programma p = programma();
            speler.sendOverlayMessage(Component.translatable("gui.guhs.koepel.oven.programma",
                    Component.translatable("gui.guhs.koepel.oven.p." + p.id()), recept(p)).withStyle(ChatFormatting.GOLD));
            return true;
        }
        if (past(0, ItemResource.of(inHand))) {           // an ingredient by hand
            ItemStack een = inHand.copy();
            int rest = KoepelVakken.past(vakken(), een, 0, IN) ? KoepelVakken.stop(vakken(), een, 0, IN) : een.getCount();
            inHand.shrink(inHand.getCount() - rest);
            return true;
        }
        return false;
    }

    /** "12x Roze graan -> 6x Roze broodje" */
    static Component recept(Programma p) {
        net.minecraft.network.chat.MutableComponent c = Component.empty();
        for (int i = 0; i < p.in.size(); i++) {
            Ingr in = p.in.get(i);
            if (i > 0) {
                c.append(" + ");
            }
            c.append(in.aantal + "x ").append(new ItemStack(item(in.item)).getHoverName());
        }
        return c.append(" -> " + p.aantal + "x ").append(new ItemStack(item(p.uit)).getHoverName());
    }

    @Override
    protected void clientTick() {
        if (bezig() && level != null && level.getGameTime() % 6 == 0) {
            RandomSource r = level.getRandom();
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, worldPosition.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.4,
                    worldPosition.getY() + 2.2, worldPosition.getZ() + 1.2, 0, 0.04, 0);
        }
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(Component.translatable("gui.guhs.koepel.oven.uitlezing", Component.translatable("gui.guhs.koepel.oven.p." + programma().id()))
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putInt("Programma", programma);
        uit.putInt("Voortgang", voortgang);
    }

    @Override
    protected void laden(ValueInput in) {
        programma = in.getIntOr("Programma", 0);
        voortgang = in.getIntOr("Voortgang", 0);
    }

    /** (Tests) */
    public int voortgang() {
        return voortgang;
    }

    static boolean isKnabbel(ItemStack s) {
        return s.is(ModItems.KAAS_KNABBELS.get());
    }

    static boolean isKnabbelblok(ItemStack s) {
        return s.is(KoepelVakken.guhs("block_of_kaasknabbels"));
    }
}
