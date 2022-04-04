package dev.iot.telegrambot.telegramraspi.storage;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KeyValueService {

    private final KeyValueRepository keyValueRepository;

    public KeyValueService(KeyValueRepository keyValueRepository) {
        this.keyValueRepository = keyValueRepository;
    }

    public void setKeyValue(String key, String value) {
        KeyValueEntity entity = keyValueRepository.findByKey(key);
        if (entity == null) {
            entity = new KeyValueEntity();
        }
        entity.setKey(key);
        entity.setValue(value);
        keyValueRepository.save(entity);
    }

    public String searchValue(String key) {
        KeyValueEntity storedId = keyValueRepository.findByKey(key);
        return storedId != null ? storedId.getValue() : key;
    }
}
