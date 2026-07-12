package com.zzyl.nursing.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.zzyl.common.annotation.Excel;
import com.zzyl.common.core.domain.BaseEntity;

/**
 * 健康评估对象 health_assessment
 *
 * @author hinana
 * @date 2026-07-11
 */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @ApiModel("健康评估实体")
        public class HealthAssessment extends BaseEntity
        {
        private static final long serialVersionUID = 1L;

                /** 主键 */
        @ApiModelProperty("主键")
        private Long id;

                /** 老人姓名 */
                @Excel(name = "老人姓名")
        @ApiModelProperty("老人姓名")
        private String elderName;

                /** 身份证号 */
                @Excel(name = "身份证号")
        @ApiModelProperty("身份证号")
        private String idCard;

                /** 出生日期 */
                @JsonFormat(pattern = "yyyy-MM-dd")
                @Excel(name = "出生日期", width = 30, dateFormat = "yyyy-MM-dd")
        @ApiModelProperty("出生日期")
        private LocalDateTime birthDate;

                /** 年龄 */
                @Excel(name = "年龄")
        @ApiModelProperty("年龄")
        private Integer age;

                /** 性别（0：男，1：女） */
                @Excel(name = "性别", readConverterExp = "0=：男，1：女")
        @ApiModelProperty("性别（0：男，1：女）")
        private Integer gender;

                /** 健康评分，范围0-100 */
                @Excel(name = "健康评分，范围0-100")
        @ApiModelProperty("健康评分，范围0-100")
        private BigDecimal healthScore;

                /** 总体风险等级：healthy/caution/risk/danger/severeDanger */
                @Excel(name = "总体风险等级：healthy/caution/risk/danger/severeDanger")
        @ApiModelProperty("总体风险等级：healthy/caution/risk/danger/severeDanger")
        private String riskLevel;

                /** 是否建议入住（0：建议，1：不建议） */
                @Excel(name = "是否建议入住", readConverterExp = "0=：建议，1：不建议")
        @ApiModelProperty("是否建议入住（0：建议，1：不建议）")
        private Integer suggestionForAdmission;

                /** 推荐护理等级 */
                @Excel(name = "推荐护理等级")
        @ApiModelProperty("推荐护理等级")
        private String nursingLevelName;

                /** 入住情况（0：已入住，1：未入住） */
                @Excel(name = "入住情况", readConverterExp = "0=：已入住，1：未入住")
        @ApiModelProperty("入住情况（0：已入住，1：未入住）")
        private Integer admissionStatus;

                /** 总检日期 */
                @JsonFormat(pattern = "yyyy-MM-dd")
                @Excel(name = "总检日期", width = 30, dateFormat = "yyyy-MM-dd")
        @ApiModelProperty("总检日期")
        private LocalDateTime totalCheckDate;

                /** 体检机构 */
                @Excel(name = "体检机构")
        @ApiModelProperty("体检机构")
        private String physicalExamInstitution;

                /** 原始体检报告URL */
                @Excel(name = "原始体检报告URL")
        @ApiModelProperty("原始体检报告URL")
        private String physicalReportUrl;

                /** AI分析报告URL */
                @Excel(name = "AI分析报告URL")
        @ApiModelProperty("AI分析报告URL")
        private String analysisReportUrl;

                /** 评估时间 */
                @JsonFormat(pattern = "yyyy-MM-dd")
                @Excel(name = "评估时间", width = 30, dateFormat = "yyyy-MM-dd")
        @ApiModelProperty("评估时间")
        private LocalDateTime assessmentTime;

                /** AI分析报告总结 */
                @Excel(name = "AI分析报告总结")
        @ApiModelProperty("AI分析报告总结")
        private String reportSummary;

                /** 风险分布JSON */
                @Excel(name = "风险分布JSON")
        @ApiModelProperty("风险分布JSON")
        private String diseaseRisk;

                /** 异常分析JSON，包含severity和evidenceSufficient */
                @Excel(name = "异常分析JSON，包含severity和evidenceSufficient")
        @ApiModelProperty("异常分析JSON，包含severity和evidenceSufficient")
        private String abnormalAnalysis;

                /** 八大系统评分JSON */
                @Excel(name = "八大系统评分JSON")
        @ApiModelProperty("八大系统评分JSON")
        private String systemScore;


        }