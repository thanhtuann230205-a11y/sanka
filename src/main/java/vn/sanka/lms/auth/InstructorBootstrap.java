package vn.sanka.lms.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit environment credentials only; never a shared default instructor account. */
@Component
public class InstructorBootstrap implements ApplicationRunner {
    private final StudentRepository students;
    private final PasswordEncoder encoder;
    @Value("${SANKA_INSTRUCTOR_EMAIL:}") private String email;
    @Value("${SANKA_INSTRUCTOR_PASSWORD:}") private String password;
    public InstructorBootstrap(StudentRepository students,PasswordEncoder encoder){this.students=students;this.encoder=encoder;}
    @Override @Transactional public void run(ApplicationArguments args) {
        if(email.isBlank() && password.isBlank())return;
        if(!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length()>190
            || password.length()<12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("Cấu hình tài khoản giảng viên cần email hợp lệ và mật khẩu ít nhất 12 ký tự, tối đa 72 byte");
        String normalized=email.strip().toLowerCase(java.util.Locale.ROOT);
        var existing=students.findByEmail(normalized);
        if(existing.isPresent()) {
            if(!"INSTRUCTOR".equals(existing.get().getRole()))
                throw new IllegalStateException("Email giảng viên trùng tài khoản sinh viên; hãy dùng email riêng");
            return;
        }
        var instructor=new Student("Giảng viên",normalized,encoder.encode(password));
        instructor.setRole("INSTRUCTOR");students.save(instructor);
    }
}
