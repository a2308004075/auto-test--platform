/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求-用例关联数据访问接口
 */
package com.platform.requirement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.requirement.entity.RequirementCaseRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 需求-用例关联 Mapper
 */
@Mapper
public interface RequirementCaseRelationMapper extends BaseMapper<RequirementCaseRelation> {
}
