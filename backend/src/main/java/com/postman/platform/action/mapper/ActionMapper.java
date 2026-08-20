package com.postman.platform.action.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.postman.platform.action.entity.Action;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActionMapper extends BaseMapper<Action> {
}
