/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 模块数据访问接口
 */
package com.platform.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.project.entity.ApiModule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 接口分组 Mapper
 */
@Mapper
public interface ApiModuleMapper extends BaseMapper<ApiModule> {
}
