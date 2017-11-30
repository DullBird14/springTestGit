package com.cys.chapter.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisClientFactory {
    private static JedisPoolConfig config = null;
    private static JedisPool pool = null;
    static {
        config = new JedisPoolConfig();
        config.setMaxTotal(10);
        config.setMaxIdle(5);
        config.setMaxWaitMillis(30000L);
        config.setTestOnBorrow(true);
        pool = new JedisPool(config, "127.0.0.1", 6379, 10000);
    }

    public static Jedis getInstance(){
        return pool.getResource();
    }
}
