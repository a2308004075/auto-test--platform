/**
 * @author HXN
 * @date 2026-08-30
 * @description 评论数据访问接口
 */
package com.platform.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.common.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
