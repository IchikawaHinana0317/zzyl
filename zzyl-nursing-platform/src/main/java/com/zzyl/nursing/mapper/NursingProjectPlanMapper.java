package com.zzyl.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzyl.nursing.vo.NursingProjectPlanVo;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.zzyl.nursing.domain.NursingProjectPlan;
import org.apache.ibatis.annotations.Param;

/**
 * 护理计划和项目关联Mapper接口
 *
 * @author hinana
 * @date 2026-05-25
 */
@Mapper
public interface NursingProjectPlanMapper extends BaseMapper<NursingProjectPlan>
{

    /**
     * 批量插入护理计划和项目关联
     *
     * @param projectPlans 与某一计划相关的所有护理计划项目列表
     * @return
     */
    int batchInsert(@Param("list") List<NursingProjectPlan> projectPlans);

    /**
     * 根据计划id查询项目计划
     * @param id
     * @return 与计划关联的护理项目计划列表
     */
    List<NursingProjectPlanVo> selectByPlanId(Long id);


    /**
     * 删除某一计划关联的护理项目计划
     * @param planId
     * @return
     */
    int deleteByPlanId(Long planId);

    /**
     * 查询护理计划和项目关联
     *
     * @param id 护理计划和项目关联主键
     * @return 护理计划和项目关联
     */
    public NursingProjectPlan selectNursingProjectPlanById(Long id);

    /**
     * 查询护理计划和项目关联列表
     *
     * @param nursingProjectPlan 护理计划和项目关联
     * @return 护理计划和项目关联集合
     */
    public List<NursingProjectPlan> selectNursingProjectPlanList(NursingProjectPlan nursingProjectPlan);

    /**
     * 新增护理计划和项目关联
     *
     * @param nursingProjectPlan 护理计划和项目关联
     * @return 结果
     */
    public int insertNursingProjectPlan(NursingProjectPlan nursingProjectPlan);

    /**
     * 修改护理计划和项目关联
     *
     * @param nursingProjectPlan 护理计划和项目关联
     * @return 结果
     */
    public int updateNursingProjectPlan(NursingProjectPlan nursingProjectPlan);

    /**
     * 删除护理计划和项目关联
     *
     * @param id 护理计划和项目关联主键
     * @return 结果
     */
    public int deleteNursingProjectPlanById(Long id);

    /**
     * 批量删除护理计划和项目关联
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteNursingProjectPlanByIds(Long[] ids);
}
