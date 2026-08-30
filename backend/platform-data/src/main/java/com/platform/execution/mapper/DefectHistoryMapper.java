/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷变更记录 Mapper
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.DefectHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 缺陷变更记录数据访问层
 */
@Mapper
public interface DefectHistoryMapper extends BaseMapper<DefectHistory> {
}
