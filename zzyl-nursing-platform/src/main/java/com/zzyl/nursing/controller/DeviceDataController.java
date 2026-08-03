package com.zzyl.nursing.controller;

import com.zzyl.common.core.domain.R;
import com.zzyl.nursing.dto.DeviceDataDto;
import com.zzyl.nursing.service.IRoomService;
import com.zzyl.nursing.vo.RoomVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;

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
import com.zzyl.nursing.domain.DeviceData;
import com.zzyl.nursing.service.IDeviceDataService;
import com.zzyl.common.core.page.TableDataInfo;

/**
 * 设备数据Controller
 *
 * @author hinana
 * @date 2026-08-01
 */
@RestController
@RequestMapping("/nursing/data")
@Api(tags = "设备数据相关接口")
public class DeviceDataController extends BaseController
{
    @Autowired
    private IDeviceDataService deviceDataService;
    @Autowired
    private IRoomService roomService;


    /*
    @PreAuthorize("@ss.hasPermi('nursing:data:list')")
    @GetMapping("/list")
    @ApiOperation("查询设备数据列表")
    public TableDataInfo<List<DeviceData>> list(@ApiParam(value = "设备数据查询条件") DeviceData deviceData)
    {
        startPage();
        List<DeviceData> list = deviceDataService.selectDeviceDataList(deviceData);
        return getDataTable(list);
    }
    */



    /**
     * 获取设备数据详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:data:query')")
    @GetMapping(value = "/{id}")
    @ApiOperation("获取设备数据详细信息")
    public R<DeviceData> getInfo(@ApiParam(value = "设备数据ID", required = true)
                                   @PathVariable("id") Long id)
    {
                return R.ok(deviceDataService.selectDeviceDataById(id));
    }

    /**
     * 新增设备数据
     */
    @PreAuthorize("@ss.hasPermi('nursing:data:add')")
    @Log(title = "设备数据", businessType = BusinessType.INSERT)
    @PostMapping
    @ApiOperation("新增设备数据")
    public AjaxResult add(@ApiParam(value = "设备数据实体", required = true) @RequestBody DeviceData deviceData)
    {
        return toAjax(deviceDataService.insertDeviceData(deviceData));
    }

    /**
     * 修改设备数据
     */
    @PreAuthorize("@ss.hasPermi('nursing:data:edit')")
    @Log(title = "设备数据", businessType = BusinessType.UPDATE)
    @PutMapping
    @ApiOperation("修改设备数据")
    public AjaxResult edit(@ApiParam(value = "设备数据实体", required = true)  @RequestBody DeviceData deviceData)
    {
        return toAjax(deviceDataService.updateDeviceData(deviceData));
    }

    /**
     * 删除设备数据
     */
    @PreAuthorize("@ss.hasPermi('nursing:data:remove')")
    @Log(title = "设备数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @ApiOperation("删除设备数据")
    public AjaxResult remove(@ApiParam(value = "设备数据ID数组", required = true) @PathVariable Long[] ids)
    {
        return toAjax(deviceDataService.deleteDeviceDataByIds(ids));
    }

    /**
     * 查询设备数据列表
     */
    @GetMapping("/list")
    @ApiOperation("查询设备数据列表")
    public TableDataInfo<List<DeviceData>> list(DeviceDataDto dto)
    {
        startPage();
        List<DeviceData> list = deviceDataService.selectDeviceDataList(dto);
        return getDataTable(list);
    }









}