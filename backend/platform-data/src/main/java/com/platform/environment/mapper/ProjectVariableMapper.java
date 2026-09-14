/**
 * @author HXN
 * @date 2026-08-24
 * @description 项目全局变量数据访问接口
 */
package com.platform.environment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.environment.entity.ProjectVariable;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目全局变量 Mapper
 */
@Mapper
public interface ProjectVariableMapper extends BaseMapper<ProjectVariable> {
}
