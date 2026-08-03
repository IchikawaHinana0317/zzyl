package com.zzyl.nursing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

import com.huaweicloud.sdk.iotda.v5.model.ServiceCapability;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.nursing.domain.Device;
import com.zzyl.nursing.dto.DeviceDto;
import com.zzyl.nursing.vo.DeviceDetailVo;
import com.zzyl.nursing.vo.DevicePropertyVo;
import com.zzyl.nursing.vo.ProductVo;

/**
 * 设备Service接口
 * 
 * @author hinana
 * @date 2026-07-31
 */
public interface IDeviceService extends IService<Device>
{


    /**
     * 查询设备列表
     *
     * @param device 设备
     * @return 设备集合
     */
    public List<Device> selectDeviceList(Device device);



    /**
     * 同步华为云的所有产品
     *
     */
    public void syncProductList();


    /**
     *获得产品列表
     */
    public List<ProductVo> allProduct();

    /**
     * 注册设备
     */
    void registerDevice(DeviceDto deviceDto);

    /**
     * 查询设备详情
     * @param iotId
     * @return
     */
    DeviceDetailVo queryDeviceDetail(String iotId);


    /**
     * 查询设备上报数据
     * @param iotId
     * @return
     */
    List<DevicePropertyVo> queryServiceProperties(String iotId);

    /**
     * 修改设备
     * @param deviceDto
     */
    int updateDevice(DeviceDto deviceDto);


    /**
     * 删除设备
     * @param iotId
     */
    int deleteDevice(String iotId);

    /**
     * 查询产品服务与服务对应的属性
     * @param productKey
     * @return
     */
    List<ServiceCapability> queryProduct(String productKey);
}
