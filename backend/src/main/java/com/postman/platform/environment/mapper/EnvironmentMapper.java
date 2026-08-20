package com.postman.platform.environment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.postman.platform.environment.entity.Environment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 环境配置 Mapper
 */
@Mapper
public interface EnvironmentMapper extends BaseMapper<Environment> {
}
