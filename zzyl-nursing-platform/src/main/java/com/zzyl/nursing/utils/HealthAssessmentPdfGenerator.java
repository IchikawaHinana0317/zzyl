package com.zzyl.nursing.utils;

import com.zzyl.nursing.vo.health.AbnormalDataVo;
import com.zzyl.nursing.vo.health.HealthAssessmentResultVo;
import com.zzyl.nursing.vo.health.RiskDistributionVo;
import com.zzyl.nursing.vo.health.SystemScore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * AI健康评估PDF生成器
 */
public final class HealthAssessmentPdfGenerator {

    private static final float PAGE_MARGIN = 50F;
    private static final float TITLE_FONT_SIZE = 18F;
    private static final float HEADING_FONT_SIZE = 14F;
    private static final float BODY_FONT_SIZE = 10F;
    private static final float LINE_HEIGHT = 18F;

    private HealthAssessmentPdfGenerator() {
    }

    /**
     * 生成AI健康评估PDF。
     *
     * @param elderName 老人姓名
     * @param idCard 身份证号
     * @param resultVo 大模型结构化结果
     * @return PDF字节数组
     */
    public static byte[] generate(
            String elderName,
            String idCard,
            HealthAssessmentResultVo resultVo) {

        if (resultVo == null) {
            throw new IllegalArgumentException("健康评估结果不能为空");
        }

        try (PDDocument document = new PDDocument();
             InputStream fontInputStream =
                     new ClassPathResource("fonts/chinese.ttf").getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDType0Font font = PDType0Font.load(
                    document,
                    fontInputStream,
                    true
            );

            PdfWriter writer = new PdfWriter(document, font);

            writer.writeTitle("AI健康评估分析报告");

            writer.writeLine("老人姓名：" + safeText(elderName));
            writer.writeLine("身份证号：" + maskIdCard(idCard));
            writer.writeLine("总检日期：" + safeText(resultVo.getTotalCheckDate()));

            writer.writeBlankLine();
            writer.writeHeading("一、总体健康评估");

            if (resultVo.getHealthAssessment() != null) {
                writer.writeLine(
                        "健康评分："
                                + resultVo.getHealthAssessment().getHealthIndex()
                );
                writer.writeLine(
                        "风险等级："
                                + translateRiskLevel(
                                resultVo.getHealthAssessment().getRiskLevel()
                        )
                );
            } else {
                writer.writeLine("暂无总体健康评估结果");
            }

            writer.writeBlankLine();
            writer.writeHeading("二、报告总结");
            writer.writeWrappedText(resultVo.getSummarize());

            writer.writeBlankLine();
            writer.writeHeading("三、风险分布");
            writeRiskDistribution(writer, resultVo.getRiskDistribution());

            writer.writeBlankLine();
            writer.writeHeading("四、八大系统评分");
            writeSystemScore(writer, resultVo.getSystemScore());

            writer.writeBlankLine();
            writer.writeHeading("五、异常指标分析");
            writeAbnormalData(writer, resultVo.getAbnormalData());

            writer.writeBlankLine();
            writer.writeHeading("六、重要说明");
            writer.writeWrappedText(
                    "本报告由人工智能根据上传的体检报告生成，"
                            + "仅用于健康信息整理和风险提示，"
                            + "不能替代执业医师的诊断、治疗意见或急诊判断。"
            );

            writer.close();

            document.save(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("生成AI分析PDF失败", e);
        }
    }

    private static void writeRiskDistribution(
            PdfWriter writer,
            RiskDistributionVo risk) throws IOException {

        if (risk == null) {
            writer.writeLine("暂无风险分布数据");
            return;
        }

        writer.writeLine("健康：" + risk.getHealthy() + "%");
        writer.writeLine("提示：" + risk.getCaution() + "%");
        writer.writeLine("风险：" + risk.getRisk() + "%");
        writer.writeLine("危险：" + risk.getDanger() + "%");
        writer.writeLine("严重危险：" + risk.getSevereDanger() + "%");
    }

    private static void writeSystemScore(
            PdfWriter writer,
            SystemScore score) throws IOException {

        if (score == null) {
            writer.writeLine("暂无系统评分数据");
            return;
        }

        writer.writeLine("呼吸系统：" + scoreText(score.getBreathingSystem()));
        writer.writeLine("消化系统：" + scoreText(score.getDigestiveSystem()));
        writer.writeLine("内分泌系统：" + scoreText(score.getEndocrineSystem()));
        writer.writeLine("免疫系统：" + scoreText(score.getImmuneSystem()));
        writer.writeLine("循环系统：" + scoreText(score.getCirculatorySystem()));
        writer.writeLine("泌尿系统：" + scoreText(score.getUrinarySystem()));
        writer.writeLine("运动系统：" + scoreText(score.getMotionSystem()));
        writer.writeLine("感官系统：" + scoreText(score.getSenseSystem()));
    }

    private static void writeAbnormalData(
            PdfWriter writer,
            List<AbnormalDataVo> abnormalData) throws IOException {

        List<AbnormalDataVo> list =
                abnormalData == null
                        ? Collections.emptyList()
                        : abnormalData;

        if (list.isEmpty()) {
            writer.writeLine("未识别到明确异常指标");
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            AbnormalDataVo item = list.get(i);

            writer.writeSubHeading(
                    (i + 1)
                            + ". "
                            + safeText(item.getConclusion())
            );

            writer.writeLine(
                    "检查项目："
                            + safeText(item.getExaminationItem())
            );
            writer.writeLine(
                    "检查结果："
                            + safeText(item.getResult())
            );
            writer.writeLine(
                    "参考值："
                            + safeText(item.getReferenceValue())
            );
            writer.writeLine(
                    "单位："
                            + safeText(item.getUnit())
            );
            writer.writeLine(
                    "风险程度："
                            + translateRiskLevel(item.getSeverity())
            );
            writer.writeLine(
                    "判断依据："
                            + evidenceText(item.getEvidenceSufficient())
            );

            writer.writeWrappedText(
                    "异常解读："
                            + safeText(item.getInterpret())
            );
            writer.writeWrappedText(
                    "健康建议："
                            + safeText(item.getAdvice())
            );

            writer.writeBlankLine();
        }
    }

    private static String scoreText(Integer score) {
        return score == null ? "数据不足" : score + "分";
    }

    private static String evidenceText(Boolean value) {
        if (Boolean.TRUE.equals(value)) {
            return "依据充分";
        }
        if (Boolean.FALSE.equals(value)) {
            return "依据不足";
        }
        return "未评估";
    }

    private static String translateRiskLevel(String riskLevel) {
        if (riskLevel == null) {
            return "未知";
        }

        switch (riskLevel) {
            case "healthy":
                return "健康";
            case "caution":
                return "提示";
            case "risk":
                return "风险";
            case "danger":
                return "危险";
            case "severeDanger":
                return "严重危险";
            default:
                return "未知";
        }
    }

    private static String safeText(String text) {
        return text == null || text.trim().isEmpty()
                ? "暂无"
                : text.trim();
    }

    private static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return "******";
        }

        return idCard.substring(0, 4)
                + "**********"
                + idCard.substring(idCard.length() - 4);
    }

    /**
     * 简单的自动分页、换行写入器。
     */
    private static final class PdfWriter {

        private final PDDocument document;
        private final PDType0Font font;

        private PDPage page;
        private PDPageContentStream contentStream;
        private float y;

        private PdfWriter(
                PDDocument document,
                PDType0Font font) throws IOException {

            this.document = document;
            this.font = font;
            newPage();
        }

        private void writeTitle(String text) throws IOException {
            ensureSpace(LINE_HEIGHT * 2);
            writeText(text, TITLE_FONT_SIZE);
            y -= 8F;
        }

        private void writeHeading(String text) throws IOException {
            ensureSpace(LINE_HEIGHT * 2);
            writeText(text, HEADING_FONT_SIZE);
            y -= 4F;
        }

        private void writeSubHeading(String text) throws IOException {
            ensureSpace(LINE_HEIGHT);
            writeText(text, 11F);
        }

        private void writeLine(String text) throws IOException {
            writeWrappedText(text);
        }

        private void writeBlankLine() throws IOException {
            ensureSpace(LINE_HEIGHT);
            y -= LINE_HEIGHT;
        }

        private void writeWrappedText(String text) throws IOException {
            String value = safeText(text);
            float maxWidth =
                    page.getMediaBox().getWidth()
                            - PAGE_MARGIN * 2;

            StringBuilder currentLine = new StringBuilder();

            for (int i = 0; i < value.length(); i++) {
                char current = value.charAt(i);
                String candidate =
                        currentLine.toString() + current;

                float width =
                        font.getStringWidth(candidate)
                                / 1000
                                * BODY_FONT_SIZE;

                if (width > maxWidth
                        && currentLine.length() > 0) {

                    writeText(
                            currentLine.toString(),
                            BODY_FONT_SIZE
                    );
                    currentLine.setLength(0);
                }

                currentLine.append(current);
            }

            if (currentLine.length() > 0) {
                writeText(
                        currentLine.toString(),
                        BODY_FONT_SIZE
                );
            }
        }

        private void writeText(
                String text,
                float fontSize) throws IOException {

            ensureSpace(LINE_HEIGHT);

            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(PAGE_MARGIN, y);
            contentStream.showText(safeText(text));
            contentStream.endText();

            y -= LINE_HEIGHT;
        }

        private void ensureSpace(float requiredHeight)
                throws IOException {

            if (y - requiredHeight < PAGE_MARGIN) {
                newPage();
            }
        }

        private void newPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }

            page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            contentStream =
                    new PDPageContentStream(document, page);

            y = page.getMediaBox().getHeight()
                    - PAGE_MARGIN;
        }

        private void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }
}