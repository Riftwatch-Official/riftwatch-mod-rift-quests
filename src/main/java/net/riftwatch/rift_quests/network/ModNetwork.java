package net.riftwatch.rift_quests.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.riftwatch.rift_quests.engine.QuestEngine;

public final class ModNetwork {
    private static final String VERSION = "1";

    private ModNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(Payloads.Book.TYPE, Payloads.Book.CODEC, (payload, context) -> ClientBridge.handler().book(payload));
        registrar.playToClient(Payloads.Progress.TYPE, Payloads.Progress.CODEC, (payload, context) -> ClientBridge.handler().progress(payload));
        registrar.playToClient(Payloads.QuestCompleted.TYPE, Payloads.QuestCompleted.CODEC, (payload, context) -> ClientBridge.handler().questCompleted(payload));
        registrar.playToClient(Payloads.OpenBook.TYPE, Payloads.OpenBook.CODEC, (payload, context) -> ClientBridge.handler().openBook());
        registrar.playToServer(Payloads.Claim.TYPE, Payloads.Claim.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                QuestEngine.claim(player, payload.quest(), payload.choices());
            }
        });
        registrar.playToServer(Payloads.TaskAction.TYPE, Payloads.TaskAction.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                QuestEngine.check(player, payload.quest(), payload.task());
            }
        });
        registrar.playToServer(Payloads.Pin.TYPE, Payloads.Pin.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                QuestEngine.pin(player, payload.quest());
            }
        });
    }
}
