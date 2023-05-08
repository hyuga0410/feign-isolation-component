package io.github.hyuga0410.feign.isolation;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * RedisUtil
 *
 * @author hyuga
 * @since 2023/5/6-05-06 10:53
 */
public final class JedisTools {

    private final JedisPool jedisPool;

    JedisTools(String redisUrl, int redisPort, String redisUser, String redisPassword) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 最大连接数为 10
        jedisPoolConfig.setMaxTotal(1000);
        jedisPoolConfig.setMinIdle(500);
        jedisPoolConfig.setMaxWaitMillis(10000);
        jedisPool = new JedisPool(jedisPoolConfig, redisUrl, redisPort, 2000, redisUser, redisPassword);
    }

    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        }
    }

    public void set(String key, String value, long expireSecond) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
            jedis.expire(key, expireSecond);
        }
    }

    public void expire(String key, long expireSecond) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.expire(key, expireSecond);
        }
    }

    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }

    public void del(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        }
    }

    public static void main(String[] args) {
        JedisTools jedisTools = new JedisTools("10.210.10.154", 7001, null, "kfang.com");
        String key = "FEIGN-ISOLATION:service-agent-house:10210";
        jedisTools.set(key, "1", 5);
        System.out.println(jedisTools.get("test"));
        jedisTools.del(key);
    }

}

