package com.civwbms.service;

import io.jsonwebtoken.Claims;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CoreService {
  private final JdbcTemplate jdbc;

  public CoreService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> subsidiaries() {
    return jdbc.queryForList("SELECT * FROM subsidiaries ORDER BY id");
  }

  public Map<String, Object> dashboard(Claims user) {
    String role = String.valueOf(user.get("role"));
    Long sub = user.get("subsidiaryId", Long.class);
    Map<String, Object> m = new LinkedHashMap<>();
    if ("EMPLOYEE".equals(role)) {
      long id = user.get("userId", Number.class).longValue();
      m.put("attendance", count("attendance WHERE employee_id=?", id));
      m.put("leaves", count("leave_applications WHERE employee_id=?", id));
      m.put("scholarships", count("scholarships WHERE employee_id=?", id));
      m.put("notifications", count("notifications WHERE recipient_role='EMPLOYEE' AND recipient_id=? AND is_read=0", id));
    } else if ("CLIENT".equals(role)) {
      long id = user.get("userId", Number.class).longValue();
      m.put("orders", count("coal_orders WHERE client_id=?", id));
      m.put("pending", count("coal_orders WHERE client_id=? AND status='Pending'", id));
      m.put("approved", count("coal_orders WHERE client_id=? AND status IN ('Approved','Processing','Dispatched','Delivered')", id));
      m.put("notifications", count("notifications WHERE recipient_role='CLIENT' AND recipient_id=? AND is_read=0", id));
    } else {
      Object[] args = "HQ".equals(role) ? new Object[]{} : new Object[]{sub};
      String f = "HQ".equals(role) ? "" : " WHERE subsidiary_id=?";
      m.put("employees", count("employees" + f, args));
      m.put("attendance", count("attendance a JOIN employees e ON e.id=a.employee_id" + ("HQ".equals(role) ? " WHERE a.status='Approved'" : " WHERE e.subsidiary_id=?"), args));
      m.put("leaves", count("leave_applications l JOIN employees e ON e.id=l.employee_id" + ("HQ".equals(role) ? " WHERE l.status='Approved'" : " WHERE e.subsidiary_id=?"), args));
      m.put("inventoryTonnes", jdbc.queryForObject("SELECT COALESCE(SUM(available_quantity),0) FROM coal_inventory" + f, BigDecimal.class, args));
      m.put("orders", count("coal_orders o" + ("HQ".equals(role) ? "" : " WHERE o.subsidiary_id=?"), args));
      m.put("revenue", jdbc.queryForObject("SELECT COALESCE(SUM(total_amount),0) FROM coal_orders" + ("HQ".equals(role) ? " WHERE status IN ('Approved','Processing','Dispatched','Delivered')" : " WHERE subsidiary_id=? AND status IN ('Approved','Processing','Dispatched','Delivered')"), BigDecimal.class, args));
    }
    m.put("charts", charts(sub, role));
    return m;
  }

  public List<Map<String, Object>> list(String module, Claims user) {
    long id = user.get("userId", Number.class).longValue();
    String role = String.valueOf(user.get("role"));
    Long sub = user.get("subsidiaryId", Long.class);
    return switch (module) {
      case "attendance" -> "EMPLOYEE".equals(role)
          ? jdbc.queryForList("SELECT * FROM attendance WHERE employee_id=? ORDER BY attendance_date DESC", id)
          : jdbc.queryForList("SELECT a.*, e.full_name, e.employee_code FROM attendance a JOIN employees e ON e.id=a.employee_id WHERE e.subsidiary_id=? ORDER BY a.created_at DESC", sub);
      case "leave" -> employeeScoped("leave_applications", "l", role, id, sub);
      case "scholarship" -> employeeScoped("scholarships", "s", role, id, sub);
      case "buscard" -> employeeScoped("bus_cards", "b", role, id, sub);
      case "pension" -> employeeScoped("pensions", "p", role, id, sub);
      case "inventory" -> jdbc.queryForList("SELECT c.*, s.code subsidiary_code, s.name subsidiary_name FROM coal_inventory c JOIN subsidiaries s ON s.id=c.subsidiary_id ORDER BY c.updated_at DESC");
      case "orders" -> "CLIENT".equals(role)
          ? jdbc.queryForList("SELECT o.*, s.code subsidiary_code FROM coal_orders o JOIN subsidiaries s ON s.id=o.subsidiary_id WHERE o.client_id=? ORDER BY o.created_at DESC", id)
          : jdbc.queryForList("SELECT o.*, c.company_name, s.code subsidiary_code FROM coal_orders o JOIN clients c ON c.id=o.client_id JOIN subsidiaries s ON s.id=o.subsidiary_id " + ("HQ".equals(role) ? "" : "WHERE o.subsidiary_id=? ") + "ORDER BY o.created_at DESC", "HQ".equals(role) ? new Object[]{} : new Object[]{sub});
      case "notifications" -> jdbc.queryForList("SELECT * FROM notifications WHERE recipient_role=? AND recipient_id=? ORDER BY created_at DESC LIMIT 30", role, id);
      default -> throw new IllegalArgumentException("Unknown module");
    };
  }

  public Map<String, Object> create(String module, Map<String, Object> b, Claims user) {
    long id = user.get("userId", Number.class).longValue();
    switch (module) {
      case "attendance" -> jdbc.update("INSERT INTO attendance(employee_id, attendance_date, in_time, out_time, status) VALUES(?,?,?,?, 'Pending')", id, Date.valueOf(str(b, "attendanceDate")), Time.valueOf(str(b, "inTime") + ":00"), Time.valueOf(str(b, "outTime") + ":00"));
      case "leave" -> jdbc.update("INSERT INTO leave_applications(employee_id, leave_type, from_date, to_date, reason, document_path, status) VALUES(?,?,?,?,?,?, 'Pending')", id, str(b, "leaveType"), Date.valueOf(str(b, "fromDate")), Date.valueOf(str(b, "toDate")), str(b, "reason"), str(b, "documentPath"));
      case "scholarship" -> jdbc.update("INSERT INTO scholarships(employee_id, student_name, relationship, school_college, class_name, marks, annual_income, document_path, status) VALUES(?,?,?,?,?,?,?,?, 'Pending')", id, str(b, "studentName"), str(b, "relationship"), str(b, "schoolCollege"), str(b, "className"), dec(b, "marks"), dec(b, "annualIncome"), str(b, "documentPath"));
      case "buscard" -> jdbc.update("INSERT INTO bus_cards(employee_id, student_name, school_name, route, address, photo_path, status) VALUES(?,?,?,?,?,?, 'Pending')", id, str(b, "studentName"), str(b, "schoolName"), str(b, "route"), str(b, "address"), str(b, "photoPath"));
      case "pension" -> jdbc.update("INSERT INTO pensions(employee_id, retirement_date, service_years, last_designation, document_path, status) VALUES(?,?,?,?,?, 'Pending')", id, Date.valueOf(str(b, "retirementDate")), Integer.parseInt(str(b, "serviceYears")), str(b, "lastDesignation"), str(b, "documentPath"));
      case "inventory" -> jdbc.update("INSERT INTO coal_inventory(subsidiary_id, area, colliery, coal_grade, coal_quality, available_quantity, price_per_tonne) VALUES(?,?,?,?,?,?,?)", num(b, "subsidiaryId"), str(b, "area"), str(b, "colliery"), str(b, "coalGrade"), str(b, "coalQuality"), dec(b, "availableQuantity"), dec(b, "pricePerTonne"));
      case "orders" -> {
        Map<String, Object> inv = jdbc.queryForMap("SELECT * FROM coal_inventory WHERE id=?", num(b, "inventoryId"));
        BigDecimal qty = dec(b, "quantityRequired");
        BigDecimal price = new BigDecimal(String.valueOf(inv.get("price_per_tonne")));
        jdbc.update("INSERT INTO coal_orders(client_id, inventory_id, subsidiary_id, area, colliery, coal_grade, quantity_required, delivery_address, gst_number, price_per_tonne, total_amount, status) VALUES(?,?,?,?,?,?,?,?,?,?,?, 'Pending')", id, num(b, "inventoryId"), inv.get("subsidiary_id"), inv.get("area"), inv.get("colliery"), inv.get("coal_grade"), qty, str(b, "deliveryAddress"), str(b, "gstNumber"), price, price.multiply(qty));
      }
      default -> throw new IllegalArgumentException("Unknown module");
    }
    return Map.of("message", "Saved successfully");
  }

  public Map<String, Object> update(String module, long recordId, Map<String, Object> b, Claims user) {
    if ("inventory".equals(module)) {
      jdbc.update("UPDATE coal_inventory SET subsidiary_id=?, area=?, colliery=?, coal_grade=?, coal_quality=?, available_quantity=?, price_per_tonne=? WHERE id=?", num(b, "subsidiaryId"), str(b, "area"), str(b, "colliery"), str(b, "coalGrade"), str(b, "coalQuality"), dec(b, "availableQuantity"), dec(b, "pricePerTonne"), recordId);
      return Map.of("message", "Inventory updated");
    }
    String status = str(b, "status");
    String remarks = str(b, "remarks");
    String table = switch (module) {
      case "attendance" -> "attendance";
      case "leave" -> "leave_applications";
      case "scholarship" -> "scholarships";
      case "buscard" -> "bus_cards";
      case "pension" -> "pensions";
      case "orders" -> "coal_orders";
      default -> throw new IllegalArgumentException("Unknown module");
    };
    if ("orders".equals(module)) {
      jdbc.update("UPDATE coal_orders SET status=?, admin_remarks=? WHERE id=?", status, remarks, recordId);
      jdbc.update("INSERT INTO order_tracking(order_id, status, remarks) VALUES(?,?,?)", recordId, status, remarks);
      Map<String, Object> order = jdbc.queryForMap("SELECT client_id FROM coal_orders WHERE id=?", recordId);
      notify("CLIENT", ((Number) order.get("client_id")).longValue(), "Coal order " + status, "Your order #" + recordId + " is now " + status + ".");
    } else {
      jdbc.update("UPDATE " + table + " SET status=?, admin_remarks=? WHERE id=?", status, remarks, recordId);
      Map<String, Object> rec = jdbc.queryForMap("SELECT employee_id FROM " + table + " WHERE id=?", recordId);
      notify("EMPLOYEE", ((Number) rec.get("employee_id")).longValue(), module + " " + status, "Your " + module + " request was " + status + ".");
    }
    return Map.of("message", "Workflow updated");
  }

  public Map<String, Object> delete(String module, long id) {
    if (!"inventory".equals(module)) throw new IllegalArgumentException("Delete is enabled for inventory only");
    jdbc.update("DELETE FROM coal_inventory WHERE id=?", id);
    return Map.of("message", "Deleted");
  }

  public List<Map<String, Object>> tracking(long orderId) {
    return jdbc.queryForList("SELECT * FROM order_tracking WHERE order_id=? ORDER BY created_at", orderId);
  }

  public void markNotificationsRead(Claims user) {
    jdbc.update("UPDATE notifications SET is_read=1 WHERE recipient_role=? AND recipient_id=?", user.get("role"), user.get("userId"));
  }

  public List<Map<String, Object>> reportRows(String report) {
    return switch (report) {
      case "approved-welfare" -> jdbc.queryForList("""
          SELECT 'Leave' type, e.employee_code, e.full_name, l.status, l.created_at FROM leave_applications l JOIN employees e ON e.id=l.employee_id WHERE l.status='Approved'
          UNION ALL SELECT 'Scholarship', e.employee_code, e.full_name, s.status, s.created_at FROM scholarships s JOIN employees e ON e.id=s.employee_id WHERE s.status='Approved'
          UNION ALL SELECT 'Bus Card', e.employee_code, e.full_name, b.status, b.created_at FROM bus_cards b JOIN employees e ON e.id=b.employee_id WHERE b.status='Approved'
          UNION ALL SELECT 'Pension', e.employee_code, e.full_name, p.status, p.created_at FROM pensions p JOIN employees e ON e.id=p.employee_id WHERE p.status='Approved'
          ORDER BY created_at DESC
          """);
      case "orders" -> jdbc.queryForList("SELECT o.id order_id, c.company_name, s.code subsidiary, o.coal_grade, o.quantity_required, o.status, o.total_amount FROM coal_orders o JOIN clients c ON c.id=o.client_id JOIN subsidiaries s ON s.id=o.subsidiary_id ORDER BY o.created_at DESC");
      default -> jdbc.queryForList("SELECT s.code subsidiary, area, colliery, coal_grade, coal_quality, available_quantity, price_per_tonne FROM coal_inventory c JOIN subsidiaries s ON s.id=c.subsidiary_id ORDER BY s.code, area");
    };
  }

  private List<Map<String, Object>> employeeScoped(String table, String alias, String role, long id, Long sub) {
    if ("EMPLOYEE".equals(role)) return jdbc.queryForList("SELECT * FROM " + table + " WHERE employee_id=? ORDER BY created_at DESC", id);
    return jdbc.queryForList("SELECT " + alias + ".*, e.full_name, e.employee_code FROM " + table + " " + alias + " JOIN employees e ON e.id=" + alias + ".employee_id WHERE e.subsidiary_id=? ORDER BY " + alias + ".created_at DESC", sub);
  }

  private Map<String, Object> charts(Long sub, String role) {
    Object[] args = "HQ".equals(role) ? new Object[]{} : new Object[]{sub};
    String f = "HQ".equals(role) ? "" : " WHERE subsidiary_id=?";
    return Map.of(
        "quality", jdbc.queryForList("SELECT coal_quality label, SUM(available_quantity) value FROM coal_inventory" + f + " GROUP BY coal_quality", args),
        "orders", jdbc.queryForList("SELECT status label, COUNT(*) value FROM coal_orders" + f + " GROUP BY status", args),
        "monthly", jdbc.queryForList("SELECT DATE_FORMAT(created_at,'%Y-%m') label, COUNT(*) value FROM coal_orders" + f + " GROUP BY DATE_FORMAT(created_at,'%Y-%m') ORDER BY label", args));
  }

  private long count(String from, Object... args) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + from, Long.class, args);
  }

  private void notify(String role, long id, String title, String message) {
    jdbc.update("INSERT INTO notifications(recipient_role, recipient_id, title, message) VALUES(?,?,?,?)", role, id, title, message);
  }

  private String str(Map<String, Object> b, String key) {
    Object v = b.get(key);
    return v == null ? "" : String.valueOf(v).trim();
  }

  private long num(Map<String, Object> b, String key) {
    return Long.parseLong(str(b, key));
  }

  private BigDecimal dec(Map<String, Object> b, String key) {
    return new BigDecimal(str(b, key));
  }
}
