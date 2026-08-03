package com.zzyl.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.zzyl.nursing.domain.AlertRule;
import org.apache.ibatis.annotations.Param;

/**
 * 报警规则Mapper接口
 * 
 * @author hinana
 * @date 2026-08-02
 */
@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRule>
{
    /**
     * 查询报警规则
     * 
     * @param id 报警规则主键
     * @return 报警规则
     */
    public AlertRule selectAlertRuleById(Long id);

    /**
     * 查询报警规则列表
     * 
     * @param alertRule 报警规则
     * @return 报警规则集合
     */
    public List<AlertRule> selectAlertRuleList(AlertRule alertRule);

    /**
     * 新增报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    public int insertAlertRule(AlertRule alertRule);

    /**
     * 修改报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    public int updateAlertRule(AlertRule alertRule);

    /**
     * 删除报警规则
     * 
     * @param id 报警规则主键
     * @return 结果
     */
    public int deleteAlertRuleById(Long id);

    /**
     * 批量删除报警规则
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAlertRuleByIds(Long[] ids);

    /**
     * 统计规则数量
     */
    public int countAlertRules(int status);



    /**
     * 处理单条设备属性数据：
     * 查询当前产品下该属性的通用报警规则， iotId = "-1"
     * 以及当前设备下该属性的专属报警规则，iotId = 当前设备iotId
     * 并逐条进行报警条件判断。
     *
     */
    List<AlertRule> selectMatchedAlertRules(
            @Param("productKey") String productKey,
            @Param("iotId") String iotId,
            @Param("functionId") String functionId
    );
}
