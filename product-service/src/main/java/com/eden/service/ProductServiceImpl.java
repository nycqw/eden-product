package com.eden.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.eden.aspect.lock.annotation.Lock;
import com.eden.aspect.lock.handle.LockType;
import com.eden.domain.request.StockParam;
import com.eden.enums.MQConstants;
import com.eden.mapper.TProductMapper;
import com.eden.model.TProduct;
import com.eden.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String PRODUCT_INFO_CACHE = "productInfoCache";
    private static final String STOCK_AMOUNT_CACHE = "stockAmountCache";

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
     * 扣减库存（加分布式锁）
     */
    @Lock(type = LockType.REDIS_LOCK)
    @Override
    public boolean reduceStockAddLock(StockParam stockParam) {
        TProduct productInfo = queryProductInfo(stockParam.getProductId());
        Long stockAmount = productInfo.getStockAmount();
        Long number = stockParam.getNumber();
        if (stockAmount - number >= 0) {
            productInfo.setStockAmount(stockAmount - number);
            productMapper.updateByPrimaryKey(productInfo);
            log.info("===========扣减成功=========={}", Thread.currentThread().getName());
            return true;
        }
        return false;
    }

    /**
     * 库存信息缓存进redis 中，通过mq 异步更新数据库中的缓存信息，实现redis、mysql 中数据的最终一致性
     */
    @Lock
    @Override
    public boolean reduceStockAsync(StockParam stockParam) {
        Long productId = stockParam.getProductId();
        if (checkAndCacheStock(productId)) {
            Long number = stockParam.getNumber();
            Double remainAmount = redisUtil.hdecr(STOCK_AMOUNT_CACHE, String.valueOf(productId), number);
            if (remainAmount > 0) {
                log.info("剩余数量：{}", remainAmount.longValue());
                stockParam.setStockAmount(remainAmount.longValue());
                asyncReduceStock(stockParam, productId);
                return true;
            } else {
                redisUtil.hincr(STOCK_AMOUNT_CACHE, String.valueOf(productId), number);
            }
        }
        return false;
    }

    private boolean checkAndCacheStock(Long productId) {
        // 缓存中库存数量检查
        if (redisUtil.hHasKey(STOCK_AMOUNT_CACHE, String.valueOf(productId))) {
            Integer stockAmount = (Integer) redisUtil.hget(STOCK_AMOUNT_CACHE, String.valueOf(productId));
            if (stockAmount > 0) return true;
        }
        // 缓存中库存不存在，检查是否补货重新缓存
        TProduct productInfo = queryProductInfo(productId);
        //byte[] stockBytes = ProtoStuffUtils.serialize(productInfo.getStockAmount());
        //redisUtil.setIfNotExists(ProtoStuffUtils.serialize(productId), stockBytes);
        Long stockAmount = productInfo.getStockAmount();
        if (stockAmount > 0) {
            // 已经补货
            redisUtil.setIfNotExists(STOCK_AMOUNT_CACHE, String.valueOf(productId), String.valueOf(stockAmount));
            return true;
        }
        return false;
    }

    private void asyncReduceStock(StockParam stockParam, Long productId) {
        CorrelationData correlationData = new CorrelationData();
        correlationData.setId(String.valueOf(productId));
        rabbitTemplate.convertAndSend(
                MQConstants.PRODUCT_STOCK_REDUCE_EXCHANGE,
                MQConstants.PRODUCT_STOCK_REDUCE_KEY,
                JSON.toJSONString(stockParam),
                correlationData);
    }

}
