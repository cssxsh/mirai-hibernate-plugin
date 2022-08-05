package xyz.cssxsh.mirai.test.entry;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "user")
public class User {
    @Id
    private Long id;

    private String name;

    @OneToMany(mappedBy = "user")
    private List<Work> works;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public List<Work> getWorks() {
        return works;
    }

    public void setWorks(List<Work> works) { this.works = works;}
}
