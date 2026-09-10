package ject.official_qr_checkin_server.domain.member.repository;

import java.util.Optional;
import ject.official_qr_checkin_server.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByPhoneNumber(String phoneNumber);
}
