/**
 * @author HXN
 * @date 2026-08-30
 * @description 项目文档数据访问接口
 */
package com.platform.projectdoc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.projectdoc.entity.ProjectDoc;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目文档 Mapper
 */
@Mapper
public interface ProjectDocMapper extends BaseMapper<ProjectDoc> {
}
