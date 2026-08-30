/**
 * @author HXN
 * @date 2026-08-30
 * @description 变更记录数据访问接口
 */
package com.platform.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.common.entity.ChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 变更记录 Mapper
 */
@Mapper
public interface ChangeLogMapper extends BaseMapper<ChangeLog> {
}
