package com.sitamvan.academic.model;

import com.sitamvan.shared.data.util.AAuditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "room_facilities", uniqueConstraints = @UniqueConstraint(name = "uk_room_facility", columnNames = {"room_id", "facility_id"}))
@Getter @Setter @NoArgsConstructor
public class RoomFacility extends AAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_id", nullable = false) private Room room;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "facility_id", nullable = false) private Facility facility;
}
