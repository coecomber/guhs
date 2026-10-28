package nl.juiced.guhs.feature.fabriek;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.techmachine.MachineSoort;
import nl.juiced.guhs.feature.techmachine.TechBlockEntity;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.storage.BankContents;
import nl.juiced.guhs.storage.BankStorage;

/**
 * De Bouwblokken-fabriek (1.8.0): a smart stonecutter that also paints. Put the block you want in the voorbeeld slot (2;
 * it stays there) and it makes it, in bulk on vadskracht, from whatever it has: the basis slot (0), its linked Bank Guh,
 * its linked chest and the chests next to it. It knows every way the game has from one block to another
 * ({@link Bouwwegen}: stonecutter, furnace, crafting of one kind) and dyes ({@link Verven}: the dye from slot 1 or from
 * the links). What it makes goes into the bank, else the linked chest, else its own nine output slots (3-11).
 * <p>
 * Linking ({@link FabriekBlock}): a Banksleutel on it links that bank (any distance, any dimension); sneak + empty hand,
 * then click a chest within {@link FabriekGetallen#KOPPEL_BEREIK} blocks ({@link Koppelen}).
 */
public class BouwblokkenFabriekBlockEntity extends TechBlockEntity implements MetBank {
    public static final int BASIS = 0, VERF = 1, VOORBEELD = 2, UIT_VAN = 3, UIT_TOT = 11;
    /** At most this many kinds of the links are tried for a colour per job (a full bank holds hundreds of kinds). */
    private static final int SOORTEN_MAX = 64;

    @Nullable
    private UUID bank;
    @Nullable
    private BlockPos kist;

    /** What it saw last (it looks once per job, not every tick: a plan asks the bank and the chests). */
    private long kijkTot = Long.MIN_VALUE;
    private boolean kan, vol;

    public BouwblokkenFabriekBlockEntity(BlockPos pos, BlockState state) {
        super(FabriekFeature.BOUWBLOKKEN_FABRIEK_BE.get(), pos, state, FabriekGetallen.FABRIEK, 12);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.BOUWBLOKKEN_FABRIEK;
    }

    @Override
    public int duur() {
        return FabriekGetallen.FABRIEK_TICKS;
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return switch (vak) {
            case VERF -> FabriekFeature.isVerf(wat.toStack());
            case BASIS, VOORBEELD -> true;
            default -> false;
        };
    }

    // --- links -------------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public UUID bank() {
        return bank;
    }

    @Nullable
    public BlockPos kist() {
        return kist;
    }

    @Override
    public void koppelBank(@Nullable UUID id) {
        bank = id;
        setChanged();
        sync();
    }

    public void koppelKist(@Nullable BlockPos pos) {
        kist = pos == null ? null : pos.immutable();
        setChanged();
        sync();
    }

    @Nullable
    private BankStorage bankMaag() {
        return MetBank.maag(level, bank);
    }

    @Nullable
    private ResourceHandler<ItemResource> kistHandler() {
        return kist == null || level == null ? null : Kisten.van(level, kist, null);
    }

    /** The plain chests (no machines, no linked chest) right next to it. */
    private List<ResourceHandler<ItemResource>> buren() {
        List<ResourceHandler<ItemResource>> l = new ArrayList<>();
        if (level == null) {
            return l;
        }
        for (Direction d : Direction.values()) {
            BlockPos p = worldPosition.relative(d);
            if (p.equals(kist) || !(level.getBlockEntity(p) instanceof Container) || level.getBlockEntity(p) instanceof MachineBlockEntity) {
                continue;
            }
            ResourceHandler<ItemResource> h = Kisten.van(level, p, d.getOpposite());
            if (h != null) {
                l.add(h);
            }
        }
        return l;
    }

    // --- where the blocks come from ----------------------------------------------------------------------------------

    /** One place the fabriek takes from. */
    private interface Bron {
        long tel(ItemStack soort);

        ItemStack neem(ItemStack soort, int n);

        void soorten(Consumer<ItemStack> uit);
    }

    private List<Bron> bronnen() {
        List<Bron> l = new ArrayList<>();
        l.add(new Bron() {
            public long tel(ItemStack soort) {
                return vakken().getResource(BASIS).matches(soort) ? vakken().getAmountAsInt(BASIS) : 0;
            }

            public ItemStack neem(ItemStack soort, int n) {
                int heb = (int) tel(soort), neem = Math.min(heb, n);
                vakken().set(BASIS, vakken().getResource(BASIS), heb - neem);
                return soort.copyWithCount(neem);
            }

            public void soorten(Consumer<ItemStack> uit) {
                if (vakken().getAmountAsInt(BASIS) > 0) {
                    uit.accept(vakken().getResource(BASIS).toStack());
                }
            }
        });
        l.add(new Bron() {
            public long tel(ItemStack soort) {
                return vakken().getResource(VERF).matches(soort) ? vakken().getAmountAsInt(VERF) : 0;
            }

            public ItemStack neem(ItemStack soort, int n) {
                int heb = (int) tel(soort), neem = Math.min(heb, n);
                vakken().set(VERF, vakken().getResource(VERF), heb - neem);
                return soort.copyWithCount(neem);
            }

            public void soorten(Consumer<ItemStack> uit) {
                if (vakken().getAmountAsInt(VERF) > 0) {
                    uit.accept(vakken().getResource(VERF).toStack());
                }
            }
        });
        BankStorage maag = bankMaag();
        if (maag != null) {
            l.add(new Bron() {
                public long tel(ItemStack soort) {
                    return Features.isLoaned(soort) ? 0 : maag.count(soort);
                }

                public ItemStack neem(ItemStack soort, int n) {
                    return maag.extract(soort, n);
                }

                public void soorten(Consumer<ItemStack> uit) {
                    for (BankContents.Entry e : maag.snapshot().entries()) {
                        if (e.count() > 0) {
                            uit.accept(e.item());
                        }
                    }
                }
            });
        }
        List<ResourceHandler<ItemResource>> kisten = new ArrayList<>();
        ResourceHandler<ItemResource> k = kistHandler();
        if (k != null) {
            kisten.add(k);
        }
        kisten.addAll(buren());
        for (ResourceHandler<ItemResource> h : kisten) {
            l.add(new Bron() {
                public long tel(ItemStack soort) {
                    return Kisten.tel(h, s -> ItemStack.isSameItemSameComponents(s, soort));
                }

                public ItemStack neem(ItemStack soort, int n) {
                    return Kisten.neem(h, s -> ItemStack.isSameItemSameComponents(s, soort), n);
                }

                public void soorten(Consumer<ItemStack> uit) {
                    for (int i = 0; i < h.size(); i++) {
                        if (!h.getResource(i).isEmpty() && h.getAmountAsLong(i) > 0) {
                            uit.accept(h.getResource(i).toStack());
                        }
                    }
                }
            });
        }
        return l;
    }

    private static long tel(List<Bron> bronnen, ItemStack soort) {
        long n = 0;
        for (Bron b : bronnen) {
            n += b.tel(soort);
        }
        return n;
    }

    private static void neem(List<Bron> bronnen, ItemStack soort, int n) {
        for (Bron b : bronnen) {
            if (n <= 0) {
                return;
            }
            n -= b.neem(soort, n).getCount();
        }
    }

    /** Every kind of item the sources hold (one stack per kind, at most {@link #SOORTEN_MAX}). */
    private static List<ItemStack> soorten(List<Bron> bronnen) {
        Map<Item, ItemStack> m = new LinkedHashMap<>();
        for (Bron b : bronnen) {
            b.soorten(s -> {
                if (m.size() < SOORTEN_MAX && !s.isEmpty() && !Features.isLoaned(s)) {
                    m.putIfAbsent(s.getItem(), s.copyWithCount(1));
                }
            });
        }
        return new ArrayList<>(m.values());
    }

    // --- the plan for one job ----------------------------------------------------------------------------------------

    /** One job: take {@code basis} x {@code basisAantal} (+ one dye), give {@code resultaat}. */
    private record Plan(ItemStack basis, int basisAantal, ItemStack verf, ItemStack resultaat) {
    }

    @Nullable
    private Plan plan(List<Bron> bronnen) {
        if (!(level instanceof ServerLevel server) || vakken().getAmountAsInt(VOORBEELD) == 0) {
            return null;
        }
        ItemStack doel = vakken().getResource(VOORBEELD).toStack();
        List<ItemStack> soorten = soorten(bronnen);
        // a way from one block to another: the best one (most out per block) the sources can pay for
        Plan beste = null;
        for (Bouwwegen.Weg weg : Bouwwegen.naar(server, doel.getItem())) {
            for (ItemStack s : soorten) {
                if (s.is(doel.getItem()) || !weg.neemt(s) || tel(bronnen, s) < weg.nodig()) {
                    continue;
                }
                if (beste == null || weg.uit() * beste.basisAantal() > beste.resultaat().getCount() * weg.nodig()) {
                    beste = new Plan(s, weg.nodig(), ItemStack.EMPTY, doel.copyWithCount(weg.uit()));
                }
            }
        }
        if (beste != null) {
            return beste;
        }
        // a colour
        for (ItemStack verf : soorten) {
            if (!FabriekFeature.isVerf(verf)) {
                continue;
            }
            for (ItemStack s : soorten) {
                if (FabriekFeature.isVerf(s) || s.is(doel.getItem())) {
                    continue;
                }
                Verven.Uitkomst u = Verven.van(server, s, verf);
                if (u != null && u.resultaat().is(doel.getItem()) && tel(bronnen, s) >= u.blokken()) {
                    return new Plan(s, u.blokken(), verf, u.resultaat());
                }
            }
        }
        return null;
    }

    // --- where the blocks go -----------------------------------------------------------------------------------------

    /** Puts it all away (bank, linked chest, own slots); false: it does not fit anywhere (nothing moved). */
    private boolean bewaar(ItemStack wat, boolean simuleer) {
        BankStorage maag = bankMaag();
        if (maag != null && maag.room(wat) >= wat.getCount()) {
            if (!simuleer) {
                maag.insert(wat.copy());
            }
            return true;
        }
        ResourceHandler<ItemResource> k = kistHandler();
        if (k != null && Kisten.stop(k, wat, true).isEmpty()) {
            if (!simuleer) {
                Kisten.stop(k, wat);
            }
            return true;
        }
        return simuleer ? pastAlles(List.of(wat), UIT_VAN, UIT_TOT) : stopAlles(List.of(wat), UIT_VAN, UIT_TOT);
    }

    // --- the work ----------------------------------------------------------------------------------------------------

    private void kijk() {
        if (level == null || level.getGameTime() < kijkTot) {
            return;
        }
        kijkTot = level.getGameTime() + FabriekGetallen.FABRIEK_TICKS;
        Plan p = plan(bronnen());
        kan = p != null && bewaar(p.resultaat(), true);
        vol = p != null && !kan;
    }

    @Override
    protected void vakkenVeranderd() {
        super.vakkenVeranderd();
        kijkTot = Long.MIN_VALUE;
    }

    @Override
    protected boolean kanWerken() {
        kijk();
        if (!kan) {
            voortgang = 0;
        }
        return kan;
    }

    @Override
    protected boolean isVol() {
        kijk();
        return vol;
    }

    @Override
    protected void werk() {
        if (++voortgang < duur() || !(level instanceof ServerLevel server)) {
            return;
        }
        voortgang = 0;
        int gemaakt = 0;
        for (int i = 0; i < FabriekGetallen.FABRIEK_PER_KEER; i++) {
            List<Bron> bronnen = bronnen();
            Plan p = plan(bronnen);
            if (p == null || !bewaar(p.resultaat(), true)) {
                break;
            }
            neem(bronnen, p.basis(), p.basisAantal());
            if (!p.verf().isEmpty()) {
                neem(bronnen, p.verf(), 1);
            }
            bewaar(p.resultaat(), false);
            gemaakt++;
        }
        kijkTot = Long.MIN_VALUE;
        if (gemaakt == 0) {
            return;
        }
        MetBank.wakker(server, bank);
        server.sendParticles(ParticleTypes.WHITE_ASH, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                6, 0.25, 0.1, 0.25, 0.02);
        server.playSound(null, worldPosition, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.35f, 1.2f);
        FabriekFeature.beloon(this, "fabriek_bouwblokken");
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (bank != null) {
            regels.accept(Component.translatable(bankMaag() != null ? "gui.guhs.fabriek.bouwblokken.bank" : "gui.guhs.fabriek.bouwblokken.bank_weg")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (kist != null) {
            regels.accept(Component.translatable("gui.guhs.fabriek.bouwblokken.kist", kist.getX(), kist.getY(), kist.getZ()).withStyle(ChatFormatting.AQUA));
        }
        if (vakken().getAmountAsInt(VOORBEELD) == 0) {
            regels.accept(Component.translatable("gui.guhs.fabriek.bouwblokken.geen_voorbeeld").withStyle(ChatFormatting.GRAY));
        } else if (isVol()) {
            regels.accept(Component.translatable("gui.guhs.techmachine.vol").withStyle(ChatFormatting.GOLD));
        } else if (!kan) {
            regels.accept(Component.translatable("gui.guhs.fabriek.bouwblokken.geen_bron").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.storeNullable("Bank", UUIDUtil.CODEC, bank);
        uit.storeNullable("Kist", BlockPos.CODEC, kist);
    }

    @Override
    protected void laden(ValueInput in) {
        bank = in.read("Bank", UUIDUtil.CODEC).orElse(null);
        kist = in.read("Kist", BlockPos.CODEC).orElse(null);
    }
}
