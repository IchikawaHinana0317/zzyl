package com.zzyl.nursing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

import com.zzyl.nursing.domain.DeviceData;import com.zzyl.nursing.dto.DeviceDataDto;import com.zzyl.nursing.vo.iot.IotMsgNotifyData;

/**
 * 设备数据Service接口
 * 
 * @author hinana
 * @date 2026-08-01
 */
public interface IDeviceDataService extends IService<DeviceData>
{
    /**
     * 查询设备数据
     * 
     * @param id 设备数据主键
     * @return 设备数据
     */
    public DeviceData selectDeviceDataById(Long id);



    /**
     * 新增设备数据
     * 
     * @param deviceData 设备数据
     * @return 结果
     */
    public int insertDeviceData(DeviceData deviceData);

    /**
     * 修改设备数据
     * 
     * @param deviceData 设备数据
     * @return 结果
     */
    public int updateDeviceData(DeviceData deviceData);

    /**
     * 批量删除设备数据
     * 
     * @param ids 需要删除的设备数据主键集合
     * @return 结果
     */
    public int deleteDeviceDataByIds(Long[] ids);

    /**
     * 删除设备数据信息
     * 
     * @param id 设备数据主键
     * @return 结果
     */
    public int deleteDeviceDataById(Long id);

    /**
     * 保存设备数据信息（可能多个服务）
     * @param iotMsgNotifyData
     */
    void batchInsertDeviceData(IotMsgNotifyData iotMsgNotifyData);


    /**
     * 查询设备数据列表
     *
     * @param dto 设备数据
     * @return 设备数据集合
     */
    List<DeviceData> selectDeviceDataList(DeviceDataDto dto);
}
