/**
 * @author HXN
 * @date 2026-08-24
 * @description Action 关键字分组数据访问接口
 */
package com.platform.action.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.action.entity.ActionGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * Action 关键字分组 Mapper
 */
@Mapper
public interface ActionGroupMapper extends BaseMapper<ActionGroup> {
}
