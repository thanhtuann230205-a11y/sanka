package vn.sanka.lms.auth;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VerificationMailer {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(VerificationMailer.class);
 private final RestClient client;
 private final String clientId,clientSecret,refreshToken,sender;
 @Autowired
 public VerificationMailer(@Value("${sanka.email.client-id:}") String clientId,
                           @Value("${sanka.email.client-secret:}") String clientSecret,
                           @Value("${sanka.email.refresh-token:}") String refreshToken,
                           @Value("${sanka.email.sender:}") String sender){
  this(createClient(),clientId,clientSecret,refreshToken,sender);
 }
 VerificationMailer(RestClient client,String clientId,String clientSecret,String refreshToken,String sender){
  this.client=client;this.clientId=clean(clientId);this.clientSecret=clean(clientSecret);
  this.refreshToken=clean(refreshToken);this.sender=clean(sender);
 }
 private static String clean(String value){return value==null?"":value.strip();}
 private static RestClient createClient(){
  var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(10000);factory.setReadTimeout(20000);
  return RestClient.builder().requestFactory(factory).build();
 }
 public void send(String email,String code){
  if(clientId.isBlank()||clientSecret.isBlank()||refreshToken.isBlank()||sender.isBlank())
   throw unavailable("Chưa cấu hình gửi mã xác nhận. Quản trị viên cần thêm GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET, GMAIL_REFRESH_TOKEN và EMAIL_FROM.");
  // Addresses are inserted into MIME headers; accept only bare ASCII addresses.
  if(!validAddress(sender)||!validAddress(email))throw unavailable("Địa chỉ email gửi hoặc nhận không hợp lệ.");
  String stage="token";
  try{
   var form=new LinkedMultiValueMap<String,String>();
   form.add("client_id",clientId);form.add("client_secret",clientSecret);
   form.add("refresh_token",refreshToken);form.add("grant_type","refresh_token");
   var token=client.post().uri("https://oauth2.googleapis.com/token")
    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
   if(token==null||!token.path("access_token").isTextual()||token.path("access_token").asText().isBlank())
    throw unavailable("Chưa lấy được quyền gửi thư Gmail. Hãy kiểm tra cấu hình OAuth.");
   stage="send";
   String subject=Base64.getEncoder().encodeToString("Mã xác nhận đăng ký Sanka".getBytes(StandardCharsets.UTF_8));
   String body="Mã xác nhận đăng ký Sanka của bạn: "+code+"\nMã có hiệu lực 10 phút. Không chia sẻ mã này. Nếu bạn không đăng ký, hãy bỏ qua email.";
   String mime="From: Sanka <"+sender+">\r\nTo: "+email+"\r\nSubject: =?UTF-8?B?"+subject+"?=\r\n"
    +"MIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n"
    +Base64.getMimeEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8))+"\r\n";
   String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(mime.getBytes(StandardCharsets.UTF_8));
   var response=client.post().uri("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
    .headers(headers->headers.setBearerAuth(token.path("access_token").asText()))
    .body(Map.of("raw",raw)).retrieve().body(JsonNode.class);
   if(response==null||!response.path("id").isTextual()||response.path("id").asText().isBlank())
    throw unavailable("Gmail chưa xác nhận gửi email. Hãy thử lại sau.");
  }catch(RestClientResponseException error){
   // Never log response bodies, credentials, recipient addresses or OTPs.
   log.warn("Gmail verification email failed at {}: HTTP {}",stage,error.getStatusCode().value());
   throw unavailable("Chưa gửi được email xác nhận qua Gmail. Hãy kiểm tra cấu hình hoặc thử lại sau.");
  }catch(RestClientException error){
   log.warn("Gmail verification email failed at {}: connection or response error",stage);
   throw unavailable("Chưa gửi được email xác nhận. Hãy thử lại sau hoặc liên hệ quản trị viên.");
  }
 }
 private static boolean validAddress(String value){
  return value!=null&&value.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+");
 }
 private static ResponseStatusException unavailable(String reason){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,reason);}
}
