package com.cys.demo;

import java.util.Set;
import java.util.TreeSet;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import com.cys.config.RedisConfig;
import com.cys.config.javaConfig;
import com.cys.controller.DemoOneController;

public class MainTest {
	public static void main(String[] args) {
		//xml����
//		GenericXmlApplicationContext context = new GenericXmlApplicationContext();  
//        context.setValidating(false);  
//        context.load("/beans.xml");  
//        context.refresh(); 
//        context.getBean("testClass");
//        TestClass test = context.getBean(TestClass.class);  
//        test.test();
//		//java����
        ApplicationContext context = new AnnotationConfigApplicationContext(RedisConfig.class);
        StringRedisTemplate template = context.getBean(StringRedisTemplate.class);
        //�ַ�
//        redisStringIncrease(template);
        //zset
        redisZSetTest(template);
        //test
//        System.out.println(template.opsForList().leftPush("one", "1231"));
//        System.out.println(template.opsForList().leftPop("one"));
	}
	/**
	 * �ַ��redis api����
	 * @param template
	 */
	public static void redisStringIncrease(StringRedisTemplate template){
		ValueOperations<String, String> stringOperation = template.opsForValue();
		System.out.println(stringOperation.get("key"));
		System.out.println(stringOperation.increment("key", 15l));
		System.out.println(stringOperation.increment("key", -5l));
		template.delete("key");
	}
	/**
	 * ���򼯺ϵ�redis ����
	 */
	public static void redisZSetTest(StringRedisTemplate template){
		ZSetOperations<String, String> zSet = template.opsForZSet();
		System.out.println(zSet.add("zset-key", "c", 3));
		Set<TypedTuple<String>> values = new TreeSet<TypedTuple<String>>();
//		DefaultTypedTuple<String> temp = new DefaultTypedTuple<String>("b", 2.0);
		values.add(new DefaultTypedTuple<String>("b", 2.0));
		values.add(new DefaultTypedTuple<String>("a", 1.0));
		System.out.println(zSet.add("zset-key", values));
		System.out.println(zSet.incrementScore("zset-key", "a", 2));
		Set<TypedTuple<String>> rangeWithScores = zSet.rangeWithScores("zset-key", 0, -1);
		for(TypedTuple<String> tem : rangeWithScores){
			System.out.println(tem.getValue() + " : " + tem.getScore());
		}
		System.out.println(zSet.rank("zset-key", "a"));
		System.out.println(zSet.rank("zset-key", "c"));
		System.out.println(zSet.range("zset-key", 0, -1));
//		System.out.println(zSet.removeRange("zset-key", 0, -1));
	}
}
