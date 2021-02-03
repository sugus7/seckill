package edu.zjg.service;

import edu.zjg.dao.OrderDAO;
import edu.zjg.dao.StockDAO;
import edu.zjg.dao.UserDAO;
import edu.zjg.entity.Order;
import edu.zjg.entity.Stock;
import edu.zjg.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService{
    @Autowired
    private StockDAO stockDAO;
    @Autowired
    private OrderDAO orderDAO;
    @Autowired
    private UserDAO userDao;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public int kill(Integer id) {
        //校验reids中秒杀商品是否超时
        if (!stringRedisTemplate.hasKey("kill"+id)){
            throw new RuntimeException("当前商品的抢购活动已经结束了!");
        }
        //校验库存
        Stock stock = checkStock(id);
        //更新库存
        updateSale(stock);
        //创建订单
        return createOrder(stock);
    }

    @Override
    public String getMd5(Integer id, Integer userid) {
        //检验用户的合法性
        User user = userDao.findById(userid);
        if(user==null)throw new RuntimeException("用户信息不存在!");
        log.info("用户信息:[{}]",user.toString());
        //检验商品的合法行
        Stock stock = stockDAO.checkStock(id);
        if(stock==null) throw new RuntimeException("商品信息不合法!");
        log.info("商品信息:[{}]",stock.toString());
        //生成hashkey
        String hashKey = "KEY_"+userid+"_"+id;
        //生成md5//这里!QS#是一个盐 随机生成
        String key = DigestUtils.md5DigestAsHex((userid+id+"!Q*jS#").getBytes());
        stringRedisTemplate.opsForValue().set(hashKey, key, 3600, TimeUnit.SECONDS);
        log.info("Redis写入：[{}] [{}]", hashKey, key);
        return key;
    }
    //加入MD5和接口隐藏的kill方法
    @Override
    public int kill(Integer id, Integer userid, String md5) {
        //先验证签名
        String hashKey = "KEY_"+userid+"_"+id;
        String s  = stringRedisTemplate.opsForValue().get(hashKey);
        if (s==null) throw new RuntimeException("没有携带签名");
        if (!s.equals(md5)){
            throw new RuntimeException("当前请求不合法！！");
        }
        //校验库存
        Stock stock = checkStock(id);
        //更新库存
        updateSale(stock);
        //创建订单
        return createOrder(stock);
    }

    private Stock checkStock(Integer id){
        Stock stock = stockDAO.checkStock(id);
        if (stock.getSale().equals(stock.getCount())){
            throw new RuntimeException("库存不足！");
        }
        return stock;
    }
    //扣除库存
    private void updateSale(Stock stock){
        //在sql的层面去完成销量+1合版本号的+1，并且根据商品id和版本号同时查询更新
        int updaterows = stockDAO.updateSale(stock);
        if (updaterows==0){
            throw new RuntimeException("请购失败！");
        }
    }
    //创建库存
    private Integer createOrder(Stock stock){
        Order order = new Order();
        order.setSid(stock.getId()).setName(stock.getName()).setCreateDate(new Date());
        orderDAO.createOrder(order);
        return order.getId();
    }

}
