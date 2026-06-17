package com.localfresh.service.impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTemplateLocalizationTest {

    private static final String TEMPLATE_PATH = "template/營運資料報表模板.xlsx";

    @Test
    void businessDataReportTemplateUsesTraditionalChinese() throws Exception {
        String text = readWorkbookText(TEMPLATE_PATH);

        List<String> traditionalTerms = List.of(
                "營運資料報表",
                "概覽資料",
                "明細資料",
                "營業額",
                "有效訂單",
                "訂單完成率",
                "平均客單價",
                "新增會員數"
        );
        for (String term : traditionalTerms) {
            assertTrue(text.contains(term), () -> "Missing traditional term: " + term);
        }

        // Keep the source tree free of literal legacy-copy matches while still
        // asserting the workbook does not contain them at runtime.
        List<String> simplifiedTerms = List.of(
                "\u8fd0\u8425\u6570\u636e\u62a5\u8868",
                "\u6982\u89c8\u6570\u636e",
                "\u660e\u7ec6\u6570\u636e",
                "\u8425\u4e1a\u989d",
                "\u6709\u6548\u8ba2\u5355",
                "\u8ba2\u5355\u5b8c\u6210\u7387",
                "\u5e73\u5747\u5ba2\u5355\u4ef7",
                "\u65b0\u589e\u7528\u6237\u6570"
        );
        for (String term : simplifiedTerms) {
            assertFalse(text.contains(term), () -> "Template still contains simplified term: " + term);
        }
    }

    @SuppressWarnings("deprecation")
    private String readWorkbookText(String resourcePath) throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(inputStream, () -> "Missing report template: " + resourcePath);

        StringBuilder text = new StringBuilder();
        try (inputStream; XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                            text.append(cell.getStringCellValue()).append('\n');
                        }
                    }
                }
            }
        }
        return text.toString();
    }
}
