package pl.fadedpearl.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import pl.fadedpearl.entity.trust.TrustLossNotice;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TrustLossPacketTest {
    @Test
    void roundTripPreservesActualLossAndReason() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        TrustLossPacket.encode(new TrustLossPacket(8, TrustLossNotice.Reason.FRIEND_HIT), buffer);
        assertEquals(new TrustLossPacket(8, TrustLossNotice.Reason.FRIEND_HIT), TrustLossPacket.decode(buffer));
    }

    @Test
    void decodeBoundsNumbersAndUnknownReasons() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(200);
        buffer.writeVarInt(999);
        assertEquals(new TrustLossPacket(100, TrustLossNotice.Reason.OTHER), TrustLossPacket.decode(buffer));
    }
}
