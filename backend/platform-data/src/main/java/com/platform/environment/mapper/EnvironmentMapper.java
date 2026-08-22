/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 环境配置数据访问接口
 */
package com.platform.environment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.environment.entity.Environment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 环境配置 Mapper
 */
@Mapper
public interface EnvironmentMapper extends BaseMapper<Environment> {
}
