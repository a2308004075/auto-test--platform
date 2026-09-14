/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 工具方法数据访问接口
 */
package com.platform.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.tool.entity.ToolMethod;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ToolMethodMapper extends BaseMapper<ToolMethod> {
}
