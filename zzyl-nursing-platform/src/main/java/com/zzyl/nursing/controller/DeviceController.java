package com.zzyl.nursing.controller;

import com.huaweicloud.sdk.iotda.v5.model.ServiceCapability;
import com.zzyl.common.core.domain.R;
import com.zzyl.nursing.dto.DeviceDto;
import com.zzyl.nursing.vo.DevicePropertyVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zzyl.common.annotation.Log;
import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.common.enums.BusinessType;
import com.zzyl.nursing.domain.Device;
import com.zzyl.nursing.service.IDeviceService;
import com.zzyl.common.utils.poi.ExcelUtil;
import com.zzyl.common.core.page.TableDataInfo;

/**
 * 设备Controller
 *
 * @author hinana
 * @date 2026-07-31
 */
@RestController
@RequestMapping("/nursing/device")
@Api(tags = "设备相关接口")
public class DeviceController extends BaseController
{
    @Autowired
    private IDeviceService deviceService;

    /**
    * 查询设备列表
    */
    @PreAuthorize("@ss.hasPermi('nursing:device:list')")
    @GetMapping("/list")
    @ApiOperation("查询设备列表")
    public TableDataInfo<List<Device>> list(@ApiParam(value = "设备查询条件") Device device)
    {
        startPage();
        List<Device> list = deviceService.selectDeviceList(device);
        return getDataTable(list);
    }

    /**
     * 同步华为云所有产品
     *
     */
    @PostMapping("/syncProductList")
    @ApiOperation(value = "从物联网平台同步产品列表")
    public AjaxResult syncProductList() {
        deviceService.syncProductList();
        return success();
    }

    /**
     *
     *获得产品列表
     */
    @GetMapping("/allProduct")
    @ApiOperation(value = "查询所有产品列表")
    public AjaxResult allProduct() {
        return success(deviceService.allProduct());
    }

    /**
     *注册设备
     */
    @PostMapping("/register")
    @ApiOperation(value = "注册设备")
    public AjaxResult registerDevice(@RequestBody DeviceDto deviceDto) {
        deviceService.registerDevice(deviceDto);
        return success();
    }

    /**
     * 获取设备详细信息
     */
    @GetMapping("/{iotId}")
    @ApiOperation("获取设备详细信息")
    public AjaxResult getInfo(@PathVariable("iotId") String iotId) {
        return success(deviceService.queryDeviceDetail(iotId));
    }

    /**
     * 查询设备上报数据
     */
    @GetMapping("/queryServiceProperties/{iotId}")
    @ApiOperation("查询设备上报数据")
    public AjaxResult queryServiceProperties(@PathVariable String iotId) {

        List<DevicePropertyVo> list =
                deviceService.queryServiceProperties(iotId);

        return success(list);
    }


    /**
     * 修改设备
     */
    @PutMapping
    @ApiOperation("修改设备")
    public AjaxResult updateDevice(@RequestBody DeviceDto deviceDto)
    {
        deviceService.updateDevice(deviceDto);
        return success();
    }

    /**
     * 删除设备
     */
    @ApiOperation("删除设备")
    @DeleteMapping("/{iotId}")
    public AjaxResult deleteDevice(@PathVariable String iotId) {
        deviceService.deleteDevice(iotId);
        return success();
    }

    /**
     * 查询产品服务与服务对应的属性
     * @param productKey
     * @return
     */
    @GetMapping("/queryProduct/{productKey}")
    @ApiOperation(value = "查询产品详情")
    public AjaxResult queryProduct(@PathVariable String productKey) {
        List<ServiceCapability> capabilities = deviceService.queryProduct(productKey);
        return success(capabilities);
    }












}