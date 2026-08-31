package org.dempsay.axiom.plugin;

import org.dempsay.axiom.annotations.AgentCapability;
import org.dempsay.axiom.annotations.Severity;

/**
 * Compiled fixture so harvest can scan {@code target/test-classes}.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
public final class HarvestSample {

    private HarvestSample() { }

    /**
     * Blessed helper used only as an annotation host.
     */
    @AgentCapability(
            id = "sample_intent",
            title = "Harvested title should lose to YAML",
            severity = Severity.AVAILABLE,
            triggers = {"harvest-only-trigger"},
            antiPatterns = {"Files\\.readString"})
    public static String doIt() {
        return "ok";
    }
}
