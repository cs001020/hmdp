package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Redis数据
 *
 * @author CHEN
 * @date 2022/10/08
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
