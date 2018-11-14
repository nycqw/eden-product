package com.eden.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.eden.annotation.FlowNode;
import com.eden.mapper.TProductMapper;
import com.eden.model.TProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

/**
 * @author chenqw
 * @since  2018/11/3
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
    @FlowNode(order = 1, description = "产品上架")
    public void saveProductInfo(TProduct productInfo) {
        productMapper.insert(productInfo);
    }

}
