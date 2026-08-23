/**
 * @author HXN
 * @date 2026-08-23 10:00
 * @description 套件分组数据访问接口
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.SuiteGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 套件分组 Mapper
 */
@Mapper
public interface SuiteGroupMapper extends BaseMapper<SuiteGroup> {
}
