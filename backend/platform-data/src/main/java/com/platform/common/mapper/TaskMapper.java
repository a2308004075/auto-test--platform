/**
 * @author HXN
 * @date 2026-08-30
 * @description 通用任务数据访问接口
 */
package com.platform.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.common.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通用任务 Mapper
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
