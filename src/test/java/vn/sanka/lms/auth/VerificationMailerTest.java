package vn.sanka.lms.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class VerificationMailerTest {
 @Test void sendsMailjetMessageWithBasicAuthentication(){
  var builder=RestClient.builder().baseUrl("https://api.mailjet.com");
  var server=MockRestServiceServer.bindTo(builder).build();
  String auth="Basic "+Base64.getEncoder().encodeToString("fake-key:fake-secret".getBytes(StandardCharsets.ISO_8859_1));
  server.expect(requestTo("https://api.mailjet.com/v3.1/send")).andExpect(method(HttpMethod.POST))
   .andExpect(header("Authorization",auth)).andExpect(headerDoesNotExist("api-key"))
   .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
   .andExpect(jsonPath("$.Messages[0].From.Email").value("sender@example.test"))
   .andExpect(jsonPath("$.Messages[0].To[0].Email").value("recipient@example.test"))
   .andExpect(jsonPath("$.Messages[0].Subject").value("Mã xác nhận đăng ký Sanka"))
   .andExpect(jsonPath("$.Messages[0].TextPart").value(org.hamcrest.Matchers.containsString("123456")))
   .andRespond(withSuccess("{\"Messages\":[{\"Status\":\"success\"}]}",MediaType.APPLICATION_JSON));
  new VerificationMailer(builder.build()," fake-key "," fake-secret "," sender@example.test ").send("recipient@example.test","123456");
  server.verify();
 }
 @Test void missingConfigurationNeverMakesRequest(){
  var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
  assertThatThrownBy(()->new VerificationMailer(builder.build(),"fake-key","","sender@example.test").send("recipient@example.test","123456"))
   .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getReason()).contains("MAILJET_SECRET_KEY"));
  server.verify();
 }
 @ParameterizedTest @ValueSource(strings={"{\"Messages\":[{\"Status\":\"error\",\"Errors\":[{\"ErrorMessage\":\"private-details 123456\"}]}]}","{\"Messages\":[]}","{}"})
 void rejectsMessageLevelErrorsEvenWithHttp200(String body){
  var builder=RestClient.builder().baseUrl("https://api.mailjet.com");var server=MockRestServiceServer.bindTo(builder).build();
  server.expect(anything()).andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
  assertThatThrownBy(()->new VerificationMailer(builder.build(),"fake-key","fake-secret","sender@example.test").send("recipient@example.test","123456"))
   .isInstanceOfSatisfying(ResponseStatusException.class,e->{assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);assertThat(e.getReason()).doesNotContain("private-details","123456","fake-secret");});
  server.verify();
 }
 @ParameterizedTest @ValueSource(ints={401,403,429,500})
 void handlesProviderErrorsWithoutExposingCredentials(int status){
  var builder=RestClient.builder().baseUrl("https://api.mailjet.com");var server=MockRestServiceServer.bindTo(builder).build();
  server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status)).body("private-details fake-secret 123456"));
  assertThatThrownBy(()->new VerificationMailer(builder.build(),"fake-key","fake-secret","sender@example.test").send("recipient@example.test","123456"))
   .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getReason()).doesNotContain("private-details","fake-secret","123456"));
  server.verify();
 }
}
