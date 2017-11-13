package dv606.sb223df.moveit;

/**
 * Created by Simon on 06/05/2016.
 * Entity representing a run
 */
public class Run {

    private long id;
    private String coordinates;
    private int time;
    private int distance;
    private String date;

    /* Getters */
    public long getId() { return id; }
    public String getCoordinates() { return coordinates; }
    public int getTime() { return time; }
    public int getDistance() { return distance; }
    public String getDate() { return date; }

    /* Setters */
    public void setId(long id) { this.id = id; }
    public void setCoordinates(String coordinates) { this.coordinates = coordinates; }
    public void setTime(int time) { this.time = time; }
    public void setDistance(int distance) { this.distance = distance; }
    public void setDate(String date) { this.date = date; }

}
