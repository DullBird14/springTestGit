package com.cys.chapter.four;

import com.cys.chapter.util.JedisClientFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

/**
 * 商品市场，实现事务交易
 */
public class fourChapterTest {
	public Jedis client = JedisClientFactory.getInstance();

	/**
	 * 初始化商品市场
	 */
	private void init(){
		client.hset("users:17", "name", "xiaoMing");
		client.hset("users:17", "funds", "43");
		client.hset("users:27", "name", "xiaoHong");
		client.hset("users:27", "funds", "125");
		client.set("goods:17", "itemL");
		client.set("goods:17", "itemM");
		client.set("goods:17", "itemN");
		client.set("goods:27", "itemO");
		client.set("goods:27", "itemP");
		client.set("goods:27", "itemQ");
	}

	/**
	 * 将商品提交到市场出售，并从自己的仓库中删除
	 * @param itemId	商品名称
	 * @param sellerId	卖家ID
	 * @param price		价格
	 * @return	成功返回true 其他返回false
	 */
	boolean putItemToMarket(String itemId, String sellerId, Double price){
		String goodKey = "goods:" + sellerId;
		String sellItemName = itemId + "." + sellerId;
		long time = System.currentTimeMillis() + 5000;
		Pipeline pipelined = client.pipelined();

		while (time > System.currentTimeMillis()) {
			try {
				pipelined.watch(goodKey);
				if (!pipelined.sismember(goodKey, sellItemName).get()) {
					client.unwatch();
					return false;
				}

				pipelined.multi();
				pipelined.zadd("market:", price, sellItemName);
				pipelined.srem(goodKey, itemId);
				pipelined.exec();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * 购买商品
	 * @param buyerId 	买家ID
	 * @param itemId	商品ID
	 * @param sellerId	卖家ID
	 * @param lprice	价格
	 * @return	购买成功true,否则false
	 */
	boolean purchaseGood(String buyerId, String itemId, String sellerId, Double lprice){
		String itemMarketName = itemId + "." + sellerId;
		String buyerKey = "users:" + buyerId;
		String sellerKey = "users:" + sellerId;
		long time = System.currentTimeMillis() + 5000;
		Pipeline pipelined = client.pipelined();

		while (time > System.currentTimeMillis()) {
			try {
				pipelined.watch("market", buyerKey);
				Double price = pipelined.zscore("maket", itemMarketName).get();
				Double buyerFounds = Double.parseDouble(pipelined.hget(buyerKey, "founds").get());
				if ((!lprice.equals(price))
                        || buyerFounds.doubleValue() < lprice.doubleValue()) {
                    client.unwatch();
                    //信息已经变化或者钱不够
                    return false;
                }

				pipelined.multi();
				pipelined.hincrBy(sellerKey, "founds", lprice.longValue());
				pipelined.hincrBy(buyerKey, "founds", -lprice.longValue());
				pipelined.zrem("market:", itemMarketName);
				pipelined.sadd("goods:" + buyerId, itemId);
				pipelined.exec();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
}
