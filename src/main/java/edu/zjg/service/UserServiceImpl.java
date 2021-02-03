package edu.zjg.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;
@Service
@Slf4j
@Transactional
public class UserServiceImpl implements UserService{
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    //向redis中写入访问的数据
    @Override
    public int saveUserCount(Integer userId) {
        //根据不用用户的id生成调用次数的key
        String limitKey = "LIMIT"+"_"+userId;
        //获取redis中指定key的调用次数
        String limitNum = stringRedisTemplate.opsForValue().get(limitKey);
        int limit = -1;
        if (limitNum==null){
            //第一次调用放入redis中设置的值为0
            stringRedisTemplate.opsForValue().set(limitKey,"0",3600, TimeUnit.SECONDS);
        }else{
            //不是第一次调用就将次数+1
            limit = Integer.parseInt(limitNum)+1;
            stringRedisTemplate.opsForValue().set(limitKey,String.valueOf(limit),3600,TimeUnit.SECONDS);
        }
        return limit;
    }
    //取redis中的访问次数
    @Override
    public boolean getUserCount(Integer userId) {
        //根据userid相对应的key获取调用次数
        String limitKey = "LIMIT"+"_"+userId;
        //获取key的调用次数
        String limitNum = stringRedisTemplate.opsForValue().get(limitKey);
        if (limitNum==null){
            log.error("该用户没有访问申请验证记录，疑似异常");
            return  true;
        }
        return Integer.parseInt(limitNum) > 10; //flase代表没有超过 true代表超过
    }
}
