package com.zzyl.nursing.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import cn.hutool.core.util.IdcardUtil;
import com.alibaba.fastjson2.JSON;
import com.zzyl.common.ai.AIModelInvoker;
import com.zzyl.common.constant.CacheConstants;
import com.zzyl.common.exception.base.BaseException;
import com.zzyl.common.utils.DateUtils;
import com.zzyl.common.utils.IDCardUtils;
import com.zzyl.common.utils.StringUtils;
import com.zzyl.common.utils.bean.BeanUtils;
import com.zzyl.nursing.dto.HealthAssessmentDto;
import com.zzyl.nursing.utils.HealthAssessmentPdfGenerator;
import com.zzyl.nursing.vo.health.HealthAssessmentResultVo;
import com.zzyl.oss.AliyunOSSOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.zzyl.nursing.mapper.HealthAssessmentMapper;
import com.zzyl.nursing.domain.HealthAssessment;
import com.zzyl.nursing.service.IHealthAssessmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * 健康评估Service业务层处理
 *
 * @author hinana
 * @date 2026-07-11
 */
@Service
@Slf4j
public class HealthAssessmentServiceImpl extends ServiceImpl<HealthAssessmentMapper, HealthAssessment> implements IHealthAssessmentService
{
    @Autowired
    private HealthAssessmentMapper healthAssessmentMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private AIModelInvoker aiModelInvoker;
    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    /**
     * 查询健康评估
     *
     * @param id 健康评估主键
     * @return 健康评估
     */
    @Override
    public HealthAssessment selectHealthAssessmentById(Long id)
    {
                return getById(id);
    }

    /**
     * 查询健康评估列表
     *
     * @param healthAssessment 健康评估
     * @return 健康评估
     */
    @Override
    public List<HealthAssessment> selectHealthAssessmentList(HealthAssessment healthAssessment)
    {
        return healthAssessmentMapper.selectHealthAssessmentList(healthAssessment);
    }



    /**
     * 修改健康评估
     *
     * @param healthAssessment 健康评估
     * @return 结果
     */
    @Override
    public int updateHealthAssessment(HealthAssessment healthAssessment)
    {
                healthAssessment.setUpdateTime(DateUtils.getNowDate());
                return updateById(healthAssessment) ? 1 : 0;
    }

    /**
     * 批量删除健康评估
     *
     * @param ids 需要删除的健康评估主键
     * @return 结果
     */
    @Override
    public int deleteHealthAssessmentByIds(Long[] ids)
    {
                return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除健康评估信息
     *
     * @param id 健康评估主键
     * @return 结果
     */
    @Override
    public int deleteHealthAssessmentById(Long id)
    {
                return removeById(id) ? 1 : 0;
    }



    /**
     * 发起智能健康评估
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertHealthAssessment(
            HealthAssessmentDto dto) {

        String redisKey = CacheConstants.REPORT_KEY + dto.getTaskId();

        log.info(
                "开始执行智能健康评估，taskId={}，idCard={}",
                dto.getTaskId(),
                maskIdCard(dto.getIdCard())
        );

        // 1. 从Redis中取得上传阶段解析出的PDF文本
        String reportContent =
                (String)redisTemplate.opsForValue().get(redisKey);

        if (StringUtils.isBlank(reportContent)) {
            throw new BaseException(
                    "体检报告解析内容不存在或已过期，请重新上传体检报告"
            );
        }

        // 2. 拼装Prompt
        String prompt = buildPrompt(reportContent);

        // 3. 调用大模型
        String aiResult =
                aiModelInvoker.qianfanInvoker(prompt);

        if (StringUtils.isBlank(aiResult)) {
            throw new BaseException("大模型未返回有效结果");
        }


        // 4. 清理可能出现的Markdown代码块
        String pureJson = cleanJsonContent(aiResult);

        // 5. JSON反序列化
        HealthAssessmentResultVo resultVo;

        try {
            resultVo = JSON.parseObject(
                    pureJson,
                    HealthAssessmentResultVo.class
            );
        } catch (Exception e) {
            throw new BaseException(
                    "大模型返回的数据格式不正确，请重新评估"
            );
        }

        // 6. 校验模型返回的必要字段
        validateAiResult(resultVo);

        // 7. 组装数据库实体
        HealthAssessment healthAssessment =
                buildHealthAssessment(dto, resultVo);


        // 8. 写入数据库,并取对应的主键ID
        healthAssessmentMapper.insertHealthAssessment(healthAssessment);
        Long assessmentId = healthAssessment.getId();

        if (assessmentId == null) {
            throw new BaseException("健康评估保存失败");
        }

        // 9. 数据库保存成功后删除Redis临时数据
        Boolean deleted = redisTemplate.delete(redisKey);

        log.info(
                "健康评估生成成功，assessmentId={}，taskId={}，缓存删除结果={}",
                assessmentId,
                dto.getTaskId(),
                deleted
        );

        return assessmentId;
    }

    /**
     * 根据前端基本信息和AI结果组装数据库实体
     */
    private HealthAssessment buildHealthAssessment(
            HealthAssessmentDto dto,
            HealthAssessmentResultVo resultVo) {

        HealthAssessment entity = new HealthAssessment();

        // 前端提交的基础信息
        entity.setElderName(dto.getElderName());
        entity.setIdCard(dto.getIdCard());
        entity.setPhysicalExamInstitution(dto.getPhysicalExamInstitution());
        entity.setPhysicalReportUrl(dto.getPhysicalReportUrl());

        String idCard = dto.getIdCard();
        // 身份证衍生信息
        entity.setBirthDate(
                IDCardUtils.getBirthDateByIdCard(idCard)
        );
        entity.setAge(
                IDCardUtils.getAgeByIdCard(idCard)
        );
        entity.setGender(
                IDCardUtils.getGenderFromIdCard(idCard)
        );

        // AI总体评分
        double healthIndex =
                resultVo.getHealthAssessment()
                        .getHealthIndex();

        entity.setHealthScore(
                BigDecimal.valueOf(healthIndex)
                        .setScale(2, RoundingMode.HALF_UP)
        );

        entity.setRiskLevel(
                resultVo.getHealthAssessment()
                        .getRiskLevel()
        );

        // 评分达到60分建议入住
        entity.setSuggestionForAdmission(
                healthIndex >= 60 ? 0 : 1
        );

        entity.setNursingLevelName(
                getLevelNameByHealthScore(healthIndex)
        );

        // 新生成的评估默认未入住
        entity.setAdmissionStatus(1);

        // 总检日期
        if (StringUtils.isNotBlank(resultVo.getTotalCheckDate())) {
            LocalDateTime totalCheckDate = LocalDate
                    .parse(resultVo.getTotalCheckDate())
                    .atStartOfDay();

            entity.setTotalCheckDate(totalCheckDate);
        }

        entity.setAssessmentTime(LocalDateTime.now());

        entity.setReportSummary(
                resultVo.getSummarize()
        );

        // JSON结构写入数据库文本字段
        entity.setDiseaseRisk(
                JSON.toJSONString(
                        resultVo.getRiskDistribution()
                )
        );

        entity.setAbnormalAnalysis(
                JSON.toJSONString(
                        resultVo.getAbnormalData()
                )
        );

        entity.setSystemScore(
                JSON.toJSONString(
                        resultVo.getSystemScore()
                )
        );

        //分析报告PDF URL地址
        String analysisReportUrl =
                generateAndUploadAnalysisPdf(dto, resultVo);

        entity.setAnalysisReportUrl(analysisReportUrl);

        entity.setCreateTime(DateUtils.getNowDate());
        entity.setUpdateTime(DateUtils.getNowDate());

        return entity;
    }

    /**
     * 生成AI分析报告PDF并上传OSS。
     */
    private String generateAndUploadAnalysisPdf(
            HealthAssessmentDto dto,
            HealthAssessmentResultVo resultVo) {

        try {
            byte[] pdfBytes =
                    HealthAssessmentPdfGenerator.generate(
                            dto.getElderName(),
                            dto.getIdCard(),
                            resultVo
                    );

            String fileName =
                    dto.getElderName()
                            + "-AI健康分析报告-"
                            + System.currentTimeMillis()
                            + ".pdf";

            String url =
                    aliyunOSSOperator.upload(
                            pdfBytes,
                            fileName
                    );

            if (StringUtils.isBlank(url)) {
                throw new BaseException(
                        "AI分析报告上传OSS失败"
                );
            }

            log.info(
                    "AI分析报告PDF生成并上传成功，taskId={}，url={}",
                    dto.getTaskId(),
                    url
            );

            return url;

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "生成或上传AI分析报告PDF失败，taskId={}，idCard={}",
                    dto.getTaskId(),
                    maskIdCard(dto.getIdCard()),
                    e
            );

            throw new BaseException(
                    "生成AI分析报告PDF失败"
            );
        }
    }

    /**
     * 校验大模型返回的必要字段
     */
    private void validateAiResult(
            HealthAssessmentResultVo resultVo) {

        if (resultVo == null) {
            throw new BaseException("大模型返回结果为空");
        }

        if (resultVo.getHealthAssessment() == null) {
            throw new BaseException(
                    "大模型返回结果缺少healthAssessment"
            );
        }

        String riskLevel =
                resultVo.getHealthAssessment()
                        .getRiskLevel();

        if (!isValidRiskLevel(riskLevel)) {
            throw new BaseException(
                    "大模型返回的风险等级不合法："
                            + riskLevel
            );
        }

        validateHealthScore(
                resultVo.getHealthAssessment()
                        .getHealthIndex()
        );
    }

    /**
     * 校验健康评分
     */
    private void validateHealthScore(double score) {
        if (score < 0 || score > 100) {
            throw new BaseException(
                    "健康评分值不合法：" + score
            );
        }
    }

    /**
     * 校验风险等级
     */
    private boolean isValidRiskLevel(
            String riskLevel) {

        return "healthy".equals(riskLevel)
                || "caution".equals(riskLevel)
                || "risk".equals(riskLevel)
                || "danger".equals(riskLevel)
                || "severeDanger".equals(riskLevel);
    }

    /**
     * 根据健康评分推荐护理等级
     */
    private String getLevelNameByHealthScore(
            double healthScore) {

        validateHealthScore(healthScore);

        if (healthScore >= 90) {
            return "四级护理等级";
        }

        if (healthScore >= 80) {
            return "三级护理等级";
        }

        if (healthScore >= 70) {
            return "二级护理等级";
        }

        if (healthScore >= 60) {
            return "一级护理等级";
        }

        return "特级护理等级";
    }

    /**
     * 清理模型偶尔返回的Markdown代码块
     */
    private String cleanJsonContent(String content) {
        String result = content.trim();

        if (result.startsWith("```json")) {
            result = result.substring(7);
        } else if (result.startsWith("```")) {
            result = result.substring(3);
        }

        if (result.endsWith("```")) {
            result = result.substring(
                    0,
                    result.length() - 3
            );
        }

        return result.trim();
    }

    /**
     * 日志中对身份证号脱敏
     */
    private String maskIdCard(String idCard) {
        if (StringUtils.isBlank(idCard)
                || idCard.length() < 8) {
            return "******";
        }

        return idCard.substring(0, 4)
                + "**********"
                + idCard.substring(
                idCard.length() - 4
        );
    }

    /**
     * 构造Prompt
     */
    private String buildPrompt(String reportContent) {
        return "你是一名医疗健康体检报告结构化分析助手。\n"
                + "\n"
                + "你的任务是：\n"
                + "1. 从体检报告中提取异常指标；\n"
                + "2. 对异常指标做谨慎、非诊断性的健康风险解释；\n"
                + "3. 生成结构化健康评估结果；\n"
                + "4. 所有结论只能基于输入报告，不得编造未出现的数据。\n"
                + "\n"
                + "重要规则：\n"
                + "- 不能做疾病确诊，只能使用“可能、提示、建议进一步检查”等表述。\n"
                + "- 若某字段缺少依据，返回null、空数组或“数据不足”，不得猜测。\n"
                + "- 健康评分范围为0到100，分数越高表示整体状态越好。\n"
                + "- 风险等级只能是healthy、caution、risk、danger、severeDanger。\n"
                + "- 风险分布五项之和必须等于100。\n"
                + "- 单项异常severity只能使用上述五个风险等级。\n"
                + "- evidenceSufficient表示当前报告信息是否足以支撑该条判断。\n"
                + "- 只输出合法JSON，不输出Markdown代码块或其他文字。\n"
                + "\n"
                + "【体检报告内容】\n"
                + reportContent
                + "\n\n"
                + "请严格返回以下JSON结构：\n"
                + "{\n"
                + "  \"totalCheckDate\": \"yyyy-MM-dd或null\",\n"
                + "  \"healthAssessment\": {\n"
                + "    \"riskLevel\": \"healthy|caution|risk|danger|severeDanger\",\n"
                + "    \"healthIndex\": 0.00\n"
                + "  },\n"
                + "  \"riskDistribution\": {\n"
                + "    \"healthy\": 0.00,\n"
                + "    \"caution\": 0.00,\n"
                + "    \"risk\": 0.00,\n"
                + "    \"danger\": 0.00,\n"
                + "    \"severeDanger\": 0.00\n"
                + "  },\n"
                + "  \"abnormalData\": [\n"
                + "    {\n"
                + "      \"conclusion\": \"\",\n"
                + "      \"examinationItem\": \"\",\n"
                + "      \"result\": \"\",\n"
                + "      \"referenceValue\": \"\",\n"
                + "      \"unit\": \"\",\n"
                + "      \"interpret\": \"\",\n"
                + "      \"advice\": \"\",\n"
                + "      \"severity\": \"healthy|caution|risk|danger|severeDanger\",\n"
                + "      \"evidenceSufficient\": true\n"
                + "    }\n"
                + "  ],\n"
                + "  \"systemScore\": {\n"
                + "    \"breathingSystem\": null,\n"
                + "    \"digestiveSystem\": null,\n"
                + "    \"endocrineSystem\": null,\n"
                + "    \"immuneSystem\": null,\n"
                + "    \"circulatorySystem\": null,\n"
                + "    \"urinarySystem\": null,\n"
                + "    \"motionSystem\": null,\n"
                + "    \"senseSystem\": null\n"
                + "  },\n"
                + "  \"summarize\": \"100到250字的体检总结\"\n"
                + "}";
    }
}