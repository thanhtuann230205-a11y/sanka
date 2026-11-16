package vn.sanka.lms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReadinessControllerTest {
 @Autowired MockMvc mvc;

 @Test void staticSiteCanProbeWithoutLoginOrCookies() throws Exception {
  mvc.perform(get("/api/readiness").header("Origin", "https://sanka-loading.onrender.com"))
   .andExpect(status().isOk())
   .andExpect(header().string("Access-Control-Allow-Origin", "*"))
   .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"))
   .andExpect(header().string("Cache-Control", "no-store"))
   .andExpect(jsonPath("$.application").value("sanka"))
   .andExpect(jsonPath("$.status").value("ready"));
 }

 @Test void probeDoesNotExposeProtectedPages() throws Exception {
  mvc.perform(get("/start")).andExpect(status().is3xxRedirection());
 }
}
