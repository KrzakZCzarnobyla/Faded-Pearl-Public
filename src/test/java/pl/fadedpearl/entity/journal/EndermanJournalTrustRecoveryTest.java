package pl.fadedpearl.entity.journal;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import pl.fadedpearl.entity.persistence.FadedPersistenceCodec;

import static org.junit.jupiter.api.Assertions.*;

final class EndermanJournalTrustRecoveryTest {
    @Test
    void recoveryTrustUsesTheSameMigrationPreferenceAsTheEntity() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(FadedPersistenceCodec.TRUST, 21);
        tag.putInt(FadedPersistenceCodec.TRUST_LEVEL, 63);
        tag.putInt(FadedPersistenceCodec.TRUST_SYSTEM_VERSION_KEY, FadedPersistenceCodec.TRUST_SYSTEM_VERSION);
        assertEquals(63, FadedPersistenceCodec.readTrust(tag));
        tag.remove(FadedPersistenceCodec.TRUST_LEVEL);
        assertEquals(21, FadedPersistenceCodec.readTrust(tag));
        tag.remove(FadedPersistenceCodec.TRUST_SYSTEM_VERSION_KEY);
        assertEquals(0, FadedPersistenceCodec.readTrust(tag));
    }
}
