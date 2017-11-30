package com.cys.chapter.one;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import com.cys.config.RedisConfig;

public class OnePonitThree {
	private static final int VOTE_SCORE = 432;
	
	private static final int ONE_WEEK_IN_SECONDS = 2 * 60 * 1000;
	
	private ZSetOperations<String, String> opsForZSet;
	private ValueOperations<String, String> opsForString;
	private SetOperations<String, String> opsForSet;
	private HashOperations<String, Object, Object> opsForHash;
	
	public OnePonitThree() {
		init(getRedisTemplate());
	}
	/*
	 *  发布文章投票
	 */
	public static void main(String[] args) {
		OnePonitThree entity = new OnePonitThree();
		StringRedisTemplate redisTemplate = entity.getRedisTemplate();
		entity.postArticle(redisTemplate, "redis第一章1.3", "user:1", "aaa");
		entity.postArticle(redisTemplate, "redis第一章1.3_1", "user:2", "aaa");
		entity.postArticle(redisTemplate, "redis第一章1.3_3", "user:3", "aaa");
		BigDecimal bd = new BigDecimal(redisTemplate.opsForHash().get("article:1", "time").toString());
		System.out.println(bd.longValue());
		System.out.println(redisTemplate.opsForHash().get("article:1", "time"));
		System.out.println(redisTemplate.opsForHash().get("article:1", "votes"));
		System.out.println(redisTemplate.opsForZSet().score("score:", "article:1"));
		entity.voteArticle(redisTemplate, "user:1", "article:1");
		entity.voteArticle(redisTemplate, "user:2", "article:1");
		entity.voteArticle(redisTemplate, "user:1", "article:3");
		entity.voteArticle(redisTemplate, "user:4", "article:3");
		entity.voteArticle(redisTemplate, "user:2", "article:3");
		List<Map<Object, Object>> articles = entity.getArticles(1, 2);
		entity.printAll(articles);
		Set<TypedTuple<String>> rangeWithScores = redisTemplate.opsForZSet()
				.reverseRangeWithScores("score:", 0, -1);
		for(TypedTuple<String> temp : rangeWithScores){
			System.out.println(temp.getValue() + ":" + temp.getScore());
		}
	}
//	/**
//	 * 获取redis模板
//	 * @return
//	 */
//	public RedisTemplate<String, Object> getRedisTemplate(){
//        ApplicationContext context = new AnnotationConfigApplicationContext(RedisConfig.class);
//        return (RedisTemplate<String, Object>)context.getBean(RedisTemplate.class);
//	}
	
	/**
	 * 获取redis模板
	 * @return
	 */
	public StringRedisTemplate getRedisTemplate(){
        ApplicationContext context = new AnnotationConfigApplicationContext(RedisConfig.class);
        return context.getBean(StringRedisTemplate.class);
	}
	/**
	 * 初始化相关参数
	 * @param redisTemplate
	 */
	private void init(StringRedisTemplate redisTemplate){
		opsForZSet = redisTemplate.opsForZSet();
		opsForString = redisTemplate.opsForValue();
		opsForSet = redisTemplate.opsForSet();
		opsForHash = redisTemplate.opsForHash();
		final int initValue = 0;
		opsForString.set("article:", initValue + "");
	}
	/**
	 * 发布文章
	 * @param redisTemplate
	 * @param title
	 * @param user
	 * @param link
	 */
	public void postArticle(StringRedisTemplate redisTemplate, 
			final String title, final String user, final String link){
//		System.out.println(opsForString.get("article:"));
		Long ids = opsForString.increment("article:", 1);
		String votedKey = "voted:" + ids;
		opsForSet.add(votedKey, user);
		opsForSet.getOperations().expire(votedKey, ONE_WEEK_IN_SECONDS, TimeUnit.MILLISECONDS);
		
		String articleKey = "article:" + ids;
		final long now = System.currentTimeMillis();
//		System.out.println(now);
		Map<Object, Object> params = new HashMap<Object, Object>(){{
			put("title", title);
			put("poster", user);
			put("link", link);
			put("time", now + "");
			put("votes", 1 + "");
		}};
		opsForHash.putAll(articleKey, params);
		
		opsForZSet.add("score:", articleKey, now );
		opsForZSet.add("time:", articleKey, now + VOTE_SCORE);
	}
	
	
	public void voteArticle(StringRedisTemplate redisTemplate,
			String user, String aticle){
		long votedTime = System.currentTimeMillis() - ONE_WEEK_IN_SECONDS;
		BigDecimal bigDecimal = new BigDecimal(opsForZSet.score("time:", aticle));
		if(bigDecimal.longValue() < votedTime){
//			throw new IllegalStateException("超过了投票时间！");
			System.out.println("文章已经过期：超过了投票时间！");
			return ;
		}
		
		String votedKey = aticle.replaceAll(".*:(\\d+)", "voted:$1");
		if(null != opsForSet.add(votedKey, user)){
			opsForZSet.incrementScore("score:", aticle, VOTE_SCORE);
			opsForHash.increment(aticle, "votes", 1);
		}
	}
	/**
	 * 按照特定排序获取文章
	 * @param paget
	 * @param rows
	 * @return
	 */
	public List<Map<Object, Object>> getArticles(int page, int rows){
		int start = (page - 1) * rows;
		int end = page * rows - 1;
		Set<String> reverseRange = opsForZSet.reverseRange("score:", start, end);
		List<Map<Object, Object>> result = new ArrayList<Map<Object, Object>>();
		for(String id : reverseRange){
			result.add(opsForHash.entries(id));
		}
		return result;
	}
	/**
	 * 打印获取文章所有的信息
	 * @param list
	 */
	public void printAll(List<Map<Object, Object>> list){
		for(Map<Object, Object> map : list){
			Set<Entry<Object, Object>> entrySet = map.entrySet();
			for(Entry<Object, Object> entry: entrySet){
				System.out.println(entry.getKey() + ":" + entry.getValue());
			}
			System.out.println("===========================================");
		}
	}
	/**
	 * 添加移除一个组
	 * @param article 文章ID
	 * @param addGroupNames 添加的组
	 * @param leaveGroupNames 需要移除的组
	 */
	public void addAndRemoveGroup(String article, String[] addGroupNames, String[] leaveGroupNames){
		for(String name : addGroupNames)
			opsForSet.add(name, article);
		for(String name : leaveGroupNames)
			opsForSet.remove(name, article);
	}
	/**
	 * 从组里面取出数据，并按照一定顺序排列
	 * @param page 
	 * @param rows
	 * @param oderByItem 排列的数组，最好用枚举类
	 * @return 
	 */
	public Object getGroupArticles(int page, int rows, String oderByItem, String groupName,
			StringRedisTemplate redisTemplate){
		int start = (page - 1) * rows;
		int end = start + rows - 1;
		String orderKey = oderByItem + ":" + groupName;
		Long zCard = opsForZSet.zCard(orderKey);
		if(0 == zCard){
			opsForZSet.intersectAndStore(oderByItem, groupName, orderKey);
		}
		opsForZSet.getOperations().expire(orderKey, 60, TimeUnit.SECONDS);
		return opsForZSet.reverseRange(orderKey, start, end);
	}
}
