package com.civwbms.controller;

import com.civwbms.service.CoreService;
import com.civwbms.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
  private final CoreService core;
  private final ExportService export;

  public ReportController(CoreService core, ExportService export) {
    this.core = core;
    this.export = export;
  }

  @GetMapping("/{name}")
  public Object rows(@PathVariable String name) {
    return core.reportRows(name);
  }

  @GetMapping("/{name}/pdf")
  public ResponseEntity<byte[]> pdf(@PathVariable String name) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + name + ".pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(export.pdf("CIVWBMS " + name + " Report", core.reportRows(name)));
  }

  @GetMapping("/{name}/excel")
  public ResponseEntity<byte[]> excel(@PathVariable String name) throws Exception {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + name + ".xlsx")
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(export.excel(core.reportRows(name)));
  }
}
