package com.postman.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.postman.platform.auth.entity.GlobalSettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 全局配置 Mapper
 */
@Mapper
public interface GlobalSettingsMapper extends BaseMapper<GlobalSettings> {

    /**
     * 根据配置键查询配置
     */
    @Select("SELECT * FROM global_settings WHERE config_key = #{configKey}")
    GlobalSettings selectByConfigKey(@Param("configKey") String configKey);
}
