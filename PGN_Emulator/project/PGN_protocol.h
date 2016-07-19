/* 
 * ======== PGN_protocol.h ========
 */
#ifndef _PGN_PROTOCOL_H_
#define _PGN_PROTOCOL_H_

/*----------------------------------------------------------------------------+
 | Global defines                                                         |
 +----------------------------------------------------------------------------*/
#define FALSE                         0
#define TRUE                          1
#define PGN_ID                        0xAA

//test ID's for imaginary nodes
#define SENSOR_ID_1					  0x01
#define SENSOR_ID_2					  0x02
#define SENSOR_ID_3					  0x03

#define EOF_1                         0x0D
#define EOF_2                         0x0A
#define EOF                           0x0D0A
#define TIMEOUT_UART                  1000        // For a timeout of 50 ms, SMCLK=8MHz -> 80 = 8MHz/(2*0.050)

/*----------------------------------------------------------------------------+
 | Code BYTE 1                                                         |
 +----------------------------------------------------------------------------*/
#define REQUEST_FRAME         0x10          // Request frame from smartphone to PGN
#define RESPONSE_FRAME        0x08          // Response frame from PGN to smartphone
#define NUF_FRAME             0x04          // NUF frame from PGN to smartphone
#define ACK_FRAME             0x02          // ACK frame from smartphone to PGN

/*----------------------------------------------------------------------------+
 | Code BYTE 2                                                         |
 +----------------------------------------------------------------------------*/
// Smartphone => PGN
#define GET_NODE_LIST         0x01
#define GET_NUM_NODES         0x02
#define GET_TEMPERATURE       0x08
#define GET_HUMIDITY	      0x09
#define CLEAR_LED      	      0x10
#define SET_LED               0x11
#define TOGGLE_LED            0x12
#define SET_ALL_LEDS          0x13
#define CLEAR_ALL_LEDS        0x14
#define TOGGLE_ALL_LEDS       0x15

// PGN => smartphone
#define NUF_NEW_NODE          0x01 
#define NUF_UPDATE_TEMP       0x08
#define NUF_UPDATE_HUM        0x09         

/*----------------------------------------------------------------------------+
 | Default responses                                                   |
 +----------------------------------------------------------------------------*/
#define RESP_LEDS_LENGTH	  0x03
#define DEFAULT_RSP_LEDS_LEN    6 //actual length for harcoded responses

#define NUF_LENTGH			  0x05
#define DEFAULT_NUF_LEN		  8



#define DEFAULT_RSP_GET_NODE_LIST_LEN 10
#define RSP_GET_NODE_LIST_LEN         0x07
#define DEFAULT_NUM_NODES             0x03 //SENSOR_ID_1, 2 and 3
#define DEFAULT_RSP_GET_NODE_LIST     { RSP_GET_NODE_LIST_LEN, PGN_ID,RESPONSE_FRAME, GET_NODE_LIST, DEFAULT_NUM_NODES, SENSOR_ID_1, SENSOR_ID_2, SENSOR_ID_3, EOF_1, EOF_2 }


#define DEFAULT_RSP_TOGGLE_LED        { RESP_LEDS_LENGTH, PGN_ID, RESPONSE_FRAME, TOGGLE_LED, EOF_1,EOF_2 }

//Non-static length responses (get list or num of nodes, temperature, humidity...)
 //#define DEFAULT_RSP_GET_NODE_LIST alredy defined
#define DEFAULT_RSP_GET_NUM_NODES		  {0x04, PGN_ID, RESPONSE_FRAME, GET_NUM_NODES, DEFAULT_NUM_NODES, EOF_1,EOF_2} //3 nodes
 #define GET_NUM_NODES_LEN		7
#define DEFAULT_RSP_GET_TEMPERATURE		  {0x05, PGN_ID, RESPONSE_FRAME, GET_TEMPERATURE, 0x02, 0x05, EOF_1,EOF_2} //25ÂºC
 #define GET_TEMPERATURE_LEN	8
#define DEFAULT_RSP_GET_HUMIDITY		  {0x05, PGN_ID, RESPONSE_FRAME, GET_HUMIDITY, 0x07, 0x07, EOF_1,EOF_2} //77% humidity
 #define GET_HUMIDITY_LEN		8

//Static length responses (when PGN is asked to clear, set or toggle leds)
#define DEFAULT_RSP_CLEAR_LED 		  { RESP_LEDS_LENGTH, PGN_ID, RESPONSE_FRAME, CLEAR_LED, EOF_1,EOF_2 }
#define DEFAULT_RSP_SET_LED 		  { RESP_LEDS_LENGTH, PGN_ID, RESPONSE_FRAME, SET_LED, EOF_1,EOF_2 }
 //#define DEFAULT_RSP_TOGGLE_LED already defined
#define DEFAULT_RSP_SET_ALL_LEDS 	  { RESP_LEDS_LENGTH, PGN_ID, RESPONSE_FRAME, SET_ALL_LEDS, EOF_1,EOF_2 }
#define DEFAULT_RSP_CLEAR_ALL_LEDS	  { RESP_LEDS_LENGTH, PGN_ID, RESPONSE_FRAME, CLEAR_ALL_LEDS, EOF_1,EOF_2 }
#define DEFAULT_RSP_TOGGLE_ALL_LEDS	  { RESP_LEDS_LENGTH, PGN_ID, RESPONSE_FRAME, TOGGLE_ALL_LEDS, EOF_1,EOF_2 }

//NUF messages
#define DEFAULT_NUF_NEW_NODE		{NUF_LENTGH, PGN_ID, NUF_FRAME, NUF_NEW_NODE, 0x01, 0x04,EOF_1,EOF_2 } //aparece un nuevo nodo con ID 0x04
#define DEFAULT_UPDATE_TEMP			{0x06, PGN_ID, NUF_FRAME, NUF_UPDATE_TEMP, 0x01, 0x02,0x03, EOF_1,EOF_2 } //el nodo 1 cambia su temperatura a 23ºC
#define DEFAULT_UPDATE_HUM			{0x06, PGN_ID, NUF_FRAME, NUF_UPDATE_HUM, 0x03, 0x07, 0x07,EOF_1,EOF_2 } //el nodo 3 cambia su humedad a 77%

#endif  
/*
 * _PGN_PROTOCOL_H_
 *------------------------ Nothing Below This Line --------------------------
 */