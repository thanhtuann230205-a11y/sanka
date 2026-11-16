package vn.sanka.lms;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReadinessController {
 // Public, read-only probe for the separate static loading page. No cookies or user data.
 @CrossOrigin(origins = "*")
 @GetMapping("/api/readiness")
 public ResponseEntity<Map<String,String>> ready() {
  return ResponseEntity.ok().cacheControl(CacheControl.noStore())
   .body(Map.of("application", "sanka", "status", "ready"));
 }
}
