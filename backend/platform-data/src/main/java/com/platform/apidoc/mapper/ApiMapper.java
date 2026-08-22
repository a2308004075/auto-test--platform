/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 接口数据访问接口
 */
package com.platform.apidoc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.apidoc.entity.Api;
import org.apache.ibatis.annotations.Mapper;

/**
 * 接口定义 Mapper
 */
@Mapper
public interface ApiMapper extends BaseMapper<Api> {
}
