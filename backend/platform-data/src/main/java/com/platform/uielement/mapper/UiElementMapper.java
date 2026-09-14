/**
 * @author HXN
 * @date 2026-08-30 14:00
 * @description 界面元素数据访问接口
 */
package com.platform.uielement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.uielement.entity.UiElement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 界面元素 Mapper
 */
@Mapper
public interface UiElementMapper extends BaseMapper<UiElement> {

    /**
     * 批量插入界面元素（导入解析结果时使用，created_at/updated_at 由数据库默认值生成）
     *
     * @param list 元素列表
     * @return 插入条数
     */
    int insertBatch(@Param("list") List<UiElement> list);
}
