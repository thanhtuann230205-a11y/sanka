package vn.sanka.lms.auth;

import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.client.oidc.userinfo.*;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.user.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;

@Service
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest,OidcUser> {
 private final GoogleAccountService accounts;
 private final OidcUserService delegate=new OidcUserService();
 public GoogleOidcUserService(GoogleAccountService accounts){this.accounts=accounts;}
 @Override public OidcUser loadUser(OidcUserRequest request){
  var google=delegate.loadUser(request);
  try{
   var student=accounts.resolve(google.getSubject(),google.getEmail(),Boolean.TRUE.equals(google.getEmailVerified()),google.getClaimAsString("hd"),google.getFullName());
   return new SankaGoogleUser(google,student.getEmail(),student.getRole());
  }catch(DataIntegrityViolationException error){throw new OAuth2AuthenticationException(new OAuth2Error("google_link_conflict"),"Tài khoản vừa được cập nhật. Vui lòng thử đăng nhập lại.");}
 }
 static class SankaGoogleUser extends DefaultOidcUser {
  private static final long serialVersionUID=1L;
  private final String email;
  SankaGoogleUser(OidcUser google,String email,String role){super(List.of(new SimpleGrantedAuthority("ROLE_"+role)),google.getIdToken(),google.getUserInfo());this.email=email;}
  @Override public String getName(){return email;}
 }
}
