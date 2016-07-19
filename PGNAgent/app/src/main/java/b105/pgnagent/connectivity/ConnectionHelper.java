package b105.pgnagent.connectivity;

/**
 * Created by Paco on 21/06/2016.
 */
public interface ConnectionHelper {
    /**
     * Starts the connection
     */
    void start();

    /**
     * Safely stops and destroys the connection. To be called when application goes to background or terminates
     */
    void stop();

    /**
     *Sends given byte buffer over the connection
     * @param tx_buffer byte[]
     */
    void sendMessage(byte[] tx_buffer );

    /**
     * Lets connection automatically send "get_node_list" message when it establishes
     * @param doGetNodeList boolean
     */
    void setDoGetNodeList(boolean doGetNodeList);

}
