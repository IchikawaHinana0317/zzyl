package com.zzyl.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.zzyl.nursing.domain.NursingPlan;
import org.apache.ibatis.annotations.Param;

/**
 * 护理计划Mapper接口
 * 
 * @author hinana
 * @date 2026-05-24
 */
@Mapper
public interface NursingPlanMapper extends BaseMapper<NursingPlan>
{
    /**
     * 查询护理计划
     * 
     * @param id 护理计划主键
     * @return 护理计划
     */
    public NursingPlan selectNursingPlanById(Long id);

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
     * @param nursingPlan 护理计划
     * @return 结果
     */
    public int insertNursingPlan(NursingPlan nursingPlan);

    /**
     * 修改护理计划
     * 
     * @param nursingPlan 护理计划
     * @return 结果
     */
    public int updateNursingPlan(NursingPlan nursingPlan);

    /**
     * 删除护理计划
     * 
     * @param id 护理计划主键
     * @return 结果
     */
    public int deleteNursingPlanById(Long id);

    /**
     * 批量删除护理计划
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteNursingPlanByIds(Long[] ids);

    /**
     * 查询未被绑定的护理计划
     *  nursing_plan.id 没有出现在 nursing_level.plan_id 中，就表示这个护理计划还没有被护理等级绑定，所以可以在新增护理等级时作为下拉选项。
     * @return 护理计划列表
     */
    List<NursingPlan> getAvailable();

    /**
     * 查询未被绑定的护理计划以及当前护理等级绑定的计划
     *
     * @param levelId 护理等级id
     * @return 护理计划列表
     */
    List<NursingPlan> getAvailableByLevelId(@Param("levelId") Long levelId);


}
