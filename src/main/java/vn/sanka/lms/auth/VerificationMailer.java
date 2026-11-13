package vn.sanka.lms.auth;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
@Service
public class VerificationMailer {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(VerificationMailer.class);
 private final String key,secret,sender;
 private final RestClient client;
 @Autowired
 public VerificationMailer(@Value("${sanka.email.api-key:}") String key,
                           @Value("${sanka.email.secret-key:}") String secret,
                           @Value("${sanka.email.sender:}") String sender){
  this(createClient(),key,secret,sender);
 }
 VerificationMailer(RestClient client,String key,String secret,String sender){
  this.client=client;this.key=clean(key);this.secret=clean(secret);this.sender=clean(sender);
 }
 private static String clean(String value){return value==null?"":value.strip();}
 private static RestClient createClient(){
  var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(10000);factory.setReadTimeout(20000);
  return RestClient.builder().baseUrl("https://api.mailjet.com").requestFactory(factory).build();
 }
 public void send(String email,String code){
  if(key.isBlank()||secret.isBlank()||sender.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa cấu hình gửi mã xác nhận. Quản trị viên cần thêm MAILJET_API_KEY, MAILJET_SECRET_KEY và EMAIL_FROM.");
  try{
   var response=client.post().uri("/v3.1/send").headers(headers->headers.setBasicAuth(key,secret))
    .body(Map.of("Messages",List.of(Map.of(
     "From",Map.of("Name","Sanka","Email",sender),"To",List.of(Map.of("Email",email)),
     "Subject","Mã xác nhận đăng ký Sanka",
     "TextPart","Mã xác nhận đăng ký Sanka của bạn: "+code+"\nMã có hiệu lực 10 phút. Không chia sẻ mã này. Nếu bạn không đăng ký, hãy bỏ qua email."))))
    .retrieve().body(JsonNode.class);
   // A successful HTTP response does not guarantee that Mailjet accepted the message.
   var messages=response==null?null:response.path("Messages");
   if(messages==null||!messages.isArray()||messages.size()!=1||!"success".equals(messages.get(0).path("Status").asText()))
    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Mailjet chưa chấp nhận email. Hãy kiểm tra email người gửi đã xác minh và trạng thái tài khoản Mailjet.");
  }catch(RestClientResponseException error){
   // Do not log response bodies: they can contain addresses or message content.
   log.warn("Mailjet verification email failed: HTTP {}",error.getStatusCode().value());
   throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa gửi được email xác nhận qua Mailjet. Hãy kiểm tra cấu hình hoặc thử lại sau.");
  }catch(RestClientException error){
   throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa gửi được email xác nhận. Hãy thử lại sau hoặc liên hệ quản trị viên.");
  }
 }
}
