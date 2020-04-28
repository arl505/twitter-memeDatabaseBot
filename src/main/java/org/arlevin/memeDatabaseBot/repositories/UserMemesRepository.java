package org.arlevin.memeDatabaseBot.repositories;

import java.util.List;
import java.util.Optional;
import org.arlevin.memeDatabaseBot.entity.UserMemesEntity;
import org.arlevin.memeDatabaseBot.entity.UserMemesEntity.UserMemesId;
import org.springframework.data.repository.CrudRepository;

public interface UserMemesRepository extends CrudRepository<UserMemesEntity, UserMemesId> {

  Optional<List<UserMemesEntity>> findAllByUserIdAndDescription(String userId, String description);
}