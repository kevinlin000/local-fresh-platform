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

        List<String> simplifiedTerms = List.of(
                "运营数据报表",
                "概览数据",
                "明细数据",
                "营业额",
                "有效订单",
                "订单完成率",
                "平均客单价",
                "新增用户数"
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
