package com.cys.chapter.two;

import com.cys.chapter.util.JedisClientFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.SortingParams;

/*
题目：1.商品浏览原有的存储是有序集合，key为商品id，value为时间戳，
由于时间戳没有实际作用，要求改为列表实现记录用户最近浏览的25个商品
 */
public class WebLogin {
    private Jedis client = JedisClientFactory.getInstance();

    /**
     * 根据传入的令牌获取用户
     * @param token 令牌
     */
    void checkToken(String token){
        client.hget("login:", token);
    }

    /**
     * 根据令牌统计用户登入情况，最近浏览的商品，还有统计商品浏览的次数
     * @param token 令牌
     * @param user  用户ID
     * @param item  商品ID
     */
    void updateToken(String token, String user, String item){
        String time = System.currentTimeMillis()+"";
        client.hset("login", token, user);
        client.hset("recent", token, time);

        if (null != item) {
            //题目1改动
//            client.zadd("viewed" + token, Double.valueOf(time), item);
//            client.zremrangeByRank("viewed" + token, 0, -26);
//            client.zincrby("viewed", -1.0, item);
            client.lrem("viewed", 0, item);
            client.lpush("viewed", item);
            client.ltrim("viewed", 0, 25);

        }
    }

    /**
     * 打印结构的信息
     */
    void printAll(){
        SortingParams params = new SortingParams();
        params.desc();
        params.alpha();
        System.out.println(client.sort("viewed"));
        System.out.println(client.lrange("viewed", 0 , -1));
        System.out.println(client.hgetAll("recent"));

    }

    /**
     * 关闭redis连接
     */
    void close(){
        client.close();
    }

    public static void main(String[] args) {
        WebLogin login = new WebLogin();
        for (int i = 0; i < 30; i++) {
            login.updateToken("123456", "14", i+"");
        }
        login.updateToken("123455", "13", "100");
        login.printAll();
        login.close();
    }
}
