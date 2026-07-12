package com.zzyl.nursing.service.impl;


import cn.hutool.core.util.ObjectUtil;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.nursing.mapper.NursingLevelMapper;
import com.zzyl.nursing.domain.NursingLevel;
import com.zzyl.nursing.service.INursingLevelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;
import java.util.List;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.nursing.vo.NursingLevelVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.NursingLevelMapper;
import com.zzyl.nursing.domain.NursingLevel;
import com.zzyl.nursing.service.INursingLevelService;
import org.springframework.util.ObjectUtils;

/**
 * 护理等级Service业务层处理
 * 
 * @author hinana
 * @date 2026-05-24
 */
@Slf4j
@Service
public class NursingLevelServiceImpl extends ServiceImpl<NursingLevelMapper, NursingLevel>  implements INursingLevelService
{
    @Autowired
    private NursingLevelMapper nursingLevelMapper;
    @Autowired
    private RedisTemplate<Object,Object> redisTemplate;

    /**
     * 查询护理等级
     * 
     * @param id 护理等级主键
     * @return 护理等级
     */
    @Override
    public NursingLevel selectNursingLevelById(Long id)
    {
               return getById(id);
    }

    /**
     * 查询护理等级列表
     * 
     * @param nursingLevel 护理等级
     * @return 护理等级
     */
    @Override
    public List<NursingLevelVo> selectNursingLevelVoList(NursingLevel nursingLevel)
    {
        return nursingLevelMapper.selectNursingLevelVoList(nursingLevel);
    }

    /**
     * 新增护理等级
     * 
     * @param nursingLevel 护理等级
     * @return 结果
     */
    @Override
    public int insertNursingLevel(NursingLevel nursingLevel)
    {

        int flag = nursingLevelMapper.insertNursingLevel(nursingLevel);
        log.info("因为进行新增操作，删除原本旧缓存！");
        deleteCache();
        return flag;

    }

    /**
     * 修改护理等级
     * 
     * @param nursingLevel 护理等级
     * @return 结果
     */
    @Override
    public int updateNursingLevel(NursingLevel nursingLevel)
    {

        int flag = nursingLevelMapper.updateNursingLevel(nursingLevel);
        log.info("因为进行修改操作，删除原本旧缓存！");
        deleteCache();
        return flag;

    }

    /**
     * 批量删除护理等级
     * 
     * @param ids 需要删除的护理等级主键
     * @return 结果
     */
    @Override
    public int deleteNursingLevelByIds(Long[] ids)
    {
        int flag = nursingLevelMapper.deleteNursingLevelByIds(ids);
        log.info("因为进行批量删除操作，删除原本旧缓存！");
        deleteCache();
        return flag;

    }

    /**
     * 删除护理等级信息
     *
     * @param id 护理等级主键
     * @return 结果
     */
    @Override
    public int deleteNursingLevelById(Long id)
    {
        int flag = nursingLevelMapper.deleteNursingLevelById(id);
        log.info("因为进行删除操作，删除原本旧缓存！");
        deleteCache();
        return flag;

    }


    /**
     * 查询所有护理等级
     *
     * @return 护理等级列表
     */
    @Override
    public List<NursingLevel> listAll() {
        // 从缓存中获取
        List<NursingLevel> list = (List<NursingLevel>) redisTemplate.opsForValue().get(CacheConstants.NURSING_LEVEL_ALL_KEY);
        // 缓存中有数据，直接返回
        if(ObjectUtil.isNotEmpty(list)){
            log.info("查询缓存命中成功!");
            return list;
        }
        // 缓存中没有数据，从数据库中查询
        list = nursingLevelMapper.listAll();
        // 将数据写入缓存
        log.info("缓存中没有数据，重新从数据库加载到缓存！");
        redisTemplate.opsForValue().set(CacheConstants.NURSING_LEVEL_ALL_KEY, list);
        return list;
    }

    /**
     * 删除缓存
     */
    private void deleteCache() {
        // 删除缓存
        redisTemplate.delete(CacheConstants.NURSING_LEVEL_ALL_KEY);
    }
}


