package com.zzyl.nursing.controller;

import com.zzyl.common.constant.CacheConstants;
import com.zzyl.common.core.domain.R;
import com.zzyl.common.utils.PDFUtil;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.nursing.dto.HealthAssessmentDto;
import com.zzyl.oss.AliyunOSSOperator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.zzyl.common.annotation.Log;
import com.zzyl.common.core.controller.BaseController;
import com.zzyl.common.core.domain.AjaxResult;
import com.zzyl.common.enums.BusinessType;
import com.zzyl.nursing.domain.HealthAssessment;
import com.zzyl.nursing.service.IHealthAssessmentService;
import com.zzyl.common.utils.poi.ExcelUtil;
import com.zzyl.common.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 健康评估Controller
 *
 * @author hinana
 * @date 2026-07-11
 */
@Slf4j
@RestController
@RequestMapping("/nursing/healthAssessment")
@Api(tags = "健康评估相关接口")
public class HealthAssessmentController extends BaseController
{
    @Autowired
    private IHealthAssessmentService healthAssessmentService;
    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final long REPORT_CACHE_MINUTES = 30L;


/**
 * 查询健康评估列表
 */
@PreAuthorize("@ss.hasPermi('nursing:healthAssessment:list')")
@GetMapping("/list")
@ApiOperation("查询健康评估列表")
    public TableDataInfo<List<HealthAssessment>> list(@ApiParam(value = "健康评估查询条件") HealthAssessment healthAssessment)
    {
        startPage();
        List<HealthAssessment> list = healthAssessmentService.selectHealthAssessmentList(healthAssessment);
        return getDataTable(list);
    }

    /**
     * 导出健康评估列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:export')")
    @Log(title = "健康评估", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ApiOperation("导出健康评估列表")
    public void export(HttpServletResponse response, @ApiParam(value = "健康评估查询条件") HealthAssessment healthAssessment)
    {
        List<HealthAssessment> list = healthAssessmentService.selectHealthAssessmentList(healthAssessment);
        ExcelUtil<HealthAssessment> util = new ExcelUtil<HealthAssessment>(HealthAssessment.class);
        util.exportExcel(response, list, "健康评估数据");
    }

    /**
     * 获取健康评估详细信息
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:query')")
    @GetMapping(value = "/{id}")
    @ApiOperation("获取健康评估详细信息")
    public R<HealthAssessment> getInfo(@ApiParam(value = "健康评估ID", required = true)
                                   @PathVariable("id") Long id)
    {
                return R.ok(healthAssessmentService.selectHealthAssessmentById(id));
    }



    /**
     * 修改健康评估
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:edit')")
    @Log(title = "健康评估", businessType = BusinessType.UPDATE)
    @PutMapping
    @ApiOperation("修改健康评估")
    public AjaxResult edit(@ApiParam(value = "健康评估实体", required = true)  @RequestBody HealthAssessment healthAssessment)
    {
        return toAjax(healthAssessmentService.updateHealthAssessment(healthAssessment));
    }

    /**
     * 删除健康评估
     */
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:remove')")
    @Log(title = "健康评估", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @ApiOperation("删除健康评估")
    public AjaxResult remove(@ApiParam(value = "健康评估ID数组", required = true) @PathVariable Long[] ids)
    {
        return toAjax(healthAssessmentService.deleteHealthAssessmentByIds(ids));
    }




    /**
     * 上传体检报告并缓存解析后的文本。
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("idCardNo") String idCardNo) {


        String originalFilename = file.getOriginalFilename();
        String taskId = UUID.randomUUID().toString().replace("-", "");
        String redisKey = CacheConstants.REPORT_KEY + taskId;

        try {
            log.info(
                    "开始上传体检报告，taskId={}，idCardNo={}，fileName={}，size={}",
                    taskId,
                    idCardNo,
                    originalFilename,
                    file.getSize()
            );

            // 1. 上传原始 PDF 到 OSS
            String url = aliyunOSSOperator.upload(
                    file.getBytes(),
                    originalFilename
            );

            // 2. 从上传文件中提取 PDF 文本
            String reportContent =
                    PDFUtil.pdfToString(file.getInputStream());

            if (StringUtils.isBlank(reportContent)) {
                log.warn(
                        "体检报告未提取到有效文本，taskId={}，fileName={}",
                        taskId,
                        originalFilename
                );
                return AjaxResult.error(
                        "未能从PDF中提取有效文本，请确认文件不是扫描图片型PDF"
                );
            }

            // 3. 临时保存到 Redis，并设置过期时间
            redisTemplate.opsForValue().set(
                    redisKey,
                    reportContent,
                    REPORT_CACHE_MINUTES,
                    TimeUnit.MINUTES
            );



            // 4. 向前端返回上传结果
            AjaxResult result = AjaxResult.success("体检报告上传成功");
            result.put("taskId", taskId);
            result.put("url", url);
            result.put("fileName", originalFilename);
            result.put("originalFilename", originalFilename);
            result.put("expireMinutes", REPORT_CACHE_MINUTES);

            return result;

        } catch (Exception e) {
            log.error(
                    "体检报告上传或解析失败，taskId={}，idCardNo={}，fileName={}",
                    taskId,
                    idCardNo,
                    originalFilename,
                    e
            );

            return AjaxResult.error("体检报告处理失败：" + e.getMessage());
        }
    }




    /**
     * 新增健康评估
     */
    @ApiOperation("新增健康评估")
    @PreAuthorize("@ss.hasPermi('nursing:healthAssessment:add')")
    @Log(title = "健康评估", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody @ApiParam("从前端接收的健康评估对象") HealthAssessmentDto dto)
    {
        Long id = healthAssessmentService.insertHealthAssessment(dto);
        return success(id);
    }
}