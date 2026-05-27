package com.zzyl.nursing.service.impl;


import com.zzyl.common.utils.bean.BeanUtils;
import com.zzyl.nursing.domain.NursingProjectPlan;
import com.zzyl.nursing.dto.NursingPlanDto;
import com.zzyl.nursing.mapper.NursingPlanMapper;
import com.zzyl.nursing.domain.NursingPlan;
import com.zzyl.nursing.mapper.NursingProjectPlanMapper;
import com.zzyl.nursing.service.INursingPlanService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.nursing.vo.NursingPlanVo;
import com.zzyl.nursing.vo.NursingProjectPlanVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.NursingPlanMapper;
import com.zzyl.nursing.domain.NursingPlan;
import com.zzyl.nursing.service.INursingPlanService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 护理计划Service业务层处理
 * 
 * @author hinana
 * @date 2026-05-24
 */
@Service
public class NursingPlanServiceImpl extends ServiceImpl<NursingPlanMapper, NursingPlan>  implements INursingPlanService
{
    @Autowired
    private NursingPlanMapper nursingPlanMapper;

    @Autowired
    private NursingProjectPlanMapper nursingProjectPlanMapper;

    /**
     * 根据ID查询护理计划
     *
     * @param id 护理计划主键
     * @return 护理计划
     */
    @Override
    public NursingPlanVo selectNursingPlanById(Long id)
    {
        // 1.根据id查询护理计划
        //通过计划ID查护理计划表，获取除相关联护理项目外的字段
        NursingPlan nursingPlan = nursingPlanMapper.selectNursingPlanById(id);

        // 2.根据护理计划id查询关联的所有护理项目
        List<NursingProjectPlanVo> list = nursingProjectPlanMapper.selectByPlanId(id);

        // 3.封装结果并返回
        NursingPlanVo nursingPlanVo = new NursingPlanVo();
        BeanUtils.copyProperties(nursingPlan, nursingPlanVo);
        nursingPlanVo.setProjectPlans(list);

        return nursingPlanVo;
    }

    /**
     * 查询护理计划列表
     * 
     * @param nursingPlan 护理计划
     * @return 护理计划
     */
    @Override
    public List<NursingPlan> selectNursingPlanList(NursingPlan nursingPlan)
    {
        return nursingPlanMapper.selectNursingPlanList(nursingPlan);
    }



    /**
     * 新增护理计划
     * @param nursingPlanDto 护理计划
     * @return 结果
     */
    @Transactional
    @Override
    public int insertNursingPlan(NursingPlanDto nursingPlanDto)
    {
        // 保存护理计划

        NursingPlan nursingPlan = new NursingPlan();
        // 属性拷贝
        BeanUtils.copyProperties(nursingPlanDto, nursingPlan);
        nursingPlan.setCreateTime(DateUtils.getNowDate());
        //nursingPlanMapper.insert(nursingPlan);
        save(nursingPlan);

        // 批量保存护理项目计划关系
        Long planId = nursingPlan.getId();

        List<NursingProjectPlan> projectPlans = nursingPlanDto.getProjectPlans();
        if (projectPlans != null && !projectPlans.isEmpty()) {
            projectPlans.forEach(item -> item.setPlanId(planId));
            nursingProjectPlanMapper.batchInsert(projectPlans);
        }
        return 1;
    }


    /**
     * 修改护理计划
     * 
     * @param nursingPlanDto 护理计划
     * @return 结果
     */
    @Override
    @Transactional
    public int updateNursingPlan(NursingPlanDto nursingPlanDto) {
        Long planId = nursingPlanDto.getId();

        // 1. 先删除旧关联
        nursingProjectPlanMapper.deleteByPlanId(planId);

        // 2. 如果新关联列表不为空，再重新插入
        List<NursingProjectPlan> projectPlans = nursingPlanDto.getProjectPlans();
        if (projectPlans != null && !projectPlans.isEmpty()) {
            for (NursingProjectPlan item : projectPlans) {
                item.setPlanId(planId);
            }
            nursingProjectPlanMapper.batchInsert(projectPlans);
        }

        // 3. 修改护理计划主表信息
        NursingPlan nursingPlan = new NursingPlan();
        BeanUtils.copyProperties(nursingPlanDto, nursingPlan);
        return updateById(nursingPlan) ? 1 : 0;
    }

    /**
     * 批量删除护理计划
     * 
     * @param ids 需要删除的护理计划主键
     * @return 结果
     */
    @Override
    public int deleteNursingPlanByIds(Long[] ids)
    {
                return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }


    /**
     * 删除护理计划信息
     *
     * @param id 护理计划主键
     * @return 结果
     */
    @Override
    public int deleteNursingPlanById(Long id)
    {

        // 1. 先删除与计划关联的项目计划表
        nursingProjectPlanMapper.deleteByPlanId(id);
        // 2. 删除计划表
        return removeById(id) ? 1 : 0;
    }


    /**
     * 查询未被绑定的护理计划
     *
     * @return 护理计划列表
     */
    @Override
    public List<NursingPlan> getAvailable()
    {
        return nursingPlanMapper.getAvailable();
    }

    /**
     * 查询未被绑定的护理计划以及当前护理等级绑定的计划
     *
     * @param levelId 护理等级id
     * @return 护理计划列表
     */
    @Override
    public List<NursingPlan> getAvailableByLevelId(Long levelId)
    {
        return nursingPlanMapper.getAvailableByLevelId(levelId);
    }


}


