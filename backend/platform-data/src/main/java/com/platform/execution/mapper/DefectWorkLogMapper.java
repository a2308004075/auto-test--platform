/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷工时记录 Mapper
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.DefectWorkLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 缺陷工时记录数据访问层
 */
@Mapper
public interface DefectWorkLogMapper extends BaseMapper<DefectWorkLog> {
}
