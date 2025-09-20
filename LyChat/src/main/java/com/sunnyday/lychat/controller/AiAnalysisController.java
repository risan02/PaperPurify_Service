package com.sunnyday.lychat.controller;


import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.domain.AjaxResult;
import com.sunnyday.lychat.AiFileUtils;
import com.sunnyday.lychat.entity.AiAnalysisResultVo;
import com.sunnyday.lychat.entity.AiDimensionVo;
import com.sunnyday.lychat.entity.QualityDimensionVo;
import com.sunnyday.lychat.service.AiJapanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;

/**
 * AI内容分析控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiAnalysisController {

    @Autowired
    private AiJapanService aiJapanService;

    @GetMapping(value = "/chatTest")
    public String chatTest() {
        return "ni hao ! hello !";
    }
    /**
     * 内容分析接口
     */
    @PostMapping("/contentAnalyse")
    public AjaxResult contentAnalyse(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 验证文件
            if (file.isEmpty()) {
                return AjaxResult.error("ファイルをアップロードしてください");
            }

            // 2. 验证文件类型
            String fileName = file.getOriginalFilename();
            String fileType = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            if (!"pdf".equals(fileType) && !"docx".equals(fileType)) {
                return AjaxResult.error("PDFまたはDOCX形式のファイルのみ対応しています");
            }

            // 3. 验证文件大小 (最大10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return AjaxResult.error("ファイルサイズは10MB以下にしてください");
            }

            // 4. 调用AI分析服务
            // mock
//            AiAnalysisResultVo result = analyzeContentMock(file);

            String fileContent = AiFileUtils.readFileContent(file);
            String aiResult = aiJapanService.chat(String.valueOf(System.currentTimeMillis()), fileContent);
            log.info("AI分析結果: " + aiResult);
            // 将aiResult的json数据转换为AiAnalysisResultVo对象
            AiAnalysisResultVo result = JSON.parseObject(aiResult, AiAnalysisResultVo.class);

            return AjaxResult.success(result);
        } catch (Exception e) {
            return AjaxResult.error("分析中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * AI内容分析方法 (模拟实现)
     */
    private AiAnalysisResultVo analyzeContentMock(MultipartFile file) {
        // 这里应该是调用AI模型的逻辑
        // 暂时返回模拟数据

        AiAnalysisResultVo result = new AiAnalysisResultVo();
        result.setAiScore(79);

        // AI分析维度
        AiDimensionVo dimension1 = new AiDimensionVo();
        dimension1.setName("言語的困惑度");
        dimension1.setLevel("高");
        dimension1.setEvaluation("専門用語が使われているにもかかわらず、文章の構造があまりにも整然としていて、自然な流暢さに欠けている。");

        AiDimensionVo dimension2 = new AiDimensionVo();
        dimension2.setName("構造とテンプレート使用傾向");
        dimension2.setLevel("中");
        dimension2.setEvaluation("抽象的な定義から入り、論理展開が非常に典型的で、独自性に欠ける。");

        AiDimensionVo dimension3 = new AiDimensionVo();
        dimension3.setName("専門用語密度と論理的一貫性");
        dimension3.setLevel("高");
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