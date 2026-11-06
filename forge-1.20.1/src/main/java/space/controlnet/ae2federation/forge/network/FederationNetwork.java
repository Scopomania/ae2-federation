package space.controlnet.ae2federation.forge.network;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The mod's one packet channel. 1.21 registered each payload type with its own codec; on Forge 1.20.1 they are numbered
 * messages on a SimpleChannel, registered in a fixed order so both sides agree on the ids.
 */
public final class FederationNetwork {
    private static final String PROTOCOL = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("ae2federation", "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextId;

    private FederationNetwork() {
    }

    public static <T> void toClient(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder, BiConsumer<T, NetworkEvent.Context> handler) {
        register(type, encoder, decoder, handler, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static <T> void toServer(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder, BiConsumer<T, NetworkEvent.Context> handler) {
        register(type, encoder, decoder, handler, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <T> void register(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder, BiConsumer<T, NetworkEvent.Context> handler,
            NetworkDirection direction) {
        CHANNEL.registerMessage(nextId++, type, encoder, decoder, (message, context) -> {
            var current = context.get();
            handler.accept(message, current);
            current.setPacketHandled(true);
        }, Optional.of(direction));
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
