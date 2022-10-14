package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注或者取关
     *
     * @param followUserId 遵循用户id
     * @param isFollow     是遵循
     * @return {@link Result}
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 是否关注
     *
     * @param followUserId 遵循用户id
     * @return {@link Result}
     */
    Result isFollow(Long followUserId);

    /**
     * 共同关注
     *
     * @param id id
     * @return {@link Result}
     */
    Result followCommons(Long id);
}
