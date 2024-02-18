package org.toadwritecode;

import org.toadwritecode.client.Client;
import org.toadwritecode.dto.Address;
import org.toadwritecode.dto.Event;
import org.toadwritecode.dto.Payload;
import org.toadwritecode.dto.Result;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HandlerImpl implements Handler {

    private final Client client;
    private final ExecutorService executor;

    public HandlerImpl(Client client, int threadNum) {
        this.client = client;
        this.executor = Executors.newFixedThreadPool(threadNum);
    }

    @Override
    public Duration timeout() {
        return null;
    }

    @Override
    public void performOperation() {
        while (true) {
            Event event = client.readData();
            CompletableFuture[] futures = event.recipients().stream()
                    .map(recipient -> CompletableFuture.runAsync(() -> sendData(recipient, event.payload()), executor))
                    .toArray(CompletableFuture<?>[]::new);
            CompletableFuture.allOf(futures)
                    .join();
        }
    }

    private void sendData(Address recipient, Payload payload) {
        boolean delivered = Boolean.FALSE;
        while (!delivered) {
            Result result = client.sendData(recipient, payload);
            if (result == Result.REJECTED) {
                try {
                    Thread.sleep(timeout().toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } else {
                delivered = Boolean.TRUE;
            }
        }
    }

}
