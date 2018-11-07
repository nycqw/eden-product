package com.eden.service;

import model.TProduct;

public interface ProductService {

    TProduct queryProductInfo(Long productId);

    void saveProductInfo(TProduct productInfo);
}
