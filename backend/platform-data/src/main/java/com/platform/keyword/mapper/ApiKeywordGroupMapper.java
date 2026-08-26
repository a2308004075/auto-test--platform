/**
 * @author HXN
 * @date 2026-08-26
 * @description 接口关键字分组数据访问接口
 */
package com.platform.keyword.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.keyword.entity.ApiKeywordGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 接口关键字分组 Mapper
 */
@Mapper
public interface ApiKeywordGroupMapper extends BaseMapper<ApiKeywordGroup> {
}
