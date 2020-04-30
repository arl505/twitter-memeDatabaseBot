package org.arlevin.memeDatabaseBot.repositories;

import org.arlevin.memeDatabaseBot.entity.SequenceNumberEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface SequenceNumberRepository extends CrudRepository<SequenceNumberEntity, Long> {

  @Modifying
  @Query(value = "delete from sequence_number where sequence_number < :highNum", nativeQuery = true)
  void deleteLessThanHighNum(@Param("highNum") Long highNum);
}