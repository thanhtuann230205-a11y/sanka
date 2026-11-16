package vn.sanka.lms.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class VerificationMailerTest {
 private static final String TOKEN="https://oauth2.googleapis.com/token";
 private static final String SEND="https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
 private VerificationMailer mailer(RestClient.Builder builder){
  return new VerificationMailer(builder.build()," client-id "," client-secret "," refresh-token "," sender@gmail.com ");
 }
 private void token(MockRestServiceServer server){
  server.expect(requestTo(TOKEN)).andExpect(method(HttpMethod.POST))
   .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
   .andExpect(content().string("client_id=client-id&client_secret=client-secret&refresh_token=refresh-token&grant_type=refresh_token"))
   .andRespond(withSuccess("{\"access_token\":\"access-token\"}",MediaType.APPLICATION_JSON));
 }
 @Test void refreshesTokenAndSendsUtf8MimeMessage(){
  var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();token(server);
  server.expect(requestTo(SEND)).andExpect(method(HttpMethod.POST))
   .andExpect(header("Authorization","Bearer access-token"))
   .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
   .andExpect(request->{
    String raw=new ObjectMapper().readTree(((MockClientHttpRequest)request).getBodyAsString()).path("raw").asText();
    String mime=new String(Base64.getUrlDecoder().decode(raw),StandardCharsets.UTF_8);
    assertThat(mime).contains("From: Sanka <sender@gmail.com>\r\n","To: recipient@example.test\r\n");
    String encodedSubject=mime.split("Subject: =\\?UTF-8\\?B\\?")[1].split("\\?=")[0];
    assertThat(new String(Base64.getDecoder().decode(encodedSubject),StandardCharsets.UTF_8)).isEqualTo("Mã xác nhận đăng ký Sanka");
    String body=new String(Base64.getMimeDecoder().decode(mime.split("\r\n\r\n",2)[1]),StandardCharsets.UTF_8);
    assertThat(body).contains("Mã xác nhận đăng ký Sanka của bạn: 123456","10 phút");
    assertThat(mime).doesNotContain("client-secret","refresh-token","access-token");
   }).andRespond(withSuccess("{\"id\":\"sent-id\"}",MediaType.APPLICATION_JSON));
  mailer(builder).send("recipient@example.test","123456");server.verify();
 }
 @Test void missingConfigurationNeverMakesRequest(){
  var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
  assertThatThrownBy(()->new VerificationMailer(builder.build(),"id","secret","","sender@gmail.com").send("recipient@example.test","123456"))
   .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getReason()).contains("GMAIL_REFRESH_TOKEN"));
  server.verify();
 }
 @Test void rejectsHeaderInjectionBeforeNetworkRequest(){
  var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
  assertThatThrownBy(()->mailer(builder).send("recipient@example.test\r\nBcc: other@example.test","123456")).isInstanceOf(ResponseStatusException.class);
  server.verify();
 }
 @ParameterizedTest @ValueSource(strings={"{}","{\"access_token\":\"\"}","{\"access_token\":123}"})
 void rejectsMalformedTokenResponses(String body){
  var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
  server.expect(requestTo(TOKEN)).andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
  assertThatThrownBy(()->mailer(builder).send("recipient@example.test","123456")).isInstanceOf(ResponseStatusException.class);server.verify();
 }
 @ParameterizedTest @ValueSource(strings={"{}","{\"id\":\"\"}","{\"id\":123}"})
 void rejectsMalformedSendResponses(String body){
  var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();token(server);
  server.expect(requestTo(SEND)).andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
  assertThatThrownBy(()->mailer(builder).send("recipient@example.test","123456")).isInstanceOf(ResponseStatusException.class);server.verify();
 }
 @ParameterizedTest @ValueSource(ints={400,401,403,429,500})
 void handlesTokenErrorsWithoutExposingCredentials(int status){
  var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
  server.expect(requestTo(TOKEN)).andRespond(withStatus(HttpStatus.valueOf(status)).body("private-details client-secret refresh-token 123456"));
  assertSafeFailure(builder);server.verify();
 }
 @ParameterizedTest @ValueSource(ints={401,403,429,500})
 void handlesSendErrorsWithoutExposingCredentials(int status){
  var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();token(server);
  server.expect(requestTo(SEND)).andRespond(withStatus(HttpStatus.valueOf(status)).body("private-details access-token 123456"));
  assertSafeFailure(builder);server.verify();
 }
 private void assertSafeFailure(RestClient.Builder builder){
  assertThatThrownBy(()->mailer(builder).send("recipient@example.test","123456"))
   .isInstanceOfSatisfying(ResponseStatusException.class,e->{
    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(e.getReason()).doesNotContain("private-details","client-secret","refresh-token","access-token","123456");
   });
 }
}
