/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Token 黑名单数据访问接口
 */
package com.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.auth.entity.TokenBlacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Token 黑名单 Mapper
 */
@Mapper
public interface TokenBlacklistMapper extends BaseMapper<TokenBlacklist> {

    /**
     * 检查 Token JTI 是否在黑名单中
     */
    @Select("SELECT COUNT(*) > 0 FROM token_blacklist WHERE token_jti = #{jti}")
    boolean existsByTokenJti(@Param("jti") String jti);
}
