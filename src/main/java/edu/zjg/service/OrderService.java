package edu.zjg.service;

public interface OrderService {
    //用来处理秒杀下单的方法 并返回订单id
    int kill(Integer id);
    //用md5签名的方法
    String getMd5(Integer id, Integer userid);
    //用来处理秒杀的下单方法 并返回订单id加入 md5接口隐藏
    int kill(Integer id, Integer userid, String md5);
}
