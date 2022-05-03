package dev.iot.telegrambot.telegramraspi.storage;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KeyValueService {

    private final KeyValueRepository keyValueRepository;

    public KeyValueService(KeyValueRepository keyValueRepository) {
        this.keyValueRepository = keyValueRepository;
    }

    public void setKeyValue(String key, String value, String ext) {
        KeyValueEntity entity = keyValueRepository.findByKey(key);
        if (entity == null) {
            entity = new KeyValueEntity();
        }
        entity.setKey(key);
        entity.setValue(value);
        entity.setExt(ext);
        keyValueRepository.save(entity);
    }

    public String searchValue(String key) {
        KeyValueEntity storedId = keyValueRepository.findByKey(key);
        return storedId != null ? storedId.getValue() : key;
    }

    public KeyValueEntity loadEntityByValue(String value) {
        return keyValueRepository.findByValue(value);
    }

    public void updateValueExt(String value, String signature) {
        KeyValueEntity entity = keyValueRepository.findByValue(value);
        entity.setExt(signature);
        keyValueRepository.save(entity);
    }
}
