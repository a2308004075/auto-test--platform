package com.postman.platform.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.postman.platform.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目 Mapper
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
