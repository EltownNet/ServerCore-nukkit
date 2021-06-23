package net.eltown.servercore.components.messaging;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.tinyrabbit.TinyRabbitListener;

public class MessageListener {

    private final ServerCore instance;
    private final TinyRabbitListener listener;

    public MessageListener(final ServerCore instance) {
        this.instance = instance;

        this.listener = new TinyRabbitListener("localhost");
        this.listener.throwExceptions(true);

        this.startListening();
    }

    private void startListening() {
        this.listener.receive((delivery -> {

        }), "ServerCore/GroupManager", "groups.extern");
    }

}
