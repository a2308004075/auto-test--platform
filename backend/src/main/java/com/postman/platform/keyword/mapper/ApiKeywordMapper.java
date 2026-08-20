package com.postman.platform.keyword.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.postman.platform.keyword.entity.ApiKeyword;
import org.apache.ibatis.annotations.Mapper;

/**
 * 接口关键字绑定 Mapper
 */
@Mapper
public interface ApiKeywordMapper extends BaseMapper<ApiKeyword> {
}
