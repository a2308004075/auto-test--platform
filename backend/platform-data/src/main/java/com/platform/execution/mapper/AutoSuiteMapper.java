/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化套件数据访问接口
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.AutoSuite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动化套件 Mapper
 */
@Mapper
public interface AutoSuiteMapper extends BaseMapper<AutoSuite> {
}
