package ject.official_qr_checkin_server.domain.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MemberTests {

	@Test
	void createsMemberWithNormalizedIdentity() {
		Member member = Member.create("  홍길동  ", "010-1234-5678");

		assertEquals("홍길동", member.getName());
		assertEquals("01012345678", member.getPhoneNumber().getValue());
		assertEquals(MemberStatus.ACTIVE, member.getStatus());
	}

	@Test
	void rejectsInvalidPhoneNumber() {
		assertThrows(
				IllegalArgumentException.class,
				() -> Member.create("홍길동", "02-1234-5678")
		);
	}

	@Test
	void deactivatesMemberWithoutDeletingIdentity() {
		Member member = Member.create("홍길동", "01012345678");

		member.deactivate();

		assertEquals(MemberStatus.INACTIVE, member.getStatus());
	}
}
