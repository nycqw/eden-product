package com.eden.service;

import com.eden.domain.request.StockParam;
import com.eden.model.TProduct;

public interface IProductService {

    /**
     * 获取产品信息
     *
     * @param productId 产品ID
     * @return 产品信息
     */
    TProduct queryProductInfo(Long productId);

    /**
     * 新增产品信息
     *
     * @param productInfo 产品信息
     */
    void saveProductInfo(TProduct productInfo);

    boolean reduceStockAddLock(StockParam stockParam);

    boolean reduceStockAsync(StockParam stockParam);

    boolean reduceStockAsync2(StockParam stockParam);
}
