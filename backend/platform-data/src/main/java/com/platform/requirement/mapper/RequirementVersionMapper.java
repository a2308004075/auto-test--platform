/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求版本数据访问接口
 */
package com.platform.requirement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.requirement.entity.RequirementVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 需求版本 Mapper
 */
@Mapper
public interface RequirementVersionMapper extends BaseMapper<RequirementVersion> {
}
