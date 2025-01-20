package com.ruoyi.common.core.redis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.apache.poi.ss.formula.functions.T;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * caffeine缓存实现，用来替换redis
 *
 * @author hancher
 * @date 2025/1/16 10:41
 * @since 3.8.9
 */

@Component
public class CaffeineCacheImpl implements RedisCache {


    private static class CacheObject<T> {
        private T value;
        private Duration expire;

        public T getValue() {
            return value;
        }

        public CacheObject(T value, Duration expire) {
            this.value = value;
            this.expire = expire;
        }

        public CacheObject<T> setValue(T value) {
            this.value = value;
            return this;
        }

        public Duration getExpire() {
            return expire;
        }

        public CacheObject<T> setExpire(Duration expire) {
            this.expire = expire;
            return this;
        }
    }

    /**
     * 缓存管理器
     */
    private final Cache<String, CacheObject<?>> caffeeCache;

    public CaffeineCacheImpl() {
        // 默认1w个空间
        caffeeCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfter(new Expiry<String, CacheObject<?>>() {
                    @Override
                    public long expireAfterCreate(String key, CacheObject value, long currentTime) {
                        if (Objects.isNull(value.expire)) {
                            // 模拟永不过期
                            return Long.MAX_VALUE;
                        }
                        return value.expire.toNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, CacheObject value, long currentTime, @NonNegative long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(String key, CacheObject value, long currentTime, @NonNegative long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    @Override
    public <T> void setCacheObject(String key, T value) {
        caffeeCache.put(key, new CacheObject<>(value, null));
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    @Override
    public <T> void setCacheObject(String key, T value, Integer timeout, TimeUnit timeUnit) {
        caffeeCache.put(key, new CacheObject<>(value, Duration.ofSeconds(timeUnit.toSeconds(timeout))));


    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    @Override
    public boolean expire(String key, long timeout) {
        CacheObject<?> v = caffeeCache.getIfPresent(key);
        if (Objects.isNull(v)) {
            return false;
        }
        v.setExpire(Duration.ofSeconds(timeout));
        caffeeCache.put(key, v);
        return true;
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return expire(key, unit.toSeconds(timeout));
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    @Override
    public long getExpire(String key) {
        CacheObject<?> v = caffeeCache.getIfPresent(key);
        if (Objects.isNull(v)) {
            return 0;
        }
        return v.getExpire().getSeconds();
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    @Override
    public Boolean hasKey(String key) {
        return caffeeCache.getIfPresent(key) != null;
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    @Override
    public <T> T getCacheObject(String key) {
        CacheObject<T> v = (CacheObject<T>) caffeeCache.getIfPresent(key);
        if (Objects.isNull(v)) {
            return null;
        }
        return v.getValue();
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    @Override
    public boolean deleteObject(String key) {
        caffeeCache.invalidate(key);
        return true;
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    @Override
    public boolean deleteObject(Collection collection) {
        if (CollectionUtils.isEmpty(collection)) {
            return false;
        }

        collection.forEach(e-> caffeeCache.invalidate(Objects.toString(e)));
        return true;
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    @Override
    public <T> long setCacheList(String key, List<T> dataList) {
        setCacheObject(key, dataList);

        return dataList.size();
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    @Override
    public <T> List<T> getCacheList(String key) {
        return getCacheObject(key);
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    @Override
    public <T> BoundSetOperations<String, T> setCacheSet(String key, Set<T> dataSet) {
        throw new UnsupportedOperationException("caffeine does not support set operation for now!");
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    @Override
    public <T> Set<T> getCacheSet(String key) {
        throw new UnsupportedOperationException("caffeine does not support set operation for now!");

    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    @Override
    public <T> void setCacheMap(String key, Map<String, T> dataMap) {
        setCacheObject(key, dataMap);
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    @Override
    public <T> Map<String, T> getCacheMap(String key) {
        return getCacheObject(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    @Override
    public <T> void setCacheMapValue(String key, String hKey, T value) {
        Map<String, Object> cacheMap = getCacheMap(key);
        if (Objects.nonNull(cacheMap)) {
            cacheMap.put(hKey, value);
        }
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    @Override
    public <T> T getCacheMapValue(String key, String hKey) {
        Map<String, Object> cacheMap = getCacheMap(key);
        if (Objects.nonNull(cacheMap)) {
            return (T) cacheMap.get(hKey);
        }
        return null;
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    @Override
    public <T> List<T> getMultiCacheMapValue(String key, Collection<Object> hKeys) {
        Map<String, Object> cacheMap = getCacheMap(key);
        if (Objects.nonNull(cacheMap)) {
            List<T> res = new ArrayList<>();
            for (Object hKey : hKeys) {
                res.add((T) cacheMap.get(hKey));
            }

            return res;
        }
        return null;
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    @Override
    public boolean deleteCacheMapValue(String key, String hKey) {
        CacheObject<Map<String, T>> v = (CacheObject<Map<String, T>>) caffeeCache.getIfPresent(key);
        if (Objects.isNull(v) || Objects.isNull(v.getValue().get(hKey))) {
            return false;
        }

        v.getValue().remove(hKey);
        caffeeCache.put(key, v);

        return true;
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    @Override
    public Collection<String> keys(String pattern) {
        // 这里使用缓存的所有键进行匹配（模拟 Redis 的 keys 命令）
        return caffeeCache.asMap().keySet().stream()
                .filter(key -> key.matches(pattern))  // 使用正则表达式进行匹配
                .collect(Collectors.toSet());
    }
}
