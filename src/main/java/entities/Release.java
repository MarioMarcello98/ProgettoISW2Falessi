package entities;
import java.time.LocalDateTime;
public class Release {
    int id;
    String name;
    LocalDateTime date;
    public Release(int id, String name, LocalDateTime date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public LocalDateTime getDate() {
        return date;
    }
    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}