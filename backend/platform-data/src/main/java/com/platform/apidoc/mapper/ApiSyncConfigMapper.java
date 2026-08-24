/**
 * @author HXN
 * @date 2026-08-24
 * @description Swagger 同步配置 Mapper
 */
package com.platform.apidoc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.apidoc.entity.ApiSyncConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiSyncConfigMapper extends BaseMapper<ApiSyncConfig> {
}
