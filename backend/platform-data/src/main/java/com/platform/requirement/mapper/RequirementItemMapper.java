/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求条目数据访问接口
 */
package com.platform.requirement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.requirement.entity.RequirementItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 需求条目 Mapper
 */
@Mapper
public interface RequirementItemMapper extends BaseMapper<RequirementItem> {
}
