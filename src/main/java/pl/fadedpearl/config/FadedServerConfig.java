package pl.fadedpearl.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-authoritative tuning. Defaults preserve the pre-config gameplay values. */
public final class FadedServerConfig {
    public static final int DEFAULT_ENCOUNTER_CHECK_INTERVAL_TICKS = 200;
    public static final int DEFAULT_ENCOUNTER_MAX_Y = 45;
    public static final int DEFAULT_ENCOUNTER_MIN_HORIZONTAL_SPACING = 100;
    public static final int DEFAULT_TRUST_GAIN_PERCENT = 100;
    public static final int DEFAULT_TRUST_LOSS_PERCENT = 100;
    public static final int DEFAULT_RECOVERY_GRACE_SECONDS = 30;
    public static final int DEFAULT_ANCHOR_RECALL_TRUST_REWARD = 1;
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue CAVE_ENCOUNTERS_ENABLED;
    public static final ForgeConfigSpec.IntValue CAVE_ENCOUNTER_CHECK_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue CAVE_ENCOUNTER_MAX_Y;
    public static final ForgeConfigSpec.IntValue CAVE_ENCOUNTER_MIN_HORIZONTAL_SPACING;
    public static final ForgeConfigSpec.IntValue TRUST_GAIN_PERCENT;
    public static final ForgeConfigSpec.IntValue TRUST_LOSS_PERCENT;
    public static final ForgeConfigSpec.IntValue RECOVERY_GRACE_SECONDS;
    public static final ForgeConfigSpec.IntValue ANCHOR_RECALL_TRUST_REWARD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("encounters");
        CAVE_ENCOUNTERS_ENABLED = builder
                .comment("Allow wounded Fade encounters to appear naturally in Overworld caves.")
                .define("enabled", true);
        CAVE_ENCOUNTER_CHECK_INTERVAL_TICKS = builder
                .comment("Ticks between cave encounter checks. 20 ticks = 1 second.")
                .defineInRange("checkIntervalTicks", DEFAULT_ENCOUNTER_CHECK_INTERVAL_TICKS, 20, 72000);
        CAVE_ENCOUNTER_MAX_Y = builder
                .comment("A player must be at or below this Y level for a cave encounter check.")
                .defineInRange("maximumY", DEFAULT_ENCOUNTER_MAX_Y, -64, 320);
        CAVE_ENCOUNTER_MIN_HORIZONTAL_SPACING = builder
                .comment("Minimum horizontal distance between remembered cave encounter sites.")
                .defineInRange("minimumHorizontalSpacing", DEFAULT_ENCOUNTER_MIN_HORIZONTAL_SPACING, 32, 1024);
        builder.pop();

        builder.push("trust");
        TRUST_GAIN_PERCENT = builder
                .comment("Scales positive trust changes. 100 keeps the default balance; 0 disables gains.")
                .defineInRange("gainPercent", DEFAULT_TRUST_GAIN_PERCENT, 0, 500);
        TRUST_LOSS_PERCENT = builder
                .comment("Scales trust losses. 100 keeps the default balance; 0 disables losses.")
                .defineInRange("lossPercent", DEFAULT_TRUST_LOSS_PERCENT, 0, 500);
        ANCHOR_RECALL_TRUST_REWARD = builder
                .comment("Trust awarded after a successful same-dimension anchor recall.")
                .defineInRange("anchorRecallReward", DEFAULT_ANCHOR_RECALL_TRUST_REWARD, 0, 10);
        builder.pop();

        builder.push("recovery");
        RECOVERY_GRACE_SECONDS = builder
                .comment("Seconds a missing companion must remain absent before recovery may recreate it.")
                .defineInRange("graceSeconds", DEFAULT_RECOVERY_GRACE_SECONDS, 5, 300);
        builder.pop();

        SPEC = builder.build();
    }

    public static int recoveryGraceTicks() {
        return RECOVERY_GRACE_SECONDS.get() * 20;
    }

    private FadedServerConfig() {}
}
