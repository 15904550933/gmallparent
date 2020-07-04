package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.list.repository
 * @create 2020-06-19 11:52
 * @Description: 操作es的API工具类
 *
 */

public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {

}
