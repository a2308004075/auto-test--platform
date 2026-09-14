/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 关键字数据访问接口
 */
package com.platform.keyword.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.keyword.entity.Keyword;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一关键字 Mapper
 */
@Mapper
public interface KeywordMapper extends BaseMapper<Keyword> {
}
