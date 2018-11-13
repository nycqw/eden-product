package com.eden.service;

import com.eden.model.TProduct;

public interface ProductService {

    TProduct queryProductInfo(Long productId);

    void saveProductInfo(TProduct productInfo);
}
