package xyz.cssxsh.mirai.test.entry;

import jakarta.persistence.*;

@Entity
@Table(name = "work")
public class Work {
    private Long pid;
    private String content;

    private User user;

    @Id
    public Long getPid() {
        return pid;
    }

    public void setPid(Long pid) {
        this.pid = pid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @ManyToOne
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
