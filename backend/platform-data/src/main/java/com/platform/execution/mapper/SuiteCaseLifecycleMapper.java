/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 套件内用例级生命周期 Mapper
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.SuiteCaseLifecycle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 套件内用例级生命周期数据访问接口
 */
@Mapper
public interface SuiteCaseLifecycleMapper extends BaseMapper<SuiteCaseLifecycle> {
}
