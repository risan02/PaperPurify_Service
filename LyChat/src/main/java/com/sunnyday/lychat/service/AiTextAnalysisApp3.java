package com.sunnyday.lychat.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 优化版：调整权重 + 平滑评分
 * 单文件可运行
 */
public class AiTextAnalysisApp3 {

    public static void main(String[] args) throws IOException {

        // ============= 修改你的文本文件路径 =============
//        String filePath = "D:\\starPlan\\01 意向\\2025-11-05 兆尹科技 SQL问题优化（王平一）\\达梦SQL优化交付.pdf";
//        String filePath = "C:\\Users\\ly\\Desktop\\tmp\\志望理由書\\東京大学経済学部志望理由書.docx";
//        String filePath = "C:\\Users\\ly\\Desktop\\tmp\\志望理由書\\九州大学薬学部志望理由書.docx";
//        String filePath = "C:\\Users\\ly\\Desktop\\tmp\\志望理由書\\青山学院大学理工学部志望理由書.docx";
//        String filePath = "C:\\Users\\ly\\Desktop\\tmp\\志望理由書\\慶應義塾大学環境情報学部志望理由書.docx";
//        String filePath = "C:\\Users\\ly\\Desktop\\tmp\\志望理由書\\慶應義塾大学総合政策学部志望理由書.pdf";
//        String filePath = "C:\\Users\\ly\\Desktop\\tmp\\志望理由書\\慶應義塾大学総合政策学部志望理由書.docx";
//        String filePath = "C:\\Users\\ly\\Desktop\\tmp\\志望理由書\\私が東京大学経済学部を志望する理由は.docx";
//        String filePath = "C:\\Users\\ly\\Desktop\\tmp\\志望理由書\\同志社大学商学部志望理由書.docx";
//        String filePath = "D:\\tmp\\志望理由书--交付文档.pdf";

//        AI
//        String filePath = "C:\\Users\\ly\\Desktop\\tmp\\志望理由書\\AI\\京都精華大学メディア表現学部志望理由書.docx";
//        String filePath = "C:\\Users\\ly\\Desktop\\tmp\\志望理由書\\AI\\京都精華大学メディア表現学部志望理由書２.docx";
        String filePath = "C:\\Users\\ly\\Desktop\\tmp\\志望理由書\\AI\\立命館大学法学部志望理由書.docx";

        String text = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");

        AiTextAnalyzer3 analyzer = new AiTextAnalyzer3();
        AiAnalysisResult3 result = analyzer.analyze(text);

        // ============= 输出检测结果 =============
        System.out.println("===== AI 文本分析结果（新版权重） =====");
        System.out.printf("总体 AI 率：%.3f（≈ %.1f%%）%n",
                result.getOverallAiRate(),
                result.getOverallAiRate() * 100.0);

        System.out.println("\n----- 六大维度 -----");
        for (AiAnalysisResult3.FeatureScore fs : result.getFeatures()) {
            System.out.printf("[%s] 分数：%.3f%n", fs.getName(), fs.getScore());
            System.out.println("说明：" + fs.getComment());
            System.out.println();
        }
    }
}

/**
 * 返回结构体：AI率 + 六维度
 */
class AiAnalysisResult3 {

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

    /**
     * 单个维度的封装
     */
    public static class FeatureScore {
        private String name;
        private double score;
        private String comment;

        public FeatureScore() {}

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

/**
 * 核心检测器
 * 负责：分析文本 → 返回 AI率 + 六维度
 */
class AiTextAnalyzer3 {

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

    // ============================ 对外分析入口 ============================

    public AiAnalysisResult3 analyze(String rawText) {

        String text = normalize(rawText);
        List<String> sentences = splitToSentences(text);

        // 六大维度
        double languageComplexity = computeLanguageComplexity(text);
        double burstiness = computeBurstiness(sentences);
        double topicEntropy = computeTopicEntropy(text);
        double reasoningComplexity = computeReasoningComplexity(sentences);
        double emotionVariance = computeEmotionVariance(sentences);

        // 模板相似度（AI-like）
        double templateSimilarityAiLike = computeTemplateSimilarity(text);
        double templateHumanScore = 1.0 - templateSimilarityAiLike; // 用户看到的是“越高越人类”

        // 封装维度
        List<AiAnalysisResult3.FeatureScore> features = new ArrayList<>();
        features.add(new AiAnalysisResult3.FeatureScore("LanguageComplexity", languageComplexity, explainLanguageComplexity(languageComplexity)));
        features.add(new AiAnalysisResult3.FeatureScore("Burstiness", burstiness, explainBurstiness(burstiness)));
        features.add(new AiAnalysisResult3.FeatureScore("TopicEntropy", topicEntropy, explainTopicEntropy(topicEntropy)));
        features.add(new AiAnalysisResult3.FeatureScore("ReasoningComplexity", reasoningComplexity, explainReasoningComplexity(reasoningComplexity)));
        features.add(new AiAnalysisResult3.FeatureScore("EmotionVariance", emotionVariance, explainEmotionVariance(emotionVariance)));
        features.add(new AiAnalysisResult3.FeatureScore("TemplateSimilarity", templateHumanScore, explainTemplateSimilarity(templateHumanScore)));

        // ============================ 新 AI 率权重（2025 优化版） ============================
        double aiLike =
                0.30 * (1 - languageComplexity) +     // 语言复杂度（强指标）
                        0.25 * (1 - topicEntropy) +          // 主题熵（强指标）
                        0.20 * (1 - burstiness) +            // 句式波动（中强）
                        0.15 * (templateSimilarityAiLike) +  // 模板相似度（中）
                        0.05 * (1 - reasoningComplexity) +   // 逻辑（弱）
                        0.05 * (1 - emotionVariance);        // 情绪（弱）

        aiLike = clamp(aiLike);

        AiAnalysisResult3 result = new AiAnalysisResult3();
        result.setOverallAiRate(aiLike);
        result.setFeatures(features);
        return result;
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

    private String explainLanguageComplexity(double s) {
        if (s < 0.3) return "语言模式较为单一，有模板化倾向。";
        if (s < 0.7) return "语言复杂度适中，自然。";
        return "语言多样性强，更接近人类写作。";
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

    private String explainBurstiness(double s) {
        if (s < 0.3) return "句式较为均匀，略偏 AI 风格。";
        if (s < 0.7) return "句式变化适中，自然。";
        return "句式波动大，写作风格较自由偏人类。";
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

    private String explainTopicEntropy(double s) {
        if (s < 0.3) return "主题集中，重复度高。";
        if (s < 0.7) return "主题信息分布自然。";
        return "表达丰富多样，内容信息量较大。";
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

    private String explainReasoningComplexity(double s) {
        if (s < 0.45) return "逻辑连接偏弱，可适当增加因果/转折衔接。";
        if (s < 0.75) return "逻辑结构自然，有一定推理性。";
        return "逻辑连接丰富，结构复杂。";
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

    private String explainEmotionVariance(double s) {
        if (s < 0.45) return "情绪表达平稳，中性为主。";
        if (s < 0.75) return "情绪表达自然，有一定起伏。";
        return "情绪较丰富，主观色彩明显。";
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

    private String explainTemplateSimilarity(double humanScore) {
        if (humanScore < 0.4) return "有一定模板化特征。";
        if (humanScore < 0.8) return "表达有部分接近模板，但仍保持个性。";
        return "与模板相似度低，写作风格个性化。";
    }
}

