package com.moviebooking.service.ai.pipeline.transformer;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Utility làm sạch văn bản trước khi đưa vào pipeline AI.
 * Sử dụng JSoup (đã có trong project cho crawler) để strip HTML.
 */
@Component
public class TextCleaner {

    /**
     * Loại bỏ toàn bộ HTML tags, chuẩn hóa khoảng trắng,
     * và loại bỏ ký tự điều khiển.
     */
    public String cleanHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        // Strip tất cả HTML tags, giữ lại plain text
        String text = Jsoup.clean(html, Safelist.none());
        // Decode HTML entities (&amp; -> &, &lt; -> <, etc.)
        text = Jsoup.parse(text).text();
        return normalizeWhitespace(text);
    }

    /**
     * Chuẩn hóa khoảng trắng: loại bỏ khoảng trắng thừa,
     * giữ nguyên xuống dòng đơn giản.
     */
    public String normalizeWhitespace(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // Thay thế nhiều dòng trống liên tiếp bằng 1 dòng trống
        text = text.replaceAll("(\\r?\\n){3,}", "\n\n");
        // Loại bỏ khoảng trắng thừa trong mỗi dòng
        text = text.replaceAll("[ \\t]+", " ");
        // Trim từng dòng
        String[] lines = text.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.trim()).append("\n");
        }
        return sb.toString().trim();
    }
}
