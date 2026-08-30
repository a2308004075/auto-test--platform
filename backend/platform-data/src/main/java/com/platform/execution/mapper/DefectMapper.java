/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷 Mapper
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.Defect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 缺陷数据访问层
 */
@Mapper
public interface DefectMapper extends BaseMapper<Defect> {

    /**
     * 查询项目下最大缺陷序号（用于生成缺陷编号）
     */
    @Select("SELECT MAX(CAST(SUBSTRING_INDEX(defect_no, '-', -1) AS UNSIGNED)) " +
            "FROM defect WHERE project_id = #{projectId} AND defect_no LIKE CONCAT('BUG-', #{projectId}, '-%')")
    Integer selectMaxSequence(@Param("projectId") Long projectId);
}
