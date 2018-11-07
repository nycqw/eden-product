package com.eden.mapper;

import model.TProduct;

public interface TProductMapper {
    int deleteByPrimaryKey(Long productId);

    int insert(TProduct record);

    int insertSelective(TProduct record);

    TProduct selectByPrimaryKey(Long productId);

    int updateByPrimaryKeySelective(TProduct record);

    int updateByPrimaryKey(TProduct record);
}