package org.arlevin.memeDatabaseBot.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sequence_number")
public class SequenceNumberEntity {

  @Id
  @Column(name = "sequence_number")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long sequenceNumber;
}