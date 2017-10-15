package com.cys.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {
	@Bean
	public RedisConnectionFactory redisCF(){
		JedisConnectionFactory jcf = new JedisConnectionFactory();
		jcf.setPort(6379);
		return jcf;
	}
	
	@Bean
	public StringRedisTemplate redisTemplate(RedisConnectionFactory rcf){
		return new StringRedisTemplate(rcf);
	}
}
