package com.zzyl.nursing.service.impl;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.*;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.common.exception.base.BaseException;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.dto.DeviceDto;
import com.zzyl.nursing.utils.DateTimeZoneConverter;
import com.zzyl.nursing.vo.DeviceDetailVo;
import com.zzyl.nursing.vo.DevicePropertyVo;
import com.zzyl.nursing.vo.ProductVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.DeviceMapper;
import com.zzyl.nursing.domain.Device;
import com.zzyl.nursing.service.IDeviceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 设备Service业务层处理
 *
 * @author hinana
 * @date 2026-07-31
 */
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements IDeviceService
{
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private IoTDAClient client;

    /**
     * 查询设备列表
     *
     * @param device 设备
     * @return 设备
     */
    @Override
    public List<Device> selectDeviceList(Device device)
    {
        return deviceMapper.selectDeviceList(device);
    }

    /**
     * 同步华为云的所有产品
     *
     */
    @Override
    public void syncProductList()
    {
        // 请求参数
        ListProductsRequest listProductsRequest = new ListProductsRequest();
        // 设置条数
        listProductsRequest.setLimit(50);
        // 发起请求
        ListProductsResponse response = client.listProducts(listProductsRequest);
        if(response.getHttpStatusCode() != 200) {
            throw new BaseException("物联网接口 - 查询产品，同步失败");
        }
        // 存储到redis
        redisTemplate.opsForValue().set(CacheConstants.IOT_ALL_PRODUCT_LIST, JSONUtil.toJsonStr(response.getProducts()));

    }

    /**
     *获得产品列表
     */
    public List<ProductVo> allProduct()
    {
        // 从redis中查询数据
        String jsonStr = redisTemplate.opsForValue().get(CacheConstants.IOT_ALL_PRODUCT_LIST);
        // 如果数据为空，则返回一个空集合
        if(StringUtils.isEmpty(jsonStr)){
            return Collections.emptyList();
        }
        // 解析数据，并返回
        return JSONUtil.toList(jsonStr, ProductVo.class);
    }

    /**
     * 注册设备
     *
     * @param dto 设备注册参数
     */
    @Override
    public void registerDevice(DeviceDto dto) {

        // 1. 判断设备名称是否已存在
        int count = deviceMapper.countByDeviceName(dto.getDeviceName());
        if (count > 0) {
            throw new BaseException("设备名称已存在，请重新输入");
        }

        // 2. 判断设备标识码是否已存在
        count = deviceMapper.countByNodeId(dto.getNodeId());
        if (count > 0) {
            throw new BaseException("设备标识码已存在，请重新输入");
        }

        // 3. 判断同一位置是否已经绑定相同产品
        count = deviceMapper.countSameProductAtLocation(
                dto.getProductKey(),
                dto.getBindingLocation(),
                dto.getLocationType(),
                dto.getPhysicalLocationType()
        );

        if (count > 0) {
            throw new BaseException("该老人/位置已绑定该产品，请重新选择");
        }

        // 4. 调用华为云 IoT 接口注册设备
        // 生成设备密钥
        String secret = UUID.randomUUID()
                .toString()
                .replace("-", "");
        AddDeviceRequest request = new AddDeviceRequest();

        AddDevice body = new AddDevice();
        body.setProductId(dto.getProductKey());
        body.setDeviceName(dto.getDeviceName());
        body.setNodeId(dto.getNodeId());

        AuthInfo authInfo = new AuthInfo();
        authInfo.setSecret(secret);
        body.setAuthInfo(authInfo);

        request.setBody(body);

        AddDeviceResponse response;

        try {
            response = client.addDevice(request);
        } catch (Exception e) {
            throw new BaseException("物联网接口 - 注册设备，调用失败");
        }

        // 5. 保存本地设备信息
        Device device = BeanUtil.toBean(dto, Device.class);

        device.setSecret(secret);
        device.setIotId(response.getDeviceId());
        device.setCreateTime(DateUtils.getNowDate());


        int rows = deviceMapper.insertDevice(device);

        if (rows <= 0) {
            throw new BaseException("设备本地保存失败");
        }
    }


    @Override
    public DeviceDetailVo queryDeviceDetail(String iotId) {
        // 查询本地设备数据
        Device device = deviceMapper.selectDeviceByIoTId(iotId);

        if(ObjectUtil.isEmpty(device)) {
            return null;
        }
        // 调用华为云接口查询设备详情
        ShowDeviceRequest request = new ShowDeviceRequest();
        request.setDeviceId(iotId);
        ShowDeviceResponse response;
        try {
            response = client.showDevice(request);
        } catch (Exception e) {
            throw new BaseException("物联网接口 - 查询设备详情，调用失败");
        }
        // 属性拷贝
        DeviceDetailVo deviceVo = BeanUtil.toBean(device, DeviceDetailVo.class);
        deviceVo.setDeviceStatus(response.getStatus());
        String activeTimeStr = response.getActiveTime();
        // 日期转换
        if(StringUtils.isNotEmpty(activeTimeStr)) {
            // 把字符串转换为LocalDateTime
            LocalDateTime activeTime = LocalDateTimeUtil.parse(activeTimeStr, DatePattern.UTC_MS_PATTERN);
            // 日期时区转换
            deviceVo.setActiveTime(DateTimeZoneConverter.utcToShanghai(activeTime));
        }

        return deviceVo;
    }


    @Override
    public List<DevicePropertyVo> queryServiceProperties(String iotId) {

        ShowDeviceShadowRequest request = new ShowDeviceShadowRequest();

        request.setDeviceId(iotId);

        ShowDeviceShadowResponse response;

        try {
            response = client.showDeviceShadow(request);
        } catch (Exception e) {
            throw new BaseException(
                    "物联网接口 - 查询设备影子，调用失败"
            );
        }

        if (response.getHttpStatusCode() != 200) {
            throw new BaseException(
                    "物联网接口 - 查询设备影子，调用失败"
            );
        }

        List<DeviceShadowData> shadow = response.getShadow();

        if (CollUtil.isEmpty(shadow)) {
            return Collections.emptyList();
        }
        // 获取上报数据的reported （参考返回的json数据）
        DeviceShadowProperties reported = shadow.get(0).getReported();

        if (reported == null || reported.getProperties() == null) {
            return Collections.emptyList();
        }
        // 把数据转换为JSONObject(map)，方便处理
        JSONObject jsonObject = JSONUtil.parseObj(reported.getProperties());

        // 事件上报时间
        String eventTimeStr = reported.getEventTime();
        // 把字符串转换为LocalDateTime
        LocalDateTime eventTimeLocalDateTime = LocalDateTimeUtil.parse(eventTimeStr, "yyyyMMdd'T'HHmmss'Z'");
        // 时区转换
        LocalDateTime eventTime = DateTimeZoneConverter.utcToShanghai(eventTimeLocalDateTime);

        List<DevicePropertyVo> list = new ArrayList<>();

        jsonObject.forEach((key, value) -> {
            DevicePropertyVo vo = new DevicePropertyVo();
            vo.setFunctionId(key);
            vo.setValue(value);
            vo.setEventTime(eventTime);
            list.add(vo);
        });

        return list;
    }


    /**
     * 修改设备
     *
     * @param deviceDto 设备
     * @return 结果
     */
    @Override
    public int updateDevice(DeviceDto deviceDto)
    {
        // 修改IoT平台上的设备
        UpdateDeviceRequest request = new UpdateDeviceRequest();
        request.setDeviceId(deviceDto.getIotId());
        UpdateDevice body = new UpdateDevice();
        body.setDeviceName(deviceDto.getDeviceName());
        request.setBody(body);
        try {
            client.updateDevice(request);
        } catch (Exception e) {
            throw new BaseException("调用IoT平台修改设备失败");
        }
        // 修改本地存储的设备
        Device device = BeanUtil.toBean(deviceDto, Device.class);
        int flag;
        try {
            flag = deviceMapper.updateDevice(device);
        } catch (Exception e) {
            throw new BaseException("该老人/位置已绑定该类型的设备，请重新选择绑定位置");
        }
        return flag;
    }

    /**
     * 删除设备
     *
     * @param iotId 设备ID
     */
    @Override
    public int deleteDevice(String iotId) {
        // 删除IoTDA平台上的设备
        DeleteDeviceRequest request = new DeleteDeviceRequest();
        request.setDeviceId(iotId);
        try {
            client.deleteDevice(request);
        } catch (Exception e) {
            throw new BaseException("调用IoT平台设备删除失败");
        }
        // 删除本地存储的设备
        return deviceMapper.deleteDeviceByIoTId(iotId);
    }



    /**
     * 查询产品服务与服务对应的属性
     * @param productKey
     * @return
     */
    @Override
   public List<ServiceCapability> queryProduct(String productKey)
    {
        // 参数校验
        if(StringUtils.isEmpty(productKey)) {
            throw new BaseException("请输入正确的参数");
        }
        // 调用华为云IOT平台接口
        ShowProductRequest showProductRequest = new ShowProductRequest();
        showProductRequest.setProductId(productKey);
        ShowProductResponse response;

        try {
            response = client.showProduct(showProductRequest);
        } catch (Exception e) {
            throw new BaseException("查询产品详情失败");
        }
        // 判断是否存在服务数据
        List<ServiceCapability> serviceCapabilities = response.getServiceCapabilities();
        if(CollUtil.isEmpty(serviceCapabilities)) {
            return Collections.emptyList();
        }

        return serviceCapabilities;
    }







}