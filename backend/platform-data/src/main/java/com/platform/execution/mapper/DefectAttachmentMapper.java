/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷附件 Mapper
 */
package com.platform.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.execution.entity.DefectAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 缺陷附件数据访问层
 */
@Mapper
public interface DefectAttachmentMapper extends BaseMapper<DefectAttachment> {
}
