/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化用例数据访问接口
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.AutoCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动化用例 Mapper
 */
@Mapper
public interface AutoCaseMapper extends BaseMapper<AutoCase> {
}
