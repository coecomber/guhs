package nl.juiced.guhs.feature.vadskracht;

/** A consumer of vadskracht (every machine). See {@link VadsKnoop} and the ready-made {@link MachineBlockEntity}. */
public interface VadsVerbruiker extends VadsKnoop {
    /**
     * The NOMINAL demand in VK per second while switched on, also when it idles (so the readout is stable and a net does not
     * flicker); 0 = switched off. Call {@link VadsKracht#veranderd} when it changes.
     */
    int vadsVraag();

    /**
     * The net started or stopped for this consumer. Called when the answer changes and once after load; a rebuilt net may
     * repeat the same answer, so only remember it.
     */
    void vadsStroom(boolean aan);
}
