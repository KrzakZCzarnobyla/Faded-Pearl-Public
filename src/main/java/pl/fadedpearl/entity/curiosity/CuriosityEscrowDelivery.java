package pl.fadedpearl.entity.curiosity;

/** Atomic delivery result: persistent escrow is cleared only after a confirmed materialisation. */
public final class CuriosityEscrowDelivery {
    public enum Result { CLEAR_ESCROW, RETAIN_ESCROW }

    public static Result resolve(boolean inventoryAccepted, boolean dropAccepted) {
        return inventoryAccepted || dropAccepted ? Result.CLEAR_ESCROW : Result.RETAIN_ESCROW;
    }

    public static Result execute(java.util.function.BooleanSupplier inventoryAttempt,
                                 java.util.function.BooleanSupplier dropAttempt,
                                 Runnable clearEscrow) {
        boolean inventoryAccepted = inventoryAttempt.getAsBoolean();
        boolean dropAccepted = !inventoryAccepted && dropAttempt.getAsBoolean();
        Result result = resolve(inventoryAccepted, dropAccepted);
        if (result == Result.CLEAR_ESCROW) clearEscrow.run();
        return result;
    }

    private CuriosityEscrowDelivery() {}
}
