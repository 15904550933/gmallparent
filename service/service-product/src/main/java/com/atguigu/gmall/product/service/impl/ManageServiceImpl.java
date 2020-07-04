package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.xml.internal.bind.v2.TODO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author smy
 * @BelongsProject: gmallparent
 * @BelongsPackage: com.atguigu.gmall.product.service.impl
 * @create 2020-06-09 11:30
 * @Description:
 */
@Service
public class ManageServiceImpl implements ManageService {

    //通常会调用mapper层Autowired
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private RabbitService rabbitService;

    /**
     * 查询所有的一级分类
     *
     * @return
     */
    @Override
    public List<BaseCategory1> getCategory1() {
        //select * from base_category1; 表与实体类mapper名称对应
        return baseCategory1Mapper.selectList(null);
    }

    /**
     * 查询某一级分类下的二级分类
     *
     * @param category1Id
     * @return
     */
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        //select * from base_category2 where category1_id = category1Id;
        QueryWrapper<BaseCategory2> wrapper = new QueryWrapper<>();
        wrapper.eq("category1_id", category1Id);

        return baseCategory2Mapper.selectList(wrapper);

    }

    /**
     * 查询某二级分类下的三级分类
     *
     * @param category2Id
     * @return
     */
    @GmallCache(prefix = "getCategory3:")
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        //select * from base_category2 where category2_id = category2Id;
        QueryWrapper<BaseCategory3> wrapper = new QueryWrapper<>();
        wrapper.eq("category2_id", category2Id);

        return baseCategory3Mapper.selectList(wrapper);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        /*
        select * from base_attr_info where category_id = category1Id and category_leve = 1 or
        select * from base_attr_info where category_id = category2Id and category_leve = 2 or
        select * from base_attr_info where category_id = category3Id and category_leve = 3 or
        category_leve 表示层级关系
        category1Id category_leve = 1
        category2Id category_leve = 2
        category3Id category_leve = 3
        -------------------------------------------------------
        扩展功能 ： 需要根基分类Id 需要得到属性名，最好还能得到属性值得名称
            通过分类Id 能够得到属性名
            base_attr_value 属性值在这
         */

        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

    /**
     * 保存保存或修改属性信息
     *
     * @param baseAttrInfo
     */
    @Transactional
    @Override
    public void saveInfo(BaseAttrInfo baseAttrInfo) {
        if (baseAttrInfo != null) {
            //判断是添加还是修改
            if (baseAttrInfo.getId() != null) {
                //修改
                baseAttrInfoMapper.updateById(baseAttrInfo);
                //删除属性值信息
                QueryWrapper<BaseAttrValue> wrapper = new QueryWrapper<>();
                wrapper.eq("attr_id", baseAttrInfo.getId());
                baseAttrValueMapper.delete(wrapper);
            } else {
                //添加
                baseAttrInfoMapper.insert(baseAttrInfo);
            }
            //获取平台属性值集合
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            //将属性值信息再添加到数据中
            if (attrValueList.size() > 0) {
                for (BaseAttrValue baseAttrValue : attrValueList) {
                    //设置属性值的属性ID
                    baseAttrValue.setAttrId(baseAttrInfo.getId());
                    //循环将数据添加到属性值表中
                    baseAttrValueMapper.insert(baseAttrValue);
                }
            }
        }
    }

    /**
     * 修改平台属性：根据属性ID查询属性信息
     *
     * @param attrId
     * @return
     */
    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        if (attrId != null) {
            //根据属性ID查询属性值信息集合
            QueryWrapper<BaseAttrValue> wrapper = new QueryWrapper<>();
            wrapper.eq("attr_id", attrId);
            List<BaseAttrValue> baseAttrValues = baseAttrValueMapper.selectList(wrapper);
            //查询平台属性信息
            BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
            //将该平台属性值添加到平台属性中
            baseAttrInfo.setAttrValueList(baseAttrValues);
            return baseAttrInfo;
        }
        return null;
    }

    /**
     * 查询所有的销售属性数据
     *
     * @return
     */
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    public IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam, SpuInfo spuInfo) {
        //封装查询条件
        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id", spuInfo.getCategory3Id());
        queryWrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 保存商品数据
     *
     * @param spuInfo
     */
    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        /*添加spuInfo商品表*/
        spuInfoMapper.insert(spuInfo);

        /*添加spuImage 商品图片表*/
        //获取SPU图片集合
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            //循环将商品ID添加到图片对象中插入到数据库
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }

        /*添加spuSaleAttr 销售属性表*/
        //获取销售属性集合
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            //循环将商品ID添加到销售属性对象中插入到数据库
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                /*添加spuSaleAttrValue 销售属性值表*/
                //获取销售属性值集合
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    //循环将商品ID和销售属性值名称添加到销售属性值对象中插入到数据库
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    /**
     * 根据spuId 查询spuImageList
     *
     * @param spuId
     * @return
     */
    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        QueryWrapper<SpuImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id", spuId);
        return spuImageMapper.selectList(queryWrapper);
    }

    /**
     * 根据spuId 查询销售属性集合
     *
     * @param spuId
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        //根据SpuID查询销售属性集合
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    /**
     * 保存数据
     *
     * @param skuInfo
     */
    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
    /*
        skuInfo 库存单元表 --- spuInfo！
        skuImage 库存单元图片表 --- spuImage!
        skuSaleAttrValue sku销售属性值表{sku与销售属性值的中间表} --- skuInfo ，spuSaleAttrValue
        skuAttrValue sku与平台属性值的中间表 --- skuInfo ，baseAttrValue
     */
        //保存sku信息
        skuInfoMapper.insert(skuInfo);
        //获得sku图片集合
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (null != skuImageList && skuImageList.size() > 0) {
            // 循环遍历 将图片信息添加到数据库
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }

        //获取sku的销售属性集合
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        // 调用判断集合方法
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            //循环遍历  将销售属性添加到数据库
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }

        //获取sku的平台属性集合
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            //循环将平台属性添加到数据库中
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }

        //发送一个消息队列,商品上架，发送的内容就是skuId
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuInfo.getId());

    }

    /**
     * SKU分页列表
     *
     * @param pageParam
     * @return
     */
    @Override
    public IPage<SkuInfo> selectPage(Page<SkuInfo> pageParam) {
        QueryWrapper<SkuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        //查询分页列表数据
        IPage<SkuInfo> page = skuInfoMapper.selectPage(pageParam, queryWrapper);
        return page;
    }

    /**
     * 商品上架
     *      表示这个商品可以上架 isSale = 1 并没有真正剪商品发布带es上
     * @param skuId
     */
    @Override
    @Transactional
    public void onSale(Long skuId) {
        // 更改销售状态
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsSale(1);
        skuInfoMapper.updateById(skuInfoUp);

        //发送一个商品上架的信息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuId);

    }

    /**
     * 商品下架
     *
     * @param skuId
     */
    @Override
    @Transactional
    public void cancelSale(Long skuId) {
        // 更改销售状态
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsSale(0);
        skuInfoMapper.updateById(skuInfoUp);

        //发送一个商品下架的信息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuId);

    }


    /**
     * 根据skuId 查询skuInfo
     * @param skuId
     * @return
     */
    @Override
    @GmallCache(prefix = "getSkuInfo:")
    public SkuInfo getSkuInfo(Long skuId) {
//         return getSkuInfoRedisson(skuId);
        return getSkuInfoDB(skuId);
    }

    /**
     * 根据skuId 查询skuInfo  Redisson
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedisson(Long skuId) {
        // 在此获取skuInfo 的时候，先查询缓存，如果缓存中有数据，则查询，没有查询数据库并放入缓存!
        SkuInfo skuInfo = null;
        try {
            // 先判断缓存中是否有数据，查询缓存必须知道缓存的key是什么！
            // 定义缓存的key 商品详情的缓存key=sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            // 根据key 获取缓存中的数据
            // 如果查询一个不存在的数据，那么缓存中应该是一个空对象{这个对象有地址，但是属性Id，price 等没有值}
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            // 存储数据为什么使用String ，存储对象的时候建议使用Hash---{hset(skuKey,字段名,字段名所对应的值); 便于对当前对象中属性修改}
            // 对于商品详情来讲：我们只做显示，并没有修改。所以此处可以使用String 来存储!
            if (skuInfo==null){
                // 从数据库中获取数据，防止缓存击穿做分布式锁
                // 定义分布式锁的key lockKey=sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                // 使用redisson
                RLock lock = redissonClient.getLock(lockKey);
                // 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX2, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                if (res) {
                    try {
                        // 从数据库中获取数据
                        skuInfo = getSkuInfoDB(skuId);
                        if (skuInfo==null){
                            // 为了防止缓存穿透，设置一个空对象放入缓存,这个时间建议不要太长！
                            SkuInfo skuInfo1 = new SkuInfo();
                            // 放入缓存
                            redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            // 返回数据
                            return  skuInfo1;
                        }
                        // 从数据库中获取到了数据，放入缓存
                        redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // 解锁
                        lock.unlock();
                    }
                }else {
                    // 此时的线程并没有获取到分布式锁，应该等待,
                    Thread.sleep(1000);
                    // 等待完成之后，还需要查询数据！
                    return getSkuInfo(skuId);
                }
            }else {
                // 表示缓存中有数据了
                // 弯！稍加严禁一点：
                //            if (skuInfo.getId()==null){ // 这个对象有地址，但是属性Id，price 等没有值！
                //                return null;
                //            }
                // 缓存中有数据，应该直接返回即可！
                return skuInfo; // 情况一：这个对象有地址，但是属性Id，price 等没有值！  情况二：就是既有地址，又有属性值！

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 如何中途发送了异常：数据库挺一下！
        return  getSkuInfoDB(skuId);
    }


    /**
     * 根据skuId 查询skuInfo  Redis
     * a. 先从缓存中获取数据：
     *      true:
     *          return;
     *      false:
     *          1. 从数据库中获取：考虑缓存击穿
     *              1.1 分布式锁：
     *                  获取数据库中的数据，在获取数据的时候。
     *                  防止缓存穿透问题：
     *                      skuInfo == null
     *                          true: new SkuInfo();
     *                          false: 返回数据库中数据，并放入缓存
     *              1.2 没有获取到分布式锁的人：
     *                  睡一会，醒了以后继续获取数据。
     *          2. 获取数据的时候，如果出现了不可逆的错误，让数据库兜里，顶上查询压力。
     *
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedis(Long skuId){
        try {
            //再次获取skuInfo的时候先查询缓存，如果缓存中有数据，则查询，没有查询数据库并放入缓存中
            SkuInfo skuInfo = null;
            //先判断缓存中是否有数据，查询缓存必须知道缓存的key是什么！
            //定义缓存的key 商品详情的缓存key=sku:skuId"info
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //根据key 获取缓存中的数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);

            //如果缓存中是空
            if (skuInfo == null){
                //获取数据库中的数据，放入缓存  分布式锁，为了防止缓存击穿
                //定义分布式锁的key
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                //还需要一个uuid座位锁的值
                String uuid = UUID.randomUUID().toString();
                //上锁
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                if (isExist){
                    System.out.println("获取到锁");
                    //去数据库获取数据，并放入缓存
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo == null){
                        //为了防止缓存穿透，要设置一个空对象放入缓存中，用于返回
                        SkuInfo skuInfo1 = new SkuInfo();
                        //将空对象放入缓存
                        redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        //返回数据
                        return skuInfo1;
                    }
                    //如果不是空，将查询出来的结果放入数据库中
                    redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);

                    /*删除锁，使用lua脚本删除*/
                    //定义lua 脚本
                    String script="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    //构建RedisScript
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    //指定好返回的数据类型
                    redisScript.setResultType(Long.class);
                    //指定好lua脚本
                    redisScript.setScriptText(script);
                    //执行lua脚本  第一个参数是redisScript对象  第二个参数是锁的key 第三个参数是key对应的值
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey),uuid);

                    //返回正常操作
                    return skuInfo;
                }else{
                    //此时的线程并没有获取到分布式锁，应该等待
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //等待完成后 重新查询数据
                    getSkuInfo(skuId);
                }
            }else{
                //不是空 判断是不是空对象
    //            if (skuInfo.getId() == null){
    //                return null;
    //            }
                //缓存中有数据  直接返回缓存中的数据
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //如果中途发生异常，数据库顶上！
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoDB(Long skuId) {
        //根据skuId查询skuinfo信息
        //如果查询不存在数据，返回空对象
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        //如果为空对象，不能调用setSkuImageList方法
        if (null != skuInfo) {
            // 根据skuId 查询图片列表集合
            QueryWrapper<SkuImage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("sku_id", skuId);
            List<SkuImage> skuImageList = skuImageMapper.selectList(queryWrapper);
            skuInfo.setSkuImageList(skuImageList);
        }

        return skuInfo;
    }


    /**
     * 通过三级分类id查询分类信息
     *
     * @param category3Id
     * @return
     */
    @Override
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }

    /**
     * 获取sku价格
     *
     * @param skuId
     * @return
     */
    @Override
    @GmallCache(prefix = "getSkuPrice:")
    public BigDecimal getSkuPrice(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (null != skuInfo) {
            return skuInfo.getPrice();
        }
        return new BigDecimal("0");
    }

    /**
     * 根据spuId，skuId 查询销售属性集合
     *
     * @param skuId
     * @param spuId
     * @return
     */
    @Override
    @GmallCache(prefix = "getSpuSaleAttrListCheckBySku:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    /**
     * 根据spuId 查询map 集合属性
     *
     * @param spuId
     * @return
     */
    @Override
    @GmallCache(prefix = "getSkuValueIdsMap:")
    public Map getSkuValueIdsMap(Long spuId) {
        Map<Object, Object> map = new HashMap<>();
        // key = 125|123 ,value = 37
        //根据spuid查询销售属性值集合
        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
        if (mapList != null && mapList.size() > 0) {
            // 循环遍历
            for (Map skuMap : mapList) {
                // key = 125|123 ,value = 37
                map.put(skuMap.get("value_ids"), skuMap.get("sku_id"));
            }
        }
        return map;
    }


    /**
     * 获取全部分类信息
     * @return 阿里巴巴JSONObject集合对象
     */
    @Override
    @GmallCache(prefix = "getBaseCategoryListIndex:")
    public List<JSONObject> getBaseCategoryList() {
        List<JSONObject> list = new ArrayList<>();
        //先获取所有的分类数据
        //select *from base_category_view
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //按照一级分类Id 进行分组
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //初始化一个index 构建json 字符串 ”index“:1
        int index = 1;
        //获取一级分类数据
        for (Map.Entry<Long, List<BaseCategoryView>> entry : category1Map.entrySet()) {
            //获取一级分类Id
            Long category1Id = entry.getKey();
            //一级分类Id下所有对应集合数据
            List<BaseCategoryView> category2List = entry.getValue();

            //声明一个对象保存一级分类数据 一级分类的Json字符串
            JSONObject category1 = new JSONObject();
            category1.put("index", index);
            category1.put("categoryId", category1Id);
            //由于刚刚按照了一级分类Id 进行分组
            String categoryName = category2List.get(0).getCategory1Name();
            category1.put("categoryName", categoryName);
//            category1.put("categoryChild", ); //二级分类数据

            //变量迭代
            index++;

            //获取二级分类数据
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //创建一个 二级分类对象的集合
            ArrayList<Object> category2Child = new ArrayList<>();
            //分别获取二级分类中的数据
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                //获取二级分类Id
                Long category2Id = entry2.getKey();
                //获取二级分类下的所有数据
                List<BaseCategoryView> category3List = entry2.getValue();

                //声明一个对象保存二级分类数据 二级分类的Json字符串
                JSONObject category2 = new JSONObject();
                category2.put("index", index);
                category2.put("categoryId", category2Id);
                category2.put("categoryName", category3List.get(0).getCategory2Name());
//                category2.put("categoryChild", ); //三级分类数据

                //二级分类名称是多个，所以将二级分类对象添加到集合中
                category2Child.add(category2);


                //处理三级分类数据
                //创建一个 三级分类对象的集合
                List<JSONObject> category3Child = new ArrayList<>();
                category3List.stream().forEach((category3View -> {
                    //创建一个三级分类对象
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId", category3View.getCategory3Id());
                    category3.put("categoryName", category3View.getCategory3Name());

                    //将三级分类数据添加到集合
                    category3Child.add(category3);
                }));
                //将三级分类数据放入二级分类的categoryChild
                category2.put("categoryChild",category3Child);
            }
            //将二级分类数据放入一级分类的categoryChild
            category1.put("categoryChild",category2Child);

            //按照JSON数据接口方式，封装一级分类、二级分类、三级分类
            list.add(category1);
        }
        //封装完成之后将数据返回
        return list;
    }

    /**
     * 通过品牌Id 来查询数据
     * 品牌信息 base_trademark
     * @param tmId
     * @return
     */
    @Override
    public BaseTrademark getBaseTrademarkById(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }

    /**
     * 通过skuId 集合来查询数据 获取平台属性，平台属性值
     * @param skuId
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long skuId) {
        /*
        进行多表关联查询
        base_attr_info  平台属性
        base_attr_value 平台属性值
        sku_attr_value  中间表
         */
        return baseAttrInfoMapper.selectAttrInfoList(skuId);
    }
}
