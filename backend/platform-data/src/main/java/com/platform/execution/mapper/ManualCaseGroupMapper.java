/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动化用例分组数据访问接口
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.ManualCaseGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 手动化用例分组 Mapper
 */
@Mapper
public interface ManualCaseGroupMapper extends BaseMapper<ManualCaseGroup> {
}
