package com.sunnyday.lychat.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 优化版：平滑评分 + 从磁盘读取文本 + 单文件可运行
 */
public class AiTextAnalysisApp2 {

    public static void main(String[] args) throws IOException {

        // ====================== 1. 修改你的文件路径 ======================
        // 示例：txt、从 PDF/Word 抽取后的纯文本等
        String filePath = "D:\\tmp\\志望理由书--交付文档.pdf";

        String text = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");

        AiTextAnalyzer2 analyzer = new AiTextAnalyzer2();
        AiAnalysisResult2 result = analyzer.analyze(text);

        // ====================== 2. 输出检测结果 ======================
        System.out.println("===== AI 文本分析结果（平滑版） =====");
        System.out.printf("总体 AI 率：%.3f（≈ %.1f%%）%n",
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

/**
 * 分析结果结构：
 * - overallAiRate: 0~1，越高越像 AI
 * - features: 六大维度结果
 */
class AiAnalysisResult2 {

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
     * 单个维度分数：
     * - name：维度名
     * - score：0~1，越高越像“人类写作”
     * - comment：说明
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
 * 核心检测器：
 * - analyze(String text)：输入整篇文档内容
 * - 返回：AI率 + 六大维度
 */
class AiTextAnalyzer2 {

    // ====== 轻量日文情感/逻辑词库（可后续扩展） ======

    private static final String[] JP_POSITIVE = {
            "嬉し", "楽", "感謝", "満足", "充実", "希望", "期待", "興味", "尊敬", "光栄",
            "幸せ", "誇り", "自信", "喜び", "成長", "達成", "挑戦したい"
    };

    private static final String[] JP_NEGATIVE = {
            "不安", "心配", "悩", "困難", "問題", "挫折", "恐れ", "落ち込", "怒り", "失敗",
            "悲し", "弱い", "迷い", "不満"
    };

    private static final String[] LOGIC_WORDS = {
            "しかし", "だが", "一方で", "そのため", "なので", "ために",
            "もし", "そして", "さらに", "つまり", "したがって",
            "例えば", "なぜなら", "ゆえに", "ところが", "それにもかかわらず"
    };

    // 模板库：可以换成你的范文/AI 生成样例
    private final List<String> templateTexts = List.of(
            "本志望理由書では、私が貴学を志望する理由と、将来の研究計画について述べたいと思います。",
            "私は幼い頃から日本の文化と社会に強い関心を抱いてきました。",
            "これまでの学習と経験を通じて、私はデータサイエンスの分野で専門性を高めていきたいと考えています。",
            "貴学の教育理念に共感し、より深い専門性を身につけたいと考えています。"
    );

    // ============================ 公共入口 ============================

    public AiAnalysisResult2 analyze(String rawText) {

        String text = normalize(rawText);
        List<String> sentences = splitToSentences(text);

        double languageComplexity = computeLanguageComplexity(text);
        double burstiness = computeBurstiness(sentences);
        double topicEntropy = computeTopicEntropy(text);
        double reasoningComplexity = computeReasoningComplexity(sentences);
        double emotionVariance = computeEmotionVariance(sentences);

        // 注意：模板相似度越高越像 AI，这里先算“AI 相似度”
        double templateSimilarityAiLike = computeTemplateSimilarity(text);
        // 对用户展示时，我们展示“TemplateHumanScore = 1 - 相似度”
        double templateHumanScore = 1.0 - templateSimilarityAiLike;

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
                "TemplateSimilarity", templateHumanScore,
                explainTemplateSimilarity(templateHumanScore)
        ));

        // ================= 综合 AI 率（平滑版） =================
        // AI-like 程度：越高越像 AI
        double aiLike =
                0.20 * (1 - languageComplexity) +
                        0.15 * (1 - burstiness) +
                        0.15 * (1 - topicEntropy) +
                        0.20 * (1 - reasoningComplexity) +
                        0.10 * (1 - emotionVariance) +
                        0.20 * (templateSimilarityAiLike); // 模板相似度本身越高越像 AI

        aiLike = clamp01(aiLike);

        AiAnalysisResult2 result = new AiAnalysisResult2();
        result.setOverallAiRate(aiLike);
        result.setFeatures(features);
        return result;
    }

    // ============================ 工具方法 ============================

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }

    private List<String> splitToSentences(String text) {
        if (text.isEmpty()) return Collections.emptyList();
        String[] arr = text.split("[。！？!?.\\n]");
        return Arrays.stream(arr)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
    }

    private double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    // 软归一化：x / (x + c)，避免极端 0/1
    private double smoothRatio(double x, double c) {
        if (x <= 0) return 0.0;
        return x / (x + c);
    }

    // logistic 平滑：用于像“熵”这种左右都有合理区间的情况
    private double logistic(double x, double mid, double k) {
        double z = -k * (x - mid);
        return 1.0 / (1.0 + Math.exp(z));
    }

    // ====================== ① 语言复杂度（4-gram 熵，logistic 平滑） ======================

    private double computeLanguageComplexity(String text) {
        if (text.length() < 12) return 0.5; // 太短，不好判断

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

        // 经验：日文/中文类文本 4-gram 熵大概在 [1.5, 4.5] 左右
        // mid = 2.8，k = 1.0，使得中间值附近比较敏感，两侧渐近 0/1 但不极端
        double score = logistic(entropy, 2.8, 1.0);
        return clamp01(score);
    }

    private String explainLanguageComplexity(double s) {
        if (s < 0.3) return "语言模式较为单一、可预测性强，存在一定模板化或 AI 特征。";
        if (s < 0.7) return "语言复杂度适中，整体自然。";
        return "语言变化较丰富，多样性强，更接近人类写作。";
    }

    // ====================== ② 句式波动（标准差 / 均值，平滑映射） ======================

    private double computeBurstiness(List<String> sentences) {
        if (sentences.isEmpty()) return 0.5;

        List<Integer> lens = sentences.stream().map(String::length).collect(Collectors.toList());
        double mean = lens.stream().mapToDouble(i -> i).average().orElse(0.0);
        double var = lens.stream().mapToDouble(i -> (i - mean) * (i - mean)).average().orElse(0.0);
        double std = Math.sqrt(var);

        if (mean <= 0) return 0.5;

        double ratio = std / mean; // std/mean，越大说明句长波动越大
        // 一般 0~1 比较常见，这里用 smoothRatio 映射，避免直接 0/1
        double score = smoothRatio(ratio, 0.5); // c=0.5 可调
        return clamp01(score);
    }

    private String explainBurstiness(double s) {
        if (s < 0.3) return "句子长度较为均匀，节奏平整，有一定 AI 风格。";
        if (s < 0.7) return "句子长短有一定变化，阅读节奏自然。";
        return "句式长短波动明显，写作风格较为自由，偏人类写作。";
    }

    // ====================== ③ 主题熵（2-gram 熵，logistic 平滑） ======================

    private double computeTopicEntropy(String text) {
        if (text.length() < 5) return 0.5;

        Map<String, Integer> freq = new HashMap<>();
        int total = 0;

        for (int i = 0; i < text.length() - 1; i++) {
            String bg = text.substring(i, i + 2);
            freq.merge(bg, 1, Integer::sum);
            total++;
        }
        if (total == 0) return 0.5;

        double entropy = 0.0;
        for (int c : freq.values()) {
            double p = (double) c / total;
            entropy += -p * Math.log(p);
        }

        // 经验：2-gram 熵大多在 [0.5, 3.0] 左右
        double score = logistic(entropy, 1.8, 1.2); // mid=1.8,k=1.2
        return clamp01(score);
    }

    private String explainTopicEntropy(double s) {
        if (s < 0.3) return "表达模式较为单一，重复度偏高，可能有模板化倾向。";
        if (s < 0.7) return "表达多样性适中，整体自然。";
        return "表达和用词较为丰富，内容信息多样。";
    }

    // ====================== ④ 推理复杂度（逻辑词密度，平滑） ======================

    private double computeReasoningComplexity(List<String> sentences) {
        if (sentences.isEmpty()) return 0.5;

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

        if (totalChars == 0) return 0.5;

        double density = (double) logicCount / totalChars; // 每字符逻辑词密度

        // 典型志望理由书逻辑词密度一般在 0 ~ 0.01 之间
        // 用 smoothRatio 软映射，C 取 0.003 左右
        double score = smoothRatio(density, 0.003);
        // 再稍微往中间偏一点，避免大部分都是接近 0
        double adjusted = 0.3 + 0.7 * score; // 把 [0,1] 映射到 [0.3,1]
        return clamp01(adjusted);
    }

    private String explainReasoningComplexity(double s) {
        if (s < 0.4) return "逻辑连接较少，推理结构偏线性，建议适当增加因果、转折等逻辑衔接。";
        if (s < 0.75) return "逻辑结构较为清晰，存在一定的因果和转折关系。";
        return "逻辑连接较为丰富，结构多层，推理性较强，类似熟练写作。";
    }

    // ====================== ⑤ 情绪起伏（方差 + 平滑） ======================

    private double computeEmotionVariance(List<String> sentences) {
        if (sentences.isEmpty()) return 0.5;

        List<Double> emoScores = new ArrayList<>();

        for (String s : sentences) {
            int pos = 0, neg = 0;
            for (String w : JP_POSITIVE) {
                if (s.contains(w)) pos++;
            }
            for (String w : JP_NEGATIVE) {
                if (s.contains(w)) neg++;
            }
            // 情绪分：正数偏积极，负数偏消极，0 表示中性
            emoScores.add((double) (pos - neg));
        }

        double mean = emoScores.stream().mapToDouble(d -> d).average().orElse(0.0);
        double var = emoScores.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(0.0);

        // 方差一般在 0~2 区间内，这里用 smoothRatio 映射
        double scoreRaw = smoothRatio(var, 0.8); // C=0.8，让小波动也有一点分数
        // 再做一个中性偏移，使“几乎没情绪波动”的情况接近 0.4~0.5，而不是 0
        double adjusted = 0.3 + 0.7 * scoreRaw; // [0,1] -> [0.3,1]
        return clamp01(adjusted);
    }

    private String explainEmotionVariance(double s) {
        if (s < 0.4) return "情绪表达较为平直，主观色彩较弱，略显理性。";
        if (s < 0.75) return "情绪表达有一定起伏，整体较自然。";
        return "情绪起伏较大，主观色彩明显，具有较强个人表达。";
    }

    // ====================== ⑥ 模板相似度（3-gram + 余弦，相似度本身用于 AI 率） ======================

    private double computeTemplateSimilarity(String text) {
        Map<String, Integer> docVec = buildCharNgramVector(text, 3);
        if (docVec.isEmpty()) return 0.0;

        double maxSim = 0.0;
        for (String tpl : templateTexts) {
            Map<String, Integer> tplVec = buildCharNgramVector(tpl, 3);
            double sim = cosineSimilarity(docVec, tplVec);
            if (sim > maxSim) {
                maxSim = sim;
            }
        }

        // 相似度一般在 0~0.6 左右，直接返回（AI-like），外面会做 clamp
        return clamp01(maxSim);
    }

    private Map<String, Integer> buildCharNgramVector(String text, int n) {
        Map<String, Integer> map = new HashMap<>();
        if (text.length() < n) return map;
        for (int i = 0; i <= text.length() - n; i++) {
            String gram = text.substring(i, i + n);
            map.merge(gram, 1, Integer::sum);
        }
        return map;
    }

    private double cosineSimilarity(Map<String, Integer> v1, Map<String, Integer> v2) {
        Set<String> keys = new HashSet<>();
        keys.addAll(v1.keySet());
        keys.addAll(v2.keySet());

        double dot = 0.0;
        double n1 = 0.0;
        double n2 = 0.0;

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
        // 注意：传进来的是 “越高越像人类” 的值 = 1 - 原始相似度
        if (humanScore < 0.4) {
            return "与常见范文/模板相似度偏高，存在较明显的模板化痕迹。";
        } else if (humanScore < 0.75) {
            return "与常见范文有一定相似，但整体仍保持一定个性。";
        } else {
            return "与内置模板相似度较低，模板化特征不明显。";
        }
    }
}
