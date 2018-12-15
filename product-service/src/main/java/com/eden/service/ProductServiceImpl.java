package com.eden.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.eden.aspect.lock.annotation.DistributedLock;
import com.eden.aspect.lock.handle.LockType;
import com.eden.mapper.TProductMapper;
import com.eden.model.TProduct;
import com.eden.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

/**
 * @author chenqw
 * @since 2018/11/3
 */
@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private TProductMapper productMapper;

    @Autowired
    private RedisUtil redisUtil;

    private static final String PRODUCT_INFO_CACHE = "productInfoCache";
    private static final String STOCK_AMOUNT_CACHE = "stockAmountCache";

    private Object lock1 = new Object();
    private Object lock2 = new Object();

    @Cacheable(value = PRODUCT_INFO_CACHE)
    @Override
    public TProduct queryProductInfo(Long productId) {
        return productMapper.selectByPrimaryKey(productId);
    }

    @Override
    public void saveProductInfo(TProduct productInfo) {
        productMapper.insert(productInfo);
    }

    /**
     * 扣减库存（未加分布式锁）
     *
     * @param productId 产品ID
     * @param number    扣减数量
     * @return
     */
    @Override
    public boolean reduceStockNormal(Long productId, int number) {
        Integer stockAmount = getStockAmount(productId);

        if (stockAmount != null && stockAmount > 0) {
            synchronized (lock2) {
                stockAmount = (Integer) redisUtil.hget(STOCK_AMOUNT_CACHE, String.valueOf(productId));
                if (stockAmount > 0) {
                    double remainAmount = redisUtil.hdecr(STOCK_AMOUNT_CACHE, String.valueOf(productId), number);
                    log.info("=======================================库存数量：{}", stockAmount);
                    log.info("=======================================剩余数量：{}", remainAmount);
                    // 有库存但少于该用户的购买数
                    if (remainAmount < 0) {
                        // 库存恢复
                        redisUtil.hincr(STOCK_AMOUNT_CACHE, String.valueOf(productId), number);
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取现有库存
     *
     * @param productId 产品ID
     * @return
     */
    private Integer getStockAmount(Long productId) {
        Integer stockAmount = (Integer) redisUtil.hget(STOCK_AMOUNT_CACHE, String.valueOf(productId));
        if (stockAmount == null) {
            synchronized (lock1) {
                stockAmount = (Integer) redisUtil.hget(STOCK_AMOUNT_CACHE, String.valueOf(productId));
                // 初始化缓存
                if (stockAmount == null) {
                    TProduct productInfo = queryProductInfo(productId);
                    if (productInfo != null) {
                        redisUtil.hset(STOCK_AMOUNT_CACHE, String.valueOf(productInfo.getProductId()), productInfo.getStockAmount());
                        return (Integer) redisUtil.hget(STOCK_AMOUNT_CACHE, String.valueOf(productId));
                    }
                    return null;
                }
            }
        }
        return stockAmount;
    }

    /**
     * 扣减库存（加分布式锁）
     *
     * @param productId 产品ID
     * @param number    扣减数量
     * @return
     */
    @DistributedLock(type = LockType.ZOOKEEPER_LOCK)
    @Override
    public boolean reduceStockAddLock(Long productId, Integer number) {
        TProduct productInfo = queryProductInfo(productId);
        Long stockAmount = productInfo.getStockAmount();
        if (stockAmount - number >= 0) {
            productInfo.setStockAmount(stockAmount - number);
            productMapper.updateByPrimaryKey(productInfo);
            log.info("===========扣减成功=========={}", Thread.currentThread().getName());
            return true;
        }
        return false;
    }

}
