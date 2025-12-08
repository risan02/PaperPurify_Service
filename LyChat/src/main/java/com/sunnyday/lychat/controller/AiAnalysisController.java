package com.sunnyday.lychat.controller;


import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.domain.AjaxResult;
import com.sunnyday.lychat.AiFileUtils;
import com.sunnyday.lychat.entity.AiAnalysisResultVo;
import com.sunnyday.lychat.entity.AiDimensionVo;
import com.sunnyday.lychat.entity.QualityDimensionVo;
import com.sunnyday.lychat.service.AiJapanService;
import com.sunnyday.lychat.service.AiTextAnalysisService;
import com.sunnyday.lychat.service.AiPromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Locale;

/**
 * AI内容分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiAnalysisController {

    @Autowired
    private AiJapanService aiJapanService;

    @Autowired
    private AiTextAnalysisService aiTextAnalysisService;

    @Autowired
    private AiPromptService aiPromptService;

    @Autowired
    private MessageSource messageSource;

    @GetMapping(value = "/chatTest")
    public String chatTest() {
        return "ni hao ! hello !";
    }
    /**
     * 内容分析接口
     */
    @PostMapping("/contentAnalyse")
    public AjaxResult contentAnalyse(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            // 1. 从请求头获取语言参数
            String acceptLanguage = request.getHeader("Accept-Language");
            Locale locale = parseLocale(acceptLanguage);
            log.info("当前语言环境: {}", locale);

            // 2. 验证文件
            if (file.isEmpty()) {
                String errorMsg = messageSource.getMessage("error.file.empty", null, locale);
                return AjaxResult.error(errorMsg);
            }

            // 3. 验证文件类型
            String fileName = file.getOriginalFilename();
            String fileType = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            if (!"pdf".equals(fileType) && !"docx".equals(fileType)) {
                String errorMsg = messageSource.getMessage("error.file.type", null, locale);
                return AjaxResult.error(errorMsg);
            }

            // 4. 验证文件大小 (最大10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                String errorMsg = messageSource.getMessage("error.file.size", null, locale);
                return AjaxResult.error(errorMsg);
            }

            // 5. 读取文件内容
            String fileContent = AiFileUtils.readFileContent(file);
            
            // 6. 获取对应语言的提示词（作为系统消息）
            String systemPrompt = aiPromptService.getSystemPrompt(locale);
            
            // 7. 构建完整的提示词，明确要求使用指定语言返回
            String languageRequirement = locale.equals(Locale.SIMPLIFIED_CHINESE) 
                    ? "\n\n【重要语言要求】你必须使用中文（简体）返回所有内容：\n" +
                      "- qualityDimensions中每个维度的name字段必须使用中文\n" +
                      "- qualityDimensions中每个维度的evaluation字段（评价说明）必须使用中文\n" +
                      "- recommendations数组中的每个修改建议必须使用中文\n" +
                      "- 所有返回的文本内容都必须使用中文，不得使用日语或其他语言\n"
                    : "\n\n【重要言語要件】あなたは日本語ですべての内容を返す必要があります：\n" +
                      "- qualityDimensionsの各次元のnameフィールドは日本語を使用する必要があります\n" +
                      "- qualityDimensionsの各次元のevaluationフィールド（評価説明）は日本語を使用する必要があります\n" +
                      "- recommendations配列の各修正提案は日本語を使用する必要があります\n" +
                      "- 返されるすべてのテキストコンテンツは日本語を使用する必要があり、中国語やその他の言語を使用してはいけません\n";
            
            // 8. 构建完整的用户消息，包含系统提示词、语言要求和文档内容
            String fullPrompt = systemPrompt + languageRequirement + "\n\n请分析以下文档内容：\n" + fileContent;
            
            // 9. 调用AI服务，使用不带固定系统消息的方法，完全由我们控制提示词
            // 这样确保语言要求被正确传递，不会被固定的日语系统消息覆盖
            String aiResult = aiJapanService.chatWithoutSystemMessage(String.valueOf(System.currentTimeMillis()), fullPrompt);
            log.info("AI分析結果: " + aiResult);
            
            // 8. 将aiResult的json数据转换为AiAnalysisResultVo对象
            AiAnalysisResultVo result = JSON.parseObject(aiResult, AiAnalysisResultVo.class);
            
            // 9. 通过数学公式计算AI痕迹分析维度（6个维度）和AI率（一次性计算，避免重复）
            AiTextAnalysisService.AnalysisResult analysisResult = aiTextAnalysisService.analyzeWithScore(fileContent, locale);
            
            // 10. 填充AI痕迹分析结果
            result.setAiDimensions(analysisResult.getDimensions());
            result.setAiScore(analysisResult.getAiScore());

            return AjaxResult.success(result);
        } catch (Exception e) {
            log.error("分析过程中发生错误", e);
            String acceptLanguage = request.getHeader("Accept-Language");
            Locale locale = parseLocale(acceptLanguage);
            String errorMsg = messageSource.getMessage("error.analysis.failed", new Object[]{e.getMessage()}, locale);
            return AjaxResult.error(errorMsg);
        }
    }

    /**
     * 解析Accept-Language头为Locale对象
     */
    private Locale parseLocale(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return Locale.JAPANESE; // 默认日文 ja_JP
        }
        
        // 解析语言代码：ja-JP 或 zh-CN
        if (acceptLanguage.startsWith("zh")) {
            return Locale.SIMPLIFIED_CHINESE; // zh-CN
        } else if (acceptLanguage.startsWith("ja")) {
            // 确保返回 ja_JP 而不是 ja
            return new Locale("ja", "JP"); // ja_JP
        }
        
        return Locale.JAPANESE; // 默认日文 ja_JP
    }

    /**
     * AI内容分析方法 (模拟实现)
     */
    private AiAnalysisResultVo analyzeContentMock(MultipartFile file) {
        // 这里应该是调用AI模型的逻辑
        // 暂时返回模拟数据

        AiAnalysisResultVo result = new AiAnalysisResultVo();
        result.setAiScore(79);

        // AI分析维度（Mock数据，实际使用时会通过算法计算）
        AiDimensionVo dimension1 = new AiDimensionVo();
        dimension1.setName("言語的困惑度");
        dimension1.setLevel(3.5); // 0.0-10.0，保留1位小数
        dimension1.setEvaluation("専門用語が使われているにもかかわらず、文章の構造があまりにも整然としていて、自然な流暢さに欠けている。");

        AiDimensionVo dimension2 = new AiDimensionVo();
        dimension2.setName("構造とテンプレート使用傾向");
        dimension2.setLevel(5.0);
        dimension2.setEvaluation("抽象的な定義から入り、論理展開が非常に典型的で、独自性に欠ける。");

        AiDimensionVo dimension3 = new AiDimensionVo();
        dimension3.setName("専門用語密度と論理的一貫性");
        dimension3.setLevel(3.2);
        dimension3.setEvaluation("「第一性原理的思考」「制度設計」「政策立案型エコノミスト」など高度な専門用語が一貫して使用されているが、論理展開にやや無理がある。");

        result.setAiDimensions(Arrays.asList(dimension1, dimension2, dimension3));

        // 质量维度
        QualityDimensionVo quality1 = new QualityDimensionVo();
        quality1.setName("志望動機の明確性と具体性");
        quality1.setScore(89);
        quality1.setEvaluation("志望動機は明確で具体的ですが、より個人的な経験や具体例を追加すると説得力が増します。");

        QualityDimensionVo quality2 = new QualityDimensionVo();
        quality2.setName("学部専門との適合度");
        quality2.setScore(79);
        quality2.setEvaluation("選択した専門分野との関連性はありますが、より深い関連性を示す具体例が必要です。");

        QualityDimensionVo quality3 = new QualityDimensionVo();
        quality3.setName("学習計画の合理性");
        quality3.setScore(69);
        quality3.setEvaluation("学習計画は基本的に合理的ですが、時間配分と具体的な実施方法についてより詳細な記述が必要です。");

        QualityDimensionVo quality4 = new QualityDimensionVo();
        quality4.setName("文章構造と論理展開");
        quality4.setScore(81);
        quality4.setEvaluation("文章構造は整っていますが、段落間のつながりや論理の流れに改善の余地があります。");

        QualityDimensionVo quality5 = new QualityDimensionVo();
        quality5.setName("表現力と説得力");
        quality5.setScore(91);
        quality5.setEvaluation("言語表現が流暢で、学術用語が正確で、文体が美しく、かなりの可読性があります。");

        QualityDimensionVo quality6 = new QualityDimensionVo();
        quality6.setName("文法と日本語の正確性");
        quality6.setScore(99);
        quality6.setEvaluation("文法は正確で、日本語表現も自然です。ただし、一部の表現がやや硬い印象です。");

        result.setQualityDimensions(Arrays.asList(quality1, quality2, quality3, quality4, quality5, quality6));

        // 修改建议
        result.setRecommendations(Arrays.asList(
                "結論部分の論理展開に一貫性が不足しています。具体例を追加して説明を補強してください。",
                "専門用語の使用がやや過剰です。読み手の理解度に合わせて平易な表現に置き換えることを検討してください。",
                "独自の考察部分をさらに拡充し、既存研究との差異を明確に示すとより説得力が増します。",
                "第2-4段落の表現方法を見直し、より自然な流れになるように調整してください。",
                "結論部分の論理的一貫性を強化し、論文全体の主張を明確にまとめてください。"
        ));
        return result;
    }


}