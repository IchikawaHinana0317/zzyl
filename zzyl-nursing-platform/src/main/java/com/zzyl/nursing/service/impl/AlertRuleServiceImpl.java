package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.config.WebSocketServer;
import com.zzyl.nursing.domain.AlertData;
import com.zzyl.nursing.domain.DeviceData;
import com.zzyl.nursing.mapper.DeviceMapper;
import com.zzyl.nursing.service.IAlertDataService;
import com.zzyl.nursing.vo.AlertNotifyVo;
import com.zzyl.system.mapper.SysUserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.AlertRuleMapper;
import com.zzyl.nursing.domain.AlertRule;
import com.zzyl.nursing.service.IAlertRuleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 报警规则Service业务层处理
 *
 * @author hinana
 * @date 2026-08-02
 */
@Service
@Slf4j
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRule> implements IAlertRuleService
{
    @Autowired
    private AlertRuleMapper alertRuleMapper;
    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private IAlertDataService alertDataService;

    @Value("${alert.deviceMaintainerRole}")
    private String deviceMaintainerRole;

    @Value("${alert.managerRole}")
    private String managerRole;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private  SysUserRoleMapper userRoleMapper;


    /**
     * 查询报警规则
     *
     * @param id 报警规则主键
     * @return 报警规则
     */
    @Override
    public AlertRule selectAlertRuleById(Long id)
    {
                return getById(id);
    }

    /**
     * 查询报警规则列表
     *
     * @param alertRule 报警规则
     * @return 报警规则
     */
    @Override
    public List<AlertRule> selectAlertRuleList(AlertRule alertRule)
    {
        return alertRuleMapper.selectAlertRuleList(alertRule);
    }

    /**
     * 新增报警规则
     *
     * @param alertRule 报警规则
     * @return 结果
     */
    @Override
    public int insertAlertRule(AlertRule alertRule)
    {
                alertRule.setCreateTime(DateUtils.getNowDate());
                        return save(alertRule) ? 1 : 0;
    }

    /**
     * 修改报警规则
     *
     * @param alertRule 报警规则
     * @return 结果
     */
    @Override
    public int updateAlertRule(AlertRule alertRule)
    {
                alertRule.setUpdateTime(DateUtils.getNowDate());
                return updateById(alertRule) ? 1 : 0;
    }

    /**
     * 批量删除报警规则
     *
     * @param ids 需要删除的报警规则主键
     * @return 结果
     */
    @Override
    public int deleteAlertRuleByIds(Long[] ids)
    {
                return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除报警规则信息
     *
     * @param id 报警规则主键
     * @return 结果
     */
    @Override
    public int deleteAlertRuleById(Long id)
    {
                return removeById(id) ? 1 : 0;
    }

    /**
     * 定时针对报警规则对设备上报数据进行过滤处理
     */
    @Override
    public void alertFilter() {
        // 1. 查询是否存在启用状态的报警规则
        long count = alertRuleMapper.countAlertRules(1);
        if (count <= 0) {
            return;
        }

        // 2. 获取 Redis Hash 中所有设备的最新上报数据
        List<Object> values = redisTemplate.opsForHash()
                .values(CacheConstants.IOT_DEVICE_LAST_DATA);

        if (CollUtil.isEmpty(values)) {
            return;
        }

        // 3. 将所有设备最新一次上报的数据合并
        List<DeviceData> deviceDatas = new ArrayList<>();
        //遍历每个设备的数据集合
        for (Object value : values) {
            if (value == null || StrUtil.isBlank(value.toString())) {
                continue;
            }

            try {
                List<DeviceData> list =
                        JSONUtil.toList(value.toString(), DeviceData.class);

                if (CollUtil.isNotEmpty(list)) {
                    deviceDatas.addAll(list);
                }
            } catch (Exception e) {
                // 某台设备数据格式异常时，不影响其他设备继续处理
                log.error("解析设备最新上报数据失败，数据内容：{}", value, e);
            }
        }

        if (CollUtil.isEmpty(deviceDatas)) {
            return;
        }

        // 4. 处理每条设备属性数据
        for (DeviceData deviceData : deviceDatas) {
            alertFilter(deviceData);
        }
    }

    /**
     * 处理单条设备属性数据：
     * 查询当前产品下该属性的通用报警规则，
     * 以及当前设备下该属性的专属报警规则，
     * 并逐条进行报警条件判断。
     *
     * @param deviceData 某台设备某个属性的最新上报数据
     */
    private void alertFilter(DeviceData deviceData) {
        // 1.判断当前上报的数据是否超过了1分钟
        LocalDateTime alarmTime = deviceData.getAlarmTime();
        long between = LocalDateTimeUtil.between(alarmTime, LocalDateTime.now(), ChronoUnit.SECONDS);
        if (between > 60) {
            return;
        }
        // 2. 查询当前设备属性数据对应的所有启用规则
        // 包括产品通用规则和当前设备专属规则
        List<AlertRule> alertRules =
                alertRuleMapper.selectMatchedAlertRules(
                        deviceData.getProductKey(),
                        deviceData.getIotId(),
                        deviceData.getFunctionId()
                );

        // 3. 没有匹配的报警规则，结束处理
        if (CollUtil.isEmpty(alertRules)) {
            return;
        }

        // 4. 遍历当前设备属性数据对应的每个规则，针对具体设备属性数据进行报警处理
        for (AlertRule alertRule : alertRules) {
            deviceDataAlarmHandler(alertRule, deviceData);
        }

    }

    /**
     * 判断一条设备属性数据是否触发某条报警规则。
     *
     * 处理流程：
     * 1. 判断上报时间是否处于规则生效时段；
     * 2. 判断属性值是否达到报警阈值；没达到则清除异常次数返回
     * 3. 判断当前规则是否处于沉默周期；在沉默周期则直接返回
     * 4. 统计连续异常次数，判断是否达到持续周期；
     * 5. 根据报警类型查找需要通知的用户；
     * 6. 为所有接收人生成报警数据。
     *
     * @param rule       当前需要匹配的报警规则
     * @param deviceData 某台设备某个属性的一次上报数据
     */
    private void deviceDataAlarmHandler(AlertRule rule, DeviceData deviceData) {

        //1.判断当前设备属性数据的上报时间，是否处于报警规则的生效时段内。
        String[] split = rule.getAlertEffectivePeriod().split("~");

        LocalTime startTime = LocalTime.parse(split[0]); //如00:00:00
        LocalTime endTime = LocalTime.parse(split[1]);  //如23:59:59

        // 取出设备属性数据实际上报时间中的“时分秒”
        LocalTime reportTime = LocalDateTimeUtil
                .of(deviceData.getAlarmTime())
                .toLocalTime();

        //上报时间早于开始时间，或者晚于结束时间，说明当前报警规则尚未生效或已经失效。
        if (reportTime.isBefore(startTime) || reportTime.isAfter(endTime)) {
            return;
        }

        // 当前设备的物联网设备ID
        String iotId = deviceData.getIotId();


         //构造“连续异常次数”的 Redis Key:设备iotId + 属性functionId + 规则id
         //原因：同一台设备有多个属性,同一个属性配置多条报警规则。
        //示例alert:trigger:watch001:HeartRate:12
        String aggCountKey = CacheConstants.ALERT_TRIGGER_COUNT_PREFIX
                        + iotId
                        + ":"
                        + deviceData.getFunctionId()
                        + ":"
                        + rule.getId();

       //2.将设备实际上报的属性值与规则阈值比较。
        // compare结果：= 0：上报值等于规则阈值; > 0：上报值大于规则阈值; < 0：上报值小于规则阈值
        //左边是设备上报值；右边是规则阈值。
        int compare = NumberUtil.compare(Double.valueOf(deviceData.getDataValue()), rule.getValue());

        /*
         * 当前代码支持两种运算符：
         * >=：上报值大于等于阈值时异常
         * < ：上报值小于阈值时异常
         */
        boolean abnormal =
                (">=".equals(rule.getOperator()) && compare >= 0)
                        || ("<".equals(rule.getOperator()) && compare < 0);

        if (abnormal) {
            log.info(
                    "设备属性数据符合报警规则，iotId：{}，functionId：{}，threshold={},real_value：{}",
                    iotId,
                    deviceData.getFunctionId(),
                    rule.getValue(),
                    deviceData.getDataValue()
            );
        } else {
           //当前数据恢复正常时，删除之前累计的连续异常次数。
           // 例如：前两次心率异常，count=2；第三次恢复正常；则连续异常被打断，如果后面再异常，必须重新从1开始统计。
            redisTemplate.delete(aggCountKey);
            return;
        }

        //3.判断当前报警规则是否处于沉默周期。
        //沉默周期用于防止同一设备、同一属性、同一规则 ,在短时间内重复产生相同报警。
        String silentKey =
                CacheConstants.ALERT_SILENT_PREFIX
                        + iotId
                        + ":"
                        + deviceData.getFunctionId()
                        + ":"
                        + rule.getId();

        String silentData =
                redisTemplate.opsForValue().get(silentKey);

        /*
         * Redis中存在沉默标记，
         * 说明该规则刚刚触发过报警，还没结束沉默周期
         * 当前不再重复生成报警数据。
         */
        if (StringUtils.isNotEmpty(silentData)) {
            return;
        }

       //4.统计连续异常次数
        // Redis中不存在记录：说明这是第一次异常，count=1。Redis中已经有记录：在原次数基础上加1。
        String aggData =
                redisTemplate.opsForValue().get(aggCountKey);

        int count = StringUtils.isEmpty(aggData)
                ? 1
                : Integer.parseInt(aggData) + 1;

         //判断连续异常次数是否达到规则设置的持续周期。
        // 例如：duration=3:第1次异常：count=1，继续累计 第2次异常：count=2，继续累计 第3次异常：count=3，正式触发报警
        //没达到持续周期，则更新redis里面的异常次数
        if (ObjectUtil.notEqual(count, rule.getDuration()))
        {
            redisTemplate.opsForValue().set(
                    aggCountKey,
                    String.valueOf(count)
            );
            return;
        }

      //5.已经达到持续周期，准备正式触发报警。

       // 删除连续异常次数，

        redisTemplate.delete(aggCountKey);


        // 写入沉默周期标记。

        redisTemplate.opsForValue().set(
                silentKey,
                "1",
                rule.getAlertSilentPeriod(),
                TimeUnit.MINUTES
        );

        /*
         * 6.根据报警数据类型，查询需要接收报警的用户。
         *
         * userIds中保存业务接收人：
         * 老人异常 → 护理员
         * 设备异常 → 维修工或行政人员
         */
        List<Long> userIds = new ArrayList<>();
        //老人异常数据。
        if (Integer.valueOf(0).equals(rule.getAlertDataType())) {


            /*
             * 随身设备，例如智能手表。
             * 根据老人ID查询对应护理员。
             */
            if (Integer.valueOf(0).equals(deviceData.getLocationType())) {

                userIds =
                        deviceMapper.selectNursingIdsByIotIdWithElder(iotId);

            }
            /*
             * 固定在床位上的设备。LocationType 1+PhysicalLocationType 2
             *
             * 设备 → 床位 → 老人 → 护理员。
             */
            else if (
                    Integer.valueOf(1).equals(deviceData.getLocationType())
                            && Integer.valueOf(2).equals(
                            deviceData.getPhysicalLocationType()
                    )
            ) {
                userIds = deviceMapper.selectNursingIdsByIotIdWithBed(iotId);
            }

        } else {

            /*
             * 设备异常数据。
             *
             * 例如：
             * 设备离线、电量过低、传感器故障。
             *
             * 根据角色名称查询维修工或行政人员。
             */
            userIds = userRoleMapper.selectUserIdByRoleName(deviceMaintainerRole);
        }

        /*
         * 不论是老人异常还是设备异常，
         * 都需要额外通知超级管理员。
         */
        List<Long> managerIds =
                userRoleMapper.selectUserIdByRoleName(managerRole);

        /*
         * 合并业务接收人与超级管理员。
         */
        Collection<Long> allUserIds =
                CollUtil.addAll(userIds, managerIds);

        /*
         * 去重。
         *
         * 某个用户可能既是维修人员，
         * 又拥有超级管理员角色，
         * 防止为同一用户生成两条相同报警数据。
         */
        allUserIds = CollUtil.distinct(allUserIds);

        /*
         * 7.为所有接收人生成报警记录，这些记录都是同类型的记录，只有接收人那里不一样
         * 并批量保存到数据库。
         */
        List<AlertData> alertDataList = insertAlertData(allUserIds, rule, deviceData);
        // websocket推送消息
        webSocketNotity(alertDataList.get(0), rule, allUserIds);

    }

    /**
     * 为所有报警接收人生成报警数据，并批量保存。
     *
     * 同一条设备属性数据触发同一条报警规则后，
     * 可能需要同时通知多个用户。
     *
     * 因此：
     * 每个接收人对应一条 AlertData，
     * 这些 AlertData 的设备、规则、报警原因等内容相同，
     * 主要区别是 userId 不同。
     *
     * @param allUserIds 所有需要接收报警的用户ID
     * @param rule       当前被触发的报警规则
     * @param deviceData 触发报警的设备属性数据
     */
    private List<AlertData> insertAlertData(
            Collection<Long> allUserIds,
            AlertRule rule,
            DeviceData deviceData
    ) {

        /*
         * 一、将设备属性数据拷贝成报警数据。
         *
         * DeviceData中已经包含：
         * iotId、设备名称、产品信息、属性标识、
         * 属性值、上报时间、位置类型等。
         *
         * 这些字段也是报警记录需要保存的信息。
         */
        AlertData alertData =
                BeanUtil.toBean(deviceData, AlertData.class);

        // 保存触发本次报警的规则ID
        alertData.setAlertRuleId(rule.getId());

        /*
         * 二、构造方便用户阅读的报警原因。
         *
         * 例如：
         * 心率<60,持续3个周期就报警
         */
        String alertReason = CharSequenceUtil.format(
                "{}{}{},持续{}个周期就报警",
                rule.getFunctionName(),
                rule.getOperator(),
                rule.getValue(),
                rule.getDuration()
        );

        alertData.setAlertReason(alertReason);

        // 新生成的报警默认为未处理状态
        alertData.setStatus(0);

        /*
         * 设置报警数据类型：
         * 0：老人异常数据
         * 1：设备异常数据
         */
        alertData.setType(rule.getAlertDataType());

        /*
         * 三、为每个接收人复制一份报警数据。
         *
         * 基础报警内容相同，
         * 但每条记录设置不同的userId。
         */
        List<AlertData> list = allUserIds.stream()
                .map(userId -> {

                    /*
                     * 每次都复制一个新对象，
                     * 不能直接反复修改同一个alertData对象，
                     * 否则List中的对象可能全部指向同一个实例。
                     */
                    AlertData dbAlertData =
                            BeanUtil.toBean(alertData, AlertData.class);

                    // 当前这条报警数据对应的接收人
                    dbAlertData.setUserId(userId);

                    /*
                     * 清空ID，让数据库为每条报警记录生成新主键。
                     */
                    dbAlertData.setId(null);

                    return dbAlertData;
                })
                .collect(Collectors.toList());

        /*
         * 四、批量插入报警数据。
         *
         * 一个接收人对应一条报警记录。
         */
        alertDataService.saveBatch(list);

        return list;
    }


    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * websocket推送消息
     * @param alertData
     * @param rule
     * @param allUserIds
     */
    private void webSocketNotity(AlertData alertData, AlertRule rule, Collection<Long> allUserIds) {

        //属性拷贝
        AlertNotifyVo alertNotifyVo = BeanUtil.toBean(alertData, AlertNotifyVo.class);

        alertNotifyVo.setFunctionName(rule.getFunctionName());
        alertNotifyVo.setAlertDataType(alertData.getType());
        alertNotifyVo.setNotifyType(1);
        // 向指定的人推送消息
        webSocketServer.sendMessageToConsumer(alertNotifyVo, allUserIds);

    }









}