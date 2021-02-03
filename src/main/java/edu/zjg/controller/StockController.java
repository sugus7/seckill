package edu.zjg.controller;
import com.google.common.util.concurrent.RateLimiter;
import edu.zjg.service.OrderService;
import edu.zjg.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.TimeUnit;
@RestController
@RequestMapping("stock")
@Slf4j
public class StockController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;
    //创建令牌桶实例
    private RateLimiter rateLimiter = RateLimiter.create(10);
    //生成md5的方法
    @RequestMapping("md5")
    public String getMd5(Integer id,Integer userid){
        String md5;
        try {
            md5 = orderService.getMd5(id,userid);
        }catch (Exception e){
            e.printStackTrace();
            return "获取MD5失败！"+e.getMessage();
        }
        return "获取md5信息为："+md5;
    }
    @GetMapping("sale")
    public String sale(Integer id){
        //1.没有获取token请求一直等待到获取到token令牌
//        log.info(" 等待时间："+rateLimiter.acquire());
        //2.设置一个等待时间，如果在等待时间内获取到了token令牌，则处理业务，如果在等待时间内没有过去到响应token,则抛弃
        if (rateLimiter.tryAcquire(5, TimeUnit.SECONDS)){
            System.out.println("当前请求被限流，直接抛弃，无法调用后后续秒杀逻辑。。。");
            return "抢购失败！";
        }
        System.out.println("处理业务。。。。。。。。。");
        return "抢购成功！";
    }
    //开发秒杀方法
    @GetMapping("kill")
    public String kill(Integer id){
        System.out.println("秒杀商品的id="+id);
        try{
            int orderId =  orderService.kill(id);
            //根据秒杀商品id去调用秒杀业务
            return "秒杀成功，订单id为："+String.valueOf(orderId);
        }catch(Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
    //开发一个秒杀项目，令牌桶+乐观锁+md5签名（hash接口隐藏）
    @GetMapping("killtokenMD5")
    public String killtoken(Integer id,Integer userid,String md5){
        System.out.println("秒杀商品的id="+id);
        //加入令牌桶的限流
        if (rateLimiter.tryAcquire(30,TimeUnit.SECONDS)){
            log.info("抛弃的请求，抢购失败！！");
            return "抢购失败！请重新抢购";
        }
        try{
            int orderId =  orderService.kill(id,userid,md5);
            //根据秒杀商品id去调用秒杀业务
            return "秒杀成功，订单id为："+String.valueOf(orderId);
        }catch(Exception e) {
            e.printStackTrace();
            return e.getMessage();

        }
    }
    //开发一个秒杀项目，令牌桶+乐观锁+md5签名（hash接口隐藏）+单用户访问频率限制
    @GetMapping("killtokenMD5limit")
    public String killtokenlimit(Integer id,Integer userid,String md5){
        //加入令牌桶的限流
        if (rateLimiter.tryAcquire(30,TimeUnit.SECONDS)){
            log.info("抛弃的请求，抢购失败！！");
            return "抢购失败！请重新抢购";
        }
        try{
            //加入单用户限制调用频率
            int count = userService.saveUserCount(userid);
            log.info("用户截至该次的访问次数为：[{}]",count);
            //判断单个使用次数是否超过限制
            boolean isBanned = userService.getUserCount(userid);
            if (isBanned){
                log.info("购买失败，超过频率限制！");
                return "购买失败，超过频率设置！";
            }
            //根据秒杀商品id去调用秒杀业务
            int orderId =  orderService.kill(id,userid,md5);
            return "秒杀成功，订单id为："+String.valueOf(orderId);
        }catch(Exception e) {
            e.printStackTrace();
            return e.getMessage();

        }
    }

}
