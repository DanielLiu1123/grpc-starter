package com.freemanan.starter.grpc.extensions.transcoderjson;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.CompletableFuture;

/**
 * {@link CompletableFuture} adapter for {@link ListenableFuture}.
 *
 * @param <T> The type of the future result
 */
public class FutureAdapter<T> {

    private final CompletableFuture<T> completableFuture;

    private FutureAdapter(ListenableFuture<T> listenableFuture) {
        this.completableFuture = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean cancelled = listenableFuture.cancel(mayInterruptIfRunning);
                super.cancel(cancelled);
                return cancelled;
            }
        };

        Futures.addCallback(
                listenableFuture,
                new FutureCallback<T>() {
                    @Override
                    public void onSuccess(T result) {
                        completableFuture.complete(result);
                    }

                    @Override
                    public void onFailure(Throwable ex) {
                        completableFuture.completeExceptionally(ex);
                    }
                },
                directExecutor());
    }

    private CompletableFuture<T> getCompletableFuture() {
        return completableFuture;
    }

    /**
     * Converts a {@link ListenableFuture} to a {@link CompletableFuture}.
     *
     * @param listenableFuture The listenable future to adapt
     * @param <T>              The type of the future result
     * @return The adapted {@link CompletableFuture}
     */
    public static <T> CompletableFuture<T> toCompletable(ListenableFuture<T> listenableFuture) {
        FutureAdapter<T> listenableFutureAdapter = new FutureAdapter<>(listenableFuture);
        return listenableFutureAdapter.getCompletableFuture();
    }
}
