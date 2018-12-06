package com.eden.controller;

import com.eden.service.ProductService;
import com.eden.domain.result.Result;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.eden.model.TProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @RequestMapping("/deducting")
    @ResponseBody
    public Result deductingStock(Long productId, Integer number) throws Exception {
        int threadNum = 500;
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
        for (int i = 0; i < threadNum; i++) {
            executorService.submit(() -> {
                countDownLatch.countDown();
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                }
                productService.deductingStock(productId, number);
            });
        }
        return Result.success();
    }

}
