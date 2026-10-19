package com.sitamvan.academic.scheduling;

import java.util.List;

public record ScheduleGenerationProposal(List<Entry> entries, List<String> warnings) {
    public ScheduleGenerationProposal {
        entries = List.copyOf(entries);
        warnings = List.copyOf(warnings);
    }

    public record Entry(Long requirementId, Long teacherId, Long roomId, List<Long> timeSlotIds) {
        public Entry { timeSlotIds = List.copyOf(timeSlotIds); }
    }
}
