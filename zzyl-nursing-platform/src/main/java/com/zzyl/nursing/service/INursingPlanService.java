package com.zzyl.nursing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import com.zzyl.nursing.domain.NursingPlan;
import com.zzyl.nursing.dto.NursingPlanDto;
import com.zzyl.nursing.vo.NursingPlanVo;

/**
 * 护理计划Service接口
 * 
 * @author hinana
 * @date 2026-05-24
 */
public interface INursingPlanService extends IService<NursingPlan>
{
    /**
     * 根据ID查询护理计划
     * 
     * @param id 护理计划主键
     * @return 护理计划
     */
    public NursingPlanVo selectNursingPlanById(Long id);

    /**
     * 查询护理计划列表
     * 
     * @param nursingPlan 护理计划
     * @return 护理计划集合
     */
    public List<NursingPlan> selectNursingPlanList(NursingPlan nursingPlan);

    /**
     * 新增护理计划
     * 
     * @param nursingPlanDto 护理计划
     * @return 结果
     */
    public int insertNursingPlan(NursingPlanDto nursingPlanDto);

    /**
     * 修改护理计划
     * 
     * @param nursingPlanDto 护理计划
     * @return 结果
     */
    public int updateNursingPlan(NursingPlanDto nursingPlanDto);

    /**
     * 批量删除护理计划
     * 
     * @param ids 需要删除的护理计划主键集合
     * @return 结果
     */
    public int deleteNursingPlanByIds(Long[] ids);

    /**
     * 删除护理计划信息
     * 
     * @param id 护理计划主键
     * @return 结果
     */
    public int deleteNursingPlanById(Long id);

    /**
     * 查询未被绑定的护理计划
     *
     * @return 护理计划列表
     */
    List<NursingPlan> getAvailable();

    /**
     * 查询未被绑定的护理计划以及当前护理等级绑定的计划
     *
     * @param levelId 护理等级id
     * @return 护理计划列表
     */
    List<NursingPlan> getAvailableByLevelId(Long levelId);
}
