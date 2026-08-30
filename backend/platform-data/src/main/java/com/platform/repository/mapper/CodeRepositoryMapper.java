/**
 * @author HXN
 * @date 2026-08-30 10:00
 * @description 测试代码仓库数据访问接口
 */
package com.platform.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.repository.entity.CodeRepository;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试代码仓库 Mapper
 */
@Mapper
public interface CodeRepositoryMapper extends BaseMapper<CodeRepository> {
}
