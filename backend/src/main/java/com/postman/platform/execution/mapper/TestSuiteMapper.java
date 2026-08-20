package com.postman.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.postman.platform.execution.entity.TestSuite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试套件 Mapper
 */
@Mapper
public interface TestSuiteMapper extends BaseMapper<TestSuite> {
}
