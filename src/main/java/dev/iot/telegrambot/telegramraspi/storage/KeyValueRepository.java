package dev.iot.telegrambot.telegramraspi.storage;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeyValueRepository extends CrudRepository<KeyValueEntity, Long> {

    KeyValueEntity findByKey(String key);

    KeyValueEntity findByValue(String value);

}
