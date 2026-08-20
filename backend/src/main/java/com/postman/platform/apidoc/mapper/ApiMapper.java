package com.postman.platform.apidoc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.postman.platform.apidoc.entity.Api;
import org.apache.ibatis.annotations.Mapper;

/**
 * 接口定义 Mapper
 */
@Mapper
public interface ApiMapper extends BaseMapper<Api> {
}
