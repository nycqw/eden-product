package com.eden.mapper;

import com.eden.domain.request.StockParam;
import com.eden.model.TProduct;

public interface TProductMapper {
    int deleteByPrimaryKey(Long id);

    int insert(TProduct record);

    int insertSelective(TProduct record);

    TProduct selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TProduct record);

    int updateByPrimaryKey(TProduct record);

    void updateStock(StockParam stockParam);
}