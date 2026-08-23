/**
 * @author HXN
 * @date 2026-08-23
 * @description 环境变量数据访问接口
 */
package com.platform.environment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.environment.entity.EnvironmentVariable;
import org.apache.ibatis.annotations.Mapper;

/**
 * 环境变量 Mapper
 */
@Mapper
public interface EnvironmentVariableMapper extends BaseMapper<EnvironmentVariable> {
}
