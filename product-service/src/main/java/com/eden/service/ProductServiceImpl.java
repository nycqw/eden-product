package com.eden.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.eden.mapper.TProductMapper;
import com.eden.model.TProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

/**
 * @author chenqw
 * @date 2018/11/3
 */
@Service
public class ProductServiceImpl implements ProductService{

    @Autowired
    private TProductMapper productMapper;

    @Cacheable
    @Override
    public TProduct queryProductInfo(Long productId) {
        return productMapper.selectByPrimaryKey(productId);
    }

    @Override
    public void saveProductInfo(TProduct productInfo) {
        productMapper.insert(productInfo);
    }

}
