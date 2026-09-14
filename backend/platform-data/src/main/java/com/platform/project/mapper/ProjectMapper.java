/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 项目数据访问接口
 */
package com.platform.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目 Mapper
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
