package com.eden.mapper;

import com.eden.model.TFlowNode;

public interface TFlowNodeMapper {
    int deleteByPrimaryKey(Long id);

    int insert(TFlowNode record);

    int insertSelective(TFlowNode record);

    TFlowNode selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TFlowNode record);

    int updateByPrimaryKey(TFlowNode record);
}