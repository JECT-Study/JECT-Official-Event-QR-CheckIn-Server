package ject.official_qr_checkin_server.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalTime;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.model.EventTimetable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EventTimetableTests {
    @Test
    void acceptsOpenEndedItemAndTrimsSchedule() {
        var item = builder().schedule("  퇴장  ").build();
        assertThat(item.getSchedule()).isEqualTo("퇴장");
        assertThat(item.getEndTime()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"13:29", "13:30"})
    void rejectsEndNotAfterStart(String end) {
        assertThatIllegalArgumentException().isThrownBy(() -> builder().endTime(LocalTime.parse(end)).build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void rejectsBlankSchedule(String schedule) {
        assertThatIllegalArgumentException().isThrownBy(() -> builder().schedule(schedule).build());
    }

    @Test
    void enforcesScheduleLength() {
        assertThat(builder().schedule("가".repeat(255)).build().getSchedule()).hasSize(255);
        assertThatIllegalArgumentException().isThrownBy(() -> builder().schedule("가".repeat(256)).build());
    }

    @Test
    void requiresEventAndStartTime() {
        assertThatNullPointerException().isThrownBy(() -> builder().event(null).build());
        assertThatNullPointerException().isThrownBy(() -> builder().startTime(null).build());
    }

    private EventTimetable.EventTimetableBuilder builder() {
        return EventTimetable.builder().event(Event.builder().build()).startTime(LocalTime.of(13, 30)).schedule("체크인");
    }
}
