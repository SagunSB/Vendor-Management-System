package com.civwbms.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExportService {
  public byte[] pdf(String title, List<Map<String, Object>> rows) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document doc = new Document();
    PdfWriter.getInstance(doc, out);
    doc.open();
    doc.add(new Paragraph(title));
    doc.add(new Paragraph(" "));
    if (!rows.isEmpty()) {
      PdfPTable table = new PdfPTable(rows.getFirst().keySet().size());
      rows.getFirst().keySet().forEach(k -> table.addCell(k.replace("_", " ").toUpperCase()));
      rows.forEach(r -> r.values().forEach(v -> table.addCell(v == null ? "" : String.valueOf(v))));
      doc.add(table);
    }
    doc.close();
    return out.toByteArray();
  }

  public byte[] excel(List<Map<String, Object>> rows) throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      var sheet = wb.createSheet("Report");
      if (!rows.isEmpty()) {
        Row h = sheet.createRow(0);
        int c = 0;
        for (String k : rows.getFirst().keySet()) h.createCell(c++).setCellValue(k);
        int rnum = 1;
        for (Map<String, Object> row : rows) {
          Row r = sheet.createRow(rnum++);
          c = 0;
          for (Object v : row.values()) r.createCell(c++).setCellValue(v == null ? "" : String.valueOf(v));
        }
      }
      wb.write(out);
      return out.toByteArray();
    }
  }
}
