/**
 * @author HXN
 * @date 2026-09-14
 * @description 代码仓库分组数据访问接口
 */
package com.platform.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.repository.entity.CodeRepositoryGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 代码仓库分组 Mapper
 */
@Mapper
public interface CodeRepositoryGroupMapper extends BaseMapper<CodeRepositoryGroup> {
}
