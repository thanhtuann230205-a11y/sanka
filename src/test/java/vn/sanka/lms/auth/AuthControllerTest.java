package vn.sanka.lms.auth;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BeanPropertyBindingResult;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthControllerTest {
 private final StudentRepository students=mock(StudentRepository.class);
 private final PasswordEncoder encoder=mock(PasswordEncoder.class);
 private final EmailRegistrationService verification=mock(EmailRegistrationService.class);
 private final org.springframework.mock.web.MockHttpSession session=new org.springframework.mock.web.MockHttpSession();
 private final AuthController controller=new AuthController(students,encoder,verification);
 private final AuthController.Registration form=new AuthController.Registration("Test","new@example.com","password123");
 private final BeanPropertyBindingResult errors=new BeanPropertyBindingResult(form,"registration");

 @Test void unrelatedConstraintDoesNotClaimEmailExists(){
  when(students.findByEmail(form.email())).thenReturn(Optional.empty());
  when(encoder.encode(form.password())).thenReturn("encoded");
  when(verification.start(anyString(),anyString(),anyString())).thenThrow(new DataIntegrityViolationException("Other constraint"));
  assertThat(controller.register(form,errors,session)).isEqualTo("register");
  assertThat(errors.getFieldError("email")).isNull();
  assertThat(errors.getGlobalError().getCode()).isEqualTo("registration.failed");
 }

 @Test void concurrentRegistrationReportsDuplicateEmail(){
  when(students.findByEmail(form.email())).thenReturn(Optional.empty(),Optional.of(new Student("Test",form.email(),"encoded")));
  when(verification.start(anyString(),anyString(),anyString())).thenThrow(new DataIntegrityViolationException("Duplicate email"));
  assertThat(controller.register(form,errors,session)).isEqualTo("register");
  assertThat(errors.getFieldError("email").getCode()).isEqualTo("duplicate");
 }

 @Test void existingEmailIsRejectedBeforeSave(){
  when(students.findByEmail(form.email())).thenReturn(Optional.of(new Student("Test",form.email(),"encoded")));
  assertThat(controller.register(form,errors,session)).isEqualTo("register");
  assertThat(errors.getFieldError("email").getCode()).isEqualTo("duplicate");
  verify(students,never()).saveAndFlush(any());
 }

 @Test void successfulRegistrationRequiresVerification(){
  when(students.findByEmail(form.email())).thenReturn(Optional.empty());
  when(verification.start(anyString(),anyString(),anyString())).thenReturn("verification-token");
  assertThat(controller.register(form,errors,session)).isEqualTo("redirect:/register/verify");
  assertThat(session.getAttribute("registration.token")).isEqualTo("verification-token");
  verify(students,never()).saveAndFlush(any());
  assertThat(errors.hasErrors()).isFalse();
 }
}
