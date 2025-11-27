package com.bluecone.app.infra.redis.core;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 统一封装 RedisTemplate，屏蔽序列化与模板细节。
 * 作为缓存、分布式锁、幂等、会话、限流等能力的唯一低层入口。
 */
@Component
public class RedisOps {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper redisObjectMapper;

    public RedisOps(RedisTemplate<String, Object> redisTemplate,
                    StringRedisTemplate stringRedisTemplate,
                    ObjectMapper redisObjectMapper) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisObjectMapper = redisObjectMapper;
    }

    /**
     * 读取字符串值，适用于简单标志或计数。
     *
     * @param key Redis key
     * @return 字符串值，不存在返回 null
     */
    public String getString(String key) {
        return execute("GET", key, () -> stringRedisTemplate.opsForValue().get(key));
    }

    /**
     * 写入字符串，可选 TTL，TTL 为空表示持久化。
     *
     * @param key   Redis key
     * @param value 字符串值
     * @param ttl   过期时间，null 表示不过期
     */
    public void setString(String key, String value, Duration ttl) {
        executeVoid("SET", key, () -> {
            if (ttl == null) {
                stringRedisTemplate.opsForValue().set(key, value);
            } else {
                stringRedisTemplate.opsForValue().set(key, value, ttl);
            }
        });
    }

    /**
     * 仅在 key 不存在时写入字符串，可选 TTL。
     *
     * @param key   Redis key
     * @param value 字符串值
     * @param ttl   过期时间，成功写入且不传表示不过期
     * @return 成功写入返回 true
     */
    public Boolean setIfAbsent(String key, String value, Duration ttl) {
        return execute("SETNX", key, () -> ttl == null
                ? stringRedisTemplate.opsForValue().setIfAbsent(key, value)
                : stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl));
    }

    /**
     * 将字符串数值自增指定步长。
     *
     * @param key   Redis key
     * @param delta 自增步长
     * @return 自增后的数值
     */
    public Long incr(String key, long delta) {
        return execute("INCRBY", key, () -> stringRedisTemplate.opsForValue().increment(key, delta));
    }

    /**
     * 将字符串数值自减指定步长。
     *
     * @param key   Redis key
     * @param delta 自减步长
     * @return 自减后的数值
     */
    public Long decr(String key, long delta) {
        return execute("DECRBY", key, () -> stringRedisTemplate.opsForValue().decrement(key, delta));
    }

    /**
     * 设置 key 过期时间。
     *
     * @param key Redis key
     * @param ttl 过期时间，不可为空
     * @return 成功设置返回 true
     */
    public Boolean expire(String key, Duration ttl) {
        Assert.notNull(ttl, "ttl must not be null");
        return execute("EXPIRE", key, () -> stringRedisTemplate.expire(key, ttl));
    }

    /**
     * 删除 key。
     *
     * @param key Redis key
     * @return 成功删除返回 true
     */
    public Boolean delete(String key) {
        return execute("DEL", key, () -> stringRedisTemplate.delete(key));
    }

    /**
     * 获取对象并转换为指定类型。
     *
     * @param key  Redis key
     * @param type 目标类型
     * @param <T>  泛型返回
     * @return 映射后的对象，不存在返回 null
     */
    public <T> T getObject(String key, Class<T> type) {
        return execute("GET", key, () -> convert(redisTemplate.opsForValue().get(key), type));
    }

    /**
     * 写入对象，可选 TTL。
     *
     * @param key   Redis key
     * @param value 存储对象
     * @param ttl   过期时间，null 表示不过期
     */
    public void setObject(String key, Object value, Duration ttl) {
        executeVoid("SET", key, () -> {
            if (ttl == null) {
                redisTemplate.opsForValue().set(key, value);
            } else {
                redisTemplate.opsForValue().set(key, value, ttl);
            }
        });
    }

    /**
     * 获取哈希字段并转换为指定类型。
     *
     * @param key   Redis key
     * @param field 哈希字段
     * @param type  目标类型
     * @param <T>   泛型返回
     * @return 映射后的值，不存在返回 null
     */
    public <T> T hGet(String key, String field, Class<T> type) {
        return execute("HGET", key, () -> convert(redisTemplate.opsForHash().get(key, field), type));
    }

    /**
     * 获取哈希全部字段并映射为指定类型。
     *
     * @param key       Redis key
     * @param valueType 值类型
     * @param <T>       泛型返回
     * @return 映射后的 Map，不存在返回空 Map
     */
    public <T> Map<String, T> hGetAll(String key, Class<T> valueType) {
        return execute("HGETALL", key, () -> {
            Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
            if (raw == null || raw.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, T> mapped = new LinkedHashMap<>(raw.size());
            raw.forEach((k, v) -> mapped.put(String.valueOf(k), convert(v, valueType)));
            return mapped;
        });
    }

    /**
     * 写入哈希字段。
     *
     * @param key   Redis key
     * @param field 哈希字段
     * @param value 值
     */
    public void hSet(String key, String field, Object value) {
        executeVoid("HSET", key, () -> redisTemplate.opsForHash().put(key, field, value));
    }

    /**
     * 删除一个或多个哈希字段。
     *
     * @param key    Redis key
     * @param fields 需删除的字段
     */
    public void hDel(String key, String... fields) {
        executeVoid("HDEL", key, () -> redisTemplate.opsForHash().delete(key, (Object[]) fields));
    }

    /**
     * 左侧入队列表。
     *
     * @param key   Redis key
     * @param value 值
     */
    public void lPush(String key, Object value) {
        executeVoid("LPUSH", key, () -> redisTemplate.opsForList().leftPush(key, value));
    }

    /**
     * 右侧入队列表。
     *
     * @param key   Redis key
     * @param value 值
     */
    public void rPush(String key, Object value) {
        executeVoid("RPUSH", key, () -> redisTemplate.opsForList().rightPush(key, value));
    }

    /**
     * 左侧弹出列表元素。
     *
     * @param key  Redis key
     * @param type 目标类型
     * @param <T>  泛型返回
     * @return 映射值，列表为空返回 null
     */
    public <T> T lPop(String key, Class<T> type) {
        return execute("LPOP", key, () -> convert(redisTemplate.opsForList().leftPop(key), type));
    }

    /**
     * 右侧弹出列表元素。
     *
     * @param key  Redis key
     * @param type 目标类型
     * @param <T>  泛型返回
     * @return 映射值，列表为空返回 null
     */
    public <T> T rPop(String key, Class<T> type) {
        return execute("RPOP", key, () -> convert(redisTemplate.opsForList().rightPop(key), type));
    }

    /**
     * 获取列表区间元素。
     *
     * @param key   Redis key
     * @param start 起始下标
     * @param end   结束下标
     * @param type  目标类型
     * @param <T>   泛型返回
     * @return 映射列表，不存在返回空列表
     */
    public <T> List<T> lRange(String key, long start, long end, Class<T> type) {
        return execute("LRANGE", key, () -> {
            List<Object> raw = redisTemplate.opsForList().range(key, start, end);
            if (raw == null || raw.isEmpty()) {
                return Collections.emptyList();
            }
            return raw.stream().map(value -> convert(value, type)).collect(Collectors.toList());
        });
    }

    /**
     * 向集合添加元素。
     *
     * @param key    Redis key
     * @param values 元素
     */
    public void sAdd(String key, Object... values) {
        executeVoid("SADD", key, () -> redisTemplate.opsForSet().add(key, values));
    }

    /**
     * 获取集合全部成员。
     *
     * @param key       Redis key
     * @param valueType 目标类型
     * @param <T>       泛型返回
     * @return 映射后的集合，不存在返回空集合
     */
    public <T> Set<T> sMembers(String key, Class<T> valueType) {
        return execute("SMEMBERS", key, () -> {
            Set<Object> raw = redisTemplate.opsForSet().members(key);
            if (raw == null || raw.isEmpty()) {
                return Collections.emptySet();
            }
            Set<T> mapped = new LinkedHashSet<>(raw.size());
            raw.forEach(item -> mapped.add(convert(item, valueType)));
            return mapped;
        });
    }

    /**
     * 判断元素是否在集合中。
     *
     * @param key   Redis key
     * @param value 元素
     * @return 在集合中返回 true
     */
    public Boolean sIsMember(String key, Object value) {
        return execute("SISMEMBER", key, () -> redisTemplate.opsForSet().isMember(key, value));
    }

    /**
     * 向有序集合添加元素及分数。
     *
     * @param key   Redis key
     * @param value 元素
     * @param score 分数
     */
    public void zAdd(String key, Object value, double score) {
        executeVoid("ZADD", key, () -> redisTemplate.opsForZSet().add(key, value, score));
    }

    /**
     * 获取有序集合区间。
     *
     * @param key   Redis key
     * @param start 起始排名
     * @param end   结束排名
     * @param type  目标类型
     * @param <T>   泛型返回
     * @return 映射后的成员，按分数排序
     */
    public <T> Set<T> zRange(String key, long start, long end, Class<T> type) {
        return execute("ZRANGE", key, () -> {
            Set<Object> raw = redisTemplate.opsForZSet().range(key, start, end);
            if (raw == null || raw.isEmpty()) {
                return Collections.emptySet();
            }
            Set<T> mapped = new LinkedHashSet<>(raw.size());
            raw.forEach(item -> mapped.add(convert(item, type)));
            return mapped;
        });
    }

    /**
     * 从有序集合移除成员。
     *
     * @param key    Redis key
     * @param values 待移除成员
     */
    public void zRemove(String key, Object... values) {
        executeVoid("ZREM", key, () -> redisTemplate.opsForZSet().remove(key, values));
    }

    private void executeVoid(String operation, String key, Runnable runnable) {
        execute(operation, key, () -> {
            runnable.run();
            return null;
        });
    }

    private <T> T execute(String operation, String key, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            String message = String.format("Redis 操作失败，op=%s, key=%s", operation, key);
            throw new RedisOperationException(operation, key, message, ex);
        }
    }

    private <T> T convert(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return redisObjectMapper.convertValue(value, type);
    }
}
