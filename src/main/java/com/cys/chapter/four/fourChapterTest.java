package com.cys.chapter.four;

import com.cys.chapter.util.JedisClientFactory;
import redis.clients.jedis.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 商品市场，实现事务交易
 */
public class fourChapterTest {
	public Jedis client = JedisClientFactory.getInstance();

	fourChapterTest(){
		init();
	}
	/**
	 * 初始化商品市场
	 */
	private void init(){
		client.hset("users:17", "name", "xiaoMing");
		client.hset("users:17", "funds", "43");
		client.hset("users:27", "name", "xiaoHong");
		client.hset("users:27", "funds", "125");
		client.sadd("goods:17", "itemL", "itemM", "itemN");
		client.sadd("goods:27", "itemO", "itemP", "itemQ");
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
				client.watch(goodKey);
				if (!client.sismember(goodKey, itemId)) {
					client.unwatch();
					return false;
				}
				pipelined.multi();
				pipelined.zadd("market:", price, sellItemName);
				pipelined.srem(goodKey, itemId);
				pipelined.exec();
				pipelined.close();
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
				pipelined.watch("market:", buyerKey);
				Response<Double> maket = pipelined.zscore("market:", itemMarketName);
				Response<String> founds = pipelined.hget(buyerKey, "funds");
				pipelined.sync();
				Double price = maket.get();
				Double buyerFunds = Double.parseDouble(founds.get());
				if ((!lprice.equals(price))
                        || buyerFunds.doubleValue() < lprice.doubleValue()) {
                    client.unwatch();
                    //信息已经变化或者钱不够
                    return false;
                }

				pipelined.multi();
				pipelined.hincrBy(sellerKey, "funds", lprice.longValue());
				pipelined.hincrBy(buyerKey, "funds", -lprice.longValue());
				pipelined.zrem("market:", itemMarketName);
				pipelined.sadd("goods:" + buyerId, itemId);
				pipelined.exec();
				pipelined.close();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * 一个测试打印结果方法
	 */
	void  printlnMessage(){
		synchronized (fourChapterTest.class) {
			System.out.println(Thread.currentThread());
			Map<String, String> userInfoOne = client.hgetAll("users:27");
			System.out.println("=============users:27====================");
			for (Map.Entry<String, String> entry : userInfoOne.entrySet()) {
                System.out.println(entry.getKey() + "  :  " + entry.getValue());
            }
			System.out.println("=============goods:27====================");
			System.out.println(client.smembers("goods:27"));
			Map<String, String> userInfoTwo = client.hgetAll("users:17");
			System.out.println("=============users:17====================");
			for (Map.Entry<String, String> entry : userInfoTwo.entrySet()) {
                System.out.println(entry.getKey() + "  :  " + entry.getValue());
            }
			System.out.println("=============goods:17====================");
			System.out.println(client.smembers("goods:17"));
			Set<Tuple> market = client.zrangeWithScores("market:", 0, -1);
			System.out.println("=============market====================");
			for (Tuple tuple : market) {
                System.out.println(tuple.getElement() + "::" + tuple.getScore());
            }
		}
	}

	public static void main(String[] args) {
		final fourChapterTest test = new fourChapterTest();
		final fourChapterTest testTwo = new fourChapterTest();

//		test.putItemToMarket("itemL", "17", 66.0);
//		test.printlnMessage();
//		test.purchaseGood("27","itemL", "17", 66.0);
//		test.printlnMessage();
		Thread one = new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (test.putItemToMarket("itemL", "17", 66.0)) {
						System.out.println("17上架成功");
					}
					test.printlnMessage();
//					Random rand =new Random(500);
//					try {
//						System.out.println("17进入sleep");
//						Thread.currentThread().sleep(rand.nextInt());
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					System.out.println("17结束sleep");
					if (test.purchaseGood("17", "itemL", "27", 66.0)) {
						System.out.println("17购买成功");
					}
					test.printlnMessage();
				}
			}
		}, "Thread-one");

		Thread two = new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (testTwo.putItemToMarket("itemL", "27", 66.0)) {
						System.out.println("27上架成功");
					}
					testTwo.printlnMessage();
//					Random rand =new Random(500);
//					try {
//						System.out.println("27进入sleep");
//						Thread.currentThread().sleep(rand.nextInt());
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					System.out.println("27醒来");
					if (testTwo.purchaseGood("27", "itemL", "17", 66.0)) {
						System.out.println("27购买成功");
					}
					testTwo.printlnMessage();
				}
			}
		}, "Thread-two");
		one.start();
		two.start();
	}
}
