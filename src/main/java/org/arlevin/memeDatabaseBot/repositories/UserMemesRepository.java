package org.arlevin.memeDatabaseBot.repositories;

import org.arlevin.memeDatabaseBot.domain.UserMemesEntity;
import org.arlevin.memeDatabaseBot.domain.UserMemesEntity.UserMemesId;
import org.springframework.data.repository.CrudRepository;

public interface UserMemesRepository extends CrudRepository<UserMemesEntity, UserMemesId> {

}