package pl.fadedpearl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import pl.fadedpearl.client.EchoNeedleClientState;
import pl.fadedpearl.world.EchoNeedleLocator;

import java.util.function.Supplier;

/** One-byte locator payload: validity, 16-way relative bearing, and a three-way height category. */
public record EchoNeedlePacket(boolean active, int sector, EchoNeedleLocator.Height height) {
    private static final int ACTIVE_BIT = 0x40;
    private static final int SECTOR_MASK = 0x0F;

    public EchoNeedlePacket {
        sector = active ? Math.floorMod(sector, EchoNeedleLocator.SECTOR_COUNT) : 0;
        height = active && height != null ? height : EchoNeedleLocator.Height.SIMILAR;
    }

    static void encode(EchoNeedlePacket packet, FriendlyByteBuf buffer) {
        int encoded = packet.active ? ACTIVE_BIT : 0;
        if (packet.active) encoded |= packet.sector | (packet.height.ordinal() << 4);
        buffer.writeByte(encoded);
    }

    static EchoNeedlePacket decode(FriendlyByteBuf buffer) {
        int encoded = buffer.readUnsignedByte();
        boolean active = (encoded & ACTIVE_BIT) != 0;
        if (!active) return clear();
        int sector = encoded & SECTOR_MASK;
        EchoNeedleLocator.Height height = EchoNeedleLocator.Height.fromCode((encoded >>> 4) & 0x03);
        return new EchoNeedlePacket(true, sector, height);
    }

    static void handle(EchoNeedlePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoNeedleClientState.accept(packet)));
        context.get().setPacketHandled(true);
    }

    public static EchoNeedlePacket clear() {
        return new EchoNeedlePacket(false, 0, EchoNeedleLocator.Height.SIMILAR);
    }
}
