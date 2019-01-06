package com.eden.mapper;

import com.eden.model.TSwitch;

public interface TSwitchMapper {
    int deleteByPrimaryKey(Long id);

    int insert(TSwitch record);

    int insertSelective(TSwitch record);

    TSwitch selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TSwitch record);

    int updateByPrimaryKey(TSwitch record);

    TSwitch selectByName(String name);
}