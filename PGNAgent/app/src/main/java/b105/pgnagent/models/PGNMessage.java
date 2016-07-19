package b105.pgnagent.models;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


/**
 * Created by Paco on 02/03/2016.
 *
 * Defines object type for WGN Messages and a few useful methods and constants
 */
@SuppressWarnings("unused")
public class PGNMessage {
    //How each message ends
    private static final byte[] EOF = {0x0D, 0x0A};
    //Useful for initializing variables
    private static final byte ZERO = 0x00;
    //Position of first Data Payload byte
    private static final int DATA_PAYLOAD_STARTING_POINT = 4;
    //wgnLength + EOF
    private static final int NOT_INCLUDED_BYTES = 3;

    //To use in case temp and/or hum are unknown
    public static final int NOT_SET_VALUE = -777;

    //Hardcoded constants for most messages
    public static final byte AP_ID = 0x00;
    public static final byte REQ_GET_LENGTH = 0x03;
    public static final byte REQ_LED_LENGTH = 0x04;

    //Four possible values of directionCode field
    public static final byte REQ = 0x10;
    public static final byte RES = 0x08;
    public static final byte NUF = 0x04;
    public static final byte ACK = 0x02;

    //Thirteen possible values of messageCode field
    public static final byte GET_NODE_LIST                = 0x01;
    public static final String STR_GET_NODE_LIST       = "get_node_list";
    public static final byte GET_NUM_NODES            = 0x02;
    public static final String STR_GET_NUM_NODES   = "get_num_nodes";
    public static final byte GET_TEMPERATURE          = 0x08;
    public static final String STR_GET_TEMPERATURE = "get_temperature";
    public static final byte GET_HUMIDITY                 = 0x09;
    public static final String STR_GET_HUMIDITY        = "get_humidity";
    public static final byte CLEAR_LED                        = 0x10;
    public static final String STR_CLEAR_LED              = "clear_led";
    public static final byte SET_LED                            = 0x11;
    public static final String STR_SET_LED                   = "set_led";
    public static final byte TOGGLE_LED                     = 0x12;
    public static final String STR_TOGGLE_LED            = "toggle_led";
    public static final byte SET_ALL_LEDS                    = 0x13;
    public static final String STR_SET_ALL_LEDS           = "set_all_leds";
    public static final byte CLEAR_ALL_LEDS                = 0x14;
    public static final String STR_CLEAR_ALL_LEDS      = "clear_all_leds";
    public static final byte TOGGLE_ALL_LEDS             = 0x15;
    public static final String STR_TOGGLE_ALL_LEDS    = "toggle_all_leds";

    public static final byte NUF_NEW_NODE               = 0x01;
    public static final String STR_NUF_NEW_NODE     = "nuf_new_node";
    public static final byte NUF_UPDATE_TEMP          = 0x08;
    public static final String STR_NUF_UPDATE_TEMP = "nuf_update_temp";
    public static final byte NUF_UPDATE_HUM           = 0x09;
    public static final String STR_NUF_UPDATE_HUM  = "nuf_update_hum";

    //Possible messages
    public static final BiMap<Byte, String> MSG_TYPES_BIMAP = HashBiMap.create(
            new HashMap<Byte , String>() {{
                put(GET_NODE_LIST, STR_GET_NODE_LIST);
                put(GET_NUM_NODES, STR_GET_NUM_NODES);
                put(GET_TEMPERATURE, STR_GET_TEMPERATURE);
                put(GET_HUMIDITY, STR_GET_HUMIDITY);
                put(CLEAR_LED, STR_CLEAR_LED);
                put(SET_LED, STR_SET_LED);
                put(TOGGLE_LED, STR_TOGGLE_LED);
                put(SET_ALL_LEDS, STR_SET_ALL_LEDS);
                put(CLEAR_ALL_LEDS, STR_CLEAR_ALL_LEDS);
                put(TOGGLE_ALL_LEDS, STR_TOGGLE_ALL_LEDS);
            }}
    );

    //Needs to be in a separate BiMap due to some repeated messageCodes
    public static final BiMap<Byte, String> NUF_MSG_TYPES_BIMAP = HashBiMap.create(
            new HashMap<Byte , String>() {{
                put(NUF_NEW_NODE, STR_NUF_NEW_NODE);
                put(NUF_UPDATE_TEMP, STR_NUF_UPDATE_TEMP);
                put(NUF_UPDATE_HUM, STR_NUF_UPDATE_HUM);
            }}
    );

    //Possible messages from SmartPhone to PGN
   public static ArrayList<String> PHONE_MESSAGES = new ArrayList<>(
            Arrays.asList(
                new String[] {
                        STR_GET_NODE_LIST,
                        STR_GET_NUM_NODES
                }
            )
    );

    //Message parameters
    private byte pgnLength;
    private byte targetId; //PGN, sensor, AP, NUF
    private byte directionCode; //REQ,RES,NUF,ACK
    private byte messageCode; //13 possible types of messages
    private byte[] dataPayload; //has to be [wgnLength - 3] long

    /**
     * PGNMessage Constructor
     *
     * @param pgnLength byte
     * @param targetId byte
     * @param directionCode byte
     * @param messageCode byte
     * @param dataPayload byte[]
     */
    public PGNMessage(byte pgnLength,
                      byte targetId,
                      byte directionCode,
                      byte messageCode,
                      byte[] dataPayload) {
        this.pgnLength = pgnLength;
        this.targetId = targetId;
        this.directionCode = directionCode;
        this.messageCode = messageCode;
        this.dataPayload = dataPayload;
    }


    //============================= CLASS METHODS =============================

    /**
     * Returns message's bytes
     * @return byte[] buffer
     */
    public byte[] buffer() {
        ArrayList<Byte> buffer = new ArrayList<>();

        buffer.add(this.pgnLength);
        buffer.add(this.targetId);
        buffer.add(this.directionCode);
        buffer.add(this.messageCode);
        if (this.dataPayload.length > 0) {
            for (byte x : this.dataPayload) {
                buffer.add(x);
            }
        }

        buffer.add(EOF[0]);
        buffer.add(EOF[1]);

        Byte[] bytesArray = new Byte[buffer.size()];
        buffer.toArray(bytesArray);
        //return to primitive type, ZERO if no value is found
        return toPrimitive(bytesArray, ZERO);
    }

    /**
     * Returns PGNMessage from a byte buffer.
     * @param buffer byte[]
     * @return pgnMsg PGNMessage
     */
    public static PGNMessage getPGNMessage(byte[] buffer) {
        PGNMessage pgnMsg = new PGNMessage(ZERO,ZERO,ZERO,ZERO,null);

        if (buffer.length > 4) {
            pgnMsg.pgnLength = buffer[0];
            pgnMsg.targetId = buffer[1];
            //throw error when directionCode = 0b000XXXX1
            String strMessageCode = Integer.toBinaryString((int) buffer[2]);
            if (strMessageCode.endsWith("1")) { //last bit implies error.
                System.out.println("Last bit error in message.");
            }
            pgnMsg.directionCode = buffer[2];
            pgnMsg.messageCode = buffer[3];

            int dataPayloadLength = buffer[0] - NOT_INCLUDED_BYTES;

            if (dataPayloadLength > 0) {
                pgnMsg.dataPayload = new byte[dataPayloadLength];
                System.arraycopy(
                        buffer, DATA_PAYLOAD_STARTING_POINT,
                        pgnMsg.dataPayload, ZERO,
                        dataPayloadLength
                );
            } else { //msg does not include a payload
                pgnMsg.dataPayload = new byte[ZERO];
            }

            return pgnMsg;
        } else return null;
    }

    /**
     * Gets associated String according to its messageCode attribute
     * @return String text
     */
    public String getText() {
        return MSG_TYPES_BIMAP.get(this.getMessageCode());
    }

    //Source code from:
    // http://commons.apache.org/proper/commons-lang/javadocs/api-3.3.1/src-html/org/apache/commons/lang3/ArrayUtils.html#line.3229
    /**
     * <p>Converts an array of object Bytes to primitives handling {@code null}.</p>
     *
     * <p>This method returns {@code null} for a {@code null} input array.</p>
     *
     * @param array  a {@code Byte} array, may be {@code null}
     * @param valueForNull  the value to insert if {@code null} found
     * @return a {@code byte} array, {@code null} if null array input
     */
    public static byte[] toPrimitive(final Byte[] array, final byte valueForNull) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return new byte[0];
        }
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            final Byte b = array[i];
            result[i] = (b == null ? valueForNull : b.byteValue());
        }
        return result;
    }

    //============================= GETTERS AND SETTERS =============================
    /**
     * @return byte pgnLength
     */
    public byte getPgnLength() {
        return pgnLength;
    }

    /**
     * @param pgnLength byte
     */
    public void setPgnLength(byte pgnLength) {
        this.pgnLength = pgnLength;
    }

    /**
     * @return byte targetId
     */
    public byte getTargetId() {
        return targetId;
    }

    /**
     * @param targetId byte
     */
    public void setTargetId(byte targetId) {
        this.targetId = targetId;
    }

    /**
     * @return byte directionCode
     */
    public byte getDirectionCode() {
        return directionCode;
    }

    /**
     * @param directionCode byte
     */
    public void setDirectionCode(byte directionCode) {
        this.directionCode = directionCode;
    }

    /**
     * @return byte messageCode
     */
    public byte getMessageCode() {
        return messageCode;
    }

    /**
     * @param messageCode byte
     */
    public void setMessageCode(byte messageCode) {
        this.messageCode = messageCode;
    }

    /**
     * @return byte dataPayload
     */
    public byte[] getDataPayload() {
        return dataPayload;
    }

    /**
     * @param dataPayload byte
     */
    public void setDataPayload(byte[] dataPayload) {
        this.dataPayload = dataPayload;
    }

}
