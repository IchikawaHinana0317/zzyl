package com.zzyl.nursing.service.impl;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.nursing.mapper.NursingProjectMapper;
import com.zzyl.nursing.domain.NursingProject;
import com.zzyl.nursing.service.INursingProjectService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;
import java.util.List;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.nursing.vo.NursingProjectVo;
import com.zzyl.nursing.vo.RoomVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.NursingProjectMapper;
import com.zzyl.nursing.domain.NursingProject;
import com.zzyl.nursing.service.INursingProjectService;

/**
 * 护理项目Service业务层处理
 * 
 * @author hinana
 * @date 2026-05-24
 */
@Service
@Slf4j
public class NursingProjectServiceImpl extends ServiceImpl<NursingProjectMapper, NursingProject>  implements INursingProjectService
{
    @Autowired
    private NursingProjectMapper nursingProjectMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 查询护理项目
     * 
     * @param id 护理项目主键
     * @return 护理项目
     */
    @Override
    public NursingProject selectNursingProjectById(Long id)
    {
               return getById(id);
    }

    /**
     * 查询护理项目列表
     * 
     * @param nursingProject 护理项目
     * @return 护理项目
     */
    @Override
    public List<NursingProject> selectNursingProjectList(NursingProject nursingProject)
    {
        return nursingProjectMapper.selectNursingProjectList(nursingProject);
    }

    /**
     * 新增护理项目
     * 
     * @param nursingProject 护理项目
     * @return 结果
     */
    @Override
    public int insertNursingProject(NursingProject nursingProject)
    {
        int flag = nursingProjectMapper.insertNursingProject(nursingProject);
        log.info("因为进行新增操作，删除原本旧缓存！");
        deleteCache();
        return flag;
    }

    /**
     * 修改护理项目
     * 
     * @param nursingProject 护理项目
     * @return 结果
     */
    @Override
    public int updateNursingProject(NursingProject nursingProject)
    {

        int flag = nursingProjectMapper.updateNursingProject(nursingProject);
        log.info("因为进行修改操作，删除原本旧缓存！");
        deleteCache();
        return flag;
    }

    /**
     * 批量删除护理项目
     * 
     * @param ids 需要删除的护理项目主键
     * @return 结果
     */
    @Override
    public int deleteNursingProjectByIds(Long[] ids)
    {
        int flag =nursingProjectMapper.deleteNursingProjectByIds(ids);
        log.info("因为进行批量删除操作，删除原本旧缓存！");
        deleteCache();
        return flag;
    }

    /**
     * 删除护理项目信息
     *
     * @param id 护理项目主键
     * @return 结果
     */
    @Override
    public int deleteNursingProjectById(Long id)
    {
        int flag = nursingProjectMapper.deleteNursingProjectById(id);
        log.info("因为进行删除操作，删除原本旧缓存！");
        deleteCache();
        return flag;
    }

    /**
     * 查询所有护理项目名称与ID
     *
     * @return 结果
     */
    @Override
    public List<NursingProjectVo> getAll() {
        //查询缓存
        List<NursingProjectVo> nursingProjectVoList=(List<NursingProjectVo>) redisTemplate.opsForValue().get(CacheConstants.NURSING_PROJECT_ALL_KEY);
        //如果有缓存，直接用
        if(ObjectUtil.isNotEmpty(nursingProjectVoList))
        {
            log.info("查询缓存命中成功!");
            return nursingProjectVoList;
        }
        //如果没有，则查数据库
        nursingProjectVoList = nursingProjectMapper.getAll();
        log.info("缓存中没有数据，重新从数据库加载到缓存！");
        redisTemplate.opsForValue().set(CacheConstants.NURSING_PROJECT_ALL_KEY,nursingProjectVoList);
        return nursingProjectVoList;
    }

    /**
     * 删除缓存
     */
    private void deleteCache() {
        // 删除缓存
        redisTemplate.delete(CacheConstants.NURSING_PROJECT_ALL_KEY);
    }



    }
