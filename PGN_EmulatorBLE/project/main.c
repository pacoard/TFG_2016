/* --COPYRIGHT--,BSD
 * Copyright (c) 2012, Texas Instruments Incorporated
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * *  Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *  Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * *  Neither the name of Texas Instruments Incorporated nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * --/COPYRIGHT--*/
//******************************************************************************
//   MSP430F5529LP:  simpleUsbBackchannel example
//
//   Description: 	Demonstrates simple sending over USB, as well as the F5529's
//                  backchannel UART.
//
//   Texas Instruments Inc.
//   August 2013
//******************************************************************************

// Basic MSP430 and driverLib #includes
#include "msp430.h"
#include "driverlib/MSP430F5xx_6xx/wdt_a.h"
#include "driverlib/MSP430F5xx_6xx/ucs.h"
#include "driverlib/MSP430F5xx_6xx/pmm.h"
#include "driverlib/MSP430F5xx_6xx/sfr.h"

#include "types.h"

// Application #includes
#include "BCUart.h"           // Include the backchannel UART "library"
#include "hal.h"              // Modify hal.h to select your hardware
#include "PGN_protocol.h"     // PGN protocol codes and defines

/* You have a choice between implementing this as a CDC USB device, or a HID-
 * Datapipe device.  With CDC, the USB device presents a COM port on the host;
 * you interact with it with a terminal application, like Hyperterminal or
 * Docklight.  With HID-Datapipe, you interact with it using the Java HID Demo
 * App available within the MSP430 USB Developers Package.
 *
 * By default, this app uses CDC.  The HID calls are included, but commented
 * out.
 *
 * See the F5529 LaunchPad User's Guide for simple instructions to convert
 * this demo to HID-Datapipe.  For deeper information on CDC and HID-Datapipe,
 * see the USB API Programmer's Guide in the USB Developers Package.
 */

//buttons and interrupts
#define BUTTONS_MASK 0x02 //both buttons use the same mask
int switchMessage;
BYTE *nufMessage;


// Prototypes
void processReceivedFrame(uint8_t * buf, uint8_t len);
void *copy_buffer( void *dst, const void *src, UINT len );
void __buttonsInit(void);

// Hardcoded responses
const BYTE rsp_get_node_list[DEFAULT_RSP_GET_NODE_LIST_LEN] = DEFAULT_RSP_GET_NODE_LIST;
const BYTE rsp_toggle_led[DEFAULT_RSP_LEDS_LEN] = DEFAULT_RSP_TOGGLE_LED;
// Rest of harcoded responses
//const BYTE rsp_get_node_list = ; already done
BYTE rsp_get_num_nodes[GET_NUM_NODES_LEN] = DEFAULT_RSP_GET_NUM_NODES;
const BYTE rsp_get_temperature[GET_TEMPERATURE_LEN] = DEFAULT_RSP_GET_TEMPERATURE;
const BYTE rsp_get_humidity[GET_HUMIDITY_LEN] = DEFAULT_RSP_GET_HUMIDITY;

const BYTE rsp_clear_led[DEFAULT_RSP_LEDS_LEN] = DEFAULT_RSP_CLEAR_LED;
const BYTE rsp_set_led[DEFAULT_RSP_LEDS_LEN] = DEFAULT_RSP_SET_LED;
 //const BYTE rsp_toggle_led[DEFAULT_RSP_LEDS_LEN] = ; already done
const BYTE rsp_set_all_leds[DEFAULT_RSP_LEDS_LEN] = DEFAULT_RSP_SET_ALL_LEDS;
const BYTE rsp_clear_all_leds[DEFAULT_RSP_LEDS_LEN] = DEFAULT_RSP_CLEAR_ALL_LEDS;
const BYTE rsp_toggle_all_leds[DEFAULT_RSP_LEDS_LEN] = DEFAULT_RSP_TOGGLE_ALL_LEDS;

//NUF
BYTE nuf_new_node[DEFAULT_NUF_LEN] = DEFAULT_NUF_NEW_NODE;
const BYTE nuf_update_temp[9] = DEFAULT_UPDATE_TEMP;
const BYTE nuf_update_hum[9] = DEFAULT_UPDATE_HUM;

// Global variables
WORD rxByteCount;                        // Momentarily stores the number of bytes received
BYTE rx_buf_bcuart[BC_RXBUF_SIZE];       // Same size as the UART's rcv buffer
BYTE tx_buf_bcuart[BC_RXBUF_SIZE];       


BYTE node_id = 0x03;  //increment every NUF_NEW_NODE message
void main(void)
{
    WDTCTL = WDTPW + WDTHOLD;		// Halt the dog

    // MSP430 USB requires a Vcore setting of at least 2.  2 is high enough
	// for 8MHz MCLK, below.
    PMM_setVCore(PMM_BASE, PMM_CORE_LEVEL_2);

    initPorts();           // Config all the GPIOS for low-power (output low)
    initClocks(8000000);   // Config clocks. MCLK=SMCLK=FLL=8MHz; ACLK=REFO=32kHz
    //bcUartInitLaunchpad();   // Init the back-channel UART for F5529 launchpad (9600 bps).
    bcUartInitPGN();        // Init the back-channel UART for PGN (9600 bps).
    switchMessage = 0;
    __buttonsInit();
    __enable_interrupt();  // Enable interrupts globally
    
    while(1)
    {
       // Look for rcv'ed command on the backchannel UART. If any, process.
       rxByteCount = bcUartReadCommandTimeout(rx_buf_bcuart);
       if(rxByteCount != -1)
       {
         processReceivedFrame(rx_buf_bcuart, rxByteCount);
       }
       else // Timeout
       {
         // Do other things
//         tx_buf_bcuart[0] = 0xCC;
//         tx_buf_bcuart[1] = 0xDD;
//         bcUartSend(tx_buf_bcuart, 2);
       }
    }
}

// Process received frame and send the appropiate answer
void processReceivedFrame(uint8_t * buf, uint8_t len)
{
    // 1st byte is the length not counting the two bytes of the EOF and
    // the length byte itself. Check
    if (buf[0] == len-3) //CHECK LENGTH
    { /*switch(buf[1]) {...}*/ // WE DONT CHECK DESTINY ID YET. Should address the message
                                // to the actual device it is intented to. 
      switch(buf[2])  // 3rd byte corresponds to the 1st byte of the code
      {
        
      case REQUEST_FRAME:
        switch(buf[3])  // 4th byte corresponds to the 2nd byte of the code
        {
        // TO-DO: Implement all possible request codes
        case GET_NODE_LIST: //Send message as if there were 3 online sensors in the WSN
          // Send appropiate response
          copy_buffer(tx_buf_bcuart, rsp_get_node_list, DEFAULT_RSP_GET_NODE_LIST_LEN);
          bcUartSend(tx_buf_bcuart, DEFAULT_RSP_GET_NODE_LIST_LEN);
          node_id = 0x03;
          break;
        case GET_NUM_NODES:
          rsp_get_num_nodes[4] = node_id;
          copy_buffer(tx_buf_bcuart, rsp_get_num_nodes, GET_NUM_NODES_LEN);
          bcUartSend(tx_buf_bcuart, GET_NUM_NODES_LEN);
          break;
        case GET_TEMPERATURE:
          copy_buffer(tx_buf_bcuart, rsp_get_temperature, GET_TEMPERATURE_LEN);
          bcUartSend(tx_buf_bcuart, GET_TEMPERATURE_LEN);
          break;
        case GET_HUMIDITY:
          copy_buffer(tx_buf_bcuart, rsp_get_humidity, GET_HUMIDITY_LEN);
          bcUartSend(tx_buf_bcuart, GET_HUMIDITY_LEN);
          break;
        case CLEAR_LED:
          copy_buffer(tx_buf_bcuart, rsp_clear_led, DEFAULT_RSP_LEDS_LEN);
          bcUartSend(tx_buf_bcuart, DEFAULT_RSP_LEDS_LEN);
          break;
        case SET_LED:
          copy_buffer(tx_buf_bcuart, rsp_set_led, DEFAULT_RSP_LEDS_LEN);
          bcUartSend(tx_buf_bcuart, DEFAULT_RSP_LEDS_LEN);
          break;
        case TOGGLE_LED:
          // Send appropiate response
          copy_buffer(tx_buf_bcuart, rsp_toggle_led, DEFAULT_RSP_LEDS_LEN);
          bcUartSend(tx_buf_bcuart, DEFAULT_RSP_LEDS_LEN);
          break;
        case SET_ALL_LEDS:
          copy_buffer(tx_buf_bcuart, rsp_set_all_leds, DEFAULT_RSP_LEDS_LEN);
          bcUartSend(tx_buf_bcuart, DEFAULT_RSP_LEDS_LEN);
          break;
        case CLEAR_ALL_LEDS:
          copy_buffer(tx_buf_bcuart, rsp_clear_all_leds, DEFAULT_RSP_LEDS_LEN);
          bcUartSend(tx_buf_bcuart, DEFAULT_RSP_LEDS_LEN);
          break;
        case TOGGLE_ALL_LEDS:
          copy_buffer(tx_buf_bcuart, rsp_toggle_all_leds, DEFAULT_RSP_LEDS_LEN);
          bcUartSend(tx_buf_bcuart, DEFAULT_RSP_LEDS_LEN);
          break;
        default:
          // Do nothing
          //Should send error message here
          break;
        }
        
      case ACK_FRAME:
        // Do something
        break;
        
      default:
        // Do nothing
        break;
      }
    }
}
          
  
// Auxiliary function to copy the content of a buffer into another
void *copy_buffer( void *dst, const void *src, UINT len )
{
  BYTE *pDst;
  const BYTE *pSrc;

  pSrc = src;
  pDst = dst;

  while ( len-- )
    *pDst++ = *pSrc++;

  return ( pDst );
}

//Initialize buttons and interrupts
void __buttonsInit(void) {
    //desactivate bit => &= ~BUTTONS_MASK;
    //activate bit => |= BUTTONS_MASK;
  
  //left button => sends message
  P2DIR &= ~BUTTONS_MASK; //input direction
  P2REN |= BUTTONS_MASK; //pullup resistor
  P2OUT |= BUTTONS_MASK; //set as pullup resistor
  P2IES |= BUTTONS_MASK; //set hit-lo edge
  //P2SEL |= BUTTONS_MASK; //I/O fuction
  P2IFG &= ~BUTTONS_MASK; //clear flag
  P2IE |= BUTTONS_MASK; //enable interrupt
  
  //right button => switches message to send
  P1DIR &= ~BUTTONS_MASK; //input direction
  P1REN |= BUTTONS_MASK; //pullup resistor
  P1OUT |= BUTTONS_MASK; //set as pullup resistor
  P1IES |= BUTTONS_MASK; //set hit-lo edge
  //P1SEL |= BUTTONS_MASK; //I/O fuction
  P1IFG &= ~BUTTONS_MASK; //clear flag
  P1IE |= BUTTONS_MASK; //enable interrupt
  
}

//Service routine for left button
#pragma vector=PORT2_VECTOR
__interrupt void Port_2(void) {
  P2IFG &= ~BUTTONS_MASK; //clear flag
  
  //send message
    switch (switchMessage) {
    case 0: //new_node
      node_id++;
      nuf_new_node[5] = node_id;
      copy_buffer(tx_buf_bcuart, nuf_new_node, DEFAULT_NUF_LEN);
      bcUartSend(tx_buf_bcuart, DEFAULT_NUF_LEN);
      break;
    case 1: //update_temp
      copy_buffer(tx_buf_bcuart, nuf_update_temp, 9);
      bcUartSend(tx_buf_bcuart, 9);
      break;
    case 2: //update_hum
      copy_buffer(tx_buf_bcuart, nuf_update_hum, 9);
      bcUartSend(tx_buf_bcuart, 9);
      break;
    default: break;
    }
    
}

//Service routine for right button
#pragma vector=PORT1_VECTOR
__interrupt void Port_1(void) {
  P1IFG &= ~BUTTONS_MASK; //clear flag
  
  //switch message
  switchMessage++;
  if (switchMessage > 2) {
    switchMessage = 0;
  }
  
}

