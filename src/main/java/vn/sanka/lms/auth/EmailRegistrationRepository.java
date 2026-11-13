package vn.sanka.lms.auth;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface EmailRegistrationRepository extends JpaRepository<EmailRegistration,String>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from EmailRegistration r where r.email=:email")
 Optional<EmailRegistration> lockEmail(@Param("email") String email);
 Optional<EmailRegistration> findByToken(String token);
}
