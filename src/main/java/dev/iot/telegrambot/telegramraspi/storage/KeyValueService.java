package dev.iot.telegrambot.telegramraspi.storage;

import org.springframework.stereotype.Service;

@Service
public class KeyValueService {

    private final KeyValueRepository keyValueRepository;

    public KeyValueService(KeyValueRepository keyValueRepository) {
        this.keyValueRepository = keyValueRepository;
    }

    public void setKeyValue(String key, String value, String ext) {
        KeyValueEntity entity = keyValueRepository.findByKeyid(key);
        if (entity == null) {
            entity = new KeyValueEntity();
        }
        entity.setKeyid(key);
        entity.setValueid(value);
        entity.setExt(ext);
        keyValueRepository.save(entity);
    }

    public String searchValue(String key) {
        KeyValueEntity storedId = keyValueRepository.findByKeyid(key);
        return storedId != null ? storedId.getValueid() : key;
    }

    public KeyValueEntity loadEntityByValue(String value) {
        return keyValueRepository.findByValueid(value);
    }

    public void updateValueExt(String value, String signature) {
        KeyValueEntity entity = keyValueRepository.findByValueid(value);
        entity.setExt(signature);
        keyValueRepository.save(entity);
    }
}
