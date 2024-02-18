package org.toadwritecode.task2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class HandlerImpl implements Handler {

    private final Client client;

    public HandlerImpl(Client client) {
        this.client = client;
    }

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        CompletableFuture<ApplicationStatusResponse> future1 = CompletableFuture.supplyAsync(() -> makeRequest(client::getApplicationStatus1, id));
        CompletableFuture<ApplicationStatusResponse> future2 = CompletableFuture.supplyAsync(() -> makeRequest(client::getApplicationStatus2, id));

        try {
            return CompletableFuture.anyOf(future1, future2)
                    .thenApply(ApplicationStatusResponse.class::cast)
                    .completeOnTimeout(new ApplicationStatusResponse.Failure(null, 2), 15, TimeUnit.SECONDS)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            return new ApplicationStatusResponse.Failure(null, 2);
        }
    }

    private ApplicationStatusResponse makeRequest(ResponseFunction function, String id) {
        try {
            Response response = function.apply(id);
            if (response instanceof Response.Success success) {
                return new ApplicationStatusResponse.Success(success.applicationId(), success.applicationStatus());
            } else if (response instanceof Response.RetryAfter) {
                return new ApplicationStatusResponse.Failure(null, 1);
            } else {
                return new ApplicationStatusResponse.Failure(null, 1);
            }
        } catch (Exception ex) {
            return new ApplicationStatusResponse.Failure(null, 1);
        }
    }

    @FunctionalInterface
    interface ResponseFunction {
        Response apply(String id) throws Exception;
    }

}
