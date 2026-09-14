/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷分组 Mapper
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.DefectGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 缺陷分组数据访问层
 */
@Mapper
public interface DefectGroupMapper extends BaseMapper<DefectGroup> {
}
