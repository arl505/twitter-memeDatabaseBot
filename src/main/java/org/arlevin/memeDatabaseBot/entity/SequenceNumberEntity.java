package org.arlevin.memeDatabaseBot.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sequence_number")
public class SequenceNumberEntity {

  @Id
  @Column(name = "sequence_number")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long sequenceNumber;
}
