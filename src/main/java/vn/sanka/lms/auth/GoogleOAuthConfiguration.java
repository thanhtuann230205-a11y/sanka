package vn.sanka.lms.auth;

import org.springframework.context.annotation.*;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Environment;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;

@Configuration(proxyBeanMethods=false)
@Conditional(GoogleOAuthConfiguration.Configured.class)
public class GoogleOAuthConfiguration {
 static boolean enabled(Environment env){return !env.getProperty("sanka.google.client-id","").isBlank()&&!env.getProperty("sanka.google.client-secret","").isBlank();}
 public static class Configured implements Condition {
  public boolean matches(ConditionContext context,AnnotatedTypeMetadata metadata){return enabled(context.getEnvironment());}
 }
 @Bean ClientRegistrationRepository googleClients(Environment env){
  var google=CommonOAuth2Provider.GOOGLE.getBuilder("google")
   .clientId(env.getProperty("sanka.google.client-id").strip())
   .clientSecret(env.getProperty("sanka.google.client-secret").strip())
   .redirectUri(env.getProperty("sanka.google.redirect-uri","{baseUrl}/login/oauth2/code/google"))
   .scope("openid","profile","email").build();
  return new InMemoryClientRegistrationRepository(google);
 }
}
