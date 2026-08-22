/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试套件数据访问接口
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.TestSuite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试套件 Mapper
 */
@Mapper
public interface TestSuiteMapper extends BaseMapper<TestSuite> {
}
