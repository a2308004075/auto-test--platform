/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 菜单数据访问接口
 */
package com.platform.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.sys.entity.Menu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统菜单 Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
}
