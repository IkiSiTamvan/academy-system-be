package com.sitamvan.academic.scheduling;

/** Port for a future generator implementation; no solver is selected in this phase. */
public interface ScheduleGenerator {
    ScheduleGenerationProposal generate(ScheduleGenerationRequest request);
}
