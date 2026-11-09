package vn.sanka.lms;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.sanka.lms.auth.Student;
import vn.sanka.lms.auth.StudentRepository;
import static org.assertj.core.api.Assertions.assertThat;

/** Real HTTP requests: only the cookie, not a MockHttpSession, crosses servers. */
class SharedLoginSessionTest {
    private ServletWebServerApplicationContext start(String database) {
        return (ServletWebServerApplicationContext) new SpringApplicationBuilder(SankaApplication.class).run(
            "--spring.profiles.active=test", "--spring.config.import=", "--server.port=0",
            "--spring.datasource.url="+database, "--spring.datasource.username=sa",
            "--spring.datasource.password=", "--spring.datasource.driver-class-name=org.h2.Driver",
            "--SANKA_INSTRUCTOR_EMAIL=", "--SANKA_INSTRUCTOR_PASSWORD=",
            "--logging.level.root=WARN");
    }
    private String base(ServletWebServerApplicationContext app) {
        return "http://localhost:"+app.getWebServer().getPort();
    }
    private HttpResponse<String> get(HttpClient client,String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private HttpResponse<String> post(HttpClient client,String url,String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private String csrf(String html) {
        var match=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(match.find()).as("CSRF input rendered").isTrue();
        return URLEncoder.encode(match.group(1),StandardCharsets.UTF_8);
    }

    @Test void cookieSurvivesDifferentInstancesAndRestartButLogoutRevokesIt() throws Exception {
        String database="jdbc:h2:mem:shared_login_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        var cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        var client=HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
        ServletWebServerApplicationContext first=null,second=null,restarted=null;
        String email="shared-session@example.test";
        try {
            first=start(database);
            first.getBean(StudentRepository.class).saveAndFlush(new Student("Shared learner",email,
                    first.getBean(PasswordEncoder.class).encode("test-password123")));
            var login=get(client,base(first)+"/login");
            assertThat(login.statusCode()).isEqualTo(200);
            assertThat(login.headers().allValues("Set-Cookie")).anySatisfy(cookie ->
                assertThat(cookie).contains("SANKA_SESSION=","HttpOnly","SameSite=Lax"));
            var signedIn=post(client,base(first)+"/login","username="+URLEncoder.encode(email,StandardCharsets.UTF_8)
                    +"&password=test-password123&_csrf="+csrf(login.body()));
            assertThat(signedIn.statusCode()).isEqualTo(302);
            assertThat(signedIn.headers().firstValue("Location").orElse("")).endsWith("/start");
            assertThat(get(client,base(first)+"/dictionary").statusCode()).isEqualTo(200);
            var jdbc=first.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT max_inactive_interval FROM SANKA_SESSION WHERE principal_name=?",Integer.class,email))
                    .as("8-hour idle timeout").isEqualTo(8*60*60);

            second=start(database);
            assertThat(get(client,base(second)+"/dictionary").statusCode()).as("same cookie on another live instance").isEqualTo(200);
            first.close();first=null;
            assertThat(get(client,base(second)+"/competency").statusCode()).as("original instance shut down").isEqualTo(200);
            second.close();second=null;

            restarted=start(database);
            var page=get(client,base(restarted)+"/dictionary");
            assertThat(page.statusCode()).as("all previous application instances restarted").isEqualTo(200);
            String oldCookie=cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("SANKA_SESSION"))
                    .findFirst().map(c->c.getName()+"="+c.getValue()).orElseThrow();
            assertThat(post(client,base(restarted)+"/logout","").statusCode()).as("CSRF still enforced").isEqualTo(403);
            var logout=post(client,base(restarted)+"/logout","_csrf="+csrf(page.body()));
            assertThat(logout.statusCode()).isEqualTo(302);
            assertThat(restarted.getBean(JdbcTemplate.class).queryForObject(
                    "SELECT COUNT(*) FROM SANKA_SESSION WHERE principal_name=?",Integer.class,email)).isZero();
            var retired=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build().send(
                    HttpRequest.newBuilder(URI.create(base(restarted)+"/dictionary")).header("Cookie",oldCookie).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(retired.statusCode()).isEqualTo(302);
            assertThat(retired.headers().firstValue("Location").orElse("")).endsWith("/login");
        } finally {
            if(first!=null)first.close();
            if(second!=null)second.close();
            if(restarted!=null)restarted.close();
        }
    }
}
