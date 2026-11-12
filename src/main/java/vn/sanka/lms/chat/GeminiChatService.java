package vn.sanka.lms.chat;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class GeminiChatService {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(GeminiChatService.class);
 public record Message(@NotNull @Pattern(regexp="user|model") String role,@NotBlank @Size(max=2000) String text){}
 public record Question(@NotBlank @Size(max=1500) String message,
                        @Size(max=12) List<@NotNull @Valid Message> history,
                        @Pattern(regexp="vi|en|ja") String language){}
 private final RestClient client;
 private final String key,model;
 @Autowired
 public GeminiChatService(@Value("${sanka.chat.api-key:}") String key,@Value("${sanka.chat.model:gemini-2.5-flash-lite}") String model){
  this(createClient(),key,model);
 }
 GeminiChatService(RestClient client,String key,String model){this.client=client;this.key=key==null?"":key.strip();this.model=model==null?"":model.strip();}
 private static RestClient createClient(){
  var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(10000);factory.setReadTimeout(30000);
  return RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta")
   .requestFactory(factory).build();
 }
 public boolean enabled(){return key!=null&&!key.isBlank();}
 public String answer(Question question){
  if(!enabled())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Trợ lý chưa được cấu hình. Hãy thêm GEMINI_API_KEY trên máy chủ.");
  if(!model.matches("[a-zA-Z0-9._-]+"))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cấu hình model AI chưa hợp lệ.");
  var contents=new ArrayList<Map<String,Object>>();
  if(question.history()!=null)for(var turn:question.history())contents.add(turn(turn.role(),turn.text()));
  contents.add(turn("user",question.message().strip()));
  String instruction="""
   Bạn là trợ lý học tiếng Nhật Sanka. Giải thích ngắn, rõ ràng, thân thiện, dùng văn bản thuần.
   Hỗ trợ từ vựng, cách đọc, ngữ pháp, dịch ví dụ và tiếng Nhật kỹ thuật. Khi giải thích từ, ghi cách đọc, nghĩa và ví dụ.
   Chỉ hướng dẫn cách làm và ví dụ tương tự khi người học xin đáp án bài kiểm tra đang làm, không làm bài thay.
   Sanka: kiểm tra đầu vào trước; Zero hoặc S5 học cơ bản S5, qua S5 học S4; đạt S4 trở lên mới chọn chuyên ngành.
   Các ngành gồm cơ khí, điện, cơ điện tử, tự động hóa, CNTT. Bài học dùng flashcard, học hết các thẻ mới hoàn thành.
   Trình độ S5 tương ứng N5, S4 tương ứng N4. Từ điển lọc theo ngành. Bài viết do giảng viên chấm.
   Bạn không có quyền truy cập tài khoản, điểm, bài nộp hay thay đổi tiến độ. Không tự nhận đã xem dữ liệu cá nhân hoặc thực hiện thao tác.
   Không yêu cầu mật khẩu hay API key. Nếu không chắc, nói rõ. Không bịa chức năng hay tài liệu nguồn.
   Trả lời theo ngôn ngữ được yêu cầu: 
   """+Objects.requireNonNullElse(question.language(),"vi");
  try{
   var response=client.post().uri("/models/"+model+":generateContent").header("x-goog-api-key",key)
    .body(Map.of("systemInstruction",Map.of("parts",List.of(Map.of("text",instruction))),"contents",contents,
     "generationConfig",Map.of("maxOutputTokens",1000,"temperature",0.5)))
    .retrieve().body(JsonNode.class);
   var parts=response==null?null:response.path("candidates").path(0).path("content").path("parts");
   var reply=new StringBuilder();
   if(parts!=null&&parts.isArray())for(var part:parts)if(!part.path("thought").asBoolean(false)&&part.hasNonNull("text"))reply.append(part.get("text").asText()).append('\n');
   String text=reply.toString().strip();
   if(text.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"AI chưa trả lời được câu này. Hãy diễn đạt lại câu hỏi.");
   return text.substring(0,Math.min(text.length(),6000));
  }catch(RestClientResponseException error){
   int status=error.getStatusCode().value();
   // Never log provider bodies or exception messages: they may contain credentials or prompts.
   log.warn("Gemini request failed: HTTP {}, model={}",status,model);
   if(status==429)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"AI đang hết hạn mức hoặc quá tải. Vui lòng thử lại sau.");
   String message=switch(status){
    case 400 -> "Gemini từ chối yêu cầu (400). Hãy kiểm tra API key và điều kiện sử dụng của dự án Google AI Studio.";
    case 401,403 -> "API key không hợp lệ hoặc không có quyền gọi Gemini ("+status+"). Hãy cập nhật GEMINI_API_KEY trên Render.";
    case 404 -> "Không tìm thấy model Gemini (404). Hãy kiểm tra GEMINI_MODEL trên Render và chọn model có trong Google AI Studio.";
    default -> "Gemini đang gặp lỗi ("+status+"). Vui lòng thử lại sau.";
   };
   throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,message);
  }catch(RestClientException error){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Kết nối AI mất quá nhiều thời gian. Vui lòng thử lại.");}
 }
 private Map<String,Object> turn(String role,String text){return Map.of("role",role,"parts",List.of(Map.of("text",text)));}
}
