package net.spindle.createwarehouse.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(
                        GantryAddressTargetPayload.TYPE,
                        GantryAddressTargetPayload.STREAM_CODEC,
                        GantryAddressTargetPayload::handle);
    }
}
