package com.eden.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.eden.domain.request.StockParam;
import com.eden.enums.MQConstants;
import com.eden.mapper.TProductMapper;
import com.eden.model.TProduct;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author chenqw
 * @version 1.0
 * @since 2018/12/16
 */
@Slf4j
@Component
public class ProductStockReduceListener {

    @Autowired
    private TProductMapper productMapper;

    @RabbitListener(bindings = @QueueBinding(
            key = MQConstants.PRODUCT_STOCK_REDUCE_KEY,
            value = @Queue(value = MQConstants.PRODUCT_STOCK_REDUCE_QUEUE, durable = "true"),
            exchange = @Exchange(value = MQConstants.PRODUCT_STOCK_REDUCE_EXCHANGE, type = "topic")
    ))
    public void reduceStock(String msg, Channel channel, Message message) {
        StockParam stockParam = JSON.parseObject(msg, new TypeReference<StockParam>() {
        });

        TProduct productInfo = productMapper.selectByPrimaryKey(stockParam.getProductId());
        if (productInfo.getStockAmount() - stockParam.getPurchaseAmount() >= 0) {
            productMapper.updateStock(stockParam);
            log.info("===========扣减成功==========");
        } else {
            log.warn("===========扣减失败==========");
        }

        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (Exception ex) {
                log.error("mq basicNack exception", ex.getMessage());
            }
        }
    }

}
