package vn.sanka.lms.chat;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;

class GeminiChatServiceTest {
 @Test void sendsKeyOnlyInHeaderAndReturnsText(){
  var builder=RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
  var server=MockRestServiceServer.bindTo(builder).build();
  server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent"))
   .andExpect(method(HttpMethod.POST)).andExpect(header("x-goog-api-key","test-secret"))
   .andExpect(jsonPath("$.contents[0].role").value("user"))
   .andExpect(jsonPath("$.contents[1].parts[0].text").value("学校 là gì?"))
   .andExpect(jsonPath("$.systemInstruction.parts[0].text").exists())
   .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"学校 (がっこう): trường học.\"}]}}]}",MediaType.APPLICATION_JSON));
  var service=new GeminiChatService(builder.build(),"test-secret","test-model");
  assertThat(service.answer(new GeminiChatService.Question("学校 là gì?",List.of(new GeminiChatService.Message("user","Xin chào")),"vi")))
   .isEqualTo("学校 (がっこう): trường học.");server.verify();
 }
 @Test void missingKeyDoesNotCallProvider(){
  var service=new GeminiChatService(RestClient.create(),"","test-model");
  assertThat(service.enabled()).isFalse();
  assertThatThrownBy(()->service.answer(new GeminiChatService.Question("Xin chào",null,"vi")))
   .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
 }
 @Test void quotaErrorIsSafeAndReadable(){
  var builder=RestClient.builder().baseUrl("https://example.test");var server=MockRestServiceServer.bindTo(builder).build();
  server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("private-provider-details"));
  var service=new GeminiChatService(builder.build(),"test-secret","test-model");
  assertThatThrownBy(()->service.answer(new GeminiChatService.Question("Xin chào",null,"vi")))
   .isInstanceOfSatisfying(ResponseStatusException.class,e->{assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);assertThat(e.getReason()).doesNotContain("private-provider-details","test-secret");});server.verify();
 }
}
