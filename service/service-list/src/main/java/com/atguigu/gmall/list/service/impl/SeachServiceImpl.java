package com.atguigu.gmall.list.service.impl;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.bouncycastle.cert.ocsp.Req;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.hibernate.validator.resourceloading.AggregateResourceBundleLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.list.service.impl
 * @create 2020-06-19 11:48
 * @Description:
 */
@Service
public class SeachServiceImpl implements SearchService {

    //注入service-product-client对象
    @Autowired
    private ProductFeignClient productFeignClient;

    //操作es的API工具类，启动类在list下，全盘扫描。可以自动注入
    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    /**
     * 商品上架
     *
     * @param skuId
     */
    @Override
    public void upperGoods(Long skuId) {
        //声明一个实体类 Goods
        Goods goods = new Goods();

        //查询商品基本信息
        SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
        //赋值基本信息
        if (null != skuInfo) {
            goods.setId(skuId);
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setTitle(skuInfo.getSkuName());
            goods.setCreateTime(new Date());


            //查询商品分类信息
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            //赋值商品分类信息
            if (null != categoryView) {
                goods.setCategory1Id(categoryView.getCategory1Id());
                goods.setCategory1Name(categoryView.getCategory1Name());
                goods.setCategory2Id(categoryView.getCategory2Id());
                goods.setCategory2Name(categoryView.getCategory2Name());
                goods.setCategory3Id(categoryView.getCategory3Id());
                goods.setCategory3Name(categoryView.getCategory3Name());
            }


            //查询平台属性信息
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuInfo.getId());
            //流式编程，map会循环多次取值
            /*
            SELECT
                bai.id,
                bai.attr_name,
                bai.category_id,
                bai.category_level,
                bav.id attr_value_id,
                bav.value_name,
                bav.attr_id
            FROM
                base_attr_info bai
                INNER JOIN base_attr_value bav ON bai.id = bav.attr_id
                INNER JOIN sku_attr_value sav ON sav.value_id = bav.id
            WHERE
                sav.sku_id = #{skuId}
             */
            List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
                //通过baseAttrInfo获取平台属性Id
                SearchAttr searchAttr = new SearchAttr();

                //赋值平台属性值Id
                searchAttr.setAttrId(baseAttrInfo.getId());
                //赋值平台属性名称
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                //赋值平台属性值名称
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                searchAttr.setAttrValue(attrValueList.get(0).getValueName());

                //将每个平台属性对象searchAttr返回去
                return searchAttr;

                //collect(Collectors.toList())  -->  变成集合
            }).collect(Collectors.toList());
            //存储平台属性信息
            if (null != searchAttrList) {
                goods.setAttrs(searchAttrList);
            }


            //查询品牌信息
            BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
            //储存品牌信息
            if (null != trademark) {
                goods.setTmId(trademark.getId());
                goods.setTmName(trademark.getTmName());
                goods.setTmLogoUrl(trademark.getLogoUrl());
            }

        }

        //将数据保存到es中！
        goodsRepository.save(goods);


    }

    /**
     * 下架商品列表
     *
     * @param skuId
     */
    @Override
    public void lowerGoods(Long skuId) {
        this.goodsRepository.deleteById(skuId);
    }

    /**
     * 上架多个商品列表(skuId)
     */
    @Override
    public void upperGoods() {
        //读取一个Excel表格
    }

    /**
     * 上架多个商品列表(skuId)
     *
     * @param skuId 不定参数
     */
    @Override
    public void upperGoods(Long... skuId) {

    }

    /**
     * 更新热点
     *
     * @param skuId
     */
    @Override
    public void incrHotScore(Long skuId) {
        //需要借助redis
        String key = "hotScore";
        //用户每访问一次，那么这个数据应该 +1 , 成员以商品Id为单位
        Double hotScore = redisTemplate.opsForZSet().incrementScore(key + skuId, "skuId:" + skuId, 1);
        //按照规定更新es数据
        if (hotScore % 10 == 0) {
            //更新一次es中的hotScore操作
            //获取到es中得对象
            Optional<Goods> optional = goodsRepository.findById(skuId);
            //获取到了当前对象
            Goods goods = optional.get();
            //覆盖
            goods.setHotScore(Math.round(hotScore));
            //保存
            goodsRepository.save(goods);
        }
    }

    /**
     * 检索数据
     * @param searchParam
     * @return
     */
    @Override
    public SearchResponseVo search(SearchParam searchParam) throws Exception {
        //构建DSL语句:利用JAVA代码实现动态DSL语句
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //执行DSL语句
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //获取返回的结果集
        SearchResponseVo responseVo = parseSearchResult(searchResponse);

        //赋值分页相关的数据属性，设定一个默认值
        responseVo.setPageSize(searchParam.getPageSize());
        responseVo.setPageNo(searchParam.getPageNo());
        //根据总条数没有显示的多少来激素按(总页数计算公式)
        long totalPages = (responseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize();

        // 总页数
        responseVo.setTotalPages(totalPages);

        return responseVo;
    }

    /**
     * //获取返回的结果集
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        //声明一个searchResponseVo对象
        SearchResponseVo searchResponseVo = new SearchResponseVo();

        //获取到品牌信息（获取Agg终得品牌信息，没有重复）
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        //获取到了品牌Id Agg , 获取到桶(Buckets)信息
        //Aggregation ---> ParsedLongTerms
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");

        List<SearchResponseTmVo> responseTmVoList = tmIdAgg.getBuckets().stream().map(bucket -> {
            // 声明一个品牌对象
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            // 获取到品牌Id
            String tmId = ((Terms.Bucket) bucket).getKeyAsString();
            searchResponseTmVo.setTmId(Long.parseLong(tmId));

            // 赋值品牌的名称
            Map<String, Aggregation> tmIdAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
            // Aggregation -- ParsedStringTerms
            ParsedStringTerms tmNameAgg = (ParsedStringTerms) tmIdAggregationMap.get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            // 赋值品牌的logoUrl
            ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms) tmIdAggregationMap.get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);
            // 返回品牌对象
            return searchResponseTmVo;
        }).collect(Collectors.toList());
        // 赋值品牌整个集合数据
        searchResponseVo.setTrademarkList(responseTmVoList);

        // 赋值商品 goodsList
        SearchHits hits = searchResponse.getHits();
        SearchHit[] subHits = hits.getHits();
        // 声明一个商品对象集合
        List<Goods> goodsList = new ArrayList<>();
        if (null!=subHits && subHits.length>0){
            // 循环遍历集合
            for (SearchHit subHit : subHits) {
                // json 字符串
                String sourceAsString = subHit.getSourceAsString();
                // 将json 字符串转化为 goods
                Goods goods = JSON.parseObject(sourceAsString, Goods.class);

                //获取高亮中的title
                if (null != subHit.getHighlightFields().get("title")){
                    //说明title中有数据，又则获取高亮字段
                    Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                    //将Goods中得title进行替换
                    goods.setTitle(title.toString());

                }

                // 将对象添加到集合
                goodsList.add(goods);
            }
        }
        searchResponseVo.setGoodsList(goodsList);

        // 平台属性
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        // 判断集合中是否有数据
        if (!CollectionUtils.isEmpty(buckets)){
            List<SearchResponseAttrVo> responseAttrVoList = buckets.stream().map(bucket -> {
                // 声明一个对象
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                // 赋值属性Id
                searchResponseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // 赋值属性名称
                ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
                String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
                searchResponseAttrVo.setAttrName(attrName);

                // 赋值属性值名称
                ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
                // 属性值可能有多个，循环遍历
                List<? extends Terms.Bucket> valueAggBucketsList = attrValueAgg.getBuckets();
                // 获取到集合中的每个数据
                // Terms.Bucket::getKeyAsString 通过key 来获取value
                List<String> valueList = valueAggBucketsList.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                // 将获取到的属性值放入集合中
                searchResponseAttrVo.setAttrValueList(valueList);

                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            // 将属性，属性值数据放入返回对象
            searchResponseVo.setAttrsList(responseAttrVoList);
        }
        // 赋值总条数
        searchResponseVo.setTotal(hits.totalHits);
        // 返回对象
        return searchResponseVo;
    }

    /**
     * //构建DSL语句:利用JAVA代码实现动态DSL语句
     * @param searchParam
     * @return
     */
    private SearchRequest buildQueryDsl(SearchParam searchParam) {


        //定义查询器{}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //构建QueryBuilder,放入 .query();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();


        //判断输入的查询关键字是否为空,构建查询语句
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            //构建match,放入 .must();
            //.operator(Operator.AND) --> 表示拆分的词语 必须 同时存在才会查询数据
            //.operator(Operator.OR)  --> 表示拆分的词语 不必 同时存在才会查询数据
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            //{query:{bool:{must:[match:{"title":"searchParam.getKeyword()"}]}}}
            boolQueryBuilder.must(title);
        }


        //按照分类Id查询
        if (null != searchParam.getCategory1Id()) {
            //构建term (category1Id) ,放入 .filter();
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());

            //{query:{bool:{filter:[term{"category1Id":"searchParam.getCategory1Id()"}]}}}
            boolQueryBuilder.filter(category1Id);
        }

        if (null != searchParam.getCategory2Id()) {
            //构建term (category2Id) ,放入 .filter();
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());

            //{query:{bool:{filter:[term{"category2Id":"searchParam.getCategory2Id()"}]}}}
            boolQueryBuilder.filter(category2Id);
        }

        if (null != searchParam.getCategory3Id()) {
            //构建term (category3Id) ,放入 .filter();
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());

            //{query:{bool:{filter:[term{"category3Id":"searchParam.getCategory3Id()"}]}}}
            boolQueryBuilder.filter(category3Id);
        }


        //查询品牌，判断用户是否输入了品牌查询条件 有参数 ---> （trademark = 2:华为）
        //获取用户查询的品牌数据
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            //用户输入了品牌查询 通过key获取值  --->  2:华为
            //将value进行分割
            //split[0] = 2  ,  split[1] = "华为"
            String[] split = trademark.split(":");

            //判断数据格式是否正确
            if (null != split && split.length == 2) {
                //构建term (tmId) ,放入 .filter();
                TermQueryBuilder tmId = QueryBuilders.termQuery("tmId", split[0]);

                //{query:{bool:{filter:term{"tmId":"split[0]"}}}}
                boolQueryBuilder.filter(tmId);
            }
        }


        //根据用户的平台属性值，进行查询 ---> 平台属性Id:平台属性值名称:平台属性名 ---> 23:4G:运行内存
        //判断用户是否进行了平台属性值过滤
        String[] props = searchParam.getProps();
        if (null != props && props.length > 0) {
            //循环遍历props
            for (String prop : props) {
                //对当前数据进行分割
                String[] split = prop.split(":");
                //判断数据格式
                if (null != split && split.length == 3) {
                    //如何对平台属性值进行过滤
                    //创建一个又一个 bool 对象
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    //{"bool": {"must": [ {"term": {"attrs.attrId": "split[0]"}}]}}
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                    //{"bool": {"must": [ {"term": {"attrs.attrValue": "split[1]"}}]}}
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue", split[1]));

                    //开始嵌套
                    //{"bool": {"must": [{"nested": {"path": "attrs","query": {"bool": {"must": [{"term": {"attrs.attrValue": "1700-2799"}}]}}}}]}}
                    boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));

                    //整合查询
                    //{"query": {"bool": {"filter": [{"bool": {"must": [{"nested": {"path": "attrs","query": {"bool": {"must": [{"term": {"attrs.attrValue": "1700-2799"}}]}}}}]}}]}}}
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }
        //{query:{bool}}
        searchSourceBuilder.query(boolQueryBuilder);

        //分页设置，高亮查询，排序 等 与 query 并列
        //分页设置
        //计算每页开始的起始条数(分页公式)
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());


        //高亮查询
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        //设置好的高亮对象放入方法中

        searchSourceBuilder.highlighter(highlightBuilder);


        //做排序
        //先获取用户是否点了排序功能 ---> 1:hotScore 2:price
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            // 1:hotScore 2:price
            //对数据进行分割
            String[] split = order.split(":");
            //判断一下格式
            if (null != split && split.length == 2) {
                //声明一个field字段，用它记录按照什么进行排序
                String field = null;
                switch (split[0]) {
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                //设置排序规则
                //{"sort": [{"hotScore": {"order": "desc"}}]}
//                searchSourceBuilder.sort(field, "price".equals(split[1])?SortOrder.ASC:SortOrder.DESC);
                searchSourceBuilder.sort(field, "asc".equals(split[1])?SortOrder.ASC:SortOrder.DESC);
            } else {
                searchSourceBuilder.sort("hotScore", SortOrder.ASC);
            }
        }

        //设置聚合
        //tmIdAgg ---> 普通字段
        //{"aggs": {"tmIdAgg": {"terms": {"field": "tmId"},"aggs": {"tmNameAgg": {"terms": {"field": "tmName"}},"tmLogoUrlAgg": {"terms": {"field": "tmLogoUrl"}}}}}
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));
        //将品牌得agg放入查询器
        searchSourceBuilder.aggregation(termsAggregationBuilder);


        //设置平台属性聚合,并放入查询器
        //nested ---> 内嵌聚合
        //{"aggs": {"attrAgg": {"nested": {"path": "attrs"},"aggs": {"attrIdAgg": {"terms": {"field": "attrs.attrId"},"aggs": {"attrNameAgg": {"terms": {"field": "attrs.attrName"}},"attrValueAgg": {"terms": {"field": "attrs.attrValue"}}}}}}}
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "attrs")
                                        .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")))
                                        );

        //数据结果集进行过滤，查询数据的时候，结果集显示 ---> "id", "defaultImg" ,"title", "price"
        searchSourceBuilder.fetchSource(new String[]{"id", "defaultImg" ,"title", "price"}, null);

        //指定index,type ---> GET /goods/info/_search
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);

        //打印DSL语句
        System.out.println("dsl : " + searchSourceBuilder.toString());

        return searchRequest;
    }
}
