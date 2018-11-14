package com.eden.mapper;

import com.eden.model.TServiceLog;

public interface TServiceLogMapper {
    int deleteByPrimaryKey(Long id);

    int insert(TServiceLog record);

    int insertSelective(TServiceLog record);

    TServiceLog selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TServiceLog record);

    int updateByPrimaryKey(TServiceLog record);
}