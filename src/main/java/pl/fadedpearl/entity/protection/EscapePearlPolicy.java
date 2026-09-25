package pl.fadedpearl.entity.protection;

/** Pure activation policy for the permanent owner-strike protection granted by an Evasion Pearl. */
public final class EscapePearlPolicy {
    private EscapePearlPolicy() {}

    public enum Decision {
        IGNORE,
        PROTECT_AND_EVADE
    }

    public static Decision decide(boolean armed, boolean healed, boolean ownerAttack,
                                  boolean bypassesInvulnerability) {
        if (!armed || !healed || !ownerAttack || bypassesInvulnerability)
            return Decision.IGNORE;
        return Decision.PROTECT_AND_EVADE;
    }
}
