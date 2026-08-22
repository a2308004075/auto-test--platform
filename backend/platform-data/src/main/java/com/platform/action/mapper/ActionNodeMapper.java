/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 节点数据访问接口
 */
package com.platform.action.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.action.entity.ActionNode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActionNodeMapper extends BaseMapper<ActionNode> {
}
