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

import javax.annotation.PostConstruct;
import java.util.List;

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

    /**
     * 初始化所有的秒杀产品
     */
    @PostConstruct
    public void init() {
        List<TProduct> productList = productMapper.selectList();
        for (TProduct product : productList) {
            boolean result = redisUtil.hSetNX(STOCK_AMOUNT_CACHE, String.valueOf(product.getProductId()), String.valueOf(product.getStockAmount()));
            if (!result) {
                Integer stockAmount = (Integer) redisUtil.hget(STOCK_AMOUNT_CACHE, String.valueOf(product.getProductId()));
                if (stockAmount == 0) {
                    redisUtil.hset(STOCK_AMOUNT_CACHE, String.valueOf(product.getProductId()), String.valueOf(product.getStockAmount()));
                }
            }
        }
    }

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
        Integer stockAmount = (Integer) redisUtil.hget(STOCK_AMOUNT_CACHE, String.valueOf(productId));
        if (stockAmount > 0) {
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

    /**
     * 尽早返回原则：若不符合条件尽早返回，不执行多余的逻辑
     * 在秒杀场景下大部分扣减库存的请求都是失败的，所以将判断是否扣减成功放在最前面
     * @param stockParam
     * @return
     */
    @Lock
    @Override
    public boolean reduceStockAsync2(StockParam stockParam) {
        Long productId = stockParam.getProductId();
        if (!redisUtil.hHasKey(STOCK_AMOUNT_CACHE, String.valueOf(productId))) {
            return false;
        }

        Long number = stockParam.getNumber();
        Double remainAmount = redisUtil.hdecr(STOCK_AMOUNT_CACHE, String.valueOf(productId), number);
        if (remainAmount < 0) {
            redisUtil.hincr(STOCK_AMOUNT_CACHE, String.valueOf(productId), number);
            log.info("库存不足，操作回退");
            return false;
        } else {
            // 异步库存扣减
            stockParam.setStockAmount(remainAmount.longValue());
            asyncReduceStock(stockParam, productId);
            if (remainAmount == 0) {
                redisUtil.hdel(STOCK_AMOUNT_CACHE, String.valueOf(productId));
            }
            log.info("剩余数量：{}", remainAmount.longValue());
            return true;
        }
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
