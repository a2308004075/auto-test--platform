/**
 * @author HXN
 * @date 2026-08-30 10:00
 * @description 代码仓库拉取历史数据访问接口
 */
package com.platform.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.repository.entity.CodeRepositoryPullLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 代码仓库拉取历史 Mapper
 */
@Mapper
public interface CodeRepositoryPullLogMapper extends BaseMapper<CodeRepositoryPullLog> {
}
