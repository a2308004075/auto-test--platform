/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷关联 Mapper
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.DefectRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 缺陷关联数据访问层
 */
@Mapper
public interface DefectRelationMapper extends BaseMapper<DefectRelation> {
}
