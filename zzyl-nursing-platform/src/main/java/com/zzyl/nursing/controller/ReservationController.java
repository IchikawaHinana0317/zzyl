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
import com.zzyl.nursing.domain.Reservation;
import com.zzyl.nursing.service.IReservationService;
import com.zzyl.common.utils.poi.ExcelUtil;
import com.zzyl.common.core.page.TableDataInfo;

/**
 * 预约信息Controller
 *
 * @author hinana
 * @date 2026-07-14
 */
@RestController
@RequestMapping("/nursing/reservation")
@Api(tags = "预约信息相关接口")
public class ReservationController extends BaseController
{
    @Autowired
    private IReservationService reservationService;

/**
 * 查询预约信息列表
 */
@PreAuthorize("@ss.hasPermi('nursing:reservation:list')")
@GetMapping("/list")
@ApiOperation("查询预约信息列表")
    public TableDataInfo<List<Reservation>> list(@ApiParam(value = "预约信息查询条件") Reservation reservation)
    {
        startPage();
        List<Reservation> list = reservationService.selectReservationList(reservation);
        return getDataTable(list);
    }

    /**
     * 导出预约信息列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:reservation:export')")
    @Log(title = "预约信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ApiOperation("导出预约信息列表")
    public void export(HttpServletResponse response, @ApiParam(value = "预约信息查询条件") Reservation reservation)
    {
        List<Reservation> list = reservationService.selectReservationList(reservation);
        ExcelUtil<Reservation> util = new ExcelUtil<Reservation>(Reservation.class);
        util.exportExcel(response, list, "预约信息数据");
    }

    /**
     * 获取预约信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:reservation:query')")
    @GetMapping(value = "/{id}")
    @ApiOperation("获取预约信息详细信息")
    public R<Reservation> getInfo(@ApiParam(value = "预约信息ID", required = true)
                                   @PathVariable("id") Long id)
    {
                return R.ok(reservationService.selectReservationById(id));
    }

    /**
     * 新增预约信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:reservation:add')")
    @Log(title = "预约信息", businessType = BusinessType.INSERT)
    @PostMapping
    @ApiOperation("新增预约信息")
    public AjaxResult add(@ApiParam(value = "预约信息实体", required = true) @RequestBody Reservation reservation)
    {
        return toAjax(reservationService.insertReservation(reservation));
    }

    /**
     * 修改预约信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:reservation:edit')")
    @Log(title = "预约信息", businessType = BusinessType.UPDATE)
    @PutMapping
    @ApiOperation("修改预约信息")
    public AjaxResult edit(@ApiParam(value = "预约信息实体", required = true)  @RequestBody Reservation reservation)
    {
        return toAjax(reservationService.updateReservation(reservation));
    }

    /**
     * 删除预约信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:reservation:remove')")
    @Log(title = "预约信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @ApiOperation("删除预约信息")
    public AjaxResult remove(@ApiParam(value = "预约信息ID数组", required = true) @PathVariable Long[] ids)
    {
        return toAjax(reservationService.deleteReservationByIds(ids));
    }
}