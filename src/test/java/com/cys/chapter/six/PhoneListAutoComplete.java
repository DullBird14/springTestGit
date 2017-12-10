package com.cys.chapter.six;

import com.cys.chapter.util.JedisClientFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.io.IOException;

/**
 *
 */
public class PhoneListAutoComplete {
    public Jedis client = JedisClientFactory.getInstance();

    /**
     * 更新最近的联系人，如果已经存在，先移除，并且添加到最前面
     * @param user      用户
     * @param contact   联系人
     */
    void addAndUpdateContact(String user, String contact){
        try {
            String recentKey = "recent:" + user;
            Pipeline pipelined = client.pipelined();
            pipelined.multi();
            pipelined.lrem(recentKey, 0, contact);
            pipelined.lpush(recentKey, contact);
            pipelined.ltrim(recentKey, 0, 99);
            pipelined.exec();
            pipelined.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
