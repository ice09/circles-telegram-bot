package dev.iot.telegrambot.telegramraspi.storage;

import javax.persistence.*;

@Entity
@Table(name = "key_value_entity")
public class KeyValueEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue
    private Long id;
    private String keyid;
    private String valueid;

    private String ext;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyid() {
        return keyid;
    }

    public String getValueid() {
        return valueid;
    }

    public void setKeyid(String key) {
        this.keyid = key;
    }

    public void setValueid(String value) {
        this.valueid = value;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    @Override
    public String toString() {
        return "KeyValueEntity{" +
                "id=" + id +
                ", key='" + keyid + '\'' +
                ", value='" + valueid + '\'' +
                ", ext='" + ext + '\'' + "}";
    }
}