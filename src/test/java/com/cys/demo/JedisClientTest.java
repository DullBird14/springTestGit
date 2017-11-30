package com.cys.demo;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisClientTest {

    public static void main(String[] args) {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(10);
		config.setMaxIdle(5);
		config.setMaxWaitMillis(30000L);
		config.setTestOnBorrow(true);
		JedisPool pool = new JedisPool(config, "127.0.0.1", 6379, 10000);
//		Jedis client = new Jedis("127.0.0.1", 6379);
		Jedis client = pool.getResource();
		System.out.println(client.get("article:"));
		System.out.println(client.hgetAll("article:1"));
		client.close();
	}
}
