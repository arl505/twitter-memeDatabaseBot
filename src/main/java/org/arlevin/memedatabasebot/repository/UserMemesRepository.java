package org.arlevin.memedatabasebot.repository;

import java.util.List;
import java.util.Optional;
import org.arlevin.memedatabasebot.entity.UserMemesEntity;
import org.arlevin.memedatabasebot.entity.UserMemesEntity.UserMemesId;
import org.springframework.data.repository.CrudRepository;

public interface UserMemesRepository extends CrudRepository<UserMemesEntity, UserMemesId> {

  Optional<List<UserMemesEntity>> findAllByUserIdAndDescription(String userId, String description);
}