/**
 * @author HXN
 * @date 2026-08-23
 * @description 自动化用例分组数据访问接口
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.AutoCaseGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动化用例分组 Mapper
 */
@Mapper
public interface AutoCaseGroupMapper extends BaseMapper<AutoCaseGroup> {
}
