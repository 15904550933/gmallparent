package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModelProperty;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author smy
 * @create 2020-06-10 14:41
 */
@Mapper
public interface BaseTrademarkMapper extends BaseMapper<BaseTrademark> {

}
