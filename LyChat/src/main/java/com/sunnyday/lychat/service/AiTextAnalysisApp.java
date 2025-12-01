package com.sunnyday.lychat.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 完整可运行的 AI 文本检测（带日文 NLP + 情感词库 + 从磁盘读取文本）
 * 特点：
 * - 单文件，无任何外部依赖
 * - 输入：文本内容（String）
 * - 输出：AI率 + 六大维度得分 + 六大维度说明
 */
public class AiTextAnalysisApp {

    public static void main(String[] args) throws IOException {

        // ============================================================
        // ① 在此处修改你的志望理由书文本文件路径
        // ============================================================
        String filePath = "D:\\tmp\\志望理由书--交付文档.pdf";

        String text = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");

        AiTextAnalyzer2 analyzer = new AiTextAnalyzer2();
        AiAnalysisResult2 result = analyzer.analyze(text);

        // ============================================================
        // 输出结果
        // ============================================================
        System.out.println("===== AI 文本分析结果 =====");
        System.out.printf("总体 AI 率：%.2f（≈ %.1f%%）%n",
                result.getOverallAiRate(),
                result.getOverallAiRate() * 100.0);

        System.out.println("\n----- 六大维度 -----");
        for (AiAnalysisResult2.FeatureScore fs : result.getFeatures()) {
            System.out.printf("[%s] 分数：%.3f%n", fs.getName(), fs.getScore());
            System.out.println("说明：" + fs.getComment());
            System.out.println();
        }
    }
}


// ============================ 结果结构 ============================

class AiAnalysisResult {

    private double overallAiRate;

    private List<FeatureScore> features;

    public double getOverallAiRate() {
        return overallAiRate;
    }

    public void setOverallAiRate(double overallAiRate) {
        this.overallAiRate = overallAiRate;
    }

    public List<FeatureScore> getFeatures() {
        return features;
    }

    public void setFeatures(List<FeatureScore> features) {
        this.features = features;
    }

    public static class FeatureScore {
        private String name;
        private double score;
        private String comment;

        public FeatureScore() {
        }

        public FeatureScore(String name, double score, String comment) {
            this.name = name;
            this.score = score;
            this.comment = comment;
        }

        public String getName() {
            return name;
        }

        public double getScore() {
            return score;
        }

        public String getComment() {
            return comment;
        }
    }
}


// ============================ 核心检测器 ============================

class AiTextAnalyzer {

    // ============================ 日文 NLP 轻量词库 ============================

    // 常见日文正向情感词（日本語評価極性辞書的常见词片段）
    private static final String[] JP_POSITIVE = {
            "嬉し", "楽", "感謝", "満足", "充実", "希望", "期待", "興味", "尊敬", "光栄",
            "幸せ", "誇り", "自信", "喜び", "成長", "達成", "挑戦したい"
    };

    // 常见日文负向情感词
    private static final String[] JP_NEGATIVE = {
            "不安", "心配", "悩", "困難", "問題", "挫折", "恐れ", "落ち込", "怒り", "失敗",
            "悲し", "弱い", "迷い", "不満"
    };

    // 日文逻辑连接词库（比上一版更全）
    private static final String[] LOGIC_WORDS = {
            "しかし", "だが", "一方で", "そのため", "なので", "ために",
            "もし", "そして", "さらに", "つまり", "したがって",
            "例えば", "なぜなら", "ゆえに", "ところが", "それにもかかわらず"
    };

    // 你可以扩充“模板库”
    private final List<String> templateTexts = List.of(
            "本志望理由書では、私が貴学を志望する理由と、将来の研究計画について述べたいと思います。",
            "私は幼い頃から日本の文化と社会に強い関心を抱いてきました。",
            "これまでの学習と経験を通じて、私はデータサイエンスの分野で専門性を高めていきたいと考えています。",
            "貴学の教育理念に共感し、より深い専門性を身につけたいと考えています。"
    );

    // ============================================================
    // 核心入口方法，你的后端只需调用此方法即可
    // ============================================================
    public AiAnalysisResult2 analyze(String rawText) {

        String text = normalize(rawText);
        List<String> sentences = splitToSentences(text);

        double languageComplexity = computeLanguageComplexity(text);
        double burstiness = computeBurstiness(sentences);
        double topicEntropy = computeTopicEntropy(text);
        double reasoningComplexity = computeReasoningComplexity(sentences);
        double emotionVariance = computeEmotionVariance(sentences);
        double templateSimilarity = computeTemplateSimilarity(text);

        List<AiAnalysisResult2.FeatureScore> features = new ArrayList<>();

        features.add(new AiAnalysisResult2.FeatureScore(
                "LanguageComplexity", languageComplexity,
                explainLanguageComplexity(languageComplexity)
        ));
        features.add(new AiAnalysisResult2.FeatureScore(
                "Burstiness", burstiness,
                explainBurstiness(burstiness)
        ));
        features.add(new AiAnalysisResult2.FeatureScore(
                "TopicEntropy", topicEntropy,
                explainTopicEntropy(topicEntropy)
        ));
        features.add(new AiAnalysisResult2.FeatureScore(
                "ReasoningComplexity", reasoningComplexity,
                explainReasoningComplexity(reasoningComplexity)
        ));
        features.add(new AiAnalysisResult2.FeatureScore(
                "EmotionVariance", emotionVariance,
                explainEmotionVariance(emotionVariance)
        ));
        features.add(new AiAnalysisResult2.FeatureScore(
                "TemplateSimilarity", templateSimilarity,
                explainTemplateSimilarity(templateSimilarity)
        ));

        // ===========================
        // 综合 AI 率（可调整权重）
        // ===========================
        double aiScore =
                0.25 * (1 - languageComplexity) +
                        0.15 * (1 - burstiness) +
                        0.15 * (1 - topicEntropy) +
                        0.15 * (1 - reasoningComplexity) +
                        0.15 * (1 - emotionVariance) +
                        0.15 * templateSimilarity;

        aiScore = clamp(aiScore);

        AiAnalysisResult2 result = new AiAnalysisResult2();
        result.setOverallAiRate(aiScore);
        result.setFeatures(features);

        return result;
    }


    // ============================ 工具函数 ============================

    private String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private List<String> splitToSentences(String text) {
        String[] arr = text.split("[。！？!?.\\n]");
        return Arrays.stream(arr)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }


    // ============================ ① 语言复杂度（4-gram 熵） ============================

    private double computeLanguageComplexity(String text) {
        if (text.length() < 12) return 0.5;

        Map<String, Integer> freq = new HashMap<>();
        int n = 4, total = 0;

        for (int i = 0; i <= text.length() - n; i++) {
            String g = text.substring(i, i + n);
            freq.merge(g, 1, Integer::sum);
            total++;
        }

        double entropy = 0;
        for (int c : freq.values()) {
            double p = (double) c / total;
            entropy += -p * Math.log(p);
        }

        return clamp((entropy - 1.5) / (4.0 - 1.5));
    }

    private String explainLanguageComplexity(double s) {
        if (s < 0.3) return "语言模式重复率高，可能存在模板化或 AI 特征。";
        if (s < 0.7) return "语言复杂度适中，自然度良好。";
        return "语言变化丰富，多样性强，更像人类写作。";
    }


    // ============================ ② 句式波动 ============================

    private double computeBurstiness(List<String> sents) {
        if (sents.isEmpty()) return 0.5;

        List<Integer> lens = sents.stream().map(String::length).collect(Collectors.toList());
        double mean = lens.stream().mapToDouble(i -> i).average().orElse(0);
        double var = lens.stream().mapToDouble(i -> (i - mean) * (i - mean)).average().orElse(0);
        double std = Math.sqrt(var);

        if (mean == 0) return 0.5;

        double B = (std - mean) / (std + mean);
        return clamp((B + 1) / 2);
    }

    private String explainBurstiness(double s) {
        if (s < 0.3) return "句子长度过于均匀，类似 AI 生成文本。";
        if (s < 0.7) return "句式变化适中。";
        return "句长波动明显，符合自然写作。";
    }


    // ============================ ③ 主题熵 ============================

    private double computeTopicEntropy(String text) {
        if (text.length() < 5) return 0.5;

        Map<String, Integer> freq = new HashMap<>();
        int total = 0;

        for (int i = 0; i < text.length() - 1; i++) {
            String bg = text.substring(i, i + 2);
            freq.merge(bg, 1, Integer::sum);
            total++;
        }

        double ent = 0;
        for (int c : freq.values()) {
            double p = (double) c / total;
            ent += -p * Math.log(p);
        }

        return clamp((ent - 0.5) / (3.0 - 0.5));
    }

    private String explainTopicEntropy(double s) {
        if (s < 0.3) return "表达重复度较高，主题集中，可能模板化。";
        if (s < 0.7) return "主题分布均衡，自然性良好。";
        return "表达丰富，信息多样性强。";
    }


    // ============================ ④ 推理复杂度 ============================

    private double computeReasoningComplexity(List<String> sents) {

        int total = 0, count = 0;

        for (String s : sents) {
            total += s.length();
            for (String w : LOGIC_WORDS) {
                int idx = s.indexOf(w);
                while (idx >= 0) {
                    count++;
                    idx = s.indexOf(w, idx + w.length());
                }
            }
        }

        if (total == 0) return 0.5;

        double density = (double) count / total;
        return clamp(density / 0.01);
    }

    private String explainReasoningComplexity(double s) {
        if (s < 0.3) return "逻辑结构偏弱，句子过于线性。";
        if (s < 0.7) return "逻辑结构适中，有一定推理深度。";
        return "逻辑连接多层，结构复杂，偏人类写作。";
    }


    // ============================ ⑤ 情绪方差（结合日文极性词库） ============================

    private double computeEmotionVariance(List<String> sents) {

        List<Integer> emo = new ArrayList<>();

        for (String s : sents) {
            int pos = 0, neg = 0;

            for (String w : JP_POSITIVE)
                if (s.contains(w)) pos++;

            for (String w : JP_NEGATIVE)
                if (s.contains(w)) neg++;

            emo.add(pos - neg);
        }

        double mean = emo.stream().mapToDouble(i -> i).average().orElse(0);
        double var = emo.stream().mapToDouble(i -> (i - mean) * (i - mean)).average().orElse(0);

        return clamp(var / 2.0);
    }

    private String explainEmotionVariance(double s) {
        if (s < 0.3) return "情绪波动很小，较为平直，类似 AI 输出。";
        if (s < 0.7) return "情绪表达适中，自然。";
        return "情绪起伏较大，更具人类个性化特征。";
    }


    // ============================ ⑥ 模板相似度 ============================

    private double computeTemplateSimilarity(String text) {
        Map<String, Integer> docVec = buildVector(text, 3);
        if (docVec.isEmpty()) return 0;

        double maxSim = 0;

        for (String tpl : templateTexts) {
            Map<String, Integer> tplVec = buildVector(tpl, 3);
            maxSim = Math.max(maxSim, cosine(docVec, tplVec));
        }
        return clamp(maxSim);
    }

    private Map<String, Integer> buildVector(String text, int n) {
        Map<String, Integer> map = new HashMap<>();
        if (text.length() < n) return map;

        for (int i = 0; i <= text.length() - n; i++) {
            String g = text.substring(i, i + n);
            map.merge(g, 1, Integer::sum);
        }
        return map;
    }

    private double cosine(Map<String, Integer> v1, Map<String, Integer> v2) {
        Set<String> keys = new HashSet<>();
        keys.addAll(v1.keySet());
        keys.addAll(v2.keySet());

        double dot = 0, n1 = 0, n2 = 0;

        for (String k : keys) {
            int a = v1.getOrDefault(k, 0);
            int b = v2.getOrDefault(k, 0);
            dot += a * b;
            n1 += a * a;
            n2 += b * b;
        }

        if (n1 == 0 || n2 == 0) return 0;
        return dot / (Math.sqrt(n1) * Math.sqrt(n2));
    }

    private String explainTemplateSimilarity(double s) {
        if (s < 0.3) return "几乎没有套用模板或范文的痕迹。";
        if (s < 0.7) return "部分表达与常见范文有相似性。";
        return "具有明显模板化表达，可能参考了范文或 AI 生成格式。";
    }
}
