package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    /**
     * 根据id查询商户信息
     *
     * @param id id
     * @return {@link Result}
     */
    Result queryById(Long id);

    /**
     * 更新店铺信息
     *
     * @param shop 商店
     * @return {@link Result}
     */
    Result update(Shop shop);
}
