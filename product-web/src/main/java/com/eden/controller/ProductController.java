package com.eden.controller;

import com.eden.service.ProductService;
import com.eden.util.Result;
import lombok.extern.slf4j.Slf4j;
import model.TProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author chenqw
 * @date 2018/11/3
 */
@Slf4j
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
    public Result saveProductInfo(TProduct productInfo) {
        productService.saveProductInfo(productInfo);
        return Result.success();
    }
}
