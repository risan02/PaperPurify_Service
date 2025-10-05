package com.sunnyday.lychat.controller;

import com.sunnyday.lychat.service.ConsultantService;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import reactor.core.publisher.Flux;

@RestController
//@RequestMapping("/api")
public class ChatController {
    @Autowired
    private ConsultantService consultantService;

    @GetMapping(value = "/chatTest")
    public String chatTest() {
        return "ni hao ! hello !";
    }

    @PostMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(
            @RequestParam("memoryId") String memoryId,
            @RequestParam("message") String message,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        
        // 构建包含文件内容的提示信息
        String fileContent = "";
        
        if (files != null && files.length > 0) {
            fileContent = "\n\n用户上传了以下文件:\n";
            
            for (MultipartFile file : files) {
                // TODO: 这里需要添加实际的文件内容读取逻辑
                // 根据文件类型使用相应的库读取内容
                // 例如使用Apache PDFBox读取PDF文件，Apache POI读取Word文件等
                
                fileContent += "文件名: " + file.getOriginalFilename() + "\n";
                fileContent += "文件大小: " + file.getSize() + " bytes\n\n";
                
                // 这里可以添加文件内容解析逻辑
                 String content = readFileContent(file);
                 fileContent += "文件内容:\n" + content + "\n\n";
            }
        }
        
        // 将文件信息添加到消息中
        String finalFileContent = fileContent;
        return consultantService.chat(memoryId, message + finalFileContent);
    }

    @GetMapping(value = "/chat2", produces = "text/html;charset=utf-8")
    public Flux<String> chat2(String memoryId, String message) {
        return null; // consultantService.chat2(memoryId,message);
    }

    // 文件内容读取方法
    private String readFileContent(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String fileExtension = "";
            
            if (fileName != null && fileName.contains(".")) {
                fileExtension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
            }
            
            switch (fileExtension) {
                case ".txt":
                    // 处理文本文件，自动检测编码
                    return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                
                case ".pdf":
                    // 处理PDF文件
                    return extractTextFromPDF(file);
                
                case ".doc":
                    // 处理旧版Word文档（.doc）
                    return extractTextFromDOC(file);
                
                case ".docx":
                    // 处理新版Word文档（.docx）
                    return extractTextFromDOCX(file);
                
                default:
                    return "不支持的文件类型: " + fileExtension + "。支持的文件类型：txt、pdf、doc、docx";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "文件读取失败: " + e.getMessage();
        }
    }

    // PDF文件解析方法
    private String extractTextFromPDF(MultipartFile file) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(file.getInputStream())) {
            org.apache.pdfbox.text.PDFTextStripper pdfStripper = new org.apache.pdfbox.text.PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    // DOC文件解析方法
    private String extractTextFromDOC(MultipartFile file) throws Exception {
        try (org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(file.getInputStream())) {
            return doc.getDocumentText();
        }
    }

    // DOCX文件解析方法
    private String extractTextFromDOCX(MultipartFile file) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(file.getInputStream())) {
            org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(doc);
            return extractor.getText();
        }
    }
}