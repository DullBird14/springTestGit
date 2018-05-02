package com.cys.search;

import redis.clients.jedis.Pipeline;

public interface RedisMethodInterface {
    public void execute(Pipeline con, String desKey, String[] keys);
}
