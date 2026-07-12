package com.zzyl.nursing.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 从前端接收的健康评估请求DTO
 */
@Data
public class HealthAssessmentDto {

    /**
     * 老人姓名
     */
    @NotBlank(message = "老人姓名不能为空")
    private String elderName;

    /**
     * 身份证号
     */
    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    /**
     * 体检机构
     */
    @NotBlank(message = "体检机构不能为空")
    private String physicalExamInstitution;

    /**
     * 原始体检报告OSS地址
     */
    @NotBlank(message = "体检报告地址不能为空")
    private String physicalReportUrl;

    /**
     * 上传报告后生成的临时任务ID
     */
    @NotBlank(message = "报告解析任务ID不能为空")
    private String taskId;
}