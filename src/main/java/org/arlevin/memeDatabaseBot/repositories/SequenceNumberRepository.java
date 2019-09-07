package org.arlevin.memeDatabaseBot.repositories;

import org.arlevin.memeDatabaseBot.domain.SequenceNumberEntity;
import org.springframework.data.repository.CrudRepository;

public interface SequenceNumberRepository extends CrudRepository<SequenceNumberEntity, Long> {

  void deleteBySequenceNumberLessThan(Long sequenceNumber);
}