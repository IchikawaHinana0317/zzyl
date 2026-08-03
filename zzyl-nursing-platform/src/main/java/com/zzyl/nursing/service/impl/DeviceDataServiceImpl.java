package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.*;

import cn.hutool.core.bean.BeanUtil;import cn.hutool.core.collection.CollUtil;import cn.hutool.core.date.LocalDateTimeUtil;import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.nursing.domain.Device;import com.zzyl.nursing.dto.DeviceDataDto;import com.zzyl.nursing.mapper.DeviceMapper;import com.zzyl.nursing.utils.DateTimeZoneConverter;import com.zzyl.nursing.vo.iot.IotMsgNotifyData;
import com.zzyl.nursing.vo.iot.IotMsgService;
import lombok.extern.slf4j.Slf4j;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.DeviceDataMapper;
import com.zzyl.nursing.domain.DeviceData;
import com.zzyl.nursing.service.IDeviceDataService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 设备数据Service业务层处理
 *
 * @author hinana
 * @date 2026-08-01
 */
@Service
@Slf4j
public class DeviceDataServiceImpl extends ServiceImpl<DeviceDataMapper, DeviceData> implements IDeviceDataService
{
    @Autowired
    private DeviceDataMapper deviceDataMapper;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    /**
     * 查询设备数据
     *
     * @param id 设备数据主键
     * @return 设备数据
     */
    @Override
    public DeviceData selectDeviceDataById(Long id)
    {
                return getById(id);
    }



    /**
     * 新增设备数据
     *
     * @param deviceData 设备数据
     * @return 结果
     */
    @Override
    public int insertDeviceData(DeviceData deviceData)
    {
                deviceData.setCreateTime(DateUtils.getNowDate());
                        return save(deviceData) ? 1 : 0;
    }

    /**
     * 修改设备数据
     *
     * @param deviceData 设备数据
     * @return 结果
     */
    @Override
    public int updateDeviceData(DeviceData deviceData)
    {
                deviceData.setUpdateTime(DateUtils.getNowDate());
                return updateById(deviceData) ? 1 : 0;
    }

    /**
     * 批量删除设备数据
     *
     * @param ids 需要删除的设备数据主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataByIds(Long[] ids)
    {
                return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除设备数据信息
     *
     * @param id 设备数据主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataById(Long id)
    {
                return removeById(id) ? 1 : 0;
    }




    /**
     * 保存设备数据
     * @param iotMsgNotifyData
     */
    @Override
    public void batchInsertDeviceData(IotMsgNotifyData iotMsgNotifyData) {
        String iotId = iotMsgNotifyData.getHeader().getDeviceId();

        // 1. 查询设备信息
        Device device = deviceMapper.selectDeviceByIoTId(iotId);
        if (ObjectUtil.isEmpty(device)) {
            log.error("设备不存在，iotId：{}", iotId);
            return;
        }

        // 2. 获取本次上报的所有服务
        List<IotMsgService> services = iotMsgNotifyData.getBody().getServices();
        if (CollUtil.isEmpty(services)) {
            log.info("设备上报的服务为空，iotId：{}", iotId);
            return;
        }

        // 保存这台设备本次上报中，所有服务的所有属性
        List<DeviceData> allDeviceDataList = new ArrayList<>();

        // 3. 遍历所有服务
        services.forEach(service -> {
            Map<String, Object> properties = service.getProperties();

            if (CollUtil.isEmpty(properties)) {
                log.info("服务属性为空，iotId：{}", iotId);
                return;
            }

            // 处理当前服务的上报时间
            String eventTimeStr = service.getEventTime();
            LocalDateTime localDateTime = LocalDateTimeUtil.parse(
                    eventTimeStr,
                    "yyyyMMdd'T'HHmmss'Z'"
            );
            LocalDateTime eventTime =
                    DateTimeZoneConverter.utcToShanghai(localDateTime);

            // 4. 一个属性生成一条 DeviceData
            properties.forEach((functionId, value) -> {
                DeviceData deviceData =
                        BeanUtil.toBean(device, DeviceData.class);

                deviceData.setId(null);
                deviceData.setCreateTime(new Date());
                deviceData.setAlarmTime(eventTime);
                deviceData.setFunctionId(functionId);
                deviceData.setDataValue(String.valueOf(value));

                allDeviceDataList.add(deviceData);
            });
        });

        // 5. 没有有效属性数据，不继续保存
        if (CollUtil.isEmpty(allDeviceDataList)) {
            log.info("本次上报没有可保存的属性数据，iotId：{}", iotId);
            return;
        }

        // 6. 所有服务处理结束后，统一保存到数据库
        saveBatch(allDeviceDataList);

        // 7. 使用相同 iotId 覆盖 Redis 中的旧数据
        // 此时保存的是这台设备本次上报中所有服务的全部属性
        redisTemplate.opsForHash().put(
                CacheConstants.IOT_DEVICE_LAST_DATA,
                device.getIotId(),
                JSONUtil.toJsonStr(allDeviceDataList)
        );
    }

    /**
     * 查询设备数据列表
     *
     * @param dto 设备数据
     * @return 设备数据
     */
    @Override
    public List<DeviceData> selectDeviceDataList(DeviceDataDto dto) {
        return deviceDataMapper.selectDeviceDataList(dto);
    }









}