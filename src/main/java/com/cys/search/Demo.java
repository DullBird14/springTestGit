package com.cys.search;

import com.hankcs.hanlp.HanLP;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Demo {
    private Jedis client = null;

    public Demo(Jedis client){
        this.client = client;
    }
    /**
     * 解析文章，创建反向索引
     */
    public void getWordFromText(ContextBean bean){
        //调用分词方法，获取文章得到的分词集合
        List<String> words = HanLP.extractKeyword(bean.getContent(), 10);
        System.out.println(words);

        Pipeline pipelined = client.pipelined();
        for (String word : words) {
            pipelined.sadd("idx:" +word,  bean.getId());
        }
        pipelined.sync();
    }

    /**
     *  枚举调用的方法类型，包括交集，并集，差集
     */
    private static enum RedisCommonType{
        SINTERSTORE,SUNIONSTORE,SDIFFERSTORE
    }

    /**
     * 通用方法，用于交集并集差集
     * @param type  枚举类型
     * @param keys  需要做集合的键
     * @param timeOut   临时结果集的超时时间
     * @return  返回临时集合的uuid，查询的时候需要idx: + uuid为key
     */
    private String commonRedisMethod(RedisCommonType type, List<String> keys, int timeOut){
        //java获取uuid的方法
        String uuid = getUUid();
        String key = "idx:" + uuid;
        Pipeline pipelined = client.pipelined();
        keys = reBulidKeys(keys);
        RedisMethodInterface method = Demo.easyFactory(type);
        method.execute(pipelined, key, keys.toArray(new String[]{}));

//        Response<Set<String>> smembers = pipelined.smembers(key);
        pipelined.expire(key, timeOut);
        pipelined.sync();
//        System.out.println(smembers.get().toString());
        return uuid;
    }

    /**
     * 重组id，将需要搜索的条件，重建成idx:key的形式
     * @param keys
     * @return
     */
    private List<String> reBulidKeys(List<String> keys) {
        List<String> temps = new ArrayList<String>();
        for (String key : keys) {
            temps.add("idx:" + key);
        }
        return temps;
    }

    /**
     * 生成字符串的uuid
     * @return  字符串
     */
    private String getUUid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 简单工厂,通过传入的枚举类，返回对应的对象
     * @param type  枚举类
     * @return  接口对象
     */
    public static RedisMethodInterface easyFactory(RedisCommonType type){
        RedisMethodInterface entity = null;
        if (type.equals(RedisCommonType.SINTERSTORE)) {
            entity = new RedisMethodInterface() {
                public void execute(Pipeline con, String desKey, String[] keys) {
                    con.sinterstore(desKey, keys);
                }
            };
        }else if(type.equals(RedisCommonType.SDIFFERSTORE)){
            entity = new RedisMethodInterface() {
                public void execute(Pipeline con, String desKey, String[] keys) {
                    con.sdiffstore(desKey, keys);
                }
            };
        }else if(type.equals(RedisCommonType.SUNIONSTORE)) {
            entity = new RedisMethodInterface() {
                public void execute(Pipeline con, String desKey, String[] keys) {
                    con.sunionstore(desKey, keys);
                }
            };
        }
        return entity;
    }

    /**
     * 查询方法，通过查询的关键字，获取到搜索的文章
     * @param all   传入查询的关键字，如果有同义词，需要如此结构{{'开心','快乐'},{悲伤}}，其中{'开心','快乐'}为同义词集合，（应该用不到）
     *              例子中的逻辑为，查询文章关键字包含悲伤且包含开心或快乐的文章。
     * @param unWanted  传入不希望匹配到的关键字，结构为{a,b},不支持同义词，前提是all参数必须有值
     * @return  返回一个查询结果的临时集合
     */
    public String queryContext(List<List<String>> all, List<String> unWanted){
        List<String> result = null;
        List<String> intersectWords = new ArrayList<String>();
        for (List<String> keyArray : all) {
            if (keyArray.size() > 1) {
                intersectWords.add(commonRedisMethod(RedisCommonType.SUNIONSTORE, keyArray, 30));
            } else {
                intersectWords.add(keyArray.get(0));
            }
        }

        if (intersectWords.size() >1) {
            final String id = commonRedisMethod(RedisCommonType.SINTERSTORE, intersectWords, 30);
            intersectWords = new ArrayList<String>(){{
                this.add(id);
            }};
        }

        if (unWanted.size() >= 1) {
            ArrayList<String> unWantedRebulid = new ArrayList<String>();
            unWantedRebulid.addAll(intersectWords);
            unWantedRebulid.addAll(unWanted);
            return commonRedisMethod(RedisCommonType.SDIFFERSTORE, unWantedRebulid, 30);
        }
        return intersectWords.get(0);
    }

    public static void main(String[] args) {
        Jedis client = new Jedis("192.168.3.11", 6379);
        Demo aa = new Demo(client);
        ContextBean bean = new ContextBean("开心，收到附件是看附件士大夫斯达克警方和水电开发和，悲伤", "1");
        aa.getWordFromText(bean);
        ContextBean beanOne = new ContextBean("哈哈哈哈，也是很快乐，悲伤", "2");
        aa.getWordFromText(beanOne);
        ContextBean beanTwo = new ContextBean("我什么都不是，就是一个测试", "3");
        aa.getWordFromText(beanTwo);
//        System.out.println(client.smembers("idx:程序"));
//        System.out.println(client.smembers("idx:人员"));
//        System.out.println(client.smembers("idx:特别"));
//        System.out.println(client.smembers("idx:并不"));
//        System.out.println(client.smembers("idx:高级"));
//        System.out.println(client.smembers("idx:软件"));
        List<List<String>> all = new ArrayList<List<String>>();
        List<String> one = new ArrayList<String>(){{
//            this.add("程序");
//            this.add("软件");
            this.add("开心");
            this.add("快乐");
        }};

        List<String> two = new ArrayList<String>(){{
            this.add("悲伤");
        }};
        all.add(one);
        all.add(two);
        List<String> unWanted = new ArrayList<String>(){{
            this.add("快乐");
//            this.add("分析");
        }};

        String s = aa.queryContext(all, unWanted);
        System.out.println(s);
        System.out.println(client.smembers("idx:" + s));
//        client.set("abc", "aaaa");
//        System.out.println(client.get("abc"));
//        String content = "程序员(英文Programmer)是从事程序开发、维护的专业人员。一般将程序员分为程序设计人员和程序编码人员，但两者的界限并不非常清楚，特别是在中国。软件从业人员分为初级程序员、高级程序员、系统分析员和项目经理四大类。";
//        List<String> keywordList = HanLP.extractKeyword(content, 10);
//        System.out.println(keywordList);
    }





}

