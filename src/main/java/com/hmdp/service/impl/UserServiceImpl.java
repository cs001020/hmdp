package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("phone");
        }
        String code = RandomUtil.randomNumbers(6);
        session.setAttribute("code",code);
        //set k v ex
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        log.debug("发送短信验证码成功,验证码："+code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        // 校验手机号
        String phone = loginForm.getPhone();
            //检查格式
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        if (phoneInvalid){
            return Result.fail("手机号格式错误");
        }

        // 校验验证码
            //从Redis中获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        // 不一致--报错
        if (cacheCode == null || !cacheCode.equals(code)){
            return Result.fail("验证码错误");
        }

        // 根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 判断用户是否存在

        // 不存在--创建新用户并保存
        if(user == null){
            user = createUserWithPhone(phone);
        }
        // 存在--保存用户
        // TODO 保存在redis上
        String token = UUID.randomUUID().toString();
            //user转hash
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((key,value) ->value.toString())
        );

        String key = LOGIN_USER_KEY + token;

        //存入redis，设置时间
        stringRedisTemplate.opsForHash().putAll(key,userMap);
        stringRedisTemplate.expire(key,LOGIN_USER_TTL,TimeUnit.MINUTES);

        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return null;
    }


}
