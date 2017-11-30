package com.cys.chapter.three;

import com.cys.chapter.util.JedisClientFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/*
习题第三章:
1.移除voteArticle竞争条件(还没看出来问题的原因)
2.减少和数据库的交互

 */
public class OnePointerThreeTransaction {
    public Jedis client = JedisClientFactory.getInstance();

    private static final int ONE_WEEK_IN_SECONDS = 2 * 60 * 1000;
    private static final int VOTE_SCORE = 432;

    OnePointerThreeTransaction(){
        init();
    }

    /**
     * 初始化必要的参数
     */
    private void init() {
        final int initValue = 0;
        client.set("article:", initValue + "");
    }

    /**
     * 发布文章
     * @param title 标题
     * @param user  用户
     * @param link  连接
     */
    public void postArticle(final String title, final String user, final String link){
        Long ids = client.incrBy("article:", 1);
        String votedKey = "voted:" + ids;
        client.set(votedKey, user);
        client.expire(votedKey, ONE_WEEK_IN_SECONDS);

        String articleKey = "article:" + ids;
        final long now = System.currentTimeMillis();
        Map<String, String> params = getArticleParam(title, user, link, now+"");
        client.hmset(articleKey, params);

        client.zadd("score:", now*1.0, articleKey);
        client.zadd("time:", now + VOTE_SCORE, articleKey);
    }

    /**
     * 返回一个MAP参数集合
     * @param title 标题
     * @param user  用户
     * @param link  连接
     * @param now   时间
     * @return  返回一个MAP
     */
    static HashMap<String, String> getArticleParam(final String title, final String user, final String link, final String now){
        return new HashMap<String, String>(){{
            put("title", title);
            put("poster", user);
            put("link", link);
            put("time", now);
            put("votes", 1 + "");
        }};
    }
    /**
     * 给一个文章投票
     * @param user  用户ID
     * @param aticle    文章ID
     */
    void voteArticle(String user, String aticle){
        long votedTime = System.currentTimeMillis() - ONE_WEEK_IN_SECONDS;
        BigDecimal bigDecimal = new BigDecimal(client.zscore("time:", aticle));
        if(bigDecimal.longValue() < votedTime){
            System.out.println("文章已经过期：超过了投票时间！");
            return ;
        }

        String votedKey = aticle.replaceAll(".*:(\\d+)", "voted:$1");
        //习题1,利用multi开启事务
        Transaction multi = client.multi();
        if(null != multi.zadd(votedKey, 0, user)){
            multi.zincrby("score:", VOTE_SCORE, aticle);
            multi.hincrBy(aticle, "votes", 1);
        }
        multi.exec();
    }

    /**
     * 按照特定排序获取文章
     * @param page  页数
     * @param rows  行数
     * @return  返回结果
     */
    public List<Map<String, String>> getArticles(int page, int rows){
        int start = (page - 1) * rows;
        int end = page * rows - 1;
        Set<String> reverseRange = client.zrevrange("score:", start, end);
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        Transaction multi = client.multi();
        for(String id : reverseRange){
            result.add(multi.hgetAll(id).get());
        }
        multi.exec();
        return result;
    }

    /**
     * 打印获取文章所有的信息
     * @param list  需要打印的列表
     */
    public void printAll(List<Map<String, String>> list){
        for(Map<String, String> map : list){
            Set<Map.Entry<String, String>> entrySet = map.entrySet();
            for(Map.Entry<String, String> entry: entrySet){
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
        for(String name : addGroupNames) {
            client.sadd(name, article);
        }
        for(String name : leaveGroupNames) {
            client.srem(name, article);
        }
    }
    /**
     * 从组里面取出数据，并按照一定顺序排列
     * @param page  页数
     * @param rows  行数
     * @param oderByItem 排列的数组，最好用枚举类
     * @return  返回一个降序的有序集合
     */
    public Object getGroupArticles(int page, int rows, String oderByItem, String groupName){
        int start = (page - 1) * rows;
        int end = start + rows - 1;
        String orderKey = oderByItem + ":" + groupName;
        Long zCard = client.zcard(orderKey);
        if(0 == zCard){
            client.zinterstore(oderByItem, groupName, orderKey);
        }
        client.expire(orderKey, 60);
        return client.zrevrange(orderKey, start, end);
    }

    public static void main(String[] args) {
//        testConcurrentRedis();
        testArticleFunction();
    }

    /**
     * 测试第一章提交文章，投票，分组，获取的功能
     */
    private static void testArticleFunction() {
        CountDownLatch count = new CountDownLatch(1);
        OnePointerThreeTransaction entity = new OnePointerThreeTransaction();
        entity.postArticle("redis第一章1.3", "user:1", "aaa");
        entity.postArticle("redis第一章1.3_1", "user:2", "aaa");
        entity.postArticle("redis第一章1.3_3", "user:3", "aaa");
        BigDecimal bd = new BigDecimal(entity.client.hget("article:1", "time"));
        System.out.println(bd.longValue());
        System.out.println(entity.client.hget("article:1", "time"));
        System.out.println(entity.client.hget("article:1", "votes"));
        System.out.println(entity.client.zscore("score:", "article:1"));
        entity.voteArticle("user:1", "article:1");
        entity.voteArticle("user:2", "article:1");
        entity.voteArticle("user:1", "article:3");
        entity.voteArticle("user:4", "article:3");
        entity.voteArticle("user:2", "article:3");
        List<Map<String, String>> articles = entity.getArticles(1, 2);
        entity.printAll(articles);
        Set<Tuple> tuples = entity.client.zrevrangeWithScores("score:", 0, -1);
        for(Tuple temp : tuples){
            System.out.println(temp.getElement() + ":" + temp.getScore());
        }
        entity.client.close();
        try {
            count.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试代码,测试并发下的zadd的表现(暂时定论是线程安全的)
     */
    private static void testConcurrentRedis() {
        final CountDownLatch count = new CountDownLatch(1);
        for (int i = 0; i <10 ; i++) {
            Thread testZadd = new Thread(new Runnable() {
                public void run() {
                    try {
                        count.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    OnePointerThreeTransaction test = new OnePointerThreeTransaction();
                    System.out.println(Thread.currentThread().getName() + "::"
                            + test.client.zadd("test", 0 , "14"));
                    test.client.close();
                }
            }, "test"+i);
            testZadd.start();
        }
        count.countDown();
    }
}
