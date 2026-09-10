package ject.official_qr_checkin_server.infrastructure.notion;

import ject.official_qr_checkin_server.domain.event.model.CheckedStatus;

public enum NotionAttendance {
    UNCHECKED(null),
    PRESENT("참석"),
    LATE("지각"),
    ABSENT_UNEXCUSED("불참(예외 미인정 사유)"),
    ABSENT_DOCUMENTED("불참(증빙자료 제출)"),
    ABSENT_RECRUITED("불참(충원)");

    private final String option;

    NotionAttendance(String option) {
        this.option = option;
    }

    public String option() {
        return option;
    }

    public static NotionAttendance from(CheckedStatus status) {
        return switch (status) {
            case CHECKED -> PRESENT;
            case TARDY -> LATE;
            case UNCHECKED -> UNCHECKED;
        };
    }
}
