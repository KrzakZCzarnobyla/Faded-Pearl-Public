package pl.fadedpearl.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import pl.fadedpearl.world.EchoNeedleLocator;

import static org.junit.jupiter.api.Assertions.*;

final class EchoNeedlePacketTest {
    @Test
    void activePayloadRoundTripsInExactlyOneByteWithoutCoordinatesOrDistance() {
        EchoNeedlePacket packet = new EchoNeedlePacket(true, 15, EchoNeedleLocator.Height.ABOVE);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        EchoNeedlePacket.encode(packet, buffer);
        assertEquals(1, buffer.readableBytes());
        assertEquals(packet, EchoNeedlePacket.decode(buffer));
    }

    @Test
    void constructorAndDecoderSanitizeUntrustedValues() {
        assertEquals(15, new EchoNeedlePacket(true, -1, null).sector());
        assertEquals(EchoNeedleLocator.Height.SIMILAR,
                new EchoNeedlePacket(true, -1, null).height());

        FriendlyByteBuf invalidHeight = new FriendlyByteBuf(Unpooled.buffer());
        invalidHeight.writeByte(0x40 | 0x30 | 0x07);
        EchoNeedlePacket decoded = EchoNeedlePacket.decode(invalidHeight);
        assertTrue(decoded.active());
        assertEquals(7, decoded.sector());
        assertEquals(EchoNeedleLocator.Height.SIMILAR, decoded.height());

        FriendlyByteBuf inactiveGarbage = new FriendlyByteBuf(Unpooled.buffer());
        inactiveGarbage.writeByte(0xBF);
        assertEquals(EchoNeedlePacket.clear(), EchoNeedlePacket.decode(inactiveGarbage));
    }
}
