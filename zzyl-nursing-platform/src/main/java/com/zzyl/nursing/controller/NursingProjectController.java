package com.zzyl.nursing.controller;

import com.zzyl.common.annotation.Log;
import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.common.core.page.TableDataInfo;
import com.zzyl.common.enums.BusinessType;
import com.zzyl.common.utils.poi.ExcelUtil;
import com.zzyl.nursing.domain.NursingProject;
import com.zzyl.nursing.service.INursingProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 护理项目Controller
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
@RestController
@RequestMapping("/nursing/project")
public class NursingProjectController extends BaseController
{
    @Autowired
    private INursingProjectService nursingProjectService;

    /**
     * 查询护理项目列表
     */
    // 权限校验：需具备护理项目管理列表权限
    @PreAuthorize("@ss.hasPermi('nursing:project:list')")
    // 映射GET请求到/list路径
    @GetMapping("/list")
    public TableDataInfo list(NursingProject nursingProject)
    {
        // 开启分页功能
        startPage();
        // 调用服务层查询护理项目列表
        List<NursingProject> list = nursingProjectService.selectNursingProjectList(nursingProject);
        // 返回分页数据表格
        return getDataTable(list);
    }

    /**
     * 导出护理项目列表
     */
    // 权限校验：需具备护理项目管理导出权限
    @PreAuthorize("@ss.hasPermi('nursing:project:export')")
    // 记录操作日志：业务类型为导出
    @Log(title = "护理项目", businessType = BusinessType.EXPORT)
    // 映射POST请求到/export路径
    @PostMapping("/export")
    public void export(HttpServletResponse response, NursingProject nursingProject)
    {
        // 查询需要导出的护理项目数据
        List<NursingProject> list = nursingProjectService.selectNursingProjectList(nursingProject);
        // 创建Excel工具类实例
        ExcelUtil<NursingProject> util = new ExcelUtil<NursingProject>(NursingProject.class);
        // 执行Excel导出操作
        util.exportExcel(response, list, "护理项目数据");
    }

    /**
     * 获取护理项目详细信息
     */
    // 权限校验：需具备护理项目管理查询权限
    @PreAuthorize("@ss.hasPermi('nursing:project:query')")
    // 映射GET请求到/{id}路径
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        // 调用服务层根据ID查询护理项目详情
        return success(nursingProjectService.selectNursingProjectById(id));
    }

    /**
     * 新增护理项目
     */
    // 权限校验：需具备护理项目管理新增权限
    @PreAuthorize("@ss.hasPermi('nursing:project:add')")
    // 记录操作日志：业务类型为新增
    @Log(title = "护理项目", businessType = BusinessType.INSERT)
    // 映射POST请求到根路径
    @PostMapping
    public AjaxResult add(@RequestBody NursingProject nursingProject)
    {
        // 调用服务层插入护理项目数据
        return toAjax(nursingProjectService.insertNursingProject(nursingProject));
    }

    /**
     * 修改护理项目
     */
    @PreAuthorize("@ss.hasPermi('nursing:project:edit')")
    @Log(title = "护理项目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody NursingProject nursingProject)
    {
        return toAjax(nursingProjectService.updateNursingProject(nursingProject));
    }

    /**
     * 删除护理项目
     */
    // 权限校验：需具备护理项目管理删除权限
    @PreAuthorize("@ss.hasPermi('nursing:project:remove')")
    // 记录操作日志：业务类型为删除
    @Log(title = "护理项目", businessType = BusinessType.DELETE)
    // 映射DELETE请求到/{ids}路径
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        // 调用服务层批量删除护理项目
        return toAjax(nursingProjectService.deleteNursingProjectByIds(ids));
    }
}
