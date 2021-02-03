package edu.zjg.dao;

import edu.zjg.entity.Stock;

public interface StockDAO {
    //根据商品id查询库存信息的方法
    Stock checkStock(Integer id);
    int updateSale(Stock s);
}
