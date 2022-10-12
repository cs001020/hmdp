import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.HmDianPingApplication;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@SpringBootTest(classes = HmDianPingApplication.class)
public class LoginTest {
    @Resource
    private UserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成1000名用户
     */
    @Test
    public void testGenerateUser(){
        Long phone=17600000000L;
        for (int i = 0; i < 1000; i++) {
            User user = new User();
            user.setPhone(phone.toString());
            phone++;
            //生成随机昵称
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
            userMapper.insert(user);
        }
    }

    /**
     * 登陆1000名用户 并将token进行存储 用于秒杀优惠券一人一单测试
     * 使用时 注意修改需要登陆的用户id的访问 以及token文件输出路径
     *
     * @throws IOException ioexception
     */
    @Test
    public void testLogin() throws IOException {
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.gt(true,User::getId,1011L);
        List<User> users = userMapper.selectList(userLambdaQueryWrapper);
        File tokens=new File("D:\\Downloads\\apache-jmeter-5.5\\test\\tokens.txt");
        FileOutputStream fileOutputStream = new FileOutputStream(tokens);
        for (User user : users) {
            String token = UUID.randomUUID().toString(true);
            fileOutputStream.write(token.getBytes());
            fileOutputStream.write("\r\n".getBytes());
            //userDTO转map
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
            Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>()
                    , CopyOptions.create().setIgnoreNullValue(true)
                            .setFieldValueEditor(
                                    (name, value) -> value.toString()
                            ));
            //保存用户信息到redis
            stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, map);
            //设置过期时间
            stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        }
    }
}
