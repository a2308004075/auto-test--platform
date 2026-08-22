/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 字典数据访问接口
 */
package com.platform.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.sys.entity.Dict;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据字典 Mapper
 */
@Mapper
public interface DictMapper extends BaseMapper<Dict> {
}
