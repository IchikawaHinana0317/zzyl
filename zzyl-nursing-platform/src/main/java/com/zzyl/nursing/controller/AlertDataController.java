package com.zzyl.nursing.controller;

import com.zzyl.common.core.domain.R;
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
import com.zzyl.nursing.domain.AlertData;
import com.zzyl.nursing.service.IAlertDataService;
import com.zzyl.common.utils.poi.ExcelUtil;
import com.zzyl.common.core.page.TableDataInfo;

/**
 * 报警数据Controller
 *
 * @author hinana
 * @date 2026-08-02
 */
@RestController
@RequestMapping("/nursing/AlertData")
@Api(tags = "报警数据相关接口")
public class AlertDataController extends BaseController
{
    @Autowired
    private IAlertDataService alertDataService;

/**
 * 查询报警数据列表
 */
@PreAuthorize("@ss.hasPermi('nursing:AlertData:list')")
@GetMapping("/list")
@ApiOperation("查询报警数据列表")
    public TableDataInfo<List<AlertData>> list(@ApiParam(value = "报警数据查询条件") AlertData alertData)
    {
        startPage();
        List<AlertData> list = alertDataService.selectAlertDataList(alertData);
        return getDataTable(list);
    }

    /**
     * 导出报警数据列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:AlertData:export')")
    @Log(title = "报警数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ApiOperation("导出报警数据列表")
    public void export(HttpServletResponse response, @ApiParam(value = "报警数据查询条件") AlertData alertData)
    {
        List<AlertData> list = alertDataService.selectAlertDataList(alertData);
        ExcelUtil<AlertData> util = new ExcelUtil<AlertData>(AlertData.class);
        util.exportExcel(response, list, "报警数据数据");
    }

    /**
     * 获取报警数据详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:AlertData:query')")
    @GetMapping(value = "/{id}")
    @ApiOperation("获取报警数据详细信息")
    public R<AlertData> getInfo(@ApiParam(value = "报警数据ID", required = true)
                                   @PathVariable("id") Long id)
    {
                return R.ok(alertDataService.selectAlertDataById(id));
    }

    /**
     * 新增报警数据
     */
    @PreAuthorize("@ss.hasPermi('nursing:AlertData:add')")
    @Log(title = "报警数据", businessType = BusinessType.INSERT)
    @PostMapping
    @ApiOperation("新增报警数据")
    public AjaxResult add(@ApiParam(value = "报警数据实体", required = true) @RequestBody AlertData alertData)
    {
        return toAjax(alertDataService.insertAlertData(alertData));
    }

    /**
     * 修改报警数据
     */
    @PreAuthorize("@ss.hasPermi('nursing:AlertData:edit')")
    @Log(title = "报警数据", businessType = BusinessType.UPDATE)
    @PutMapping
    @ApiOperation("修改报警数据")
    public AjaxResult edit(@ApiParam(value = "报警数据实体", required = true)  @RequestBody AlertData alertData)
    {
        return toAjax(alertDataService.updateAlertData(alertData));
    }

    /**
     * 删除报警数据
     */
    @PreAuthorize("@ss.hasPermi('nursing:AlertData:remove')")
    @Log(title = "报警数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @ApiOperation("删除报警数据")
    public AjaxResult remove(@ApiParam(value = "报警数据ID数组", required = true) @PathVariable Long[] ids)
    {
        return toAjax(alertDataService.deleteAlertDataByIds(ids));
    }
}