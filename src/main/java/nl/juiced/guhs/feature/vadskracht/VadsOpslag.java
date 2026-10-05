package nl.juiced.guhs.feature.vadskracht;

/**
 * Storage of vadskracht (the Knabbelbatterij), in VK (= VK-seconds). A net charges it with what its sources give more than
 * its machines ask, and empties it when they ask more. The ready-made one is {@link BatterijBlockEntity}.
 */
public interface VadsOpslag extends VadsKnoop {
    long vadsInhoud();

    long vadsMax();

    /** Puts up to vk in; returns what really went in. */
    long vadsLaad(long vk);

    /** Takes up to vk out; returns what really came out. */
    long vadsOntlaad(long vk);
}
