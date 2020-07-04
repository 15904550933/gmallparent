package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategory1;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.product.mapper
 * @create 2020-06-09 11:31
 * @Description:
 */
@Mapper
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {
    //细节：如果接口中传递多个参数，则需要指明参数与sql 条件中的那个参数！
    //编写：xml文件
    List<BaseAttrInfo> selectBaseAttrInfoList(@Param("category1Id") Long category1Id,
                                              @Param("category2Id") Long category2Id,
                                              @Param("category3Id") Long category3Id);

    /**
     * 通过skuId 集合来查询数据 获取平台属性，平台属性值
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> selectAttrInfoList(Long skuId);
}
