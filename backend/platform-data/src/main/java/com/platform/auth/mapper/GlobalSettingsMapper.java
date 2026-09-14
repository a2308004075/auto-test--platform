/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 全局设置数据访问接口
 */
package com.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.auth.entity.GlobalSettings;
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
