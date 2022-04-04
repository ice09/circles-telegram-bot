package dev.iot.telegrambot.telegramraspi.storage;

import javax.persistence.*;

@Entity
@Table(name = "key_value_entity")
public class KeyValueEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue
    private Long id;
    private String key;
    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "KeyValueEntity{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}