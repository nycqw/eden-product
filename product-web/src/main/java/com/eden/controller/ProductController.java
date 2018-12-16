package com.eden.controller;

import com.eden.domain.request.StockParam;
import com.eden.domain.result.Result;
import com.eden.model.TProduct;
import com.eden.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author chenqw
 * @since 2018/11/3
 */

@Slf4j
@Validated
@Controller
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @RequestMapping("/query")
    @ResponseBody
    public Result queryProductInfo(Long productId) {
        TProduct productInfo = productService.queryProductInfo(productId);
        return Result.success(productInfo);
    }

    @RequestMapping("/save")
    @ResponseBody
    public Result saveProductInfo(@RequestBody @Valid TProduct productInfo) {
        productService.saveProductInfo(productInfo);
        return Result.success();
    }

    @RequestMapping("/reduce/lock")
    @ResponseBody
    public Result stockLock(@RequestBody StockParam stockParam) {
        productService.reduceStockAddLock(stockParam);
        return Result.success();
    }

    @RequestMapping("/reduce/async")
    @ResponseBody
    public Result stockAsync(@RequestBody StockParam stockParam) {
        productService.reduceStockAsync(stockParam);
        return Result.success();
    }

    /*@RequestMapping("/deducting")
    @ResponseBody
    public Result deductingStock(Long productId, Integer number) {
        int threadNum = 10;
        long beginTime = System.currentTimeMillis();
        log.info("开始时间：{}", beginTime);

        CyclicBarrier cyclicBarrier = new CyclicBarrier(threadNum, () -> {
            long endTime = System.currentTimeMillis();
            log.info("结束时间：{}", endTime);
            log.info("总时长：{}", (endTime - beginTime) / threadNum);
        });

        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);

        for (int i = 0; i < threadNum; i++) {
            executorService.submit(() -> {
                countDownLatch.countDown();
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                }
                productService.reduceStockAddLock(productId, number);
                try {
                    cyclicBarrier.await();
                } catch (InterruptedException e) {
                } catch (BrokenBarrierException e) {
                }
            });
        }
        return Result.success();
    }*/

}
