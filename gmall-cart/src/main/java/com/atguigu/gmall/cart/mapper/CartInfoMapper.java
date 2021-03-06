package com.atguigu.gmall.cart.mapper;

import com.atguigu.gmall.bean.CartInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

/**
 * @author Jimmy Hao
 * 2018-08-12 17:06
 */
public interface CartInfoMapper extends Mapper<CartInfo> {
    void deleteCartByIds(@Param("ids") String ids);

}
