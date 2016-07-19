package b105.pgnagent.models;


/**
 * Defines serializable model for each sensor shown in the WSN area, so that
 * it can be passed among activities
 *
 * Created by Paco on 06/05/2016.
 */
public class Node implements java.io.Serializable {
    private static final long serialVersionUID = 7;

    private int id;
    private int temp;
    private int hum;

    /**
     * Constructor
     *
     * @param id int
     * @param temp int
     * @param hum int
     */
    public Node(int id, int temp, int hum) {
        this.id = id;
        this.temp = temp;
        this.hum = hum;
    }

    //SETTERS AND GETTERS

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTemp() {
        return temp;
    }

    public void setTemp(int temp) {
        this.temp = temp;
    }

    public int getHum() {
        return hum;
    }

    public void setHum(int hum) {
        this.hum = hum;
    }

}
