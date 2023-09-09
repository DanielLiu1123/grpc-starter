package com.freemanan.starter.grpc.extensions.discovery;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple LRU cache implementation, based on {@link ConcurrentHashMap}.
 *
 * @author Freeman
 */
public class LRUCache<K, V> {
    private static final Logger log = LoggerFactory.getLogger(LRUCache.class);

    private static final long NEVER_EXPIRE = 0;

    private final Map<K, CacheEntry<V>> cache;
    private final long maxCacheSize;
    private final long cleanupIntervalMillis;
    private final BiConsumer<K, V> onEvict;

    public LRUCache(long maxCacheSize, long cleanupIntervalMillis, BiConsumer<K, V> onEvict) {
        this.maxCacheSize = maxCacheSize;
        this.cleanupIntervalMillis = cleanupIntervalMillis;
        this.onEvict = onEvict;
        this.cache = new ConcurrentHashMap<>();

        startCleanupThread();
    }

    private static <V> void refresh(CacheEntry<V> entry) {
        entry.setAccessTime(System.currentTimeMillis());
        entry.setExpirationTime(
                entry.getTimeoutMs() == NEVER_EXPIRE ? NEVER_EXPIRE : entry.getLastAccessTime() + entry.getTimeoutMs());
    }

    private static <K, V> void logIfNecessary(K k, V existingEntry) {
        if (log.isDebugEnabled()) {
            log.debug("Evicted key: {}, value: {}", k, existingEntry);
        }
    }

    public void put(K key, V value) {
        put(key, value, NEVER_EXPIRE);
    }

    public void put(K key, V value, long timeoutMillis) {
        evictIfNecessary();
        cache.put(key, new CacheEntry<>(value, timeoutMillis));
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            refresh(entry);
            return entry.getValue();
        }
        return null;
    }

    public V getOrSupply(K key, Supplier<V> supplier) {
        return getOrSupply(key, supplier, NEVER_EXPIRE);
    }

    public V getOrSupply(K key, Supplier<V> supplier, long timeoutMillis) {
        return cache.compute(key, (k, existingEntry) -> {
                    if (existingEntry != null) {
                        if (!existingEntry.isExpired()) {
                            refresh(existingEntry);
                            return existingEntry;
                        } else {
                            if (onEvict != null) {
                                onEvict.accept(k, existingEntry.getValue());
                                logIfNecessary(k, existingEntry.getValue());
                            }
                        }
                    }

                    V value = supplier.get();
                    return new CacheEntry<>(value, timeoutMillis);
                })
                .getValue();
    }

    public void clear() {
        cache.forEach(this::evictEntry);
    }

    private void evictIfNecessary() {
        if (cache.size() >= maxCacheSize) {
            evictExpiredEntries();
            evictLeastRecentlyUsedEntry();
        }
    }

    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(cleanupIntervalMillis);
                    evictExpiredEntries();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    private void evictExpiredEntries() {
        cache.forEach((key, entry) -> {
            if (entry.isExpired()) {
                evictEntry(key, entry);
            }
        });
    }

    private void evictLeastRecentlyUsedEntry() {
        Optional<Map.Entry<K, CacheEntry<V>>> lruEntry = cache.entrySet().stream()
                .min(Comparator.comparingLong(entry -> entry.getValue().getLastAccessTime()));

        lruEntry.ifPresent(entry -> evictEntry(entry.getKey(), entry.getValue()));
    }

    private void evictEntry(K key, CacheEntry<V> entry) {
        if (onEvict != null) {
            onEvict.accept(key, entry.getValue());
            logIfNecessary(key, entry.getValue());
        }
        cache.remove(key);
    }

    private static final class CacheEntry<V> {
        private final V value;
        private final long timeoutMs;
        private long lastAccessTime;
        private long expirationTime;

        CacheEntry(V value, long timeoutMs) {
            this.value = value;
            this.timeoutMs = timeoutMs;
            this.lastAccessTime = System.currentTimeMillis();
            this.expirationTime = timeoutMs == NEVER_EXPIRE ? NEVER_EXPIRE : lastAccessTime + timeoutMs;
        }

        boolean isExpired() {
            return expirationTime != NEVER_EXPIRE && System.currentTimeMillis() > expirationTime;
        }

        V getValue() {
            return value;
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }

        void setAccessTime(long accessTime) {
            this.lastAccessTime = accessTime;
        }

        void setExpirationTime(long expirationTime) {
            this.expirationTime = expirationTime;
        }

        long getTimeoutMs() {
            return timeoutMs;
        }
    }
}
