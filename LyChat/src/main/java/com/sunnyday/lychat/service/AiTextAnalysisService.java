package com.sunnyday.lychat.service;

import com.sunnyday.lychat.entity.AiDimensionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI文本分析服务
 * 基于数学公式计算AI生成痕迹的6个维度
 */
@Service
public class AiTextAnalysisService {

    @Autowired
    private MessageSource messageSource;

    // ====== 日文情绪词库（轻量） ======
    private static final String[] JP_POSITIVE = {
            "嬉し", "楽", "感謝", "満足", "充実", "希望", "期待",
            "尊敬", "光栄", "誇り", "喜び"
    };

    private static final String[] JP_NEGATIVE = {
            "不安", "心配", "悩", "困難", "問題", "挫折",
            "恐れ", "怒り", "失敗", "悲し"
    };

    private static final String[] LOGIC_WORDS = {
            "しかし", "だが", "一方で", "そのため", "なので",
            "もし", "そして", "さらに", "つまり", "したがって"
    };

    // 模板库（可自己扩展）
    private final List<String> templateTexts = List.of(
            "本志望理由書では、私が貴学を志望する理由と、将来の研究計画について述べたいと思います。",
            "私は幼い頃から日本の文化と社会に強い関心を抱いてきました。",
            "これまでの学習と経験を通じて、私はデータサイエンスの分野で専門性を高めていきたいと考えています。",
            "貴学の教育理念に共感し、より深い専門性を身につけたいと考えています。"
    );

    /**
     * 内部结果类：保存6个维度的原始计算值
     */
    private static class DimensionValues {
        double languageComplexity;
        double burstiness;
        double topicEntropy;
        double reasoningComplexity;
        double emotionVariance;
        double templateSimilarityAiLike;
        double templateHumanScore;
    }

    /**
     * 一次性计算所有6个维度的原始值（避免重复计算）
     * 
     * @param rawText 原始文本内容
     * @return 维度值对象
     */
    private DimensionValues computeAllDimensions(String rawText) {
        String text = normalize(rawText);
        List<String> sentences = splitToSentences(text);

        DimensionValues values = new DimensionValues();
        values.languageComplexity = computeLanguageComplexity(text);
        values.burstiness = computeBurstiness(sentences);
        values.topicEntropy = computeTopicEntropy(text);
        values.reasoningComplexity = computeReasoningComplexity(sentences);
        values.emotionVariance = computeEmotionVariance(sentences);
        values.templateSimilarityAiLike = computeTemplateSimilarity(text);
        values.templateHumanScore = 1.0 - values.templateSimilarityAiLike; // 用户看到的是"越高越人类"

        return values;
    }

    /**
     * 分析文本，返回AI生成痕迹的6个维度
     * 
     * @param rawText 原始文本内容
     * @param locale 语言环境
     * @return AI痕迹分析维度列表（6个维度）
     */
    public List<AiDimensionVo> analyzeAiDimensions(String rawText, Locale locale) {
        // 一次性计算所有维度值
        DimensionValues values = computeAllDimensions(rawText);

        // 封装为AiDimensionVo列表（6个维度）
        List<AiDimensionVo> dimensions = new ArrayList<>();
        
        // 维度1: 言語的困惑度 (LanguageComplexity)
        AiDimensionVo dim1 = new AiDimensionVo();
        dim1.setName(messageSource.getMessage("ai.dimension.linguistic_perplexity", null, locale));
        dim1.setLevel(roundTo1Decimal(values.languageComplexity * 10.0)); // 转换为0-10范围，保留1位小数
        dim1.setEvaluation(explainLanguageComplexity(values.languageComplexity, locale));
        dimensions.add(dim1);

        // 维度2: 句式变化幅度 (Burstiness)
        AiDimensionVo dim2 = new AiDimensionVo();
        dim2.setName(messageSource.getMessage("ai.dimension.sentence_variation", null, locale));
        dim2.setLevel(roundTo1Decimal(values.burstiness * 10.0));
        dim2.setEvaluation(explainBurstiness(values.burstiness, locale));
        dimensions.add(dim2);

        // 维度3: 语义分布熵 (TopicEntropy)
        AiDimensionVo dim3 = new AiDimensionVo();
        dim3.setName(messageSource.getMessage("ai.dimension.semantic_entropy", null, locale));
        dim3.setLevel(roundTo1Decimal(values.topicEntropy * 10.0));
        dim3.setEvaluation(explainTopicEntropy(values.topicEntropy, locale));
        dimensions.add(dim3);

        // 维度4: 推理复杂性 (ReasoningComplexity)
        AiDimensionVo dim4 = new AiDimensionVo();
        dim4.setName(messageSource.getMessage("ai.dimension.inference_complexity", null, locale));
        dim4.setLevel(roundTo1Decimal(values.reasoningComplexity * 10.0));
        dim4.setEvaluation(explainReasoningComplexity(values.reasoningComplexity, locale));
        dimensions.add(dim4);

        // 维度5: 情绪起伏度 (EmotionVariance)
        AiDimensionVo dim5 = new AiDimensionVo();
        dim5.setName(messageSource.getMessage("ai.dimension.emotional_fluctuation", null, locale));
        dim5.setLevel(roundTo1Decimal(values.emotionVariance * 10.0));
        dim5.setEvaluation(explainEmotionVariance(values.emotionVariance, locale));
        dimensions.add(dim5);

        // 维度6: 構造とテンプレート使用傾向 (TemplateSimilarity)
        AiDimensionVo dim6 = new AiDimensionVo();
        dim6.setName(messageSource.getMessage("ai.dimension.template_tendency", null, locale));
        dim6.setLevel(roundTo1Decimal(values.templateHumanScore * 10.0));
        dim6.setEvaluation(explainTemplateSimilarity(values.templateHumanScore, locale));
        dimensions.add(dim6);

        return dimensions;
    }

    /**
     * 计算总体AI率（0-100，分数越高说明由AI生成的痕迹越明显）
     * 基于已计算的6个维度值进行加权计算
     * 
     * @param rawText 原始文本内容
     * @return AI生成可能性分数（0-100）
     */
    public Integer calculateAiScore(String rawText) {
        // 一次性计算所有维度值
        DimensionValues values = computeAllDimensions(rawText);

        // AI率权重计算（2025优化版）- 基于已计算的维度值
        double aiLike =
                0.30 * (1 - values.languageComplexity) +     // 语言复杂度（强指标）
                0.25 * (1 - values.topicEntropy) +          // 主题熵（强指标）
                0.20 * (1 - values.burstiness) +            // 句式波动（中强）
                0.15 * (values.templateSimilarityAiLike) +  // 模板相似度（中）
                0.05 * (1 - values.reasoningComplexity) +   // 逻辑（弱）
                0.05 * (1 - values.emotionVariance);        // 情绪（弱）

        aiLike = clamp(aiLike);
        
        // 转换为0-100的整数分数
        return (int) Math.round(aiLike * 100.0);
    }

    /**
     * 同时分析6个维度和计算AI率（推荐使用，避免重复计算）
     * 
     * @param rawText 原始文本内容
     * @param locale 语言环境
     * @return 包含6个维度列表和AI率的分析结果对象
     */
    public AnalysisResult analyzeWithScore(String rawText, Locale locale) {
        // 一次性计算所有维度值
        DimensionValues values = computeAllDimensions(rawText);

        // 封装为AiDimensionVo列表（6个维度）
        List<AiDimensionVo> dimensions = new ArrayList<>();
        
        // 维度1: 言語的困惑度 (LanguageComplexity)
        AiDimensionVo dim1 = new AiDimensionVo();
        dim1.setName(messageSource.getMessage("ai.dimension.linguistic_perplexity", null, locale));
        dim1.setLevel(roundTo1Decimal(values.languageComplexity * 10.0));
        dim1.setEvaluation(explainLanguageComplexity(values.languageComplexity, locale));
        dimensions.add(dim1);

        // 维度2: 句式变化幅度 (Burstiness)
        AiDimensionVo dim2 = new AiDimensionVo();
        dim2.setName(messageSource.getMessage("ai.dimension.sentence_variation", null, locale));
        dim2.setLevel(roundTo1Decimal(values.burstiness * 10.0));
        dim2.setEvaluation(explainBurstiness(values.burstiness, locale));
        dimensions.add(dim2);

        // 维度3: 语义分布熵 (TopicEntropy)
        AiDimensionVo dim3 = new AiDimensionVo();
        dim3.setName(messageSource.getMessage("ai.dimension.semantic_entropy", null, locale));
        dim3.setLevel(roundTo1Decimal(values.topicEntropy * 10.0));
        dim3.setEvaluation(explainTopicEntropy(values.topicEntropy, locale));
        dimensions.add(dim3);

        // 维度4: 推理复杂性 (ReasoningComplexity)
        AiDimensionVo dim4 = new AiDimensionVo();
        dim4.setName(messageSource.getMessage("ai.dimension.inference_complexity", null, locale));
        dim4.setLevel(roundTo1Decimal(values.reasoningComplexity * 10.0));
        dim4.setEvaluation(explainReasoningComplexity(values.reasoningComplexity, locale));
        dimensions.add(dim4);

        // 维度5: 情绪起伏度 (EmotionVariance)
        AiDimensionVo dim5 = new AiDimensionVo();
        dim5.setName(messageSource.getMessage("ai.dimension.emotional_fluctuation", null, locale));
        dim5.setLevel(roundTo1Decimal(values.emotionVariance * 10.0));
        dim5.setEvaluation(explainEmotionVariance(values.emotionVariance, locale));
        dimensions.add(dim5);

        // 维度6: 構造とテンプレート使用傾向 (TemplateSimilarity)
        AiDimensionVo dim6 = new AiDimensionVo();
        dim6.setName(messageSource.getMessage("ai.dimension.template_tendency", null, locale));
        dim6.setLevel(roundTo1Decimal(values.templateHumanScore * 10.0));
        dim6.setEvaluation(explainTemplateSimilarity(values.templateHumanScore, locale));
        dimensions.add(dim6);

        // 计算AI率
        double aiLike =
                0.30 * (1 - values.languageComplexity) +
                0.25 * (1 - values.topicEntropy) +
                0.20 * (1 - values.burstiness) +
                0.15 * (values.templateSimilarityAiLike) +
                0.05 * (1 - values.reasoningComplexity) +
                0.05 * (1 - values.emotionVariance);
        aiLike = clamp(aiLike);
        Integer aiScore = (int) Math.round(aiLike * 100.0);

        return new AnalysisResult(dimensions, aiScore);
    }

    /**
     * 分析结果封装类
     */
    public static class AnalysisResult {
        private final List<AiDimensionVo> dimensions;
        private final Integer aiScore;

        public AnalysisResult(List<AiDimensionVo> dimensions, Integer aiScore) {
            this.dimensions = dimensions;
            this.aiScore = aiScore;
        }

        public List<AiDimensionVo> getDimensions() {
            return dimensions;
        }

        public Integer getAiScore() {
            return aiScore;
        }
    }

    // ============================ 工具函数 ============================

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }

    private List<String> splitToSentences(String text) {
        if (text.isEmpty()) return Collections.emptyList();
        String[] arr = text.split("[。！？!?.\\n]");
        return Arrays.stream(arr).map(String::trim).filter(t -> !t.isEmpty()).collect(Collectors.toList());
    }

    private double clamp(double v) {
        return Math.min(1.0, Math.max(0.0, v));
    }

    private double smoothRatio(double x, double c) {
        if (x <= 0) return 0.0;
        return x / (x + c);
    }

    private double logistic(double x, double mid, double k) {
        return 1.0 / (1.0 + Math.exp(-k * (x - mid)));
    }

    /**
     * 保留1位小数
     * @param value 原始值
     * @return 保留1位小数的值
     */
    private double roundTo1Decimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // ====================== ① 语言复杂度（4-gram 熵） ======================

    private double computeLanguageComplexity(String text) {
        if (text.length() < 12) return 0.5;

        Map<String, Integer> freq = new HashMap<>();
        int n = 4;
        int total = 0;

        for (int i = 0; i <= text.length() - n; i++) {
            String g = text.substring(i, i + n);
            freq.merge(g, 1, Integer::sum);
            total++;
        }

        double entropy = 0.0;
        for (int c : freq.values()) {
            double p = (double) c / total;
            entropy += -p * Math.log(p);
        }

        return clamp(logistic(entropy, 2.8, 1.0));
    }

    private String explainLanguageComplexity(double s, Locale locale) {
        String key;
        if (s < 0.3) {
            key = "ai.dimension.linguistic_perplexity.evaluation.low";
        } else if (s < 0.7) {
            key = "ai.dimension.linguistic_perplexity.evaluation.medium";
        } else {
            key = "ai.dimension.linguistic_perplexity.evaluation.high";
        }
        return messageSource.getMessage(key, null, locale);
    }

    // ====================== ② 句式波动 ======================

    private double computeBurstiness(List<String> sentences) {
        if (sentences.isEmpty()) return 0.5;

        List<Integer> lens = sentences.stream().map(String::length).collect(Collectors.toList());
        double mean = lens.stream().mapToDouble(i -> i).average().orElse(0.0);
        double var = lens.stream().mapToDouble(i -> (i - mean) * (i - mean)).average().orElse(0.0);
        double std = Math.sqrt(var);

        if (mean <= 0) return 0.5;

        double ratio = std / mean;
        return clamp(smoothRatio(ratio, 0.5));
    }

    private String explainBurstiness(double s, Locale locale) {
        String key;
        if (s < 0.3) {
            key = "ai.dimension.sentence_variation.evaluation.low";
        } else if (s < 0.7) {
            key = "ai.dimension.sentence_variation.evaluation.medium";
        } else {
            key = "ai.dimension.sentence_variation.evaluation.high";
        }
        return messageSource.getMessage(key, null, locale);
    }

    // ====================== ③ 主题熵 ======================

    private double computeTopicEntropy(String text) {
        if (text.length() < 5) return 0.5;

        Map<String, Integer> freq = new HashMap<>();
        int total = 0;

        for (int i = 0; i < text.length() - 1; i++) {
            String bg = text.substring(i, i + 2);
            freq.merge(bg, 1, Integer::sum);
            total++;
        }

        double entropy = 0.0;
        for (int c : freq.values()) {
            double p = (double) c / total;
            entropy += -p * Math.log(p);
        }

        return clamp(logistic(entropy, 1.8, 1.2));
    }

    private String explainTopicEntropy(double s, Locale locale) {
        String key;
        if (s < 0.3) {
            key = "ai.dimension.semantic_entropy.evaluation.low";
        } else if (s < 0.7) {
            key = "ai.dimension.semantic_entropy.evaluation.medium";
        } else {
            key = "ai.dimension.semantic_entropy.evaluation.high";
        }
        return messageSource.getMessage(key, null, locale);
    }

    // ====================== ④ 推理复杂度 ======================

    private double computeReasoningComplexity(List<String> sentences) {
        if (sentences.isEmpty()) return 0.45; // 给一个中性偏上的默认值

        int totalChars = 0;
        int logicCount = 0;

        for (String s : sentences) {
            totalChars += s.length();
            for (String w : LOGIC_WORDS) {
                int idx = s.indexOf(w);
                while (idx >= 0) {
                    logicCount++;
                    idx = s.indexOf(w, idx + w.length());
                }
            }
        }

        if (totalChars == 0) return 0.45;

        double density = (double) logicCount / totalChars;

        // 日语文书逻辑词本来就少，因此我们让分数区间偏高一点
        double score = 0.4 + 0.6 * smoothRatio(density, 0.003);
        return clamp(score);
    }

    private String explainReasoningComplexity(double s, Locale locale) {
        String key;
        if (s < 0.45) {
            key = "ai.dimension.inference_complexity.evaluation.low";
        } else if (s < 0.75) {
            key = "ai.dimension.inference_complexity.evaluation.medium";
        } else {
            key = "ai.dimension.inference_complexity.evaluation.high";
        }
        return messageSource.getMessage(key, null, locale);
    }

    // ====================== ⑤ 情绪起伏 ======================

    private double computeEmotionVariance(List<String> sentences) {
        if (sentences.isEmpty()) return 0.45;

        List<Double> emoScores = new ArrayList<>();

        for (String s : sentences) {
            int pos = 0, neg = 0;
            for (String w : JP_POSITIVE) if (s.contains(w)) pos++;
            for (String w : JP_NEGATIVE) if (s.contains(w)) neg++;
            emoScores.add((double) (pos - neg));
        }

        double mean = emoScores.stream().mapToDouble(i -> i).average().orElse(0.0);
        double var = emoScores.stream().mapToDouble(i -> (i - mean) * (i - mean)).average().orElse(0.0);

        // 志望理由书本身情绪平稳 → 给中性偏上的基准分
        double score = 0.4 + 0.6 * smoothRatio(var, 0.8);
        return clamp(score);
    }

    private String explainEmotionVariance(double s, Locale locale) {
        String key;
        if (s < 0.45) {
            key = "ai.dimension.emotional_fluctuation.evaluation.low";
        } else if (s < 0.75) {
            key = "ai.dimension.emotional_fluctuation.evaluation.medium";
        } else {
            key = "ai.dimension.emotional_fluctuation.evaluation.high";
        }
        return messageSource.getMessage(key, null, locale);
    }

    // ====================== ⑥ 模板相似度（AI-like） ======================

    private double computeTemplateSimilarity(String text) {
        Map<String, Integer> docVec = buildCharNgramVector(text, 3);
        if (docVec.isEmpty()) return 0.0;

        double maxSim = 0.0;
        for (String tpl : templateTexts) {
            Map<String, Integer> tplVec = buildCharNgramVector(tpl, 3);
            maxSim = Math.max(maxSim, cosineSimilarity(docVec, tplVec));
        }

        return clamp(maxSim); // 越高越像 AI 模板
    }

    private Map<String, Integer> buildCharNgramVector(String text, int n) {
        Map<String, Integer> map = new HashMap<>();
        if (text.length() < n) return map;

        for (int i = 0; i <= text.length() - n; i++) {
            String g = text.substring(i, i + n);
            map.merge(g, 1, Integer::sum);
        }
        return map;
    }

    private double cosineSimilarity(Map<String, Integer> v1, Map<String, Integer> v2) {
        Set<String> keys = new HashSet<>();
        keys.addAll(v1.keySet());
        keys.addAll(v2.keySet());

        double dot = 0.0, n1 = 0.0, n2 = 0.0;

        for (String k : keys) {
            int a = v1.getOrDefault(k, 0);
            int b = v2.getOrDefault(k, 0);
            dot += a * b;
            n1 += a * a;
            n2 += b * b;
        }
        if (n1 == 0.0 || n2 == 0.0) return 0.0;
        return dot / (Math.sqrt(n1) * Math.sqrt(n2));
    }

    private String explainTemplateSimilarity(double humanScore, Locale locale) {
        String key;
        if (humanScore < 0.4) {
            key = "ai.dimension.template_tendency.evaluation.low";
        } else if (humanScore < 0.8) {
            key = "ai.dimension.template_tendency.evaluation.medium";
        } else {
            key = "ai.dimension.template_tendency.evaluation.high";
        }
        return messageSource.getMessage(key, null, locale);
    }
}

